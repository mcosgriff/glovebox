package io.cosgriff.glovebox.ui.vehicle

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
import androidx.compose.ui.text.font.FontFamily
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
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.data.photo.VehiclePhotoStore
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.domain.ScheduleSummaries
import io.cosgriff.glovebox.domain.ScheduleSummary
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.ui.VehicleDetailsRoute
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import io.cosgriff.glovebox.ui.service.toEntry
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
import javax.inject.Inject

data class VehicleDetails(
    val vehicle: VehicleEntity,
    /** Null when the vehicle has no schedule items at all, i.e. setup was skipped. */
    val schedule: ScheduleSummary?,
    /** Null until NHTSA has answered for this vehicle. */
    val recalls: OpenRecalls?,
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
    private val photos: VehiclePhotoStore,
    private val picker: VehiclePhotoPicker,
    private val archiver: VehicleArchiver,
) : ViewModel() {
    val vehicleId = savedStateHandle.toRoute<VehicleDetailsRoute>().vehicleId

    /** Null while loading, and if the vehicle no longer exists. */
    val details: StateFlow<VehicleDetails?> = combine(
        vehicles.observe(vehicleId),
        schedule.observeAll(vehicleId),
        recalls.observe(vehicleId),
    ) { vehicle, items, list ->
        if (vehicle == null) return@combine null
        VehicleDetails(
            vehicle = vehicle,
            schedule = items.takeIf { it.isNotEmpty() }?.let { all -> ScheduleSummaries.of(all.map { it.tracked to it.useSevere }) },
            recalls = if (vehicle.recallsCheckedOn == null) null else Recalls.open(list.map { it.toStored() }),
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
    val lastOdometer: StateFlow<Int?> = combine(service.observeRecords(vehicleId), odometer.observe(vehicleId)) { records, readings ->
        VehicleReminders.currentOdometer(records.map { it.toEntry() }, readings.map { OdometerReading(it.date, it.odometer) })
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

    fun pickPhoto() = picker.pick(vehicleId)

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
    onSetUpSchedule: (vehicleId: Long) -> Unit,
    /** The vehicle has gone to the archive; [undo] brings it back. */
    onArchived: (vehicleName: String, undo: () -> Unit) -> Unit,
    onDelete: (vehicleId: Long) -> Unit,
    vm: VehicleDetailsViewModel = hiltViewModel(),
) {
    val details = vm.details.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val v = details.vehicle
    val photo by vm.photo.collectAsStateWithLifecycle()
    val photoFailed by vm.photoFailed.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val lastOdometer by vm.lastOdometer.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Spec?>(null) }
    var selling by rememberSaveable { mutableStateOf(false) }
    val name = v.nickname ?: v.model

    if (selling) {
        MarkAsSoldSheet(
            vehicleName = name,
            lastOdometer = lastOdometer,
            onArchive = { sale ->
                selling = false
                vm.markAsSold(sale) { undo -> onArchived(name, undo) }
            },
            onDismiss = { selling = false },
        )
    }
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
                SectionLabel(if (v.vin != null) "FROM THE VIN" else "VEHICLE")
                Card {
                    val rows = listOfNotNull(
                        v.vin?.let { Triple("VIN", it, true) },
                        Triple("Vehicle", VehicleText.title(v.year, v.make, v.model, v.trim), false),
                        v.drivetrain?.let { Triple("Drivetrain", it, false) },
                        v.engine?.let { Triple("Engine", it, false) },
                        v.bodyClass?.let { Triple("Body", it, false) },
                    )
                    rows.forEach { (label, value, mono) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (mono) FontFamily.Monospace else null,
                                textAlign = TextAlign.End,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Text(
                        if (v.vin != null) "Source: NHTSA vPIC, decoded from the VIN" else "Entered by you",
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
                }
                Text(
                    "You enter these. Glovebox doesn't look them up, so check your owner's or service manual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card {
                val schedule = details.schedule
                LinkRow(
                    title = "Maintenance schedule",
                    detail = schedule?.text() ?: "Not set up yet",
                    // Until a set-up schedule can be reopened, only a skipped setup leads anywhere.
                    onClick = if (schedule == null) ({ onSetUpSchedule(v.id) }) else null,
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
