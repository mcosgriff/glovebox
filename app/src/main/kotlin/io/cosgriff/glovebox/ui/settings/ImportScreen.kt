package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.components.DangerTextButton
import io.cosgriff.glovebox.ui.components.PrimaryButton
import io.cosgriff.glovebox.ui.components.CheckRow
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.LabelledCard
import io.cosgriff.glovebox.ui.components.RowsCard
import io.cosgriff.glovebox.domain.CarfaxRefusal
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.ui.service.ruleInSentence
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.torque.text
import io.cosgriff.glovebox.ui.schedule.text
import io.cosgriff.glovebox.ui.schedule.repeatNote
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.orText
import java.time.LocalDate
import io.cosgriff.glovebox.domain.CarfaxReport
import io.cosgriff.glovebox.domain.FileDistances
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import io.cosgriff.glovebox.ui.route
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.data.db.MilestoneDao
import io.cosgriff.glovebox.data.db.MilestoneEntity
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.SavedMilestone
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.SavedRecord
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.ImportFormat
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.SavedScheduleItem
import io.cosgriff.glovebox.domain.SavedTorqueSpec
import io.cosgriff.glovebox.domain.ScheduleImport
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueImport
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.clusterSwap
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toReadings
import io.cosgriff.glovebox.ui.service.toSaved
import io.cosgriff.glovebox.ui.service.toTracked
import io.cosgriff.glovebox.ui.ImportRoute
import io.cosgriff.glovebox.ui.theme.PlexMono
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import io.cosgriff.glovebox.ui.service.answering
import io.cosgriff.glovebox.ui.service.toRef
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.VehicleDocuments
import javax.inject.Inject
import javax.inject.Singleton

/** An import file the user picked: its name and what it says. Held in memory only, never written anywhere. */
/**
 * [file] is set when the pick was read already, as a Carfax report PDF is; otherwise [text] is parsed as an import file.
 * [pdf] is the report itself, held in memory only so that it can be kept in the vehicle's documents if the user asks.
 */
data class PickedImport(val name: String, val text: String, val file: ImportFile? = null, val pdf: ByteArray? = null)

/**
 * Lets the user pick an import file with Android's file picker. A singleton with its launcher in
 * the activity, like the other pickers: choosing a file leaves the app, which can lock it.
 */
@Singleton
class ImportPicker @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var launch: (() -> Unit)? = null

    private val _picked = MutableStateFlow<PickedImport?>(null)
    val picked: StateFlow<PickedImport?> = _picked.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    fun attach(launcher: () -> Unit) {
        launch = launcher
    }

    fun detach(launcher: () -> Unit) {
        if (launch === launcher) launch = null
    }

    fun pick() {
        _problem.value = null
        launch?.invoke()
    }

    /** Null when the user backed out. */
    fun onPicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            // A PDF is read as a Carfax report: its service visits become a service-history import.
            val bytes = withContext(Dispatchers.IO) { readBytes(uri, PDF_MAX_BYTES) }
            if (bytes != null && bytes.size >= 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF") {
                if (bytes.size > PDF_MAX_BYTES) {
                    _problem.value = AppText.res.getString(R.string.import_pdf_too_big)
                    return@launch
                }
                val file = withContext(Dispatchers.Default) {
                    PdfWords.read(context, bytes)?.let { CarfaxReport.read(it, LocalDate.now()) }
                }
                if (file == null) _problem.value = AppText.res.getString(R.string.import_pdf_unreadable)
                else _picked.value = PickedImport(displayName(uri) ?: AppText.res.getString(R.string.import_carfax_report), "", file, pdf = bytes)
                return@launch
            }
            val text = withContext(Dispatchers.IO) { read(uri) }
            when (text) {
                null -> _problem.value = AppText.res.getString(R.string.document_problem_unreadable)
                TOO_BIG -> _problem.value = AppText.res.getString(R.string.import_too_big)
                else -> _picked.value = PickedImport(displayName(uri) ?: AppText.res.getString(R.string.import_file), text)
            }
        }
    }

    fun clear() {
        _picked.value = null
        _problem.value = null
    }

    /** Stops reading as soon as the file is past the size cap, so a huge file is never held in memory. */
    private fun read(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (out.size() <= ImportFormat.MAX_BYTES) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            if (out.size() > ImportFormat.MAX_BYTES) TOO_BIG else out.toString(Charsets.UTF_8.name())
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    /** Up to [cap] bytes and one more, so a caller can tell a file over the cap; null when it can't be opened. */
    private fun readBytes(uri: Uri, cap: Int): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (out.size() <= cap) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private fun displayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        // Not valid JSON, so it can never be mistaken for a file's contents.
        const val TOO_BIG = "\u0000too-big"

        /** A Carfax report is a few MB at most; this leaves room without holding something huge in memory. */
        const val PDF_MAX_BYTES = 20 * 1024 * 1024
    }
}

/** One of the file's items as the preview lists it. [yours] is set when it would replace something saved. */
data class PlanLine(val name: String, val file: String, val yours: String? = null)

/**
 * A record from the file whose reading looks mistyped, and whether the user has chosen to import it
 * anyway. [key] is its date and odometer, which no two records of a file share.
 */
data class DoubtLine(val key: Pair<LocalDate, Int>, val title: String, val why: String, val included: Boolean)

