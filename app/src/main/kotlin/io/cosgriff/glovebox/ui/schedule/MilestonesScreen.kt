package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.outlined.CheckCircle
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import kotlinx.coroutines.flow.flow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.MilestoneDao
import io.cosgriff.glovebox.data.db.MilestoneEntity
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.JobDone
import io.cosgriff.glovebox.domain.MilestoneEdits
import io.cosgriff.glovebox.domain.MilestoneExport
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.export.SaveAsImportFile
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.MilestoneJobStatus
import io.cosgriff.glovebox.domain.SavedMilestone
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.domain.MilestonePlace
import io.cosgriff.glovebox.domain.MilestoneStatus
import io.cosgriff.glovebox.domain.Milestones
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.actual
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.ui.MilestonesRoute
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.service.toTracked
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.cosgriff.glovebox.ui.service.answering
import io.cosgriff.glovebox.ui.service.NoLinks
import javax.inject.Inject

data class MilestonesState(
    val vehicleName: String,
    val milestones: List<MilestoneStatus>,
    /** Miles the vehicle has really covered; null with no reading yet. */
    val current: Int?,
    val swap: ClusterSwap?,
    val unit: DistanceUnit,
    /** False when nothing tracked repeats by mileage, so there is nothing to build milestones from. */
    val hasMileageItems: Boolean,
    /** True when these are the user's own imported table, shown in place of the view built from the schedule. */
    val fromTable: Boolean,
    /** The year, make and model: who else the table would suit, for the name of a file it is saved to. */
    val model: String = "",
    /** The user's table as written, by mileage; a table that repeats shows more milestones than this. Empty with no table. */
    val table: List<SavedMilestone> = emptyList(),
    /** The length of the cycle when the user's table starts again after its last milestone; null when it ends. */
    val repeatsEveryMiles: Int? = null,
    /** The maker's schedule that ships for this model, while the vehicle has no table of its own; it is offered, never applied. */
    val known: KnownScheduleList? = null,
    /** For a table that was added from a bundled schedule: whose guide it is, said beside it for as long as the table is kept. */
    val startedFrom: String? = null,
    /** The vehicle's drive type, which decides which of a maker's jobs are for it. */
    val drivetrain: String? = null,
    val transmission: String? = null,
)

/**
 * A vehicle's schedule drawn out by mileage. Everything here is worked out from the schedule and
 * the service log each time; nothing is stored, and nothing here decides what is due.
 */
