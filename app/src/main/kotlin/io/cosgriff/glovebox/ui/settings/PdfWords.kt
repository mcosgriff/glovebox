package io.cosgriff.glovebox.ui.settings

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import io.cosgriff.glovebox.domain.PdfWord
import java.io.IOException

/**
 * The words of a PDF with where each starts on its page, for `:domain` to read (CarfaxReport). The PDF is
 * read from memory and never written anywhere; PDFBox is told to keep its working data in memory too.
 */
object PdfWords {
    /** Null when it isn't a PDF that can be opened (damaged, or encrypted: its decryption isn't bundled). */
    fun read(context: Context, bytes: ByteArray): List<PdfWord>? = try {
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
        val words = mutableListOf<PdfWord>()

        init {
            sortByPosition = true
        }

        override fun writeString(text: String, positions: MutableList<TextPosition>) {
            val word = StringBuilder()
            var x = 0f
            var y = 0f
            fun flush() {
                if (word.isNotEmpty()) words += PdfWord(currentPageNo, x, y, word.toString())
                word.setLength(0)
            }
            positions.forEach { p ->
                val u = p.unicode
                if (u.isBlank()) {
                    flush()
                } else {
                    if (word.isEmpty()) {
                        x = p.xDirAdj
                        y = p.yDirAdj
                    }
                    word.append(u)
                }
            }
            flush()
        }
    }
}
