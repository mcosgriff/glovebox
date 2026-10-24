package io.cosgriff.glovebox.ui.export

import android.graphics.Bitmap
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ExportDocument
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.ExportPaper
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportScheduleLine
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * The PDF export's layout: each vehicle on pages of its own, its details, sections only where there is something
 * to say, records and visits with their photos, a footer on every page, and page breaks that never strand a heading.
 * Drawn through [ShadowPdfDocument], which keeps what each page holds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35], shadows = [ShadowPdfDocument::class])
class PdfExportTest {
    private val res get() = AppText.res
    private val today = LocalDate.of(2026, 10, 1)
    private val words = AppExportWords

    private fun record(day: Int, odometer: Int, vararg items: String, cost: Long? = null, notes: String? = null, photos: List<String> = emptyList(), visit: Long? = null, lastDone: Boolean = false, imported: Boolean = false) =
        ExportRecord(LocalDate.of(2025, 1, day), odometer, items.toList(), "Corner Garage", cost, notes, photos, lastDone, imported, visit)

    private fun vehicle(records: List<ExportRecord> = emptyList(), title: String = "2019 Toyota 4Runner") = ExportVehicle(
        title = title, nickname = null, vin = null, unit = DistanceUnit.MILES, odometer = null, soldOn = null, records = records, schedule = emptyList(), recalls = emptyList(),
    )

    private fun write(vararg vehicles: ExportVehicle, photo: (String) -> Bitmap? = { null }): List<List<String>> {
        val out = ByteArrayOutputStream()
        runBlocking { PdfExport { photo(it) }.write(ExportDocument(today, ExportOptions(), vehicles.toList()), out) }
        return pages(out.toByteArray())
    }

    private fun footer(page: Int) = words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.PAGE, page.toString())

    @Test
    fun `a vehicle with everything is laid out section by section, each page with its footer`() {
        val v = ExportVehicle(
            title = "2019 Toyota 4Runner",
            nickname = "Blue",
            vin = "JTEBU5JR7K5900001",
            plate = "CO · ABC 1234",
            unit = DistanceUnit.MILES,
            odometer = 60_000,
            soldOn = LocalDate.of(2026, 9, 1),
            clusterSwap = ClusterSwap(LocalDate.of(2024, 1, 1), offset = 20_000, startReading = 10_000),
            records = listOf(record(2, 41_000, "Oil & filter change", cost = 6_250, notes = "Made-up note", imported = true), record(3, 30_000, "Tire rotation", lastDone = true)),
            schedule = listOf(ExportScheduleLine("Oil change", "Every 5,000 mi", LastDone(LocalDate.of(2025, 1, 2), 41_000)), ExportScheduleLine("Coolant", "Every 100,000 mi", null)),
            recalls = listOf(ExportRecall("99V999000", "MADE UP", completedOn = LocalDate.of(2025, 2, 2)), ExportRecall("99V999001", "ALSO MADE UP", null)),
            whatItTakes = listOf("Oil" to "Made-up grade, 6 qt"),
            documents = listOf(ExportPaper("Title", "Title", LocalDate.of(2019, 5, 1)), ExportPaper("Insurance", "Card", null)),
        )
        val pages = write(v)
        pages.forEachIndexed { i, p -> assertEquals(footer(i + 1), p.last()) }
        val page = pages.flatten().joinToString("\n")
        listOf(
            "2019 Toyota 4Runner", "Blue", "JTEBU5JR7K5900001", "CO · ABC 1234", "Made-up note", "99V999000", "99V999001", "Oil change", "Coolant",
            "Made-up grade, 6 qt", "Card", words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.WHAT_IT_TAKES), words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.DOCUMENTS_ON_FILE),
            words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.MAINTENANCE_SCHEDULE), words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.RECALLS),
            words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.IMPORTED_NOTE), words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.LAST_DONE_NOTE),
            words.money(6_250), footer(1), words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.EXPORTED_ON, words.date(today)),
        ).forEach { assertTrue("missing: $it", page.contains(it)) }
    }

    @Test
    fun `with nothing to say a vehicle says it has no records and has no empty sections`() {
        val page = write(vehicle()).single().joinToString("\n")
        assertTrue(page.contains(words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.NO_RECORDS)))
        assertTrue(!page.contains(words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.MAINTENANCE_SCHEDULE)))
        assertTrue(!page.contains(words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.RECALLS)))
    }

    @Test
    fun `an export of no vehicles is still one page with its footer`() {
        val pages = write()
        assertEquals(1, pages.size)
        assertEquals(listOf(words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.EXPORTED_ON, words.date(today)), footer(1)), pages.single())
    }

    @Test
    fun `each vehicle starts its own page, and a long history runs on with every page numbered`() {
        val many = (1..28).map { record(it, 1_000 * it, "Oil & filter change", "Tire rotation", cost = 100L * it, notes = "Note $it") }
        val pages = write(vehicle(many), vehicle(title = "2013 Volkswagen GTI"))
        assertTrue(pages.size >= 3)
        pages.forEachIndexed { i, page -> assertEquals(footer(i + 1), page.last()) }
        assertTrue(pages.last().contains("2013 Volkswagen GTI"))
        // A record's heading never ends a page: the line after each heading is on the same page.
        pages.forEach { page ->
            val i = page.indexOfLast { it.startsWith("Jan ") }
            if (i >= 0) assertTrue(page.drop(i + 1).any { it.startsWith("•") })
        }
    }

    @Test
    fun `a visit is one entry with each record's own lines, and its total`() {
        val visit = listOf(record(5, 41_000, "Oil & filter change", cost = 5_000, visit = 9, photos = listOf("a.enc")), record(5, 41_000, "Wiper blades", cost = 2_500, visit = 9, notes = "Made-up", imported = true, lastDone = true))
        val page = write(vehicle(visit)) { Bitmap.createBitmap(2_000, 1_000, Bitmap.Config.ARGB_8888) }.flatten().joinToString("\n")
        assertTrue(page.contains(words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.VISIT, words.records(2))))
        assertTrue(page.contains(words.phrase(io.cosgriff.glovebox.domain.ExportPhrase.VISIT_TOTAL, words.money(7_500))))
        assertTrue(page.contains("Wiper blades"))
        // Scaled down to a third of a page at most.
        assertTrue(page, page.contains("[picture 452 x 226]"))
    }

    @Test
    fun `a photo is drawn under its record, scaled to fit, and one that can't be read is said so`() {
        val records = listOf(record(1, 1_000, "Oil & filter change", photos = listOf("tall.enc", "gone.enc")))
        val page = write(vehicle(records)) { name -> if (name == "tall.enc") Bitmap.createBitmap(300, 3_000, Bitmap.Config.ARGB_8888) else null }.joinToString("\n") { it.joinToString("\n") }
        assertTrue(page, page.contains("[picture 22 x 226]"))
        assertTrue(page.contains(res.getString(R.string.export_photo_unreadable)))
    }
}
