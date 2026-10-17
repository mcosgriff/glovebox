package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.NextDue
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.SavedScheduleItem
import io.cosgriff.glovebox.domain.SavedTorqueSpec
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.ServiceLinks
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.nextDueAfter
import io.cosgriff.glovebox.domain.readings
import io.cosgriff.glovebox.domain.toDash
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.AppText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** "Oct 17, 2026", in the app's language (see [AppText]). */
val longDate: DateTimeFormatter get() = AppText.current.longDate

/** "Oct 17": a date in the current year. */
val shortDate: DateTimeFormatter get() = AppText.current.shortDate

/** "Oct 2026" */
val monthYear: DateTimeFormatter get() = AppText.current.monthYear

/** "12,345": a count. A distance always goes through [asDistance], which says its unit. */
fun Int.asNumber(): String = AppText.current.number.format(this)

/** What this vehicle's odometer counts in; every distance stored for it is in this unit. */
val VehicleEntity.unit: DistanceUnit get() = DistanceUnit.of(distanceUnit)

/** "12,345 mi" or "12,345 km": a distance of a vehicle that counts in [unit]. There is no unit-less version on purpose. */
fun Int.asDistance(unit: DistanceUnit): String = AppText.res.getString(
    when (unit) {
        DistanceUnit.MILES -> R.string.distance_mi
        DistanceUnit.KILOMETERS -> R.string.distance_km
    },
    asNumber(),
)

/** "mi", beside a field or a figure typed in. */
val DistanceUnit.symbolWord: String
    get() = AppText.res.getString(
        when (this) {
            DistanceUnit.MILES -> R.string.unit_mi
            DistanceUnit.KILOMETERS -> R.string.unit_km
        },
    )

/** "Miles", for a field's label or a choice. */
val DistanceUnit.titleWord: String
    get() = AppText.res.getString(
        when (this) {
            DistanceUnit.MILES -> R.string.unit_miles_title
            DistanceUnit.KILOMETERS -> R.string.unit_kilometres_title
        },
    )

/** "miles", in a sentence. */
val DistanceUnit.pluralWord: String
    get() = AppText.res.getString(
        when (this) {
            DistanceUnit.MILES -> R.string.unit_miles
            DistanceUnit.KILOMETERS -> R.string.unit_kilometres
        },
    )

/** "7,500 mi / 12 mo", "30,000 mi" or "36 mo". [unit] is the vehicle's, which is what the interval is in. */
fun Interval.text(unit: DistanceUnit): String = parts(unit, R.string.interval_slash)

/** "7,500 mi or 12 mo": the same, in a sentence. */
fun Interval.orText(unit: DistanceUnit): String = parts(unit, R.string.either_or)

/** "Every 7,500 mi or 12 mo" */
fun Interval.every(unit: DistanceUnit): String = AppText.res.getString(R.string.interval_every, orText(unit))

/** "First at 100,000 mi, then every 50,000 mi / 60 mo" for a job with a first time of its own; the plain [text] otherwise. */
fun Interval.text(unit: DistanceUnit, firstAt: Int?): String =
    if (firstAt == null) text(unit) else AppText.res.getString(R.string.interval_first_then_every, firstAt.asDistance(unit), text(unit))

private fun Interval.parts(unit: DistanceUnit, join: Int): String {
    val distance = miles?.asDistance(unit)
    val time = months?.let { AppText.res.getString(R.string.months_short, it.asNumber()) }
    return if (distance != null && time != null) AppText.res.getString(join, distance, time) else distance ?: time.orEmpty()
}

/** The interval and rule in force for a schedule item; null if it has no usable interval. */
/**
 * [swap] is the vehicle's cluster replacement, if it has had one. A job's first mileage
 * (`firstMiles`) is distance really covered, and everything in `:domain` is in dash terms, so it
 * is put into those here. **Pass the vehicle's swap wherever statuses are worked out.**
 */
fun ScheduleItemEntity.toTracked(defaultRule: DueRule, swap: ClusterSwap?): TrackedItem? {
    val miles = if (useSevere) severeMiles ?: intervalMiles else intervalMiles
    val months = if (useSevere) severeMonths ?: intervalMonths else intervalMonths
    if (miles == null && months == null) return null
    // A job only for severe use isn't a reminder on normal use: it is on the list, switched off.
    if (severeOnly && !useSevere) return null
    val rule = ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: defaultRule
    val snooze = if (snoozedUntilOdometer != null || snoozedUntilDate != null) Snooze(snoozedUntilOdometer, snoozedUntilDate) else null
    val zeroAt = -(swap?.offset ?: 0)
    return TrackedItem(id, name, Interval(miles, months), rule, snooze, firstAt = firstMiles?.let { it + zeroAt }, zeroAt = zeroAt)
}

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

/** "73,412 mi or Apr 10, 2027" */
fun NextDue.text(unit: DistanceUnit): String = either(odometer?.asDistance(unit), date?.format(longDate))

