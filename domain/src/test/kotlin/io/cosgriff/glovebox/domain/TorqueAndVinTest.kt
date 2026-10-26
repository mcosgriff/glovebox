package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TorqueAndVinTest {
    @Test
    fun `15 N·m is about 11 ft-lb, not 2`() {
        val t = Torque.of(15.0, TorqueUnit.NEWTON_METER)
        assertEquals(11, t.rounded(TorqueUnit.FOOT_POUND))
        assertEquals(135, t.rounded(TorqueUnit.INCH_POUND))
    }

    @Test
    fun `lug nuts 83 ft-lb round-trip`() {
        val t = Torque.of(83.0, TorqueUnit.FOOT_POUND)
        assertEquals(113, t.rounded(TorqueUnit.NEWTON_METER))
        assertEquals(83.0, t.inUnit(TorqueUnit.FOOT_POUND), 1e-9)
    }

    @Test
    fun `check digit validation`() {
        // Classic valid test VIN from the NHTSA/ISO 3779 examples.
        assertTrue(Vin.hasValidCheckDigit("1M8GDM9AXKP042788"))
        assertFalse(Vin.hasValidCheckDigit("1M8GDM9A1KP042788"))
        assertFalse(Vin.isWellFormed("1M8GDM9AXKP04278O")) // letter O not allowed
        assertEquals("JTEBU5JR4K5612345", Vin.normalize(" jtebu5jr4k5612345 "))
    }

    @Test
    fun `a VIN added later only has to look like one`() {
        assertTrue(Vin.isPlausible("1M8GDM9AXKP042788"))
        assertTrue(Vin.isPlausible("1M8GDM9A1KP042788")) // wrong check digit: fine for an import
        assertTrue(Vin.isPlausible("WP0ZZZ91Z")) // pre-1981 serial
        assertFalse(Vin.isPlausible("AB12"))
        assertFalse(Vin.isPlausible("1M8GDM9AXKP0427889"))
        assertFalse(Vin.isPlausible("1m8-gdm9a"))
    }

    @Test
    fun `a VIN can be saved when it looks like one and isn't the one already saved`() {
        assertTrue(Vin.canSave("1M8GDM9AXKP042788", saved = null))
        assertTrue(Vin.canSave("1M8GDM9AXKP042788", saved = "1M8GDM9A1KP042788")) // a correction
        assertTrue(Vin.canSave("1M8GDM9A1KP042788", saved = "1M8GDM9AXKP042788")) // a wrong check digit only warns
        assertFalse(Vin.canSave("1M8GDM9AXKP042788", saved = "1M8GDM9AXKP042788")) // unchanged
        assertFalse(Vin.canSave("", saved = "1M8GDM9AXKP042788")) // a VIN is corrected, never cleared
        assertFalse(Vin.canSave("AB12", saved = null))
    }
}
