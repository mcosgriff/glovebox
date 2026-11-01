package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.rule
import io.cosgriff.glovebox.data.db.toModel
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.ActiveInterval
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.CountedInterval
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.GearboxOnRecord
import io.cosgriff.glovebox.domain.Gearboxes
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.LogEdits
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.OilMonths
import io.cosgriff.glovebox.domain.OwnOil
import io.cosgriff.glovebox.domain.RecordSummary
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.ServiceLinks
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.actual
import io.cosgriff.glovebox.domain.asReadOn
import io.cosgriff.glovebox.domain.readings
import io.cosgriff.glovebox.domain.toDash
import java.time.LocalDate

/**
 * A vehicle's log, readings and tracked reminders, set up the one way the screens and workers need them: in dash
 * terms through the vehicle's cluster swap, with every link a logged line counts for (built once, not per record),
 * and the lead time in the vehicle's own unit. [items] are all the vehicle's schedule items, tracked or not;
 * [records] and [readings] are as saved. Screens read a vehicle through it rather than gathering the rows (#577).
 * Read one through [VehicleLogs], or build it with [VehicleEntity.log].
 */
class VehicleLog(
    val vehicle: VehicleEntity,
    val items: List<ScheduleItemEntity>,
    val records: List<ServiceRecordWithItems>,
    /** The standalone odometer readings, as typed; [updates] has them in dash terms. */
    val readings: List<OdometerReadingEntity>,
    val swap: ClusterSwap?,
    val entries: List<ServiceEntry>,
    val updates: List<OdometerReading>,
    val tracked: List<TrackedItem>,
    val lead: LeadTime,
    /** The rule an item without its own counts by. */
    val defaultRule: DueRule,
    /** Which of [items] answer to which catalog line ([answering]), built once for every record. */
    val answering: Map<String, Set<Long>> = items.answering(),
    /** Each of [items]' group by id ([ServiceGroup.ofItem]): what a line logged against one is in when it has no catalog line. */
    val itemGroups: Map<Long, ServiceGroup> = items.associate { it.id to ServiceGroup.ofItem(it.templateKey, it.service) },
) {
    /**
     * [record] as the Markdown, the PDF report and History's timeline all read it (#579): its lines and their groups,
     * jobs, part rows, charges and cost.
     */
    fun summary(record: ServiceRecordWithItems): RecordSummary = RecordSummary.of(record.toModel(), itemGroups)

    /** The unit every figure in the vehicle's tables is in, as typed (ADR-0006). */
    val unit: DistanceUnit get() = DistanceUnit.of(vehicle.distanceUnit)

    /** The highest reading on record, as the dash shows it. */
    val odometer: Int? get() = VehicleReminders.currentOdometer(entries, updates)

    /** [odometer] as the miles really covered: the old cluster's reading added back after a swap ([ClusterSwap]). */
    val actualOdometer: Int? get() = odometer?.let { swap.actual(it) }

    fun statuses(today: LocalDate): List<ItemStatus> = VehicleReminders.statuses(tracked, entries, updates, today, lead)

    /**
     * [statuses] with each last done as it was read, so recording a cluster replacement doesn't look like the
     * service being logged again (`NoticeRules.cycle`) and announce everything a second time.
     */
    fun statusesAsRead(today: LocalDate): List<ItemStatus> =
        statuses(today).map { s -> s.copy(lastDone = s.lastDone?.let { swap.asReadOn(it) }) }

    val inputs: ReminderInputs get() = ReminderInputs(tracked, entries, updates, lead)

    /**
     * Schedule item [itemId] as its reminder counts it (#575): the one answer to what an item counts by, with the
     * owner's own oil interval and Glovebox's oil months in it, and its first time. Null when it gives no reminder.
     */
    fun tracked(itemId: Long): TrackedItem? = tracked.firstOrNull { it.id == itemId }

    /**
     * How reminders would move if the vehicle's records were [records] instead (#430): a delete, an edit, re-filed
     * lines or a file's records. [records] are the whole log after the change, readings as typed; what comes back is
     * in dash terms. A sold vehicle moves no reminders.
     */
    fun whatIf(records: List<DraftRecord>): WhatIf {
        val after = records.map { it.toEntry(swap, answering) }
        val moving = if (vehicle.archivedOn != null) emptyList() else tracked
        return WhatIf(LogEdits.reminderChanges(moving, entries, after), after)
    }
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
        readings = readings,
        swap = swap,
        entries = records.map { it.toEntry(swap, links) },
        updates = readings.toReadings(swap),
        tracked = tracked(items, settings.defaultRule, swap),
        lead = settings.leadFor(DistanceUnit.of(distanceUnit)),
        defaultRule = settings.defaultRule,
        answering = links,
    )
}

