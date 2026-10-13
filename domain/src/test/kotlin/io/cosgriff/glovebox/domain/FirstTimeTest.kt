package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** A job whose first time isn't one interval in: "first at 100,000, then every 50,000". */
class FirstTimeTest {
    private val today = LocalDate.of(2026, 10, 13)
    private val coolant = TrackedItem(1, "Coolant", Interval(50_000, 60), DueRule.WHICHEVER_FIRST, firstAt = 100_000)
    private val oil = TrackedItem(2, "Oil", Interval(10_000, 12), DueRule.WHICHEVER_FIRST)

    private fun status(odometer: Int, entries: List<ServiceEntry> = emptyList(), item: TrackedItem = coolant) =
        VehicleReminders.statuses(listOf(item), entries, listOf(OdometerReading(today, odometer)), today).single()

    @Test
    fun `before the first mileage it counts down to it`() {
        val s = status(31_000)
        assertTrue(s.awaitingFirst)
        assertTrue(s.counting)
        assertNull(s.lastDone)
        assertEquals(DueState.OK, s.status.state)
        assertEquals(100_000, s.status.dueAtOdometer)
        assertEquals(69_000, s.status.milesRemaining)
        // By distance only: when the vehicle went into service isn't known.
        assertNull(s.status.dueByDate)
        assertEquals(ReminderGroup.LATER, ReminderPresentation.group(s, today))
        val progress = ReminderPresentation.progress(s)
        assertEquals(DueHeadline.Miles(69_000), progress.headline)
        assertEquals(0.31, progress.fraction!!, 1e-9)
    }

    @Test
    fun `it is due soon inside the lead distance`() {
        assertEquals(DueState.DUE_SOON, status(99_600).status.state)
        assertEquals(DueState.OK, status(99_400).status.state)
    }

    @Test
    fun `past the first mileage with nothing logged it is not logged yet, as any item is`() {
        listOf(100_000, 120_000).forEach { odometer ->
            val s = status(odometer)
            assertFalse(s.awaitingFirst)
            assertFalse(s.counting)
            assertEquals(ReminderGroup.NOT_LOGGED, ReminderPresentation.group(s, today))
        }
    }

    @Test
    fun `once logged the interval counts from the last time, wherever that was`() {
        val early = status(90_000, listOf(ServiceEntry(1, today.minusMonths(2), 88_000, setOf(1))))
        assertFalse(early.awaitingFirst)
        assertEquals(138_000, early.status.dueAtOdometer)
        assertEquals(today.minusMonths(2).plusMonths(60), early.status.dueByDate)
    }

    @Test
    fun `any job the vehicle hasn't reached counts down to one interval from zero`() {
        val plugs = TrackedItem(3, "Spark plugs", Interval(120_000, 144), DueRule.WHICHEVER_FIRST)
        val s = status(31_000, item = plugs)
        assertTrue(s.awaitingFirst)
        assertEquals(120_000, s.status.dueAtOdometer)
        assertEquals(89_000, s.status.milesRemaining)
        assertEquals(ReminderGroup.LATER, ReminderPresentation.group(s, today))
    }

    @Test
    fun `a job the vehicle is already past is still not logged yet`() {
        val s = status(31_000, item = oil)
        assertFalse(s.counting)
        assertEquals(DueState.OVERDUE, s.status.state)
    }

    @Test
    fun `a job that goes by time alone, or a vehicle with no reading, has no first mileage`() {
        assertFalse(status(31_000, item = TrackedItem(4, "Brake fluid", Interval(null, 36), DueRule.WHICHEVER_FIRST)).counting)
        assertFalse(status(31_000, item = TrackedItem(5, "Belt", Interval(60_000, 72), DueRule.TIME_ONLY)).counting)
        val unread = VehicleReminders.statuses(listOf(coolant), emptyList(), emptyList(), today).single()
        assertFalse(unread.counting)
    }

    @Test
    fun `the first time is announced once as it comes into its lead distance`() {
        fun vehicle(odometer: Int) = NoticeVehicle(7, "2020 4Runner", listOf(status(odometer)), today)
        val far = NoticeRules.daily(listOf(vehicle(31_000)), NoticeLog(), NoticePrefs(), today)
        assertTrue(far.notices.none { it is Notice.ServiceDue })
        val near = NoticeRules.daily(listOf(vehicle(99_700)), far.log, NoticePrefs(), today)
        assertEquals(listOf("Coolant"), near.notices.filterIsInstance<Notice.ServiceDue>().single().services)
        val again = NoticeRules.daily(listOf(vehicle(99_800)), near.log, NoticePrefs(), today.plusDays(1))
        assertTrue(again.notices.none { it is Notice.ServiceDue })
    }

    @Test
    fun `the generated milestones start such a job at its first mileage`() {
        val points = Milestones.generate(listOf(coolant, oil), from = 1, current = 195_000, ahead = 6)
        val coolantAt = points.filter { m -> m.jobs.any { it.name == "Coolant" } }.map { it.atMiles }
        assertEquals(listOf(100_000, 150_000, 200_000, 250_000), coolantAt)
        assertTrue(points.any { it.atMiles == 50_000 && it.jobs.single().name == "Oil" })
    }

    @Test
    fun `a file gives the first time in its own unit and it is put into the vehicle's`() {
        val file = """{ "glovebox": "import", "version": 1, "kind": "interval-schedule", "items": [
            { "name": "Coolant", "everyKm": 80000, "everyMonths": 60, "firstAtKm": 160000 } ] }"""
        val parsed = ImportFormat.parse(file, today) as ImportFile.IntervalSchedule
        assertEquals(160_000, parsed.items.single().firstAt)
        assertEquals(100_000, FileDistances.schedule(parsed.items, parsed.unit, DistanceUnit.MILES).single().firstAt)
    }
}
