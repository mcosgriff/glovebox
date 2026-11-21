package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.screenState
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.data.db.toRows
import io.cosgriff.glovebox.data.db.toModel
import io.cosgriff.glovebox.data.db.jobRows
import io.cosgriff.glovebox.data.db.toEntity
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.torque.text
import io.cosgriff.glovebox.ui.schedule.text
import io.cosgriff.glovebox.ui.schedule.repeatNote
import io.cosgriff.glovebox.ui.service.label
import java.time.LocalDate
import io.cosgriff.glovebox.domain.FileDistances
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.lifecycle.SavedStateHandle
import io.cosgriff.glovebox.ui.route
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.MilestoneDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.TorqueDao
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.SavedMilestone
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.domain.ImportFormat
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.ScheduleImport
import io.cosgriff.glovebox.domain.TorqueImport
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.toSaved
import io.cosgriff.glovebox.data.db.toSaved
import io.cosgriff.glovebox.ui.ImportRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.cosgriff.glovebox.data.log.HistoryChoices
import io.cosgriff.glovebox.data.log.HistoryImportPlanner
import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.data.db.CatalogRuleDao
import io.cosgriff.glovebox.data.db.ShopRecommendationDao
import io.cosgriff.glovebox.domain.ShopRecommendation
import io.cosgriff.glovebox.data.db.toRule
import io.cosgriff.glovebox.domain.RuleFiling
import javax.inject.Inject

/** One of the file's items as the preview lists it. [yours] is set when it would replace something saved. */
data class PlanLine(val name: String, val file: String, val yours: String? = null)

/**
 * A record from the file whose reading looks mistyped, and whether the user has chosen to import it
 * anyway. [key] is its date and odometer, which no two records of a file share.
 */
data class DoubtLine(val key: Pair<LocalDate, Int>, val title: String, val why: String, val included: Boolean)

/**
 * An open shop recommendation a record in the file looks like the work for (#593), [why] in words, and whether it
 * closes on import: ticked unless the user unticked it.
 */
