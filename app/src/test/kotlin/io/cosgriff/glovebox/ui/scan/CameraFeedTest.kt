package io.cosgriff.glovebox.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The camera screens' shell: asking for the camera, and what shows without it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class CameraFeedTest {
    // Effects run on the test's own dispatcher, so CameraX's callbacks come back on the main thread as they do in the app.
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    private val activity get() = compose.activity
    private var closes = 0

    private fun str(@StringRes id: Int) = activity.getString(id)

    private fun show() = compose.setContent {
        CameraScreen(why = "Made-up reason for the camera", instead = "Do it by hand", onClose = { closes++ }) {
            Text("The camera's own screen")
        }
    }

    /** The answer to the last permission prompt, as Android gives it. */
    private fun answer(granted: Boolean) {
        compose.waitUntil(5_000) { shadowOf(activity).lastRequestedPermission != null }
        val request = shadowOf(activity).lastRequestedPermission
        assertEquals(listOf(Manifest.permission.CAMERA), request.requestedPermissions.toList())
        if (granted) shadowOf(activity.application).grantPermissions(Manifest.permission.CAMERA)
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            intArrayOf(if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED),
        )
        compose.waitForIdle()
    }

    @Test
    fun `the camera is asked for on the way in, and until it is given the screen says why`() {
        show()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.scan_camera_off)).assertExists()
        compose.onNodeWithText("Made-up reason for the camera").assertExists()
        compose.onAllNodesWithText("The camera's own screen").assertCountEquals(0)
        answer(granted = true)
        compose.onNodeWithText("The camera's own screen").assertExists()
    }

    @Test
    fun `refused, the screen offers the app's settings and to ask again`() {
        show()
        compose.waitForIdle()
        answer(granted = false)
        compose.onNodeWithText(str(R.string.scan_open_settings)).performClick()
        val settings = shadowOf(activity).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, settings.action)
        assertEquals("package:${activity.packageName}", settings.dataString)

        val refused = shadowOf(activity).lastRequestedPermission
        compose.onNodeWithText(str(R.string.scan_ask_again)).performClick()
        val again = shadowOf(activity).lastRequestedPermission
        assertTrue(again !== refused)
        assertEquals(listOf(Manifest.permission.CAMERA), again.requestedPermissions.toList())
    }

    @Test
    fun `without the camera there is always a way on by hand, and a way out`() {
        show()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.scan_allow_camera)).performClick()
        assertTrue(shadowOf(activity).lastRequestedPermission.requestedPermissions.contains(Manifest.permission.CAMERA))
        compose.onNodeWithText("Do it by hand").performClick()
        compose.onNodeWithContentDescription(str(R.string.scan_close_scanner)).performClick()
        assertEquals(2, closes)
    }

    @Test
    fun `with the camera already allowed, nothing is asked`() {
        shadowOf(activity.application).grantPermissions(Manifest.permission.CAMERA)
        show()
        compose.waitForIdle()
        compose.onNodeWithText("The camera's own screen").assertExists()
        assertEquals(null, shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun `the top bar's flashlight is offered only where there is one, and says which way it will switch`() {
        shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, true)
        var torch = false
        compose.setContent {
            CameraTopBar("Made-up title", "Close it", torch, onTorch = { torch = it }, onClose = { closes++ })
        }
        compose.onNodeWithContentDescription(str(R.string.scan_flashlight_on)).performClick()
        assertTrue(torch)
        compose.onNodeWithContentDescription("Close it").performClick()
        assertEquals(1, closes)
    }

    @Test
    fun `no flashlight, no button for one`() {
        shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, false)
        compose.setContent { CameraTopBar("Made-up title", "Close it", torch = true, onTorch = {}, onClose = {}) }
        compose.onAllNodesWithText(str(R.string.scan_flashlight_off)).assertCountEquals(0)
        compose.onNodeWithText("Made-up title").assertExists()
    }
}
