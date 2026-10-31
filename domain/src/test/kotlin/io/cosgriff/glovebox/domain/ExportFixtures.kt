package io.cosgriff.glovebox.domain

/** A made-up record's summary: [labels] as its lines, each in [groups] and job [jobOf] at its place (none past their ends). */
fun summary(
    vararg labels: String,
    costCents: Long? = null,
    groups: List<ServiceGroup> = emptyList(),
    jobs: List<ExportJob> = emptyList(),
    jobOf: List<Int?> = emptyList(),
    charges: InvoiceCharges = InvoiceCharges.NONE,
): RecordSummary = RecordSummary(
    labels.mapIndexed { i, label -> SummaryLine(label, groups.getOrElse(i) { ServiceGroup.OTHER }, jobOf.getOrNull(i)) },
    jobs,
    costCents,
    charges,
)

/** This record with its cost as [cents]. */
fun ExportRecord.costing(cents: Long?): ExportRecord = copy(summary = summary.copy(costCents = cents))

/** This record with [jobs], its lines in them by [jobOf] (as they were when null). */
fun ExportRecord.withJobs(jobs: List<ExportJob>, jobOf: List<Int?>? = null): ExportRecord = copy(
    summary = summary.copy(
        jobs = jobs,
        lines = jobOf?.let { j -> summary.lines.mapIndexed { i, line -> line.copy(job = j.getOrNull(i)) } } ?: summary.lines,
    ),
)
