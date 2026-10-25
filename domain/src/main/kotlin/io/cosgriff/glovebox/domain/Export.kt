package io.cosgriff.glovebox.domain

import java.time.LocalDate

enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    MARKDOWN("md", "text/markdown"),
}

/** Which service dates an export covers. */
sealed interface ExportDates {
    data object AllTime : ExportDates
    data object ThisYear : ExportDates

    /** Both ends included. */
    data class Custom(val from: LocalDate, val to: LocalDate) : ExportDates
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.PDF,
    val dates: ExportDates = ExportDates.AllTime,
    val costsAndNotes: Boolean = true,
    val scheduleAndRecalls: Boolean = true,
    /** "What it takes": the oil, tires, fluids and parts the owner has entered. */
    val whatItTakes: Boolean = true,
    /** The list of documents on file: their type, name and date, never their contents. */
    val documents: Boolean = true,
    /** Receipt and parts photos; only a PDF can carry them. */
    val photos: Boolean = true,
) {
    val includesPhotos: Boolean get() = photos && format == ExportFormat.PDF
}

/** One service visit as it is written out. [photoFiles] are names in the encrypted photo folder. */
data class ExportRecord(
    val date: LocalDate,
    val odometer: Int,
    val items: List<String>,
    /** "DIY", or the shop's name. */
    val doneBy: String,
    val costCents: Long?,
    val notes: String?,
    val photoFiles: List<String> = emptyList(),
    /** A bare date and mileage the owner gave afterwards; [doneBy] means nothing for it. */
    val enteredAsLastDone: Boolean = false,
    /** Came from a file the owner imported, not logged in the app at the time. */
    val imported: Boolean = false,
    /** Shared with the other records the owner linked as one visit (#307); null for one in none. */
    val visitId: Long? = null,
    /** The owner did the work; [doneBy] says it in words. */
    val diy: Boolean = false,
    /** Each of [items]' group in the PDF report, in the same order. */
    val categories: List<ServiceGroup> = emptyList(),
)

/** [itemId] is the schedule item the line is, to find its status in [ReportFacts]. */
data class ExportScheduleLine(val name: String, val every: String, val lastDone: LastDone?, val itemId: Long? = null)

/** [completedOn] is the owner's own mark; NHTSA can't confirm a repair. */
/** [archivedOn] is the owner saying it doesn't apply to this vehicle. */
data class ExportRecall(val campaign: String, val component: String, val completedOn: LocalDate?, val archivedOn: LocalDate? = null)

/** One of the vehicle's documents as an export lists it. [type] is the type's label. */
data class ExportPaper(val type: String, val name: String, val date: LocalDate?, val expiresOn: LocalDate? = null)

data class ExportVehicle(
    /** "2019 Toyota 4Runner SR5" */
    val title: String,
    val nickname: String?,
    val vin: String?,
    /** "CO · ABC 1234" (LicensePlates.label); null when the owner hasn't entered one. */
    val plate: String? = null,
    /** What this vehicle's odometer counts in; every distance below is in it. */
    val unit: DistanceUnit,
    /** The highest reading on record, as the dash shows it. */
    val odometer: Int?,
    val soldOn: LocalDate?,
    /** Set when the instrument cluster was replaced; record readings are as they were read at the time. */
    val clusterSwap: ClusterSwap? = null,
    /** Every record; [Exports.document] narrows them to the chosen dates. */
    val records: List<ExportRecord>,
    val schedule: List<ExportScheduleLine>,
    val recalls: List<ExportRecall>,
    /** "What it takes", as (label, one line), only the rows the owner has filled in (WhatItTakes.lines). */
    val whatItTakes: List<Pair<String, String>> = emptyList(),
    /** The documents the owner keeps for it, in the order the Documents screen lists them. */
    val documents: List<ExportPaper> = emptyList(),
    /** "4WD · 4.0L V6": the drive and engine as stored; null when neither is known. */
    val specs: String? = null,
    /** What only the PDF report uses; null leaves its charts and "due next" empty. */
    val report: ReportFacts? = null,
) {
    /** Null when no record in the export has a cost. */
    val totalCents: Long? get() = Spending.total(records.map { it.costCents })

    /**
     * [records] as the export writes them: a visit's records together as one entry, where the first of them
     * comes, and every other record an entry of its own. A visit cut down to one record by the dates is no visit.
     */
    val entries: List<List<ExportRecord>> get() = Visits.grouped(records) { it.visitId }
}

/** Everything one export says, already filtered and ordered: both writers just lay it out. */
data class ExportDocument(val exportedOn: LocalDate, val options: ExportOptions, val vehicles: List<ExportVehicle>) {
    val recordCount: Int get() = vehicles.sumOf { it.records.size }
    val photoFiles: List<String> get() = if (options.includesPhotos) vehicles.flatMap { v -> v.records.flatMap { it.photoFiles } } else emptyList()
}

