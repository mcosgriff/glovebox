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
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.JobDone
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
        val entries = records.map { it.toEntry(swap) }
        val tracked = items.mapNotNull { it.toTracked(s.defaultRule) }.filter { it.interval.miles != null }
        val keys = tracked.map { it.id }.toSet()
        // A manual's mileages are miles really covered, so everything is placed by actual mileage.
        val current = VehicleReminders.currentOdometer(entries, readings.toReadings(swap))?.let { swap.actual(it) }
        val name = "${vehicle.year} ${vehicle.nickname ?: vehicle.model}"
        if (rows.isNotEmpty()) {
            // The user's own table is shown as it is, in place of the view built from the schedule.
            val saved = rows.map { r -> SavedMilestone(r.milestone.id, r.milestone.atMiles, r.jobs.sortedBy { it.id }.map { TableJob(it.name, it.service) }, r.milestone.orMonths) }
            // A logged line completes a job by its catalog service, or by being called the same thing.
            val done = records.flatMap { r ->
                val e = r.toEntry(swap)
                r.items.flatMap { line ->
                    MilestoneImport.doneKeys(line.label, line.catalogId).map { JobDone(it, e.date, swap.actual(e.odometer), swap.asReadOn(e.odometer, e.date), e.id) }
                }
            }
            return@combine MilestonesState(name, Milestones.status(MilestoneImport.milestones(saved), done, current, continues = false), current, swap, hasMileageItems = true, fromTable = true)
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

    var removing by rememberSaveable { mutableStateOf(false) }
    if (removing) {
        AlertDialog(
            onDismissRequest = { removing = false },
            title = { Text("Remove your milestone table?") },
            text = {
                Text(
                    "The ${state.milestones.size} milestones you imported go, and this screen goes back to the ones built from your schedule. " +
                        "Your service history and reminders aren't touched. To get the table back, import the file again.",
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
                        "The milestone table you imported, with what your log says was done. " +
                            "It's a guide: the Reminders tab is what tells you something is due. The figures are the ones in your file; Glovebox hasn't checked them."
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
                        MilestoneCard(m, state.current, state.swap, open = isOpen(m), onToggle = { toggle(m) }, onOpenRecord = onOpenRecord)
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
            if (state.fromTable) {
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
            // A table ends where the file ends; only the view built from the schedule can go further.
            if (state.milestones.isNotEmpty() && !state.fromTable) {
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
private fun MilestoneCard(m: MilestoneStatus, current: Int?, swap: ClusterSwap?, open: Boolean, onToggle: () -> Unit, onOpenRecord: (Long) -> Unit) {
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
