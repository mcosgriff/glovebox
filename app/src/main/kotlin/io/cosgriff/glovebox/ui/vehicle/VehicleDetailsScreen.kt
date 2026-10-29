package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.domain.VehicleBulletins
import io.cosgriff.glovebox.ui.bulletins.BulletinCatalog
import io.cosgriff.glovebox.ui.components.BackToTop
import io.cosgriff.glovebox.ui.components.BackToTopRoom
import io.cosgriff.glovebox.ui.service.editableDecimal
import io.cosgriff.glovebox.ui.screenState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.cosgriff.glovebox.data.log.highestReading
import io.cosgriff.glovebox.ui.service.nicknameOrModel
import io.cosgriff.glovebox.ui.components.DangerTextButton
import io.cosgriff.glovebox.ui.components.InkOutlinedButton
import io.cosgriff.glovebox.ui.components.DoubleCheckTag
import io.cosgriff.glovebox.ui.components.InkChip
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.ui.components.orNullIfBlank
import io.cosgriff.glovebox.ui.components.copyText
import io.cosgriff.glovebox.ui.components.CheckedField
import io.cosgriff.glovebox.ui.components.Chevron
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.VehicleCheckIn
import io.cosgriff.glovebox.ui.settings.CheckInDialog
import io.cosgriff.glovebox.ui.settings.checkInLabel
import io.cosgriff.glovebox.ui.service.typedDecimal
import io.cosgriff.glovebox.domain.DistanceKind
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.longDate
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.ui.service.line
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.offsetLabel
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.pluralWord
import androidx.compose.foundation.combinedClickable
import io.cosgriff.glovebox.ui.service.whatItTakes
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.domain.TakesItem
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.flow.flow
import io.cosgriff.glovebox.ui.components.openWebPage
import io.cosgriff.glovebox.domain.MakerDocuments
import io.cosgriff.glovebox.domain.MakerDocumentList
import androidx.compose.material3.Switch
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.input.KeyboardType
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import io.cosgriff.glovebox.domain.DistanceConversion
import io.cosgriff.glovebox.data.db.UnitDao
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.export.ExportSheet
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.cosgriff.glovebox.ui.route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.domain.VinRefresh
import io.cosgriff.glovebox.ui.components.DecodedField
import io.cosgriff.glovebox.ui.components.savedFields
import io.cosgriff.glovebox.ui.components.storedFields
import io.cosgriff.glovebox.data.photo.VehiclePhotoStore
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.domain.ScheduleSummaries
import io.cosgriff.glovebox.domain.ScheduleSummary
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.domain.BodyStyles
import io.cosgriff.glovebox.domain.Powertrains
import io.cosgriff.glovebox.ui.components.label
import io.cosgriff.glovebox.domain.Vin
import io.cosgriff.glovebox.ui.VehicleDetailsRoute
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.data.log.clusterSwap
import io.cosgriff.glovebox.data.log.ownOil
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.ui.components.OilChange
import io.cosgriff.glovebox.ui.components.OilIntervalSheet
import io.cosgriff.glovebox.ui.components.oilChange
import io.cosgriff.glovebox.ui.components.oilChangeLine
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
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
    /** The vehicle's oil change as its schedule gives it (#517); null when it has none, and then there is no "Oil change" row. */
    val oil: OilChange? = null,
)

/** Looking the saved VIN up again (#462). */
sealed interface VinLookup {
    data object Idle : VinLookup
    data object Asking : VinLookup
    data object Failed : VinLookup

    /** NHTSA decodes the VIN as another year or make; [title] is what it says, null if it couldn't say. */
    data class NotThisVehicle(val title: String?) : VinLookup

