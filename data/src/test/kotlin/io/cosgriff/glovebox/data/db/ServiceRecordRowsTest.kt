package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.domain.InvoiceCharges
import io.cosgriff.glovebox.domain.JobPrice
import io.cosgriff.glovebox.domain.RecordJob
import io.cosgriff.glovebox.domain.RecordLine
import io.cosgriff.glovebox.domain.RecordPartRow
import io.cosgriff.glovebox.domain.ServiceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Rows to the model and back, with no database (#574). Made-up figures throughout. */
class ServiceRecordRowsTest {
    private val record = ServiceRecordEntity(
        id = 7, vehicleId = 1, date = LocalDate.of(2026, 3, 14), odometer = 53_000, diy = false, shopName = "Made-up Motors",
        costCents = 8_025, notes = "Made-up notes", enteredAsLastDone = true, imported = true, visitId = 3, shopSuppliesCents = 425,
    )

    /** As Room hands them back: ids from the database, jobs and part rows in no particular order. */
    private val saved = ServiceRecordWithItems(
        record = record,
        items = listOf(
            ServiceRecordItemEntity(id = 40, recordId = 7, scheduleItemId = 11, label = "Oil changed", catalogId = "oil", jobId = 91),
            ServiceRecordItemEntity(id = 41, recordId = 7, scheduleItemId = null, label = "Wiper fluid"),
            ServiceRecordItemEntity(id = 42, recordId = 7, scheduleItemId = 12, label = "Tires rotated", catalogId = "rotation", jobId = 91),
            ServiceRecordItemEntity(id = 43, recordId = 7, scheduleItemId = null, label = "Cabin filter", jobId = 90),
        ),
        photos = emptyList(),
        jobs = listOf(
            ServiceRecordJobEntity(id = 91, recordId = 7, laborCents = 4_000, position = 1),
            ServiceRecordJobEntity(id = 90, recordId = 7, wording = "Filter", laborCents = 1_000, partsCents = 2_500, position = 0),
        ),
        parts = listOf(
            ServiceRecordJobPartEntity(id = 3, jobId = 91, wording = null, itemId = 42, position = 2),
            ServiceRecordJobPartEntity(id = 1, jobId = 91, wording = "Synthetic oil", quantityMilli = 4_500, eachCents = 800, totalCents = 3_600, itemId = 40, position = 0),
            ServiceRecordJobPartEntity(id = 2, jobId = 91, wording = "Oil filter", eachCents = 1_150, position = 1),
        ),
    )

    @Test
    fun `rows read as the record they are, jobs and part rows by position`() {
        val model = saved.toModel()
        assertEquals(
            ServiceRecord(
                id = 7, vehicleId = 1, date = record.date, odometer = 53_000, diy = false, shopName = "Made-up Motors", costCents = 8_025,
                notes = "Made-up notes", enteredAsLastDone = true, imported = true, visitId = 3,
                lines = listOf(
                    RecordLine("Oil changed", "oil", 11, job = 1),
                    RecordLine("Wiper fluid"),
                    RecordLine("Tires rotated", "rotation", 12, job = 1),
                    RecordLine("Cabin filter", job = 0),
                ),
                jobs = listOf(
                    RecordJob("Filter", 1_000, 2_500),
                    RecordJob(
                        laborCents = 4_000,
                        rows = listOf(RecordPartRow("Synthetic oil", 4_500, 800, 3_600, line = 0), RecordPartRow("Oil filter", eachCents = 1_150), RecordPartRow(line = 2)),
                    ),
                ),
                charges = InvoiceCharges(shopSuppliesCents = 425),
            ),
            model,
        )
        assertEquals(listOf(JobPrice(1_000, 2_500), JobPrice(4_000, 3_600)), model.jobPrices)
    }

    @Test
    fun `rows to the model and back are the same rows, renumbered`() {
        val rows = saved.toModel().toRows()
        assertEquals(record, rows.record)
        // The lines keep their order; their jobs and the part rows point at the new numbers.
        assertEquals(saved.items.map { it.copy(id = 0, jobId = null) }, rows.items.map { it.copy(id = 0, jobId = null) })
        assertEquals(listOf(1L, 2L, 3L, 4L), rows.items.map { it.id })
        assertEquals(listOf(2L, null, 2L, 1L), rows.items.map { it.jobId })
        assertEquals(
            listOf(
                ServiceRecordJobEntity(id = 1, recordId = 7, wording = "Filter", laborCents = 1_000, partsCents = 2_500, position = 0),
                ServiceRecordJobEntity(id = 2, recordId = 7, laborCents = 4_000, position = 1),
            ),
            rows.jobs,
        )
        assertEquals(
            listOf(
                ServiceRecordJobPartEntity(jobId = 2, wording = "Synthetic oil", quantityMilli = 4_500, eachCents = 800, totalCents = 3_600, itemId = 1, position = 0),
                ServiceRecordJobPartEntity(jobId = 2, wording = "Oil filter", eachCents = 1_150, position = 1),
                ServiceRecordJobPartEntity(jobId = 2, itemId = 3, position = 2),
            ),
            rows.parts,
        )
        // And read back once more, as the database would hand them over: the same record.
        val again = ServiceRecordWithItems(rows.record, rows.items, emptyList(), rows.jobs, rows.parts)
        assertEquals(saved.toModel(), again.toModel())
    }

    @Test
    fun `an edit opens on its part rows as typed, with nothing filled in`() {
        // The filter's row has an each and no quantity or total: it stays that way, and stays out of the parts figure.
        val job = saved.toModel().jobs[1]
        assertEquals(listOf(4_500L, null, null), job.rows.map { it.quantityMilli })
        assertEquals(listOf(3_600L, null, null), job.rows.map { it.totalCents })
        assertNull(job.partsCents)
        assertEquals(3_600L, job.price.partsCents)
    }

    @Test
    fun `a record with no jobs has lines in none`() {
        val plain = saved.copy(items = saved.items.map { it.copy(jobId = null) }, jobs = emptyList(), parts = emptyList())
        val model = plain.toModel()
        assertEquals(listOf(null, null, null, null), model.jobOf)
        assertEquals(emptyList<ServiceRecordJobEntity>(), model.toRows().jobs)
        assertEquals(listOf<Long?>(null, null, null, null), model.toRows().items.map { it.jobId })
    }
}
