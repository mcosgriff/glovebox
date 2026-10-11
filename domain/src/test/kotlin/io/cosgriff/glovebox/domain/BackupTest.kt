package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test
    fun `short passwords are refused however clever`() {
        assertEquals(PasswordStrength.TOO_SHORT, BackupRules.strength("aB3\$xY9!q"))
        assertFalse(PasswordStrength.TOO_SHORT.allowed)
    }

    @Test
    fun `repetition and a single kind of character are weak`() {
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("aaaaaaaaaaaaaaaaaaaaaaaa"))
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("abababababab"))
        assertEquals(PasswordStrength.WEAK, BackupRules.strength("qwertyuiopas"))
        assertFalse(PasswordStrength.WEAK.allowed)
    }

    @Test
    fun `length matters more than symbols`() {
        assertEquals(PasswordStrength.FAIR, BackupRules.strength("Tr0ub4dor&3x"))
        assertEquals(PasswordStrength.STRONG, BackupRules.strength("purple wagon 42"))
        assertEquals(PasswordStrength.STRONG, BackupRules.strength("correcthorsebattery"))
        assertEquals(PasswordStrength.VERY_STRONG, BackupRules.strength("correct horse battery staple"))
        assertTrue(PasswordStrength.FAIR.allowed)
    }

    @Test
    fun `bars follow the strength`() {
        assertEquals(listOf(0, 1, 2, 3, 4), PasswordStrength.entries.map { it.bars })
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
    fun `names and counts read naturally`() {
        assertEquals("glovebox-backup-2026-10-11.glovebox", BackupRules.fileName(today, "glovebox"))
        assertEquals("2 vehicles · 14 records · 9 photos", BackupRules.contents(2, 14, 9))
        assertEquals("1 vehicle · 1 record · 1 photo", BackupRules.contents(1, 1, 1))
    }
}
