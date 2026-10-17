package io.cosgriff.glovebox.domain

/** What is wrong with a milestone as typed, for under each field; `app` words it. [jobs] is by the job's place in the list. */
data class MilestoneProblems(
    val miles: MilesProblem? = null,
    /** Not whole months from 1 to [MilestoneImport.MAX_MONTHS]. */
    val months: Boolean = false,
    /** About the list of jobs as a whole. */
    val work: WorkProblem? = null,
    val jobs: Map<Int, JobProblem> = emptyMap(),
)

sealed interface MilesProblem {
    /** Not a whole number from 1 to [MilestoneImport.MAX_MILES]. */
    data object NotANumber : MilesProblem

    /** The table has a milestone at [miles] already. */
    data class Taken(val miles: Int) : MilesProblem

    /** Past the end of a table that repeats [every]. */
    data class PastCycle(val every: Int) : MilesProblem
}

enum class WorkProblem {
    NONE,

    /** More than [MilestoneImport.MAX_WORK]. */
    TOO_MANY,
}

sealed interface JobProblem {
    data object NoName : JobProblem

    /** Over [MilestoneImport.MAX_NAME] characters. */
    data object TooLong : JobProblem

    data object NotOneLine : JobProblem

    /** Another job here has the same name. */
    data object Repeated : JobProblem

    /** Another job here is for the same catalog line. */
    data class ServiceTaken(val service: String) : JobProblem
}

/** Why a cycle can't be saved. */
sealed interface RepeatProblem {
    /** Not a whole number from 1 to [MilestoneImport.MAX_MILES]. */
    data object NotANumber : RepeatProblem

    /** Shorter than the table, whose last milestone is at [lastMiles]. */
    data class ShorterThanTable(val lastMiles: Int) : RepeatProblem
}

sealed interface MilestoneEdit {
    data class Ok(val milestone: ImportedMilestone) : MilestoneEdit
    data class Refused(val problems: MilestoneProblems) : MilestoneEdit
}

sealed interface RepeatEdit {
    /** [everyMiles] null: the table ends at its last milestone. */
    data class Ok(val everyMiles: Int?) : RepeatEdit
    data class Refused(val problem: RepeatProblem) : RepeatEdit
}

/**
 * The user's own milestone table as it is typed in the app. A table made here keeps exactly the
 * rules of one read from a file ([MilestoneImport]), from the same code, so the two can't drift and
 * an edited table can be imported over, or one day written back out, without surprises. A form is
 * kinder than a file in one way only: space around a name is trimmed, not refused.
 *
 * Like the table itself this is information for the Milestones screen: nothing here knows about
 * reminders.
 */
object MilestoneEdits {
    /** Whether the table has room for another milestone. */
    fun canAdd(size: Int): Boolean = size < MilestoneImport.MAX_ITEMS

    /** Whether a milestone has room for another job. */
    fun canAddJob(size: Int): Boolean = size < MilestoneImport.MAX_WORK

    /**
     * Checks one milestone. [others] are the mileages of the table's other milestones, and
     * [repeatsEveryMiles] the cycle of a table that repeats: a milestone can't sit past the end of it.
     */
    fun check(
        milesText: String,
        monthsText: String,
        jobs: List<TableJob>,
        others: List<Int>,
        repeatsEveryMiles: Int?,
    ): MilestoneEdit {
        val miles = OdometerInput.parse(milesText)?.takeIf { it <= MilestoneImport.MAX_MILES }
        val months = monthsText.trim().takeIf { it.isNotEmpty() }?.let { t -> t.toIntOrNull()?.takeIf { it in 1..MilestoneImport.MAX_MONTHS } ?: -1 }
        val names = jobs.map { it.name.trim() }
        val jobProblems = mutableMapOf<Int, JobProblem>()
        names.forEachIndexed { i, name ->
            when {
                name.isEmpty() -> jobProblems[i] = JobProblem.NoName
                name.length > MilestoneImport.MAX_NAME -> jobProblems[i] = JobProblem.TooLong
                name.any { it.isISOControl() } -> jobProblems[i] = JobProblem.NotOneLine
            }
        }
        MilestoneImport.repeated(names.map { n -> n.takeIf { it.isNotEmpty() }?.let(MilestoneImport::nameKey) }).forEach { (i, _) ->
            jobProblems.putIfAbsent(i, JobProblem.Repeated)
        }
        // Two jobs for one service would be one job twice: the log couldn't tell them apart.
        MilestoneImport.repeated(jobs.map { it.service }).forEach { (i, _) ->
            jobProblems.putIfAbsent(i, JobProblem.ServiceTaken(jobs[i].service!!))
        }
        val problems = MilestoneProblems(
            miles = when {
                miles == null -> MilesProblem.NotANumber
                miles in others -> MilesProblem.Taken(miles)
                repeatsEveryMiles != null && !MilestoneImport.cycleCovers(repeatsEveryMiles, miles) -> MilesProblem.PastCycle(repeatsEveryMiles)
                else -> null
            },
            months = months == -1,
            work = when {
                jobs.isEmpty() -> WorkProblem.NONE
                jobs.size > MilestoneImport.MAX_WORK -> WorkProblem.TOO_MANY
                else -> null
            },
            jobs = jobProblems,
        )
        return if (problems == MilestoneProblems() && miles != null) {
            MilestoneEdit.Ok(ImportedMilestone(miles, jobs.mapIndexed { i, job -> TableJob(names[i], job.service) }, months))
        } else {
            MilestoneEdit.Refused(problems)
        }
    }

    /**
     * How the table repeats, as typed: empty means it ends at its last milestone. [lastMiles] is the
     * table's highest mileage, which the cycle can't be shorter than.
     */
    fun repeat(text: String, lastMiles: Int): RepeatEdit {
        if (text.isBlank()) return RepeatEdit.Ok(null)
        val every = OdometerInput.parse(text)?.takeIf { it <= MilestoneImport.MAX_MILES }
            ?: return RepeatEdit.Refused(RepeatProblem.NotANumber)
        if (!MilestoneImport.cycleCovers(every, lastMiles)) return RepeatEdit.Refused(RepeatProblem.ShorterThanTable(lastMiles))
        return RepeatEdit.Ok(every)
    }
}
