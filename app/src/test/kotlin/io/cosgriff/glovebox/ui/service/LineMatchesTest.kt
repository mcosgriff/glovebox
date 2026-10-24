package io.cosgriff.glovebox.ui.service

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What a custom item is offered as it is typed (#309, #312): the user's rule first, then known wording, then a guess. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class LineMatchesTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val lines = ServiceCatalog.forVehicle(emptyList()).flatMap { c -> c.options.map { LineChoice(it, c) } }
    private val picked = mutableListOf<String>()

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(typed: String, rules: List<CatalogRule> = emptyList()) = compose.setContent {
        GloveboxTheme { LineMatches(typed, lines, onPick = { picked += it.option.id }, rules = rules) }
    }

    @Test
    fun `wording Glovebox knows offers its line, with its category, and a tap picks it`() {
        show("Oil and filter changed")
        compose.onNodeWithText(string(R.string.log_custom_matches)).assertExists()
        compose.onNodeWithText(catalogCategoryLabel("engine")).assertExists()
        compose.onNode(hasText(catalogLabel("oil")) and hasClickAction()).performClick()
        assertEquals(listOf("oil"), picked)
    }

    @Test
    fun `the user's rule comes first and is said to be theirs`() {
        show("Hitch greased", rules = listOf(CatalogRule("Hitch greased", "washer")))
        compose.onNodeWithText(string(R.string.log_line_by_rule, catalogCategoryLabel("fluids"))).assertExists()
        compose.onNode(hasText(catalogLabel("washer")) and hasClickAction()).performClick()
        assertEquals(listOf("washer"), picked)
    }

    @Test
    fun `a guess is labelled a guess, never picked for the user`() {
        show("Watr pump replaced")
        compose.onNodeWithText(string(R.string.log_line_guess, catalogCategoryLabel("cooling"))).assertExists()
        assertEquals(emptyList<String>(), picked)
    }

    @Test
    fun `wording that looks like nothing offers nothing`() {
        show("Zzzz")
        compose.onNodeWithText(string(R.string.log_custom_matches)).assertDoesNotExist()
    }

    @Test
    fun `filing lists every line by category, opening one or all of them`() {
        var dismissed = 0
        compose.setContent {
            GloveboxTheme { FileUnderDialog("Hitch greased", lines.filter { it.category.id in setOf("tires", "brakes") }, onPick = { picked += it.option.id }, onDismiss = { dismissed++ }) }
        }
        compose.onNodeWithText(string(R.string.log_file_under_every)).assertExists()
        compose.onNodeWithText(catalogLabel("alignment")).assertDoesNotExist()
        compose.onNode(hasText(catalogCategoryLabel("tires")) and hasClickAction()).performClick()
        compose.onNodeWithText(catalogLabel("alignment")).assertExists()
        compose.onNode(hasText(catalogCategoryLabel("tires")) and hasClickAction()).performClick()
        compose.onNodeWithText(catalogLabel("alignment")).assertDoesNotExist()

        compose.onNodeWithText(string(R.string.expand_all)).performClick()
        compose.onNodeWithText(catalogLabel("rotors")).assertExists()
        compose.onNodeWithText(string(R.string.collapse_all)).performClick()
        compose.onNodeWithText(catalogLabel("rotors")).assertDoesNotExist()

        compose.onNodeWithText(string(R.string.expand_all)).performClick()
        compose.onNodeWithText(catalogLabel("rotors")).performClick()
        assertEquals(listOf("rotors"), picked)
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        assertEquals(1, dismissed)
    }
}
