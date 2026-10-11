package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RulePreviewTest {
    private val today = LocalDate.of(2026, 10, 10)
    private val interval = Interval(10_000, 12)
    private val last = LastDone(LocalDate.of(2026, 6, 14), 60_100)

    // 68,412 now, 1,688 mi to go; at 65 mi/day that is 26 days away.
    private fun preview(rule: DueRule, pace: Double? = 65.0, odometer: Int = 68_412) =
        RulePreviews.preview(interval, last, rule, odometer, today, pace)

    @Test
    fun `both limits are filled in whatever the rule`() {
        for (rule in DueRule.entries) {
            val p = preview(rule)
            assertEquals(70_100, p.dueAtOdometer)
            assertEquals(LocalDate.of(2026, 11, 5), p.mileageDate)
            assertEquals(LocalDate.of(2027, 6, 14), p.dueByDate)
            assertEquals(0.8312, p.milesFraction, 1e-9)
            assertEquals(118.0 / 365, p.timeFraction, 1e-9)
        }
    }

    @Test
    fun `whichever first takes the earlier limit`() {
        val p = preview(DueRule.WHICHEVER_FIRST)
        assertEquals(DecidedBy.MILEAGE, p.decidedBy)
        assertEquals(LocalDate.of(2026, 11, 5), p.nextDate)
        assertFalse(p.canExceedInterval)
    }

    @Test
    fun `whichever last takes the later limit and can run past an interval`() {
        val p = preview(DueRule.WHICHEVER_LAST)
        assertEquals(DecidedBy.TIME, p.decidedBy)
        assertEquals(LocalDate.of(2027, 6, 14), p.nextDate)
        assertTrue(p.canExceedInterval)
        // 247 days at 65 mi/day on top of 68,412.
        assertEquals(84_467, p.odometerAtTimeDue)
    }

    @Test
    fun `mileage only and time only ignore the other limit`() {
        assertEquals(DecidedBy.MILEAGE, preview(DueRule.MILEAGE_ONLY).decidedBy)
        assertEquals(LocalDate.of(2027, 6, 14), preview(DueRule.TIME_ONLY).nextDate)
    }

    @Test
    fun `without a driving pace the first and last rules can't say which limit wins`() {
        val first = preview(DueRule.WHICHEVER_FIRST, pace = null)
        assertNull(first.mileageDate)
        assertNull(first.decidedBy)
        assertNull(first.nextDate)
        assertNull(first.odometerAtTimeDue)
        assertNull(preview(DueRule.WHICHEVER_LAST, pace = null).decidedBy)
        // The single-limit rules don't need one.
        assertEquals(DecidedBy.TIME, preview(DueRule.TIME_ONLY, pace = null).decidedBy)
    }

    @Test
    fun `a mileage limit already reached is due today, pace or not`() {
        val p = preview(DueRule.WHICHEVER_FIRST, pace = null, odometer = 70_500)
        assertEquals(today, p.mileageDate)
        assertEquals(DecidedBy.MILEAGE, p.decidedBy)
        assertEquals(1.0, p.milesFraction, 0.0)
    }

    @Test
    fun `pace per month is rounded to the nearest ten`() {
        assertEquals(2_080, RulePreviews.milesPerMonth(68.4))
        assertEquals(1_980, RulePreviews.milesPerMonth(65.0))
    }

    @Test
    fun `lead time accepts zero up to the caps`() {
        assertEquals(LeadTime(500, 14), LeadTimeInput.parse("500", "14"))
        assertEquals(LeadTime(0, 0), LeadTimeInput.parse("0", "0"))
        assertEquals(LeadTime(1_000, 30), LeadTimeInput.parse("1,000", " 30 "))
        assertNull(LeadTimeInput.parse("", "14"))
        assertNull(LeadTimeInput.parse("500", "abc"))
        assertNull(LeadTimeInput.parse("5001", "14"))
        assertNull(LeadTimeInput.parse("500", "91"))
        assertNull(LeadTimeInput.parse("-5", "14"))
    }
}
