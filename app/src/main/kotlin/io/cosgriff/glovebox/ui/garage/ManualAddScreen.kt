package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.ui.service.symbolWord
import kotlinx.coroutines.flow.first
import io.cosgriff.glovebox.ui.schedule.DistanceUnitSwitch
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.theme.PlexMono
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.VehiclePick
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A list from NHTSA to pick from. Typing your own value works whatever state it is in. */
sealed interface PickList {
    data object Loading : PickList

    /** Offline, or NHTSA didn't answer. */
    data object Unavailable : PickList
    data class Loaded(val names: List<String>) : PickList
}

@HiltViewModel
class ManualAddViewModel @Inject constructor(
    private val nhtsa: NhtsaClient,
    private val vehicles: VehicleDao,
    private val odometer: OdometerDao,
    settings: SettingsRepository,
) : ViewModel() {
    /** What this vehicle's odometer counts in; starts as the unit chosen in Settings. */
    var unit by mutableStateOf(DistanceUnit.MILES)

    var year by mutableStateOf<Int?>(null)
        private set

    /** As NHTSA spells it when picked from the list, or as typed. */
    var make by mutableStateOf("")
        private set
    var model by mutableStateOf("")
        private set
    var trim by mutableStateOf("")
    var engine by mutableStateOf("")

    /** One of [VehiclePick.drivetrains], or null if the user hasn't said. */
    var drivetrain by mutableStateOf<String?>(null)
    var odometerText by mutableStateOf("")
        private set
    var saving by mutableStateOf(false)
        private set

    var makes by mutableStateOf<PickList>(PickList.Loading)
        private set

    /** For the chosen make and year; reloaded when either changes. */
    var models by mutableStateOf<PickList>(PickList.Loaded(emptyList()))
        private set
    private var modelsJob: Job? = null

    val canSave: Boolean get() = year != null && make.isNotBlank() && model.isNotBlank() && !saving

    init {
        viewModelScope.launch { unit = settings.settings.first().distanceUnit }
        loadMakes()
    }

    fun loadMakes() {
        makes = PickList.Loading
        viewModelScope.launch {
            makes = runCatching { nhtsa.makes() }.fold({ PickList.Loaded(VehiclePick.tidy(it)) }, { PickList.Unavailable })
        }
    }

    fun chooseYear(value: Int) {
        if (value == year) return
        year = value
        loadModels()
    }

    fun chooseMake(value: String) {
        if (value == make) return
        make = value
        model = "" // the old model belongs to the old make
        loadModels()
    }

    fun chooseModel(value: String) {
        model = value
    }

    fun onOdometerChange(value: String) {
        odometerText = value.filter(Char::isDigit).trimStart('0').take(MileageUpdate.MAX_DIGITS)
    }

    /** Only the make and the model year are sent. */
    private fun loadModels() {
        modelsJob?.cancel()
        val y = year
        if (y == null || make.isBlank()) {
            models = PickList.Loaded(emptyList())
            return
        }
        models = PickList.Loading
        modelsJob = viewModelScope.launch {
            models = runCatching { nhtsa.models(make, y) }.fold({ PickList.Loaded(VehiclePick.tidy(it)) }, { PickList.Unavailable })
        }
    }

    fun save(onSaved: (vehicleId: Long) -> Unit) {
        val y = year ?: return
        if (!canSave) return
        saving = true
        viewModelScope.launch {
            val today = LocalDate.now()
            val id = vehicles.insert(
                VehicleEntity(
                    vin = null,
                    enteredByHand = true,
                    year = y,
                    make = make,
                    model = model,
                    trim = VehicleText.clean(trim, VehicleText.MAX_NICKNAME),
                    engine = VehicleText.clean(engine, VehicleText.MAX_SPEC),
                    drivetrain = drivetrain,
                    createdOn = today,
                    distanceUnit = unit.name,
                ),
            )
            odometerText.toIntOrNull()?.let {
                odometer.insert(OdometerReadingEntity(vehicleId = id, date = today, odometer = it))
            }
            onSaved(id)
        }
    }
}

private enum class Picking { YEAR, MAKE, MODEL }

