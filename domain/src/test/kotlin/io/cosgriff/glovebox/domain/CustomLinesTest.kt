package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CustomLinesTest {
    private fun day(d: Int) = LocalDate.of(2025, 3, d)

    private fun line(id: Long, label: String, date: LocalDate = day(1), catalogId: String? = null, scheduleItemId: Long? = null, recall: Boolean = false) =
        SavedLine(id, recordId = id * 10, date = date, label = label, catalogId = catalogId, scheduleItemId = scheduleItemId, recall = recall)

    @Test
    fun `only a line with no home is custom`() {
        val lines = listOf(
            line(1, "Hitch pin replaced"),
            line(2, "Oil and filter changed", catalogId = "oil"),
            line(3, "Grease hitch", scheduleItemId = 7),
            line(4, "Recall 21V-000: fuel pump", recall = true),
        )
        assertEquals(1, CustomLines.count(lines))
        assertEquals(listOf("Hitch pin replaced"), CustomLines.groups(lines, emptyList()).map { it.label })
    }

    @Test
    fun `lines written the same way are one group, worded as last written`() {
        val lines = listOf(
            line(1, "Hitch pin replaced", day(3)),
            line(2, "hitch pin  replaced ", day(9)),
            line(3, "Roof rack fitted", day(5)),
            line(4, "HITCH PIN REPLACED", day(1)),
        )
        val groups = CustomLines.groups(lines, emptyList())
        assertEquals(listOf("hitch pin  replaced", "Roof rack fitted"), groups.map { it.label })
        val hitch = groups.first()
        assertEquals(3, hitch.count)
        assertEquals(listOf(1L, 2L, 4L), hitch.lineIds)
        assertEquals(setOf(10L, 20L, 40L), hitch.recordIds)
        assertEquals(listOf(day(1), day(3), day(9)), hitch.dates)
    }

    @Test
    fun `the user's rule is suggested before the wording Glovebox recognises`() {
        val rules = listOf(CatalogRule("Water pump replaced", "cooling-check"))
        assertEquals(LineSuggestion("cooling-check", SuggestionSource.YOUR_RULE), CustomLines.suggest("Water pump replaced", rules))
        assertEquals(LineSuggestion("water-pump", SuggestionSource.GLOVEBOX), CustomLines.suggest("Water pump replaced", emptyList()))
        assertNull(CustomLines.suggest("Roof rack fitted", rules))
    }

    private val rules = listOf(CatalogRule("Brakes done", "brake-service"))
    private val sample = CustomLines.groups(
        listOf(line(1, "Water pump replaced"), line(2, "water pump replaced"), line(3, "Roof rack fitted"), line(4, "Brakes done")),
        rules,
    )
    private fun group(label: String) = sample.single { it.label.equals(label, ignoreCase = true) }

    @Test
    fun `a suggestion stands until the user chooses, and leaving a group under Custom is a choice`() {
        val pump = group("Water pump replaced")
        assertEquals("water-pump", CustomLines.picked(pump, emptyMap()))
        assertEquals("cooling-check", CustomLines.picked(pump, mapOf(pump.key to "cooling-check")))
        assertNull(CustomLines.picked(pump, mapOf(pump.key to null)))
        assertNull(CustomLines.picked(group("Roof rack fitted"), emptyMap()))
    }

    @Test
    fun `assignments file every line of a picked group, and nothing else`() {
        val rack = group("Roof rack fitted")
        val brakes = group("Brakes done")
        assertEquals(
            listOf(1L to "water-pump", 2L to "water-pump", 3L to "accessory"),
            CustomLines.assignments(sample, mapOf(rack.key to "accessory", brakes.key to null)).sortedBy { it.first },
        )
    }

    @Test
    fun `remember is asked for a pick that isn't already the user's rule, and kept unless unticked`() {
        val pump = group("Water pump replaced")
        val brakes = group("Brakes done")
        assertEquals(true, CustomLines.asksToRemember(pump, "water-pump"))
        assertEquals(false, CustomLines.asksToRemember(brakes, "brake-service"))
        assertEquals(true, CustomLines.asksToRemember(brakes, "brake-inspect"))
        assertEquals(false, CustomLines.asksToRemember(pump, null))

        assertEquals(listOf(CatalogRule("water pump replaced", "water-pump")), CustomLines.rulesToKeep(sample, emptyMap(), emptyMap()))
        assertEquals(emptyList<CatalogRule>(), CustomLines.rulesToKeep(sample, emptyMap(), mapOf(pump.key to false)))
        assertEquals(
            listOf(CatalogRule("water pump replaced", "water-pump"), CatalogRule("Brakes done", "brake-inspect")),
            CustomLines.rulesToKeep(sample, mapOf(brakes.key to "brake-inspect"), emptyMap()),
        )
    }
}
