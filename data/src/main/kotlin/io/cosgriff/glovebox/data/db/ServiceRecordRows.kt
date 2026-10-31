package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.domain.InvoiceCharges
import io.cosgriff.glovebox.domain.JobPrice
import io.cosgriff.glovebox.domain.RecordJob
import io.cosgriff.glovebox.domain.RecordLine
import io.cosgriff.glovebox.domain.RecordPartRow
import io.cosgriff.glovebox.domain.ServiceRecord

/**
 * The saved rows as one [ServiceRecord] (#574): its lines in the rows' order, its jobs and each job's part rows by
 * their `position`, each pointing at the other by place rather than by id. The photos are left out.
 */
fun ServiceRecordWithItems.toModel(): ServiceRecord {
    val jobRows = jobs.sortedBy { it.position }
    val jobIndex = jobRows.withIndex().associate { (i, job) -> job.id to i }
    val lineIndex = items.withIndex().associate { (i, item) -> item.id to i }
    return ServiceRecord(
        id = record.id,
        vehicleId = record.vehicleId,
        date = record.date,
        odometer = record.odometer,
        diy = record.diy,
        shopName = record.shopName,
        costCents = record.costCents,
        notes = record.notes,
        enteredAsLastDone = record.enteredAsLastDone,
        imported = record.imported,
        visitId = record.visitId,
        lines = items.map { RecordLine(it.label, it.catalogId, it.scheduleItemId, it.jobId?.let(jobIndex::get)) },
        jobs = jobRows.map { job ->
            RecordJob(
                job.wording, job.laborCents, job.partsCents,
                parts.filter { it.jobId == job.id }.sortedBy { it.position }
                    .map { RecordPartRow(it.wording, it.quantityMilli, it.eachCents, it.totalCents, it.itemId?.let(lineIndex::get)) },
            )
        },
        charges = charges,
    )
}

/**
 * [this] as rows to write. Its jobs and lines are numbered from 1, only so the lines and part rows can point at them;
 * the write gives them ids of their own. Every job is written: which ones say enough to keep is the caller's
 * ([ServiceRecord.withoutBareJobs]).
 */
fun ServiceRecord.toRows(): RecordRows {
    val record = ServiceRecordEntity(
        id = id,
        vehicleId = vehicleId,
        date = date,
        odometer = odometer,
        diy = diy,
        shopName = shopName,
        costCents = costCents,
        notes = notes,
        enteredAsLastDone = enteredAsLastDone,
        imported = imported,
        visitId = visitId,
        shopSuppliesCents = charges.shopSuppliesCents,
        taxCents = charges.taxCents,
        feesCents = charges.feesCents,
    )
    val items = lines.mapIndexed { i, line ->
        ServiceRecordItemEntity(
            id = i + 1L, recordId = id, scheduleItemId = line.scheduleItemId, label = line.label, catalogId = line.catalogId,
            jobId = line.job?.let { it + 1L },
        )
    }
    val jobRows = jobs.mapIndexed { i, job ->
        ServiceRecordJobEntity(id = i + 1L, recordId = id, wording = job.wording, laborCents = job.laborCents, partsCents = job.partsCents, position = i)
    }
    val parts = jobs.flatMapIndexed { i, job ->
        job.rows.mapIndexed { k, row ->
            ServiceRecordJobPartEntity(
                jobId = i + 1L, wording = row.wording, quantityMilli = row.quantityMilli, eachCents = row.eachCents,
                totalCents = row.totalCents, itemId = row.line?.let { it + 1L }, position = k,
            )
        }
    }
    return RecordRows(record, items, jobRows, parts)
}

/** Every job's prices, in no particular order: what breakdowns and spending add up. */
val ServiceRecordWithItems.jobPrices: List<JobPrice> get() = toModel().jobPrices

/** What the invoice charged for the whole visit (#502). */
val ServiceRecordWithItems.charges: InvoiceCharges
    get() = InvoiceCharges(record.shopSuppliesCents, record.taxCents, record.feesCents)
