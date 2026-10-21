package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.ui.components.DateButton
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.components.FilledCard
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.service.asNumber
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.service.every
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.LastDoneEntries
import io.cosgriff.glovebox.domain.LastDoneRow
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.NoteTone
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.toDash
import io.cosgriff.glovebox.ui.LastDoneRoute
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.service.toTracked
import io.cosgriff.glovebox.ui.theme.PlexMono
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import io.cosgriff.glovebox.ui.service.answering
import javax.inject.Inject

data class LastDoneState(
    val vehicleName: String,
    val rows: List<LastDoneRow>,
    /** In dash terms; null for a vehicle with no mileage yet. */
    val odometer: Int?,
    val swap: ClusterSwap?,
    val unit: DistanceUnit,
) {
    val pending: Int get() = rows.count { it.lastDone == null }
}

@HiltViewModel
class LastDoneViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    schedule: ScheduleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    settings: SettingsRepository,
    private val actions: RecordActions,
) : ViewModel() {
    val vehicleId = savedStateHandle.route<LastDoneRoute>().vehicleId

    private var entities: Map<Long, ScheduleItemEntity> = emptyMap()

    /** Null while loading, and if the vehicle no longer exists. */
    val state: StateFlow<LastDoneState?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeTracked(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
        settings.settings,
    ) { vehicle, items, records, readings, s ->
        if (vehicle == null) return@combine null
        entities = items.associateBy { it.id }
        val swap = vehicle.clusterSwap()
        val entries = records.map { it.toEntry(swap, items.answering()) }
        LastDoneState(
            vehicleName = "${vehicle.year} ${vehicle.nickname ?: vehicle.model}",
            rows = LastDoneEntries.rows(
                items.mapNotNull { it.toTracked(s.defaultRule, swap) },
                entries,
                records.filter { it.record.enteredAsLastDone }.map { it.record.id }.toSet(),
                swap,
            ),
            odometer = VehicleReminders.currentOdometer(entries, readings.toReadings(swap)),
            swap = swap,
            unit = vehicle.unit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(row: LastDoneRow, date: LocalDate, odometer: Int) = viewModelScope.launch {
        entities[row.item.id]?.let { actions.addLastDone(vehicleId, it, date, odometer) }
    }
}

/**
 * "When were these last done?": a date and mileage for any reminder the user knows, so a vehicle
 * that already has history doesn't start with everything "Not logged yet". Reached from schedule
 * setup, the Maintenance schedule screen and vehicle home.
 */
@Composable
fun LastDoneScreen(onDone: () -> Unit, onOpenRecord: (recordId: Long) -> Unit, vm: LastDoneViewModel = hiltViewModel()) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    // The item's id, so the sheet survives the list reordering under it.
    var entering by rememberSaveable { mutableStateOf<Long?>(null) }

    state.rows.firstOrNull { it.item.id == entering }?.let { row ->
        LastDoneSheet(
            itemName = row.item.name,
            currentOdometer = state.odometer,
            swap = state.swap,
            unit = state.unit,
            onSave = { date, odometer ->
                vm.save(row, date, odometer)
                entering = null
            },
            onDismiss = { entering = null },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    stringResource(R.string.last_done_title),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(state.vehicleName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                stringResource(R.string.last_done_intro),
                color = scheme.onSurfaceVariant,
            )

            if (state.rows.isEmpty()) {
                Text(stringResource(R.string.home_none_tracked), color = scheme.onSurfaceVariant)
            } else {
                SectionLabel(
                    when (val n = state.pending) {
                        0 -> stringResource(R.string.last_done_all, state.rows.size.asNumber())
                        else -> stringResource(R.string.last_done_pending, n.asNumber(), state.rows.size.asNumber())
                    },
                )
                FilledCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        state.rows.forEachIndexed { i, row ->
                            if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                            Row(row, state.unit, onEnter = { entering = row.item.id }, onOpenRecord = onOpenRecord)
                        }
                    }
                }
            }

            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.done)) }
        }
    }
}

@Composable
private fun Row(row: LastDoneRow, unit: DistanceUnit, onEnter: () -> Unit, onOpenRecord: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val last = row.lastDone
    val recordId = row.recordId
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (last == null) R.string.last_done_enter else R.string.readings_open_record),
                onClick = { if (last == null || recordId == null) onEnter() else onOpenRecord(recordId) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.item.name, style = MaterialTheme.typography.titleSmall)
            Text(
                row.item.interval.every(unit),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        if (last == null) {
            Text(stringResource(R.string.add), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
        } else {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${last.date.format(longDate)} · ${last.odometer.asDistance(unit)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                )
                // An entered one is never passed off as a service logged at the time.
                Text(
                    stringResource(if (row.entered) R.string.details_entered_by_you else R.string.from_your_log),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
    }
}

/**
 * A date and a mileage for one reminder. [currentOdometer] is in dash terms; what is typed is the
 * reading on the chosen date, and is saved as typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastDoneSheet(
    itemName: String,
    currentOdometer: Int?,
    swap: ClusterSwap?,
    unit: DistanceUnit,
    onSave: (date: LocalDate, odometer: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var day by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var odometerText by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(day)
    val odometer = OdometerInput.parse(odometerText.typedNumber())
    val note = LastDoneEntries.odometerNote(odometer?.let { swap.toDash(it, date) }, currentOdometer, showErrors)


    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(itemName, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.last_done_sheet_intro, stringResource(R.string.last_done_tag)),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            DateButton(R.string.date_button, date) { day = it.toEpochDay() }

            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it.filter(Char::isDigit).trimStart('0').take(MileageUpdate.MAX_DIGITS) },
                label = { Text(stringResource(R.string.last_done_odometer, unit.symbolWord)) },
                supportingText = note?.let { { Text(it.text(unit)) } },
                isError = note?.tone == NoteTone.WARNING,
                singleLine = true,
                textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        if (odometer != null && LastDoneEntries.problems(date, odometer, LocalDate.now()).isEmpty()) onSave(date, odometer) else showErrors = true
                    },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
