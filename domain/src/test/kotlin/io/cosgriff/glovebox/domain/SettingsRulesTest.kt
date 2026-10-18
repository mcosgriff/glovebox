package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {
    @Test
    fun `stored seconds map back to their choice`() {
        LockDelay.entries.forEach { assertEquals(it, LockDelay.fromSeconds(it.seconds)) }
    }

    @Test
    fun `an unexpected number of seconds becomes the nearest stricter choice`() {
        assertEquals(LockDelay.IMMEDIATELY, LockDelay.fromSeconds(30))
        assertEquals(LockDelay.ONE_MINUTE, LockDelay.fromSeconds(120))
        assertEquals(LockDelay.FIVE_MINUTES, LockDelay.fromSeconds(3600))
        assertEquals(LockDelay.ON_RESTART, LockDelay.fromSeconds(-5))
    }

    @Test
    fun `immediately locks on every return`() {
        assertTrue(LockDelay.relocks(lockAfterSeconds = 0, awaySeconds = 0))
    }

    @Test
    fun `a delay locks only once it has passed`() {
        assertFalse(LockDelay.relocks(lockAfterSeconds = 60, awaySeconds = 59))
        assertTrue(LockDelay.relocks(lockAfterSeconds = 60, awaySeconds = 60))
        assertFalse(LockDelay.relocks(lockAfterSeconds = 300, awaySeconds = 299))
    }

    @Test
    fun `on restart never locks on return`() {
        assertFalse(LockDelay.relocks(lockAfterSeconds = -1, awaySeconds = 1_000_000))
    }
}
