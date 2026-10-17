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

/** The line under an odometer field; `app` words it. Readings are as the dash reads them. */
sealed interface OdometerNote {
    val tone: NoteTone

    /** Saving was tried with no reading. */
    data object Missing : OdometerNote {
        override val tone = NoteTone.WARNING
    }

    /** Below a reading from on or before this date; it is said without a unit, as a reading to look for. */
    data class LowerThanEarlier(val earlier: Int) : OdometerNote {
        override val tone = NoteTone.WARNING
    }

    /** Editing a record: what its reading was before. */
    data class Was(val original: Int) : OdometerNote {
        override val tone = NoteTone.CHANGED
    }

    data object Unchanged : OdometerNote {
        override val tone = NoteTone.PLAIN
    }

    /** Logging a new service: the vehicle's last reading. */
    data class LastRecorded(val reading: Int) : OdometerNote {
        override val tone = NoteTone.PLAIN
    }

    /** "When were these last done?": above the vehicle's latest reading, which it would then become. */
    data class HigherThanLatest(val latest: Int) : OdometerNote {
        override val tone = NoteTone.WARNING
    }

    data class Latest(val reading: Int) : OdometerNote {
        override val tone = NoteTone.PLAIN
    }
}

/** Who did a record's work, as History says it; `app` words it. */
sealed interface DoneBy {
    /** A bare "last done" (#78), which doesn't say who. */
    data object EnteredAsLastDone : DoneBy

    data object Diy : DoneBy

    /** [name] is the shop's, when one was given. */
    data class Shop(val name: String?) : DoneBy
}

/** [imported] came from an import file; [mark] is set while the record is just added or restored. */
data class Who(val by: DoneBy, val imported: Boolean, val mark: RecordMark?)

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
            return if (showErrors) OdometerNote.Missing else lastOdometer?.let(OdometerNote::LastRecorded)
        }
        // Worth a warning, not a block: the earlier reading may be the wrong one.
        val earlier = readings.filter { !it.date.isAfter(date) && it.odometer > entered }.maxOfOrNull { it.odometer }
        return when {
            earlier != null -> OdometerNote.LowerThanEarlier(earlier)
            original == null -> lastOdometer?.let(OdometerNote::LastRecorded)
            entered != original -> OdometerNote.Was(original)
            else -> OdometerNote.Unchanged
        }
    }

    /**
     * Who did the work, as the History list says it; a record just added or restored says so. A bare
     * "last done" ([enteredAsLastDone]) doesn't say who, so it says what it is instead, and a record
     * that came from an import file ([imported]) is marked as one.
     */
    fun who(diy: Boolean, shopName: String?, mark: RecordMark?, enteredAsLastDone: Boolean = false, imported: Boolean = false): Who {
        val by = when {
            enteredAsLastDone -> DoneBy.EnteredAsLastDone
            diy -> DoneBy.Diy
            else -> DoneBy.Shop(shopName?.trim()?.takeIf { it.isNotEmpty() })
        }
        return Who(by, imported, mark)
    }

    /** True when [entry] is the most recent time [itemId] was done. */
    fun isLatestFor(itemId: Long, entry: ServiceEntry, entries: List<ServiceEntry>): Boolean =
        itemId in entry.itemIds && ServiceLog.lastDone(itemId, entries) == LastDone(entry.date, entry.odometer)
}

fun Money.format(cents: Long): String =
    String.format(Locale.US, "$%,d.%02d", cents / 100, cents % 100)

fun ServiceCatalog.categoryOf(catalogId: String?): CatalogCategory? =
    catalogId?.let { id -> categories.firstOrNull { c -> c.items.any { it.id == id } } }

/**
 * The Log service line a bare "last done" stands for: the one that resets the typical item made
 * from [templateKey], or the one the user's own item answers to ([service]). Null for an item no
 * line covers.
 */
fun ServiceCatalog.lineFor(templateKey: String?, service: String? = null): CatalogItem? {
    val items = categories.flatMap { it.items }
    return if (templateKey != null) items.firstOrNull { it.resets == templateKey } else items.firstOrNull { it.id == service }
}

/** The ids of the Log service lines that reset a reminder (`app` names them): see [lineFor]. */
fun ServiceCatalog.resetters(templateKey: String?, service: String? = null): List<String> = listOfNotNull(lineFor(templateKey, service)?.id)
