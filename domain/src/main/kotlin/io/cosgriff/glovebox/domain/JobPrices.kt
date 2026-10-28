package io.cosgriff.glovebox.domain

/**
 * One job's labor and parts as the shop printed them (#481). Null is "not entered", which is not zero. A job
 * covers one or more of a record's lines and is never split between them.
 */
data class JobPrice(val laborCents: Long?, val partsCents: Long?) {
    val priced: Boolean get() = laborCents != null || partsCents != null

    /** Labor and parts together; null when neither was entered. */
    val totalCents: Long? get() = if (priced) (laborCents ?: 0) + (partsCents ?: 0) else null
}

/**
 * What an invoice prints for the whole visit rather than for a job (#502): shop supplies, tax, and fees and
 * sublet. Null is "not entered", which is not zero.
 */
data class InvoiceCharges(val shopSuppliesCents: Long? = null, val taxCents: Long? = null, val feesCents: Long? = null) {
    val entered: Boolean get() = shopSuppliesCents != null || taxCents != null || feesCents != null

    /** The charges entered, added up; zero when none is. */
    val sumCents: Long get() = (shopSuppliesCents ?: 0) + (taxCents ?: 0) + (feesCents ?: 0)

    companion object {
        val NONE = InvoiceCharges()
    }
}

/** What the gap between the total and the figures entered is called. No cause is ever named for one below. */
enum class GapKind {
    /** Above zero with neither tax nor fees entered. */
    TAX_AND_FEES,

    /** Above zero with tax or fees entered: what is left can't be called tax and fees. */
    OTHER_CHARGES,

    /** Below zero: a discount and a mistyped price look the same. */
    BELOW,
}

/**
 * A record's cost as its jobs' prices and the invoice's [charges] split it, worked out on every read and never
 * stored. [totalCents] is the record's own cost, exactly as typed; it is never filled in or corrected from the
 * prices.
 */
data class CostBreakdown(val laborCents: Long, val partsCents: Long, val totalCents: Long?, val charges: InvoiceCharges = InvoiceCharges.NONE) {
    /** What the line prices add up to. */
    val linesCents: Long get() = laborCents + partsCents

    /** What every figure entered adds up to: the line prices and the invoice's charges. */
    val enteredCents: Long get() = linesCents + charges.sumCents

    /**
     * The total less every figure entered: above zero is what the invoice charged besides them, below zero the
     * total falls short of them (no cause is named). Null without a total; zero is nothing.
     */
    val gapCents: Long? get() = totalCents?.minus(enteredCents)

    /** What [gapCents] is called; null when there is no gap. */
    val gapKind: GapKind?
        get() {
            val gap = gapCents ?: return null
            return when {
                gap < 0 -> GapKind.BELOW
                gap == 0L -> null
                charges.taxCents == null && charges.feesCents == null -> GapKind.TAX_AND_FEES
                else -> GapKind.OTHER_CHARGES
            }
        }

    /**
     * Whole percentages of each part of the bar, adding up to 100. A gap below zero has no share: then the
     * shares are of the figures entered.
     */
    val shares: Shares
        get() {
            val gap = gapCents?.coerceAtLeast(0) ?: 0
            val parts = listOf(laborCents, partsCents, gap, charges.shopSuppliesCents ?: 0, charges.taxCents ?: 0, charges.feesCents ?: 0)
            val base = parts.sum()
            if (base == 0L) return Shares(0, 0, 0)
            val exact = parts.map { it * 100.0 / base }
            val whole = exact.map { it.toInt() }.toMutableList()
            // Largest remainder: the points rounding lost go to the shares that lost most.
            exact.indices.sortedByDescending { exact[it] - whole[it] }.take(100 - whole.sum()).forEach { whole[it]++ }
            return Shares(whole[0], whole[1], whole[2], whole[3], whole[4], whole[5])
        }

    data class Shares(val labor: Int, val parts: Int, val gap: Int, val shopSupplies: Int = 0, val tax: Int = 0, val fees: Int = 0)
}

object JobPrices {
    /**
     * Null when no job has a price and no charge is entered: then nothing about breakdowns or gaps shows
     * anywhere.
     */
    fun breakdown(jobs: List<JobPrice>, totalCents: Long?, charges: InvoiceCharges = InvoiceCharges.NONE): CostBreakdown? {
        if (jobs.none { it.priced } && !charges.entered) return null
        return CostBreakdown(jobs.sumOf { it.laborCents ?: 0 }, jobs.sumOf { it.partsCents ?: 0 }, totalCents, charges)
    }
}

/**
 * One job on the Log service form (#481): the form's keys for its [lines], the shop's [wording] if it came with
 * one, and its prices as typed. Its parts are [parts], one figure, or with [rows] the invoice's part rows (#502),
 * whose totals make the parts figure. [id] is the form's own, not a row's.
 */
data class JobDraft<K>(
    val id: Int,
    val lines: List<K>,
    val wording: String? = null,
    val labor: String = "",
    val parts: String = "",
    val rows: List<PartRowDraft<K>>? = null,
)

/** How the form's lines are grouped into jobs (#481). Each line is in exactly one job; nothing is split. */
object JobGroups {
    /**
     * [jobs] brought in line with the ticked [lines]: a line no longer ticked leaves its job (and a part row tagged
     * to it is then for the whole job), a job left with no lines goes, and each newly ticked line gets a job of its
     * own at the end.
     */
    fun <K> sync(jobs: List<JobDraft<K>>, lines: List<K>): List<JobDraft<K>> {
        val ticked = lines.toSet()
        val kept = jobs.map { job ->
            val left = job.lines.filter(ticked::contains)
            job.copy(lines = left, rows = job.rows?.map { if (it.line != null && it.line !in left) it.copy(line = null) else it })
        }.filter { it.lines.isNotEmpty() }
        val placed = kept.flatMap { it.lines }.toSet()
        var next = nextId(jobs)
        return kept + lines.filterNot(placed::contains).distinct().map { JobDraft(next++, listOf(it)) }
    }

    /**
     * The one-line job [from] priced together with [into]: its line moves there, and [into] keeps its own wording
     * and prices. Whatever [from] had typed goes with it.
     */
    fun <K> join(jobs: List<JobDraft<K>>, from: Int, into: Int): List<JobDraft<K>> {
        val source = jobs.find { it.id == from } ?: return jobs
        if (from == into || jobs.none { it.id == into }) return jobs
        return jobs.mapNotNull {
            when (it.id) {
                from -> null
                into -> it.copy(lines = it.lines + source.lines)
                else -> it
            }
        }
    }

    /** "Price separately": each of [id]'s lines a job of its own, in its place, unpriced and without the wording. */
    fun <K> split(jobs: List<JobDraft<K>>, id: Int): List<JobDraft<K>> {
        var next = nextId(jobs)
        return jobs.flatMap { job -> if (job.id == id) job.lines.map { JobDraft(next++, listOf(it)) } else listOf(job) }
    }

    private fun nextId(jobs: List<JobDraft<*>>): Int = (jobs.maxOfOrNull { it.id } ?: 0) + 1
}
