package io.cosgriff.glovebox.ui.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.JobDraft
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.PartCheck
import io.cosgriff.glovebox.domain.PartField
import io.cosgriff.glovebox.domain.PartRowDraft
import io.cosgriff.glovebox.domain.PartRows
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.components.DashedTag
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.theme.PlexMono

/** What a part row's figures say, for the note under it and the tag on its closed line; null when nothing. */
private data class RowStatus(val tag: String, val note: String, val shownClosed: Boolean)

@Composable
private fun LogServiceForm.status(row: PartRowDraft<String>): RowStatus? {
    val a = amounts(row)
    val q = a.quantityMilli?.let(::quantityText)
    val each = a.eachCents?.let(Money::format)
    val total = a.totalCents?.let(Money::format)
    val filled = stringResource(R.string.prices_row_tag_filled)
    val blank = stringResource(R.string.prices_row_tag_blank)
    return when {
        row.filled == PartField.TOTAL && q != null && each != null -> RowStatus(filled, stringResource(R.string.prices_row_filled_total, q, each), false)
        row.filled == PartField.EACH && total != null && q != null -> RowStatus(filled, stringResource(R.string.prices_row_filled_each, total, q), false)
        row.filled == PartField.QUANTITY && total != null && each != null -> RowStatus(filled, stringResource(R.string.prices_row_filled_qty, total, each), false)
        else -> when (val check = PartRows.check(a)) {
            is PartCheck.LeftBlank -> when (check.field) {
                PartField.EACH -> RowStatus(blank, stringResource(R.string.prices_row_blank_each, total.orEmpty(), q.orEmpty()), true)
                PartField.TOTAL -> RowStatus(blank, stringResource(R.string.prices_row_blank_total, q.orEmpty(), each.orEmpty()), true)
                PartField.QUANTITY -> RowStatus(blank, stringResource(R.string.prices_row_blank_qty, total.orEmpty(), each.orEmpty()), true)
            }
            is PartCheck.Mismatch -> RowStatus(
                stringResource(R.string.prices_row_tag_mismatch),
                stringResource(R.string.prices_row_mismatch, q.orEmpty(), each.orEmpty(), Money.format(check.workedCents), total.orEmpty()),
                true,
            )
            else -> if (a.totalCents == null && (row.wording.isNotBlank() || row.quantity.isNotBlank() || row.each.isNotBlank())) {
                RowStatus(stringResource(R.string.prices_row_tag_no_total), stringResource(R.string.prices_row_no_total), true)
            } else {
                null
            }
        }
    }
}

/** A job's parts as the sum of its rows: shown, never typed, with a dashed tag saying so. */
@Composable
internal fun PartsFromRows(form: LogServiceForm, job: JobDraft<String>, modifier: Modifier) {
    val cents = form.price(job).partsCents
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.prices_parts))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(cents?.let(Money::format) ?: "—", Modifier.weight(1f), fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, maxLines = 1)
                DashedTag(stringResource(R.string.prices_rows_from))
            }
        }
    }
}

/**
 * A job's part rows (#502, board PartsLog): each closed to one line until opened, "Expand all" / "Collapse all",
 * "+ Add part row", and what the rows without a total leave out.
 */
