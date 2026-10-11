package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.util.Locale

/** A reminder whose "last done" moves when the service log is edited. */
data class ReminderChange(val item: TrackedItem, val before: LastDone?, val after: LastDone?)

/** When a service next comes due, counted from the last time it was done. Either part may not apply. */
data class NextDue(val odometer: Int?, val date: LocalDate?)

fun Interval.nextDueAfter(last: LastDone): NextDue =
    NextDue(miles?.let { last.odometer + it }, months?.let { last.date.plusMonths(it.toLong()) })

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

    /** True when [entry] is the most recent time [itemId] was done. */
    fun isLatestFor(itemId: Long, entry: ServiceEntry, entries: List<ServiceEntry>): Boolean =
        itemId in entry.itemIds && ServiceLog.lastDone(itemId, entries) == LastDone(entry.date, entry.odometer)
}

fun Money.format(cents: Long): String =
    String.format(Locale.US, "$%,d.%02d", cents / 100, cents % 100)

fun ServiceCatalog.categoryOf(catalogId: String?): CatalogCategory? =
    catalogId?.let { id -> categories.firstOrNull { c -> c.items.any { it.id == id } } }

/** The Log service lines that reset the reminder made from [templateKey], as the user sees them there. */
fun ServiceCatalog.resetters(templateKey: String?): List<String> =
    if (templateKey == null) emptyList() else categories.flatMap { it.items }.filter { it.resets == templateKey }.map { it.label }
