package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.ui.components.DangerTextButton
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.domain.WorkProblem
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.MilestoneEdit
import io.cosgriff.glovebox.domain.MilestoneEdits
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.MilestoneProblems
import io.cosgriff.glovebox.domain.RepeatEdit
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.theme.PlexMono

/** A milestone of the user's own table open in the editor. [id] is null for one not saved yet. */
data class MilestoneDraft(val id: Long?, val atMiles: Int?, val orMonths: Int?, val jobs: List<TableJob>)

/** A job as the form holds it. [key] only tells the rows apart while they are moved about. */
private data class JobRow(val key: Int, val name: String, val service: String?)

/**
 * Adds or edits one milestone of the user's own table. No rendering on the canvas; follows the
 * schedule item editor. Stateless towards the database: the caller decides what Save and Delete mean.
 * What may be saved is decided by [MilestoneEdits], with the rules an imported file keeps.
 *
 * [others] are the mileages of the table's other milestones. [startsTable] is true when there is no
 * table yet, so saving this one makes it; [endsTable] when it is the only milestone, so deleting it
 * takes the table away.
 */
@Composable
fun MilestoneEditor(
    draft: MilestoneDraft,
    others: List<Int>,
    repeatsEveryMiles: Int?,
    /** What the vehicle's odometer counts in; the table is in it. */
    unit: DistanceUnit,
    startsTable: Boolean,
    endsTable: Boolean,
    onSave: (ImportedMilestone) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isNew = draft.id == null
    var milesText by remember(draft) { mutableStateOf(draft.atMiles?.asNumber().orEmpty()) }
    var monthsText by remember(draft) { mutableStateOf(draft.orMonths?.toString().orEmpty()) }
    var nextKey by remember(draft) { mutableIntStateOf(draft.jobs.size + 1) }
    // A new milestone starts with one empty job, since it can't be saved without one.
    val jobs = remember(draft) {
        mutableStateListOf<JobRow>().apply {
            addAll(draft.jobs.mapIndexed { i, job -> JobRow(i, job.name, job.service) }.ifEmpty { listOf(JobRow(0, "", null)) })
        }
    }
    var showErrors by remember(draft) { mutableStateOf(false) }
    var deleting by remember(draft) { mutableStateOf(false) }
    var discarding by remember(draft) { mutableStateOf(false) }
    val initial = remember(draft) { Triple(milesText, monthsText, jobs.toList()) }

    // A milestone can be a lot of typing, so Close and Back ask before throwing it away.
    fun close() {
        if (Triple(milesText, monthsText, jobs.toList()) == initial) onClose() else discarding = true
    }
    BackHandler(onBack = ::close)

    if (discarding) {
        AlertDialog(
            onDismissRequest = { discarding = false },
            title = { Text(stringResource(if (isNew) R.string.milestone_discard_new else R.string.discard_changes)) },
            text = { Text(stringResource(if (isNew) R.string.milestone_discard_new_body else R.string.milestone_discard_body)) },
            confirmButton = {
                DangerTextButton(
                    onClick = {
                        discarding = false
                        onClose()
                    },
                ) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = { TextButton(onClick = { discarding = false }) { Text(stringResource(R.string.keep_editing)) } },
        )
    }

    val checked = MilestoneEdits.check(milesText.typedNumber(), monthsText.typedNumber(), jobs.map { TableJob(it.name, it.service) }, others, repeatsEveryMiles)
    val problems = (checked as? MilestoneEdit.Refused)?.problems?.takeIf { showErrors } ?: MilestoneProblems()

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.milestone_delete_title)) },
            text = {
                Text(
                    if (endsTable) {
                        stringResource(R.string.milestone_delete_last)
                    } else {
                        stringResource(R.string.milestone_delete_body)
                    },
                )
            },
            confirmButton = {
                DangerTextButton(
                    onClick = {
                        deleting = false
                        onDelete()
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // Save stays put above the form: a milestone with many jobs is a long scroll.
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::close) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close)) }
            Text(stringResource(if (isNew) R.string.milestone_add_title else R.string.milestone_edit_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Button(onClick = { if (checked is MilestoneEdit.Ok) onSave(checked.milestone) else showErrors = true }) { Text(stringResource(R.string.save)) }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (startsTable) {
                Text(
                    stringResource(R.string.milestone_starts_table),
                    color = scheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.milestone_due_at))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(unit.titleWord, milesText, { milesText = it }, problems.miles != null, Modifier.weight(1f))
                    NumberField(stringResource(R.string.milestone_or_months), monthsText, { monthsText = it }, problems.months, Modifier.weight(1f))
                }
                val wrong = listOfNotNull(problems.miles?.text(unit), monthsProblem.takeIf { problems.months })
                if (wrong.isEmpty()) {
                    Text(
                        repeatsEveryMiles?.let { stringResource(R.string.milestone_due_note_repeats, unit.titleWord, it.asDistance(unit)) }
                            ?: stringResource(R.string.milestone_due_note, unit.titleWord),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                } else {
                    wrong.forEach { Text(stringResource(R.string.sentence, it), style = MaterialTheme.typography.bodySmall, color = scheme.error) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.milestone_jobs))
                jobs.forEachIndexed { i, job ->
                    key(job.key) {
                        JobCard(
                            job = job,
                            problem = problems.jobs[i]?.text,
                            canMoveUp = i > 0,
                            canMoveDown = i < jobs.lastIndex,
                            onChange = { jobs[i] = it },
                            onMove = { by -> jobs.add(i + by, jobs.removeAt(i)) },
                            onRemove = { jobs.removeAt(i) },
                        )
                    }
                }
                problems.work?.let { Text(stringResource(R.string.sentence, it.text), style = MaterialTheme.typography.bodySmall, color = scheme.error) }
                if (MilestoneEdits.canAddJob(jobs.size)) {
                    InkOutlinedButton(onClick = { jobs.add(JobRow(nextKey++, "", null)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), quiet = true) { Text(stringResource(R.string.milestone_add_job)) }
                } else {
                    Text(stringResource(R.string.sentence, WorkProblem.TOO_MANY.text), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Text(
                    stringResource(R.string.milestone_jobs_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (!isNew) {
                TextButton(onClick = { deleting = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.milestone_delete), color = scheme.error)
                }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: JobRow,
    problem: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (JobRow) -> Unit,
    onMove: (by: Int) -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), color = scheme.surface, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = job.name,
                // Longer text couldn't be saved, so it isn't taken.
                onValueChange = { onChange(job.copy(name = it.take(MilestoneImport.MAX_NAME))) },
                label = { Text(stringResource(R.string.milestone_job)) },
                singleLine = true,
                isError = problem != null,
                supportingText = problem?.let { { Text(stringResource(R.string.sentence, it)) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            JobServiceDropdown(job.service, onPick = { onChange(job.copy(service = it)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up)) }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.remove), color = scheme.error) }
            }
        }
    }
}

/** Which of Glovebox's service lines a job is, if it is one: what lets logged work complete it whatever the manual calls it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobServiceDropdown(service: String?, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val lines = remember { ServiceCatalog.categories.flatMap { c -> c.items.map { it to c.label } } }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = lines.firstOrNull { it.first.id == service }?.first?.label ?: stringResource(R.string.milestone_no_service),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.milestone_completed_by)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.milestone_no_service)) },
                onClick = {
                    onPick(null)
                    open = false
                },
            )
            lines.forEach { (line, category) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(line.label)
                            Text(category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = {
                        onPick(line.id)
                        open = false
                    },
                )
            }
        }
    }
}


/**
 * Whether the table starts again after its last milestone, and over what cycle. Empty means it ends.
 * [lastMiles] is the table's highest mileage, which the cycle can't be shorter than.
 */
@Composable
fun MilestoneRepeatDialog(repeatsEveryMiles: Int?, lastMiles: Int, unit: DistanceUnit, onSave: (Int?) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(repeatsEveryMiles?.asNumber().orEmpty()) }
    var showError by remember { mutableStateOf(false) }
    val checked = MilestoneEdits.repeat(text.typedNumber(), lastMiles)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.milestone_repeat_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.milestone_repeat_body, unit.pluralWord, lastMiles.asDistance(unit)),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.milestone_repeats_every, unit.pluralWord)) },
                    singleLine = true,
                    isError = showError && checked is RepeatEdit.Refused,
                    supportingText = (checked as? RepeatEdit.Refused)?.takeIf { showError }?.let { { Text(stringResource(R.string.sentence, it.problem.text(unit))) } },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.milestone_months_not_repeated), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { if (checked is RepeatEdit.Ok) onSave(checked.everyMiles) else showError = true }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
