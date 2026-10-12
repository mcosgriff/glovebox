package io.cosgriff.glovebox.ui.schedule

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.theme.PlexMono
import java.util.Locale

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
    startsTable: Boolean,
    endsTable: Boolean,
    onSave: (ImportedMilestone) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isNew = draft.id == null
    var milesText by remember(draft) { mutableStateOf(draft.atMiles?.let { String.format(Locale.US, "%,d", it) }.orEmpty()) }
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

    val checked = MilestoneEdits.check(milesText, monthsText, jobs.map { TableJob(it.name, it.service) }, others, repeatsEveryMiles)
    val problems = (checked as? MilestoneEdit.Refused)?.problems?.takeIf { showErrors } ?: MilestoneProblems()

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Delete this milestone?") },
            text = {
                Text(
                    if (endsTable) {
                        "It is the only milestone in your table, so the table goes with it and the Milestones screen goes back to the ones built from your schedule. " +
                            "Your service history and reminders aren't touched."
                    } else {
                        "The milestone and its jobs go from your table. Your service history and reminders aren't touched. This can't be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
        )
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
            Text(if (isNew) "Add milestone" else "Edit milestone", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Button(onClick = { if (checked is MilestoneEdit.Ok) onSave(checked.milestone) else showErrors = true }) { Text("Save") }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (startsTable) {
                Text(
                    "Saving this starts your own table. The Milestones screen then shows your table in place of the milestones built from your schedule, " +
                        "and no longer follows the schedule. Remove the table to go back. Your reminders aren't affected either way.",
                    color = scheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("DUE AT")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Miles", milesText, { milesText = it }, problems.miles != null, Modifier.weight(1f))
                    NumberField("Or months (optional)", monthsText, { monthsText = it }, problems.months != null, Modifier.weight(1f))
                }
                val wrong = listOfNotNull(problems.miles, problems.months)
                if (wrong.isEmpty()) {
                    Text(
                        "Miles the vehicle has covered, as your manual gives them. Months are the vehicle's age, shown for reference." +
                            (repeatsEveryMiles?.let { " The table repeats every ${it.asMiles()}, so this comes round again each time." } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                } else {
                    wrong.forEach { Text("$it.", style = MaterialTheme.typography.bodySmall, color = scheme.error) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("JOBS")
                jobs.forEachIndexed { i, job ->
                    key(job.key) {
                        JobCard(
                            job = job,
                            problem = problems.jobs[i],
                            canMoveUp = i > 0,
                            canMoveDown = i < jobs.lastIndex,
                            onChange = { jobs[i] = it },
                            onMove = { by -> jobs.add(i + by, jobs.removeAt(i)) },
                            onRemove = { jobs.removeAt(i) },
                        )
                    }
                }
                problems.work?.let { Text("$it.", style = MaterialTheme.typography.bodySmall, color = scheme.error) }
                if (MilestoneEdits.canAddJob(jobs.size)) {
                    OutlinedButton(
                        onClick = { jobs.add(JobRow(nextKey++, "", null)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    ) { Text("Add a job") }
                } else {
                    Text("A milestone can have ${MilestoneImport.MAX_WORK} jobs at most.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Text(
                    "A job shows as completed when your log has that work. Pick one of Glovebox's service lines and logging that line completes it; " +
                        "otherwise it is completed by logged work with exactly the job's name. " +
                        "Milestones never create reminders: for one, add the job to the Maintenance schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (!isNew) {
                TextButton(onClick = { deleting = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Delete this milestone", color = scheme.error)
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
                label = { Text("Job") },
                singleLine = true,
                isError = problem != null,
                supportingText = problem?.let { { Text("$it.") } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            JobServiceDropdown(job.service, onPick = { onChange(job.copy(service = it)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove", color = scheme.error) }
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
            value = lines.firstOrNull { it.first.id == service }?.first?.label ?: NO_SERVICE,
            onValueChange = {},
            readOnly = true,
            label = { Text("Completed by logging") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(NO_SERVICE) },
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

/** Nothing is suggested from the name: which service a job is, is the user's to say. */
private const val NO_SERVICE = "None of these: work with this name"

/**
 * Whether the table starts again after its last milestone, and over what cycle. Empty means it ends.
 * [lastMiles] is the table's highest mileage, which the cycle can't be shorter than.
 */
@Composable
fun MilestoneRepeatDialog(repeatsEveryMiles: Int?, lastMiles: Int, onSave: (Int?) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(repeatsEveryMiles?.let { String.format(Locale.US, "%,d", it) }.orEmpty()) }
    var showError by remember { mutableStateOf(false) }
    val checked = MilestoneEdits.repeat(text, lastMiles)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Does the table repeat?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Many manuals end with \"then repeat from the beginning\". Enter the length of the whole cycle and every milestone comes round again that many miles later, " +
                        "with the same work. Leave it empty for a table that ends at ${lastMiles.asMiles()}.",
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Repeats every (miles)") },
                    singleLine = true,
                    isError = showError && checked is RepeatEdit.Refused,
                    supportingText = (checked as? RepeatEdit.Refused)?.takeIf { showError }?.let { { Text("${it.why}.") } },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Months aren't repeated: they are the vehicle's age the first time round.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { if (checked is RepeatEdit.Ok) onSave(checked.everyMiles) else showError = true }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
