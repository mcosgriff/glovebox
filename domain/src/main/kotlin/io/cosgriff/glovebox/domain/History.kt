package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.util.Locale

/** A reminder whose "last done" moves when the service log is edited. */
data class ReminderChange(val item: TrackedItem, val before: LastDone?, val after: LastDone?)

/** When a service next comes due, counted from the last time it was done. Either part may not apply. */
data class NextDue(val odometer: Int?, val date: LocalDate?)

fun Interval.nextDueAfter(last: LastDone): NextDue =
    NextDue(miles?.let { last.odometer + it }, months?.let { last.date.plusMonths(it.toLong()) })

enum class NoteTone { PLAIN, CHANGED, WARNING }

/** Why a record is picked out in the History list. */
enum class RecordMark { ADDED, RESTORED }

data class OdometerNote(val text: String, val tone: NoteTone)

object LogEdits {
    /**
     * Which reminders move if the log goes from [before] to [after] — used to preview an edit or
     * delete. Works because "last done" is always derived from the log.
     */
    fun reminderChanges(
        items: List<TrackedItem>,
        before: List<ServiceEntry>,
        after: List<ServiceEntry>,
    ): List<ReminderChange> = items.mapNotNull { item ->
        val was = ServiceLog.lastDone(item.id, before)
        val now = ServiceLog.lastDone(item.id, after)
        if (was == now) null else ReminderChange(item, was, now)
    }

    /** How long a delete or an edit can be undone from the bar at the bottom of the screen. */
    const val UNDO_SECONDS = 30

    /**
     * The line under the odometer field on the Log service and Edit service forms.
     * [original] is the record's reading when editing, null when logging a new service.
     * [readings] are the vehicle's other readings, without the record being edited.
     */
    fun odometerNote(
        entered: Int?,
        original: Int?,
        date: LocalDate,
        readings: List<OdometerReading>,
        lastOdometer: Int?,
        showErrors: Boolean,
    ): OdometerNote? {
        if (entered == null) {
            return if (showErrors) OdometerNote("Enter the odometer reading in whole miles.", NoteTone.WARNING) else lastRecorded(lastOdometer)
        }
        // Worth a warning, not a block: the earlier reading may be the wrong one.
        val earlier = readings.filter { !it.date.isAfter(date) && it.odometer > entered }.maxOfOrNull { it.odometer }
        return when {
            earlier != null -> OdometerNote("Lower than an earlier reading (${miles(earlier)}). Double-check it.", NoteTone.WARNING)
            original == null -> lastRecorded(lastOdometer)
            entered != original -> OdometerNote("Was ${miles(original)}", NoteTone.CHANGED)
            else -> OdometerNote("Unchanged", NoteTone.PLAIN)
        }
    }

    private fun lastRecorded(lastOdometer: Int?): OdometerNote? =
        lastOdometer?.let { OdometerNote("Last recorded: ${miles(it)} mi", NoteTone.PLAIN) }

    private fun miles(value: Int): String = String.format(Locale.US, "%,d", value)

    /**
     * Who did the work, as the History list says it; a record just added or restored says so. A bare
     * "last done" ([enteredAsLastDone]) doesn't say who, so it says what it is instead, and a record
     * that came from an import file ([imported]) is marked as one.
     */
    fun who(diy: Boolean, shopName: String?, mark: RecordMark?, enteredAsLastDone: Boolean = false, imported: Boolean = false): String {
        val by = when {
            enteredAsLastDone -> LastDoneEntries.TAG
            diy -> "DIY"
            else -> shopName?.trim()?.takeIf { it.isNotEmpty() } ?: "Shop"
        }
        val who = if (imported) "$by \u00b7 imported" else by
        return when (mark) {
            RecordMark.ADDED -> "$who \u00b7 just added"
            RecordMark.RESTORED -> "$who \u00b7 restored"
            null -> who
        }
    }

    /** True when [entry] is the most recent time [itemId] was done. */
    fun isLatestFor(itemId: Long, entry: ServiceEntry, entries: List<ServiceEntry>): Boolean =
        itemId in entry.itemIds && ServiceLog.lastDone(itemId, entries) == LastDone(entry.date, entry.odometer)
}

fun Money.format(cents: Long): String =
    String.format(Locale.US, "$%,d.%02d", cents / 100, cents % 100)

fun ServiceCatalog.categoryOf(catalogId: String?): CatalogCategory? =
    catalogId?.let { id -> categories.firstOrNull { c -> c.items.any { it.id == id } } }

/** The Log service line a bare "last done" for the reminder made from [templateKey] stands for; null for the user's own items. */
fun ServiceCatalog.lineFor(templateKey: String?): CatalogItem? =
    if (templateKey == null) null else categories.flatMap { it.items }.firstOrNull { it.resets == templateKey }

/** The Log service lines that reset the reminder made from [templateKey], as the user sees them there. */
fun ServiceCatalog.resetters(templateKey: String?): List<String> =
    if (templateKey == null) emptyList() else categories.flatMap { it.items }.filter { it.resets == templateKey }.map { it.label }
