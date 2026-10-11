package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReminderPresentationTest {
    private val today = LocalDate.of(2026, 10, 10)

    private fun status(
        interval: Interval,
        rule: DueRule = DueRule.WHICHEVER_FIRST,
        lastDate: LocalDate? = today.minusDays(30),
        lastOdometer: Int = 60_000,
        odometer: Int = 61_000,
        id: Long = 1,
        name: String = "Item",
    ): ItemStatus {
        val item = TrackedItem(id, name, interval, rule)
        val last = lastDate?.let { LastDone(it, lastOdometer) }
        return ItemStatus(item, last, ReminderEngine.status(interval, rule, last, odometer, today, milesPerDay = null))
    }

    @Test
    fun `never logged has no progress`() {
        val p = ReminderPresentation.progress(status(Interval(5_000, 6), lastDate = null))
        assertEquals(DueHeadline.NotLogged, p.headline)
        assertNull(p.fraction)
    }

    @Test
    fun `mileage only`() {
        val p = ReminderPresentation.progress(status(Interval(5_000, null), odometer = 63_312))
        assertEquals(DueHeadline.Miles(1_688), p.headline)
        assertEquals(0.6624, p.fraction!!, 1e-9)
    }

    @Test
    fun `overdue mileage is capped at a full bar`() {
        val p = ReminderPresentation.progress(status(Interval(5_000, null), odometer = 68_312))
        assertEquals(DueHeadline.Miles(-3_312), p.headline)
        assertEquals(1.0, p.fraction!!, 0.0)
    }

    @Test
    fun `time only`() {
        val s = status(Interval(null, 6), lastDate = LocalDate.of(2026, 7, 10)) // due 2027-01-10
        val p = ReminderPresentation.progress(s)
        assertEquals(DueHeadline.Days(92), p.headline)
        assertEquals(0.5, p.fraction!!, 1e-9) // 92 of 184 days gone
    }

    @Test
    fun `whichever first shows the limit that is further along, without needing a pace`() {
        // 4,500 of 5,000 mi used but only 30 of ~180 days: mileage is what matters.
        val p = ReminderPresentation.progress(status(Interval(5_000, 6), odometer = 64_500))
        assertEquals(DueHeadline.Miles(500), p.headline)
        assertEquals(0.9, p.fraction!!, 1e-9)

        // Barely driven, but five months in: time is what matters.
        val q = ReminderPresentation.progress(status(Interval(5_000, 6), lastDate = today.minusMonths(5), odometer = 60_200))
        assertEquals(DueHeadline.Days(31), q.headline)
    }

    @Test
    fun `whichever last shows the limit that is lagging`() {
        val p = ReminderPresentation.progress(
            status(Interval(5_000, 6), rule = DueRule.WHICHEVER_LAST, odometer = 64_500),
        )
        assertEquals(DueHeadline.Days(151), p.headline) // due 2027-03-10
    }

    @Test
    fun `mileage-only rule ignores the time interval`() {
        val p = ReminderPresentation.progress(
            status(Interval(5_000, 6), rule = DueRule.MILEAGE_ONLY, lastDate = today.minusMonths(5), odometer = 60_200),
        )
        assertEquals(DueHeadline.Miles(4_800), p.headline)
    }

    @Test
    fun `coming up puts overdue first, then the most used, and lists never-logged apart`() {
        val overdue = status(Interval(5_000, null), odometer = 66_000, id = 1, name = "Rotation")
        val soon = status(Interval(5_000, null), odometer = 64_800, id = 2, name = "Oil")
        val fine = status(Interval(30_000, null), odometer = 61_000, id = 3, name = "Air filter")
        val neverB = status(Interval(5_000, null), lastDate = null, id = 4, name = "Wipers")
        val neverA = status(Interval(5_000, null), lastDate = null, id = 5, name = "Coolant")

        val up = ReminderPresentation.comingUp(listOf(fine, neverB, soon, overdue, neverA))

        assertEquals(listOf("Rotation", "Oil", "Air filter"), up.counting.map { it.first.item.name })
        assertEquals(listOf("Coolant", "Wipers"), up.notLogged.map { it.item.name })
        // Never-logged items don't inflate the overdue count.
        assertEquals(1, up.overdue)
        assertEquals(1, up.dueSoon)
    }
}
