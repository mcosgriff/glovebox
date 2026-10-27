package io.cosgriff.glovebox.ui.export

import android.graphics.Bitmap
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ExportDocument
import io.cosgriff.glovebox.domain.ExportJob
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.ExportPhrase
import io.cosgriff.glovebox.domain.ExportPaper
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportScheduleLine
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.Exports
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.ReportFacts
import io.cosgriff.glovebox.domain.ReportStatus
import io.cosgriff.glovebox.domain.Reports
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.longDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * The PDF report: an infographic page, the log on as many pages as it takes with every page numbered, a page a
 * photo, and labels on the chart that keep clear of each other. Drawn through [ShadowPdfDocument].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, sdk = [35], shadows = [ShadowPdfDocument::class])
class ReportRendererTest {
    private val res get() = AppText.res
    private val today = LocalDate.of(2026, 10, 24)
    private val renderer = ReportRenderer(ReportFonts(RuntimeEnvironment.getApplication()))

    private val records = SampleReport.records

    private fun record(date: LocalDate, odometer: Int, diy: Boolean, vararg lines: Pair<String, ServiceGroup>) = SampleReport.record(date, odometer, diy, *lines)

    private fun vehicle(records: List<ExportRecord> = this.records, title: String = "2019 Toyota 4Runner SR5") = SampleReport.vehicle(today, records, title)

    private fun doc(vararg vehicles: ExportVehicle, options: ExportOptions = ExportOptions()) = Exports.document(vehicles.toList(), options, today)

    /** Each page's text as drawn, and the canvas positions of the text drawn whole. */
    private fun write(doc: ExportDocument, photo: (String) -> Bitmap? = { null }): List<List<String>> {
        val out = ByteArrayOutputStream()
        runBlocking { renderer.writePdf(doc, out) { photo(it) } }
        return pages(out.toByteArray())
    }

    private fun page(n: Int, of: Int) = res.getString(R.string.report_page, n.toString(), of.toString())

    @Test
    fun `page one is the infographic and page two the log, each numbered`() {
        val pages = write(doc(vehicle()))
        assertEquals(2, pages.size)
        val (one, two) = pages
        listOf(
            res.getString(R.string.report_brand), "2019 Toyota 4Runner SR5", "JTEBU5JR7K5900001", res.getString(R.string.report_status_current),
            "68,412", res.getString(R.string.report_tile_services), "18,212", "1 / 2",
            res.getString(R.string.report_category_oil), res.getString(R.string.report_category_fluids), "78.4K",
            "Brake inspection", res.getString(R.string.report_due_soon), res.getString(R.string.report_due_just_done),
            res.getString(R.string.report_recall_open), res.getString(R.string.report_who_diy), page(1, 2),
        ).forEach { assertTrue("missing from page 1: $it", one.any { line -> line.contains(it) }) }
        listOf(
            res.getString(R.string.report_log_title, "2019 Toyota 4Runner SR5"), res.getString(R.string.report_every_service), "Corner Garage",
            res.getString(R.string.report_who_owner), "$42.00", res.getString(R.string.report_schedule_title), "Diff fluid", "80,200 mi",
            res.getString(R.string.report_per_month_mi), "Made-up title", page(2, 2),
            res.getString(R.string.report_footer, today.format(longDate)),
        ).forEach { assertTrue("missing from page 2: $it", two.any { line -> line.contains(it) }) }
    }

    @Test
    fun `the Now marker hides the tick under it, and a next-due label beside it moves to the right of its dot`() {
        val canvas = RecordingCanvas()
        val pages = renderer.layout(doc(vehicle()))
        renderer.draw(pages.first(), canvas, 1, pages.size, null)
        val drawn = canvas.at
        fun at(text: String) = drawn.single { it.first == text }
        assertFalse("70K is under Now", drawn.any { it.first == "70K mi" })
        assertTrue(drawn.any { it.first == "80K mi" })
        assertTrue(drawn.any { it.first == "Now 68.4K" })
        // Brakes are next due at 69,800, just past Now: its label sits level with the lane, not above it like Tires'.
        val brakes = at("69.8K").third - at(res.getString(R.string.report_category_brakes)).third
        val tires = at("73.4K").third - at(res.getString(R.string.report_category_tires)).third
        assertEquals(17f, brakes - tires, 1.5f)
        assertTrue(at("69.8K").second > at("Now 68.4K").second)
    }

    @Test
    fun `a long log runs onto continued pages, and every footer counts all the pages`() {
        val many = (1..40).map { i -> record(LocalDate.of(2024, 1, 1).plusDays(i * 9L), 30_000 + i * 900, i % 2 == 0, "Oil & filter" to ServiceGroup.OIL_ENGINE) }
        val pages = write(doc(vehicle(many)))
        assertTrue(pages.size >= 3)
        pages.forEachIndexed { i, p -> assertTrue(p.toString(), p.contains(page(i + 1, pages.size))) }
        assertTrue(pages[2].contains(res.getString(R.string.report_log_continued, "2019 Toyota 4Runner SR5")))
        // Each record is drawn once, on one page.
        assertEquals(40, pages.flatten().count { it == res.getString(R.string.report_who_owner) || it == "Corner Garage" })
    }

