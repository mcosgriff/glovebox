package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A vehicle's lines on Log service ([ServiceCatalog.forVehicle]): what each saves, and the line each reminder ticks. */
class VehicleLinesTest {
    private val typicalOil = ScheduleRef(1, "Engine oil & filter", "oil", tracked = true)
    private val myOil = ScheduleRef(2, "Oil change", null, tracked = true, service = "oil")
    private val myWash = ScheduleRef(4, "Wash underbody", null, tracked = true)
    private val oldThing = ScheduleRef(5, "Old custom thing", null, tracked = false)
    private val checkDiff = ScheduleRef(6, "Inspect front differential oil", null, tracked = true, service = "front-diff-check")
    private val changeDiff = ScheduleRef(7, "Change front differential oil", null, tracked = true, service = "front-diff")
    private val lines = ServiceCatalog.forVehicle(listOf(typicalOil, myOil, myWash, oldThing, checkDiff, changeDiff))

    @Test
    fun `a catalog line saves its catalog id, and one of the user's own items saves none`() {
        val oil = lines.options.single { it.id == "oil" }
        assertEquals("oil", oil.catalogId)
        val wash = lines.options.single { it.name == "Wash underbody" }
        assertNull(wash.catalogId)
        // Every line but the user's own is the catalog's.
        assertEquals(lines.options.filter { it.name == null }, lines.options.filter { it.catalogId != null })
        assertEquals(ServiceCatalog.categories.flatMap { c -> c.items.map { it.id } }, lines.catalog.flatMap { c -> c.options.map { it.catalogId } })
        assertTrue(lines.catalog.none { it.id == ServiceCatalog.YOUR_ITEMS })
    }

    @Test
    fun `Log it on a reminder ticks the item's own line`() {
        // The typical item, and the user's own item set to the same line: both tick that line, saved against the typical one.
        assertEquals("oil", lines.optionFor(typicalOil.id)?.id)
        assertEquals("oil", lines.optionFor(myOil.id)?.id)
        assertEquals(typicalOil.id, lines.optionFor(myOil.id)?.scheduleItemId)
        // One of the user's own that answers to no line is its line under "Your items".
        assertEquals(myWash.id, lines.optionFor(myWash.id)?.scheduleItemId)
        assertEquals("Wash underbody", lines.optionFor(myWash.id)?.name)
        assertEquals(ServiceCatalog.YOUR_ITEMS, lines.categoryOf(lines.optionFor(myWash.id)!!.id)?.id)
    }

    @Test
    fun `Log it on a check ticks the check, never a line that merely includes it`() {
        // A change of the oil also counts as the check of it, but the check's own line is the check.
        assertEquals(setOf(checkDiff.id, changeDiff.id), ServiceLinks.answering(listOf(checkDiff, changeDiff))["front-diff"])
        assertEquals("front-diff-check", lines.optionFor(checkDiff.id)?.id)
        assertEquals("front-diff", lines.optionFor(changeDiff.id)?.id)
    }

    @Test
    fun `a reminder no line stands for ticks nothing`() {
        // Not tracked, so not under "Your items", and answering to no line.
        assertNull(lines.optionFor(oldThing.id))
        assertNull(lines.optionFor(99))
        assertNull(lines.optionFor(null))
    }

    @Test
    fun `an edit ticks a saved line as its catalog line, else as its item's line, else as custom`() {
        assertEquals("oil", lines.optionOf(RecordLine("Oil and filter changed", "oil", typicalOil.id))?.id)
        // Saved under the catalog line whatever item it was saved against.
        assertEquals("oil", lines.optionOf(RecordLine("Oil changed", "oil", myWash.id))?.id)
        assertEquals(lines.optionFor(myWash.id), lines.optionOf(RecordLine("Washed", null, myWash.id)))
        // A line this version doesn't know goes by its item; with none, it is custom.
        assertEquals("oil", lines.optionOf(RecordLine("Oil", "a-later-line", myOil.id))?.id)
        assertNull(lines.optionOf(RecordLine("Hitch greased")))
    }
}
