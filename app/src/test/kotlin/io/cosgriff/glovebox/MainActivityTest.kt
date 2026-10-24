package io.cosgriff.glovebox

import android.Manifest
import android.content.Intent
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.lock.AppLock
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.notify.PendingDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = SHELL_PHONE)
class MainActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph
    private var controller: ActivityController<MainActivity>? = null

    @After fun close() {
        controller?.pause()?.stop()?.destroy()
        graph.close()
    }

    private fun launch(intent: Intent = Intent(app, MainActivity::class.java)): MainActivity =
        Robolectric.buildActivity(MainActivity::class.java, intent).also { controller = it }.setup().get()

    /** Past the welcome, with the lock off. */
    private fun returning() = graph.settings { setWelcomeSeen(); setBiometricLock(false) }

    private fun secure(activity: MainActivity) = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    @Test
    fun `a first run opens on the welcome, before any lock`() {
        graph.settings { setBiometricLock(true) }
        launch()
        compose.waitForText(string(R.string.welcome_title))
        compose.onNodeWithText(string(R.string.unlock_locked_and_encrypted)).assertDoesNotExist()
    }

    @Test
    fun `adding the first vehicle from the welcome goes straight to Add vehicle, unlocked`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launch()
        compose.waitForText(string(R.string.welcome_add_vehicle))
        compose.onNodeWithText(string(R.string.welcome_add_vehicle)).performClick()
        compose.waitForText(string(R.string.add_vehicle_title))
        assertFalse(graph.get<AppLock>().locked.value)
        assertTrue(runBlocking { graph.get<io.cosgriff.glovebox.data.settings.SettingsRepository>().settings.first().welcomeSeen })
    }

    @Test
    fun `restoring from the welcome opens the file picker`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val activity = launch()
        compose.waitForText(string(R.string.welcome_restore))
        compose.onNodeWithText(string(R.string.welcome_restore)).performClick()
        idleMainUntil { shadowOf(activity).peekNextStartedActivityForResult() != null }
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, shadowOf(activity).nextStartedActivityForResult.intent.action)
    }

    @Test
    fun `a shortcut used while the welcome showed doesn't follow it`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launch(Intent(app, MainActivity::class.java).also(Destination.Backup::putInto))
        compose.waitForText(string(R.string.welcome_add_vehicle))
        compose.onNodeWithText(string(R.string.welcome_add_vehicle)).performClick()
        compose.waitForText(string(R.string.add_vehicle_title))
        assertNull(graph.get<PendingDestination>().destination.value)
    }

    @Test
    fun `with the lock off the app opens on the garage, hidden from recent apps by default`() {
        returning()
        val activity = launch()
        compose.waitForText(string(R.string.garage_title))
        compose.waitUntil(10_000) { secure(activity) }
    }

    @Test
    fun `with hiding in recent apps off, screenshots are allowed`() {
        graph.settings { setWelcomeSeen(); setBiometricLock(false); setHideInRecents(false) }
        val activity = launch()
        compose.waitForText(string(R.string.garage_title))
        compose.waitUntil(10_000) { !secure(activity) }
    }

    @Test
    fun `with the lock on the screens wait behind it`() {
        graph.settings { setWelcomeSeen(); setBiometricLock(true) }
        launch()
        compose.waitForText(string(R.string.unlock_locked_and_encrypted))
        compose.onNodeWithText(string(R.string.reminders_rules)).assertDoesNotExist()
    }

    @Test
    fun `a notification's destination is opened once the screens are up`() {
        returning()
        launch(Intent(app, MainActivity::class.java).also(Destination.Reminders::putInto))
        compose.waitForText(string(R.string.reminders_rules))
        assertNull(graph.get<PendingDestination>().destination.value)
    }

    @Test
    fun `a notification tapped while the app is open is opened too`() {
        returning()
        launch()
        compose.waitForText(string(R.string.garage_title))
        controller!!.newIntent(Intent(app, MainActivity::class.java).also(Destination.Reminders::putInto))
        compose.waitForText(string(R.string.reminders_rules))
    }

    @Test
    fun `a restored activity doesn't replay the tap it was first opened with`() {
        returning()
        launch(Intent(app, MainActivity::class.java).also(Destination.Reminders::putInto))
        compose.waitForText(string(R.string.reminders_rules))
        graph.get<PendingDestination>().take()
        val bundle = android.os.Bundle()
        controller!!.saveInstanceState(bundle).pause().stop().destroy()
        controller = Robolectric.buildActivity(MainActivity::class.java, Intent(app, MainActivity::class.java).also(Destination.Backup::putInto))
            .setup(bundle)
        compose.waitForIdle()
        assertNull(graph.get<PendingDestination>().destination.value)
    }

    @Test
    fun `tapping the launcher icon while the app runs in a task begun elsewhere shows the running app`() {
        returning()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClass(app, MainActivity::class.java)
        val built = Robolectric.buildActivity(MainActivity::class.java, launcher)
        shadowOf(built.get()).setIsTaskRoot(false)
        val activity = built.create().get()
        assertTrue(activity.isFinishing)
        assertNull(graph.get<PendingDestination>().destination.value)
    }

    @Test
    fun `the launcher icon at the root of its own task opens the app as usual`() {
        returning()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClass(app, MainActivity::class.java)
        val activity = launch(launcher)
        assertFalse(activity.isFinishing)
        compose.waitForText(string(R.string.garage_title))
    }
}
