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

    /** A reading a week for [years], with a service every six months among them. */
    private fun weekly(years: Int): List<ReadingRow> {
        val weeks = years * 52
        val readings = (0 until weeks).map { w ->
            val date = today.minusWeeks((weeks - 1 - w).toLong())
            LoggedReading(w + 1L, date, 10_000 + w * 230, fromRecord = w % 26 == 0)
        }
        return MileageReadings.rows(readings, null, unit)
    }

    @Test
    fun `a reading a week gets a mark only where there is room`() {
        val s = MileageChart.series(weekly(15), null, null, today)!!
        assertEquals(780, s.points.size)
        val marked = MileageChart.marked(s.points, room = 25)
        // A service every six months is closer than the room allows, so every other one is marked, and no update.
        assertEquals(16, marked.size)
        assertTrue(marked.dropLast(1).all { it.row.reading.fromRecord })
        val gap = (s.end.toEpochDay() - s.start.toEpochDay()) / 25.0
        marked.zipWithNext().forEach { (a, b) -> assertTrue(b.date.toEpochDay() - a.date.toEpochDay() >= gap || b.row.current) }
        assertTrue(marked.last().row.current)
    }

    @Test
    fun `services before updates, and every reading when there is room`() {
        val s = MileageChart.series(weekly(1), null, null, today)!!
        val marked = MileageChart.marked(s.points, room = 10)
        assertTrue(s.points.filter { it.row.reading.fromRecord }.all { it in marked })
        assertEquals(s.points, MileageChart.marked(s.points, room = 1_000))
    }

    @Test
    fun `a reading to check is always marked`() {
        val readings = (0 until 104).map { w -> LoggedReading(w + 1L, today.minusWeeks(103L - w), 10_000 + w * 200) } +
            LoggedReading(500, today.minusWeeks(50).plusDays(1), 99_999)
        val s = MileageChart.series(MileageReadings.rows(readings, null, unit), null, null, today)!!
        val odd = s.points.single { it.row.reading.id == 500L }
        assertTrue(odd.doubtful)
        assertTrue(odd in MileageChart.marked(s.points, room = 5))
    }
}
