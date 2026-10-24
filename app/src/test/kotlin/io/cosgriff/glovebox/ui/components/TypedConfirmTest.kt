package io.cosgriff.glovebox.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.TypedConfirmation
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A destructive action waits on the word being typed (CLAUDE.md: confirm by typing). */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class TypedConfirmTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the button only works once the word is typed`() {
        var typed by mutableStateOf("")
        var deleted = 0
        compose.draw {
            TypedConfirmField("DELETE", typed, onTyped = { typed = it })
            DangerButton(onClick = { deleted++ }, enabled = TypedConfirmation.matches(typed, "DELETE")) { Text("Delete everything") }
        }
        compose.onNodeWithText(Components.string(R.string.type_word_to_confirm, "DELETE")).assertExists()
        compose.onNodeWithText("Delete everything").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("delete ")
        assertEquals("delete ", typed)
        compose.onNodeWithText("Delete everything").assertIsEnabled().performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun `a field that is switched off takes nothing`() {
        compose.draw { TypedConfirmField("DELETE", "", onTyped = {}, enabled = false) }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).assertIsNotEnabled()
    }
}
