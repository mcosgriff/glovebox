package io.cosgriff.glovebox.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.CostBreakdown
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.components.WorkedOutTag
import io.cosgriff.glovebox.ui.service.andList
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlin.math.absoluteValue

/** One entry of "Work done" (#481, board PricesRecord): a line on its own, or a priced job with its lines under it. */
@Composable
internal fun WorkJobRow(job: WorkJob) {
    val scheme = MaterialTheme.colorScheme
    val single = job.lines.singleOrNull()?.takeIf { job.heading == null }
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(job.heading ?: job.lines.first().label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            val total = job.totalCents
            when {
                total != null -> Text(Money.format(total), fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
                single != null -> Category(single)
            }
        }
        if (single != null) {
            if (job.priced) Category(single)
            Prices(job)
            Note(single)
        } else {
            Prices(job)
            // The job's lines on a 2dp rule, each with what it means for its reminder.
            Row(Modifier.padding(top = 4.dp).height(IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(scheme.outlineVariant))
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    job.lines.forEach { line ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(line.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Category(line)
                            }
                            Note(line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Category(line: WorkLine) {
    Text(line.category ?: stringResource(R.string.log_custom), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Note(line: WorkLine) {
    line.reminderNote?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = if (line.current) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Labor $300.00   Parts $420.00": only what was entered. */
@Composable
private fun Prices(job: WorkJob) {
    if (!job.priced) return
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        job.laborCents?.let { Text(stringResource(R.string.prices_labor_amount, Money.format(it)), style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono) }
        job.partsCents?.let { Text(stringResource(R.string.prices_parts_amount, Money.format(it)), style = MaterialTheme.typography.bodyMedium, fontFamily = PlexMono) }
    }
}

/**
 * "Cost breakdown", only on a priced record: one bar of labor, parts and the gap worked out (dashed), each with
 * its share and amount, then the total as typed. A gap below zero gets a row and no part of the bar.
 */
@Composable
internal fun CostBreakdownCard(breakdown: CostBreakdown, unpriced: List<String>) {
    val scheme = MaterialTheme.colorScheme
    // Labor in ink's softer step, parts in the accent: the board's two fills.
    val labor = statusColors().soon
    val parts = scheme.primary
    val gap = breakdown.gapCents?.takeIf { it != 0L }
    val shares = breakdown.shares
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.prices_breakdown_section))
        FilledCard(padding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (breakdown.laborCents > 0) Box(Modifier.weight(breakdown.laborCents.toFloat()).fillMaxHeight().background(labor, BarShape))
                    if (breakdown.partsCents > 0) Box(Modifier.weight(breakdown.partsCents.toFloat()).fillMaxHeight().background(parts, BarShape))
                    if (gap != null && gap > 0) Box(Modifier.weight(gap.toFloat()).fillMaxHeight().dashed(scheme.onSurfaceVariant))
                }
                ShareRow({ Box(Modifier.size(12.dp).background(labor, BarShape)) }, stringResource(R.string.prices_labor), shares.labor, breakdown.laborCents)
                ShareRow({ Box(Modifier.size(12.dp).background(parts, BarShape)) }, stringResource(R.string.prices_parts), shares.parts, breakdown.partsCents)
                if (gap != null) {
                    ShareRow(
                        { Box(Modifier.size(12.dp).dashed(scheme.onSurfaceVariant)) },
                        stringResource(if (gap > 0) R.string.prices_tax_and_fees else R.string.prices_less_than_lines),
                        shares.gap.takeIf { gap > 0 },
                        gap.absoluteValue,
                        workedOut = true,
                    )
                }
                breakdown.totalCents?.let { total ->
                    HorizontalDivider(color = scheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.log_total_cost), Modifier.weight(1f).padding(start = 24.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(Money.format(total), fontFamily = PlexMono, fontWeight = FontWeight.Bold)
                    }
                }
                val notes = listOfNotNull(
                    when {
                        gap == null -> null
                        gap > 0 -> stringResource(R.string.prices_record_over_note)
                        else -> stringResource(R.string.prices_record_under_note, Money.format(gap.absoluteValue))
                    },
                    unpriced.takeIf { it.isNotEmpty() }?.let { pluralStringResource(R.plurals.prices_unpriced, it.size, andList(it)) },
                )
                notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
            }
        }
    }
}

private val BarShape = RoundedCornerShape(2.dp)

/** The gap's mark: an outline, dashed, never a fill, since it was worked out and not typed. */
private fun Modifier.dashed(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
    )
}

@Composable
private fun ShareRow(swatch: @Composable () -> Unit, label: String, share: Int?, cents: Long, workedOut: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        swatch()
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (workedOut) WorkedOutTag()
        }
        share?.let { Text(stringResource(R.string.prices_share, it.asNumber()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(Money.format(cents), fontFamily = PlexMono)
    }
}
