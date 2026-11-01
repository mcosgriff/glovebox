package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ServiceRecordTest {
    private val day = LocalDate.of(2026, 3, 14)

    /** Made-up figures: oil and rotation priced as one job with its parts itemized, a wiper fluid alone and unpriced. */
    private val record = ServiceRecord(
        id = 7,
        vehicleId = 1,
        date = day,
        odometer = 53_000,
        diy = false,
        shopName = "Made-up Motors",
        costCents = 8_025,
        lines = listOf(
            RecordLine("Oil changed", "oil", 11, job = 0),
            RecordLine("Wiper fluid"),
            RecordLine("Tires rotated", "rotation", 12, job = 0),
        ),
        jobs = listOf(
            RecordJob(
                laborCents = 4_000,
                rows = listOf(
                    RecordPartRow("Synthetic oil", 4_500, 800, 3_600, line = 0),
                    RecordPartRow("Oil filter", eachCents = 1_150),
                    RecordPartRow(line = 2),
                ),
            ),
        ),
        charges = InvoiceCharges(shopSuppliesCents = 425),
    )

    @Test
    fun `a job's parts are its rows' totals, and a job without rows is its parts figure`() {
        assertEquals(JobPrice(4_000, 3_600), record.jobs.single().price)
        assertEquals(JobPrice(null, 900), RecordJob(partsCents = 900).price)
        // A parts figure beside rows counts for nothing: the rows are the parts.
        assertEquals(JobPrice(null, 100), RecordJob(partsCents = 900, rows = listOf(RecordPartRow(totalCents = 100))).price)
    }

    @Test
    fun `the work is in the lines' order, a job where its first line is`() {
        assertEquals(listOf(WorkEntry(0, listOf(0, 2)), WorkEntry(null, listOf(1))), record.work)
        assertEquals(listOf(0, null, 0), record.jobOf)
        assertEquals(listOf("Oil changed", null, "Tires rotated"), record.jobs.single().rows.map(record::lineOf))
    }

    @Test
    fun `a line pointing at a job the record doesn't have stands alone`() {
        val r = record.copy(lines = record.lines + RecordLine("Cabin filter", job = 4))
        assertEquals(WorkEntry(null, listOf(3)), r.work.last())
    }

    @Test
    fun `only a job that says something is kept, and its lines follow it`() {
        val bare = RecordJob()
        val r = ServiceRecord(
            vehicleId = 1, date = day, odometer = 1, diy = true,
            lines = listOf(RecordLine("A", job = 0), RecordLine("B", job = 1), RecordLine("C", job = 2), RecordLine("D", job = 2), RecordLine("E", job = 3), RecordLine("F", job = 4), RecordLine("G", job = 5)),
            jobs = listOf(bare, RecordJob(laborCents = 1), bare, RecordJob(wording = "Shop's words"), RecordJob(itemized = true), bare),
        ).withoutBareJobs()
        assertEquals(listOf(null, 0, 1, 1, 2, 3, null), r.jobOf)
        assertEquals(listOf(RecordJob(laborCents = 1), bare, RecordJob(wording = "Shop's words"), RecordJob(itemized = true)), r.jobs)
        assertTrue(r.jobs.last().itemized)
        assertFalse(bare.itemized)
    }

    @Test
    fun `an edit keeps the record's id, its bare last done, its import and its visit`() {
        val saved = record.copy(enteredAsLastDone = true, imported = true, visitId = 3)
        val edit = record.copy(id = 0, odometer = 53_100)
        val replaced = saved.editedAs(edit)
        assertEquals(saved.copy(odometer = 53_100), replaced)
        assertEquals(edit, record.copy(id = 0).editedAs(edit).copy(id = 0))
    }

    @Test
    fun `an import compares it by its stored figures and its rows' lines`() {
        val saved = record.toSaved()
        assertEquals(listOf("Oil changed", "Wiper fluid", "Tires rotated"), saved.labels)
        assertEquals(setOf(11L, 12L), saved.itemIds)
        assertEquals(InvoiceCharges(shopSuppliesCents = 425), saved.charges)
        val job = saved.jobs.single()
        assertEquals(listOf("Oil changed", "Tires rotated"), job.labels)
        // As stored: a job with rows has no parts figure of its own.
        assertNull(job.partsCents)
        assertEquals(listOf("Oil changed", null, "Tires rotated"), job.rows.map { it.line })
    }

    @Test
    fun `an export writes the parts worked out from the rows`() {
        val job = record.exportJobs().single()
        assertEquals(3_600L, job.partsCents)
        assertEquals(listOf("Synthetic oil", "Oil filter", null), job.rows.map { it.wording })
        assertEquals(listOf("Oil changed", null, "Tires rotated"), job.rows.map { it.line })
    }

    private val schedule = listOf(ScheduleRef(11, "Oil change", "oil", true), ScheduleRef(12, "Tire rotation", "rotation", true))

    @Test
    fun `an imported record is filed, and its rows point at the record's lines`() {
        val file = ImportedRecord(
            date = day, odometer = 53_000, diy = false, shopName = "Made-up Motors",
            work = listOf(ImportedWork("Wiper fluid"), ImportedWork("Oil changed", "oil", job = 0), ImportedWork("Tires rotated", "rotation", job = 0)),
            jobs = listOf(ImportedJob(laborCents = 4_000, rows = listOf(ImportedPartRow("Synthetic oil", totalCents = 3_600, line = 0), ImportedPartRow("Lug nut", line = 1), ImportedPartRow("Nothing", line = 5)))),
            charges = InvoiceCharges(taxCents = 300),
        )
        val r = HistoryImport.added(file, 1, schedule)
        assertTrue(r.imported)
        assertEquals(listOf(RecordLine("Wiper fluid"), RecordLine("Oil changed", "oil", 11, 0), RecordLine("Tires rotated", "rotation", 12, 0)), r.lines)
        assertEquals(listOf("Oil changed", "Tires rotated", null), r.jobs.single().rows.map(r::lineOf))
        assertEquals(InvoiceCharges(taxCents = 300), r.charges)
    }

    @Test
    fun `a file that says nothing about prices or charges keeps the saved ones on the same lines`() {
        val saved = record.copy(enteredAsLastDone = true, visitId = 3)
        // The same lines, in another order and other capitals.
        val file = ImportedRecord(
            date = day, odometer = 53_000, diy = true, costCents = 9_000,
            work = listOf(ImportedWork("tires rotated", "rotation"), ImportedWork("Oil changed", "oil"), ImportedWork("Wiper fluid")),
        )
        val r = HistoryImport.replacing(saved, file, schedule)
        assertEquals(7L, r.id)
        assertEquals(3L, r.visitId)
        assertTrue(r.imported)
        assertFalse(r.enteredAsLastDone)
        assertTrue(r.diy)
        assertEquals(9_000L, r.costCents)
        assertEquals(listOf("tires rotated", "Oil changed", "Wiper fluid"), r.lines.map { it.label })
        assertEquals(listOf(0, 0, null), r.jobOf)
        // Each part row still on the line it was for.
        assertEquals(listOf("Oil changed", null, "tires rotated"), r.jobs.single().rows.map(r::lineOf))
        assertEquals(saved.charges, r.charges)
    }

    @Test
    fun `a file with jobs or charges of its own replaces the saved ones`() {
        val file = ImportedRecord(
            date = day, odometer = 53_000, diy = false,
            work = listOf(ImportedWork("Oil changed", "oil", job = 0)),
            jobs = listOf(ImportedJob(laborCents = 5_000)),
            charges = InvoiceCharges(feesCents = 100),
        )
        val r = HistoryImport.replacing(record, file, schedule)
        assertEquals(listOf(RecordJob(laborCents = 5_000)), r.jobs)
        assertEquals(InvoiceCharges(feesCents = 100), r.charges)
        // A file with no charges keeps the saved ones even when its jobs are new.
        assertEquals(record.charges, HistoryImport.replacing(record, file.copy(charges = InvoiceCharges.NONE), schedule).charges)
    }
}
