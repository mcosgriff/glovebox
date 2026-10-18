package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NoticesTest {
    private val today = LocalDate.of(2026, 10, 11)

    private val oil = TrackedItem(1, "Oil & filter", Interval(10_000, 12), DueRule.WHICHEVER_FIRST)
    private val rotation = TrackedItem(2, "Tire rotation", Interval(5_000, null), DueRule.WHICHEVER_FIRST)
    private val wipers = TrackedItem(3, "Wipers", Interval(null, 12), DueRule.WHICHEVER_FIRST)

    // Oil was done at 60,100 and is due at 70,100; rotation was done at 60,100 and is due at 65,100.
    private val entries = listOf(ServiceEntry(1, LocalDate.of(2026, 4, 10), 60_100, setOf(1L, 2L)))

    private fun vehicle(odometer: Int, on: LocalDate = today, readingOn: LocalDate? = on, vararg items: TrackedItem = arrayOf(oil, rotation, wipers)) =
        NoticeVehicle(
            id = 7,
            name = "2019 4Runner",
            statuses = VehicleReminders.statuses(items.toList(), entries, listOf(OdometerReading(on, odometer)), on),
            lastReadingOn = readingOn,
        )

    private fun daily(v: NoticeVehicle, log: NoticeLog = NoticeLog(), prefs: NoticePrefs = NoticePrefs(), on: LocalDate = today) =
        NoticeRules.daily(listOf(v), log, prefs, on)

    @Test
    fun `nothing is said while everything is far off`() {
        val d = daily(vehicle(61_000))
        assertTrue(d.notices.isEmpty())
    }

    @Test
    fun `a service entering its lead time is announced once`() {
        // 500 mi lead: rotation due at 65,100 is "soon" from 64,600.
        val first = daily(vehicle(64_700))
        assertEquals(listOf(Notice.ServiceDue(7, "2019 4Runner", listOf("Tire rotation"))), first.notices)
        val next = daily(vehicle(64_800, today.plusDays(1)), first.log, on = today.plusDays(1))
        assertTrue(next.notices.isEmpty())
    }

    @Test
    fun `logging the service again starts a new announcement`() {
        val first = daily(vehicle(64_700))
        // Rotated at 64,900, then driven to within 500 mi of the next one at 69,900.
        val again = listOf(ServiceEntry(1, LocalDate.of(2026, 4, 10), 60_100, setOf(1L, 2L)), ServiceEntry(2, today, 64_900, setOf(2L)))
        val later = today.plusDays(60)
        val v = NoticeVehicle(7, "2019 4Runner", VehicleReminders.statuses(listOf(rotation), again, listOf(OdometerReading(later, 69_500)), later), later)
        val d = NoticeRules.daily(listOf(v), first.log, NoticePrefs(), later)
        assertEquals(listOf(Notice.ServiceDue(7, "2019 4Runner", listOf("Tire rotation"))), d.notices)
    }

    @Test
    fun `an overdue service is nudged at once and then weekly`() {
        val first = daily(vehicle(66_000))
        assertEquals(listOf(Notice.Overdue(7, "2019 4Runner", listOf("Tire rotation"))), first.notices)
        val day6 = today.plusDays(6)
        assertTrue(daily(vehicle(66_100, day6), first.log, on = day6).notices.isEmpty())
        val day7 = today.plusDays(7)
        val week = daily(vehicle(66_200, day7), first.log, on = day7)
        assertEquals(listOf(Notice.Overdue(7, "2019 4Runner", listOf("Tire rotation"))), week.notices)
        assertEquals(day7, week.log.overdueNudged[2])
    }

    @Test
    fun `a snoozed service stays quiet until the snooze passes`() {
        val snoozed = rotation.copy(snooze = Snooze(untilOdometer = 67_000))
        assertTrue(daily(vehicle(66_000, items = arrayOf(snoozed))).notices.isEmpty())
        val after = daily(vehicle(67_000, items = arrayOf(snoozed)))
        assertEquals(listOf(Notice.Overdue(7, "2019 4Runner", listOf("Tire rotation"))), after.notices)
    }

    @Test
    fun `a service that was never logged isn't announced`() {
        // Wipers has no entry, which the engine treats as overdue.
        val d = daily(vehicle(61_000, items = arrayOf(wipers)))
        assertTrue(d.notices.none { it is Notice.Overdue || it is Notice.ServiceDue })
    }

    @Test
    fun `switches silence a kind without building a backlog`() {
        val off = NoticePrefs(serviceDue = false, overdue = false, mileage = false)
        val quiet = daily(vehicle(64_700), prefs = off)
        assertTrue(quiet.notices.isEmpty())
        // Turned on the next day: already-due things were noted, so nothing floods out.
        val next = daily(vehicle(64_700, today.plusDays(1)), quiet.log, on = today.plusDays(1))
        assertTrue(next.notices.isEmpty())
    }

    @Test
    fun `mileage is asked for when the last reading is older than the cadence`() {
        val stale = today.minusDays(14)
        val d = daily(vehicle(61_000, readingOn = stale))
        assertEquals(listOf(Notice.MileageCheckIn(7, "2019 4Runner")), d.notices)
        // Not again until another cadence has passed, even if no reading comes in.
        val day13 = today.plusDays(13)
        assertTrue(NoticeRules.daily(listOf(vehicle(61_000, day13, readingOn = stale)), d.log, NoticePrefs(), day13).notices.isEmpty())
        val day14 = today.plusDays(14)
        assertEquals(1, NoticeRules.daily(listOf(vehicle(61_000, day14, readingOn = stale)), d.log, NoticePrefs(), day14).notices.size)
    }

    @Test
    fun `a recent reading or a vehicle with no reminders skips the check-in`() {
        assertTrue(daily(vehicle(61_000, readingOn = today.minusDays(13))).notices.isEmpty())
        val bare = NoticeVehicle(8, "2011 GTI", emptyList(), null)
        assertTrue(NoticeRules.daily(listOf(bare), NoticeLog(), NoticePrefs(), today).notices.isEmpty())
    }

    @Test
    fun `a weekly cadence asks sooner`() {
        val d = daily(vehicle(61_000, readingOn = today.minusDays(7)), prefs = NoticePrefs(mileageCadence = MileageCadence.WEEKLY))
        assertEquals(listOf(Notice.MileageCheckIn(7, "2019 4Runner")), d.notices)
    }

    @Test
    fun `the log forgets what is no longer due`() {
        val log = NoticeLog(dueAnnounced = mapOf(99L to "x"), overdueNudged = mapOf(98L to today), mileageAsked = mapOf(97L to today))
        val d = daily(vehicle(61_000), log)
        assertEquals(NoticeLog(), d.log)
    }

    @Test
    fun `time until the daily check wraps to tomorrow`() {
        assertEquals(60, NoticeRules.minutesUntil(8, 0, nowMinuteOfDay = 7 * 60))
        assertEquals(23 * 60, NoticeRules.minutesUntil(8, 0, nowMinuteOfDay = 9 * 60))
        assertEquals(24 * 60, NoticeRules.minutesUntil(8, 0, nowMinuteOfDay = 8 * 60))
    }

    @Test
    fun `labels read naturally`() {
    }
}
