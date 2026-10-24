package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.ui.DocumentViewerRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The in-app document viewer, for documents whose file can't be read: it says so and offers another app, which
 * can't read it either. Drawing a real PDF needs the sandboxed renderer, which Robolectric hasn't got.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [29])
class DocumentViewerScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private var closed = 0

    private fun document(format: String, fileName: String): Long {
        val id = g.add(VehicleFixtures.wagon)
        return runBlocking {
            g.db.documents().insert(DocumentEntity(vehicleId = id, type = "REPORT", name = "History report", fileName = fileName, format = format, sizeBytes = 5_000, addedOn = LocalDate.now()))
        }
    }

    private fun show(documentId: Long) {
        val vm = g.viewModel { DocumentViewerViewModel(routeHandle(DocumentViewerRoute(documentId)), g.db.documents(), g.documentStore) }
        compose.setContent { GloveboxTheme { DocumentViewerScreen(onClose = { closed++ }, vm = vm) } }
    }

    private fun shown(text: String) = compose.awaitThat { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun `a PDF that can't be read says so and offers another app, which can't read it either`() {
        show(document("PDF", "document-gone.pdf"))
        shown("History report")
        shown(g.string(R.string.documents_unreadable))
        compose.onNode(hasText(g.string(R.string.documents_open_elsewhere)) and hasClickAction()).click()
        compose.waitForIdle()
        compose.onNode(hasContentDescription(g.string(R.string.close))).click()
        assertEquals(1, closed)
    }

    @Test
    fun `a picture's menu hands it out, or says it can't`() {
        show(document("JPEG", "document-gone.jpg"))
        shown("History report")
        compose.onNode(hasContentDescription(g.string(R.string.more_for, "History report"))).click()
        compose.onNode(hasText(g.string(R.string.share)) and hasClickAction()).click()
        shown(g.string(R.string.documents_unreadable))
    }

    @Test
    fun `a document deleted while it is open closes the viewer`() {
        val id = document("PDF", "document-gone.pdf")
        show(id)
        shown("History report")
        runBlocking { g.db.documents().delete(id) }
        compose.awaitThat { closed == 1 }
    }
}
