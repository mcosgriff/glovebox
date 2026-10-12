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
import androidx.compose.material3.ButtonDefaults
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
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.JobDone
import io.cosgriff.glovebox.domain.MilestoneJobStatus
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
import javax.inject.Inject

data class MilestonesState(
    val vehicleName: String,
    val milestones: List<MilestoneStatus>,
    /** Miles the vehicle has really covered; null with no reading yet. */
    val current: Int?,
    val swap: ClusterSwap?,
    /** False when nothing tracked repeats by mileage, so there is nothing to build milestones from. */
    val hasMileageItems: Boolean,
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
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<MilestonesRoute>().vehicleId

    /** How many milestones past the odometer are shown; "Show more" raises it. */
    private val ahead = MutableStateFlow(savedStateHandle.get<Int>(AHEAD) ?: Milestones.AHEAD)

    private val log = combine(service.observeRecords(vehicleId), odometer.observe(vehicleId)) { records, readings -> records to readings }

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<MilestonesState?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeTracked(vehicleId),
        log,
        settings.settings,
        ahead,
    ) { vehicle, items, (records, readings), s, count ->
        if (vehicle == null) return@combine null
        val swap = vehicle.clusterSwap()
        val entries = records.map { it.toEntry(swap) }
        val tracked = items.mapNotNull { it.toTracked(s.defaultRule) }.filter { it.interval.miles != null }
        val keys = tracked.map { it.id }.toSet()
        // A manual's mileages are miles really covered, so everything is placed by actual mileage.
        val current = VehicleReminders.currentOdometer(entries, readings.toReadings(swap))?.let { swap.actual(it) }
        val done = entries.flatMap { e ->
            e.itemIds.filter { it in keys }.map { JobDone(it.toString(), e.date, swap.actual(e.odometer), swap.asReadOn(e.odometer, e.date), e.id) }
        }
        val list = Milestones.generate(tracked, Milestones.startFor(done, current), current ?: 0, count)
        MilestonesState(
            vehicleName = "${vehicle.year} ${vehicle.nickname ?: vehicle.model}",
            milestones = Milestones.status(list, done, current, continues = true),
            current = current,
            swap = swap,
            hasMileageItems = tracked.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val handle = savedStateHandle

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
    LaunchedEffect(state.milestones.isNotEmpty()) {
        if (!placed && state.milestones.isNotEmpty()) {
            val next = state.milestones.indexOfFirst { it.place == MilestonePlace.NEXT }
            if (next > 0) list.scrollToItem(next + HEADER_ITEMS)
            placed = true
        }
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
                    "Your schedule laid out by mileage, the way an owner's manual prints it, with what your log says was done. " +
                        "It's a guide: the Reminders tab is what tells you something is due. The intervals are the ones you set, not the manufacturer's.",
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
            items(state.milestones, key = { it.milestone.atMiles }) { m -> MilestoneCard(m, state.current, state.swap, onOpenRecord) }
            if (state.milestones.isNotEmpty()) {
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

/** Items in the list before the first milestone, for scrolling to one. */
private const val HEADER_ITEMS = 1

@Composable
private fun MilestoneCard(m: MilestoneStatus, current: Int?, swap: ClusterSwap?, onOpenRecord: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val next = m.place == MilestonePlace.NEXT
    Surface(
        Modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (next) 1.5.dp else 1.dp, if (next) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    m.milestone.atMiles.asMiles(),
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    color = if (m.place == MilestonePlace.PAST) scheme.onSurfaceVariant else scheme.onSurface,
                )
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
            }
            // With a replaced cluster the dash never shows this figure, so say what it will show.
            if (swap != null && m.place != MilestonePlace.PAST && m.milestone.atMiles - swap.offset > 0) {
                Text("${(m.milestone.atMiles - swap.offset).asMiles()} on the dash", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            m.jobs.forEachIndexed { i, job ->
                if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                JobRow(job, onOpenRecord)
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
            .heightIn(min = if (done == null) 32.dp else 48.dp)
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
