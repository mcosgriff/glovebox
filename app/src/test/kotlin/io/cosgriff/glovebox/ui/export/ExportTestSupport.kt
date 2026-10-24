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

/**
 * Android's [PdfDocument] draws through Skia's PDF backend, which Robolectric doesn't carry: on the JVM every page
 * would fail with "document is closed". This stands in for it in tests. Each page gets a canvas that keeps the text
 * and pictures drawn on it, and [PdfDocument.writeTo] writes them out as plain lines, a page at a time:
 *
 *     --- page 1 (612 x 792)
 *     <each piece of text, as drawn>
 *     [picture 40 x 30]
 *
 * So a test reads back what [PdfExport] put on each page, and where the pages break, without a PDF renderer.
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

    override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
        drawn += text
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
