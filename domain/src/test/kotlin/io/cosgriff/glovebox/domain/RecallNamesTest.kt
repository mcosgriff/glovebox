package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecallNamesTest {
    private val ford2019 = listOf("F-150", "F-250", "F-250 SD", "F-250 CREW CAB", "F-350 SD", "TRANSIT", "TRANSIT VAN", "TRANSIT HIGH ROOF 15 PASS")

    @Test
    fun `the names offered begin with the vehicle's model, whatever the case or hyphens, and never are the model itself`() {
        assertEquals(listOf("F-250 CREW CAB", "F-250 SD"), RecallNames.offered("F-250", ford2019, chosen = emptySet()))
        assertEquals(listOf("F-250 CREW CAB", "F-250 SD"), RecallNames.offered("f250", ford2019, chosen = emptySet()))
        assertEquals(listOf("TRANSIT HIGH ROOF 15 PASS", "TRANSIT VAN"), RecallNames.offered("Transit", ford2019, chosen = emptySet()))
        assertEquals(emptyList<String>(), RecallNames.offered("Ranger", ford2019, chosen = emptySet()))
    }

    @Test
    fun `a name already ticked is still offered when NHTSA no longer lists it, so it can be unticked`() {
        assertEquals(listOf("F-250 CREW CAB", "F-250 SD", "F-250 SUPER DUTY"), RecallNames.offered("F-250", ford2019, chosen = setOf("F-250 SUPER DUTY")))
    }

    @Test
    fun `recalls are checked under the model and every ticked name, each once`() {
        assertEquals(listOf("F-250", "F-250 SD", "F-250 CREW CAB"), RecallNames.checkedUnder("F-250", listOf("F-250 SD", "f-250", "F-250 CREW CAB", "F-250 sd")))
        assertEquals(listOf("4Runner"), RecallNames.checkedUnder("4Runner", emptyList()))
    }
}
