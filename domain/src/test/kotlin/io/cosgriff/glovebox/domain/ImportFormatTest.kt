package io.cosgriff.glovebox.domain

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
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

class ImportFormatTest {
    // Gradle runs a module's tests from the module's folder.
    private val docs = File("../docs/import")
    private val torqueSchema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
        .getSchema(File(docs, "schema/torque-specs.schema.json").readText())

    private fun schemaAccepts(file: File): Boolean = torqueSchema.validate(file.readText(), InputFormat.JSON).isEmpty()
    private fun examples(folder: String): List<File> = File(docs, "examples/$folder").listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }

    private fun torque(items: String) = ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "torque-specs", "items": $items }""")
    private fun errors(file: ImportFile): List<String> = (file as ImportFile.Invalid).errors.map { it.toString() }

    @Test
    fun `the published example is accepted by the schema and by the app`() {
        val file = File(docs, "examples/torque-specs.json")
        assertTrue(schemaAccepts(file))
        val parsed = ImportFormat.parse(file.readText()) as ImportFile.TorqueSpecs
        assertEquals(listOf("Lug nuts", "Oil drain plug", "Valve cover bolts", "Skid plate bolts"), parsed.items.map { it.name })
        assertEquals(TorqueSource.FACTORY, parsed.items[0].source)
        assertEquals(135.58, parsed.items[0].torque!!.newtonMeters, 0.01)
        assertEquals(40.0, parsed.items[1].torque!!.newtonMeters, 0.0)
        assertEquals(null, parsed.items[3].torque)
    }

    @Test
    fun `every invalid example is refused by both the schema and the app`() {
        val files = examples("invalid")
        assertTrue("the invalid examples are missing", files.size > 20)
        for (file in files) {
            assertFalse("the schema accepts ${file.name}", schemaAccepts(file))
            assertTrue("the app accepts ${file.name}", ImportFormat.parse(file.readText()) is ImportFile.Invalid)
        }
    }

    @Test
    fun `rules the schema can't state are still enforced by the app`() {
        for (file in examples("beyond-schema")) {
            assertTrue("${file.name} belongs in invalid", schemaAccepts(file))
            assertTrue("the app accepts ${file.name}", ImportFormat.parse(file.readText()) is ImportFile.Invalid)
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
    fun `a kind that is specified but not built says so`() {
        val file = ImportFormat.parse("""{ "glovebox": "import", "version": 1, "kind": "service-history", "items": [] }""")
        assertEquals(ImportFile.NotYet(ImportKind.SERVICE_HISTORY), file)
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
        val schema = Json.parseToJsonElement(File(docs, "schema/torque-specs.schema.json").readText())
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
    }
}
