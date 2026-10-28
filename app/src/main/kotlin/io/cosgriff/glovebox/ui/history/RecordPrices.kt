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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import io.cosgriff.glovebox.domain.GapKind
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.components.WorkedOutTag
import io.cosgriff.glovebox.ui.service.andList
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.quantityText
import io.cosgriff.glovebox.ui.theme.chargeColors
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
            PartsAsPrinted(job)
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
            // After the lines, as on the board: what was fitted is read after what it was for.
            PartsAsPrinted(job)
        }
    }
}

/** "Parts as printed" (#502): each row's wording, figures and line, and its total, on a quiet tint. */
@Composable
private fun PartsAsPrinted(job: WorkJob) {
    if (job.rows.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel(stringResource(R.string.prices_parts_as_printed))
            job.rows.forEach { row ->
                val figures = when {
                    row.quantityMilli != null && row.eachCents != null -> stringResource(R.string.prices_row_qty_each, quantityText(row.quantityMilli), Money.format(row.eachCents))
                    row.quantityMilli != null -> stringResource(R.string.prices_row_qty_only, quantityText(row.quantityMilli))
                    row.eachCents != null -> stringResource(R.string.prices_row_each_only, Money.format(row.eachCents))
                    else -> null
                }
                // A row picked from the job's lines is named by its line. One typed as printed says its line; in a
                // job of several lines, an untagged one is for the whole job, and says so.
                val line = if (row.wording == null) null else row.line ?: stringResource(R.string.prices_row_whole_job).takeIf { job.lines.size > 1 }
                val detail = when {
                    figures != null && line != null -> stringResource(R.string.prices_row_detail, figures, line)
                    else -> figures ?: line
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(row.wording ?: row.line ?: stringResource(R.string.prices_row_unnamed), style = MaterialTheme.typography.bodyMedium)
                        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
                    }
                    Text(row.totalCents?.let(Money::format) ?: "—", fontFamily = PlexMono)
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
    val gap = breakdown.gapCents?.takeIf { it != 0L }
    val shares = breakdown.shares
    val c = breakdown.charges
    // Labor in ink's softer step, parts in the accent: the board's two fills, and the charges' three beside them.
    val fills = chargeColors()
    val parts = listOfNotNull(
        Part(stringResource(R.string.prices_labor), breakdown.laborCents, shares.labor, statusColors().soon),
        Part(stringResource(R.string.prices_parts), breakdown.partsCents, shares.parts, scheme.primary),
        c.shopSuppliesCents?.let { Part(stringResource(R.string.prices_shop_supplies), it, shares.shopSupplies, fills.shopSupplies) },
        c.taxCents?.let { Part(stringResource(R.string.prices_tax), it, shares.tax, fills.tax) },
        c.feesCents?.let { Part(stringResource(R.string.prices_fees), it, shares.fees, fills.fees) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.prices_breakdown_section))
        FilledCard(padding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Every part at least 4dp wide, so a small charge still shows.
                    parts.filter { it.cents > 0 }.forEach { Box(Modifier.weight(it.cents.toFloat()).widthIn(min = 4.dp).fillMaxHeight().background(it.color, BarShape)) }
                    if (gap != null && gap > 0) Box(Modifier.weight(gap.toFloat()).widthIn(min = 4.dp).fillMaxHeight().dashed(scheme.onSurfaceVariant))
                }
                parts.forEach { ShareRow({ Box(Modifier.size(12.dp).background(it.color, BarShape)) }, it.label, it.share, it.cents) }
                val kind = breakdown.gapKind
                if (gap != null && kind != null) {
                    ShareRow(
                        { Box(Modifier.size(12.dp).dashed(scheme.onSurfaceVariant)) },
                        stringResource(kind.label()),
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
                    when (kind) {
                        null -> null
                        GapKind.BELOW -> stringResource(R.string.prices_record_under_note, Money.format(gap!!.absoluteValue))
                        GapKind.TAX_AND_FEES -> stringResource(if (c.entered) R.string.prices_record_charges_tax_note else R.string.prices_record_over_note)
                        GapKind.OTHER_CHARGES -> stringResource(R.string.prices_record_other_note)
                    },
                    unpriced.takeIf { it.isNotEmpty() }?.let { pluralStringResource(R.plurals.prices_unpriced, it.size, andList(it)) },
                )
                notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
            }
        }
    }
}

/** One fill of the breakdown bar with its row. */
private data class Part(val label: String, val cents: Long, val share: Int, val color: Color)

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
