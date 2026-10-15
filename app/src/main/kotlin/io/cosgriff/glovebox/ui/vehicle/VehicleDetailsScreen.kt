package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import android.os.PersistableBundle
import android.os.Build
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipDescription
import android.content.ClipData
import io.cosgriff.glovebox.domain.DistanceConversion
import io.cosgriff.glovebox.data.db.UnitDao
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.export.ExportSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.data.photo.VehiclePhotoStore
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.ClusterSwaps
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.domain.ScheduleSummaries
import io.cosgriff.glovebox.domain.ScheduleSummary
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.domain.Vin
import io.cosgriff.glovebox.ui.VehicleDetailsRoute
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.cosgriff.glovebox.ui.service.NoLinks
import javax.inject.Inject

data class VehicleDetails(
    val vehicle: VehicleEntity,
    /** Null when the vehicle has no schedule items at all, i.e. setup was skipped. */
    val schedule: ScheduleSummary?,
    /** Null until NHTSA has answered for this vehicle. */
    val recalls: OpenRecalls?,
    val torqueSpecs: Int,
)

/** The user-entered rows under "What it takes". */
enum class Spec(val label: String, val hint: String) {
    OIL("Engine oil", "Grade and how much it holds"),
    OIL_FILTER("Oil filter", "Part number"),
    TIRES("Tire size", "As printed on the sidewall"),
    WIPERS("Wiper blades", "Driver, passenger and rear lengths"),
}

