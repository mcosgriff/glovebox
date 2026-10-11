package io.cosgriff.glovebox.domain

import java.time.LocalDate

/** What a vehicle's reminders are worked out from. */
data class ReminderInputs(
    val items: List<TrackedItem>,
    val entries: List<ServiceEntry>,
    val updates: List<OdometerReading>,
    val lead: LeadTime = LeadTime(),
)

/** What saving a new odometer reading would do, shown before the user commits to it. */
data class MileagePreview(
    /** The highest reading so far; null for a vehicle with no mileage yet. */
    val last: OdometerReading?,
    /** New reading minus [last]; negative when it is lower. */
    val delta: Int?,
    /** Lower than an earlier reading: worth a warning, not a block. */
    val below: Boolean,
    /** The mileage-counted reminders closest to due once the reading is saved, most urgent first. */
    val impact: List<Pair<ItemStatus, DueHeadline.Miles>>,
)

object MileageUpdate {
    /** Longest odometer the keypad accepts: 9,999,999 mi. */
    const val MAX_DIGITS = 7

    fun preview(inputs: ReminderInputs, odometer: Int, today: LocalDate, limit: Int = 2): MileagePreview {
        val readings = ServiceLog.readings(inputs.entries, inputs.updates)
        val last = readings.maxWithOrNull(compareBy<OdometerReading> { it.odometer }.thenBy { it.date })
        val after = VehicleReminders.statuses(inputs.items, inputs.entries, inputs.updates + OdometerReading(today, odometer), today, inputs.lead)
        return MileagePreview(
            last = last,
            delta = last?.let { odometer - it.odometer },
            below = ServiceLog.isBelowPreviousReading(odometer, today, readings),
            impact = ReminderPresentation.comingUp(after).counting
                .mapNotNull { (status, progress) -> (progress.headline as? DueHeadline.Miles)?.let { status to it } }
                .take(limit),
        )
    }

    /** Keypad entry: digits only, no leading zeros, at most [MAX_DIGITS]. */
    fun press(digits: String, digit: Char): String = when {
        !digit.isDigit() -> digits
        digits.isEmpty() && digit == '0' -> digits
        digits.length >= MAX_DIGITS -> digits
        else -> digits + digit
    }
}
