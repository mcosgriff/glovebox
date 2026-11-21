package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.domain.enumOrNull
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.SavedScheduleItem

/** The item's own rule; null follows the default rule, as does a name this version doesn't know. */
val ScheduleItemEntity.rule: DueRule? get() = enumOrNull<DueRule>(ruleOverride)

/**
 * A milestone of a table, a maker's or a file's, as the rows it is saved as: the milestone, and its jobs in
 * order, which take the milestone's id when they are inserted.
 */
fun ImportedMilestone.toRows(vehicleId: Long): Pair<MilestoneEntity, List<MilestoneJobEntity>> =
    MilestoneEntity(vehicleId = vehicleId, atMiles = atMiles, orMonths = orMonths) to jobRows()

fun ImportedMilestone.jobRows(): List<MilestoneJobEntity> = work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) }

/** A schedule item as an import, a maker's schedule or a file written from the vehicle sees it. */
fun ScheduleItemEntity.toSaved() = SavedScheduleItem(
    id = id,
    name = name,
    normal = savedInterval(intervalMiles, intervalMonths),
    severe = savedInterval(severeMiles, severeMonths),
    rule = this.rule,
    templateKey = templateKey,
    tracked = tracked,
    service = service,
    firstAt = firstMiles,
    severeOnly = severeOnly,
)

private fun savedInterval(miles: Int?, months: Int?): Interval? = if (miles == null && months == null) null else Interval(miles, months)