    @Test
    fun `each photo gets a page of its own with its caption, and one that can't be read says so`() {
        val withPhotos = records.mapIndexed { i, r -> if (i == 3) r.copy(photoFiles = listOf("wide.enc", "gone.enc")) else r }
        val pages = write(doc(vehicle(withPhotos))) { if (it == "wide.enc") Bitmap.createBitmap(2_000, 1_000, Bitmap.Config.ARGB_8888) else null }
        assertEquals(4, pages.size)
        // Inside three-quarter-inch margins: 672 design pixels wide, at 0.75 on the PDF page.
        assertTrue(pages[2].toString(), pages[2].contains("[picture 672 x 336]"))
        val caption = res.getString(R.string.report_caption, LocalDate.of(2026, 10, 10).format(longDate), "68,412 mi", "Oil & filter")
        assertTrue(pages[2].contains(caption))
        assertTrue(pages[3].contains(res.getString(R.string.export_photo_unreadable)))
        assertTrue(pages[0].any { it.contains(res.getString(R.string.report_footer_first_photos, "2")) })
    }

    @Test
    fun `each vehicle has its own report, numbered through the whole file`() {
        val pages = write(doc(vehicle(), vehicle(emptyList(), title = "2013 Volkswagen GTI")))
        assertEquals(4, pages.size)
        assertTrue(pages[2].contains("2013 Volkswagen GTI"))
        assertTrue(pages[3].contains(res.getString(R.string.report_no_service)))
        assertTrue(pages[3].contains(page(4, 4)))
    }

    @Test
    fun `leaving the schedule out leaves out what is due, the table and the status`() {
        val drawn = write(doc(vehicle(), options = ExportOptions(scheduleAndRecalls = false))).flatten()
        assertFalse(drawn.contains(res.getString(R.string.report_due_next)))
        assertFalse(drawn.contains(res.getString(R.string.report_schedule_title)))
        assertFalse(drawn.contains(res.getString(R.string.report_status_current)))
        assertFalse(drawn.contains(res.getString(R.string.report_recalls_title)))
        assertTrue(drawn.contains(res.getString(R.string.report_chart_title)))
    }

    @Test
    fun `a visit is one entry under its date, with each record's notes and the visit's total`() {
        val visit = listOf(
            record(LocalDate.of(2026, 10, 10), 68_412, false, "Oil & filter" to ServiceGroup.OIL_ENGINE).copy(visitId = 7, notes = "Made-up note"),
            record(LocalDate.of(2026, 10, 10), 68_412, false, "Tire rotation" to ServiceGroup.TIRES).copy(visitId = 7, imported = true),
        )
        val log = write(doc(vehicle(records.take(3) + visit)))[1]
        assertTrue(log.toString(), log.any { it.contains(res.getString(R.string.export_visit, AppExportWords.records(2))) })
        assertTrue(log.any { it.contains(res.getString(R.string.export_visit_total, "$84.00")) })
        assertTrue(log.any { it.contains("Made-up note") })
        assertTrue(log.any { it.contains(res.getString(R.string.export_imported_note).substringBefore(';')) })
    }

    // Made-up invoice: plugs and coils priced as one job, the oil change priced alone, the rotation free.
    private val priced = record(
        LocalDate.of(2026, 10, 10), 68_412, false,
        "Spark plugs" to ServiceGroup.OIL_ENGINE, "Tire rotation" to ServiceGroup.TIRES, "Ignition coils" to ServiceGroup.OIL_ENGINE, "Oil & filter change" to ServiceGroup.OIL_ENGINE,
    ).copy(
        costCents = 49_731,
        jobs = listOf(ExportJob("Remove & Replace Spark Plugs and Ignition Coils", 18_000, 21_240), ExportJob("Oil & filter change", 3_995, 2_850)),
        jobOf = listOf(0, null, 0, 1),
    )

    @Test
    fun `a priced record has its labor and parts after the cost, and only a job of one line puts its price on the chip`() {
        val canvas = RecordingCanvas()
        val pages = renderer.layout(doc(vehicle(records.take(3) + priced)))
        renderer.draw(pages[1], canvas, 2, pages.size, null)
        fun at(text: String) = canvas.at.single { it.first == text }
        val labor = res.getString(R.string.export_job_labor, "$219.95")
        val parts = res.getString(R.string.export_job_parts, "$240.90")
        // On the cost's line, after it.
        assertEquals(at("$497.31").third, at(labor).third, 0.01f)
        assertTrue(at(labor).second > at("$497.31").second)
        // Then the parts, on the same line or, past the log's width, wrapped onto the next.
        assertTrue(at(parts).third > at(labor).third || at(parts).second > at(labor).second)
        // The oil change's labor and parts together, on its chip after its name.
        assertEquals(at("Oil & filter change").third, at("$68.45").third, 1f)
        assertTrue(at("$68.45").second > at("Oil & filter change").second)
        // The plugs and coils share a job: neither chip carries its $392.40, nor any part of it.
        assertFalse(canvas.drawn.any { it.contains("$392.40") || it.contains("$180.00") || it.contains("$212.40") })
        // A record with no prices says nothing about them.
        assertEquals(1, canvas.drawn.count { it.startsWith(res.getString(R.string.export_job_labor, "")) })
    }

