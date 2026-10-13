package io.cosgriff.glovebox.ui.garage

import kotlinx.coroutines.flow.first
import io.cosgriff.glovebox.ui.schedule.DistanceUnitSwitch
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import io.cosgriff.glovebox.domain.DecodeQuality
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.domain.Vin
import io.cosgriff.glovebox.domain.VinDecode
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A decoded value the user can correct before the vehicle is saved. */
enum class DecodedField(val label: String) {
    BODY("Body class"),
    DRIVE("Drive type"),
    ENGINE("Engine"),
    FUEL("Fuel"),
    TRANSMISSION("Transmission"),
}

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    private val nhtsa: NhtsaClient,
    private val vehicles: VehicleDao,
    private val odometer: OdometerDao,
    private val recallRepository: RecallRepository,
    settings: SettingsRepository,
) : ViewModel() {
    /** What this vehicle's odometer counts in; starts as the unit chosen in Settings. */
    var unit by mutableStateOf(DistanceUnit.MILES)

    init {
        viewModelScope.launch { unit = settings.settings.first().distanceUnit }
    }

    var vin by mutableStateOf("")
        private set
    var decoding by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Set once NHTSA has answered; the confirm step shows while it is. */
    var decoded by mutableStateOf<DecodedVehicle?>(null)
        private set

    // What will be saved: vPIC's answer to start with, then whatever the user corrects.
    var year by mutableStateOf<Int?>(null)
        private set
    var make by mutableStateOf("")
        private set
    var model by mutableStateOf("")
        private set
    var fields by mutableStateOf(emptyMap<DecodedField, String>())
        private set
    var trim by mutableStateOf("")
        private set
    var trimChoices by mutableStateOf(emptyList<String>())
        private set

    /** The user picked or typed the trim, so it is theirs and not vPIC's guess. */
    var trimConfirmed by mutableStateOf(false)
        private set
    var odometerText by mutableStateOf("")
        private set
    var saving by mutableStateOf(false)
        private set

    /** What NHTSA lists for the year, make and model on screen. */
    var recalls by mutableStateOf<RecallLookup>(RecallLookup.NotAsked)
        private set
    private var recallJob: Job? = null

    val vinValid: Boolean get() = Vin.hasValidCheckDigit(vin)

    val quality: DecodeQuality?
        get() = decoded?.let { VinDecode.quality(it.errorCode, it.year, it.make, it.model) }

    /** A vehicle is never saved without a real year, make and model. */
    val canSave: Boolean get() = year != null && make.isNotBlank() && model.isNotBlank() && !saving

    fun onVinChange(input: String) {
        val normalized = Vin.normalize(input).take(17)
        if (normalized == vin) return // e.g. an 18th character typed
        vin = normalized
        error = null
    }

    /** A scanned VIN has already passed its check digit, so it goes straight to NHTSA. */
    fun useScanned(scanned: String) {
        onVinChange(scanned)
        if (vinValid && !decoding) decode()
    }

    fun decode() = viewModelScope.launch {
        decoding = true
        error = null
        runCatching { nhtsa.decodeVin(vin) }
            .onSuccess { show(it) }
            .onFailure { error = "Couldn't reach NHTSA. Check your connection and try again." }
        decoding = false
    }

    private fun show(d: DecodedVehicle) {
        year = d.year
        make = d.make.orEmpty()
        model = d.model.orEmpty()
        fields = buildMap {
            d.bodyClass?.let { put(DecodedField.BODY, it) }
            d.driveType?.let { put(DecodedField.DRIVE, it) }
            listOfNotNull(d.displacementL?.let { "${it}L" }, d.cylinders?.let { "$it-cyl" }).joinToString(" ").ifEmpty { null }
                ?.let { put(DecodedField.ENGINE, it) }
            d.fuel?.let { put(DecodedField.FUEL, it) }
            d.transmission?.let { put(DecodedField.TRANSMISSION, it) }
        }
        trimChoices = VinDecode.trimChoices(d.trim)
        // More than one candidate is left for the user to choose between.
        trim = trimChoices.singleOrNull().orEmpty()
        trimConfirmed = false
        decoded = d
        lookUpRecalls()
    }

    /** Back from the confirm step to the VIN, which is kept. */
    fun backToVin() {
        decoded = null
        recallJob?.cancel()
        recalls = RecallLookup.NotAsked
    }

    fun correctVehicle(newYear: Int, newMake: String, newModel: String) {
        year = newYear
        make = newMake
        model = newModel
        lookUpRecalls()
    }

    fun correct(field: DecodedField, value: String) {
        val clean = VehicleText.clean(value, VehicleText.MAX_SPEC)
        fields = if (clean == null) fields - field else fields + (field to clean)
    }

    fun onTrimChange(value: String) {
        trim = value.take(VehicleText.MAX_NICKNAME)
        trimConfirmed = true
    }

    fun onOdometerChange(value: String) {
        odometerText = value.filter(Char::isDigit).trimStart('0').take(MileageUpdate.MAX_DIGITS)
    }

    /** Only the year, make and model are sent. A failure here never blocks adding the vehicle. */
    private fun lookUpRecalls() {
        recallJob?.cancel()
        val y = year ?: 0
        if (!Recalls.canCheck(y, make, model)) {
            recalls = RecallLookup.NotAsked
            return
        }
        recalls = RecallLookup.Asking
        recallJob = viewModelScope.launch {
            recalls = runCatching { nhtsa.recalls(y, make, model) }.fold({ RecallLookup.Found(it) }, { RecallLookup.Failed })
        }
    }

    fun save(onSaved: (vehicleId: Long) -> Unit) {
        val d = decoded ?: return
        val y = year ?: return
        if (!canSave) return
        saving = true
        viewModelScope.launch {
            val today = LocalDate.now()
            val savedTrim = VehicleText.clean(trim, VehicleText.MAX_NICKNAME)
            val id = vehicles.insert(
                VehicleEntity(
                    vin = d.vin,
                    year = y,
                    make = make.trim(),
                    model = model.trim(),
                    trim = savedTrim,
                    trimConfirmed = trimConfirmed && savedTrim != null,
                    engine = fields[DecodedField.ENGINE],
                    fuel = fields[DecodedField.FUEL],
                    drivetrain = fields[DecodedField.DRIVE],
                    transmission = fields[DecodedField.TRANSMISSION],
                    bodyClass = fields[DecodedField.BODY],
                    createdOn = today,
                    distanceUnit = unit.name,
                ),
            )
            odometerText.toIntOrNull()?.let {
                odometer.insert(OdometerReadingEntity(vehicleId = id, date = today, odometer = it))
            }
            // Keep the list already on screen; if it never arrived, the garage re-checks later.
            (recalls as? RecallLookup.Found)?.let { recallRepository.store(id, it.notices, today) }
            onSaved(id)
        }
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
fun AddVehicleScreen(
    onSaved: (vehicleId: Long) -> Unit,
    onBack: () -> Unit,
    onNoVin: () -> Unit,
    onScan: () -> Unit,
    /** A VIN the scanner just handed back; it is decoded straight away. */
    scannedVin: String?,
    onScannedVinUsed: () -> Unit,
    vm: AddVehicleViewModel = hiltViewModel(),
) {
    LaunchedEffect(scannedVin) {
        if (scannedVin != null) {
            vm.useScanned(scannedVin)
            onScannedVinUsed()
        }
    }
    val decoded = vm.decoded
    if (decoded == null) {
        VinEntry(vm, onBack, onNoVin, onScan)
    } else {
        BackHandler(onBack = vm::backToVin)
        ConfirmVehicle(vm, decoded, onSaved)
    }
}

/** Design: 1 · Add vehicle (VIN). */
@Composable
private fun VinEntry(vm: AddVehicleViewModel, onBack: () -> Unit, onNoVin: () -> Unit, onScan: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        TopBar("Add a vehicle", onBack)
        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Start with the VIN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "We look it up with NHTSA's VIN decoder and fill in year, make, model, drivetrain and engine for you.",
                    color = scheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                // Canvas: solid ink, so the orange stays for Decode VIN.
                colors = ButtonDefaults.buttonColors(containerColor = scheme.secondary, contentColor = scheme.onSecondary),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(end = 10.dp).size(22.dp))
                Text("Scan VIN barcode", style = MaterialTheme.typography.titleMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
                Text("or type it", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val complete = vm.vin.length == 17
                OutlinedTextField(
                    value = vm.vin,
                    onValueChange = vm::onVinChange,
                    label = { Text("Vehicle Identification Number") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    isError = complete && !vm.vinValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "No I, O or Q · letters are uppercased",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        when {
                            !complete -> "${vm.vin.length} / 17"
                            vm.vinValid -> "17 / 17 ✓ check digit"
                            else -> "Check digit doesn't match"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            !complete -> scheme.onSurfaceVariant
                            vm.vinValid -> statusColors().ok
                            else -> scheme.error
                        },
                    )
                }
                if (complete && !vm.vinValid) {
                    Text(
                        "One of the characters is wrong. Compare it with the sticker, one character at a time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                }
            }

            Card {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Where to find it", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    listOf(
                        "Driver's door jamb sticker — has a scannable barcode",
                        "Base of the windshield, driver's side",
                        "Registration or insurance card",
                    ).forEachIndexed { i, place ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                                }
                            }
                            Text(place, style = MaterialTheme.typography.bodyMedium, color = statusColors().soon)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.decode()
                },
                enabled = vm.vinValid && !vm.decoding,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (vm.decoding) CircularProgressIndicator(Modifier.size(24.dp)) else Text("Decode VIN", style = MaterialTheme.typography.titleMedium)
            }
            vm.error?.let { Text(it, color = scheme.error) }
            TextButton(onClick = onNoVin, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("No VIN? Enter year, make and model manually")
            }
        }
    }
}

