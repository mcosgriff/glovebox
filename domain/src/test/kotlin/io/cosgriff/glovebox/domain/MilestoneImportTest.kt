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
}
