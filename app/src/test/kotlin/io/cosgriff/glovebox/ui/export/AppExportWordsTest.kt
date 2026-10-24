package io.cosgriff.glovebox.ui.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ExportPhrase
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.TestGraph
import io.cosgriff.glovebox.ui.settings.awaitValue
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** An export's words in the app's language, and the "save as" it is handed to. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class AppExportWordsTest {
    @get:Rule val g = TestGraph()

    @Test
    fun `every phrase has words of its own`() {
        val worded = ExportPhrase.entries.associateWith { AppExportWords.phrase(it, "A", "B", "C") }
        val shared = worded.entries.groupBy { it.value }.filterValues { it.size > 1 }.mapValues { (_, v) -> v.map { it.key } }
        assertEquals("phrases worded the same: $shared", emptyMap<String, List<ExportPhrase>>(), shared)
        assertEquals(AppText.res.getString(R.string.export_page, "3"), AppExportWords.phrase(ExportPhrase.PAGE, "3"))
    }

    @Test
    fun `figures carry their unit and dates their format`() {
        assertEquals(AppText.res.getQuantityString(R.plurals.export_record_count, 2, "2"), AppExportWords.records(2))
        assertEquals("41,000 mi", AppExportWords.distance(41_000, DistanceUnit.MILES))
        assertEquals("41,000 km", AppExportWords.distance(41_000, DistanceUnit.KILOMETERS))
        assertEquals("$62.50", AppExportWords.money(6_250))
        assertEquals("Jan 2, 2025", AppExportWords.date(LocalDate.of(2025, 1, 2)))
    }

    @Test
    fun `save as asks for the file's own type and name, and an answer that isn't OK is no answer`() {
        val contract = SaveAs()
        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), SaveAs.Request("application/pdf", "glovebox.pdf"))
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals("glovebox.pdf", intent.getStringExtra(Intent.EXTRA_TITLE))
        val picked = Uri.parse("content://documents/1")
        assertEquals(picked, contract.parseResult(Activity.RESULT_OK, Intent().setData(picked)))
        assertNull(contract.parseResult(Activity.RESULT_CANCELED, Intent().setData(picked)))
    }

    @Test
    fun `a finished save is announced to the nav host`() = runTest {
        val saver = ExportSaver(g.context, g.appScope)
        saver.attach { _, _ -> }
        saver.save(File(g.context.cacheDir, "x.md").apply { writeText("x") }, "x.md", io.cosgriff.glovebox.domain.ExportFormat.MARKDOWN)
        saver.onDestination(Uri.fromFile(File(g.context.filesDir, "x.md")))
        assertEquals(AppText.res.getString(R.string.export_saved, "x.md"), ExportResultsViewModel(saver).results.awaitValue { true })
    }

    @Test
    fun `an import file is written in the clear only until it is copied out`() = runTest {
        val saver = ExportSaver(g.context, g.appScope)
        var asked: Pair<String, String>? = null
        saver.attach { mime, name -> asked = mime to name }
        saver.saveImportFile("made-up import file", "torque.json")
        assertEquals(io.cosgriff.glovebox.domain.ImportFiles.MIME_TYPE to "torque.json", asked)
        val out = File(g.context.filesDir, "torque.json")
        saver.onDestination(Uri.fromFile(out))
        assertEquals(AppText.res.getString(R.string.export_saved_file, "torque.json"), saver.results.awaitValue { true })
        assertEquals("made-up import file", out.readText())
        saver.clear()
        assertEquals(emptyList<File>(), File(g.context.cacheDir, "export").listFiles().orEmpty().toList())
    }
}
