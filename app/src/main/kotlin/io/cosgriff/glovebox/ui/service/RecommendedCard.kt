package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.RecommendedChoice
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.WorkCategory
import io.cosgriff.glovebox.ui.components.RecommendationHead
import io.cosgriff.glovebox.ui.components.RecommendedDoneRow
import io.cosgriff.glovebox.ui.components.SectionLabel

/**
 * "Recommended by the shop" on Log service (#572, board RecommendedLog): one row for each piece of work the invoice
 * recommended and each red or yellow finding, to keep for later, add to the notes, both, or neither. Nothing is saved
 * until the record is.
 */
@Composable
internal fun RecommendedByShop(form: LogServiceForm, onChangeLine: (Int) -> Unit) {
    val choices = form.recommended
    if (choices.isEmpty()) return
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(pluralStringResource(R.plurals.log_recommended_section, choices.size, choices.size.asNumber()), Modifier.weight(1f))
            TextButton(onClick = form::keepAll, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(if (choices.all { it.keep }) R.string.log_recommended_keep_none else R.string.log_recommended_keep_all), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(stringResource(R.string.log_recommended_about), style = MaterialTheme.typography.bodyMedium, color = muted)
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                choices.forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ChoiceRow(form, c, onToggleKeep = { form.toggleKeep(i) }, onToggleNote = { form.toggleNote(i) }, onChangeLine = { onChangeLine(i) })
                }
            }
        }
        val kept = choices.count { it.keep }
        val noted = choices.count { it.note }
        val gone = choices.count { it.letGo }
        val keptText = pluralStringResource(R.plurals.log_recommended_kept, kept, kept.asNumber())
        val notedText = pluralStringResource(R.plurals.log_recommended_noted, noted, noted.asNumber())
        Text(
            if (gone == 0) {
                stringResource(R.string.log_recommended_summary, keptText, notedText)
            } else {
                stringResource(R.string.log_recommended_summary_let_go, keptText, notedText, pluralStringResource(R.plurals.log_recommended_gone, gone, gone.asNumber()))
            },
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(form: LogServiceForm, choice: RecommendedChoice, onToggleKeep: () -> Unit, onToggleNote: () -> Unit, onChangeLine: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val item = choice.item
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecommendationHead(item)
        if (choice.keep) {
            form.replaces(choice)?.let { old ->
                val date = old.date.format(longDate)
                Text(
                    old.item.cents?.let { stringResource(R.string.log_recommended_replaces, date, Money.format(it)) }
                        ?: stringResource(R.string.log_recommended_replaces_unpriced, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.log_recommended_keep), stringResource(R.string.log_recommended_keep_for, item.text), choice.keep, onToggleKeep)
            ChoiceChip(stringResource(R.string.log_recommended_note), stringResource(R.string.log_recommended_note_for, item.text), choice.note, onToggleNote)
        }
        if (choice.keep) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val line = choice.line
                Text(
                    when {
                        line == null -> stringResource(R.string.log_recommended_no_line)
                        line.guessed -> stringResource(R.string.log_recommended_line_guess, catalogLabel(line.catalogId))
                        else -> stringResource(R.string.log_recommended_line, catalogLabel(line.catalogId))
                    },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
                val changeFor = stringResource(R.string.log_recommended_change_for, item.text)
                TextButton(
                    onClick = onChangeLine,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = changeFor },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(R.string.log_recommended_change), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (choice.letGo) Text(stringResource(R.string.log_recommended_let_go), style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

/** Keep for later or Add to notes: outlined, solid ink with a tick once chosen, as the canvas draws a choice. */
@Composable
private fun ChoiceChip(label: String, spoken: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = spoken },
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = MaterialTheme.shapes.small,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.secondary,
            borderWidth = 1.5.dp,
            selectedBorderWidth = 1.5.dp,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary,
        ),
    )
}

/**
 * "Recommended earlier · done with these?" on Log service (#572, board RecommendedLogDone): open recommendations a
 * ticked line looks like the work for, by its line or, with none, its wording. Each starts ticked and closes when the
 * record is saved; unticked, it stays open. Never closed without asking.
 */
@Composable
internal fun DoneWithThese(form: LogServiceForm) {
    val matches = form.doneMatches
    if (matches.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.log_done_section))
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                matches.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RecommendedDoneRow(
                        m.recommendation, stringResource(R.string.log_done_why, m.line.label),
                        ticked = m.recommendation.id !in form.notDone, onToggle = { form.toggleDone(m.recommendation.id) },
                    )
                }
            }
        }
        Text(stringResource(R.string.log_done_about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Picks the catalog line a kept recommendation comes off with (#572): only the catalog's lines, never the user's own. */
@Composable
internal fun RecommendedLineDialog(text: String, categories: List<WorkCategory>, rules: List<CatalogRule>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val lines = categories.filter { it.id != ServiceCatalog.YOUR_ITEMS }.flatMap { c -> c.options.map { LineChoice(it, c) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_recommended_line_title)) },
        text = {
            LinePicker(
                text, lines, { onPick(it.option.id) }, Modifier.verticalScroll(rememberScrollState()), rules,
                about = stringResource(R.string.log_recommended_line_about, text),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
