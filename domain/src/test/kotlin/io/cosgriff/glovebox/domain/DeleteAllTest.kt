package io.cosgriff.glovebox.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAllTest {
    @Test
    fun `the word is forgiving about case and spaces but nothing else`() {
        assertTrue(TypedConfirmation.matches("DELETE", DeleteAll.WORD))
        assertTrue(TypedConfirmation.matches(" delete ", DeleteAll.WORD))
        assertFalse(TypedConfirmation.matches("DELET", DeleteAll.WORD))
        assertFalse(TypedConfirmation.matches("", DeleteAll.WORD))
    }

    @Test
    fun `a phone with a screen lock is asked for it`() {
        assertTrue(DeleteAll.asksForScreenLock(ScreenLock.BIOMETRIC))
        assertTrue(DeleteAll.asksForScreenLock(ScreenLock.CREDENTIAL))
    }

    @Test
    fun `a phone with no screen lock has no second check`() {
        assertFalse(DeleteAll.asksForScreenLock(ScreenLock.NONE))
    }
}
