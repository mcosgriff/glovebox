package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnownTakesTest {
    // Gradle runs a module's tests from the module's folder.
    private val assets = File("../app/src/main/assets/${KnownTakes.ASSET_DIR}")
    private val bundled: List<KnownTakesList> by lazy {
        assets.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }.map { file ->
            when (val parsed = KnownTakes.parse(file.readText())) {
                is KnownTakesFile.Ok -> parsed.list.also { assertEquals("${file.name} is named after its id", "${it.id}.json", file.name) }
                is KnownTakesFile.Invalid -> throw AssertionError("${file.name}: ${parsed.errors}")
            }
        }
    }

    // A made-up model with made-up values: the rules are tested on this, never on a real vehicle's figures.
    private val someTakes = """{
        "oil": { "grade": "5W-30", "approval": "API SP", "capacity": 5.5, "capacityUnit": "qt" },
        "tires": { "sizes": ["215/55R17 94V", "225/45R18 91W"], "pressure": { "front": 33, "rear": 31, "unit": "psi" } },
        "brakeFluid": "DOT 4",
        "sparkPlugs": { "part": "ACME 1234", "gap": "1.1 mm" }
    }"""

    private val someVariants = """[
        { "drive": "AWD", "takes": { "transferCase": "75W-90 GL-5" } },
        { "gearbox": "manual", "takes": { "transmission": "Acme MTF 3" } },
        { "gearbox": "automatic", "takes": { "transmission": "Acme ATF 9" } }
    ]"""

    private fun file(takes: String = someTakes, extra: String = "\"variants\": $someVariants,") = """
        { "id": "made-up", "title": "Made-up Roadster", "make": "Acme", "model": "Roadster", "years": { "from": 2001, "to": 2005 },
          "source": { "name": "Acme owner's manual", "url": "https://example.test/om.pdf", "retrieved": "2026-01-02", "note": "A made-up list.", "basis": "Made up for the tests." },
          $extra "takes": $takes }
    """.trimIndent()

    private val roadster get() = (KnownTakes.parse(file()) as KnownTakesFile.Ok).list

    private fun errors(text: String) = (KnownTakes.parse(text) as KnownTakesFile.Invalid).errors.map { it.toString() }

    @Test
    fun `every list that ships is read and says where it came from and why it may ship`() {
        assertTrue(bundled.isNotEmpty())
        assertEquals(bundled.size, bundled.map { it.id }.toSet().size)
        for (list in bundled) assertTrue(list.source.basis.isNotBlank() && list.source.url?.startsWith("https://") != false)
    }

    @Test
    fun `no two lists claim the same vehicle`() {
        for (a in bundled) for (b in bundled) {
            if (a.id < b.id && a.make.equals(b.make, true) && a.model.equals(b.model, true) && a.engine == b.engine) {
                assertTrue("${a.id} and ${b.id} overlap", a.years.last < b.years.first || b.years.last < a.years.first)
            }
        }
    }

    @Test
    fun `the 4Runner lists say what Toyota's owner's manuals say for each run of years`() {
        // Read out of Toyota's own documents (docs/known-takes.md has which, and the pages).
        fun takes(year: Int, drive: String? = "4WD/4-Wheel Drive/4x4") =
            KnownTakes.forVehicle(KnownTakes.match(bundled, year, "TOYOTA", "4Runner", "4.0L 6-cyl")!!, drive, GearboxOnRecord.ofText("Automatic"))
        for (year in 2017..2024) {
            val t = takes(year)
            assertEquals("0W-20", t.oilGrade)
            assertEquals(if (year <= 2021) "ILSAC GF-5" else "ILSAC GF-6A", t.oilApproval)
            assertEquals(6.6, t.oilCapacity!!, 0.0)
            assertEquals(VehicleSpecEntry.Volume.QUARTS, t.oilCapacityUnit)
            assertEquals("Toyota Genuine ATF WS", t.transmissionFluid)
            assertEquals("75W-85 GL-5", t.differentialOil)
            assertEquals("Toyota Transfer gear oil LF 75W", t.transferCaseOil)
            assertEquals("Toyota Super Long Life Coolant", t.coolant)
            assertEquals("DOT 3", t.brakeFluid)
            assertEquals("DENSO SK16HR11", t.sparkPlug)
            assertEquals("0.043 in", t.sparkPlugGap)
            // Two sizes, by option, and nothing says which a vehicle has: offered, not filled.
            assertNull(t.tireSize)
            assertEquals(listOf(32, 32, 32), listOf(t.tirePressureFront, t.tirePressureRear, t.tirePressureSpare))
            // From 2021 the low beams and fog lights are LED and the halogen high beam is only on some.
            if (year <= 2020) assertEquals(listOf("H11", "9005", "H16"), listOf(t.bulbLow, t.bulbHigh, t.bulbFog)) else assertNull(t.summary(TakesItem.BULBS))
            assertNull(t.summary(TakesItem.BATTERY))
            assertNull(t.summary(TakesItem.WIPERS))
            // A transfer case only where the vehicle is known to be four-wheel drive.
            assertNull(takes(year, "4x2").transferCaseOil)
            assertNull(takes(year, null).transferCaseOil)
        }
        assertEquals(listOf("P265/70R17 113S", "P245/60R20 107H"), KnownTakes.match(bundled, 2019, "Toyota", "4Runner")!!.tireSizes)
        assertNull(KnownTakes.match(bundled, 2016, "TOYOTA", "4Runner"))
        assertNull(KnownTakes.match(bundled, 2025, "TOYOTA", "4Runner"))
    }

    @Test
    fun `the GTI lists say what Volkswagen's fluid charts say, and only where a chart was read`() {
        // Volkswagen's own documents filed with NHTSA (docs/known-takes.md has which).
        fun takes(year: Int, gearbox: String?) = KnownTakes.forVehicle(KnownTakes.match(bundled, year, "VOLKSWAGEN", "GTI", "2.0L 4-cyl")!!, "FWD", GearboxOnRecord.ofText(gearbox))
        for (year in 2010..2014) assertEquals(4.9, takes(year, null).oilCapacity!!, 0.0)
        for (year in 2010..2012) {
            // Only the oil capacity table covers these years.
            assertEquals(listOf(TakesItem.OIL), TakesItem.entries.filter { takes(year, "Automatic").summary(it) != null })
        }
        for (year in 2013..2014) {
            val dsg = takes(year, "Automatic")
            assertEquals("VW 502 00", dsg.oilApproval)
            assertNull(dsg.oilGrade) // the chart allows two grades
            assertEquals("DOT 4", dsg.brakeFluid)
            assertEquals("101 905 631 H", dsg.sparkPlug)
            assertNull(dsg.sparkPlugGap) // given as a range, which a row can't hold
            assertEquals("VW DSG fluid G 052 182 A2", dsg.transmissionFluid)
            assertEquals("VW gear oil G 052 171 A2", takes(year, "Manual/Standard").transmissionFluid)
            assertNull(takes(year, null).transmissionFluid)
        }
        assertEquals("VW G 12 plus-plus (purple)", takes(2013, null).coolant)
        assertEquals("VW G 13 (pink)", takes(2014, null).coolant)
        assertNull(KnownTakes.match(bundled, 2015, "Volkswagen", "GTI"))
    }

    @Test
    fun `a list is read, and only for its own make, model and years`() {
        val r = roadster
        assertEquals(listOf("215/55R17 94V", "225/45R18 91W"), r.tireSizes)
        assertNull(r.takes.tireSize)
        assertEquals(33, r.takes.tirePressureFront)
        assertEquals(r, KnownTakes.match(listOf(r), 2003, "ACME", "road ster"))
        assertNull(KnownTakes.match(listOf(r), 2006, "Acme", "Roadster"))
        assertNull(KnownTakes.match(listOf(r), 2003, "Acme", "Roadster GT"))
    }

    @Test
    fun `a single tire size is filled`() {
        val one = (KnownTakes.parse(file("""{ "tires": { "sizes": ["215/55R17 94V"] } }""", "")) as KnownTakesFile.Ok).list
        assertEquals("215/55R17 94V", one.takes.tireSize)
    }

    @Test
    fun `a variant is used only for a vehicle known to have that drive or gearbox`() {
        fun t(drive: String?, gearbox: String?) = KnownTakes.forVehicle(roadster, drive, GearboxOnRecord.ofText(gearbox))
        assertEquals("75W-90 GL-5", t("AWD/All-Wheel Drive", null).transferCaseOil)
        assertNull(t("4WD/4-Wheel Drive/4x4", null).transferCaseOil)
        assertNull(t(null, null).transferCaseOil)
        assertEquals("Acme MTF 3", t(null, "Manual/Standard").transmissionFluid)
        assertEquals("Acme ATF 9", t(null, "Dual-Clutch Transmission (DCT)").transmissionFluid)
        assertEquals("Acme ATF 9", t(null, "Automatic").transmissionFluid)
        assertNull(t(null, null).transmissionFluid)
        assertEquals("Acme ATF 9", t(null, "Automated Manual Transmission (AMT)").transmissionFluid)
    }

    @Test
    fun `every value has to be written as its row stores it`() {
        assertTrue(errors(file("""{ "oil": { "grade": "5w30" } }""", "")).any { "takes.oil.grade" in it && "5W-30" in it })
        assertTrue(errors(file("""{ "brakeFluid": "DOT 7" } """, "")).any { "takes.brakeFluid" in it })
        assertTrue(errors(file("""{ "tires": { "sizes": ["215/55 R17"] } }""", "")).any { "215/55R17" in it })
        assertTrue(errors(file("""{ "sparkPlugs": { "gap": ".044" } }""", "")).any { "0.044 in" in it })
        assertTrue(errors(file("""{ "oil": { "capacity": 5.5 } }""", "")).any { "capacityUnit" in it })
        assertTrue(errors(file("""{ "oil": { "capacity": 5.55, "capacityUnit": "qt" } }""", "")).any { "takes.oil.capacity" in it })
        assertTrue(errors(file("""{ "wipers": { "driver": 40 } }""", "")).any { "takes.wipers.driver" in it })
        assertTrue(errors(file("""{ "bulbs": {} }""", "")).any { "takes.bulbs" in it })
        assertTrue(errors(file("""{}""", "")).any { "takes" in it })
        assertTrue(errors(file("""{ "oli": { "grade": "5W-30" } }""", "")).any { "takes.oli" in it })
    }

    @Test
    fun `variants can't contradict the whole model or each other`() {
        assertTrue(errors(file(extra = """"variants": [{ "drive": "AWD", "takes": { "brakeFluid": "DOT 3" } }],""")).any { "brakeFluid" in it })
        // Different drives can't both fit, so they may each say the same row; a drive and a gearbox could.
        assertTrue(KnownTakes.parse(file(extra = """"variants": [{ "drive": "AWD", "takes": { "coolant": "A" } }, { "drive": "4WD", "takes": { "coolant": "B" } }],""")) is KnownTakesFile.Ok)
        assertTrue(errors(file(extra = """"variants": [{ "drive": "AWD", "takes": { "coolant": "A" } }, { "gearbox": "manual", "takes": { "coolant": "B" } }],""")).any { "coolant" in it })
        assertTrue(errors(file(extra = """"variants": [{ "takes": { "coolant": "A" } }],""")).any { "variants[0]" in it })
        assertTrue(errors(file(extra = """"variants": [{ "drive": "FWD", "takes": { "coolant": "A" } }],""")).any { "variants[0].drive" in it })
    }

    @Test
    fun `filling takes only rows the owner left empty, each whole`() {
        val known = KnownTakes.forVehicle(roadster, null, GearboxOnRecord.ofText("Manual/Standard"))
        val mine = WhatItTakes(oilGrade = "0W-20", coolant = "Mine")
        val plan = KnownTakes.plan(known, mine)
        assertEquals(listOf(TakesItem.TIRES, TakesItem.TRANSMISSION, TakesItem.BRAKE_FLUID, TakesItem.SPARK_PLUGS), plan.filled)
        assertEquals(listOf(TakesItem.OIL), plan.kept)
        // The owner's oil row stays exactly theirs: no capacity or approval slipped in beside their grade.
        assertEquals("0W-20", plan.result.oilGrade)
        assertNull(plan.result.oilApproval)
        assertNull(plan.result.oilCapacity)
        assertEquals("Mine", plan.result.coolant)
        assertEquals("Acme MTF 3", plan.result.transmissionFluid)
        assertEquals("DOT 4", plan.result.brakeFluid)
        assertTrue(KnownTakes.plan(known, plan.result).filled.isEmpty())
    }

    @Test
    fun `a filled row stays to be checked until the owner checks it or changes it`() {
        val line = listOf<TakesPart>(TakesPart.Entered("DOT 4"))
        assertTrue(KnownTakes.stillToCheck(line, line, checked = false))
        assertFalse(KnownTakes.stillToCheck(line, line, checked = true))
        assertFalse(KnownTakes.stillToCheck(line, listOf(TakesPart.Entered("DOT 3")), checked = false))
        assertFalse(KnownTakes.stillToCheck(line, null, checked = false))
    }
}
