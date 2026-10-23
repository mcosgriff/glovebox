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

    private fun file(table: String = someTable, intervals: String = someIntervals, extra: String = "") = """
        { "id": "made-up", "title": "Made-up Roadster", "make": "Acme", "model": "Roadster", "years": { "from": 2001, "to": 2005 },
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
            list.years.forEach { year -> assertEquals(list.id, KnownSchedules.match(bundled, year, list.make, list.model, engine, list.engine?.fuel, list.engine?.powertrain)?.id) }
        }
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
        assertNull(KnownSchedules.match(bundled, 2016, "Toyota", "4Runner"))
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
        assertNull(KnownSchedules.match(bundled, 2015, "Volkswagen", "GTI"))
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
        assertEquals(Interval(10_000, null), by.getValue("oil").normal)
        assertEquals(Interval(20_000, 24), by.getValue("cabin-air").normal)
        assertEquals(Interval(60_000, 72), by.getValue("spark-plugs").normal)
        assertEquals(Interval(60_000, 72), by.getValue("engine-air").normal)
        assertEquals(Interval(40_000, null), by.getValue("trans-fluid").normal)
        assertEquals(Interval(null, 24) to DueRule.TIME_ONLY, by.getValue("brake-fluid").let { it.normal to it.rule })
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
