package io.cosgriff.glovebox.ui.settings

import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import io.cosgriff.glovebox.domain.FindingColour
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

/**
 * The second pass over an invoice's pages (#572): small filled shapes come back with their colour, in the words'
 * units from the page's top left, and rules and boxes don't. The PDF is made up here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class PdfWordsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun report(): ByteArray {
        PDFBoxResourceLoader.init(context)
        val page = PDPage(PDRectangle.LETTER)
        val height = page.mediaBox.height
        val out = ByteArrayOutputStream()
        PDDocument().use { doc ->
            doc.addPage(page)
            PDPageContentStream(doc, page).use { s ->
                s.beginText()
                s.setFont(PDType1Font.HELVETICA, 10f)
                s.newLineAtOffset(50f, height - 200f)
                s.showText("Axles")
                s.endText()
                // A red dot just left of the word, 12 points across, and a long grey rule that is no mark.
                s.setNonStrokingColor(0.99f, 0.29f, 0.30f)
                s.addRect(32f, height - 204f, 12f, 12f)
                s.fill()
                s.setNonStrokingColor(0.78f, 0.78f, 0.79f)
                s.addRect(28f, height - 260f, 500f, 1f)
                s.fill()
            }
            doc.save(out)
        }
        return out.toByteArray()
    }

    @Test
    fun `a small filled shape comes back with its colour and box, beside its word`() {
        val read = PdfWords.readInvoice(context, report())!!
        val word = read.words.single { it.text == "Axles" }
        val mark = read.marks.single()
        assertEquals(FindingColour.RED, FindingColour.of(mark.rgb))
        assertEquals(32f, mark.left, 0.5f)
        assertEquals(12f, mark.right - mark.left, 0.5f)
        // On the word's line.
        assertTrue((mark.top + mark.bottom) / 2 in word.top - 6f..word.bottom + 6f)
    }
}
