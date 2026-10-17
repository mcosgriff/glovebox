package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryTest {
    private val oil = TrackedItem(1, "Oil", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
    private val rotation = TrackedItem(2, "Rotation", Interval(5_000, null), DueRule.WHICHEVER_FIRST)
    private val june = ServiceEntry(10, LocalDate.of(2026, 6, 14), 60_100, setOf(1, 2))
    private val october = ServiceEntry(11, LocalDate.of(2026, 10, 10), 68_412, setOf(1))

    @Test
    fun `deleting the latest record rolls its reminders back to the one before`() {
        val changes = LogEdits.reminderChanges(listOf(oil, rotation), listOf(june, october), listOf(june))
        assertEquals(1, changes.size) // rotation wasn't on the October record
        assertEquals(oil, changes.single().item)
        assertEquals(LastDone(october.date, 68_412), changes.single().before)
        assertEquals(LastDone(june.date, 60_100), changes.single().after)
    }

    @Test
    fun `deleting the only record leaves the reminder never logged`() {
        val changes = LogEdits.reminderChanges(listOf(oil), listOf(october), emptyList())
        assertNull(changes.single().after)
    }

    @Test
    fun `editing an older record changes nothing`() {
        val edited = june.copy(odometer = 60_150)
        assertEquals(emptyList<ReminderChange>(), LogEdits.reminderChanges(listOf(oil), listOf(june, october), listOf(edited, october)))
    }

    @Test
    fun `editing the odometer of the latest record moves the reminder`() {
        val edited = october.copy(odometer = 68_450)
        val change = LogEdits.reminderChanges(listOf(oil), listOf(june, october), listOf(june, edited)).single()
        assertEquals(68_450, change.after?.odometer)
    }

    @Test
    fun `latest record for an item`() {
        val entries = listOf(june, october)
        assertTrue(LogEdits.isLatestFor(1, october, entries))
        assertFalse(LogEdits.isLatestFor(1, june, entries))
        assertTrue(LogEdits.isLatestFor(2, june, entries))
        assertFalse(LogEdits.isLatestFor(2, october, entries)) // not on that record
    }

    @Test
    fun `next due counts from the last time it was done`() {
        val last = LastDone(LocalDate.of(2026, 10, 10), 68_412)
        assertEquals(NextDue(73_412, LocalDate.of(2027, 4, 10)), oil.interval.nextDueAfter(last))
        assertEquals(NextDue(73_412, null), rotation.interval.nextDueAfter(last))
    }

    @Test
    fun `money formatting`() {
        assertEquals("$89.50", Money.format(8_950))
        assertEquals("$1,234.05", Money.format(123_405))
        assertEquals("$0.00", Money.format(0))
    }

    @Test
    fun `catalog category lookup`() {
        assertEquals("tires", ServiceCatalog.categoryOf("rotation")?.id)
        assertNull(ServiceCatalog.categoryOf(null))
        assertNull(ServiceCatalog.categoryOf("nope"))
    }

    @Test
    fun `last entry is the visit last done comes from`() {
        val entries = listOf(
            ServiceEntry(1, LocalDate.of(2026, 1, 5), 60_000, setOf(7L)),
            ServiceEntry(2, LocalDate.of(2026, 8, 1), 66_000, setOf(7L, 8L)),
            ServiceEntry(3, LocalDate.of(2026, 9, 1), 67_000, setOf(8L)),
        )
        assertEquals(2L, ServiceLog.lastEntry(7, entries)?.id)
        assertEquals(null, ServiceLog.lastEntry(9, entries))
    }

    @Test
    fun `resetters name the log lines that reset a reminder`() {
        assertEquals(listOf("oil"), ServiceCatalog.resetters("oil"))
        assertEquals(emptyList<String>(), ServiceCatalog.resetters(null))
        assertEquals(emptyList<String>(), ServiceCatalog.resetters("not-a-key"))
    }
}
