package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GearboxTest {
    @Test
    fun `only a manual is filled in from the decode's style, never an automatic`() {
        assertEquals(Gearbox.MANUAL, Gearboxes.suggested("Toyota", "6-speed Manual/Standard"))
        assertEquals(Gearbox.MANUAL, Gearboxes.suggested("Volkswagen", "7-speed Manual"))
        // vPIC calls a dual-clutch "Automatic" too.
        listOf("Automatic", "6-speed Automatic", "8-speed Automatic", "CVT", "Automated Manual", null, "").forEach {
            assertNull("$it", Gearboxes.suggested("Toyota", it))
        }
    }

    @Test
    fun `Volkswagen's 7-speed is a dual-clutch and its 8-speed an automatic, and a 6-speed or no count says nothing`() {
        assertEquals(Gearbox.DUAL_CLUTCH, Gearboxes.suggested("VOLKSWAGEN", "7-speed Automatic"))
        assertEquals(Gearbox.AUTOMATIC, Gearboxes.suggested("Volkswagen", "8-speed Automatic"))
        assertNull(Gearboxes.suggested("Volkswagen", "6-speed Automatic"))
        assertNull(Gearboxes.suggested("Volkswagen", "Automatic"))
        assertNull(Gearboxes.suggested("Volkswagen", "7-speed"))
        // Another make's speeds mean nothing here.
        assertNull(Gearboxes.suggested("Audi", "7-speed Automatic"))
        assertNull(Gearboxes.suggested(null, "7-speed Automatic"))
    }

    @Test
    fun `a stored answer reads back, and a vehicle on record before the question starts at Manual only where it said so`() {
        Gearbox.entries.forEach { assertEquals(it, Gearboxes.of(it.name)) }
        assertNull(Gearboxes.of(null))
        assertNull(Gearboxes.of("SEMI_AUTOMATIC"))
        assertEquals(Gearbox.MANUAL, Gearboxes.fromStored("5-speed Manual/Standard"))
        assertNull(Gearboxes.fromStored("7-speed Automatic"))
        assertNull(Gearboxes.fromStored(null))
    }

    @Test
    fun `the owner's answer decides manual or not, and without one the transmission text does, as before`() {
        assertTrue(GearboxOnRecord(Gearbox.MANUAL, "Automatic").manual)
        assertFalse(GearboxOnRecord(Gearbox.MANUAL, "Automatic").shiftsItself)
        assertTrue(GearboxOnRecord(Gearbox.DUAL_CLUTCH, "Manual").shiftsItself)
        assertFalse(GearboxOnRecord(Gearbox.AUTOMATIC, "Manual").manual)
        assertTrue(GearboxOnRecord.ofText("6-speed Manual/Standard").manual)
        assertTrue(GearboxOnRecord.ofText("CVT").shiftsItself)
        assertFalse(GearboxOnRecord(null).manual || GearboxOnRecord(null).shiftsItself)
    }

    @Test
    fun `an item for one gearbox reminds only on that gearbox`() {
        assertTrue(Gearboxes.reminds(null, null))
        assertTrue(Gearboxes.reminds(null, Gearbox.MANUAL))
        assertTrue(Gearboxes.reminds(Gearbox.DUAL_CLUTCH, Gearbox.DUAL_CLUTCH))
        assertFalse(Gearboxes.reminds(Gearbox.DUAL_CLUTCH, null))
        assertFalse(Gearboxes.reminds(Gearbox.DUAL_CLUTCH, Gearbox.AUTOMATIC))
        assertFalse(Gearboxes.reminds(Gearbox.AUTOMATIC, Gearbox.DUAL_CLUTCH))
    }

    @Test
    fun `speeds are read from the decode's text`() {
        assertEquals(7, Gearboxes.speeds("7-speed Automatic"))
        assertEquals(10, Gearboxes.speeds("10-Speed Automatic"))
        assertNull(Gearboxes.speeds("Automatic"))
    }
}
