package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.CostBreakdown
import io.cosgriff.glovebox.domain.JobDraft
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.components.WorkedOutTag
import io.cosgriff.glovebox.ui.theme.PlexMono
import kotlin.math.absoluteValue

/** What a job is called: the shop's wording, or its lines joined ("Spark plugs + Ignition coils"). */
@Composable
private fun LogServiceForm.heading(job: JobDraft<String>): String {
    job.wording?.let { return it }
    val names = job.lines.map(::lineName)
    return names.drop(1).fold(names.first()) { a, b -> stringResource(R.string.prices_lines_joined, a, b) }
}

/**
 * "Work and prices" on Log service and Edit service (#481, board PricesLog): every ticked line, each its own job
 * until the user prices it together with another. The price fields stay hidden behind one row until asked for,
 * and stay out once any job has a price.
 */
@Composable
internal fun WorkAndPrices(form: LogServiceForm) {
    val jobs = form.jobs
    if (jobs.isEmpty()) return
    val lineCount = jobs.sumOf { it.lines.size }
    val open = form.pricesShown
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pluralStringResource(R.plurals.prices_work_section, lineCount, lineCount.asNumber()), Modifier.weight(1f))
            if (open) Text(stringResource(R.string.prices_optional), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PricesCard {
            jobs.forEachIndexed { i, job ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                JobRow(form, job, others = jobs.filter { it.id != job.id }, open = open)
            }
            if (!open) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AddPricesRow(onClick = { form.pricesOpen = true })
            }
        }
        if (open && !form.pricesValid) {
            Text(stringResource(R.string.prices_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PricesCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

@Composable
private fun JobRow(form: LogServiceForm, job: JobDraft<String>, others: List<JobDraft<String>>, open: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val heading = form.heading(job)
    val multi = job.lines.size > 1
    val total = form.price(job).totalCents
    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (!multi) {
                    Text(
                        form.lineCategory(job.lines.single()) ?: stringResource(R.string.log_custom),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            if (open && total != null) Text(Money.format(total), fontFamily = PlexMono, fontWeight = FontWeight.SemiBold)
        }
        if (multi) {
            // The job's lines under it, on a 2dp rule: priced as one, never split between them.
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(scheme.outlineVariant))
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    job.lines.forEach { key ->
                        Row {
                            Text(form.lineName(key), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(form.lineCategory(key) ?: stringResource(R.string.log_custom), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (open) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PriceField(stringResource(R.string.prices_labor), heading, job.labor, { form.setLabor(job.id, it) }, Modifier.weight(1f))
                PriceField(stringResource(R.string.prices_parts), heading, job.parts, { form.setParts(job.id, it) }, Modifier.weight(1f))
            }
            if (multi) {
                TextButton(onClick = { form.split(job.id) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(stringResource(R.string.prices_separately), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                }
            } else if (others.isNotEmpty()) {
                PriceTogether(form, job, others)
            }
        }
    }
}

/** "Priced together with…" and its menu of the other jobs, under a one-line job. */
@Composable
private fun PriceTogether(form: LogServiceForm, job: JobDraft<String>, others: List<JobDraft<String>>) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    // Every one-line job has this button: TalkBack says whose.
    val described = stringResource(R.string.prices_together_for, form.heading(job))
    Box {
        TextButton(
            onClick = { menu = true },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = described },
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Text(stringResource(R.string.prices_together), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = MaterialTheme.shapes.small,
            containerColor = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Text(
                stringResource(R.string.prices_together_note),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).width(240.dp),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            others.forEach { other ->
                DropdownMenuItem(
                    text = { Text(form.heading(other)) },
                    onClick = {
                        menu = false
                        form.join(job.id, other.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun PriceField(label: String, job: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    val described = stringResource(R.string.prices_field_for, label, job)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            prefix = { Text(stringResource(R.string.log_cost_prefix), fontFamily = PlexMono) },
            singleLine = true,
            isError = value.isNotBlank() && Money.parseCents(value.typedDecimal()) == null,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = fieldColors(),
            // Every job has a Labor and a Parts: TalkBack says whose.
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = described },
        )
    }
}

@Composable
private fun AddPricesRow(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(color = scheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
        }
        Column {
            Text(stringResource(R.string.prices_add), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.prices_add_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/**
 * "Cost" (#481): the total as printed, and once anything is priced, what the line prices add up to and the gap
 * worked out from them. The total is never filled in or corrected from the prices.
 */
@Composable
internal fun CostCard(form: LogServiceForm) {
    val scheme = MaterialTheme.colorScheme
    val breakdown = if (form.pricesShown) form.breakdown else null
    val costLabel = stringResource(R.string.log_cost_optional)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.prices_cost_section))
        PricesCard {
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.log_total_cost))
                OutlinedTextField(
                    value = form.costText,
                    onValueChange = { form.costText = it },
                    placeholder = { Text(stringResource(R.string.log_cost_placeholder), fontFamily = PlexMono) },
                    prefix = { Text(stringResource(R.string.log_cost_prefix), fontFamily = PlexMono) },
                    singleLine = true,
                    isError = !form.costValid,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = costLabel },
                )
                Text(
                    when {
                        !form.costValid -> stringResource(R.string.log_cost_error)
                        breakdown != null && breakdown.totalCents == null -> stringResource(R.string.prices_total_missing, Money.format(breakdown.linesCents))
                        form.diy -> stringResource(R.string.prices_total_diy)
                        else -> stringResource(R.string.prices_total_shop)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (form.costValid) scheme.onSurfaceVariant else scheme.error,
                )
            }
            if (breakdown != null) {
                HorizontalDivider(color = scheme.outlineVariant)
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmountRow(stringResource(R.string.prices_labor_on_lines), breakdown.laborCents)
                    AmountRow(stringResource(R.string.prices_parts_on_lines), breakdown.partsCents)
                    GapRow(breakdown)
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, cents: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(Money.format(cents), fontFamily = PlexMono)
    }
}

/** The gap on its tint, tagged as worked out, with what it is; nothing when there is none. */
@Composable
private fun GapRow(breakdown: CostBreakdown) {
    val gap = breakdown.gapCents?.takeIf { it != 0L } ?: return
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = scheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (gap > 0) R.string.prices_tax_and_fees else R.string.prices_less_than_lines), style = MaterialTheme.typography.bodyMedium)
                WorkedOutTag()
                Text(Money.format(gap.absoluteValue), Modifier.weight(1f), fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
            }
        }
        Text(
            stringResource(if (gap > 0) R.string.prices_over_note else R.string.prices_under_note),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}
