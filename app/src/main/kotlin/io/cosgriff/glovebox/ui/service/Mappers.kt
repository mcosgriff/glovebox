package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.domain.enumOrNull
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.domain.VehicleReminders
import java.text.DecimalFormatSymbols
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.RecordMark
import io.cosgriff.glovebox.domain.DoneBy
import io.cosgriff.glovebox.domain.Who
import io.cosgriff.glovebox.domain.OdometerNote
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.domain.DocumentProblem
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.rule
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ActiveInterval
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
import io.cosgriff.glovebox.domain.VisitRecord
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
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
    val interval = activeInterval() ?: return null
    // A job only for severe use isn't a reminder on normal use: it is on the list, switched off.
    if (severeOnly && !useSevere) return null
    val rule = this.rule ?: defaultRule
    val snooze = if (snoozedUntilOdometer != null || snoozedUntilDate != null) Snooze(snoozedUntilOdometer, snoozedUntilDate) else null
    val zeroAt = -(swap?.offset ?: 0)
    return TrackedItem(id, name, interval, rule, snooze, firstAt = firstMiles?.let { it + zeroAt }, zeroAt = zeroAt)
}

/** The interval the item counts by: its E85 one while that is on, else its severe one while that is on, else its own ([ActiveInterval]). */
fun ScheduleItemEntity.activeInterval(): Interval? = ActiveInterval.of(
    normal = savedInterval(intervalMiles, intervalMonths),
    severe = savedInterval(severeMiles, severeMonths),
    useSevere = useSevere,
    e85 = savedInterval(e85Miles, e85Months),
    useE85 = useE85,
)

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
/**
 * "2019 Blue truck": the year, then the nickname, or the model when there is none. The order is wording, so it is
 * a string resource. A blank nickname counts as none, as in `VehicleChoice`.
 */
fun VehicleEntity.shortName(): String = AppText.res.getString(R.string.vehicle_short_name, "$year", nicknameOrModel())

/** "Blue truck": the nickname, or the model when there is none. */
fun VehicleEntity.nicknameOrModel(): String = nickname?.takeIf { it.isNotBlank() } ?: model

/** "2019 Blue truck · 4.0L V6": a screen's subtitle, with the engine when it is known. */
fun VehicleEntity.subtitle(): String = listOfNotNull(shortName(), engine?.takeIf { it.isNotBlank() }).joinToString(" · ")

/** "2019 Toyota 4Runner", which a suggested file name is made from. */
fun VehicleEntity.fileStem(): String = "$year $make $model"

/** A record as `Visits` sees it (#307). */
fun ServiceRecordEntity.toVisit(): VisitRecord = VisitRecord(id, visitId, date, odometer, shopName.takeIf { !diy })

fun ServiceRecordWithItems.toEntry(swap: ClusterSwap?, answering: Map<String, Set<Long>>): ServiceEntry =
    swap.toDash(ServiceEntry(record.id, record.date, record.odometer, ServiceLinks.itemIds(items.map { it.scheduleItemId to it.catalogId }, answering)))

/** Standalone readings in dash terms, plus the replacement cluster's first reading if there is one. */
fun List<OdometerReadingEntity>.toReadings(swap: ClusterSwap?): List<OdometerReading> =
    swap.readings(map { OdometerReading(it.date, it.odometer) })

/**
 * A vehicle's log, readings and tracked reminders, set up the one way the screens and workers need them: in dash
 * terms through the vehicle's cluster swap, with every link a logged line counts for (built once, not per record),
 * and the lead time in the vehicle's own unit. Build it with [VehicleEntity.log].
 */
class VehicleLog(
    val swap: ClusterSwap?,
    val entries: List<ServiceEntry>,
    val updates: List<OdometerReading>,
    val tracked: List<TrackedItem>,
    val lead: LeadTime,
) {
    /** The highest reading on record, as the dash shows it. */
    val odometer: Int? get() = VehicleReminders.currentOdometer(entries, updates)

    fun statuses(today: LocalDate): List<ItemStatus> = VehicleReminders.statuses(tracked, entries, updates, today, lead)

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
        swap = swap,
        entries = records.map { it.toEntry(swap, links) },
        updates = readings.toReadings(swap),
        tracked = items.filter { it.tracked }.mapNotNull { it.toTracked(settings.defaultRule, swap) },
        lead = settings.leadFor(unit),
    )
}

