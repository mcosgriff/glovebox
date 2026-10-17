package io.cosgriff.glovebox.domain

import java.util.Locale

/** What is wrong with a milestone as typed, each a sentence for under its field. [jobs] is by the job's place in the list. */
data class MilestoneProblems(
    val miles: String? = null,
    val months: String? = null,
    /** About the list of jobs as a whole. */
    val work: String? = null,
    val jobs: Map<Int, String> = emptyMap(),
)

sealed interface MilestoneEdit {
    data class Ok(val milestone: ImportedMilestone) : MilestoneEdit
    data class Refused(val problems: MilestoneProblems) : MilestoneEdit
}

sealed interface RepeatEdit {
    /** [everyMiles] null: the table ends at its last milestone. */
    data class Ok(val everyMiles: Int?) : RepeatEdit
    data class Refused(val why: String) : RepeatEdit
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
        unit: DistanceUnit,
        /** A catalog line's name, from its id. */
        serviceName: (String) -> String,
    ): MilestoneEdit {
        val miles = OdometerInput.parse(milesText)?.takeIf { it <= MilestoneImport.MAX_MILES }
        val months = monthsText.trim().takeIf { it.isNotEmpty() }?.let { t -> t.toIntOrNull()?.takeIf { it in 1..MilestoneImport.MAX_MONTHS } ?: -1 }
        val names = jobs.map { it.name.trim() }
        val jobProblems = mutableMapOf<Int, String>()
        names.forEachIndexed { i, name ->
            when {
                name.isEmpty() -> jobProblems[i] = "Give it a name, or remove it"
                name.length > MilestoneImport.MAX_NAME -> jobProblems[i] = "Keep the name to ${MilestoneImport.MAX_NAME} characters"
                name.any { it.isISOControl() } -> jobProblems[i] = "The name has to be on one line"
            }
        }
        MilestoneImport.repeated(names.map { n -> n.takeIf { it.isNotEmpty() }?.let(MilestoneImport::nameKey) }).forEach { (i, _) ->
            jobProblems.putIfAbsent(i, "This job is already in the milestone")
        }
        // Two jobs for one service would be one job twice: the log couldn't tell them apart.
        MilestoneImport.repeated(jobs.map { it.service }).forEach { (i, _) ->
            jobProblems.putIfAbsent(i, "\"${jobs[i].service?.let(serviceName).orEmpty()}\" is already picked for another job here")
        }
        val problems = MilestoneProblems(
            miles = when {
                miles == null -> "Enter the mileage as a whole number from 1 to ${miles(MilestoneImport.MAX_MILES)}"
                miles in others -> "There is already a milestone at ${unit.format(miles)}. Add the work to that one"
                repeatsEveryMiles != null && !MilestoneImport.cycleCovers(repeatsEveryMiles, miles) ->
                    "This table repeats every ${unit.format(repeatsEveryMiles)}, so a milestone can't be past that. Change how it repeats first"
                else -> null
            },
            months = "Enter whole months from 1 to ${MilestoneImport.MAX_MONTHS}, or leave it empty".takeIf { months == -1 },
            work = when {
                jobs.isEmpty() -> "Add at least one job"
                jobs.size > MilestoneImport.MAX_WORK -> "A milestone can have ${MilestoneImport.MAX_WORK} jobs at most"
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
    fun repeat(text: String, lastMiles: Int, unit: DistanceUnit): RepeatEdit {
        if (text.isBlank()) return RepeatEdit.Ok(null)
        val every = OdometerInput.parse(text)?.takeIf { it <= MilestoneImport.MAX_MILES }
            ?: return RepeatEdit.Refused("Enter whole ${unit.plural} from 1 to ${miles(MilestoneImport.MAX_MILES)}, or leave it empty for a table that ends")
        if (!MilestoneImport.cycleCovers(every, lastMiles)) {
            return RepeatEdit.Refused("It is the length of the whole cycle, so it can't be less than your last milestone (${unit.format(lastMiles)})")
        }
        return RepeatEdit.Ok(every)
    }

    private fun miles(value: Int): String = String.format(Locale.US, "%,d", value)
}
