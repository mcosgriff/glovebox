package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
)

data class ExportScheduleLine(val name: String, val every: String, val lastDone: LastDone?)

/** [completedOn] is the owner's own mark; NHTSA can't confirm a repair. */
data class ExportRecall(val campaign: String, val component: String, val completedOn: LocalDate?)

data class ExportVehicle(
    /** "2019 Toyota 4Runner SR5" */
    val title: String,
    val nickname: String?,
    val vin: String?,
    /** The highest reading on record. */
    val odometer: Int?,
    val soldOn: LocalDate?,
    /** Every record; [Exports.document] narrows them to the chosen dates. */
    val records: List<ExportRecord>,
    val schedule: List<ExportScheduleLine>,
    val recalls: List<ExportRecall>,
) {
    /** Null when no record in the export has a cost. */
    val totalCents: Long? get() = records.mapNotNull { it.costCents }.takeIf { it.isNotEmpty() }?.sum()
}

/** Everything one export says, already filtered and ordered: both writers just lay it out. */
data class ExportDocument(val exportedOn: LocalDate, val options: ExportOptions, val vehicles: List<ExportVehicle>) {
    val recordCount: Int get() = vehicles.sumOf { it.records.size }
    val photoFiles: List<String> get() = if (options.includesPhotos) vehicles.flatMap { v -> v.records.flatMap { it.photoFiles } } else emptyList()
}

object Exports {
    private val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

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
            )
        },
    )

    /** "glovebox-4runner-2026-10-10.md". [subject] is the vehicle's short name, or null for all vehicles. */
    fun fileName(subject: String?, date: LocalDate, format: ExportFormat): String =
        "glovebox-${slug(subject ?: "all-vehicles")}-$date.${format.extension}"

    /** Lower-case letters and digits joined by single hyphens, safe in any file system. */
    fun slug(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).trim('-').ifEmpty { "vehicle" }

    fun datesLabel(dates: ExportDates, today: LocalDate): String = when (dates) {
        ExportDates.AllTime -> "All time"
        ExportDates.ThisYear -> "${today.year}"
        is ExportDates.Custom -> "${dates.from.format(longDate)} to ${dates.to.format(longDate)}"
    }

    fun miles(odometer: Int): String = String.format(Locale.US, "%,d mi", odometer)

    fun date(date: LocalDate): String = date.format(longDate)

    /** "4 records · $1,234.56 total" */
    fun summary(vehicle: ExportVehicle): String = listOfNotNull(
        if (vehicle.records.size == 1) "1 record" else "${vehicle.records.size} records",
        vehicle.totalCents?.let { "${Money.format(it)} total" },
    ).joinToString(" · ")

    fun lastDone(line: ExportScheduleLine): String = line.lastDone?.let { "${date(it.date)} at ${miles(it.odometer)}" } ?: "Not logged"

    fun recallStatus(recall: ExportRecall): String = recall.completedOn?.let { "Marked done ${date(it)}" } ?: "Open"

    /** Said under any recall list: where it comes from and what a "done" means. */
    const val RECALL_NOTE = "Recalls are listed by NHTSA for the year, make and model. \"Marked done\" is the owner's own record; " +
        "NHTSA can't confirm a repair for a specific vehicle."

    /** Said under any schedule: the intervals are the owner's settings, not factory data. */
    const val SCHEDULE_NOTE = "Intervals are the ones the owner set in Glovebox, not the manufacturer's schedule."

    fun markdown(doc: ExportDocument): String = buildString {
        val today = doc.exportedOn
        doc.vehicles.forEachIndexed { i, v ->
            if (i > 0) append("\n---\n\n")
            append("# Service history: ${text(v.title)}\n\n")
            v.nickname?.let { append("- Nickname: ${text(it)}\n") }
            v.vin?.let { append("- VIN: `$it`\n") }
            v.odometer?.let { append("- Odometer: ${miles(it)} (highest reading on record)\n") }
            v.soldOn?.let { append("- Sold: ${date(it)}\n") }
            append("- Period: ${datesLabel(doc.options.dates, today)}\n")
            append("- ${summary(v)}\n")
            append("- Exported from Glovebox on ${date(today)}\n")

            append("\n## Service records\n\n")
            if (v.records.isEmpty()) append("No service records in this period.\n")
            v.records.forEach { r ->
                append("### ${date(r.date)} · ${miles(r.odometer)}\n\n")
                r.items.forEach { append("- ${text(it)}\n") }
                append("\nDone by: ${text(r.doneBy)}")
                r.costCents?.let { append(" · Cost: ${Money.format(it)}") }
                append("\n")
                r.notes?.let { notes -> append("\n" + notes.lines().joinToString("\n") { "> ${text(it)}" } + "\n") }
                append("\n")
            }

            if (v.schedule.isNotEmpty()) {
                append("## Maintenance schedule\n\n| Item | Every | Last done |\n|---|---|---|\n")
                v.schedule.forEach { append("| ${cell(it.name)} | ${cell(it.every)} | ${lastDone(it)} |\n") }
                append("\n$SCHEDULE_NOTE\n\n")
            }
            if (v.recalls.isNotEmpty()) {
                append("## Recalls\n\n| Campaign | Component | Status |\n|---|---|---|\n")
                v.recalls.forEach { append("| ${cell(it.campaign)} | ${cell(it.component)} | ${recallStatus(it)} |\n") }
                append("\n$RECALL_NOTE\n\n")
            }
        }
    }.trimEnd() + "\n"

    /** User text, kept from being read as Markdown structure. */
    private fun text(raw: String): String = raw.replace("\\", "\\\\").replace(Regex("([*_`#\\[\\]<>|])"), "\\\\$1")

    /** [text], on one line, for a table cell. */
    private fun cell(raw: String): String = text(raw.replace(Regex("\\s+"), " ").trim())
}