    /** What NHTSA has that is empty here; nothing else is ever offered. */
    data class Found(val additions: Map<DecodedField, String>) : VinLookup
}

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
    private val knownTakes: KnownTakesRows,
    makerDocuments: MakerDocumentCatalog,
    bulletinCatalog: BulletinCatalog,
    settings: SettingsRepository,
    private val nhtsa: NhtsaClient,
) : ViewModel() {
    val vehicleId = savedStateHandle.route<VehicleDetailsRoute>().vehicleId

    /** A maker's list for this vehicle's "What it takes", and the rows filled from one still to check. */
    val known: StateFlow<TakesKnown> = knownTakes.observe(vehicles.observe(vehicleId))
        .screenState(viewModelScope, TakesKnown())

    /** The maker's own manual and guides for this vehicle's make, model and year, to open in the browser. */
    val makerDocs: StateFlow<MakerDocumentList?> = combine(vehicles.observe(vehicleId), flow { emit(makerDocuments.lists()) }) { v, lists ->
        v?.let { MakerDocuments.forVehicle(lists, it.year, it.make, it.model) }
    }.screenState(viewModelScope, null)

    /** The maker's free-repair bulletins bundled for this vehicle's year, make and model (#459); null for a model without a list. */
    val bulletins: StateFlow<VehicleBulletins?> = bulletinCatalog.forVehicle(vehicles.observe(vehicleId)).screenState(viewModelScope, null)

    fun fillKnown() = viewModelScope.launch { details.value?.vehicle?.let { knownTakes.fill(it) } }

    /** The row as it was before [item]'s editor saved [after]; [checked] is the owner saying they checked it against their manual. */
    /** [item]'s row as [change] leaves it, written whole, with its DOUBLE-CHECK tag settled. */
    private suspend fun saveTakes(item: TakesItem, change: (WhatItTakes) -> WhatItTakes, checked: Boolean) {
        val v = details.value?.vehicle ?: return
        knownTakes.save(v, item, change(v.whatItTakes()), checked)
    }

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
            oil = items.oilChange(vehicle.unit),
        )
    }.screenState(viewModelScope, null)

    /** Decrypted in memory only while this screen is open. */
    val photo: StateFlow<ImageBitmap?> = vehicles.observe(vehicleId)
        .map { it?.photoFile }
        .distinctUntilChanged()
        .mapLatest { name -> name?.let { photos.load(it) }?.asImageBitmap() }
        .screenState(viewModelScope, null)

    val photoFailed: StateFlow<Boolean> = picker.failedFor.map { it == vehicleId }
        .screenState(viewModelScope, false)

    /** The highest reading so far, which a sale's final odometer starts from. */
    val lastOdometer: StateFlow<Int?> = combine(
        vehicles.observe(vehicleId),
        service.observeRecords(vehicleId),
        odometer.observe(vehicleId),
    ) { vehicle, records, readings ->
        vehicle?.highestReading(records, readings)
    }.screenState(viewModelScope, null)

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

    /** The mileage check-in every vehicle without one of its own gets, and whether check-ins are on at all. */
    val noticePrefs: StateFlow<NoticePrefs?> = settings.settings.map { it.notices }
        .screenState(viewModelScope, null)

    fun setCheckIn(checkIn: VehicleCheckIn) = viewModelScope.launch { vehicles.setMileageCheckIn(vehicleId, checkIn.stored) }

    /** [plate] is already cleaned (LicensePlates.clean); null clears it. */
    fun savePlate(plate: String?, region: String?) = viewModelScope.launch { vehicles.setPlate(vehicleId, plate, region) }

    /** Each is already checked and in its standard form (VehicleSpecEntry); null clears it. */
    fun saveOil(grade: String?, approval: String?, capacity: Double?, unit: VehicleSpecEntry.Volume?, filter: String?, checked: Boolean) = viewModelScope.launch {
        saveTakes(TakesItem.OIL, { it.copy(oilGrade = grade, oilApproval = approval, oilCapacity = capacity, oilCapacityUnit = unit, oilFilter = filter) }, checked)
    }

    fun saveTires(front: String?, rear: String?, pFront: Int?, pRear: Int?, pSpare: Int?, unit: VehicleSpecEntry.Pressure, checked: Boolean) = viewModelScope.launch {
        saveTakes(
            TakesItem.TIRES,
            { it.copy(tireSize = front, tireSizeRear = rear, tirePressureFront = pFront, tirePressureRear = pRear, tirePressureSpare = pSpare, tirePressureUnit = unit) },
            checked,
        )
    }

    fun saveWipers(driver: Int?, passenger: Int?, rear: Int?, note: String?, checked: Boolean) = viewModelScope.launch {
        saveTakes(TakesItem.WIPERS, { it.copy(wiperDriver = driver, wiperPassenger = passenger, wiperRear = rear, wiperNote = note) }, checked)
    }

    /** Null removes it. Readings are stored as typed, so nothing else has to be rewritten. */
    /** Changes what the odometer counts in, converting every figure stored for the vehicle with it. */
    fun convertTo(unit: DistanceUnit) = viewModelScope.launch { units.convert(vehicleId, unit) }

    /** The owner's own oil change interval (#517), as typed in the vehicle's unit; null is "Use the maker's". */
    fun setOwnOil(own: Interval?) = viewModelScope.launch { vehicles.setOwnOil(vehicleId, own?.miles, own?.months) }

    fun setClusterSwap(swap: ClusterSwap?) = viewModelScope.launch {
        vehicles.setClusterSwap(vehicleId, swap?.offset ?: 0, swap?.on, swap?.startReading)
    }

    fun pickPhoto() = picker.pick(vehicleId)

    /** For a vehicle added without one. The details the user entered are kept as they are. */
    fun setVin(vin: String) = viewModelScope.launch { vehicles.setVin(vehicleId, vin) }

    private val _vinLookup = MutableStateFlow<VinLookup>(VinLookup.Idle)
    val vinLookup: StateFlow<VinLookup> = _vinLookup

    /** Only the VIN is sent. The drivetrain is never offered: it is the owner's pick (docs/rules/vehicles.md). */
    fun lookUpVin() {
        val v = details.value?.vehicle ?: return
        val vin = v.vin ?: return
        _vinLookup.value = VinLookup.Asking
        viewModelScope.launch {
            _vinLookup.value = runCatching { nhtsa.decodeVin(vin) }.fold(
                onSuccess = { d ->
                    if (VinRefresh.sameVehicle(v.year, v.make, d.year, d.make)) {
                        VinLookup.Found(VinRefresh.additions(v.savedFields(), d.storedFields() - DecodedField.DRIVE))
                    } else {
                        VinLookup.NotThisVehicle(d.year?.let { VehicleText.title(it, d.make.orEmpty(), d.model.orEmpty(), trim = null) })
                    }
                },
                onFailure = { VinLookup.Failed },
            )
        }
    }

    /** Fills only what is still empty, so an edit made while the dialog was open stands. */
    fun fillFromVin() {
        val found = (_vinLookup.value as? VinLookup.Found)?.additions ?: return
        _vinLookup.value = VinLookup.Idle
        viewModelScope.launch {
            vehicles.fillFromVin(
                vehicleId,
                bodyClass = found[DecodedField.BODY],
                engine = found[DecodedField.ENGINE],
                fuel = found[DecodedField.FUEL],
                transmission = found[DecodedField.TRANSMISSION],
                engineCode = found[DecodedField.ENGINE_CODE],
                tpms = found[DecodedField.TPMS],
                series = found[DecodedField.SERIES],
            )
        }
    }

    fun closeVinLookup() {
        _vinLookup.value = VinLookup.Idle
    }

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
    /** The maker's bulletins for the vehicle's year (#459). */
    onOpenBulletins: (vehicleId: Long) -> Unit,
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
    val lastOdometer by vm.lastOdometer.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<TakesItem?>(null) }
    val known by vm.known.collectAsStateWithLifecycle()
    val makerDocs by vm.makerDocs.collectAsStateWithLifecycle()
    val bulletins by vm.bulletins.collectAsStateWithLifecycle()
    var selling by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var editingVin by rememberSaveable { mutableStateOf(false) }
    var editingPlate by rememberSaveable { mutableStateOf(false) }
    var editingCheckIn by rememberSaveable { mutableStateOf(false) }
    val noticePrefs by vm.noticePrefs.collectAsStateWithLifecycle()
    var settingOffset by rememberSaveable { mutableStateOf(false) }
    var settingOil by rememberSaveable { mutableStateOf(false) }
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

    val oil = details.oil
    if (settingOil && oil != null) {
        OilIntervalSheet(
            oil = oil,
            own = v.ownOil(),
            unit = v.unit,
            onSave = {
                vm.setOwnOil(it)
                settingOil = false
            },
            onDismiss = { settingOil = false },
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

    if (editingCheckIn) {
        noticePrefs?.let { prefs ->
            CheckInDialog(
                current = VehicleCheckIn.of(v.mileageCheckInDays),
                default = prefs.mileageEveryDays,
                onSave = {
                    vm.setCheckIn(it)
                    editingCheckIn = false
                },
                onDismiss = { editingCheckIn = false },
            )
        }
    }

    when (val lookup = vm.vinLookup.collectAsStateWithLifecycle().value) {
        VinLookup.Idle -> Unit
        else -> VinLookupDialog(lookup, onAdd = vm::fillFromVin, onDismiss = vm::closeVinLookup)
    }

    if (editingVin) {
        VinDialog(
            saved = v.vin,
            onSave = {
                vm.setVin(it)
                editingVin = false
            },
            onDismiss = { editingVin = false },
        )
    }
    val name = v.nicknameOrModel()

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
        TakesItem.OIL -> OilDialog(v, TakesItem.OIL in known.toCheck, onSave = { g, a, c, u, f, ok -> vm.saveOil(g, a, c, u, f, ok); editing = null }, onDismiss = { editing = null })
        TakesItem.TIRES -> TiresDialog(
            v, TakesItem.TIRES in known.toCheck, known.list?.tireSizes.orEmpty(),
            onSave = { f, r, pf, pr, ps, u, ok -> vm.saveTires(f, r, pf, pr, ps, u, ok); editing = null },
            onDismiss = { editing = null },
        )
        TakesItem.WIPERS -> WipersDialog(v, TakesItem.WIPERS in known.toCheck, onSave = { d, p, r, n, ok -> vm.saveWipers(d, p, r, n, ok); editing = null }, onDismiss = { editing = null })
        null -> Unit
        else -> Unit // the rest are edited on their own page
    }
    VehicleDetailsContent(
        details, photo, photoFailed, known, makerDocs, nickname, noticePrefs ?: NoticePrefs(),
        onNicknameChange = {
            nickname = it
            vm.rename(it)
        },
        onBack = onBack, onOpenRecalls = onOpenRecalls, onOpenSchedule = onOpenSchedule, onDelete = onDelete,
        onOpenTorqueSpecs = onOpenTorqueSpecs, onOpenMoreTakes = onOpenMoreTakes, onOpenReadings = onOpenReadings,
        onOpenImport = onOpenImport, onOpenDocuments = onOpenDocuments, onClearHistory = onClearHistory,
        onEditTakes = { editing = it }, onFillKnown = vm::fillKnown, onEditPlate = { editingPlate = true },
        onEditCheckIn = { editingCheckIn = true }, onEditVin = { editingVin = true }, onLookUpVin = vm::lookUpVin, onSetOffset = { settingOffset = true }, onSetOil = { settingOil = true }, onChangeUnit = { changingUnit = true },
        onSell = { selling = true }, onPickPhoto = vm::pickPhoto, onRemovePhoto = vm::removePhoto,
        bulletins = bulletins, onOpenBulletins = onOpenBulletins,
    )
}

