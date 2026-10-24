package io.cosgriff.glovebox.ui.history

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.HistoryOrder
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * History's header and filters scroll away with the records, in both views, and the Top button brings them
 * back: through [HistoryContent], from made-up [Fixtures.longHistory].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class HistoryScrollTest(private val view: HistoryView) {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(id: Int) = context.getString(id)
    private val title get() = compose.onNodeWithText(string(R.string.history_title))
    private val search get() = compose.onNodeWithText(string(R.string.history_search))
    private val top get() = compose.onNodeWithContentDescription(string(R.string.scroll_back_to_top))
    private val list get() = compose.onNode(hasScrollToIndexAction())

    private fun show() {
        compose.setContent {
            GloveboxTheme {
                HistoryContent(
                    Fixtures.longHistory, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
                    onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
                    view = view, onLogIt = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun `at the top the filters show and there is no Top button`() {
        show()
        title.assertIsDisplayed()
        search.assertIsDisplayed()
        top.assertDoesNotExist()
    }

    @Test
    fun `the filters scroll away with the records, and Top brings them back`() {
        show()
        list.performScrollToIndex(20)
        compose.waitForIdle()
        title.assertDoesNotExist()
        search.assertDoesNotExist()
        top.assertIsDisplayed()

        top.performClick()
        compose.waitForIdle()
        title.assertIsDisplayed()
        search.assertIsDisplayed()
        compose.onNodeWithText(string(R.string.history_all)).assertIsDisplayed()
        top.assertDoesNotExist()
    }

    @Test
    fun `the Top button goes while scrolling further down and comes back going up`() {
        show()
        // The timeline has a row for each year and each gap as well as each record.
        list.performScrollToIndex(if (view == HistoryView.LIST) 12 else 24)
        compose.waitForIdle()
        top.assertIsDisplayed()

        // A finger held on the way down: the button gets out of the way.
        list.performTouchInput { down(center); moveBy(Offset(0f, -viewConfiguration.touchSlop - 200f)) }
        compose.waitForIdle()
        top.assertDoesNotExist()

        // Still held, now on the way back up: it is there to take.
        list.performTouchInput { moveBy(Offset(0f, viewConfiguration.touchSlop + 400f)) }
        compose.waitForIdle()
        top.assertIsDisplayed()

        list.performTouchInput { up() }
        compose.waitForIdle()
        top.assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun views() = HistoryView.entries.toList()
    }
}