/** Design: 1a · Add without a VIN. */
@Composable
fun ManualAddScreen(onSaved: (vehicleId: Long) -> Unit, onBack: () -> Unit, vm: ManualAddViewModel = hiltViewModel()) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var picking by rememberSaveable { mutableStateOf<Picking?>(null) }
    val thisYear = LocalDate.now().year

    when (picking) {
        Picking.YEAR -> PickerDialog(
            title = "Model year",
            list = PickList.Loaded(VehiclePick.years(thisYear).map { it.toString() }),
            numeric = true,
            // Every year that can be chosen is in the list, so typing only narrows it.
            custom = { _, _ -> null },
            onPick = {
                vm.chooseYear(it.toInt())
                picking = null
            },
            onDismiss = { picking = null },
        )
        Picking.MAKE -> PickerDialog(
            title = "Make",
            list = vm.makes,
            display = Recalls::makeName,
            onRetry = vm::loadMakes,
            onPick = {
                vm.chooseMake(it)
                picking = null
            },
            onDismiss = { picking = null },
        )
        Picking.MODEL -> PickerDialog(
            title = "Model",
            list = vm.models,
            emptyNote = "NHTSA lists no ${vm.year ?: ""} ${Recalls.makeName(vm.make)} models. Type the model above.",
            onPick = {
                vm.chooseModel(it)
                picking = null
            },
            onDismiss = { picking = null },
        )
        null -> Unit
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text(
                "Add without a VIN",
                Modifier.padding(start = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "For older, imported or kit vehicles, or when the decode fails. Pick from NHTSA's make and model lists or type your own.",
                color = scheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickField("Year", vm.year?.toString(), Modifier.weight(1f)) { picking = Picking.YEAR }
                PickField("Make", vm.make.takeIf { it.isNotEmpty() }?.let(Recalls::makeName), Modifier.weight(1f)) { picking = Picking.MAKE }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickField(
                    "Model",
                    vm.model.takeIf { it.isNotEmpty() },
                    Modifier.weight(1f),
                    enabled = vm.year != null && vm.make.isNotEmpty(),
                ) { picking = Picking.MODEL }
                OutlinedTextField(
                    value = vm.trim,
                    onValueChange = { vm.trim = it.take(VehicleText.MAX_NICKNAME) },
                    label = { Text("Trim (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1f),
                )
            }
            if (vm.year == null || vm.make.isEmpty()) {
                Text("Choose the year and make first; the model list depends on both.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = vm.engine,
                onValueChange = { vm.engine = it.take(VehicleText.MAX_SPEC) },
                label = { Text("Engine (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Drivetrain (optional)", style = MaterialTheme.typography.labelLarge, color = statusColors().soon)
                // Nothing is chosen to begin with, and tapping the chosen one clears it: a guess is never saved.
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    VehiclePick.drivetrains.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = vm.drivetrain == d,
                            onClick = { vm.drivetrain = if (vm.drivetrain == d) null else d },
                            shape = SegmentedButtonDefaults.itemShape(i, VehiclePick.drivetrains.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = scheme.secondary,
                                activeContentColor = scheme.onSecondary,
                            ),
                            icon = {},
                        ) { Text(d) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DistanceUnitSwitch(vm.unit, onPick = { vm.unit = it })
                Text("What the odometer counts in. Readings and intervals for this vehicle are typed and shown in it. It can be changed later in Vehicle details.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = vm.odometerText,
                onValueChange = vm::onOdometerChange,
                label = { Text("Odometer (${vm.unit.symbolWord})") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Without a VIN, recalls are checked by year, make and model only, and can't be confirmed for your car. " +
                        "You can add the VIN later in Vehicle details.",
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors().soon,
                )
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

/** Looks like a text field, opens a picker. */
@Composable
private fun PickField(label: String, value: String?, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        color = scheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) scheme.outline else scheme.outlineVariant),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(
                    value ?: "Choose",
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        !enabled -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                        value == null -> scheme.onSurfaceVariant
                        else -> scheme.onSurface
                    },
                    maxLines = 1,
                )
            }
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/**
 * Pick from [list] or type a value of your own. The text box filters the list and, when what is
 * typed isn't in it, offers to use it as it stands. That is the list's "Other…", and it is all
 * there is when NHTSA can't be reached.
 */
@Composable
private fun PickerDialog(
    title: String,
    list: PickList,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    display: (String) -> String = { it },
    numeric: Boolean = false,
    custom: (typed: String, options: List<String>) -> String? = VehiclePick::custom,
    emptyNote: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var query by rememberSaveable(title) { mutableStateOf("") }
    val options = (list as? PickList.Loaded)?.names.orEmpty()
    val shown = VehiclePick.filter(options, query)
    val typed = custom(query, options)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = if (numeric) it.filter(Char::isDigit).take(4) else it.take(VehicleText.MAX_NICKNAME) },
                    label = { Text(if (numeric) "Type the year" else "Search or type your own") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    list == PickList.Loading -> Text("Loading NHTSA's list…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    list == PickList.Unavailable -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Couldn't load NHTSA's list. Type it above instead.",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        if (onRetry != null) TextButton(onClick = onRetry) { Text("Retry") }
                    }
                    options.isEmpty() && emptyNote != null -> Text(emptyNote, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    if (typed != null) {
                        item {
                            PickRow("Use “$typed”", accent = true) { onPick(typed) }
                            HorizontalDivider(color = scheme.outlineVariant)
                        }
                    }
                    items(shown, key = { it }) { option -> PickRow(display(option)) { onPick(option) } }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PickRow(text: String, accent: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
}
