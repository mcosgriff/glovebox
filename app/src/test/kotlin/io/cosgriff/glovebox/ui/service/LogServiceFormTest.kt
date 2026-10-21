package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.domain.WorkOption
import org.junit.Assert.assertEquals
import org.junit.Test

class LogServiceFormTest {
    // One of the user's own lines: it carries its name, so nothing here needs the app's resources.
    private val hitch = WorkOption("schedule-7", "Grease hitch", 7)

    private fun form(vararg custom: String) = LogServiceForm(editing = true).apply { customItems = custom.toList() }

    @Test
    fun `a custom line filed under a line is ticked there and keeps its wording`() {
        val form = form("Hitch greased, ball and pin", "Roof rack fitted")
        form.fileCustom("Hitch greased, ball and pin", hitch)
        assertEquals(setOf("schedule-7"), form.selected)
        assertEquals(listOf("Roof rack fitted"), form.customItems)
        assertEquals("Hitch greased, ball and pin", form.labelOf(hitch))
        // Still one line on the form, not two.
        assertEquals(2, form.itemCount)
    }

    @Test
    fun `wording that is the line's own name isn't kept as anything else`() {
        val form = form("Grease hitch")
        form.fileCustom("Grease hitch", hitch)
        assertEquals(emptyMap<String, String>(), form.keptLabels)
        assertEquals("Grease hitch", form.labelOf(hitch))
    }

    @Test
    fun `a line already ticked isn't filed onto`() {
        val form = form("Hitch greased")
        form.toggle(hitch.id)
        form.fileCustom("Hitch greased", hitch)
        assertEquals(listOf("Hitch greased"), form.customItems)
        assertEquals("Grease hitch", form.labelOf(hitch))
    }

    @Test
    fun `unticking the line lets its kept wording go`() {
        val form = form("Hitch greased")
        form.fileCustom("Hitch greased", hitch)
        form.toggle(hitch.id)
        assertEquals(emptySet<String>(), form.selected)
        assertEquals(emptyMap<String, String>(), form.keptLabels)
        // Ticked again, it is the line's own name.
        form.toggle(hitch.id)
        assertEquals("Grease hitch", form.labelOf(hitch))
    }
}
