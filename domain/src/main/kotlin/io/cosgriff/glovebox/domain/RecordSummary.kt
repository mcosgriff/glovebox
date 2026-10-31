package io.cosgriff.glovebox.domain

/**
 * A service record as everything that shows it reads it (#579): its lines, each in its [ServiceGroup], its priced
 * jobs with their part rows, its charges, and so the gap between the prices and its cost ([breakdown]). The Markdown
 * export, the PDF report and History's timeline all read this, so "which group is this line in" has one answer
 * ([ServiceGroup.of]). Each lays out only what its design shows of it.
 */
data class RecordSummary(
    val lines: List<SummaryLine>,
    /** The record's jobs (#481), in their order; [SummaryLine.job] points into them. */
    val jobs: List<ExportJob> = emptyList(),
    /** The total as typed. */
    val costCents: Long? = null,
    /** What the invoice charged for the whole visit (#502). */
    val charges: InvoiceCharges = InvoiceCharges.NONE,
) {
    /** The lines' labels, in their order. */
    val items: List<String> get() = lines.map { it.label }

    /** Each line's group, in the lines' order. */
    val groups: List<ServiceGroup> get() = lines.map { it.group }

    /** Each line's index in [jobs], in the lines' order; null for a line in no job. Empty when there are no jobs. */
    val jobOf: List<Int?> get() = if (jobs.isEmpty()) emptyList() else lines.map { it.job }

    /**
     * The labor and parts on its lines, its charges and the gap to its cost; null when nothing is priced, as on the
     * record's screen.
     */
    val breakdown: CostBreakdown? get() = JobPrices.breakdown(jobs.map { JobPrice(it.laborCents, it.partsCents) }, costCents, charges)

    /**
     * Line [index]'s own price, labor and parts together: only when its job is priced and covers that line alone.
     * A job of several lines gives none of them a price, since it is never split between them.
     */
    fun lineTotal(index: Int): Long? {
        val j = jobOf.getOrNull(index) ?: return null
        if (jobOf.count { it == j } != 1) return null
        return jobs.getOrNull(j)?.let { JobPrice(it.laborCents, it.partsCents).totalCents }
    }

    /**
     * Without its costs, as "Costs and notes" off leaves it: a job's wording is what the shop called the work, and a
     * part row's wording and quantity say what was fitted, so they stay; every price, the charges and the cost go.
     */
    fun withoutCosts(): RecordSummary = copy(
        jobs = jobs.map { j -> j.copy(laborCents = null, partsCents = null, rows = j.rows.map { it.copy(eachCents = null, totalCents = null) }) },
        costCents = null,
        charges = InvoiceCharges.NONE,
    )

    companion object {
        /** [record]'s summary; [itemGroups] is each of the vehicle's schedule items' group by id ([ServiceGroup.ofItem]). */
        fun of(record: ServiceRecord, itemGroups: Map<Long, ServiceGroup>): RecordSummary = RecordSummary(
            lines = record.lines.map { SummaryLine(it.label, ServiceGroup.of(it, itemGroups), it.job) },
            jobs = record.exportJobs(),
            costCents = record.costCents,
            charges = record.charges,
        )
    }
}

/** One of a record's lines: its label as written, its group, and its job's index in [RecordSummary.jobs] (null for none). */
data class SummaryLine(val label: String, val group: ServiceGroup = ServiceGroup.OTHER, val job: Int? = null)
