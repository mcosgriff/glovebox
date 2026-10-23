package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Which interval decides when a service is due. */
enum class DueRule { WHICHEVER_FIRST, WHICHEVER_LAST, MILEAGE_ONLY, TIME_ONLY }

/** A repeat interval. Either part may be null (e.g. "every 2 years" has no mileage). */
data class Interval(val miles: Int?, val months: Int?) {
    init {
        require(miles != null || months != null) { "An interval needs miles, months, or both" }
        require(miles == null || miles > 0) { "miles must be positive" }
        require(months == null || months > 0) { "months must be positive" }
    }
}

/** When a service was last performed. Derived from the service log, never stored separately. */
data class LastDone(val date: LocalDate, val odometer: Int)

/** An odometer reading at a point in time — used to estimate driving pace. */
data class OdometerReading(val date: LocalDate, val odometer: Int)

/** How early to start nagging. */
data class LeadTime(val miles: Int = 500, val days: Int = 14)

enum class DueState { OK, DUE_SOON, OVERDUE }

/** Which limit produced the due date shown to the user. */
enum class DecidedBy { MILEAGE, TIME }

data class DueStatus(
    val state: DueState,
    /** Odometer the service is due at, if a mileage interval applies. */
    val dueAtOdometer: Int?,
    /** Calendar date the service is due by, if a time interval applies. */
    val dueByDate: LocalDate?,
    /** Positive = miles left, negative = miles overdue. Null when mileage doesn't apply. */
    val milesRemaining: Int?,
    /** Positive = days left, negative = days overdue. Null when time doesn't apply. */
    val daysRemaining: Long?,
    /** Best single estimate of the due date, using the driving pace for mileage. */
    val estimatedDueDate: LocalDate?,
    val decidedBy: DecidedBy,
) {
    /**
     * The date to give as an estimate beside a due point: only when the driving pace decides it, since a
     * date from time alone is already exact, and not once it is overdue, when it would be a day in the past.
     */
    val paceEstimate: LocalDate? get() = estimatedDueDate?.takeIf { decidedBy == DecidedBy.MILEAGE && state != DueState.OVERDUE }
}

object ReminderEngine {
    /** The average year, for paces given per year or per month. */
    const val DAYS_A_YEAR = 365.25

    /** When [milesLeft] runs out at [milesPerDay]: today if it already has, null with no pace to go by. */
    fun dateAtPace(milesLeft: Int, today: LocalDate, milesPerDay: Double?): LocalDate? = when {
        milesLeft <= 0 -> today
        milesPerDay != null && milesPerDay > 0 -> today.plusDays(ceil(milesLeft / milesPerDay).toLong())
        else -> null
    }

