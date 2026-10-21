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

    @Test
    fun `the first rule for a text wins`() {
        val twice = listOf(CatalogRule("Brakes done", "brake-service"), CatalogRule("brakes done", "brake-inspect"))
        assertEquals("brake-service", CatalogRules.match("Brakes done", twice))
    }
}
