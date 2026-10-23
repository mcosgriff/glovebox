package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * How one service with both a mileage and a time interval comes out under a rule — the worked
 * example on the Reminder rules screen. Both limits are always filled in, whichever the rule uses.
 */
data class RulePreview(
    val dueAtOdometer: Int,
    /** When the mileage limit is expected at the current driving pace; null without a pace. */
    val mileageDate: LocalDate?,
    /** Share of the mileage interval used, 0..1. */
    val milesFraction: Double,
    val dueByDate: LocalDate,
    /** Share of the time interval used, 0..1. */
    val timeFraction: Double,
    /** The limit that sets the reminder; null when that depends on a pace that isn't known yet. */
    val decidedBy: DecidedBy?,
    /** Expected odometer on [dueByDate]; null without a pace. */
    val odometerAtTimeDue: Int?,
    /** True when the rule lets a service run past one of its intervals. */
    val canExceedInterval: Boolean,
) {
    /** The date of the next reminder, where one can be given. */
    val nextDate: LocalDate?
        get() = when (decidedBy) {
            DecidedBy.MILEAGE -> mileageDate
            DecidedBy.TIME -> dueByDate
            null -> null
        }
}

object RulePreviews {

    /** [interval] must have both miles and months. */
    fun preview(
        interval: Interval,
        last: LastDone,
        rule: DueRule,
        currentOdometer: Int,
        today: LocalDate,
        milesPerDay: Double?,
    ): RulePreview {
        val miles = requireNotNull(interval.miles) { "The preview needs a mileage interval" }
        requireNotNull(interval.months) { "The preview needs a time interval" }
        val pace = milesPerDay?.takeIf { it > 0 }

        val next = interval.nextDueAfter(last)
        val dueAtOdometer = next.odometer!!
        val dueByDate = next.date!!
        val milesLeft = dueAtOdometer - currentOdometer
        val mileageDate = ReminderEngine.dateAtPace(milesLeft, today, pace)
        val totalDays = ChronoUnit.DAYS.between(last.date, dueByDate).coerceAtLeast(1)
        val daysLeft = ChronoUnit.DAYS.between(today, dueByDate)

        return RulePreview(
            dueAtOdometer = dueAtOdometer,
            mileageDate = mileageDate,
            milesFraction = (1.0 - milesLeft.toDouble() / miles).coerceIn(0.0, 1.0),
            dueByDate = dueByDate,
            timeFraction = (1.0 - daysLeft.toDouble() / totalDays).coerceIn(0.0, 1.0),
            decidedBy = when (rule) {
                DueRule.MILEAGE_ONLY -> DecidedBy.MILEAGE
                DueRule.TIME_ONLY -> DecidedBy.TIME
                DueRule.WHICHEVER_FIRST -> mileageDate?.let { if (it.isBefore(dueByDate)) DecidedBy.MILEAGE else DecidedBy.TIME }
                DueRule.WHICHEVER_LAST -> mileageDate?.let { if (it.isAfter(dueByDate)) DecidedBy.MILEAGE else DecidedBy.TIME }
            },
            odometerAtTimeDue = pace?.let { currentOdometer + (it * daysLeft.coerceAtLeast(0)).roundToInt() },
            canExceedInterval = rule == DueRule.WHICHEVER_LAST,
        )
    }

    /** "~2,080 mi/month" figure: rounded to the nearest 10 so it doesn't look more exact than it is. */
    fun milesPerMonth(milesPerDay: Double): Int = (milesPerDay * ReminderEngine.DAYS_A_YEAR / 12 / 10).roundToInt() * 10
}

object LeadTimeInput {
    const val MAX_MILES = 5_000
    const val MAX_DAYS = 90

    /** Whole numbers from zero (no early reminder) up to the caps; null if either isn't one. */
    fun parse(miles: String, days: String): LeadTime? {
        val m = number(miles)?.takeIf { it <= MAX_MILES } ?: return null
        val d = number(days)?.takeIf { it <= MAX_DAYS } ?: return null
        return LeadTime(m, d)
    }

    private fun number(text: String): Int? = WholeNumber.parse(text, min = 0, maxDigits = 6)
}
