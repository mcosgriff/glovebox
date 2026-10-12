package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LastDoneEntryTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val oil = TrackedItem(1, "Oil & filter", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
    private val brakes = TrackedItem(2, "Brake fluid", Interval(null, 36), DueRule.TIME_ONLY)
    private val coolant = TrackedItem(3, "Coolant", Interval(100_000, 120), DueRule.WHICHEVER_FIRST)

    @Test
    fun `both a date that has happened and a mileage are needed`() {
        assertTrue(LastDoneEntries.problems(today, 60_100, today).isEmpty())
        assertEquals(setOf(LastDoneProblem.DATE_IN_FUTURE), LastDoneEntries.problems(today.plusDays(1), 60_100, today))
        assertEquals(setOf(LastDoneProblem.ODOMETER_MISSING), LastDoneEntries.problems(today.minusYears(2), null, today))
    }

    @Test
    fun `the note shows the latest reading and questions one above it`() {
        assertEquals(OdometerNote("Latest reading: 68,412 mi", NoteTone.PLAIN), LastDoneEntries.odometerNote(null, 68_412, false))
        assertEquals(OdometerNote("Latest reading: 68,412 mi", NoteTone.PLAIN), LastDoneEntries.odometerNote(60_100, 68_412, false))
        assertEquals(NoteTone.WARNING, LastDoneEntries.odometerNote(70_000, 68_412, false)?.tone)
        assertEquals(NoteTone.WARNING, LastDoneEntries.odometerNote(null, 68_412, true)?.tone)
        // A vehicle with no mileage yet has nothing to compare with.
        assertNull(LastDoneEntries.odometerNote(60_100, null, false))
    }

    @Test
    fun `never-logged reminders come first, and an entered one is told from a logged one`() {
        val entries = listOf(
            ServiceEntry(10, LocalDate.of(2026, 6, 14), 60_100, setOf(1)), // logged service
            ServiceEntry(11, LocalDate.of(2024, 3, 1), 41_000, setOf(3)), // bare last done
        )
        val rows = LastDoneEntries.rows(listOf(oil, brakes, coolant), entries, enteredIds = setOf(11), swap = null)

        assertEquals(listOf("Brake fluid", "Coolant", "Oil & filter"), rows.map { it.item.name })
        assertNull(rows[0].lastDone)
        assertEquals(LastDone(LocalDate.of(2024, 3, 1), 41_000), rows[1].lastDone)
        assertTrue(rows[1].entered)
        assertEquals(10L, rows[2].recordId)
        assertTrue(!rows[2].entered)
    }

    @Test
    fun `entering a last done starts the reminder counting like any record`() {
        val before = VehicleReminders.statuses(listOf(oil), emptyList(), listOf(OdometerReading(today, 68_412)), today).single()
        assertNull(before.lastDone)

        val entered = ServiceEntry(1, LocalDate.of(2026, 6, 14), 64_000, setOf(1))
        val after = VehicleReminders.statuses(listOf(oil), listOf(entered), listOf(OdometerReading(today, 68_412)), today).single()
        assertEquals(69_000, after.status.dueAtOdometer)
        assertEquals(588, after.status.milesRemaining)
    }

    @Test
    fun `a last done from before a cluster replacement is shown as it read that day`() {
        val swap = ClusterSwaps.of(LocalDate.of(2026, 3, 10), oldReading = 120_000, newReading = 0)
        val typed = ServiceEntry(1, LocalDate.of(2025, 12, 1), 118_000, setOf(1))
        val row = LastDoneEntries.rows(listOf(oil), listOf(swap.toDash(typed)), setOf(1), swap).single()
        assertEquals(LastDone(LocalDate.of(2025, 12, 1), 118_000), row.lastDone)
    }

    @Test
    fun `history says what the record is in place of who did it`() {
        assertEquals("Entered as last done", LogEdits.who(diy = true, shopName = null, mark = null, enteredAsLastDone = true))
        assertEquals("Entered as last done · just added", LogEdits.who(true, null, RecordMark.ADDED, true))
        assertEquals("DIY", LogEdits.who(true, null, null))
    }
}
