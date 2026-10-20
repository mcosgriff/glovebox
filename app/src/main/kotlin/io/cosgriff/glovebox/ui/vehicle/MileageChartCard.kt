package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.ChartPoint
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.MileageChart
import io.cosgriff.glovebox.domain.MileageSeries
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.monthYear
import io.cosgriff.glovebox.ui.theme.PlexMono
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** A reading on the chart, by what the list would open: its own row, or the service record it was logged with. */
fun ChartPoint.key(): String = "${if (row.reading.fromRecord) "record" else "reading"}:${row.reading.id}"

/**
 * The odometer over time (#288), above the list of readings: the same readings, flagged the same
 * way. [series] is null when the range has fewer than two readings; [readings] is how many there
 * are in all, which says whether another range would draw. [years] is how far back it reaches, null
 * for every reading; the slider runs from one year to [most], the years the readings cover, which
 * is every reading, since [first].
 */
@Composable
fun MileageChartCard(
    series: MileageSeries?,
    readings: Int,
    years: Int?,
    most: Int,
    first: LocalDate,
    unit: DistanceUnit,
    selected: String?,
    onYears: (Int?) -> Unit,
    onSelect: (String?) -> Unit,
    onOpen: (ChartPoint) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    if (readings < 2) {
        Text(stringResource(R.string.chart_needs_two), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        return
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = scheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Readings that all fall within a year leave nothing to choose.
            if (most > 1) YearsSlider(years, most, first, onYears)
            if (series == null) {
                Text(stringResource(R.string.chart_too_few_in_range), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                return@Column
            }
            PerYear(series, unit)
            Plot(series, unit, selected, onSelect)
            Legend(swap = series.swapOn != null, toCheck = series.toCheck > 0)
            series.points.firstOrNull { it.key() == selected }?.let { point -> Selected(point, unit) { onOpen(point) } }
        }
    }
}

/** One year at a time, up to every reading: a 15-year-old vehicle has 15 ranges, not three. */
@Composable
private fun YearsSlider(years: Int?, most: Int, first: LocalDate, onYears: (Int?) -> Unit) {
    val shown = years?.coerceAtMost(most) ?: most
    val label = if (shown == most) {
        stringResource(R.string.chart_range_all, first.format(monthYear))
    } else {
        pluralStringResource(R.plurals.chart_range_last, shown, shown.asNumber())
    }
    val name = stringResource(R.string.chart_range)
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Slider(
            value = shown.toFloat(),
            onValueChange = { v -> v.roundToInt().let { onYears(if (it >= most) null else it) } },
            valueRange = 1f..most.toFloat(),
            // Stops at each whole year between the two ends.
            steps = most - 2,
            modifier = Modifier.semantics {
                contentDescription = name
                stateDescription = label
            },
        )
    }
}