fun ReminderChange.text(unit: DistanceUnit): String =
    after?.let { AppText.res.getString(R.string.reminder_change_next_due, item.name, item.interval.nextDueAfter(it).text(unit)) }
        ?: AppText.res.getString(R.string.reminder_change_not_logged, item.name)

/** "A or B", or whichever of the two there is. */
private fun either(a: String?, b: String?): String =
    if (a != null && b != null) AppText.res.getString(R.string.either_or, a, b) else a ?: b.orEmpty()

/** "In 1,688 mi", "Overdue 12 days" */
fun DueHeadline.text(unit: DistanceUnit): String {
    val res = AppText.res
    return when (this) {
        DueHeadline.NotLogged -> res.getString(R.string.due_not_logged)
        is DueHeadline.Miles -> when {
            remaining < 0 -> res.getString(R.string.due_overdue_by, abs(remaining).asDistance(unit))
            remaining == 0 -> res.getString(R.string.due_now)
            else -> res.getString(R.string.due_in, remaining.asDistance(unit))
        }
        is DueHeadline.Days -> when {
            remaining < 0 -> res.getString(R.string.due_overdue_by, days(abs(remaining)))
            remaining == 0L -> res.getString(R.string.due_today)
            else -> res.getString(R.string.due_in, days(remaining))
        }
        is DueHeadline.Snoozed -> res.getString(R.string.due_snoozed_until, snooze.until(unit))
    }
}

/** "69,412 mi", "Oct 24", or "70,100 mi or Apr 10, 2027" — the year only when it isn't this one. */
fun Snooze.until(unit: DistanceUnit, today: LocalDate = LocalDate.now()): String = either(
    untilOdometer?.asDistance(unit),
    untilDate?.let { it.format(if (it.year == today.year) shortDate else longDate) },
)

/** Days up to two months, then whole months — "In 151 days" is harder to picture than "In 5 months". */
private fun days(n: Long): String = if (n <= 60) {
    AppText.res.getQuantityString(R.plurals.days, n.toInt(), n.toInt().asNumber())
} else {
    (n / 30).toInt().let { AppText.res.getQuantityString(R.plurals.months, it, it.asNumber()) }
}

fun DueRule.ruleLabel(): String = AppText.res.getString(
    when (this) {
        DueRule.WHICHEVER_FIRST -> R.string.rule_whichever_first
        DueRule.WHICHEVER_LAST -> R.string.rule_whichever_last
        DueRule.MILEAGE_ONLY -> R.string.rule_mileage_only
        DueRule.TIME_ONLY -> R.string.rule_time_only
    },
)

/** A torque row as an import or a file written from the vehicle sees it. */
fun TorqueSpecEntity.toSaved() = SavedTorqueSpec(
    id = id,
    name = name,
    category = TorqueCategory.of(category),
    socket = socketSize,
    torque = newtonMeters?.let(::Torque),
    source = TorqueSource.of(source),
    note = note,
)

/** A schedule item as an import or a file written from the vehicle sees it. */
fun ScheduleItemEntity.toSaved() = SavedScheduleItem(
    id = id,
    name = name,
    normal = savedInterval(intervalMiles, intervalMonths),
    severe = savedInterval(severeMiles, severeMonths),
    rule = ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() },
    templateKey = templateKey,
    tracked = tracked,
    service = service,
    firstAt = firstMiles,
    severeOnly = severeOnly,
)

private fun savedInterval(miles: Int?, months: Int?): Interval? = if (miles == null && months == null) null else Interval(miles, months)

/** Everything entered under "What it takes", as `:domain` reads it. */
fun VehicleEntity.whatItTakes(): WhatItTakes = WhatItTakes(
    oilGrade = oilSpec,
    oilApproval = oilApproval,
    oilCapacity = oilCapacity,
    oilCapacityUnit = oilCapacityUnit?.let { u -> VehicleSpecEntry.Volume.entries.firstOrNull { it.name == u } },
    oilFilter = oilFilter,
    tireSize = tireSize,
    tireSizeRear = tireSizeRear,
    tirePressureFront = tirePressureFront,
    tirePressureRear = tirePressureRear,
    tirePressureSpare = tirePressureSpare,
    tirePressureUnit = tirePressureUnit?.let { u -> VehicleSpecEntry.Pressure.entries.firstOrNull { it.name == u } },
    wiperDriver = wiperDriverIn,
    wiperPassenger = wiperPassengerIn,
    wiperRear = wiperRearIn,
    wiperNote = wiperBlades,
    transmissionFluid = transmissionFluid,
    differentialOil = differentialOil,
    transferCaseOil = transferCaseOil,
    coolant = coolantType,
    brakeFluid = brakeFluid,
    engineAirFilter = engineAirFilter,
    cabinAirFilter = cabinAirFilter,
    sparkPlug = sparkPlug,
    sparkPlugGap = sparkPlugGap,
    batteryGroup = batteryGroup,
    batteryCca = batteryCca,
    bulbLow = bulbLow,
    bulbHigh = bulbHigh,
    bulbFog = bulbFog,
)
