package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.LogDraft.Companion.customKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Log service's draft (#578) and what an invoice merges into it (#484): made-up figures, no Android. */
class LogDraftTest {
    /** Figures typed with a point, as an English form types them; the app's locale is the app's to test. */
    private val figures = object : TypedFigures {
        override fun cents(text: String) = text.takeIf { it.isNotBlank() }?.let(Money::parseCents)
        override fun milli(text: String) = text.takeIf { it.isNotBlank() }?.let(PartQuantity::parseMilli)
        override fun costText(cents: Long) = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
        override fun quantityText(milli: Long): String = BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()
    }

    private val oil = WorkOption("oil", scheduleItemId = 1)
    private val rotation = WorkOption("rotation", scheduleItemId = 2)
    private val hitch = WorkOption("schedule-7", "Grease hitch", 7)
    private val terms = LogTerms(
        listOf(WorkCategory("engine", listOf(oil)), WorkCategory("wheels", listOf(rotation)), WorkCategory(ServiceCatalog.YOUR_ITEMS, listOf(hitch))),
        // `:domain` names no catalog line; these stand in for the app's names.
        nameOf = { it.name ?: "Line ${it.id}" },
        figures = figures,
    )

    /** A new service opened from the oil change's reminder. */
    private val opened = LogDraft().toggle("oil")

    private fun job(wording: String, labor: Long?, parts: Long?, filed: String? = null, laborUnread: Boolean = false) =
        FilledJob(wording, filed, emptyList(), labor, parts, laborUnread, partsUnread = false)

    private fun fill(jobs: List<FilledJob> = listOf(job("Front Wheel Alignment", 12_995, 0)), leftOut: List<String> = emptyList(), date: LocalDate? = LocalDate.of(2026, 6, 2)) =
        InvoiceFill(
            date = date,
            odometer = 62_410,
            shop = "main street auto",
            totalCents = 21_072,
            jobs = jobs,
            recommendations = emptyList(),
            odometerIn = null,
            vinMismatch = null,
            unread = emptySet(),
            leftOut = leftOut,
        )

    private fun LogDraft.merged(fill: InvoiceFill = fill()) = fill(fill, terms)!!

    private fun LogDraft.jobOf(key: String) = jobs(terms).single { it.lines == listOf(key) }

    private fun LogDraft.priceOf(key: String) = price(jobOf(key), terms)

    @Test
    fun `a service is a custom line of its wording, priced, beside what was ticked`() {
        val merge = opened.merged()
        val d = merge.draft
        assertEquals(listOf("Front Wheel Alignment"), d.customItems)
        // The oil change ticked from its reminder stays, unpriced.
        assertTrue("oil" in d.selected)
        assertEquals(JobPrice(12_995, 0), d.priceOf(customKey("Front Wheel Alignment")))
        assertEquals(JobPrice(null, null), d.priceOf("oil"))
        assertTrue(merge.opensPrices)
        assertTrue(d.invoiceRead)
    }

    @Test
    fun `fill twice and the second changes nothing`() {
        val once = opened.merged().draft
        assertNull(once.fill(fill(listOf(job("Brake Inspection", 4_400, null))), terms))
        assertEquals(listOf("Front Wheel Alignment"), once.customItems)
    }

    @Test
    fun `a service the user's rule files ticks that line and keeps the shop's wording`() {
        val d = opened.merged(fill(listOf(job("Rotate & Balance", 4_000, null, filed = "rotation")))).draft
        assertTrue("rotation" in d.selected)
        assertEquals(emptyList<String>(), d.customItems)
        assertEquals("Rotate & Balance", d.lineName("rotation", terms))
        assertEquals(JobPrice(4_000, null), d.priceOf("rotation"))
    }

    @Test
    fun `the shop's wording that is the line's own name isn't kept as anything else`() {
        val d = opened.merged(fill(listOf(job("Line rotation", 4_000, null, filed = "rotation")))).draft
        assertEquals(emptyMap<String, String>(), d.keptLabels)
    }

