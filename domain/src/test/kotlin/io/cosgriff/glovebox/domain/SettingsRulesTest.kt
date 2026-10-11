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

    @Test
    fun `theme note says what is showing`() {
        assertEquals("Follows your phone \u2014 currently light", SettingsText.theme(followsPhone = true, dark = false))
        assertEquals("Follows your phone \u2014 currently dark", SettingsText.theme(followsPhone = true, dark = true))
        assertEquals("Always dark, whatever your phone is set to", SettingsText.theme(followsPhone = false, dark = true))
    }

    @Test
    fun `notifications are only On when every kind is`() {
        assertEquals("On", SettingsText.notifications(NoticePrefs()))
        assertEquals("3 of 4 on", SettingsText.notifications(NoticePrefs(mileage = false)))
        assertEquals("Off", SettingsText.notifications(NoticePrefs(serviceDue = false, overdue = false, recalls = false, mileage = false)))
    }

    @Test
    fun `the schedule row names the vehicle, counts several, and is absent with none`() {
        assertNull(SettingsText.schedules(emptyList()))
        assertEquals("Tacoma", SettingsText.schedules(listOf("Tacoma")))
        assertEquals("2 vehicles", SettingsText.schedules(listOf("Tacoma", "Civic")))
    }
}
