package io.cosgriff.glovebox.data.milestones

import io.cosgriff.glovebox.data.db.MakersSchedules
import io.cosgriff.glovebox.data.db.MilestoneWithJobs
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.log.NoLinks
import io.cosgriff.glovebox.data.log.VehicleLog
import io.cosgriff.glovebox.data.log.gearboxOnRecord
import io.cosgriff.glovebox.data.log.toEntry
import io.cosgriff.glovebox.data.log.tracked
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.GearboxOnRecord
import io.cosgriff.glovebox.domain.JobDone
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import io.cosgriff.glovebox.domain.MilestoneContinuation
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.MilestoneStatus
import io.cosgriff.glovebox.domain.Milestones
import io.cosgriff.glovebox.domain.SavedMilestone
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.actual
import io.cosgriff.glovebox.domain.asReadOn

/**
 * Where a table added from a maker's list started (`vehicles.milestonesKnownList`), or a vehicle's schedule items
 * (`schedule_items.knownList`, [ScheduleItems.startedFrom][io.cosgriff.glovebox.data.db.ScheduleItems.startedFrom]):
 * [credit] is that list's ([KnownSchedules.credit]), null once the list no longer ships.
 */
data class StartedFrom(val credit: String?)

/**
 * What the Milestones screen shows of a vehicle, without its wording (#621). Every figure is actual mileage, in the
 * vehicle's own unit as typed (ADR-0006): a manual's mileages are miles really covered.
 */
data class MilestonesView(
    val milestones: List<MilestoneStatus>,
    /** Miles the vehicle has really covered; null with no reading yet. */
    val current: Int?,
    val swap: ClusterSwap?,
    val unit: DistanceUnit,
    /** False when nothing tracked repeats by mileage, so there is nothing to build milestones from. */
    val hasMileageItems: Boolean,
    /** True when these are the user's own table, shown in place of the view built from the schedule. */
    val fromTable: Boolean,
    /** The user's table as written, by mileage; a table that repeats shows more milestones than this. Empty with no table. */
    val table: List<SavedMilestone> = emptyList(),
    /** The length of the cycle when the user's table starts again after its last milestone; null when it ends. */
    val repeatsEveryMiles: Int? = null,
    /** The maker's schedule the vehicle is on ([MakersSchedules.forVehicle]), while it has no table of its own. */
    val known: KnownScheduleList? = null,
    /** For a table that was added from a maker's list, whose list it was. */
    val startedFrom: StartedFrom? = null,
    /** The vehicle's drive type, while there is no table: it decides which of a maker's jobs are for it. */
    val drivetrain: String? = null,
    /** And its gearbox (#549), the same way. */
    val gearbox: GearboxOnRecord = GearboxOnRecord(null),
    /** Where a maker's table ends when Glovebox carries it on past its last column (#568); null when it isn't. */
    val continuedAfter: Int? = null,
    /** The names of the maker's jobs that aren't carried on, as the table gives them no even spacing. */
    val notContinued: List<String> = emptyList(),
)

/**
 * A vehicle's milestones (#621): its log, its own table and the makers' lists laid out by actual mileage. Worked out
 * each time and stored nowhere; nothing here decides what is due (milestones.md: a view, never a second source).
 *
 * The user's table, when there is one, is shown in place of the view built from the schedule. A job of a table is
 * done by a logged line's catalog line or its name ([MilestoneImport.doneKeys]); a milestone built from the schedule
 * is done by the schedule item a line counts for.
 */
object VehicleMilestones {
    /**
     * [log]'s milestones, with [table] (the user's own, empty with none) and the bundled [lists]. [ahead] is how many
     * milestones past the odometer are shown, which "Show more" raises from [Milestones.AHEAD].
     */
    fun of(log: VehicleLog, table: List<MilestoneWithJobs>, lists: List<KnownScheduleList>, ahead: Int = Milestones.AHEAD): MilestonesView =
        if (table.isEmpty()) generated(log, lists, ahead) else typed(log, table, lists, ahead)

