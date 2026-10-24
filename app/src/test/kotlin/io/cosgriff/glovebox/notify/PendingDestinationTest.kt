package io.cosgriff.glovebox.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingDestinationTest {
    @Test
    fun `a destination is taken exactly once`() {
        val pending = PendingDestination()
        pending.set(Destination.Backup)
        assertEquals(Destination.Backup, pending.destination.value)
        assertEquals(Destination.Backup, pending.take())
        assertNull(pending.take())
    }

    @Test
    fun `an ordinary launch doesn't wipe out a destination still waiting`() {
        val pending = PendingDestination()
        pending.set(Destination.Reminders)
        pending.set(null)
        assertEquals(Destination.Reminders, pending.destination.value)
    }

    @Test
    fun `clearing only clears the destination it was asked about, not a newer one`() {
        val pending = PendingDestination()
        pending.set(Destination.Reminders)
        pending.set(Destination.Recalls(2))
        assertFalse(pending.clear(Destination.Reminders))
        assertEquals(Destination.Recalls(2), pending.destination.value)
        assertTrue(pending.clear(Destination.Recalls(2)))
        assertNull(pending.destination.value)
    }
}
