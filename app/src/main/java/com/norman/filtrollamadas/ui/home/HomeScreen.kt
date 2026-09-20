package com.norman.filtrollamadas.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.data.db.CallEntry
import com.norman.filtrollamadas.data.settings.AppSettings
import com.norman.filtrollamadas.data.settings.ForwardingState
import com.norman.filtrollamadas.domain.Decision
import com.norman.filtrollamadas.domain.PhoneNumbers
import com.norman.filtrollamadas.telephony.Telefonia
import com.norman.filtrollamadas.ui.SetupStatus
import com.norman.filtrollamadas.ui.components.CallRow
import com.norman.filtrollamadas.ui.components.EmptyState
import com.norman.filtrollamadas.ui.components.IconBadge
import com.norman.filtrollamadas.ui.components.Pill
import com.norman.filtrollamadas.ui.components.ScreenScaffold
import com.norman.filtrollamadas.ui.components.SectionCard
import com.norman.filtrollamadas.ui.components.StatTile
import com.norman.filtrollamadas.ui.containerViewModel
import com.norman.filtrollamadas.ui.dateTimeLabel
import com.norman.filtrollamadas.ui.rememberSetupStatus
import com.norman.filtrollamadas.ui.roleRequestIntent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class ForwardAction { ACTIVAR, CONSULTAR, DESACTIVAR }

data class HomeUi(
    val settings: AppSettings = AppSettings(),
    val divertedToday: Int = 0,
    val allowedToday: Int = 0,
    val divertedWeek: Int = 0,
    val recent: List<CallEntry> = emptyList(),
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.database.callDao()
    private val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val weekStart = startOfDay - 6 * 86_400_000L

    val ui: StateFlow<HomeUi> = combine(
        c.settings.flow,
        dao.observeCount(Decision.DESVIADA.name, startOfDay),
        dao.observeCount(Decision.PERMITIDA.name, startOfDay),
        dao.observeCount(Decision.DESVIADA.name, weekStart),
        dao.observeRecent(5),
    ) { s, dToday, aToday, dWeek, recent -> HomeUi(s, dToday, aToday, dWeek, recent) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { c.settings.update { it.copy(enabled = value) } }
    }

    fun recordForwarding(action: ForwardAction, target: String) {
        if (action == ForwardAction.CONSULTAR) return
        viewModelScope.launch {
            c.settings.update {
                it.copy(
                    forwardingState = if (action == ForwardAction.ACTIVAR) ForwardingState.ACTIVADO else ForwardingState.DESACTIVADO,
                    forwardingTarget = target,
                    forwardingAt = System.currentTimeMillis(),
                )
            }
        }
    }
}

