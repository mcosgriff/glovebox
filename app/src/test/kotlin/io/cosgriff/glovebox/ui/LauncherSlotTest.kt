package io.cosgriff.glovebox.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSlotTest {
    private val first: () -> Unit = {}
    private val second: () -> Unit = {}

    @Test
    fun `the attached launcher is the one used, until the activity that attached it lets it go`() {
        val slot = LauncherSlot<() -> Unit>()
        assertNull(slot.launcher)
        slot.attach(first)
        assertSame(first, slot.launcher)
        assertTrue(slot.detach(first))
        assertNull(slot.launcher)
    }

    @Test
    fun `an old activity going after a new one attached doesn't take the new one's launcher`() {
        val slot = LauncherSlot<() -> Unit>()
        slot.attach(first)
        slot.attach(second)
        assertFalse(slot.detach(first))
        assertSame(second, slot.launcher)
    }
}
