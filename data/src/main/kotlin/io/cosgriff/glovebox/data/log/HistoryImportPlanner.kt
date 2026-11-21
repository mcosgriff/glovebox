package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.ImportClose
import io.cosgriff.glovebox.data.db.toModel
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.CatalogRules
import io.cosgriff.glovebox.domain.CustomLines
import io.cosgriff.glovebox.domain.FileDistances
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.LoggedLine
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.RuleFiling
import io.cosgriff.glovebox.domain.SavedRecord
import io.cosgriff.glovebox.domain.ServiceRecord
import io.cosgriff.glovebox.domain.ShopRecommendation
import io.cosgriff.glovebox.domain.ShopRecommendations
import io.cosgriff.glovebox.domain.asReadOn
import java.time.LocalDate

/**
 * What the owner has chosen on a history import's preview. Both start empty: every doubtful record left out, every
 * offered recommendation ticked.
 */
data class HistoryChoices(
    /** Doubtful records ([HistoryImportPlan.doubts]) to import anyway, by [HistoryDoubt.key]. */
    val kept: Set<Pair<LocalDate, Int>> = emptySet(),
    /** Recommendations offered as done ([HistoryImportPlan.offers]) the owner unticked, by id: they stay open (#593). */
    val unticked: Set<Long> = emptySet(),
)

/**
 * A record from the file whose reading looks mistyped ([HistoryImport.doubtful]), with the reading in the vehicle's
 * unit, as it will be saved. [jump] is null when it doesn't fit the readings around it at all. [kept]: the owner
 * chose to import it anyway.
 */
data class HistoryDoubt(val record: ImportedRecord, val jump: Jump?, val kept: Boolean) {
    /** Its date and odometer, which no two records of a file share: what [HistoryChoices.kept] names it by. */
    val key: Pair<LocalDate, Int> get() = record.date to record.odometer

    /** The reading on record it jumps far ahead of, [from], as the dash read it then, and [by] how far. */
    data class Jump(val from: OdometerReading, val by: Int)
}

/**
 * An open recommendation a record being imported looks like the work for (#593): the [line] of [record] (as it would
 * be written) that matches it, and whether it closes on import, [ticked] unless the owner unticked it.
 */
data class HistoryOffer(val recommendation: ShopRecommendation, val line: LoggedLine, val record: ServiceRecord, val ticked: Boolean)

/** One history import as written, in one transaction (`RecordActions.applyImport`): nothing is worked out after this. */
data class HistoryImportWrite(
    val added: List<ServiceRecord>,
    /** Saved records under their own ids, so their photos and any recall they were the work for stay with them. */
    val replaced: List<ServiceRecord>,
    /** The ticked offers, each by the record it was offered for. */
    val done: List<ImportClose>,
    /** Recommendations a replaced record had closed and no longer looks like the work for. */
    val reopened: List<Long>,
)

/**
 * A service-history file set against one vehicle's log, with the owner's [HistoryChoices] in it (#618): what the
 * preview shows, with no wording, and the [write] for each mode. Nothing has been written when this exists.
 */
class HistoryImportPlan internal constructor(
    /** The file's records against the saved ones, with the doubtful ones the owner hasn't kept left out. */
    val matched: ImportPlan<ImportedRecord, SavedRecord>,
    /** What the owner's rules filed differently ([CatalogRules.file]). */
    val filing: RuleFiling,
    /** Lines that will go under Custom with a guess at their catalog line: (the wording, the line's id). */
    val guesses: List<Pair<String, String>>,
    /** Every doubtful record, kept or left out, in the order the file gives them. */
    val doubts: List<HistoryDoubt>,
    /** The reminders whose last done moves, for each mode. A sold vehicle has none. */
    val moves: Map<ImportMode, List<ReminderChange>>,
    /** Open recommendations the records look like the work for, for each mode. */
    val offers: Map<ImportMode, List<HistoryOffer>>,
    private val writes: Map<ImportMode, HistoryImportWrite>,
) {
    /** The import in [mode]: only the ticked offers close. */
    fun write(mode: ImportMode): HistoryImportWrite = writes.getValue(mode)
}

/**
 * Plans a service-history import (#618), the one way every screen that offers one does. Takes only values, so it can
 * be planned from wherever the import starts.
 */
