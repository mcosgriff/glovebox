package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneContinuationTest {
    private val oil = TableJob("Change oil and filter", "oil")
    private val brakes = TableJob("Inspect brakes")
    private val plugs = TableJob("Replace spark plugs", "spark-plugs")
    private val belt = TableJob("Replace timing belt")

    /** A made-up 10,000 grid to 120,000: oil and brakes at every column, plugs at every 40,000, the belt once at 60,000. */
    private val grid = (1..12).map { i ->
        val at = i * 10_000
        SavedMilestone(i.toLong(), at, listOfNotNull(oil, brakes, plugs.takeIf { at % 40_000 == 0 }, belt.takeIf { at == 60_000 }))
    }

    private fun keys(m: Milestone) = m.jobs.map { it.name }.toSet()

    @Test
    fun `a cycle is taken only where the table shows it at least twice and evenly, to its end`() {
        assertEquals(10_000, MilestoneContinuation.cycle((1..12).map { it * 10_000 }, 120_000))
        assertEquals(40_000, MilestoneContinuation.cycle(listOf(40_000, 80_000, 120_000), 120_000))
        assertEquals(50_000, MilestoneContinuation.cycle(listOf(100_000, 150_000), 150_000)) // first at 100,000, shown twice
        assertNull(MilestoneContinuation.cycle(listOf(60_000), 120_000)) // once
        assertNull(MilestoneContinuation.cycle(listOf(100_000), 150_000)) // "first at 100,000, then every 50,000" shown only once
        assertNull(MilestoneContinuation.cycle(listOf(30_000, 60_000, 100_000), 120_000)) // uneven
        assertNull(MilestoneContinuation.cycle(listOf(20_000, 40_000, 60_000), 120_000)) // stops before the end
    }

    @Test
    fun `a 10,000 grid table is continued to the car's mileage plus the table's length`() {
        val out = MilestoneContinuation.continued(grid, emptyList(), current = 120_000)
        val more = out.milestones.filter { it.continued }
        assertEquals((13..24).map { it * 10_000 }, more.map { it.atMiles })
        assertEquals(120_000, out.endsAt)
        assertTrue(out.continues)
        // The table as written comes first, unmarked.
        assertEquals(grid.map { it.atMiles }, out.milestones.filterNot { it.continued }.map { it.atMiles })
        assertEquals(setOf(oil.name, brakes.name), keys(more.first { it.atMiles == 130_000 }))
        assertEquals(setOf(oil.name, brakes.name, plugs.name), keys(more.first { it.atMiles == 160_000 }))
        assertEquals(setOf(oil.name, brakes.name, plugs.name), keys(more.first { it.atMiles == 240_000 }))
        // A continued milestone has no months: the guide doesn't say how old the car will be then.
        assertTrue(more.all { it.orMonths == null && it.repeatOf == null })
    }

    @Test
    fun `a job continues under the same key, so the log completes it`() {
        val out = MilestoneContinuation.continued(grid, emptyList(), current = 120_000)
        val at = out.milestones.first { it.atMiles == 130_000 }
        assertTrue(at.jobs.any { it.key == MilestoneImport.jobKey(oil) })
        val done = listOf(JobDone(MilestoneImport.jobKey(oil), java.time.LocalDate.of(2030, 1, 1), 128_500, 128_500, 9))
        val status = Milestones.status(out.milestones, done, 129_000, continues = false)
        assertEquals(9L, status.first { it.milestone.atMiles == 130_000 }.jobs.first { it.job.name == oil.name }.done?.recordId)
    }

    @Test
    fun `an irregular row stops, and is named as not continued`() {
        val out = MilestoneContinuation.continued(grid, emptyList(), current = 120_000)
        assertEquals(listOf(belt.name), out.notContinued.map { it.name })
        assertTrue(out.milestones.filter { it.continued }.none { m -> m.jobs.any { it.name == belt.name } })
    }

    @Test
    fun `a job given a first time past the end appears from its first time on`() {
        val pump = ImportedScheduleItem("Replace water pump", Interval(20_000, null), firstAt = 150_000)
        val early = ImportedScheduleItem("Change oil and filter", Interval(10_000, 12), service = "oil") // first time is the table's own
        val out = MilestoneContinuation.continued(grid, listOf(pump, early), current = 120_000)
        val withPump = out.milestones.filter { m -> m.jobs.any { it.name == pump.name } }.map { it.atMiles }
        assertEquals(listOf(150_000, 170_000, 190_000, 210_000, 230_000), withPump)
        assertTrue(out.milestones.first { it.atMiles == 150_000 }.continued)
        // Oil isn't doubled by its reminder.
        assertEquals(1, out.milestones.first { it.atMiles == 150_000 }.jobs.count { it.name == oil.name })
    }

    @Test
    fun `the continuation reaches further as the car does`() {
        val young = MilestoneContinuation.continued(grid, emptyList(), current = 30_000)
        assertEquals(150_000, young.milestones.last().atMiles)
        val none = MilestoneContinuation.continued(grid, emptyList(), current = null)
        assertFalse(none.continues) // with no reading the table itself is as far as the car needs
    }

    @Test
    fun `a table with a stated repeat uses it, not a continuation`() {
        val out = MilestoneContinuation.laidOut(grid, repeatsEvery = 120_000, fromMaker = true, later = emptyList(), current = 120_000)
        assertFalse(out.continues)
        val after = out.milestones.filter { it.atMiles > 120_000 }
        assertTrue(after.isNotEmpty())
        assertTrue(after.all { it.repeatOf != null })
        // Its own repeat brings the belt back round, as the guide says.
        assertTrue(out.milestones.any { m -> m.atMiles == 180_000 && m.jobs.any { it.name == belt.name } })
        assertTrue(out.notContinued.isEmpty())
    }

    @Test
    fun `only a maker's table is continued, and the user's own ends where it ends`() {
        val own = MilestoneContinuation.laidOut(grid, repeatsEvery = null, fromMaker = false, later = emptyList(), current = 120_000)
        assertFalse(own.continues)
        assertEquals(120_000, own.milestones.last().atMiles)
        val makers = MilestoneContinuation.laidOut(grid, repeatsEvery = null, fromMaker = true, later = emptyList(), current = 120_000)
        assertTrue(makers.continues)
    }

    @Test
    fun `an empty table has nothing to continue`() {
        val out = MilestoneContinuation.continued(emptyList(), emptyList(), current = 50_000)
        assertTrue(out.milestones.isEmpty())
        assertFalse(out.continues)
    }
}
