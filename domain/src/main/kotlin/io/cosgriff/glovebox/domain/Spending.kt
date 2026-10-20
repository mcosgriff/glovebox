package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * What a vehicle's records cost, as History's line says it. Only records with a cost are added up:
 * [counted] of the [records] had one. [thisYearCents] is null when no costed record is in today's
 * calendar year, or when every one is (then the total says it all). [since] is the month of the
 * earliest costed record.
 */
data class SpendingSummary(
    val totalCents: Long,
    val thisYearCents: Long?,
    val since: YearMonth,
    val counted: Int,
    val records: Int,
)

object Spending {
    /** The sum of the costs given, null when none is. The export's total is this same sum. */
    fun total(costs: List<Long?>): Long? = costs.filterNotNull().takeIf { it.isNotEmpty() }?.sum()

    /** Null when no record has a cost: then there is nothing to say. Nothing is estimated for the others. */
    fun <T> summary(records: List<T>, today: LocalDate, date: (T) -> LocalDate, cost: (T) -> Long?): SpendingSummary? {
        val costed = records.filter { cost(it) != null }
        val total = total(costed.map(cost)) ?: return null
        val (thisYear, before) = costed.partition { date(it).year == today.year }
        return SpendingSummary(
            totalCents = total,
            thisYearCents = total(thisYear.map(cost)).takeIf { before.isNotEmpty() },
            since = YearMonth.from(costed.minOf(date)),
            counted = costed.size,
            records = records.size,
        )
    }
}
