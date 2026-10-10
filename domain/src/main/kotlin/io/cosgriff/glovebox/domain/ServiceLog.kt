package io.cosgriff.glovebox.domain

import java.time.LocalDate

/** A logged service visit. [itemIds] are the schedule items this visit resets. */
data class ServiceEntry(
    val id: Long,
    val date: LocalDate,
    val odometer: Int,
    val itemIds: Set<Long>,
)

object ServiceLog {
    /**
     * The most recent time [itemId] was done, derived from the log.
     *
     * Because "last done" is always recomputed from the records, editing or deleting a
     * record automatically rolls reminders back — there is no stored value to fix up.
     */
    fun lastDone(itemId: Long, entries: List<ServiceEntry>): LastDone? =
        entries
            .filter { itemId in it.itemIds }
            .maxWithOrNull(compareBy<ServiceEntry> { it.date }.thenBy { it.odometer })
            ?.let { LastDone(it.date, it.odometer) }

    /** Readings usable for pace estimation, from records plus standalone mileage updates. */
    fun readings(entries: List<ServiceEntry>, updates: List<OdometerReading>): List<OdometerReading> =
        entries.map { OdometerReading(it.date, it.odometer) } + updates

    /** Warn when an entered odometer is lower than an earlier reading. */
    fun isBelowPreviousReading(odometer: Int, date: LocalDate, readings: List<OdometerReading>): Boolean =
        readings.any { !it.date.isAfter(date) && it.odometer > odometer }
}
