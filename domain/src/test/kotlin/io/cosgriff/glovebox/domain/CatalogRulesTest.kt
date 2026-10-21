package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRulesTest {
    private val rules = listOf(
        CatalogRule("Water pump gasket replaced", "water-pump"),
        CatalogRule("Tire(s) mounted", "new-tires"),
    )

    @Test
    fun `a rule matches the whole text, ignoring case, spaces and the (s)`() {
        assertEquals("water-pump", CatalogRules.match("Water pump gasket replaced", rules))
        assertEquals("water-pump", CatalogRules.match("  WATER pump   gasket replaced ", rules))
        assertEquals("new-tires", CatalogRules.match("Tires mounted", rules))
        assertEquals("new-tires", CatalogRules.match("tire(s) mounted", rules))
    }

    @Test
    fun `never part of the text`() {
        assertNull(CatalogRules.match("Water pump inspected", rules))
        assertNull(CatalogRules.match("Water pump gasket replaced again", rules))
        assertNull(CatalogRules.match("gasket replaced", rules))
        assertNull(CatalogRules.match("", rules))
    }

    @Test
    fun `a rule whose line is no longer in the catalog is kept but never applied`() {
        val gone = CatalogRule("Flux capacitor serviced", "flux-capacitor")
        assertFalse(CatalogRules.known(gone))
        assertTrue(CatalogRules.known(rules.first()))
        assertNull(CatalogRules.match("Flux capacitor serviced", rules + gone))
    }

    private fun record(vararg work: ImportedWork) =
        ImportedRecord(date = java.time.LocalDate.of(2025, 3, 1), odometer = 50_000, work = work.toList(), diy = false)

    @Test
    fun `an import's lines are filed by a rule only where the file says nothing`() {
        val file = listOf(
            record(ImportedWork("Water pump gasket replaced"), ImportedWork("Tire(s) mounted", service = "balance")),
            record(ImportedWork("water pump gasket replaced"), ImportedWork("Roof rack fitted")),
        )
        val filed = CatalogRules.file(file, rules, overWording = false)
        assertEquals(listOf("water-pump", "balance"), filed.items[0].work.map { it.service })
        assertEquals(listOf("water-pump", null), filed.items[1].work.map { it.service })
        assertEquals(2, filed.lines)
        assertEquals(listOf("Water pump gasket replaced"), filed.wordings)
    }

    @Test
    fun `on a Carfax report the user's rule wins over the wording Glovebox recognises`() {
        // "Tire(s) mounted" read as a balance by Glovebox's wording; the user said it is new tires.
        val report = listOf(record(ImportedWork("Tire(s) mounted", service = "balance"), ImportedWork("Water pump gasket replaced", service = "water-pump")))
        val filed = CatalogRules.file(report, rules, overWording = true)
        assertEquals(listOf("new-tires", "water-pump"), filed.items[0].work.map { it.service })
        // The pump's rule agrees with the wording, so no line was filed differently because of it.
        assertEquals(1, filed.lines)
        assertEquals(listOf("Tire(s) mounted"), filed.wordings)
    }

    @Test
    fun `with no rules an import is as it came`() {
        val file = listOf(record(ImportedWork("Water pump gasket replaced")))
        assertEquals(RuleFiling(file, 0, emptyList()), CatalogRules.file(file, emptyList(), overWording = true))
    }

    @Test
    fun `the first rule for a text wins`() {
        val twice = listOf(CatalogRule("Brakes done", "brake-service"), CatalogRule("brakes done", "brake-inspect"))
        assertEquals("brake-service", CatalogRules.match("Brakes done", twice))
    }
}
