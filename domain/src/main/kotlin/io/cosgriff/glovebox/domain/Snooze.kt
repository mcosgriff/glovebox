package io.cosgriff.glovebox.domain

import java.time.LocalDate

/**
 * A reminder held back until an odometer reading, a date, or whichever of the two comes first.
 *
 * It only quietens the reminder. "Last done" still comes from the service log, so when the snooze
 * passes the reminder shows how far overdue it really is.
 */
data class Snooze(val untilOdometer: Int? = null, val untilDate: LocalDate? = null) {
    init {
        require(untilOdometer != null || untilDate != null) { "A snooze needs an odometer, a date, or both" }
    }

    fun isActive(currentOdometer: Int, today: LocalDate): Boolean =
        (untilOdometer == null || currentOdometer < untilOdometer) && (untilDate == null || today.isBefore(untilDate))
}

enum class SnoozeKind { MILES_500, MILES_1000, WEEKS_2, MONTH_1, NEXT_OIL_CHANGE }

data class SnoozeOption(val kind: SnoozeKind, val snooze: Snooze)

object Snoozing {
    /** [ScheduleTemplateItem.key] of the oil change, which other reminders can be snoozed to. */
    const val OIL_TEMPLATE_KEY = "oil"

    /**
     * What the snooze sheet offers for [item]. Mileage choices need a known odometer; "with the
     * next oil change" needs an oil reminder that has been logged and isn't itself overdue.
     */
    fun options(item: ItemStatus, currentOdometer: Int?, today: LocalDate, oil: ItemStatus?): List<SnoozeOption> = buildList {
        if (currentOdometer != null) {
            add(SnoozeOption(SnoozeKind.MILES_500, Snooze(untilOdometer = currentOdometer + 500)))
            add(SnoozeOption(SnoozeKind.MILES_1000, Snooze(untilOdometer = currentOdometer + 1_000)))
        }
        add(SnoozeOption(SnoozeKind.WEEKS_2, Snooze(untilDate = today.plusWeeks(2))))
        add(SnoozeOption(SnoozeKind.MONTH_1, Snooze(untilDate = today.plusMonths(1))))
        nextOilChange(item, currentOdometer, today, oil)?.let { add(SnoozeOption(SnoozeKind.NEXT_OIL_CHANGE, it)) }
    }

    private fun nextOilChange(item: ItemStatus, currentOdometer: Int?, today: LocalDate, oil: ItemStatus?): Snooze? {
        if (oil == null || oil.item.id == item.item.id || oil.lastDone == null || oil.snooze != null) return null
        if (oil.status.state == DueState.OVERDUE) return null
        val odometer = oil.status.dueAtOdometer?.takeIf { currentOdometer != null && it > currentOdometer }
        val date = oil.status.dueByDate?.takeIf { it.isAfter(today) }
        return if (odometer == null && date == null) null else Snooze(odometer, date)
    }

    /**
     * "Skip this one": hold the reminder until the interval after the one that was missed. No
     * service is recorded, so the log stays truthful and the reminder returns if that passes too.
     * Null when [item] has never been logged — there is no interval to skip yet.
     */
    fun skip(item: ItemStatus, currentOdometer: Int?, today: LocalDate): Snooze? {
        if (item.lastDone == null) return null
        val odometer = item.status.dueAtOdometer?.let { due ->
            val step = item.item.interval.miles!!
            var next = due + step
            while (currentOdometer != null && next <= currentOdometer) next += step
            next
        }
        val date = item.status.dueByDate?.let { due ->
            val step = item.item.interval.months!!.toLong()
            var next = due.plusMonths(step)
            while (!next.isAfter(today)) next = next.plusMonths(step)
            next
        }
        return if (odometer == null && date == null) null else Snooze(odometer, date)
    }
}
