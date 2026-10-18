package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.ui.styledStringResource
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import io.cosgriff.glovebox.domain.WhatItTakes
import androidx.compose.material3.ButtonDefaults
import io.cosgriff.glovebox.domain.NoteTone
import io.cosgriff.glovebox.ui.theme.PlexMono
import java.util.UUID
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.toDash
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.WorkCategory
import io.cosgriff.glovebox.domain.WorkOption
import io.cosgriff.glovebox.ui.LogServiceRoute
import io.cosgriff.glovebox.ui.photo.AttachMenu
import io.cosgriff.glovebox.ui.photo.PhotoTarget
import io.cosgriff.glovebox.ui.photo.PhotoThumb
import io.cosgriff.glovebox.ui.photo.RecordPhotoPicker
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import io.cosgriff.glovebox.domain.ServiceLinks
import javax.inject.Inject

/** A tracked reminder that's due, offered as a one-tap chip. [optionId] is the tick box it sets. */
data class DueSuggestion(val optionId: String, val name: String, val overdue: Boolean)

/** What the form needs from the database, loaded once when the screen opens. */
data class LogServiceData(
    val vehicleName: String,
    val categories: List<WorkCategory>,
    val due: List<DueSuggestion>,
    val tracked: List<TrackedItem>,
    /** The whole log, including the record being edited. */
    val entries: List<ServiceEntry>,
    /** Readings to sanity-check the odometer against; excludes the record being edited. */
    val readings: List<OdometerReading>,
    val lastOdometer: Int?,
    /**
     * Set when the cluster was replaced. [entries], [readings] and [lastOdometer] are in dash terms; what
     * is typed belongs to whichever cluster was in on the form's date, and is saved as typed.
     */
    val swap: ClusterSwap?,
    /** What the vehicle's odometer counts in; the reading is typed and saved in it. */
    val unit: DistanceUnit,
    /** Which schedule items answer to which catalog line, for previewing what an edit moves. */
    val answering: Map<String, Set<Long>>,
    /** What the user wrote on Vehicle details, for the hints beside the lines. */
    val specs: WhatItTakes,
    /** The record being edited; null when logging a new one. */
    val original: ServiceRecordWithItems?,
)