    /** The user's own table, as it is. */
    private fun typed(log: VehicleLog, rows: List<MilestoneWithJobs>, lists: List<KnownScheduleList>, ahead: Int): MilestonesView {
        val vehicle = log.vehicle
        val swap = log.swap
        val current = log.actualOdometer
        val saved = rows.map { r -> SavedMilestone(r.milestone.id, r.milestone.atMiles, r.ordered.map { TableJob(it.name, it.service) }, r.milestone.orMonths) }
        // A logged line completes a job by its catalog service, or by being called the same thing.
        val done = log.records.flatMap { r ->
            val e = r.toEntry(swap, NoLinks)
            r.items.flatMap { line ->
                MilestoneImport.doneKeys(line.label, line.catalogId).map { JobDone(it, e.date, swap.actual(e.odometer), swap.asReadOn(e.odometer, e.date), e.id) }
            }
        }
        // A table that repeats goes on past what is shown, like the view built from the schedule; one that ends doesn't.
        val every = vehicle.milestonesRepeatEveryMiles?.takeIf { it >= saved.maxOf { m -> m.atMiles } }
        // A maker's table that doesn't repeat is carried on past its last column from its own rows (#568).
        val makers = tableList(vehicle, lists)
        val laid = MilestoneContinuation.laidOut(
            saved, every, fromMaker = vehicle.milestonesKnownList != null,
            // As the table shows them: a one-gearbox job on "Don't know" too, since the view reminds of nothing (#549).
            later = makers?.let { KnownSchedules.intervalsIn(it, log.unit, vehicle.drivetrain, vehicle.gearboxOnRecord(), onTable = true) }.orEmpty(),
            current = current, more = ahead - Milestones.AHEAD,
        )
        return MilestonesView(
            milestones = Milestones.status(laid.milestones, done, current, continues = every != null),
            continuedAfter = laid.endsAt.takeIf { laid.continues },
            notContinued = if (laid.continues) laid.notContinued.map { it.name } else emptyList(),
            current = current,
            swap = swap,
            unit = log.unit,
            hasMileageItems = true,
            fromTable = true,
            table = saved,
            repeatsEveryMiles = every,
            startedFrom = vehicle.milestonesKnownList?.let { StartedFrom(makers?.let(KnownSchedules::credit)) },
        )
    }

    /** The view built from the vehicle's schedule: each tracked item with a mileage interval, by its multiples. */
    private fun generated(log: VehicleLog, lists: List<KnownScheduleList>, ahead: Int): MilestonesView {
        val vehicle = log.vehicle
        val swap = log.swap
        val current = log.actualOdometer
        val tracked = byActualMileage(log)
        val keys = tracked.map { it.id }.toSet()
        val done = log.entries.flatMap { e ->
            e.itemIds.filter { it in keys }.map { JobDone(it.toString(), e.date, swap.actual(e.odometer), swap.asReadOn(e.odometer, e.date), e.id) }
        }
        val list = Milestones.generate(tracked, Milestones.startFor(done, current), current ?: 0, ahead)
        return MilestonesView(
            milestones = Milestones.status(list, done, current, continues = true),
            current = current,
            swap = swap,
            unit = log.unit,
            hasMileageItems = tracked.isNotEmpty(),
            fromTable = false,
            known = MakersSchedules.forVehicle(lists, vehicle, log.items),
            drivetrain = vehicle.drivetrain,
            gearbox = vehicle.gearboxOnRecord(),
        )
    }

    /**
     * The log's tracked items that repeat by mileage, taken again with no swap: [VehicleLog.tracked] is in dash terms,
     * and milestones are by actual mileage, and so is a first time.
     */
    private fun byActualMileage(log: VehicleLog): List<TrackedItem> =
        log.vehicle.tracked(log.items, log.defaultRule, swap = null).filter { it.interval.miles != null }

    /**
     * The maker's list the table was added from, while it still ships. Not [MakersSchedules.forVehicle]: that is the
     * list the schedule is on, and a table keeps the list it came from whatever the schedule is taken up from later.
     */
    private fun tableList(vehicle: VehicleEntity, lists: List<KnownScheduleList>): KnownScheduleList? =
        vehicle.milestonesKnownList?.let { id -> lists.firstOrNull { it.id == id } }
}
