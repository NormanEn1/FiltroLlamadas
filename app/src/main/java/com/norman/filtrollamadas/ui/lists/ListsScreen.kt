package com.norman.filtrollamadas.ui.lists

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.automirrored.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.data.db.ListEntry
import com.norman.filtrollamadas.data.db.PendingNumber
import com.norman.filtrollamadas.domain.Decision
import com.norman.filtrollamadas.domain.ListType
import com.norman.filtrollamadas.domain.PhoneNumbers
import com.norman.filtrollamadas.ui.components.ChoiceSegments
import com.norman.filtrollamadas.ui.components.EmptyState
import com.norman.filtrollamadas.ui.components.IconBadge
import com.norman.filtrollamadas.ui.components.Pill
import com.norman.filtrollamadas.ui.components.ScreenScaffold
import com.norman.filtrollamadas.ui.containerViewModel
import com.norman.filtrollamadas.ui.dayLabel
import com.norman.filtrollamadas.ui.localDate
import com.norman.filtrollamadas.ui.timeLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A partir de cuántos intentos se considera "insistente". */
const val INSISTENT_ATTEMPTS = 3

class NumbersViewModel(c: AppContainer) : ViewModel() {
    private val lists = c.database.listDao()
    private val calls = c.database.callDao()

    val pending: StateFlow<List<PendingNumber>> = calls.observePending(Decision.DESVIADA.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reviewed: StateFlow<List<PendingNumber>> = calls.observeReviewed(Decision.DESVIADA.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val white: StateFlow<List<ListEntry>> = lists.observe(ListType.BLANCA.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val black: StateFlow<List<ListEntry>> = lists.observe(ListType.NEGRA.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Clasifica números de la bandeja; devuelve las claves para poder deshacer. */
    fun classify(items: List<PendingNumber>, type: ListType): List<String> {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            lists.upsertAll(items.map { ListEntry(it.numberKey, it.rawNumber.orEmpty(), type.name, "", now) })
        }
        return items.map { it.numberKey }
    }

    /** Deshace una clasificación; los que estaban "revisados" vuelven a ese estado. */
    fun undo(keys: List<String>, restoreReviewed: List<PendingNumber> = emptyList()) {
        viewModelScope.launch {
            lists.deleteAll(keys)
            if (restoreReviewed.isNotEmpty()) {
                val now = System.currentTimeMillis()
                lists.upsertAll(restoreReviewed.map { ListEntry(it.numberKey, it.rawNumber.orEmpty(), ListType.REVISADO.name, "", now) })
            }
        }
    }

    fun add(raw: String, label: String, type: ListType) {
        val key = PhoneNumbers.key(raw)
        if (key.length < 7) return
        viewModelScope.launch {
            lists.upsert(ListEntry(key, raw.trim(), type.name, label.trim(), System.currentTimeMillis()))
        }
    }

    fun remove(e: ListEntry) {
        viewModelScope.launch { lists.delete(e.numberKey) }
    }
}

@Composable
fun NumbersScreen() {
    val vm = containerViewModel { NumbersViewModel(it) }
    val pending by vm.pending.collectAsStateWithLifecycle()
    val reviewed by vm.reviewed.collectAsStateWithLifecycle()
    val white by vm.white.collectAsStateWithLifecycle()
    val black by vm.black.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showReviewed by rememberSaveable { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmReviewAll by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    fun act(items: List<PendingNumber>, type: ListType, message: String) {
        if (items.isEmpty()) return
        // Deshacer debe devolver cada número a su estado anterior (p. ej. seguir "revisado").
        val previouslyReviewed = items.filter { r -> reviewed.any { it.numberKey == r.numberKey } }
        val keys = vm.classify(items, type)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(message, actionLabel = "Deshacer", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) vm.undo(keys, previouslyReviewed)
        }
    }

    fun restore(p: PendingNumber) {
        vm.undo(listOf(p.numberKey))
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("${PhoneNumbers.pretty(p.rawNumber)} volvió a pendientes", duration = SnackbarDuration.Short)
        }
    }

    val insistent = pending.filter { it.attempts >= INSISTENT_ATTEMPTS }

    ScreenScaffold(
        title = "Números",
        snackbarHostState = snackbar,
        floatingActionButton = {
            if (tab != 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Agregar") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ChoiceSegments(
                    options = listOf(
                        0 to "Revisar (${pending.size})",
                        1 to "Permitidos (${white.size})",
                        2 to "Bloqueados (${black.size})",
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                )
            }

            when (tab) {
                0 -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showReviewed,
                                onClick = { showReviewed = false },
                                label = { Text("Pendientes (${pending.size})") },
                            )
                            FilterChip(
                                selected = showReviewed,
                                onClick = { showReviewed = true },
                                label = { Text("Revisados (${reviewed.size})") },
                            )
                        }
                    }
                    item {
                        Text(
                            if (!showReviewed) {
                                "Números desconocidos que se desviaron. Permite a quien reconozcas; " +
                                    "bloquea a los insistentes para que no timbren aunque vuelvan a llamar."
                            } else {
                                "Ya los viste. Siguen desviándose como desconocidos y vuelven a Pendientes " +
                                    "si llaman de nuevo. Aún puedes permitirlos o bloquearlos."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    if (showReviewed) {
                        item {
                            GroupCard {
                                if (reviewed.isEmpty()) {
                                    EmptyState(Icons.Rounded.DoneAll, "Sin revisados", "Los números que marques como revisados aparecerán aquí.")
                                } else {
                                    reviewed.forEach { p ->
                                        PendingRow(
                                            p = p,
                                            onAllow = { act(listOf(p), ListType.BLANCA, "${PhoneNumbers.pretty(p.rawNumber)} permitido") },
                                            onBlock = { act(listOf(p), ListType.NEGRA, "${PhoneNumbers.pretty(p.rawNumber)} bloqueado") },
                                            onRestore = { restore(p) },
                                        )
                                    }
                                }
                            }
                        }
                    } else if (pending.isNotEmpty()) {
                        item {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { act(insistent, ListType.NEGRA, "${insistent.size} insistentes bloqueados") },
                                    enabled = insistent.isNotEmpty(),
                                    label = { Text("Bloquear insistentes (${insistent.size})") },
                                    leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                                )
                                AssistChip(
                                    onClick = { confirmReviewAll = true },
                                    label = { Text("Marcar todo revisado") },
                                    leadingIcon = { Icon(Icons.Rounded.DoneAll, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                                )
                            }
                        }
                    }
                    if (!showReviewed) item {
                        GroupCard {
                            if (pending.isEmpty()) {
                                EmptyState(Icons.Rounded.CheckCircle, "Todo al día", "No hay números desconocidos por revisar.")
                            } else {
                                pending.forEach { p ->
                                    PendingRow(
                                        p = p,
                                        onAllow = { act(listOf(p), ListType.BLANCA, "${PhoneNumbers.pretty(p.rawNumber)} permitido") },
                                        onBlock = { act(listOf(p), ListType.NEGRA, "${PhoneNumbers.pretty(p.rawNumber)} bloqueado") },
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    val isWhite = tab == 1
                    val items = if (isWhite) white else black
                    item {
                        Text(
                            if (isWhite) "Siempre timbran, aunque no estén en tus contactos (domicilios, médico, colegio…)."
                            else "Siempre se desvían, aunque insistan o estén en tus contactos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    item {
                        GroupCard {
                            if (items.isEmpty()) {
                                EmptyState(
                                    if (isWhite) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                    "Lista vacía",
                                    "Clasifica números desde «Revisar», desde el Registro o con el botón Agregar.",
                                )
                            } else {
                                items.forEach { e ->
                                    ListItem(
                                        leadingContent = {
                                            if (isWhite) IconBadge(Icons.Rounded.CheckCircle, cs.secondaryContainer, cs.onSecondaryContainer)
                                            else IconBadge(Icons.Rounded.Block, cs.errorContainer, cs.onErrorContainer)
                                        },
                                        headlineContent = { Text(PhoneNumbers.pretty(e.rawNumber)) },
                                        supportingContent = if (e.label.isNotBlank()) { { Text(e.label) } } else null,
                                        trailingContent = {
                                            IconButton(onClick = { vm.remove(e) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Quitar")
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmReviewAll) {
        AlertDialog(
            onDismissRequest = { confirmReviewAll = false },
            icon = { Icon(Icons.Rounded.DoneAll, contentDescription = null) },
            title = { Text("¿Marcar todo como revisado?") },
            text = {
                Text(
                    "Los ${pending.size} números pasarán a «Revisados». Seguirán desviándose como desconocidos " +
                        "y volverán a Pendientes si llaman de nuevo. Para que nunca timbren, usa «Bloquear»."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReviewAll = false
                    act(pending, ListType.REVISADO, "${pending.size} números marcados como revisados")
                }) { Text("Marcar") }
            },
            dismissButton = { TextButton(onClick = { confirmReviewAll = false }) { Text("Cancelar") } },
        )
    }

    if (showAdd) {
        val type = if (tab == 1) ListType.BLANCA else ListType.NEGRA
        AddNumberDialog(
            type = type,
            onDismiss = { showAdd = false },
            onConfirm = { number, label ->
                vm.add(number, label, type)
                showAdd = false
            },
        )
    }
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { content() }
    }
}

/**
 * Dos líneas: datos arriba y acciones abajo, para que nunca se salga del marco
 * aunque el número sea largo o la pantalla angosta.
 */
@Composable
private fun PendingRow(p: PendingNumber, onAllow: () -> Unit, onBlock: () -> Unit, onRestore: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    val insistent = p.attempts >= INSISTENT_ATTEMPTS
    val badgeBg = if (insistent) cs.errorContainer else cs.surfaceContainerHighest
    val badgeFg = if (insistent) cs.onErrorContainer else cs.onSurfaceVariant

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                if (p.attempts > 1) {
                    Text("${p.attempts}×", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = badgeFg)
                } else {
                    Icon(Icons.AutoMirrored.Rounded.PhoneForwarded, contentDescription = null, tint = badgeFg, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    PhoneNumbers.pretty(p.rawNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(if (p.attempts == 1) "1 intento" else "${p.attempts} intentos")
                        if (insistent) append(" · Insistente")
                        append(" · ${dayLabel(localDate(p.lastAt)).lowercase()} ${timeLabel(p.lastAt)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (insistent) cs.error else cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val pad = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            if (onRestore != null) {
                OutlinedButton(onClick = onRestore, contentPadding = pad, modifier = Modifier.weight(1f)) {
                    Text("Pendiente", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                }
            }
            FilledTonalButton(onClick = onAllow, contentPadding = pad, modifier = Modifier.weight(1f)) {
                Text("Permitir", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(
                onClick = onBlock,
                contentPadding = pad,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = cs.errorContainer, contentColor = cs.onErrorContainer),
            ) {
                Text("Bloquear", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(Modifier.padding(top = 12.dp), color = cs.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun AddNumberDialog(type: ListType, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var number by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    val valid = PhoneNumbers.key(number).length >= 7
    val white = type == ListType.BLANCA

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (white) Icons.Rounded.CheckCircle else Icons.Rounded.Block, contentDescription = null) },
        title = { Text(if (white) "Agregar a permitidos" else "Agregar a bloqueados") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Número") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = number.isNotBlank() && !valid,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Descripción (opcional)") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(number, label) }, enabled = valid) { Text("Agregar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