object Exports {
    fun inRange(date: LocalDate, dates: ExportDates, today: LocalDate): Boolean = when (dates) {
        ExportDates.AllTime -> true
        ExportDates.ThisYear -> date.year == today.year
        is ExportDates.Custom -> !date.isBefore(dates.from) && !date.isAfter(dates.to)
    }

    /**
     * Applies [options] to [vehicles]: records outside the dates go, the rest are put newest first,
     * and whatever a toggle switched off is emptied, so neither writer has to check the options again.
     */
    fun document(vehicles: List<ExportVehicle>, options: ExportOptions, today: LocalDate): ExportDocument = ExportDocument(
        exportedOn = today,
        options = options,
        vehicles = vehicles.map { v ->
            v.copy(
                records = v.records
                    .filter { inRange(it.date, options.dates, today) }
                    .sortedWith(compareByDescending<ExportRecord> { it.date }.thenByDescending { it.odometer })
                    .map { r ->
                        r.copy(
                            costCents = r.costCents.takeIf { options.costsAndNotes },
                            notes = r.notes.takeIf { options.costsAndNotes },
                            photoFiles = r.photoFiles.takeIf { options.includesPhotos }.orEmpty(),
                        )
                    },
                schedule = v.schedule.takeIf { options.scheduleAndRecalls }.orEmpty(),
                recalls = v.recalls.takeIf { options.scheduleAndRecalls }.orEmpty(),
                whatItTakes = v.whatItTakes.takeIf { options.whatItTakes }.orEmpty(),
                documents = v.documents.takeIf { options.documents }.orEmpty(),
            )
        },
    )

    /** "glovebox-4runner-2026-10-10.md". [subject] is the vehicle's short name, or null for all vehicles. */
    fun fileName(subject: String?, date: LocalDate, format: ExportFormat): String =
        "glovebox-${slug(subject ?: "all-vehicles")}-$date.${format.extension}"

    /** Lower-case letters and digits joined by single hyphens, safe in any file system. */
    fun slug(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).trim('-').ifEmpty { "vehicle" }

    /** The vehicle's real mileage, where a replaced cluster makes it differ from the dash. */
    fun actualMileage(vehicle: ExportVehicle): Int? = vehicle.clusterSwap?.let { swap -> vehicle.odometer?.let { swap.actual(it) } }
}

/** The phrases an export is written with; [ExportWords] gives each in the app's language. */
enum class ExportPhrase {
    ALL_TIME, DATE_RANGE, CLUSTER_SHOWS_MORE, CLUSTER_SHOWS_LESS, TOTAL, LAST_DONE_AT, NOT_LOGGED,
    RECALL_MARKED_DONE, RECALL_ARCHIVED, RECALL_OPEN,
    VISIT, VISIT_TOTAL,
    RECALL_NOTE, LAST_DONE_NOTE, IMPORTED_NOTE, SCHEDULE_NOTE, TAKES_NOTE, DOCUMENTS_NOTE,
    SERVICE_HISTORY, SERVICE_HISTORY_OF, LABELLED, NICKNAME, VIN, PLATE, ODOMETER, ODOMETER_HIGHEST, ACTUAL_MILEAGE,
    INSTRUMENT_CLUSTER, SOLD, PERIOD, RECORDS, EXPORTED_ON,
    WHAT_IT_TAKES, DOCUMENTS_ON_FILE, TYPE, NAME, DATE, EXPIRES, SERVICE_RECORDS, NO_RECORDS, DONE_BY, DONE_BY_LABELLED, COST,
    MAINTENANCE_SCHEDULE, ITEM, EVERY, LAST_DONE, RECALLS, CAMPAIGN, COMPONENT, STATUS, PHOTO_UNREADABLE, PAGE,
}

/**
 * The words an export is written in. `app` gives them from its resources, in the app's language;
 * the tests give the English. Figures, names and notes the owner typed go in as they are.
 */
interface ExportWords {
    fun phrase(phrase: ExportPhrase, vararg args: String): String
    fun records(count: Int): String
    fun date(date: LocalDate): String
    /** A distance in the vehicle's own unit: "68,412 mi" or "110,099 km". */
    fun distance(distance: Int, unit: DistanceUnit): String
    fun money(cents: Long): String
}

/** What both writers say, in [words]: the Markdown here, the PDF in `app`. */
class ExportText(private val words: ExportWords) {
    fun phrase(phrase: ExportPhrase, vararg args: String): String = words.phrase(phrase, *args)

    fun date(date: LocalDate): String = words.date(date)

    fun distance(distance: Int, unit: DistanceUnit): String = words.distance(distance, unit)

    fun money(cents: Long): String = words.money(cents)