    @Test
    fun `a record with parts priced and no labor entered shows no labor`() {
        val partsOnly = priced.copy(jobs = listOf(ExportJob(null, null, 21_240), ExportJob(null, null, 2_850)))
        val drawn = write(doc(vehicle(records.take(3) + partsOnly)))[1]
        assertTrue(drawn.contains(res.getString(R.string.export_job_parts, "$240.90")))
        assertTrue(drawn.contains("$28.50"))
        assertFalse(drawn.toString(), drawn.any { it.startsWith(res.getString(R.string.export_job_labor, "")) })
    }

    @Test
    fun `switching costs off leaves no price on the report`() {
        val drawn = write(doc(vehicle(records.take(3) + priced), options = ExportOptions(costsAndNotes = false)))[1]
        assertTrue(drawn.contains("Oil & filter change"))
        assertFalse(drawn.toString(), drawn.any { it.contains("$") })
    }

    @Test
    fun `a meta line too long for the log wraps instead of running past it`() {
        val long = priced.copy(doneBy = "Made-up Automotive Service Center of the Valley", photoFiles = listOf("a.enc", "b.enc"))
        val canvas = RecordingCanvas()
        val pages = renderer.layout(doc(vehicle(records.take(3) + long)))
        renderer.draw(pages[1], canvas, 2, pages.size, null)
        fun at(text: String) = canvas.at.single { it.first == text }
        val meta = listOf(long.doneBy, "$497.31", res.getString(R.string.export_job_labor, "$219.95"), res.getString(R.string.export_job_parts, "$240.90")).map(::at)
        // Onto a second line, and nothing starts past the log, which ends 432 design pixels from the margin beside the average a month.
        assertTrue(meta.last().third > meta.first().third)
        meta.forEach { assertTrue(it.toString(), it.second < 48f + 432f) }
    }

    @Test
    fun `items past due are counted in the status, and a snoozed one is shown as snoozed`() {
        val snoozed = SampleReport.items.map { if (it.id == 3L) it.copy(snooze = Snooze(untilOdometer = 85_000)) else it }
        val readings = SampleReport.entries.map { OdometerReading(it.date, it.odometer) } + OdometerReading(today, 79_000)
        val statuses = VehicleReminders.statuses(snoozed, SampleReport.entries, listOf(readings.last()), today, LeadTime(1_500))
        val v = vehicle().copy(odometer = 79_000, report = ReportFacts(statuses, SampleReport.categories, readings))
        val overdue = (Reports.build(doc(v)).single().status as ReportStatus.Overdue).count
        val first = write(doc(v)).first()
        assertTrue(first.toString(), first.any { it.contains(res.getQuantityString(R.plurals.report_status_overdue, overdue, overdue.toString())) })
        assertTrue(first.contains(res.getString(R.string.report_due_overdue)))
        assertTrue(first.contains(res.getString(R.string.report_due_snoozed)))
    }

    @Test
    fun `a long name in what's due next wraps onto a second line instead of being cut short`() {
        val long = "Change brake fluid (at 3 years, then every 2 years)"
        val renamed = SampleReport.items.map { if (it.id == 3L) it.copy(name = long) else it }
        val statuses = VehicleReminders.statuses(renamed, SampleReport.entries, emptyList(), today, LeadTime(1_500))
        val v = vehicle().copy(report = ReportFacts(statuses, SampleReport.categories, SampleReport.entries.map { OdometerReading(it.date, it.odometer) }))
        val first = write(doc(v)).first()
        assertTrue(first.toString(), first.joinToString(" ").replace(Regex("\\s+"), " ").contains(long))
        assertFalse(first.toString(), first.any { it.contains("…") })
    }

    @Test
    fun `a table that runs past the page repeats its header on the next`() {
        val papers = (1..60).map { ExportPaper("Receipt", "Made-up paper $it", LocalDate.of(2026, 1, 1)) }
        val pages = write(doc(vehicle().copy(documents = papers)))
        val header = AppExportWords.phrase(ExportPhrase.TYPE)
        val withHeader = pages.drop(1).count { page -> page.contains(header) && page.any { it.startsWith("Made-up paper") } }
        assertTrue(pages.joinToString("\n"), withHeader >= 2)
        assertEquals(60, pages.flatten().count { it.startsWith("Made-up paper") })
    }

    @Test
    fun `a preview page is the same drawing at thumbnail size`() {
        val pages = renderer.layout(doc(vehicle()))
        val bitmap = renderer.bitmap(pages[1], 2, pages.size, 420)
        assertEquals(420, bitmap.width)
        assertEquals(544, bitmap.height)
    }
}
