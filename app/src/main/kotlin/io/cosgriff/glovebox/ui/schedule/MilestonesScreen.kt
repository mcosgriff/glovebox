package io.cosgriff.glovebox.ui.schedule

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import io.cosgriff.glovebox.ui.service.asMiles
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
import java.util.Locale
import io.cosgriff.glovebox.ui.service.answering
import io.cosgriff.glovebox.ui.service.NoLinks
import javax.inject.Inject

data class MilestonesState(
    val vehicleName: String,
    val milestones: List<MilestoneStatus>,
    /** Miles the vehicle has really covered; null with no reading yet. */
    val current: Int?,
    val swap: ClusterSwap?,
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
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<MilestonesRoute>().vehicleId

    /** How many milestones past the odometer are shown; "Show more" raises it. */
    private val ahead = MutableStateFlow(savedStateHandle.get<Int>(AHEAD) ?: Milestones.AHEAD)

    private val log = combine(service.observeRecords(vehicleId), odometer.observe(vehicleId), table.observe(vehicleId)) { records, readings, rows ->
        Triple(records, readings, rows)
    }

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<MilestonesState?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeTracked(vehicleId),
        log,
        settings.settings,
        ahead,
    ) { vehicle, items, (records, readings, rows), s, count ->
        if (vehicle == null) return@combine null
        val swap = vehicle.clusterSwap()
        val entries = records.map { it.toEntry(swap, items.answering()) }
        val tracked = items.mapNotNull { it.toTracked(s.defaultRule) }.filter { it.interval.miles != null }
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
                hasMileageItems = true,
                fromTable = true,
                table = saved,
                model = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                repeatsEveryMiles = every,
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
            hasMileageItems = tracked.isNotEmpty(),
            fromTable = false,
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

    fun setRepeat(everyMiles: Int?) = viewModelScope.launch { table.setRepeat(vehicleId, everyMiles) }

    /**
     * Writes the table out as an import file and hands it to Android's "save as". The file holds the
     * table and nothing about the vehicle; like every export it is in the clear, and the working copy
     * is deleted once it has been copied out or the user backs out.
     */
    fun saveAsFile() {
        val s = state.value?.takeIf { it.table.isNotEmpty() } ?: return
        val name = MilestoneExport.fileName(s.model)
        viewModelScope.launch {
            val file = saver.newFile()
            withContext(Dispatchers.IO) { file.writeText(MilestoneExport.file(s.table, s.repeatsEveryMiles)) }
            saver.save(file, name, MilestoneExport.MIME_TYPE, "Saved $name") {}
        }
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

    var repeating by rememberSaveable { mutableStateOf(false) }
    if (repeating && state.table.isNotEmpty()) {
        MilestoneRepeatDialog(
            repeatsEveryMiles = state.repeatsEveryMiles,
            lastMiles = state.table.maxOf { it.atMiles },
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
            title = { Text("Remove your milestone table?") },
            text = {
                Text(
                    "${if (state.table.size == 1) "Its one milestone goes" else "All ${state.table.size} of its milestones go"}, " +
                        "and this screen goes back to the ones built from your schedule. " +
                        "Your service history and reminders aren't touched. This can't be undone: to get the table back you would type it in or import it again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removing = false
                        vm.removeTable()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removing = false }) { Text("Cancel") } },
        )
    }

    // Below everything the list remembers, so it is where it was left when the editor closes.
    val editing = vm.editing
    if (editing != null) {
        MilestoneEditor(
            draft = editing,
            others = state.table.filter { it.id != editing.id }.map { it.atMiles },
            repeatsEveryMiles = state.repeatsEveryMiles,
            startsTable = state.table.isEmpty(),
            endsTable = state.table.size == 1,
            onSave = { vm.save(editing, it) },
            onDelete = { vm.delete(editing) },
            onClose = vm::closeEditor,
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Column(Modifier.padding(start = 8.dp)) {
                Text("Milestones", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(state.vehicleName, state.current?.let { "${it.asMiles()}${if (state.swap != null) " actual" else ""}" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            state = list,
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    if (state.fromTable) {
                        "Your own milestone table, with what your log says was done. " +
                            (state.repeatsEveryMiles?.let { "It repeats every ${it.asMiles()}, so after its last milestone it starts again from the first. " } ?: "") +
                            "It's a guide: the Reminders tab is what tells you something is due. The figures are the ones you typed or imported; Glovebox hasn't checked them."
                    } else {
                        "Your schedule laid out by mileage, the way an owner's manual prints it, with what your log says was done. " +
                            "It's a guide: the Reminders tab is what tells you something is due. The intervals are the ones you set, not the manufacturer's."
                    },
                    color = scheme.onSurfaceVariant,
                )
            }
            if (!state.hasMileageItems) {
                item {
                    Text(
                        "Milestones are built from tracked items that repeat by mileage, and none of this vehicle's do yet. " +
                            "Give an item a mileage interval on the Maintenance schedule and it appears here.",
                        color = scheme.onSurface,
                    )
                }
            }
            // Always there, even with nothing to build from, so HEADER_ITEMS holds.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Expand all" to true, "Collapse all" to false).forEach { (label, open) ->
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
                            onEdit = if (state.fromTable) ({ vm.edit(m.milestone.repeatOf ?: m.milestone.atMiles) }) else null,
                        )
                    }
                } else {
                    item(key = "gap-$first") {
                        // Nothing was logged against any of these; one line says so in place of a card each.
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = "Show them", onClick = { openedRuns = openedRuns + first }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${String.format(Locale.US, "%,d", first)} to ${row.last().milestone.atMiles.asMiles()} · ${row.size} with nothing logged",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                            Text("Show", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
                        }
                    }
                }
            }
            // A table ends where the file ends unless it repeats; then it can go further, like the view built from the schedule.
            if (state.milestones.isNotEmpty() && (!state.fromTable || state.repeatsEveryMiles != null)) {
                item {
                    OutlinedButton(
                        onClick = vm::showMore,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    ) {
                        Text("Show further ahead")
                    }
                }
            }
            if (state.fromTable) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (MilestoneEdits.canAdd(state.table.size)) {
                            OutlinedButton(
                                onClick = vm::addMilestone,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                            ) {
                                Text("Add a milestone")
                            }
                        }
                        Surface(Modifier.fillMaxWidth(), color = scheme.surface, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                Modifier.heightIn(min = 56.dp)
                                    .clickable(role = Role.Button, onClickLabel = "Change how the table repeats", onClick = { repeating = true })
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(state.repeatsEveryMiles?.let { "Repeats every ${it.asMiles()}" } ?: "Doesn't repeat", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (state.repeatsEveryMiles != null) "After the last milestone it starts again from the first" else "The table ends at its last milestone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                Text("Change", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = vm::saveAsFile, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Save as an import file", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "A copy to keep, or to pass to someone with the same model. It holds this table and nothing about your vehicle or its history, and it isn't encrypted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = { removing = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                    ) {
                        Text("Remove this table", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (!state.fromTable) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = vm::addMilestone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Make my own table", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Type in the table from your owner's manual, a milestone at a time. It is then shown here in place of these.",
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
                    .clickable(role = Role.Button, onClickLabel = if (open) "Collapse" else "Expand", onClick = onToggle)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        m.milestone.atMiles.asMiles(),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.SemiBold,
                        color = if (m.place == MilestonePlace.PAST) scheme.onSurfaceVariant else scheme.onSurface,
                    )
                    Text(
                        listOfNotNull(
                            if (m.jobs.size == 1) "1 job" else "${m.jobs.size} jobs",
                            "$completed completed".takeIf { completed > 0 },
                            m.milestone.orMonths?.let { "or $it months" },
                            m.milestone.repeatOf?.let { "repeat of ${it.asMiles()}" },
                            // With a replaced cluster the dash never shows the milestone's figure, so say what it will show.
                            swap?.takeIf { m.place != MilestonePlace.PAST && m.milestone.atMiles - it.offset > 0 }
                                ?.let { "${(m.milestone.atMiles - it.offset).asMiles()} on the dash" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (completed > 0) statusColors().ok else scheme.onSurfaceVariant,
                    )
                }
                if (next) {
                    Surface(color = scheme.primaryContainer, shape = CircleShape) {
                        Text(
                            current?.let { "NEXT · IN ${(m.milestone.atMiles - it).asMiles().uppercase()}" } ?: "NEXT",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                        )
                    }
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
                        JobRow(job, onOpenRecord)
                    }
                    if (onEdit != null) {
                        HorizontalDivider(color = scheme.outlineVariant)
                        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(m.milestone.repeatOf?.let { "Edit the ${it.asMiles()} milestone" } ?: "Edit this milestone")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: MilestoneJobStatus, onOpenRecord: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val done = job.done
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(done?.let { d -> Modifier.clickable(role = Role.Button, onClickLabel = "Open the record", onClick = { onOpenRecord(d.recordId) }) } ?: Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(job.job.name, style = MaterialTheme.typography.bodyLarge)
            // A job nothing in the log matches is simply listed: no "missed", no tick-box.
            if (done != null) {
                Text(
                    "Completed ${done.date.format(longDate)} at ${done.readMiles.asMiles()}" +
                        if (done.readMiles != done.actualMiles) " on the dash (${done.actualMiles.asMiles()} actual)" else "",
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
