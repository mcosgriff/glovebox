package io.cosgriff.glovebox.ui.service

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
import androidx.compose.ui.text.font.FontFamily
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
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.WorkCategory
import io.cosgriff.glovebox.ui.LogServiceRoute
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** A tracked reminder that's due, offered as a one-tap chip. [optionId] is the tick box it sets. */
data class DueSuggestion(val optionId: String, val label: String)

/** What the form needs from the database, loaded once when the screen opens. */
data class LogServiceData(
    val vehicleName: String,
    val categories: List<WorkCategory>,
    val due: List<DueSuggestion>,
    val readings: List<OdometerReading>,
    val lastOdometer: Int?,
)

@HiltViewModel
class LogServiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    private val service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<LogServiceRoute>().vehicleId

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

    init {
        viewModelScope.launch {
            val vehicle = vehicles.observe(vehicleId).first()
            val items = schedule.allFor(vehicleId)
            val defaultRule = settings.settings.first().defaultRule
            val entries = service.recordsFor(vehicleId).map { r ->
                ServiceEntry(r.record.id, r.record.date, r.record.odometer, r.items.mapNotNull { it.scheduleItemId }.toSet())
            }
            val updates = odometer.readingsFor(vehicleId).map { OdometerReading(it.date, it.odometer) }
            val categories = ServiceCatalog.forVehicle(items.map { ScheduleRef(it.id, it.name, it.templateKey, it.tracked) })
            val optionFor = categories.flatMap { it.options }
                .filter { it.scheduleItemId != null }
                .associateBy { it.scheduleItemId }

            val tracked = items.filter { it.tracked }.mapNotNull { item ->
                val miles = if (item.useSevere) item.severeMiles ?: item.intervalMiles else item.intervalMiles
                val months = if (item.useSevere) item.severeMonths ?: item.intervalMonths else item.intervalMonths
                if (miles == null && months == null) return@mapNotNull null
                val rule = item.ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: defaultRule
                TrackedItem(item.id, item.name, Interval(miles, months), rule)
            }
            val due = VehicleReminders.statuses(tracked, entries, updates, LocalDate.now())
                // Never-logged items count as due, but offering all of them would bury the form on a
                // new vehicle; they can still be ticked in the categories below.
                .filter { it.lastDone != null && it.status.state != DueState.OK }
                .sortedBy { it.status.state != DueState.OVERDUE }
                .mapNotNull { s ->
                    val option = optionFor[s.item.id] ?: return@mapNotNull null
                    val why = if (s.status.state == DueState.OVERDUE) "overdue" else "due soon"
                    DueSuggestion(option.id, "${option.label} · $why")
                }

            categoryId = categories.first().id
            data = LogServiceData(
                vehicleName = vehicle?.let { "${it.year} ${it.nickname ?: it.model}" }.orEmpty(),
                categories = categories,
                due = due,
                readings = ServiceLog.readings(entries, updates),
                lastOdometer = VehicleReminders.currentOdometer(entries, updates),
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
            val options = d.categories.flatMap { it.options }.filter { it.id in selected }
            service.insertWithItems(
                ServiceRecordEntity(
                    vehicleId = vehicleId,
                    date = date,
                    odometer = odo,
                    diy = diy,
                    shopName = shopName.trim().takeIf { !diy && it.isNotEmpty() },
                    costCents = costText.takeIf { it.isNotBlank() }?.let(Money::parseCents),
                    notes = notes.trim().takeIf { it.isNotEmpty() },
                ),
                options.map { ServiceRecordItemEntity(recordId = 0, scheduleItemId = it.scheduleItemId, label = it.label) } +
                    customItems.map { ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = it) },
            )
            onDone()
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogServiceScreen(onDone: () -> Unit, vm: LogServiceViewModel = hiltViewModel()) {
    val data = vm.data ?: return // first frame while the vehicle loads
    var pickingDate by remember { mutableStateOf(false) }
    var addingCustom by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
            Text("Log service", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
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
                        ) { Text(vm.date.format(dateFormat), color = MaterialTheme.colorScheme.onSurface) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Odometer (mi)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = vm.odometerText,
                            onValueChange = { vm.odometerText = it },
                            singleLine = true,
                            isError = vm.showErrors && vm.odometer == null,
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                val odometerNote = when {
                    vm.showErrors && vm.odometer == null -> "Enter the odometer reading in whole miles." to true
                    vm.belowEarlierReading -> "That's lower than an earlier reading — double-check it." to true
                    data.lastOdometer != null -> "Last recorded: ${String.format(Locale.US, "%,d", data.lastOdometer)} mi" to false
                    else -> null
                }
                odometerNote?.let { (text, warn) ->
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                modifier = Modifier.heightIn(min = 48.dp),
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
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
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
            Button(onClick = { vm.save(onDone) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Save service")
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
private fun DatePickerSheet(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
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
