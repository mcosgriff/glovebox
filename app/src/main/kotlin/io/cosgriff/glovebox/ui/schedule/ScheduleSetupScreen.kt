package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import io.cosgriff.glovebox.data.db.MilestoneDao
import io.cosgriff.glovebox.data.db.MilestoneEntity
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.ScheduleTemplateItem
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.ui.ScheduleSetupRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A schedule item being set up; nothing is written to the database until the user confirms. */
data class DraftItem(
    val key: String,
    val name: String,
    val normal: Interval,
    val severe: Interval?,
    val useSevere: Boolean = false,
    /** Null = use the app-wide default rule. */
    val rule: DueRule? = null,
    val tracked: Boolean = true,
    /** The suggestion this came from; null for items the user added. */
    val template: ScheduleTemplateItem? = null,
    /** One the user added, not one of the typical items Glovebox suggested. Only these can be pointed at a service line. */
    val own: Boolean = template == null,
    /** For the user's own: the CatalogItem id of the built-in service line that resets it; null = logged under "Your items". */
    val service: String? = null,
    /** For an item from a maker's schedule that ships with the app: that list's id, kept on the row for good. */
    val knownList: String? = null,
    /** The distance covered at which the job is first due, when that isn't one interval in; null for most. */
    val firstAt: Int? = null,
    /** A job that exists only under severe use; [normal] is its interval then. On normal use it is listed, switched off. */
    val severeOnly: Boolean = false,
) {
    /** On the list but not reminding: a job only for severe use, while the schedule is on normal use. */
    val dormant: Boolean get() = severeOnly && !useSevere
    val active: Interval get() = if (useSevere) severe ?: normal else normal
    val showsSevere: Boolean get() = useSevere && severe != null && severe != normal
}

@HiltViewModel
class ScheduleSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicles: VehicleDao,
    private val schedule: ScheduleDao,
    settings: SettingsRepository,
    private val catalog: KnownScheduleCatalog,
    private val milestones: MilestoneDao,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<ScheduleSetupRoute>().vehicleId

    /**
     * The maker's schedule that ships for this vehicle, when there is one. It is then the schedule
     * setup starts from, in place of the typical list, and its table becomes the vehicle's milestones.
     */
    var known by mutableStateOf<KnownScheduleList?>(null)
        private set

    /** The maker's schedules for this model and year, one per engine, when the vehicle's engine isn't on record. */
    var engineChoices by mutableStateOf<List<KnownScheduleList>>(emptyList())
        private set

    private var chosenEngine: String? = null

    var vehicle by mutableStateOf<VehicleEntity?>(null)
        private set

    /** Null until the vehicle has loaded. */
    var items by mutableStateOf<List<DraftItem>?>(null)
        private set
    var severeUse by mutableStateOf(false)
        private set

    /** "Know when these were last done?": true goes on to ask, false leaves them to be logged as they come up. */
    var enterLastDone by mutableStateOf(false)

    /** The item open in the editor. A key that isn't in [items] means a new item. */
    var editing by mutableStateOf<DraftItem?>(null)
        private set

    val defaultRule = settings.settings.map { it.defaultRule }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings().defaultRule)

    private var customCount = 0

    init {
        viewModelScope.launch {
            val v = vehicles.observe(vehicleId).first()
            vehicle = v
            val unit = v?.unit ?: DistanceUnit.MILES
            val lists = catalog.lists()
            val list = v?.let { KnownSchedules.match(lists, it.year, it.make, it.model, it.engine, it.fuel) }
            known = list
            // The maker has a schedule per engine and this vehicle's isn't on record: the owner says which.
            if (list == null && v != null) engineChoices = KnownSchedules.engineChoices(lists, v.year, v.make, v.model, v.engine, v.fuel)
            items = if (list != null && v != null) {
                knownItems(list, v)
            } else {
                ScheduleTemplates.generic(v?.drivetrain, v?.fuel, unit).map {
                    DraftItem(key = it.key, name = it.label, normal = it.normal, severe = it.severe, template = it)
                }
            }
        }
    }

    private fun knownItems(list: KnownScheduleList, v: VehicleEntity): List<DraftItem> =
        KnownSchedules.intervalsIn(list, v.unit, v.drivetrain, v.transmission).mapIndexed { i, it ->
            DraftItem(
                key = "known-$i", name = it.name, normal = it.normal, severe = it.severe, rule = it.rule,
                own = true, service = it.service, knownList = list.id, firstAt = it.firstAt, severeOnly = it.severeOnly,
            )
        }

    /**
     * The owner says which of [engineChoices] is their engine. The schedule becomes that list's, and
     * the engine is written to the vehicle when it is saved, if it has none on record.
     */
    fun chooseEngine(list: KnownScheduleList) {
        val v = vehicle ?: return
        known = list
        chosenEngine = list.engine?.label
        engineChoices = emptyList()
        severeUse = false
        items = knownItems(list, v)
    }

    /** "Neither of these": the typical list stays, and the question isn't asked again on this screen. */
    fun declineEngines() {
        engineChoices = emptyList()
    }

    fun chooseSevereUse(on: Boolean) {
        severeUse = on
        items = items?.map { it.copy(useSevere = on) }
    }

    fun toggle(key: String) {
        items = items?.map { if (it.key == key) it.copy(tracked = !it.tracked) else it }
    }

    fun toggleAll() {
        val list = items ?: return
        val allOn = list.all { it.tracked }
        items = list.map { it.copy(tracked = !allOn) }
    }

    fun edit(key: String) {
        editing = items?.firstOrNull { it.key == key }
    }

    fun addOwn() {
        editing = DraftItem(key = "custom-${customCount++}", name = "", normal = Interval(5_000, 6), severe = null)
    }

    fun isNew(item: DraftItem): Boolean = items?.none { it.key == item.key } == true

    fun saveEdit(item: DraftItem) {
        val list = items ?: return
        items = if (list.any { it.key == item.key }) list.map { if (it.key == item.key) item else it } else list + item
        editing = null
    }

    fun stopTracking(key: String) {
        items = items?.map { if (it.key == key) it.copy(tracked = false) else it }
        editing = null
    }

    fun closeEditor() {
        editing = null
    }

    /** Unticked items are kept as untracked rows so they can be switched on later. */
    fun save(onDone: (vehicleId: Long) -> Unit) = viewModelScope.launch {
        val list = items ?: return@launch
        schedule.insertAll(
            list.map {
                ScheduleItemEntity(
                    vehicleId = vehicleId,
                    name = it.name,
                    templateKey = it.template?.key,
                    intervalMiles = it.normal.miles,
                    intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles,
                    severeMonths = it.severe?.months,
                    useSevere = it.useSevere,
                    ruleOverride = it.rule?.name,
                    tracked = it.tracked,
                    service = it.service.takeIf { _ -> it.own },
                    knownList = it.knownList,
                    firstMiles = it.firstAt,
                    severeOnly = it.severeOnly,
                )
            },
        )
        chosenEngine?.let { vehicles.setEngineIfBlank(vehicleId, it) }
        // The maker's table goes with its schedule: it is what the Milestones screen then shows.
        known?.let { list ->
            val unit = vehicle?.unit ?: DistanceUnit.MILES
            milestones.addKnown(
                vehicleId,
                list.id,
                KnownSchedules.milestonesIn(list, unit, vehicle?.drivetrain, vehicle?.transmission).map { m ->
                    MilestoneEntity(vehicleId = vehicleId, atMiles = m.atMiles, orMonths = m.orMonths) to
                        m.work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) }
                },
            )
        }
        onDone(vehicleId)
    }
}

