package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.VehicleSpecEntry.Volume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleSpecEntryTest {
    @Test
    fun `an oil grade is written one way however it is typed`() {
        listOf("5W-30", "5w30", "5W 30", "SAE 5W-30", " sae 5w-30 ").forEach { assertEquals(it, "5W-30", VehicleSpecEntry.oilGrade(it)) }
        assertEquals("0W-20", VehicleSpecEntry.oilGrade("0w20"))
        assertEquals("0W-8", VehicleSpecEntry.oilGrade("0W-8"))
        assertEquals("30", VehicleSpecEntry.oilGrade("SAE 30"))
        listOf("5W-35", "7W-30", "5W", "W-30", "synthetic", "", "30W").forEach { assertNull(it, VehicleSpecEntry.oilGrade(it)) }
        assertEquals(VehicleSpecEntry.commonGrades, VehicleSpecEntry.commonGrades.map { VehicleSpecEntry.oilGrade(it) })
    }

    @Test
    fun `capacity is a sensible amount to one decimal place`() {
        assertEquals(6.6, VehicleSpecEntry.oilCapacity("6.6", Volume.QUARTS)!!, 0.0)
        assertEquals(5.2, VehicleSpecEntry.oilCapacity("5,2", Volume.LITRES)!!, 0.0)
        assertEquals(5.0, VehicleSpecEntry.oilCapacity("5", Volume.QUARTS)!!, 0.0)
        listOf("0.5", "21", "6.65", "six", "").forEach { assertNull(it, VehicleSpecEntry.oilCapacity(it, Volume.QUARTS)) }
        assertNull(VehicleSpecEntry.oilCapacity("19.5", Volume.LITRES))
    }

    @Test
    fun `filter part numbers are uppercased, up to three of them`() {
        assertEquals("04152-YZZA1", VehicleSpecEntry.partNumbers(" 04152-yzza1 "))
        assertEquals("06L115562B, XG10575", VehicleSpecEntry.partNumbers("06L115562B,xg10575"))
        listOf("", "a", "04152_YZZA1", "1, 2, 3, 4", "PART#1").forEach { assertNull(it, VehicleSpecEntry.partNumbers(it)) }
    }

    @Test
    fun `a tire size is checked and written as printed`() {
        assertEquals("P265/70R17", VehicleSpecEntry.tireSize("p265/70r17"))
        assertEquals("P265/70R17", VehicleSpecEntry.tireSize("P265 / 70 R17"))
        assertEquals("225/40ZR18 92Y", VehicleSpecEntry.tireSize("225/40zr18 92y"))
        assertEquals("LT285/75R16 126/123R", VehicleSpecEntry.tireSize("LT285/75R16 126/123R"))
        assertEquals("33x12.50R15LT", VehicleSpecEntry.tireSize("33X12.50R15 LT"))
        assertEquals("35x12.50R17LT 121Q", VehicleSpecEntry.tireSize("35x12.50r17lt 121q"))
        listOf(
            "P263/70R17", // a width not a multiple of 5
            "P265/95R17", // no such aspect ratio
            "P265/70R30", // no such rim
            "225/40R18 92X", // no such speed rating
            "265/70",
            "big ones",
        ).forEach { assertNull(it, VehicleSpecEntry.tireSize(it)) }
    }

    @Test
    fun `a wiper blade is ten to thirty-four inches`() {
        assertEquals(26, VehicleSpecEntry.wiperLength("26"))
        assertEquals(17, VehicleSpecEntry.wiperLength("17\""))
        listOf("9", "35", "26.5", "", "long").forEach { assertNull(it, VehicleSpecEntry.wiperLength(it)) }
    }

    @Test
    fun `each reads as one line`() {
        assertEquals("0W-20 · API SP · 6.6 qt · filter 04152-YZZA1", VehicleSpecEntry.oilSummary("0W-20", "API SP", 6.6, Volume.QUARTS, "04152-YZZA1"))
        assertEquals("5W-40 · VW 502 00 · 5.7 L", VehicleSpecEntry.oilSummary("5W-40", "VW 502 00", 5.7, Volume.LITRES, null))
        assertEquals("6 qt", VehicleSpecEntry.oilSummary(null, null, 6.0, Volume.QUARTS, null))
        assertNull(VehicleSpecEntry.oilSummary(null, null, null, null, null))
        assertEquals("P265/70R17", VehicleSpecEntry.tireSummary("P265/70R17", null))
        assertEquals("P265/70R17", VehicleSpecEntry.tireSummary("P265/70R17", "P265/70R17"))
        assertEquals("Front 225/40R18 · Rear 255/35R18", VehicleSpecEntry.tireSummary("225/40R18", "255/35R18"))
        assertEquals("26\" / 17\" · rear 14\"", VehicleSpecEntry.wiperSummary(26, 17, 14, null))
        assertEquals("22\" / 22\" · Bosch ICON", VehicleSpecEntry.wiperSummary(22, 22, null, "Bosch ICON"))
        assertNull(VehicleSpecEntry.wiperSummary(null, null, null, null))
    }
}
