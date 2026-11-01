package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.components.DangerTextButton
import io.cosgriff.glovebox.ui.components.CheckRow
import io.cosgriff.glovebox.ui.components.LabelledCard
import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.ui.components.RecommendedDoneRow
import io.cosgriff.glovebox.domain.CarfaxRefusal
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.torque.text
import io.cosgriff.glovebox.ui.schedule.text
import io.cosgriff.glovebox.ui.service.label
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors

@Composable
internal fun PlanCard(view: PlanView) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_in_file, view.kind.label.uppercase(AppText.locale), view.total.asNumber())) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Group(pluralStringResource(R.plurals.import_new, view.new.size, view.new.size.asNumber()), stringResource(R.string.import_new_note), view.new)
            Group(pluralStringResource(view.noun.fills, view.fills.size, view.fills.size.asNumber()), stringResource(R.string.import_fills_note), view.fills)
            Group(pluralStringResource(R.plurals.import_differs, view.differs.size, view.differs.size.asNumber()), stringResource(R.string.import_differs_note), view.differs, color = scheme.onPrimaryContainer)
            if (view.same > 0) {
                Text(pluralStringResource(R.plurals.import_same, view.same, view.same.asNumber()), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            view.doubts.count { !it.included }.takeIf { it > 0 }?.let { n ->
                Text(
                    pluralStringResource(R.plurals.import_left_out, n, view.count(n)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            }
            view.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(view.caution, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun Group(title: String, detail: String, lines: List<PlanLine>, color: Color = MaterialTheme.colorScheme.onSurface) {
    if (lines.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        lines.take(SHOWN).forEach { line ->
            Column {
                Text(line.name, style = MaterialTheme.typography.bodyMedium)
                line.yours?.let { Text(stringResource(R.string.import_yours, it), style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant) }
                Text(
                    if (line.yours == null) line.file else stringResource(R.string.import_file_value, line.file),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = PlexMono,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (lines.size > SHOWN) Text(stringResource(R.string.and_more, (lines.size - SHOWN).asNumber()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

/** Long files are summarised on the page; the confirmation for a replace lists everything it changes. */
private const val SHOWN = 12

/**
 * A typical item Glovebox suggested is being tracked for a job the file also has (the same name, or
 * the item the file's service resets), so there would be two reminders for one job. Stopping the typical one is the user's choice, made here.
 */
@Composable
internal fun DoublesCard(names: List<String>, stop: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_doubles), note = pluralStringResource(R.plurals.import_doubles_note, names.size)) {
        CheckRow(pluralStringResource(R.plurals.import_stop_typical, names.size, names.size.asNumber()), stop, detail = names.joinToString(", "), onChange = onChange)
    }
}

/**
 * Records whose odometer looks mistyped, each left out unless ticked. The vehicle's odometer is its
 * highest reading, so a wrong one from a report would become the odometer and put every mileage
 * reminder out. Nothing is decided wrong: the user ticks what the report has right.
 */
@Composable
internal fun DoubtsCard(doubts: List<DoubtLine>, onInclude: (Pair<LocalDate, Int>, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_readings_to_check), note = stringResource(R.string.import_readings_note)) {
        Column {
            doubts.forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                CheckRow(d.title, d.included, detail = d.why, mono = true) { onInclude(d.key, it) }
            }
        }
    }
}

/** Which reminders count from a different service once the import is in, as the delete sheet lists them. */
@Composable
internal fun MovesCard(moves: List<String>) {
    LabelledCard(stringResource(R.string.import_moves)) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            moves.take(SHOWN).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (moves.size > SHOWN) Text(stringResource(R.string.and_more, (moves.size - SHOWN).asNumber()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ErrorsCard(file: ImportFile.Invalid) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_cant), note = stringResource(R.string.import_fix_file)) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            file.carfax?.let { why ->
                Text(
                    when (why) {
                        CarfaxRefusal.NotAReport -> stringResource(R.string.carfax_not_a_report)
                        is CarfaxRefusal.NoVisits ->
                            if (why.withoutMileage == 0) {
                                stringResource(R.string.carfax_no_visits)
                            } else {
                                pluralStringResource(R.plurals.carfax_no_visits_without_mileage, why.withoutMileage, why.withoutMileage.asNumber())
                            }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            file.errors.forEach { e ->
                Column {
                    if (e.path.isNotEmpty()) Text(e.path, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, color = scheme.error)
                    Text(e.message.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (file.more > 0) Text(stringResource(R.string.and_more, file.more.asNumber()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ResultCard(r: ImportResult) {
    val scheme = MaterialTheme.colorScheme
    fun count(n: Int) = r.noun.count(n)
    LabelledCard(stringResource(R.string.import_imported_into, r.vehicleName.uppercase(AppText.locale))) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(r.added to R.string.import_added, r.filled to R.string.import_filled, r.replaced to R.string.import_replaced).filter { it.first > 0 }.forEach { (n, what) ->
                Text(stringResource(what, count(n)), style = MaterialTheme.typography.bodyLarge, color = statusColors().ok)
            }
            if (r.stopped > 0) {
                Text(
                    pluralStringResource(R.plurals.import_stopped, r.stopped, r.stopped.asNumber()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = statusColors().ok,
                )
            }
            if (r.unchanged > 0) {
                Text(pluralStringResource(R.plurals.import_unchanged, r.unchanged, count(r.unchanged)), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            r.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
            when (r.keptCopy) {
                true -> Text(stringResource(R.string.import_kept_copy), style = MaterialTheme.typography.bodyLarge, color = statusColors().ok)
                false -> Text(stringResource(R.string.import_copy_failed), style = MaterialTheme.typography.bodyMedium, color = scheme.error)
                null -> Unit
            }
        }
    }
}

/** For a Carfax report: whether to file the PDF itself with the vehicle's documents when the import is written. */
@Composable
internal fun KeepCopyCard(keep: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    RowsCard {
        CheckRow(stringResource(R.string.import_keep_copy), keep, detail = stringResource(R.string.import_keep_copy_note), onChange = onChange)
    }
}

@Composable
internal fun ReplaceDialog(view: PlanView, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_replace_title, view.count(view.differs.size))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                view.differs.forEach { line ->
                    Column {
                        Text(line.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.import_yours, line.yours.orEmpty()), style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
                        Text(stringResource(R.string.import_becomes, line.file), style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono)
                    }
                }
                Text(stringResource(R.string.delete_all_cant_undo), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = { DangerTextButton(onClick = onConfirm) { Text(stringResource(R.string.import_replace_and_import)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun Choice(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/**
 * "Recommended earlier · done with these?" in a history import's preview (#593, board ImportDone): open shop
 * recommendations a record in the file looks like the work for. Each starts ticked and closes only when the user
 * imports; unticked, it stays open.
 */
@Composable
internal fun DoneCard(done: List<DoneLine>, onTick: (id: Long, yes: Boolean) -> Unit) {
    LabelledCard(stringResource(R.string.import_done_section), note = stringResource(R.string.import_done_about)) {
        Column {
            done.forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RecommendedDoneRow(d.recommendation, d.why, d.ticked) { onTick(d.recommendation.id, !d.ticked) }
            }
        }
    }
}
