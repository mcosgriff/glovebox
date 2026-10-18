package io.cosgriff.glovebox.ui.export

import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.domain.DistanceUnit
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import io.cosgriff.glovebox.domain.ExportDocument
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.ExportPhrase
import io.cosgriff.glovebox.domain.ExportText
import io.cosgriff.glovebox.domain.Exports
import java.io.OutputStream

/**
 * Lays an [ExportDocument] out as a printable PDF with Android's own [PdfDocument]. The document
 * arrives filtered and ordered, so this only decides where things go on the page.
 */
class PdfExport(private val loadPhoto: suspend (fileName: String) -> Bitmap?) {
    private val words = ExportText(AppExportWords)

    private val title = paint(20f, bold = true)
    private val heading = paint(13f, bold = true)
    private val strong = paint(10.5f, bold = true)
    private val body = paint(10.5f)
    private val mono = paint(10.5f).apply { typeface = Typeface.MONOSPACE }
    private val small = paint(9f).apply { color = MUTED }
    private val rule = Paint().apply {
        color = RULE
        strokeWidth = 0.75f
    }

    private lateinit var pdf: PdfDocument
    private lateinit var exportedLine: String
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f
    private val canvas: Canvas get() = page!!.canvas

    suspend fun write(doc: ExportDocument, out: OutputStream) {
        pdf = PdfDocument()
        exportedLine = words.phrase(ExportPhrase.EXPORTED_ON, words.date(doc.exportedOn))
        try {
            doc.vehicles.forEach { vehicle(it, doc) }
            if (doc.vehicles.isEmpty()) newPage()
            finishPage()
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    private suspend fun vehicle(v: ExportVehicle, doc: ExportDocument) {
        newPage() // each vehicle starts its own page, so one can be handed over on its own
        text(words.phrase(ExportPhrase.SERVICE_HISTORY), small)
        text(v.title, title, after = 6f)
        v.nickname?.let { labelled(words.phrase(ExportPhrase.NICKNAME), it) }
        v.vin?.let { labelled(words.phrase(ExportPhrase.VIN), it, mono) }
        v.plate?.let { labelled(words.phrase(ExportPhrase.PLATE), it, mono) }
        v.odometer?.let { labelled(words.phrase(ExportPhrase.ODOMETER), words.phrase(ExportPhrase.ODOMETER_HIGHEST, words.distance(it, v.unit))) }
        Exports.actualMileage(v)?.let { labelled(words.phrase(ExportPhrase.ACTUAL_MILEAGE), words.distance(it, v.unit)) }
        v.clusterSwap?.let { labelled(words.phrase(ExportPhrase.INSTRUMENT_CLUSTER), words.clusterNote(it, v.unit)) }
        v.soldOn?.let { labelled(words.phrase(ExportPhrase.SOLD), words.date(it)) }
        labelled(words.phrase(ExportPhrase.PERIOD), words.datesLabel(doc.options.dates, doc.exportedOn))
        labelled(words.phrase(ExportPhrase.RECORDS), words.summary(v))
        gap(10f)

        if (v.whatItTakes.isNotEmpty()) {
            section(words.phrase(ExportPhrase.WHAT_IT_TAKES))
            table(listOf("", ""), v.whatItTakes.map { (label, line) -> listOf(label, line) }, fractions = listOf(0.3f, 0.7f))
            text(words.phrase(ExportPhrase.TAKES_NOTE), small, before = 4f)
        }

        if (v.documents.isNotEmpty()) {
            section(words.phrase(ExportPhrase.DOCUMENTS_ON_FILE))
            table(listOf(words.phrase(ExportPhrase.TYPE), words.phrase(ExportPhrase.NAME), words.phrase(ExportPhrase.DATE)), v.documents.map { listOf(it.type, it.name, words.paperDate(it)) }, fractions = listOf(0.3f, 0.45f, 0.25f))
            text(words.phrase(ExportPhrase.DOCUMENTS_NOTE), small, before = 4f)
        }

        section(words.phrase(ExportPhrase.SERVICE_RECORDS))
        if (v.records.isEmpty()) text(words.phrase(ExportPhrase.NO_RECORDS), body)
        v.records.forEach { record(it, v.unit) }

        if (v.schedule.isNotEmpty()) {
            section(words.phrase(ExportPhrase.MAINTENANCE_SCHEDULE))
            table(listOf(words.phrase(ExportPhrase.ITEM), words.phrase(ExportPhrase.EVERY), words.phrase(ExportPhrase.LAST_DONE)), v.schedule.map { listOf(it.name, it.every, words.lastDone(it, v.unit)) })
            text(words.phrase(ExportPhrase.SCHEDULE_NOTE), small, before = 4f)
        }
        if (v.recalls.isNotEmpty()) {
            section(words.phrase(ExportPhrase.RECALLS))
            table(listOf(words.phrase(ExportPhrase.CAMPAIGN), words.phrase(ExportPhrase.COMPONENT), words.phrase(ExportPhrase.STATUS)), v.recalls.map { listOf(it.campaign, it.component, words.recallStatus(it)) })
            text(words.phrase(ExportPhrase.RECALL_NOTE), small, before = 4f)
        }
    }

    private suspend fun record(r: ExportRecord, unit: DistanceUnit) {
        val head = "${words.date(r.date)} · ${words.distance(r.odometer, unit)}"
        val lines = r.items.map { "•  $it" }
        // Keep a record's heading with its work, so a page never ends on a bare date.
        room(height(head, strong) + lines.sumOf { height(it, body).toDouble() }.toFloat() + 24f)
        val top = y
        text(head, strong, after = 2f)
        r.costCents?.let {
            val cost = words.money(it)
            canvas.drawText(cost, RIGHT - strong.measureText(cost), top - strong.ascent(), strong)
        }
        lines.forEach { text(it, body) }
        text(if (r.enteredAsLastDone) words.phrase(ExportPhrase.LAST_DONE_NOTE) else words.phrase(ExportPhrase.DONE_BY, r.doneBy), small, before = 2f)
        if (r.imported) text(words.phrase(ExportPhrase.IMPORTED_NOTE), small, before = 2f)
        r.notes?.let { text(it, body, before = 3f, indent = 10f) }
        r.photoFiles.forEach { photo(it) }
        gap(6f)
        room(1f)
        canvas.drawLine(LEFT, y, RIGHT, y, rule)
        gap(8f)
    }

    /** Scaled to the page: as wide as the text at most, and no more than a third of a page tall. */
    private suspend fun photo(fileName: String) {
        val bitmap = loadPhoto(fileName)
        if (bitmap == null) {
            text(words.phrase(ExportPhrase.PHOTO_UNREADABLE), small, before = 3f)
            return
        }
        val scale = minOf(WIDTH / bitmap.width, MAX_PHOTO_HEIGHT / bitmap.height, 1f)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        gap(6f)
        room(h)
        canvas.drawBitmap(bitmap, null, RectF(LEFT, y, LEFT + w, y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        y += h
        bitmap.recycle()
    }

    /** [fractions] share the page's width between the columns; a header of blanks isn't drawn. */
    private fun table(header: List<String>, rows: List<List<String>>, fractions: List<Float> = listOf(0.36f, 0.26f, 0.38f)) {
        val widths = fractions.map { it * WIDTH }
        fun row(cells: List<String>, paint: TextPaint) {
            val layouts = cells.mapIndexed { i, cell -> layout(cell, paint, widths[i] - 8f) }
            val h = layouts.maxOf { it.height } + 6f
            room(h)
            var x = LEFT
            layouts.forEachIndexed { i, l ->
                canvas.withTranslation(x, y + 3f) { l.draw(this) }
                x += widths[i]
            }
            y += h
            canvas.drawLine(LEFT, y, RIGHT, y, rule)
        }
        if (header.any { it.isNotBlank() }) row(header, strong)
        rows.forEach { row(it, body) }
    }

    private fun section(name: String) {
        gap(8f)
        room(60f) // never leave a section heading alone at the foot of a page
        text(name, heading, after = 2f)
        canvas.drawLine(LEFT, y, RIGHT, y, rule)
        gap(8f)
    }

    private fun labelled(label: String, value: String, valuePaint: TextPaint = body) {
        val h = height(value, valuePaint, WIDTH - LABEL_WIDTH)
        room(h)
        canvas.drawText(label, LEFT, y - small.ascent(), small)
        draw(layout(value, valuePaint, WIDTH - LABEL_WIDTH), LEFT + LABEL_WIDTH)
        y += h + 2f
    }

    private fun text(text: String, paint: TextPaint, before: Float = 0f, after: Float = 0f, indent: Float = 0f) {
        val l = layout(text, paint, WIDTH - indent)
        y += before
        // Long notes can be taller than what is left; they move to the next page whole unless they wouldn't fit there either.
        if (l.height <= BOTTOM - TOP) room(l.height.toFloat())
        draw(l, LEFT + indent)
        y += l.height + after
    }

    private fun draw(l: StaticLayout, x: Float) {
        canvas.withTranslation(x, y) { l.draw(this) }
    }

    private fun layout(text: String, paint: TextPaint, width: Float = WIDTH): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
            .build()

    private fun height(text: String, paint: TextPaint, width: Float = WIDTH): Float = layout(text, paint, width).height.toFloat()

    private fun gap(points: Float) {
        y += points
    }

    /** Makes sure [needed] points fit below the cursor, starting a new page if they don't. */
    private fun room(needed: Float) {
        if (y + needed > BOTTOM) newPage()
    }

    private fun newPage() {
        finishPage()
        pageNumber++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        y = TOP
    }

    /** The footer goes on as each page is closed: the export date and the page number. */
    private fun finishPage() {
        val p = page ?: return
        val baseline = PAGE_HEIGHT - 30f
        p.canvas.drawText(exportedLine, LEFT, baseline, small)
        val number = words.phrase(ExportPhrase.PAGE, pageNumber.asNumber())
        p.canvas.drawText(number, RIGHT - small.measureText(number), baseline, small)
        pdf.finishPage(p)
        page = null
    }

    private fun paint(size: Float, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        color = INK
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private companion object {
        // US Letter in points (1/72 inch).
        const val PAGE_WIDTH = 612
        const val PAGE_HEIGHT = 792
        const val LEFT = 54f
        const val RIGHT = PAGE_WIDTH - 54f
        const val TOP = 54f
        const val BOTTOM = PAGE_HEIGHT - 60f
        const val WIDTH = RIGHT - LEFT
        const val LABEL_WIDTH = 70f
        const val MAX_PHOTO_HEIGHT = (BOTTOM - TOP) / 3f

        // Print colours: the canvas's ink, muted and line tokens.
        const val INK = 0xFF17191C.toInt()
        const val MUTED = 0xFF5A5F66.toInt()
        const val RULE = 0xFFDEDCD5.toInt()
    }
}
