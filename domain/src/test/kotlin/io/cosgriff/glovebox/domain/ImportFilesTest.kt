package io.cosgriff.glovebox.domain

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImportFilesTest {
    private fun item(
        id: Long,
        name: String,
        normal: Interval? = Interval(5_000, 6),
        severe: Interval? = null,
        rule: DueRule? = null,
        templateKey: String? = null,
        tracked: Boolean = true,
        service: String? = null,
    ) = SavedScheduleItem(id, name, normal, severe, rule, templateKey, tracked, service)

    private fun row(
        id: Long,
        name: String,
        category: TorqueCategory = TorqueCategory.ENGINE,
        socket: String? = null,
        torque: Torque? = null,
        source: TorqueSource = TorqueSource.USER,
        note: String? = null,
    ) = SavedTorqueSpec(id, name, category, socket, torque, source, note)

    private fun scheduleFile(items: List<SavedScheduleItem>) = ScheduleExport.file(items, DistanceUnit.MILES)

    private fun schemaAccepts(kind: ImportKind, text: String): Boolean {
        val schema = File("../docs/import/schema/${kind.id}.schema.json").readText()
        assertTrue(ImportFiles.schema(kind) in schema)
        return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema).validate(text, InputFormat.JSON).isEmpty()
    }

    private val schedule = listOf(
        item(1, "Engine oil & filter", Interval(7_500, 12), Interval(5_000, 6), DueRule.WHICHEVER_FIRST, templateKey = "oil"),
        item(2, "Tire rotation", Interval(5_000, null), templateKey = "rotation", tracked = false),
        item(3, "Grease driveshaft", Interval(15_000, null), rule = DueRule.MILEAGE_ONLY, service = "driveshaft"),
        item(4, "Wash \"the\" underside \\ rinse", Interval(null, 3)),
    )

    @Test
    fun `a schedule goes out as its tracked items and is read back the same`() {
        val written = scheduleFile(schedule)
        assertEquals(3, written.count)
        assertEquals(emptyList<LeftOut>(), written.leftOut)
        assertTrue(schemaAccepts(ImportKind.INTERVAL_SCHEDULE, written.text!!))
        assertEquals(
            listOf(
                // A typical item carries the line that resets it, so it is reset by the same tick where it lands.
                ImportedScheduleItem("Engine oil & filter", Interval(7_500, 12), Interval(5_000, 6), DueRule.WHICHEVER_FIRST, "oil"),
                ImportedScheduleItem("Grease driveshaft", Interval(15_000, null), rule = DueRule.MILEAGE_ONLY, service = "driveshaft"),
                ImportedScheduleItem("Wash \"the\" underside \\ rinse", Interval(null, 3)),
            ),
            (ImportFormat.parse(written.text!!) as ImportFile.IntervalSchedule).items,
        )
    }

    @Test
    fun `read back into the vehicle it came from, the user's own items are the same and the typical ones are doubles`() {
        val items = (ImportFormat.parse(scheduleFile(schedule).text!!) as ImportFile.IntervalSchedule).items
        val plan = ScheduleImport.plan(items, schedule)
        assertEquals(listOf(3L, 4L), plan.same.map { it.second.id })
        assertEquals(listOf("Engine oil & filter"), plan.new.map { it.name })
        assertEquals(listOf(1L), ScheduleImport.doubles(items, schedule).map { it.id })
    }

    @Test
    fun `what a schedule file can't hold is left out and said, and the rest still goes`() {
        val written = scheduleFile(
            listOf(
                item(1, "Oil change"),
                item(2, "Timing belt", Interval(600_000, null)),
                item(3, "x".repeat(61)),
                item(4, "OIL change", Interval(3_000, null)),
                item(5, "No interval", normal = null),
                item(6, "Coolant", Interval(60_000, 601)),
            ),
        )
        assertEquals(1, written.count)
        assertEquals(
            listOf(
                LeftOut("Timing belt", "everyMiles must be from 1 to 500000"),
                LeftOut("x".repeat(61), "name is 61 characters; the most allowed is 60"),
                LeftOut("OIL change", "another item in the schedule has the same name"),
                LeftOut("No interval", "needs \"everyMiles\", \"everyMonths\" or both"),
                LeftOut("Coolant", "everyMonths must be from 1 to 600"),
            ),
            written.leftOut,
        )
        assertEquals(listOf("Oil change"), (ImportFormat.parse(written.text!!) as ImportFile.IntervalSchedule).items.map { it.name })
    }

    @Test
    fun `with nothing that can be written there is no file`() {
        assertNull(scheduleFile(emptyList()).text)
        assertNull(scheduleFile(listOf(item(1, "Oil change", tracked = false))).text)
        val none = scheduleFile(listOf(item(1, "Timing belt", Interval(600_000, null))))
        assertNull(none.text)
        assertEquals(1, none.leftOut.size)
        assertNull(TorqueExport.file(emptyList(), TorqueUnit.FOOT_POUND).text)
    }

    @Test
    fun `no more goes in a file than one may hold`() {
        val written = scheduleFile((1..ScheduleImport.MAX_ITEMS + 2).map { item(it.toLong(), "Job $it") })
        assertEquals(ScheduleImport.MAX_ITEMS, written.count)
        assertEquals(listOf(LeftOut("Job 201", "a file holds 200 at most"), LeftOut("Job 202", "a file holds 200 at most")), written.leftOut)
        assertTrue(ImportFormat.parse(written.text!!) is ImportFile.IntervalSchedule)
    }

    private val specs = listOf(
        row(1, "Lug nuts", TorqueCategory.WHEELS, "21 mm", Torque.of(100.0, TorqueUnit.FOOT_POUND), TorqueSource.FACTORY, "Star pattern, re-check after 50 mi"),
        row(2, "Oil drain plug", socket = "14 mm", torque = Torque(40.0)),
        row(3, "Valve cover bolts", torque = Torque.of(90.0, TorqueUnit.INCH_POUND)),
        row(4, "Skid plate bolts", TorqueCategory.OTHER, "12 mm"),
        row(5, "Axle nut", TorqueCategory.DRIVELINE, torque = Torque(1_000.0)),
    )

    @Test
    fun `torque specs go out in the user's unit and come back as the same settings`() {
        for (unit in TorqueUnit.entries) {
            val written = TorqueExport.file(specs, unit)
            assertEquals(emptyList<LeftOut>(), written.leftOut)
            assertTrue(schemaAccepts(ImportKind.TORQUE_SPECS, written.text!!))
            val items = (ImportFormat.parse(written.text!!) as ImportFile.TorqueSpecs).items
            assertEquals(specs.map { it.name }, items.map { it.name })
            assertEquals(TorqueSource.FACTORY, items[0].source)
            assertEquals("Star pattern, re-check after 50 mi", items[0].note)
            assertNull(items[3].torque)
            // Read back over the rows it was written from, nothing is new and nothing differs.
            val plan = TorqueImport.plan(items, specs)
            assertEquals("in ${unit.symbol}", 5, plan.same.size)
        }
    }

    @Test
    fun `a value is written as a wrench would be set, and in newton metres when the unit's cap is in the way`() {
        val text = TorqueExport.file(specs, TorqueUnit.FOOT_POUND).text!!
        assertTrue("\"value\": 100,\n" in text)
        assertTrue("\"value\": 29.5,\n" in text)
        // 1,000 N·m is 737.56 ft-lb, over the 737 a file may give in that unit.
        assertTrue("\"value\": 1000,\n" in text)
        assertTrue("\"unit\": \"Nm\"" in text)
        // A row that is only a name says nothing about where a value came from.
        assertFalse(Regex(""""Skid plate bolts",\s+"category": "other",\s+"socket": "12 mm",\s+"source"""").containsMatchIn(text))
    }

    @Test
    fun `two rows for one fastener can't both go in a file`() {
        val written = TorqueExport.file(listOf(row(1, "Drain plug", torque = Torque(40.0)), row(2, "DRAIN plug"), row(3, "Drain plug", TorqueCategory.DRIVELINE)), TorqueUnit.NEWTON_METER)
        assertEquals(2, written.count)
        assertEquals(listOf(LeftOut("DRAIN plug", "another row in the same category has the same name")), written.leftOut)
        assertTrue(ImportFormat.parse(written.text!!) is ImportFile.TorqueSpecs)
    }

    @Test
    fun `the published examples survive being imported and written out again`() {
        val docs = File("../docs/import/examples")
        val schedule = (ImportFormat.parse(File(docs, "interval-schedule.json").readText()) as ImportFile.IntervalSchedule).items
        val saved = schedule.mapIndexed { i, it -> SavedScheduleItem(i.toLong(), it.name, it.normal, it.severe, it.rule, null, true, it.service) }
        assertEquals(schedule, (ImportFormat.parse(scheduleFile(saved).text!!) as ImportFile.IntervalSchedule).items)

        val torque = (ImportFormat.parse(File(docs, "torque-specs.json").readText()) as ImportFile.TorqueSpecs).items
        val rows = torque.mapIndexed { i, it -> SavedTorqueSpec(i.toLong(), it.name, it.category, it.socket, it.torque, it.source, it.note) }
        val back = (ImportFormat.parse(TorqueExport.file(rows, TorqueUnit.FOOT_POUND).text!!) as ImportFile.TorqueSpecs).items
        assertEquals(4, TorqueImport.plan(back, rows).same.size)
    }

    @Test
    fun `the suggested names say what the file is and which model it is for`() {
        assertEquals("glovebox-schedule-2003-honda-accord.json", ScheduleExport.fileName("2003 Honda Accord"))
        assertEquals("glovebox-torque-specs-2003-honda-accord.json", TorqueExport.fileName("2003 Honda Accord"))
        assertEquals("glovebox-milestones-2003-honda-accord.json", MilestoneExport.fileName("2003 Honda Accord"))
    }

    private val km = DistanceUnit.KILOMETERS

    @Test
    fun `a file's schedule figures come into a kilometre vehicle as a manual gives them, and go back out the same`() {
        val file = listOf(ImportedScheduleItem("Oil change", Interval(7_500, 12), Interval(5_000, 6)), ImportedScheduleItem("Brake fluid", Interval(null, 36)))
        val into = FileDistances.schedule(file, km)
        assertEquals(listOf(Interval(12_000, 12), Interval(null, 36)), into.map { it.normal })
        assertEquals(Interval(8_000, 6), into[0].severe)
        assertEquals(file, FileDistances.schedule(file, DistanceUnit.MILES))

        val saved = into.mapIndexed { i, it -> SavedScheduleItem(i.toLong(), it.name, it.normal, it.severe, it.rule, null, true, it.service) }
        val back = (ImportFormat.parse(ScheduleExport.file(saved, km).text!!) as ImportFile.IntervalSchedule).items
        assertEquals(file, back)
    }

    @Test
    fun `a milestone table comes into a kilometre vehicle with its cycle, and goes back out the same`() {
        val file = listOf(ImportedMilestone(30_000, listOf(TableJob("B"))), ImportedMilestone(10_000, listOf(TableJob("A"))))
        val (table, repeats) = FileDistances.milestones(file, 120_000, km)
        assertEquals(listOf(16_000, 48_000), table.map { it.atMiles })
        assertEquals(192_000, repeats)

        val saved = table.mapIndexed { i, m -> SavedMilestone(i.toLong(), m.atMiles, m.work, m.orMonths) }
        val back = ImportFormat.parse(MilestoneExport.file(saved, repeats, km)) as ImportFile.MilestoneSchedule
        assertEquals(listOf(10_000, 30_000), back.items.map { it.atMiles })
        assertEquals(120_000, back.repeatsEveryMiles)
    }

    @Test
    fun `a history file's readings come into a kilometre vehicle converted exactly`() {
        val record = ImportedRecord(java.time.LocalDate.of(2024, 3, 1), 41_000, listOf(ImportedWork("Oil change")), diy = true)
        assertEquals(listOf(65_983), FileDistances.history(listOf(record), km).map { it.odometer })
        assertEquals(listOf(record), FileDistances.history(listOf(record), DistanceUnit.MILES))
    }

    @Test
    fun `a kilometre figure with no whole number of miles still makes a file the app accepts`() {
        // 7,500 km is 4,687.5 mi: the file says 4,688, which reads back as 7,501 km. Off by one, never refused.
        val written = ScheduleExport.file(listOf(item(1, "Oil change", Interval(7_500, 6))), km)
        val back = (ImportFormat.parse(written.text!!) as ImportFile.IntervalSchedule).items.single()
        assertEquals(4_688, back.normal.miles)
        assertEquals(7_501, FileDistances.schedule(listOf(back), km).single().normal.miles)
    }
}
