package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.NextDue
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.nextDueAfter
import java.time.format.DateTimeFormatter
import java.util.Locale

val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

fun Int.asMiles(): String = String.format(Locale.US, "%,d mi", this)

/** The interval and rule in force for a schedule item; null if it has no usable interval. */
fun ScheduleItemEntity.toTracked(defaultRule: DueRule): TrackedItem? {
    val miles = if (useSevere) severeMiles ?: intervalMiles else intervalMiles
    val months = if (useSevere) severeMonths ?: intervalMonths else intervalMonths
    if (miles == null && months == null) return null
    val rule = ruleOverride?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: defaultRule
    return TrackedItem(id, name, Interval(miles, months), rule)
}

fun ServiceRecordWithItems.toEntry(): ServiceEntry =
    ServiceEntry(record.id, record.date, record.odometer, items.mapNotNull { it.scheduleItemId }.toSet())

/** "73,412 mi or Apr 10, 2027" */
fun NextDue.text(): String = listOfNotNull(odometer?.asMiles(), date?.format(longDate)).joinToString(" or ")

fun ReminderChange.text(): String =
    after?.let { "${item.name} — next due ${item.interval.nextDueAfter(it).text()}" }
        ?: "${item.name} — back to not logged yet"