@Composable
fun HomeScreen(pendingCount: Int, onOpenNumbers: () -> Unit, onOpenLog: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = containerViewModel { HomeViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui.settings
    val ctx = LocalContext.current
    val setup = rememberSetupStatus()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ForwardAction?>(null) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    val destinationOk = s.destinationNumber.isNotBlank()
    val canEnable = setup.required && destinationOk

    fun forward(action: ForwardAction) {
        val code = when (action) {
            ForwardAction.ACTIVAR -> Telefonia.codeActivate(s.destinationDialable)
            ForwardAction.CONSULTAR -> Telefonia.CODE_QUERY
            ForwardAction.DESACTIVAR -> Telefonia.CODE_DEACTIVATE
        }
        if (Telefonia.dial(ctx, code, s)) {
            vm.recordForwarding(action, s.destinationDialable)
        } else {
            permLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
            scope.launch { snackbar.showSnackbar("Concede el permiso de llamadas para marcar el código") }
        }
    }

    ScreenScaffold(title = "Filtro", snackbarHostState = snackbar) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                HeroCard(enabled = s.enabled, canEnable = canEnable, destLabel = s.destinationLabel, onToggle = { vm.setEnabled(it) })
            }
            if (!setup.complete || !destinationOk) {
                item {
                    SetupCard(
                        setup = setup,
                        destinationOk = destinationOk,
                        onRole = { roleRequestIntent(ctx)?.let { roleLauncher.launch(it) } },
                        onPermissions = { permLauncher.launch(it) },
                        onDestination = onOpenSettings,
                    )
                }
            }
            if (pendingCount > 0) {
                item { PendingCard(pendingCount, onOpenNumbers) }
            }
            item { ForwardingCard(s, onAction = { pending = it }, onConfigure = onOpenSettings) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val cs = MaterialTheme.colorScheme
                    StatTile(ui.divertedToday.toString(), "Desviadas hoy", Icons.Rounded.Block, cs.errorContainer, cs.onErrorContainer, Modifier.weight(1f))
                    StatTile(ui.divertedWeek.toString(), "Desviadas 7 días", Icons.Rounded.DateRange, cs.primaryContainer, cs.onPrimaryContainer, Modifier.weight(1f))
                    StatTile(ui.allowedToday.toString(), "Permitidas hoy", Icons.Rounded.Call, cs.secondaryContainer, cs.onSecondaryContainer, Modifier.weight(1f))
                }
            }
            item {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Actividad reciente",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        TextButton(onClick = onOpenLog) { Text("Ver todo") }
                    }
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        if (ui.recent.isEmpty()) {
                            EmptyState(Icons.Rounded.History, "Sin actividad", "Cuando Filtro revise una llamada aparecerá aquí.")
                        } else {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                ui.recent.forEach { CallRow(it, onClick = onOpenLog) }
                            }
                        }
                    }
                }
            }
        }
    }

    pending?.let { action ->
        ConfirmForwardDialog(
            action = action,
            s = s,
            onDismiss = { pending = null },
            onConfirm = {
                pending = null
                forward(action)
            },
        )
    }
}

@Composable
private fun HeroCard(enabled: Boolean, canEnable: Boolean, destLabel: String, onToggle: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val brush = if (enabled) {
        Brush.linearGradient(listOf(cs.primary, cs.tertiary))
    } else {
        Brush.linearGradient(listOf(cs.surfaceContainerHigh, cs.surfaceContainerHighest))
    }
    val fg = if (enabled) cs.onPrimary else cs.onSurface
    Card(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(brush).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(fg.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (enabled) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (enabled) "Protección activa" else "Protección apagada",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = fg,
                        )
                        Text(
                            if (enabled) "Las llamadas no identificadas se desvían a $destLabel" else "Todas las llamadas timbran normalmente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = fg.copy(alpha = 0.85f),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        enabled = canEnable || enabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = cs.primary,
                            checkedTrackColor = cs.onPrimary,
                            checkedBorderColor = cs.onPrimary,
                        ),
                    )
                }
                if (!canEnable && !enabled) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Completa la configuración pendiente para activarla.",
                        style = MaterialTheme.typography.labelLarge,
                        color = fg.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingCard(count: Int, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Inbox, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (count == 1) "1 número por revisar" else "$count números por revisar",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Permite a quien conozcas o bloquea a los insistentes", style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = onOpen) { Text("Revisar") }
        }
    }
}

