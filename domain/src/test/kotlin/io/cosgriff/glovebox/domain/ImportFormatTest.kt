package io.cosgriff.glovebox.domain

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SchemaRegistryConfig
import com.networknt.schema.SpecificationVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class ImportFormatTest {
    // Gradle runs a module's tests from the module's folder.
    private val docs = File("../docs/import")
    private val kinds = ImportFormat.supported.map { it.id }
    // Formats are checked, as check-jsonschema does by default: "2024-02-30" has the shape of a date and isn't one.
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemaRegistryConfig(SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
    }
    private val schemas = kinds.associateWith { registry.getSchema(File(docs, "schema/$it.schema.json").readText()) }

    private fun schemaAccepts(kind: String, file: File): Boolean = schemas.getValue(kind).validate(file.readText(), InputFormat.JSON).isEmpty()
    private fun examples(folder: String): List<File> = File(docs, "examples/$folder").listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
    private fun schedule(items: String) = ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "interval-schedule", "items": $items }""")

    private fun torque(items: String) = ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "torque-specs", "items": $items }""")
    private fun errors(file: ImportFile): List<String> = (file as ImportFile.Invalid).errors.map { it.toString() }

    @Test
    fun `the published example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/torque-specs.json")
        assertTrue(schemaAccepts("torque-specs", file))
        val parsed = ImportFormat.parse(file.readText()) as ImportFile.TorqueSpecs
        assertEquals(listOf("Lug nuts", "Oil drain plug", "Valve cover bolts", "Skid plate bolts"), parsed.items.map { it.name })
        assertEquals(TorqueSource.FACTORY, parsed.items[0].source)
        assertEquals(135.58, parsed.items[0].torque!!.newtonMeters, 0.01)
        assertEquals(40.0, parsed.items[1].torque!!.newtonMeters, 0.0)
        assertEquals(null, parsed.items[3].torque)
    }

    @Test
    fun `every invalid example is refused by both the schema and the app`() {
        for (kind in kinds) {
            val files = examples("invalid/$kind")
            assertTrue("the invalid examples for $kind are missing", files.size > 15)
            for (file in files) {
                assertFalse("the schema accepts $kind/${file.name}", schemaAccepts(kind, file))
                assertTrue("the app accepts $kind/${file.name}", ImportFormat.parse(file.readText()) is ImportFile.Invalid)
            }
        }
    }

    @Test
    fun `rules the schema can't state are still enforced by the app`() {
        for (kind in kinds) {
            val files = examples("beyond-schema/$kind")
            assertTrue(files.isNotEmpty())
            for (file in files) {
                assertTrue("$kind/${file.name} belongs in invalid", schemaAccepts(kind, file))
                assertTrue("the app accepts $kind/${file.name}", ImportFormat.parse(file.readText()) is ImportFile.Invalid)
            }
        }
    }

    @Test
    fun `errors say where and what`() {
        val file = torque("""[ { "name": "Lug nuts", "category": "Wheels", "torqe": { "value": 100, "unit": "ft-lb" } } ]""")
        assertEquals(
            listOf(
                "items[0].torqe: isn't a field Glovebox knows; remove it or check the spelling",
                "items[0].category: must be one of \"engine\", \"wheels\", \"driveline\", \"suspension\", \"other\"",
            ),
            errors(file),
        )
    }

    @Test
    fun `every problem is reported, up to a cap`() {
        val bad = (1..40).joinToString(",") { """{ "name": "Bolt $it" }""" }
        val file = torque("[$bad]") as ImportFile.Invalid
        assertEquals(ImportFormat.MAX_ERRORS, file.errors.size)
        assertEquals(40 - ImportFormat.MAX_ERRORS, file.more)
        assertEquals("items[0].category: is required", file.errors.first().toString())
    }

    @Test
    fun `text that isn't JSON is refused in plain words`() {
        assertEquals(1, errors(ImportFormat.parse("name,category\nLug nuts,wheels")).size)
        assertTrue(ImportFormat.parse("") is ImportFile.Invalid)
    }

    @Test
    fun `a file for a newer version is refused as such`() {
        val file = ImportFormat.parse("""{ "glovebox": "import", "version": 7, "kind": "something-new", "items": [], "extra": true }""")
        assertEquals(listOf("version: this file is for a newer version of Glovebox (format 7; this app reads 1)"), errors(file).takeLast(1))
    }

    @Test
    fun `every kind has a schema, a valid example and both folders of bad ones`() {
        assertEquals(ImportKind.entries.map { it.id }, kinds)
        for (kind in kinds) {
            assertTrue(File(docs, "schema/$kind.schema.json").isFile)
            assertTrue(File(docs, "examples/$kind.json").isFile)
        }
    }

    @Test
    fun `torque is capped per unit exactly as the schema says`() {
        assertTrue(torque("""[ { "name": "A", "category": "other", "torque": { "value": 737, "unit": "ft-lb" } } ]""") is ImportFile.TorqueSpecs)
        assertTrue(torque("""[ { "name": "A", "category": "other", "torque": { "value": 737.5, "unit": "ft-lb" } } ]""") is ImportFile.Invalid)
        assertTrue(torque("""[ { "name": "A", "category": "other", "torque": { "value": 1000, "unit": "Nm" } } ]""") is ImportFile.TorqueSpecs)
        assertTrue(torque("""[ { "name": "A", "category": "other", "torque": { "value": 8850.5, "unit": "in-lb" } } ]""") is ImportFile.Invalid)
        // The stated caps never let through more than the app stores.
        TorqueUnit.entries.forEach { assertTrue(Torque.of(TorqueImport.maxIn(it).toDouble(), it).newtonMeters <= TorqueSpecs.MAX_NEWTON_METERS) }
    }

    @Test
    fun `the same fastener twice in a file is refused, ignoring case, but not across categories`() {
        val twice = torque("""[ { "name": "Drain plug", "category": "engine" }, { "name": "DRAIN plug", "category": "engine" } ]""")
        assertEquals(listOf("items[1].name: \"DRAIN plug\" is already in this file under the same category, at items[0]"), errors(twice))
        assertTrue(torque("""[ { "name": "Drain plug", "category": "engine" }, { "name": "Drain plug", "category": "driveline" } ]""") is ImportFile.TorqueSpecs)
    }

    @Test
    fun `the schema's lists match the app's`() {
        var schema = Json.parseToJsonElement(File(docs, "schema/torque-specs.schema.json").readText())
        fun at(vararg path: String): JsonElement = path.fold(schema) { e, key -> e.jsonObject.getValue(key) }
        fun enum(vararg path: String) = at(*path).jsonArray.map { it.jsonPrimitive.content }.toSet()
        fun int(vararg path: String) = at(*path).jsonPrimitive.content.toInt()
        assertEquals(TorqueImport.categories.keys, enum("\$defs", "spec", "properties", "category", "enum"))
        assertEquals(TorqueImport.sources.keys, enum("\$defs", "spec", "properties", "source", "enum"))
        assertEquals(TorqueImport.units.keys, enum("\$defs", "torque", "properties", "unit", "enum"))
        assertEquals(VehicleText.MAX_SPEC, int("\$defs", "spec", "properties", "name", "maxLength"))
        assertEquals(TorqueImport.MAX_SOCKET, int("\$defs", "spec", "properties", "socket", "maxLength"))
        assertEquals(TorqueImport.MAX_NOTE, int("\$defs", "spec", "properties", "note", "maxLength"))
        assertEquals(TorqueImport.MAX_ITEMS, int("properties", "items", "maxItems"))

        schema = Json.parseToJsonElement(File(docs, "schema/interval-schedule.schema.json").readText())
        assertEquals(ScheduleImport.rules.keys, enum("\$defs", "item", "properties", "rule", "enum"))
        assertEquals(ScheduleImport.MAX_NAME, int("\$defs", "item", "properties", "name", "maxLength"))
        assertEquals(ScheduleImport.MAX_MILES, int("\$defs", "miles", "maximum"))
        assertEquals(ScheduleImport.MAX_MONTHS, int("\$defs", "months", "maximum"))
        assertEquals(ScheduleImport.MAX_ITEMS, int("properties", "items", "maxItems"))

        schema = Json.parseToJsonElement(File(docs, "schema/service-history.schema.json").readText())
        // Every line the Log service screen offers, and nothing else.
        assertEquals(HistoryImport.services.keys, enum("\$defs", "work", "properties", "service", "enum"))
        assertEquals(HistoryImport.MAX_LABEL, int("\$defs", "work", "properties", "label", "maxLength"))
        assertEquals(HistoryImport.MAX_SHOP, int("\$defs", "doneBy", "properties", "name", "maxLength"))
        assertEquals(HistoryImport.MAX_NOTES, int("\$defs", "record", "properties", "notes", "maxLength"))
        assertEquals(HistoryImport.MAX_ODOMETER, int("\$defs", "record", "properties", "odometer", "maximum"))
        assertEquals(HistoryImport.MAX_WORK, int("\$defs", "record", "properties", "work", "maxItems"))
        assertEquals(HistoryImport.MAX_ITEMS, int("properties", "items", "maxItems"))
    }

    private fun history(items: String) =
        ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "service-history", "items": $items }""", LocalDate.of(2026, 10, 12))

    @Test
    fun `the history example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/service-history.json")
        assertTrue(schemaAccepts("service-history", file))
        val items = (ImportFormat.parse(file.readText()) as ImportFile.ServiceHistory).items
        assertEquals(3, items.size)
        assertEquals(
            ImportedRecord(
                date = LocalDate.of(2024, 3, 1),
                odometer = 41_000,
                work = listOf(ImportedWork("Oil & filter change", "oil"), ImportedWork("Tire rotation", "rotation"), ImportedWork("Replaced left tail lamp")),
                diy = false,
                shopName = "Corner Garage",
                costCents = 6_250,
                notes = "0W-20, 6.2 qt\nNew drain plug gasket",
            ),
            items[0],
        )
        assertEquals(41_000L, items[1].costCents)
        assertEquals(null, items[1].shopName)
        assertTrue(items[2].diy)
    }

    @Test
    fun `a record is of work already done, on a real date`() {
        val line = """"odometer": 41000, "work": [ { "label": "Oil change" } ], "doneBy": { "type": "diy" }"""
        assertEquals(listOf("items[0].date: is in the future; a record is of work already done"), errors(history("""[ { "date": "2026-10-13", $line } ]""")))
        assertTrue(history("""[ { "date": "2026-10-12", $line } ]""") is ImportFile.ServiceHistory)
        assertEquals(listOf("items[0].date: must be a real date written as \"YYYY-MM-DD\""), errors(history("""[ { "date": "2024-02-30", $line } ]""")))
        assertEquals(listOf("items[0].date: must be a real date written as \"YYYY-MM-DD\""), errors(history("""[ { "date": "3/1/2024", $line } ]""")))
    }

    @Test
    fun `one visit is one record`() {
        val record = """{ "date": "2024-03-01", "odometer": 41000, "work": [ { "label": "Oil change" } ], "doneBy": { "type": "diy" } }"""
        assertEquals(
            listOf("items[1]: has the same date and odometer as items[0]; put the work of one visit in one record"),
            errors(history("[ $record, $record ]")),
        )
    }

    @Test
    fun `cost is dollars as text and only a shop has a name`() {
        fun one(extra: String) = history("""[ { "date": "2024-03-01", "odometer": 41000, "work": [ { "label": "Oil change" } ], $extra } ]""")
        assertEquals(125_000L, (one(""""doneBy": { "type": "diy" }, "cost": "1250"""") as ImportFile.ServiceHistory).items.single().costCents)
        assertEquals(650L, (one(""""doneBy": { "type": "diy" }, "cost": "6.5"""") as ImportFile.ServiceHistory).items.single().costCents)
        assertTrue(one(""""doneBy": { "type": "diy" }, "cost": 62.5""") is ImportFile.Invalid)
        assertEquals(
            listOf("items[0].doneBy.name: only a shop has a name; leave it out for \"diy\""),
            errors(one(""""doneBy": { "type": "diy", "name": "Me" }""")),
        )
    }

    @Test
    fun `the schedule example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/interval-schedule.json")
        assertTrue(schemaAccepts("interval-schedule", file))
        val items = (ImportFormat.parse(file.readText()) as ImportFile.IntervalSchedule).items
        assertEquals(
            listOf(
                ImportedScheduleItem("Oil change", Interval(7_500, 12), Interval(5_000, 6), DueRule.WHICHEVER_FIRST),
                ImportedScheduleItem("Rotate tires", Interval(5_000, null)),
                ImportedScheduleItem("Brake fluid flush", Interval(null, 36)),
                ImportedScheduleItem("Grease driveshaft", Interval(15_000, null), Interval(5_000, null), DueRule.MILEAGE_ONLY),
            ),
            items,
        )
    }

    @Test
    fun `a schedule item needs an interval, in whole miles or months`() {
        assertEquals(listOf("items[0]: needs \"everyMiles\", \"everyMonths\" or both"), errors(schedule("""[ { "name": "Oil change" } ]""")))
        assertEquals(listOf("items[0].everyMiles: must be a whole number, without quotes"), errors(schedule("""[ { "name": "Oil change", "everyMiles": 7500.5 } ]""")))
        assertEquals(listOf("items[0].everyMonths: must be from 1 to 600"), errors(schedule("""[ { "name": "Oil change", "everyMonths": 0 } ]""")))
        assertEquals(listOf("items[0].severe: needs \"everyMiles\", \"everyMonths\" or both"), errors(schedule("""[ { "name": "Oil change", "everyMiles": 7500, "severe": {} } ]""")))
        // A whole number written with a decimal point is still a whole number, to the schema as well.
        assertTrue(schedule("""[ { "name": "Oil change", "everyMiles": 7500.0 } ]""") is ImportFile.IntervalSchedule)
    }

    @Test
    fun `the same job twice in a schedule file is refused`() {
        val twice = schedule("""[ { "name": "Oil change", "everyMiles": 7500 }, { "name": "oil CHANGE", "everyMonths": 12 } ]""")
        assertEquals(listOf("items[1].name: \"oil CHANGE\" is already in this file, at items[0]"), errors(twice))
    }

    private fun milestones(items: String) = ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "milestone-schedule", "items": $items }""")

    @Test
    fun `the milestone example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/milestone-schedule.json")
        assertTrue(schemaAccepts("milestone-schedule", file))
        val items = (ImportFormat.parse(file.readText()) as ImportFile.MilestoneSchedule).items
        assertEquals(listOf(10_000, 20_000, 30_000, 65_000, 100_000), items.map { it.atMiles })
        assertEquals(ImportedMilestone(65_000, listOf(TableJob("Replace spark plugs", "spark-plugs"))), items[3])
        assertEquals(36, items[2].orMonths)
        assertEquals(TableJob("Inspect drive belts"), items[2].work.last())
    }

    @Test
    fun `milestones may come in any order and are put in mileage order`() {
        val file = milestones("""[ { "atMiles": 60000, "work": [ { "name": "A" } ] }, { "atMiles": 30000, "work": [ { "name": "B" } ] } ]""")
        assertEquals(listOf(30_000, 60_000), (file as ImportFile.MilestoneSchedule).items.map { it.atMiles })
    }

    @Test
    fun `one mileage is one milestone, and a job appears once in it`() {
        assertEquals(
            listOf("items[1].atMiles: is the same mileage as items[0]; put all the work for one mileage in one milestone"),
            errors(milestones("""[ { "atMiles": 30000, "work": [ { "name": "A" } ] }, { "atMiles": 30000, "work": [ { "name": "B" } ] } ]""")),
        )
        assertEquals(
            listOf("items[0].work[1].name: \"inspect BELTS\" is already in this milestone, at work[0]"),
            errors(milestones("""[ { "atMiles": 30000, "work": [ { "name": "Inspect belts" }, { "name": "inspect BELTS" } ] } ]""")),
        )
        assertEquals(
            listOf("items[0].work[1].service: \"rotation\" is already used in this milestone, at work[0]"),
            errors(milestones("""[ { "atMiles": 30000, "work": [ { "name": "Rotate", "service": "rotation" }, { "name": "Swap", "service": "rotation" } ] } ]""")),
        )
    }

    @Test
    fun `the milestone schema's lists match the app's`() {
        val schema = Json.parseToJsonElement(File(docs, "schema/milestone-schedule.schema.json").readText())
        fun at(vararg path: String): JsonElement = path.fold(schema) { e, key -> e.jsonObject.getValue(key) }
        fun int(vararg path: String) = at(*path).jsonPrimitive.content.toInt()
        assertEquals(HistoryImport.services.keys, at("\$defs", "job", "properties", "service", "enum").jsonArray.map { it.jsonPrimitive.content }.toSet())
        assertEquals(MilestoneImport.MAX_NAME, int("\$defs", "job", "properties", "name", "maxLength"))
        assertEquals(MilestoneImport.MAX_MILES, int("\$defs", "milestone", "properties", "atMiles", "maximum"))
        assertEquals(MilestoneImport.MAX_MONTHS, int("\$defs", "milestone", "properties", "orMonths", "maximum"))
        assertEquals(MilestoneImport.MAX_WORK, int("\$defs", "milestone", "properties", "work", "maxItems"))
        assertEquals(MilestoneImport.MAX_ITEMS, int("properties", "items", "maxItems"))
    }
}
