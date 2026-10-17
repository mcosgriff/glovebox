package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.util.Locale

enum class LastDoneProblem { DATE_IN_FUTURE, ODOMETER_MISSING }

/** A tracked reminder on the "When were these last done?" list. */
data class LastDoneRow(
    val item: TrackedItem,
    /** Null while it has never been logged. As the odometer read that day. */
    val lastDone: LastDone?,
    /** The record [lastDone] comes from. */
    val recordId: Long?,
    /** True when that record is a bare "last done" and not a logged service. */
    val entered: Boolean,
)

/**
 * A "last done" given on its own: a date and a mileage for one reminder, without the rest of a
 * service record. It is the quick way to start a reminder counting on a vehicle that already has
 * history. It is saved as a service record flagged as such, so "last done" is still only ever
 * derived from the log, and nothing here is assumed or estimated: the user gives both figures.
 */
object LastDoneEntries {
    fun problems(date: LocalDate, odometer: Int?, today: LocalDate): Set<LastDoneProblem> = buildSet {
        if (date.isAfter(today)) add(LastDoneProblem.DATE_IN_FUTURE)
        if (odometer == null) add(LastDoneProblem.ODOMETER_MISSING)
    }

    /**
     * The line under the odometer field. [entered] and [currentOdometer] are both in dash terms. A
     * reading above the current one is allowed, since the current one may be stale, but it would
     * move the vehicle's odometer, so it is worth a second look.
     */
    fun odometerNote(entered: Int?, currentOdometer: Int?, showErrors: Boolean): OdometerNote? = when {
        entered == null && showErrors -> OdometerNote.Missing
        entered != null && currentOdometer != null && entered > currentOdometer -> OdometerNote.HigherThanLatest(currentOdometer)
        else -> currentOdometer?.let(OdometerNote::Latest)
    }

    /**
     * Every tracked reminder with what the log says about it: never-logged ones first, since those
     * are what the list is for, then by name. [entries] are in dash terms; [enteredIds] are the
     * records that are bare "last done" entries.
     */
    fun rows(items: List<TrackedItem>, entries: List<ServiceEntry>, enteredIds: Set<Long>, swap: ClusterSwap?): List<LastDoneRow> =
        items.map { item ->
            val last = ServiceLog.lastEntry(item.id, entries)
            LastDoneRow(
                item = item,
                lastDone = last?.let { swap.asReadOn(LastDone(it.date, it.odometer)) },
                recordId = last?.id,
                entered = last != null && last.id in enteredIds,
            )
        }.sortedWith(compareBy<LastDoneRow> { it.lastDone != null }.thenBy { it.item.name })
}
