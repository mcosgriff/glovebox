package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.rule
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.ActiveInterval
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.ServiceLinks
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.readings
import io.cosgriff.glovebox.domain.toDash
import java.time.LocalDate

/**
 * A vehicle's log, readings and tracked reminders, set up the one way the screens and workers need them: in dash
 * terms through the vehicle's cluster swap, with every link a logged line counts for (built once, not per record),
 * and the lead time in the vehicle's own unit. [items] are all the vehicle's schedule items, tracked or not;
 * [records] are as saved. Read one through [VehicleLogs], or build it with [VehicleEntity.log].
 */
class VehicleLog(
    val vehicle: VehicleEntity,
    val items: List<ScheduleItemEntity>,
    val records: List<ServiceRecordWithItems>,
    val swap: ClusterSwap?,
    val entries: List<ServiceEntry>,
    val updates: List<OdometerReading>,
    val tracked: List<TrackedItem>,
    val lead: LeadTime,
    /** The rule an item without its own counts by. */
    val defaultRule: DueRule,
) {
    /** The highest reading on record, as the dash shows it. */
    val odometer: Int? get() = VehicleReminders.currentOdometer(entries, updates)

    fun statuses(today: LocalDate): List<ItemStatus> = VehicleReminders.statuses(tracked, entries, updates, today, lead)

    /**
     * [statuses] with each last done as it was read, so recording a cluster replacement doesn't look like the
     * service being logged again (`NoticeRules.cycle`) and announce everything a second time.
     */
    fun statusesAsRead(today: LocalDate): List<ItemStatus> =
        statuses(today).map { s -> s.copy(lastDone = s.lastDone?.let { swap.asReadOn(it) }) }

    val inputs: ReminderInputs get() = ReminderInputs(tracked, entries, updates, lead)
}

/** [items] may be every schedule item or only the tracked ones: only tracked items get a reminder either way. */
fun VehicleEntity.log(
    items: List<ScheduleItemEntity>,
    records: List<ServiceRecordWithItems>,
    readings: List<OdometerReadingEntity>,
    settings: Settings,
): VehicleLog {
    val swap = clusterSwap()
    val links = items.answering()
    return VehicleLog(
        vehicle = this,
        items = items,
        records = records,
        swap = swap,
        entries = records.map { it.toEntry(swap, links) },
        updates = readings.toReadings(swap),
        tracked = items.filter { it.tracked }.mapNotNull { it.toTracked(settings.defaultRule, swap) },
        lead = settings.leadFor(DistanceUnit.of(distanceUnit)),
        defaultRule = settings.defaultRule,
    )
}

/** The highest reading on record, as the dash shows it, where nothing else about the log is needed. */
fun VehicleEntity.highestReading(records: List<ServiceRecordWithItems>, readings: List<OdometerReadingEntity>): Int? {
    val swap = clusterSwap()
    return VehicleReminders.currentOdometer(records.map { it.toEntry(swap, NoLinks) }, readings.toReadings(swap))
}

/** The interval and rule in force for a schedule item, as a reminder ([TrackedItem.of]); null if it is none. */
fun ScheduleItemEntity.toTracked(defaultRule: DueRule, swap: ClusterSwap?): TrackedItem? {
    val snooze = if (snoozedUntilOdometer != null || snoozedUntilDate != null) Snooze(snoozedUntilOdometer, snoozedUntilDate) else null
    return TrackedItem.of(id, name, activeInterval(), rule ?: defaultRule, snooze, firstMiles, severeOnly, useSevere, swap)
}

/** The interval the item counts by: its E85 one while that is on, else its severe one while that is on, else its own ([ActiveInterval]). */
fun ScheduleItemEntity.activeInterval(): Interval? = ActiveInterval.of(
    normal = savedInterval(intervalMiles, intervalMonths),
    severe = savedInterval(severeMiles, severeMonths),
    useSevere = useSevere,
    e85 = savedInterval(e85Miles, e85Months),
    useE85 = useE85,
)

private fun savedInterval(miles: Int?, months: Int?): Interval? = if (miles == null && months == null) null else Interval(miles, months)

/** Null unless the vehicle has had its instrument cluster replaced. */
fun VehicleEntity.clusterSwap(): ClusterSwap? {
    val on = clusterSwappedOn ?: return null
    val start = clusterStartReading ?: return null
    return ClusterSwap(on, odometerOffset, start)
}

fun ScheduleItemEntity.toRef(): ScheduleRef = ScheduleRef(id, name, templateKey, tracked, service)

/** Which of these schedule items answer to which built-in service line: see [ServiceLinks]. */
fun List<ScheduleItemEntity>.answering(): Map<String, Set<Long>> = ServiceLinks.answering(map { it.toRef() })

/** For a caller that only wants the readings out of the log and doesn't care which reminders a line counts for. */
val NoLinks: Map<String, Set<Long>> = emptyMap()

/**
 * The odometer is in dash terms (see [ClusterSwap]); `record.odometer` stays as it was typed.
 * [answering] is the vehicle's own ([answering] above): a line counts for the item it was saved
 * against and for every item that answers to the catalog line it was picked from.
 */
fun ServiceRecordWithItems.toEntry(swap: ClusterSwap?, answering: Map<String, Set<Long>>): ServiceEntry =
    swap.toDash(ServiceEntry(record.id, record.date, record.odometer, ServiceLinks.itemIds(items.map { it.scheduleItemId to it.catalogId }, answering)))

/** Standalone readings in dash terms, plus the replacement cluster's first reading if there is one. */
fun List<OdometerReadingEntity>.toReadings(swap: ClusterSwap?): List<OdometerReading> =
    swap.readings(map { OdometerReading(it.date, it.odometer) })
