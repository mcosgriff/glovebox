package io.cosgriff.glovebox.ui.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.LeftOut
import io.cosgriff.glovebox.domain.WrittenFile
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.counted
import io.cosgriff.glovebox.ui.settings.show
import io.cosgriff.glovebox.ui.settings.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Save as an import file": a file never claims to hold more than it does, and what it leaves out is listed first. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class SaveAsImportFileTest {
    @get:Rule val compose = createComposeRule()

    private val saved = mutableListOf<String>()

    private fun show(file: WrittenFile) = compose.show { SaveAsImportFile("Holds your torque specs.", write = { file }, onSave = { saved += it }) }

    @Test
    fun `a file with everything in it is saved at once`() {
        show(WrittenFile(text = "{}", count = 3, leftOut = emptyList()))
        compose.onNodeWithText("Holds your torque specs.").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.save_file_button)).performClick()
        assertEquals(listOf("{}"), saved)
    }

    @Test
    fun `what is left out is listed, and the rest is saved only if the user goes ahead`() {
        show(WrittenFile(text = "{}", count = 2, leftOut = listOf(LeftOut("Lug nuts", "no unit given"))))
        compose.onNodeWithText(text(R.string.save_file_button)).performClick()
        compose.onNodeWithText(counted(R.plurals.save_file_left_out, 1, "1")).assertIsDisplayed()
        compose.onNodeWithText("Lug nuts").assertIsDisplayed()
        compose.onNodeWithText("No unit given").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertTrue(saved.isEmpty())

        compose.onNodeWithText(text(R.string.save_file_button)).performClick()
        compose.onNodeWithText(text(R.string.save_file_rest_button)).performClick()
        assertEquals(listOf("{}"), saved)
    }

    @Test
    fun `when nothing can be written there is nothing to save`() {
        show(WrittenFile(text = null, count = 0, leftOut = listOf(LeftOut("Lug nuts", "no unit given"))))
        compose.onNodeWithText(text(R.string.save_file_button)).performClick()
        compose.onNodeWithText(text(R.string.save_file_nothing)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.save_file_stricter)).assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasText(text(R.string.save_file_rest_button))).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(text(R.string.ok)).performClick()
        assertTrue(saved.isEmpty())
    }
}
