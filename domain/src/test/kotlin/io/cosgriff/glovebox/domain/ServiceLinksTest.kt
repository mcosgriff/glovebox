package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ServiceLinksTest {
    private val typicalOil = ScheduleRef(1, "Engine oil & filter", "oil", tracked = true)
    private val myOil = ScheduleRef(2, "Oil change", null, tracked = true, service = "oil")
    private val myWipers = ScheduleRef(3, "Wipers", null, tracked = true, service = "wipers")
    private val myWash = ScheduleRef(4, "Wash underbody", null, tracked = true)

    @Test
    fun `a catalog line is answered by the typical item it resets and by the user's own items set to it`() {
        val answering = ServiceLinks.answering(listOf(typicalOil, myOil, myWipers, myWash))
        assertEquals(setOf(1L, 2L), answering["oil"])
        assertEquals(setOf(3L), answering["wipers"])
        assertNull(answering["rotation"])
    }

    @Test
    fun `a service on a typical item is ignored`() {
        val odd = ScheduleRef(9, "Tire rotation", "rotation", tracked = true, service = "oil")
        assertEquals(mapOf("rotation" to setOf(9L)), ServiceLinks.answering(listOf(odd)))
    }

    @Test
    fun `one logged line counts for every item that answers to it`() {
        val answering = ServiceLinks.answering(listOf(typicalOil, myOil, myWipers, myWash))
        // Ticked on Log service: saved against the typical item, picked from the catalog's oil line.
        assertEquals(setOf(1L, 2L), ServiceLinks.itemIds(listOf(1L to "oil"), answering))
        // A bare last done entered for the user's own item is filed under the same line, so it counts for both too.
        assertEquals(setOf(1L, 2L), ServiceLinks.itemIds(listOf(2L to "oil"), answering))
        // Logged under "Your items": no catalog line, so only that item.
        assertEquals(setOf(4L), ServiceLinks.itemIds(listOf(4L to null), answering))
        // A recall's line is linked to nothing.
        assertTrue(ServiceLinks.itemIds(listOf(null to null), answering).isEmpty())
    }

    @Test
    fun `linking an item to a service makes past work count straight away`() {
        val day = LocalDate.of(2026, 6, 14)
        val oil = TrackedItem(2, "Oil change", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        val lines = listOf(1L to "oil") // logged before the user's item answered to anything

        val before = ServiceEntry(7, day, 60_100, ServiceLinks.itemIds(lines, ServiceLinks.answering(listOf(typicalOil, myOil.copy(service = null)))))
        assertNull(ServiceLog.lastDone(oil.id, listOf(before)))

        val after = ServiceEntry(7, day, 60_100, ServiceLinks.itemIds(lines, ServiceLinks.answering(listOf(typicalOil, myOil))))
        assertEquals(LastDone(day, 60_100), ServiceLog.lastDone(oil.id, listOf(after)))
    }

    @Test
    fun `an item that answers to a catalog line is logged through that line, not listed a second time`() {
        val categories = ServiceCatalog.forVehicle(listOf(typicalOil, myOil, myWipers, myWash))
        val yours = categories.first { it.id == ServiceCatalog.YOUR_ITEMS }.options
        assertEquals(listOf("Wash underbody"), yours.map { it.name })

        val options = categories.flatMap { it.options }.associateBy { it.id }
        // Saved against the typical item where there is one, otherwise against the user's own.
        assertEquals(1L, options.getValue("oil").scheduleItemId)
        assertEquals(3L, options.getValue("wipers").scheduleItemId)
        assertNull(options.getValue("battery").scheduleItemId)
    }

    @Test
    fun `a last done is filed under the line the item answers to`() {
        assertEquals("oil", ServiceCatalog.lineFor(null, "oil")?.id)
        assertEquals("wipers", ServiceCatalog.lineFor(null, "wipers")?.id)
        assertNull(ServiceCatalog.lineFor(null, null))
        assertEquals(listOf("wipers"), ServiceCatalog.resetters(null, "wipers"))
        assertEquals(listOf("oil"), ServiceCatalog.resetters("oil"))
    }
}