/**
 * The tracked ones of [items] as reminders ([TrackedItem.of]): what each counts by, with the vehicle's own oil interval
 * ([ownOil]) and Glovebox's oil months in it (#517, ADR-0015). Read it through [VehicleLog.tracked]; this is for a
 * caller that needs another [swap], such as milestones (by actual mileage, so none).
 */
fun VehicleEntity.tracked(items: List<ScheduleItemEntity>, defaultRule: DueRule, swap: ClusterSwap? = clusterSwap()): List<TrackedItem> {
    val unit = DistanceUnit.of(distanceUnit)
    val own = ownOil()
    val gearbox = Gearboxes.of(this.gearbox)
    // A job a maker's list gives one gearbox only reminds only while the vehicle has that one (#549).
    return items.filter { it.tracked && Gearboxes.reminds(Gearboxes.of(it.gearboxOnly), gearbox) }.mapNotNull { it.toTracked(defaultRule, swap, unit, own) }
}

/** What is on record about the vehicle's gearbox for matching a maker's list: the owner's answer, and vPIC's text. */
fun VehicleEntity.gearboxOnRecord(): GearboxOnRecord = GearboxOnRecord(Gearboxes.of(gearbox), transmission)

/**
 * The oil change as its schedule gives it, without the owner's own interval: what the owner's would replace, with
 * Glovebox's months when it has none. For an item that [isOil]; a reminder counts by [VehicleLog.tracked].
 */
fun ScheduleItemEntity.scheduledOil(unit: DistanceUnit): CountedInterval? = countedInterval(unit, ownOil = null)

/**
 * The interval and rule in force for a schedule item, as a reminder ([TrackedItem.of]); null if it is none. [unit] is
 * the vehicle's, which an oil change's worked-out months are figured in, and [ownOil] the owner's own oil change
 * interval for it ([VehicleEntity.ownOil]), which an oil change counts by instead ([countedInterval]).
 */
private fun ScheduleItemEntity.toTracked(defaultRule: DueRule, swap: ClusterSwap?, unit: DistanceUnit, ownOil: Interval?): TrackedItem? {
    val snooze = if (snoozedUntilOdometer != null || snoozedUntilDate != null) Snooze(snoozedUntilOdometer, snoozedUntilDate) else null
    val counted = countedInterval(unit, ownOil)
    return TrackedItem.of(
        id, name, counted?.interval, rule ?: defaultRule, snooze, firstMiles, severeOnly, useSevere, swap,
        monthsWorkedOut = counted?.monthsWorkedOut ?: false,
    )
}

/**
 * The interval the item's figures give: its E85 one while that is on, else its severe one while that is on, else its
 * own ([ActiveInterval.of]). Without an oil change's worked-out months; a reminder counts by [countedInterval].
 */
private fun ScheduleItemEntity.activeInterval(): Interval? = ActiveInterval.of(
    normal = savedInterval(intervalMiles, intervalMonths),
    severe = savedInterval(severeMiles, severeMonths),
    useSevere = useSevere,
    e85 = savedInterval(e85Miles, e85Months),
    useE85 = useE85,
)

/**
 * What a reminder counts by (#517, [ActiveInterval.counted]): for an oil change, [ownOil] when the owner has set one
 * ([VehicleEntity.ownOil]), Severe or not; otherwise [activeInterval]. Either way with the months Glovebox works out
 * for an oil change that has none.
 */
private fun ScheduleItemEntity.countedInterval(unit: DistanceUnit, ownOil: Interval?): CountedInterval? =
    OwnOil.counted(activeInterval(), ownOil, isOil(), unit)

/** Whether this is the vehicle's oil change ([OilMonths.isOil]): the item the owner's own oil interval replaces. */
fun ScheduleItemEntity.isOil(): Boolean = OilMonths.isOil(templateKey, service)

/** The owner's own oil change interval for this vehicle ([OwnOil]), in its unit as typed; null when it isn't set. */
fun VehicleEntity.ownOil(): Interval? = OwnOil.of(ownOilMiles, ownOilMonths)

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
fun ServiceRecordWithItems.toEntry(swap: ClusterSwap?, answering: Map<String, Set<Long>>): ServiceEntry = toDraft().toEntry(swap, answering)

/** As [ServiceRecordWithItems.toEntry]. */
fun DraftRecord.toEntry(swap: ClusterSwap?, answering: Map<String, Set<Long>>): ServiceEntry =
    swap.toDash(ServiceEntry(id, date, odometer, ServiceLinks.itemIds(lines, answering)))

/** Standalone readings in dash terms, plus the replacement cluster's first reading if there is one. */
fun List<OdometerReadingEntity>.toReadings(swap: ClusterSwap?): List<OdometerReading> =
    swap.readings(map { OdometerReading(it.date, it.odometer) })
