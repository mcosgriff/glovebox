package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnownSchedulesTest {
    // Gradle runs a module's tests from the module's folder.
    private val assets = File("../app/src/main/assets/${KnownSchedules.ASSET_DIR}")
    private val bundled: List<KnownScheduleList> by lazy {
        assets.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }.map { file ->
            when (val parsed = KnownSchedules.parse(file.readText())) {
                is KnownScheduleFile.Ok -> parsed.list
                is KnownScheduleFile.Invalid -> throw AssertionError("${file.name}: ${parsed.errors}")
            }
        }
    }

    // A made-up model with a made-up schedule: the rules are tested on this, never on a real vehicle's.
    private val someTable = """[
        { "atMiles": 4000, "orMonths": 4, "work": [ { "name": "Change the oil", "service": "oil" }, { "name": "Look at the belts" } ] },
        { "atMiles": 8000, "orMonths": 8, "work": [ { "name": "Change the oil", "service": "oil" }, { "name": "Swap the tires round", "service": "rotation" } ] },
        { "atMiles": 12000, "orMonths": 12, "work": [ { "name": "Change the oil", "service": "oil" }, { "name": "Grease the hinges" } ] },
        { "atMiles": 16000, "orMonths": 16, "work": [ { "name": "Change the oil", "service": "oil" }, { "name": "Swap the tires round", "service": "rotation" } ] }
    ]"""
    private val someIntervals = """[
        { "name": "Change the oil", "everyMiles": 4000, "everyMonths": 4, "severe": { "everyMiles": 2000 }, "service": "oil" },
        { "name": "Swap the tires round", "everyMiles": 8000, "everyMonths": 8, "service": "rotation" }
    ]"""

    private fun file(table: String = someTable, intervals: String = someIntervals, extra: String = "", model: String = "Roadster") = """
        { "id": "made-up", "title": "Made-up Roadster", "make": "Acme", "model": "$model", "years": { "from": 2001, "to": 2005 },
          "source": { "name": "Acme service booklet", "url": "https://example.test/booklet", "retrieved": "2026-01-02", "note": "A made-up booklet.", "basis": "Made up for the tests." },
          $extra "milestones": $table, "intervals": $intervals }
    """.trimIndent()

    private val roadster get() = (KnownSchedules.parse(file()) as KnownScheduleFile.Ok).list

    private fun errors(text: String) = (KnownSchedules.parse(text) as KnownScheduleFile.Invalid).errors.map { it.toString() }

    private fun saved(id: Long, name: String, templateKey: String? = null, tracked: Boolean = true) =
        SavedScheduleItem(id, name, Interval(5_000, 6), null, null, templateKey, tracked)

    @Test
    fun `every bundled list parses, says where it came from and why it may be shipped`() {
        assertTrue("no lists found in ${assets.absolutePath}", bundled.isNotEmpty())
        bundled.forEach { list ->
            assertTrue(list.id, list.source.url!!.startsWith("https://"))
            assertTrue(list.id, list.source.basis.length > 40)
            assertTrue(list.id, list.milestones.isNotEmpty())
        }
        assertEquals(bundled.size, bundled.map { it.id }.toSet().size)
    }

    @Test
    fun `no two bundled lists are for the same vehicle`() {
        bundled.forEach { list ->
            // A list for one engine is found by that engine; one for every engine, by the model alone.
            val engine = list.engine?.let { "${it.displacementL}L" }
            (listOf(list.model) + list.modelAliases).forEach { model ->
                list.years.forEach { year -> assertEquals(list.id, KnownSchedules.match(bundled, year, list.make, model, engine, list.engine?.fuel, list.engine?.powertrain, list.body)?.id) }
            }
        }
    }

    @Test
    fun `a list is found by another name NHTSA gives the same car, and by nothing near it`() {
        val list = (KnownSchedules.parse(file(extra = "\"modelAliases\": [\"Super Roadster\"],")) as KnownScheduleFile.Ok).list
        assertEquals("made-up", KnownSchedules.match(listOf(list), 2003, "Acme", "Super Roadster")?.id)
        assertEquals("made-up", KnownSchedules.match(listOf(list), 2003, "ACME", "superroadster")?.id)
        assertEquals("made-up", KnownSchedules.match(listOf(list), 2003, "Acme", "Roadster")?.id)
        assertNull(KnownSchedules.match(listOf(list), 2003, "Acme", "Super Roadster Sport"))
        assertNull(KnownSchedules.match(listOf(list), 2003, "Acme", "Super"))
        assertTrue(errors(file(extra = "\"modelAliases\": [\"roadster\"],")).single().contains("repeats the model"))
        assertTrue(errors(file(extra = "\"modelAliases\": [],")).isNotEmpty())
    }

    @Test
    fun `a plug-in typed without NHTSA's (PHEV) still finds its list, and other brackets still count`() {
        assertEquals("toyota-prius-prime-2023-2024", KnownSchedules.match(bundled, 2023, "Toyota", "Prius Prime")?.id)
        assertEquals("toyota-rav4-prime-2021-2024", KnownSchedules.match(bundled, 2022, "Toyota", "rav4 prime")?.id)
        val hybrid = (KnownSchedules.parse(file(model = "Roadster (HEV)")) as KnownScheduleFile.Ok).list
        assertEquals("made-up", KnownSchedules.match(listOf(hybrid), 2003, "Acme", "Roadster")?.id)
        // A bracket that names the model (as Honda's "VT750 (Shadow Aero 750)" does) is part of it.
        val named = (KnownSchedules.parse(file(model = "Roadster (Spyder)")) as KnownScheduleFile.Ok).list
        assertNull(KnownSchedules.match(listOf(named), 2003, "Acme", "Roadster"))
    }

    @Test
    fun `a model with a list per engine is matched by the engine, and asked about when it isn't known`() {
        fun engineList(id: String, engine: String) = (KnownSchedules.parse(file(extra = "\"engine\": $engine,").replace("\"id\": \"made-up\"", "\"id\": \"$id\"")) as KnownScheduleFile.Ok).list
        val four = engineList("four", """{ "label": "1.6L 4-cylinder", "displacementL": 1.6 }""")
        val six = engineList("six", """{ "label": "2.8L V6", "displacementL": 2.8 }""")
        val diesel = engineList("diesel", """{ "label": "2.8L diesel", "displacementL": 2.8, "fuel": "Diesel" }""")
        val lists = listOf(four, six)
        fun match(engine: String?, fuel: String? = null, of: List<KnownScheduleList> = lists) = KnownSchedules.match(of, 2003, "Acme", "Roadster", engine, fuel)?.id
        fun choices(engine: String?, fuel: String? = null, of: List<KnownScheduleList> = lists) = KnownSchedules.engineChoices(of, 2003, "Acme", "Roadster", engine, fuel).map { it.id }
        // As a VIN decode writes it, and as an owner might.
        assertEquals("four", match("1.6L 4-cyl"))
        assertEquals("six", match("2.8 L V6"))
        assertEquals(emptyList<String>(), choices("1.6L 4-cyl"))
        // Not on record, or no size in what was typed: no list is chosen, and both are offered.
        listOf(null, "", "V6", "the big one").forEach { engine ->
            assertNull("$engine", match(engine))
            assertEquals("$engine", listOf("four", "six"), choices(engine))
        }
        // An engine none of them is for: nothing, and nothing to pick between.
        assertNull(match("3.5L 6-cyl"))
        assertEquals(emptyList<String>(), choices("3.5L 6-cyl"))
        // Two engines of one size told apart by fuel; with the fuel unknown the owner is asked.
        val both = listOf(six.copy(engine = six.engine!!.copy(fuel = "Gasoline")), diesel)
        assertEquals("diesel", match("2.8L 6-cyl", "Diesel", both))
        assertEquals("six", match("2.8L 6-cyl", "Gasoline", both))
        assertNull(match("2.8L 6-cyl", null, both))
        assertEquals(listOf("six", "diesel"), choices("2.8L 6-cyl", null, both))
        assertEquals(listOf("diesel"), choices(null, "Diesel", both))
        // A list for every engine still matches whatever the engine, as the ones that ship do.
        assertEquals("made-up", KnownSchedules.match(listOf(roadster), 2003, "Acme", "Roadster", null, null)?.id)
        assertEquals("made-up", KnownSchedules.match(listOf(roadster), 2003, "Acme", "Roadster", "9.9L", "Coal")?.id)
        // A wrong year or model offers nothing to pick between either.
        assertEquals(emptyList<String>(), KnownSchedules.engineChoices(lists, 2010, "Acme", "Roadster"))
    }

    @Test
    fun `an E170 Corolla gets its own list, a 2019 two-litre keeps the newer one, and the Valvematic pump reminds no one`() {
        assertEquals("toyota-corolla-2017-2019", KnownSchedules.match(bundled, 2019, "Toyota", "Corolla", "1.8L 4-cyl")?.id)
        assertEquals("toyota-corolla-2019-2022-2.0", KnownSchedules.match(bundled, 2019, "Toyota", "Corolla", "2.0L 4-cyl")?.id)
        assertNull(KnownSchedules.match(bundled, 2019, "Toyota", "Corolla", null))
        assertEquals(
            listOf("toyota-corolla-2017-2019", "toyota-corolla-2019-2022-2.0"),
            KnownSchedules.engineChoices(bundled, 2019, "Toyota", "Corolla", null).map { it.id }.sorted(),
        )
        assertEquals("toyota-corolla-2016", KnownSchedules.match(bundled, 2016, "Toyota", "Corolla", "1.8L 4-cyl")?.id)
        assertNull(KnownSchedules.match(bundled, 2015, "Toyota", "Corolla", "1.8L 4-cyl"))
        listOf("toyota-corolla-2016", "toyota-corolla-2017-2019").forEach { id ->
            val list = bundled.single { it.id == id }
            val pump = list.milestones.flatMap { m -> m.work.filter { "vacuum pump" in it.name }.map { m.atMiles to it } }
            assertEquals(id, listOf(120000), pump.map { it.first })
            assertEquals(id, "Replace brake booster vacuum pump (Valvematic engine)", pump.single().second.name)
            assertNull(id, pump.single().second.service)
        }
    }

    @Test
    fun `a hybrid and its petrol twin of one size are told apart by powertrain, and asked about when it isn't known`() {
        fun engineList(id: String, engine: String) = (KnownSchedules.parse(file(extra = "\"engine\": $engine,").replace("\"id\": \"made-up\"", "\"id\": \"$id\"")) as KnownScheduleFile.Ok).list
        val gas = engineList("gas", """{ "label": "2.5L 4-cylinder", "displacementL": 2.5, "powertrain": "gas" }""")
        val hybrid = engineList("hybrid", """{ "label": "2.5L hybrid", "displacementL": 2.5, "powertrain": "hybrid" }""")
        assertEquals(Powertrain.HYBRID, hybrid.engine!!.powertrain)
        val lists = listOf(gas, hybrid)
        fun match(engine: String?, powertrain: Powertrain?) = KnownSchedules.match(lists, 2003, "Acme", "Roadster", engine, "Gasoline", powertrain)?.id
        fun choices(engine: String?, powertrain: Powertrain?) = KnownSchedules.engineChoices(lists, 2003, "Acme", "Roadster", engine, "Gasoline", powertrain).map { it.id }
        assertEquals("hybrid", match("2.5L 4-cyl", Powertrain.HYBRID))
        assertEquals("gas", match("2.5L 4-cyl", Powertrain.GAS))
        // The size alone says nothing about which: the owner is asked, and only between the two.
        assertNull(match("2.5L 4-cyl", null))
        assertEquals(listOf("gas", "hybrid"), choices("2.5L 4-cyl", null))
        assertEquals(listOf("hybrid"), choices(null, Powertrain.HYBRID))
        // A plug-in is neither: nothing matches and nothing is offered.
        assertNull(match("2.5L 4-cyl", Powertrain.PLUG_IN))
        assertEquals(emptyList<String>(), choices("2.5L 4-cyl", Powertrain.PLUG_IN))
        // A list without a powertrain is for every powertrain, as the ones that ship are.
        assertEquals("made-up", KnownSchedules.match(listOf(roadster), 2003, "Acme", "Roadster", null, null, Powertrain.HYBRID)?.id)
        assertTrue(errors(file(extra = "\"engine\": { \"label\": \"2.5L\", \"displacementL\": 2.5, \"powertrain\": \"steam\" },")).single().contains("must be one of"))
    }

    @Test
    fun `a sedan and a hatchback of one engine are told apart by body, and asked about when it isn't known`() {
        fun bodyList(id: String, body: String) = (
            KnownSchedules.parse(file(extra = "\"engine\": { \"label\": \"1.6L\", \"displacementL\": 1.6 }, \"body\": \"$body\",").replace("\"id\": \"made-up\"", "\"id\": \"$id\""))
                as KnownScheduleFile.Ok
            ).list
        val sedan = bodyList("sedan", "sedan")
        val hatch = bodyList("hatch", "hatchback")
        assertEquals(BodyStyle.HATCHBACK, hatch.body)
        val lists = listOf(sedan, hatch)
        fun match(engine: String?, body: BodyStyle?) = KnownSchedules.match(lists, 2003, "Acme", "Roadster", engine, null, null, body)?.id
        fun choices(engine: String?, body: BodyStyle?) = KnownSchedules.engineChoices(lists, 2003, "Acme", "Roadster", engine, null, null, body).map { it.id }
        assertEquals("sedan", match("1.6L 4-cyl", BodyStyle.SEDAN))
        assertEquals("hatch", match("1.6L 4-cyl", BodyStyle.HATCHBACK))
        assertEquals(emptyList<String>(), choices("1.6L 4-cyl", BodyStyle.SEDAN))
        // A blank body matches neither; the owner is asked, between the two.
        assertNull(match("1.6L 4-cyl", null))
        assertEquals(listOf("sedan", "hatch"), choices("1.6L 4-cyl", null))
        assertEquals(listOf("hatch"), choices(null, BodyStyle.HATCHBACK))
        // An engine neither is for rules both out, whatever the body.
        assertEquals(emptyList<String>(), choices("2.8L V6", null))
        // A list for every engine and one body is offered by its body alone.
        val anyEngine = (KnownSchedules.parse(file(extra = "\"body\": \"sedan\",")) as KnownScheduleFile.Ok).list
        assertNull(KnownSchedules.match(listOf(anyEngine), 2003, "Acme", "Roadster"))
        assertEquals(listOf("made-up"), KnownSchedules.engineChoices(listOf(anyEngine), 2003, "Acme", "Roadster").map { it.id })
        assertEquals("made-up", KnownSchedules.match(listOf(anyEngine), 2003, "Acme", "Roadster", body = BodyStyle.SEDAN)?.id)
        assertEquals(emptyList<String>(), KnownSchedules.engineChoices(listOf(anyEngine), 2003, "Acme", "Roadster", body = BodyStyle.HATCHBACK))
        // A list without a body is for every body, as the ones that ship mostly are.
        assertEquals("made-up", KnownSchedules.match(listOf(roadster), 2003, "Acme", "Roadster", body = BodyStyle.HATCHBACK)?.id)
        assertEquals("made-up", KnownSchedules.match(listOf(roadster), 2003, "Acme", "Roadster")?.id)
        assertTrue(errors(file(extra = "\"body\": \"coupe\",")).single().contains("must be one of"))
    }

    @Test
    fun `the 2026 Corolla gas lists go by body, and the earlier Corollas don't`() {
        fun match(year: Int, engine: String?, body: BodyStyle?) = KnownSchedules.match(bundled, year, "Toyota", "Corolla", engine, "Gasoline", null, body)?.id
        assertEquals("toyota-corolla-2026-sedan", match(2026, "2.0L 4-cyl", BodyStyle.SEDAN))
        assertEquals("toyota-corolla-2026-hatchback", match(2026, "2.0L 4-cyl", BodyStyle.HATCHBACK))
        assertNull(match(2026, "2.0L 4-cyl", null))
        assertEquals(
            listOf("toyota-corolla-2026-hatchback", "toyota-corolla-2026-sedan"),
            KnownSchedules.engineChoices(bundled, 2026, "Toyota", "Corolla", "2.0L 4-cyl", "Gasoline").map { it.id }.sorted(),
        )
        // The lists before 2026 name no body, so they match as they did, whatever is on record.
        listOf(null, BodyStyle.SEDAN, BodyStyle.HATCHBACK).forEach { body ->
            assertEquals("toyota-corolla-2025-2.0", match(2025, "2.0L 4-cyl", body))
            assertEquals("toyota-corolla-2023-2024-2.0", match(2024, "2.0L 4-cyl", body))
            assertEquals("toyota-corolla-2019-2022-2.0", match(2021, "2.0L 4-cyl", body))
        }
        assertEquals(listOf("toyota-corolla-2026-hatchback", "toyota-corolla-2026-sedan"), bundled.filter { it.body != null }.map { it.id }.sorted())
    }

    @Test
    fun `a body is taken from vPIC only where it says so plainly`() {
        assertEquals(BodyStyle.SEDAN, BodyStyles.fromVpic("Sedan/Saloon"))
        assertEquals(BodyStyle.HATCHBACK, BodyStyles.fromVpic("Hatchback/Liftback/Notchback"))
        assertNull(BodyStyles.fromVpic(null))
        assertNull(BodyStyles.fromVpic(""))
        assertNull(BodyStyles.fromVpic("Coupe"))
        assertNull(BodyStyles.fromVpic("Sport Utility Vehicle (SUV)/Multi-Purpose Vehicle (MPV)"))
        assertEquals(BodyStyle.HATCHBACK, BodyStyles.of("HATCHBACK"))
        assertNull(BodyStyles.of(null))
        assertNull(BodyStyles.of("COUPE"))
    }

    @Test
    fun `a powertrain is taken from vPIC only where it says so plainly`() {
        assertEquals(Powertrain.HYBRID, Powertrains.fromVpic("Strong HEV (Hybrid Electric Vehicle)", "Gasoline", "Electric"))
        assertEquals(Powertrain.PLUG_IN, Powertrains.fromVpic("PHEV (Plug-in Hybrid Electric Vehicle)", "Gasoline", "Electric"))
        assertEquals(Powertrain.ELECTRIC, Powertrains.fromVpic("BEV (Battery Electric Vehicle)", "Electric", null))
        assertEquals(Powertrain.FLEX_FUEL, Powertrains.fromVpic(null, "Gasoline", "Ethanol (E85)"))
        assertEquals(Powertrain.FLEX_FUEL, Powertrains.fromVpic(null, "Flexible Fuel Vehicle (FFV)", null))
        // A blank is "can't tell", never petrol; so is a mild hybrid or one of unknown level.
        assertNull(Powertrains.fromVpic(null, "Gasoline", null))
        assertNull(Powertrains.fromVpic("Mild HEV (Hybrid Electric Vehicle)", "Gasoline", "Electric"))
        assertNull(Powertrains.fromVpic("HEV (Hybrid Electric Vehicle) - Level Unknown", "Gasoline", "Electric"))
        assertEquals(Powertrain.PLUG_IN, Powertrains.of("PLUG_IN"))
        assertNull(Powertrains.of(null))
        assertNull(Powertrains.of("STEAM"))
    }

    @Test
    fun `an engine's size is read from its text and never guessed`() {
        assertEquals(4.0, Engines.displacementL("4.0L 6-cyl")!!, 0.0)
        assertEquals(2.0, Engines.displacementL("2.0 l turbo")!!, 0.0)
        assertEquals(1.25, Engines.displacementL("1.25L")!!, 0.0)
        assertNull(Engines.displacementL("V6"))
        assertNull(Engines.displacementL("1GR-FE"))
        assertNull(Engines.displacementL("4 LED headlights"))
        assertNull(Engines.displacementL(null))
        assertTrue(errors(file(extra = "\"engine\": { \"label\": \"Big\", \"displacementL\": 40 },")).single().contains("between 0.1 and 20"))
        assertTrue(errors(file(extra = "\"engine\": { \"displacementL\": 2.0 },")).isNotEmpty())
    }

    @Test
    fun `the 4Runner list is the maintenance log of Toyota's guide`() {
        val list = KnownSchedules.match(bundled, 2020, "TOYOTA", "4Runner")!!
        assertEquals(2017..2024, list.years)
        // 2016 has a list of its own: its guide has no cabin-filter inspection at 15,000, 45,000, 75,000 and 105,000.
        assertEquals("toyota-4runner-2015-2016", KnownSchedules.match(bundled, 2016, "Toyota", "4Runner")?.id)
        assertNull(KnownSchedules.match(bundled, 2025, "Toyota", "4Runner"))
        // One page of the log per 5,000 miles and 6 months, to 120,000.
        assertEquals((5_000..120_000 step 5_000).toList(), list.milestones.map { it.atMiles })
        assertTrue(list.milestones.all { it.orMonths == it.atMiles / 5_000 * 6 })
        fun at(miles: Int) = list.milestones.single { it.atMiles == miles }.work
        fun services(miles: Int) = at(miles).mapNotNull { it.service }.toSet()
        assertEquals(setOf("rotation", "brake-inspect"), services(5_000))
        assertEquals(setOf("oil", "rotation", "brake-inspect"), services(10_000))
        // The differential oils are checked every 15,000, and changed then only for towing.
        assertEquals(setOf("driveshaft", "rotation", "brake-inspect", "front-diff-check", "rear-diff-check", "front-diff", "rear-diff"), services(15_000))
        assertEquals(
            setOf(
                "driveshaft", "cabin-air", "engine-air", "oil", "rotation", "brake-inspect",
                "front-diff-check", "rear-diff-check", "transfer-case-check", "front-diff", "rear-diff", "transfer-case",
            ),
            services(30_000),
        )
        assertEquals(setOf("coolant", "oil", "rotation", "brake-inspect"), services(100_000))
        assertTrue("spark-plugs" in services(120_000))
        assertTrue("trans-fluid" in services(60_000))
        // A job only for special operating conditions says so in its name, whatever service logs it.
        assertTrue(at(15_000).filter { it.service in setOf("front-diff", "rear-diff") }.all { " · towing" in it.name })
        assertEquals(
            setOf("oil", "rotation", "driveshaft", "cabin-air", "engine-air", "spark-plugs", "coolant", "front-diff-check", "rear-diff-check", "transfer-case-check"),
            list.intervals.filterNot { it.severeOnly }.mapNotNull { it.service }.toSet(),
        )
        // The guide's "additional items for special operating conditions" that are changes of oil: only for severe use.
        assertEquals(
            mapOf("front-diff" to Interval(15_000, 18), "rear-diff" to Interval(15_000, 18), "transfer-case" to Interval(30_000, 36), "trans-fluid" to Interval(60_000, 72)),
            list.intervals.filter { it.severeOnly }.associate { it.service to it.normal },
        )
        assertTrue(list.intervals.filter { it.severeOnly }.all { " · " in it.name && it.severe == null })
        // Under dust, salt or towing the propeller shaft is greased every 5,000.
        assertEquals(Interval(5_000, 6), list.intervals.single { it.service == "driveshaft" }.severe)
        assertEquals(Interval(15_000, 18), list.intervals.single { it.service == "front-diff-check" }.normal)
        assertEquals(Interval(30_000, 36), list.intervals.single { it.service == "transfer-case-check" }.normal)
        // The coolant is replaced first at 100,000 and then every 50,000.
        assertEquals(100_000 to Interval(50_000, 60), list.intervals.single { it.service == "coolant" }.let { it.firstAt to it.normal })
        assertEquals(Interval(10_000, 12) to Interval(5_000, 6), list.intervals.single { it.service == "oil" }.let { it.normal to it.severe })
        assertEquals("Toyota Warranty and Maintenance Guide", list.source.name)
    }

    @Test
    fun `a list is read as strictly as an import file`() {
        assertTrue(errors(file(extra = "\"notes\": \"x\",")).any { "notes" in it })
        assertTrue(errors(file(table = "[]")).isNotEmpty())
        assertTrue(errors(file(intervals = """[ { "name": "Change the oil", "everyMiles": 4000, "service": "no-such" } ]""")).any { "service" in it })
        assertTrue(errors(file().replace("\"basis\": \"Made up for the tests.\"", "\"basis\": \"\"")).any { "basis" in it })
    }

    @Test
    fun `an interval has to be what the table says`() {
        fun refused(intervals: String) = errors(file(intervals = intervals)).single()
        // Not at every multiple: the hinges are greased once, at 12,000.
        assertTrue("isn't every 4000" in refused("""[ { "name": "Grease the hinges", "everyMiles": 4000 } ]"""))
        assertTrue("isn't every 6000" in refused("""[ { "name": "Change the oil", "everyMiles": 6000, "service": "oil" } ]"""))
        assertTrue("isn't a job in the table" in refused("""[ { "name": "Flush the brakes", "everyMiles": 8000 } ]"""))
        assertTrue("months that aren't the table's" in refused("""[ { "name": "Change the oil", "everyMiles": 4000, "everyMonths": 6, "service": "oil" } ]"""))
        assertTrue("no distance and isn't marked time-only" in refused("""[ { "name": "Change the oil", "everyMonths": 4, "service": "oil" } ]"""))
        // By time alone, and said to be: the table's mileages for it aren't held against it.
        assertTrue(KnownSchedules.parse(file(intervals = """[ { "name": "Grease the hinges", "everyMonths": 24, "rule": "time-only" } ]""")) is KnownScheduleFile.Ok)
        // A first time of its own: in the table there and every interval after it, or it is refused.
        val hinges = ImportedScheduleItem("Grease the hinges", Interval(4_000, null), firstAt = 12_000)
        assertTrue("isn't 12000 and then every 4000" in KnownSchedules.contradictions(roadster.milestones, listOf(hinges)).single())
        assertEquals(emptyList<String>(), KnownSchedules.contradictions(roadster.milestones, listOf(hinges.copy(normal = Interval(8_000, 6)))))
        // Once, at its own interval, with the table ending before a second time: nothing contradicts it.
        assertEquals(emptyList<String>(), KnownSchedules.contradictions(roadster.milestones, listOf(ImportedScheduleItem("Grease the hinges", Interval(12_000, 12)))))
    }

    @Test
    fun `a list is offered only for its make, model and years`() {
        val lists = listOf(roadster)
        assertEquals("made-up", KnownSchedules.match(lists, 2003, "ACME", " roadster ")?.id)
        assertNull(KnownSchedules.match(lists, 2006, "Acme", "Roadster"))
        assertNull(KnownSchedules.match(lists, 2003, "Acme", "Roadster GT"))
    }

    @Test
    fun `reminders are added beside the user's own and never over them`() {
        val existing = listOf(
            saved(1, "change the OIL"),
            saved(2, "Oil & filter change", templateKey = "oil"),
            saved(3, "Tire rotation", templateKey = "rotation"),
            saved(4, "Cabin air filter", templateKey = "cabin-air"),
        )
        val plan = KnownSchedules.plan(roadster.intervals, existing)
        assertEquals(listOf("Swap the tires round"), plan.new.map { it.name })
        assertEquals(listOf("Change the oil"), plan.kept.map { it.name })
        // Only the typical item that doubles something being added: the oil isn't being added.
        assertEquals(listOf(3L), plan.doubles.map { it.id })
        assertEquals(emptyList<Long>(), KnownSchedules.plan(roadster.intervals, existing.map { it.copy(tracked = false) }).doubles.map { it.id })
    }

    @Test
    fun `a kilometre vehicle gets the figures as a manual for that market prints them`() {
        assertEquals(listOf(6_400, 12_800, 19_200, 25_600), KnownSchedules.milestonesIn(roadster, DistanceUnit.KILOMETERS, null).map { it.atMiles })
        assertEquals(Interval(6_400, 4), KnownSchedules.intervalsIn(roadster, DistanceUnit.KILOMETERS, null).first().normal)
        assertEquals(roadster.milestones, KnownSchedules.milestonesIn(roadster, DistanceUnit.MILES, null))
    }

    @Test
    fun `what is said names the guide and says to check it`() {
        val list = roadster
        assertEquals("Acme service booklet, Made-up Roadster", KnownSchedules.credit(list))
        val plan = KnownSchedules.plan(list.intervals, listOf(saved(1, "Change the oil")))
        assertEquals(1, plan.new.size)
        assertEquals(1, plan.kept.size)
    }

    @Test
    fun `a vehicle known to drive two wheels doesn't get the four-wheel-drive jobs`() {
        val list = KnownSchedules.match(bundled, 2022, "Toyota", "4Runner")!!
        fun jobs(drivetrain: String?) = KnownSchedules.milestonesIn(list, DistanceUnit.MILES, drivetrain).flatMap { m -> m.work.map { it.name } }.toSet()
        fun reminders(drivetrain: String?) = KnownSchedules.intervalsIn(list, DistanceUnit.MILES, drivetrain).map { it.name }
        // As vPIC decodes a 4Runner: "4WD/4-Wheel Drive/4x4" or "4x2". The pickers say 4WD, AWD, RWD, FWD.
        listOf("4WD/4-Wheel Drive/4x4", "4WD", "AWD", "AWD/All-Wheel Drive", null, "").forEach { drive ->
            assertTrue("$drive", "Inspect front differential oil (4WD)" in reminders(drive))
            assertTrue("$drive", "Inspect transfer case oil (4WD)" in reminders(drive))
            assertTrue("$drive", jobs(drive).any { it.startsWith("Replace transfer case oil (4WD)") })
        }
        listOf("4x2", "RWD", "RWD/Rear-Wheel Drive", "FWD").forEach { drive ->
            assertTrue("$drive", jobs(drive).none { "(4WD)" in it })
            assertTrue("$drive", reminders(drive).none { "(4WD)" in it })
            // What every 4Runner has stays: the rear differential, and all 24 milestones.
            assertTrue("$drive", "Inspect rear differential oil" in reminders(drive))
            assertTrue("$drive", jobs(drive).any { it.startsWith("Replace rear differential oil") })
            assertEquals(24, KnownSchedules.milestonesIn(list, DistanceUnit.MILES, drive).size)
        }
        assertEquals(14, reminders("4WD").size)
        assertEquals(10, reminders("4x2").size)
    }

    @Test
    fun `every job a list names for four-wheel drive is marked so, and the other way round`() {
        bundled.forEach { list ->
            val names = (list.milestones.flatMap { m -> m.work.map { it.name } } + list.intervals.map { it.name }).toSet()
            assertEquals(list.id, names.filter { "(4WD)" in it }.map(MilestoneImport::nameKey).toSet(), list.fourWheelDriveOnly)
        }
        assertTrue(errors(file(extra = "\"fourWheelDriveOnly\": [\"Rebuild the gearbox\"],")).single().contains("isn't a job in this list"))
    }

    @Test
    fun `a vehicle known to drive two wheels doesn't get the all-wheel-drive jobs`() {
        val table = someTable.replace("{ \"name\": \"Grease the hinges\" }", "{ \"name\": \"Change the coupling oil (AWD)\" }")
        val list = (KnownSchedules.parse(file(table, extra = "\"allWheelDriveOnly\": [\"Change the coupling oil (AWD)\"],")) as KnownScheduleFile.Ok).list
        assertEquals(setOf(MilestoneImport.nameKey("Change the coupling oil (AWD)")), list.allWheelDriveOnly)
        assertTrue(list.fourWheelDriveOnly.isEmpty())
        fun jobs(drivetrain: String?) = KnownSchedules.milestonesIn(list, DistanceUnit.MILES, drivetrain).flatMap { m -> m.work.map { it.name } }
        // Only a vehicle known to drive two wheels loses it; a four-wheel-drive one, or one with nothing on record, keeps it.
        listOf("AWD/All-Wheel Drive", "AWD", "4WD/4-Wheel Drive/4x4", null, "").forEach { drive ->
            assertTrue("$drive", "Change the coupling oil (AWD)" in jobs(drive))
        }
        listOf("4x2", "FWD/Front-Wheel Drive", "RWD").forEach { drive ->
            assertTrue("$drive", jobs(drive).none { "(AWD)" in it })
            assertEquals("$drive", 4, KnownSchedules.milestonesIn(list, DistanceUnit.MILES, drive).size)
        }
        assertTrue(errors(file(extra = "\"allWheelDriveOnly\": [\"Rebuild the gearbox\"],")).single().contains("isn't a job in this list"))
    }

    @Test
    fun `a vehicle known to shift itself doesn't get the manual gearbox's jobs, and a manual doesn't get the automatic's`() {
        val table = someTable
            .replace("{ \"name\": \"Grease the hinges\" }", "{ \"name\": \"Change the manual gearbox oil\" }")
            .replace("{ \"name\": \"Look at the belts\" }", "{ \"name\": \"Change the automatic's fluid\" }")
        val extra = "\"manualOnly\": [\"Change the manual gearbox oil\"], \"manualExcluded\": [\"Change the automatic's fluid\"],"
        val list = (KnownSchedules.parse(file(table, extra = extra)) as KnownScheduleFile.Ok).list
        fun jobs(transmission: String?) = KnownSchedules.milestonesIn(list, DistanceUnit.MILES, null, transmission).flatMap { m -> m.work.map { it.name } }
        // As vPIC writes a gearbox, and as an owner might.
        listOf("6-speed Automatic", "CVT", "7-speed Dual-Clutch Transmission (DCT)", "Automated Manual").forEach { t ->
            assertTrue(t, "Change the manual gearbox oil" !in jobs(t))
            assertTrue(t, "Change the automatic's fluid" in jobs(t))
        }
        listOf("6-speed Manual/Standard", "Manual").forEach { t ->
            assertTrue(t, "Change the manual gearbox oil" in jobs(t))
            assertTrue(t, "Change the automatic's fluid" !in jobs(t))
        }
        // Nothing on record, or something else: both kept, since leaving a job out is the worse mistake.
        listOf(null, "", "Other").forEach { t ->
            assertTrue("$t", "Change the manual gearbox oil" in jobs(t))
            assertTrue("$t", "Change the automatic's fluid" in jobs(t))
        }
        assertTrue(errors(file(extra = "\"manualOnly\": [\"Rebuild the gearbox\"],")).single().contains("isn't a job in this list"))
        assertTrue(errors(file(extra = "\"manualOnly\": [\"Change the oil\"], \"manualExcluded\": [\"Change the oil\"],")).single().contains("also in manualExcluded"))
    }

    @Test
    fun `a job the guide gives only as an interval, apart from its table, is a reminder and never on the table`() {
        val extra = """"additional": [
            { "name": "Replace the brake fluid", "everyMonths": 24, "rule": "time-only", "service": "brake-fluid" },
            { "name": "Change the coupling oil (AWD)", "everyMiles": 20000 }
        ], "allWheelDriveOnly": ["Change the coupling oil (AWD)"],"""
        val list = (KnownSchedules.parse(file(extra = extra)) as KnownScheduleFile.Ok).list
        assertEquals(2, list.additional.size)
        fun reminders(drive: String?) = KnownSchedules.intervalsIn(list, DistanceUnit.MILES, drive).map { it.name }
        assertEquals(listOf("Change the oil", "Swap the tires round", "Replace the brake fluid", "Change the coupling oil (AWD)"), reminders("AWD"))
        assertTrue("Change the coupling oil (AWD)" !in reminders("FWD"))
        // The table is the guide's, as printed: nothing is placed on it.
        val table = KnownSchedules.milestonesIn(list, DistanceUnit.MILES, "AWD").flatMap { m -> m.work.map { it.name } }
        assertTrue(table.none { it == "Replace the brake fluid" || it == "Change the coupling oil (AWD)" })
        // A job of the table can't be one apart from it as well, by its line or its name.
        assertTrue(errors(file(extra = """"additional": [ { "name": "Oil again", "everyMiles": 3000, "service": "oil" } ],""")).single().contains("is a job of the table"))
        assertTrue(errors(file(extra = """"additional": [ { "name": "Look at the belts", "everyMiles": 3000 } ],""")).single().contains("is a job of the table"))
    }

    @Test
    fun `a flex-fuel list's E85 intervals are held to its table, and only a flex-fuel list has them`() {
        val flex = """"engine": { "label": "2.0L flex-fuel", "displacementL": 2.0, "powertrain": "flex-fuel" },"""
        // The table is drawn for E85 use: the oil in every box. On other fuel the guide says every 8,000 / 8.
        val intervals = """[
            { "name": "Change the oil", "everyMiles": 8000, "everyMonths": 8, "service": "oil" },
            { "name": "Swap the tires round", "everyMiles": 8000, "everyMonths": 8, "service": "rotation" },
            { "name": "Grease the hinges · towing", "everyMiles": 12000, "severeOnly": true }
        ]"""
        val table = someTable.replace("""{ "name": "Grease the hinges" }""", """{ "name": "Grease the hinges · towing" }""")
        fun with(e85: String, engine: String = flex, items: String = intervals) = file(table = table, intervals = items, extra = """$engine "e85": $e85,""")
        val oil = """[ { "name": "Change the oil", "everyMiles": 4000, "everyMonths": 4 } ]"""

        val list = (KnownSchedules.parse(with(oil)) as KnownScheduleFile.Ok).list
        assertEquals(mapOf("change the oil" to Interval(4_000, 4)), list.e85)
        val reminders = KnownSchedules.intervalsIn(list, DistanceUnit.MILES, null).associateBy { it.name }
        assertEquals(Interval(8_000, 8), reminders.getValue("Change the oil").normal)
        assertEquals(Interval(4_000, 4), reminders.getValue("Change the oil").e85)
        assertNull(reminders.getValue("Swap the tires round").e85)
        // A schedule figure, like the others.
        assertEquals(Interval(6_400, 4), KnownSchedules.intervalsIn(list, DistanceUnit.KILOMETERS, null).first().e85)

        // Without the E85 interval, the oil every 8,000 isn't the table's.
        assertTrue(errors(file(table = table, intervals = intervals, extra = flex)).single().contains("isn't every 8000"))
        // Only a flex-fuel list.
        assertTrue(errors(with(oil, engine = "")).single().contains("only for a list whose engine is flex-fuel"))
        assertTrue(errors(with(oil, engine = flex.replace("flex-fuel\" }", "gas\" }"))).single().contains("only for a list whose engine is flex-fuel"))
        // A job among the intervals, once, never one only for severe use.
        assertTrue(errors(with("""[ { "name": "Look at the belts", "everyMiles": 4000 } ]""")).any { "isn't one of the list's intervals" in it })
        assertTrue(errors(with("""[ { "name": "Change the oil", "everyMiles": 4000, "everyMonths": 4 }, { "name": "change the OIL", "everyMiles": 4000, "everyMonths": 4 } ]""")).single().contains("already in e85"))
        assertTrue(errors(with("""[ { "name": "Grease the hinges · towing", "everyMiles": 12000 } ]""")).any { "only for severe use" in it })
        // Held to the table as any interval is.
        assertTrue(errors(with("""[ { "name": "Change the oil", "everyMiles": 8000, "everyMonths": 8 } ]""")).single().contains("isn't every 8000"))
        assertTrue(errors(with("""[ { "name": "Change the oil", "everyMiles": 4000, "everyMonths": 6 } ]""")).single().contains("months that aren't the table's"))
        assertTrue(errors(with("""[ { "name": "Change the oil", "everyMiles": 4000, "everyMonths": 4, "rule": "time-only" } ]""")).single().contains("rule"))
        // The ordinary interval still has to fall on the table, with its months.
        val odd = intervals.replace(""""everyMiles": 8000, "everyMonths": 8, "service": "oil"""", """"everyMiles": 6000, "everyMonths": 6, "service": "oil"""")
        assertTrue(errors(with(oil, items = odd)).single().contains("isn't in the table at every 6000"))
        val months = intervals.replace(""""everyMiles": 8000, "everyMonths": 8, "service": "oil"""", """"everyMiles": 8000, "everyMonths": 6, "service": "oil"""")
        assertTrue(errors(with(oil, items = months)).single().contains("months that aren't the table's"))
    }

    @Test
    fun `every bundled E85 interval is for a flex-fuel list's oil, and its oil otherwise follows the guide's normal schedule`() {
        val fuelled = bundled.filter { it.e85.isNotEmpty() }
        assertTrue(fuelled.isNotEmpty())
        fuelled.forEach { list ->
            assertEquals(list.id, Powertrain.FLEX_FUEL, list.engine?.powertrain)
            val oil = list.intervals.single { it.service == "oil" }
            assertEquals(list.id, Interval(2_500, 3), list.e85[MilestoneImport.nameKey(oil.name)])
            assertEquals(list.id, Interval(5_000, 6), oil.normal)
        }
        // Every flex-fuel list has it.
        assertEquals(bundled.filter { it.engine?.powertrain == Powertrain.FLEX_FUEL }.map { it.id }, fuelled.map { it.id })
    }

    @Test
    fun `every job a list names for all-wheel drive is marked so, and the other way round`() {
        bundled.forEach { list ->
            val names = (list.milestones.flatMap { m -> m.work.map { it.name } } + list.intervals.map { it.name }).toSet()
            assertEquals(list.id, names.filter { "(AWD)" in it }.map(MilestoneImport::nameKey).toSet(), list.allWheelDriveOnly)
        }
    }

    @Test
    fun `four-wheel drive and all-wheel drive are told apart`() {
        assertEquals(DriveKind.FOUR_WHEEL, Drivetrains.kind("4WD/4-Wheel Drive/4x4"))
        assertEquals(DriveKind.FOUR_WHEEL, Drivetrains.kind("2WD/4WD"))
        assertEquals(DriveKind.ALL_WHEEL, Drivetrains.kind("AWD/All-Wheel Drive"))
        assertEquals(DriveKind.ALL_WHEEL, Drivetrains.kind("AWD"))
        assertEquals(DriveKind.TWO_WHEEL, Drivetrains.kind("4x2"))
        assertEquals(DriveKind.TWO_WHEEL, Drivetrains.kind("FWD/Front-Wheel Drive"))
        assertEquals(DriveKind.TWO_WHEEL, Drivetrains.kind("RWD/Rear-Wheel Drive"))
        assertEquals(DriveKind.UNKNOWN, Drivetrains.kind(null))
        assertEquals(DriveKind.UNKNOWN, Drivetrains.kind("6x4"))
        assertEquals(DriveKind.UNKNOWN, Drivetrains.kind("Other"))
    }

    @Test
    fun `the GTI list is Volkswagen's card for the car`() {
        val list = KnownSchedules.match(bundled, 2011, "VOLKSWAGEN", "GTI")!!
        assertEquals(2010..2014, list.years)
        // 2015 is the Mk7, a list of its own.
        assertEquals("volkswagen-gti-2015", KnownSchedules.match(bundled, 2015, "Volkswagen", "GTI")?.id)
        assertNull(KnownSchedules.match(bundled, 2011, "Volkswagen", "Golf"))
        // One column per 10,000 miles to 120,000; the card prints no months for them.
        assertEquals((10_000..120_000 step 10_000).toList(), list.milestones.map { it.atMiles })
        assertTrue(list.milestones.all { it.orMonths == null })
        fun services(miles: Int) = list.milestones.single { it.atMiles == miles }.work.mapNotNull { it.service }.toSet()
        assertEquals(setOf("oil", "rotation", "brake-inspect"), services(10_000))
        assertEquals(setOf("oil", "rotation", "brake-inspect", "cabin-air"), services(20_000))
        assertEquals(setOf("oil", "rotation", "brake-inspect", "brake-fluid"), services(30_000))
        assertEquals(setOf("oil", "rotation", "brake-inspect", "cabin-air", "trans-fluid"), services(40_000))
        assertEquals(setOf("oil", "rotation", "brake-inspect", "cabin-air", "engine-air", "spark-plugs"), services(60_000))
        assertEquals(setOf("oil", "rotation", "brake-inspect", "cabin-air", "engine-air", "spark-plugs", "trans-fluid"), services(120_000))
        // The brake fluid is by time alone on the card: drawn at 30,000 and every 20,000 after.
        assertEquals(listOf(30_000, 50_000, 70_000, 90_000, 110_000), list.milestones.filter { m -> m.work.any { it.service == "brake-fluid" } }.map { it.atMiles })
        val by = list.intervals.associateBy { it.service }
        // The table prints no months, but the reissues' "or 1 year" gives the oil change its own (#517).
        assertEquals(Interval(10_000, 12), by.getValue("oil").normal)
        assertEquals(Interval(20_000, 24), by.getValue("cabin-air").normal)
        assertEquals(Interval(60_000, 72), by.getValue("spark-plugs").normal)
        assertEquals(Interval(60_000, 72), by.getValue("engine-air").normal)
        assertEquals(Interval(40_000, null), by.getValue("trans-fluid").normal)
        assertEquals(Interval(null, 24) to DueRule.TIME_ONLY, by.getValue("brake-fluid").let { it.normal to it.rule })
    }

    @Test
    fun `each Mk5 GTI year has a list of its own, from Volkswagen's reissue`() {
        (2006..2009).forEach { year ->
            val list = KnownSchedules.match(bundled, year, "Volkswagen", "GTI")!!
            assertEquals("volkswagen-gti-$year", list.id)
            assertEquals(year..year, list.years)
        }
        assertNull(KnownSchedules.match(bundled, 2005, "Volkswagen", "GTI"))
        fun list(year: Int) = KnownSchedules.match(bundled, year, "Volkswagen", "GTI")!!
        fun oil(year: Int) = list(year).intervals.single { it.service == "oil" }.normal
        // 2006–2008: the "every 5,000 miles" oil change is the GTI's (the owner's ruling, #145), so the table is in 5,000s.
        (2006..2008).forEach { year ->
            assertEquals(Interval(5_000, null), oil(year))
            assertTrue(list(year).milestones.all { m -> m.work.any { it.service == "oil" } })
            assertEquals(5_000, list(year).milestones.first().atMiles)
        }
        // 2009: every 10,000, each heading "or 1 year after last service": the table has no months, the oil change has 12 (#517).
        assertEquals(Interval(10_000, 12), oil(2009))
        assertTrue(list(2009).milestones.all { it.orMonths == null })
        // 2008's brake fluid rows and time table disagree (3 years, 2 years): no months, so no reminder.
        assertTrue(list(2008).milestones.any { m -> m.work.any { it.service == "brake-fluid" } })
        assertTrue((list(2008).intervals + list(2008).additional).none { it.service == "brake-fluid" })
        // 2009's timing belt is "(if applicable)": in the table, with no service and no reminder.
        val belt = list(2009).milestones.single { it.atMiles == 110_000 }.work.single { "timing belt" in it.name }
        assertNull(belt.service)
        assertTrue((list(2009).intervals + list(2009).additional).none { it.service == "timing-belt" })
        // The time-dependent items are reminders apart from the table, by time alone.
        assertEquals(Interval(null, 24) to DueRule.TIME_ONLY, list(2009).additional.single { it.service == "brake-fluid" }.let { it.normal to it.rule })
    }

    @Test
    fun `the 2017 to 2019 GTI lists are each their own year's card`() {
        fun list(year: Int) = KnownSchedules.match(bundled, year, "Volkswagen", "Golf GTI")!!
        assertEquals("volkswagen-gti-2016", list(2016).id)
        assertEquals("volkswagen-gti-2017", list(2017).id)
        assertEquals("volkswagen-gti-2018", list(2018).id)
        assertEquals("volkswagen-gti-2019", list(2019).id)
        assertEquals("volkswagen-gti-2020-2021", list(2020).id)
        fun additional(year: Int) = list(year).additional.associateBy { it.name }

        // 2017: the reissue's grid ends at 100,000 with no months, and gives the GTI no spark plug or DSG interval.
        assertEquals((10_000..100_000 step 10_000).toList(), list(2017).milestones.map { it.atMiles })
        assertTrue(list(2017).milestones.all { it.orMonths == null })
        assertEquals(setOf("engine-air", "brake-fluid", "cabin-air"), list(2017).additional.mapNotNull { it.service }.toSet())
        assertTrue(list(2017).manualExcluded.isEmpty())

        // 2018: spark plugs for the 2.0L every 40,000 or 4 years; the 6-speed DSG every 40,000.
        val y18 = list(2018).additional.associateBy { it.service }
        assertEquals(Interval(40_000, 48), y18.getValue("spark-plugs").normal)
        assertEquals(Interval(40_000, null), y18.getValue("trans-fluid").normal)
        // 2019, newest revision: the GTI is named on the 80,000-mile plug and 7-speed DSG rows.
        val y19 = list(2019).additional.associateBy { it.service }
        assertEquals(Interval(80_000, 72), y19.getValue("spark-plugs").normal)
        assertEquals(Interval(80_000, null), y19.getValue("trans-fluid").normal)

        for (year in 2018..2019) {
            assertEquals((10_000..120_000 step 10_000).toList(), list(year).milestones.map { it.atMiles })
            assertEquals((1..12).map { it * 12 }, list(year).milestones.map { it.orMonths })
            // The panoramic sunroof: a function check in the grid, and its own service apart from it.
            assertEquals(listOf(40_000, 80_000, 120_000), list(year).milestones.filter { m -> m.work.any { it.name == "Check panoramic sunroof function (if fitted)" } }.map { it.atMiles })
            val sunroof = additional(year).getValue("Service panoramic sunroof rails (if fitted)")
            assertEquals(Interval(40_000, 24) to 40_000, sunroof.normal to sunroof.firstAt)
            assertEquals(null, sunroof.service)
        }
    }

    @Test
    fun `a car known to have a manual gearbox doesn't get the dual-clutch fluid change`() {
        val list = KnownSchedules.match(bundled, 2011, "Volkswagen", "GTI")!!
        fun dsg(transmission: String?) =
            KnownSchedules.intervalsIn(list, DistanceUnit.MILES, "FWD/Front-Wheel Drive", transmission).count { it.service == "trans-fluid" } to
                KnownSchedules.milestonesIn(list, DistanceUnit.MILES, "FWD/Front-Wheel Drive", transmission).count { m -> m.work.any { it.service == "trans-fluid" } }
        // As vPIC decodes a GTI: "Automatic" for the DSG, "Manual/Standard" for the six-speed.
        assertEquals(1 to 3, dsg("Automatic"))
        assertEquals(1 to 3, dsg("Dual-Clutch Transmission (DCT)"))
        assertEquals(1 to 3, dsg(null))
        assertEquals(0 to 0, dsg("Manual/Standard"))
        assertEquals(0 to 0, dsg("6-speed manual"))
        assertEquals(false, Transmissions.isManual("Automated Manual Transmission (AMT)"))
    }
}
