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

    /** The log with [items] added as new records, numbered below zero as unsaved ones are. */
    private fun withFile(before: List<ServiceEntry>, items: List<ImportedRecord>) =
        before + items.mapIndexed { i, r -> ServiceEntry(-1L - i, r.date, r.odometer, emptySet()) }

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
    fun `jobs are part of the work, but a file with none says nothing about the saved ones`() {
        val priced = file(work = arrayOf(ImportedWork("Spark plugs", job = 0), ImportedWork("Ignition coils", job = 0))).copy(jobs = listOf(ImportedJob("Plugs and coils", 18_000, 21_240)))
        val labels = listOf("ignition COILS", "Spark plugs")
        val savedJob = JobSummary("plugs and coils", 18_000, 21_240, listOf("Ignition coils", "spark plugs"))
        fun plan(item: ImportedRecord, jobs: List<JobSummary>) = HistoryImport.plan(listOf(item), listOf(saved(1, labels = labels).copy(jobs = jobs)), schedule)
        // The same jobs, whatever the order and capitals.
        assertEquals(1, plan(priced, listOf(savedJob)).same.size)
        assertEquals(1, plan(priced, listOf(savedJob.copy(partsCents = 21_241))).differs.size)
        assertEquals(1, plan(priced, emptyList()).differs.size)
        // Without jobs the file can't say the prices are different.
        val unpriced = file(work = arrayOf(ImportedWork("Spark plugs"), ImportedWork("Ignition coils")))
        assertEquals(1, plan(unpriced, listOf(savedJob)).same.size)
    }

    @Test
    fun `a file with no jobs leaves the saved jobs on the same lines`() {
        val item = file(work = arrayOf(ImportedWork("ignition coils"), ImportedWork("Spark plugs"), ImportedWork("Tire rotation")))
        val saved = listOf("Spark plugs" to 7L, "Tire rotation" to null, "Ignition coils" to 7L)
        assertEquals(listOf(7L, 7L, null), HistoryImport.keptJobs(item, saved))
        // The same label twice takes the saved lines' jobs in turn.
        val twice = file(work = arrayOf(ImportedWork("Wiper blade"), ImportedWork("Wiper blade")))
        assertEquals(listOf(3L, null), HistoryImport.keptJobs(twice, listOf("Wiper blade" to 3L, "wiper blade" to null)))
        // Other lines, or jobs of the file's own, and the saved jobs go with the lines they priced.
        assertNull(HistoryImport.keptJobs(file(), saved))
        assertNull(HistoryImport.keptJobs(item.copy(jobs = listOf(ImportedJob(laborCents = 1))), saved))
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
    fun `records that don't fit the readings around them are pointed out`() {
        val before = listOf(ServiceEntry(1, day, 41_000, setOf(1)))
        val items = listOf(
            file(day.minusMonths(6), 45_000), // higher than the records after it
            file(day.plusMonths(6), 39_000), // lower than the records before it
            file(day.plusMonths(7), 47_000), // fine
        )
        val after = withFile(before, items)
        assertEquals(listOf(45_000, 39_000), HistoryImport.doubtful(after, emptyList(), DistanceUnit.MILES).map { it.entry.odometer })
    }

    @Test
    fun `one mistyped reading is named, not every record it disagrees with`() {
        val items = listOf(file(day.minusMonths(14), 43_000), file(day.minusMonths(6), 36_150), file(day, 41_000), file(day.plusMonths(3), 44_000))
        val after = withFile(emptyList(), items)
        assertEquals(listOf(43_000), HistoryImport.doubtful(after, emptyList(), DistanceUnit.MILES).map { it.entry.odometer })
        // Two visits on one day in either order are fine.
        val sameDay = listOf(file(day, 41_000), file(day, 41_020))
        assertTrue(HistoryImport.doubtful(withFile(emptyList(), sameDay), emptyList(), DistanceUnit.MILES).isEmpty())
    }

    @Test
    fun `the newest reading is asked about when it jumps far ahead`() {
        // About 10,000 a year, then 60,000 more in eighteen months: a slipped digit, with nothing after it to disagree.
        val saved = listOf(OdometerReading(day.minusYears(3), 50_000), OdometerReading(day.minusYears(2), 60_000))
        val items = listOf(file(day.minusYears(1), 70_000), file(day.plusMonths(6), 130_000))
        val after = withFile(emptyList(), items)
        val doubt = HistoryImport.doubtful(after, saved, DistanceUnit.MILES).single()
        assertEquals(130_000, doubt.entry.odometer)
        assertEquals(OdometerReading(day.minusYears(1), 70_000), doubt.before)
        // The same visit after a believable stretch isn't.
        val fine = listOf(file(day.minusYears(1), 70_000), file(day.plusMonths(6), 85_000))
        val ok = withFile(emptyList(), fine)
        assertTrue(HistoryImport.doubtful(ok, saved, DistanceUnit.MILES).isEmpty())
        // Too fast for any vehicle, with no history to judge a pace by.
        val first = listOf(file(day.minusMonths(2), 20_000), file(day, 45_000))
        val fast = withFile(emptyList(), first)
        assertEquals(listOf(45_000), HistoryImport.doubtful(fast, emptyList(), DistanceUnit.MILES).map { it.entry.odometer })
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
