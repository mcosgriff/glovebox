package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SpendingTest {
    private val today = LocalDate.of(2026, 10, 19)

    private data class Spent(val date: LocalDate, val cents: Long?)

    private fun summary(vararg records: Spent, on: LocalDate = today) =
        Spending.summary(records.toList(), on, date = { it.date }, cost = { it.cents })

    @Test
    fun `this year and since the first cost`() {
        val s = summary(
            Spent(LocalDate.of(2026, 9, 2), 12_400),
            Spent(LocalDate.of(2025, 4, 11), 30_000),
            Spent(LocalDate.of(2021, 3, 20), 8_995),
        )!!
        assertEquals(12_400L, s.thisYearCents)
        assertEquals(51_395L, s.totalCents)
        assertEquals(YearMonth.of(2021, 3), s.since)
        assertEquals(3, s.counted)
        assertEquals(3, s.records)
    }

    @Test
    fun `the year is the calendar year of today`() {
        val records = arrayOf(Spent(LocalDate.of(2026, 1, 1), 100), Spent(LocalDate.of(2025, 12, 31), 200))
        assertEquals(100L, summary(*records)!!.thisYearCents)
        // New Year's Eve: the whole year counts, and a year ago to the day doesn't.
        assertEquals(100L, summary(*records, Spent(LocalDate.of(2025, 12, 31), 50), on = LocalDate.of(2026, 12, 31))!!.thisYearCents)
        // New Year's Day: nothing yet this year.
        assertNull(summary(Spent(LocalDate.of(2025, 12, 31), 200), on = LocalDate.of(2026, 1, 1))!!.thisYearCents)
    }

    @Test
    fun `records without a cost are left out and counted`() {
        val s = summary(
            Spent(LocalDate.of(2026, 5, 1), null),
            Spent(LocalDate.of(2024, 6, 1), 5_000),
            Spent(LocalDate.of(2020, 1, 1), null),
            Spent(LocalDate.of(2026, 2, 1), 0),
        )!!
        assertEquals(5_000L, s.totalCents)
        assertEquals(0L, s.thisYearCents)
        // The earliest record has no cost, so it doesn't set "since".
        assertEquals(YearMonth.of(2024, 6), s.since)
        assertEquals(2, s.counted)
        assertEquals(4, s.records)
    }

    @Test
    fun `no costs at all make no line`() {
        assertNull(summary())
        assertNull(summary(Spent(today, null), Spent(today.minusYears(2), null)))
        assertNull(Spending.total(listOf(null, null)))
    }

    @Test
    fun `costs only from this year need no separate this year`() {
        val s = summary(Spent(LocalDate.of(2026, 3, 1), 4_000), Spent(LocalDate.of(2026, 8, 1), 1_000))!!
        assertNull(s.thisYearCents)
        assertEquals(5_000L, s.totalCents)
        assertEquals(YearMonth.of(2026, 3), s.since)
    }

    @Test
    fun `the total is the export's`() {
        val records = listOf(
            ExportRecord(LocalDate.of(2026, 1, 5), 10_000, listOf("Oil"), "DIY", 4_500, null),
            ExportRecord(LocalDate.of(2025, 1, 5), 5_000, listOf("Wipers"), "DIY", null, null),
            ExportRecord(LocalDate.of(2024, 1, 5), 1_000, listOf("Tires"), "Shop", 80_000, null),
        )
        val vehicle = ExportVehicle("2019 Toyota 4Runner", null, null, unit = DistanceUnit.MILES, odometer = 10_000, soldOn = null, records = records, schedule = emptyList(), recalls = emptyList())
        assertEquals(vehicle.totalCents, Spending.summary(records, today, { it.date }, { it.costCents })!!.totalCents)
    }
}