/** The highest reading on record, as the dash shows it, where nothing else about the log is needed. */
fun VehicleEntity.highestReading(records: List<ServiceRecordWithItems>, readings: List<OdometerReadingEntity>): Int? {
    val swap = clusterSwap()
    return VehicleReminders.currentOdometer(records.map { it.toEntry(swap, NoLinks) }, readings.toReadings(swap))
}

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
/**
 * How far off or overdue, without saying which: "1,688 mi", "21 days". Null when there is no amount
 * (due now, due today, snoozed, not logged). For a phrase of a screen's own ("Next: Oil in 1,688 mi").
 */
fun DueHeadline.amount(unit: DistanceUnit): String? = when (this) {
    is DueHeadline.Miles -> remaining.takeIf { it != 0 }?.let { abs(it).asDistance(unit) }
    is DueHeadline.Days -> remaining.takeIf { it != 0L }?.let { days(abs(it)) }
    else -> null
}

/** Whether the headline counts down (true), is past (false), or neither (null). */
val DueHeadline.ahead: Boolean?
    get() = when (this) {
        is DueHeadline.Miles -> if (remaining == 0) null else remaining > 0
        is DueHeadline.Days -> if (remaining == 0L) null else remaining > 0
        else -> null
    }

private fun days(n: Long): String = if (n <= 60) {
    AppText.res.getQuantityString(R.plurals.days, n.toInt(), n.toInt().asNumber())
} else {
    (n / 30).toInt().let { AppText.res.getQuantityString(R.plurals.months, it, it.asNumber()) }
}

/** "whichever comes first": the rule inside a sentence. */
fun DueRule.ruleInSentence(): String = AppText.res.getString(
    when (this) {
        DueRule.WHICHEVER_FIRST -> R.string.rule_whichever_first_sentence
        DueRule.WHICHEVER_LAST -> R.string.rule_whichever_last_sentence
        DueRule.MILEAGE_ONLY -> R.string.rule_mileage_only_sentence
        DueRule.TIME_ONLY -> R.string.rule_time_only_sentence
    },
)

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
    rule = this.rule,
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
    oilCapacityUnit = enumOrNull<VehicleSpecEntry.Volume>(oilCapacityUnit),
    oilFilter = oilFilter,
    tireSize = tireSize,
    tireSizeRear = tireSizeRear,
    tirePressureFront = tirePressureFront,
    tirePressureRear = tirePressureRear,
    tirePressureSpare = tirePressureSpare,
    tirePressureUnit = enumOrNull<VehicleSpecEntry.Pressure>(tirePressureUnit),
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

/** "+120,000 mi", "−2,500 mi": what to add to a replaced cluster's reading to get the actual mileage. */
fun offsetLabel(offset: Int, unit: DistanceUnit): String =
    AppText.res.getString(if (offset < 0) R.string.offset_minus else R.string.offset_plus, abs(offset).asDistance(unit))

