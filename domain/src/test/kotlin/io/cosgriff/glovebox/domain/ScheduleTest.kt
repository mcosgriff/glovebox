package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
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
    fun `the E85 interval wins while it is on, and severe use governs otherwise`() {
        val normal = Interval(5_000, 6)
        val severe = Interval(2_500, null)
        val e85 = Interval(2_500, 3)
        assertEquals(normal, ActiveInterval.of(normal, severe, useSevere = false))
        // Part by part: severe use's missing months fall back to the normal ones.
        assertEquals(Interval(2_500, 6), ActiveInterval.of(normal, severe, useSevere = true))
        assertEquals(e85, ActiveInterval.of(normal, severe, useSevere = true, e85 = e85, useE85 = true))
        assertEquals(e85, ActiveInterval.of(normal, null, useSevere = false, e85 = e85, useE85 = true))
        // Off, it changes nothing; on, an item without one keeps its own.
        assertEquals(normal, ActiveInterval.of(normal, null, useSevere = false, e85 = e85, useE85 = false))
        assertEquals(normal, ActiveInterval.of(normal, null, useSevere = false, e85 = null, useE85 = true))
        assertNull(ActiveInterval.of(null, null, useSevere = true))
    }

    @Test
    fun `differential fluid is only suggested when the drivetrain has one to service`() {
        fun diff(drivetrain: String?) = ScheduleTemplates.generic(drivetrain, null, DistanceUnit.MILES).firstOrNull { it.key == "diff-fluid" }?.name
        assertEquals(TemplateName.DIFF_AND_TRANSFER, diff("4WD/4-Wheel Drive/4x4"))
        assertEquals(TemplateName.DIFF_AND_TRANSFER, diff("AWD/All-Wheel Drive"))
        assertEquals(TemplateName.DIFF, diff("RWD/Rear-Wheel Drive"))
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
