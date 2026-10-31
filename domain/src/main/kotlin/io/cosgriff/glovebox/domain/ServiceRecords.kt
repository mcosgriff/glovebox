package io.cosgriff.glovebox.domain

import java.time.LocalDate

/**
 * A service record as one piece (#574): what Log service, the record's screen, exports and imports read and write,
 * in place of rebuilding it from the saved rows each time. `:data` turns it into rows and back (`toModel`,
 * `toRows`), and owns the numbering the rows point at each other by.
 *
 * Everything points by place, never by a row's id: a line at its job by its index in [jobs], a part row at its line
 * by its index in [lines]. [lines] are in the record's order and [jobs] in theirs.
 */
data class ServiceRecord(
    /** 0 for a record not saved yet. */
    val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    /** As the dash read that day, in the vehicle's own unit. */
    val odometer: Int,
    val diy: Boolean,
    val shopName: String? = null,
    /** The total as typed; never worked out from, or overwritten by, the prices. */
    val costCents: Long? = null,
    val notes: String? = null,
    /** Given as a bare "last done", not logged when the work was done. */
    val enteredAsLastDone: Boolean = false,
    /** Came from an import file (or was replaced by one). */
    val imported: Boolean = false,
    /** The visit it is linked into (#307); null for one in none. */
    val visitId: Long? = null,
    val lines: List<RecordLine> = emptyList(),
    /** Its priced jobs (#481), in their order; empty when nothing was priced. */
    val jobs: List<RecordJob> = emptyList(),
    /** What the invoice charged for the whole visit (#502). */
    val charges: InvoiceCharges = InvoiceCharges.NONE,
) {
    /** Each job's prices, in [jobs]' order: what breakdowns and spending add up. */
    val jobPrices: List<JobPrice> get() = jobs.map { it.price }

    /** Each line's index in [jobs], in [lines]' order; null for a line in no job. */
    val jobOf: List<Int?> get() = lines.map { it.job }

    /** The indexes in [lines] of job [job]'s lines, in their order. */
    fun linesOf(job: Int): List<Int> = lines.indices.filter { lines[it].job == job }

    /**
     * The work as the record lists it, in its lines' order: a line in no job on its own, and a job where its first
     * line is, with all its lines. A line pointing at a job the record doesn't have stands alone.
     */
    val work: List<WorkEntry>
        get() {
            val placed = mutableSetOf<Int>()
            return lines.indices.mapNotNull { i ->
                val job = lines[i].job?.takeIf { it in jobs.indices } ?: return@mapNotNull WorkEntry(null, listOf(i))
                if (placed.add(job)) WorkEntry(job, linesOf(job)) else null
            }
        }

    /** The label of the line part row [row] is for; null for a row for the whole job. */
    fun lineOf(row: RecordPartRow): String? = row.line?.let(lines::getOrNull)?.label

    /**
     * Without the jobs that say nothing: a job is kept when it has a price, more than one line, the shop's own
     * wording, or itemized parts. A line alone and unpriced is a line as it always was, in no job.
     */
    fun withoutBareJobs(): ServiceRecord {
        val kept = jobs.indices.filter { j -> jobs[j].let { it.price.priced || it.wording != null || it.itemized } || linesOf(j).size > 1 }
        val index = kept.withIndex().associate { (i, j) -> j to i }
        return copy(lines = lines.map { it.copy(job = it.job?.let(index::get)) }, jobs = kept.map(jobs::get))
    }

    /**
     * [edit] saved in this record's place: this record's id, and what the form doesn't show carried over. Correcting
     * a bare "last done" doesn't make it a service logged at the time, correcting an imported record doesn't make it
     * one logged here, and an edit leaves it in whatever visit it was linked into.
     */
    fun editedAs(edit: ServiceRecord): ServiceRecord =
        edit.copy(id = id, enteredAsLastDone = enteredAsLastDone, imported = imported, visitId = visitId)

    /** As an import compares it with a file's record. */
    fun toSaved(): SavedRecord = SavedRecord(
        id = id,
        date = date,
        odometer = odometer,
        labels = lines.map { it.label },
        itemIds = lines.mapNotNull { it.scheduleItemId }.toSet(),
        diy = diy,
        shopName = shopName,
        costCents = costCents,
        notes = notes,
        enteredAsLastDone = enteredAsLastDone,
        jobs = jobs.mapIndexed { j, job ->
            JobSummary(
                job.wording, job.laborCents, job.partsCents, linesOf(j).map { lines[it].label },
                job.rows.map { PartSummary(it.wording, it.quantityMilli, it.eachCents, it.totalCents, lineOf(it)) },
            )
        },
        charges = charges,
    )

    /** Its jobs as an export writes them, in their order; [jobOf] points into them. */
    fun exportJobs(): List<ExportJob> = jobs.map { job ->
        ExportJob(job.wording, job.laborCents, job.price.partsCents, job.rows.map { ExportPartRow(it.wording, it.quantityMilli, it.eachCents, it.totalCents, lineOf(it)) })
    }
}

/**
 * One line of work on a record. [catalogId] is the catalog line it is filed under and [scheduleItemId] the reminder
 * it resets; both null for a custom line. [job] is its job's index in [ServiceRecord.jobs]; null for a line in none.
 */
data class RecordLine(
    val label: String,
    val catalogId: String? = null,
    val scheduleItemId: Long? = null,
    val job: Int? = null,
)

/**
 * A job as the shop priced it (#481): one labor and one parts figure for its lines, never split between them. Null
 * is "not entered", not zero. [partsCents] is the figure as stored, and a job with [rows] stores none: its parts
 * are their totals, worked out on every read ([price]).
 */
data class RecordJob(
    /** The shop's name for the job, as typed or read; null names it by its lines. */
    val wording: String? = null,
    val laborCents: Long? = null,
    val partsCents: Long? = null,
    /** The invoice's part rows (#502), in their order. */
    val rows: List<RecordPartRow> = emptyList(),
    /**
     * Its parts are rows, even none: the form's "Itemize parts". Only the form can have it with no rows; a saved
     * job with none reads back as one parts figure.
     */
    val itemized: Boolean = rows.isNotEmpty(),
) {
    /** Its prices as typed; with part rows, its parts are their totals added up. */
    val price: JobPrice
        get() = JobPrice(laborCents, if (rows.isEmpty()) partsCents else PartRows.partsCents(rows.map { it.amounts }))
}

/**
 * One row of a job's parts as the invoice prints it (#502), as typed: nothing is filled in. [line] is the index in
 * [ServiceRecord.lines] of the line it is for, a label only; null for the whole job.
 */
data class RecordPartRow(
    val wording: String? = null,
    /** In thousandths, so a decimal quantity reads back exactly as typed. */
    val quantityMilli: Long? = null,
    val eachCents: Long? = null,
    val totalCents: Long? = null,
    val line: Int? = null,
) {
    val amounts: PartAmounts get() = PartAmounts(quantityMilli, eachCents, totalCents)
}

/** One entry of [ServiceRecord.work]: [job] is the index in its jobs, null for a line alone; [lines] index its lines. */
data class WorkEntry(val job: Int?, val lines: List<Int>)
