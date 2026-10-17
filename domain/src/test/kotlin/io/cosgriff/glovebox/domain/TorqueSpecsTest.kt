package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorqueSpecsTest {
    @Test
    fun `torque typed in any unit is kept in newton-metres`() {
        assertEquals(112.5, TorqueSpecs.parse("83", TorqueUnit.FOOT_POUND)!!.newtonMeters, 0.1)
        assertEquals(40.0, TorqueSpecs.parse(" 40 ", TorqueUnit.NEWTON_METER)!!.newtonMeters, 1e-9)
        assertEquals(10.0, TorqueSpecs.parse("88,5", TorqueUnit.INCH_POUND)!!.newtonMeters, 0.01)
    }

    @Test
    fun `nonsense, zero and impossible torques are refused`() {
        assertNull(TorqueSpecs.parse("", TorqueUnit.FOOT_POUND))
        assertNull(TorqueSpecs.parse("tight", TorqueUnit.FOOT_POUND))
        assertNull(TorqueSpecs.parse("0", TorqueUnit.FOOT_POUND))
        assertNull(TorqueSpecs.parse("-30", TorqueUnit.FOOT_POUND))
        assertNull(TorqueSpecs.parse("900", TorqueUnit.FOOT_POUND)) // over 1,000 N·m
    }

    @Test
    fun `a value comes back for editing the way it was typed`() {
        val lugNuts = TorqueSpecs.parse("83", TorqueUnit.FOOT_POUND)!!
        assertEquals("83", TorqueSpecs.editText(lugNuts, TorqueUnit.FOOT_POUND))
        assertEquals("112.5", TorqueSpecs.editText(lugNuts, TorqueUnit.NEWTON_METER))
        assertEquals("7.5", TorqueSpecs.editText(TorqueSpecs.parse("7.5", TorqueUnit.NEWTON_METER)!!, TorqueUnit.NEWTON_METER))
    }

    @Test
    fun `search looks at every word across name, size and note`() {
        assertTrue(TorqueSpecs.matches("", "Lug nuts"))
        assertTrue(TorqueSpecs.matches("drain plug", "Oil drain plug", "14 mm", null))
        assertTrue(TorqueSpecs.matches("LUG 21", "Lug nuts", "21 mm", "Star pattern"))
        assertFalse(TorqueSpecs.matches("caliper", "Lug nuts", "21 mm", null))
    }

    @Test
    fun `unknown stored values fall back instead of failing`() {
        assertEquals(TorqueCategory.WHEELS, TorqueCategory.of("WHEELS"))
        assertEquals(TorqueCategory.OTHER, TorqueCategory.of("Brakes"))
        assertEquals(TorqueSource.FACTORY, TorqueSource.of("FACTORY"))
        assertEquals(TorqueSource.USER, TorqueSource.of(""))
    }

    @Test
    fun `starter rows are names only`() {
        assertTrue(TorqueSpecs.common.map { it.key }.containsAll(listOf("oil-drain-plug", "lug-nuts")))
        assertEquals(TorqueSpecs.common.size, TorqueSpecs.common.map { it.key }.toSet().size)
    }
}