/**
 * A file set against the chosen vehicle, in words: what the page shows whichever kind the file is.
 * Nothing has been written when this exists.
 */
data class PlanView(
    val kind: ImportKind,
    /** Rows for torque specs, items for a schedule. */
    val noun: PlanNoun,
    val total: Int,
    val new: List<PlanLine>,
    val fills: List<PlanLine>,
    val differs: List<PlanLine>,
    val same: Int,
    /** Said under the preview, whatever is in it. */
    val caution: String,
    /** Typical schedule items being tracked under a name the file also uses; empty for other kinds. */
    val doubles: List<String> = emptyList(),
    /** Service history only: the reminders whose last done moves, for each choice the user can make. */
    val moves: Map<ImportMode, List<String>> = emptyMap(),
    /** Service history only: records whose reading looks wrong, left out unless the user includes them. */
    val doubts: List<DoubtLine> = emptyList(),
    /** Something true of the file as a whole, said before the import and again after it. */
    val note: String? = null,
) {
    fun changes(mode: ImportMode): Int = new.size + fills.size + if (mode == ImportMode.REPLACE_MATCHING) differs.size else 0
    fun count(n: Int): String = noun.count(n)
}

/** What the chosen file comes to for the chosen vehicle. */
sealed interface ImportPreview {
    data class Invalid(val file: ImportFile.Invalid) : ImportPreview
    /** A good file, but there are several vehicles and none has been chosen. */
    data object NeedsVehicle : ImportPreview

    /** A good file that can't go into the chosen vehicle; [why] says so. */
    data class Refused(val why: String) : ImportPreview

    /** [write] saves it in one transaction; `stopDoubles` also stops tracking [PlanView.doubles]. */
    class Ready(val view: PlanView, val write: suspend (mode: ImportMode, stopDoubles: Boolean) -> Unit) : ImportPreview
}