/** The estimate the user asked for: the reminders' pace, over a year. */
@Composable
private fun PerYear(series: MileageSeries, unit: DistanceUnit) {
    val scheme = MaterialTheme.colorScheme
    val perYear = series.perYear
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (perYear == null) {
            Text(stringResource(R.string.chart_no_pace), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.chart_per_year, perYear.asDistance(unit)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.chart_per_year_note, series.start.format(monthYear), series.end.format(monthYear)),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Plot(series: MileageSeries, unit: DistanceUnit, selected: String?, onSelect: (String?) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val line = scheme.primary
    val check = scheme.error
    val guide = scheme.outline
    val pace = scheme.onSurfaceVariant
    val hole = scheme.surface
    val description = describe(series, unit)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.height(PLOT_HEIGHT), verticalArrangement = Arrangement.SpaceBetween) {
            Text(series.high.asDistance(unit), style = MaterialTheme.typography.labelSmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
            Text(series.low.asDistance(unit), style = MaterialTheme.typography.labelSmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
        }
        // The dates go under the plot itself, so the first one sits under the first reading.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(
                Modifier.fillMaxWidth().height(PLOT_HEIGHT)
                    .semantics { contentDescription = description }
                    .pointerInput(series) {
                        detectTapGestures { tap ->
                            val nearest = series.points.minByOrNull { abs(spot(series, it.date, it.plotted, size.width.toFloat(), size.height.toFloat(), INSET.toPx()).x - tap.x) }
                            onSelect(nearest?.key()?.takeIf { it != selected })
                        }
                    },
            ) {
                drawSeries(series, selected, line = line, check = check, guide = guide, pace = pace, hole = hole)
            }
            Row(Modifier.fillMaxWidth()) {
                Text(series.start.format(monthYear), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(series.end.format(monthYear), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Where [plotted] on [date] falls in a canvas [width] by [height], [inset] in from each edge so a
 * point there is drawn whole: dates across, the distance covered up.
 */
private fun spot(series: MileageSeries, date: LocalDate, plotted: Int, width: Float, height: Float, inset: Float): Offset {
    val w = width - 2 * inset
    val h = height - 2 * inset
    val days = ChronoUnit.DAYS.between(series.start, series.end).coerceAtLeast(1)
    val x = ChronoUnit.DAYS.between(series.start, date).toFloat() / days * w
    val span = series.high - series.low
    val y = if (span == 0) h / 2 else h - (plotted - series.low).toFloat() / span * h
    return Offset(x + inset, y + inset)
}

private fun DrawScope.drawSeries(series: MileageSeries, selected: String?, line: Color, check: Color, guide: Color, pace: Color, hole: Color) {
    val inset = INSET.toPx()
    fun at(date: LocalDate, plotted: Int) = spot(series, date, plotted, size.width, size.height, inset)
    fun at(p: ChartPoint) = at(p.date, p.plotted)

    series.swapOn?.let { on ->
        val x = at(on, series.low).x
        drawLine(guide, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    }
    // The pace: a straight line from the first reading at the reminders' rate.
    series.milesPerDay?.let { perDay ->
        val first = series.points.first()
        val end = at(series.end, (first.plotted + perDay * ChronoUnit.DAYS.between(series.start, series.end)).toInt())
        drawLine(pace, at(first), end, strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    }
    val path = Path()
    series.points.forEachIndexed { i, p -> at(p).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
    drawPath(path, line, style = Stroke(width = 2.dp.toPx()))

    // Readings closer together than a mark is wide share the line; the tapped one is always marked.
    val marked = MileageChart.marked(series.points, room = ((size.width - 2 * inset) / MARK_GAP.toPx()).toInt())
    val shown = marked + series.points.filter { it.key() == selected && it !in marked }
    shown.forEach { p ->
        val c = at(p)
        val colour = if (p.doubtful) check else line
        val r = 4.dp.toPx()
        if (p.key() == selected) drawLine(guide, Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 1.dp.toPx())
        // A service is a filled point, a mileage update a hollow one.
        drawCircle(if (p.row.reading.fromRecord) colour else hole, r, c)
        drawCircle(colour, r, c, style = Stroke(width = 2.dp.toPx()))
        // A reading to check has a ring round it, so it isn't told by colour alone.
        if (p.doubtful) drawCircle(check, r + 4.dp.toPx(), c, style = Stroke(width = 1.5.dp.toPx()))
        if (p.key() == selected || p.row.current) drawCircle(colour, r + 3.dp.toPx(), c, style = Stroke(width = 1.dp.toPx()))
    }
}

/** What TalkBack says for the chart; the list under it reads out every reading. */
@Composable
private fun describe(series: MileageSeries, unit: DistanceUnit): String = listOfNotNull(
    pluralStringResource(R.plurals.chart_description, series.points.size, series.points.size.asNumber(), series.start.format(monthYear), series.end.format(monthYear)),
    series.perYear?.let { stringResource(R.string.chart_per_year, it.asDistance(unit)) },
    series.toCheck.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.chart_to_check, it, it.asNumber()) },
).joinToString(". ")

@Composable
private fun Legend(swap: Boolean, toCheck: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val line = scheme.primary
    val check = scheme.error
    val hole = scheme.surface
    val guide = scheme.outline
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Key(stringResource(R.string.chart_key_service)) { drawCircle(line, 4.dp.toPx()) }
        Key(stringResource(R.string.chart_key_update)) {
            drawCircle(hole, 4.dp.toPx())
            drawCircle(line, 4.dp.toPx(), style = Stroke(2.dp.toPx()))
        }
        if (toCheck) {
            Key(stringResource(R.string.chart_key_check)) {
                drawCircle(check, 4.dp.toPx(), style = Stroke(2.dp.toPx()))
                drawCircle(check, 8.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            }
        }
        if (swap) {
            Key(stringResource(R.string.chart_key_swap)) {
                drawLine(guide, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            }
        }
    }
}

@Composable
private fun Key(label: String, mark: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(18.dp), onDraw = mark)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The tapped reading, as typed, with the way to what the list would open for it. */
@Composable
private fun Selected(point: ChartPoint, unit: DistanceUnit, onOpen: () -> Unit) {
    val reading = point.row.reading
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(reading.odometer.asDistance(unit), style = MaterialTheme.typography.titleSmall, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(if (reading.fromRecord) R.string.readings_from_service else R.string.readings_from_update, reading.date.format(longDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (reading.fromRecord) R.string.readings_open_record else R.string.readings_correct_or_delete))
        }
    }
}

private val PLOT_HEIGHT = 180.dp

/** The least space between two readings' marks: a mark is 8dp across, with its ring 16. */
private val MARK_GAP = 14.dp

/** Room round the plot, so a point on its edge is drawn whole. */
private val INSET = 8.dp
