package io.cosgriff.glovebox.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.os.Build
import android.hardware.biometrics.BiometricPrompt
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import io.cosgriff.glovebox.MainActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.string
import io.cosgriff.glovebox.waitForText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBiometricPrompt
import org.robolectric.util.ReflectionHelpers

/**
 * The lock as `MainActivity` shows it, with the lock on and the welcome already seen. Android's own prompt is
 * Robolectric's stand-in, answered by the test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = io.cosgriff.glovebox.SHELL_PHONE)
class LockScreenTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun settings() = graph.settings { setWelcomeSeen(); setBiometricLock(true) }

    @After fun close() {
        if (::scenario.isInitialized) scenario.close()
        graph.close()
    }

    private fun biometrics(fingerprintSensor: Boolean = true) {
        // A fingerprint needs a screen lock behind it.
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(true)
        biometricService(app, connected = true)
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, fingerprintSensor)
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun locked(): Boolean = graph.get<AppLock>().locked.value

    /** Android's prompt is up and waiting for an answer. */
    private fun prompted() {
        compose.waitUntil(10_000) { ShadowBiometricPrompt.getCurrentPrompt() != null }
    }

    @Test
    fun `a phone with no screen lock has nothing to lock with, so the app opens`() {
        biometricService(app, connected = false)
        launch()
        compose.waitForText(string(R.string.garage_title))
        assertFalse(locked())
    }

    @Test
    fun `with a fingerprint the lock asks for it on arrival, and a recognised finger opens the app`() {
        biometrics()
        launch()
        compose.waitForText(string(R.string.unlock_instruction_fingerprint))
        compose.onNodeWithText(string(R.string.unlock_locked_and_encrypted)).assertExists()
        compose.onNodeWithContentDescription(string(R.string.unlock_button_fingerprint)).assertExists()
        prompted()
        assertTrue(locked())

        ShadowBiometricPrompt.authenticateCurrentSessionSuccessfully()
        compose.waitForText(string(R.string.garage_title))
        assertFalse(locked())
    }

    @Test
    fun `without a fingerprint sensor the lock asks to unlock, not to touch a sensor`() {
        biometrics(fingerprintSensor = false)
        launch()
        compose.waitForText(string(R.string.unlock_instruction_biometric))
        compose.onNodeWithContentDescription(string(R.string.unlock_button_biometric)).assertExists()
    }

    @Test
    fun `cancelling says nothing, and the round button asks again`() {
        biometrics()
        launch()
        prompted()
        ShadowBiometricPrompt.authenticateCurrentSessionWithError(BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED, "Cancelled")
        compose.waitForIdle()
        compose.onNodeWithText("Cancelled").assertDoesNotExist()
        assertTrue(locked())

        compose.onNodeWithContentDescription(string(R.string.unlock_button_fingerprint)).performClick()
        prompted()
        ShadowBiometricPrompt.authenticateCurrentSessionSuccessfully()
        compose.waitForText(string(R.string.garage_title))
    }

    @Test
    fun `too many tries says to wait or use the screen lock, which the link asks for`() {
        biometrics()
        launch()
        prompted()
        ShadowBiometricPrompt.authenticateCurrentSessionWithError(BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT, "Too many attempts")
        compose.waitForText(string(R.string.unlock_wait_or_use_screen_lock))

        compose.onNodeWithText(string(R.string.unlock_use_screen_lock)).performClick()
        prompted()
        // Asking again clears the message.
        compose.onNodeWithText(string(R.string.unlock_wait_or_use_screen_lock)).assertDoesNotExist()
        ShadowBiometricPrompt.authenticateCurrentSessionSuccessfully()
        compose.waitForText(string(R.string.garage_title))
    }

    @Test
    fun `locked out until the screen lock is used says so`() {
        biometrics()
        launch()
        prompted()
        ShadowBiometricPrompt.authenticateCurrentSessionWithError(BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT, "Locked")
        compose.waitForText(string(R.string.unlock_off_until_screen_lock))
    }

    @Test
    fun `a sensor that can't be used passes on Android's own words`() {
        biometrics()
        launch()
        prompted()
        ShadowBiometricPrompt.authenticateCurrentSessionWithError(BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE, " Sensor busy ")
        compose.waitForText("Sensor busy")
    }

    @Test
    fun `any other failure with nothing to say asks to try again`() {
        biometrics()
        launch()
        prompted()
        ShadowBiometricPrompt.authenticateCurrentSessionWithError(BiometricPrompt.BIOMETRIC_ERROR_VENDOR, "")
        compose.waitForText(string(R.string.unlock_couldnt_check))
    }
}

/**
 * Android 10: the screen lock is asked for on the system's confirm-credentials screen, answered as an activity
 * result. The app is told it runs on 10 (Robolectric's sandboxes for two SDKs can't share one test JVM).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = io.cosgriff.glovebox.SHELL_PHONE)
class LockScreenAndroid10Test {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun settings() {
        graph.settings { setWelcomeSeen(); setBiometricLock(true) }
        // A PIN and no fingerprint.
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(true)
        biometricService(app, connected = false)
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.Q)
    }

    @After fun close() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 35)
        if (::scenario.isInitialized) scenario.close()
        graph.close()
    }

    private fun confirmCredentials(): android.content.Intent {
        var asked: android.content.Intent? = null
        compose.waitUntil(10_000) {
            scenario.onActivity { asked = shadowOf(it).peekNextStartedActivityForResult()?.intent }
            asked != null
        }
        return checkNotNull(asked)
    }

    private fun answer(resultCode: Int) = scenario.onActivity {
        val request = shadowOf(it).nextStartedActivityForResult
        shadowOf(it).receiveResult(request.intent, resultCode, null)
    }

    @Test
    fun `a screen lock alone is asked for, with no link to ask for it instead`() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForText(string(R.string.unlock_instruction_screen_lock))
        compose.onNodeWithText(string(R.string.unlock_use_screen_lock)).assertDoesNotExist()
        assertEquals("android.app.action.CONFIRM_DEVICE_CREDENTIAL", confirmCredentials().action)

        answer(Activity.RESULT_OK)
        compose.waitForText(string(R.string.garage_title))
        assertFalse(graph.get<AppLock>().locked.value)
    }

    @Test
    fun `backing out of the system screen leaves the app locked, with nothing said`() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertNotNull(confirmCredentials())
        answer(Activity.RESULT_CANCELED)
        compose.waitForIdle()
        assertTrue(graph.get<AppLock>().locked.value)
        compose.onNodeWithText(string(R.string.unlock_couldnt_check)).assertDoesNotExist()
    }
}

/** Robolectric's biometric service: connected, it says a biometric is enrolled; not, that there is no hardware. */
private fun biometricService(context: android.content.Context, connected: Boolean) =
    org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowBiometricManager>(
        context.getSystemService(android.hardware.biometrics.BiometricManager::class.java),
    ).setCanAuthenticate(connected)
