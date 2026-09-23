package com.norman.filtrollamadas.ui.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.data.db.CallEntry
import com.norman.filtrollamadas.domain.Decision
import com.norman.filtrollamadas.domain.PhoneNumbers
import com.norman.filtrollamadas.domain.Reason
import com.norman.filtrollamadas.ui.components.ChoiceSegments
import com.norman.filtrollamadas.ui.components.EmptyState
import com.norman.filtrollamadas.ui.components.ScreenScaffold
import com.norman.filtrollamadas.ui.components.SectionCard
import com.norman.filtrollamadas.ui.components.StatTile
import com.norman.filtrollamadas.ui.containerViewModel
import com.norman.filtrollamadas.ui.localDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

enum class StatsRange(val days: Int, val label: String) {
    D7(7, "7 días"),
    D30(30, "30 días"),
    D90(90, "90 días"),
}

data class DayPoint(val date: LocalDate, val diverted: Int, val allowed: Int)

data class TopNumber(val numberKey: String, val rawNumber: String?, val name: String?, val count: Int)

data class StatsUi(
    val range: StatsRange = StatsRange.D7,
    val days: List<DayPoint> = emptyList(),
    val diverted: Int = 0,
    val allowed: Int = 0,
    val previousDiverted: Int = 0,
    /** Variación % de desviadas frente al período anterior; null si no hay con qué comparar. */
    val changePercent: Int? = null,
    val perDay: Double = 0.0,
    val byReason: List<Pair<String, Int>> = emptyList(),
    val byHour: List<Int> = List(24) { 0 },
    val top: List<TopNumber> = emptyList(),
)

class StatsViewModel(c: AppContainer) : ViewModel() {
    val range = MutableStateFlow(StatsRange.D7)

