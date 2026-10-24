package io.cosgriff.glovebox.ui.settings

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.CarfaxRefusal
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportFormat
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.ImportRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/**
 * Reading a picked import file: JSON as text, a PDF as a Carfax report, in memory only, and anything that
 * can't be read said plainly. Also the one place a report is kept: in the vehicle's documents, when asked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ImportPickerTest {
    @get:Rule(order = 1) val main = MainDispatcherRule()

    @get:Rule(order = 0) val g = TestGraph()
    private val picker = ImportPicker(g.context, g.appScope)
    private val res get() = AppText.res

    @Before fun reset() = runBlocking { g.resetSettings() }


    private fun file(bytes: ByteArray): Uri = Uri.fromFile(File(g.context.cacheDir, "picked-${System.nanoTime()}").apply { writeBytes(bytes) })

    @Test
    fun `a JSON file is held as text, under a name of its own when the picker gives none`() = runTest {
        picker.onPicked(file("""{"kind": "torque-specs"}""".toByteArray()))
        val picked = picker.picked.awaitValue { it != null }!!
        assertEquals("""{"kind": "torque-specs"}""", picked.text)
        assertEquals(res.getString(R.string.import_file), picked.name)
        assertNull(picked.pdf)
        assertNull(picked.file)

        picker.clear()
        assertNull(picker.picked.value)
        assertNull(picker.problem.value)
    }

    @Test
    fun `backing out of the picker changes nothing`() = runTest {
        picker.onPicked(null)
        assertNull(picker.picked.value)
        assertNull(picker.problem.value)
    }

    @Test
    fun `a file past the size cap is refused without being held`() = runTest {
        picker.onPicked(file(ByteArray(ImportFormat.MAX_BYTES + 1) { 'a'.code.toByte() }))
        assertEquals(res.getString(R.string.import_too_big), picker.problem.awaitValue { it != null })
        assertNull(picker.picked.value)
    }

    @Test
    fun `a file that can't be opened is said to be unreadable`() = runTest {
        picker.onPicked(Uri.fromFile(File(g.context.cacheDir, "not-there.json")))
        assertEquals(res.getString(R.string.document_problem_unreadable), picker.problem.awaitValue { it != null })
    }

    @Test
    fun `a damaged PDF is said to be unreadable`() = runTest {
        picker.onPicked(file("%PDF-1.4 this is not really a PDF".toByteArray()))
        assertEquals(res.getString(R.string.import_pdf_unreadable), picker.problem.awaitValue { it != null })
    }

    @Test
    fun `a PDF that isn't a Carfax report is refused with that reason`() = runTest {
        val pdf = pdf(listOf(55f to "Quarterly statement"), entries = listOf(Entry("06/14/2019", "12,480", "Bank", "Payment received")))
        picker.onPicked(file(pdf))
        val picked = picker.picked.awaitValue { it != null }!!
        assertEquals(CarfaxRefusal.NotAReport, (picked.file as ImportFile.Invalid).carfax)
    }

    @Test
    fun `a Carfax report's service visits become a service-history file, and the PDF stays in memory`() = runTest {
        val pdf = carfax()
        picker.onPicked(file(pdf))
        val picked = picker.picked.awaitValue { it != null }!!
        assertEquals(res.getString(R.string.import_carfax_report), picked.name)
        assertArrayEquals(pdf, picked.pdf)
        val visits = (picked.file as ImportFile.ServiceHistory).items
        assertEquals(listOf(LocalDate.of(2019, 6, 14) to 12_480), visits.map { it.date to it.odometer })
        assertEquals("Acme Motors", visits.single().shopName)
    }

    @Test
    fun `the words of a PDF are read with their places, and an encrypted or broken one gives nothing`() {
        val words = PdfWords.read(g.context, carfax())!!
        assertTrue(words.any { it.text == "CARFAX" && it.page == 1 })
        assertNull(PdfWords.read(g.context, "%PDF-1.4 broken".toByteArray()))
    }

    @Test
    fun `asked to, a report is filed with the vehicle's documents once the import is written`() = runTest {
        val id = g.vehicle()
        val vm = ImportViewModel(
            g.db.vehicles(), g.settings, g.db.torque(), g.db.schedule(), g.db.service(), g.db.odometer(), g.db.milestones(),
            g.db.catalogRules(), g.actions, picker, g.documents, routeHandle(ImportRoute(id)),
        )
        picker.onPicked(file(carfax()))
        picker.picked.awaitValue { it != null }
        val ready = vm.preview.awaitValue { it is ImportPreview.Ready } as ImportPreview.Ready
        assertEquals(1, ready.view.new.size)

        vm.import(ready, ImportMode.ADD_NEW, stopDoubles = false, keepCopy = true)
        val result = vm.result.awaitValue { it?.keptCopy != null }!!
        assertEquals(1, result.added)
        assertEquals(1, g.db.service().recordsFor(id).size)
        assertEquals(true, result.keptCopy)
        val kept = g.db.documents().documentsFor(id).single()
        assertEquals("REPORT", kept.type)
        assertEquals("PDF", kept.format)
    }

    @Test
    fun `picking asks the launcher the activity attached, and only that one`() {
        var asked = 0
        val launcher: () -> Unit = { asked++ }
        picker.pick()
        assertEquals(0, asked)
        picker.attach(launcher)
        picker.pick()
        assertEquals(1, asked)
        picker.detach {}
        picker.pick()
        assertEquals("another launcher can't detach this one", 2, asked)
        picker.detach(launcher)
        picker.pick()
        assertEquals(2, asked)
    }

    private data class Entry(val date: String, val miles: String?, val source: String, val comment: String)

    /** A made-up report laid out like a Carfax history: a title line, then entries in its four columns. */
    private fun carfax(): ByteArray = pdf(
        listOf(55f to "CARFAX Vehicle History Report"),
        entries = listOf(
            Entry("04/02/2019", null, "Ohio Motor Vehicle Dept.", "Title issued or updated"),
            Entry("06/14/2019", "12,480", "Acme Motors", "Oil and filter changed"),
        ),
    )

    private fun pdf(title: List<Pair<Float, String>>, entries: List<Entry>): ByteArray {
        PDFBoxResourceLoader.init(g.context as Context)
        val page = PDPage(PDRectangle.LETTER)
        val height = page.mediaBox.height
        val out = ByteArrayOutputStream()
        PDDocument().use { doc ->
            doc.addPage(page)
            PDPageContentStream(doc, page).use { s ->
                fun at(x: Float, y: Float, text: String) {
                    s.beginText()
                    s.setFont(PDType1Font.HELVETICA, 8f)
                    s.newLineAtOffset(x, height - y)
                    s.showText(text)
                    s.endText()
                }
                var y = 100f
                title.forEach { (x, text) -> at(x, y, text) }
                y += 40f
                entries.forEach { e ->
                    at(55f, y, e.date)
                    e.miles?.let { at(129f, y, it) }
                    at(182f, y, e.source)
                    at(356f, y, e.comment)
                    y += 39f
                }
            }
            doc.save(out)
        }
        return out.toByteArray()
    }
}