/** Design: 3 · NHTSA decode result. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmVehicle(vm: AddVehicleViewModel, d: DecodedVehicle, onSaved: (vehicleId: Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var editing by rememberSaveable { mutableStateOf<DecodedField?>(null) }
    var editingVehicle by rememberSaveable { mutableStateOf(false) }
    val identified = vm.year != null && vm.make.isNotBlank() && vm.model.isNotBlank()

    editing?.let { field ->
        FieldDialog(
            label = field.label,
            current = vm.fields[field].orEmpty(),
            onSave = {
                vm.correct(field, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    if (editingVehicle) {
        VehicleDialog(
            year = vm.year,
            make = vm.make,
            model = vm.model,
            onSave = { y, make, model ->
                vm.correctVehicle(y, make, model)
                editingVehicle = false
            },
            onDismiss = { editingVehicle = false },
        )
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        TopBar("Is this your vehicle?", onBack = vm::backToVin)
        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (vm.quality) {
                DecodeQuality.PARTIAL -> Notice(
                    "NHTSA decoded this VIN but flagged it",
                    "Check the details below and correct anything that's wrong before you continue.",
                    d.errorText,
                )
                DecodeQuality.UNIDENTIFIED -> Notice(
                    "NHTSA couldn't identify this VIN",
                    "Go back and check it against the sticker, or enter the year, make and model yourself.",
                    d.errorText,
                )
                else -> Unit
            }

            Surface(
                onClick = { editingVehicle = true },
                modifier = Modifier.fillMaxWidth(),
                color = scheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(d.vin, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
                    Text(
                        if (identified) VehicleText.title(vm.year ?: 0, vm.make, vm.model, trim = null) else "Enter year, make and model",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (identified) scheme.onSurface else scheme.primary,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VinDecode.shortDrive(vm.fields[DecodedField.DRIVE])?.let { Pill(it, scheme.primaryContainer, scheme.onPrimaryContainer) }
                        vm.fields[DecodedField.ENGINE]?.let { Pill(it, scheme.surfaceVariant, statusColors().soon) }
                        vm.fields[DecodedField.BODY]?.let { Pill(it, scheme.surfaceVariant, statusColors().soon) }
                    }
                }
            }

            Card {
                DecodedField.entries.forEach { field ->
                    val value = vm.fields[field]
                    // The engine's model code and power are vPIC's own and aren't kept, so they aren't editable.
                    val shown = if (field == DecodedField.ENGINE) {
                        listOfNotNull(value, d.engineModel, d.horsepower?.let { "$it hp" }).joinToString(" · ").ifEmpty { null }
                    } else {
                        value
                    }
                    FieldRow(field.label, shown ?: "Add", muted = shown == null, onClick = { editing = field })
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                d.gvwr?.let {
                    FieldRow("GVWR", it)
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                d.plant?.let {
                    FieldRow("Built in", it)
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                vm.recalls.text()?.let {
                    FieldRow("Recalls (NHTSA)", it)
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                    Text(
                        "Source: NHTSA vPIC decoder · tap the vehicle, or any of the first five values, to correct it",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DistanceUnitSwitch(vm.unit, onPick = { vm.unit = it })
                Text(
                    "What the odometer counts in. Readings and intervals for this vehicle are typed and shown in it. It can be changed later in Vehicle details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            val ambiguous = vm.trimChoices.size > 1
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = vm.trim,
                    onValueChange = vm::onTrimChange,
                    label = { Text(if (ambiguous && !vm.trimConfirmed) "Trim — please confirm" else "Trim") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1.6f),
                )
                OutlinedTextField(
                    value = vm.odometerText,
                    onValueChange = vm::onOdometerChange,
                    label = { Text("Odometer (${vm.unit.symbol})") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            if (ambiguous) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "The VIN doesn't say which trim this is. Pick yours, or type it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.trimChoices.forEach { choice ->
                            FilterChip(
                                selected = vm.trim == choice,
                                onClick = { vm.onTrimChange(choice) },
                                label = { Text(choice) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = scheme.secondary,
                                    selectedLabelColor = scheme.onSecondary,
                                ),
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    vm.save(onSaved)
                },
                enabled = vm.canSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Next: maintenance schedule", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        Text(
            title,
            Modifier.padding(start = 8.dp).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** [detail] is vPIC's own wording, shown small for anyone who wants it. */