@HiltViewModel
class MilestonesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
    private val table: MilestoneDao,
    private val saver: ExportSaver,
    catalog: KnownScheduleCatalog,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<MilestonesRoute>().vehicleId

    /** How many milestones past the odometer are shown; "Show more" raises it. */
    private val ahead = MutableStateFlow(savedStateHandle.get<Int>(AHEAD) ?: Milestones.AHEAD)

    private val log = combine(service.observeRecords(vehicleId), odometer.observe(vehicleId), table.observe(vehicleId)) { records, readings, rows ->
        Triple(records, readings, rows)
    }

    private val knownLists = flow { emit(catalog.lists()) }

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<MilestonesState?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeTracked(vehicleId),
        log,
        combine(settings.settings, knownLists) { s, lists -> s to lists },
        ahead,
    ) { vehicle, items, (records, readings, rows), (s, lists), count ->
        if (vehicle == null) return@combine null
        val swap = vehicle.clusterSwap()
        val entries = records.map { it.toEntry(swap, items.answering()) }
        val tracked = items.mapNotNull { it.toTracked(s.defaultRule, swap = null) }.filter { it.interval.miles != null } // no swap: milestones are by actual mileage, and so is a first time
        val keys = tracked.map { it.id }.toSet()
        // A manual's mileages are miles really covered, so everything is placed by actual mileage.
        val current = VehicleReminders.currentOdometer(entries, readings.toReadings(swap))?.let { swap.actual(it) }
        val name = "${vehicle.year} ${vehicle.nickname ?: vehicle.model}"
        if (rows.isNotEmpty()) {
            // The user's own table is shown as it is, in place of the view built from the schedule.
            val saved = rows.map { r -> SavedMilestone(r.milestone.id, r.milestone.atMiles, r.ordered.map { TableJob(it.name, it.service) }, r.milestone.orMonths) }
            // A logged line completes a job by its catalog service, or by being called the same thing.
            val done = records.flatMap { r ->
                val e = r.toEntry(swap, NoLinks)
                r.items.flatMap { line ->
                    MilestoneImport.doneKeys(line.label, line.catalogId).map { JobDone(it, e.date, swap.actual(e.odometer), swap.asReadOn(e.odometer, e.date), e.id) }
                }
            }
            // A table that repeats goes on past what is shown, like the view built from the schedule; one that ends doesn't.
            val every = vehicle.milestonesRepeatEveryMiles?.takeIf { it >= saved.maxOf { m -> m.atMiles } }
            val list = if (every != null) MilestoneImport.milestones(saved, every, current, more = count - Milestones.AHEAD) else MilestoneImport.milestones(saved)
            return@combine MilestonesState(
                vehicleName = name,
                milestones = Milestones.status(list, done, current, continues = every != null),
                current = current,
                swap = swap,
                unit = vehicle.unit,
                hasMileageItems = true,
                fromTable = true,
                table = saved,
                model = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                repeatsEveryMiles = every,
                startedFrom = vehicle.milestonesKnownList?.let { id -> startedFrom(lists.firstOrNull { it.id == id }?.let(KnownSchedules::credit)) },
            )
        }
        val done = entries.flatMap { e ->
            e.itemIds.filter { it in keys }.map { JobDone(it.toString(), e.date, swap.actual(e.odometer), swap.asReadOn(e.odometer, e.date), e.id) }
        }
        val list = Milestones.generate(tracked, Milestones.startFor(done, current), current ?: 0, count)
        MilestonesState(
            vehicleName = name,
            milestones = Milestones.status(list, done, current, continues = true),
            current = current,
            swap = swap,
            unit = vehicle.unit,
            hasMileageItems = tracked.isNotEmpty(),
            fromTable = false,
            known = KnownSchedules.match(lists, vehicle.year, vehicle.make, vehicle.model, vehicle.engine, vehicle.fuel),
            drivetrain = vehicle.drivetrain,
            transmission = vehicle.transmission,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val handle = savedStateHandle

    /** Takes the user's table away; the screen goes back to the view built from the schedule. */
    fun removeTable() = viewModelScope.launch { table.deleteTable(vehicleId) }

    /** The milestone open in the editor; null while the list is showing. */
    var editing by mutableStateOf<MilestoneDraft?>(null)
        private set

    fun addMilestone() {
        editing = MilestoneDraft(id = null, atMiles = null, orMonths = null, jobs = emptyList())
    }

    /** Opens the table's milestone at [atMiles]; a repeat is edited through the one it repeats. */
    fun edit(atMiles: Int) {
        editing = state.value?.table?.firstOrNull { it.atMiles == atMiles }?.let { MilestoneDraft(it.id, it.atMiles, it.orMonths, it.work) }
    }

    fun closeEditor() {
        editing = null
    }

    /** [milestone] has been through MilestoneEdits; this only writes it. */
    fun save(draft: MilestoneDraft, milestone: ImportedMilestone) {
        editing = null
        viewModelScope.launch {
            table.save(
                MilestoneEntity(id = draft.id ?: 0, vehicleId = vehicleId, atMiles = milestone.atMiles, orMonths = milestone.orMonths),
                milestone.work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) },
            )
        }
    }

    fun delete(draft: MilestoneDraft) {
        editing = null
        draft.id?.let { id -> viewModelScope.launch { table.deleteMilestone(vehicleId, id) } }
    }

    /**
     * Adds the maker's table that ships for this model, in the vehicle's unit, as its milestone table.
     * It is then an ordinary table: edited, saved as a file or removed like one the user typed.
     */
    fun addKnown() {
        val s = state.value ?: return
        val list = s.known ?: return
        viewModelScope.launch {
            table.addKnown(
                vehicleId,
                list.id,
                KnownSchedules.milestonesIn(list, s.unit, s.drivetrain, s.transmission).map { m ->
                    MilestoneEntity(vehicleId = vehicleId, atMiles = m.atMiles, orMonths = m.orMonths) to
                        m.work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) }
                },
            )
        }
    }

    fun setRepeat(everyMiles: Int?) = viewModelScope.launch { table.setRepeat(vehicleId, everyMiles) }

    /**
     * Writes the table out as an import file and hands it to Android's "save as". The file holds the
     * table and nothing about the vehicle; like every export it is in the clear, and the working copy
     * is deleted once it has been copied out or the user backs out.
     */
    fun saveAsFile(text: String) {
        val s = state.value ?: return
        viewModelScope.launch { saver.saveImportFile(text, MilestoneExport.fileName(s.model)) }
    }

    fun showMore() {
        ahead.value += Milestones.MORE
        handle[AHEAD] = ahead.value
    }

    private companion object {
        const val AHEAD = "ahead"
    }
}