/** "340 KB", "4.2 MB": a file's size. */
fun fileSize(bytes: Long): String = if (bytes < 1024 * 1024) {
    AppText.res.getString(R.string.size_kb, ((bytes + 1023) / 1024).coerceAtLeast(1).toInt().asNumber())
} else {
    val format = java.text.NumberFormat.getNumberInstance(AppText.locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    AppText.res.getString(R.string.size_mb, format.format(bytes / (1024.0 * 1024.0)))
}

/** "Vehicle history report" */
val DocumentType.label: String
    get() = AppText.res.getString(
        when (this) {
            DocumentType.REPORT -> R.string.document_report
            DocumentType.TITLE -> R.string.document_title
            DocumentType.REGISTRATION -> R.string.document_registration
            DocumentType.PURCHASE -> R.string.document_purchase
            DocumentType.WARRANTY -> R.string.document_warranty
            DocumentType.INSURANCE -> R.string.document_insurance
            DocumentType.OTHER -> R.string.document_other
        },
    )

/** Why a document wasn't added. */
val DocumentProblem.text: String
    get() = when (this) {
        DocumentProblem.EMPTY -> AppText.res.getString(R.string.document_problem_empty)
        DocumentProblem.TOO_BIG -> AppText.res.getString(R.string.document_problem_too_big, (VehicleDocuments.MAX_BYTES / (1024 * 1024)).toInt().asNumber())
        DocumentProblem.UNREADABLE -> AppText.res.getString(R.string.document_problem_unreadable)
        DocumentProblem.NOT_A_DOCUMENT -> AppText.res.getString(R.string.document_problem_not_a_document)
        DocumentProblem.FAILED -> AppText.res.getString(R.string.document_problem_failed)
    }

/** The line under an odometer field. */
fun OdometerNote.text(unit: DistanceUnit): String = when (this) {
    OdometerNote.Missing -> AppText.res.getString(R.string.odometer_note_missing, unit.pluralWord)
    is OdometerNote.LowerThanEarlier -> AppText.res.getString(R.string.odometer_note_lower, earlier.asNumber())
    is OdometerNote.Was -> AppText.res.getString(R.string.odometer_note_was, original.asNumber())
    OdometerNote.Unchanged -> AppText.res.getString(R.string.odometer_note_unchanged)
    is OdometerNote.LastRecorded -> AppText.res.getString(R.string.odometer_note_last_recorded, reading.asDistance(unit))
    is OdometerNote.HigherThanLatest -> AppText.res.getString(R.string.odometer_note_higher, latest.asDistance(unit))
    is OdometerNote.Latest -> AppText.res.getString(R.string.odometer_note_latest, reading.asDistance(unit))
}

/** "DIY", "Corner Garage · imported", "Entered as last done · just added". */
val Who.text: String
    get() {
        val res = AppText.res
        var who = when (val by = by) {
            DoneBy.EnteredAsLastDone -> res.getString(R.string.last_done_tag)
            DoneBy.Diy -> res.getString(R.string.who_diy)
            is DoneBy.Shop -> by.name ?: res.getString(R.string.who_shop)
        }
        if (imported) who = res.getString(R.string.who_imported, who)
        return when (mark) {
            RecordMark.ADDED -> res.getString(R.string.who_just_added, who)
            RecordMark.RESTORED -> res.getString(R.string.who_restored, who)
            null -> who
        }
    }

/** "2 photos"; null for none, so callers can leave the phrase out. */
fun photoCount(count: Int): String? = count.takeIf { it > 0 }?.let { AppText.res.getQuantityString(R.plurals.photos, it, it.asNumber()) }

/** "A", "A and B", "A and B and C": a list in a sentence. */
fun andList(items: List<String>): String = items.reduceOrNull { a, b -> AppText.res.getString(R.string.list_and, a, b) }.orEmpty()

/** "Fuel pump": NHTSA's component, or "Recall" when it named none. */
fun recallComponent(component: String): String = Recalls.componentLabel(component) ?: AppText.res.getString(R.string.recall)

/** The line a completed recall adds to the service log, saved as it reads then: "Recall 20V-682 · Fuel pump". */
fun recallServiceLine(notice: RecallNotice): String =
    AppText.res.getString(R.string.recall_service_line, Recalls.campaignLabel(notice.campaign), recallComponent(notice.component))

/**
 * A whole number or amount as the user typed it, put the way the parsers in `:domain` read it: the
 * app's grouping mark dropped and its decimal mark made a point. Nothing changes in English.
 */
fun String.typedNumber(): String {
    val symbols = DecimalFormatSymbols.getInstance(AppText.locale)
    return filterNot { it == symbols.groupingSeparator }.replace(symbols.decimalSeparator, '.')
}

/** A figure with decimals as typed, its decimal mark made a point; a comma is left for the parser, which takes it as one too. */
fun String.typedDecimal(): String = replace(DecimalFormatSymbols.getInstance(AppText.locale).decimalSeparator, '.')

/** A figure with decimals to edit, "6.2" or "5", with the app's decimal mark and no grouping, which [typedDecimal] reads back. */
fun Double.editableDecimal(): String =
    java.math.BigDecimal.valueOf(this).stripTrailingZeros().toPlainString().replace('.', DecimalFormatSymbols.getInstance(AppText.locale).decimalSeparator)

/** A cost to edit, "45.99", with the app's decimal mark and no grouping, which [typedNumber] reads back. */
fun editableCost(cents: Long): String =
    "${cents / 100}${DecimalFormatSymbols.getInstance(AppText.locale).decimalSeparator}${(cents % 100).toString().padStart(2, '0')}"
