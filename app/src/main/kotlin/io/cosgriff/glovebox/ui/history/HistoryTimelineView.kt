package io.cosgriff.glovebox.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.HistoryTimeline
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.LineHue
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.ReminderProgress
import io.cosgriff.glovebox.domain.TimelineRow
import io.cosgriff.glovebox.domain.Visits
import io.cosgriff.glovebox.ui.components.Chevron
import io.cosgriff.glovebox.ui.service.HistoryFocus
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.catalogCategoryLabel
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.monthYear
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.service.yearOnly
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors
import java.time.LocalDate

/** How History shows its records. Kept in the saved state with the sort order; nothing is stored. */
enum class HistoryView { LIST, TIMELINE }

/** The List / Timeline switch under History's header. Design: 6e · History — timeline view. */
@Composable
internal fun HistoryViewSwitch(view: HistoryView, onChoose: (HistoryView) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // In dark the page's own surface hardly stands off the track, so the chosen side takes the next step up.
    val chosenColor = if (scheme.background.luminance() < 0.5f) scheme.surfaceContainerHighest else scheme.surface
    val segment = RoundedCornerShape(9.dp)
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(scheme.surfaceVariant).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HistoryView.entries.forEach { v ->
            val on = v == view
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .then(if (on) Modifier.shadow(1.dp, segment).background(chosenColor, segment) else Modifier)
                    .clip(segment)
                    .selectable(selected = on, role = Role.Tab, onClick = { if (!on) onChoose(v) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (v == HistoryView.LIST) R.string.history_view_list else R.string.history_view_timeline),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * History as a timeline: the vehicle's next reminder, then [groups] (records, or visits of several) newest
 * first under their years, with the time between each, back to the oldest record. [next] is the reminder
 * the garage card calls "Next", from the same statuses as the Reminders tab; null shows none.
 */
@Composable
internal fun HistoryTimelineList(
    vehicle: VehicleEntity,
    groups: List<List<ServiceRecordWithItems>>,
    next: Pair<ItemStatus, ReminderProgress>?,
    since: LocalDate?,
    fresh: HistoryFocus?,
    listState: LazyListState,
    onOpenRecord: (Long) -> Unit,
    onLogIt: ((vehicleId: Long, scheduleItemId: Long) -> Unit)?,
) {
    val unit = vehicle.unit
    val rows = HistoryTimeline.rows(groups) { it.first().record.date }
    val hues = groups.flatten().flatMap { r -> r.items.map { HistoryTimeline.hue(it.catalogId) } }.toSet()
    // Everything drawn on the line, in order; the key comes after it, off the line.
    val onLine: List<LineRow> =
        listOfNotNull(next?.let { LineRow.Next(it.first, it.second) }) + rows.map(LineRow::Past) + listOfNotNull(since?.let(LineRow::Since))

    LazyColumn(state = listState, contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp)) {
        itemsIndexed(onLine, key = { i, row -> row.key ?: "row-$i" }) { i, row ->
            val first = i == 0
            when (row) {
                is LineRow.Next -> OnLine(first, last = false, dotTop = 22.dp, dot = { DashedDot() }) {
                    ComingUpCard(row.status, row.progress, unit, onLogIt?.let { log -> { log(vehicle.id, row.status.item.id) } })
                }
                is LineRow.Since -> OnLine(first, last = true, dotTop = 4.dp, bottom = 0.dp, dot = { EndDot() }) {
                    Text(
                        stringResource(R.string.history_timeline_since, row.date.format(monthYear)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is LineRow.Past -> when (val past = row.row) {
                    is TimelineRow.Year -> OnLine(first, last = false, dotTop = 8.dp, bottom = 10.dp, dot = { YearTag(past.year) })
                    is TimelineRow.Gap -> OnLine(first, last = false, dotTop = 0.dp) {
                        Text(
                            if (past.months == 0) stringResource(R.string.history_timeline_under_a_month)
                            else pluralStringResource(R.plurals.history_timeline_apart, past.months, past.months.asNumber()),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is TimelineRow.Entry -> OnLine(first, last = false, dotTop = 16.dp, dot = { RingDot() }) {
                        val g = past.entry
                        if (g.size == 1) {
                            val r = g.single()
                            TimelineCard(r, unit, mark = fresh?.mark?.takeIf { r.record.id == fresh.recordId }, onClick = { onOpenRecord(r.record.id) })
                        } else {
                            TimelineVisitCard(g, unit, fresh, onOpenRecord)
                        }
                    }
                }
            }
        }
        if (hues.isNotEmpty()) {
            item(key = "legend") { Legend(hues, Modifier.padding(top = 20.dp)) }
        }
    }
}

/** What is drawn on the line: the next reminder, the past, and where the records start. */
private sealed interface LineRow {
    /** Keeps the row's place as records come and go; null for a gap, which is placed by its index. */
    val key: String?

    data class Next(val status: ItemStatus, val progress: ReminderProgress) : LineRow {
        override val key: String get() = "next"
    }

    data class Past(val row: TimelineRow<List<ServiceRecordWithItems>>) : LineRow {
        override val key: String?
            get() = when (row) {
                is TimelineRow.Year -> "year-${row.year}"
                is TimelineRow.Entry -> "record-${row.entry.first().record.id}"
                is TimelineRow.Gap -> null
            }
    }

    data class Since(val date: LocalDate) : LineRow {
        override val key: String get() = "since"
    }
}

/**
 * A row on the line: the dot (or year) in a 40dp gutter with the line through it, and what it marks beside it.
 * The line starts at the first row's dot and ends at the last's.
 */
@Composable
private fun OnLine(
    first: Boolean,
    last: Boolean,
    dotTop: Dp,
    bottom: Dp = 12.dp,
    dot: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().drawBehind {
            val x = (GUTTER / 2).toPx().let { if (layoutDirection == LayoutDirection.Rtl) size.width - it else it }
            val centre = (dotTop + 8.dp).toPx()
            drawLine(
                lineColor,
                Offset(x, if (first) centre else 0f),
                Offset(x, if (last) centre else size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }.padding(bottom = bottom),
    ) {
        Box(Modifier.width(GUTTER).padding(top = dotTop), contentAlignment = Alignment.TopCenter) { dot() }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

private val GUTTER = 40.dp

@Composable
private fun RingDot() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(16.dp).background(scheme.surface, CircleShape).border(3.dp, scheme.primary, CircleShape))
}

/** Not done yet: the ring is dashed, as the card beside it is. */
@Composable
private fun DashedDot() {
    val scheme = MaterialTheme.colorScheme
    Canvas(Modifier.size(16.dp)) {
        drawCircle(scheme.background)
        val stroke = 2.dp.toPx()
        drawCircle(
            scheme.primary,
            radius = (size.minDimension - stroke) / 2,
            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
    }
}

@Composable
private fun EndDot() {
    Box(Modifier.padding(top = 3.dp).size(10.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
}

@Composable
private fun YearTag(year: Int) {
    val scheme = MaterialTheme.colorScheme
    Text(
        LocalDate.of(year, 1, 1).format(yearOnly),
        // A larger font may make it wider than the gutter; it stays on one line, centred on the line.
        Modifier.wrapContentWidth(unbounded = true).background(scheme.onSurface, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = PlexMono,
        fontWeight = FontWeight.SemiBold,
        color = scheme.surface,
    )
}

/**
 * The vehicle's next reminder, dashed because it hasn't happened: when it is due, as the Reminders tab and
 * vehicle home say it, with the date from the driving pace only where vehicle home gives one. A tap is Log
 * service with that job ticked, as from any reminder row.
 */
@Composable
private fun ComingUpCard(status: ItemStatus, progress: ReminderProgress, unit: DistanceUnit, onLogIt: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val due = progress.headline.text(unit)
    val estimate = status.status.paceEstimate?.takeIf { progress.headline is DueHeadline.Miles }
    val logIt = stringResource(R.string.log_it)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .then(if (onLogIt != null) Modifier.clickable(role = Role.Button, onClickLabel = logIt, onClick = onLogIt) else Modifier)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    scheme.primary,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = size.copy(width = size.width - stroke, height = size.height - stroke),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.history_timeline_coming_up),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = scheme.onPrimaryContainer,
        )
        Text(status.item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            estimate?.let { stringResource(R.string.history_timeline_due_estimate, due, stringResource(R.string.home_estimate, it.format(longDate))) } ?: due,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineCard(r: ServiceRecordWithItems, unit: DistanceUnit, mark: io.cosgriff.glovebox.domain.RecordMark?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fresh = mark != null
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(if (fresh) 1.5.dp else 1.dp, if (fresh) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(r.record.date.format(longDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(r.record.odometer.asDistance(unit), style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
            }
            Lines(r)
            Text(
                LogEdits.who(r.record.diy, r.record.shopName, mark, r.record.enteredAsLastDone, r.record.imported).text,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** A visit (#307) on the line: its date, and its reading when its records agree; then each record, opening that record. */
@Composable
private fun TimelineVisitCard(records: List<ServiceRecordWithItems>, unit: DistanceUnit, fresh: HistoryFocus?, onOpenRecord: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val first = records.first().record
    val readings = Visits.readings(records.map { it.record.odometer })
    val oneDay = records.all { it.record.date == first.date }
    val marked = records.any { it.record.id == fresh?.recordId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(if (marked) 1.5.dp else 1.dp, if (marked) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(end = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(first.date.format(longDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (readings.size == 1) {
                    Text(readings.single().asDistance(unit), style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                pluralStringResource(R.plurals.history_visit, records.size, records.size.asNumber()),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            records.forEach { r ->
                HorizontalDivider(Modifier.padding(top = 8.dp, end = 6.dp), color = scheme.outlineVariant)
                val own = listOfNotNull(
                    r.record.date.takeIf { !oneDay }?.format(longDate),
                    r.record.odometer.takeIf { readings.size > 1 }?.asDistance(unit),
                    LogEdits.who(r.record.diy, r.record.shopName, fresh?.mark?.takeIf { r.record.id == fresh.recordId }, r.record.enteredAsLastDone, r.record.imported).text,
                ).joinToString(" · ")
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { onOpenRecord(r.record.id) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Lines(r)
                        Text(own, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Chevron()
                }
            }
        }
    }
}

/** A record's lines as written, each with its category's colour. */
@Composable
private fun Lines(r: ServiceRecordWithItems) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        r.items.forEach { line ->
            Row(
                Modifier.background(scheme.background, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Swatch(HistoryTimeline.hue(line.catalogId).color())
                Text(line.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Decorative: the line or key beside it names what it stands for. */
@Composable
private fun Swatch(color: Color) {
    Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)).clearAndSetSemantics { })
}

/** The colour key, for the colours on the timeline only. */
@Composable
private fun Legend(hues: Set<LineHue>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LineHue.entries.filter { it in hues }.forEach { hue ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(hue.color())
                Text(hue.label(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LineHue.color(): Color = when (this) {
    LineHue.ENGINE -> MaterialTheme.colorScheme.primary
    LineHue.TIRES -> statusColors().soon
    LineHue.BRAKES -> MaterialTheme.colorScheme.error
    LineHue.FLUIDS -> MaterialTheme.colorScheme.tertiary
    LineHue.OTHER -> MaterialTheme.colorScheme.outline
}

@Composable
private fun LineHue.label(): String = when (this) {
    LineHue.ENGINE -> catalogCategoryLabel("engine")
    LineHue.TIRES -> catalogCategoryLabel("tires")
    LineHue.BRAKES -> catalogCategoryLabel("brakes")
    LineHue.FLUIDS -> stringResource(R.string.history_timeline_hue_fluids)
    LineHue.OTHER -> stringResource(R.string.history_timeline_hue_other)
}
