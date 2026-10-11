package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ArchiveTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test
    fun `a sale today at or above the last reading is fine`() {
        assertTrue(Archive.problems(Sale(today, 68_412), lastOdometer = 68_412, today).isEmpty())
        assertTrue(Archive.problems(Sale(today.minusDays(30), 70_000), lastOdometer = 68_412, today).isEmpty())
    }

    @Test
    fun `the odometer can be left out`() {
        assertTrue(Archive.problems(Sale(today, null), lastOdometer = 68_412, today).isEmpty())
        assertTrue(Archive.problems(Sale(today, 500), lastOdometer = null, today).isEmpty())
    }

    @Test
    fun `a future date and a lower odometer are both refused`() {
        assertEquals(
            setOf(SaleProblem.DATE_IN_FUTURE, SaleProblem.ODOMETER_BELOW_LAST),
            Archive.problems(Sale(today.plusDays(1), 68_000), lastOdometer = 68_412, today),
        )
    }

    @Test
    fun `only a higher odometer adds a reading, dated the sale`() {
        val sold = today.minusDays(3)
        assertEquals(OdometerReading(sold, 70_000), Archive.finalReading(Sale(sold, 70_000), lastOdometer = 68_412))
        assertEquals(OdometerReading(sold, 500), Archive.finalReading(Sale(sold, 500), lastOdometer = null))
        assertNull(Archive.finalReading(Sale(sold, 68_412), lastOdometer = 68_412))
        assertNull(Archive.finalReading(Sale(sold, null), lastOdometer = 68_412))
    }
}
