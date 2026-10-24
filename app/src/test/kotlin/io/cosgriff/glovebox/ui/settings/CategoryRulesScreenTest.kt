package io.cosgriff.glovebox.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.catalogLabel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The user's category rules: each can be pointed at another line or deleted, and neither touches a record. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class CategoryRulesScreenTest {
    @get:Rule(order = 1) val compose = createComposeRule()

    @get:Rule(order = 0) val g = TestGraph()


    private fun show() = compose.show { CategoryRulesScreen(onBack = {}, vm = CategoryRulesViewModel(g.db.catalogRules())) }

    @Test
    fun `with no rules the page says how they are made`() {
        show()
        compose.waitForText(text(R.string.rules_empty))
    }

    @Test
    fun `a rule is pointed at another line, or deleted after asking`() {
        val day = LocalDate.of(2026, 1, 1)
        runBlocking {
            g.db.catalogRules().put("Watr pump", "bulbs", day)
            g.db.catalogRules().put("Mystery job", "a-line-a-later-version-dropped", day)
        }
        show()
        compose.waitForText("Watr pump")
        // A line this version doesn't know is kept and shown, never applied.
        compose.onNodeWithText(text(R.string.rules_unknown_line)).assertIsDisplayed()

        compose.onNodeWithText("Watr pump").performClick()
        compose.onNodeWithText(text(R.string.expand_all)).performClick()
        compose.onAllNodesWithText(catalogLabel("water-pump"))[0].performClick()
        compose.waitFor { runBlocking { g.db.catalogRules().all() }.single { it.text == "Watr pump" }.catalogId == "water-pump" }

        compose.onNodeWithContentDescription(text(R.string.rules_delete_for, "Mystery job")).performClick()
        compose.onNodeWithText(text(R.string.rules_delete_body, "Mystery job")).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(2, runBlocking { g.db.catalogRules().all() }.size)

        compose.onNodeWithContentDescription(text(R.string.rules_delete_for, "Mystery job")).performClick()
        compose.onNodeWithText(text(R.string.delete)).performClick()
        compose.waitFor { runBlocking { g.db.catalogRules().all() }.map { it.text } == listOf("Watr pump") }
    }
}
