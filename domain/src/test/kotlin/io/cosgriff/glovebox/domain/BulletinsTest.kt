package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class BulletinsTest {
    // Gradle runs a module's tests from the module's folder.
    private val bundled: List<BulletinList> by lazy {
        File("../app/src/main/assets/${Bulletins.ASSET_DIR}").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }.map { file ->
            when (val parsed = Bulletins.parse(file.readText())) {
                is BulletinFile.Ok -> parsed.list
                is BulletinFile.Invalid -> throw AssertionError("${file.name}: ${parsed.errors}")
            }
        }
    }

    private fun bulletin(
        id: String = "11000001",
        kind: String = "SERVICE_CAMPAIGN",
        byWording: Boolean = false,
        products: String = """[{ "model": "ROADSTER", "years": [2019, 2020] }]""",
        pdf: String? = "https://static.nhtsa.gov/odi/tsbs/2025/MC-11000001-0001.pdf",
        date: String? = "2025-02-10",
    ) = """{ "id": "$id", "number": "26TD01", "date": ${date?.let { "\"$it\"" } ?: "null"}, "kind": "$kind", "byWording": $byWording,
        "summary": "A made-up summary.", "products": $products, "components": ["STRUCTURE"], "pdf": ${pdf?.let { "\"$it\"" } ?: "null"} }"""

    private fun file(vararg bulletins: String, aliases: String = "[]", model: String = "Roadster", years: String = "[2018, 2019, 2020, 2021]") =
        """{ "make": "Acme", "model": "$model", "modelAliases": $aliases, "years": $years, "asOf": "2026-10-24", "bulletins": [${bulletins.joinToString(",")}] }"""

    private fun read(text: String) = (Bulletins.parse(text) as BulletinFile.Ok).list

    private fun errors(text: String) = (Bulletins.parse(text) as BulletinFile.Invalid).errors.map { it.toString() }

    @Test
    fun `every file that ships is read, and every model with a maker's schedule has one`() {
        assertTrue(bundled.isNotEmpty())
        val schedules = File("../app/src/main/assets/known-schedules").listFiles { f -> f.extension == "json" }.orEmpty()
            .map { (KnownSchedules.parse(it.readText()) as KnownScheduleFile.Ok).list }
        for (s in schedules) {
            assertTrue("${s.make} ${s.model} has no bulletins file", bundled.any { KnownLists.same(it.make, s.make) && KnownLists.same(it.model, s.model) })
        }
    }

    @Test
    fun `a bulletin reads back as written`() {
        val list = read(file(bulletin(byWording = true, kind = "WARRANTY_EXTENSION")))
        assertEquals(LocalDate.of(2026, 10, 24), list.asOf)
        val b = list.bulletins.single()
        assertEquals("11000001", b.id)
        assertEquals("26TD01", b.number)
        assertEquals(LocalDate.of(2025, 2, 10), b.date)
        assertEquals(BulletinKind.WARRANTY_EXTENSION, b.kind)
        assertTrue(b.byWording)
        assertEquals(listOf(BulletinProduct("ROADSTER", listOf(2019, 2020))), b.products)
        assertEquals("https://static.nhtsa.gov/odi/tsbs/2025/MC-11000001-0001.pdf", b.pdf)
    }

    @Test
    fun `a bulletin may have no date and no pdf`() {
        val b = read(file(bulletin(pdf = null, date = null))).bulletins.single()
        assertNull(b.date)
        assertNull(b.pdf)
    }

    @Test
    fun `a link goes only to NHTSA, over https`() {
        assertTrue(errors(file(bulletin(pdf = "https://example.com/a.pdf"))).any { "pdf" in it })
        assertTrue(errors(file(bulletin(pdf = "http://static.nhtsa.gov/a.pdf"))).any { "pdf" in it })
    }

    @Test
    fun `an unknown kind, or a bulletin for no year, is refused`() {
        assertTrue(errors(file(bulletin(kind = "SERVICE_BULLETIN"))).any { "kind" in it })
        assertTrue(errors(file(bulletin(products = """[{ "model": "ROADSTER", "years": [] }]"""))).any { "years" in it })
    }

    @Test
    fun `a vehicle gets the bulletins for its year, matched by make and model as NHTSA or an alias spells them`() {
        val list = read(
            file(
                bulletin(id = "1", products = """[{ "model": "ROADSTER", "years": [2019] }]"""),
                bulletin(id = "2", products = """[{ "model": "ROADSTER HYBRID", "years": [2020] }]"""),
                aliases = """["Roadster Hybrid"]""",
            ),
        )
        assertEquals(listOf("1"), Bulletins.forVehicle(listOf(list), 2019, "ACME", "roadster")!!.bulletins.map { it.id })
        assertEquals(listOf("2"), Bulletins.forVehicle(listOf(list), 2020, "Acme", "Roadster Hybrid")!!.bulletins.map { it.id })
        assertEquals(LocalDate.of(2026, 10, 24), Bulletins.forVehicle(listOf(list), 2019, "Acme", "Roadster")!!.asOf)
        // A covered model with nothing for its year still says so, as of when; an uncovered model or year has no list at all.
        assertEquals(emptyList<Bulletin>(), Bulletins.forVehicle(listOf(list), 2021, "Acme", "Roadster")!!.bulletins)
        assertNull(Bulletins.forVehicle(listOf(list), 2019, "Acme", "Coupe"))
        assertNull(Bulletins.forVehicle(listOf(list), 2022, "Acme", "Roadster"))
    }

    @Test
    fun `a list named for the model wins over one that only has it as an alias, for the years it covers`() {
        val gas = read(file(bulletin(id = "1", products = """[{ "model": "ROADSTER", "years": [2020, 2026] }]"""), aliases = """["Roadster Plug-in"]""", years = "[2020, 2026]"))
        val plugIn = read(file(bulletin(id = "2", products = """[{ "model": "ROADSTER PLUG-IN", "years": [2020] }]"""), model = "Roadster Plug-in", years = "[2020, 2021]"))
        assertEquals(listOf("2"), Bulletins.forVehicle(listOf(gas, plugIn), 2020, "Acme", "Roadster Plug-in")!!.bulletins.map { it.id })
        // 2026's plug-in is on the gas model's schedule, as an alias.
        assertEquals(listOf("1"), Bulletins.forVehicle(listOf(gas, plugIn), 2026, "Acme", "Roadster Plug-in")!!.bulletins.map { it.id })
    }

    @Test
    fun `a plug-in typed without NHTSA's (PHEV) gets the bulletins for its own model`() {
        val prime = read(file(bulletin(id = "1", products = """[{ "model": "ROADSTER PRIME", "years": [2020] }]"""), model = "Roadster Prime (PHEV)"))
        assertEquals(listOf("1"), Bulletins.forVehicle(listOf(prime), 2020, "Acme", "Roadster Prime")!!.bulletins.map { it.id })
        assertTrue(Bulletins.forVehicle(bundled, 2023, "Toyota", "RAV4 Prime")!!.bulletins.all { b -> b.products.any { it.model == "RAV4 PRIME" } })
    }

    @Test
    fun `runs of model years are found for the app to word`() {
        assertEquals(listOf(2016..2018, 2020..2020), Bulletins.yearRuns(listOf(2020, 2016, 2017, 2018, 2017)))
    }

    @Test
    fun `the 2011 GTI has Volkswagen's warranty extensions`() {
        val gti = Bulletins.forVehicle(bundled, 2011, "VOLKSWAGEN", "GTI")!!
        assertTrue(gti.bulletins.any { it.number == "VWP-18-07" && it.kind == BulletinKind.WARRANTY_EXTENSION })
    }

    @Test
    fun `a summary that says its program expired on a date has ended then`() {
        assertEquals(LocalDate.of(2022, 12, 1), Bulletins.endedOn("DP: Customer Support Program 26TE01 expired December 1, 2022."))
    }

    @Test
    fun `the maker's other ways of saying a program expired are read, with either kind of date`() {
        assertEquals(LocalDate.of(2019, 10, 31), Bulletins.endedOn("Dealer Package: Limited Service Campaign 26TD01 Expired on October 31, 2019 In the involved vehicles…"))
        assertEquals(LocalDate.of(2019, 11, 30), Bulletins.endedOn("Technical Instructions: This Limited Service Campaign has expired as of November 30, 2019."))
        assertEquals(LocalDate.of(2025, 2, 7), Bulletins.endedOn("26TD02 expired on 2/7/2025 A seam near the made-up part…"))
        assertEquals(LocalDate.of(2023, 10, 31), Bulletins.endedOn("TI: 26TD03 expired on 10/31/2023."))
    }

    @Test
    fun `a campaign no longer active as of a date has ended then`() {
        assertEquals(
            LocalDate.of(2018, 2, 20),
            Bulletins.endedOn("Dealer Package: Date 02/20/18 ? As of February 20, 2018, Special Service Campaign Z0Z is no longer active. ? Because…"),
        )
    }

    @Test
    fun `nothing else is read as an end`() {
        // A campaign made permanent: the one summary on file with "expiration" in it.
        assertNull(Bulletins.endedOn("Toyota converted Limited Service Campaign (LSC) Z0Z into Special Service Campaign (SSC) Z0Z with no expiration date."))
        // A term counted from each vehicle's first use, which Glovebox doesn't know.
        assertNull(Bulletins.endedOn("Coverage is for 10 years or 150,000 miles from the date of first use, whichever comes first."))
        // Expired, but no date to say when.
        assertNull(Bulletins.endedOn("All vehicles with Z0Z incomplete have been marked expired in TIS."))
        // A part that no longer works is not a program that ended.
        assertNull(Bulletins.endedOn("As of March 1, 2024, the made-up shutter may no longer open or close as designed."))
        assertNull(Bulletins.endedOn("A made-up summary."))
    }

    @Test
    fun `a date that doesn't exist is no end, rather than a guess or a crash`() {
        assertNull(Bulletins.endedOn("TI: 26TD03 expired on 13/45/2023."))
        assertNull(Bulletins.endedOn("Campaign 26TD04 expired February 30, 2024."))
    }

    @Test
    fun `every bundled summary that says its program expired is read as ended`() {
        // A refresh that brings a new way of saying it fails here, rather than showing an ended program as running.
        val saysEnded = Regex("""\bexpired\b|\bno longer active\b""", RegexOption.IGNORE_CASE)
        val missed = bundled.flatMap { list -> list.bulletins.filter { saysEnded.containsMatchIn(it.summary) && it.endedOn == null }.map { "${list.model} ${it.number}" } }
        assertEquals(emptyList<String>(), missed)
        // The lists as of 2026-10-24 have 17; a refresh may add to them.
        assertTrue(bundled.sumOf { list -> list.bulletins.count { it.endedOn != null } } >= 17)
    }
}
