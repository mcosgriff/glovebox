package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The export's English, as `strings.xml` has it, so the writers' tests read the whole document.
 * `app`'s own words are held to the same English by its device test (`FormatsTest`).
 */
object EnglishExportWords : ExportWords {
    private val longDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    override fun phrase(phrase: ExportPhrase, vararg args: String): String = String.format(Locale.US, ENGLISH.getValue(phrase), *args)

    override fun records(count: Int): String = if (count == 1) "1 record" else "$count records"

    override fun date(date: LocalDate): String = date.format(longDate)

    override fun distance(distance: Int, unit: DistanceUnit): String =
        String.format(Locale.US, "%,d %s", distance, if (unit == DistanceUnit.MILES) "mi" else "km")

    override fun money(cents: Long): String = String.format(Locale.US, "$%,d.%02d", cents / 100, cents % 100)

    val ENGLISH: Map<ExportPhrase, String> = mapOf(
        ExportPhrase.ALL_TIME to "All time",
        ExportPhrase.DATE_RANGE to "%1\$s to %2\$s",
        ExportPhrase.CLUSTER_SHOWS_MORE to "Replaced %1\$s. Readings from that date are the replacement's, which shows %2\$s more than the vehicle has covered; earlier readings are the original's.",
        ExportPhrase.CLUSTER_SHOWS_LESS to "Replaced %1\$s. Readings from that date are the replacement's, which shows %2\$s less than the vehicle has covered; earlier readings are the original's.",
        ExportPhrase.TOTAL to "%1\$s total",
        ExportPhrase.LAST_DONE_AT to "%1\$s at %2\$s",
        ExportPhrase.NOT_LOGGED to "Not logged",
        ExportPhrase.RECALL_MARKED_DONE to "Marked done %1\$s",
        ExportPhrase.RECALL_ARCHIVED to "Archived as not applying %1\$s",
        ExportPhrase.RECALL_OPEN to "Open",
        ExportPhrase.RECALL_NOTE to "Recalls are listed by NHTSA for the year, make and model. \"Marked done\" is the owner's own record; NHTSA can't confirm a repair for a specific vehicle.",
        ExportPhrase.LAST_DONE_NOTE to "Entered by the owner as when this was last done; not logged at the time.",
        ExportPhrase.VISIT to "One visit · %1\$s",
        ExportPhrase.VISIT_TOTAL to "Visit total: %1\$s",
        ExportPhrase.IMPORTED_NOTE to "Imported by the owner from a file; not logged in Glovebox at the time.",
        ExportPhrase.SCHEDULE_NOTE to "Intervals are the ones the owner set in Glovebox, not the manufacturer's schedule.",
        ExportPhrase.TAKES_NOTE to "Entered by the owner from their manuals and the parts they use; Glovebox hasn't checked them against the vehicle.",
        ExportPhrase.DOCUMENTS_NOTE to "Kept by the owner in Glovebox. Only this list is in the export; the documents themselves aren't.",
        ExportPhrase.SERVICE_HISTORY to "Service history",
        ExportPhrase.SERVICE_HISTORY_OF to "Service history: %1\$s",
        ExportPhrase.LABELLED to "%1\$s: %2\$s",
        ExportPhrase.NICKNAME to "Nickname",
        ExportPhrase.VIN to "VIN",
        ExportPhrase.PLATE to "License plate",
        ExportPhrase.ODOMETER to "Odometer",
        ExportPhrase.ODOMETER_HIGHEST to "%1\$s (highest reading on record)",
        ExportPhrase.ACTUAL_MILEAGE to "Actual mileage",
        ExportPhrase.INSTRUMENT_CLUSTER to "Instrument cluster",
        ExportPhrase.SOLD to "Sold",
        ExportPhrase.PERIOD to "Period",
        ExportPhrase.RECORDS to "Records",
        ExportPhrase.EXPORTED_ON to "Exported from Glovebox on %1\$s",
        ExportPhrase.WHAT_IT_TAKES to "What it takes",
        ExportPhrase.DOCUMENTS_ON_FILE to "Documents on file",
        ExportPhrase.EXPIRES to "Expires",
        ExportPhrase.TYPE to "Type",
        ExportPhrase.NAME to "Name",
        ExportPhrase.DATE to "Date",
        ExportPhrase.SERVICE_RECORDS to "Service records",
        ExportPhrase.NO_RECORDS to "No service records in this period.",
        ExportPhrase.DONE_BY to "Done by %1\$s",
        ExportPhrase.DONE_BY_LABELLED to "Done by: %1\$s",
        ExportPhrase.COST to "Cost: %1\$s",
        ExportPhrase.MAINTENANCE_SCHEDULE to "Maintenance schedule",
        ExportPhrase.ITEM to "Item",
        ExportPhrase.EVERY to "Every",
        ExportPhrase.LAST_DONE to "Last done",
        ExportPhrase.RECALLS to "Recalls",
        ExportPhrase.CAMPAIGN to "Campaign",
        ExportPhrase.COMPONENT to "Component",
        ExportPhrase.STATUS to "Status",
        ExportPhrase.PHOTO_UNREADABLE to "[A photo attached to this record couldn't be read]",
        ExportPhrase.PAGE to "Page %1\$s",
    )
}
