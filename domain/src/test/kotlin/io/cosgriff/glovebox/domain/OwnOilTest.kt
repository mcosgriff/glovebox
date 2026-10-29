package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** The owner's own oil change interval (#517), set on Vehicle details. */
class OwnOilTest {
    private val miles = DistanceUnit.MILES

    private fun counted(
        normal: Interval?,
        own: Interval?,
        severe: Interval? = null,
        useSevere: Boolean = false,
        e85: Interval? = null,
        useE85: Boolean = false,
        oil: Boolean = true,
        unit: DistanceUnit = miles,
    ) = ActiveInterval.counted(normal, severe, useSevere, e85, useE85, oil = oil, unit = unit, own = own)

    @Test
    fun `the owner's interval is the one an oil change counts by`() {
        assertEquals(CountedInterval(Interval(5_000, 6), owners = true), counted(Interval(10_000, 12), own = Interval(5_000, 6)))
    }

    @Test
    fun `it is used whether Severe is on or off`() {
        val own = Interval(6_000, 6)
        val maker = Interval(10_000, 12)
        val severe = Interval(5_000, 6)
        assertEquals(CountedInterval(own, owners = true), counted(maker, own, severe = severe, useSevere = false))
        assertEquals(CountedInterval(own, owners = true), counted(maker, own, severe = severe, useSevere = true))
        // And over a flex-fuel list's E85 figure too: it is the owner's word for this vehicle.
        assertEquals(CountedInterval(own, owners = true), counted(maker, own, e85 = Interval(2_500, null), useE85 = true))
    }

    @Test
    fun `its months follow its miles until the owner types months`() {
        // 7,500 mi with no months typed: 9 months, Glovebox's.
        assertEquals(CountedInterval(Interval(7_500, 9), monthsWorkedOut = true, owners = true), counted(Interval(10_000, 12), own = OwnOil.of(7_500, null)))
        assertEquals(CountedInterval(Interval(16_000, 12), monthsWorkedOut = true, owners = true), counted(Interval(16_000, 12), own = OwnOil.of(16_000, null), unit = DistanceUnit.KILOMETERS))
        assertEquals(9, OwnOil.months(7_500, typed = null, miles))
        assertEquals(6, OwnOil.months(5_000, typed = null, miles))
        assertNull(OwnOil.months(null, typed = null, miles))
    }

    @Test
    fun `typed months stay as typed whatever the miles`() {
        assertEquals(CountedInterval(Interval(7_500, 12), owners = true), counted(Interval(10_000, 12), own = OwnOil.of(7_500, 12)))
        assertEquals(12, OwnOil.months(3_000, typed = 12, miles))
        // Months alone are the owner's too.
        assertEquals(CountedInterval(Interval(null, 6), owners = true), counted(Interval(10_000, null), own = OwnOil.of(null, 6)))
    }

    @Test
    fun `Use the maker's is no interval of the owner's, so the maker's counts with its worked-out months`() {
        assertNull(OwnOil.of(null, null))
        assertEquals(CountedInterval(Interval(10_000, 12), monthsWorkedOut = true), counted(Interval(10_000, null), own = OwnOil.of(null, null)))
        assertFalse(counted(Interval(10_000, 12), own = null)!!.owners)
    }

    @Test
    fun `only the oil change takes it`() {
        assertEquals(CountedInterval(Interval(30_000, null)), counted(Interval(30_000, null), own = Interval(5_000, 6), oil = false))
    }

    @Test
    fun `an oil change with no figures of its own still counts by the owner's`() {
        assertEquals(CountedInterval(Interval(5_000, 6), owners = true), counted(null, own = Interval(5_000, 6)))
    }

    @Test
    fun `what the sheet saves is what was typed, with months left to follow the miles when blank`() {
        assertEquals(Interval(7_500, null), OwnOil.parse("7,500", ""))
        assertEquals(Interval(7_500, 12), OwnOil.parse("7500", "12"))
        assertEquals(Interval(null, 6), OwnOil.parse("", "6"))
        assertNull(OwnOil.parse("", ""))
        assertNull(OwnOil.parse("0", ""))
        assertNull(OwnOil.parse("abc", "6"))
    }
}