/** Vehicle details as given, with no ViewModel: what [VehicleDetailsScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun VehicleDetailsContent(
    details: VehicleDetails,
    photo: ImageBitmap?,
    photoFailed: Boolean,
    known: TakesKnown,
    makerDocs: MakerDocumentList?,
    nickname: String,
    /** For the mileage check-in row: the default it may follow, and whether check-ins are on at all. */
    noticePrefs: NoticePrefs,
    onNicknameChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenRecalls: (vehicleId: Long) -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    onDelete: (vehicleId: Long) -> Unit,
    onOpenTorqueSpecs: (vehicleId: Long) -> Unit,
    onOpenMoreTakes: (vehicleId: Long) -> Unit,
    onOpenReadings: (vehicleId: Long) -> Unit,
    onOpenImport: (vehicleId: Long) -> Unit,
    onOpenDocuments: (vehicleId: Long) -> Unit,
    onClearHistory: (vehicleId: Long) -> Unit,
    onEditTakes: (TakesItem) -> Unit,
    onFillKnown: () -> Unit,
    onEditPlate: () -> Unit,
    onEditCheckIn: () -> Unit,
    /** Add the VIN, or correct the one saved. */
    onEditVin: () -> Unit,
    /** Look the saved VIN up again, to fill what is empty. */
    onLookUpVin: () -> Unit,
    onSetOffset: () -> Unit,
    /** The owner's own oil change interval (#517). */
    onSetOil: () -> Unit,
    onChangeUnit: () -> Unit,
    onSell: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    /** Null for a model Glovebox has no bulletins for; then there is no row. */
    bulletins: VehicleBulletins? = null,
    onOpenBulletins: (vehicleId: Long) -> Unit = {},
) {
    val v = details.vehicle
    val swap = v.clusterSwap()
    val takes = v.whatItTakes()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            ScreenHeader(stringResource(R.string.home_vehicle_details), onBack)

            Column(
                Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp + BackToTopRoom),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PhotoBox(photo, hasPhoto = v.photoFile != null, name = v.nicknameOrModel(), onPick = onPickPhoto, onRemove = onRemovePhoto)
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
                    onValueChange = { onNicknameChange(it.take(VehicleText.MAX_NICKNAME)) },
                    label = { Text(stringResource(R.string.details_nickname)) },
                    placeholder = { Text(v.model) },
                    supportingText = { Text(stringResource(R.string.details_nickname_note)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                RowsCard {
                    val plate = LicensePlates.label(v.plate, v.plateRegion)
                    DetailRow(stringResource(R.string.details_plate), plate, mono = v.plate != null, onClick = onEditPlate)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(if (v.enteredByHand) R.string.details_section_vehicle else R.string.details_section_from_vin))
                    RowsCard {
                        // A tap adds the VIN or corrects it (#468). It is the one value worth having elsewhere (an
                        // insurer's or a parts site's form), so a long press copies it; it is the one row in mono.
                        val vinLabel = stringResource(R.string.details_vin)
                        DetailRow(
                            vinLabel,
                            v.vin,
                            mono = true,
                            onClick = onEditVin,
                            onLongClickLabel = v.vin?.let { stringResource(R.string.details_copy_vin) },
                            onLongClick = v.vin?.let { vin -> { copyText(context, vinLabel, vin, sensitive = true); haptics.performHapticFeedback(HapticFeedbackType.LongPress) } },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val rows = listOfNotNull(
                            stringResource(R.string.details_vehicle) to (VehicleText.title(v.year, v.make, v.model, v.trim) ?: stringResource(R.string.unknown_vehicle)),
                            v.drivetrain?.let { stringResource(R.string.details_drivetrain) to it },
                            v.transmission?.let { stringResource(R.string.add_vehicle_transmission) to it },
                            v.engine?.let { stringResource(R.string.add_vehicle_engine) to it },
                            v.engineCode?.let { stringResource(R.string.add_vehicle_engine_code) to it },
                            Powertrains.of(v.powertrain)?.let { stringResource(R.string.add_vehicle_powertrain) to stringResource(it.label()) },
                            v.bodyClass?.let { stringResource(R.string.details_body) to it },
                            BodyStyles.of(v.bodyStyle)?.let { stringResource(R.string.add_vehicle_body_style) to stringResource(it.label()) },
                            v.series?.let { stringResource(R.string.add_vehicle_series) to it },
                            v.tpms?.let { stringResource(R.string.add_vehicle_tpms) to it },
                        )
                        rows.forEach { (label, value) ->
                            DetailRow(label, value)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        if (v.vin != null) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onLookUpVin),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    Text(
                                        stringResource(R.string.details_vin_lookup),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        stringResource(R.string.details_vin_lookup_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Chevron()
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
                    RowsCard {
                        takes.basics().forEachIndexed { i, spec ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val value = takes.line(spec)
                            // A long press copies the row's part numbers, for a parts site's search box.
                            val parts = takes.partNumbers(spec)
                            val partsLabel = stringResource(R.string.details_part_numbers)
                            DetailRow(
                                spec.label,
                                value,
                                onClick = { onEditTakes(spec) },
                                onLongClickLabel = parts?.let { stringResource(R.string.details_copy_parts) },
                                onLongClick = parts?.let { p -> { copyText(context, partsLabel, p, sensitive = false); haptics.performHapticFeedback(HapticFeedbackType.LongPress) } },
                                under = if (spec in known.toCheck) ({ DoubleCheckTag() }) else null,
                            )
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
                        DetailRow(
                            stringResource(R.string.offset_title),
                            swap?.let { offsetLabel(it.offset, v.unit) } ?: stringResource(R.string.none),
                            mono = swap != null,
                            onClick = onSetOffset,
                        )
                    }
                    known.toCheckFrom?.let { KnownTakesNote(it) }
                    known.list?.let { list -> known.offer?.let { plan -> KnownTakesOffer(list, plan, onFill = onFillKnown, Modifier.padding(top = 4.dp)) } }
                    Text(
                        stringResource(R.string.details_takes_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                makerDocs?.let { maker ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.details_section_maker))
                        RowsCard {
                            maker.documents.forEachIndexed { i, doc ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                val res = LocalResources.current
                                val vinLabel = stringResource(R.string.details_vin)
                                LinkRow(
                                    title = stringResource(doc.titleRes),
                                    detail = when {
                                        doc.vinLookup -> stringResource(if (v.vin != null) R.string.maker_doc_vin_lookup else R.string.maker_doc_vin_lookup_none, maker.make)
                                        else -> {
                                            val code = doc.code.orEmpty().let { c -> doc.edition?.let { stringResource(R.string.maker_doc_code_edition, c, it) } ?: c }
                                            stringResource(if (doc.viaNhtsa) R.string.maker_doc_from_nhtsa else R.string.maker_doc_from, maker.make, code)
                                        }
                                    },
                                    onClick = {
                                        // The VIN never goes in the link: it is copied, marked sensitive, for the owner to paste on the maker's page.
                                        if (doc.vinLookup) v.vin?.let { copyText(context, vinLabel, it, sensitive = true) }
                                        if (!context.openWebPage(doc.url)) Toast.makeText(context, res.getString(R.string.licences_no_browser, doc.url), Toast.LENGTH_LONG).show()
                                    },
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.details_maker_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                RowsCard {
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
                    details.oil?.let { oil ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        LinkRow(
                            title = stringResource(R.string.oil_title),
                            detail = oilChangeLine(oil, v.ownOil(), v.unit),
                            onClick = onSetOil,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val open = details.recalls
                    LinkRow(
                        title = stringResource(R.string.details_recalls),
                        detail = when {
                            open == null -> stringResource(R.string.details_recalls_not_checked)
                            open.count == 0 -> stringResource(R.string.no_open_recalls)
                            open.doNotDrive -> stringResource(R.string.details_recalls_do_not_drive, open.count.asNumber())
                            open.parkOutside -> stringResource(R.string.details_recalls_park_outside, open.count.asNumber())
                            else -> stringResource(R.string.details_recalls_open, open.count.asNumber())
                        },
                        detailColor = when {
                            open == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            open.count == 0 -> statusColors().ok
                            open.doNotDrive || open.parkOutside -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        onClick = { onOpenRecalls(v.id) },
                    )
                    bulletins?.let { b ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        LinkRow(
                            title = stringResource(R.string.details_bulletins),
                            detail = if (b.bulletins.isEmpty()) {
                                stringResource(R.string.details_bulletins_none, b.asOf.format(longDate))
                            } else {
                                pluralStringResource(R.plurals.details_bulletins_count, b.bulletins.size, b.bulletins.size.asNumber(), b.asOf.format(longDate))
                            },
                            onClick = { onOpenBulletins(v.id) },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LinkRow(
                        title = stringResource(R.string.details_units),
                        detail = stringResource(R.string.details_units_detail, v.unit.titleWord),
                        onClick = onChangeUnit,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LinkRow(
                        title = stringResource(R.string.readings_title),
                        detail = stringResource(R.string.details_readings_detail),
                        onClick = { onOpenReadings(v.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LinkRow(
                        title = stringResource(R.string.channel_mileage),
                        detail = when {
                            !noticePrefs.mileage -> stringResource(R.string.details_checkin_all_off)
                            else -> when (val own = VehicleCheckIn.of(v.mileageCheckInDays)) {
                                VehicleCheckIn.Default -> stringResource(R.string.checkin_as_default, checkInLabel(noticePrefs.mileageEveryDays))
                                VehicleCheckIn.Off -> stringResource(R.string.details_checkin_off)
                                is VehicleCheckIn.Every -> checkInLabel(own.days)
                            }
                        },
                        onClick = onEditCheckIn,
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
                    InkOutlinedButton(onClick = onSell, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.details_mark_sold))
                    }
                    Text(
                        stringResource(R.string.details_sold_note),
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    DangerTextButton(onClick = { onDelete(v.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.details_delete), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        BackToTop(scrollState, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
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
    val shape = MaterialTheme.shapes.large
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

/** [onClick] null shows the row as information only, without a chevron. */
/**
 * One of this screen's label-and-value rows (#479): the label grey at the start, the value semibold at the end,
 * at least 48dp tall. A null [value] reads "Add", in the accent; [mono] sets a value that is there in Plex Mono.
 * With [onClick] the row is a button ending in a [Chevron], and [onLongClick] (named by [onLongClickLabel] for
 * TalkBack) copies something. [under] goes under the label: the DOUBLE-CHECK tag.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String?,
    mono: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    under: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(role = Role.Button, onClick = onClick, onLongClickLabel = onLongClickLabel, onLongClick = onLongClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            under?.invoke()
        }
        Text(
            value ?: stringResource(R.string.add),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono && value != null) PlexMono else null,
            color = if (value == null) MaterialTheme.colorScheme.primary else Color.Unspecified,
            textAlign = TextAlign.End,
        )
        if (onClick != null) Chevron()
    }
}

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

/**
 * Adds a VIN, or corrects the [saved] one (#468). Only the VIN is written: nothing is decoded from it, and nothing is
 * sent. It can't be cleared, and a wrong check digit only warns ([Vin.canSave]).
 */
@Composable
private fun VinDialog(saved: String?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(saved.orEmpty()) }
    val plausible = Vin.isPlausible(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (saved == null) R.string.details_add_vin else R.string.details_change_vin)) },
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
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = Vin.canSave(text, saved)) { Text(stringResource(R.string.save)) } },
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

/** Save is offered only when every filled-in field is valid. */
@Composable
internal fun SpecEditor(
    title: String,
    valid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    /** Set for a row filled from a maker's list and not yet checked: the owner can say they have checked it. */
    checkedAgainstManual: MutableState<Boolean>? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
                checkedAgainstManual?.let { CheckedAgainstManual(it) }
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

@Composable
private fun OilDialog(v: VehicleEntity, toCheck: Boolean, onSave: (String?, String?, Double?, VehicleSpecEntry.Volume?, String?, Boolean) -> Unit, onDismiss: () -> Unit) {
    val checked = rememberSaveable { mutableStateOf(false) }
    var grade by rememberSaveable { mutableStateOf(v.oilSpec.orEmpty()) }
    var approval by rememberSaveable { mutableStateOf(v.oilApproval.orEmpty()) }
    var capacity by rememberSaveable { mutableStateOf(v.oilCapacity?.editableDecimal().orEmpty()) }
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
                checked.value,
            )
        },
        onDismiss = onDismiss,
        checkedAgainstManual = checked.takeIf { toCheck },
    ) {
        CheckedField(grade, { grade = it }, stringResource(R.string.oil_grade), stringResource(R.string.oil_grade_error), VehicleSpecEntry::oilGrade, supporting = stringResource(R.string.oil_grade_note))
        // The common grades a tap away.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VehicleSpecEntry.commonGrades.forEach { g ->
                InkChip(g, selected = VehicleSpecEntry.oilGrade(grade) == g, mono = true) { grade = g }
            }
        }
        CheckedField(approval, { approval = it }, stringResource(R.string.oil_approval), stringResource(R.string.oil_approval_error), VehicleSpecEntry::oilApproval, supporting = stringResource(R.string.oil_approval_note))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckedField(
                capacity, { capacity = it }, stringResource(R.string.oil_capacity), stringResource(R.string.oil_capacity_error, unit.max.toInt().asNumber(), stringResource(unit.word)),
                { VehicleSpecEntry.oilCapacity(it.typedDecimal(), unit) }, modifier = Modifier.weight(1f), keyboard = KeyboardType.Decimal,
            )
            listOf(false to stringResource(R.string.unit_qt), true to stringResource(R.string.unit_litre)).forEach { (l, symbol) ->
                InkChip(symbol, selected = litres == l) { litres = l }
            }
        }
        CheckedField(
            filter, { filter = it }, stringResource(R.string.oil_filter), stringResource(R.string.oil_filter_error),
            VehicleSpecEntry::partNumbers, supporting = stringResource(R.string.oil_filter_note),
        )
    }
}

@Composable
private fun TiresDialog(
    v: VehicleEntity,
    toCheck: Boolean,
    /** The sizes a maker's list gives for the model, to pick from; it can't know which the vehicle has. */
    listedSizes: List<String>,
    onSave: (String?, String?, Int?, Int?, Int?, VehicleSpecEntry.Pressure, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val checked = rememberSaveable { mutableStateOf(false) }
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
                checked.value,
            )
        },
        onDismiss = onDismiss,
        checkedAgainstManual = checked.takeIf { toCheck },
    ) {
        val error = stringResource(R.string.tires_size_error)
        CheckedField(front, { front = it }, stringResource(if (differ) R.string.front else R.string.tires_size), error, VehicleSpecEntry::tireSize, supporting = stringResource(R.string.tires_size_note))
        if (listedSizes.isNotEmpty()) {
            Text(stringResource(R.string.known_takes_sizes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listedSizes.forEach { size ->
                    InkChip(size, selected = VehicleSpecEntry.tireSize(front) == size, mono = true) { front = size }
                }
            }
        }
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
            listOf(false to stringResource(R.string.unit_psi), true to stringResource(R.string.unit_kpa)).forEach { (k, symbol) -> InkChip(symbol, selected = kpa == k) { kpa = k } }
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
private fun WipersDialog(v: VehicleEntity, toCheck: Boolean, onSave: (Int?, Int?, Int?, String?, Boolean) -> Unit, onDismiss: () -> Unit) {
    val checked = rememberSaveable { mutableStateOf(false) }
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
                checked.value,
            )
        },
        onDismiss = onDismiss,
        checkedAgainstManual = checked.takeIf { toCheck },
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

/** What looking the VIN up again found, and the one way to use it: fill what is empty. */
@Composable
private fun VinLookupDialog(lookup: VinLookup, onAdd: () -> Unit, onDismiss: () -> Unit) {
    val additions = (lookup as? VinLookup.Found)?.additions.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_vin_lookup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (lookup) {
                    VinLookup.Asking -> Text(stringResource(R.string.details_vin_lookup_asking))
                    VinLookup.Failed -> Text(stringResource(R.string.details_vin_lookup_failed))
                    is VinLookup.NotThisVehicle -> Text(
                        lookup.title?.let { stringResource(R.string.details_vin_lookup_other, it) } ?: stringResource(R.string.details_vin_lookup_unidentified),
                    )
                    is VinLookup.Found -> if (additions.isEmpty()) {
                        Text(stringResource(R.string.details_vin_lookup_nothing))
                    } else {
                        Text(stringResource(R.string.details_vin_lookup_found))
                        additions.forEach { (field, value) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(field.label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                            }
                        }
                    }
                    VinLookup.Idle -> Unit
                }
            }
        },
        confirmButton = {
            if (additions.isNotEmpty()) {
                TextButton(onClick = onAdd) { Text(stringResource(R.string.details_vin_lookup_add)) }
            } else if (lookup != VinLookup.Asking) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        },
        dismissButton = if (additions.isNotEmpty() || lookup == VinLookup.Asking) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        } else {
            null
        },
    )
}
