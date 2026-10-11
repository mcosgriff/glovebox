package io.cosgriff.glovebox.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.DecodedVehicle
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.Vin
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    private val nhtsa: NhtsaClient,
    private val vehicles: VehicleDao,
    private val odometer: OdometerDao,
    private val recallRepository: RecallRepository,
) : ViewModel() {
    var vin by mutableStateOf("")
        private set
    var odometerText by mutableStateOf("")
    var decoding by mutableStateOf(false)
        private set
    var decoded by mutableStateOf<DecodedVehicle?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** What NHTSA lists for the decoded year, make and model. */
    var recalls by mutableStateOf<RecallLookup>(RecallLookup.NotAsked)
        private set

    val vinValid: Boolean get() = Vin.hasValidCheckDigit(vin)

    fun onVinChange(input: String) {
        val normalized = Vin.normalize(input).take(17)
        if (normalized == vin) return // e.g. an 18th character typed; keep the decode result
        vin = normalized
        decoded = null
        error = null
        recalls = RecallLookup.NotAsked
    }

    fun decode() = viewModelScope.launch {
        decoding = true
        error = null
        runCatching { nhtsa.decodeVin(vin) }
            .onSuccess { decoded = it }
            .onFailure { error = "Couldn't reach NHTSA. Check your connection, or add the vehicle manually." }
        decoding = false
        decoded?.let { lookUpRecalls(it) }
    }

    /** Only the year, make and model are sent. A failure here never blocks adding the vehicle. */
    private suspend fun lookUpRecalls(d: DecodedVehicle) {
        val year = d.year ?: 0
        val make = d.make.orEmpty()
        val model = d.model.orEmpty()
        if (!Recalls.canCheck(year, make, model)) return
        recalls = RecallLookup.Asking
        val found = runCatching { nhtsa.recalls(year, make, model) }
        // The VIN may have been edited while NHTSA was answering.
        if (decoded === d) recalls = found.fold({ RecallLookup.Found(it) }, { RecallLookup.Failed })
    }

    fun save(onSaved: (vehicleId: Long) -> Unit) = viewModelScope.launch {
        val d = decoded ?: return@launch
        val today = LocalDate.now()
        val id = vehicles.insert(
            VehicleEntity(
                vin = d.vin,
                year = d.year ?: 0,
                make = d.make ?: "Unknown",
                model = d.model ?: "Unknown",
                trim = d.trim,
                engine = listOfNotNull(d.displacementL?.let { "${it}L" }, d.cylinders?.let { "$it-cyl" }).joinToString(" ").ifEmpty { null },
                drivetrain = d.driveType,
                bodyClass = d.bodyClass,
                createdOn = today,
            ),
        )
        odometerText.filter(Char::isDigit).toIntOrNull()?.let {
            odometer.insert(OdometerReadingEntity(vehicleId = id, date = today, odometer = it))
        }
        // Keep the list already on screen; if it never arrived, the garage re-checks later.
        (recalls as? RecallLookup.Found)?.let { recallRepository.store(id, it.notices, today) }
        onSaved(id)
    }
}

sealed interface RecallLookup {
    data object NotAsked : RecallLookup
    data object Asking : RecallLookup
    data object Failed : RecallLookup
    data class Found(val notices: List<RecallNotice>) : RecallLookup
}

/** Nothing can be marked done before the vehicle is saved, so every recall found is still open. */
private fun RecallLookup.text(): String? = when (this) {
    RecallLookup.NotAsked -> null
    RecallLookup.Asking -> "Checking…"
    RecallLookup.Failed -> "Couldn't check"
    is RecallLookup.Found -> when (val n = notices.map { it.campaign }.filter { it.isNotBlank() }.distinct().size) {
        0 -> "None found"
        else -> "$n found · $n open"
    }
}

@Composable
fun AddVehicleScreen(onSaved: (vehicleId: Long) -> Unit, vm: AddVehicleViewModel = hiltViewModel()) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Start with the VIN", style = MaterialTheme.typography.headlineSmall)
        Text(
            "We look it up with NHTSA's VIN decoder and fill in year, make, model, drivetrain and engine.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // TODO: "Scan VIN barcode" with CameraX + ML Kit (Code 39) goes above this field.
        OutlinedTextField(
            value = vm.vin,
            onValueChange = vm::onVinChange,
            label = { Text("Vehicle Identification Number") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            supportingText = {
                Text(
                    when {
                        vm.vin.length < 17 -> "${vm.vin.length} / 17 · no I, O or Q"
                        vm.vinValid -> "17 / 17 ✓ check digit"
                        else -> "Check digit doesn't match — re-check the VIN"
                    },
                )
            },
            isError = vm.vin.length == 17 && !vm.vinValid,
            modifier = Modifier.fillMaxWidth(),
        )

        val d = vm.decoded
        if (d == null) {
            Button(
                onClick = {
                    focusManager.clearFocus() // hide the keyboard so the result is visible
                    vm.decode()
                },
                enabled = vm.vinValid && !vm.decoding,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (vm.decoding) CircularProgressIndicator(Modifier.height(24.dp)) else Text("Decode VIN")
            }
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            DecodedCard(d, vm.recalls.text())
            OutlinedTextField(
                value = vm.odometerText,
                onValueChange = { vm.odometerText = it },
                label = { Text("Odometer (mi)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.save(onSaved)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Next: maintenance schedule")
            }
        }
    }
}

@Composable
private fun DecodedCard(d: DecodedVehicle, recalls: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${d.year ?: ""} ${d.make.orEmpty()} ${d.model.orEmpty()}".trim(), style = MaterialTheme.typography.titleLarge)
            listOf(
                "Trim" to d.trim,
                "Drive type" to d.driveType,
                "Body" to d.bodyClass,
                "Engine" to listOfNotNull(d.displacementL?.let { "${it}L" }, d.cylinders?.let { "$it-cyl" }, d.engineModel).joinToString(" · ").ifEmpty { null },
                "Fuel" to d.fuel,
                "Transmission" to d.transmission,
                "Built in" to d.plant,
                "Recalls (NHTSA)" to recalls,
            ).forEach { (k, v) ->
                if (v != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v)
                }
            }
            if (d.errorCode != null && d.errorCode != "0") {
                Text("NHTSA note: ${d.errorText}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
