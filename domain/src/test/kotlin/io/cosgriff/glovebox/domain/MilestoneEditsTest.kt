package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneEditsTest {
    private val oil = TableJob("Change engine oil and filter", "oil")
    private val belts = TableJob("Inspect drive belts")

    private fun check(miles: String = "30,000", months: String = "", jobs: List<TableJob> = listOf(oil), others: List<Int> = emptyList(), repeats: Int? = null) =
        MilestoneEdits.check(miles, months, jobs, others, repeats, DistanceUnit.MILES) { if (it == "oil") "Oil & filter change" else it }

    private fun problems(edit: MilestoneEdit) = (edit as MilestoneEdit.Refused).problems

    @Test
    fun `a milestone is a mileage, perhaps months, and its jobs in the order typed`() {
        assertEquals(
            MilestoneEdit.Ok(ImportedMilestone(30_000, listOf(belts, oil), 36)),
            check(months = " 36 ", jobs = listOf(TableJob("  Inspect drive belts "), oil)),
        )
        assertEquals(MilestoneEdit.Ok(ImportedMilestone(30_000, listOf(oil))), check(miles = "30000"))
    }

    @Test
    fun `the mileage and months have the file's limits`() {
        assertEquals("Enter the mileage as a whole number from 1 to 1,000,000", problems(check(miles = "")).miles)
        assertTrue(problems(check(miles = "0")).miles != null)
        assertTrue(problems(check(miles = "1,000,001")).miles != null)
        assertTrue(problems(check(miles = "30000.5")).miles != null)
        assertTrue(check(miles = "1,000,000") is MilestoneEdit.Ok)
        assertEquals("Enter whole months from 1 to 600, or leave it empty", problems(check(months = "0")).months)
        assertTrue(problems(check(months = "601")).months != null)
        assertTrue(problems(check(months = "a year")).months != null)
        assertTrue(check(months = "600") is MilestoneEdit.Ok)
    }

    @Test
    fun `one mileage is one milestone, and none sits past the end of a cycle`() {
        assertEquals("There is already a milestone at 30,000 mi. Add the work to that one", problems(check(others = listOf(10_000, 30_000))).miles)
        assertTrue(check(others = listOf(10_000, 20_000)) is MilestoneEdit.Ok)
        assertEquals(
            "This table repeats every 20,000 mi, so a milestone can't be past that. Change how it repeats first",
            problems(check(repeats = 20_000)).miles,
        )
        assertTrue(check(repeats = 30_000) is MilestoneEdit.Ok)
    }

    @Test
    fun `a milestone needs a job, and each job a name`() {
        assertEquals("Add at least one job", problems(check(jobs = emptyList())).work)
        assertEquals(mapOf(1 to "Give it a name, or remove it"), problems(check(jobs = listOf(oil, TableJob("  ")))).jobs)
        assertEquals(mapOf(0 to "Keep the name to 80 characters"), problems(check(jobs = listOf(TableJob("x".repeat(81))))).jobs)
        assertTrue(check(jobs = listOf(TableJob("x".repeat(80)))) is MilestoneEdit.Ok)
        assertEquals(mapOf(0 to "The name has to be on one line"), problems(check(jobs = listOf(TableJob("Inspect\nbelts")))).jobs)
        assertEquals("A milestone can have 40 jobs at most", problems(check(jobs = (1..41).map { TableJob("Job $it") })).work)
        assertTrue(check(jobs = (1..30).map { TableJob("Job $it") }) is MilestoneEdit.Ok)
    }

    @Test
    fun `a name and a service appear once each in a milestone`() {
        assertEquals(mapOf(1 to "This job is already in the milestone"), problems(check(jobs = listOf(belts, TableJob(" inspect drive BELTS")))).jobs)
        assertEquals(
            mapOf(1 to "\"Oil & filter change\" is already picked for another job here"),
            problems(check(jobs = listOf(oil, TableJob("Oil service", "oil")))).jobs,
        )
        // Two jobs with no service aren't the same job for that.
        assertTrue(check(jobs = listOf(belts, TableJob("Inspect hoses"))) is MilestoneEdit.Ok)
    }

    @Test
    fun `every problem is found at once`() {
        val p = problems(check(miles = "", months = "0", jobs = listOf(TableJob(""), oil, TableJob("Oil", "oil"))))
        assertTrue(p.miles != null && p.months != null)
        assertEquals(setOf(0, 2), p.jobs.keys)
        assertNull(p.work)
    }

    @Test
    fun `what the editor accepts, the file reader accepts, and the other way round`() {
        fun file(at: String, months: String, name: String) = ImportFormat.parse(
            """{ "glovebox": "import", "version": 1, "kind": "milestone-schedule", "items": [ { "atMiles": $at, "orMonths": $months, "work": [ { "name": "$name" } ] } ] }""",
        )
        for ((at, months, name) in listOf(
            Triple("1", "1", "A"),
            Triple("1000000", "600", "x".repeat(80)),
            Triple("0", "12", "A"),
            Triple("1000001", "12", "A"),
            Triple("30000", "0", "A"),
            Triple("30000", "601", "A"),
            Triple("30000", "12", "x".repeat(81)),
        )) {
            assertEquals("$at / $months / ${name.length}", file(at, months, name) is ImportFile.MilestoneSchedule, check(at, months, listOf(TableJob(name))) is MilestoneEdit.Ok)
        }
    }

    @Test
    fun `a table repeats over a cycle no shorter than itself, or ends`() {
        assertEquals(RepeatEdit.Ok(null), MilestoneEdits.repeat("  ", 100_000, DistanceUnit.MILES))
        assertEquals(RepeatEdit.Ok(120_000), MilestoneEdits.repeat("120,000", 100_000, DistanceUnit.MILES))
        assertEquals(RepeatEdit.Ok(100_000), MilestoneEdits.repeat("100000", 100_000, DistanceUnit.MILES))
        assertEquals(
            RepeatEdit.Refused("It is the length of the whole cycle, so it can't be less than your last milestone (100,000 mi)"),
            MilestoneEdits.repeat("99,999", 100_000, DistanceUnit.MILES),
        )
        assertTrue(MilestoneEdits.repeat("1,000,001", 100_000, DistanceUnit.MILES) is RepeatEdit.Refused)
        assertTrue(MilestoneEdits.repeat("often", 100_000, DistanceUnit.MILES) is RepeatEdit.Refused)
    }

    @Test
    fun `a table and a milestone are full at the file's limits`() {
        assertTrue(MilestoneEdits.canAdd(199))
        assertFalse(MilestoneEdits.canAdd(200))
        assertTrue(MilestoneEdits.canAddJob(39))
        assertFalse(MilestoneEdits.canAddJob(40))
    }

    @Test
    fun `repeats are found by index, each against the one before it`() {
        assertEquals(mapOf(2 to 0, 4 to 2), MilestoneImport.repeated(listOf("a", "b", "a", null, "a", null)))
        assertEquals(emptyMap<Int, Int>(), MilestoneImport.repeated(listOf<String?>(null, null)))
    }
}
