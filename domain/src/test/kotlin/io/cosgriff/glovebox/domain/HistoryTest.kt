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
    fun `a category's jobs are the ones logged, in catalog order`() {
        assertEquals(listOf("oil", "engine-air"), ServiceCatalog.jobsIn("engine", setOf("engine-air", "rotation", "oil", "nope")).map { it.id })
        assertEquals(emptyList<CatalogItem>(), ServiceCatalog.jobsIn("brakes", setOf("oil")))
        assertEquals(emptyList<CatalogItem>(), ServiceCatalog.jobsIn("nope", setOf("oil")))
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

    private data class Rec(val id: Long, val date: LocalDate, val odometer: Int)

    private val swap = ClusterSwap(LocalDate.of(2025, 1, 1), offset = 80_000, startReading = 10)
    private val spring = Rec(1, LocalDate.of(2026, 4, 2), 61_000)
    private val summer = Rec(2, LocalDate.of(2026, 7, 9), 64_500)
    private val pastAddedLater = Rec(3, LocalDate.of(2025, 11, 20), 55_200)

    private fun sorted(records: List<Rec>, order: HistoryOrder, swap: ClusterSwap? = null) =
        HistoryOrdering.sorted(records, order, { it.date }, { swap.toDash(it.odometer, it.date) }, { it.id }).map { it.id }

    @Test
    fun `a past record added last still sorts by its date`() {
        assertEquals(listOf(2L, 1L, 3L), sorted(listOf(spring, summer, pastAddedLater), HistoryOrder()))
        assertEquals(listOf(3L, 1L, 2L), sorted(listOf(spring, summer, pastAddedLater), HistoryOrder(descending = false)))
    }

    @Test
    fun `by mileage, highest or lowest first`() {
        // Logged out of order: the higher reading has the earlier date.
        val typo = Rec(4, LocalDate.of(2026, 3, 1), 70_000)
        val records = listOf(spring, summer, typo)
        assertEquals(listOf(4L, 2L, 1L), sorted(records, HistoryOrder(HistorySort.MILEAGE)))
        assertEquals(listOf(1L, 2L, 4L), sorted(records, HistoryOrder(HistorySort.MILEAGE, descending = false)))
        assertEquals(listOf(2L, 1L, 4L), sorted(records, HistoryOrder(HistorySort.DATE)))
    }

    @Test
    fun `mileage is compared in dash terms across a cluster replacement`() {
        // Typed off the old cluster at 85,000 (5,000 on the new dash), then 9,000 on the new one.
        val old = Rec(5, LocalDate.of(2024, 12, 1), 85_000)
        val new = Rec(6, LocalDate.of(2025, 6, 1), 9_000)
        assertEquals(listOf(6L, 5L), sorted(listOf(old, new), HistoryOrder(HistorySort.MILEAGE), swap))
        assertEquals(listOf(5L, 6L), sorted(listOf(old, new), HistoryOrder(HistorySort.MILEAGE)))
    }

    @Test
    fun `ties fall to the other key and then the id`() {
        val day = LocalDate.of(2026, 5, 5)
        val a = Rec(7, day, 62_000)
        val b = Rec(8, day, 62_400)
        val c = Rec(9, day, 62_400)
        assertEquals(listOf(9L, 8L, 7L), sorted(listOf(a, b, c), HistoryOrder()))
        assertEquals(listOf(7L, 8L, 9L), sorted(listOf(c, a, b), HistoryOrder(descending = false)))
        val sameReading = Rec(10, LocalDate.of(2026, 6, 6), 62_000)
        assertEquals(listOf(10L, 7L), sorted(listOf(a, sameReading), HistoryOrder(HistorySort.MILEAGE)))
    }
}
