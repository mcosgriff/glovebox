package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.DangerButton
import io.cosgriff.glovebox.ui.components.TypedConfirmField
import io.cosgriff.glovebox.ui.components.BorderedCard
import io.cosgriff.glovebox.ui.service.asNumber
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.export.ExportSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.DeleteVehicleRoute
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import io.cosgriff.glovebox.ui.service.NoLinks
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
    val documents: Int = 0,
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
    private val documents: DocumentDao,
    private val wipe: DataWipe,
    private val archiver: VehicleArchiver,
) : ViewModel() {
    private val vehicleId = savedStateHandle.route<DeleteVehicleRoute>().vehicleId

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
                documents = documents.documentsFor(vehicleId).size,
                lastOdometer = VehicleReminders.currentOdometer(
                    records.map { it.toEntry(v.clusterSwap(), NoLinks) },
                    readings.toReadings(v.clusterSwap()),
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
            unit = d.vehicle.unit,
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
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel))
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
                    stringResource(R.string.delete_vehicle_title, "${d.vehicle.year} ${d.name}"),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.delete_vehicle_removes), color = scheme.onSurfaceVariant)
            }

            BorderedCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    listOf(
                        stringResource(R.string.delete_vehicle_records) to d.records.asNumber(),
                        stringResource(R.string.delete_vehicle_photos) to d.photos.asNumber(),
                        stringResource(R.string.delete_vehicle_schedule) to d.scheduleItems.asNumber(),
                        stringResource(R.string.delete_vehicle_torque) to d.torqueSpecs.asNumber(),
                        stringResource(R.string.delete_vehicle_recalls) to d.recalls.asNumber(),
                        stringResource(R.string.readings_title) to d.mileageReadings.asNumber(),
                        stringResource(R.string.documents_title) to d.documents.asNumber(),
                        stringResource(R.string.delete_vehicle_details) to
                            if (d.vehicle.photoFile != null) pluralStringResource(R.plurals.photos, 1, 1.asNumber()) else stringResource(R.string.delete_vehicle_no_photo),
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = scheme.outlineVariant)
                    }
                    Text(
                        stringResource(R.string.delete_vehicle_cant_undo),
                        Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InkOutlinedButton(onClick = { exporting = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.export_first))
                }
                // Already archived vehicles have nothing gentler left to offer.
                if (d.vehicle.archivedOn == null) {
                    InkOutlinedButton(onClick = { archiving = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.delete_vehicle_archive_instead))
                    }
                }
            }

            TypedConfirmField(d.name, typed, { typed = it }, capitalization = KeyboardCapitalization.None)

            DangerButton(onClick = { vm.delete(onDeleted) }, enabled = confirmed && !vm.deleting) {
                Text(stringResource(R.string.delete_vehicle_button), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