    @Test
    fun `nothing priced keeps the prices closed`() {
        assertFalse(opened.merged(fill(listOf(job("Front Wheel Alignment", null, null)))).opensPrices)
        assertTrue(opened.merged(fill(listOf(job("Front Wheel Alignment", null, null, laborUnread = true)))).opensPrices)
    }

    @Test
    fun `a figure that didn't read is outlined until something is typed in it`() {
        val d = opened.merged(fill(listOf(job("Front Wheel Alignment", null, 0, laborUnread = true)))).draft
        val alignment = d.jobOf(customKey("Front Wheel Alignment"))
        assertTrue(d.laborUnread(alignment))
        assertFalse(d.partsUnread(alignment))
        val typed = d.setLabor(alignment.id, "129.95", terms)
        assertFalse(typed.laborUnread(typed.jobs(terms).single { it.id == alignment.id }))
    }

    @Test
    fun `filing a priced custom line keeps its prices`() {
        val d = opened.merged(fill(listOf(job("Rotate & Balance", 4_000, 1_500)))).draft.fileCustom("Rotate & Balance", rotation, terms)
        assertEquals(JobPrice(4_000, 1_500), d.priceOf("rotation"))
        assertEquals("Rotate & Balance", d.lineName("rotation", terms))
    }

    @Test
    fun `a line filed while its labor couldn't be read stays outlined`() {
        val d = opened.merged(fill(listOf(job("Rotate & Balance", null, 0, laborUnread = true)))).draft.fileCustom("Rotate & Balance", rotation, terms)
        assertTrue(d.laborUnread(d.jobOf("rotation")))
    }

    @Test
    fun `two services filed under one line keep both prices, the second as a line of its own`() {
        val d = opened.merged(fill(listOf(job("Rotate & Balance", 4_000, null, filed = "rotation"), job("Tire rotation, rear", 2_500, null, filed = "rotation")))).draft
        assertEquals(JobPrice(4_000, null), d.priceOf("rotation"))
        assertEquals(JobPrice(2_500, null), d.priceOf(customKey("Tire rotation, rear")))
    }

    @Test
    fun `a line already priced keeps its price, and the invoice's service is a line of its own`() {
        val priced = opened.setLabor(opened.jobOf("oil").id, "50.00", terms)
        val d = priced.merged(fill(listOf(job("Oil Service", 3_995, 2_850, filed = "oil")))).draft
        assertEquals(JobPrice(5_000, null), d.priceOf("oil"))
        assertEquals(JobPrice(3_995, 2_850), d.priceOf(customKey("Oil Service")))
    }

    @Test
    fun `a line already priced with another isn't taken either`() {
        val both = opened.toggle("rotation")
        val joined = both.join(both.jobOf("rotation").id, both.jobOf("oil").id, terms)
        val d = joined.merged(fill(listOf(job("Rotate & Balance", 4_000, null, filed = "rotation")))).draft
        assertEquals(listOf("oil", "rotation"), d.jobs(terms).single { it.lines.size > 1 }.lines)
        assertEquals(JobPrice(4_000, null), d.priceOf(customKey("Rotate & Balance")))
    }

    @Test
    fun `an unpriced line ticked already takes the service's prices`() {
        val d = opened.merged(fill(listOf(job("Oil Service", 3_995, 2_850, filed = "oil")))).draft
        assertEquals(JobPrice(3_995, 2_850), d.priceOf("oil"))
        assertEquals(emptyList<String>(), d.customItems)
        // Ticked before, it keeps its own name.
        assertEquals("Line oil", d.lineName("oil", terms))
    }

    @Test
    fun `duplicate wording left out, said so and never dropped quietly`() {
        val typed = opened.addCustom("Front Wheel Alignment")
        val priced = typed.setLabor(typed.jobOf(customKey("Front Wheel Alignment")).id, "10.00", terms)
        // One the reader left out already, and one the form has no room for.
        val merge = priced.merged(fill(leftOut = listOf("Front wheel alignment")))
        assertEquals(listOf("Front wheel alignment", "Front Wheel Alignment"), merge.leftOut)
        assertEquals(JobPrice(1_000, null), merge.draft.priceOf(customKey("Front Wheel Alignment")))
    }

