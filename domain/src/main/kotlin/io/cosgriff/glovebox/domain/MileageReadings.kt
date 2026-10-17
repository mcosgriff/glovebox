package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One odometer reading on a vehicle, exactly as it was typed. [id] is its `odometer_readings` row,
 * or, when [fromRecord], the service record it was logged with.
 */
data class LoggedReading(val id: Long, val date: LocalDate, val odometer: Int, val fromRecord: Boolean = false)

/**
 * A reading as the Mileage readings screen lists it. [current] marks the one the vehicle's odometer
 * is taken from. [outOfLine] and [jump] are the two ways a reading can look mistyped; neither says
 * it is, only that it is the one to look at.
 */
data class ReadingRow(
    val reading: LoggedReading,
    val current: Boolean = false,
    /** It doesn't fit the readings around it: an odometer only goes up. */
    val outOfLine: Boolean = false,
    /** How far it is above the reading before it, when that is further than a vehicle is driven in the time. */
    val jump: Int? = null,
)

/**
 * A vehicle's odometer readings laid out so a wrong one can be found and put right.
 *
 * The current odometer is the highest reading on record, so one mistyped high reading (849,000 for
 * 84,900) becomes the odometer and makes every mileage reminder overdue. Nothing here decides a
 * reading is wrong or changes one: it points, and the user edits or deletes.
 */
object MileageReadings {
    /** A jump smaller than this is never questioned, however quickly it came. In miles; a kilometre vehicle gets 16,000 km and 240 km a day. */
    const val FAR_MILES = 10_000

    /** More than this a day, every day since the reading before, is more than a vehicle is driven. */
    const val FAR_MILES_PER_DAY = 150

    /**
     * Whether [odometer] on [date] is implausibly far above [before], the reading it follows:
     * worth asking about before it is saved, never refused. Both are in the same terms.
     */
    fun farAhead(odometer: Int, date: LocalDate, before: OdometerReading?, unit: DistanceUnit): Boolean {
        if (before == null) return false
        val delta = odometer.toLong() - before.odometer
        val days = ChronoUnit.DAYS.between(before.date, date).coerceAtLeast(1)
        return delta > unit.scheduled(FAR_MILES) && delta > unit.scheduled(FAR_MILES_PER_DAY) * days
    }

    /**
     * Every reading, newest first, with the current one and the doubtful ones marked. [swap] is the
     * vehicle's cluster replacement: readings are compared in dash terms and shown as typed.
     */
    fun rows(readings: List<LoggedReading>, swap: ClusterSwap?, unit: DistanceUnit): List<ReadingRow> {
        // Oldest first, and of two on one day the lower first, as they would have been read.
        val ordered = readings.sortedWith(compareBy<LoggedReading> { it.date }.thenBy { swap.toDash(it.odometer, it.date) })
        val dash = ordered.map { swap.toDash(it.odometer, it.date) }
        val fits = fitting(dash)
        // The odometer is the highest reading, which may be the replacement cluster's own first reading and so none of these.
        val highest = swap.readings(ordered.map { OdometerReading(it.date, it.odometer) }).maxOfOrNull { it.odometer }
        val current = ordered.indices.lastOrNull { dash[it] == highest }
        return ordered.mapIndexed { i, reading ->
            // The reading before it that the rest agree with; a doubtful one is no yardstick.
            val before = (i - 1 downTo 0).firstOrNull { it in fits }?.let { OdometerReading(ordered[it].date, dash[it]) }
            ReadingRow(
                reading = reading,
                current = i == current,
                outOfLine = i !in fits,
                jump = before?.takeIf { i in fits && farAhead(dash[i], reading.date, it, unit) }?.let { dash[i] - it.odometer },
            )
        }.reversed()
    }

    /**
     * The readings that agree with each other: the longest run that never goes down, in date order.
     * Whatever is outside it is the odd one out, which names the one mistyped reading and not every
     * reading it happens to disagree with. Of two runs as long as each other, the one that ends
     * later is believed, since later readings are the ones still being added to.
     * (`HistoryImport.doubtful` does the same for an import, weighing saved readings over new ones.)
     */
    private fun fitting(odometers: List<Int>): Set<Int> {
        if (odometers.isEmpty()) return emptySet()
        val length = IntArray(odometers.size) { 1 }
        val previous = IntArray(odometers.size) { -1 }
        for (i in odometers.indices) {
            for (j in 0 until i) {
                // On a tie the nearer reading is kept as the one before, which keeps the run in recent readings.
                if (odometers[j] <= odometers[i] && length[j] + 1 >= length[i]) {
                    length[i] = length[j] + 1
                    previous[i] = j
                }
            }
        }
        val longest = length.max()
        val fits = mutableSetOf<Int>()
        var at = length.indices.last { length[it] == longest }
        while (at >= 0) {
            fits += at
            at = previous[at]
        }
        return fits
    }

    /** What is wrong with an edited reading, or null when it can be saved. */
    fun problem(odometer: Int?, date: LocalDate, today: LocalDate): ReadingProblem? = when {
        odometer == null -> ReadingProblem.NOT_A_READING
        date.isAfter(today) -> ReadingProblem.IN_FUTURE
        else -> null
    }
}

/** Why an edited reading can't be saved; `app` words it. */
enum class ReadingProblem {
    /** Not a whole number above zero. */
    NOT_A_READING,
    IN_FUTURE,
}
