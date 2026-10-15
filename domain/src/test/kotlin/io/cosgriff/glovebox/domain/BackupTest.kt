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
    fun `names and counts read naturally`() {
        assertEquals("glovebox-backup-2026-10-11.glovebox", BackupRules.fileName(today, "glovebox"))
        assertEquals("2 vehicles · 14 records · 9 photos", BackupRules.contents(2, 14, 9))
        assertEquals("1 vehicle · 1 record · 1 photo", BackupRules.contents(1, 1, 1))
        assertEquals("1 vehicle · 0 records · 0 photos · 2 documents (4.2 MB)", BackupRules.contents(1, 0, 0, 2, 4_400_000))
    }

    @Test
    fun `the restore summary reads as a sentence`() {
        assertEquals("2 vehicles, 14 service records and 9 photos", BackupRules.restoreSummary(2, 14, 9))
        assertEquals("1 vehicle, 1 service record and 1 photo", BackupRules.restoreSummary(1, 1, 1))
        assertEquals("0 vehicles, 0 service records and 0 photos", BackupRules.restoreSummary(0, 0, 0))
        assertEquals("1 vehicle, 3 service records, 2 photos and 1 document", BackupRules.restoreSummary(1, 3, 2, 1))
    }

    @Test
    fun `replacing data is confirmed by typing a word`() {
        assertTrue(TypedConfirmation.matches(" replace ", BackupRules.REPLACE_WORD))
        assertFalse(TypedConfirmation.matches("replac", BackupRules.REPLACE_WORD))
    }
}