    /**
     * Status of one schedule item.
     *
     * [lastDone] null means it has never been logged; the item is then treated as due now,
     * which nudges the user to record when it was last done.
     */
    fun status(
        interval: Interval,
        rule: DueRule,
        lastDone: LastDone?,
        currentOdometer: Int,
        today: LocalDate,
        milesPerDay: Double?,
        lead: LeadTime = LeadTime(),
    ): DueStatus {
        if (lastDone == null) {
            return DueStatus(
                state = DueState.OVERDUE,
                dueAtOdometer = null, dueByDate = null,
                milesRemaining = null, daysRemaining = null,
                estimatedDueDate = today,
                decidedBy = if (interval.miles != null) DecidedBy.MILEAGE else DecidedBy.TIME,
            )
        }

        val useMiles = interval.miles != null && rule != DueRule.TIME_ONLY ||
            interval.months == null // fall back to miles if time isn't available
        val useTime = interval.months != null && rule != DueRule.MILEAGE_ONLY ||
            interval.miles == null

        val next = interval.nextDueAfter(lastDone)
        val dueAtOdo = next.odometer.takeIf { useMiles }
        val dueByDate = next.date.takeIf { useTime }

        val milesLeft = dueAtOdo?.let { it - currentOdometer }
        val daysLeft = dueByDate?.let { ChronoUnit.DAYS.between(today, it) }

        // Estimated calendar date the mileage limit is hit, at the current pace.
        val mileageDate: LocalDate? = milesLeft?.let { dateAtPace(it, today, milesPerDay) }

        val milesReached = milesLeft != null && milesLeft <= 0
        val timeReached = daysLeft != null && daysLeft <= 0
        val milesSoon = milesLeft != null && milesLeft <= lead.miles
        val timeSoon = daysLeft != null && daysLeft <= lead.days

        val bothApply = dueAtOdo != null && dueByDate != null
        val (overdue, soon) = when {
            !bothApply -> (milesReached || timeReached) to (milesSoon || timeSoon)
            rule == DueRule.WHICHEVER_LAST -> (milesReached && timeReached) to (milesSoon && timeSoon)
            else -> (milesReached || timeReached) to (milesSoon || timeSoon)
        }

        val (estimate, decidedBy) = pickEstimate(rule, mileageDate, dueByDate, dueAtOdo != null)

        return DueStatus(
            state = when {
                overdue -> DueState.OVERDUE
                soon -> DueState.DUE_SOON
                else -> DueState.OK
            },
            dueAtOdometer = dueAtOdo,
            dueByDate = dueByDate,
            milesRemaining = milesLeft,
            daysRemaining = daysLeft,
            estimatedDueDate = estimate,
            decidedBy = decidedBy,
        )
    }

    /**
     * Status of a job that has never been logged and is first due at an odometer reading still
     * ahead ([firstAt] > [currentOdometer]). By mileage only: a maker's "or 120 months" for the first
     * time counts from the day the vehicle went into service, which Glovebox doesn't know.
     */
    fun first(firstAt: Int, currentOdometer: Int, today: LocalDate, milesPerDay: Double?, lead: LeadTime = LeadTime()): DueStatus {
        val left = firstAt - currentOdometer
        return DueStatus(
            state = when {
                left <= 0 -> DueState.OVERDUE
                left <= lead.miles -> DueState.DUE_SOON
                else -> DueState.OK
            },
            dueAtOdometer = firstAt,
            dueByDate = null,
            milesRemaining = left,
            daysRemaining = null,
            estimatedDueDate = dateAtPace(left, today, milesPerDay),
            decidedBy = DecidedBy.MILEAGE,
        )
    }

    private fun pickEstimate(
        rule: DueRule,
        mileageDate: LocalDate?,
        timeDate: LocalDate?,
        hasMiles: Boolean,
    ): Pair<LocalDate?, DecidedBy> = when {
        mileageDate == null && timeDate == null -> null to if (hasMiles) DecidedBy.MILEAGE else DecidedBy.TIME
        mileageDate == null -> timeDate to DecidedBy.TIME
        timeDate == null -> mileageDate to DecidedBy.MILEAGE
        rule == DueRule.WHICHEVER_LAST ->
            if (mileageDate.isAfter(timeDate)) mileageDate to DecidedBy.MILEAGE else timeDate to DecidedBy.TIME
        else ->
            if (mileageDate.isBefore(timeDate)) mileageDate to DecidedBy.MILEAGE else timeDate to DecidedBy.TIME
    }

    /**
     * Average miles per day from odometer readings (service records + mileage updates).
     * Returns null until there are two readings at least a week apart.
     */
    fun milesPerDay(readings: List<OdometerReading>): Double? {
        if (readings.size < 2) return null
        // Several readings can share a day (a mileage update and a service); the highest one counts.
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.date }.thenBy { it.odometer })
        val first = sorted.first()
        val last = sorted.last()
        val days = ChronoUnit.DAYS.between(first.date, last.date)
        if (days < 7) return null
        val miles = last.odometer - first.odometer
        return if (miles > 0) miles.toDouble() / days else null
    }
}
