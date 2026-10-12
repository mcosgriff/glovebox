package io.cosgriff.glovebox.domain

import java.time.LocalDate

/** One piece of work at a milestone. [key] is the same wherever the same job appears, which is how logged work is matched to it. */
data class MilestoneJob(val key: String, val name: String)

/**
 * A mileage point with the work listed there, the way an owner's manual prints a schedule.
 * [atMiles] is miles the vehicle has really covered, not what a replaced cluster reads.
 * [repeatOf] is set on a later round of a table that repeats: the mileage it is written at in the table.
 */
data class Milestone(val atMiles: Int, val jobs: List<MilestoneJob>, val orMonths: Int? = null, val repeatOf: Int? = null)

/** One time a job was logged. [actualMiles] places it among the milestones; [readMiles] is what the dash said that day. */
data class JobDone(val key: String, val date: LocalDate, val actualMiles: Int, val readMiles: Int, val recordId: Long)

enum class MilestonePlace { PAST, NEXT, AHEAD }

data class MilestoneJobStatus(val job: MilestoneJob, val done: JobDone?)

data class MilestoneStatus(val milestone: Milestone, val place: MilestonePlace, val jobs: List<MilestoneJobStatus>)

/**
 * Milestones are a way of looking at the schedule, never a second source of what is due: reminders
 * come from the interval schedule and the service log, and nothing here creates, moves or silences
 * one. What was done at a milestone is derived from the log every time, never stored.
 */
object Milestones {
    /** How many milestones past the current odometer are shown to begin with, the next one included. */
    const val AHEAD = 6

    /** How many more each "show more" adds. */
    const val MORE = 6

    /** The most that are ever built, however short the intervals or long the history. */
    const val MAX_POINTS = 300

    /**
     * Milestones built from the schedule: every multiple of an item's mileage interval is a point,
     * and items that fall on the same point share it. Items with no mileage interval have no points.
     *
     * The list runs from the first point at or after [from] to the [ahead]th point past [current],
     * so it starts where the vehicle's records start instead of at a string of empty cards.
     */
    fun generate(items: List<TrackedItem>, from: Int, current: Int, ahead: Int = AHEAD): List<Milestone> {
        val steps = items.mapNotNull { item -> item.interval.miles?.let { item to it } }
        if (steps.isEmpty()) return emptyList()
        // The [ahead]th distinct point past the odometer is as far as the list goes.
        val horizon = steps.flatMap { (_, step) -> (1..ahead).map { (current / step + it).toLong() * step } }
            .distinct().sorted().take(ahead).last()
        val byPoint = sortedMapOf<Int, MutableList<MilestoneJob>>()
        for ((item, step) in steps.sortedBy { it.first.name }) {
            // Newest first, so a very short interval can't crowd out the points that matter most.
            var at = horizon / step * step
            var made = 0
            while (at >= maxOf(from, 1) && made < MAX_POINTS) {
                byPoint.getOrPut(at.toInt()) { mutableListOf() } += MilestoneJob(item.id.toString(), item.name)
                at -= step
                made++
            }
        }
        return byPoint.entries.toList().takeLast(MAX_POINTS).map { Milestone(it.key, it.value) }
    }

    /**
     * What the log says against each milestone.
     *
     * A job at a milestone is completed by the latest time it was logged after the previous
     * milestone that lists it, up to and including this one. So an oil change at 48,000 belongs to
     * 50,000, and of two in that stretch the later is shown. Every service lands on exactly one
     * milestone for its job.
     *
     * [continues] says the series goes on past the end of [milestones], as a generated one and a
     * table that repeats do: work logged beyond a job's last point shown then belongs to a point
     * further on, and is left out. For a table that simply ends, it belongs to the job's last milestone.
     *
     * [current] is the vehicle's actual mileage; null for a vehicle with no reading yet.
     */
    fun status(milestones: List<Milestone>, done: List<JobDone>, current: Int?, continues: Boolean): List<MilestoneStatus> {
        val sorted = milestones.sortedBy { it.atMiles }
        val pointsFor = sorted.flatMap { m -> m.jobs.map { it.key to m.atMiles } }.groupBy({ it.first }, { it.second })
        val completed = mutableMapOf<Pair<String, Int>, JobDone>()
        for (d in done) {
            val points = pointsFor[d.key] ?: continue
            val at = points.firstOrNull { it >= d.actualMiles } ?: if (continues) continue else points.last()
            val slot = d.key to at
            val have = completed[slot]
            if (have == null || latest.compare(d, have) > 0) completed[slot] = d
        }
        val next = sorted.firstOrNull { current == null || it.atMiles > current }
        return sorted.map { m ->
            MilestoneStatus(
                milestone = m,
                place = when {
                    m === next -> MilestonePlace.NEXT
                    next != null && m.atMiles > next.atMiles -> MilestonePlace.AHEAD
                    else -> MilestonePlace.PAST
                },
                jobs = m.jobs.map { MilestoneJobStatus(it, completed[it.key to m.atMiles]) },
            )
        }
    }

    /**
     * The list as it is shown: a run of [minRun] or more past milestones with nothing logged against
     * any of them is folded into one row, so a vehicle with a long gap in its records isn't a screen
     * of empty cards. Each inner list is one row; a single-element list is an ordinary milestone.
     */
    fun folded(statuses: List<MilestoneStatus>, minRun: Int = 3): List<List<MilestoneStatus>> {
        val rows = mutableListOf<List<MilestoneStatus>>()
        var run = mutableListOf<MilestoneStatus>()
        fun flush() {
            if (run.size >= minRun) rows += run else run.forEach { rows += listOf(it) }
            run = mutableListOf()
        }
        for (s in statuses) {
            if (s.place == MilestonePlace.PAST && s.jobs.none { it.done != null }) {
                run += s
            } else {
                flush()
                rows += listOf(s)
            }
        }
        flush()
        return rows
    }

    private val latest = compareBy<JobDone> { it.date }.thenBy { it.actualMiles }

    /**
     * Where a generated list should start: at the vehicle's earliest logged job, so the milestones
     * before the user kept records aren't drawn as a run of empty cards. With nothing logged it
     * starts just past the odometer, at the next one.
     */
    fun startFor(done: List<JobDone>, current: Int?): Int = done.minOfOrNull { it.actualMiles } ?: ((current ?: 0) + 1)
}
