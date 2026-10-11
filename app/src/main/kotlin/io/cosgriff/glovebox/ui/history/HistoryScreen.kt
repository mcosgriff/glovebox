package io.cosgriff.glovebox.ui.history

import io.cosgriff.glovebox.ui.export.ExportSheet
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.CatalogCategory
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.categoryOf
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.longDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryState(
    val vehicles: List<VehicleEntity>,
    /** The vehicle whose history is showing; null when the garage is empty. */
    val vehicle: VehicleEntity?,
    /** Newest first. */
    val records: List<ServiceRecordWithItems>,
) {
    /** Categories that actually appear in this vehicle's records, in catalog order. */
    val categories: List<CatalogCategory>
        get() {
            val used = records.flatMap { it.items }.mapNotNull { ServiceCatalog.categoryOf(it.catalogId)?.id }.toSet()
            return ServiceCatalog.categories.filter { it.id in used }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicles: VehicleDao,
    service: ServiceDao,
) : ViewModel() {
    /** Set when opened from Archived vehicles: that one vehicle's history, to read only. */
    private val archivedId: Long? = savedStateHandle.get<Long>("vehicleId")
    val archived: Boolean get() = archivedId != null

    private val chosenVehicle = MutableStateFlow<Long?>(null)

    /** Category id to show, or null for all. */
    var filter by mutableStateOf<String?>(null)

    private val source: Flow<HistoryState> =
        if (archivedId != null) {
            combine(vehicles.observe(archivedId), service.observeRecords(archivedId)) { vehicle, records ->
                HistoryState(listOfNotNull(vehicle), vehicle, records)
            }
        } else {
            combine(vehicles.observeActive(), chosenVehicle) { list, chosen ->
                list to (list.firstOrNull { it.id == chosen } ?: list.firstOrNull())
            }.flatMapLatest { (list, vehicle) ->
                if (vehicle == null) {
                    flowOf(HistoryState(list, null, emptyList()))
                } else {
                    service.observeRecords(vehicle.id).map { HistoryState(list, vehicle, it) }
                }
            }
        }

    val state: StateFlow<HistoryState?> = source.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun chooseVehicle(id: Long) {
        chosenVehicle.value = id
        filter = null
    }
}

@Composable
fun HistoryScreen(
    onOpenRecord: (recordId: Long) -> Unit,
    /** Shown as a back arrow; the History tab itself has none. */
    onBack: (() -> Unit)? = null,
    vm: HistoryViewModel = hiltViewModel(),
) {
    val state = vm.state.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val categories = state.categories
    // A filter can outlive the last record that matched it (e.g. after a delete).
    val filter = vm.filter?.takeIf { f -> categories.any { it.id == f } }
    val shown = state.records.filter { r ->
        filter == null || r.items.any { ServiceCatalog.categoryOf(it.catalogId)?.id == filter }
    }

    var exporting by rememberSaveable { mutableStateOf(false) }
    if (exporting) ExportSheet(vehicleId = state.vehicle?.id, onDismiss = { exporting = false })

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
        }
        Column(
            Modifier.padding(start = 20.dp, top = if (onBack != null) 0.dp else 16.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("History", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                if (state.vehicle != null) {
                    OutlinedButton(onClick = { exporting = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Export") }
                }
            }
            state.vehicle?.let { v ->
                Text(
                    "${v.label()} · ${shown.size} ${if (shown.size == 1) "record" else "records"}" + if (vm.archived) " · archived, read-only" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.vehicles.size > 1) {
            ChipRow {
                state.vehicles.forEach { v ->
                    Chip(v.label(), selected = v.id == state.vehicle?.id) { vm.chooseVehicle(v.id) }
                }
            }
        }
        if (categories.isNotEmpty()) {
            ChipRow {
                Chip("All", selected = filter == null) { vm.filter = null }
                categories.forEach { c ->
                    Chip(c.label, selected = filter == c.id) { vm.filter = c.id }
                }
            }
        }

        when {
            state.vehicle == null -> EmptyNote("Add a vehicle in the Garage to start its service history.")
            state.records.isEmpty() && vm.archived -> EmptyNote("No service records were logged for this vehicle.")
            state.records.isEmpty() -> EmptyNote("No service records yet. Use Log service in the Garage to add the first one.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.record.id }) { r -> RecordCard(r, onClick = { onOpenRecord(r.record.id) }) }
            }
        }
    }
}

@Composable
private fun RecordCard(r: ServiceRecordWithItems, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.record.date.format(longDate), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(r.record.odometer.asMiles(), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
            Text(r.items.joinToString(", ") { it.label })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (r.record.diy) "DIY" else r.record.shopName ?: "Shop",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                r.record.costCents?.let {
                    Text(Money.format(it), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        // Canvas: the chosen filter is solid ink.
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
        ),
    )
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun VehicleEntity.label(): String = "$year ${nickname ?: model}"
