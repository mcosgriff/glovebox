package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MilestonesTest {
    private val oil = TrackedItem(1, "Oil & filter", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
    private val rotation = TrackedItem(2, "Tire rotation", Interval(5_000, null), DueRule.MILEAGE_ONLY)
    private val airFilter = TrackedItem(3, "Engine air filter", Interval(30_000, null), DueRule.MILEAGE_ONLY)
    private val brakeFluid = TrackedItem(4, "Brake fluid", Interval(null, 36), DueRule.TIME_ONLY)

    private val day = LocalDate.of(2028, 1, 1)
    private fun done(key: Long, miles: Int, date: LocalDate = day, record: Long = miles.toLong()) = JobDone(key.toString(), date, miles, miles, record)
    private fun job(status: List<MilestoneStatus>, at: Int, key: Long) = status.first { it.milestone.atMiles == at }.jobs.first { it.job.key == key.toString() }

    @Test
    fun `items on the same mileage share a milestone`() {
        val list = Milestones.generate(listOf(oil, rotation, airFilter, brakeFluid), from = 20_001, current = 24_000, ahead = 2)
        assertEquals(listOf(25_000, 30_000), list.map { it.atMiles })
        assertEquals(listOf("Oil & filter", "Tire rotation"), list[0].jobs.map { it.name })
        assertEquals(listOf("Engine air filter", "Oil & filter", "Tire rotation"), list[1].jobs.map { it.name })
    }

    @Test
    fun `an item with no mileage interval has no points`() {
        assertTrue(Milestones.generate(listOf(brakeFluid), 1, 10_000).isEmpty())
        assertTrue(Milestones.generate(emptyList(), 1, 10_000).isEmpty())
    }

    @Test
    fun `intervals that don't line up give every point of either`() {
        val every7500 = TrackedItem(5, "Oil", Interval(7_500, null), DueRule.MILEAGE_ONLY)
        val list = Milestones.generate(listOf(every7500, rotation), from = 1, current = 0, ahead = 4)
        assertEquals(listOf(5_000, 7_500, 10_000, 15_000), list.map { it.atMiles })
        assertEquals(listOf("Oil", "Tire rotation"), list.last().jobs.map { it.name })
    }

    @Test
    fun `the list starts where asked and runs a set number past the odometer`() {
        val list = Milestones.generate(listOf(oil), from = 38_500, current = 49_000, ahead = 3)
        assertEquals(listOf(40_000, 45_000, 50_000, 55_000, 60_000), list.map { it.atMiles })
    }

    @Test
    fun `a very short interval can't produce an endless list`() {
        val everyMile = TrackedItem(9, "Check tires", Interval(1, null), DueRule.MILEAGE_ONLY)
        val list = Milestones.generate(listOf(everyMile), from = 1, current = 250_000)
        assertEquals(Milestones.MAX_POINTS, list.size)
        assertEquals(250_000 + Milestones.AHEAD, list.last().atMiles)
    }

    @Test
    fun `work done early belongs to the milestone it was heading for`() {
        val list = Milestones.generate(listOf(oil), from = 40_000, current = 49_000)
        val status = Milestones.status(list, listOf(done(1, 48_000)), current = 49_000, continues = true)

        assertEquals(done(1, 48_000), job(status, 50_000, 1).done)
        assertNull(job(status, 45_000, 1).done)
        // The 50,000 milestone is still ahead of the odometer, and already done.
        assertEquals(MilestonePlace.NEXT, status.first { it.milestone.atMiles == 50_000 }.place)
    }

    @Test
    fun `of two in the same stretch the later is shown`() {
        val list = Milestones.generate(listOf(oil), from = 40_000, current = 49_000)
        val status = Milestones.status(list, listOf(done(1, 48_000, day), done(1, 46_500, day.minusMonths(2))), 49_000, continues = true)
        assertEquals(48_000, job(status, 50_000, 1).done?.actualMiles)
    }

    @Test
    fun `work done exactly on a milestone belongs to it, and one mile later to the next`() {
        val list = Milestones.generate(listOf(oil), from = 40_000, current = 52_000)
        val status = Milestones.status(list, listOf(done(1, 45_000), done(1, 45_001, day.plusDays(1))), 52_000, continues = true)
        assertEquals(45_000, job(status, 45_000, 1).done?.actualMiles)
        assertEquals(45_001, job(status, 50_000, 1).done?.actualMiles)
    }

    @Test
    fun `work done late leaves the milestone it missed empty`() {
        val list = Milestones.generate(listOf(oil), from = 40_000, current = 57_000)
        val status = Milestones.status(list, listOf(done(1, 56_000)), 57_000, continues = true)
        assertNull(job(status, 55_000, 1).done)
        assertEquals(56_000, job(status, 60_000, 1).done?.actualMiles)
    }

    @Test
    fun `each job is matched on its own`() {
        val list = Milestones.generate(listOf(oil, rotation), from = 40_000, current = 49_000)
        val status = Milestones.status(list, listOf(done(1, 48_000)), 49_000, continues = true)
        assertEquals(48_000, job(status, 50_000, 1).done?.actualMiles)
        assertNull(job(status, 50_000, 2).done)
    }

    @Test
    fun `milestones are past, next or ahead of the odometer`() {
        val list = Milestones.generate(listOf(oil), from = 40_000, current = 50_000, ahead = 2)
        val status = Milestones.status(list, emptyList(), 50_000, continues = true)
        assertEquals(
            listOf(40_000 to MilestonePlace.PAST, 45_000 to MilestonePlace.PAST, 50_000 to MilestonePlace.PAST, 55_000 to MilestonePlace.NEXT, 60_000 to MilestonePlace.AHEAD),
            status.map { it.milestone.atMiles to it.place },
        )
        // No reading yet: the first one is next.
        assertEquals(MilestonePlace.NEXT, Milestones.status(list, emptyList(), null, continues = true).first().place)
    }

    @Test
    fun `in a generated list, work for a point further on than the list is left for that point`() {
        // The air filter's next point is 60,000, beyond a list that stops at 55,000.
        val list = Milestones.generate(listOf(oil, airFilter), from = 28_000, current = 49_000, ahead = 2)
        assertEquals(55_000, list.last().atMiles)
        val status = Milestones.status(list, listOf(done(3, 41_000)), 49_000, continues = true)
        assertNull(job(status, 30_000, 3).done)
    }

    @Test
    fun `in a table that ends, a job's last milestone takes everything after the one before it`() {
        val plugs = MilestoneJob("spark-plugs", "Replace spark plugs")
        val belts = MilestoneJob("belts", "Inspect drive belts")
        val table = listOf(Milestone(30_000, listOf(belts)), Milestone(60_000, listOf(belts)), Milestone(65_000, listOf(plugs)))
        val log = listOf(
            JobDone("spark-plugs", day, 71_000, 71_000, 1), // the only milestone for it: any time counts
            JobDone("belts", day, 88_000, 88_000, 2), // past its last milestone
        )
        val status = Milestones.status(table, log, current = 90_000, continues = false)
        assertEquals(71_000, status[2].jobs.single().done?.actualMiles)
        assertEquals(88_000, status[1].jobs.single().done?.actualMiles)
        assertNull(status[0].jobs.single().done)
    }

    @Test
    fun `a job nothing in the log matches is listed with no status`() {
        val table = listOf(Milestone(60_000, listOf(MilestoneJob("belts", "Inspect drive belts"))))
        val status = Milestones.status(table, listOf(done(1, 58_000)), 61_000, continues = false)
        assertEquals("Inspect drive belts", status.single().jobs.single().job.name)
        assertNull(status.single().jobs.single().done)
    }

    @Test
    fun `a generated list starts at the earliest logged job, or at the next milestone with nothing logged`() {
        assertEquals(31_800, Milestones.startFor(listOf(done(1, 48_000), done(2, 31_800)), 49_000))
        assertEquals(49_001, Milestones.startFor(emptyList(), 49_000))
        assertEquals(1, Milestones.startFor(emptyList(), null))
    }
}