@Composable
internal fun PartRowsList(form: LogServiceForm, job: JobDraft<String>) {
    val rows = job.rows ?: return
    val scheme = MaterialTheme.colorScheme
    val allOpen = rows.all { it.id in form.openRows }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel(pluralStringResource(R.plurals.prices_rows_section, rows.size, rows.size.asNumber()))
                Text(stringResource(R.string.prices_rows_as_printed), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            TextButton(onClick = { form.openAllRows(job, !allOpen) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(if (allOpen) R.string.prices_rows_collapse_all else R.string.prices_rows_expand_all), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                Icon(if (allOpen) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.padding(start = 4.dp).size(18.dp))
            }
        }
        rows.forEach { row -> PartRowCard(form, job, row) }
        OutlinedButton(
            onClick = { form.addRow(job.id) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, scheme.outline),
        ) {
            Text(stringResource(R.string.prices_row_add), color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
        }
        val missing = PartRows.missingTotals(rows.map(form::amounts))
        if (missing > 0) {
            Text(pluralStringResource(R.plurals.prices_rows_missing, missing, missing.asNumber()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PartRowCard(form: LogServiceForm, job: JobDraft<String>, row: PartRowDraft<String>) {
    val scheme = MaterialTheme.colorScheme
    val open = row.id in form.openRows
    val name = row.wording.trim().ifEmpty { stringResource(R.string.prices_row_unnamed) }
    val status = form.status(row)
    Surface(
        color = if (open) scheme.surfaceContainerLow else scheme.surface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (open) OpenRow(form, job, row, name, status) else ClosedRow(form, job, row, name, status)
    }
}

/** A row as one line: its name, its figures and line, its total, and a tag when something wants a look. */
@Composable
private fun ClosedRow(form: LogServiceForm, job: JobDraft<String>, row: PartRowDraft<String>, name: String, status: RowStatus?) {
    val scheme = MaterialTheme.colorScheme
    val a = form.amounts(row)
    val figures = when {
        a.quantityMilli != null && a.eachCents != null -> stringResource(R.string.prices_row_qty_each, quantityText(a.quantityMilli!!), Money.format(a.eachCents!!))
        a.quantityMilli != null -> stringResource(R.string.prices_row_qty_only, quantityText(a.quantityMilli!!))
        a.eachCents != null -> stringResource(R.string.prices_row_each_only, Money.format(a.eachCents!!))
        else -> null
    }
    val line = if (job.lines.size > 1) row.line?.let(form::lineName) ?: stringResource(R.string.prices_row_whole_job) else null
    val detail = when {
        figures != null && line != null -> stringResource(R.string.prices_row_detail, figures, line)
        else -> figures ?: line ?: stringResource(R.string.prices_row_nothing)
    }
    val described = stringResource(R.string.prices_row_edit, name)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClickLabel = described) { form.toggleRow(row.id) }
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(detail, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (status != null && status.shownClosed) DashedTag(status.tag)
            }
        }
        Text(
            a.totalCents?.let(Money::format) ?: "—",
            fontFamily = PlexMono,
            fontWeight = FontWeight.SemiBold,
            color = if (a.totalCents != null) scheme.onSurface else scheme.onSurfaceVariant,
        )
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

@Composable
private fun OpenRow(form: LogServiceForm, job: JobDraft<String>, row: PartRowDraft<String>, name: String, status: RowStatus?) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            val partLabel = stringResource(R.string.prices_row_part)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(partLabel)
                OutlinedTextField(
                    value = row.wording,
                    onValueChange = { form.nameRow(job.id, row.id, it) },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = partLabel },
                )
            }
            IconButton(onClick = { form.removeRow(job.id, row.id) }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.prices_row_remove, name), tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = { form.toggleRow(row.id) }) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.prices_row_done, name), tint = scheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RowFigure(form, job, row, PartField.QUANTITY, name, Modifier.width(64.dp))
            RowFigure(form, job, row, PartField.EACH, name, Modifier.weight(1f))
            RowFigure(form, job, row, PartField.TOTAL, name, Modifier.weight(1f))
        }
        if (status != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DashedTag(status.tag)
                Text(status.note, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        if (job.lines.size > 1) LinePicker(form, job, row)
    }
}

/** Qty, Each or Total; a figure filled in from the other two sits on the worked-out tint. */
@Composable
private fun RowFigure(form: LogServiceForm, job: JobDraft<String>, row: PartRowDraft<String>, field: PartField, name: String, modifier: Modifier) {
    val label = stringResource(
        when (field) {
            PartField.QUANTITY -> R.string.prices_row_qty
            PartField.EACH -> R.string.prices_row_each
            PartField.TOTAL -> R.string.prices_row_total
        },
    )
    val described = stringResource(R.string.prices_row_field_for, label, name)
    val text = row.text(field)
    val filled = row.filled == field
    val bad = text.isNotBlank() && form.amounts(row).let {
        when (field) {
            PartField.QUANTITY -> it.quantityMilli
            PartField.EACH -> it.eachCents
            PartField.TOTAL -> it.totalCents
        }
    } == null
    val tint = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = text,
            onValueChange = { form.typeRow(job.id, row.id, field, it) },
            prefix = if (field == PartField.QUANTITY) null else ({ Text(stringResource(R.string.log_cost_prefix), fontFamily = PlexMono) }),
            singleLine = true,
            isError = bad,
            textStyle = MaterialTheme.typography.titleSmall.copy(
                fontFamily = PlexMono,
                fontWeight = FontWeight.SemiBold,
                textAlign = if (field == PartField.QUANTITY) TextAlign.Center else TextAlign.Start,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = if (filled) OutlinedTextFieldDefaults.colors(focusedContainerColor = tint, unfocusedContainerColor = tint) else fieldColors(),
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = described },
        )
    }
}

/** "For line": which of the job's lines the row is for, or the whole job. A label only. */
@Composable
private fun LinePicker(form: LogServiceForm, job: JobDraft<String>, row: PartRowDraft<String>) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val whole = stringResource(R.string.prices_row_whole_job)
    val label = stringResource(R.string.prices_row_for)
    val current = row.line?.let(form::lineName) ?: whole
    val described = stringResource(R.string.prices_field_for, label, current)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FieldLabel(label)
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { menu = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = described },
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, scheme.outline),
            ) {
                Text(current, Modifier.weight(1f), color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = MaterialTheme.shapes.small,
                containerColor = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                (listOf<String?>(null) + job.lines).forEach { key ->
                    DropdownMenuItem(
                        text = { Text(key?.let(form::lineName) ?: whole) },
                        onClick = {
                            menu = false
                            form.tagRow(job.id, row.id, key)
                        },
                    )
                }
            }
        }
    }
}
