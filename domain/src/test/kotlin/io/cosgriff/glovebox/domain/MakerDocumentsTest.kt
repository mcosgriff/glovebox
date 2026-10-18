package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MakerDocumentsTest {
    // Gradle runs a module's tests from the module's folder.
    private val bundled: List<MakerDocumentList> by lazy {
        File("../app/src/main/assets/${MakerDocuments.ASSET_DIR}").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }.map { file ->
            when (val parsed = MakerDocuments.parse(file.readText())) {
                is MakerDocumentFile.Ok -> parsed.list
                is MakerDocumentFile.Invalid -> throw AssertionError("${file.name}: ${parsed.errors}")
            }
        }
    }

    private fun file(documents: String) = """{ "make": "Acme", "model": "Roadster", "documents": $documents }"""

    private fun errors(text: String) = (MakerDocuments.parse(text) as MakerDocumentFile.Invalid).errors.map { it.toString() }

    @Test
    fun `every file that ships is read`() {
        assertTrue(bundled.isNotEmpty())
    }

    @Test
    fun `the 4Runner has Toyota's manual and guide for every year a list covers`() {
        for (year in 2017..2024) {
            val docs = MakerDocuments.forVehicle(bundled, year, "TOYOTA", "4Runner")!!.documents
            // 2022 has two owner's manuals, which Toyota splits by production date.
            val manuals = if (year == 2022) 2 else 1
            assertEquals("$year", List(manuals) { MakerDocumentKind.OWNERS_MANUAL } + MakerDocumentKind.MAINTENANCE_GUIDE, docs.map { it.kind })
            assertTrue(docs.last().url.endsWith("T-MMS-${year % 100}4Runner.pdf"))
            assertEquals(year == 2022, docs.first().edition != null)
        }
        assertEquals(null, MakerDocuments.forVehicle(bundled, 2016, "Toyota", "4Runner"))
        assertEquals("Toyota", MakerDocuments.forVehicle(bundled, 2019, "TOYOTA", "4RUNNER")!!.make)
    }

    @Test
    fun `the GTI has Volkswagen's own cards and booklets, and its manual lookup from 2012`() {
        fun docs(year: Int) = MakerDocuments.forVehicle(bundled, year, "VOLKSWAGEN", "GTI")!!.documents
        fun kinds(year: Int) = docs(year).map { it.kind }
        val (manual, guide, warranty, chart) = MakerDocumentKind.entries
        assertEquals(listOf(guide, warranty), kinds(2010))
        assertEquals(listOf(guide), kinds(2011)) // Volkswagen has no digital manual or warranty booklet for 2011
        assertEquals(listOf(manual, guide, warranty), kinds(2012))
        assertEquals(listOf(manual, guide, warranty, chart), kinds(2013))
        assertEquals(listOf(manual, guide, chart), kinds(2014))
        // The manual is found on Volkswagen's site by VIN, which never goes in the link.
        for (year in 2012..2014) assertTrue(docs(year).first().vinLookup && docs(year).first().code == null)
        assertTrue(docs(2011).single().url.startsWith("https://ownersliterature.vw.com/"))
    }

    @Test
    fun `a link goes only to a maker's or NHTSA's server, over https`() {
        fun doc(url: String) = file("""[{ "kind": "owners-manual", "years": { "from": 2001, "to": 2001 }, "code": "X1", "url": "$url" }]""")
        assertTrue(MakerDocuments.parse(doc("https://static.nhtsa.gov/odi/tsbs/2012/x.pdf")) is MakerDocumentFile.Ok)
        assertTrue(errors(doc("http://static.nhtsa.gov/x.pdf")).any { "https" in it })
        assertTrue(errors(doc("https://manuals.example.test/x.pdf")).any { "manuals.example.test" in it })
        // A VIN lookup page has no code; a document must have one.
        val lookup = file("""[{ "kind": "owners-manual", "years": { "from": 2001, "to": 2001 }, "vinLookup": true, "url": "https://www.vw.com/x.html" }]""")
        assertTrue(MakerDocuments.parse(lookup) is MakerDocumentFile.Ok)
        assertTrue(errors(file("""[{ "kind": "owners-manual", "years": { "from": 2001, "to": 2001 }, "url": "https://www.vw.com/x.html" }]""")).any { "code" in it })
    }

    @Test
    fun `one document of a kind per year`() {
        val two = file(
            """[{ "kind": "owners-manual", "years": { "from": 2001, "to": 2003 }, "code": "A", "url": "https://static.nhtsa.gov/a.pdf" },
                { "kind": "owners-manual", "years": { "from": 2003, "to": 2004 }, "code": "B", "url": "https://static.nhtsa.gov/b.pdf" }]""",
        )
        assertTrue(errors(two).any { "2003" in it })
        val editions = two.replace("\"code\": \"A\"", "\"code\": \"A\", \"edition\": \"early\"").replace("\"code\": \"B\"", "\"code\": \"B\", \"edition\": \"late\"")
        assertTrue(MakerDocuments.parse(editions) is MakerDocumentFile.Ok)
    }
}
