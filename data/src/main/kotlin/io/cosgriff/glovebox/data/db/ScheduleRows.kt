package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.domain.enumOrNull
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.ImportedScheduleItem

/** The item's own rule; null follows the default rule, as does a name this version doesn't know. */
val ScheduleItemEntity.rule: DueRule? get() = enumOrNull<DueRule>(ruleOverride)

/**
 * A milestone of a table, a maker's or a file's, as the rows it is saved as: the milestone, and its jobs in
 * order, which take the milestone's id when they are inserted.
 */
fun ImportedMilestone.toRows(vehicleId: Long): Pair<MilestoneEntity, List<MilestoneJobEntity>> =
    MilestoneEntity(vehicleId = vehicleId, atMiles = atMiles, orMonths = orMonths) to jobRows()

fun ImportedMilestone.jobRows(): List<MilestoneJobEntity> = work.map { MilestoneJobEntity(milestoneId = 0, name = it.name, service = it.service) }

/**
 * An interval from an import file or a maker's schedule as an item of the user's own, tracked. [knownList] is
 * the bundled list it came from, kept on the row for good; [useSevere] whether severe use is on, and [useE85]
 * whether the vehicle runs mostly on E85 (which matters only to an item with an E85 interval).
 */
fun ImportedScheduleItem.toEntity(vehicleId: Long, knownList: String? = null, useSevere: Boolean = false, useE85: Boolean = false): ScheduleItemEntity = ScheduleItemEntity(
    vehicleId = vehicleId,
    name = name,
    intervalMiles = normal.miles,
    intervalMonths = normal.months,
    severeMiles = severe?.miles,
    severeMonths = severe?.months,
    useSevere = useSevere,
    severeOnly = severeOnly,
    e85Miles = e85?.miles,
    e85Months = e85?.months,
    useE85 = useE85 && e85 != null,
    ruleOverride = rule?.name,
    service = service,
    knownList = knownList,
    firstMiles = firstAt,
    gearboxOnly = gearbox?.name,
)
