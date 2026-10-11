package io.cosgriff.glovebox.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedConfirmationTest {
    @Test
    fun `case and stray spaces don't matter`() {
        assertTrue(TypedConfirmation.matches("4runner ", "4Runner"))
        assertTrue(TypedConfirmation.matches(" old  blue", "Old Blue"))
    }

    @Test
    fun `anything else doesn't confirm`() {
        assertFalse(TypedConfirmation.matches("4Runne", "4Runner"))
        assertFalse(TypedConfirmation.matches("", "4Runner"))
        assertFalse(TypedConfirmation.matches("OldBlue", "Old Blue"))
    }

    @Test
    fun `a blank name can never be confirmed by typing nothing`() {
        assertFalse(TypedConfirmation.matches("", " "))
    }
}
