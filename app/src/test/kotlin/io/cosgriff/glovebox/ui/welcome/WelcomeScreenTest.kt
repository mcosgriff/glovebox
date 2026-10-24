package io.cosgriff.glovebox.ui.welcome

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.show
import io.cosgriff.glovebox.ui.settings.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The first-run welcome: two choices, then add a vehicle or restore; Android is asked about notifications only when they are on. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class WelcomeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val done = mutableListOf<Pair<WelcomeChoice, WelcomeAction>>()

    @Test
    fun `with notifications off, the choices go straight through`() {
        compose.show { WelcomeScreen { choice, action -> done += choice to action } }
        compose.onNode(isToggleable() and hasText(text(R.string.welcome_lock_title))).assertIsOn().performClick()
        compose.onNode(isToggleable() and hasText(text(R.string.welcome_notifications_title))).performClick()
        compose.onNodeWithText(text(R.string.welcome_notifications_off)).assertExists()
        compose.onNodeWithText(text(R.string.welcome_restore)).performScrollTo().performClick()
        assertEquals(listOf(WelcomeChoice(lockApp = false, notifications = false) to WelcomeAction.RESTORE), done)
    }

    @Test
    fun `with notifications on, Android is asked first, and whatever it answers the welcome carries on`() {
        compose.show { WelcomeScreen { choice, action -> done += choice to action } }
        compose.onNodeWithText(text(R.string.welcome_notifications_ask)).assertExists()
        compose.onNodeWithText(text(R.string.welcome_add_vehicle)).performScrollTo().performClick()
        assertTrue("nothing until Android answers", done.isEmpty())

        val asked = shadowOf(compose.activity).lastRequestedPermission
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), asked.requestedPermissions.toList())
        compose.activity.onRequestPermissionsResult(asked.requestCode, asked.requestedPermissions, intArrayOf(PackageManager.PERMISSION_DENIED))
        compose.waitForIdle()
        assertEquals(listOf(WelcomeChoice(lockApp = true, notifications = true) to WelcomeAction.ADD_VEHICLE), done)
    }
}
