package io.cosgriff.glovebox.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(vehicles: VehicleDao) : ViewModel() {
    val active: StateFlow<List<VehicleEntity>?> =
        vehicles.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Latest known odometer per vehicle id. */
    val odometers: StateFlow<Map<Long, Int>> =
        vehicles.observeOdometers()
            .map { list -> list.associate { it.vehicleId to it.odometer } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

@Composable
fun GarageScreen(
    onAddVehicle: () -> Unit,
    onLogService: (vehicleId: Long) -> Unit,
    vm: GarageViewModel = hiltViewModel(),
) {
    val vehicles by vm.active.collectAsStateWithLifecycle()
    val odometers by vm.odometers.collectAsStateWithLifecycle()
    when {
        vehicles == null -> Unit
        vehicles!!.isEmpty() -> EmptyGarage(onAddVehicle)
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Text("Garage", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp)) }
            items(vehicles!!, key = { it.id }) { v -> VehicleCard(v, odometers[v.id], onLogService = { onLogService(v.id) }) }
            item { Button(onClick = onAddVehicle, modifier = Modifier.fillMaxWidth()) { Text("Add a vehicle") } }
        }
    }
}

@Composable
private fun VehicleCard(v: VehicleEntity, odometer: Int?, onLogService: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${v.year} ${v.nickname ?: v.model}", style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(v.trim, v.drivetrain, v.engine).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Odometer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        odometer?.let { String.format(Locale.US, "%,d mi", it) } ?: "Not recorded",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Button(onClick = onLogService, modifier = Modifier.heightIn(min = 48.dp)) { Text("Log service") }
            }
        }
    }
}

@Composable
private fun EmptyGarage(onAddVehicle: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text("No vehicles yet", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Add one with its VIN and we'll fill in the details, the factory maintenance schedule and any recalls.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAddVehicle, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Add a vehicle") }
    }
}
