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
            list.years.forEach { year -> assertEquals(list.id, KnownSchedules.match(bundled, year, list.make, list.model)?.id) }
        }
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
            list.intervals.mapNotNull { it.service }.toSet(),
        )
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
        assertTrue("no distance" in refused("""[ { "name": "Change the oil", "everyMonths": 4, "service": "oil" } ]"""))
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
        assertTrue(KnownSchedules.tableOffer(list).startsWith("4 milestones, as the Acme service booklet lists them."))
        assertTrue("doesn't create or change a reminder" in KnownSchedules.tableOffer(list))
        val plan = KnownSchedules.plan(list.intervals, listOf(saved(1, "Change the oil")))
        assertTrue(KnownSchedules.intervalOffer(list, plan).startsWith("1 reminder from the Acme service booklet"))
        assertTrue("1 item of your own with the same name is left as it is." in KnownSchedules.intervalOffer(list, plan))
        assertTrue("Check it against the guide for your own vehicle." in KnownSchedules.startedFrom(KnownSchedules.credit(list)))
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
        assertEquals(10, reminders("4WD").size)
        assertEquals(8, reminders("4x2").size)
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
}
