package io.cosgriff.glovebox.ui.service

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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

/** What this vehicle's odometer counts in; every distance stored for it is in this unit. */
val VehicleEntity.unit: DistanceUnit get() = DistanceUnit.of(distanceUnit)

/** "12,345 mi" or "12,345 km": a distance of a vehicle that counts in [unit]. There is no unit-less version on purpose. */
fun Int.asDistance(unit: DistanceUnit): String = unit.format(this)

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
    val rule = ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: defaultRule
    val snooze = if (snoozedUntilOdometer != null || snoozedUntilDate != null) Snooze(snoozedUntilOdometer, snoozedUntilDate) else null
    return TrackedItem(id, name, Interval(miles, months), rule, snooze, firstAt = firstMiles?.let { it - (swap?.offset ?: 0) })
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
fun NextDue.text(unit: DistanceUnit): String = listOfNotNull(odometer?.asDistance(unit), date?.format(longDate)).joinToString(" or ")

fun ReminderChange.text(unit: DistanceUnit): String =
    after?.let { "${item.name} — next due ${item.interval.nextDueAfter(it).text(unit)}" }
        ?: "${item.name} — back to not logged yet"

/** "In 1,688 mi", "Overdue 12 days" */
fun DueHeadline.text(unit: DistanceUnit): String = when (this) {
    DueHeadline.NotLogged -> "Not logged yet"
    is DueHeadline.Miles -> when {
        remaining < 0 -> "Overdue ${abs(remaining).asDistance(unit)}"
        remaining == 0 -> "Due now"
        else -> "In ${remaining.asDistance(unit)}"
    }
    is DueHeadline.Days -> when {
        remaining < 0 -> "Overdue ${days(abs(remaining))}"
        remaining == 0L -> "Due today"
        else -> "In ${days(remaining)}"
    }
    is DueHeadline.Snoozed -> "Snoozed until ${snooze.until(unit)}"
}

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** "69,412 mi", "Oct 24", or "70,100 mi or Apr 10, 2027" — the year only when it isn't this one. */
fun Snooze.until(unit: DistanceUnit, today: LocalDate = LocalDate.now()): String = listOfNotNull(
    untilOdometer?.asDistance(unit),
    untilDate?.let { it.format(if (it.year == today.year) shortDate else longDate) },
).joinToString(" or ")

/** Days up to two months, then whole months — "In 151 days" is harder to picture than "In 5 months". */
private fun days(n: Long): String = when {
    n == 1L -> "1 day"
    n <= 60 -> "$n days"
    else -> "${n / 30} months"
}

fun DueRule.ruleLabel(): String = when (this) {
    DueRule.WHICHEVER_FIRST -> "Whichever comes first"
    DueRule.WHICHEVER_LAST -> "Whichever comes last"
    DueRule.MILEAGE_ONLY -> "Mileage only"
    DueRule.TIME_ONLY -> "Time only"
}

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
)

private fun savedInterval(miles: Int?, months: Int?): Interval? = if (miles == null && months == null) null else Interval(miles, months)
