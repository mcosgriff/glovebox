package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.data.db.VehicleEntity
import java.time.LocalDate
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.components.PrimaryButton
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.theme.PlexMono

private const val README = "https://github.com/mcosgriff/glovebox/blob/main/docs/import/README.md"

/** Settings → Import. The format, and what an import may and may not change, are in `docs/import/README.md`. */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onOpenTorqueSpecs: (vehicleId: Long) -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    /** "When were these last done?", offered after a schedule import. */
    onEnterLastDone: (vehicleId: Long) -> Unit,
    onOpenMilestones: (vehicleId: Long) -> Unit,
    /** After a history import. A sold vehicle's history is its own screen. */
    onOpenHistory: (vehicleId: Long, archived: Boolean) -> Unit,
    vm: ImportViewModel = hiltViewModel(),
) {
    val vehicles = vm.vehicles.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val vehicleId by vm.vehicleId.collectAsStateWithLifecycle()
    val picked by vm.picked.collectAsStateWithLifecycle()
    val problem by vm.problem.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    ImportContent(
        vehicles, vehicleId, vm.vehicleFixed, picked?.name, picked?.pdf != null, problem, preview, result,
        onBack = onBack,
        onPick = vm::pick,
        onChooseVehicle = vm::chooseVehicle,
        onInclude = vm::include,
        onTickDone = vm::tickDone,
        onImport = { ready, mode, stop, keep -> vm.import(ready, mode, stop, keep) },
        onOpenTorqueSpecs = onOpenTorqueSpecs,
        onOpenSchedule = onOpenSchedule,
        onEnterLastDone = onEnterLastDone,
        onOpenMilestones = onOpenMilestones,
        onOpenHistory = { id, archived ->
            vm.showHistory(id)
            onOpenHistory(id, archived)
        },
    )
}