@HiltViewModel
class LogServiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
    private val actions: RecordActions,
    private val photoPicker: RecordPhotoPicker,
) : ViewModel() {
    /** Names this form to the photo picker, which holds its new photos until it is saved or left. */
    private val photoKey = UUID.randomUUID().toString()

    /** Photos attached on this form and not yet saved, as file names. */
    val newPhotos: StateFlow<List<String>> = photoPicker.drafts.map { it[photoKey].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val photoProblem: StateFlow<String?> = photoPicker.problem

    /** Photos the record being edited already has, less any taken off on this form. */
    var keptPhotos by mutableStateOf(emptyList<PhotoEntity>())
        private set
    private var removedPhotoIds = emptySet<Long>()

    val photoRoom: Int get() = RecordPhotos.room(keptPhotos.size + newPhotos.value.size)

    fun takePhoto() = photoPicker.capture(PhotoTarget.Draft(photoKey), photoRoom)
    fun choosePhotos() = photoPicker.pick(PhotoTarget.Draft(photoKey), photoRoom)
    fun removeNewPhoto(fileName: String) = photoPicker.removeDraft(photoKey, fileName)

    /** The row goes when the form is saved; until then Back leaves the record as it was. */
    fun removeKeptPhoto(photo: PhotoEntity) {
        keptPhotos = keptPhotos - photo
        removedPhotoIds = removedPhotoIds + photo.id
    }

    /** Leaving without saving: the photos attached here were never the record's. */
    override fun onCleared() {
        photoPicker.discardDraft(photoKey)
    }

    private val route = savedStateHandle.toRoute<LogServiceRoute>()
    private val vehicleId = route.vehicleId
    val editing: Boolean = route.recordId != 0L

    var data by mutableStateOf<LogServiceData?>(null)
        private set

    var date by mutableStateOf<LocalDate>(LocalDate.now())
    var odometerText by mutableStateOf("")
    var categoryId by mutableStateOf("")
    var selected by mutableStateOf(emptySet<String>())
        private set
    var customItems by mutableStateOf(emptyList<String>())
        private set
    var diy by mutableStateOf(true)
    var shopName by mutableStateOf("")
    var costText by mutableStateOf("")
    var notes by mutableStateOf("")
    var showErrors by mutableStateOf(false)
        private set
    private var saving = false

    val odometer: Int? get() = OdometerInput.parse(odometerText.typedNumber())
    val costValid: Boolean get() = costText.isBlank() || Money.parseCents(costText.typedNumber()) != null
    val itemCount: Int get() = selected.size + customItems.size

    /** True when the entered odometer is lower than a reading on or before [date]. Worth a warning, not a block. */
    val belowEarlierReading: Boolean
        get() {
            val odo = odometer ?: return false
            return ServiceLog.isBelowPreviousReading(data?.swap.toDash(odo, date), date, data?.readings.orEmpty())
        }

    private val selectedOptions: List<WorkOption>
        get() = data?.categories.orEmpty().flatMap { it.options }.filter { it.id in selected }

    /** Edit only: how reminders move if the form is saved as it stands. */
    val reminderChanges: List<ReminderChange>
        get() {
            val d = data ?: return emptyList()
            val original = d.original ?: return emptyList()
            val edited = ServiceEntry(
                id = original.record.id,
                date = date,
                odometer = d.swap.toDash(odometer ?: original.record.odometer, date),
                // As it would be saved: against one item, and counting for every item that answers to the line.
                itemIds = ServiceLinks.itemIds(selectedOptions.map { it.scheduleItemId to it.id.takeIf { id -> id in d.answering } }, d.answering),
            )
            return LogEdits.reminderChanges(d.tracked, d.entries, d.entries.filter { it.id != edited.id } + edited)
        }

    init {
        viewModelScope.launch {
            val vehicle = vehicles.observe(vehicleId).first()
            val items = schedule.allFor(vehicleId)
            val prefs = settings.settings.first()
            val defaultRule = prefs.defaultRule
            val records = service.recordsFor(vehicleId)
            val original = records.firstOrNull { editing && it.record.id == route.recordId }
            val swap = vehicle?.clusterSwap()
            val entries = records.map { it.toEntry(swap, items.answering()) }
            val others = entries.filter { it.id != original?.record?.id }
            val updates = odometer.readingsFor(vehicleId).toReadings(swap)
            val categories = ServiceCatalog.forVehicle(items.map { it.toRef() })
            val options = categories.flatMap { it.options }
            // The line to tick for a reminder: the catalog line that is its own, or failing that the one saved
            // against it, which wins. Its own only: a change of oil also resets the check of it, but "Log it" on
            // the check must tick the check (ServiceLinks.own, not .answering).
            val own = ServiceLinks.own(items.map { it.toRef() })
            val optionFor: Map<Long?, WorkOption> = options.flatMap { o -> own[o.id].orEmpty().map { id -> id to o } }.toMap() +
                options.filter { it.scheduleItemId != null }.associateBy { it.scheduleItemId }
            val tracked = items.filter { it.tracked }.mapNotNull { it.toTracked(defaultRule, swap) }

            val due = if (editing) {
                emptyList()
            } else {
                VehicleReminders.statuses(tracked, entries, updates, LocalDate.now(), prefs.leadFor(vehicle?.unit ?: DistanceUnit.MILES))
                    // Never-logged items count as due, but offering all of them would bury the form on a
                    // new vehicle; they can still be ticked in the categories below.
                    .filter { it.lastDone != null && it.status.state != DueState.OK }
                    .sortedBy { it.status.state != DueState.OVERDUE }
                    .mapNotNull { s ->
                        val option = optionFor[s.item.id] ?: return@mapNotNull null
                        DueSuggestion(option.id, option.label, overdue = s.status.state == DueState.OVERDUE)
                    }
            }

            if (original != null) {
                val r = original.record
                date = r.date
                odometerText = r.odometer.asNumber()
                diy = r.diy
                shopName = r.shopName.orEmpty()
                costText = r.costCents?.let(::editableCost).orEmpty()
                notes = r.notes.orEmpty()
                keptPhotos = original.photos
                val ticked = mutableSetOf<String>()
                val custom = mutableListOf<String>()
                original.items.forEach { item ->
                    val option = options.firstOrNull { it.id == item.catalogId }
                        ?: item.scheduleItemId?.let(optionFor::get)
                    if (option != null) ticked += option.id else custom += item.label
                }
                selected = ticked
                customItems = custom
            }

            if (!editing) optionFor[route.scheduleItemId]?.let { selected = setOf(it.id) }

            categoryId = categories.firstOrNull { c -> c.options.any { it.id in selected } }?.id ?: categories.first().id
            data = LogServiceData(
                vehicleName = vehicle?.let { "${it.year} ${it.nickname ?: it.model}" }.orEmpty(),
                categories = categories,
                due = due,
                tracked = tracked,
                entries = entries,
                readings = ServiceLog.readings(others, updates),
                lastOdometer = VehicleReminders.currentOdometer(others, updates),
                swap = swap,
                unit = vehicle?.unit ?: DistanceUnit.MILES,
                answering = items.answering(),
                specs = vehicle?.whatItTakes() ?: WhatItTakes(),
                original = original,
            )
        }
    }

    fun toggle(optionId: String) {
        selected = if (optionId in selected) selected - optionId else selected + optionId
    }

    fun addCustom(label: String) {
        val trimmed = label.trim()
        if (trimmed.isNotEmpty() && trimmed !in customItems) customItems = customItems + trimmed
    }

    fun removeCustom(label: String) {
        customItems = customItems - label
    }

    fun save(onDone: () -> Unit) {
        val d = data ?: return
        val odo = odometer
        if (odo == null || itemCount == 0 || !costValid || date.isAfter(LocalDate.now())) {
            showErrors = true
            return
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            val catalogIds = d.categories.filter { it.id != ServiceCatalog.YOUR_ITEMS }.flatMap { it.options }.map { it.id }.toSet()
            val record = ServiceRecordEntity(
                id = d.original?.record?.id ?: 0,
                vehicleId = vehicleId,
                date = date,
                odometer = odo,
                diy = diy,
                shopName = shopName.trim().takeIf { !diy && it.isNotEmpty() },
                costCents = costText.takeIf { it.isNotBlank() }?.let { Money.parseCents(it.typedNumber()) },
                notes = notes.trim().takeIf { it.isNotEmpty() },
                // Correcting a bare "last done" here doesn't turn it into a service logged at the time.
                enteredAsLastDone = d.original?.record?.enteredAsLastDone ?: false,
                // Nor does correcting an imported record make it one that was logged here.
                imported = d.original?.record?.imported ?: false,
            )
            val items = selectedOptions.map {
                ServiceRecordItemEntity(
                    recordId = 0,
                    scheduleItemId = it.scheduleItemId,
                    label = it.label,
                    catalogId = it.id.takeIf { id -> id in catalogIds },
                )
            } + customItems.map { ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = it) }
            val added = photoPicker.takeDraft(photoKey)
            if (d.original != null) actions.replace(d.original, record, items, added, removedPhotoIds) else actions.add(record, items, added)
            onDone()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogServiceScreen(
    /** Closed, or an edit saved: back to where it was opened from. */
    onDone: () -> Unit,
    /** A new service saved. */
    onAdded: () -> Unit,
    /** "Delete this record" on the edit form: the changes are dropped and the record's screen asks to confirm. */
    onDeleteInstead: () -> Unit,
    vm: LogServiceViewModel = hiltViewModel(),
) {
    val data = vm.data ?: return // first frame while the vehicle loads
    var pickingDate by remember { mutableStateOf(false) }
    var addingCustom by remember { mutableStateOf(false) }
    val notesLabel = stringResource(R.string.log_notes)

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close)) }
            Text(stringResource(if (vm.editing) R.string.log_edit_title else R.string.log_service), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                Text(
                    data.vehicleName,
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(stringResource(R.string.log_date))
                        // Drawn as the odometer field's twin; it opens the date picker instead of the keyboard.
                        Surface(
                            onClick = { pickingDate = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { role = Role.Button },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                                Text(vm.date.format(longDate), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(stringResource(R.string.odometer_in, data.unit.symbolWord))
                        OutlinedTextField(
                            value = vm.odometerText,
                            onValueChange = { vm.odometerText = it },
                            singleLine = true,
                            isError = vm.showErrors && vm.odometer == null,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        )
                        // Only on the user's tap: a reading filled in by itself gets saved untouched.
                        val current = data.lastOdometer?.let { data.swap.asReadOn(it, vm.date) }
                        if (current != null && !vm.editing && vm.odometerText.isBlank()) {
                            TextButton(
                                onClick = { vm.odometerText = current.asNumber() },
                                modifier = Modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) { Text(stringResource(R.string.log_use_current)) }
                        }
                    }
                }
                LogEdits.odometerNote(
                    entered = vm.odometer,
                    original = data.original?.record?.odometer,
                    date = vm.date,
                    // Set beside what is typed, so in terms of the cluster that was in on the form's date.
                    readings = data.readings.map { it.copy(odometer = data.swap.asReadOn(it.odometer, vm.date)) },
                    lastOdometer = data.lastOdometer?.let { data.swap.asReadOn(it, vm.date) },
                    showErrors = vm.showErrors,
                )?.let { note ->
                    Text(
                        note.text(data.unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (note.tone) {
                            NoteTone.WARNING -> MaterialTheme.colorScheme.error
                            NoteTone.CHANGED -> MaterialTheme.colorScheme.onPrimaryContainer
                            NoteTone.PLAIN -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (data.due.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(stringResource(R.string.log_due_section))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.due.forEach { s ->
                            val on = s.optionId in vm.selected
                            FilterChip(
                                selected = on,
                                onClick = {
                                    vm.toggle(s.optionId)
                                    // Open the category it was ticked in, so the tick is in sight and not only a count on a button.
                                    if (!on) data.categories.firstOrNull { c -> c.options.any { it.id == s.optionId } }?.let { vm.categoryId = it.id }
                                },
                                // Canvas: "+" offers it, a tick and the soft tint show it is on the list.
                                label = {
                                    val due = stringResource(if (s.overdue) R.string.log_due_overdue else R.string.log_due_soon, s.name)
                                    Text(stringResource(if (on) R.string.log_chip_on else R.string.log_chip_off, due), fontWeight = FontWeight.SemiBold)
                                },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = on,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.5.dp,
                                    selectedBorderWidth = 1.5.dp,
                                ),
                                modifier = Modifier.heightIn(min = 40.dp),
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.log_what_section))
                data.categories.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { category ->
                            CategoryButton(
                                label = category.label,
                                count = category.options.count { it.id in vm.selected },
                                active = category.id == vm.categoryId,
                                onClick = { vm.categoryId = category.id },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Column(Modifier.weight(1f)) {}
                    }
                }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        val intervals = remember(data) { data.tracked.associate { it.id to it.interval } }
                        data.categories.first { it.id == vm.categoryId }.options.forEach { option ->
                            CheckRow(
                                label = option.label,
                                hint = ServiceCatalog.hint(option.id, option.scheduleItemId?.let(intervals::get), data.specs)?.text(data.unit),
                                checked = option.id in vm.selected,
                                onToggle = { vm.toggle(option.id) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        vm.customItems.forEach { label ->
                            CheckRow(label = label, hint = stringResource(R.string.log_custom), checked = true, onToggle = { vm.removeCustom(label) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        TextButton(onClick = { addingCustom = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.log_add_custom_button))
                        }
                    }
                }
            }

            Surface(
                color = if (vm.showErrors && vm.itemCount == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val labels = data.categories.flatMap { it.options }.filter { it.id in vm.selected }.map { it.label } + vm.customItems
                val count = pluralStringResource(R.plurals.log_items, labels.size, labels.size.asNumber())
                val listed = if (labels.isEmpty()) stringResource(R.string.log_pick_above) else labels.joinToString(", ")
                Text(
                    styledStringResource(R.string.log_items_summary, count, SpanStyle(fontWeight = FontWeight.Bold), listed),
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (vm.editing) {
                val changes = vm.reminderChanges
                Surface(
                    color = if (changes.isEmpty()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.log_changes_section),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (changes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (changes.isEmpty()) {
                            Text(stringResource(R.string.log_no_changes), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            changes.forEach {
                                Text(it.text(data.unit), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.log_who_section))
                // Canvas: the chosen side is solid ink.
                val colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondary,
                    activeContentColor = MaterialTheme.colorScheme.onSecondary,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = vm.diy,
                        onClick = { vm.diy = true },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        colors = colors,
                        icon = {}, // canvas: the solid fill is the mark
                    ) { Text(stringResource(R.string.log_diy)) }
                    SegmentedButton(
                        selected = !vm.diy,
                        onClick = { vm.diy = false },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        colors = colors,
                        icon = {},
                    ) { Text(stringResource(R.string.log_a_shop)) }
                }
                if (!vm.diy) {
                    OutlinedTextField(
                        value = vm.shopName,
                        onValueChange = { vm.shopName = it },
                        label = { Text(stringResource(R.string.log_shop_name)) },
                        placeholder = { Text(stringResource(R.string.log_shop_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            CostAndPhotos(vm)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.log_notes_label))
                OutlinedTextField(
                    value = vm.notes,
                    onValueChange = { vm.notes = it },
                    placeholder = { Text(stringResource(R.string.log_optional)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = notesLabel },
                )
            }

            // The fields with problems may be scrolled out of view, so repeat what's missing here.
            val missing = listOfNotNull(
                // The picker refuses future days, so this is only ever a form left open across a clock change.
                stringResource(R.string.log_missing_date).takeIf { vm.date.isAfter(LocalDate.now()) },
                stringResource(R.string.log_missing_odometer).takeIf { vm.odometer == null },
                stringResource(R.string.log_missing_item).takeIf { vm.itemCount == 0 },
                stringResource(R.string.log_missing_cost).takeIf { !vm.costValid },
            )
            if (vm.showErrors && missing.isNotEmpty()) {
                Text(
                    stringResource(R.string.log_to_save, andList(missing)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { vm.save(if (vm.editing) onDone else onAdded) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(if (vm.editing) R.string.log_save_changes else R.string.log_save))
            }
            if (vm.editing) {
                TextButton(
                    onClick = onDeleteInstead,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.log_delete)) }
            }
        }
    }

    if (pickingDate) {
        DatePickerSheet(
            initial = vm.date,
            onPick = {
                vm.date = it
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
    if (addingCustom) {
        CustomItemDialog(
            onAdd = {
                vm.addCustom(it)
                addingCustom = false
            },
            onDismiss = { addingCustom = false },
        )
    }
}

/** The small label above a field, as the canvas draws them. */
@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Canvas: fields are white (or the dark surface) boxes on the page colour. */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
)

/** Design: "Total cost" beside "Receipt / photo" on 5 · Log service, with the attached photos under them. */
@Composable
private fun CostAndPhotos(vm: LogServiceViewModel) {
    val scheme = MaterialTheme.colorScheme
    val newPhotos by vm.newPhotos.collectAsStateWithLifecycle()
    val problem by vm.photoProblem.collectAsStateWithLifecycle()
    val count = vm.keptPhotos.size + newPhotos.size
    val costLabel = stringResource(R.string.log_cost_optional)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.log_total_cost))
                OutlinedTextField(
                    value = vm.costText,
                    onValueChange = { vm.costText = it },
                    placeholder = { Text(stringResource(R.string.log_cost_placeholder), fontFamily = PlexMono) },
                    prefix = { Text(stringResource(R.string.log_cost_prefix), fontFamily = PlexMono) },
                    singleLine = true,
                    isError = !vm.costValid,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = costLabel },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.log_receipt))
                AttachMenu(onTake = vm::takePhoto, onChoose = vm::choosePhotos, modifier = Modifier.fillMaxWidth()) { open ->
                    val enabled = vm.photoRoom > 0
                    val ink = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.38f)
                    val dash = if (enabled) scheme.outline else scheme.outlineVariant
                    Row(
                        Modifier.fillMaxWidth().height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .drawBehind {
                                // Twice the width, because the outer half is clipped away.
                                drawRoundRect(
                                    color = dash,
                                    cornerRadius = CornerRadius(12.dp.toPx()),
                                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                                )
                            }
                            .clickable(enabled = enabled, role = Role.Button, onClick = open),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(18.dp), tint = ink)
                        Text(
                            photoCount(count)?.let { stringResource(R.string.log_attach_count, it) } ?: stringResource(R.string.log_attach),
                            style = MaterialTheme.typography.labelLarge,
                            color = ink,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (!vm.costValid) {
            Text(stringResource(R.string.log_cost_error), style = MaterialTheme.typography.bodySmall, color = scheme.error)
        }
        if (count > 0) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.keptPhotos.forEachIndexed { i, photo ->
                    PhotoThumb(photo.fileName, stringResource(R.string.photo_number, (i + 1).asNumber()), Modifier.size(96.dp), onRemove = { vm.removeKeptPhoto(photo) })
                }
                newPhotos.forEachIndexed { i, name ->
                    PhotoThumb(name, stringResource(R.string.photo_number, (vm.keptPhotos.size + i + 1).asNumber()), Modifier.size(96.dp), onRemove = { vm.removeNewPhoto(name) })
                }
            }
        }
        Text(
            problem ?: if (vm.photoRoom == 0) {
                stringResource(R.string.log_photos_full, RecordPhotos.MAX_PER_RECORD.asNumber())
            } else {
                stringResource(R.string.log_photos_note)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (problem != null) scheme.error else scheme.onSurfaceVariant,
        )
    }
}

/**
 * One category of "What was done?": a tab that opens its list below, never a tick of its own. The open
 * one is outlined in ink with a caret, not filled, so it doesn't read as chosen; [count] is how many of
 * its lines are ticked, said as a tick on the soft tint like the due chips.
 */
@Composable
private fun CategoryButton(label: String, count: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val state = listOfNotNull(
        stringResource(R.string.log_tab_open).takeIf { active },
        pluralStringResource(R.plurals.log_tab_ticked, count, count.asNumber()).takeIf { count > 0 },
    ).joinToString(", ")
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).semantics { stateDescription = state },
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = BorderStroke(if (active) 2.dp else 1.5.dp, if (active) scheme.onSurface else scheme.outlineVariant),
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.Bold else null)
            if (count > 0) {
                Surface(color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer, shape = CircleShape) {
                    Text(
                        stringResource(R.string.log_tab_count, count.asNumber()),
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (active) Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CheckRow(label: String, hint: String?, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 8.dp))
        Text(label, Modifier.weight(1f))
        if (hint != null) {
            // The user's own note may be long; the line's name keeps its room.
            Text(
                hint,
                Modifier.padding(end = 8.dp).widthIn(max = 150.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerSheet(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The picker works in UTC midnights; a service can't be logged for a future day.
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state = state) }
}

@Composable
private fun CustomItemDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_add_custom)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.log_what_was_done)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = { TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
