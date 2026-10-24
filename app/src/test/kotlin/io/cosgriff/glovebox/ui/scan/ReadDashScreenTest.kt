package io.cosgriff.glovebox.ui.scan

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The dash reader with the camera allowed. Robolectric's phone has no back camera, so no frame ever arrives:
 * what is tested is what the reader says before it has read anything.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ReadDashScreenTest {
    // Effects run on the test's own dispatcher, so CameraX's callbacks come back on the main thread as they do in the app.
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    private var closes = 0

    @Before fun allowCamera() = shadowOf(compose.activity.application).grantPermissions(Manifest.permission.CAMERA)

    @Test
    fun `with a last reading the hint says what it looks for, in the vehicle's unit`() {
        compose.setContent { ReadDashScreen(last = 61_250, unit = DistanceUnit.KILOMETERS, onClose = { closes++ }, onUse = {}) }
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.read_dash_instruction)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.read_dash_hint_from, 61_250.asDistance(DistanceUnit.KILOMETERS))).assertExists()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.close)).performClick()
        assertEquals(1, closes)
    }

    @Test
    fun `without one it only says how to hold the phone`() {
        compose.setContent { ReadDashScreen(last = null, unit = DistanceUnit.MILES, onClose = {}, onUse = {}) }
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.read_dash_title)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.read_dash_hint)).assertExists()
    }
}
