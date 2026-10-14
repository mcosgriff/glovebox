package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ServiceCatalogTest {
    private val catalogItems = ServiceCatalog.categories.flatMap { it.items }

    @Test
    fun `catalog ids are unique and each reminder is reset by at most one line`() {
        val ids = catalogItems.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val resets = catalogItems.mapNotNull { it.resets }
        assertEquals(resets.size, resets.toSet().size)
    }

    @Test
    fun `every suggested schedule item can be logged from the catalog`() {
        val resets = catalogItems.mapNotNull { it.resets }.toSet()
        val templateKeys = (ScheduleTemplates.generic("4WD", null, DistanceUnit.MILES) + ScheduleTemplates.generic("RWD", null, DistanceUnit.MILES)).map { it.key }.toSet()
        assertEquals(emptySet<String>(), templateKeys - resets)
    }

    @Test
    fun `catalog lines link to the vehicle's schedule items by template key`() {
        val schedule = listOf(
            ScheduleRef(1, "Engine oil & filter", "oil", tracked = true),
            ScheduleRef(2, "Tire rotation", "rotation", tracked = false),
        )
        val options = ServiceCatalog.forVehicle(schedule).flatMap { it.options }.associateBy { it.id }
        assertEquals(1L, options.getValue("oil").scheduleItemId)
        // Untracked items still get the link, so history counts if tracking is switched back on.
        assertEquals(2L, options.getValue("rotation").scheduleItemId)
        assertNull(options.getValue("coolant").scheduleItemId)
        assertNull(options.getValue("wipers").scheduleItemId)
    }

    @Test
    fun `the user's own tracked items are listed first`() {
        val schedule = listOf(
            ScheduleRef(1, "Engine oil & filter", "oil", tracked = true),
            ScheduleRef(7, "Grease hitch", null, tracked = true),
            ScheduleRef(8, "Old custom thing", null, tracked = false),
        )
        val categories = ServiceCatalog.forVehicle(schedule)
        assertEquals(ServiceCatalog.YOUR_ITEMS, categories.first().id)
        assertEquals(listOf(WorkOption("schedule-7", "Grease hitch", 7)), categories.first().options)

        // No own items: no empty category.
        assertEquals("engine", ServiceCatalog.forVehicle(schedule.take(1)).first().id)
    }

    @Test
    fun `statuses come from the log`() {
        val today = LocalDate.of(2026, 10, 10)
        val oil = TrackedItem(1, "Oil", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
        val rotation = TrackedItem(2, "Rotation", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
        val entries = listOf(ServiceEntry(10, today.minusDays(30), 80_000, setOf(1)))
        val updates = listOf(OdometerReading(today, 81_000))

        val statuses = VehicleReminders.statuses(listOf(oil, rotation), entries, updates, today).associateBy { it.item.id }

        assertEquals(DueState.OK, statuses.getValue(1).status.state)
        assertEquals(4_000, statuses.getValue(1).status.milesRemaining)
        // Never logged counts as due, to nudge the user to record it.
        assertNull(statuses.getValue(2).lastDone)
        assertEquals(DueState.OVERDUE, statuses.getValue(2).status.state)
        assertEquals(81_000, VehicleReminders.currentOdometer(entries, updates))
        assertNull(VehicleReminders.currentOdometer(emptyList(), emptyList()))
    }

    @Test
    fun `money parsing`() {
        assertEquals(123_450L, Money.parseCents("$1,234.5"))
        assertEquals(4_599L, Money.parseCents("45.99"))
        assertEquals(8_000L, Money.parseCents(" 80 "))
        assertEquals(0L, Money.parseCents("0"))
        assertNull(Money.parseCents(""))
        assertNull(Money.parseCents("12.345"))
        assertNull(Money.parseCents("-5"))
        assertNull(Money.parseCents("abc"))
        assertNull(Money.parseCents("1.2.3"))
    }

    @Test
    fun `odometer parsing`() {
        assertEquals(84_210, OdometerInput.parse("84,210"))
        assertEquals(84_210, OdometerInput.parse(" 84210 "))
        assertNull(OdometerInput.parse(""))
        assertNull(OdometerInput.parse("0"))
        assertNull(OdometerInput.parse("84210.5"))
        assertNull(OdometerInput.parse("99999999999"))
        assertTrue(OdometerInput.parse("1") == 1)
    }

    @Test
    fun `a change of oil counts as the check of it, and never the other way round`() {
        val schedule = listOf(
            ScheduleRef(1, "Inspect front differential oil", templateKey = null, tracked = true, service = "front-diff-check"),
            ScheduleRef(2, "Change front differential oil", templateKey = null, tracked = true, service = "front-diff"),
            ScheduleRef(3, "Inspect rear differential oil", templateKey = null, tracked = true, service = "rear-diff-check"),
        )
        val answering = ServiceLinks.answering(schedule)
        assertEquals(setOf(1L), answering["front-diff-check"])
        assertEquals(setOf(1L, 2L), answering["front-diff"])
        // The one line for all of them counts as each check.
        assertEquals(setOf(1L, 3L), answering["diff-fluid"])
        assertEquals(listOf("service:front-diff", "name:front differential oil change", "service:front-diff-check"), MilestoneImport.doneKeys("Front differential oil change", "front-diff"))
        assertEquals(listOf("service:front-diff-check", "name:checked it"), MilestoneImport.doneKeys("Checked it", "front-diff-check"))
        // Every covered line is a real one.
        val ids = HistoryImport.services.keys
        assertTrue(HistoryImport.services.values.all { ids.containsAll(it.covers) })
    }
}
