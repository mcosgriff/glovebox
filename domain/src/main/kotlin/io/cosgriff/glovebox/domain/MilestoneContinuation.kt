package io.cosgriff.glovebox.domain

/**
 * A maker's table carried on past its last column, built from its own rows (#568). [milestones] is the
 * table as written followed by the continued milestones, each [Milestone.continued]; [endsAt] is the table's
 * last column; [notContinued] are the table's jobs whose marks follow no single cycle, so the maker's table
 * ends there for them.
 */
data class ContinuedTable(val milestones: List<Milestone>, val endsAt: Int, val notContinued: List<MilestoneJob>) {
    /** True when the table was carried on at all. */
    val continues: Boolean get() = milestones.any { it.continued }
}

/**
 * The milestones view of a maker's table past its last column. Like the rest of the view it is worked out
 * each time and stored nowhere, and it creates, moves or silences no reminder.
 *
 * Nothing is invented: a job's cycle is taken only where the table itself marks the job at least twice,
 * at an even spacing, and on to the end of the table (its next mark would be past the last column). A job
 * the guide gives a first time past the end ([ImportedScheduleItem.firstAt]) comes in from that first time.
 * A table that repeats is laid out by its own repeat instead ([MilestoneImport.milestones]); this is for one
 * that doesn't.
 */
object MilestoneContinuation {
    /**
     * The spacing [marks] (where the table lists a job, any order) keep up to [end], the table's last column;
     * null when they keep none: fewer than two marks, uneven gaps, or a row that stops before the end.
     */
    fun cycle(marks: List<Int>, end: Int): Int? {
        val at = marks.distinct().sorted()
        if (at.size < 2) return null
        val step = at[1] - at[0]
        if (step <= 0 || at.zipWithNext().any { (a, b) -> b - a != step }) return null
        return step.takeIf { at.last().toLong() + step > end }
    }

    /**
     * How far the continuation reaches: the vehicle's mileage plus the table's length, so there is always a
     * whole table's worth ahead of the car. With no reading yet that is the table itself.
     */
    fun horizon(end: Int, current: Int?): Long = maxOf(0, current ?: 0).toLong() + end

    /**
     * The user's table as the Milestones screen lays it out. A stated repeat ([repeatsEvery]) always wins: the
     * table comes round by its own cycle. Otherwise a table from a maker's list ([fromMaker]) is [continued], and
     * any other table ends where it ends. [more] is how many points "show further ahead" has added.
     */
    fun laidOut(
        table: List<SavedMilestone>, repeatsEvery: Int?, fromMaker: Boolean, later: List<ImportedScheduleItem>, current: Int?, more: Int = 0,
    ): ContinuedTable {
        val end = table.maxOfOrNull { it.atMiles } ?: 0
        return when {
            repeatsEvery != null -> ContinuedTable(MilestoneImport.milestones(table, repeatsEvery, current, more), end, emptyList())
            fromMaker -> continued(table, later, current)
            else -> ContinuedTable(MilestoneImport.milestones(table), end, emptyList())
        }
    }

    /**
     * [table] carried on to [horizon]. [later] are the guide's reminders, in the vehicle's unit; only those
     * whose first time is past the table's end, with a distance interval, are used.
     */
    fun continued(table: List<SavedMilestone>, later: List<ImportedScheduleItem>, current: Int?): ContinuedTable {
        val base = MilestoneImport.milestones(table)
        val end = base.lastOrNull()?.atMiles ?: return ContinuedTable(emptyList(), 0, emptyList())
        val limit = horizon(end, current)
        // Each job once, by key, in the order the table first lists it; the name it has at its last mark.
        val marks = linkedMapOf<String, MutableList<Int>>()
        val names = mutableMapOf<String, MilestoneJob>()
        for (m in base) for (j in m.jobs) {
            marks.getOrPut(j.key) { mutableListOf() } += m.atMiles
            names[j.key] = j
        }
        val byPoint = sortedMapOf<Long, MutableList<MilestoneJob>>()
        fun place(job: MilestoneJob, first: Long, step: Int) {
            var at = first
            var made = 0
            while (at <= limit && at <= Int.MAX_VALUE && made < Milestones.MAX_POINTS) {
                val jobs = byPoint.getOrPut(at) { mutableListOf() }
                if (jobs.none { it.key == job.key }) jobs += job
                at += step
                made++
            }
        }
        val stopped = mutableListOf<MilestoneJob>()
        for ((key, at) in marks) {
            val step = cycle(at, end)
            if (step == null) stopped += names.getValue(key) else place(names.getValue(key), at.max().toLong() + step, step)
        }
        for (item in later) {
            val first = item.firstAt ?: continue
            val step = item.normal.miles ?: continue
            if (first <= end || step <= 0) continue
            place(MilestoneJob(MilestoneImport.jobKey(TableJob(item.name, item.service)), item.name), first.toLong(), step)
        }
        val more = byPoint.entries.take(Milestones.MAX_POINTS).map { (at, jobs) -> Milestone(at.toInt(), jobs, continued = true) }
        return ContinuedTable(base + more, end, stopped)
    }
}
