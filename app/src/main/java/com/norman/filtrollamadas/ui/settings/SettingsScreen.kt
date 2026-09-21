package com.norman.filtrollamadas.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.norman.filtrollamadas.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonAddDisabled
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.data.settings.AppSettings
import com.norman.filtrollamadas.data.settings.ThemeMode
import com.norman.filtrollamadas.telephony.PhoneLine
import com.norman.filtrollamadas.telephony.Telefonia
import com.norman.filtrollamadas.ui.components.ChoiceRow
import com.norman.filtrollamadas.ui.components.ChoiceSegments
import com.norman.filtrollamadas.ui.components.ScreenScaffold
import com.norman.filtrollamadas.ui.components.SectionCard
import com.norman.filtrollamadas.ui.components.SwitchRow
import com.norman.filtrollamadas.ui.containerViewModel
import com.norman.filtrollamadas.ui.rememberSetupStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = c.settings.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { c.settings.update(block) }
    }

    fun setRetention(days: Int) {
        viewModelScope.launch {
            c.settings.update { it.copy(retentionDays = days) }
            c.purgeOldEntries()
        }
    }
}

@Composable
fun SettingsScreen() {
    val vm = containerViewModel { SettingsViewModel(it) }
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val setup = rememberSetupStatus()
    val lines = remember(setup) { Telefonia.lines(ctx) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    ScreenScaffold(title = "Ajustes", snackbarHostState = snackbar) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                DestinationSection(s) { number, label, prefix ->
                    vm.update { it.copy(destinationNumber = number, destinationLabel = label, useCountryPrefix = prefix) }
                    scope.launch { snackbar.showSnackbar("Destino guardado. Recuerda activar el desvío en Inicio.") }
                }
            }
            item {
                SectionCard(title = "Línea protegida") {
                    if (!setup.phoneState) {
                        ListItem(
                            headlineContent = { Text("Acceso a las líneas") },
                            supportingContent = { Text("Concede el permiso para elegir la SIM que se filtra") },
                            trailingContent = {
                                FilledTonalButton(onClick = { permLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE)) }) {
                                    Text("Permitir")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    } else {
                        LineOption("Todas las líneas", "Filtra en cualquier SIM", s.lineId.isBlank()) {
                            vm.update { it.copy(lineId = "", lineComponent = "", lineLabel = "") }
                        }
                        lines.forEach { line: PhoneLine ->
                            LineOption(line.label, "Solo esta SIM; los códigos de desvío se marcan por ella", s.lineId == line.id) {
                                vm.update { it.copy(lineId = line.id, lineComponent = line.component, lineLabel = line.label) }
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Reglas de filtrado") {
                    SwitchRow("Desviar números ocultos", "Llamadas con número privado o restringido", Icons.Rounded.VisibilityOff, s.divertHidden) { v ->
                        vm.update { it.copy(divertHidden = v) }
                    }
                    SwitchRow("Desviar números desconocidos", "Números que no están en tus contactos", Icons.Rounded.PersonAddDisabled, s.divertUnknown) { v ->
                        vm.update { it.copy(divertUnknown = v) }
                    }
                    ChoiceRow("Dejar pasar a quienes llamaste", "Si los llamaste en este período, timbran normal", Icons.Rounded.History) {
                        ChoiceSegments(
                            options = listOf(0 to "No", 7 to "7 d", 15 to "15 d", 30 to "30 d", 90 to "90 d"),
                            selected = s.recentDays,
                            enabled = setup.callLog,
                        ) { v -> vm.update { it.copy(recentDays = v) } }
                    }
                    SwitchRow("Dejar pasar si insiste", "Si el mismo número vuelve a llamar pronto, timbra", Icons.Rounded.Replay, s.allowRepeat) { v ->
                        vm.update { it.copy(allowRepeat = v) }
                    }
                    if (s.allowRepeat) {
                        ChoiceRow("Ventana de rellamada", null, Icons.Rounded.Timer) {
                            ChoiceSegments(
                                options = listOf(2 to "2 min", 3 to "3 min", 5 to "5 min", 10 to "10 min"),
                                selected = s.repeatMinutes,
                            ) { v -> vm.update { it.copy(repeatMinutes = v) } }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Notificaciones y registro") {
                    SwitchRow(
                        "Avisar cada desvío",
                        if (setup.notifications) "Un solo aviso con el total del día (suena solo la primera vez)" else "Falta el permiso de notificaciones",
                        Icons.Rounded.Notifications,
                        s.notifyOnDivert,
                    ) { v -> vm.update { it.copy(notifyOnDivert = v) } }
                    ChoiceRow("Conservar el registro", "Las llamadas más antiguas se borran solas", Icons.Rounded.Storage) {
                        ChoiceSegments(
                            options = listOf(30 to "1 mes", 90 to "3 meses", 180 to "6 meses", 365 to "1 año"),
                            selected = s.retentionDays,
                        ) { v -> vm.setRetention(v) }
                    }
                }
            }
            item {
                SectionCard(title = "Apariencia") {
                    ChoiceRow("Tema", null, Icons.Rounded.DarkMode) {
                        ChoiceSegments(
                            options = listOf(ThemeMode.SISTEMA to "Sistema", ThemeMode.CLARO to "Claro", ThemeMode.OSCURO to "Oscuro"),
                            selected = s.themeMode,
                        ) { v -> vm.update { it.copy(themeMode = v) } }
                    }
                    SwitchRow(
                        "Colores del fondo de pantalla",
                        "Usa la paleta dinámica de Android",
                        Icons.Rounded.Palette,
                        s.dynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    ) { v -> vm.update { it.copy(dynamicColor = v) } }
                }
            }
            item {
                SectionCard(title = "Privacidad") {
                    ListItem(
                        leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        headlineContent = { Text("Sin acceso a Internet") },
                        supportingContent = {
                            Text("Filtro no tiene permiso de red: tus contactos, tu registro de llamadas y el log nunca salen del teléfono.")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    ListItem(
                        modifier = Modifier.clickable {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                            )
                        },
                        leadingContent = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                        headlineContent = { Text("Permisos de la app") },
                        supportingContent = { Text("Revisar o retirar permisos en los ajustes de Android") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            item { AboutSection() }
        }
    }
}

@Composable
private fun DestinationSection(s: AppSettings, onSave: (String, String, Boolean) -> Unit) {
    var number by rememberSaveable(s.destinationNumber) { mutableStateOf(s.destinationNumber) }
    var label by rememberSaveable(s.destinationLabel) { mutableStateOf(s.destinationLabel) }
    var prefix by rememberSaveable(s.useCountryPrefix) { mutableStateOf(s.useCountryPrefix) }
    val digits = number.filter(Char::isDigit)
    val valid = digits.length in 7..15
    val changed = digits != s.destinationNumber || label.trim() != s.destinationLabel || prefix != s.useCountryPrefix

    SectionCard(title = "Destino de la redirección") {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = number,
                onValueChange = { v -> number = v.filter { it.isDigit() || it == ' ' || it == '+' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Número destino") },
                placeholder = { Text("3001234567") },
                leadingIcon = { Icon(Icons.Rounded.PhoneIphone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = number.isNotBlank() && !valid,
                supportingText = {
                    Text(
                        if (number.isNotBlank() && !valid) "Debe tener entre 7 y 15 dígitos"
                        else "El teléfono que atenderá las llamadas (p. ej. tu iPhone con filtrado)"
                    )
                },
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del destino") },
                placeholder = { Text("iPhone") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
                singleLine = true,
            )
        }
        SwitchRow(
            "Anteponer indicativo 57",
            "Úsalo si el operador rechaza el código sin indicativo",
            Icons.Rounded.Public,
            prefix,
        ) { prefix = it }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (valid) "Código: **67*${if (prefix && digits.length == 10) "57$digits" else digits}#" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onSave(digits, label.trim().ifBlank { "Destino" }, prefix) }, enabled = valid && changed) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val info = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
    }
    val version = info?.versionName ?: "1.0.0"
    val build = info?.longVersionCode ?: 1L

    SectionCard(title = "Acerca de") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(52.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Filtro", style = MaterialTheme.typography.titleLarge)
                Text("Neom · Versión $version (2026)", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Text("Compilación $build", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        AboutRow(Icons.Rounded.Shield, "Qué hace", "Silencia las llamadas no identificadas y las desvía al teléfono destino que configures.")
        AboutRow(Icons.Rounded.Lock, "Privacidad", "Sin permiso de Internet. Todo se decide y se guarda en este teléfono.")
        AboutRow(Icons.Rounded.Copyright, "Licencia", "MIT · Código abierto, uso libre y gratuito. © 2026 Neom.")
        AboutRow(Icons.Rounded.PhoneAndroid, "Requisitos", "Android 10 o superior · desvío condicional del operador (**67*)")
        AboutRow(Icons.Rounded.Code, "Construida con", "Kotlin · Jetpack Compose · Material 3 · Room")
        AboutRow(Icons.Rounded.Apps, "Paquete", ctx.packageName)
        AboutRow(
            Icons.Rounded.Code,
            "Código fuente",
            "github.com/NormanEn1/FiltroLlamadas",
            onClick = { openUrl(ctx, REPO_URL) },
        )
        AboutRow(
            Icons.Rounded.BugReport,
            "Reportar un problema",
            "Abre un issue en GitHub (indica operador y modelo)",
            onClick = { openUrl(ctx, "$REPO_URL/issues") },
        )
    }
}

private const val REPO_URL = "https://github.com/NormanEn1/FiltroLlamadas"

/** Abre el navegador del teléfono; no requiere permiso de red en esta app. */
private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun LineOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
