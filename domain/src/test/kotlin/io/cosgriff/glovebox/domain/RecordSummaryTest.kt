package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** A record as the Markdown, the PDF report and the timeline all read it (#579). Made-up invoice. */
class RecordSummaryTest {
    /** The owner's own item, "Hitch service", filed by its service under suspension; and a typical oil change. */
    private val ownItem = 41L
    private val oilItem = 42L
    private val itemGroups = mapOf(ownItem to ServiceGroup.SUSPENSION, oilItem to ServiceGroup.OIL_ENGINE)

    private val record = ServiceRecord(
        vehicleId = 1, date = LocalDate.of(2026, 6, 14), odometer = 64_000, diy = false, shopName = "Corner Garage", costCents = 15_000,
        lines = listOf(
            RecordLine("Oil & filter change", catalogId = "oil", scheduleItemId = oilItem, job = 0),
            // Log service saves no catalog line for the owner's own items.
            RecordLine("Hitch service", catalogId = null, scheduleItemId = ownItem, job = 0),
            RecordLine("Air freshener"),
        ),
        jobs = listOf(
            RecordJob(
                "Oil service and hitch", laborCents = 6_000,
                rows = listOf(RecordPartRow("Oil filter", 1_000, 1_500, 1_500, line = 0), RecordPartRow("Grease", 2_000, null, 500)),
            ),
        ),
        charges = InvoiceCharges(taxCents = 600),
    )

    private val summary = RecordSummary.of(record, itemGroups)

    @Test
    fun `a line logged against the owner's own item is in that item's group, a custom line in Other`() {
        assertEquals(listOf(ServiceGroup.OIL_ENGINE, ServiceGroup.SUSPENSION, ServiceGroup.OTHER), summary.groups)
        assertEquals(ServiceGroup.SUSPENSION, ServiceGroup.of(record.lines[1], itemGroups))
        // Its catalog line decides before its item does.
        assertEquals(ServiceGroup.OIL_ENGINE, ServiceGroup.of(RecordLine("Oil", catalogId = "oil", scheduleItemId = ownItem), itemGroups))
        assertEquals(ServiceGroup.OTHER, ServiceGroup.of(RecordLine("Gone", scheduleItemId = 999), itemGroups))
    }

    @Test
    fun `it carries the job's part rows, each with the line it is for`() {
        assertEquals(listOf("Oil & filter change", "Hitch service", "Air freshener"), summary.items)
        assertEquals(listOf(0, 0, null), summary.jobOf)
        val job = summary.jobs.single()
        assertEquals(
            listOf(ExportPartRow("Oil filter", 1_000, 1_500, 1_500, "Oil & filter change"), ExportPartRow("Grease", 2_000, null, 500)),
            job.rows,
        )
        // Parts are the rows added up.
        assertEquals(2_000L, job.partsCents)
    }

    @Test
    fun `it works out the gap between the prices and the cost`() {
        val b = summary.breakdown!!
        assertEquals(6_000L, b.laborCents)
        assertEquals(2_000L, b.partsCents)
        assertEquals(600L, b.charges.taxCents)
        // 150.00 - 60.00 - 20.00 - 6.00 tax: 64.00 left, tax entered so not "tax and fees".
        assertEquals(6_400L, b.gapCents)
        assertEquals(GapKind.OTHER_CHARGES, b.gapKind)
        // A job of several lines gives none of them its price.
        assertNull(summary.lineTotal(0))
    }

    @Test
    fun `with costs off the wording and what was fitted stay, and every price goes`() {
        val off = summary.withoutCosts()
        assertNull(off.costCents)
        assertNull(off.breakdown)
        assertEquals(InvoiceCharges.NONE, off.charges)
        assertEquals("Oil service and hitch", off.jobs.single().wording)
        assertEquals(listOf(ExportPartRow("Oil filter", 1_000, null, null, "Oil & filter change"), ExportPartRow("Grease", 2_000, null, null)), off.jobs.single().rows)
        assertEquals(summary.groups, off.groups)
    }
}