    /** "2 records" */
    fun records(count: Int): String = words.records(count)

    fun datesLabel(dates: ExportDates, today: LocalDate): String = when (dates) {
        ExportDates.AllTime -> phrase(ExportPhrase.ALL_TIME)
        ExportDates.ThisYear -> "${today.year}"
        is ExportDates.Custom -> phrase(ExportPhrase.DATE_RANGE, date(dates.from), date(dates.to))
    }

    /** Said wherever readings from two clusters appear together, so the jump isn't taken for tampering. */
    fun clusterNote(swap: ClusterSwap, unit: DistanceUnit): String = phrase(
        if (swap.offset < 0) ExportPhrase.CLUSTER_SHOWS_MORE else ExportPhrase.CLUSTER_SHOWS_LESS,
        date(swap.on),
        distance(kotlin.math.abs(swap.offset), unit),
    )

    /** "4 records · $1,234.56 total" */
    fun summary(vehicle: ExportVehicle): String = listOfNotNull(
        words.records(vehicle.records.size),
        vehicle.totalCents?.let { phrase(ExportPhrase.TOTAL, money(it)) },
    ).joinToString(" · ")

    fun lastDone(line: ExportScheduleLine, unit: DistanceUnit): String =
        line.lastDone?.let { phrase(ExportPhrase.LAST_DONE_AT, date(it.date), distance(it.odometer, unit)) } ?: phrase(ExportPhrase.NOT_LOGGED)

    fun recallStatus(recall: ExportRecall): String = recall.completedOn?.let { phrase(ExportPhrase.RECALL_MARKED_DONE, date(it)) }
        ?: recall.archivedOn?.let { phrase(ExportPhrase.RECALL_ARCHIVED, date(it)) }
        ?: phrase(ExportPhrase.RECALL_OPEN)

    fun paperDate(paper: ExportPaper): String = paper.date?.let { date(it) } ?: ""

    fun paperExpiry(paper: ExportPaper): String = paper.expiresOn?.let { date(it) } ?: ""

    /** A visit's heading: the first record's date, and the reading when every record gives the same one. */
    fun visitHeading(visit: List<ExportRecord>, unit: DistanceUnit): String =
        listOfNotNull(date(visit.first().date), Visits.readings(visit.map { it.odometer }).singleOrNull()?.let { distance(it, unit) }).joinToString(" · ")

    /** What a record in [visit] says of its own date and reading: only where they differ from the rest, as entered. */
    fun visitOwn(r: ExportRecord, visit: List<ExportRecord>, unit: DistanceUnit): String? = listOfNotNull(
        date(r.date).takeIf { visit.any { it.date != r.date } },
        distance(r.odometer, unit).takeIf { Visits.readings(visit.map { it.odometer }).size > 1 },
    ).joinToString(" · ").ifEmpty { null }

    /** The visit's total, only when more than one of its records has a cost: one alone would only be repeated. */
    fun visitTotal(visit: List<ExportRecord>): Long? =
        Visits.total(visit.map { it.costCents }).takeIf { visit.count { it.costCents != null } > 1 }

    private fun labelled(label: ExportPhrase, value: String): String = phrase(ExportPhrase.LABELLED, phrase(label), value)

