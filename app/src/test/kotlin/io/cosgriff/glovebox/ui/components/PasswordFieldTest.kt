package io.cosgriff.glovebox.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class PasswordFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `a password is hidden until the eye shows it`() {
        var value by mutableStateOf("")
        var show by mutableStateOf(false)
        compose.draw { PasswordField("Backup password", value, onChange = { value = it }, show = show, onToggleShow = { show = !show }, toggleLabel = "Show password", error = true) }
        compose.onNode(hasSetTextAction()).performTextInput("made-up secret")
        assertEquals("made-up secret", value)
        // Marked a password for accessibility and autofill whether shown or not.
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithContentDescription("Show password").performClick()
        assertEquals(true, show)
        compose.onNodeWithContentDescription("Show password").performClick()
        assertEquals(false, show)
    }
}
