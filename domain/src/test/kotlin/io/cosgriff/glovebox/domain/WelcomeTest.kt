package io.cosgriff.glovebox.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeTest {
    @Test
    fun `shown once, on a phone with nothing on it`() {
        assertTrue(Welcome.shouldShow(alreadySeen = false, hasVehicles = false))
        assertFalse(Welcome.shouldShow(alreadySeen = true, hasVehicles = false))
    }

    @Test
    fun `an install that already has vehicles never sees it`() {
        assertFalse(Welcome.shouldShow(alreadySeen = false, hasVehicles = true))
        assertFalse(Welcome.shouldShow(alreadySeen = true, hasVehicles = true))
    }
}
