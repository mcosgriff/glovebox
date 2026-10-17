package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test
    fun `short passwords rate weak however clever`() {
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("aB3\$xY9!q"))
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("password1"))
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("a"))
    }

    @Test
    fun `repetition and a single kind of character are weak`() {
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("aaaaaaaaaaaaaaaaaaaaaaaa"))
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("abababababab"))
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("qwertyuiopas"))
    }

    @Test
    fun `length matters more than symbols`() {
        assertEquals(PasswordStrength.FAIR, BackupRules.strength("Tr0ub4dor&3x"))
        assertEquals(PasswordStrength.STRONG, BackupRules.strength("purple wagon 42"))
        assertEquals(PasswordStrength.STRONG, BackupRules.strength("correcthorsebattery"))
        assertEquals(PasswordStrength.VERY_STRONG, BackupRules.strength("correct horse battery staple"))
    }

    @Test
    fun `bars follow the strength`() {
        assertEquals(listOf(1, 2, 3, 4), PasswordStrength.entries.map { it.bars })
    }

    @Test
    fun `no reminder when there is nothing to lose`() {
        assertFalse(BackupRules.shouldRemind(hasData = false, lastBackupOn = null, lastRemindedOn = null, today = today))
    }

    @Test
    fun `reminds when there has never been a backup or it is a month old`() {
        assertTrue(BackupRules.shouldRemind(true, null, null, today))
        assertTrue(BackupRules.shouldRemind(true, today.minusDays(30), null, today))
        assertFalse(BackupRules.shouldRemind(true, today.minusDays(29), null, today))
    }

    @Test
    fun `reminds at most once a month`() {
        assertFalse(BackupRules.shouldRemind(true, null, today.minusDays(29), today))
        assertTrue(BackupRules.shouldRemind(true, null, today.minusDays(30), today))
        // A fresh backup silences it whatever was last said.
        assertFalse(BackupRules.shouldRemind(true, today, today.minusDays(60), today))
    }

    @Test
    fun `a backup is named by its date`() {
        assertEquals("glovebox-backup-2026-10-11.glovebox", BackupRules.fileName(today, "glovebox"))
    }

    @Test
    fun `replacing data is confirmed by typing a word`() {
        assertTrue(TypedConfirmation.matches(" replace ", BackupRules.REPLACE_WORD))
        assertFalse(TypedConfirmation.matches("replac", BackupRules.REPLACE_WORD))
    }
}