    /** The whole document as Markdown. */
    fun markdown(doc: ExportDocument): String = buildString {
        val today = doc.exportedOn
        doc.vehicles.forEachIndexed { i, v ->
            if (i > 0) append("\n---\n\n")
            append("# ${phrase(ExportPhrase.SERVICE_HISTORY_OF, text(v.title))}\n\n")
            v.nickname?.let { append("- ${labelled(ExportPhrase.NICKNAME, text(it))}\n") }
            v.vin?.let { append("- ${labelled(ExportPhrase.VIN, "`$it`")}\n") }
            v.plate?.let { append("- ${labelled(ExportPhrase.PLATE, "`$it`")}\n") }
            v.odometer?.let { append("- ${labelled(ExportPhrase.ODOMETER, phrase(ExportPhrase.ODOMETER_HIGHEST, distance(it, v.unit)))}\n") }
            Exports.actualMileage(v)?.let { append("- ${labelled(ExportPhrase.ACTUAL_MILEAGE, distance(it, v.unit))}\n") }
            v.clusterSwap?.let { append("- ${labelled(ExportPhrase.INSTRUMENT_CLUSTER, clusterNote(it, v.unit))}\n") }
            v.soldOn?.let { append("- ${labelled(ExportPhrase.SOLD, date(it))}\n") }
            append("- ${labelled(ExportPhrase.PERIOD, datesLabel(doc.options.dates, today))}\n")
            append("- ${summary(v)}\n")
            append("- ${phrase(ExportPhrase.EXPORTED_ON, date(today))}\n")

            if (v.whatItTakes.isNotEmpty()) {
                append("\n## ${phrase(ExportPhrase.WHAT_IT_TAKES)}\n\n| | |\n|---|---|\n")
                v.whatItTakes.forEach { (label, line) -> append("| ${cell(label)} | ${cell(line)} |\n") }
                append("\n${phrase(ExportPhrase.TAKES_NOTE)}\n")
            }

            if (v.documents.isNotEmpty()) {
                append("\n## ${phrase(ExportPhrase.DOCUMENTS_ON_FILE)}\n\n")
                append("| ${phrase(ExportPhrase.TYPE)} | ${phrase(ExportPhrase.NAME)} | ${phrase(ExportPhrase.DATE)} | ${phrase(ExportPhrase.EXPIRES)} |\n|---|---|---|---|\n")
                v.documents.forEach { append("| ${cell(it.type)} | ${cell(it.name)} | ${paperDate(it)} | ${paperExpiry(it)} |\n") }
                append("\n${phrase(ExportPhrase.DOCUMENTS_NOTE)}\n")
            }

            append("\n## ${phrase(ExportPhrase.SERVICE_RECORDS)}\n\n")
            if (v.records.isEmpty()) append("${phrase(ExportPhrase.NO_RECORDS)}\n")
            v.entries.forEach { entry ->
                if (entry.size == 1) {
                    val r = entry.single()
                    append("### ${date(r.date)} · ${distance(r.odometer, v.unit)}\n\n")
                    r.items.forEach { append("- ${text(it)}\n") }
                    append("\n")
                    source(r, own = null, bold = false)
                    afterLines(r)
                } else {
                    // A visit (#307): one entry, each record with its own source, lines, cost and notes.
                    append("### ${visitHeading(entry, v.unit)}\n\n${phrase(ExportPhrase.VISIT, records(entry.size))}\n\n")
                    entry.forEach { r ->
                        source(r, visitOwn(r, entry, v.unit), bold = true)
                        append("\n")
                        r.items.forEach { append("- ${text(it)}\n") }
                        afterLines(r)
                    }
                    visitTotal(entry)?.let { append("${phrase(ExportPhrase.VISIT_TOTAL, money(it))}\n\n") }
                }
            }

            if (v.schedule.isNotEmpty()) {
                append("## ${phrase(ExportPhrase.MAINTENANCE_SCHEDULE)}\n\n")
                append("| ${phrase(ExportPhrase.ITEM)} | ${phrase(ExportPhrase.EVERY)} | ${phrase(ExportPhrase.LAST_DONE)} |\n|---|---|---|\n")
                v.schedule.forEach { append("| ${cell(it.name)} | ${cell(it.every)} | ${lastDone(it, v.unit)} |\n") }
                append("\n${phrase(ExportPhrase.SCHEDULE_NOTE)}\n\n")
            }
            if (v.recalls.isNotEmpty()) {
                append("## ${phrase(ExportPhrase.RECALLS)}\n\n")
                append("| ${phrase(ExportPhrase.CAMPAIGN)} | ${phrase(ExportPhrase.COMPONENT)} | ${phrase(ExportPhrase.STATUS)} |\n|---|---|---|\n")
                v.recalls.forEach { append("| ${cell(it.campaign)} | ${cell(it.component)} | ${recallStatus(it)} |\n") }
                append("\n${phrase(ExportPhrase.RECALL_NOTE)}\n\n")
            }
        }
    }.trimEnd() + "\n"

    /**
     * Who did a record's work, and its cost. On its own it follows the record's lines; in a visit it heads them, in
     * bold, with the record's own date and reading where they differ from the rest ([own]).
     */
    private fun StringBuilder.source(r: ExportRecord, own: String?, bold: Boolean) {
        val by = (if (r.enteredAsLastDone) phrase(ExportPhrase.LAST_DONE_NOTE) else phrase(ExportPhrase.DONE_BY_LABELLED, text(r.doneBy)))
            .let { if (bold) "**$it**" else it }
        append(listOfNotNull(by, own, r.costCents?.let { phrase(ExportPhrase.COST, money(it)) }).joinToString(" · ") + "\n")
    }

    /** A record's imported note and notes, after its lines. */
    private fun StringBuilder.afterLines(r: ExportRecord) {
        if (r.imported) append("\n${phrase(ExportPhrase.IMPORTED_NOTE)}\n")
        r.notes?.let { notes -> append("\n" + notes.lines().joinToString("\n") { "> ${text(it)}" } + "\n") }
        append("\n")
    }

    private fun text(raw: String): String = raw.replace("\\", "\\\\").replace(Regex("([*_`#\\[\\]<>|])"), "\\\\$1")

    /** [text], on one line, for a table cell. */
    private fun cell(raw: String): String = text(raw.replace(Regex("\\s+"), " ").trim())
}
