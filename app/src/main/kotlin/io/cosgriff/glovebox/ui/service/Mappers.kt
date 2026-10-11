package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.NextDue
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.nextDueAfter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

fun Int.asMiles(): String = String.format(Locale.US, "%,d mi", this)

/** The interval and rule in force for a schedule item; null if it has no usable interval. */
fun ScheduleItemEntity.toTracked(defaultRule: DueRule): TrackedItem? {
    val miles = if (useSevere) severeMiles ?: intervalMiles else intervalMiles
    val months = if (useSevere) severeMonths ?: intervalMonths else intervalMonths
    if (miles == null && months == null) return null
    val rule = ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: defaultRule
    val snooze = if (snoozedUntilOdometer != null || snoozedUntilDate != null) Snooze(snoozedUntilOdometer, snoozedUntilDate) else null
    return TrackedItem(id, name, Interval(miles, months), rule, snooze)
}

fun ServiceRecordWithItems.toEntry(): ServiceEntry =
    ServiceEntry(record.id, record.date, record.odometer, items.mapNotNull { it.scheduleItemId }.toSet())

/** "73,412 mi or Apr 10, 2027" */
fun NextDue.text(): String = listOfNotNull(odometer?.asMiles(), date?.format(longDate)).joinToString(" or ")

fun ReminderChange.text(): String =
    after?.let { "${item.name} — next due ${item.interval.nextDueAfter(it).text()}" }
        ?: "${item.name} — back to not logged yet"

/** "In 1,688 mi", "Overdue 12 days" */
fun DueHeadline.text(): String = when (this) {
    DueHeadline.NotLogged -> "Not logged yet"
    is DueHeadline.Miles -> when {
        remaining < 0 -> "Overdue ${abs(remaining).asMiles()}"
        remaining == 0 -> "Due now"
        else -> "In ${remaining.asMiles()}"
    }
    is DueHeadline.Days -> when {
        remaining < 0 -> "Overdue ${days(abs(remaining))}"
        remaining == 0L -> "Due today"
        else -> "In ${days(remaining)}"
    }
    is DueHeadline.Snoozed -> "Snoozed until ${snooze.until()}"
}

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** "69,412 mi", "Oct 24", or "70,100 mi or Apr 10, 2027" — the year only when it isn't this one. */
fun Snooze.until(today: LocalDate = LocalDate.now()): String = listOfNotNull(
    untilOdometer?.asMiles(),
    untilDate?.let { it.format(if (it.year == today.year) shortDate else longDate) },
).joinToString(" or ")

/** Days up to two months, then whole months — "In 151 days" is harder to picture than "In 5 months". */
private fun days(n: Long): String = when {
    n == 1L -> "1 day"
    n <= 60 -> "$n days"
    else -> "${n / 30} months"
}
