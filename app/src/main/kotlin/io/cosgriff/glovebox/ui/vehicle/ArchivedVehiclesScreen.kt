package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.components.Pill
import io.cosgriff.glovebox.ui.components.ScreenHeader
import androidx.compose.ui.platform.LocalResources
import io.cosgriff.glovebox.ui.service.asNumber
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import io.cosgriff.glovebox.ui.export.ExportSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.cosgriff.glovebox.ui.service.NoLinks
import javax.inject.Inject

data class ArchivedVehicle(
    val vehicle: VehicleEntity,
    /** The highest reading on record; null if the vehicle never had one. */
    val finalOdometer: Int?,
    val records: Int,
    /** Receipt and parts photos across its records. */
    val photos: Int,
)

@HiltViewModel
class ArchivedVehiclesViewModel @Inject constructor(
    vehicles: VehicleDao,
    service: ServiceDao,
    odometer: OdometerDao,
    private val archiver: VehicleArchiver,
) : ViewModel() {
    /** Most recently sold first. Null while loading. */
    val archived: StateFlow<List<ArchivedVehicle>?> = combine(
        vehicles.observeArchived(),
        service.observeAllRecords(),
        odometer.observeAll(),
    ) { list, records, readings ->
        val recordsBy = records.groupBy { it.record.vehicleId }
        val readingsBy = readings.groupBy { it.vehicleId }
        list.map { v ->
            val mine = recordsBy[v.id].orEmpty()
            ArchivedVehicle(
                vehicle = v,
                finalOdometer = VehicleReminders.currentOdometer(
                    mine.map { it.toEntry(v.clusterSwap(), NoLinks) },
                    readingsBy[v.id].orEmpty().toReadings(v.clusterSwap()),
                ),
                records = mine.size,
                photos = mine.sumOf { it.photos.size },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [onMoved] gets the Undo, which archives it again with the same sold date. */
    fun moveBack(v: VehicleEntity, onMoved: (undo: () -> Unit) -> Unit) = viewModelScope.launch {
        val soldOn = v.archivedOn ?: return@launch
        archiver.moveBack(v.id)
        onMoved { archiver.archiveAgain(v.id, soldOn) }
    }
}

/** Design: 4f · Archived vehicles. */
@Composable
fun ArchivedVehiclesScreen(
    onBack: () -> Unit,
    onViewHistory: (vehicleId: Long) -> Unit,
    onDelete: (vehicleId: Long) -> Unit,
    /** Shows a message with one action on the app's snackbar. */
    onMessage: (message: String, actionLabel: String, onAction: () -> Unit) -> Unit,
    vm: ArchivedVehiclesViewModel = hiltViewModel(),
) {
    val archived = vm.archived.collectAsStateWithLifecycle().value ?: return // first frame while loading
    var exporting by rememberSaveable { mutableStateOf<Long?>(null) }
    exporting?.let { id -> ExportSheet(vehicleId = id, onDismiss = { exporting = null }) }
    val res = LocalResources.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(stringResource(R.string.archived_title), onBack)
        }
        item {
            Text(
                stringResource(if (archived.isEmpty()) R.string.archived_empty else R.string.archived_intro),
                Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(archived, key = { it.vehicle.id }) { a ->
            val name = a.vehicle.nickname ?: a.vehicle.model
            ArchivedCard(
                a,
                onViewHistory = { onViewHistory(a.vehicle.id) },
                onExport = { exporting = a.vehicle.id },
                onMoveBack = { vm.moveBack(a.vehicle) { undo -> onMessage(res.getString(R.string.archived_moved_back, name), res.getString(R.string.undo), undo) } },
                onDelete = { onDelete(a.vehicle.id) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun ArchivedCard(
    a: ArchivedVehicle,
    onViewHistory: () -> Unit,
    onExport: () -> Unit,
    onMoveBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val v = a.vehicle
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        VehicleText.title(v.year, v.make, v.model, trim = null) ?: stringResource(R.string.unknown_vehicle),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        listOfNotNull(
                            v.nickname,
                            v.archivedOn?.let { stringResource(R.string.archived_sold_on, it.format(longDate)) },
                            a.finalOdometer?.let { stringResource(R.string.archived_final, it.asDistance(v.unit)) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Pill(stringResource(R.string.archived_sold_tag), scheme.surfaceVariant, statusColors().soon)
            }
            Text(
                listOfNotNull(
                    pluralStringResource(R.plurals.service_records, a.records, a.records.asNumber()),
                    a.photos.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.photos, it, it.asNumber()) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = statusColors().soon,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(stringResource(R.string.archived_view_history) to onViewHistory, stringResource(R.string.export) to onExport).forEach { (label, action) ->
                    OutlinedButton(
                        onClick = action,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, scheme.onSurface),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                    ) {
                        Text(label)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onMoveBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(R.string.archived_move_back), fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