@Composable
private fun Notice(title: String, body: String, detail: String?) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth(),
        color = scheme.primaryContainer,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, scheme.primary),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = statusColors().soon)
            detail?.let { Text("NHTSA: $it", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

/** [onClick] null is a value shown for information only. */
@Composable
private fun FieldRow(label: String, value: String, muted: Boolean = false, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Correct $label", onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Text(
            value,
            Modifier.weight(1f).padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (muted) scheme.primary else scheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(color = container, shape = CircleShape) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun FieldDialog(label: String, current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable(label) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(VehicleText.MAX_SPEC) },
                supportingText = { Text("Replaces what NHTSA decoded. Leave empty to clear it.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun VehicleDialog(year: Int?, make: String, model: String, onSave: (Int, String, String) -> Unit, onDismiss: () -> Unit) {
    var yearText by rememberSaveable { mutableStateOf(year?.toString().orEmpty()) }
    var makeText by rememberSaveable { mutableStateOf(make) }
    var modelText by rememberSaveable { mutableStateOf(model) }
    val parsedYear = VinDecode.parseYear(yearText, LocalDate.now().year)
    val cleanMake = VehicleText.clean(makeText, VehicleText.MAX_NICKNAME)
    val cleanModel = VehicleText.clean(modelText, VehicleText.MAX_NICKNAME)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Year, make and model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Model year") },
                    isError = yearText.length == 4 && parsedYear == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = makeText,
                    onValueChange = { makeText = it },
                    label = { Text("Make") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text("Model") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text(
                    "Recalls are looked up by these three, so spell them the way the manufacturer does.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedYear!!, cleanMake!!, cleanModel!!) },
                enabled = parsedYear != null && cleanMake != null && cleanModel != null,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
