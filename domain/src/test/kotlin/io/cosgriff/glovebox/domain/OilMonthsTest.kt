package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OilMonthsTest {
    private fun oil(normal: Interval?, severe: Interval? = null, useSevere: Boolean = false, unit: DistanceUnit = DistanceUnit.MILES) =
        ActiveInterval.counted(normal, severe, useSevere, oil = true, unit = unit)

    @Test
    fun `an oil change with miles and no months gets one month for every 833 miles`() {
        assertEquals(CountedInterval(Interval(10_000, 12), monthsWorkedOut = true), oil(Interval(10_000, null)))
        assertEquals(CountedInterval(Interval(7_500, 9), monthsWorkedOut = true), oil(Interval(7_500, null)))
        assertEquals(CountedInterval(Interval(5_000, 6), monthsWorkedOut = true), oil(Interval(5_000, null)))
    }

    @Test
    fun `never more than 12 months, and never none`() {
        assertEquals(Interval(15_000, 12), oil(Interval(15_000, null))?.interval)
        assertEquals(12, OilMonths.forDistance(100_000, DistanceUnit.MILES))
        assertEquals(1, OilMonths.forDistance(100, DistanceUnit.MILES))
    }

    @Test
    fun `months the item has are its own and are left as they are`() {
        assertEquals(CountedInterval(Interval(10_000, 24)), oil(Interval(10_000, 24)))
        assertEquals(CountedInterval(Interval(null, 12)), oil(Interval(null, 12)))
        assertFalse(oil(Interval(7_500, 12))!!.monthsWorkedOut)
    }

    @Test
    fun `only the oil change gets them`() {
        val diff = ActiveInterval.counted(Interval(60_000, null), null, useSevere = false, oil = false, unit = DistanceUnit.MILES)
        assertEquals(CountedInterval(Interval(60_000, null)), diff)
        assertNull(ActiveInterval.counted(null, null, useSevere = false, oil = true, unit = DistanceUnit.MILES))
    }

    @Test
    fun `severe miles go through the same rule, after the switch picks them`() {
        val counted = oil(Interval(10_000, null), severe = Interval(5_000, null), useSevere = true)
        assertEquals(CountedInterval(Interval(5_000, 6), monthsWorkedOut = true), counted)
        // The normal months still stand behind severe miles that have none: they are the maker's.
        assertEquals(CountedInterval(Interval(5_000, 12)), oil(Interval(10_000, 12), severe = Interval(5_000, null), useSevere = true))
    }

    @Test
    fun `an E85 oil change without months gets them too`() {
        val counted = ActiveInterval.counted(Interval(5_000, 6), null, useSevere = false, e85 = Interval(2_500, null), useE85 = true, oil = true, unit = DistanceUnit.MILES)
        assertEquals(CountedInterval(Interval(2_500, 3), monthsWorkedOut = true), counted)
    }

    @Test
    fun `a kilometre vehicle's figure is taken as the schedule figure it is`() {
        // 16,000 km is a manual's 10,000 mi; 12,000 km its 7,500; 8,000 km its 5,000.
        assertEquals(12, OilMonths.forDistance(16_000, DistanceUnit.KILOMETERS))
        assertEquals(9, OilMonths.forDistance(12_000, DistanceUnit.KILOMETERS))
        assertEquals(6, OilMonths.forDistance(8_000, DistanceUnit.KILOMETERS))
        assertEquals(CountedInterval(Interval(15_000, 11), monthsWorkedOut = true), oil(Interval(15_000, null), unit = DistanceUnit.KILOMETERS))
    }

    @Test
    fun `the oil change is the typical one or the owner's set to it`() {
        assertTrue(OilMonths.isOil(templateKey = "oil", service = null))
        assertTrue(OilMonths.isOil(templateKey = null, service = "oil"))
        assertFalse(OilMonths.isOil(templateKey = "rotation", service = "oil"))
        assertFalse(OilMonths.isOil(templateKey = null, service = "rotation"))
        assertFalse(OilMonths.isOil(templateKey = null, service = null))
    }

    @Test
    fun `a GTI a year past its oil change shows overdue by time`() {
        // #517: last oil 2024-10-24 at 126,677 mi; on 2026-10-28 at 131,550 mi it read "due in 5,127 mi".
        val counted = oil(Interval(10_000, null))!!
        val status = ReminderEngine.status(
            counted.interval, DueRule.WHICHEVER_FIRST, LastDone(LocalDate.of(2024, 10, 24), 126_677),
            currentOdometer = 131_550, today = LocalDate.of(2026, 10, 28), milesPerDay = 6.7,
        )
        assertEquals(DueState.OVERDUE, status.state)
        assertEquals(LocalDate.of(2025, 10, 24), status.dueByDate)
        assertEquals(DecidedBy.TIME, status.decidedBy)
    }

    @Test
    fun `a tracked item says whether its months are Glovebox's`() {
        val item = TrackedItem.of(1, "Oil", Interval(10_000, 12), DueRule.WHICHEVER_FIRST, null, null, false, false, null, monthsWorkedOut = true)
        assertTrue(item!!.monthsWorkedOut)
        assertFalse(TrackedItem.of(1, "Oil", Interval(10_000, 12), DueRule.WHICHEVER_FIRST, null, null, false, false, null)!!.monthsWorkedOut)
    }
}
