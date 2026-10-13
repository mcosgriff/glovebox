package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashReadingTest {
    @Test
    fun `a lone number is the reading, with or without a comma or leading zeros`() {
        assertEquals(93_412, DashReading.pick("93412", null))
        assertEquals(93_412, DashReading.pick("93,412", null))
        assertEquals(93_412, DashReading.pick("ODO 093412 mi", null))
        assertEquals(7, DashReading.pick("000007", null))
    }

    @Test
    fun `nothing readable is no reading`() {
        assertNull(DashReading.pick("", null))
        assertNull(DashReading.pick("TRIP A\nmi", 93_000))
        assertNull(DashReading.pick("000000", null))
        // More digits than the sheet takes can't be an odometer.
        assertNull(DashReading.pick("12345678", null))
    }

    @Test
    fun `with a last reading, the odometer is the number at or just past it`() {
        val dash = "10:45  72\nTRIP A 345.6\n93412\n280"
        assertEquals(93_412, DashReading.pick(dash, 93_000))
        assertEquals(93_412, DashReading.pick(dash, 93_412))
        // Two numbers past the last reading: the nearer one.
        assertEquals(93_412, DashReading.pick("93412\n293412", 93_000))
    }

    @Test
    fun `without a last reading the longest whole number wins over a trip meter`() {
        assertEquals(93_412, DashReading.pick("TRIP 345.6\n93412\n72", null))
        assertEquals(1_234, DashReading.pick("1234\n999.9", null))
        // Only a number with tenths in view: its whole part.
        assertEquals(93_412, DashReading.pick("93412.7", null))
    }

    @Test
    fun `with a last reading, a number that can't be the odometer is never offered`() {
        // Lower than the last reading: a trip meter, the clock, or a stray digit read out of the trim.
        assertNull(DashReading.pick("1234\n72", 93_000))
        assertNull(DashReading.pick("8", 93_000))
        // Too far past it to be this vehicle.
        assertNull(DashReading.pick("393412", 93_000))
        assertEquals(293_000, DashReading.pick("293000", 93_000))
    }

    @Test
    fun `a gap where the comma goes is read through, but only when the last reading backs it`() {
        assertEquals(93_412, DashReading.pick("93 412", 93_000))
        assertEquals(listOf(93, 412, 93_412), DashReading.candidates("93 412").map { it.value })
        // With no last reading, two numbers side by side stay two numbers.
        assertEquals(412, DashReading.pick("93 412", null))
        // The temperature beside the trip meter is not an odometer.
        assertEquals(93_412, DashReading.pick("72 345\n93412", 93_000))
        // Not across lines, and not into a decimal.
        assertEquals(listOf(93, 412), DashReading.candidates("93\n412").map { it.value })
        assertEquals(listOf(93, 412), DashReading.candidates("93 412.5").map { it.value })
    }

    @Test
    fun `a whole number at the last reading beats a decimal that comes to the same`() {
        val picked = DashReading.candidates("93412.5 93412")
        assertEquals(listOf(true, false), picked.map { it.decimal })
        assertEquals(93_412, DashReading.pick("93412.5 93412", 93_412))
    }

    @Test
    fun `a reading is offered once three of the last five frames agree`() {
        assertNull(DashReading.steady(listOf(93_412, 93_412)))
        assertEquals(93_412, DashReading.steady(listOf(93_412, null, 93_412, 98_412, 93_412)))
        assertNull(DashReading.steady(listOf(93_412, 93_412, 93_412, null, null, 93_112, null)))
        assertNull(DashReading.steady(listOf(1, 2, 3, 4, 5)))
        assertNull(DashReading.steady(emptyList()))
        // The camera moved to another number: the old one stops being offered once it loses the majority.
        assertEquals(345, DashReading.steady(listOf(93_412, 93_412, 93_412, 345, 345, 345)))
    }
}
