package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnownSpecsTest {
    // Gradle runs a module's tests from the module's folder.
    private val assets = File("../app/src/main/assets/${KnownSpecs.ASSET_DIR}")
    private val bundled: List<KnownSpecList> by lazy {
        assets.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }.map { file ->
            when (val parsed = KnownSpecs.parse(file.readText())) {
                is KnownSpecFile.Ok -> parsed.list
                is KnownSpecFile.Invalid -> throw AssertionError("${file.name}: ${parsed.errors}")
            }
        }
    }

    // A made-up model with made-up values: the rules are tested on this, never on a real vehicle's figures.
    private val someItems = """[
        { "name": "Lug nuts", "category": "wheels", "socket": "19 mm", "torque": { "value": 90, "unit": "ft-lb" }, "note": "Star pattern." },
        { "name": "Oil drain plug", "category": "engine", "socket": "17 mm", "torque": { "value": 25, "unit": "ft-lb" } },
        { "name": "Axle nut", "category": "driveline", "torque": { "value": 200, "unit": "Nm" } },
        { "name": "Panhard bar", "category": "suspension", "torque": { "value": 100, "unit": "ft-lb" } }
    ]"""

    private fun file(extra: String = "", items: String = someItems) = """
        { "id": "made-up", "title": "Made-up Roadster", "make": "Acme", "model": "Roadster", "years": { "from": 2001, "to": 2005 },
          "source": { "name": "example.test", "url": "https://example.test/list", "retrieved": "2026-01-02", "note": "A made-up list.", "basis": "Made up for the tests." },
          $extra "items": $items }
    """.trimIndent()

    private val roadster get() = (KnownSpecs.parse(file()) as KnownSpecFile.Ok).list
    private val lists get() = listOf(roadster)

    private fun errors(text: String) = (KnownSpecs.parse(text) as KnownSpecFile.Invalid).errors.map { it.toString() }

    private fun saved(id: Long, name: String, category: TorqueCategory, torque: Torque? = null, socket: String? = null, note: String? = null) =
        SavedTorqueSpec(id, name, category, socket, torque, TorqueSource.USER, note)

    @Test
    fun `every list that ships is read, says where it came from and why it may ship, and has a value for every row`() {
        assertTrue(bundled.isNotEmpty())
        assertEquals(bundled.size, bundled.map { it.id }.toSet().size)
        for (list in bundled) {
            assertTrue(list.source.basis.isNotBlank())
            assertTrue(list.items.isNotEmpty())
            assertTrue(list.items.all { it.torque != null && it.torque!!.newtonMeters <= TorqueSpecs.MAX_NEWTON_METERS })
            // One row per fastener, so adding a list twice can't double anything.
            assertEquals(list.items.size, list.items.map { TorqueImport.key(it.name, it.category) }.toSet().size)
        }
    }

    @Test
    fun `no two lists claim the same vehicle`() {
        for (a in bundled) for (b in bundled) {
            if (a.id < b.id && a.make.equals(b.make, true) && a.model.equals(b.model, true)) {
                assertTrue("${a.id} and ${b.id} overlap", a.years.last < b.years.first || b.years.last < a.years.first)
            }
        }
    }

    @Test
    fun `the 4Runner lists say what Toyota's owner's manuals say for each run of years`() {
        // Read out of Toyota's own documents (docs/known-specs.md has which); the figure changed twice.
        fun ftLb(year: Int) = KnownSpecs.match(bundled, year, "TOYOTA", "4Runner")!!.items.associate { it.name to it.torque!!.inUnit(TorqueUnit.FOOT_POUND) }
        fun same(expected: Map<String, Double>, year: Int) {
            val actual = ftLb(year)
            assertEquals("$year", expected.keys, actual.keys)
            expected.forEach { (name, value) -> assertEquals("$year $name", value, actual.getValue(name), 0.001) }
        }
        for (year in 2010..2011) same(mapOf("Lug nuts" to 83.0), year)
        for (year in 2012..2014) same(mapOf("Lug nuts" to 81.0), year)
        val byWheel = mapOf("Lug nuts, steel wheels" to 83.0, "Lug nuts, aluminum wheels" to 76.0)
        for (year in 2015..2016) same(byWheel, year)
        for (year in 2017..2024) same(byWheel + ("Spare tire carrier" to 34.4), year)
        assertNull(KnownSpecs.match(bundled, 2009, "TOYOTA", "4Runner"))
        assertNull(KnownSpecs.match(bundled, 2025, "TOYOTA", "4Runner"))
        assertTrue(bundled.filter { it.model == "4Runner" }.all { it.source.name == "Toyota owner's manual" && it.source.url!!.startsWith("https://assets.sia.toyota.com/") })
    }

    @Test
    fun `a list is read with its source and its values`() {
        assertEquals("example.test", roadster.source.name)
        assertEquals("Made up for the tests.", roadster.source.basis)
        assertEquals(2001..2005, roadster.years)
        assertEquals(4, roadster.items.size)
        assertEquals(90.0, roadster.items[0].torque!!.inUnit(TorqueUnit.FOOT_POUND), 0.001)
        assertEquals(200.0, roadster.items[2].torque!!.newtonMeters, 0.001)
        assertEquals("19 mm", roadster.items[0].socket)
    }

    @Test
    fun `a list is offered for its model and years and nothing else`() {
        assertEquals(roadster, KnownSpecs.match(lists, 2003, "ACME", "Roadster"))
        assertEquals(roadster, KnownSpecs.match(lists, 2001, "Acme", "roadster"))
        assertEquals(roadster, KnownSpecs.match(lists, 2005, "acme", "Road ster"))
        assertNull(KnownSpecs.match(lists, 2000, "ACME", "Roadster"))
        assertNull(KnownSpecs.match(lists, 2006, "ACME", "Roadster"))
        assertNull(KnownSpecs.match(lists, 2003, "ACME", "Coupe"))
        assertNull(KnownSpecs.match(lists, 2003, "OTHER", "Roadster"))
        assertNull(KnownSpecs.match(emptyList(), 2003, "ACME", "Roadster"))
    }

    @Test
    fun `adding a list fills empty rows and never touches one the user has filled in`() {
        val existing = listOf(
            saved(1, "lug NUTS", TorqueCategory.WHEELS),
            saved(2, "Oil drain plug", TorqueCategory.ENGINE, Torque.of(29.0, TorqueUnit.FOOT_POUND)),
            saved(3, "Axle nut", TorqueCategory.DRIVELINE, note = "Stake it"),
            // The same name in another category is another fastener.
            saved(4, "Panhard bar", TorqueCategory.OTHER, Torque(100.0)),
        )
        val plan = KnownSpecs.plan(roadster, existing)
        assertEquals(listOf(1L), plan.fills.map { it.second.id })
        assertEquals(listOf(2L, 3L), plan.kept.map { it.second.id })
        assertEquals(listOf("Panhard bar"), plan.new.map { it.name })
        assertEquals(2, plan.changes)
        // Once it is all there, there is nothing left to offer.
        val all = roadster.items.mapIndexed { i, it -> SavedTorqueSpec(i.toLong(), it.name, it.category, it.socket, it.torque, TorqueSource.REFERENCE, it.note) }
        assertEquals(0, KnownSpecs.plan(roadster, all).changes)
    }

    @Test
    fun `a reference value stays one until it is vouched for or changed`() {
        val ref = TorqueSource.REFERENCE
        val value = Torque.of(85.0, TorqueUnit.FOOT_POUND)
        // Saved untouched, or with only the socket or note changed: still to double-check.
        assertEquals(ref, KnownSpecs.sourceAfterEdit(ref, value, Torque.of(85.0, TorqueUnit.FOOT_POUND), fromFactoryManual = false))
        // Shown and saved to one decimal: that isn't a change.
        assertEquals(ref, KnownSpecs.sourceAfterEdit(ref, value, Torque(value.newtonMeters + 0.01), fromFactoryManual = false))
        // The user says the manual agrees.
        assertEquals(TorqueSource.FACTORY, KnownSpecs.sourceAfterEdit(ref, value, value, fromFactoryManual = true))
        // The user checked it some other way: it is their entry now, with the same number.
        assertEquals(TorqueSource.USER, KnownSpecs.sourceAfterEdit(ref, value, value, fromFactoryManual = false, checked = true))
        assertEquals(TorqueSource.FACTORY, KnownSpecs.sourceAfterEdit(ref, value, value, fromFactoryManual = true, checked = true))
        // The user puts their own figure in.
        assertEquals(TorqueSource.USER, KnownSpecs.sourceAfterEdit(ref, value, Torque.of(83.0, TorqueUnit.FOOT_POUND), fromFactoryManual = false))
        // Taking the value out leaves nothing to have a source.
        assertEquals(TorqueSource.USER, KnownSpecs.sourceAfterEdit(ref, value, null, fromFactoryManual = true))
        // Rows that were never from a list behave as they always did.
        assertEquals(TorqueSource.USER, KnownSpecs.sourceAfterEdit(TorqueSource.USER, value, value, fromFactoryManual = false))
        assertEquals(TorqueSource.FACTORY, KnownSpecs.sourceAfterEdit(null, null, value, fromFactoryManual = true))
        assertEquals(TorqueSource.USER, KnownSpecs.sourceAfterEdit(TorqueSource.FACTORY, value, value, fromFactoryManual = false))
    }

    @Test
    fun `a list file is held to the import format and to saying its source`() {
        assertTrue(KnownSpecs.parse(file()) is KnownSpecFile.Ok)
        // A printed manual has no address; every list has to say why it may be shipped.
        assertTrue(KnownSpecs.parse(file().replace(""""url": "https://example.test/list", """, "")) is KnownSpecFile.Ok)
        assertEquals(listOf("source.basis: is required"), errors(file().replace(""", "basis": "Made up for the tests."""", "")))
        assertEquals(listOf("items[0]: has no torque value; a known spec is a value"), errors(file(items = """[ { "name": "Lug nuts", "category": "wheels" } ]""")))
        assertEquals(
            listOf("items[0].source: isn't for a bundled list: every value in one is a reference to double-check"),
            errors(file(items = """[ { "name": "Lug nuts", "category": "wheels", "torque": { "value": 85, "unit": "ft-lb" }, "source": "factory-manual" } ]""")),
        )
        assertEquals(listOf("extra: isn't a field Glovebox knows; remove it or check the spelling"), errors(file(extra = """"extra": 1,""")))
        assertTrue(errors(file().replace("https://example.test/list", "http://example.test/list")).single().startsWith("source.url"))
        assertTrue(errors(file().replace(""""to": 2005""", """"to": 1999""")).single().startsWith("years.to"))
        assertTrue(errors(file().replace(""""retrieved": "2026-01-02", """, "")).single().startsWith("source.retrieved"))
        assertTrue(KnownSpecs.parse("not json") is KnownSpecFile.Invalid)
    }

    @Test
    fun `an unconfirmed reference value is kept out of a file the user saves`() {
        val rows = listOf(
            SavedTorqueSpec(1, "Lug nuts", TorqueCategory.WHEELS, "21 mm", Torque.of(85.0, TorqueUnit.FOOT_POUND), TorqueSource.REFERENCE, null),
            SavedTorqueSpec(2, "Oil drain plug", TorqueCategory.ENGINE, "14 mm", Torque.of(30.0, TorqueUnit.FOOT_POUND), TorqueSource.FACTORY, null),
        )
        val written = TorqueExport.file(rows, TorqueUnit.FOOT_POUND)
        assertEquals(1, written.count)
        assertEquals(listOf(LeftOut("Lug nuts", TorqueExport.UNCHECKED)), written.leftOut)
        assertEquals(listOf("Oil drain plug"), (ImportFormat.parse(written.text!!) as ImportFile.TorqueSpecs).items.map { it.name })
        // Nothing but reference values: no file at all.
        assertNull(TorqueExport.file(rows.take(1), TorqueUnit.FOOT_POUND).text)
    }

    @Test
    fun `an old value in the database is still read`() {
        assertEquals(TorqueSource.REFERENCE, TorqueSource.of("REFERENCE"))
        assertEquals(TorqueSource.USER, TorqueSource.of("SOMETHING ELSE"))
    }
}
