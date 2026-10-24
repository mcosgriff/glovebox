package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TrackedItemTest {
    /** The old cluster came out at 130,000 mi and a new one went in at zero. */
    private val swap = ClusterSwaps.of(LocalDate.of(2026, 3, 10), oldReading = 130_000, newReading = 0)

    private fun item(
        interval: Interval? = Interval(50_000, null),
        firstMiles: Int? = null,
        severeOnly: Boolean = false,
        useSevere: Boolean = false,
        swap: ClusterSwap? = null,
    ) = TrackedItem.of(1, "Timing belt", interval, DueRule.WHICHEVER_FIRST, null, firstMiles, severeOnly, useSevere, swap)

    @Test
    fun `a job's first mileage is distance covered, counted from the new cluster's zero`() {
        val tracked = item(firstMiles = 100_000, swap = swap)!!
        assertEquals(-130_000, tracked.zeroAt)
        assertEquals(-30_000, tracked.firstAt)
        assertEquals(100_000, item(firstMiles = 100_000)!!.firstAt)
        assertEquals(0, item()!!.zeroAt)
    }

    @Test
    fun `a severe-only job is off on normal use, and a job with no interval is no reminder`() {
        assertNull(item(severeOnly = true))
        assertEquals(Interval(50_000, null), item(severeOnly = true, useSevere = true)!!.interval)
        assertNull(item(interval = null))
    }
}
