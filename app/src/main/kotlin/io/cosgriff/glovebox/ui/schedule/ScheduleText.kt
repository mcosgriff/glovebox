package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.JobProblem
import io.cosgriff.glovebox.domain.KnownIntervalPlan
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.MilesProblem
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.RepeatConflict
import io.cosgriff.glovebox.domain.RepeatProblem
import io.cosgriff.glovebox.domain.ScheduleTemplateItem
import io.cosgriff.glovebox.domain.TemplateName
import io.cosgriff.glovebox.domain.WorkProblem
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.pluralWord

/** A typical item's name, as it is saved when the item is added. */
val ScheduleTemplateItem.label: String
    get() = AppText.res.getString(
        when (name) {
            TemplateName.OIL -> R.string.template_oil
            TemplateName.ROTATION -> R.string.template_rotation
            TemplateName.ENGINE_AIR -> R.string.template_engine_air
            TemplateName.CABIN_AIR -> R.string.template_cabin_air
            TemplateName.BRAKE_INSPECTION -> R.string.template_brake_inspection
            TemplateName.BRAKE_FLUID -> R.string.template_brake_fluid
            TemplateName.TRANSMISSION_FLUID -> R.string.template_transmission_fluid
            TemplateName.DIFF_AND_TRANSFER -> R.string.template_diff_and_transfer
            TemplateName.DIFF -> R.string.template_diff
            TemplateName.ENGINE_COOLANT -> R.string.template_engine_coolant
            TemplateName.COOLANT -> R.string.template_coolant
            TemplateName.SPARK_PLUGS -> R.string.template_spark_plugs
        },
    )

fun MilesProblem.text(unit: DistanceUnit): String = when (this) {
    MilesProblem.NotANumber -> AppText.res.getString(R.string.milestone_miles_bad, MilestoneImport.MAX_MILES.asNumber())
    is MilesProblem.Taken -> AppText.res.getString(R.string.milestone_miles_taken, miles.asDistance(unit))
    is MilesProblem.PastCycle -> AppText.res.getString(R.string.milestone_miles_past_cycle, every.asDistance(unit))
}

val WorkProblem.text: String
    get() = when (this) {
        WorkProblem.NONE -> AppText.res.getString(R.string.milestone_work_none)
        WorkProblem.TOO_MANY -> AppText.res.getString(R.string.milestone_work_too_many, MilestoneImport.MAX_WORK.asNumber())
    }

val JobProblem.text: String
    get() = when (this) {
        JobProblem.NoName -> AppText.res.getString(R.string.milestone_job_no_name)
        JobProblem.TooLong -> AppText.res.getString(R.string.milestone_job_too_long, MilestoneImport.MAX_NAME.asNumber())
        JobProblem.NotOneLine -> AppText.res.getString(R.string.milestone_job_one_line)
        JobProblem.Repeated -> AppText.res.getString(R.string.milestone_job_repeated)
        is JobProblem.ServiceTaken -> AppText.res.getString(R.string.milestone_job_service_taken, catalogLabel(service))
    }

val monthsProblem: String get() = AppText.res.getString(R.string.milestone_months_bad, MilestoneImport.MAX_MONTHS.asNumber())

fun RepeatProblem.text(unit: DistanceUnit): String = when (this) {
    RepeatProblem.NotANumber -> AppText.res.getString(R.string.milestone_repeat_bad, unit.pluralWord, MilestoneImport.MAX_MILES.asNumber())
    is RepeatProblem.ShorterThanTable -> AppText.res.getString(R.string.milestone_repeat_shorter, lastMiles.asDistance(unit))
}

/** Why a file can't go into a vehicle whose table repeats otherwise. */
fun RepeatConflict.text(unit: DistanceUnit): String {
    fun says(every: Int?) = every?.let { AppText.res.getString(R.string.repeat_says_every, it.asDistance(unit)) } ?: AppText.res.getString(R.string.repeat_says_not)
    return AppText.res.getString(R.string.repeat_conflict, says(saved), says(file))
}

/** Said on the Import page before and after: whether the table goes on past its last milestone. */
fun repeatNote(every: Int?, unit: DistanceUnit): String =
    every?.let { AppText.res.getString(R.string.repeat_note_every, it.asDistance(unit)) } ?: AppText.res.getString(R.string.repeat_note_ends)

/** What is said where a maker's table is offered. */
fun tableOffer(list: KnownScheduleList): String =
    AppText.res.getString(R.string.known_table_offer, milestones(list.milestones.size), list.source.name, list.source.note)

/** What is said where a maker's reminders are offered. */
fun intervalOffer(list: KnownScheduleList, plan: KnownIntervalPlan): String {
    val res = AppText.res
    val offer = res.getString(R.string.known_interval_offer, res.getQuantityString(R.plurals.reminders, plan.new.size, plan.new.size.asNumber()), list.source.name, list.source.note)
    if (plan.kept.isEmpty()) return offer
    return res.getString(R.string.known_interval_offer_kept, offer, res.getQuantityString(R.plurals.known_kept, plan.kept.size, plan.kept.size.asNumber()))
}

/** The line kept beside whatever came from a maker's list. */
fun startedFrom(credit: String?): String =
    AppText.res.getString(R.string.known_started_from, credit ?: AppText.res.getString(R.string.known_makers_guide))

private fun milestones(n: Int) = AppText.res.getQuantityString(R.plurals.milestones, n, n.asNumber())