object HistoryImportPlanner {
    /**
     * [file] against [log]. [rules] are the owner's catalog rules, [recommendations] every recommendation kept for the
     * vehicle, open or not (one a replaced record had closed may open again). [overWording]: the file is a Carfax
     * report, whose recognised wording the owner's rules win over ([CatalogRules.file]).
     */
    fun plan(
        file: ImportFile.ServiceHistory,
        log: VehicleLog,
        rules: List<CatalogRule>,
        recommendations: List<ShopRecommendation>,
        overWording: Boolean,
        choices: HistoryChoices = HistoryChoices(),
    ): HistoryImportPlan {
        val vehicleId = log.vehicle.id
        // The owner's rules file the lines the file leaves unsaid before anything is compared, so the plan and the
        // write see the same lines.
        val filing = CatalogRules.file(file.items, rules, overWording)
        // The file says which unit its readings are in; they are saved as this vehicle's dash would have shown them.
        val all = FileDistances.history(filing.items, file.unit, log.unit)
        val byId = log.records.associateBy { it.record.id }
        val refs = log.items.map { it.toRef() }
        val saved = log.records.map { it.toModel().toSaved() }
        fun added(plan: ImportPlan<ImportedRecord, SavedRecord>) = plan.new.map { HistoryImport.added(it, vehicleId, refs) }
        // Same record, same id: its photos and any recall it was the work for stay with it.
        fun replaced(plan: ImportPlan<ImportedRecord, SavedRecord>, mode: ImportMode) = plan.replaced(mode).mapNotNull { (item, mine) ->
            byId[mine.id]?.let { HistoryImport.replacing(it.toModel(), item, refs) }
        }
        // What moves is read from the records the import would write, the new ones numbered so an entry leads back.
        fun whatIf(plan: ImportPlan<ImportedRecord, SavedRecord>, mode: ImportMode) = log.whatIf(replaced(plan, mode) + added(plan).numberedAsNew())

        // Readings that look mistyped are found with the whole file in, then left out unless the owner keeps them:
        // the odometer is the highest reading on record, so one wrong one would put every mileage reminder out.
        val whole = HistoryImport.plan(all, saved, refs)
        val doubts = HistoryImport.doubtful(whatIf(whole, ImportMode.ADD_NEW).entries, log.updates, log.unit).map { d ->
            val record = whole.new[checkNotNull(newIndexOf(d.entry.id))]
            HistoryDoubt(
                record = record,
                jump = d.before?.let { b -> HistoryDoubt.Jump(OdometerReading(b.date, log.swap.asReadOn(b.odometer, b.date)), d.entry.odometer - b.odometer) },
                kept = (record.date to record.odometer) in choices.kept,
            )
        }
        val leftOut = doubts.filter { !it.kept }.map { it.key }.toSet()
        val items = all.filter { (it.date to it.odometer) !in leftOut }
        val plan = HistoryImport.plan(items, saved, refs)
        val added = added(plan)

        val offers = mutableMapOf<ImportMode, List<HistoryOffer>>()
        val writes = mutableMapOf<ImportMode, HistoryImportWrite>()
        for (mode in ImportMode.entries) {
            val replacing = replaced(plan, mode)
            val records = added + replacing
            // Offered for each mode; nothing closes unless it is left ticked.
            val offered = ShopRecommendations.offeredOnImport(recommendations, records)
            offers[mode] = offered.map { o -> HistoryOffer(o.recommendation, o.line, records[o.record], ticked = o.recommendation.id !in choices.unticked) }
            val done = offered.filter { it.recommendation.id !in choices.unticked }.map { o ->
                if (o.record < added.size) ImportClose(o.recommendation.id, added = o.record) else ImportClose(o.recommendation.id, savedId = records[o.record].id)
            }
            writes[mode] = HistoryImportWrite(added, replacing, done, ShopRecommendations.reopenedOnImport(recommendations, replacing))
        }
        return HistoryImportPlan(
            matched = plan,
            filing = filing,
            guesses = CustomLines.guessesFor(items, refs),
            doubts = doubts,
            moves = ImportMode.entries.associateWith { mode -> whatIf(plan, mode).changes },
            offers = offers,
            writes = writes,
        )
    }
}
