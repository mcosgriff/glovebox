package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.domain.InvoiceCharges
import io.cosgriff.glovebox.domain.JobPrice
import io.cosgriff.glovebox.domain.PartAmounts
import io.cosgriff.glovebox.domain.PartRows

/** [job]'s part rows, in their order (#502); empty when its parts are one figure. */
fun ServiceRecordWithItems.rowsOf(job: ServiceRecordJobEntity): List<ServiceRecordJobPartEntity> =
    parts.filter { it.jobId == job.id }.sortedBy { it.position }

/**
 * [job]'s prices as typed. With part rows, its parts are their totals added up, worked out on every read and never
 * stored (#502).
 */
fun ServiceRecordWithItems.priceOf(job: ServiceRecordJobEntity): JobPrice {
    val rows = rowsOf(job)
    if (rows.isEmpty()) return JobPrice(job.laborCents, job.partsCents)
    return JobPrice(job.laborCents, PartRows.partsCents(rows.map { PartAmounts(it.quantityMilli, it.eachCents, it.totalCents) }))
}

/** Every job's prices, in no particular order: what breakdowns and spending add up. */
val ServiceRecordWithItems.jobPrices: List<JobPrice> get() = jobs.map(::priceOf)

/** What the invoice charged for the whole visit (#502). */
val ServiceRecordWithItems.charges: InvoiceCharges
    get() = InvoiceCharges(record.shopSuppliesCents, record.taxCents, record.feesCents)
