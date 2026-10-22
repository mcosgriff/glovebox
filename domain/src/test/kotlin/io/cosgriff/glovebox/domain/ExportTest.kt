package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportTest {
    private val text = ExportText(EnglishExportWords)

    private val today = LocalDate.of(2026, 10, 11)

    // Made-up vehicle and records.
    private val oil = ExportRecord(LocalDate.of(2026, 8, 1), 66_000, listOf("Oil & filter change", "Tire rotation"), "DIY", 6_250, "0W-20, 6.2 qt\nNew gasket", listOf("a.jpg"))
    private val brakes = ExportRecord(LocalDate.of(2025, 3, 15), 52_300, listOf("Front brake pads"), "Corner Garage", 41_000, null, listOf("b.jpg", "c.jpg"))
    private val wipers = ExportRecord(LocalDate.of(2026, 1, 9), 60_100, listOf("Wiper blades"), "DIY", null, null)
    private val truck = ExportVehicle(
        unit = DistanceUnit.MILES,
        title = "2019 Toyota 4Runner SR5",
        nickname = "Old Blue",
        vin = "1M8GDM9AXKP042788",
        plate = "CO · ABC 1234",
        odometer = 68_412,
        soldOn = null,
        records = listOf(brakes, oil, wipers),
        schedule = listOf(
            ExportScheduleLine("Oil & filter", "10,000 mi / 12 mo", LastDone(LocalDate.of(2026, 8, 1), 66_000)),
            ExportScheduleLine("Brake fluid", "36 mo", null),
        ),
        recalls = listOf(ExportRecall("20V-682", "Fuel pump", null), ExportRecall("19V-670", "Steering", LocalDate.of(2026, 2, 2))),
    )

    @Test
    fun `date ranges include both ends`() {
        val custom = ExportDates.Custom(LocalDate.of(2026, 1, 9), LocalDate.of(2026, 8, 1))
        assertTrue(Exports.inRange(LocalDate.of(2026, 1, 9), custom, today))
        assertTrue(Exports.inRange(LocalDate.of(2026, 8, 1), custom, today))
        assertFalse(Exports.inRange(LocalDate.of(2026, 8, 2), custom, today))
        assertTrue(Exports.inRange(LocalDate.of(2026, 1, 1), ExportDates.ThisYear, today))
        assertFalse(Exports.inRange(LocalDate.of(2025, 12, 31), ExportDates.ThisYear, today))
        assertTrue(Exports.inRange(LocalDate.of(1999, 1, 1), ExportDates.AllTime, today))
    }

    @Test
    fun `records are narrowed to the dates and put newest first`() {
        val doc = Exports.document(listOf(truck), ExportOptions(dates = ExportDates.ThisYear), today)
        assertEquals(listOf(oil.date, wipers.date), doc.vehicles.single().records.map { it.date })
        assertEquals(2, doc.recordCount)
    }

    @Test
    fun `switching costs and notes off removes both, and the total with them`() {
        val v = Exports.document(listOf(truck), ExportOptions(costsAndNotes = false), today).vehicles.single()
        assertTrue(v.records.all { it.costCents == null && it.notes == null })
        assertEquals(null, v.totalCents)
        assertEquals("3 records", text.summary(v))
    }

    @Test
    fun `the total adds only the records in the export`() {
        val all = Exports.document(listOf(truck), ExportOptions(), today).vehicles.single()
        assertEquals("3 records · \$472.50 total", text.summary(all))
        val year = Exports.document(listOf(truck), ExportOptions(dates = ExportDates.ThisYear), today).vehicles.single()
        assertEquals("2 records · \$62.50 total", text.summary(year))
    }

    @Test
    fun `schedule and recalls go together`() {
        val v = Exports.document(listOf(truck), ExportOptions(scheduleAndRecalls = false), today).vehicles.single()
        assertTrue(v.schedule.isEmpty() && v.recalls.isEmpty())
    }

    @Test
    fun `photos travel only in a PDF, and only when asked for`() {
        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), Exports.document(listOf(truck), ExportOptions(), today).photoFiles)
        assertTrue(Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN), today).photoFiles.isEmpty())
        assertTrue(Exports.document(listOf(truck), ExportOptions(photos = false), today).photoFiles.isEmpty())
        // Photos outside the dates don't count either.
        assertEquals(listOf("a.jpg"), Exports.document(listOf(truck), ExportOptions(dates = ExportDates.ThisYear), today).photoFiles)
    }

    @Test
    fun `file names are safe and say what is inside`() {
        assertEquals("glovebox-4runner-2026-10-11.md", Exports.fileName("4Runner", today, ExportFormat.MARKDOWN))
        assertEquals("glovebox-old-blue-2-2026-10-11.pdf", Exports.fileName("Old Blue #2!", today, ExportFormat.PDF))
        assertEquals("glovebox-all-vehicles-2026-10-11.pdf", Exports.fileName(null, today, ExportFormat.PDF))
        assertEquals("glovebox-vehicle-2026-10-11.pdf", Exports.fileName("???", today, ExportFormat.PDF))
    }

    @Test
    fun `markdown has the vehicle, each record and both tables`() {
        val md = text.markdown(Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertTrue(md.startsWith("# Service history: 2019 Toyota 4Runner SR5\n"))
        assertTrue("- VIN: `1M8GDM9AXKP042788`" in md)
        assertTrue("- License plate: `CO · ABC 1234`" in md)
        assertTrue("- 3 records · \$472.50 total" in md)
        assertTrue("- Exported from Glovebox on Oct 11, 2026" in md)
        // Newest first.
        assertTrue(md.indexOf("### Aug 1, 2026 · 66,000 mi") < md.indexOf("### Jan 9, 2026 · 60,100 mi"))
        assertTrue(md.indexOf("### Jan 9, 2026") < md.indexOf("### Mar 15, 2025 · 52,300 mi"))
        assertTrue("- Oil & filter change\n- Tire rotation\n\nDone by: DIY · Cost: \$62.50\n\n> 0W-20, 6.2 qt\n> New gasket\n" in md)
        assertTrue("Done by: Corner Garage · Cost: \$410.00" in md)
        assertTrue("| Oil & filter | 10,000 mi / 12 mo | Aug 1, 2026 at 66,000 mi |" in md)
        assertTrue("| Brake fluid | 36 mo | Not logged |" in md)
        assertTrue("| 20V-682 | Fuel pump | Open |" in md)
        assertTrue("| 19V-670 | Steering | Marked done Feb 2, 2026 |" in md)
        assertTrue(text.phrase(ExportPhrase.RECALL_NOTE) in md)
    }

    @Test
    fun `a replaced cluster is explained, with the actual mileage`() {
        val swapped = truck.copy(odometer = 2_412, clusterSwap = ClusterSwaps.of(LocalDate.of(2026, 9, 1), oldReading = 67_000, newReading = 1_000))
        val md = text.markdown(Exports.document(listOf(swapped), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertTrue(md.contains("- Odometer: 2,412 mi (highest reading on record)"))
        assertTrue(md.contains("- Actual mileage: 68,412 mi"))
        assertTrue(md.contains("- Instrument cluster: Replaced Sep 1, 2026. Readings from that date are the replacement's, which shows 66,000 mi less"))
        // Records keep the reading that was on the dash that day.
        assertTrue(md.contains("### Aug 1, 2026 · 66,000 mi"))

        val plain = text.markdown(Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertFalse(plain.contains("Actual mileage"))
        assertFalse(plain.contains("Instrument cluster"))
    }

    @Test
    fun `a last done entered afterwards is not passed off as a logged service`() {
        val entered = ExportRecord(LocalDate.of(2024, 3, 1), 41_000, listOf("Coolant"), "DIY", null, null, enteredAsLastDone = true)
        val md = text.markdown(Exports.document(listOf(truck.copy(records = listOf(entered, oil))), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertTrue(md.contains("### Mar 1, 2024 · 41,000 mi\n\n- Coolant\n\n${text.phrase(ExportPhrase.LAST_DONE_NOTE)}\n"))
        assertTrue(md.contains("Done by: DIY"))
    }

    @Test
    fun `a visit is one entry, each record with its own source, lines and cost`() {
        val imported = oil.copy(doneBy = "Corner Garage", notes = null, photoFiles = emptyList(), imported = true, visitId = 4)
        val recall = ExportRecord(oil.date, 66_000, listOf("Recall 20V-682 · Fuel pump"), "Corner Garage", null, null, visitId = 4)
        val md = text.markdown(Exports.document(listOf(truck.copy(records = listOf(imported, recall, wipers))), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertEquals(
            "### Aug 1, 2026 · 66,000 mi\n\nOne visit · 2 records\n\n" +
                "**Done by: Corner Garage** · Cost: $62.50\n\n- Oil & filter change\n- Tire rotation\n\n${text.phrase(ExportPhrase.IMPORTED_NOTE)}\n\n" +
                "**Done by: Corner Garage**\n\n- Recall 20V-682 · Fuel pump\n\n" +
                "### Jan 9, 2026",
            md.substringAfter("## Service records\n\n").substringBefore(" · 60,100 mi"),
        )
        // The records still count one by one, and with one cost there is no visit total to repeat it.
        assertTrue(md.contains("- 3 records · $62.50 total"))
        assertFalse(md.contains("Visit total"))
    }

    @Test
    fun `a visit whose records disagree says each one's own reading, and adds up its costs`() {
        val a = wipers.copy(costCents = 2_000, visitId = 1)
        val b = wipers.copy(odometer = 60_112, items = listOf("Cabin air filter"), costCents = 3_500, visitId = 1)
        val doc = Exports.document(listOf(truck.copy(records = listOf(a, b))), ExportOptions(format = ExportFormat.MARKDOWN), today)
        val visit = doc.vehicles.single().entries.single()
        assertEquals("Jan 9, 2026", text.visitHeading(visit, DistanceUnit.MILES))
        assertEquals("60,112 mi", text.visitOwn(b, visit, DistanceUnit.MILES))
        assertEquals(5_500L, text.visitTotal(visit))
        assertTrue(text.markdown(doc).contains("Visit total: $55.00"))
    }

    @Test
    fun `a visit cut down to one record by the dates is no visit`() {
        val old = brakes.copy(visitId = 2)
        val doc = Exports.document(listOf(truck.copy(records = listOf(old, wipers.copy(visitId = 2)))), ExportOptions(dates = ExportDates.ThisYear), today)
        assertEquals(listOf(1), doc.vehicles.single().entries.map { it.size })
    }

    @Test
    fun `an imported record says so`() {
        val imported = oil.copy(imported = true)
        val md = text.markdown(Exports.document(listOf(truck.copy(records = listOf(imported, wipers))), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertEquals(1, Regex(Regex.escape(text.phrase(ExportPhrase.IMPORTED_NOTE))).findAll(md).count())
        assertTrue(md.contains("Done by: DIY · Cost: $62.50\n\n${text.phrase(ExportPhrase.IMPORTED_NOTE)}"))
    }

    @Test
    fun `markdown leaves out what was switched off`() {
        val md = text.markdown(
            Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN, costsAndNotes = false, scheduleAndRecalls = false), today),
        )
        assertFalse("Cost:" in md)
        assertFalse("gasket" in md)
        assertFalse("## Maintenance schedule" in md)
        assertFalse("## Recalls" in md)
    }

    @Test
    fun `markdown says so when a period has no records`() {
        val dates = ExportDates.Custom(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val md = text.markdown(Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN, dates = dates), today))
        assertTrue("No service records in this period." in md)
        assertTrue("- Period: Jan 1, 2020 to Dec 31, 2020" in md)
    }

    @Test
    fun `text the user typed can't break the markdown`() {
        val odd = truck.copy(
            nickname = "# *Blue*",
            records = listOf(oil.copy(items = listOf("Pads | rotors"), notes = "see [receipt]")),
            schedule = listOf(ExportScheduleLine("Belt | tensioner", "60,000 mi", null)),
        )
        val md = text.markdown(Exports.document(listOf(odd), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertTrue("- Nickname: \\# \\*Blue\\*" in md)
        assertTrue("- Pads \\| rotors" in md)
        assertTrue("> see \\[receipt\\]" in md)
        assertTrue("| Belt \\| tensioner | 60,000 mi | Not logged |" in md)
    }

    @Test
    fun `several vehicles are written one after another`() {
        val second = truck.copy(title = "2011 Volkswagen GTI", nickname = null, vin = null, records = emptyList(), schedule = emptyList(), recalls = emptyList())
        val md = text.markdown(Exports.document(listOf(truck, second), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assertTrue(md.indexOf("# Service history: 2019 Toyota 4Runner SR5") < md.indexOf("\n---\n"))
        assertTrue(md.indexOf("\n---\n") < md.indexOf("# Service history: 2011 Volkswagen GTI"))
    }

    @Test
    fun `what it takes is its own section, and can be left out`() {
        val takes = listOf("Engine oil" to "0W-20 \u00b7 6.6 qt \u00b7 filter 04152-YZZA1", "Tires" to "P265/70R17 \u00b7 32 psi")
        val with = Exports.document(listOf(truck.copy(whatItTakes = takes)), ExportOptions(format = ExportFormat.MARKDOWN), today)
        val md = text.markdown(with)
        assert("## What it takes" in md)
        assert("| Engine oil | 0W-20 \u00b7 6.6 qt \u00b7 filter 04152-YZZA1 |" in md) { md }
        assert(text.phrase(ExportPhrase.TAKES_NOTE) in md)
        // Before the records, where someone taking the car over looks first.
        assert(md.indexOf("## What it takes") < md.indexOf("## Service records"))
        val without = Exports.document(listOf(truck.copy(whatItTakes = takes)), ExportOptions(format = ExportFormat.MARKDOWN, whatItTakes = false), today)
        assert("What it takes" !in text.markdown(without))
        assert("What it takes" !in text.markdown(Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN), today)))
    }

    @Test
    fun `documents on file are listed by name only, and can be left out`() {
        val papers = listOf(ExportPaper("Title", "Title | 2019", LocalDate.of(2019, 6, 1)), ExportPaper("Other", "Window sticker", null))
        val md = text.markdown(Exports.document(listOf(truck.copy(documents = papers)), ExportOptions(format = ExportFormat.MARKDOWN), today))
        assert("## Documents on file" in md) { md }
        assert("| Title | Title \\| 2019 | Jun 1, 2019 |" in md) { md }
        assert("| Other | Window sticker |  |" in md) { md }
        assert(text.phrase(ExportPhrase.DOCUMENTS_NOTE) in md)
        assert(md.indexOf("## Documents on file") < md.indexOf("## Service records"))
        val without = Exports.document(listOf(truck.copy(documents = papers)), ExportOptions(format = ExportFormat.MARKDOWN, documents = false), today)
        assert("Documents on file" !in text.markdown(without))
        assert("Documents on file" !in text.markdown(Exports.document(listOf(truck), ExportOptions(format = ExportFormat.MARKDOWN), today)))
    }
}
