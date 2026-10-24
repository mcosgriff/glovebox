package io.cosgriff.glovebox.ui.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.OutputStream
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ExportPaper
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportScheduleLine
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.ReportFacts
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import java.time.LocalDate

/**
 * Android's [PdfDocument] draws through Skia's PDF backend, which Robolectric doesn't carry: on the JVM every page
 * would fail with "document is closed". This stands in for it in tests. Each page gets a canvas that keeps the text
 * and pictures drawn on it, and [PdfDocument.writeTo] writes them out as plain lines, a page at a time:
 *
 *     --- page 1 (612 x 792)
 *     <each piece of text, as drawn>
 *     [picture 40 x 30]
 *
 * So a test reads back what [ReportRenderer] put on each page, and where the pages break, without a PDF renderer.
 */
@Implements(PdfDocument::class)
class ShadowPdfDocument {
    private val pages = mutableListOf<Pair<PdfDocument.PageInfo, RecordingCanvas>>()

    @Implementation
    protected fun __constructor__() = Unit

    @Implementation
    protected fun startPage(info: PdfDocument.PageInfo): PdfDocument.Page {
        val canvas = RecordingCanvas()
        pages += info to canvas
        val make = PdfDocument.Page::class.java.getDeclaredConstructor(Canvas::class.java, PdfDocument.PageInfo::class.java)
        make.isAccessible = true
        return make.newInstance(canvas, info)
    }

    @Implementation
    protected fun finishPage(page: PdfDocument.Page) = Unit

    @Implementation
    protected fun writeTo(out: OutputStream) {
        val text = pages.joinToString("\n") { (info, canvas) ->
            (listOf("--- page ${info.pageNumber} (${info.pageWidth} x ${info.pageHeight})") + canvas.drawn).joinToString("\n")
        }
        out.write(text.toByteArray())
    }

    @Implementation
    protected fun close() = Unit
}

/** A canvas that keeps what is drawn on it: each piece of text, and a line for each picture. */
class RecordingCanvas : Canvas() {
    val drawn = mutableListOf<String>()

    /** Each piece of text drawn whole, with where its baseline starts. */
    val at = mutableListOf<Triple<String, Float, Float>>()

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        drawn += text
        at += Triple(text, x, y)
    }

    override fun drawText(text: String, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
        drawn += text.substring(start, end)
    }

    override fun drawText(text: CharSequence, start: Int, end: Int, x: Float, y: Float, paint: Paint) {
        drawn += text.subSequence(start, end).toString()
    }

    override fun drawText(text: CharArray, index: Int, count: Int, x: Float, y: Float, paint: Paint) {
        drawn += String(text, index, count)
    }

    override fun drawTextRun(text: CharArray, index: Int, count: Int, contextIndex: Int, contextCount: Int, x: Float, y: Float, isRtl: Boolean, paint: Paint) {
        drawn += String(text, index, count)
    }

    override fun drawTextRun(text: CharSequence, start: Int, end: Int, contextStart: Int, contextEnd: Int, x: Float, y: Float, isRtl: Boolean, paint: Paint) {
        drawn += text.subSequence(start, end).toString()
    }

    override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
        drawn += "[picture ${dst.width().toInt()} x ${dst.height().toInt()}]"
    }

    /** A layout draws only the lines inside the clip; with no bitmap behind it, the clip is the whole page and more. */
    override fun getClipBounds(bounds: Rect): Boolean {
        bounds.set(-10_000, -10_000, 10_000, 10_000)
        return true
    }

    override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) = Unit
}

/** The pages [ShadowPdfDocument] wrote, each as the lines drawn on it. */
fun pages(written: ByteArray): List<List<String>> =
    written.toString(Charsets.UTF_8).split(Regex("(?m)^--- page \\d+ \\(612 x 792\\)\\n?")).drop(1).map { page -> page.lines().filter { it.isNotEmpty() } }

/** A made-up vehicle, schedule and records, as on the design's sample report. */
object SampleReport {
    val items = listOf(
        TrackedItem(1, "Oil & filter", Interval(10_000, 12), DueRule.WHICHEVER_FIRST),
        TrackedItem(2, "Tire rotation", Interval(5_000, 6), DueRule.WHICHEVER_FIRST),
        TrackedItem(3, "Brake inspection", Interval(15_000, null), DueRule.MILEAGE_ONLY),
        TrackedItem(4, "Diff fluid", Interval(30_000, null), DueRule.MILEAGE_ONLY),
    )
    private val every = mapOf(1L to "10,000 mi / 12 mo", 2L to "5,000 mi / 6 mo", 3L to "15,000 mi", 4L to "30,000 mi")
    val categories = mapOf(1L to ServiceGroup.OIL_ENGINE, 2L to ServiceGroup.TIRES, 3L to ServiceGroup.BRAKES, 4L to ServiceGroup.FLUIDS_4WD)

    fun record(date: LocalDate, odometer: Int, diy: Boolean, vararg lines: Pair<String, ServiceGroup>, photos: List<String> = emptyList()): ExportRecord =
        ExportRecord(date, odometer, lines.map { it.first }, if (diy) "DIY" else "Corner Garage", 4_200, null, photos, diy = diy, categories = lines.map { it.second })

    val records = listOf(
        record(LocalDate.of(2025, 10, 18), 50_200, false, "Diff fluid" to ServiceGroup.FLUIDS_4WD),
        record(LocalDate.of(2026, 2, 2), 54_800, true, "Brake inspection" to ServiceGroup.BRAKES),
        record(LocalDate.of(2026, 6, 14), 60_100, false, "Oil & filter" to ServiceGroup.OIL_ENGINE, "Tire rotation" to ServiceGroup.TIRES),
        record(LocalDate.of(2026, 10, 10), 68_412, true, "Oil & filter" to ServiceGroup.OIL_ENGINE, "Tire rotation" to ServiceGroup.TIRES),
    )
    val entries = listOf(
        ServiceEntry(1, records[0].date, 50_200, setOf(4)),
        ServiceEntry(2, records[1].date, 54_800, setOf(3)),
        ServiceEntry(3, records[2].date, 60_100, setOf(1, 2)),
        ServiceEntry(4, records[3].date, 68_412, setOf(1, 2)),
    )

    fun vehicle(today: LocalDate, records: List<ExportRecord> = this.records, title: String = "2019 Toyota 4Runner SR5") = ExportVehicle(
        title = title,
        nickname = null,
        vin = "JTEBU5JR7K5900001",
        unit = DistanceUnit.MILES,
        odometer = 68_412,
        soldOn = null,
        records = records,
        schedule = items.map { ExportScheduleLine(it.name, every.getValue(it.id), ServiceLog.lastDone(it.id, entries), it.id) },
        recalls = listOf(ExportRecall("19V-670", "Suspension", LocalDate.of(2026, 1, 3)), ExportRecall("20V-012", "Fuel pump", null)),
        documents = listOf(ExportPaper("Title", "Made-up title", null)),
        specs = "4WD · 4.0L V6",
        report = ReportFacts(VehicleReminders.statuses(items, entries, emptyList(), today, LeadTime(1_500)), categories, entries.map { OdometerReading(it.date, it.odometer) }),
    )
}
