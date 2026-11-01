package io.cosgriff.glovebox.ui.settings

import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.contentstream.PDFGraphicsStreamEngine
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import io.cosgriff.glovebox.domain.InvoiceMark
import io.cosgriff.glovebox.domain.InvoiceWord
import io.cosgriff.glovebox.domain.PdfWord
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** A shop's emailed invoice as read (#484): its words, and its pages' small filled marks (#572). */
class PdfInvoice(val words: List<InvoiceWord>, val marks: List<InvoiceMark>)

/**
 * The words of a PDF with where each starts on its page, for `:domain` to read (CarfaxReport). The PDF is
 * read from memory and never written anywhere; PDFBox is told to keep its working data in memory too.
 */
object PdfWords {
    /** How long a PDF may take to read; a Carfax report takes well under a second. */
    private const val TIMEOUT_MILLIS = 20_000L

    /** PDFBox recurses through a document's structure; a deep one needs more than a thread's usual stack. */
    private const val STACK_BYTES = 16L * 1024 * 1024

    /**
     * Null when it isn't a PDF that can be opened (damaged, or encrypted: its decryption isn't bundled), or one that
     * can't be read in time or without running out of stack or memory: PDFBox has looped and overflowed on crafted files.
     */
    fun read(context: Context, bytes: ByteArray): List<PdfWord>? =
        guarded(TIMEOUT_MILLIS) { readNow(context, bytes)?.map { PdfWord(it.page, it.left, it.bottom, it.text) } }

    /**
     * The words of a shop's emailed invoice with their boxes, for [io.cosgriff.glovebox.domain.InvoiceReader] (#484).
     * A PDF's text layer is exact, so no word carries a confidence. Null as for [read]; empty for a scan, which has
     * no text layer.
     */
    fun readInvoice(context: Context, bytes: ByteArray): PdfInvoice? = guarded(TIMEOUT_MILLIS) { readInvoiceNow(context, bytes) }

    /**
     * [block] on a thread of its own: null if it throws anything, errors included, or isn't done in [timeoutMillis].
     * A thread that never finishes is left to the daemon it is; nothing waits on it.
     */
    internal fun <T : Any> guarded(timeoutMillis: Long, block: () -> T?): T? {
        val task = FutureTask(block)
        val thread = Thread(null, task, "pdf-words", STACK_BYTES).apply { isDaemon = true }
        thread.start()
        return try {
            task.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            thread.interrupt()
            null
        } catch (_: ExecutionException) {
            null
        }
    }

    private fun readNow(context: Context, bytes: ByteArray): List<InvoiceWord>? = try {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(bytes, "", null, null, MemoryUsageSetting.setupMainMemoryOnly()).use { doc ->
            if (doc.isEncrypted) return null
            Stripper().apply { getText(doc) }.words
        }
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    /** The words, then a second pass over each page for its small filled marks (#572). */
    private fun readInvoiceNow(context: Context, bytes: ByteArray): PdfInvoice? = try {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(bytes, "", null, null, MemoryUsageSetting.setupMainMemoryOnly()).use { doc ->
            if (doc.isEncrypted) return null
            val words = Stripper().apply { getText(doc) }.words
            val marks = doc.pages.flatMapIndexed { i, page -> Marks(page, i + 1).apply { processPage(page) }.found }
            PdfInvoice(words, marks)
        }
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    /**
     * A page's small filled shapes with their fill colour (#572): a health report marks each item red, yellow or
     * green this way. Only the shape and colour are kept, in the text's units from the page's top left; what they
     * mean is `:domain`'s to read. Text, images and shadings are passed over.
     */
    private class Marks(page: PDPage, private val number: Int) : PDFGraphicsStreamEngine(page) {
        val found = mutableListOf<InvoiceMark>()
        private val box = page.cropBox
        private var minX = Float.MAX_VALUE
        private var minY = Float.MAX_VALUE
        private var maxX = -Float.MAX_VALUE
        private var maxY = -Float.MAX_VALUE
        private var current = PointF()

        private fun add(x: Float, y: Float) {
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
        }

        private fun reset() {
            minX = Float.MAX_VALUE
            minY = Float.MAX_VALUE
            maxX = -Float.MAX_VALUE
            maxY = -Float.MAX_VALUE
        }

        private fun fill() {
            val w = maxX - minX
            val h = maxY - minY
            val colour = graphicsState.nonStrokingColor
            if (w in MARK_SIZE && h in MARK_SIZE && !colour.isPattern) {
                val rgb = colour.toRGB() and 0xFFFFFF
                found += InvoiceMark(number, minX - box.lowerLeftX, box.upperRightY - maxY, maxX - box.lowerLeftX, box.upperRightY - minY, rgb)
            }
            reset()
        }

        override fun appendRectangle(p0: PointF, p1: PointF, p2: PointF, p3: PointF) {
            listOf(p0, p1, p2, p3).forEach { add(it.x, it.y) }
            current = p0
        }

        override fun moveTo(x: Float, y: Float) {
            add(x, y)
            current = PointF(x, y)
        }

        override fun lineTo(x: Float, y: Float) = moveTo(x, y)

        override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            add(x1, y1)
            add(x2, y2)
            moveTo(x3, y3)
        }

        override fun getCurrentPoint(): PointF = current

        override fun closePath() = Unit

        override fun endPath() = reset()

        override fun strokePath() = reset()

        override fun fillPath(windingRule: Path.FillType) = fill()

        override fun fillAndStrokePath(windingRule: Path.FillType) = fill()

        override fun clip(windingRule: Path.FillType) = Unit

        override fun drawImage(pdImage: PDImage) = Unit

        override fun shadingFill(shadingName: COSName) = Unit

        companion object {
            /** A mark is a few points across: a bullet, a dot, a small square. Rules and boxes are bigger. */
            val MARK_SIZE = 3f..24f
        }
    }

    private class Stripper : PDFTextStripper() {
        val words = mutableListOf<InvoiceWord>()

        init {
            sortByPosition = true
        }

        override fun writeString(text: String, positions: MutableList<TextPosition>) {
            val word = StringBuilder()
            var left = 0f
            var right = 0f
            var top = 0f
            var bottom = 0f
            fun flush() {
                if (word.isNotEmpty()) words += InvoiceWord(word.toString(), currentPageNo, left, top, right, bottom)
                word.setLength(0)
            }
            positions.forEach { p ->
                val u = p.unicode
                if (u.isBlank()) {
                    flush()
                } else {
                    // The position is the baseline's; the glyph stands its height above it. Some fonts give no height.
                    val height = p.heightDir.takeIf { it > 0f } ?: p.fontSizeInPt
                    if (word.isEmpty()) {
                        left = p.xDirAdj
                        top = p.yDirAdj - height
                        bottom = p.yDirAdj
                    }
                    right = p.xDirAdj + p.widthDirAdj
                    top = minOf(top, p.yDirAdj - height)
                    bottom = maxOf(bottom, p.yDirAdj)
                    word.append(u)
                }
            }
            flush()
        }
    }
}
