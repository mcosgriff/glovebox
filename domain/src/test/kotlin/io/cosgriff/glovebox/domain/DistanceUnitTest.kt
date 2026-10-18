package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitTest {
    private val mi = DistanceUnit.MILES
    private val km = DistanceUnit.KILOMETERS

    @Test
    fun `a vehicle in miles is never converted`() {
        for (n in listOf(1, 499, 5_000, 123_457)) {
            assertEquals(n, mi.measured(n))
            assertEquals(n, mi.scheduled(n))
            assertEquals(n, mi.measuredInMiles(n))
            assertEquals(n, mi.scheduledInMiles(n))
        }
    }

    @Test
    fun `a measurement is converted exactly`() {
        assertEquals(161, km.measured(100))
        assertEquals(135_185, km.measured(84_000))
        assertEquals(84_000, km.measuredInMiles(135_185))
    }

    @Test
    fun `a schedule figure comes out as a manual prints it`() {
        assertEquals(listOf(8_000, 12_000, 24_000, 48_000, 96_000, 160_000), listOf(5_000, 7_500, 15_000, 30_000, 60_000, 100_000).map(km::scheduled))
        assertEquals(5_000, km.scheduledInMiles(8_000))
        assertEquals(6_250, km.scheduledInMiles(10_000))
    }

    @Test
    fun `every typical interval goes to kilometres and back unchanged`() {
        for (item in ScheduleTemplates.generic(null, null, DistanceUnit.MILES)) {
            for (miles in listOfNotNull(item.normal.miles, item.severe?.miles)) {
                assertEquals(item.key, miles, km.scheduledInMiles(km.scheduled(miles)))
                assertEquals("${item.key}: $miles mi should be a round figure in km", 0, km.scheduled(miles) % 100)
            }
        }
    }

    @Test
    fun `changing a vehicle's unit converts readings exactly and schedule figures roundly`() {
        assertEquals(135_185, DistanceConversion.convert(84_000, mi, km, DistanceKind.MEASURED))
        assertEquals(8_000, DistanceConversion.convert(5_000, mi, km, DistanceKind.SCHEDULED))
        assertEquals(5_000, DistanceConversion.convert(8_000, km, mi, DistanceKind.SCHEDULED))
        assertEquals(84_000, DistanceConversion.convert(84_000, mi, mi, DistanceKind.MEASURED))
        // An offset can be negative.
        assertEquals(-161, DistanceConversion.convert(-100, mi, km, DistanceKind.MEASURED))
    }

    @Test
    fun `milestones keep their order and never land on each other`() {
        assertEquals(listOf(16_000, 32_000, 48_000), DistanceConversion.milestones(listOf(10_000, 20_000, 30_000), mi, km))
        assertEquals(listOf(1, 2, 3, 6_250), DistanceConversion.milestones(listOf(1, 2, 3, 10_000), km, mi))
    }

    @Test
    fun `the lead distance follows the vehicle's unit and the days don't`() {
        assertEquals(LeadTime(800, 14), LeadTime(500, 14).forUnit(km, from = mi))
        assertEquals(LeadTime(500, 14), LeadTime(500, 14).forUnit(mi, from = mi))
        // Typed in kilometres, it is kept as typed for a kilometre vehicle and converted for one in miles.
        assertEquals(LeadTime(1_000, 14), LeadTime(1_000, 14).forUnit(km, from = km))
        assertEquals(LeadTime(625, 14), LeadTime(1_000, 14).forUnit(mi, from = km))
    }

    @Test
    fun `the examples Vehicle details gives come out as it says`() {
        assertEquals(161, DistanceConversion.convert(100, mi, km, DistanceKind.MEASURED))
        assertEquals(8_000, DistanceConversion.convert(5_000, mi, km, DistanceKind.SCHEDULED))
        assertEquals(5_000, DistanceConversion.convert(8_000, km, mi, DistanceKind.SCHEDULED))
    }

    @Test
    fun `an unknown stored unit is miles`() {
        assertEquals(mi, DistanceUnit.of(null))
        assertEquals(mi, DistanceUnit.of("FURLONGS"))
        assertEquals(km, DistanceUnit.of("KILOMETERS"))
    }
}
