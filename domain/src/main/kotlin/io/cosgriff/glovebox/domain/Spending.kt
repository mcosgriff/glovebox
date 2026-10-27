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

/**
 * Labor and parts (#481) over the [priced] of the [records] whose jobs have a price, for the line under History's
 * spending line (#485). It is not a split of the spending total: records logged without prices have none, and a
 * record's cost also holds tax and fees. [laborCents] or [partsCents] is null when no record entered one, which
 * is not zero.
 */
data class SpendingSplit(
    val laborCents: Long?,
    val partsCents: Long?,
    val priced: Int,
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

    /**
     * Labor and parts summed over the [records] with a priced job; null when none has one, and then nothing about
     * prices shows. The count is of all [records], the same ones the spending line covers.
     */
    fun <T> split(records: List<T>, jobs: (T) -> List<JobPrice>): SpendingSplit? {
        val priced = records.map { jobs(it).filter(JobPrice::priced) }.filter { it.isNotEmpty() }
        if (priced.isEmpty()) return null
        val all = priced.flatten()
        return SpendingSplit(
            laborCents = total(all.map { it.laborCents }),
            partsCents = total(all.map { it.partsCents }),
            priced = priced.size,
            records = records.size,
        )
    }
}