/** Import as given, with no ViewModel: what [ImportScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun ImportContent(
    vehicles: List<VehicleEntity>,
    vehicleId: Long?,
    /** Opened from one vehicle's "Import service history": that vehicle, and no choice of another. */
    vehicleFixed: Boolean,
    /** The name of the file picked; null before one is. */
    pickedName: String?,
    /** The file picked is a PDF (a Carfax report), which can be kept with the vehicle's documents. */
    pickedPdf: Boolean,
    problem: String?,
    preview: ImportPreview?,
    result: ImportResult?,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onChooseVehicle: (Long) -> Unit,
    onInclude: (Pair<LocalDate, Int>, Boolean) -> Unit,
    /** A shop recommendation offered as done by the import ticked or unticked (#593). */
    onTickDone: (id: Long, yes: Boolean) -> Unit = { _, _ -> },
    onImport: (ImportPreview.Ready, ImportMode, stopDoubles: Boolean, keepCopy: Boolean) -> Unit,
    onOpenTorqueSpecs: (vehicleId: Long) -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    onEnterLastDone: (vehicleId: Long) -> Unit,
    onOpenMilestones: (vehicleId: Long) -> Unit,
    onOpenHistory: (vehicleId: Long, archived: Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    var mode by rememberSaveable(pickedName) { mutableStateOf(ImportMode.ADD_NEW) }
    var stopDoubles by rememberSaveable(pickedName) { mutableStateOf(false) }
    // Off by default: the report is only read, never kept, unless the user asks.
    var keepCopy by rememberSaveable(pickedName) { mutableStateOf(false) }
    var confirming by rememberSaveable { mutableStateOf(false) }

    val ready = preview as? ImportPreview.Ready
    if (confirming && ready != null) {
        ReplaceDialog(
            view = ready.view,
            onConfirm = {
                confirming = false
                onImport(ready, ImportMode.REPLACE_MATCHING, stopDoubles, keepCopy)
            },
            onDismiss = { confirming = false },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.settings_import), onBack)

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.import_intro),
                    color = scheme.onSurfaceVariant,
                )
                TextButton(onClick = { uriHandler.openUri(README) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Text(stringResource(R.string.import_how_to))
                }
            }

            if (vehicles.isEmpty()) {
                Text(stringResource(R.string.import_no_vehicle), color = scheme.onSurfaceVariant)
                return@Column
            }

            result?.let { r ->
                ResultCard(r)
                when (r.kind) {
                    ImportKind.INTERVAL_SCHEDULE -> {
                        if (r.added > 0) {
                            PrimaryButton(onClick = { onEnterLastDone(r.vehicleId) }) { Text(stringResource(R.string.home_enter_last_done)) }
                            Text(
                                stringResource(R.string.import_last_done_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = { onOpenSchedule(r.vehicleId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.small) {
                            Text(stringResource(R.string.import_see_schedule))
                        }
                    }
                    ImportKind.MILESTONE_SCHEDULE -> PrimaryButton(onClick = { onOpenMilestones(r.vehicleId) }) { Text(stringResource(R.string.import_see_milestones)) }
                    ImportKind.SERVICE_HISTORY -> PrimaryButton(
                        onClick = {
                            onOpenHistory(r.vehicleId, r.archived)
                        },
                    ) { Text(stringResource(R.string.import_see_history)) }
                    else -> PrimaryButton(onClick = { onOpenTorqueSpecs(r.vehicleId) }) { Text(stringResource(R.string.import_see_torque)) }
                }
                OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.import_another))
                }
                return@Column
            }

            val fixed = vehicles.firstOrNull { it.id == vehicleId }?.takeIf { vehicleFixed }
            if (fixed != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.import_into))
                    Text(fixed.shortName(), style = MaterialTheme.typography.titleMedium)
                }
            } else if (vehicles.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.import_into))
                    RowsCard {
                        Column(Modifier.selectableGroup()) {
                            vehicles.forEachIndexed { i, v ->
                                if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                                Choice(
                                    title = v.shortName(),
                                    detail = listOfNotNull(stringResource(R.string.import_sold).takeIf { v.archivedOn != null }, v.trim, v.engine).joinToString(" · ").ifEmpty { null },
                                    selected = v.id == vehicleId,
                                    onClick = { onChooseVehicle(v.id) },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.import_file_section))
                if (pickedName == null) {
                    PrimaryButton(onClick = onPick) { Text(stringResource(R.string.import_choose)) }
                } else {
                    RowsCard {
                        Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(pickedName, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyLarge, fontFamily = PlexMono)
                            TextButton(onClick = onPick, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.change)) }
                        }
                    }
                }
                problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.error) }
            }

            when (val p = preview) {
                null -> Unit
                is ImportPreview.Invalid -> ErrorsCard(p.file)
                ImportPreview.NeedsVehicle -> Text(stringResource(R.string.import_needs_vehicle), color = scheme.onSurface)
                is ImportPreview.Refused -> Text(p.why, color = scheme.onSurface)
                is ImportPreview.Ready -> {
                    val view = p.view
                    PlanCard(view)
                    if (view.doubts.isNotEmpty()) DoubtsCard(view.doubts, onInclude)
                    if (view.differs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(stringResource(view.noun.differ))
                            RowsCard {
                                Column(Modifier.selectableGroup()) {
                                    Choice(stringResource(R.string.import_add_new), stringResource(R.string.import_add_new_note), mode == ImportMode.ADD_NEW) { mode = ImportMode.ADD_NEW }
                                    HorizontalDivider(color = scheme.outlineVariant)
                                    Choice(
                                        stringResource(R.string.import_replace),
                                        pluralStringResource(R.plurals.import_replace_note, view.differs.size, view.count(view.differs.size)),
                                        mode == ImportMode.REPLACE_MATCHING,
                                    ) { mode = ImportMode.REPLACE_MATCHING }
                                }
                            }
                        }
                    }
                    view.done[mode].orEmpty().takeIf { it.isNotEmpty() }?.let { DoneCard(it, onTickDone) }
                    if (view.doubles.isNotEmpty()) DoublesCard(view.doubles, stopDoubles) { stopDoubles = it }
                    view.moves[mode].orEmpty().takeIf { it.isNotEmpty() }?.let { MovesCard(it) }
                    val changes = view.changes(mode)
                    if (changes == 0) {
                        Text(
                            if (view.differs.isEmpty()) {
                                stringResource(R.string.import_nothing)
                            } else {
                                stringResource(view.noun.replaceOnly)
                            },
                            color = scheme.onSurface,
                        )
                    } else {
                        if (pickedPdf) KeepCopyCard(keepCopy) { keepCopy = it }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PrimaryButton(onClick = { if (mode == ImportMode.REPLACE_MATCHING && view.differs.isNotEmpty()) confirming = true else onImport(p, mode, stopDoubles, keepCopy) }) {
                                Text(stringResource(R.string.import_button, view.count(changes)))
                            }
                            Text(
                                stringResource(R.string.import_no_undo),
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