data class DoneLine(val recommendation: ShopRecommendation, val why: String, val ticked: Boolean)

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
    /** Service history only: open shop recommendations the records look like the work for, for each choice (#593). */
    val done: Map<ImportMode, List<DoneLine>> = emptyMap(),
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
    private val logs: VehicleLogs,
    private val milestones: MilestoneDao,
    private val catalogRules: CatalogRuleDao,
    private val recommendations: ShopRecommendationDao,
    private val actions: RecordActions,
    private val picker: ImportPicker,
    private val documents: DocumentStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** The garage, then sold vehicles: those can still take service history. */
    val vehicles: StateFlow<List<VehicleEntity>?> = combine(vehicles.observeActive(), vehicles.observeArchived()) { active, sold -> active + sold }
        .screenState(viewModelScope, null)

    val picked: StateFlow<PickedImport?> = picker.picked
    val problem: StateFlow<String?> = picker.problem

    private val route = savedStateHandle.route<ImportRoute>()
    private val chosenVehicle = MutableStateFlow(route.vehicleId)

    /** Opened from a vehicle (Vehicle details), so there is nothing to choose. */
    val vehicleFixed: Boolean = route.vehicleId != null

    /** Opened from the Torque & fastener specs page: torque specs only, and only from there (#604). */
    val torqueOnly: Boolean = route.torqueOnly

    /** Doubtful records ([PlanView.doubts]) the user has chosen to import anyway, by date and odometer. */
    private val included = MutableStateFlow<Set<Pair<LocalDate, Int>>>(emptySet())

    /** Shop recommendations offered as done ([PlanView.done]) the user unticked, by id: they stay open (#593). */
    private val unticked = MutableStateFlow<Set<Long>>(emptySet())

    /** The vehicle to import into: the one chosen, or the only one there is. */
    val vehicleId: StateFlow<Long?> = combine(this.vehicles, chosenVehicle) { list, chosen ->
        chosen?.takeIf { id -> list.orEmpty().any { it.id == id } } ?: list?.singleOrNull()?.id
    }.screenState(viewModelScope, null)

    private val parsed = picker.picked.mapLatest { p -> p?.let { it.file ?: withContext(Dispatchers.Default) { ImportFormat.parse(it.text) } } }

    private val _result = MutableStateFlow<ImportResult?>(null)
    val result: StateFlow<ImportResult?> = _result.asStateFlow()

    /** Null with no file picked. Worked out afresh for each file and vehicle; nothing is written by it. */
    val preview: StateFlow<ImportPreview?> = combine(parsed, vehicleId, this.vehicles, settings.settings, combine(included, unticked, ::Pair)) { file, id, list, s, (keep, notDone) ->
        Inputs(file, list.orEmpty().firstOrNull { it.id == id }, s, keep, notDone)
    }.mapLatest { (file, vehicle, s, keep, notDone) ->
        when (file) {
            null -> null
            is ImportFile.Invalid -> ImportPreview.Invalid(file)
            else -> when {
                // Torque specs belong to their own page; every other kind to this one.
                torqueOnly && file !is ImportFile.TorqueSpecs -> ImportPreview.Refused(AppText.res.getString(R.string.import_torque_only))
                !torqueOnly && file is ImportFile.TorqueSpecs -> ImportPreview.Refused(AppText.res.getString(R.string.import_torque_elsewhere))
                vehicle == null -> ImportPreview.NeedsVehicle
                file is ImportFile.ServiceHistory -> historyPreview(vehicle, file, keep, notDone)
                // A sold vehicle gets no reminders and isn't being worked on.
                vehicle.archivedOn != null -> ImportPreview.Refused(AppText.res.getString(R.string.import_sold_only_history))
                file is ImportFile.TorqueSpecs -> torquePreview(vehicle.id, file.items, s.torqueUnit)
                file is ImportFile.IntervalSchedule -> schedulePreview(vehicle, file)
                file is ImportFile.MilestoneSchedule -> milestonePreview(vehicle, file)
                else -> null
            }
        }
    }.screenState(viewModelScope, null)

    /** Points the History tab at [vehicleId] before the user is sent there. */
    fun showHistory(vehicleId: Long) = actions.showIn(vehicleId)

    fun chooseVehicle(id: Long) {
        chosenVehicle.value = id
    }

    fun include(key: Pair<LocalDate, Int>, yes: Boolean) {
        included.value = if (yes) included.value + key else included.value - key
    }

    /** A shop recommendation offered as done by the import ticked ([yes]: it closes) or not (it stays open). */
    fun tickDone(id: Long, yes: Boolean) {
        unticked.value = if (yes) unticked.value - id else unticked.value + id
    }

    fun pick() {
        _result.value = null
        included.value = emptySet()
        unticked.value = emptySet()
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
            vehicleName = vehicle?.let { it.shortName() }.orEmpty(),
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
        val view = plan.toView(
            ImportKind.TORQUE_SPECS, PlanNoun.ROW, R.string.import_caution_torque,
            name = { it.name }, describe = { it.describe(unit) }, describeSaved = { it.describe(unit) }, differName = { _, saved -> saved.name },
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
        val view = plan.toView(
            ImportKind.INTERVAL_SCHEDULE, PlanNoun.ITEM, R.string.import_caution_schedule,
            name = { it.name }, describe = { it.describe(unit) }, describeSaved = { it.describe(unit) }, differName = { _, mine -> mine.name },
            note = convertedNote(file.unit, unit),
            doubles = doubles.map { it.name },
        )
        return ImportPreview.Ready(view) { mode, stopDoubles ->
            // The user's own items, tracked from the start, exactly like "+ Add your own item".
            val added = plan.new.map { it.toEntity(vehicleId) }
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
        val view = plan.toView(
            ImportKind.MILESTONE_SCHEDULE, PlanNoun.MILESTONE, R.string.import_caution_milestones,
            name = { it.atMiles.asDistance(unit) }, describe = { describe(it.work, it.orMonths) }, describeSaved = { describe(it.work, it.orMonths) },
            note = listOfNotNull(repeatNote(repeats, unit), convertedNote(file.unit, unit)).joinToString(" "),
        )
        return ImportPreview.Ready(view) { mode, _ ->
            val added = plan.new.map { it.toRows(vehicleId) }
            val replaced = plan.replaced(mode).mapNotNull { (item, mine) -> byId[mine.id]?.milestone?.copy(orMonths = item.orMonths)?.let { it to item.jobRows() } }
            milestones.applyImport(vehicleId, repeats, added, replaced)
            finish(view, vehicleId, mode)
        }
    }

    /** "2 lines filed by your rules: Water pump gasket replaced." Null when no rule filed anything. */
    private fun rulesNote(filing: RuleFiling): String? = filing.lines.takeIf { it > 0 }?.let { n ->
        AppText.res.getQuantityString(R.plurals.import_rules_filed, n, n.asNumber(), filing.wordings.joinToString(", "))
    }

    /**
     * "2 wordings will go under Custom with a guess at their line: “Watr pump replaced” looks like Water pump; …" (#312).
     * Said, never filed: the sort page offers each guess after the import. Null when there is none.
     */
    private fun guessesNote(guesses: List<Pair<String, String>>): String? {
        if (guesses.isEmpty()) return null
        val said = guesses.take(GUESSES_SAID).joinToString("; ") { (wording, id) -> AppText.res.getString(R.string.import_guess, wording, catalogLabel(id)) }
        val more = guesses.size - GUESSES_SAID
        val list = if (more > 0) AppText.res.getQuantityString(R.plurals.import_guesses_more, more, said, more.asNumber()) else said
        return AppText.res.getQuantityString(R.plurals.import_guesses, guesses.size, guesses.size.asNumber(), list)
    }

    private suspend fun historyPreview(vehicle: VehicleEntity, file: ImportFile.ServiceHistory, keep: Set<Pair<LocalDate, Int>>, notDone: Set<Long>): ImportPreview {
        val unit = vehicle.unit
        // Gone since it was chosen: as good as none chosen.
        val log = logs.load(vehicle.id) ?: return ImportPreview.NeedsVehicle
        val planned = HistoryImportPlanner.plan(
            file, log, catalogRules.all().map { it.toRule() }, recommendations.all(vehicle.id).map { it.toModel() },
            // A Carfax report is the PDF: there the owner's rules win over the wording Glovebox recognised.
            overWording = picker.picked.value?.pdf != null,
            choices = HistoryChoices(kept = keep, unticked = notDone),
        )
        val view = planned.matched.toView(
            ImportKind.SERVICE_HISTORY, PlanNoun.RECORD, R.string.import_caution_history,
            name = { it.title(unit) }, describe = { it.describe() }, describeSaved = { it.describe() },
            note = listOfNotNull(convertedNote(file.unit, unit), rulesNote(planned.filing), guessesNote(planned.guesses))
                .joinToString(" ").ifEmpty { null },
            moves = planned.moves.mapValues { (_, changes) -> changes.map { it.text(unit) } },
            doubts = planned.doubts.map { d ->
                DoubtLine(
                    key = d.key,
                    // As it is written in the file, which is how the user will look for it.
                    title = d.record.title(unit),
                    why = d.jump?.let { j ->
                        AppText.res.getString(R.string.import_doubt_jump, j.by.asDistance(unit), j.from.odometer.asDistance(unit), j.from.date.format(longDate))
                    } ?: AppText.res.getString(R.string.import_doubt_out_of_line),
                    included = d.kept,
                )
            },
            done = planned.offers.mapValues { (_, offers) ->
                offers.map { o -> DoneLine(o.recommendation, AppText.res.getString(R.string.import_done_why, o.line.label, o.record.date.format(longDate)), o.ticked) }
            },
        )
        return ImportPreview.Ready(view) { mode, _ ->
            val write = planned.write(mode)
            actions.applyImport(write.added, write.replaced, write.done, write.reopened)
            finish(view, vehicle.id, mode)
        }
    }

    private fun ImportedRecord.title(unit: DistanceUnit) = "${date.format(longDate)} · ${odometer.asDistance(unit)}"

    private data class Inputs(
        val file: ImportFile?,
        val vehicle: VehicleEntity?,
        val settings: Settings,
        val keep: Set<Pair<LocalDate, Int>>,
        val unticked: Set<Long>,
    )
}


/** How many guesses the preview names before "and n more". */
private const val GUESSES_SAID = 5
