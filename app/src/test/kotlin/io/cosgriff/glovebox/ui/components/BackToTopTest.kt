package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Top button over a page that scrolls whole (a `ScrollState`), as on vehicle details and settings. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = "w411dp-h600dp-xhdpi", sdk = [35])
class BackToTopTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var state: ScrollState

    private val top get() = compose.onNodeWithContentDescription(Components.string(R.string.scroll_back_to_top))
    private val page get() = compose.onNode(hasScrollAction())

    private fun show() {
        compose.draw {
            state = rememberScrollState()
            Box(Modifier.height(400.dp)) {
                Column(Modifier.verticalScroll(state)) {
                    repeat(60) { Text("Row $it", Modifier.fillMaxWidth().height(48.dp)) }
                }
                BackToTop(state, Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    private fun scrollTo(px: Int) {
        runBlocking { state.scrollTo(px) }
        compose.waitForIdle()
    }

    @Test
    fun `not there at the top, nor less than a screen down`() {
        show()
        top.assertDoesNotExist()
        scrollTo(state.viewportSize / 2)
        top.assertDoesNotExist()
    }

    @Test
    fun `there more than a screen down, and a tap goes back to the top`() {
        show()
        scrollTo(state.viewportSize * 2)
        top.assertIsDisplayed()

        top.performClick()
        compose.waitForIdle()
        assertEquals(0, state.value)
        top.assertDoesNotExist()
    }

    @Test
    fun `it goes while scrolling further down and comes back going up`() {
        show()
        scrollTo(state.viewportSize * 2)
        top.assertIsDisplayed()

        page.performTouchInput { down(center); moveBy(Offset(0f, -viewConfiguration.touchSlop - 200f)) }
        compose.waitForIdle()
        top.assertDoesNotExist()

        page.performTouchInput { moveBy(Offset(0f, viewConfiguration.touchSlop + 400f)) }
        compose.waitForIdle()
        top.assertIsDisplayed()

        page.performTouchInput { up() }
        compose.waitForIdle()
        top.assertIsDisplayed()
    }
}
