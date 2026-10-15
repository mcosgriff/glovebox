package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryImportTest {
    private val day = LocalDate.of(2024, 3, 1)

    // The vehicle's schedule: two typical items and one of the user's own.
    private val schedule = listOf(
        ScheduleRef(1, "Engine oil & filter", "oil", tracked = true),
        ScheduleRef(2, "Tire rotation", "rotation", tracked = true),
        ScheduleRef(3, "Grease driveshaft", null, tracked = true),
    )

    private fun file(date: LocalDate = day, odometer: Int = 41_000, vararg work: ImportedWork = arrayOf(ImportedWork("Oil & filter change", "oil")), diy: Boolean = true, shop: String? = null, cost: Long? = null, notes: String? = null) =
        ImportedRecord(date, odometer, work.toList(), diy, shop, cost, notes)

    private fun saved(id: Long, date: LocalDate = day, odometer: Int = 41_000, labels: List<String> = listOf("Oil & filter change"), itemIds: Set<Long> = setOf(1), lastDone: Boolean = false, cost: Long? = null) =
        SavedRecord(id, date, odometer, labels, itemIds, diy = true, shopName = null, costCents = cost, notes = null, enteredAsLastDone = lastDone)

    @Test
    fun `a service key resets the typical item, and a bare label resets the user's own item of that name`() {
        assertEquals(ResolvedWork("Oil & filter change", "oil", 1), HistoryImport.resolve(ImportedWork("Oil & filter change", "oil"), schedule))
        assertEquals(ResolvedWork("grease DRIVESHAFT", null, 3), HistoryImport.resolve(ImportedWork("grease DRIVESHAFT"), schedule))
        // A label alone never resets a typical item, whatever it says.
        assertNull(HistoryImport.resolve(ImportedWork("Tire rotation"), schedule).scheduleItemId)
        // A service with no reminder behind it is history only, but still filed under its category.
        assertEquals(ResolvedWork("New pads", "front-pads", null), HistoryImport.resolve(ImportedWork("New pads", "front-pads"), schedule))
        // The vehicle doesn't have that typical item.
        assertNull(HistoryImport.resolve(ImportedWork("Coolant", "coolant"), schedule).scheduleItemId)
    }

    @Test
    fun `records match on date and odometer`() {
        val plan = HistoryImport.plan(
            items = listOf(file(), file(day.plusDays(1)), file(odometer = 41_001), file(day.minusYears(1), 30_000, cost = 4_000)),
            existing = listOf(saved(1), saved(2, day.minusYears(1), 30_000)),
            schedule = schedule,
        )
        assertEquals(2, plan.new.size)
        assertEquals(listOf(1L), plan.same.map { it.second.id })
        assertEquals(listOf(2L), plan.differs.map { it.second.id })
    }

    @Test
    fun `the order of the work lines and their capitals don't make a difference`() {
        val item = file(work = arrayOf(ImportedWork("tire rotation", "rotation"), ImportedWork("Oil & Filter Change", "oil")))
        val plan = HistoryImport.plan(listOf(item), listOf(saved(1, labels = listOf("Oil & filter change", "Tire rotation"))), schedule)
        assertEquals(1, plan.same.size)
    }

    @Test
    fun `a bare last done is filled in by a record that resets the same reminder`() {
        val plan = HistoryImport.plan(listOf(file(shop = "Corner Garage", diy = false)), listOf(saved(1, labels = listOf("Engine oil & filter"), lastDone = true)), schedule)
        assertEquals(listOf(1L), plan.fills.map { it.second.id })
        assertEquals(1, plan.changes(ImportMode.ADD_NEW))
    }

    @Test
    fun `a bare last done is not written over by a record that would lose its reminder`() {
        val item = file(work = arrayOf(ImportedWork("Replaced tail lamp")))
        val plan = HistoryImport.plan(listOf(item), listOf(saved(1, labels = listOf("Engine oil & filter"), lastDone = true)), schedule)
        assertTrue(plan.fills.isEmpty())
        assertEquals(1, plan.differs.size)
        assertEquals(0, plan.changes(ImportMode.ADD_NEW))
    }

    @Test
    fun `imported history moves reminders the way logged work does`() {
        val oil = TrackedItem(1, "Engine oil & filter", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        val plan = HistoryImport.plan(listOf(file()), emptyList(), schedule)
        val after = HistoryImport.entriesAfter(plan, ImportMode.ADD_NEW, emptyList(), schedule, swap = null)

        val change = LogEdits.reminderChanges(listOf(oil), emptyList(), after).single()
        assertNull(change.before)
        assertEquals(LastDone(day, 41_000), change.after)
    }

    @Test
    fun `an older imported record doesn't move a reminder a newer service already set`() {
        val oil = TrackedItem(1, "Engine oil & filter", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        val before = listOf(ServiceEntry(9, day.plusMonths(6), 46_000, setOf(1)))
        val plan = HistoryImport.plan(listOf(file()), emptyList(), schedule)
        val after = HistoryImport.entriesAfter(plan, ImportMode.ADD_NEW, before, schedule, swap = null)
        assertTrue(LogEdits.reminderChanges(listOf(oil), before, after).isEmpty())
    }

    @Test
    fun `replacing a record changes which reminders it resets`() {
        val rotation = TrackedItem(2, "Tire rotation", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        val before = listOf(ServiceEntry(1, day, 41_000, setOf(1)))
        val item = file(work = arrayOf(ImportedWork("Oil & filter change", "oil"), ImportedWork("Tire rotation", "rotation")))
        val plan = HistoryImport.plan(listOf(item), listOf(saved(1)), schedule)

        assertTrue(LogEdits.reminderChanges(listOf(rotation), before, HistoryImport.entriesAfter(plan, ImportMode.ADD_NEW, before, schedule, null)).isEmpty())
        val moved = LogEdits.reminderChanges(listOf(rotation), before, HistoryImport.entriesAfter(plan, ImportMode.REPLACE_MATCHING, before, schedule, null))
        assertEquals(LastDone(day, 41_000), moved.single().after)
    }

    @Test
    fun `records that don't fit the readings around them are pointed out`() {
        val before = listOf(ServiceEntry(1, day, 41_000, setOf(1)))
        val items = listOf(
            file(day.minusMonths(6), 45_000), // higher than the records after it
            file(day.plusMonths(6), 39_000), // lower than the records before it
            file(day.plusMonths(7), 47_000), // fine
        )
        val after = HistoryImport.entriesAfter(HistoryImport.plan(items, listOf(saved(1)), schedule), ImportMode.ADD_NEW, before, schedule, null)
        assertEquals(listOf(45_000, 39_000), HistoryImport.doubtful(after, emptyList(), DistanceUnit.MILES).map { it.entry.odometer })
    }

    @Test
    fun `one mistyped reading is named, not every record it disagrees with`() {
        val items = listOf(file(day.minusMonths(14), 43_000), file(day.minusMonths(6), 36_150), file(day, 41_000), file(day.plusMonths(3), 44_000))
        val after = HistoryImport.entriesAfter(HistoryImport.plan(items, emptyList(), schedule), ImportMode.ADD_NEW, emptyList(), schedule, null)
        assertEquals(listOf(43_000), HistoryImport.doubtful(after, emptyList(), DistanceUnit.MILES).map { it.entry.odometer })
        // Two visits on one day in either order are fine.
        val sameDay = listOf(file(day, 41_000), file(day, 41_020))
        assertTrue(HistoryImport.doubtful(HistoryImport.entriesAfter(HistoryImport.plan(sameDay, emptyList(), schedule), ImportMode.ADD_NEW, emptyList(), schedule, null), emptyList(), DistanceUnit.MILES).isEmpty())
    }

    @Test
    fun `the newest reading is asked about when it jumps far ahead`() {
        // About 10,000 a year, then 60,000 more in eighteen months: a slipped digit, with nothing after it to disagree.
        val saved = listOf(OdometerReading(day.minusYears(3), 50_000), OdometerReading(day.minusYears(2), 60_000))
        val items = listOf(file(day.minusYears(1), 70_000), file(day.plusMonths(6), 130_000))
        val after = HistoryImport.entriesAfter(HistoryImport.plan(items, emptyList(), schedule), ImportMode.ADD_NEW, emptyList(), schedule, null)
        val doubt = HistoryImport.doubtful(after, saved, DistanceUnit.MILES).single()
        assertEquals(130_000, doubt.entry.odometer)
        assertEquals(OdometerReading(day.minusYears(1), 70_000), doubt.before)
        // The same visit after a believable stretch isn't.
        val fine = listOf(file(day.minusYears(1), 70_000), file(day.plusMonths(6), 85_000))
        val ok = HistoryImport.entriesAfter(HistoryImport.plan(fine, emptyList(), schedule), ImportMode.ADD_NEW, emptyList(), schedule, null)
        assertTrue(HistoryImport.doubtful(ok, saved, DistanceUnit.MILES).isEmpty())
        // Too fast for any vehicle, with no history to judge a pace by.
        val first = listOf(file(day.minusMonths(2), 20_000), file(day, 45_000))
        val fast = HistoryImport.entriesAfter(HistoryImport.plan(first, emptyList(), schedule), ImportMode.ADD_NEW, emptyList(), schedule, null)
        assertEquals(listOf(45_000), HistoryImport.doubtful(fast, emptyList(), DistanceUnit.MILES).map { it.entry.odometer })
    }

    @Test
    fun `readings from before a cluster replacement are compared on the same footing`() {
        val swap = ClusterSwaps.of(LocalDate.of(2025, 1, 1), oldReading = 60_000, newReading = 0)
        // Typed from the old cluster; the saved record after the swap reads far lower on the new one.
        val before = listOf(swap.toDash(ServiceEntry(1, LocalDate.of(2025, 6, 1), 4_000, setOf(1))))
        val plan = HistoryImport.plan(listOf(file(day, 41_000)), emptyList(), schedule)
        val after = HistoryImport.entriesAfter(plan, ImportMode.ADD_NEW, before, schedule, swap)
        assertTrue(HistoryImport.doubtful(after, swap.readings(emptyList()), DistanceUnit.MILES).isEmpty())
    }

    @Test
    fun `an imported service line counts for the user's own item that answers to it, as well as the typical one`() {
        val both = schedule + ScheduleRef(4, "Oil change", null, tracked = true, service = "oil")
        assertEquals(setOf(1L, 4L), HistoryImport.itemIds(file(), both))
        // With no typical oil item, the line is saved against the user's own.
        val onlyMine = listOf(ScheduleRef(4, "Oil change", null, tracked = true, service = "oil"))
        assertEquals(4L, HistoryImport.resolve(ImportedWork("Oil & filter change", "oil"), onlyMine).scheduleItemId)
        assertEquals(setOf(4L), HistoryImport.itemIds(file(), onlyMine))
    }
}
