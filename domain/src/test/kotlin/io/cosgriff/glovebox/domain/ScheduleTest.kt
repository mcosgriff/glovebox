package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    @Test
    fun `interval labels`() {
        assertEquals("7,500 mi / 12 mo", Interval(7_500, 12).label(DistanceUnit.MILES))
        assertEquals("30,000 mi", Interval(30_000, null).label(DistanceUnit.MILES))
        assertEquals("36 mo", Interval(null, 36).label(DistanceUnit.MILES))
    }

    @Test
    fun `severe use switches only the items that have a severe interval`() {
        val items = ScheduleTemplates.generic(null, null, DistanceUnit.MILES).associateBy { it.key }
        val oil = items.getValue("oil")
        assertEquals(Interval(7_500, 12), oil.interval(severeUse = false))
        assertEquals(Interval(5_000, 6), oil.interval(severeUse = true))
        assertTrue(oil.changesWhenSevere)

        val rotation = items.getValue("rotation")
        assertEquals(rotation.normal, rotation.interval(severeUse = true))
        assertFalse(rotation.changesWhenSevere)
    }

    @Test
    fun `differential fluid is only suggested when the drivetrain has one to service`() {
        fun diff(drivetrain: String?) = ScheduleTemplates.generic(drivetrain, null, DistanceUnit.MILES).firstOrNull { it.key == "diff-fluid" }?.name
        assertEquals("Differential & transfer case fluid", diff("4WD/4-Wheel Drive/4x4"))
        assertEquals("Differential & transfer case fluid", diff("AWD/All-Wheel Drive"))
        assertEquals("Differential fluid", diff("RWD/Rear-Wheel Drive"))
        assertNull(diff("FWD/Front-Wheel Drive"))
        assertNull(diff(null))
    }

    @Test
    fun `template keys are unique`() {
        val keys = ScheduleTemplates.generic("4WD", null, DistanceUnit.MILES).map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `interval input accepts blanks and separators`() {
        assertEquals(Interval(10_000, 12), IntervalInput.parse("10,000", "12"))
        assertEquals(Interval(5_000, null), IntervalInput.parse(" 5000 ", ""))
        assertEquals(Interval(null, 24), IntervalInput.parse("", "24"))
    }

    @Test
    fun `interval input rejects unusable entries`() {
        assertNull(IntervalInput.parse("", ""))
        assertNull(IntervalInput.parse("0", "12"))
        assertNull(IntervalInput.parse("5000", "0"))
        assertNull(IntervalInput.parse("-5000", ""))
        assertNull(IntervalInput.parse("5k", ""))
        assertNull(IntervalInput.parse("7.5", ""))
        assertNull(IntervalInput.parse("99999999999", ""))
    }

    @Test
    fun `an electric vehicle is not offered engine items`() {
        val keys = ScheduleTemplates.generic("AWD/All-Wheel Drive", fuel = "Electric", unit = DistanceUnit.MILES).map { it.key }
        assertFalse("oil" in keys)
        assertFalse("engine-air" in keys)
        assertFalse("spark-plugs" in keys)
        assertTrue(keys.containsAll(listOf("rotation", "cabin-air", "brake-fluid", "coolant")))
    }

    @Test
    fun `hybrids and unknown fuel keep the full list`() {
        val full = ScheduleTemplates.generic(null, null, DistanceUnit.MILES).map { it.key }
        assertEquals(full, ScheduleTemplates.generic(null, fuel = "Gasoline", unit = DistanceUnit.MILES).map { it.key })
        assertEquals(full, ScheduleTemplates.generic(null, fuel = null, unit = DistanceUnit.MILES).map { it.key })
        assertTrue("oil" in full)
    }

    @Test
    fun `missing lists the typical items a schedule doesn't have`() {
        val all = ScheduleTemplates.generic(null, null, DistanceUnit.MILES).map { it.key }
        // Only the user's own item so far: everything typical is still on offer.
        assertEquals(all, ScheduleTemplates.missing(null, null, DistanceUnit.MILES, listOf(null)).map { it.key })
        assertEquals(all - "oil", ScheduleTemplates.missing(null, null, DistanceUnit.MILES, listOf("oil", null)).map { it.key })
        assertTrue(ScheduleTemplates.missing(null, null, DistanceUnit.MILES, all).isEmpty())
    }
}
