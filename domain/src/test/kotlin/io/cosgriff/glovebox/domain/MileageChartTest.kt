package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MileageChartTest {
    private val today = LocalDate.of(2026, 10, 19)
    private val unit = DistanceUnit.MILES

    private fun rows(vararg readings: Pair<LocalDate, Int>, swap: ClusterSwap? = null) =
        MileageReadings.rows(readings.mapIndexed { i, (date, odometer) -> LoggedReading(i + 1L, date, odometer) }, swap, unit)

    @Test
    fun `the first reading is always the first point`() {
        val s = MileageChart.series(
            rows(today.minusYears(3) to 30_000, today.minusYears(1) to 50_000, today to 60_000, today.minusYears(5) to 10_000),
            null, null, today,
        )!!
        assertEquals(today.minusYears(5), s.start)
        assertEquals(today.minusYears(5), s.points.first().date)
        assertEquals(today, s.end)
        assertEquals(10_000, s.low)
        assertEquals(60_000, s.high)
    }

    @Test
    fun `a range starts at its first reading inside it`() {
        val all = rows(today.minusYears(3) to 30_000, today.minusMonths(18) to 45_000, today.minusMonths(6) to 55_000, today to 60_000)
        val year = MileageChart.series(all, null, 1, today)!!
        assertEquals(listOf(55_000, 60_000), year.points.map { it.plotted })
        assertEquals(today.minusMonths(6), year.start)
        assertEquals(3, MileageChart.series(all, null, 2, today)!!.points.size)
    }

    @Test
    fun `years on record count a part year as a whole one`() {
        assertEquals(1, MileageChart.yearsOnRecord(emptyList(), today))
        assertEquals(1, MileageChart.yearsOnRecord(rows(today.minusMonths(3) to 1_000, today to 2_000), today))
        assertEquals(1, MileageChart.yearsOnRecord(rows(today.minusYears(1) to 1_000), today))
        assertEquals(2, MileageChart.yearsOnRecord(rows(today.minusYears(1).minusDays(1) to 1_000), today))
        assertEquals(15, MileageChart.yearsOnRecord(rows(today.minusYears(14).minusMonths(5) to 1_000, today to 180_000), today))
    }

    @Test
    fun `the longest range is every reading`() {
        val all = rows(today.minusYears(14).minusMonths(5) to 1_000, today.minusYears(7) to 90_000, today to 180_000)
        val most = MileageChart.yearsOnRecord(all, today)
        assertEquals(MileageChart.series(all, null, null, today), MileageChart.series(all, null, most, today))
    }

    @Test
    fun `fewer than two readings make no chart`() {
        assertNull(MileageChart.series(emptyList(), null, null, today))
        assertNull(MileageChart.series(rows(today to 1_000), null, null, today))
        // Two on record, but only one in the last year.
        assertNull(MileageChart.series(rows(today.minusYears(3) to 1_000, today to 9_000), null, 1, today))
    }

    @Test
    fun `the pace is the reminders' own, and a year of it is rounded`() {
        val readings = rows(today.minusDays(365) to 40_000, today.minusDays(200) to 45_500, today to 52_000)
        val s = MileageChart.series(readings, null, null, today)!!
        val reminders = ReminderEngine.milesPerDay(listOf(OdometerReading(today.minusDays(365), 40_000), OdometerReading(today, 52_000)))
        assertEquals(reminders!!, s.milesPerDay!!, 1e-9)
        assertEquals(12_000, s.perYear)
    }

    @Test
    fun `readings under a week apart give no pace`() {
        val s = MileageChart.series(rows(today.minusDays(3) to 40_000, today to 40_200), null, null, today)!!
        assertNull(s.milesPerDay)
        assertNull(s.perYear)
    }

    @Test
    fun `a year of pace rounds to tens under a thousand and hundreds above`() {
        assertEquals(12_300, MileageChart.perYear(33.7))
        assertEquals(370, MileageChart.perYear(1.01))
        assertEquals(10, MileageChart.perYear(0.001))
    }

    @Test
    fun `a replaced cluster doesn't break the line`() {
        // The old cluster read 90,000 when the new one went in at 1,000: an offset of 89,000.
        val swap = ClusterSwap(today.minusYears(1), offset = 89_000, startReading = 1_000)
        val readings = rows(today.minusYears(2) to 80_000, today.minusMonths(6) to 6_000, today to 11_000, swap = swap)
        val s = MileageChart.series(readings, swap, null, today)!!
        assertEquals(listOf(80_000, 95_000, 100_000), s.points.map { it.plotted })
        assertEquals(swap.on, s.swapOn)
        // Shown as typed when tapped.
        assertEquals(6_000, s.points[1].row.reading.odometer)
        assertTrue(s.points.none { it.doubtful })
    }

    @Test
    fun `doubtful readings and the odometer match the list`() {
        val readings = rows(today.minusYears(2) to 40_000, today.minusYears(1) to 490_000, today.minusMonths(6) to 50_000, today to 55_000)
        val s = MileageChart.series(readings, null, null, today)!!
        assertEquals(listOf(false, true, false, false), s.points.map { it.doubtful })
        assertEquals(1, s.toCheck)
        assertEquals(readings.filter { it.current }.map { it.reading }, s.points.filter { it.row.current }.map { it.row.reading })
    }
}
