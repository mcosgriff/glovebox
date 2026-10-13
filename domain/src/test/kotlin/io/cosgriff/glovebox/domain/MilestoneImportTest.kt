package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MilestoneImportTest {
    private val oil = TableJob("Change engine oil and filter", "oil")
    private val belts = TableJob("Inspect drive belts")
    private val day = LocalDate.of(2027, 5, 1)

    @Test
    fun `milestones match on mileage`() {
        val plan = MilestoneImport.plan(
            items = listOf(ImportedMilestone(10_000, listOf(oil), 12), ImportedMilestone(30_000, listOf(oil, belts)), ImportedMilestone(60_000, listOf(belts))),
            existing = listOf(SavedMilestone(1, 10_000, listOf(oil), 12), SavedMilestone(2, 30_000, listOf(oil))),
        )
        assertEquals(listOf(60_000), plan.new.map { it.atMiles })
        assertEquals(listOf(1L), plan.same.map { it.second.id })
        assertEquals(listOf(2L), plan.differs.map { it.second.id })
    }

    @Test
    fun `the order of the lines and their capitals aren't a difference, but months and services are`() {
        fun plan(file: ImportedMilestone, saved: SavedMilestone) = MilestoneImport.plan(listOf(file), listOf(saved))
        assertEquals(1, plan(ImportedMilestone(30_000, listOf(belts, oil)), SavedMilestone(1, 30_000, listOf(oil, TableJob("INSPECT drive belts")))).same.size)
        assertEquals(1, plan(ImportedMilestone(30_000, listOf(oil), 36), SavedMilestone(1, 30_000, listOf(oil))).differs.size)
        assertEquals(1, plan(ImportedMilestone(30_000, listOf(TableJob(oil.name))), SavedMilestone(1, 30_000, listOf(oil))).differs.size)
    }

    @Test
    fun `a job is the same job wherever its service or its name appears`() {
        assertEquals(MilestoneImport.jobKey(oil), MilestoneImport.jobKey(TableJob("Replace oil", "oil")))
        assertEquals(MilestoneImport.jobKey(belts), MilestoneImport.jobKey(TableJob("inspect drive BELTS")))
        assertTrue(MilestoneImport.jobKey(oil) != MilestoneImport.jobKey(TableJob(oil.name)))
    }

    @Test
    fun `logged work completes a table's jobs by service or by name`() {
        val table = MilestoneImport.milestones(
            listOf(SavedMilestone(1, 10_000, listOf(oil)), SavedMilestone(2, 20_000, listOf(oil)), SavedMilestone(3, 30_000, listOf(oil, belts), 36)),
        )
        // An oil change logged from the catalog, and a line the user typed themselves.
        val done = MilestoneImport.doneKeys("Oil & filter change", "oil").map { JobDone(it, day, 18_500, 18_500, 7) } +
            MilestoneImport.doneKeys("inspect drive belts", null).map { JobDone(it, day, 29_000, 29_000, 8) }
        val status = Milestones.status(table, done, current = 29_500, continues = false)

        assertNull(status[0].jobs.single().done)
        assertEquals(7L, status[1].jobs.single().done?.recordId)
        assertNull(status[2].jobs[0].done)
        assertEquals(8L, status[2].jobs[1].done?.recordId)
        assertEquals(36, status[2].milestone.orMonths)
        assertEquals(MilestonePlace.NEXT, status[2].place)
    }

    @Test
    fun `a line logged under a different name with no service matches nothing`() {
        val table = MilestoneImport.milestones(listOf(SavedMilestone(1, 30_000, listOf(belts))))
        val done = MilestoneImport.doneKeys("Checked belts", null).map { JobDone(it, day, 29_000, 29_000, 8) }
        assertNull(Milestones.status(table, done, 31_000, continues = false).single().jobs.single().done)
    }

    private val cycle = listOf(SavedMilestone(1, 10_000, listOf(oil), 12), SavedMilestone(2, 20_000, listOf(oil)), SavedMilestone(3, 30_000, listOf(oil, belts), 36))

    @Test
    fun `a table that repeats comes round again with the same work`() {
        val list = MilestoneImport.milestones(cycle, every = 30_000, current = 5_000)
        // The table as written, then as far as the sixth point past the odometer.
        assertEquals(listOf(10_000, 20_000, 30_000, 40_000, 50_000, 60_000), list.map { it.atMiles })
        assertEquals(listOf(null, null, null, 10_000, 20_000, 30_000), list.map { it.repeatOf })
        assertEquals(list[2].jobs, list[5].jobs)
        // Months are the vehicle's age the first time round; nothing is made up for the next.
        assertEquals(listOf(12, null, 36, null, null, null), list.map { it.orMonths })
    }

    @Test
    fun `a cycle longer than the table leaves a gap before it starts again`() {
        val list = MilestoneImport.milestones(cycle, every = 100_000, current = 25_000)
        assertEquals(listOf(10_000, 20_000, 30_000, 110_000, 120_000, 130_000, 210_000, 220_000), list.map { it.atMiles })
    }

    @Test
    fun `a repeating table runs a set number past the odometer, and further on request`() {
        val far = MilestoneImport.milestones(cycle, every = 30_000, current = 95_000)
        assertEquals(10_000, far.first().atMiles)
        assertEquals(listOf(100_000, 110_000, 120_000, 130_000, 140_000, 150_000), far.filter { it.atMiles > 95_000 }.map { it.atMiles })
        assertEquals(210_000, MilestoneImport.milestones(cycle, 30_000, 95_000, more = Milestones.MORE).last().atMiles)
        // The whole table is shown even when that is more than the usual number ahead, and "more" still adds to it.
        val long = (1..12).map { SavedMilestone(it.toLong(), it * 10_000, listOf(oil)) }
        assertEquals(120_000, MilestoneImport.milestones(long, 120_000, current = null).last().atMiles)
        assertEquals(180_000, MilestoneImport.milestones(long, 120_000, null, more = 6).last().atMiles)
    }

    @Test
    fun `a very short cycle can't produce an endless list`() {
        val list = MilestoneImport.milestones(listOf(SavedMilestone(1, 1, listOf(oil))), every = 1, current = 900_000)
        assertEquals(Milestones.MAX_POINTS, list.size)
        assertEquals(900_006, list.last().atMiles)
    }

    @Test
    fun `work logged in a later cycle completes that cycle's milestone`() {
        val list = MilestoneImport.milestones(cycle, every = 30_000, current = 49_000)
        fun done(label: String, service: String?, miles: Int, record: Long) = MilestoneImport.doneKeys(label, service).map { JobDone(it, day, miles, miles, record) }
        val log = done("Oil & filter change", "oil", 9_500, 1) + done("Oil & filter change", "oil", 38_000, 2) + done("Inspect drive belts", null, 61_000, 3)
        val status = Milestones.status(list, log, current = 49_000, continues = true)
        fun at(miles: Int) = status.first { it.milestone.atMiles == miles }

        assertEquals(1L, at(10_000).jobs.single().done?.recordId)
        assertNull(at(30_000).jobs[0].done)
        assertEquals(2L, at(40_000).jobs.single().done?.recordId)
        assertEquals(MilestonePlace.NEXT, at(50_000).place)
        // Belts at 61,000 belong to 90,000, the next round of the 30,000 milestone, and to nothing before it.
        assertNull(at(30_000).jobs[1].done)
        assertNull(at(60_000).jobs[1].done)
        assertEquals(3L, at(90_000).jobs[1].done?.recordId)
    }

    @Test
    fun `work past the end of what is shown waits for its own milestone`() {
        val list = MilestoneImport.milestones(cycle, every = 30_000, current = 5_000)
        val log = MilestoneImport.doneKeys("Oil & filter change", "oil").map { JobDone(it, day, 61_000, 61_000, 9) }
        assertTrue(Milestones.status(list, log, 5_000, continues = true).all { m -> m.jobs.all { it.done == null } })
    }

    @Test
    fun `a file that repeats differently from the saved table is refused, and a first import never is`() {
        assertNull(MilestoneImport.repeatConflict(120_000, emptyList(), null, DistanceUnit.MILES))
        assertNull(MilestoneImport.repeatConflict(null, emptyList(), 120_000, DistanceUnit.MILES))
        assertNull(MilestoneImport.repeatConflict(30_000, cycle, 30_000, DistanceUnit.MILES))
        assertNull(MilestoneImport.repeatConflict(null, cycle, null, DistanceUnit.MILES))
        assertEquals(
            "The milestone table on this vehicle doesn't repeat, and this file's repeats every 120,000 mi. " +
                "To change that, remove the table on the Milestones screen and import this file again; to add to the table as it is, make the file say the same.",
            MilestoneImport.repeatConflict(120_000, cycle, null, DistanceUnit.MILES),
        )
        assertTrue(MilestoneImport.repeatConflict(null, cycle, 30_000, DistanceUnit.MILES)!!.startsWith("The milestone table on this vehicle repeats every 30,000 mi, and this file's doesn't repeat."))
        assertTrue(MilestoneImport.repeatConflict(60_000, cycle, 30_000, DistanceUnit.MILES) != null)
    }
}
