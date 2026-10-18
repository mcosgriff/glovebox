package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ClusterSwapTest {
    private val swapDay = LocalDate.of(2026, 3, 10)

    /** The old cluster came out at 120,000 mi and a new one went in at zero. */
    private val swap = ClusterSwaps.of(swapDay, oldReading = 120_000, newReading = 0)
    private val none: ClusterSwap? = null

    @Test
    fun `offset is what the new cluster is short by`() {
        assertEquals(120_000, swap.offset)
        assertEquals(120_000, ClusterSwaps.oldReading(swap))
        assertEquals(-2_500, ClusterSwaps.of(swapDay, 80_000, 82_500).offset)
    }

    @Test
    fun `readings before the swap are shifted and later ones are left alone`() {
        assertEquals(-2_000, swap.toDash(118_000, swapDay.minusDays(30)))
        assertEquals(1_500, swap.toDash(1_500, swapDay.plusDays(30)))
        // The day of the swap belongs to the new cluster.
        assertEquals(40, swap.toDash(40, swapDay))
    }

    @Test
    fun `asReadOn undoes toDash on either side of the swap`() {
        for (date in listOf(swapDay.minusYears(1), swapDay.minusDays(1), swapDay, swapDay.plusDays(1))) {
            assertEquals(51_234, swap.asReadOn(swap.toDash(51_234, date), date))
        }
    }

    @Test
    fun `no swap changes nothing`() {
        assertEquals(51_234, none.toDash(51_234, swapDay))
        assertEquals(51_234, none.asReadOn(51_234, swapDay))
        assertEquals(51_234, none.actual(51_234))
        assertEquals(listOf(OdometerReading(swapDay, 9)), none.readings(listOf(OdometerReading(swapDay, 9))))
    }

    @Test
    fun `actual mileage adds the offset back`() {
        assertEquals(123_400, swap.actual(3_400))
    }

    @Test
    fun `the new cluster's first reading counts as a reading`() {
        val readings = swap.readings(listOf(OdometerReading(swapDay.minusDays(10), 119_500)))
        assertEquals(listOf(OdometerReading(swapDay.minusDays(10), -500), OdometerReading(swapDay, 0)), readings)
        // So the current odometer never falls below what the new cluster started at.
        assertEquals(0, VehicleReminders.currentOdometer(emptyList(), readings))
    }

    @Test
    fun `a service before the swap comes due at the right point on the new dash`() {
        val oil = TrackedItem(1, "Oil & filter", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        val typed = listOf(
            ServiceEntry(1, swapDay.minusDays(60), 118_000, setOf(1)), // old cluster
        )
        val updates = listOf(OdometerReading(swapDay.plusDays(40), 2_600)) // new cluster
        val today = swapDay.plusDays(40)

        val status = VehicleReminders.statuses(listOf(oil), typed.map { swap.toDash(it) }, swap.readings(updates), today).single()

        // 118,000 + 5,000 = 123,000 actual, which the new dash shows as 3,000.
        assertEquals(3_000, status.status.dueAtOdometer)
        assertEquals(400, status.status.milesRemaining)
        assertEquals(DueState.DUE_SOON, status.status.state)
        // Last done is shown as it was read at the time.
        assertEquals(LastDone(swapDay.minusDays(60), 118_000), swap.asReadOn(status.lastDone!!))
    }

    @Test
    fun `without the swap the same readings would look 115,000 miles early`() {
        val oil = TrackedItem(1, "Oil & filter", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        val entries = listOf(ServiceEntry(1, swapDay.minusDays(60), 118_000, setOf(1)))
        val status = VehicleReminders.statuses(listOf(oil), entries, listOf(OdometerReading(swapDay.plusDays(40), 2_600)), swapDay.plusDays(40)).single()
        assertEquals(DueState.OK, status.status.state)
    }

    @Test
    fun `an old service logged after the swap is still counted from the old cluster`() {
        val oil = TrackedItem(1, "Oil & filter", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        // Typed in today, but dated before the swap, so the reading is the old cluster's.
        val late = ServiceEntry(7, swapDay.minusDays(5), 119_800, setOf(1))
        val status = VehicleReminders.statuses(listOf(oil), listOf(swap.toDash(late)), swap.readings(emptyList()), swapDay.plusDays(1)).single()
        assertEquals(4_800, status.status.dueAtOdometer)
    }

    @Test
    fun `pace runs straight through the swap`() {
        val readings = swap.readings(
            listOf(OdometerReading(swapDay.minusDays(50), 118_000), OdometerReading(swapDay.plusDays(50), 2_000)),
        )
        assertEquals(40.0, ReminderEngine.milesPerDay(readings)!!, 1e-9)
    }

    @Test
    fun `a cluster with more miles on it has a negative offset`() {
        val used = ClusterSwaps.of(swapDay, oldReading = 80_000, newReading = 82_500)
        assertEquals(82_000, used.toDash(79_500, swapDay.minusDays(3)))
        assertEquals(80_100, used.actual(82_600))
        assertEquals(-2_500, used.offset)
        assertEquals(120_000, swap.offset)
    }

    @Test
    fun `editor answers are checked`() {
        val today = swapDay.plusDays(1)
        assertTrue(ClusterSwaps.problems(swapDay, 120_000, 0, today).isEmpty())
        assertEquals(setOf(SwapProblem.DATE_IN_FUTURE), ClusterSwaps.problems(today.plusDays(1), 120_000, 0, today))
        assertEquals(setOf(SwapProblem.OLD_READING_MISSING, SwapProblem.NEW_READING_MISSING), ClusterSwaps.problems(swapDay, null, null, today))
        assertEquals(setOf(SwapProblem.SAME_READING), ClusterSwaps.problems(swapDay, 500, 500, today))
    }

    @Test
    fun `a new cluster may start at zero`() {
        assertEquals(0, ClusterSwaps.parseReading("0"))
        assertEquals(120_000, ClusterSwaps.parseReading("120,000"))
        assertNull(ClusterSwaps.parseReading(""))
        assertNull(ClusterSwaps.parseReading("12a"))
        assertNull(ClusterSwaps.parseReading("12345678"))
    }
}
