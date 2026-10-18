package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.TakesPart.Inches
import io.cosgriff.glovebox.domain.TakesPart.For
import io.cosgriff.glovebox.domain.TakesPart.Entered
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
    fun `each reads as one line of parts`() {
        assertEquals(
            listOf(Entered("0W-20"), Entered("API SP"), TakesPart.Capacity(6.6, Volume.QUARTS), For(TakesRole.OIL_FILTER, Entered("04152-YZZA1"))),
            VehicleSpecEntry.oilParts("0W-20", "API SP", 6.6, Volume.QUARTS, "04152-YZZA1"),
        )
        assertEquals(listOf(Entered("5W-40"), Entered("VW 502 00"), TakesPart.Capacity(5.7, Volume.LITRES)), VehicleSpecEntry.oilParts("5W-40", "VW 502 00", 5.7, Volume.LITRES, null))
        assertEquals(listOf(TakesPart.Capacity(6.0, Volume.QUARTS)), VehicleSpecEntry.oilParts(null, null, 6.0, null, null))
        assertNull(VehicleSpecEntry.oilParts(null, null, null, null, null))
        assertEquals(listOf(Entered("P265/70R17")), VehicleSpecEntry.tireParts("P265/70R17", null))
        assertEquals(listOf(Entered("P265/70R17")), VehicleSpecEntry.tireParts("P265/70R17", "P265/70R17"))
        assertEquals(
            listOf(For(TakesRole.TIRE_FRONT, Entered("225/40R18")), For(TakesRole.TIRE_REAR, Entered("255/35R18"))),
            VehicleSpecEntry.tireParts("225/40R18", "255/35R18"),
        )
        assertEquals(
            listOf(TakesPart.Both(Inches(26), Inches(17)), For(TakesRole.WIPER_REAR, Inches(14))),
            VehicleSpecEntry.wiperParts(26, 17, 14, null),
        )
        assertEquals(listOf(TakesPart.Both(Inches(22), Inches(22)), Entered("Bosch ICON")), VehicleSpecEntry.wiperParts(22, 22, null, "Bosch ICON"))
        assertEquals(listOf(Inches(24)), VehicleSpecEntry.wiperParts(24, null, null, null))
        assertNull(VehicleSpecEntry.wiperParts(null, null, null, null))
    }

    @Test
    fun `gear oil grades are written one way and other fluids are taken by name`() {
        assertEquals("75W-90 GL-5", VehicleSpecEntry.gearOil("75w90 gl5"))
        assertEquals("75W-85", VehicleSpecEntry.gearOil("SAE 75W-85"))
        assertEquals("80W-90 GL-4", VehicleSpecEntry.gearOil("80W-90 API GL-4"))
        assertEquals("Toyota LF gear oil", VehicleSpecEntry.gearOil(" Toyota  LF gear oil "))
        assertNull(VehicleSpecEntry.gearOil("72W-90"))
        assertNull(VehicleSpecEntry.gearOil(""))
        assertNull(VehicleSpecEntry.fluidName("x".repeat(41)))
    }

    @Test
    fun `brake fluid is a DOT grade`() {
        assertEquals("DOT 3", VehicleSpecEntry.brakeFluid("dot3"))
        assertEquals("DOT 5.1", VehicleSpecEntry.brakeFluid("DOT 5.1"))
        assertEquals("DOT 4 LV", VehicleSpecEntry.brakeFluid("dot 4 lv"))
        listOf("DOT 6", "DOT 4.1", "brake fluid", "").forEach { assertNull(it, VehicleSpecEntry.brakeFluid(it)) }
    }

    @Test
    fun `battery, plug gap and bulbs are checked`() {
        listOf("24F", "35", "94R", "H6", "LN3", "65").forEach { assertEquals(it, it, VehicleSpecEntry.batteryGroup(it.lowercase())) }
        assertNull(VehicleSpecEntry.batteryGroup("big one"))
        assertEquals(650, VehicleSpecEntry.cca("650 CCA"))
        assertNull(VehicleSpecEntry.cca("50"))
        assertEquals("0.044 in", VehicleSpecEntry.plugGap(".044"))
        assertEquals("0.044 in", VehicleSpecEntry.plugGap("0.044\""))
        assertEquals("1.1 mm", VehicleSpecEntry.plugGap("1,1 mm"))
        listOf("0.1", "3 mm", "0.08", "wide").forEach { assertNull(it, VehicleSpecEntry.plugGap(it)) }
        listOf("H11", "9005", "D4S", "3157", "HB3", "H7LL").forEach { assertEquals(it, it, VehicleSpecEntry.bulbCode(it.lowercase())) }
        listOf("bright", "X99", "").forEach { assertNull(it, VehicleSpecEntry.bulbCode(it)) }
    }

    @Test
    fun `tire pressure is a whole number in range and reads as the placard does`() {
        assertEquals(32, VehicleSpecEntry.tirePressure("32", VehicleSpecEntry.Pressure.PSI))
        assertEquals(240, VehicleSpecEntry.tirePressure("240 kPa", VehicleSpecEntry.Pressure.KPA))
        listOf("10", "120", "32.5", "").forEach { assertNull(it, VehicleSpecEntry.tirePressure(it, VehicleSpecEntry.Pressure.PSI)) }
        val psi = VehicleSpecEntry.Pressure.PSI
        assertEquals(listOf(TakesPart.Pressure(32, psi)), VehicleSpecEntry.pressureParts(32, 32, null, psi))
        assertEquals(
            listOf(
                TakesPart.Both(For(TakesRole.PRESSURE_FRONT, TakesPart.Pressure(35, psi)), For(TakesRole.PRESSURE_REAR, TakesPart.Pressure(38, psi))),
                For(TakesRole.PRESSURE_SPARE, TakesPart.Pressure(60, psi)),
            ),
            VehicleSpecEntry.pressureParts(35, 38, 60, null),
        )
        assertEquals(listOf(TakesPart.Pressure(230, VehicleSpecEntry.Pressure.KPA)), VehicleSpecEntry.pressureParts(230, null, null, VehicleSpecEntry.Pressure.KPA))
        assertEquals(listOf(For(TakesRole.PRESSURE_REAR, TakesPart.Pressure(38, psi))), VehicleSpecEntry.pressureParts(null, 38, null, psi))
    }

    @Test
    fun `what it takes keeps the basics apart and splits the rest by what is entered`() {
        val empty = WhatItTakes()
        assertEquals(listOf(TakesItem.OIL, TakesItem.TIRES, TakesItem.WIPERS), empty.basics())
        assertEquals(emptyList<TakesItem>(), empty.entered())
        assertEquals(TakesItem.entries.size - 3, empty.notEntered().size)
        val some = WhatItTakes(transmissionFluid = "ATF WS", engineAirFilter = "17801-0P051", cabinAirFilter = "87139-YZZ08", tireSize = "P265/70R17", tirePressureFront = 32, tirePressureRear = 32)
        assertEquals(listOf(TakesItem.OIL, TakesItem.TIRES, TakesItem.WIPERS), some.basics())
        assertEquals(listOf(TakesItem.TRANSMISSION, TakesItem.AIR_FILTERS), some.entered())
        assertEquals(TakesItem.entries.size - 5, some.notEntered().size)
        assertEquals(listOf(Entered("P265/70R17"), TakesPart.Pressure(32, VehicleSpecEntry.Pressure.PSI)), some.summary(TakesItem.TIRES))
        assertEquals(
            listOf(For(TakesRole.ENGINE_AIR, Entered("17801-0P051")), For(TakesRole.CABIN_AIR, Entered("87139-YZZ08"))),
            some.summary(TakesItem.AIR_FILTERS),
        )
        assertEquals("17801-0P051, 87139-YZZ08", some.partNumbers(TakesItem.AIR_FILTERS))
        assertEquals(listOf(TakesItem.TIRES, TakesItem.TRANSMISSION, TakesItem.AIR_FILTERS), some.lines().map { it.first })
        assertEquals(listOf(TakesPart.Pressure(32, VehicleSpecEntry.Pressure.PSI)), some.hint("rotation"))
    }
}
