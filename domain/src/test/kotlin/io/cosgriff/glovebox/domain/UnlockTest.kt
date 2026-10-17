package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockTest {
    @Test
    fun `no screen lock means nothing to ask for`() {
        assertNull(Unlock.options(ScreenLock.NONE, fingerprintSensor = true, screenLockAlone = true))
    }

    @Test
    fun `a fingerprint phone is told to touch the sensor and can skip to the screen lock`() {
        val o = Unlock.options(ScreenLock.BIOMETRIC, fingerprintSensor = true, screenLockAlone = true)!!
        assertEquals(UnlockPrompt.FINGERPRINT, o.prompt)
        assertTrue(o.fingerprint)
        assertTrue(o.screenLockLink)
    }

    @Test
    fun `a phone with no fingerprint sensor is not told to touch one`() {
        val o = Unlock.options(ScreenLock.BIOMETRIC, fingerprintSensor = false, screenLockAlone = true)!!
        assertEquals(UnlockPrompt.BIOMETRIC, o.prompt)
        assertFalse(o.fingerprint)
    }

    @Test
    fun `before Android 11 there is no separate screen lock link`() {
        assertFalse(Unlock.options(ScreenLock.BIOMETRIC, fingerprintSensor = true, screenLockAlone = false)!!.screenLockLink)
    }

    @Test
    fun `a phone with only a screen lock has nothing to skip`() {
        val o = Unlock.options(ScreenLock.CREDENTIAL, fingerprintSensor = true, screenLockAlone = true)!!
        assertEquals(UnlockPrompt.SCREEN_LOCK, o.prompt)
        assertFalse(o.fingerprint)
        assertFalse(o.screenLockLink)
    }

    @Test
    fun `cancelling says nothing`() {
        assertNull(Unlock.message(UnlockError.CANCELLED, ScreenLock.BIOMETRIC, "Authentication canceled"))
    }

    @Test
    fun `a lockout points at the screen lock only where there is another way in`() {
        assertEquals(UnlockMessage.WaitOrUseScreenLock, Unlock.message(UnlockError.LOCKED_OUT, ScreenLock.BIOMETRIC, "x"))
        assertEquals(UnlockMessage.Wait, Unlock.message(UnlockError.LOCKED_OUT, ScreenLock.CREDENTIAL, "x"))
        assertEquals(UnlockMessage.OffUntilScreenLock, Unlock.message(UnlockError.LOCKED_OUT_UNTIL_SCREEN_LOCK, ScreenLock.BIOMETRIC, "x"))
    }

    @Test
    fun `other failures use Android's wording, or a plain line when it gives none`() {
        assertEquals(
            UnlockMessage.FromSystem("Fingerprint hardware not available."),
            Unlock.message(UnlockError.UNAVAILABLE, ScreenLock.BIOMETRIC, " Fingerprint hardware not available. "),
        )
        assertEquals(UnlockMessage.CouldntCheck, Unlock.message(UnlockError.OTHER, ScreenLock.BIOMETRIC, ""))
        assertEquals(UnlockMessage.CouldntCheck, Unlock.message(UnlockError.OTHER, ScreenLock.BIOMETRIC, null))
    }
}
