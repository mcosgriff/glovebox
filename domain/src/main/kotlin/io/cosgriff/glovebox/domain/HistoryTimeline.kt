package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One row of History's timeline: a year marker, an entry, or the time between two entries. */
sealed interface TimelineRow<out T> {
    /** Starts the entries of [year]; the newest year comes first. */
    data class Year(val year: Int) : TimelineRow<Nothing>

    data class Entry<T>(val entry: T, val date: LocalDate) : TimelineRow<T>

    /** Whole calendar months between the entries either side; 0 is under a month. */
    data class Gap(val months: Int) : TimelineRow<Nothing>
}

/**
 * History as a timeline: past entries newest first, under year markers, with how far apart each is from
 * the next. It only lays out what was logged; nothing is worked out about what wasn't.
 */
object HistoryTimeline {
    /**
     * [entries] newest first (ties keep the order given), each year's opening with a [TimelineRow.Year], and a
     * [TimelineRow.Gap] between every two entries next to each other. The gap comes before a new year's marker.
     */
    fun <T> rows(entries: List<T>, date: (T) -> LocalDate): List<TimelineRow<T>> {
        val sorted = entries.map { it to date(it) }.sortedByDescending { it.second }
        val rows = mutableListOf<TimelineRow<T>>()
        var previous: LocalDate? = null
        for ((entry, day) in sorted) {
            previous?.let { rows += TimelineRow.Gap(monthsApart(it, day)) }
            if (previous?.year != day.year) rows += TimelineRow.Year(day.year)
            rows += TimelineRow.Entry(entry, day)
            previous = day
        }
        return rows
    }

    /** Whole calendar months from [older] to [newer]: Oct 18 to Nov 15 is 0, Oct 18 to Nov 18 is 1. Never negative. */
    fun monthsApart(newer: LocalDate, older: LocalDate): Int =
        ChronoUnit.MONTHS.between(minOf(newer, older), maxOf(newer, older)).toInt()
}
