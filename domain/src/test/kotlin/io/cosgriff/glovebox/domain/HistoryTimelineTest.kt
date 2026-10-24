package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistoryTimelineTest {
    private data class Rec(val id: Int, val date: LocalDate)

    private fun rec(id: Int, y: Int, m: Int, d: Int) = Rec(id, LocalDate.of(y, m, d))

    @Test
    fun `entries run newest first under their years, with the time between each`() {
        val a = rec(1, 2025, 10, 18)
        val b = rec(2, 2026, 2, 2)
        val c = rec(3, 2025, 11, 15)
        val d = rec(4, 2026, 3, 9)
        val rows = HistoryTimeline.rows(listOf(a, b, c, d)) { it.date }
        assertEquals(
            listOf(
                TimelineRow.Year(2026),
                TimelineRow.Entry(d, d.date),
                TimelineRow.Gap(1),
                TimelineRow.Entry(b, b.date),
                // The gap belongs between the two entries, so it comes before the new year's marker.
                TimelineRow.Gap(2),
                TimelineRow.Year(2025),
                TimelineRow.Entry(c, c.date),
                TimelineRow.Gap(0),
                TimelineRow.Entry(a, a.date),
            ),
            rows,
        )
    }

    @Test
    fun `nothing gives no rows, and one entry has a year and no gap`() {
        assertEquals(emptyList<TimelineRow<Rec>>(), HistoryTimeline.rows(emptyList<Rec>()) { it.date })
        val only = rec(1, 2026, 6, 1)
        assertEquals(listOf(TimelineRow.Year(2026), TimelineRow.Entry(only, only.date)), HistoryTimeline.rows(listOf(only)) { it.date })
    }

    @Test
    fun `entries on one day keep the order given, under a month apart`() {
        val first = rec(1, 2026, 6, 1)
        val second = rec(2, 2026, 6, 1)
        assertEquals(
            listOf(TimelineRow.Year(2026), TimelineRow.Entry(first, first.date), TimelineRow.Gap(0), TimelineRow.Entry(second, second.date)),
            HistoryTimeline.rows(listOf(first, second)) { it.date },
        )
    }

    @Test
    fun `months apart are whole calendar months`() {
        assertEquals(0, HistoryTimeline.monthsApart(LocalDate.of(2025, 11, 15), LocalDate.of(2025, 10, 18)))
        assertEquals(1, HistoryTimeline.monthsApart(LocalDate.of(2025, 11, 18), LocalDate.of(2025, 10, 18)))
        assertEquals(3, HistoryTimeline.monthsApart(LocalDate.of(2026, 6, 14), LocalDate.of(2026, 3, 9)))
        assertEquals(12, HistoryTimeline.monthsApart(LocalDate.of(2026, 3, 1), LocalDate.of(2025, 3, 1)))
        // Either way round.
        assertEquals(12, HistoryTimeline.monthsApart(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 3, 1)))
    }
}