    val ui: StateFlow<StatsUi> = combine(c.database.callDao().observeAll(), range) { all, r ->
        build(all, r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())

    fun setRange(r: StatsRange) {
        range.value = r
    }

    private fun build(all: List<CallEntry>, r: StatsRange): StatsUi {
        val today = LocalDate.now()
        val from = today.minusDays((r.days - 1).toLong())
        val prevFrom = from.minusDays(r.days.toLong())

        val current = all.filter { localDate(it.timestamp) >= from }
        val previous = all.filter { val d = localDate(it.timestamp); d >= prevFrom && d < from }

        val diverted = current.filter { it.decision == Decision.DESVIADA.name }
        val allowed = current.count { it.decision == Decision.PERMITIDA.name }
        val prevDiverted = previous.count { it.decision == Decision.DESVIADA.name }

        val byDay = current.groupBy { localDate(it.timestamp) }
        val days = (0 until r.days).map { i ->
            val d = from.plusDays(i.toLong())
            val entries = byDay[d].orEmpty()
            DayPoint(
                date = d,
                diverted = entries.count { it.decision == Decision.DESVIADA.name },
                allowed = entries.count { it.decision == Decision.PERMITIDA.name },
            )
        }

        val hours = IntArray(24)
        diverted.forEach { e ->
            val h = java.time.Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).hour
            hours[h] = hours[h] + 1
        }

        val top = diverted
            .filter { it.numberKey.isNotEmpty() }
            .groupBy { it.numberKey }
            .map { (key, list) ->
                TopNumber(key, list.first().rawNumber, list.firstNotNullOfOrNull { it.contactName }, list.size)
            }
            .sortedByDescending { it.count }
            .take(5)

        return StatsUi(
            range = r,
            days = days,
            diverted = diverted.size,
            allowed = allowed,
            previousDiverted = prevDiverted,
            changePercent = if (prevDiverted > 0) {
                ((diverted.size - prevDiverted) * 100.0 / prevDiverted).roundToInt()
            } else null,
            perDay = diverted.size.toDouble() / r.days,
            byReason = diverted.groupingBy { Reason.labelOf(it.reason) }.eachCount()
                .toList().sortedByDescending { it.second },
            byHour = hours.toList(),
            top = top,
        )
    }
}

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val vm = containerViewModel { StatsViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme

    ScreenScaffold(title = "Estadísticas", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ChoiceSegments(
                    options = StatsRange.entries.map { it to it.label },
                    selected = ui.range,
                    onSelect = { vm.setRange(it) },
                )
            }

            if (ui.diverted == 0 && ui.allowed == 0) {
                item {
                    EmptyState(
                        Icons.Rounded.Insights,
                        "Sin datos todavía",
                        "Cuando Filtro revise llamadas en este período verás aquí su evolución.",
                    )
                }
                return@LazyColumn
            }

            item { TrendCard(ui) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(ui.diverted.toString(), "Desviadas", Icons.Rounded.Block, cs.errorContainer, cs.onErrorContainer, Modifier.weight(1f))
                    StatTile(ui.allowed.toString(), "Permitidas", Icons.Rounded.Call, cs.secondaryContainer, cs.onSecondaryContainer, Modifier.weight(1f))
                    StatTile(
                        String.format(Locale.US, "%.1f", ui.perDay),
                        "Desviadas al día",
                        Icons.Rounded.Schedule,
                        cs.primaryContainer,
                        cs.onPrimaryContainer,
                        Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionCard(title = "Llamadas por día") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        DailyBarChart(ui.days, cs.error, cs.secondary)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CO"))
                            Text(ui.days.firstOrNull()?.date?.format(fmt).orEmpty(), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            Text(ui.days.lastOrNull()?.date?.format(fmt).orEmpty(), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot("Desviadas", cs.error)
                            LegendDot("Permitidas", cs.secondary)
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Motivos del desvío") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val total = ui.byReason.sumOf { it.second }.coerceAtLeast(1)
                        ui.byReason.forEach { (label, count) ->
                            ReasonBar(label, count, total)
                        }
                    }
                }
            }

            item {
                SectionCard(title = "A qué hora llaman") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        HourChart(ui.byHour, cs.primary)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("0", "6", "12", "18", "23").forEach {
                                Text("${it}h", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (ui.top.isNotEmpty()) {
                item {
                    SectionCard(title = "Los que más insisten") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val max = ui.top.first().count
                            ui.top.forEach { t ->
                                ReasonBar(t.name ?: PhoneNumbers.pretty(t.rawNumber), t.count, max, showPercent = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendCard(ui: StatsUi) {
    val cs = MaterialTheme.colorScheme
    val change = ui.changePercent
    val down = change != null && change < 0
    val flat = change == null || change == 0
    val container = when {
        flat -> cs.surfaceContainerHigh
        down -> cs.secondaryContainer
        else -> cs.errorContainer
    }
    val onContainer = when {
        flat -> cs.onSurface
        down -> cs.onSecondaryContainer
        else -> cs.onErrorContainer
    }
    val icon = when {
        flat -> Icons.AutoMirrored.Rounded.TrendingFlat
        down -> Icons.AutoMirrored.Rounded.TrendingDown
        else -> Icons.AutoMirrored.Rounded.TrendingUp
    }
    val title = when {
        change == null -> "Sin comparación aún"
        change == 0 -> "Igual que el período anterior"
        down -> "Bajaron ${abs(change)}%"
        else -> "Subieron $change%"
    }
    val detail = if (change == null) {
        "Necesito datos de los ${ui.range.days} días anteriores para comparar."
    } else {
        "${ui.diverted} desviadas en estos ${ui.range.days} días, frente a ${ui.previousDiverted} en los ${ui.range.days} anteriores."
    }

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(container).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = onContainer)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = onContainer.copy(alpha = 0.85f))
            }
        }
    }
}

/**
 * Barras apiladas por día (desviadas abajo, permitidas arriba) con escala
 * vertical: líneas de referencia y su valor a la izquierda.
 */
@Composable
private fun DailyBarChart(days: List<DayPoint>, divertedColor: Color, allowedColor: Color) {
    val cs = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = cs.onSurfaceVariant)
    val gridColor = cs.outlineVariant.copy(alpha = 0.6f)

    val max = (days.maxOfOrNull { it.diverted + it.allowed } ?: 0).coerceAtLeast(1)
    val step = niceStep(max)
    val top = ((max + step - 1) / step) * step          // techo redondeado de la escala
    val ticks = (0..top step step).toList()

    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        // Ancho reservado para las etiquetas del eje
        val labels = ticks.map { measurer.measure(it.toString(), labelStyle) }
        val gutter = (labels.maxOf { it.size.width }).toFloat() + 8f.dp.toPx()
        val chartLeft = gutter
        val chartWidth = size.width - chartLeft
        val chartHeight = size.height - labels.first().size.height / 2f  // espacio para la etiqueta superior
        val topPad = labels.first().size.height / 2f

        ticks.forEachIndexed { i, value ->
            val y = topPad + chartHeight - chartHeight * (value.toFloat() / top)
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f.dp.toPx(),
            )
            val layout = labels[i]
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(gutter - 8f.dp.toPx() - layout.size.width, y - layout.size.height / 2f),
            )
        }

        val n = days.size.coerceAtLeast(1)
        val slot = chartWidth / n
        val barW = (slot * 0.62f).coerceAtMost(22f.dp.toPx())
        val radius = CornerRadius(barW / 3, barW / 3)
        days.forEachIndexed { i, p ->
            val x = chartLeft + i * slot + (slot - barW) / 2
            val hDiv = chartHeight * (p.diverted.toFloat() / top)
            val hAll = chartHeight * (p.allowed.toFloat() / top)
            val base = topPad + chartHeight
            if (hAll > 0f) {
                drawRoundRect(
                    color = allowedColor,
                    topLeft = Offset(x, base - hDiv - hAll),
                    size = Size(barW, hAll),
                    cornerRadius = radius,
                )
            }
            if (hDiv > 0f) {
                drawRoundRect(
                    color = divertedColor,
                    topLeft = Offset(x, base - hDiv),
                    size = Size(barW, hDiv),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/** Paso "redondo" (1, 2, 5, 10, 20…) para tener entre 3 y 5 líneas de referencia. */
private fun niceStep(max: Int): Int {
    val raw = ceil(max / 4.0).toInt().coerceAtLeast(1)
    val magnitude = 10.0.pow(floor(log10(raw.toDouble()))).toInt().coerceAtLeast(1)
    return listOf(1, 2, 5, 10).map { it * magnitude }.first { it >= raw }
}

/** 24 barras: volumen de desvíos por hora del día. */
@Composable
private fun HourChart(hours: List<Int>, color: Color) {
    val max = (hours.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val slot = size.width / 24f
        val barW = slot * 0.6f
        hours.forEachIndexed { h, v ->
            val barH = (size.height * (v.toFloat() / max)).coerceAtLeast(if (v > 0) 3f else 0f)
            if (barH > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(h * slot + (slot - barW) / 2, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 3, barW / 3),
                )
            }
        }
    }
}

@Composable
private fun ReasonBar(label: String, count: Int, total: Int, showPercent: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val fraction = (count.toFloat() / total).coerceIn(0f, 1f)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (showPercent) "$count · ${(fraction * 100).roundToInt()}%" else "$count",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(cs.surfaceContainerHighest),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).height(10.dp).clip(CircleShape).background(cs.primary),
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
