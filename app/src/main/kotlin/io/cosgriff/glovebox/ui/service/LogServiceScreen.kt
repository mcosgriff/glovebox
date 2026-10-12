package io.cosgriff.glovebox.ui.service

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
import io.cosgriff.glovebox.domain.OdometerInput
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
import java.util.Locale
import javax.inject.Inject

/** A tracked reminder that's due, offered as a one-tap chip. [optionId] is the tick box it sets. */
data class DueSuggestion(val optionId: String, val label: String)

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

    val odometer: Int? get() = OdometerInput.parse(odometerText)
    val costValid: Boolean get() = costText.isBlank() || Money.parseCents(costText) != null
    val itemCount: Int get() = selected.size + customItems.size

    /** True when the entered odometer is lower than a reading on or before [date]. Worth a warning, not a block. */
    val belowEarlierReading: Boolean
        get() {
            val odo = odometer ?: return false
            return ServiceLog.isBelowPreviousReading(odo, date, data?.readings.orEmpty())
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
                odometer = odometer ?: original.record.odometer,
                itemIds = selectedOptions.mapNotNull { it.scheduleItemId }.toSet(),
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
            val entries = records.map { it.toEntry() }
            val others = entries.filter { it.id != original?.record?.id }
            val updates = odometer.readingsFor(vehicleId).map { OdometerReading(it.date, it.odometer) }
            val categories = ServiceCatalog.forVehicle(items.map { ScheduleRef(it.id, it.name, it.templateKey, it.tracked) })
            val options = categories.flatMap { it.options }
            val optionFor = options.filter { it.scheduleItemId != null }.associateBy { it.scheduleItemId }
            val tracked = items.filter { it.tracked }.mapNotNull { it.toTracked(defaultRule) }

            val due = if (editing) {
                emptyList()
            } else {
                VehicleReminders.statuses(tracked, entries, updates, LocalDate.now(), prefs.lead)
                    // Never-logged items count as due, but offering all of them would bury the form on a
                    // new vehicle; they can still be ticked in the categories below.
                    .filter { it.lastDone != null && it.status.state != DueState.OK }
                    .sortedBy { it.status.state != DueState.OVERDUE }
                    .mapNotNull { s ->
                        val option = optionFor[s.item.id] ?: return@mapNotNull null
                        val why = if (s.status.state == DueState.OVERDUE) "overdue" else "due soon"
                        DueSuggestion(option.id, "${option.label} · $why")
                    }
            }

            if (original != null) {
                val r = original.record
                date = r.date
                odometerText = String.format(Locale.US, "%,d", r.odometer)
                diy = r.diy
                shopName = r.shopName.orEmpty()
                costText = r.costCents?.let { String.format(Locale.US, "%d.%02d", it / 100, it % 100) }.orEmpty()
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
                costCents = costText.takeIf { it.isNotBlank() }?.let(Money::parseCents),
                notes = notes.trim().takeIf { it.isNotEmpty() },
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

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
            Text(if (vm.editing) "Edit service" else "Log service", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
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
                        Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            onClick = { pickingDate = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(vm.date.format(longDate), color = MaterialTheme.colorScheme.onSurface) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Odometer (mi)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = vm.odometerText,
                            onValueChange = { vm.odometerText = it },
                            singleLine = true,
                            isError = vm.showErrors && vm.odometer == null,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                LogEdits.odometerNote(
                    entered = vm.odometer,
                    original = data.original?.record?.odometer,
                    date = vm.date,
                    readings = data.readings,
                    lastOdometer = data.lastOdometer,
                    showErrors = vm.showErrors,
                )?.let { note ->
                    Text(
                        note.text,
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
                    SectionLabel("DUE NOW · ONE TAP")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.due.forEach { s ->
                            FilterChip(
                                selected = s.optionId in vm.selected,
                                onClick = { vm.toggle(s.optionId) },
                                label = { Text(s.label) },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("WHAT WAS DONE?")
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        data.categories.first { it.id == vm.categoryId }.options.forEach { option ->
                            CheckRow(
                                label = option.label,
                                hint = if (option.scheduleItemId != null) "Resets reminder" else null,
                                checked = option.id in vm.selected,
                                onToggle = { vm.toggle(option.id) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        vm.customItems.forEach { label ->
                            CheckRow(label = label, hint = "Custom", checked = true, onToggle = { vm.removeCustom(label) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        TextButton(onClick = { addingCustom = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("+ Add custom item")
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
                Text(
                    if (labels.isEmpty()) {
                        "0 items · Pick what was done above"
                    } else {
                        "${labels.size} ${if (labels.size == 1) "item" else "items"} · ${labels.joinToString(", ")}"
                    },
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
                            "WHAT THIS CHANGES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (changes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (changes.isEmpty()) {
                            Text("No changes to reminders.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            changes.forEach {
                                Text(it.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("WHO DID IT?")
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
                    ) { Text("I did (DIY)") }
                    SegmentedButton(
                        selected = !vm.diy,
                        onClick = { vm.diy = false },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        colors = colors,
                    ) { Text("A shop") }
                }
                if (!vm.diy) {
                    OutlinedTextField(
                        value = vm.shopName,
                        onValueChange = { vm.shopName = it },
                        label = { Text("Shop name") },
                        placeholder = { Text("e.g. dealer or local shop") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OutlinedTextField(
                value = vm.costText,
                onValueChange = { vm.costText = it },
                label = { Text("Total cost (optional)") },
                prefix = { Text("$") },
                singleLine = true,
                isError = !vm.costValid,
                supportingText = if (!vm.costValid) {
                    { Text("Enter an amount like 45.99") }
                } else {
                    null
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = vm.notes,
                onValueChange = { vm.notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Part numbers, oil brand, torque specs…") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            PhotosField(vm)

            // The fields with problems may be scrolled out of view, so repeat what's missing here.
            val missing = listOfNotNull(
                "the odometer reading".takeIf { vm.odometer == null },
                "at least one item".takeIf { vm.itemCount == 0 },
                "a valid cost".takeIf { !vm.costValid },
            )
            if (vm.showErrors && missing.isNotEmpty()) {
                Text(
                    "To save, add ${missing.joinToString(" and ")}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = { vm.save(if (vm.editing) onDone else onAdded) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(if (vm.editing) "Save changes" else "Save service")
            }
            if (vm.editing) {
                TextButton(
                    onClick = onDeleteInstead,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Delete this record") }
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

/** Design: the "Receipt / photo" row on 5 · Log service. */
@Composable
private fun PhotosField(vm: LogServiceViewModel) {
    val scheme = MaterialTheme.colorScheme
    val newPhotos by vm.newPhotos.collectAsStateWithLifecycle()
    val problem by vm.photoProblem.collectAsStateWithLifecycle()
    val count = vm.keptPhotos.size + newPhotos.size
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Receipt / photo" + (RecordPhotos.countLabel(count)?.let { " · $it" } ?: ""),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            AttachMenu(onTake = vm::takePhoto, onChoose = vm::choosePhotos) { open ->
                OutlinedButton(
                    onClick = open,
                    enabled = vm.photoRoom > 0,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(18.dp))
                    Text("Attach")
                }
            }
        }
        if (count > 0) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.keptPhotos.forEachIndexed { i, photo ->
                    PhotoThumb(photo.fileName, "photo ${i + 1}", Modifier.size(96.dp), onRemove = { vm.removeKeptPhoto(photo) })
                }
                newPhotos.forEachIndexed { i, name ->
                    PhotoThumb(name, "photo ${vm.keptPhotos.size + i + 1}", Modifier.size(96.dp), onRemove = { vm.removeNewPhoto(name) })
                }
            }
        }
        Text(
            problem ?: if (vm.photoRoom == 0) {
                "That's the most a record holds (${RecordPhotos.MAX_PER_RECORD})."
            } else {
                "Stored encrypted with the record. The copy in your gallery, if there is one, isn't touched."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (problem != null) scheme.error else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryButton(label: String, count: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        selected = active,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        contentColor = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.5.dp, if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            if (count > 0) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(22.dp)) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            Text(
                hint,
                Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@Composable
private fun CustomItemDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom item") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What was done?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = { TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
