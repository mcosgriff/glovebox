package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.data.db.MilestoneEntity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.outlined.Info
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.ScheduleExport
import io.cosgriff.glovebox.domain.WrittenFile
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.export.SaveAsImportFile
import io.cosgriff.glovebox.domain.KnownIntervalPlan
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import kotlinx.coroutines.flow.flow
import io.cosgriff.glovebox.ui.service.toSaved
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.DueRule
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
import io.cosgriff.glovebox.data.db.MilestoneDao
import kotlinx.coroutines.flow.map
import io.cosgriff.glovebox.ui.service.answering
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
    /**
     * The maker's schedule that ships for this model and what adding its reminders would do, while
     * there is still one to add. It is offered, never applied.
     */
    val known: Pair<KnownScheduleList, KnownIntervalPlan>? = null,
    /** Said once above the list when any item came from a maker's schedule: whose guide, and to check it. */
    val startedFrom: String? = null,
) {
    val use: DrivingUse get() = ScheduleSummaries.of(items.filter { it.severe != null || it.severeOnly }.map { it.tracked to it.useSevere }).use
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
    private val milestones: MilestoneDao,
    private val saver: ExportSaver,
    catalog: KnownScheduleCatalog,
) : ViewModel() {
    val vehicleId = savedStateHandle.route<ScheduleRoute>().vehicleId

    /** The user has imported a milestone table of their own, which the Milestones screen shows whatever the schedule holds. */
    val hasMilestoneTable: StateFlow<Boolean> = milestones.observe(vehicleId).map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var entities: Map<String, ScheduleItemEntity> = emptyMap()

    /** The schedule as an import file: every tracked item, as it is saved. */
    fun written(): WrittenFile = ScheduleExport.file(entities.values.sortedBy { it.id }.map { it.toSaved() }, state.value?.vehicle?.unit ?: DistanceUnit.MILES)

    /** [model] is the year, make and model, for the file's suggested name. */
    fun saveAsFile(text: String, model: String) {
        viewModelScope.launch { saver.saveImportFile(text, ScheduleExport.fileName(model)) }
    }

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<ScheduleState?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeAll(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
        combine(settings.settings, flow { emit(catalog.lists()) }) { s, lists -> s to lists },
    ) { vehicle, items, records, readings, (s, lists) ->
        if (vehicle == null) return@combine null
        val credits = lists.associate { it.id to KnownSchedules.credit(it) }
        entities = items.associateBy { it.id.toString() }
        val templates = ScheduleTemplates.generic(vehicle.drivetrain, vehicle.fuel, vehicle.unit).associateBy { it.key }
        val swap = vehicle.clusterSwap()
        val entries = records.map { it.toEntry(swap, items.answering()) }
        val updates = readings.toReadings(swap)
        val entered = records.filter { it.record.enteredAsLastDone }.map { it.record.id }.toSet()
        val statuses = VehicleReminders.statuses(
            items.filter { it.tracked }.mapNotNull { it.toTracked(s.defaultRule, swap) },
            entries,
            updates,
            LocalDate.now(),
            s.leadFor(vehicle.unit),
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
                    own = e.templateKey == null,
                    service = e.service,
                    knownList = e.knownList,
                    firstAt = e.firstMiles,
                    severeOnly = e.severeOnly,
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
                    estimate = status?.paceEstimate,
                )
            },
            defaultRule = s.defaultRule,
            // Not a typical item whose job one of the user's own already is: adding it back would be a second reminder.
            missing = items.filter { it.templateKey == null }.mapNotNull { HistoryImport.services[it.service]?.resets }.toSet().let { covered ->
                ScheduleTemplates.missing(vehicle.drivetrain, vehicle.fuel, vehicle.unit, items.map { it.templateKey }).filter { it.key !in covered }
            },
            odometer = VehicleReminders.currentOdometer(entries, updates),
            notLogged = statuses.values.count { !it.counting },
            startedFrom = items.firstNotNullOfOrNull { it.knownList }?.let { startedFrom(credits[it]) },
            known = KnownSchedules.match(lists, vehicle.year, vehicle.make, vehicle.model, vehicle.engine, vehicle.fuel)
                ?.let { list -> list to KnownSchedules.plan(KnownSchedules.intervalsIn(list, vehicle.unit, vehicle.drivetrain, vehicle.transmission), items.map { it.toSaved() }) }
                ?.takeIf { (_, plan) -> plan.new.isNotEmpty() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The item open in the editor. A key that isn't a saved item means a new one. */
    var editing by mutableStateOf<DraftItem?>(null)
        private set

    fun edit(item: DraftItem) {
        editing = item
    }

    fun addOwn() {
        editing = DraftItem.blank(NEW, state.value?.vehicle?.unit ?: DistanceUnit.MILES)
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

    /** Ticks or unticks every item at once. Only `tracked` changes; a snooze, an interval and every record stay. */
    fun setAllTracked(tracked: Boolean) {
        viewModelScope.launch { schedule.updateAll(entities.values.filter { it.tracked != tracked }.map { it.copy(tracked = tracked) }) }
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
    /**
     * Adds the reminders of the maker's schedule as items of the user's own, tracked, each remembering
     * the list it started from. [stop] are the typical items the user agreed to have replaced because
     * one of these is the same job: each is removed, or kept untracked if a service was logged against it.
     */
    fun addKnown(stop: Set<Long>) = viewModelScope.launch {
        val s = state.value ?: return@launch
        val (list, plan) = s.known ?: return@launch
        val severe = s.use == DrivingUse.SEVERE
        schedule.applyKnown(
            added = plan.new.map {
                ScheduleItemEntity(
                    vehicleId = vehicleId,
                    name = it.name,
                    intervalMiles = it.normal.miles,
                    intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles,
                    severeMonths = it.severe?.months,
                    useSevere = severe && (it.severe != null || it.severeOnly),
                    severeOnly = it.severeOnly,
                    ruleOverride = it.rule?.name,
                    service = it.service,
                    knownList = list.id,
                    firstMiles = it.firstAt,
                )
            },
            replaced = plan.doubles.map { it.id }.filter { it in stop },
        )
        // The maker's table goes with its schedule, unless the vehicle already has a table of its own.
        milestones.addKnown(
            vehicleId,
            list.id,
            KnownSchedules.milestonesIn(list, s.vehicle.unit, s.vehicle.drivetrain, s.vehicle.transmission).map { m ->
                MilestoneEntity(vehicleId = vehicleId, atMiles = m.atMiles, orMonths = m.orMonths) to
                    m.work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) }
            },
        )
    }

    fun addMissing() = viewModelScope.launch {
        val s = state.value ?: return@launch
        val severe = s.use == DrivingUse.SEVERE
        schedule.insertAll(
            s.missing.map {
                ScheduleItemEntity(
                    vehicleId = vehicleId,
                    name = it.label,
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
        firstMiles = firstAt,
        // Never on a typical item: those answer through their template key.
        service = service.takeIf { entity.templateKey == null },
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
    val hasMilestoneTable by vm.hasMilestoneTable.collectAsStateWithLifecycle()

    val editing = vm.editing
    if (editing != null) {
        val log = state.logs[editing.key]
        var enteringLastDone by rememberSaveable { mutableStateOf(false) }
        if (enteringLastDone) {
            LastDoneSheet(
                itemName = editing.name,
                currentOdometer = state.odometer,
                swap = state.vehicle.clusterSwap(),
                unit = state.vehicle.unit,
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
            unit = state.vehicle.unit,
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
                    resetBy = if (editing.own) null else ServiceCatalog.resetters(state.templateKeys[editing.key]).map(::catalogLabel),
                    estimate = log?.estimate,
                )
            },
        )
        return
    }

    ScheduleContent(
        state, hasMilestoneTable, onBack, onSetUp, onEnterLastDone, onOpenMilestones,
        onSetTracked = vm::setTracked, onEdit = vm::edit, onAddOwn = vm::addOwn, onAddKnown = { vm.addKnown(it) },
        onAddMissing = { vm.addMissing() }, onChooseSevereUse = { vm.chooseSevereUse(it) }, onSetAllTracked = vm::setAllTracked,
        onWriteFile = vm::written, onSaveFile = { vm.saveAsFile(it, "${state.vehicle.year} ${state.vehicle.make} ${state.vehicle.model}") },
    )
}

/** The maintenance schedule as given, with no ViewModel: what [ScheduleScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun ScheduleContent(
    state: ScheduleState,
    hasMilestoneTable: Boolean,
    onBack: () -> Unit,
    onSetUp: (vehicleId: Long) -> Unit,
    onEnterLastDone: (vehicleId: Long) -> Unit,
    onOpenMilestones: (vehicleId: Long) -> Unit,
    onSetTracked: (DraftItem, Boolean) -> Unit,
    onEdit: (DraftItem) -> Unit,
    onAddOwn: () -> Unit,
    onAddKnown: (stop: Set<Long>) -> Unit,
    onAddMissing: () -> Unit,
    onChooseSevereUse: (Boolean) -> Unit,
    onSetAllTracked: (Boolean) -> Unit,
    onWriteFile: () -> WrittenFile,
    onSaveFile: (text: String) -> Unit,
) {
    val v = state.vehicle
    // A ticked job that is only for severe use isn't reminding anyone on normal use, so it isn't counted.
    val tracked = state.items.count { it.tracked && !it.dormant }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    stringResource(R.string.details_schedule),
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
                    if (state.known != null) {
                        stringResource(R.string.schedule_empty_known)
                    } else {
                        stringResource(R.string.schedule_empty)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { onSetUp(v.id) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(if (state.known != null) R.string.schedule_start_known else R.string.schedule_start_typical))
                }
                // An imported table doesn't need a schedule to be worth looking at.
                if (hasMilestoneTable) MilestonesRow(onClick = { onOpenMilestones(v.id) })
            } else {
                if (state.items.any { it.severe != null || it.severeOnly }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(stringResource(R.string.schedule_how_driven))
                        DrivingUseSwitch(
                            severe = when (state.use) {
                                DrivingUse.NORMAL -> false
                                DrivingUse.SEVERE -> true
                                DrivingUse.MIXED -> null
                            },
                            onPick = onChooseSevereUse,
                        )
                        if (state.use == DrivingUse.MIXED) {
                            Text(
                                stringResource(R.string.schedule_mixed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                state.startedFrom?.let { text ->
                    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                stringResource(R.string.schedule_started_from, text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                // A tracked item that repeats by mileage is all the milestones view needs; so is a table of the user's own.
                if (hasMilestoneTable || state.items.any { it.tracked && it.active.miles != null }) MilestonesRow(onClick = { onOpenMilestones(v.id) })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.schedule_tracked, tracked.asNumber(), state.items.size.asNumber()), Modifier.weight(1f))
                    // As on setup: everything on, or with everything on, everything off.
                    val all = tracked == state.items.size
                    TextButton(onClick = { onSetAllTracked(!all) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(if (all) R.string.clear_all else R.string.select_all))
                    }
                }
            }

            if (state.notLogged > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onEnterLastDone(v.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Text(stringResource(R.string.schedule_enter_last_done))
                    }
                    Text(
                        pluralStringResource(R.plurals.schedule_not_logged, state.notLogged, state.notLogged.asNumber()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FilledCard {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    state.items.forEach { item ->
                        ItemRow(item, state.vehicle.unit, onToggle = { onSetTracked(item, !item.tracked) }, onEdit = { onEdit(item) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    TextButton(onClick = onAddOwn, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.schedule_add_own))
                    }
                }
            }

            // With nothing set up yet, setup is the way in (above); this is for a schedule that already has items.
            state.known?.takeIf { state.items.isNotEmpty() }?.let { (list, plan) ->
                var asking by rememberSaveable { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { asking = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        Text(stringResource(R.string.schedule_use_known))
                    }
                    Text(
                        stringResource(if (hasMilestoneTable) R.string.schedule_known_offer else R.string.schedule_known_offer_table, list.title, intervalOffer(list, plan)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (asking) {
                    KnownRemindersDialog(
                        list = list,
                        plan = plan,
                        unit = state.vehicle.unit,
                        onAdd = { stop ->
                            asking = false
                            onAddKnown(stop)
                        },
                        onDismiss = { asking = false },
                    )
                }
            }

            // A schedule that began with the user's own items can still pick up the typical ones.
            if (state.items.isNotEmpty() && state.missing.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onAddMissing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) {
                        val n = state.missing.size
                        Text(pluralStringResource(R.plurals.schedule_add_typical, n, n.asNumber()))
                    }
                    Text(
                        stringResource(R.string.schedule_missing, state.missing.joinToString(", ") { it.label }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                stringResource(R.string.schedule_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (tracked > 0) {
                SaveAsImportFile(
                    caption = stringResource(R.string.schedule_save_caption),
                    write = onWriteFile,
                    onSave = onSaveFile,
                )
            }
        }
    }
}

/** The way to the Milestones screen, as a link row like those on Vehicle details. */
@Composable
private fun MilestonesRow(onClick: () -> Unit) {
    FilledCard {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.milestones_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.schedule_milestones_row), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/**
 * What adding a maker's reminders will do, before it is done: each item with its interval, and the
 * typical items that would then be a second reminder for the same job, ticked to stop tracking.
 */
@Composable
private fun KnownRemindersDialog(
    list: KnownScheduleList,
    plan: KnownIntervalPlan,
    unit: DistanceUnit,
    onAdd: (stop: Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var stop by rememberSaveable { mutableStateOf(plan.doubles.map { it.id }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_known_title, list.source.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.schedule_known_added), color = scheme.onSurfaceVariant)
                plan.new.forEach { item ->
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            listOfNotNull(
                                stringResource(R.string.schedule_severe_only_short).takeIf { item.severeOnly },
                                item.normal.text(unit, item.firstAt),
                                item.severe?.let { stringResource(R.string.schedule_severe_use, it.text(unit)) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = PlexMono,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (plan.doubles.isNotEmpty()) {
                    Text(
                        stringResource(R.string.schedule_doubles),
                        color = scheme.onSurfaceVariant,
                    )
                    plan.doubles.forEach { double ->
                        val ticked = double.id in stop
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .toggleable(value = ticked, role = Role.Checkbox, onValueChange = { stop = if (it) stop + double.id else stop - double.id }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = ticked, onCheckedChange = null)
                            Text(stringResource(R.string.schedule_replace, double.name))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(stop.toSet()) }) { Text(pluralStringResource(R.plurals.schedule_add_reminders, plan.new.size, plan.new.size.asNumber())) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
