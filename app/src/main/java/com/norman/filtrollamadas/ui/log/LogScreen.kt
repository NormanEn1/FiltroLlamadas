package com.norman.filtrollamadas.ui.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.data.CsvExporter
import com.norman.filtrollamadas.data.db.CallEntry
import com.norman.filtrollamadas.data.db.ListEntry
import com.norman.filtrollamadas.domain.Decision
import com.norman.filtrollamadas.domain.ListType
import com.norman.filtrollamadas.domain.PhoneNumbers
import com.norman.filtrollamadas.domain.Reason
import com.norman.filtrollamadas.ui.components.CallRow
import com.norman.filtrollamadas.ui.components.EmptyState
import com.norman.filtrollamadas.ui.components.IconBadge
import com.norman.filtrollamadas.ui.components.Pill
import com.norman.filtrollamadas.ui.components.ScreenScaffold
import com.norman.filtrollamadas.ui.containerViewModel
import com.norman.filtrollamadas.ui.dateTimeLabel
import com.norman.filtrollamadas.ui.dayLabel
import com.norman.filtrollamadas.ui.localDate
import com.norman.filtrollamadas.ui.rememberContactName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class LogFilter(val label: String) { TODAS("Todas"), DESVIADAS("Desviadas"), PERMITIDAS("Permitidas") }

class LogViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.database.callDao()
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LogFilter.TODAS)

    val entries: StateFlow<List<CallEntry>> = combine(dao.observeAll(), query, filter) { list, q, f ->
        val qDigits = q.filter(Char::isDigit)
        list.filter { e ->
            val byDecision = when (f) {
                LogFilter.TODAS -> true
                LogFilter.DESVIADAS -> e.decision == Decision.DESVIADA.name
                LogFilter.PERMITIDAS -> e.decision == Decision.PERMITIDA.name
            }
            val byText = q.isBlank() ||
                (qDigits.isNotEmpty() && e.numberKey.contains(qDigits)) ||
                (e.note?.contains(q, ignoreCase = true) == true) ||
                Reason.labelOf(e.reason).contains(q, ignoreCase = true)
            byDecision && byText
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(e: CallEntry) {
        viewModelScope.launch { dao.delete(e.id) }
    }

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }

    fun saveNote(e: CallEntry, note: String) {
        viewModelScope.launch { dao.updateNote(e.id, note.trim().ifBlank { null }) }
    }

    fun addToList(e: CallEntry, type: ListType) {
        viewModelScope.launch {
            c.database.listDao().upsert(
                ListEntry(e.numberKey, e.rawNumber.orEmpty(), type.name, e.note.orEmpty(), System.currentTimeMillis())
            )
        }
    }

    fun export(resolver: ContentResolver, uri: Uri, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val out = resolver.openOutputStream(uri) ?: return@runCatching false
                    CsvExporter.write(out, dao.all())
                    true
                }.getOrDefault(false)
            }
            done(ok)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen() {
    val vm = containerViewModel { LogViewModel(it) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<CallEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.export(ctx.contentResolver, uri) { ok -> toast(if (ok) "Registro exportado" else "No se pudo exportar") }
    }

    ScreenScaffold(
        title = "Registro",
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { exporter.launch("filtro_registro_${LocalDate.now()}.csv") }) {
                Icon(Icons.Rounded.FileDownload, contentDescription = "Exportar CSV")
            }
            IconButton(onClick = { confirmClear = true }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Borrar registro")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar número, motivo o nota") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Limpiar") } }
                    } else null,
                    singleLine = true,
                    shape = CircleShape,
                )
            }
            item {
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LogFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { vm.filter.value = f }, label = { Text(f.label) })
                    }
                }
            }
            if (entries.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.History,
                        "Sin llamadas",
                        if (query.isBlank()) "Aquí aparecerá cada llamada que Filtro revise." else "No hay resultados para la búsqueda.",
                    )
                }
            }
            entries.groupBy { localDate(it.timestamp) }.forEach { (day, list) ->
                item(key = "d_${day.toEpochDay()}") {
                    Text(
                        dayLabel(day),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp),
                    )
                }
                item(key = "c_${day.toEpochDay()}") {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            list.forEach { e -> CallRow(e) { selected = e } }
                        }
                    }
                }
            }
        }
    }

    selected?.let { e ->
        CallDetailSheet(
            entry = e,
            onDismiss = { selected = null },
            onSaveNote = { note ->
                vm.saveNote(e, note)
                toast("Nota guardada")
            },
            onList = { type ->
                vm.addToList(e, type)
                selected = null
                toast(if (type == ListType.BLANCA) "Número permitido: siempre timbrará" else "Número bloqueado: siempre se desviará")
            },
            onCopy = {
                ctx.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("Número", e.rawNumber.orEmpty()))
                toast("Número copiado")
            },
            onCall = {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", e.rawNumber.orEmpty(), null)))
            },
            onDelete = {
                vm.delete(e)
                selected = null
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text("¿Borrar todo el registro?") },
            text = { Text("Se eliminarán todas las llamadas registradas en este teléfono. Las listas y los ajustes no cambian.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clear()
                        confirmClear = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallDetailSheet(
    entry: CallEntry,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit,
    onList: (ListType) -> Unit,
    onCopy: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val diverted = entry.decision == Decision.DESVIADA.name
    val hasNumber = entry.numberKey.isNotEmpty()
    var note by rememberSaveable(entry.id) { mutableStateOf(entry.note.orEmpty()) }
    val name = rememberContactName(entry.rawNumber, entry.contactName)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (diverted) IconBadge(Icons.AutoMirrored.Rounded.PhoneForwarded, cs.errorContainer, cs.onErrorContainer, 56.dp)
                else IconBadge(Icons.Rounded.Call, cs.secondaryContainer, cs.onSecondaryContainer, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        name ?: PhoneNumbers.pretty(entry.rawNumber),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (name != null) {
                        Text(PhoneNumbers.pretty(entry.rawNumber), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(dateTimeLabel(entry.timestamp), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (diverted) Pill("Desviada", cs.errorContainer, cs.onErrorContainer)
                else Pill("Permitida", cs.secondaryContainer, cs.onSecondaryContainer)
                Pill(Reason.labelOf(entry.reason))
                entry.lineLabel?.let { Pill(it) }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nota") },
                placeholder = { Text("Ej.: Banco – cobranza") },
                trailingIcon = if (note != entry.note.orEmpty()) {
                    { IconButton(onClick = { onSaveNote(note) }) { Icon(Icons.Rounded.Check, contentDescription = "Guardar nota") } }
                } else null,
                singleLine = true,
            )
            if (hasNumber) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onCall, modifier = Modifier.weight(1f)) { ButtonContent(Icons.Rounded.Call, "Llamar") }
                    FilledTonalButton(onClick = onCopy, modifier = Modifier.weight(1f)) { ButtonContent(Icons.Rounded.ContentCopy, "Copiar") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onList(ListType.BLANCA) }, modifier = Modifier.weight(1f)) {
                        ButtonContent(Icons.Rounded.CheckCircle, "Permitir")
                    }
                    OutlinedButton(onClick = { onList(ListType.NEGRA) }, modifier = Modifier.weight(1f)) {
                        ButtonContent(Icons.Rounded.Block, "Bloquear")
                    }
                }
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = cs.error),
            ) { ButtonContent(Icons.Rounded.Delete, "Eliminar del registro") }
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(text)
}
