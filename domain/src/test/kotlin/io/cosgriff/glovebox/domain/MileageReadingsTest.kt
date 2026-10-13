package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MileageReadingsTest {
    private val jan = LocalDate.of(2026, 1, 10)
    private fun reading(id: Long, miles: Int, days: Long = 0, fromRecord: Boolean = false) = LoggedReading(id, jan.plusDays(days), miles, fromRecord)
    private fun rows(vararg readings: LoggedReading, swap: ClusterSwap? = null) = MileageReadings.rows(readings.toList(), swap, DistanceUnit.MILES)
    private fun ids(rows: List<ReadingRow>, pick: (ReadingRow) -> Boolean) = rows.filter(pick).map { it.reading.id }

    @Test
    fun `readings are listed newest first and the highest is the odometer`() {
        val rows = rows(reading(1, 84_000), reading(3, 86_100, 60), reading(2, 85_000, 30, fromRecord = true))
        assertEquals(listOf(3L, 2L, 1L), rows.map { it.reading.id })
        assertEquals(listOf(3L), ids(rows) { it.current })
        assertTrue(rows.none { it.outOfLine || it.jump != null })
        assertTrue(rows[1].reading.fromRecord)
    }

    @Test
    fun `a reading mistyped high among others is the one out of line, and is still the odometer`() {
        // 849,000 for 84,900, with a later reading that shows it up.
        val rows = rows(reading(1, 84_000), reading(2, 849_000, 20), reading(3, 85_400, 40))
        assertEquals(listOf(2L), ids(rows) { it.outOfLine })
        assertEquals(listOf(2L), ids(rows) { it.current })
        assertTrue(rows.none { it.jump != null })
    }

    @Test
    fun `a reading mistyped high with nothing after it is a jump`() {
        val rows = rows(reading(1, 84_000), reading(2, 849_000, 20))
        assertTrue(rows.none { it.outOfLine })
        assertEquals(765_000, rows.first().jump)
        assertNull(rows.last().jump)
    }

    @Test
    fun `a reading mistyped low is the one out of line`() {
        val rows = rows(reading(1, 84_000), reading(2, 85_000, 20), reading(3, 8_600, 40))
        assertEquals(listOf(3L), ids(rows) { it.outOfLine })
        assertEquals(listOf(2L), ids(rows) { it.current })
    }

    @Test
    fun `one wrong reading doesn't put the readings round it in doubt`() {
        val rows = rows(reading(1, 80_000), reading(2, 82_000, 30), reading(3, 820_000, 60), reading(4, 84_000, 90), reading(5, 86_000, 120))
        assertEquals(listOf(3L), ids(rows) { it.outOfLine })
        // 84,000 is measured against 82,000, not against the wrong one before it.
        assertTrue(rows.none { it.jump != null })
    }

    @Test
    fun `an ordinary drive is never questioned, however quick, nor a long gap`() {
        val before = OdometerReading(jan, 84_000)
        assertFalse(MileageReadings.farAhead(86_500, jan.plusDays(2), before, DistanceUnit.MILES))
        assertFalse(MileageReadings.farAhead(94_000, jan, before, DistanceUnit.MILES))
        assertTrue(MileageReadings.farAhead(94_001, jan, before, DistanceUnit.MILES))
        // Two years of ordinary driving.
        assertFalse(MileageReadings.farAhead(110_000, jan.plusDays(730), before, DistanceUnit.MILES))
        assertTrue(MileageReadings.farAhead(849_000, jan.plusDays(730), before, DistanceUnit.MILES))
        assertFalse(MileageReadings.farAhead(849_000, jan, null, DistanceUnit.MILES))
        assertFalse(MileageReadings.farAhead(1_000, jan.plusDays(5), before, DistanceUnit.MILES))
    }

    @Test
    fun `with a replaced cluster readings are compared as the dash now reads`() {
        // The cluster was replaced on day 50 at 90,000; the new one started at 12.
        val swap = ClusterSwaps.of(jan.plusDays(50), oldReading = 90_000, newReading = 12)
        val rows = rows(reading(1, 84_000), reading(2, 89_000, 40), reading(3, 1_500, 70), swap = swap)
        assertTrue(rows.none { it.outOfLine || it.jump != null })
        assertEquals(listOf(3L), ids(rows) { it.current })
        // Shown as typed.
        assertEquals(listOf(1_500, 89_000, 84_000), rows.map { it.reading.odometer })
    }

    @Test
    fun `when the new cluster's first reading is the highest, no row is the odometer`() {
        val swap = ClusterSwaps.of(jan.plusDays(50), oldReading = 90_000, newReading = 12)
        assertTrue(rows(reading(1, 84_000), swap = swap).none { it.current })
    }

    @Test
    fun `an edited reading needs a number and a date that has happened`() {
        assertNull(MileageReadings.problem(84_900, jan, jan))
        assertEquals("Enter the reading as a whole number above zero.", MileageReadings.problem(null, jan, jan))
        assertEquals("A reading can't be dated in the future.", MileageReadings.problem(84_900, jan.plusDays(1), jan))
    }

    @Test
    fun `no readings is no rows`() {
        assertEquals(emptyList<ReadingRow>(), MileageReadings.rows(emptyList(), null, DistanceUnit.MILES))
    }
}