/** No rendering on the canvas; follows the Maintenance schedule screen it opens from. */
@Composable
fun MilestonesScreen(onBack: () -> Unit, onOpenRecord: (recordId: Long) -> Unit, vm: MilestonesViewModel = hiltViewModel()) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    val unit = state.unit
    var repeating by rememberSaveable { mutableStateOf(false) }
    if (repeating && state.table.isNotEmpty()) {
        MilestoneRepeatDialog(
            repeatsEveryMiles = state.repeatsEveryMiles,
            lastMiles = state.table.maxOf { it.atMiles },
            unit = unit,
            onSave = {
                repeating = false
                vm.setRepeat(it)
            },
            onDismiss = { repeating = false },
        )
    }

    var removing by rememberSaveable { mutableStateOf(false) }
    if (removing) {
        AlertDialog(
            onDismissRequest = { removing = false },
            title = { Text(stringResource(R.string.milestones_remove_title)) },
            text = {
                Text(
                    pluralStringResource(R.plurals.milestones_remove_body, state.table.size, state.table.size.asNumber()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removing = false
                        vm.removeTable()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    // Below everything the list remembers, so it is where it was left when the editor closes.
    val editing = vm.editing
    if (editing != null) {
        MilestoneEditor(
            draft = editing,
            others = state.table.filter { it.id != editing.id }.map { it.atMiles },
            repeatsEveryMiles = state.repeatsEveryMiles,
            unit = unit,
            startsTable = state.table.isEmpty(),
            endsTable = state.table.size == 1,
            onSave = { vm.save(editing, it) },
            onDelete = { vm.delete(editing) },
            onClose = vm::closeEditor,
        )
        return
    }

    MilestonesContent(
        state, onBack, onOpenRecord,
        onEdit = vm::edit, onShowMore = vm::showMore, onAddMilestone = vm::addMilestone, onChangeRepeat = { repeating = true },
        onSaveFile = vm::saveAsFile, onRemove = { removing = true }, onAddKnown = vm::addKnown,
    )
}

/** Milestones as given, with no ViewModel: what [MilestonesScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun MilestonesContent(
    state: MilestonesState,
    onBack: () -> Unit,
    onOpenRecord: (recordId: Long) -> Unit,
    /** Opens the editor on the user's own milestone at that mileage. */
    onEdit: (atMiles: Int) -> Unit,
    onShowMore: () -> Unit,
    onAddMilestone: () -> Unit,
    onChangeRepeat: () -> Unit,
    onSaveFile: (text: String) -> Unit,
    onRemove: () -> Unit,
    onAddKnown: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val unit = state.unit
    val list = rememberLazyListState()
    // Opens on the next milestone, with the past above it. Once only: after that the user is scrolling.
    var placed by rememberSaveable { mutableStateOf(false) }
    val rows = Milestones.folded(state.milestones)
    // Open or closed is the user's once they touch it: a card they tapped, or everything through the two
    // buttons. Until then only the next milestone is open, so the screen reads as a list of mileages.
    var all by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var openedCards by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var closedCards by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    // Runs of empty milestones the user has unfolded, by their first milestone.
    var openedRuns by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    fun isOpen(m: MilestoneStatus): Boolean {
        val at = m.milestone.atMiles
        return when {
            at in openedCards -> true
            at in closedCards -> false
            else -> all ?: (m.place == MilestonePlace.NEXT)
        }
    }
    fun toggle(m: MilestoneStatus) {
        val at = m.milestone.atMiles
        if (isOpen(m)) {
            openedCards = openedCards - at
            closedCards = closedCards + at
        } else {
            closedCards = closedCards - at
            openedCards = openedCards + at
        }
    }
    fun setAll(open: Boolean) {
        all = open
        openedCards = emptyList()
        closedCards = emptyList()
        // Expanding everything unfolds the empty runs too; collapsing puts them back.
        openedRuns = if (open) rows.filter { it.size > 1 }.map { it.first().milestone.atMiles } else emptyList()
    }
    LaunchedEffect(rows.isNotEmpty()) {
        if (!placed && rows.isNotEmpty()) {
            val next = rows.indexOfFirst { it.first().place == MilestonePlace.NEXT }
            if (next > 0) list.scrollToItem(next + HEADER_ITEMS)
            placed = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.milestones_title), onBack, subtitle = listOfNotNull(state.vehicleName, state.current?.let { if (state.swap != null) stringResource(R.string.milestones_actual, it.asDistance(unit)) else it.asDistance(unit) }).joinToString(" · "))

        LazyColumn(
            state = list,
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    if (state.startedFrom != null) {
                        stringResource(R.string.milestones_intro_known, state.startedFrom)
                    } else if (state.fromTable) {
                        state.repeatsEveryMiles?.let { stringResource(R.string.milestones_intro_table_repeats, it.asDistance(unit)) }
                            ?: stringResource(R.string.milestones_intro_table)
                    } else {
                        stringResource(R.string.milestones_intro_generated)
                    },
                    color = scheme.onSurfaceVariant,
                )
            }
            if (!state.hasMileageItems) {
                item {
                    Text(
                        stringResource(R.string.milestones_none),
                        color = scheme.onSurface,
                    )
                }
            }
            // Always there, even with nothing to build from, so HEADER_ITEMS holds.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(stringResource(R.string.expand_all) to true, stringResource(R.string.collapse_all) to false).forEach { (label, open) ->
                        OutlinedButton(
                            onClick = { setAll(open) },
                            enabled = state.milestones.isNotEmpty(),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                        ) {
                            Text(label)
                        }
                    }
                }
            }
            rows.forEach { row ->
                val first = row.first().milestone.atMiles
                if (row.size == 1 || first in openedRuns) {
                    items(row, key = { it.milestone.atMiles }) { m ->
                        MilestoneCard(
                            m, state.current, state.swap,
                            open = isOpen(m),
                            onToggle = { toggle(m) },
                            onOpenRecord = onOpenRecord,
                            // Only the user's own table can be edited; a repeat is the milestone it repeats.
                            onEdit = if (state.fromTable) ({ onEdit(m.milestone.repeatOf ?: m.milestone.atMiles) }) else null,
                            unit = unit,
                        )
                    }
                } else {
                    item(key = "gap-$first") {
                        // Nothing was logged against any of these; one line says so in place of a card each.
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.milestones_show_them), onClick = { openedRuns = openedRuns + first }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.milestones_folded, first.asNumber(), row.last().milestone.atMiles.asDistance(unit), row.size.asNumber()),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                            Text(stringResource(R.string.show), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
                        }
                    }
                }
            }
            // A table ends where the file ends unless it repeats; then it can go further, like the view built from the schedule.
            if (state.milestones.isNotEmpty() && (!state.fromTable || state.repeatsEveryMiles != null)) {
                item {
                    OutlinedButton(
                        onClick = onShowMore,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    ) {
                        Text(stringResource(R.string.milestones_further))
                    }
                }
            }
            if (state.fromTable) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (MilestoneEdits.canAdd(state.table.size)) {
                            OutlinedButton(
                                onClick = onAddMilestone,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                            ) {
                                Text(stringResource(R.string.milestones_add))
                            }
                        }
                        Surface(Modifier.fillMaxWidth(), color = scheme.surface, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                Modifier.heightIn(min = 56.dp)
                                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.milestones_change_repeat), onClick = onChangeRepeat)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(state.repeatsEveryMiles?.let { stringResource(R.string.milestones_repeats_every, it.asDistance(unit)) } ?: stringResource(R.string.milestones_no_repeat), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(if (state.repeatsEveryMiles != null) R.string.milestones_repeat_detail else R.string.milestones_ends_detail),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                Text(stringResource(R.string.change), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                item {
                    SaveAsImportFile(
                        caption = stringResource(R.string.milestones_save_caption),
                        write = { MilestoneExport.written(state.table, state.repeatsEveryMiles, unit) },
                        onSave = onSaveFile,
                    )
                }
                item {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                    ) {
                        Text(stringResource(R.string.milestones_remove), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (!state.fromTable) {
                state.known?.let { known ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onAddKnown,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, scheme.onSurface),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                            ) {
                                Text(stringResource(R.string.milestones_use_known, known.title))
                            }
                            Text(
                                stringResource(R.string.milestones_in_place, tableOffer(known)),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = onAddMilestone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.milestones_make_own), fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            stringResource(R.string.milestones_make_own_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Items in the list before the first milestone (the introduction and the two buttons), for scrolling to one. */
private const val HEADER_ITEMS = 2

/**
 * One milestone. The header is always shown and opens or closes the jobs under it; closed, it says
 * how many jobs there are and how many the log has, so a list of closed cards is still worth reading.
 */
@Composable
private fun MilestoneCard(
    m: MilestoneStatus,
    current: Int?,
    swap: ClusterSwap?,
    open: Boolean,
    onToggle: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onEdit: (() -> Unit)?,
    unit: DistanceUnit,
) {
    val scheme = MaterialTheme.colorScheme
    val next = m.place == MilestonePlace.NEXT
    val completed = m.jobs.count { it.done != null }
    // Filled and borderless like the cards on the schedule it opens from; the next one is outlined in the accent.
    Surface(
        Modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (next) BorderStroke(1.5.dp, scheme.primary) else null,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(if (open) R.string.collapse else R.string.expand), onClick = onToggle)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        m.milestone.atMiles.asDistance(unit),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.SemiBold,
                        color = if (m.place == MilestonePlace.PAST) scheme.onSurfaceVariant else scheme.onSurface,
                    )
                    Text(
                        listOfNotNull(
                            pluralStringResource(R.plurals.jobs, m.jobs.size, m.jobs.size.asNumber()),
                            pluralStringResource(R.plurals.completed, completed, completed.asNumber()).takeIf { completed > 0 },
                            m.milestone.orMonths?.let { pluralStringResource(R.plurals.milestones_or_months, it, it.asNumber()) },
                            m.milestone.repeatOf?.let { stringResource(R.string.milestones_repeat_of, it.asDistance(unit)) },
                            // With a replaced cluster the dash never shows the milestone's figure, so say what it will show.
                            swap?.takeIf { m.place != MilestonePlace.PAST && m.milestone.atMiles - it.offset > 0 }
                                ?.let { stringResource(R.string.milestones_on_dash, (m.milestone.atMiles - it.offset).asDistance(unit)) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (completed > 0) statusColors().ok else scheme.onSurfaceVariant,
                    )
                }
                if (next) {
                    Surface(color = scheme.primaryContainer, shape = CircleShape) {
                        Text(
                            current?.let { stringResource(R.string.milestones_next_in, (m.milestone.atMiles - it).asDistance(unit).uppercase(AppText.locale)) } ?: stringResource(R.string.milestones_next),
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }
                // Every job here has a record: the whole milestone is done.
                if (m.jobs.isNotEmpty() && completed == m.jobs.size) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.milestones_all_completed), modifier = Modifier.size(22.dp), tint = statusColors().ok)
                }
                Icon(
                    if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                )
            }
            if (open) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    m.jobs.forEach { job ->
                        HorizontalDivider(color = scheme.outlineVariant)
                        JobRow(job, unit, onOpenRecord)
                    }
                    if (onEdit != null) {
                        HorizontalDivider(color = scheme.outlineVariant)
                        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(m.milestone.repeatOf?.let { stringResource(R.string.milestones_edit_repeat, it.asDistance(unit)) } ?: stringResource(R.string.milestones_edit))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: MilestoneJobStatus, unit: DistanceUnit, onOpenRecord: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val done = job.done
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(done?.let { d -> Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.readings_open_record), onClick = { onOpenRecord(d.recordId) }) } ?: Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // A tick for work the log has; an empty place otherwise, so the names line up. It is never a
        // box to tick: the line under the name says which record it comes from.
        if (done != null) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp), tint = statusColors().ok)
        } else {
            Spacer(Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(job.job.name, style = MaterialTheme.typography.bodyLarge)
            // A job nothing in the log matches is simply listed: no "missed", no tick-box.
            if (done != null) {
                Text(
                    if (done.readMiles != done.actualMiles) {
                        stringResource(R.string.milestones_completed_dash, done.date.format(longDate), done.readMiles.asDistance(unit), done.actualMiles.asDistance(unit))
                    } else {
                        stringResource(R.string.milestones_completed, done.date.format(longDate), done.readMiles.asDistance(unit))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors().ok,
                )
            }
        }
        if (done != null) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
        }
    }
}
