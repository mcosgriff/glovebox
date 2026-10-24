package io.cosgriff.glovebox

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class MainActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val graph get() = ShellTestApp.graph
    private fun text(id: Int) = ShellTestApp.current.getString(id)

    @After fun close() = graph.close()

    @Test
    fun `a first run with no vehicles opens on the welcome`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.onNodeWithText(text(R.string.welcome_title)).assertExists()
        }
    }
}