private fun VehicleEntity.value(spec: Spec): String? = when (spec) {
    Spec.OIL -> oilSpec
    Spec.OIL_FILTER -> oilFilter
    Spec.TIRES -> tireSize
    Spec.WIPERS -> wiperBlades
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VehicleDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicles: VehicleDao,
    schedule: ScheduleDao,
    recalls: RecallDao,
    service: ServiceDao,
    odometer: OdometerDao,
    torque: TorqueDao,
    private val photos: VehiclePhotoStore,
    private val picker: VehiclePhotoPicker,
    private val archiver: VehicleArchiver,
    private val units: UnitDao,
) : ViewModel() {
    val vehicleId = savedStateHandle.toRoute<VehicleDetailsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val details: StateFlow<VehicleDetails?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeAll(vehicleId),
        recalls.observe(vehicleId),
        torque.observe(vehicleId),
    ) { vehicle, items, list, specs ->
        if (vehicle == null) return@combine null
        VehicleDetails(
            vehicle = vehicle,
            schedule = items.takeIf { it.isNotEmpty() }?.let { all -> ScheduleSummaries.of(all.map { it.tracked to it.useSevere }) },
            recalls = if (vehicle.recallsCheckedOn == null) null else Recalls.open(list.map { it.toStored() }),
            torqueSpecs = specs.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Decrypted in memory only while this screen is open. */
    val photo: StateFlow<ImageBitmap?> = vehicles.observe(vehicleId)
        .map { it?.photoFile }
        .distinctUntilChanged()
        .mapLatest { name -> name?.let { photos.load(it) }?.asImageBitmap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val photoFailed: StateFlow<Boolean> = picker.failedFor.map { it == vehicleId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** The highest reading so far, which a sale's final odometer starts from. */
    val lastOdometer: StateFlow<Int?> = combine(
        vehicles.observe(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
    ) { vehicle, records, readings ->
        val swap = vehicle?.clusterSwap()
        VehicleReminders.currentOdometer(records.map { it.toEntry(swap, NoLinks) }, readings.toReadings(swap))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val nicknameEdits = MutableStateFlow<String?>(null)

    init {
        // Saved as it is typed; only the latest text is ever written.
        viewModelScope.launch {
            nicknameEdits.filterNotNull().collectLatest { vehicles.setNickname(vehicleId, VehicleText.clean(it, VehicleText.MAX_NICKNAME)) }
        }
    }

    fun rename(text: String) {
        nicknameEdits.value = text
    }

    fun save(spec: Spec, text: String) = viewModelScope.launch {
        val value = VehicleText.clean(text, VehicleText.MAX_SPEC)
        when (spec) {
            Spec.OIL -> vehicles.setOilSpec(vehicleId, value)
            Spec.OIL_FILTER -> vehicles.setOilFilter(vehicleId, value)
            Spec.TIRES -> vehicles.setTireSize(vehicleId, value)
            Spec.WIPERS -> vehicles.setWiperBlades(vehicleId, value)
        }
    }

    /** Null removes it. Readings are stored as typed, so nothing else has to be rewritten. */
    /** Changes what the odometer counts in, converting every figure stored for the vehicle with it. */
    fun convertTo(unit: DistanceUnit) = viewModelScope.launch { units.convert(vehicleId, unit) }

    fun setClusterSwap(swap: ClusterSwap?) = viewModelScope.launch {
        vehicles.setClusterSwap(vehicleId, swap?.offset ?: 0, swap?.on, swap?.startReading)
    }

    fun pickPhoto() = picker.pick(vehicleId)

    /** For a vehicle added without one. The details the user entered are kept as they are. */
    fun setVin(vin: String) = viewModelScope.launch { vehicles.setVin(vehicleId, vin) }

    /** [onArchived] gets the Undo, which still works after this screen has closed. */
    fun markAsSold(sale: Sale, onArchived: (undo: () -> Unit) -> Unit) = viewModelScope.launch {
        val receipt = archiver.archive(vehicleId, sale, lastOdometer.value)
        onArchived { archiver.undo(receipt) }
    }

    fun removePhoto() = viewModelScope.launch { photos.remove(vehicleId) }
}

/** Design: 4d · Vehicle details. */
@Composable
fun VehicleDetailsScreen(
    onBack: () -> Unit,
    onOpenRecalls: (vehicleId: Long) -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    /** The vehicle has gone to the archive; [undo] brings it back. */
    onArchived: (vehicleName: String, undo: () -> Unit) -> Unit,
    onDelete: (vehicleId: Long) -> Unit,
    onOpenTorqueSpecs: (vehicleId: Long) -> Unit,
    onOpenReadings: (vehicleId: Long) -> Unit,
    vm: VehicleDetailsViewModel = hiltViewModel(),
) {
    val details = vm.details.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val v = details.vehicle
    val photo by vm.photo.collectAsStateWithLifecycle()
    val photoFailed by vm.photoFailed.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val lastOdometer by vm.lastOdometer.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Spec?>(null) }
    var selling by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var addingVin by rememberSaveable { mutableStateOf(false) }
    var settingOffset by rememberSaveable { mutableStateOf(false) }
    var changingUnit by rememberSaveable { mutableStateOf(false) }
    val swap = v.clusterSwap()

    if (changingUnit) {
        val to = DistanceUnit.entries.first { it != v.unit }
        AlertDialog(
            onDismissRequest = { changingUnit = false },
            title = { Text("Change this vehicle to ${to.plural}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("It counts in ${v.unit.plural} now.")
                    Text(DistanceConversion.explain(v.unit, to))
                    Text(
                        "Changing back converts again, and a figure can come back one out. Other vehicles aren't touched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        changingUnit = false
                        vm.convertTo(to)
                    },
                ) { Text("Convert to ${to.plural}") }
            },
            dismissButton = { TextButton(onClick = { changingUnit = false }) { Text("Cancel") } },
        )
    }

    if (settingOffset) {
        OdometerOffsetSheet(
            current = swap,
            unit = v.unit,
            onSave = {
                vm.setClusterSwap(it)
                settingOffset = false
            },
            onDismiss = { settingOffset = false },
        )
    }

    if (addingVin) {
        VinDialog(
            onSave = {
                vm.setVin(it)
                addingVin = false
            },
            onDismiss = { addingVin = false },
        )
    }
    val name = v.nickname ?: v.model

    if (selling) {
        MarkAsSoldSheet(
            vehicleName = name,
            lastOdometer = lastOdometer,
            unit = v.unit,
            onArchive = { sale ->
                selling = false
                vm.markAsSold(sale) { undo -> onArchived(name, undo) }
            },
            onDismiss = { selling = false },
            onExport = { exporting = true },
        )
    }
    // Drawn after the sold sheet so it opens on top of it.
    if (exporting) ExportSheet(vehicleId = v.id, onDismiss = { exporting = false })
    // Seeded once: after that the field is the source of truth and the database follows it.
    var nickname by rememberSaveable { mutableStateOf(v.nickname.orEmpty()) }

    editing?.let { spec ->
        SpecDialog(
            spec = spec,
            current = v.value(spec).orEmpty(),
            onSave = {
                vm.save(spec, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text(
                "Vehicle details",
                Modifier.padding(start = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PhotoBox(photo, hasPhoto = v.photoFile != null, name = v.nickname ?: v.model, onPick = vm::pickPhoto, onRemove = vm::removePhoto)
                if (photoFailed) {
                    Text(
                        "That image couldn't be read. Try another photo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it.take(VehicleText.MAX_NICKNAME)
                    vm.rename(nickname)
                },
                label = { Text("Nickname") },
                placeholder = { Text(v.model) },
                supportingText = { Text("Shown instead of the model name everywhere in the app.") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(if (v.enteredByHand) "VEHICLE" else "FROM THE VIN")
                Card {
                    if (v.vin == null) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { addingVin = true }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("VIN", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Add",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                            )
                            Chevron()
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    val rows = listOfNotNull(
                        v.vin?.let { Triple("VIN", it, true) },
                        Triple("Vehicle", VehicleText.title(v.year, v.make, v.model, v.trim), false),
                        v.drivetrain?.let { Triple("Drivetrain", it, false) },
                        v.transmission?.let { Triple("Transmission", it, false) },
                        v.engine?.let { Triple("Engine", it, false) },
                        v.bodyClass?.let { Triple("Body", it, false) },
                    )
                    rows.forEach { (label, value, mono) ->
                        // The VIN is the one value worth having elsewhere (an insurer's or a parts site's form): a long press copies it.
                        val copy = if (label == "VIN") ({ copyVin(context, value); haptics.performHapticFeedback(HapticFeedbackType.LongPress) }) else null
                        Row(
                            Modifier.fillMaxWidth()
                                .then(
                                    if (copy == null) {
                                        Modifier
                                    } else {
                                        Modifier
                                            .pointerInput(value) { detectTapGestures(onLongPress = { copy() }) }
                                            .semantics { onLongClick(label = "Copy the VIN") { copy(); true } }
                                    },
                                )
                                .padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (mono) PlexMono else null,
                                textAlign = TextAlign.End,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Text(
                        when {
                            v.enteredByHand -> "Entered by you"
                            v.trimConfirmed -> "Source: NHTSA vPIC, decoded from the VIN · trim confirmed by you"
                            else -> "Source: NHTSA vPIC, decoded from the VIN"
                        },
                        Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("WHAT IT TAKES")
                Card {
                    Spec.entries.forEachIndexed { i, spec ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val value = v.value(spec)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { editing = spec }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(spec.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value ?: "Add",
                                Modifier.weight(1f).padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (value == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                            )
                            Chevron()
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { settingOffset = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Odometer offset", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            swap?.let { ClusterSwaps.offsetLabel(it.offset, v.unit) } ?: "None",
                            Modifier.weight(1f).padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = if (swap != null) PlexMono else null,
                            textAlign = TextAlign.End,
                        )
                        Chevron()
                    }
                }
                Text(
                    "You enter these. Glovebox doesn't look them up, so check your owner's or service manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card {
                LinkRow(
                    title = "Torque & fastener specs",
                    detail = "Socket sizes and torque · " + if (details.torqueSpecs == 0) "none saved yet" else "${details.torqueSpecs} saved",
                    onClick = { onOpenTorqueSpecs(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val schedule = details.schedule
                LinkRow(
                    title = "Maintenance schedule",
                    detail = schedule?.text() ?: "Not set up yet",
                    // An empty schedule offers the typical one, or adding items one at a time.
                    onClick = { onOpenSchedule(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val open = details.recalls
                LinkRow(
                    title = "Recalls",
                    detail = when {
                        open == null -> "Not checked yet"
                        open.count == 0 -> "No open recalls"
                        else -> "${open.count} open"
                    },
                    detailColor = when {
                        open == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        open.count == 0 -> statusColors().ok
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    onClick = { onOpenRecalls(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    title = "Distance units",
                    detail = "${v.unit.title} · what the odometer counts in",
                    onClick = { changingUnit = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    title = "Mileage readings",
                    detail = "Every odometer reading · fix or delete a wrong one",
                    onClick = { onOpenReadings(v.id) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { selling = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text("Mark as sold")
                }
                Text(
                    "Sold vehicles move to an archive with their full history. Delete removes everything for good.",
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { onDelete(v.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete vehicle and all its data", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** "9 items tracked · Normal driving" */
private fun ScheduleSummary.text(): String {
    val items = if (tracked == 1) "1 item tracked" else "$tracked items tracked"
    if (tracked == 0) return items
    return items + " · " + when (use) {
        DrivingUse.NORMAL -> "Normal driving"
        DrivingUse.SEVERE -> "Severe driving"
        DrivingUse.MIXED -> "$severe on severe intervals"
    }
}

@Composable
private fun PhotoBox(photo: ImageBitmap?, hasPhoto: Boolean, name: String, onPick: () -> Unit, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    var menu by remember { mutableStateOf(false) }
    Box {
        if (hasPhoto) {
            Box(
                Modifier.fillMaxWidth()
                    .height(168.dp)
                    .clip(shape)
                    .clickable(role = Role.Button, onClickLabel = "Change photo", onClick = { menu = true }),
            ) {
                // Blank for the moment it takes to decrypt.
                photo?.let {
                    Image(it, contentDescription = "Photo of your $name", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Choose another photo") },
                    onClick = {
                        menu = false
                        onPick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove photo", color = scheme.error) },
                    onClick = {
                        menu = false
                        onRemove()
                    },
                )
            }
        } else {
            val dash = scheme.outline
            Column(
                Modifier.fillMaxWidth()
                    .height(168.dp)
                    .clip(shape)
                    .drawBehind { drawDashedBorder(dash, 20.dp.toPx(), 1.5.dp.toPx()) }
                    .clickable(role = Role.Button, onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = statusColors().soon)
                Text(
                    "Add a photo of your $name",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColors().soon,
                )
                Text("Stored encrypted on this phone", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

private fun DrawScope.drawDashedBorder(color: Color, radius: Float, width: Float) {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = width * 2, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))), // half is clipped away
    )
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

/** [onClick] null shows the row as information only, without a chevron. */
@Composable
private fun LinkRow(title: String, detail: String, detailColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = detailColor)
        }
        if (onClick != null) Chevron()
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun VinDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val plausible = Vin.isPlausible(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add the VIN") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = Vin.normalize(it).take(17) },
                label = { Text("VIN or serial number") },
                supportingText = {
                    Text(
                        when {
                            text.length == 17 && Vin.hasValidCheckDigit(text) -> "17 / 17 ✓ check digit"
                            text.length == 17 -> "The check digit doesn't match. That's normal for some imports; otherwise re-check it."
                            plausible -> "${text.length} characters. Fine for an older vehicle; newer ones have 17."
                            else -> "Letters and digits only, 5 to 17 characters."
                        },
                    )
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = plausible) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SpecDialog(spec: Spec, current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    // Opens ready to type, with the cursor after what is already there.
    var text by rememberSaveable(spec, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(current, TextRange(current.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.label) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.text.length <= VehicleText.MAX_SPEC) text = it },
                modifier = Modifier.focusRequester(focus),
                label = { Text(spec.hint) },
                supportingText = { Text("From your owner's or service manual. Leave empty to clear it.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(text.text) }),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text.text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Puts the VIN on the clipboard, marked sensitive so Android hides it in its clipboard preview. Android 13
 * and later say "Copied" themselves; earlier versions get a short message here.
 */
private fun copyVin(context: Context, vin: String) {
    val clip = ClipData.newPlainText("VIN", vin).apply {
        description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, "VIN copied", Toast.LENGTH_SHORT).show()
}
