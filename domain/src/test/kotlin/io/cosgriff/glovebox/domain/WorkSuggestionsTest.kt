package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkSuggestionsTest {
    // Made-up names in the catalog's style; the real ones live in `app`.
    private val lines = listOf(
        NamedLine("oil", "Oil & filter change"),
        NamedLine("coolant", "Coolant"),
        NamedLine("coolant-hoses", "Coolant hoses"),
        NamedLine("water-pump", "Water pump"),
        NamedLine("shocks", "Shocks / struts"),
        NamedLine("brake-lines", "Brake lines / hoses"),
        NamedLine("front-pads", "Front pads"),
        NamedLine("rear-pads", "Rear pads"),
        NamedLine("schedule-7", "Grease hitch"),
    )

    private fun suggest(typed: String) = WorkSuggestions.suggest(typed, lines)

    @Test
    fun `a whole name in what was typed is offered`() {
        assertEquals(listOf("water-pump"), suggest("Water pump replaced"))
        assertEquals(listOf("water-pump"), suggest("replaced the WATER PUMP."))
        assertEquals(listOf("schedule-7"), suggest("greased hitch? no: grease hitch"))
    }

    @Test
    fun `the name of more words comes first`() {
        assertEquals(listOf("coolant-hoses", "coolant"), suggest("Coolant hoses replaced"))
    }

    @Test
    fun `one alternative of a name is enough, and a plural s doesn't matter`() {
        assertEquals(listOf("shocks"), suggest("Rear strut replaced"))
        assertEquals(listOf("brake-lines"), suggest("Brake hose replaced"))
        // "hoses" is brake hoses there, not any hoses.
        assertEquals(listOf("coolant-hoses", "coolant"), suggest("Coolant hoses replaced"))
        assertEquals(listOf("front-pads"), suggest("front pad"))
    }

    @Test
    fun `while typing, every word so far starting a word of the name is enough`() {
        assertEquals(listOf("water-pump"), suggest("wat pu"))
        assertEquals(listOf("coolant", "coolant-hoses"), suggest("cool"))
        assertEquals(listOf("front-pads", "rear-pads"), suggest("pad"))
    }

    @Test
    fun `nothing is offered for nothing, or for words no line has`() {
        assertEquals(emptyList<String>(), suggest(""))
        assertEquals(emptyList<String>(), suggest("  a "))
        assertEquals(emptyList<String>(), suggest("Replaced roof rack"))
        // Not a word of any name, though letters of several.
        assertEquals(emptyList<String>(), suggest("ump"))
    }

    @Test
    fun `at most four`() {
        val many = (1..10).map { NamedLine("l$it", "Lamp $it") }
        assertEquals(WorkSuggestions.MAX, WorkSuggestions.suggest("lamp", many).size)
    }
}
