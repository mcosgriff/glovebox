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
        assertEquals(setOf("driveshaft", "rotation", "brake-inspect"), services(15_000))
        assertEquals(setOf("driveshaft", "cabin-air", "engine-air", "oil", "rotation", "brake-inspect"), services(30_000))
        assertEquals(setOf("coolant", "oil", "rotation", "brake-inspect"), services(100_000))
        assertTrue("spark-plugs" in services(120_000))
        // The coolant is replaced at 100,000 and then every 50,000, which no interval can say; it stays in the table.
        assertEquals(setOf("oil", "rotation", "driveshaft", "cabin-air", "engine-air", "spark-plugs"), list.intervals.mapNotNull { it.service }.toSet())
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
        assertEquals(listOf(6_400, 12_800, 19_200, 25_600), KnownSchedules.milestonesIn(roadster, DistanceUnit.KILOMETERS).map { it.atMiles })
        assertEquals(Interval(6_400, 4), KnownSchedules.intervalsIn(roadster, DistanceUnit.KILOMETERS).first().normal)
        assertEquals(roadster.milestones, KnownSchedules.milestonesIn(roadster, DistanceUnit.MILES))
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
}
