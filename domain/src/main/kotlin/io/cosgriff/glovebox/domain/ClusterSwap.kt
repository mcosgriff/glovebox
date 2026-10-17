package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.util.Locale

/**
 * An instrument cluster replacement. From [on], the dash reads [offset] miles less than the old
 * cluster would have; [offset] is negative when the replacement came with more miles on it.
 * [startReading] is what the new cluster showed when it went in.
 *
 * Readings are stored as they were typed, so setting or correcting a swap never rewrites one.
 * Everything worked out from them (reminders, pace, due points, the current odometer) is in what
 * the dash reads now, so it can be checked against the dash: readings from before the swap are
 * shifted down by [offset] on the way in ([toDash]) and back on the way out ([asReadOn]).
 */
data class ClusterSwap(val on: LocalDate, val offset: Int, val startReading: Int) {
    /** The new cluster's first reading, which is a reading like any other. */
    val reading: OdometerReading get() = OdometerReading(on, startReading)
}

/** A [reading] taken on [date], as the cluster now in the dash would have shown it. */
fun ClusterSwap?.toDash(reading: Int, date: LocalDate): Int =
    if (this != null && date.isBefore(on)) reading - offset else reading

/** The reverse of [toDash]: what the cluster in the dash on [date] showed. */
fun ClusterSwap?.asReadOn(dash: Int, date: LocalDate): Int =
    if (this != null && date.isBefore(on)) dash + offset else dash

/** Miles the vehicle has really covered when the dash reads [dash]. */
fun ClusterSwap?.actual(dash: Int): Int = dash + (this?.offset ?: 0)

fun ClusterSwap?.toDash(reading: OdometerReading): OdometerReading = reading.copy(odometer = toDash(reading.odometer, reading.date))

fun ClusterSwap?.toDash(entry: ServiceEntry): ServiceEntry = entry.copy(odometer = toDash(entry.odometer, entry.date))

fun ClusterSwap?.asReadOn(last: LastDone): LastDone = last.copy(odometer = asReadOn(last.odometer, last.date))

/** Standalone readings in dash terms, with the new cluster's first reading among them. */
fun ClusterSwap?.readings(typed: List<OdometerReading>): List<OdometerReading> =
    typed.map { toDash(it) } + listOfNotNull(this?.reading)

enum class SwapProblem { DATE_IN_FUTURE, OLD_READING_MISSING, NEW_READING_MISSING, SAME_READING }

object ClusterSwaps {
    /**
     * What stops the editor's three answers being saved; empty when they are fine. A new cluster
     * may start at zero, so [newReading] is only missing when nothing usable was typed.
     */
    fun problems(on: LocalDate, oldReading: Int?, newReading: Int?, today: LocalDate): Set<SwapProblem> = buildSet {
        if (on.isAfter(today)) add(SwapProblem.DATE_IN_FUTURE)
        if (oldReading == null) add(SwapProblem.OLD_READING_MISSING)
        if (newReading == null) add(SwapProblem.NEW_READING_MISSING)
        if (oldReading != null && oldReading == newReading) add(SwapProblem.SAME_READING)
    }

    fun of(on: LocalDate, oldReading: Int, newReading: Int): ClusterSwap = ClusterSwap(on, oldReading - newReading, newReading)

    /** What the old cluster read when it came out. */
    fun oldReading(swap: ClusterSwap): Int = swap.startReading + swap.offset

    /** Whole miles from zero up; thousands separators are fine. Unlike a reading, zero counts. */
    fun parseReading(text: String): Int? {
        val cleaned = text.filterNot { it == ',' || it.isWhitespace() }
        if (cleaned.isEmpty() || cleaned.length > MileageUpdate.MAX_DIGITS || !cleaned.all(Char::isDigit)) return null
        return cleaned.toInt()
    }
}
