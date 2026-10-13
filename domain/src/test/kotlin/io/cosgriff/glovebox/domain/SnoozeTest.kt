package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SnoozeTest {
    private val today = LocalDate.of(2026, 10, 10)

    private val oil = TrackedItem(1, "Oil & filter", Interval(10_000, 12), DueRule.WHICHEVER_FIRST)
    private val rotation = TrackedItem(2, "Tire rotation", Interval(5_000, null), DueRule.WHICHEVER_FIRST)
    private val wipers = TrackedItem(3, "Wipers", Interval(null, 12), DueRule.WHICHEVER_FIRST)

    // Odometer 68,412: rotation is 3,312 mi overdue, oil has 1,688 mi and about six months left.
    private val entries = listOf(
        ServiceEntry(1, LocalDate.of(2026, 4, 10), 60_100, setOf(1L, 2L)),
        ServiceEntry(2, LocalDate.of(2025, 9, 1), 50_000, setOf(3L)),
    )
    private val updates = listOf(OdometerReading(today, 68_412))

    private fun statuses(vararg items: TrackedItem, odometer: Int = 68_412, on: LocalDate = today) =
        VehicleReminders.statuses(items.toList(), entries, listOf(OdometerReading(on, odometer)), on).associateBy { it.item.id }

    @Test
    fun `active until the odometer or the date is reached, whichever is first`() {
        val both = Snooze(untilOdometer = 69_000, untilDate = today.plusDays(14))
        assertTrue(both.isActive(68_999, today.plusDays(13)))
        assertFalse(both.isActive(69_000, today))
        assertFalse(both.isActive(68_500, today.plusDays(14)))
        assertTrue(Snooze(untilDate = today.plusDays(1)).isActive(999_999, today))
    }

    @Test
    fun `a snoozed reminder stops counting as overdue but keeps its real distance`() {
        val snoozed = rotation.copy(snooze = Snooze(untilOdometer = 68_912))
        val s = statuses(snoozed).getValue(2)

        assertEquals(DueState.OK, s.status.state)
        assertEquals(-3_312, s.status.milesRemaining)
        assertEquals(Snooze(untilOdometer = 68_912), s.snooze)
        assertEquals(DueHeadline.Snoozed(Snooze(untilOdometer = 68_912)), ReminderPresentation.progress(s).headline)
    }

    @Test
    fun `the reminder comes back once the snooze passes`() {
        val snoozed = rotation.copy(snooze = Snooze(untilOdometer = 68_912))
        val s = statuses(snoozed, odometer = 68_912).getValue(2)

        assertEquals(DueState.OVERDUE, s.status.state)
        assertNull(s.snooze)
        assertEquals(DueHeadline.Miles(-3_812), ReminderPresentation.progress(s).headline)
    }

    @Test
    fun `a leftover snooze does nothing to a reminder that isn't due`() {
        // Logged again since it was snoozed: the old snooze is still stored but has nothing to hold back.
        val s = statuses(oil.copy(snooze = Snooze(untilOdometer = 90_000))).getValue(1)
        assertEquals(DueState.OK, s.status.state)
        assertNull(s.snooze)
        assertEquals(DueHeadline.Miles(1_688), ReminderPresentation.progress(s).headline)
    }

    @Test
    fun `snoozed reminders leave the counts, the next line and the urgent list`() {
        val all = statuses(oil, rotation.copy(snooze = Snooze(untilDate = today.plusWeeks(2))), wipers)
        val up = ReminderPresentation.comingUp(all.values.toList())

        assertEquals(1, up.overdue) // wipers; the rotation is snoozed
        assertEquals("Oil & filter", up.next?.first?.item?.name)
        assertEquals(
            listOf("Wipers", "Oil & filter"),
            ReminderPresentation.mostUrgent(listOf("Truck" to up)).map { it.status.item.name },
        )
        assertEquals(ReminderGroup.LATER, ReminderPresentation.group(all.getValue(2), today))
        // Listed after what is still counting down.
        assertEquals("Tire rotation", up.counting.last().first.item.name)
    }

    @Test
    fun `options cover miles, time and the next oil change`() {
        val all = statuses(oil, rotation)
        val options = Snoozing.options(all.getValue(2), 68_412, today, oil = all.getValue(1), unit = DistanceUnit.MILES)

        assertEquals(
            listOf(
                SnoozeOption(SnoozeKind.MILES_500, Snooze(untilOdometer = 68_912)),
                SnoozeOption(SnoozeKind.MILES_1000, Snooze(untilOdometer = 69_412)),
                SnoozeOption(SnoozeKind.WEEKS_2, Snooze(untilDate = LocalDate.of(2026, 10, 24))),
                SnoozeOption(SnoozeKind.MONTH_1, Snooze(untilDate = LocalDate.of(2026, 11, 10))),
                SnoozeOption(SnoozeKind.NEXT_OIL_CHANGE, Snooze(70_100, LocalDate.of(2027, 4, 10))),
            ),
            options,
        )
    }

    @Test
    fun `no oil change option for the oil change itself, or when it is overdue or missing`() {
        val all = statuses(oil, rotation)
        fun kinds(item: ItemStatus, oil: ItemStatus?) = Snoozing.options(item, 68_412, today, oil, DistanceUnit.MILES).map { it.kind }

        assertFalse(SnoozeKind.NEXT_OIL_CHANGE in kinds(all.getValue(1), all.getValue(1)))
        assertFalse(SnoozeKind.NEXT_OIL_CHANGE in kinds(all.getValue(2), null))
        val late = statuses(oil, rotation, odometer = 71_000)
        assertFalse(SnoozeKind.NEXT_OIL_CHANGE in kinds(late.getValue(2), late.getValue(1)))
    }

    @Test
    fun `without an odometer only the time options are offered`() {
        val all = statuses(wipers)
        assertEquals(
            listOf(SnoozeKind.WEEKS_2, SnoozeKind.MONTH_1),
            Snoozing.options(all.getValue(3), null, today, oil = null, unit = DistanceUnit.MILES).map { it.kind },
        )
    }

    @Test
    fun `skip waits for the next interval that hasn't already passed`() {
        val all = statuses(oil, rotation, wipers)
        // Rotation was due at 65,100; the next one after 68,412 is 70,100.
        assertEquals(Snooze(untilOdometer = 70_100), Snoozing.skip(all.getValue(2), 68_412, today))
        // Wipers were due 2026-09-01; the next is a year on.
        assertEquals(Snooze(untilDate = LocalDate.of(2027, 9, 1)), Snoozing.skip(all.getValue(3), 68_412, today))
        // Two intervals behind: skip to the first one still ahead.
        val far = statuses(rotation, odometer = 76_000)
        assertEquals(Snooze(untilOdometer = 80_100), Snoozing.skip(far.getValue(2), 76_000, today))
    }

    @Test
    fun `the generic schedule has the oil change other reminders are snoozed to`() {
        assertTrue(ScheduleTemplates.generic(null, null, DistanceUnit.MILES).any { it.key == Snoozing.OIL_TEMPLATE_KEY })
    }
}
