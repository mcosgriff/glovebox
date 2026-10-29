package io.cosgriff.glovebox.ui.settings

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import io.cosgriff.glovebox.domain.InvoiceWord
import io.cosgriff.glovebox.domain.PdfWord
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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
    fun readInvoice(context: Context, bytes: ByteArray): List<InvoiceWord>? = guarded(TIMEOUT_MILLIS) { readNow(context, bytes) }

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
