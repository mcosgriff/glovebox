package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Whether the user's milestone table starts again after its last milestone. On the default screen size:
 * under Robolectric a text field in a dialog on the tall screenshot size never settles and runs out of memory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class MilestoneRepeatDialogTest {
    @get:Rule val compose = createComposeRule()

    private val unit = DistanceUnit.MILES

    private fun save() = compose.onNodeWithText(text(R.string.save)).performClick()

    @Test
    fun `the repeat is a whole cycle no shorter than the table, or empty for a table that ends`() {
        val answers = mutableListOf<Int?>()
        var dismissed = 0
        compose.show { MilestoneRepeatDialog(repeatsEveryMiles = null, lastMiles = 60_000, unit = unit, onSave = { answers += it }, onDismiss = { dismissed++ }) }
        compose.onNodeWithText(text(R.string.milestone_repeat_title)).assertIsDisplayed()
        val box = compose.onNode(hasSetTextAction())

        box.performTextInput("abc")
        save()
        compose.onNodeWithText(text(R.string.sentence, text(R.string.milestone_repeat_bad, "miles", "1,000,000"))).assertIsDisplayed()
        box.performTextReplacement("30,000")
        save()
        compose.onNodeWithText(text(R.string.sentence, text(R.string.milestone_repeat_shorter, "60,000 mi"))).assertIsDisplayed()
        assertTrue(answers.isEmpty())

        box.performTextReplacement("90,000")
        save()
        box.performTextReplacement("")
        save()
        assertEquals(listOf(90_000, null), answers)
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(1, dismissed)
    }
}
