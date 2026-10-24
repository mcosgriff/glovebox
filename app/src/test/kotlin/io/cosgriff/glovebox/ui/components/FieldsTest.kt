package io.cosgriff.glovebox.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class FieldsTest {
    @get:Rule val compose = createComposeRule()
    private val check: (String) -> Any? = { it.toIntOrNull() }

    @Test
    fun `what isn't valid says so, in place of the help`() {
        compose.draw { CheckedField("12a", {}, "Battery CCA", error = "Whole amps only", check = check, supporting = "From the label") }
        compose.onNodeWithText("Whole amps only").assertExists()
        compose.onNodeWithText("From the label").assertDoesNotExist()
    }

    @Test
    fun `a valid or empty field shows its help`() {
        compose.draw { CheckedField("", {}, "Battery CCA", error = "Whole amps only", check = check, supporting = "From the label") }
        compose.onNodeWithText("From the label").assertExists()
        compose.onNodeWithText("Whole amps only").assertDoesNotExist()
    }

    @Test
    fun `blank typing is nothing, anything else is kept without its spaces`() {
        assertNull("   ".orNullIfBlank())
        assertEquals("0W-20", "  0W-20 ".orNullIfBlank())
    }
}
