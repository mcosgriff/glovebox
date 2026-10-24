package io.cosgriff.glovebox.lock

import androidx.lifecycle.ProcessLifecycleOwner
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.LockDelay
import io.cosgriff.glovebox.idleMainUntil
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class AppLockTest {
    private val graph get() = ShellTestApp.graph
    private val owner = ProcessLifecycleOwner.get()

    @After fun close() = graph.close()

    /** An AppLock once its settings have been read. */
    private fun lock(biometric: Boolean = true, after: LockDelay = LockDelay.ONE_MINUTE): AppLock {
        graph.settings { setBiometricLock(biometric); setLockAfterSeconds(after.seconds) }
        val lock = AppLock(graph.get<SettingsRepository>(), graph.scope)
        // The settings arrive on the main thread, from DataStore's own.
        val settings = AppLock::class.java.getDeclaredField("settings").apply { isAccessible = true }.get(lock) as StateFlow<*>
        idleMainUntil { settings.value != null }
        return lock
    }

    private fun away(lock: AppLock, seconds: Long) {
        lock.onStop(owner)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds))
        lock.onStart(owner)
    }

    @Test
    fun `every start of the process begins locked, until unlocked`() {
        val lock = lock()
        assertTrue(lock.locked.value)
        lock.unlock()
        assertFalse(lock.locked.value)
    }

    @Test
    fun `the lock counts as off only once the settings say so`() {
        assertFalse(lock(biometric = true).lockDisabled())
        assertTrue(lock(biometric = false).lockDisabled())
    }

    @Test
    fun `coming back after the delay locks again, and sooner doesn't`() {
        val lock = lock(after = LockDelay.ONE_MINUTE)
        lock.unlock()
        away(lock, 30)
        assertFalse(lock.locked.value)
        away(lock, 61)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `set to lock immediately, any trip to the background locks`() {
        val lock = lock(after = LockDelay.IMMEDIATELY)
        lock.unlock()
        away(lock, 0)
        assertTrue(lock.locked.value)
    }

    @Test
    fun `set to lock only on restart, the background never locks`() {
        val lock = lock(after = LockDelay.ON_RESTART)
        lock.unlock()
        away(lock, 24 * 60 * 60)
        assertFalse(lock.locked.value)
    }

    @Test
    fun `with the lock off, the background never locks`() {
        val lock = lock(biometric = false, after = LockDelay.IMMEDIATELY)
        lock.unlock()
        away(lock, 600)
        assertFalse(lock.locked.value)
    }

    @Test
    fun `unlocking forgets the trip to the background that asking for the screen lock was`() {
        val lock = lock(after = LockDelay.IMMEDIATELY)
        // Android 10's confirm-credentials screen covers the app; its answer comes just before the app is told it is back.
        lock.onStop(owner)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20))
        lock.unlock()
        lock.onStart(owner)
        assertFalse(lock.locked.value)
    }

    @Test
    fun `a return with no trip to the background recorded changes nothing`() {
        val lock = lock(after = LockDelay.IMMEDIATELY)
        lock.unlock()
        lock.onStart(owner)
        assertFalse(lock.locked.value)
    }
}
