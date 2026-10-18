package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.service.typedDecimal
import io.cosgriff.glovebox.domain.DistanceKind
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.ui.service.line
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.offsetLabel
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.pluralWord
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.combinedClickable
import io.cosgriff.glovebox.ui.service.whatItTakes
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.domain.TakesItem
import androidx.compose.material3.Switch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.input.KeyboardType
import io.cosgriff.glovebox.domain.VehicleSpecEntry
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import io.cosgriff.glovebox.domain.LicensePlates
import java.util.Locale
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
import io.cosgriff.glovebox.data.db.DocumentDao
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
import io.cosgriff.glovebox.domain.VehicleDocuments
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
    val documentCount: Int = 0,
    val documentBytes: Long = 0,
)

/** The user-entered rows under "What it takes". */

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
    documents: DocumentDao,
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
        documents.observe(vehicleId),
    ) { vehicle, items, list, specs, docs ->
        if (vehicle == null) return@combine null
        VehicleDetails(
            vehicle = vehicle,
            schedule = items.takeIf { it.isNotEmpty() }?.let { all -> ScheduleSummaries.of(all.map { it.tracked to it.useSevere }) },
            recalls = if (vehicle.recallsCheckedOn == null) null else Recalls.open(list.map { it.toStored() }),
            torqueSpecs = specs.size,
            documentCount = docs.size,
            documentBytes = docs.sumOf { it.sizeBytes },
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

    /** [plate] is already cleaned (LicensePlates.clean); null clears it. */
    fun savePlate(plate: String?, region: String?) = viewModelScope.launch { vehicles.setPlate(vehicleId, plate, region) }

    /** Each is already checked and in its standard form (VehicleSpecEntry); null clears it. */
    fun saveOil(grade: String?, approval: String?, capacity: Double?, unit: VehicleSpecEntry.Volume?, filter: String?) = viewModelScope.launch {
        vehicles.setOil(vehicleId, grade, approval, capacity, unit?.name.takeIf { capacity != null }, filter)
    }

    fun saveTires(front: String?, rear: String?, pFront: Int?, pRear: Int?, pSpare: Int?, unit: VehicleSpecEntry.Pressure) = viewModelScope.launch {
        val any = pFront != null || pRear != null || pSpare != null
        vehicles.setTires(vehicleId, front, rear.takeIf { front != null }, pFront, pRear, pSpare, unit.name.takeIf { any })
    }

    fun saveWipers(driver: Int?, passenger: Int?, rear: Int?, note: String?) = viewModelScope.launch {
        vehicles.setWipers(vehicleId, driver, passenger, rear, note)
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
    /** What it takes past oil, tires and wipers: fluids, filters, plugs, battery, bulbs. */
    onOpenMoreTakes: (vehicleId: Long) -> Unit,
    onOpenReadings: (vehicleId: Long) -> Unit,
    /** The Import page with this vehicle chosen: a Carfax report, or an import file. */
    onOpenImport: (vehicleId: Long) -> Unit,
    /** The vehicle's paperwork: a history report, the title, the registration. */
    onOpenDocuments: (vehicleId: Long) -> Unit,
    /** Deleting the service records alone, imported or all, keeping everything else. */
    onClearHistory: (vehicleId: Long) -> Unit,
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
    var editing by rememberSaveable { mutableStateOf<TakesItem?>(null) }
    val takes = v.whatItTakes()
    var selling by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var addingVin by rememberSaveable { mutableStateOf(false) }
    var editingPlate by rememberSaveable { mutableStateOf(false) }
    var settingOffset by rememberSaveable { mutableStateOf(false) }
    var changingUnit by rememberSaveable { mutableStateOf(false) }
    val swap = v.clusterSwap()

    if (changingUnit) {
        val to = DistanceUnit.entries.first { it != v.unit }
        AlertDialog(
            onDismissRequest = { changingUnit = false },
            title = { Text(stringResource(R.string.details_unit_change_title, to.pluralWord)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.details_unit_counts_in, v.unit.pluralWord))
                    Text(unitExplanation(v.unit, to))
                    Text(
                        stringResource(R.string.details_unit_change_back),
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
                ) { Text(stringResource(R.string.details_unit_convert, to.pluralWord)) }
            },
            dismissButton = { TextButton(onClick = { changingUnit = false }) { Text(stringResource(R.string.cancel)) } },
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

    if (editingPlate) {
        PlateDialog(
            plate = v.plate,
            region = v.plateRegion,
            onSave = { plate, region ->
                vm.savePlate(plate, region)
                editingPlate = false
            },
            onDismiss = { editingPlate = false },
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

    when (val item = editing) {
        TakesItem.OIL -> OilDialog(v, onSave = { g, a, c, u, f -> vm.saveOil(g, a, c, u, f); editing = null }, onDismiss = { editing = null })
        TakesItem.TIRES -> TiresDialog(v, onSave = { f, r, pf, pr, ps, u -> vm.saveTires(f, r, pf, pr, ps, u); editing = null }, onDismiss = { editing = null })
        TakesItem.WIPERS -> WipersDialog(v, onSave = { d, p, r, n -> vm.saveWipers(d, p, r, n); editing = null }, onDismiss = { editing = null })
        null -> Unit
        else -> Unit // the rest are edited on their own page
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(
                stringResource(R.string.home_vehicle_details),
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
                        stringResource(R.string.details_photo_unreadable),
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
                label = { Text(stringResource(R.string.details_nickname)) },
                placeholder = { Text(v.model) },
                supportingText = { Text(stringResource(R.string.details_nickname_note)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )

            Card {
                val plate = LicensePlates.label(v.plate, v.plateRegion)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { editingPlate = true }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.details_plate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        plate ?: stringResource(R.string.add),
                        Modifier.weight(1f).padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (v.plate != null) PlexMono else null,
                        color = if (plate == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                    Chevron()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(if (v.enteredByHand) R.string.details_section_vehicle else R.string.details_section_from_vin))
                Card {
                    if (v.vin == null) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { addingVin = true }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.details_vin), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                stringResource(R.string.add),
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
                        v.vin?.let { Triple(stringResource(R.string.details_vin), it, true) },
                        Triple(stringResource(R.string.details_vehicle), VehicleText.title(v.year, v.make, v.model, v.trim) ?: stringResource(R.string.unknown_vehicle), false),
                        v.drivetrain?.let { Triple(stringResource(R.string.details_drivetrain), it, false) },
                        v.transmission?.let { Triple(stringResource(R.string.add_vehicle_transmission), it, false) },
                        v.engine?.let { Triple(stringResource(R.string.add_vehicle_engine), it, false) },
                        v.bodyClass?.let { Triple(stringResource(R.string.details_body), it, false) },
                    )
                    val copyVinLabel = stringResource(R.string.details_copy_vin)
                    rows.forEach { (label, value, mono) ->
                        // The VIN is the one value worth having elsewhere (an insurer's or a parts site's form): a long
                        // press copies it. It is the one row in mono.
                        val copy = if (mono) ({ copyText(context, label, value, sensitive = true); haptics.performHapticFeedback(HapticFeedbackType.LongPress) }) else null
                        Row(
                            Modifier.fillMaxWidth()
                                .then(
                                    if (copy == null) {
                                        Modifier
                                    } else {
                                        Modifier
                                            .pointerInput(value) { detectTapGestures(onLongPress = { copy() }) }
                                            .semantics { onLongClick(label = copyVinLabel) { copy(); true } }
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
                            v.enteredByHand -> stringResource(R.string.details_entered_by_you)
                            v.trimConfirmed -> stringResource(R.string.details_source_trim_confirmed)
                            else -> stringResource(R.string.details_source)
                        },
                        Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.details_section_takes))
                Card {
                    takes.basics().forEachIndexed { i, spec ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val value = takes.line(spec)
                        // A long press copies the row's part numbers, for a parts site's search box.
                        val parts = takes.partNumbers(spec)
                        val partsLabel = stringResource(R.string.details_part_numbers)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .combinedClickable(
                                    role = Role.Button,
                                    onClick = { editing = spec },
                                    onLongClickLabel = parts?.let { stringResource(R.string.details_copy_parts) },
                                    onLongClick = parts?.let { p -> { copyText(context, partsLabel, p, sensitive = false); haptics.performHapticFeedback(HapticFeedbackType.LongPress) } },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(spec.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value ?: stringResource(R.string.add),
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
                    // The rest have a page of their own, like torque and fastener specs.
                    val entered = takes.entered().size
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { onOpenMoreTakes(v.id) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(stringResource(R.string.fluids_title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (entered == 0) stringResource(R.string.none_saved_yet) else stringResource(R.string.details_takes_saved, entered.asNumber(), (entered + takes.notEntered().size).asNumber()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (entered == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Chevron()
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = { settingOffset = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.offset_title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            swap?.let { offsetLabel(it.offset, v.unit) } ?: stringResource(R.string.none),
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
                    stringResource(R.string.details_takes_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card {
                LinkRow(
                    title = stringResource(R.string.details_torque),
                    detail = if (details.torqueSpecs == 0) stringResource(R.string.details_torque_none) else stringResource(R.string.details_torque_saved, details.torqueSpecs.asNumber()),
                    onClick = { onOpenTorqueSpecs(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val schedule = details.schedule
                LinkRow(
                    title = stringResource(R.string.details_schedule),
                    detail = schedule?.text() ?: stringResource(R.string.details_schedule_none),
                    // An empty schedule offers the typical one, or adding items one at a time.
                    onClick = { onOpenSchedule(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val open = details.recalls
                LinkRow(
                    title = stringResource(R.string.details_recalls),
                    detail = when {
                        open == null -> stringResource(R.string.details_recalls_not_checked)
                        open.count == 0 -> stringResource(R.string.no_open_recalls)
                        else -> stringResource(R.string.details_recalls_open, open.count.asNumber())
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
                    title = stringResource(R.string.details_units),
                    detail = stringResource(R.string.details_units_detail, v.unit.titleWord),
                    onClick = { changingUnit = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    title = stringResource(R.string.readings_title),
                    detail = stringResource(R.string.details_readings_detail),
                    onClick = { onOpenReadings(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    title = stringResource(R.string.documents_title),
                    detail = if (details.documentCount == 0) stringResource(R.string.details_documents_none) else stringResource(R.string.details_documents, documentsSummary(details.documentCount, details.documentBytes)),
                    onClick = { onOpenDocuments(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    title = stringResource(R.string.details_import),
                    detail = stringResource(R.string.details_import_detail),
                    onClick = { onOpenImport(v.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    title = stringResource(R.string.details_clear),
                    detail = stringResource(R.string.details_clear_detail),
                    onClick = { onClearHistory(v.id) },
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
                    Text(stringResource(R.string.details_mark_sold))
                }
                Text(
                    stringResource(R.string.details_sold_note),
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
                    Text(stringResource(R.string.details_delete), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** "9 items tracked · Normal driving" */
@Composable
private fun ScheduleSummary.text(): String {
    val items = pluralStringResource(R.plurals.details_items_tracked, tracked, tracked.asNumber())
    if (tracked == 0) return items
    val use = when (use) {
        DrivingUse.NORMAL -> stringResource(R.string.details_normal_driving)
        DrivingUse.SEVERE -> stringResource(R.string.details_severe_driving)
        DrivingUse.MIXED -> stringResource(R.string.details_mixed_driving, severe.asNumber())
    }
    return "$items · $use"
}

/** What the user is told before a vehicle's unit is changed, with an example of each conversion. */
@Composable
private fun unitExplanation(from: DistanceUnit, to: DistanceUnit): String {
    val scheduled = if (from == DistanceUnit.MILES) 5_000 else 8_000
    return stringResource(
        R.string.details_unit_explain,
        from.pluralWord,
        to.pluralWord,
        100.asDistance(from),
        DistanceConversion.convert(100, from, to, DistanceKind.MEASURED).asDistance(to),
        scheduled.asDistance(from),
        DistanceConversion.convert(scheduled, from, to, DistanceKind.SCHEDULED).asDistance(to),
    )
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
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.details_change_photo), onClick = { menu = true }),
            ) {
                // Blank for the moment it takes to decrypt.
                photo?.let {
                    Image(it, contentDescription = stringResource(R.string.details_photo_of, name), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.details_choose_photo)) },
                    onClick = {
                        menu = false
                        onPick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.details_remove_photo), color = scheme.error) },
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
                    stringResource(R.string.details_add_photo, name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColors().soon,
                )
                Text(stringResource(R.string.details_photo_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
internal fun Chevron() {
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
        title = { Text(stringResource(R.string.details_add_vin)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = Vin.normalize(it).take(17) },
                label = { Text(stringResource(R.string.details_vin_or_serial)) },
                supportingText = {
                    Text(
                        when {
                            text.length == 17 && Vin.hasValidCheckDigit(text) -> stringResource(R.string.add_vehicle_vin_valid)
                            text.length == 17 -> stringResource(R.string.details_vin_check_digit)
                            plausible -> pluralStringResource(R.plurals.details_vin_older, text.length, text.length)
                            else -> stringResource(R.string.details_vin_letters)
                        },
                    )
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = plausible) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** The plate and where it was issued. Either can be left empty; both empty clears it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlateDialog(plate: String?, region: String?, onSave: (String?, String?) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(plate.orEmpty()) }
    var code by rememberSaveable { mutableStateOf(region) }
    var open by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_plate)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.uppercase(Locale.US).take(LicensePlates.MAX + 2) },
                    label = { Text(stringResource(R.string.details_plate_field)) },
                    supportingText = { Text(stringResource(R.string.details_plate_note)) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
                    OutlinedTextField(
                        value = LicensePlates.region(code)?.name ?: stringResource(R.string.not_set),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.details_plate_region)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.not_set)) }, onClick = { code = null; open = false })
                        LicensePlates.regions.forEach { r ->
                            DropdownMenuItem(text = { Text(stringResource(R.string.details_plate_region_item, r.name, r.code)) }, onClick = { code = r.code; open = false })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(LicensePlates.clean(text), code) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * A field that checks what is typed: [check] gives the standard form, or null when it isn't valid.
 * Empty is always allowed and clears it. The error shows once the user has typed something wrong.
 */
@Composable
internal fun CheckedField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    error: String,
    check: (String) -> Any?,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    val bad = value.isNotBlank() && check(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = bad,
        supportingText = (if (bad) error else supporting)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = keyboard, imeAction = ImeAction.Next),
    )
}

/** Save is offered only when every filled-in field is valid. */
@Composable
internal fun SpecEditor(title: String, valid: Boolean, onSave: () -> Unit, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
                Text(
                    stringResource(R.string.details_spec_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = valid) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

internal fun String.orNullIfBlank() = trim().takeIf { it.isNotEmpty() }

@Composable
private fun OilDialog(v: VehicleEntity, onSave: (String?, String?, Double?, VehicleSpecEntry.Volume?, String?) -> Unit, onDismiss: () -> Unit) {
    var grade by rememberSaveable { mutableStateOf(v.oilSpec.orEmpty()) }
    var approval by rememberSaveable { mutableStateOf(v.oilApproval.orEmpty()) }
    var capacity by rememberSaveable { mutableStateOf(v.oilCapacity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()) }
    var litres by rememberSaveable { mutableStateOf(v.oilCapacityUnit == VehicleSpecEntry.Volume.LITRES.name) }
    var filter by rememberSaveable { mutableStateOf(v.oilFilter.orEmpty()) }
    val unit = if (litres) VehicleSpecEntry.Volume.LITRES else VehicleSpecEntry.Volume.QUARTS
    fun ok(text: String, check: (String) -> Any?) = text.isBlank() || check(text) != null
    val valid = ok(grade, VehicleSpecEntry::oilGrade) && ok(approval, VehicleSpecEntry::oilApproval) &&
        ok(capacity) { VehicleSpecEntry.oilCapacity(it.typedDecimal(), unit) } && ok(filter, VehicleSpecEntry::partNumbers)
    SpecEditor(
        title = stringResource(R.string.takes_oil),
        valid = valid,
        onSave = {
            onSave(
                grade.orNullIfBlank()?.let(VehicleSpecEntry::oilGrade),
                approval.orNullIfBlank()?.let(VehicleSpecEntry::oilApproval),
                capacity.orNullIfBlank()?.let { VehicleSpecEntry.oilCapacity(it.typedDecimal(), unit) },
                unit,
                filter.orNullIfBlank()?.let(VehicleSpecEntry::partNumbers),
            )
        },
        onDismiss = onDismiss,
    ) {
        CheckedField(grade, { grade = it }, stringResource(R.string.oil_grade), stringResource(R.string.oil_grade_error), VehicleSpecEntry::oilGrade, supporting = stringResource(R.string.oil_grade_note))
        // The common grades a tap away.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VehicleSpecEntry.commonGrades.forEach { g ->
                FilterChip(selected = VehicleSpecEntry.oilGrade(grade) == g, onClick = { grade = g }, label = { Text(g, fontFamily = PlexMono) })
            }
        }
        CheckedField(approval, { approval = it }, stringResource(R.string.oil_approval), stringResource(R.string.oil_approval_error), VehicleSpecEntry::oilApproval, supporting = stringResource(R.string.oil_approval_note))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckedField(
                capacity, { capacity = it }, stringResource(R.string.oil_capacity), stringResource(R.string.oil_capacity_error, unit.max.toInt().asNumber(), stringResource(unit.word)),
                { VehicleSpecEntry.oilCapacity(it.typedDecimal(), unit) }, modifier = Modifier.weight(1f), keyboard = KeyboardType.Decimal,
            )
            listOf(false to stringResource(R.string.unit_qt), true to stringResource(R.string.unit_litre)).forEach { (l, symbol) ->
                FilterChip(selected = litres == l, onClick = { litres = l }, label = { Text(symbol) })
            }
        }
        CheckedField(
            filter, { filter = it }, stringResource(R.string.oil_filter), stringResource(R.string.oil_filter_error),
            VehicleSpecEntry::partNumbers, supporting = stringResource(R.string.oil_filter_note),
        )
    }
}

@Composable
private fun TiresDialog(v: VehicleEntity, onSave: (String?, String?, Int?, Int?, Int?, VehicleSpecEntry.Pressure) -> Unit, onDismiss: () -> Unit) {
    var front by rememberSaveable { mutableStateOf(v.tireSize.orEmpty()) }
    var differ by rememberSaveable { mutableStateOf(v.tireSizeRear != null) }
    var rear by rememberSaveable { mutableStateOf(v.tireSizeRear.orEmpty()) }
    var pFront by rememberSaveable { mutableStateOf(v.tirePressureFront?.toString().orEmpty()) }
    var pRear by rememberSaveable { mutableStateOf(v.tirePressureRear?.toString().orEmpty()) }
    var pSpare by rememberSaveable { mutableStateOf(v.tirePressureSpare?.toString().orEmpty()) }
    var kpa by rememberSaveable { mutableStateOf(v.tirePressureUnit == VehicleSpecEntry.Pressure.KPA.name) }
    val unit = if (kpa) VehicleSpecEntry.Pressure.KPA else VehicleSpecEntry.Pressure.PSI
    val pressure = { s: String -> VehicleSpecEntry.tirePressure(s, unit) }
    val valid = (front.isBlank() || VehicleSpecEntry.tireSize(front) != null) && (!differ || rear.isBlank() || VehicleSpecEntry.tireSize(rear) != null) &&
        listOf(pFront, pRear, pSpare).all { it.isBlank() || pressure(it) != null }
    SpecEditor(
        title = stringResource(R.string.takes_tires),
        valid = valid,
        onSave = {
            onSave(
                front.orNullIfBlank()?.let(VehicleSpecEntry::tireSize),
                rear.orNullIfBlank()?.takeIf { differ }?.let(VehicleSpecEntry::tireSize),
                pFront.orNullIfBlank()?.let(pressure),
                pRear.orNullIfBlank()?.let(pressure),
                pSpare.orNullIfBlank()?.let(pressure),
                unit,
            )
        },
        onDismiss = onDismiss,
    ) {
        val error = stringResource(R.string.tires_size_error)
        CheckedField(front, { front = it }, stringResource(if (differ) R.string.front else R.string.tires_size), error, VehicleSpecEntry::tireSize, supporting = stringResource(R.string.tires_size_note))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = differ, role = Role.Switch, onValueChange = { differ = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.tires_rear_differs), Modifier.weight(1f))
            Switch(checked = differ, onCheckedChange = null)
        }
        if (differ) CheckedField(rear, { rear = it }, stringResource(R.string.rear), error, VehicleSpecEntry::tireSize)
        SectionLabel(stringResource(R.string.tires_pressure_section))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to stringResource(R.string.unit_psi), true to stringResource(R.string.unit_kpa)).forEach { (k, symbol) -> FilterChip(selected = kpa == k, onClick = { kpa = k }, label = { Text(symbol) }) }
        }
        val pError = stringResource(R.string.tires_pressure_error, stringResource(unit.word), unit.range.first.asNumber(), unit.range.last.asNumber())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckedField(pFront, { pFront = it }, stringResource(R.string.front), pError, pressure, modifier = Modifier.weight(1f), keyboard = KeyboardType.Number)
            CheckedField(pRear, { pRear = it }, stringResource(R.string.rear), pError, pressure, modifier = Modifier.weight(1f), keyboard = KeyboardType.Number)
        }
        CheckedField(pSpare, { pSpare = it }, stringResource(R.string.tires_spare), pError, pressure, keyboard = KeyboardType.Number)
    }
}

@Composable
private fun WipersDialog(v: VehicleEntity, onSave: (Int?, Int?, Int?, String?) -> Unit, onDismiss: () -> Unit) {
    var driver by rememberSaveable { mutableStateOf(v.wiperDriverIn?.toString().orEmpty()) }
    var passenger by rememberSaveable { mutableStateOf(v.wiperPassengerIn?.toString().orEmpty()) }
    var rear by rememberSaveable { mutableStateOf(v.wiperRearIn?.toString().orEmpty()) }
    var note by rememberSaveable { mutableStateOf(v.wiperBlades.orEmpty()) }
    val valid = listOf(driver, passenger, rear).all { it.isBlank() || VehicleSpecEntry.wiperLength(it) != null } && note.trim().length <= VehicleText.MAX_SPEC
    SpecEditor(
        title = stringResource(R.string.takes_wipers),
        valid = valid,
        onSave = {
            onSave(
                driver.orNullIfBlank()?.let(VehicleSpecEntry::wiperLength),
                passenger.orNullIfBlank()?.let(VehicleSpecEntry::wiperLength),
                rear.orNullIfBlank()?.let(VehicleSpecEntry::wiperLength),
                VehicleText.clean(note, VehicleText.MAX_SPEC),
            )
        },
        onDismiss = onDismiss,
    ) {
        val error = stringResource(R.string.wipers_error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckedField(driver, { driver = it }, stringResource(R.string.wipers_driver), error, VehicleSpecEntry::wiperLength, modifier = Modifier.weight(1f), keyboard = KeyboardType.Number)
            CheckedField(passenger, { passenger = it }, stringResource(R.string.wipers_passenger), error, VehicleSpecEntry::wiperLength, modifier = Modifier.weight(1f), keyboard = KeyboardType.Number)
        }
        CheckedField(rear, { rear = it }, stringResource(R.string.wipers_rear), error, VehicleSpecEntry::wiperLength, keyboard = KeyboardType.Number)
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= VehicleText.MAX_SPEC) note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.wipers_parts)) },
            singleLine = true,
        )
    }
}

/**
 * Puts [text] on the clipboard. A [sensitive] one (the VIN) is marked so Android hides it in its clipboard
 * preview. Android 13 and later say "Copied" themselves; earlier versions get a short message here.
 */
private const val IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

internal fun copyText(context: Context, label: String, text: String, sensitive: Boolean) {
    val clip = ClipData.newPlainText(label, text).apply {
        // ClipDescription.EXTRA_IS_SENSITIVE is API 33; before that the same key, spelled out, is what keyboards
        // and clipboard tools read.
        if (sensitive) description.extras = PersistableBundle().apply { putBoolean(IS_SENSITIVE, true) }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, context.getString(R.string.copied, label), Toast.LENGTH_SHORT).show()
}

/** The capacity unit, as the error says it. */
private val VehicleSpecEntry.Volume.word: Int
    get() = when (this) {
        VehicleSpecEntry.Volume.QUARTS -> R.string.unit_qt
        VehicleSpecEntry.Volume.LITRES -> R.string.unit_litre
    }

private val VehicleSpecEntry.Pressure.word: Int
    get() = when (this) {
        VehicleSpecEntry.Pressure.PSI -> R.string.unit_psi
        VehicleSpecEntry.Pressure.KPA -> R.string.unit_kpa
    }
