package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderEngineTest {
    // The 4Runner scenario from the design canvas.
    private val today = LocalDate.of(2026, 10, 10)
    private val odo = 68_412
    private val lastOil = LastDone(LocalDate.of(2026, 6, 14), 60_100)
    private val pace = (68_412 - 60_100) / 118.0 // ~70 mi/day since Jun 14

    @Test
    fun `oil change is OK with 1,688 miles left`() {
        val s = ReminderEngine.status(Interval(10_000, 12), DueRule.WHICHEVER_FIRST, lastOil, odo, today, pace)
        assertEquals(DueState.OK, s.state)
        assertEquals(70_100, s.dueAtOdometer)
        assertEquals(1_688, s.milesRemaining)
        assertEquals(DecidedBy.MILEAGE, s.decidedBy)
    }

    @Test
    fun `tire rotation is overdue by 3,312 miles`() {
        val s = ReminderEngine.status(Interval(5_000, 6), DueRule.WHICHEVER_FIRST, lastOil, odo, today, pace)
        assertEquals(DueState.OVERDUE, s.state)
        assertEquals(-3_312, s.milesRemaining)
    }

    @Test
    fun `whichever-last waits for both limits`() {
        val s = ReminderEngine.status(Interval(5_000, 12), DueRule.WHICHEVER_LAST, lastOil, odo, today, pace)
        assertFalse("miles passed but 12 months haven't", s.state == DueState.OVERDUE)
        assertEquals(DecidedBy.TIME, s.decidedBy)
    }

    @Test
    fun `mileage-only ignores time`() {
        val s = ReminderEngine.status(Interval(10_000, 12), DueRule.MILEAGE_ONLY, lastOil, odo, today, pace)
        assertNull(s.dueByDate)
        assertEquals(70_100, s.dueAtOdometer)
    }

    @Test
    fun `time-only interval works without mileage`() {
        val brakeFluid = Interval(miles = null, months = 24)
        val s = ReminderEngine.status(brakeFluid, DueRule.WHICHEVER_FIRST, lastOil, odo, today, pace)
        assertEquals(LocalDate.of(2028, 6, 14), s.dueByDate)
        assertNull(s.milesRemaining)
    }

    @Test
    fun `due soon inside lead time`() {
        val s = ReminderEngine.status(
            Interval(10_000, 12), DueRule.WHICHEVER_FIRST,
            LastDone(LocalDate.of(2026, 6, 14), 58_800), odo, today, pace,
        )
        assertEquals(388, s.milesRemaining)
        assertEquals(DueState.DUE_SOON, s.state)
    }

    @Test
    fun `never logged counts as due`() {
        val s = ReminderEngine.status(Interval(30_000, null), DueRule.WHICHEVER_FIRST, null, odo, today, pace)
        assertEquals(DueState.OVERDUE, s.state)
    }

    @Test
    fun `pace needs two readings a week apart`() {
        val one = listOf(OdometerReading(today, odo))
        assertNull(ReminderEngine.milesPerDay(one))
        val two = one + OdometerReading(today.minusDays(100), odo - 7_000)
        assertEquals(70.0, ReminderEngine.milesPerDay(two)!!, 0.001)
    }

    @Test
    fun `deleting a record rolls last-done back`() {
        val jun = ServiceEntry(1, LocalDate.of(2026, 6, 14), 60_100, setOf(OIL))
        val oct = ServiceEntry(2, LocalDate.of(2026, 10, 10), 68_412, setOf(OIL, ROTATION))
        assertEquals(68_412, ServiceLog.lastDone(OIL, listOf(jun, oct))!!.odometer)
        assertEquals(60_100, ServiceLog.lastDone(OIL, listOf(jun))!!.odometer)
        assertNull(ServiceLog.lastDone(ROTATION, listOf(jun)))
    }

    @Test
    fun `odometer lower than an earlier reading is flagged`() {
        val readings = listOf(OdometerReading(LocalDate.of(2026, 6, 14), 60_100))
        assertTrue(ServiceLog.isBelowPreviousReading(59_000, today, readings))
        assertFalse(ServiceLog.isBelowPreviousReading(68_450, today, readings))
    }

    private companion object {
        const val OIL = 1L
        const val ROTATION = 2L
    }
}