data class ImportResult(
    val kind: ImportKind,
    val noun: PlanNoun,
    val vehicleId: Long,
    val vehicleName: String,
    /** The vehicle is sold; its history has its own screen. */
    val archived: Boolean,
    val added: Int,
    val filled: Int,
    val replaced: Int,
    val unchanged: Int,
    /** Typical schedule items the user chose to stop tracking. */
    val stopped: Int = 0,
    /** [PlanView.note], said again now that it is saved. */
    val note: String? = null,
    /** The report was filed in the vehicle's documents; false when it was asked for and couldn't be. */
    val keptCopy: Boolean? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ImportViewModel @Inject constructor(
    vehicles: VehicleDao,
    settings: SettingsRepository,
    private val torque: TorqueDao,
    private val schedule: ScheduleDao,
    private val service: ServiceDao,
    private val odometer: OdometerDao,
    private val milestones: MilestoneDao,
    private val actions: RecordActions,
    private val picker: ImportPicker,
    private val documents: DocumentStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** The garage, then sold vehicles: those can still take service history. */
    val vehicles: StateFlow<List<VehicleEntity>?> = combine(vehicles.observeActive(), vehicles.observeArchived()) { active, sold -> active + sold }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val picked: StateFlow<PickedImport?> = picker.picked
    val problem: StateFlow<String?> = picker.problem

    private val route = savedStateHandle.route<ImportRoute>()
    private val chosenVehicle = MutableStateFlow(route.vehicleId)

    /** Opened from a vehicle (Vehicle details), so there is nothing to choose. */
    val vehicleFixed: Boolean = route.vehicleId != null

    /** Doubtful records ([PlanView.doubts]) the user has chosen to import anyway, by date and odometer. */
    private val included = MutableStateFlow<Set<Pair<LocalDate, Int>>>(emptySet())

    /** The vehicle to import into: the one chosen, or the only one there is. */
    val vehicleId: StateFlow<Long?> = combine(this.vehicles, chosenVehicle) { list, chosen ->
        chosen?.takeIf { id -> list.orEmpty().any { it.id == id } } ?: list?.singleOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val parsed = picker.picked.mapLatest { p -> p?.let { it.file ?: withContext(Dispatchers.Default) { ImportFormat.parse(it.text) } } }

    private val _result = MutableStateFlow<ImportResult?>(null)
    val result: StateFlow<ImportResult?> = _result.asStateFlow()

    /** Null with no file picked. Worked out afresh for each file and vehicle; nothing is written by it. */
    val preview: StateFlow<ImportPreview?> = combine(parsed, vehicleId, this.vehicles, settings.settings, included) { file, id, list, s, keep ->
        Inputs(file, list.orEmpty().firstOrNull { it.id == id }, s, keep)
    }.mapLatest { (file, vehicle, s, keep) ->
        when (file) {
            null -> null
            is ImportFile.Invalid -> ImportPreview.Invalid(file)
            else -> when {
                vehicle == null -> ImportPreview.NeedsVehicle
                file is ImportFile.ServiceHistory -> historyPreview(vehicle, file, s, keep)
                // A sold vehicle gets no reminders and isn't being worked on.
                vehicle.archivedOn != null -> ImportPreview.Refused(AppText.res.getString(R.string.import_sold_only_history))
                file is ImportFile.TorqueSpecs -> torquePreview(vehicle.id, file.items, s.torqueUnit)
                file is ImportFile.IntervalSchedule -> schedulePreview(vehicle, file)
                file is ImportFile.MilestoneSchedule -> milestonePreview(vehicle, file)
                else -> null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Points the History tab at [vehicleId] before the user is sent there. */
    fun showHistory(vehicleId: Long) = actions.showIn(vehicleId)

    fun chooseVehicle(id: Long) {
        chosenVehicle.value = id
    }

    fun include(key: Pair<LocalDate, Int>, yes: Boolean) {
        included.value = if (yes) included.value + key else included.value - key
    }

    fun pick() {
        _result.value = null
        included.value = emptySet()
        picker.pick()
    }

    /** [keepCopy] files a Carfax report's PDF in the vehicle's documents once the import is written. */
    fun import(preview: ImportPreview.Ready, mode: ImportMode, stopDoubles: Boolean, keepCopy: Boolean) = viewModelScope.launch {
        // Read first: a finished import lets go of the picked file.
        val report = picker.picked.value?.takeIf { keepCopy }
        val pdf = report?.pdf
        val id = vehicleId.value
        preview.write(mode, stopDoubles)
        if (pdf != null && id != null) {
            val kept = try {
                val name = VehicleDocuments.nameFromFile(report.name) ?: DocumentType.REPORT.label
                documents.addPdf(id, pdf, DocumentType.REPORT, name, null, LocalDate.now())
                true
            } catch (_: Exception) {
                false
            }
            _result.value = _result.value?.copy(keptCopy = kept)
        }
    }

    private fun finish(view: PlanView, vehicleId: Long, mode: ImportMode, stopped: Int = 0) {
        val vehicle = vehicles.value?.firstOrNull { it.id == vehicleId }
        val replaced = if (mode == ImportMode.REPLACE_MATCHING) view.differs.size else 0
        _result.value = ImportResult(
            kind = view.kind,
            noun = view.noun,
            vehicleId = vehicleId,
            vehicleName = vehicle?.let { "${it.year} ${it.nickname ?: it.model}" }.orEmpty(),
            archived = vehicle?.archivedOn != null,
            added = view.new.size,
            filled = view.fills.size,
            replaced = replaced,
            unchanged = view.same + view.differs.size - replaced,
            stopped = stopped,
            note = view.note,
        )
        picker.clear()
    }

    private suspend fun torquePreview(vehicleId: Long, items: List<ImportedTorqueSpec>, unit: TorqueUnit): ImportPreview {
        val rows = torque.specsFor(vehicleId)
        // As they stood when the preview was worked out; what a replace writes over.
        val byId = rows.associateBy { it.id }
        val plan = TorqueImport.plan(items, rows.map { it.toSaved() })
        val view = PlanView(
            kind = ImportKind.TORQUE_SPECS,
            noun = PlanNoun.ROW,
            total = plan.total,
            new = plan.new.map { PlanLine(it.name, it.describe(unit)) },
            fills = plan.fills.map { PlanLine(it.first.name, it.first.describe(unit)) },
            differs = plan.differs.map { (item, saved) -> PlanLine(saved.name, item.describe(unit), saved.describe(unit)) },
            same = plan.same.size,
            caution = AppText.res.getString(R.string.import_caution_torque),
        )
        return ImportPreview.Ready(view) { mode, _ ->
            val added = plan.new.map {
                TorqueSpecEntity(
                    vehicleId = vehicleId,
                    name = it.name,
                    category = it.category.name,
                    socketSize = it.socket,
                    newtonMeters = it.torque?.newtonMeters,
                    source = it.source.name,
                    note = it.note,
                )
            }
            // The saved row keeps its own name and category: they are what matched, give or take capitals.
            val replaced = plan.replaced(mode).mapNotNull { (item, saved) ->
                byId[saved.id]?.copy(socketSize = item.socket, newtonMeters = item.torque?.newtonMeters, source = item.source.name, note = item.note)
            }
            torque.applyImport(added, replaced)
            finish(view, vehicleId, mode)
        }
    }

    private suspend fun schedulePreview(vehicle: VehicleEntity, file: ImportFile.IntervalSchedule): ImportPreview {
        val vehicleId = vehicle.id
        val unit = vehicle.unit
        // The file says which unit it is in; everything from here on is in the vehicle's.
        val items = FileDistances.schedule(file.items, file.unit, unit)
        val rows = schedule.allFor(vehicleId)
        val byId = rows.associateBy { it.id }
        val saved = rows.map { it.toSaved() }
        val plan = ScheduleImport.plan(items, saved)
        val doubles = ScheduleImport.doubles(items, saved)
        val view = PlanView(
            kind = ImportKind.INTERVAL_SCHEDULE,
            noun = PlanNoun.ITEM,
            total = plan.total,
            new = plan.new.map { PlanLine(it.name, it.describe(unit)) },
            fills = emptyList(),
            differs = plan.differs.map { (item, mine) -> PlanLine(mine.name, item.describe(unit), mine.describe(unit)) },
            same = plan.same.size,
            caution = AppText.res.getString(R.string.import_caution_schedule),
            note = convertedNote(file.unit, unit),
            doubles = doubles.map { it.name },
        )
        return ImportPreview.Ready(view) { mode, stopDoubles ->
            // The user's own items, tracked from the start, exactly like "+ Add your own item".
            val added = plan.new.map {
                ScheduleItemEntity(
                    vehicleId = vehicleId,
                    name = it.name,
                    intervalMiles = it.normal.miles,
                    intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles,
                    severeMonths = it.severe?.months,
                    ruleOverride = it.rule?.name,
                    service = it.service,
                    firstMiles = it.firstAt,
                    severeOnly = it.severeOnly,
                )
            }
            // Only what the file describes changes. Tracked or not, a snooze, which interval is in use and
            // every record linked to the item stay as they are.
            val replaced = plan.replaced(mode).mapNotNull { (item, mine) ->
                byId[mine.id]?.copy(
                    intervalMiles = item.normal.miles,
                    intervalMonths = item.normal.months,
                    severeMiles = item.severe?.miles,
                    severeMonths = item.severe?.months,
                    ruleOverride = item.rule?.name,
                    service = item.service,
                    firstMiles = item.firstAt,
                    severeOnly = item.severeOnly,
                )
            }
            val stop = if (stopDoubles) doubles.map { it.id } else emptyList()
            schedule.applyImport(added, replaced, stop)
            finish(view, vehicleId, mode, stop.size)
        }
    }

    private suspend fun milestonePreview(vehicle: VehicleEntity, file: ImportFile.MilestoneSchedule): ImportPreview {
        val vehicleId = vehicle.id
        val unit = vehicle.unit
        // The file says which unit it is in; the table, and how it repeats, are kept in the vehicle's.
        val (items, repeats) = FileDistances.milestones(file.items, file.repeatsEveryMiles, file.unit, unit)
        val rows = milestones.tableFor(vehicleId)
        val byId = rows.associateBy { it.milestone.id }
        val saved = rows.map { r -> SavedMilestone(r.milestone.id, r.milestone.atMiles, r.ordered.map { TableJob(it.name, it.service) }, r.milestone.orMonths) }
        // A table repeats one way or not at all; a file that says otherwise goes no further.
        MilestoneImport.repeatConflict(repeats, saved, vehicle.milestonesRepeatEveryMiles)?.let { return ImportPreview.Refused(it.text(unit)) }
        val plan = MilestoneImport.plan(items, saved)
        val view = PlanView(
            kind = ImportKind.MILESTONE_SCHEDULE,
            noun = PlanNoun.MILESTONE,
            total = plan.total,
            new = plan.new.map { PlanLine(it.atMiles.asDistance(unit), describe(it.work, it.orMonths)) },
            fills = emptyList(),
            differs = plan.differs.map { (item, mine) -> PlanLine(item.atMiles.asDistance(unit), describe(item.work, item.orMonths), describe(mine.work, mine.orMonths)) },
            same = plan.same.size,
            caution = AppText.res.getString(R.string.import_caution_milestones),
            note = listOfNotNull(repeatNote(repeats, unit), convertedNote(file.unit, unit)).joinToString(" "),
        )
        return ImportPreview.Ready(view) { mode, _ ->
            fun jobs(m: ImportedMilestone) = m.work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) }
            val added = plan.new.map { MilestoneEntity(vehicleId = vehicleId, atMiles = it.atMiles, orMonths = it.orMonths) to jobs(it) }
            val replaced = plan.replaced(mode).mapNotNull { (item, mine) -> byId[mine.id]?.milestone?.copy(orMonths = item.orMonths)?.let { it to jobs(item) } }
            milestones.applyImport(vehicleId, repeats, added, replaced)
            finish(view, vehicleId, mode)
        }
    }

    private suspend fun historyPreview(vehicle: VehicleEntity, file: ImportFile.ServiceHistory, s: Settings, keep: Set<Pair<LocalDate, Int>>): ImportPreview {
        val unit = vehicle.unit
        // The file says which unit its readings are in; they are saved as this vehicle's dash would have shown them.
        val all = FileDistances.history(file.items, file.unit, unit)
        val swap = vehicle.clusterSwap()
        val records = service.recordsFor(vehicle.id)
        val byId = records.associateBy { it.record.id }
        val rows = schedule.allFor(vehicle.id)
        val refs = rows.map { it.toRef() }
        val saved = records.map { r ->
            SavedRecord(
                id = r.record.id,
                date = r.record.date,
                odometer = r.record.odometer,
                labels = r.items.map { it.label },
                itemIds = r.items.mapNotNull { it.scheduleItemId }.toSet(),
                diy = r.record.diy,
                shopName = r.record.shopName,
                costCents = r.record.costCents,
                notes = r.record.notes,
                enteredAsLastDone = r.record.enteredAsLastDone,
            )
        }
        val before = records.map { it.toEntry(swap, rows.answering()) }
        // Readings that look mistyped are found with the whole file in, then left out unless the user keeps them:
        // the odometer is the highest reading on record, so one wrong one would put every mileage reminder out.
        val whole = HistoryImport.plan(all, saved, refs)
        val doubts = HistoryImport.doubtful(
            HistoryImport.entriesAfter(whole, ImportMode.ADD_NEW, before, refs, swap),
            odometer.readingsFor(vehicle.id).toReadings(swap),
            unit,
        ).map { d -> whole.new[(-1L - d.entry.id).toInt()] to d }
        val leftOut = doubts.map { (r, _) -> r.date to r.odometer }.filter { it !in keep }.toSet()
        val items = all.filter { (it.date to it.odometer) !in leftOut }
        val plan = HistoryImport.plan(items, saved, refs)
        // A sold vehicle has no reminders to move.
        val tracked = if (vehicle.archivedOn != null) emptyList() else rows.filter { it.tracked }.mapNotNull { it.toTracked(s.defaultRule, swap) }
        fun after(mode: ImportMode) = HistoryImport.entriesAfter(plan, mode, before, refs, swap)
        val view = PlanView(
            kind = ImportKind.SERVICE_HISTORY,
            noun = PlanNoun.RECORD,
            total = plan.total,
            new = plan.new.map { PlanLine(it.title(unit), it.describe()) },
            fills = plan.fills.map { PlanLine(it.first.title(unit), it.first.describe()) },
            differs = plan.differs.map { (item, mine) -> PlanLine(item.title(unit), item.describe(), mine.describe()) },
            same = plan.same.size,
            caution = AppText.res.getString(R.string.import_caution_history),
            note = convertedNote(file.unit, unit),
            moves = ImportMode.entries.associateWith { mode -> LogEdits.reminderChanges(tracked, before, after(mode)).map { it.text(unit) } },
            doubts = doubts.map { (r, d) ->
                DoubtLine(
                    key = r.date to r.odometer,
                    // As it is written in the file, which is how the user will look for it.
                    title = r.title(unit),
                    why = d.before?.let { b ->
                        AppText.res.getString(
                            R.string.import_doubt_jump,
                            (d.entry.odometer - b.odometer).asDistance(unit),
                            swap.asReadOn(b.odometer, b.date).asDistance(unit),
                            b.date.format(longDate),
                        )
                    } ?: AppText.res.getString(R.string.import_doubt_out_of_line),
                    included = (r.date to r.odometer) in keep,
                )
            },
        )
        return ImportPreview.Ready(view) { mode, _ ->
            fun lines(r: ImportedRecord) = r.work.map { line ->
                val w = HistoryImport.resolve(line, refs)
                ServiceRecordItemEntity(recordId = 0, scheduleItemId = w.scheduleItemId, label = w.label, catalogId = w.catalogId)
            }
            val added = plan.new.map { r ->
                ServiceRecordEntity(
                    vehicleId = vehicle.id,
                    date = r.date,
                    odometer = r.odometer,
                    diy = r.diy,
                    shopName = r.shopName,
                    costCents = r.costCents,
                    notes = r.notes,
                    imported = true,
                ) to lines(r)
            }
            // Same record, same id: its photos and any recall it was the work for stay with it.
            val replaced = plan.replaced(mode).mapNotNull { (item, mine) ->
                byId[mine.id]?.record?.copy(
                    diy = item.diy,
                    shopName = item.shopName,
                    costCents = item.costCents,
                    notes = item.notes,
                    enteredAsLastDone = false,
                    imported = true,
                )?.let { it to lines(item) }
            }
            actions.applyImport(added, replaced)
            finish(view, vehicle.id, mode)
        }
    }

    private fun ImportedRecord.title(unit: DistanceUnit) = "${date.format(longDate)} · ${odometer.asDistance(unit)}"

    private data class Inputs(val file: ImportFile?, val vehicle: VehicleEntity?, val settings: Settings, val keep: Set<Pair<LocalDate, Int>>)
}

/** "100 ft-lb · 21 mm · Star pattern", or what to say for a row that is only a name. */
private fun describe(torque: Torque?, socket: String?, note: String?, unit: TorqueUnit): String =
    listOfNotNull(torque?.text(unit), socket, note).joinToString(" · ").ifEmpty { AppText.res.getString(R.string.import_name_only) }

private fun ImportedTorqueSpec.describe(unit: TorqueUnit) = listOfNotNull(describe(torque, socket, note, unit), factoryManual.takeIf { source == TorqueSource.FACTORY }).joinToString(" · ")
private fun SavedTorqueSpec.describe(unit: TorqueUnit) = listOfNotNull(describe(torque, socket, note, unit), factoryManual.takeIf { source == TorqueSource.FACTORY && torque != null }).joinToString(" · ")

/** "Every 7,500 mi or 12 mo · severe 5,000 mi or 6 mo · whichever comes first" */
private fun describe(normal: Interval?, severe: Interval?, rule: DueRule?, service: String?, unit: DistanceUnit, firstAt: Int? = null): String = listOfNotNull(
    firstAt?.let { AppText.res.getString(R.string.import_first_at, it.asDistance(unit)) },
    normal?.let { AppText.res.getString(if (firstAt != null) R.string.import_then_every else R.string.interval_every, it.orText(unit)) }
        ?: AppText.res.getString(R.string.import_no_interval),
    severe?.let { AppText.res.getString(R.string.import_severe, it.orText(unit)) },
    rule?.ruleInSentence(),
    HistoryImport.services[service]?.let { AppText.res.getString(R.string.import_reset_by, it.label) },
).joinToString(" · ")

/** "Oil & filter change, Tire rotation · Corner Garage · $62.50" */
private fun describe(labels: List<String>, who: String, costCents: Long?): String =
    listOfNotNull(labels.joinToString(", "), who, costCents?.let(Money::format)).joinToString(" · ")

private fun ImportedRecord.describe() = describe(work.map { it.label }, LogEdits.who(diy, shopName, null).text, costCents)
private fun SavedRecord.describe() = describe(labels, LogEdits.who(diy, shopName, null, enteredAsLastDone).text, costCents)

/** "Change oil, Rotate tires · or 12 months" */
private fun describe(work: List<TableJob>, orMonths: Int?): String =
    listOfNotNull(work.joinToString(", ") { it.name }, orMonths?.let { AppText.res.getQuantityString(R.plurals.milestones_or_months, it, it.asNumber()) }).joinToString(" · ")

private fun ImportedScheduleItem.describe(unit: DistanceUnit) = listOfNotNull(describe(normal, severe, rule, service, unit, firstAt), severeOnlyWords.takeIf { severeOnly }).joinToString(" · ")
private fun SavedScheduleItem.describe(unit: DistanceUnit) = listOfNotNull(describe(normal, severe, rule, service, unit, firstAt), severeOnlyWords.takeIf { severeOnly }).joinToString(" · ")

private const val README = "https://github.com/mcosgriff/glovebox/blob/main/docs/import/README.md"

/** Settings → Import. The format, and what an import may and may not change, are in `docs/import/README.md`. */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onOpenTorqueSpecs: (vehicleId: Long) -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    /** "When were these last done?", offered after a schedule import. */
    onEnterLastDone: (vehicleId: Long) -> Unit,
    onOpenMilestones: (vehicleId: Long) -> Unit,
    /** After a history import. A sold vehicle's history is its own screen. */
    onOpenHistory: (vehicleId: Long, archived: Boolean) -> Unit,
    vm: ImportViewModel = hiltViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val vehicles = vm.vehicles.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val vehicleId by vm.vehicleId.collectAsStateWithLifecycle()
    val picked by vm.picked.collectAsStateWithLifecycle()
    val problem by vm.problem.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var mode by rememberSaveable(picked?.name) { mutableStateOf(ImportMode.ADD_NEW) }
    var stopDoubles by rememberSaveable(picked?.name) { mutableStateOf(false) }
    // Off by default: the report is only read, never kept, unless the user asks.
    var keepCopy by rememberSaveable(picked?.name) { mutableStateOf(false) }
    var confirming by rememberSaveable { mutableStateOf(false) }

    val ready = preview as? ImportPreview.Ready
    if (confirming && ready != null) {
        ReplaceDialog(
            view = ready.view,
            onConfirm = {
                confirming = false
                vm.import(ready, ImportMode.REPLACE_MATCHING, stopDoubles, keepCopy)
            },
            onDismiss = { confirming = false },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.settings_import), onBack)

        Column(
            Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.import_intro),
                    color = scheme.onSurfaceVariant,
                )
                TextButton(onClick = { uriHandler.openUri(README) }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Text(stringResource(R.string.import_how_to))
                }
            }

            if (vehicles.isEmpty()) {
                Text(stringResource(R.string.import_no_vehicle), color = scheme.onSurfaceVariant)
                return@Column
            }

            result?.let { r ->
                ResultCard(r)
                when (r.kind) {
                    ImportKind.INTERVAL_SCHEDULE -> {
                        if (r.added > 0) {
                            PrimaryButton(onClick = { onEnterLastDone(r.vehicleId) }) { Text(stringResource(R.string.home_enter_last_done)) }
                            Text(
                                stringResource(R.string.import_last_done_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = { onOpenSchedule(r.vehicleId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
                            Text(stringResource(R.string.import_see_schedule))
                        }
                    }
                    ImportKind.MILESTONE_SCHEDULE -> PrimaryButton(onClick = { onOpenMilestones(r.vehicleId) }) { Text(stringResource(R.string.import_see_milestones)) }
                    ImportKind.SERVICE_HISTORY -> PrimaryButton(
                        onClick = {
                            vm.showHistory(r.vehicleId)
                            onOpenHistory(r.vehicleId, r.archived)
                        },
                    ) { Text(stringResource(R.string.import_see_history)) }
                    else -> PrimaryButton(onClick = { onOpenTorqueSpecs(r.vehicleId) }) { Text(stringResource(R.string.import_see_torque)) }
                }
                OutlinedButton(onClick = vm::pick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.import_another))
                }
                return@Column
            }

            val fixed = vehicles.firstOrNull { it.id == vehicleId }?.takeIf { vm.vehicleFixed }
            if (fixed != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.import_into))
                    Text("${fixed.year} ${fixed.nickname ?: fixed.model}", style = MaterialTheme.typography.titleMedium)
                }
            } else if (vehicles.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.import_into))
                    RowsCard {
                        Column(Modifier.selectableGroup()) {
                            vehicles.forEachIndexed { i, v ->
                                if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                                Choice(
                                    title = "${v.year} ${v.nickname ?: v.model}",
                                    detail = listOfNotNull(stringResource(R.string.import_sold).takeIf { v.archivedOn != null }, v.trim, v.engine).joinToString(" · ").ifEmpty { null },
                                    selected = v.id == vehicleId,
                                    onClick = { vm.chooseVehicle(v.id) },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.import_file_section))
                val file = picked
                if (file == null) {
                    PrimaryButton(onClick = vm::pick) { Text(stringResource(R.string.import_choose)) }
                } else {
                    RowsCard {
                        Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(file.name, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyLarge, fontFamily = PlexMono)
                            TextButton(onClick = vm::pick, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.change)) }
                        }
                    }
                }
                problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.error) }
            }

            when (val p = preview) {
                null -> Unit
                is ImportPreview.Invalid -> ErrorsCard(p.file)
                ImportPreview.NeedsVehicle -> Text(stringResource(R.string.import_needs_vehicle), color = scheme.onSurface)
                is ImportPreview.Refused -> Text(p.why, color = scheme.onSurface)
                is ImportPreview.Ready -> {
                    val view = p.view
                    PlanCard(view)
                    if (view.doubts.isNotEmpty()) DoubtsCard(view.doubts, vm::include)
                    if (view.differs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(stringResource(view.noun.differ))
                            RowsCard {
                                Column(Modifier.selectableGroup()) {
                                    Choice(stringResource(R.string.import_add_new), stringResource(R.string.import_add_new_note), mode == ImportMode.ADD_NEW) { mode = ImportMode.ADD_NEW }
                                    HorizontalDivider(color = scheme.outlineVariant)
                                    Choice(
                                        stringResource(R.string.import_replace),
                                        pluralStringResource(R.plurals.import_replace_note, view.differs.size, view.count(view.differs.size)),
                                        mode == ImportMode.REPLACE_MATCHING,
                                    ) { mode = ImportMode.REPLACE_MATCHING }
                                }
                            }
                        }
                    }
                    if (view.doubles.isNotEmpty()) DoublesCard(view.doubles, stopDoubles) { stopDoubles = it }
                    view.moves[mode].orEmpty().takeIf { it.isNotEmpty() }?.let { MovesCard(it) }
                    val changes = view.changes(mode)
                    if (changes == 0) {
                        Text(
                            if (view.differs.isEmpty()) {
                                stringResource(R.string.import_nothing)
                            } else {
                                stringResource(view.noun.replaceOnly)
                            },
                            color = scheme.onSurface,
                        )
                    } else {
                        if (picked?.pdf != null) KeepCopyCard(keepCopy) { keepCopy = it }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PrimaryButton(onClick = { if (mode == ImportMode.REPLACE_MATCHING && view.differs.isNotEmpty()) confirming = true else vm.import(p, mode, stopDoubles, keepCopy) }) {
                                Text(stringResource(R.string.import_button, view.count(changes)))
                            }
                            Text(
                                stringResource(R.string.import_no_undo),
                                Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(view: PlanView) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_in_file, view.kind.label.uppercase(AppText.locale), view.total.asNumber())) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Group(pluralStringResource(R.plurals.import_new, view.new.size, view.new.size.asNumber()), stringResource(R.string.import_new_note), view.new)
            Group(pluralStringResource(view.noun.fills, view.fills.size, view.fills.size.asNumber()), stringResource(R.string.import_fills_note), view.fills)
            Group(pluralStringResource(R.plurals.import_differs, view.differs.size, view.differs.size.asNumber()), stringResource(R.string.import_differs_note), view.differs, color = scheme.onPrimaryContainer)
            if (view.same > 0) {
                Text(pluralStringResource(R.plurals.import_same, view.same, view.same.asNumber()), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            view.doubts.count { !it.included }.takeIf { it > 0 }?.let { n ->
                Text(
                    pluralStringResource(R.plurals.import_left_out, n, view.count(n)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                )
            }
            view.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(view.caution, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Group(title: String, detail: String, lines: List<PlanLine>, color: Color = MaterialTheme.colorScheme.onSurface) {
    if (lines.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        lines.take(SHOWN).forEach { line ->
            Column {
                Text(line.name, style = MaterialTheme.typography.bodyMedium)
                line.yours?.let { Text(stringResource(R.string.import_yours, it), style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant) }
                Text(
                    if (line.yours == null) line.file else stringResource(R.string.import_file_value, line.file),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = PlexMono,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (lines.size > SHOWN) Text(stringResource(R.string.and_more, (lines.size - SHOWN).asNumber()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

/** Long files are summarised on the page; the confirmation for a replace lists everything it changes. */
private const val SHOWN = 12

/**
 * A typical item Glovebox suggested is being tracked for a job the file also has (the same name, or
 * the item the file's service resets), so there would be two reminders for one job. Stopping the typical one is the user's choice, made here.
 */
@Composable
private fun DoublesCard(names: List<String>, stop: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_doubles), note = pluralStringResource(R.plurals.import_doubles_note, names.size)) {
        CheckRow(pluralStringResource(R.plurals.import_stop_typical, names.size, names.size.asNumber()), stop, detail = names.joinToString(", "), onChange = onChange)
    }
}

/**
 * Records whose odometer looks mistyped, each left out unless ticked. The vehicle's odometer is its
 * highest reading, so a wrong one from a report would become the odometer and put every mileage
 * reminder out. Nothing is decided wrong: the user ticks what the report has right.
 */
@Composable
private fun DoubtsCard(doubts: List<DoubtLine>, onInclude: (Pair<LocalDate, Int>, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_readings_to_check), note = stringResource(R.string.import_readings_note)) {
        Column {
            doubts.forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                CheckRow(d.title, d.included, detail = d.why, mono = true) { onInclude(d.key, it) }
            }
        }
    }
}

/** Which reminders count from a different service once the import is in, as the delete sheet lists them. */
@Composable
private fun MovesCard(moves: List<String>) {
    LabelledCard(stringResource(R.string.import_moves)) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            moves.take(SHOWN).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (moves.size > SHOWN) Text(stringResource(R.string.and_more, (moves.size - SHOWN).asNumber()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorsCard(file: ImportFile.Invalid) {
    val scheme = MaterialTheme.colorScheme
    LabelledCard(stringResource(R.string.import_cant), note = stringResource(R.string.import_fix_file)) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            file.carfax?.let { why ->
                Text(
                    when (why) {
                        CarfaxRefusal.NotAReport -> stringResource(R.string.carfax_not_a_report)
                        is CarfaxRefusal.NoVisits ->
                            if (why.withoutMileage == 0) {
                                stringResource(R.string.carfax_no_visits)
                            } else {
                                pluralStringResource(R.plurals.carfax_no_visits_without_mileage, why.withoutMileage, why.withoutMileage.asNumber())
                            }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            file.errors.forEach { e ->
                Column {
                    if (e.path.isNotEmpty()) Text(e.path, style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, color = scheme.error)
                    Text(e.message.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (file.more > 0) Text(stringResource(R.string.and_more, file.more.asNumber()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResultCard(r: ImportResult) {
    val scheme = MaterialTheme.colorScheme
    fun count(n: Int) = r.noun.count(n)
    LabelledCard(stringResource(R.string.import_imported_into, r.vehicleName.uppercase(AppText.locale))) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(r.added to R.string.import_added, r.filled to R.string.import_filled, r.replaced to R.string.import_replaced).filter { it.first > 0 }.forEach { (n, what) ->
                Text(stringResource(what, count(n)), style = MaterialTheme.typography.bodyLarge, color = statusColors().ok)
            }
            if (r.stopped > 0) {
                Text(
                    pluralStringResource(R.plurals.import_stopped, r.stopped, r.stopped.asNumber()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = statusColors().ok,
                )
            }
            if (r.unchanged > 0) {
                Text(pluralStringResource(R.plurals.import_unchanged, r.unchanged, count(r.unchanged)), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            r.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
            when (r.keptCopy) {
                true -> Text(stringResource(R.string.import_kept_copy), style = MaterialTheme.typography.bodyLarge, color = statusColors().ok)
                false -> Text(stringResource(R.string.import_copy_failed), style = MaterialTheme.typography.bodyMedium, color = scheme.error)
                null -> Unit
            }
        }
    }
}

/** For a Carfax report: whether to file the PDF itself with the vehicle's documents when the import is written. */
@Composable
private fun KeepCopyCard(keep: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    RowsCard {
        CheckRow(stringResource(R.string.import_keep_copy), keep, detail = stringResource(R.string.import_keep_copy_note), onChange = onChange)
    }
}

@Composable
private fun ReplaceDialog(view: PlanView, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_replace_title, view.count(view.differs.size))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                view.differs.forEach { line ->
                    Column {
                        Text(line.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.import_yours, line.yours.orEmpty()), style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono, color = scheme.onSurfaceVariant)
                        Text(stringResource(R.string.import_becomes, line.file), style = MaterialTheme.typography.bodySmall, fontFamily = PlexMono)
                    }
                }
                Text(stringResource(R.string.delete_all_cant_undo), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = { DangerTextButton(onClick = onConfirm) { Text(stringResource(R.string.import_replace_and_import)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Choice(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** What a plan counts: the words for it, by kind of file. */
enum class PlanNoun(@PluralsRes private val counted: Int, @StringRes val differ: Int, @StringRes val replaceOnly: Int, @PluralsRes val fills: Int) {
    ROW(R.plurals.plan_rows, R.string.plan_rows_differ, R.string.plan_rows_replace_only, R.plurals.plan_rows_fill),
    ITEM(R.plurals.plan_items, R.string.plan_items_differ, R.string.plan_items_replace_only, R.plurals.plan_items_fill),
    MILESTONE(R.plurals.milestones, R.string.plan_milestones_differ, R.string.plan_milestones_replace_only, R.plurals.plan_milestones_fill),
    RECORD(R.plurals.records, R.string.plan_records_differ, R.string.plan_records_replace_only, R.plurals.plan_records_fill),
    ;

    /** "3 rows" */
    fun count(n: Int): String = AppText.res.getQuantityString(counted, n, n.asNumber())
}

val ImportKind.label: String
    get() = AppText.res.getString(
        when (this) {
            ImportKind.SERVICE_HISTORY -> R.string.import_kind_history
            ImportKind.INTERVAL_SCHEDULE -> R.string.details_schedule
            ImportKind.MILESTONE_SCHEDULE -> R.string.milestones_title
            ImportKind.TORQUE_SPECS -> R.string.import_kind_torque
        },
    )

/** Said under a preview when the file's unit isn't the vehicle's: what the page lists is what will be saved, not what the file says. */
private fun convertedNote(from: DistanceUnit, to: DistanceUnit): String? =
    AppText.res.getString(R.string.import_converted, from.pluralWord, to.pluralWord).takeIf { FileDistances.converts(from, to) }

private val factoryManual: String get() = AppText.res.getString(R.string.import_factory_manual)

private val severeOnlyWords: String get() = AppText.res.getString(R.string.import_severe_only)
