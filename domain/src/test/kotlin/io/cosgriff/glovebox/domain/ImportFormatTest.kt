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
        assertEquals(listOf("version: this file is for a newer version of Glovebox (format 7; this app reads 1 to 3)"), errors(file).takeLast(1))
    }

    @Test
    fun `each kind has versions of its own`() {
        assertEquals(
            listOf("version: this file is for a newer version of Glovebox (format 2; this app reads 1)"),
            errors(ImportFormat.parse("""{ "glovebox": "import", "version": 2, "kind": "torque-specs", "items": [ { "name": "A", "category": "other" } ] }""")),
        )
        assertEquals(
            listOf("version: this file is for a newer version of Glovebox (format 4; this app reads 1 to 3)"),
            errors(ImportFormat.parse("""{ "glovebox": "import", "version": 4, "kind": "service-history", "items": [] }""")),
        )
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
        assertEquals(HistoryImport.services.keys, enum("\$defs", "item", "properties", "service", "enum"))
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
        assertEquals(HistoryImport.MAX_JOBS, int("\$defs", "record2", "properties", "jobs", "maxItems"))
        assertEquals(HistoryImport.MAX_WORK, int("\$defs", "job", "properties", "work", "maxItems"))
        assertEquals(HistoryImport.MAX_WORDING, int("\$defs", "job", "properties", "wording", "maxLength"))
        assertEquals(ImportKind.SERVICE_HISTORY.version, at("properties", "version", "enum").jsonArray.maxOf { it.jsonPrimitive.content.toInt() })
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

    private fun history2(items: String) =
        ImportFormat.parse("""{ "glovebox": "import", "version": 2, "kind": "service-history", "items": $items }""", LocalDate.of(2026, 10, 12))

    @Test
    fun `the priced history example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/service-history-priced.json")
        assertTrue(schemaAccepts("service-history", file))
        val items = (ImportFormat.parse(file.readText(), LocalDate.of(2026, 10, 12)) as ImportFile.ServiceHistory).items
        assertEquals(
            ImportedRecord(
                date = LocalDate.of(2024, 6, 14),
                odometer = 45_200,
                // Lines with no price first, then each job's, every one knowing its job.
                work = listOf(
                    ImportedWork("Tire rotation", "rotation"),
                    ImportedWork("Spark plugs", "spark-plugs", job = 0),
                    ImportedWork("Ignition coils", "ignition-coils", job = 0),
                    ImportedWork("Oil & filter change", "oil", job = 1),
                ),
                diy = false,
                shopName = "Corner Garage",
                costCents = 49_731,
                notes = "Rotation free with the oil change",
                jobs = listOf(ImportedJob("Remove & Replace Spark Plugs and Ignition Coils", 18_000, 21_240), ImportedJob(null, 3_995, 2_850)),
            ),
            items[0],
        )
        // A version-2 record may be written just as a version-1 one is.
        assertEquals(emptyList<ImportedJob>(), items[1].jobs)
    }

    @Test
    fun `a version-1 file is read as it always was, and has no jobs`() {
        val v1 = File(docs, "examples/service-history.json").readText()
        val as2 = v1.replace("\"version\": 1", "\"version\": 2")
        assertEquals(ImportFormat.parse(v1), ImportFormat.parse(as2))
        assertEquals(
            listOf("items[0].jobs: isn't a field Glovebox knows; remove it or check the spelling"),
            errors(history("""[ { "date": "2024-03-01", "odometer": 41000, "work": [ { "label": "Oil" } ], "jobs": [ { "labor": "1", "work": [ { "label": "A" } ] } ], "doneBy": { "type": "diy" } } ]""")),
        )
    }

    @Test
    fun `a job says what a line alone can't, and a record keeps to thirty lines in all`() {
        fun one(jobs: String, extra: String = "") = history2("""[ { "date": "2024-03-01", "odometer": 41000, "jobs": $jobs, $extra "doneBy": { "type": "shop" } } ]""")
        assertEquals(
            listOf("items[0].jobs[0]: needs \"labor\", \"parts\", \"wording\" or more than one line of work; a line with none of them goes in the record's \"work\""),
            errors(one("""[ { "work": [ { "label": "Oil" } ] } ]""")),
        )
        // Two lines priced as one job need nothing else; zero is a price, not "not entered".
        assertEquals(listOf(ImportedJob()), (one("""[ { "work": [ { "label": "A" }, { "label": "B" } ] } ]""") as ImportFile.ServiceHistory).items.single().jobs)
        assertEquals(listOf(ImportedJob(laborCents = 0)), (one("""[ { "labor": "0", "work": [ { "label": "A" } ] } ]""") as ImportFile.ServiceHistory).items.single().jobs)
        assertEquals(
            listOf("items[0].jobs[0].parts: must be dollars as text in quotes, with at most two decimals: \"180.00\""),
            errors(one("""[ { "parts": 12.5, "work": [ { "label": "A" } ] } ]""")),
        )
        assertEquals(listOf("items[0]: needs \"work\", \"jobs\" or both"), errors(history2("""[ { "date": "2024-03-01", "odometer": 41000, "doneBy": { "type": "diy" } } ]""")))
        val twenty = (1..20).joinToString { """{ "label": "Line $it" }""" }
        val eleven = (1..11).joinToString { """{ "label": "Job line $it" }""" }
        assertEquals(
            listOf("items[0]: has 31 lines of work, in \"work\" and \"jobs\" together; the most allowed is 30"),
            errors(one("""[ { "wording": "Eleven things", "work": [ $eleven ] } ]""", """"work": [ $twenty ],""")),
        )
    }

    private fun history3(items: String) =
        ImportFormat.parse("""{ "glovebox": "import", "version": 3, "kind": "service-history", "items": $items }""", LocalDate.of(2026, 10, 12))

    @Test
    fun `the itemized history example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/service-history-itemized.json")
        assertTrue(schemaAccepts("service-history", file))
        val record = (ImportFormat.parse(file.readText(), LocalDate.of(2026, 10, 12)) as ImportFile.ServiceHistory).items.single()
        assertEquals(
            listOf(
                ImportedJob(
                    "Remove & Replace Spark Plugs and Coils", 30_000, null,
                    listOf(
                        // Each row says which of its own job's lines it is for, by index among them.
                        ImportedPartRow("Spark plug, iridium", 4_000, 1_250, 5_000, line = 0),
                        ImportedPartRow("Ignition coil", 4_000, 9_250, 37_000, line = 1),
                        ImportedPartRow("Dielectric grease", totalCents = 600),
                    ),
                ),
                ImportedJob(
                    null, 3_995, null,
                    // Nothing is worked out: the oil filter's each stays unknown, though quantity and total are given.
                    listOf(ImportedPartRow("Synthetic oil 0W-20", 4_500, 800, 3_600), ImportedPartRow("Oil filter", 1_000, null, 1_150)),
                ),
            ),
            record.jobs,
        )
        assertEquals(InvoiceCharges(shopSuppliesCents = 1_850, taxCents = 4_845), record.charges)
        assertEquals(88_240L, record.costCents)
        assertEquals(PartSummary("Spark plug, iridium", 4_000, 1_250, 5_000, "Spark plugs"), record.jobSummaries[0].rows[0])
    }

    @Test
    fun `a version-2 file is read as it always was, and has no part rows or charges`() {
        val v2 = File(docs, "examples/service-history-priced.json").readText()
        val as3 = v2.replace("\"version\": 2", "\"version\": 3")
        assertEquals(ImportFormat.parse(v2, LocalDate.of(2026, 10, 12)), ImportFormat.parse(as3, LocalDate.of(2026, 10, 12)))
        assertEquals(
            listOf("items[0].tax: isn't a field Glovebox knows; remove it or check the spelling"),
            errors(history2("""[ { "date": "2024-03-01", "odometer": 41000, "work": [ { "label": "Oil" } ], "tax": "1.00", "doneBy": { "type": "diy" } } ]""")),
        )
    }

    @Test
    fun `a part row says something, and is for one of its own job's lines`() {
        fun one(rows: String, extra: String = "") =
            history3("""[ { "date": "2024-03-01", "odometer": 41000, "jobs": [ { $extra "partRows": $rows, "work": [ { "label": "Spark plugs" }, { "label": "Ignition coils" } ] } ], "doneBy": { "type": "shop" } } ]""")
        assertEquals(
            listOf("items[0].jobs[0].partRows[0]: says only which line it is for; a row needs at least one of \"part\", \"quantity\", \"each\" or \"total\""),
            errors(one("""[ { "for": "Spark plugs" } ]""")),
        )
        assertEquals(
            listOf("items[0].jobs[0].partRows[0].for: must be the label of one of this job's lines of work"),
            errors(one("""[ { "total": "1.00", "for": "Wipers" } ]""")),
        )
        // Capitals don't matter, and part rows alone are enough for a job of one line.
        assertEquals(1, (one("""[ { "total": "1.00", "for": "IGNITION COILS" } ]""") as ImportFile.ServiceHistory).items.single().jobs.single().rows.single().line)
        assertEquals(
            listOf("items[0].jobs[0]: has both \"parts\" and \"partRows\"; give the parts as one figure or as rows, not both"),
            errors(one("""[ { "total": "1.00" } ]""", """"parts": "1.00",""")),
        )
        assertEquals(
            listOf("items[0].jobs[0].partRows[0].quantity: must be a number as text in quotes, with at most three decimals: \"4\" or \"5.7\""),
            errors(one("""[ { "quantity": "5.7125" } ]""")),
        )
        assertEquals(
            listOf("items[0].jobs[0].partRows[0].each: must be dollars as text in quotes, with at most two decimals: \"12.50\""),
            errors(one("""[ { "each": 12.5 } ]""")),
        )
    }

    @Test
    fun `charges are dollars as text, and zero is a charge`() {
        fun one(extra: String) = history3("""[ { "date": "2024-03-01", "odometer": 41000, "work": [ { "label": "Oil" } ], $extra, "doneBy": { "type": "shop" } } ]""")
        assertEquals(InvoiceCharges(feesCents = 0), (one(""""fees": "0"""") as ImportFile.ServiceHistory).items.single().charges)
        assertEquals(
            listOf("items[0].shopSupplies: must be dollars as text in quotes, with at most two decimals: \"38.50\""),
            errors(one(""""shopSupplies": 38.5""")),
        )
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
                ImportedScheduleItem("Oil change", Interval(7_500, 12), Interval(5_000, 6), DueRule.WHICHEVER_FIRST, "oil"),
                ImportedScheduleItem("Rotate tires", Interval(5_000, null), service = "rotation"),
                ImportedScheduleItem("Brake fluid flush", Interval(null, 36)),
                ImportedScheduleItem("Grease driveshaft", Interval(15_000, null), Interval(5_000, null), DueRule.MILEAGE_ONLY, "driveshaft"),
                ImportedScheduleItem("Coolant", Interval(50_000, 60), service = "coolant", firstAt = 100_000),
                ImportedScheduleItem("Differential oil when towing", Interval(15_000, null), service = "rear-diff", severeOnly = true),
            ),
            items,
        )
    }

    @Test
    fun `a schedule item needs an interval, in whole miles or months`() {
        assertEquals(listOf("items[0]: needs \"everyMiles\" or \"everyKm\", \"everyMonths\", or a distance and months"), errors(schedule("""[ { "name": "Oil change" } ]""")))
        assertEquals(listOf("items[0].everyMiles: must be a whole number, without quotes"), errors(schedule("""[ { "name": "Oil change", "everyMiles": 7500.5 } ]""")))
        assertEquals(listOf("items[0].everyMonths: must be from 1 to 600"), errors(schedule("""[ { "name": "Oil change", "everyMonths": 0 } ]""")))
        assertEquals(listOf("items[0].severe: needs \"everyMiles\" or \"everyKm\", \"everyMonths\", or a distance and months"), errors(schedule("""[ { "name": "Oil change", "everyMiles": 7500, "severe": {} } ]""")))
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
        val parsed = ImportFormat.parse(file.readText()) as ImportFile.MilestoneSchedule
        val items = parsed.items
        assertEquals(120_000, parsed.repeatsEveryMiles)
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
    fun `a table may say it repeats, with a cycle no shorter than the table`() {
        fun table(repeats: String) = ImportFormat.parse(
            """{ "glovebox": "import", "version": 1, "kind": "milestone-schedule", "repeatsEveryMiles": $repeats, "items": [ { "atMiles": 60000, "work": [ { "name": "A" } ] }, { "atMiles": 30000, "work": [ { "name": "B" } ] } ] }""",
        )
        assertEquals(null, (milestones("""[ { "atMiles": 30000, "work": [ { "name": "A" } ] } ]""") as ImportFile.MilestoneSchedule).repeatsEveryMiles)
        assertEquals(60_000, (table("60000") as ImportFile.MilestoneSchedule).repeatsEveryMiles)
        assertEquals(
            listOf("repeatsEveryMiles: is 59999, less than the highest atMiles in the file (60000); it is the length of the whole cycle, so it can't be shorter than the table"),
            errors(table("59999")),
        )
        assertEquals(listOf("repeatsEveryMiles: must be from 1 to 1000000"), errors(table("0")))
        assertEquals(listOf("repeatsEveryMiles: must be a whole number, without quotes"), errors(table("null")))
    }

    @Test
    fun `only a milestone file has a repeat`() {
        val file = ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "interval-schedule", "repeatsEveryMiles": 120000, "items": [ { "name": "Oil change", "everyMiles": 7500 } ] }""")
        assertEquals(listOf("repeatsEveryMiles: isn't a field Glovebox knows; remove it or check the spelling"), errors(file))
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
        assertEquals(MilestoneImport.MAX_MILES, int("properties", MilestoneImport.REPEATS, "maximum"))
    }

    @Test
    fun `the kilometre examples are accepted by the schema and by the app, as kilometres`() {
        fun parsed(kind: String): ImportFile {
            val file = File(docs, "examples/$kind-km.json")
            assertTrue("the schema refuses $kind-km.json", schemaAccepts(kind, file))
            return ImportFormat.parse(file.readText(), LocalDate.of(2026, 10, 12))
        }
        val schedule = parsed("interval-schedule") as ImportFile.IntervalSchedule
        assertEquals(DistanceUnit.KILOMETERS, schedule.unit)
        assertEquals(ImportedScheduleItem("Oil change", Interval(12_000, 12), Interval(8_000, 6), DueRule.WHICHEVER_FIRST, "oil"), schedule.items[0])
        // An item with only months has no distance, and takes no side.
        assertEquals(Interval(null, 24), schedule.items[2].normal)

        val history = parsed("service-history") as ImportFile.ServiceHistory
        assertEquals(DistanceUnit.KILOMETERS, history.unit)
        assertEquals(listOf(66_000, 78_250), history.items.map { it.odometer })

        val milestones = parsed("milestone-schedule") as ImportFile.MilestoneSchedule
        assertEquals(DistanceUnit.KILOMETERS, milestones.unit)
        assertEquals(listOf(15_000, 30_000, 90_000), milestones.items.map { it.atMiles })
        assertEquals(180_000, milestones.repeatsEveryMiles)
    }

    @Test
    fun `the miles examples are still miles`() {
        assertEquals(DistanceUnit.MILES, (ImportFormat.parse(File(docs, "examples/interval-schedule.json").readText()) as ImportFile.IntervalSchedule).unit)
        assertEquals(DistanceUnit.MILES, (ImportFormat.parse(File(docs, "examples/service-history.json").readText()) as ImportFile.ServiceHistory).unit)
        assertEquals(DistanceUnit.MILES, (ImportFormat.parse(File(docs, "examples/milestone-schedule.json").readText()) as ImportFile.MilestoneSchedule).unit)
    }

    @Test
    fun `a figure is in miles or kilometres, never both, and a file keeps to one`() {
        assertEquals(
            listOf("items[0].everyKm: give \"everyMiles\" or \"everyKm\", not both"),
            errors(schedule("""[ { "name": "Oil change", "everyMiles": 7500, "everyKm": 12000 } ]""")),
        )
        assertEquals(
            listOf("items[1].everyKm: is in kilometres, but items[0].everyMiles is in miles; a file gives every distance in one unit"),
            errors(schedule("""[ { "name": "Oil change", "everyMiles": 7500 }, { "name": "Rotate tires", "everyKm": 10000 } ]""")),
        )
        assertEquals(
            listOf("items[0].severe.everyMiles: is in miles, but items[0].everyKm is in kilometres; a file gives every distance in one unit"),
            errors(schedule("""[ { "name": "Oil change", "everyKm": 12000, "severe": { "everyMiles": 5000 } } ]""")),
        )
        assertEquals(listOf("items[0].everyKm: must be from 1 to 800000"), errors(schedule("""[ { "name": "Oil change", "everyKm": 800001 } ]""")))
        // A months-only item sits in a file of either unit.
        assertTrue(schedule("""[ { "name": "Brake fluid", "everyMonths": 24 }, { "name": "Oil change", "everyKm": 12000 } ]""") is ImportFile.IntervalSchedule)
    }

    @Test
    fun `a record's reading is odometer or odometerKm, and a table's cycle is in its milestones' unit`() {
        val rest = """"date": "2024-03-01", "work": [ { "label": "Oil change" } ], "doneBy": { "type": "diy" }"""
        assertEquals(listOf("items[0]: needs \"odometer\" (miles) or \"odometerKm\""), errors(history("""[ { $rest } ]""")))
        assertEquals(listOf("items[0].odometerKm: give \"odometer\" or \"odometerKm\", not both"), errors(history("""[ { $rest, "odometer": 41000, "odometerKm": 66000 } ]""")))
        assertEquals(66_000, (history("""[ { $rest, "odometerKm": 66000 } ]""") as ImportFile.ServiceHistory).items.single().odometer)

        fun table(extra: String, at: String) = ImportFormat.parse(
            """{ "glovebox": "import", "version": 1, "kind": "milestone-schedule", $extra "items": [ { $at, "work": [ { "name": "A" } ] } ] }""",
        )
        assertEquals(96_000, (table(""""repeatsEveryKm": 96000,""", """"atKm": 48000""") as ImportFile.MilestoneSchedule).repeatsEveryMiles)
        assertEquals(
            listOf("repeatsEveryMiles: is in miles, but items[0].atKm is in kilometres; a file gives every distance in one unit"),
            errors(table(""""repeatsEveryMiles": 120000,""", """"atKm": 48000""")),
        )
        assertEquals(
            listOf("repeatsEveryKm: is 20000, less than the highest atKm in the file (48000); it is the length of the whole cycle, so it can't be shorter than the table"),
            errors(table(""""repeatsEveryKm": 20000,""", """"atKm": 48000""")),
        )
        assertEquals(listOf("items[0]: needs \"atMiles\" or \"atKm\""), errors(table("", """"orMonths": 12""")))
    }

    @Test
    fun `the kilometre limits in the schemas match the app's`() {
        fun schema(kind: String) = Json.parseToJsonElement(File(docs, "schema/$kind.schema.json").readText())
        fun JsonElement.at(vararg path: String): JsonElement = path.fold(this) { e, key -> e.jsonObject.getValue(key) }
        fun JsonElement.int(vararg path: String) = at(*path).jsonPrimitive.content.toInt()
        assertEquals(ScheduleImport.MAX_KM, schema("interval-schedule").int("\$defs", "km", "maximum"))
        assertEquals(DistanceUnit.KILOMETERS.scheduled(ScheduleImport.MAX_MILES), ScheduleImport.MAX_KM)
        assertEquals(MilestoneImport.MAX_KM, schema("milestone-schedule").int("\$defs", "milestone", "properties", "atKm", "maximum"))
        assertEquals(MilestoneImport.MAX_KM, schema("milestone-schedule").int("properties", MilestoneImport.REPEATS_KM, "maximum"))
        assertEquals(DistanceUnit.KILOMETERS.scheduled(MilestoneImport.MAX_MILES), MilestoneImport.MAX_KM)
        assertEquals(HistoryImport.MAX_ODOMETER, schema("service-history").int("\$defs", "record", "properties", "odometerKm", "maximum"))
    }
}