@Composable
fun ScheduleSetupScreen(
    onDone: () -> Unit,
    /** After tracking, for a user who knows when some were last done: the list that asks. */
    onEnterLastDone: (vehicleId: Long) -> Unit,
    vm: ScheduleSetupViewModel = hiltViewModel(),
) {
    val editing = vm.editing
    if (editing != null) {
        val defaultRule by vm.defaultRule.collectAsStateWithLifecycle()
        BackHandler(onBack = vm::closeEditor)
        ScheduleItemEditor(
            item = editing,
            isNew = vm.isNew(editing),
            defaultRule = defaultRule,
            unit = vm.vehicle?.unit ?: DistanceUnit.MILES,
            onSave = vm::saveEdit,
            onStopTracking = { vm.stopTracking(editing.key) },
            onClose = vm::closeEditor,
        )
        return
    }

    val items = vm.items ?: return // first frame while the vehicle loads
    val count = items.count { it.tracked && !it.dormant }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(if (vm.known != null) R.string.details_schedule else R.string.setup_title), style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val known = vm.known
                Text(
                    if (known != null) {
                        stringResource(R.string.setup_intro_known, vm.vehicle.displayName(), known.source.name)
                    } else {
                        stringResource(R.string.setup_intro, vm.vehicle.displayName())
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (known != null) {
                        stringResource(R.string.setup_known_note, known.source.note)
                    } else {
                        stringResource(R.string.setup_typical_note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (vm.engineChoices.isNotEmpty()) {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val source = vm.engineChoices.first().source.name
                        Text(stringResource(R.string.setup_engine_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.setup_engine_body, source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        vm.engineChoices.forEach { list ->
                            OutlinedButton(
                                onClick = { vm.chooseEngine(list) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            ) { Text(list.engine?.label ?: list.title) }
                        }
                        TextButton(onClick = vm::declineEngines, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.setup_engine_neither)) }
                    }
                }
            }

            // Only where it changes something: a schedule none of whose jobs differs under severe use has no use for it.
            if (items.any { it.severe != null || it.severeOnly }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.schedule_how_driven))
                    DrivingUseSwitch(severe = vm.severeUse, onPick = vm::chooseSevereUse)
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.setup_selected, count.asNumber(), items.size.asNumber()), Modifier.weight(1f))
                TextButton(onClick = vm::toggleAll) {
                    Text(stringResource(if (count == items.size) R.string.clear_all else R.string.select_all))
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    items.forEach { item ->
                        ItemRow(item, vm.vehicle?.unit ?: DistanceUnit.MILES, onToggle = { vm.toggle(item.key) }, onEdit = { vm.edit(item.key) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    TextButton(onClick = vm::addOwn, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.schedule_add_own))
                    }
                }
            }

            LastDoneChoice(enterNow = vm.enterLastDone, onPick = { vm.enterLastDone = it })

            Button(
                onClick = { vm.save { vehicleId -> if (vm.enterLastDone) onEnterLastDone(vehicleId) else onDone() } },
                enabled = count > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(pluralStringResource(R.plurals.setup_track, count, count.asNumber()))
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.setup_skip))
            }
        }
    }
}

