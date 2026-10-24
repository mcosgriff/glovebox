package io.cosgriff.glovebox.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ChoiceDialogTest {
    @get:Rule val compose = createComposeRule()
    private val picked = mutableListOf<String>()
    private var dismissed = 0

    @Test
    fun `with a choice made the rows are radio buttons, and a tap picks and closes`() {
        compose.draw {
            ChoiceDialog("Theme", listOf("Light", "Dark"), selected = "Dark", label = { it }, onPick = { picked += it }, onDismiss = { dismissed++ }, note = "A made-up note")
        }
        compose.onNodeWithText("A made-up note").assertExists()
        compose.onNodeWithText("Dark").assertIsSelected()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and androidx.compose.ui.test.hasText("Light")).performClick()
        assertEquals(listOf("Light"), picked)
        assertEquals(1, dismissed)
    }

    @Test
    fun `without a choice it is a list of places to go, and Cancel picks nothing`() {
        compose.draw { ChoiceDialog("Go to", listOf("Garage", "History"), selected = null, label = { it }, onPick = { picked += it }, onDismiss = { dismissed++ }) }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and androidx.compose.ui.test.hasText("History")).assertExists()
        compose.onNodeWithText(Components.string(R.string.cancel)).performClick()
        assertEquals(emptyList<String>(), picked)
        assertEquals(1, dismissed)
    }
}
