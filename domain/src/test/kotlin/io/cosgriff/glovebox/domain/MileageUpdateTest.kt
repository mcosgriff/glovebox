package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MileageUpdateTest {
    private val today = LocalDate.of(2026, 10, 24)

    private val oil = TrackedItem(1, "Oil & filter", Interval(10_000, 12), DueRule.WHICHEVER_FIRST)
    private val rotation = TrackedItem(2, "Tire rotation", Interval(5_000, null), DueRule.WHICHEVER_FIRST)
    private val brakeFluid = TrackedItem(3, "Brake fluid", Interval(null, 36), DueRule.WHICHEVER_FIRST)
    private val wipers = TrackedItem(4, "Wipers", Interval(5_000, null), DueRule.WHICHEVER_FIRST)

    private val inputs = ReminderInputs(
        items = listOf(oil, rotation, brakeFluid, wipers),
        entries = listOf(
            ServiceEntry(1, LocalDate.of(2026, 8, 1), 60_100, setOf(1, 3)),
            ServiceEntry(2, LocalDate.of(2026, 3, 1), 60_100, setOf(2)),
        ),
        updates = listOf(OdometerReading(LocalDate.of(2026, 10, 10), 68_412)),
    )

    @Test
    fun `delta is measured from the highest reading`() {
        val p = MileageUpdate.preview(inputs, 68_950, today)
        assertEquals(OdometerReading(LocalDate.of(2026, 10, 10), 68_412), p.last)
        assertEquals(538, p.delta)
        assertFalse(p.below)
    }

    @Test
    fun `impact lists mileage reminders as they will stand, overdue first`() {
        val p = MileageUpdate.preview(inputs, 68_950, today)
        // Brake fluid counts by time and Wipers was never logged, so neither is listed.
        assertEquals(
            listOf("Tire rotation" to -3_850, "Oil & filter" to 1_150),
            p.impact.map { it.first.item.name to it.second.remaining },
        )
    }

    @Test
    fun `a lower reading is flagged and changes nothing`() {
        val p = MileageUpdate.preview(inputs, 68_000, today)
        assertTrue(p.below)
        assertEquals(-412, p.delta)
        // Reminders still count from the highest reading.
        assertEquals(listOf(-3_312, 1_688), p.impact.map { it.second.remaining })
    }

    @Test
    fun `first reading for a vehicle has nothing to compare with`() {
        val p = MileageUpdate.preview(ReminderInputs(listOf(oil), emptyList(), emptyList()), 12_000, today)
        assertNull(p.last)
        assertNull(p.delta)
        assertFalse(p.below)
        assertTrue(p.impact.isEmpty())
    }

    @Test
    fun `keypad drops leading zeros and stops at seven digits`() {
        assertEquals("", MileageUpdate.press("", '0'))
        assertEquals("6", MileageUpdate.press("", '6'))
        assertEquals("60", MileageUpdate.press("6", '0'))
        assertEquals("1234567", MileageUpdate.press("1234567", '8'))
        assertEquals("12", MileageUpdate.press("12", 'x'))
    }
}
