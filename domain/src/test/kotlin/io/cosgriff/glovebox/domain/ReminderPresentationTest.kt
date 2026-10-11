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
    fun `pace uses the highest reading when several share the last day`() {
        val start = today.minusDays(100)
        val readings = listOf(
            OdometerReading(today, 62_000),
            OdometerReading(start, 60_000),
            OdometerReading(today, 61_500), // earlier the same day
        )
        assertEquals(20.0, ReminderEngine.milesPerDay(readings)!!, 1e-9)
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

    @Test
    fun `due soon outranks an item that has merely used more of its interval`() {
        // 2,600 of 3,000 mi used but 400 mi left: inside the 500 mi lead, so due soon.
        val soon = status(Interval(3_000, null), odometer = 62_600, id = 1, name = "Oil")
        // 9,000 of 10,000 mi used (90%), but 1,000 mi left: still fine.
        val fine = status(Interval(10_000, null), odometer = 69_000, id = 2, name = "Coolant")

        val up = ReminderPresentation.comingUp(listOf(fine, soon))

        assertEquals(listOf("Oil", "Coolant"), up.counting.map { it.first.item.name })
    }

    @Test
    fun `attention names the reminder when there is only one`() {
        val overdue = status(Interval(5_000, null), odometer = 66_000, id = 1, name = "Rotation")
        val soon = status(Interval(5_000, null), odometer = 64_800, id = 2, name = "Oil")
        val fine = status(Interval(30_000, null), odometer = 61_000, id = 3, name = "Air filter")
        val never = status(Interval(5_000, null), lastDate = null, id = 4, name = "Wipers")

        assertEquals(
            Attention(DueState.OVERDUE, 1, "Rotation"),
            ReminderPresentation.comingUp(listOf(fine, soon, overdue)).attention,
        )
        assertEquals(
            Attention(DueState.OVERDUE, 2, null),
            ReminderPresentation.comingUp(listOf(overdue, overdue.copy(item = overdue.item.copy(id = 9, name = "Brakes")))).attention,
        )
        assertEquals(Attention(DueState.DUE_SOON, 1, "Oil"), ReminderPresentation.comingUp(listOf(fine, soon)).attention)
        assertEquals(Attention(DueState.OK, 0, null), ReminderPresentation.comingUp(listOf(fine)).attention)
        // A vehicle with nothing logged has nothing to report, not nine overdue items.
        assertNull(ReminderPresentation.comingUp(listOf(never)).attention)
    }

    @Test
    fun `next skips what is already overdue`() {
        val overdue = status(Interval(5_000, null), odometer = 66_000, id = 1, name = "Rotation")
        val soon = status(Interval(5_000, null), odometer = 64_800, id = 2, name = "Oil")
        val never = status(Interval(5_000, null), lastDate = null, id = 4, name = "Wipers")

        assertEquals("Oil", ReminderPresentation.comingUp(listOf(overdue, soon, never)).next?.first?.item?.name)
        assertNull(ReminderPresentation.comingUp(listOf(overdue, never)).next)
    }

    @Test
    fun `most urgent merges vehicles, overdue first, and keeps to the limit`() {
        val truck = ReminderPresentation.comingUp(
            listOf(
                status(Interval(5_000, null), odometer = 66_000, id = 1, name = "Rotation"), // overdue
                status(Interval(30_000, null), odometer = 61_000, id = 2, name = "Air filter"), // fine
                status(Interval(5_000, null), lastDate = null, id = 3, name = "Wipers"), // never logged
            ),
        )
        val hatch = ReminderPresentation.comingUp(
            listOf(
                status(Interval(5_000, null), odometer = 64_800, id = 4, name = "Oil"), // due soon
                status(Interval(10_000, null), odometer = 65_000, id = 5, name = "Coolant"), // fine, half used
            ),
        )

        val urgent = ReminderPresentation.mostUrgent(listOf("Truck" to truck, "Hatch" to hatch))

        assertEquals(
            listOf("Truck" to "Rotation", "Hatch" to "Oil", "Hatch" to "Coolant"),
            urgent.map { it.vehicle to it.status.item.name },
        )
        assertEquals(1, ReminderPresentation.mostUrgent(listOf("Truck" to truck, "Hatch" to hatch), limit = 1).size)
    }

    @Test
    fun `reminders tab groups by overdue, next 30 days, later and not logged`() {
        val overdue = status(Interval(5_000, null), odometer = 66_000, id = 1, name = "Rotation")
        val soonByMiles = status(Interval(5_000, null), odometer = 64_800, id = 2, name = "Oil")
        // Due in 20 days: outside the 14-day lead, inside the 30-day window.
        val in20Days = status(Interval(null, 6), lastDate = today.minusMonths(6).plusDays(20), id = 3, name = "Wipers")
        val nextYear = status(Interval(null, 24), lastDate = today.minusMonths(12), id = 4, name = "Brake fluid")
        // No driving pace, so no date estimate for a mileage reminder that isn't close.
        val farByMiles = status(Interval(30_000, null), odometer = 61_000, id = 5, name = "Air filter")
        val never = status(Interval(5_000, null), lastDate = null, id = 6, name = "Coolant")

        val truck = ReminderPresentation.comingUp(listOf(farByMiles, nextYear, overdue, never))
        val hatch = ReminderPresentation.comingUp(listOf(in20Days, soonByMiles))
        val groups = ReminderPresentation.grouped(listOf("Truck" to truck, "Hatch" to hatch), today)

        assertEquals(
            listOf(ReminderGroup.OVERDUE, ReminderGroup.NEXT_30_DAYS, ReminderGroup.LATER, ReminderGroup.NOT_LOGGED),
            groups.keys.toList(),
        )
        assertEquals(listOf("Truck" to "Rotation"), groups.names(ReminderGroup.OVERDUE))
        // The dated one first; the mileage one has no estimate without a pace.
        assertEquals(listOf("Hatch" to "Wipers", "Hatch" to "Oil"), groups.names(ReminderGroup.NEXT_30_DAYS))
        assertEquals(listOf("Truck" to "Brake fluid", "Truck" to "Air filter"), groups.names(ReminderGroup.LATER))
        assertEquals(listOf("Truck" to "Coolant"), groups.names(ReminderGroup.NOT_LOGGED))
    }

    @Test
    fun `a mileage reminder lands in next 30 days when the driving pace puts it there`() {
        val item = TrackedItem(1, "Oil", Interval(10_000, null), DueRule.WHICHEVER_FIRST)
        val last = LastDone(today.minusDays(100), 60_000)
        // 2,000 mi left at 100 mi/day is 20 days away; at 20 mi/day it is 100 days away.
        val fast = ItemStatus(item, last, ReminderEngine.status(item.interval, item.rule, last, 68_000, today, milesPerDay = 100.0))
        val slow = ItemStatus(item, last, ReminderEngine.status(item.interval, item.rule, last, 68_000, today, milesPerDay = 20.0))

        assertEquals(ReminderGroup.NEXT_30_DAYS, ReminderPresentation.group(fast, today))
        assertEquals(ReminderGroup.LATER, ReminderPresentation.group(slow, today))
    }

    @Test
    fun `empty sections are left out`() {
        val fine = status(Interval(30_000, null), odometer = 61_000)
        val groups = ReminderPresentation.grouped(listOf("Truck" to ReminderPresentation.comingUp(listOf(fine))), today)
        assertEquals(listOf(ReminderGroup.LATER), groups.keys.toList())
    }

    private fun Map<ReminderGroup, List<UrgentItem<String>>>.names(group: ReminderGroup) =
        getValue(group).map { it.vehicle to it.status.item.name }
}
