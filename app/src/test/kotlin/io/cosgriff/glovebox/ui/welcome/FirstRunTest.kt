package io.cosgriff.glovebox.ui.welcome

import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.TestGraph
import io.cosgriff.glovebox.ui.settings.awaitValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The welcome shows only with the flag unset and no vehicles, and records what the user chose. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class FirstRunTest {
    @get:Rule val g = TestGraph()

    @Before fun reset() = runBlocking { g.resetSettings() }

    private fun firstRun() = FirstRun(g.settings, g.db.vehicles(), g.appScope)

    @Test
    fun `a phone with nothing on it is welcomed`() = runTest {
        assertEquals(true, firstRun().needsWelcome.awaitValue { it != null })
    }

    @Test
    fun `an install from before the welcome, with vehicles, isn't, and is marked as needing none`() = runTest {
        g.vehicle()
        assertEquals(false, firstRun().needsWelcome.awaitValue { it != null })
        assertTrue(g.settings.settings.first().welcomeSeen)
    }

    @Test
    fun `once seen it isn't shown again`() = runTest {
        g.settings.setWelcomeSeen()
        assertEquals(false, firstRun().needsWelcome.awaitValue { it != null })
    }

    @Test
    fun `finishing with notifications off turns every kind and the backup reminder off`() = runTest {
        val run = firstRun()
        run.needsWelcome.awaitValue { it != null }
        run.finish(WelcomeChoice(lockApp = false, notifications = false))
        val s = g.settings.settings.first()
        assertFalse(s.biometricLock)
        assertFalse(s.notices.serviceDue || s.notices.overdue || s.notices.recalls || s.notices.mileage)
        assertFalse(s.backupReminder)
        assertTrue(s.welcomeSeen)
        assertEquals(false, run.needsWelcome.value)
    }

    @Test
    fun `finishing with notifications on leaves them as they are`() = runTest {
        val run = firstRun()
        run.needsWelcome.awaitValue { it != null }
        run.finish(WelcomeChoice(lockApp = true, notifications = true))
        val s = g.settings.settings.first()
        assertTrue(s.biometricLock)
        assertTrue(s.notices.serviceDue && s.notices.overdue && s.notices.recalls && s.notices.mileage)
        assertTrue(s.backupReminder)
    }
}