@Composable
private fun SetupCard(
    setup: SetupStatus,
    destinationOk: Boolean,
    onRole: () -> Unit,
    onPermissions: (Array<String>) -> Unit,
    onDestination: () -> Unit,
) {
    SectionCard(title = "Configuración pendiente") {
        SetupRow(Icons.Rounded.Shield, "Filtro de llamadas", "Permite a Filtro revisar las llamadas entrantes", setup.role, "Asignar", onRole)
        SetupRow(Icons.Rounded.Contacts, "Contactos", "Para reconocer a tus contactos (solo lectura)", setup.contacts, "Permitir") {
            onPermissions(arrayOf(Manifest.permission.READ_CONTACTS))
        }
        SetupRow(Icons.Rounded.History, "Registro de llamadas", "Deja pasar a quienes llamaste hace poco", setup.callLog, "Permitir") {
            onPermissions(arrayOf(Manifest.permission.READ_CALL_LOG))
        }
        SetupRow(Icons.Rounded.SimCard, "Líneas y marcación", "Elegir la SIM y marcar los códigos de desvío", setup.callPhone && setup.phoneState, "Permitir") {
            onPermissions(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SetupRow(Icons.Rounded.Notifications, "Notificaciones", "Aviso cada vez que se desvía una llamada", setup.notifications, "Permitir") {
                onPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
        SetupRow(Icons.Rounded.PhoneIphone, "Número destino", "El teléfono que atenderá las llamadas desviadas", destinationOk, "Configurar", onDestination)
    }
}

@Composable
private fun SetupRow(icon: ImageVector, title: String, subtitle: String, done: Boolean, action: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            if (done) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Listo", tint = MaterialTheme.colorScheme.secondary)
            } else {
                FilledTonalButton(onClick = onClick) { Text(action) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ForwardingCard(s: AppSettings, onAction: (ForwardAction) -> Unit, onConfigure: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    SectionCard(title = "Redirección") {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.PhoneIphone, cs.secondaryContainer, cs.onSecondaryContainer)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.destinationLabel.ifBlank { "Destino" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (s.destinationNumber.isBlank()) "Sin número configurado" else PhoneNumbers.pretty(s.destinationNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onConfigure) { Icon(Icons.Rounded.Edit, contentDescription = "Editar destino") }
            }
            Spacer(Modifier.height(12.dp))
            when (s.forwardingState) {
                ForwardingState.ACTIVADO -> Pill("Activado · ${dateTimeLabel(s.forwardingAt)}", cs.secondaryContainer, cs.onSecondaryContainer, Icons.Rounded.CheckCircle)
                ForwardingState.DESACTIVADO -> Pill("Desactivado · ${dateTimeLabel(s.forwardingAt)}", icon = Icons.Rounded.Info)
                else -> Pill("Aún no activado en el operador", icon = Icons.Rounded.Info)
            }
            if (s.forwardingState == ForwardingState.ACTIVADO && s.forwardingTarget != s.destinationDialable) {
                Spacer(Modifier.height(8.dp))
                Pill("Cambiaste el destino: vuelve a activar el desvío", cs.errorContainer, cs.onErrorContainer, Icons.Rounded.Warning)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val pad = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                Button(
                    onClick = { onAction(ForwardAction.ACTIVAR) },
                    enabled = s.destinationNumber.isNotBlank(),
                    contentPadding = pad,
                    modifier = Modifier.weight(1f),
                ) { Text("Activar") }
                OutlinedButton(onClick = { onAction(ForwardAction.CONSULTAR) }, contentPadding = pad, modifier = Modifier.weight(1f)) { Text("Verificar") }
                OutlinedButton(onClick = { onAction(ForwardAction.DESACTIVAR) }, contentPadding = pad, modifier = Modifier.weight(1f)) { Text("Desactivar") }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Tu operador desvía las llamadas que Filtro rechaza. El desvío se configura una sola vez.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfirmForwardDialog(action: ForwardAction, s: AppSettings, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val title: String
    val body: String
    val code: String
    when (action) {
        ForwardAction.ACTIVAR -> {
            title = "Activar desvío"
            body = "Se marcará el código de tu operador para que las llamadas que Filtro rechace lleguen a " +
                "${s.destinationLabel} (${PhoneNumbers.pretty(s.destinationNumber)})."
            code = Telefonia.codeActivate(s.destinationDialable)
        }
        ForwardAction.CONSULTAR -> {
            title = "Verificar desvío"
            body = "Tu operador mostrará en pantalla el estado actual del desvío."
            code = Telefonia.CODE_QUERY
        }
        ForwardAction.DESACTIVAR -> {
            title = "Desactivar desvío"
            body = "Las llamadas rechazadas volverán a ir a tu buzón habitual."
            code = Telefonia.CODE_DEACTIVATE
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PhoneForwarded, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column {
                Text(body)
                Spacer(Modifier.height(12.dp))
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        code,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
                if (s.lineLabel.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Se marcará por: ${s.lineLabel}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors()) { Text("Marcar código") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