    @Test
    fun `the same wording twice on one invoice is one line`() {
        val merge = opened.merged(fill(listOf(job("Brake Inspection", 4_400, null), job("Brake Inspection", 1_000, null))))
        assertEquals(listOf("Brake Inspection"), merge.draft.customItems)
        assertEquals(JobPrice(4_400, null), merge.draft.priceOf(customKey("Brake Inspection")))
        assertEquals(listOf("Brake Inspection"), merge.leftOut)
    }

    @Test
    fun `the visit's date is never after today, and the shop takes a saved spelling`() {
        val today = LocalDate.of(2026, 6, 1)
        assertNull(fill().dateOn(today))
        assertEquals(LocalDate.of(2026, 6, 2), fill().dateOn(LocalDate.of(2026, 6, 2)))
        assertEquals("Main Street Auto", fill().shopAmong(listOf(ShopUse("Main Street Auto", today, 1))))
        assertEquals("main street auto", fill().shopAmong(emptyList()))
    }

    @Test
    fun `a record opens as a draft and saves as it was`() {
        val record = ServiceRecord(
            id = 12,
            vehicleId = 3,
            date = LocalDate.of(2026, 5, 1),
            odometer = 61_000,
            diy = false,
            shopName = "Main Street Auto",
            costCents = 12_000,
            lines = listOf(
                RecordLine("Oil and filter changed", "oil", 1, job = 0),
                RecordLine("Line rotation", "rotation", 2, job = 0),
                RecordLine("Grease hitch", null, 7),
                RecordLine("Wiper fluid"),
            ),
            jobs = listOf(RecordJob(laborCents = 4_000, rows = listOf(RecordPartRow("Filter", 1_000, 1_250, 1_250, line = 0), RecordPartRow(totalCents = 3_000)))),
        )
        val draft = LogDraft.of(record, mapOf(7L to hitch), terms)
        assertEquals(setOf("oil", "rotation", "schedule-7"), draft.selected)
        assertEquals(listOf("Wiper fluid"), draft.customItems)
        assertEquals(mapOf("oil" to "Oil and filter changed"), draft.keptLabels)
        val job = draft.jobs(terms).first()
        assertEquals(listOf("oil", "rotation"), job.lines)
        assertEquals("40.00", job.labor)
        assertEquals(listOf("1", "12.50", "12.50", "oil"), job.rows!!.first().let { listOf(it.quantity, it.each, it.total, it.line) })
        assertEquals(3, draft.nextRowId)

        val visit = record.copy(id = 0, lines = emptyList(), jobs = emptyList())
        val saved = draft.toRecord(visit, terms)
        assertEquals(
            listOf(
                RecordLine("Oil and filter changed", "oil", 1, job = 0),
                RecordLine("Line rotation", "rotation", 2, job = 0),
                // The user's own line is no catalog line.
                RecordLine("Grease hitch", null, 7),
                RecordLine("Wiper fluid"),
            ),
            saved.lines,
        )
        assertEquals(record.jobs, saved.jobs)
        assertEquals(JobPrice(4_000, 4_250), saved.jobPrices.single())
    }

    @Test
    fun `a job that says nothing isn't saved`() {
        val saved = opened.toggle("rotation").toRecord(ServiceRecord(vehicleId = 3, date = LocalDate.of(2026, 5, 1), odometer = 61_000, diy = true), terms)
        assertEquals(emptyList<RecordJob>(), saved.jobs)
        assertEquals(listOf(null, null), saved.lines.map { it.job })
    }

    @Test
    fun `figures that aren't amounts keep the draft from saving`() {
        val d = opened.setLabor(opened.jobOf("oil").id, "4o.00", terms)
        assertFalse(d.pricesValid(terms))
        assertEquals(JobPrice(null, null), d.priceOf("oil"))
        val itemized = opened.itemize(opened.jobOf("oil").id, terms)
        val row = itemized.jobOf("oil").rows!!.single()
        assertTrue(itemized.pricesValid(terms))
        assertFalse(itemized.typeRow(itemized.jobOf("oil").id, row.id, PartField.QUANTITY, "two", terms).pricesValid(terms))
    }
}
