package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.components.PrimaryButton
import io.cosgriff.glovebox.ui.components.Pill
import io.cosgriff.glovebox.ui.components.InkChip
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.ui.components.OdometerField
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.symbolWord
import kotlinx.coroutines.flow.first
import io.cosgriff.glovebox.ui.components.DistanceUnitSwitch
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import io.cosgriff.glovebox.domain.VehiclePick
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.DecodedVehicle
import io.cosgriff.glovebox.ui.components.DecodedField
import io.cosgriff.glovebox.ui.components.storedFields
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.domain.DecodeQuality
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.domain.Vin
import io.cosgriff.glovebox.domain.VinDecode
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.domain.Powertrains
import io.cosgriff.glovebox.ui.components.ChoiceDialog
import io.cosgriff.glovebox.ui.components.label
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    private val nhtsa: NhtsaClient,
    private val vehicles: VehicleDao,
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
    /** NHTSA couldn't be reached for the last decode. */
    var failed by mutableStateOf(false)
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
    /** Only what vPIC says plainly ([Powertrains.fromVpic]), then the owner's pick; null is "not known". */
    var powertrain by mutableStateOf<Powertrain?>(null)
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
        failed = false
    }

    /** A scanned VIN has already passed its check digit, so it goes straight to NHTSA. */
    fun useScanned(scanned: String) {
        onVinChange(scanned)
        if (vinValid && !decoding) decode()
    }

    fun decode() = viewModelScope.launch {
        decoding = true
        failed = false
        runCatching { nhtsa.decodeVin(vin) }
            .onSuccess { show(it) }
            .onFailure { failed = true }
        decoding = false
    }

    private fun show(d: DecodedVehicle) {
        year = d.year
        make = d.make.orEmpty()
        model = d.model.orEmpty()
        fields = d.storedFields()
        powertrain = Powertrains.fromVpic(d.electrification, d.fuel, d.fuelSecondary)
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
        odometerText = OdometerInput.typed(value)
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
            val id = vehicles.add(
                VehicleEntity(
                    vin = d.vin,
                    year = y,
                    make = make.trim(),
                    model = model.trim(),
                    trim = savedTrim,
                    trimConfirmed = trimConfirmed && savedTrim != null,
                    engine = fields[DecodedField.ENGINE],
                    fuel = fields[DecodedField.FUEL],
                    powertrain = powertrain?.name,
                    drivetrain = fields[DecodedField.DRIVE],
                    transmission = fields[DecodedField.TRANSMISSION],
                    bodyClass = fields[DecodedField.BODY],
                    engineCode = fields[DecodedField.ENGINE_CODE],
                    tpms = fields[DecodedField.TPMS],
                    series = fields[DecodedField.SERIES],
                    createdOn = today,
                    distanceUnit = unit.name,
                ),
                firstReading = OdometerInput.parse(odometerText.typedNumber()),
            )
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
@Composable
private fun RecallLookup.text(): String? = when (this) {
    RecallLookup.NotAsked -> null
    RecallLookup.Asking -> stringResource(R.string.add_vehicle_recalls_checking)
    RecallLookup.Failed -> stringResource(R.string.add_vehicle_recalls_failed)
    is RecallLookup.Found -> when (val n = notices.map { it.campaign }.filter { it.isNotBlank() }.distinct().size) {
        0 -> stringResource(R.string.add_vehicle_recalls_none)
        else -> stringResource(R.string.add_vehicle_recalls_found, n.asNumber())
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
        ScreenHeader(stringResource(R.string.add_vehicle_title), onBack)
        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.add_vehicle_start_with_vin), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.add_vehicle_vin_intro),
                    color = scheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                // Canvas: solid ink, so the orange stays for Decode VIN.
                colors = ButtonDefaults.buttonColors(containerColor = scheme.secondary, contentColor = scheme.onSecondary),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(end = 10.dp).size(22.dp))
                Text(stringResource(R.string.add_vehicle_scan), style = MaterialTheme.typography.titleMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
                Text(stringResource(R.string.add_vehicle_or_type), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val complete = vm.vin.length == 17
                OutlinedTextField(
                    value = vm.vin,
                    onValueChange = vm::onVinChange,
                    label = { Text(stringResource(R.string.add_vehicle_vin_label)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    isError = complete && !vm.vinValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.add_vehicle_vin_hint),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        when {
                            !complete -> stringResource(R.string.add_vehicle_vin_count, vm.vin.length)
                            vm.vinValid -> stringResource(R.string.add_vehicle_vin_valid)
                            else -> stringResource(R.string.add_vehicle_vin_check_digit_wrong)
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
                        stringResource(R.string.add_vehicle_vin_wrong_character),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                }
            }

            RowsCard {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.add_vehicle_where_heading), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    listOf(
                        stringResource(R.string.add_vehicle_where_door),
                        stringResource(R.string.add_vehicle_where_windshield),
                        stringResource(R.string.add_vehicle_where_card),
                    ).forEachIndexed { i, place ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text((i + 1).asNumber(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                                }
                            }
                            Text(place, style = MaterialTheme.typography.bodyMedium, color = statusColors().soon)
                        }
                    }
                }
            }

            PrimaryButton(
                onClick = {
                    focusManager.clearFocus()
                    vm.decode()
                },
                enabled = vm.vinValid && !vm.decoding,
            ) {
                if (vm.decoding) CircularProgressIndicator(Modifier.size(24.dp)) else Text(stringResource(R.string.add_vehicle_decode), style = MaterialTheme.typography.titleMedium)
            }
            if (vm.failed) Text(stringResource(R.string.add_vehicle_nhtsa_unreachable), color = scheme.error)
            TextButton(onClick = onNoVin, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.add_vehicle_no_vin))
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
    var editingPowertrain by rememberSaveable { mutableStateOf(false) }
    val identified = vm.year != null && vm.make.isNotBlank() && vm.model.isNotBlank()

    if (editing == DecodedField.DRIVE) {
        DriveDialog(
            current = vm.fields[DecodedField.DRIVE],
            onSave = {
                vm.correct(DecodedField.DRIVE, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    } else editing?.let { field ->
        FieldDialog(
            label = stringResource(field.label),
            current = vm.fields[field].orEmpty(),
            onSave = {
                vm.correct(field, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    if (editingPowertrain) {
        ChoiceDialog(
            title = stringResource(R.string.add_vehicle_powertrain),
            options = Powertrain.entries + null,
            selected = vm.powertrain,
            label = { stringResource(it?.label() ?: R.string.add_vehicle_powertrain_unsure) },
            onPick = { vm.powertrain = it },
            onDismiss = { editingPowertrain = false },
            note = stringResource(R.string.add_vehicle_powertrain_note),
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
        ScreenHeader(stringResource(R.string.add_vehicle_confirm_title), onBack = vm::backToVin)
        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (vm.quality) {
                DecodeQuality.PARTIAL -> Notice(
                    stringResource(R.string.add_vehicle_partial_title),
                    stringResource(R.string.add_vehicle_partial_body),
                    d.errorText,
                )
                DecodeQuality.UNIDENTIFIED -> Notice(
                    stringResource(R.string.add_vehicle_unidentified_title),
                    stringResource(R.string.add_vehicle_unidentified_body),
                    d.errorText,
                )
                else -> Unit
            }

            Surface(
                onClick = { editingVehicle = true },
                modifier = Modifier.fillMaxWidth(),
                color = scheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(d.vin, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
                    Text(
                        VehicleText.title(vm.year ?: 0, vm.make, vm.model, trim = null)?.takeIf { identified } ?: stringResource(R.string.add_vehicle_enter_ymm),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (identified) scheme.onSurface else scheme.primary,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VinDecode.shortDrive(vm.fields[DecodedField.DRIVE])?.let { Pill(it, scheme.primaryContainer, scheme.onPrimaryContainer, bold = false) }
                        vm.fields[DecodedField.ENGINE]?.let { Pill(it, scheme.surfaceVariant, statusColors().soon, bold = false) }
                        vm.fields[DecodedField.BODY]?.let { Pill(it, scheme.surfaceVariant, statusColors().soon, bold = false) }
                    }
                }
            }

            RowsCard {
                DecodedField.entries.forEach { field ->
                    val value = vm.fields[field]
                    // The engine's power is vPIC's own and isn't kept, so it isn't editable.
                    val shown = if (field == DecodedField.ENGINE) {
                        listOfNotNull(value, d.horsepower?.let { stringResource(R.string.add_vehicle_horsepower, it) }).joinToString(" · ").ifEmpty { null }
                    } else {
                        value
                    }
                    FieldRow(stringResource(field.label), shown ?: stringResource(R.string.add), muted = shown == null, onClick = { editing = field })
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                val powertrain = vm.powertrain
                FieldRow(
                    stringResource(R.string.add_vehicle_powertrain),
                    stringResource(powertrain?.label() ?: R.string.add),
                    muted = powertrain == null,
                    onClick = { editingPowertrain = true },
                )
                HorizontalDivider(color = scheme.outlineVariant)
                d.gvwr?.let {
                    FieldRow(stringResource(R.string.add_vehicle_gvwr), it)
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                d.plant?.let {
                    FieldRow(stringResource(R.string.add_vehicle_built_in), it)
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                vm.recalls.text()?.let {
                    FieldRow(stringResource(R.string.add_vehicle_recalls), it)
                    HorizontalDivider(color = scheme.outlineVariant)
                }
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.add_vehicle_source),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DistanceUnitSwitch(vm.unit, onPick = { vm.unit = it })
                Text(
                    stringResource(R.string.add_vehicle_unit_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            val ambiguous = vm.trimChoices.size > 1
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = vm.trim,
                    onValueChange = vm::onTrimChange,
                    label = { Text(stringResource(if (ambiguous && !vm.trimConfirmed) R.string.add_vehicle_trim_confirm else R.string.add_vehicle_trim)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1.6f),
                )
                OdometerField(vm.odometerText, vm::onOdometerChange, stringResource(R.string.odometer_in, vm.unit.symbolWord), Modifier.weight(1f))
            }
            // vPIC often has no trim (a Golf GTI, for one), so an empty field shouldn't look like a failed decode.
            if (vm.trimChoices.isEmpty() && vm.trim.isBlank()) {
                Text(
                    stringResource(R.string.add_vehicle_no_trim),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (ambiguous) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.add_vehicle_pick_trim),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vm.trimChoices.forEach { choice ->
                            InkChip(choice, selected = vm.trim == choice) { vm.onTrimChange(choice) }
                        }
                    }
                }
            }

            PrimaryButton(
                onClick = {
                    focusManager.clearFocus()
                    vm.save(onSaved)
                },
                enabled = vm.canSave,
            ) {
                Text(stringResource(R.string.add_vehicle_next), style = MaterialTheme.typography.titleMedium)
            }
        }
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
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.5.dp, scheme.primary),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = statusColors().soon)
            detail?.let { Text(stringResource(R.string.add_vehicle_nhtsa_said, it), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        }
    }
}

/** [onClick] null is a value shown for information only. */
@Composable
private fun FieldRow(label: String, value: String, muted: Boolean = false, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val correct = stringResource(R.string.add_vehicle_correct, label)
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = correct, onClick = onClick) else Modifier),
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
private fun FieldDialog(label: String, current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable(label) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(VehicleText.MAX_SPEC) },
                supportingText = { Text(stringResource(R.string.add_vehicle_field_replaces)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * The drive type as one of [VehiclePick.drivetrains], as on the add-without-a-VIN screen, since vPIC often
 * has none (a Golf, for one). A decoded value that is none of them is shown and kept unless one is picked.
 * Nothing is chosen for the user, and tapping the chosen one clears it.
 */
@Composable
private fun DriveDialog(current: String?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val initial = VinDecode.shortDrive(current)?.uppercase()?.takeIf { it in VehiclePick.drivetrains }
    var chosen by rememberSaveable { mutableStateOf(initial) }
    val other = current?.takeIf { initial == null }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(DecodedField.DRIVE.label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    VehiclePick.drivetrains.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = chosen == d,
                            onClick = { chosen = if (chosen == d) null else d },
                            shape = SegmentedButtonDefaults.itemShape(i, VehiclePick.drivetrains.size),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = scheme.secondary, activeContentColor = scheme.onSecondary),
                            icon = {},
                        ) { Text(d) }
                    }
                }
                Text(
                    when {
                        other != null && chosen == null -> stringResource(R.string.add_vehicle_drive_kept, other)
                        else -> stringResource(R.string.add_vehicle_drive_replaces)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Unchanged, the decoded wording is kept as it was ("FWD/Front-Wheel Drive/4x2").
                onSave(if (chosen == initial || (chosen == null && other != null)) current.orEmpty() else chosen.orEmpty())
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        title = { Text(stringResource(R.string.add_vehicle_ymm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.add_vehicle_model_year)) },
                    isError = yearText.length == 4 && parsedYear == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = makeText,
                    onValueChange = { makeText = it },
                    label = { Text(stringResource(R.string.add_vehicle_make)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text(stringResource(R.string.add_vehicle_model)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text(
                    stringResource(R.string.add_vehicle_ymm_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedYear!!, cleanMake!!, cleanModel!!) },
                enabled = parsedYear != null && cleanMake != null && cleanModel != null,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