/**
 * Canvas: "Know when these were last done? (optional)". The canvas also offers importing a dealer
 * record and assuming everything was done on schedule; neither is built, since both would put
 * figures in the log that nobody gave.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastDoneChoice(enterNow: Boolean, onPick: (enterNow: Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val next = stringResource(R.string.setup_last_done_next)
    val asIGo = stringResource(R.string.setup_last_done_as_i_go)
    fun text(now: Boolean) = if (now) next else asIGo
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = text(enterNow),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.setup_last_done_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(false, true).forEach { now ->
                DropdownMenuItem(
                    text = { Text(text(now)) },
                    onClick = {
                        onPick(now)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun ItemRow(item: DraftItem, unit: DistanceUnit, onToggle: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f)
                .heightIn(min = 60.dp)
                .toggleable(value = item.tracked, role = Role.Checkbox, onValueChange = { onToggle() }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(checked = item.tracked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 8.dp))
            Column(Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    item.active.text(unit, item.firstAt),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = PlexMono,
                    color = if (item.showsSevere) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.severeOnly) {
                    Text(
                        stringResource(if (item.dormant) R.string.setup_severe_only_off else R.string.setup_severe_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.showsSevere) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Text(
                        stringResource(R.string.setup_severe_tag),
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_item, item.name))
        }
    }
}

/**
 * Canvas: Normal / Severe with what each means inside the button, the chosen side solid ink.
 * [severe] null selects neither, for a schedule whose items differ.
 */
@Composable
internal fun DrivingUseSwitch(severe: Boolean?, onPick: (severe: Boolean) -> Unit) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.secondary,
        activeContentColor = MaterialTheme.colorScheme.onSecondary,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(64.dp)) {
        listOf(
            false to (stringResource(R.string.use_normal) to stringResource(R.string.use_normal_detail)),
            true to (stringResource(R.string.use_severe) to stringResource(R.string.use_severe_detail)),
        ).forEachIndexed { i, (value, text) ->
            SegmentedButton(
                selected = severe == value,
                onClick = { onPick(value) },
                shape = SegmentedButtonDefaults.itemShape(i, 2, baseShape = RoundedCornerShape(14.dp)),
                colors = colors,
                icon = {},
                modifier = Modifier.fillMaxHeight(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text.first, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(text.second, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

/** What a vehicle's odometer counts in. Used where a vehicle is added; changing it later is a conversion, on Vehicle details. */
@Composable
internal fun DistanceUnitSwitch(unit: DistanceUnit, onPick: (DistanceUnit) -> Unit, modifier: Modifier = Modifier) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.secondary,
        activeContentColor = MaterialTheme.colorScheme.onSecondary,
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth().height(48.dp)) {
        DistanceUnit.entries.forEachIndexed { i, u ->
            SegmentedButton(
                selected = unit == u,
                onClick = { onPick(u) },
                shape = SegmentedButtonDefaults.itemShape(i, DistanceUnit.entries.size, baseShape = RoundedCornerShape(14.dp)),
                colors = colors,
                icon = {},
                modifier = Modifier.fillMaxHeight(),
            ) {
                Text(u.titleWord, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun VehicleEntity?.displayName(): String =
    if (this == null) AppText.res.getString(R.string.setup_vehicle) else listOfNotNull("$year", nickname ?: model, engine).joinToString(" ")
