package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.export.ExportSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.DeleteVehicleRoute
import io.cosgriff.glovebox.ui.service.toEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What deleting the vehicle takes with it, counted when the screen opens. */
data class DeleteVehicleData(
    val vehicle: VehicleEntity,
    val records: Int,
    val photos: Int,
    val scheduleItems: Int,
    val torqueSpecs: Int,
    val recalls: Int,
    val mileageReadings: Int,
    /** For "Archive instead": the highest reading so far. */
    val lastOdometer: Int?,
) {
    /** What the user types to confirm. */
    val name: String get() = vehicle.nickname ?: vehicle.model
}

@HiltViewModel
class DeleteVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicles: VehicleDao,
    private val service: ServiceDao,
    private val schedule: ScheduleDao,
    private val torque: TorqueDao,
    private val recalls: RecallDao,
    private val odometer: OdometerDao,
    private val wipe: DataWipe,
    private val archiver: VehicleArchiver,
) : ViewModel() {
    private val vehicleId = savedStateHandle.toRoute<DeleteVehicleRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    var data by mutableStateOf<DeleteVehicleData?>(null)
        private set
    var deleting by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val v = vehicles.byId(vehicleId) ?: return@launch
            val records = service.recordsFor(vehicleId)
            val readings = odometer.readingsFor(vehicleId)
            data = DeleteVehicleData(
                vehicle = v,
                records = records.size,
                photos = records.sumOf { it.photos.size },
                scheduleItems = schedule.allFor(vehicleId).size,
                torqueSpecs = torque.observe(vehicleId).first().size,
                recalls = recalls.recallsFor(vehicleId).size,
                mileageReadings = readings.size,
                lastOdometer = VehicleReminders.currentOdometer(
                    records.map { it.toEntry() },
                    readings.map { OdometerReading(it.date, it.odometer) },
                ),
            )
        }
    }

    /** No Undo: the rows cascade away and the photo files are removed. */
    fun delete(onDeleted: (vehicleName: String) -> Unit) {
        val d = data ?: return
        if (deleting) return
        deleting = true
        viewModelScope.launch {
            wipe.deleteVehicle(d.vehicle)
            onDeleted(d.name)
        }
    }

    fun archiveInstead(sale: Sale, onArchived: (vehicleName: String, undo: () -> Unit) -> Unit) = viewModelScope.launch {
        val d = data ?: return@launch
        val receipt = archiver.archive(vehicleId, sale, d.lastOdometer)
        onArchived(d.name) { archiver.undo(receipt) }
    }
}

/** Design: 4g · Delete vehicle + its data. */
@Composable
fun DeleteVehicleScreen(
    onCancel: () -> Unit,
    onDeleted: (vehicleName: String) -> Unit,
    onArchived: (vehicleName: String, undo: () -> Unit) -> Unit,
    vm: DeleteVehicleViewModel = hiltViewModel(),
) {
    val d = vm.data ?: return // first frame while loading
    val scheme = MaterialTheme.colorScheme
    var typed by rememberSaveable { mutableStateOf("") }
    var archiving by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    val confirmed = TypedConfirmation.matches(typed, d.name)

    if (archiving) {
        MarkAsSoldSheet(
            vehicleName = d.name,
            lastOdometer = d.lastOdometer,
            onArchive = { sale ->
                archiving = false
                vm.archiveInstead(sale, onArchived)
            },
            onDismiss = { archiving = false },
            onExport = { exporting = true },
        )
    }
    if (exporting) ExportSheet(vehicleId = d.vehicle.id, onDismiss = { exporting = false })

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onCancel, modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Cancel")
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(color = scheme.errorContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(28.dp), tint = scheme.error)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Delete ${d.vehicle.year} ${d.name} and all its data?",
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("This permanently removes from this phone:", color = scheme.onSurfaceVariant)
            }

            Surface(
                Modifier.fillMaxWidth(),
                color = scheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    listOf(
                        "Service records" to "${d.records}",
                        "Receipt & parts photos" to "${d.photos}",
                        "Schedule items & reminders" to "${d.scheduleItems}",
                        "Torque & fastener specs" to "${d.torqueSpecs}",
                        "Recalls and their status" to "${d.recalls}",
                        "Mileage readings" to "${d.mileageReadings}",
                        "Vehicle details and photo" to if (d.vehicle.photoFile != null) "1 photo" else "No photo",
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = scheme.outlineVariant)
                    }
                    Text(
                        "This can't be undone. Backup files you've already saved aren't changed.",
                        Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exporting = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, scheme.onSurface),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text("Export first")
                }
                // Already archived vehicles have nothing gentler left to offer.
                if (d.vehicle.archivedOn == null) {
                    OutlinedButton(
                        onClick = { archiving = true },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, scheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    ) {
                        Text("Archive instead")
                    }
                }
            }

            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("Type ")
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)) { append(d.name) }
                            append(" to confirm")
                        },
                    )
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmed) scheme.error else scheme.primary,
                    unfocusedBorderColor = if (confirmed) scheme.error else scheme.outline,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { vm.delete(onDeleted) },
                enabled = confirmed && !vm.deleting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
            ) {
                Text("Delete vehicle permanently", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
