package io.cosgriff.glovebox.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DecidedBy
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.ScheduleSummaries
import io.cosgriff.glovebox.domain.ScheduleTemplateItem
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.resetters
import io.cosgriff.glovebox.ui.ScheduleRoute
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.service.toTracked
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.runtime.saveable.rememberSaveable
import io.cosgriff.glovebox.ui.service.RecordActions
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.layout.size
import javax.inject.Inject

/**
 * What the log says about one schedule item, before it is turned into the editor's [ItemHistory].
 * [lastDone] is in dash terms; [lastRead] is the same service as the odometer read that day.
 * [entered] says the record is a bare "last done" and not a logged service.
 */
data class ItemLog(val lastDone: LastDone?, val lastRead: LastDone?, val recordId: Long?, val entered: Boolean, val estimate: LocalDate?)

data class ScheduleState(
    val vehicle: VehicleEntity,
    /** Tracked and untracked, by name. */
    val items: List<DraftItem>,
    val templateKeys: Map<String, String?>,
    val logs: Map<String, ItemLog>,
    val defaultRule: DueRule,
    /** Typical items this schedule doesn't have, e.g. after a skipped setup. */
    val missing: List<ScheduleTemplateItem>,
    /** The current odometer in dash terms; null for a vehicle with no mileage yet. */
    val odometer: Int?,
    /** Tracked reminders that have never been logged. */
    val notLogged: Int,
) {
    val use: DrivingUse get() = ScheduleSummaries.of(items.filter { it.severe != null }.map { it.tracked to it.useSevere }).use
}

/** A vehicle's schedule after setup: what is tracked, at which intervals, and when each was last done. */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    private val schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
    private val actions: RecordActions,
) : ViewModel() {
    val vehicleId = savedStateHandle.toRoute<ScheduleRoute>().vehicleId

    private var entities: Map<String, ScheduleItemEntity> = emptyMap()

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<ScheduleState?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeAll(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
        settings.settings,
    ) { vehicle, items, records, readings, s ->
        if (vehicle == null) return@combine null
        entities = items.associateBy { it.id.toString() }
        val templates = ScheduleTemplates.generic(vehicle.drivetrain, vehicle.fuel).associateBy { it.key }
        val swap = vehicle.clusterSwap()
        val entries = records.map { it.toEntry(swap) }
        val updates = readings.toReadings(swap)
        val entered = records.filter { it.record.enteredAsLastDone }.map { it.record.id }.toSet()
        val statuses = VehicleReminders.statuses(
            items.filter { it.tracked }.mapNotNull { it.toTracked(s.defaultRule) },
            entries,
            updates,
            LocalDate.now(),
            s.lead,
        ).associateBy { it.item.id }
        ScheduleState(
            vehicle = vehicle,
            items = items.mapNotNull { e ->
                val normal = interval(e.intervalMiles, e.intervalMonths) ?: return@mapNotNull null
                DraftItem(
                    key = e.id.toString(),
                    name = e.name,
                    normal = normal,
                    severe = interval(e.severeMiles, e.severeMonths),
                    useSevere = e.useSevere,
                    rule = e.ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() },
                    tracked = e.tracked,
                    template = e.templateKey?.let(templates::get),
                )
            },
            templateKeys = items.associate { it.id.toString() to it.templateKey },
            logs = items.associate { e ->
                val last = ServiceLog.lastEntry(e.id, entries)
                val status = statuses[e.id]?.status
                e.id.toString() to ItemLog(
                    lastDone = last?.let { LastDone(it.date, it.odometer) },
                    lastRead = last?.let { swap.asReadOn(LastDone(it.date, it.odometer)) },
                    recordId = last?.id,
                    entered = last != null && last.id in entered,
                    // Only an estimate when it comes from the driving pace and isn't already past.
                    estimate = status?.estimatedDueDate?.takeIf { status.decidedBy == DecidedBy.MILEAGE && status.state != DueState.OVERDUE },
                )
            },
            defaultRule = s.defaultRule,
            missing = ScheduleTemplates.missing(vehicle.drivetrain, vehicle.fuel, items.map { it.templateKey }),
            odometer = VehicleReminders.currentOdometer(entries, updates),
            notLogged = statuses.values.count { it.lastDone == null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The item open in the editor. A key that isn't a saved item means a new one. */
    var editing by mutableStateOf<DraftItem?>(null)
        private set

    fun edit(item: DraftItem) {
        editing = item
    }

    fun addOwn() {
        editing = DraftItem(key = NEW, name = "", normal = Interval(5_000, 6), severe = null)
    }

    fun isNew(item: DraftItem): Boolean = item.key == NEW

    fun closeEditor() {
        editing = null
    }

    /** A date and mileage for the saved item [item], which starts its reminder counting. */
    fun enterLastDone(item: DraftItem, date: LocalDate, odometer: Int) = viewModelScope.launch {
        val entity = entities[item.key] ?: return@launch
        actions.addLastDone(vehicleId, entity, date, odometer)
    }

    fun saveEdit(item: DraftItem) {
        editing = null
        viewModelScope.launch {
            val existing = entities[item.key]
            if (existing == null) {
                schedule.insertAll(listOf(item.applyTo(ScheduleItemEntity(vehicleId = vehicleId, name = item.name, intervalMiles = null, intervalMonths = null))))
            } else {
                schedule.update(item.applyTo(existing))
            }
        }
    }

    fun setTracked(item: DraftItem, tracked: Boolean) {
        editing = null
        viewModelScope.launch { entities[item.key]?.let { schedule.update(it.copy(tracked = tracked)) } }
    }

    /** Normal or severe for every item that has a severe interval. */
    fun chooseSevereUse(severe: Boolean) = viewModelScope.launch {
        entities.values.filter { it.useSevere != severe }.forEach { schedule.update(it.copy(useSevere = severe)) }
    }

    /**
     * Adds the typical items the schedule lacks, unticked: the user picks which to track, so a
     * dozen "not logged yet" reminders don't appear at once.
     */
    fun addMissing() = viewModelScope.launch {
        val s = state.value ?: return@launch
        val severe = s.use == DrivingUse.SEVERE
        schedule.insertAll(
            s.missing.map {
                ScheduleItemEntity(
                    vehicleId = vehicleId,
                    name = it.name,
                    templateKey = it.key,
                    intervalMiles = it.normal.miles,
                    intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles,
                    severeMonths = it.severe?.months,
                    useSevere = severe,
                    tracked = false,
                )
            },
        )
    }

    private fun interval(miles: Int?, months: Int?): Interval? = if (miles == null && months == null) null else Interval(miles, months)

    /** Keeps everything the editor doesn't touch, such as a snooze. */
    private fun DraftItem.applyTo(entity: ScheduleItemEntity) = entity.copy(
        name = name,
        intervalMiles = normal.miles,
        intervalMonths = normal.months,
        severeMiles = severe?.miles,
        severeMonths = severe?.months,
        useSevere = useSevere,
        ruleOverride = rule?.name,
        tracked = tracked,
    )

    private companion object {
        const val NEW = "new"
    }
}

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onOpenRecord: (recordId: Long) -> Unit,
    /** For a vehicle whose setup was skipped. */
    onSetUp: (vehicleId: Long) -> Unit,
    /** The "When were these last done?" list. */
    onEnterLastDone: (vehicleId: Long) -> Unit,
    /** The same schedule laid out by mileage. */
    onOpenMilestones: (vehicleId: Long) -> Unit,
    vm: ScheduleViewModel = hiltViewModel(),
) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading

    val editing = vm.editing
    if (editing != null) {
        val log = state.logs[editing.key]
        var enteringLastDone by rememberSaveable { mutableStateOf(false) }
        if (enteringLastDone) {
            LastDoneSheet(
                itemName = editing.name,
                currentOdometer = state.odometer,
                swap = state.vehicle.clusterSwap(),
                onSave = { date, odometer ->
                    vm.enterLastDone(editing, date, odometer)
                    enteringLastDone = false
                },
                onDismiss = { enteringLastDone = false },
            )
        }
        BackHandler(onBack = vm::closeEditor)
        ScheduleItemEditor(
            item = editing,
            isNew = vm.isNew(editing),
            defaultRule = state.defaultRule,
            onSave = vm::saveEdit,
            onStopTracking = { vm.setTracked(editing, false) },
            onClose = vm::closeEditor,
            history = if (vm.isNew(editing)) {
                null
            } else {
                ItemHistory(
                    lastDone = log?.lastDone,
                    lastRead = log?.lastRead,
                    onOpenRecord = log?.recordId?.let { id -> { onOpenRecord(id) } },
                    entered = log?.entered == true,
                    onEnterLastDone = { enteringLastDone = true },
                    resetBy = ServiceCatalog.resetters(state.templateKeys[editing.key]),
                    estimate = log?.estimate,
                )
            },
        )
        return
    }

    val v = state.vehicle
    val tracked = state.items.count { it.tracked }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    "Maintenance schedule",
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    listOfNotNull("${v.year} ${v.nickname ?: v.model}", v.engine).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (state.items.isEmpty()) {
                Text(
                    "Nothing is set up for this vehicle yet. Start from a typical schedule, or add your own items one at a time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { onSetUp(v.id) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Start from a typical schedule")
                }
            } else {
                if (state.items.any { it.severe != null }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("HOW DO YOU DRIVE IT?")
                        DrivingUseSwitch(
                            severe = when (state.use) {
                                DrivingUse.NORMAL -> false
                                DrivingUse.SEVERE -> true
                                DrivingUse.MIXED -> null
                            },
                            onPick = vm::chooseSevereUse,
                        )
                        if (state.use == DrivingUse.MIXED) {
                            Text(
                                "Some items are on their severe interval and some aren't. Picking one here sets them all.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                // A tracked item that repeats by mileage is all the milestones view needs.
                if (state.items.any { it.tracked && it.active.miles != null }) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .clickable(role = Role.Button, onClick = { onOpenMilestones(v.id) })
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Milestones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "This schedule by mileage, with what was done",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SectionLabel("$tracked OF ${state.items.size} TRACKED")
            }

            if (state.notLogged > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onEnterLastDone(v.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Text("Enter when they were last done")
                    }
                    Text(
                        (if (state.notLogged == 1) "1 tracked item hasn't" else "${state.notLogged} tracked items haven't") +
                            " been logged yet, so there is nothing to count from.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    state.items.forEach { item ->
                        ItemRow(item, onToggle = { vm.setTracked(item, !item.tracked) }, onEdit = { vm.edit(item) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    TextButton(onClick = vm::addOwn, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("+ Add your own item")
                    }
                }
            }

            // A schedule that began with the user's own items can still pick up the typical ones.
            if (state.items.isNotEmpty() && state.missing.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = vm::addMissing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        val n = state.missing.size
                        Text(if (n == 1) "Add 1 typical item" else "Add $n typical items")
                    }
                    Text(
                        state.missing.joinToString(", ") { it.name } + ". They're added unticked, so tick the ones to track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                "Unticked items stay here but don't remind you. The suggested intervals are common ones, not the " +
                    "manufacturer's: check your owner's manual and edit anything that differs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
