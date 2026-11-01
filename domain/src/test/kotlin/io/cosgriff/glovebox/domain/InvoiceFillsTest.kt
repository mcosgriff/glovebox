package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** A made-up shop's invoice, read and turned into what fills Log service. Nothing here is a real shop or vehicle. */
class InvoiceFillsTest {
    private val today = LocalDate.of(2026, 10, 26)
    private val vin = madeUpVin("1HGFC2F5?KH512094")
    private val otherVin = madeUpVin("WVWHV7AJ?BW251846")

    private val left = 150
    private val mid = 900
    private val right = 1700

    private fun maple(
        labor1: String = "$39.95",
        odometerOut: String = "52342",
        grandTotal: String = "$210.72",
        secondWording: String = "Front Wheel Alignment",
    ) = invoice {
        page {
            row(left to "Maple Street Garage", right to "Invoice #20417")
            row(left to "12 Maple Street", right to "10/02/2026")
            gap()
            row(left to "VIN: $vin")
            row(mid to "Odometer In: 52340")
            row(mid to "Odometer Out: $odometerOut")
            gap()
            row(left to "SERVICES")
            numbered("1", 260 to "Engine Oil and Filter Service", right to "SUBTOTAL: $68.45")
            row(right to "Labor: $labor1")
            row(right to "Parts: $28.50")
            gap()
            numbered("2", 260 to secondWording, right to "SUBTOTAL: $129.95")
            row(right to "Labor: $129.95")
            row(right to "Parts: $0.00")
            gap()
            row(left to "Recommended Services")
            row(left to "Coolant Flush", right to "$159.99")
            gap()
            row(right to "Total Labor: $169.90")
            row(right to "Total Parts: $28.50")
            row(right to "Sales Tax: $12.32")
            row(right to "GRAND TOTAL: $grandTotal")
        }
    }

    private fun fill(words: List<InvoiceWord> = maple(), vehicleVin: String? = vin, rules: List<CatalogRule> = emptyList()) =
        InvoiceFills.of(InvoiceReader.read(words, today, emptyList(), DistanceUnit.MILES, rules), vehicleVin)

    @Test
    fun `an invoice fills the visit, its jobs and its total`() {
        val f = fill()
        assertEquals(LocalDate.of(2026, 10, 2), f.date)
        assertEquals(52_342, f.odometer)
        assertEquals("Maple Street Garage", f.shop)
        assertEquals(21_072L, f.totalCents)
        assertEquals(listOf("Engine Oil and Filter Service", "Front Wheel Alignment"), f.jobs.map { it.wording })
        assertEquals(listOf(3_995L, 12_995L), f.jobs.map { it.laborCents })
        assertEquals(listOf(2_850L, 0L), f.jobs.map { it.partsCents })
        assertEquals(emptySet<FilledFigure>(), f.unread)
        assertTrue(f.jobs.none { it.laborUnread || it.partsUnread })
    }

    @Test
    fun `recommended work and odometer in go to the notes, never to the jobs`() {
        val f = fill()
        assertEquals(listOf(RecommendedItem("Coolant Flush", 15_999)), f.recommendations)
        assertEquals(52_340, f.odometerIn)
        assertTrue(f.jobs.none { "Coolant" in it.wording })
    }

    @Test
    fun `a labor figure no sum agrees with is left blank and said to be unread`() {
        val f = fill(maple(labor1 = "$38.95"))
        assertNull(f.jobs[0].laborCents)
        assertTrue(f.jobs[0].laborUnread)
        // Its parts are still vouched by the totals block.
        assertEquals(2_850L, f.jobs[0].partsCents)
    }

    @Test
    fun `a grand total no sum agrees with is left blank and said to be unread`() {
        val f = fill(maple(grandTotal = "$270.72"))
        assertNull(f.totalCents)
        assertTrue(FilledFigure.TOTAL in f.unread)
    }

    @Test
    fun `an odometer that was read and not taken is said to be unread`() {
        val f = fill(maple(odometerOut = "52042"))
        assertNull(f.odometer)
        assertTrue(FilledFigure.ODOMETER in f.unread)
    }

    @Test
    fun `a VIN that isn't the vehicle's is asked about by its last four`() {
        assertNull(fill(vehicleVin = vin).vinMismatch)
        assertNull(fill(vehicleVin = vin.lowercase()).vinMismatch)
        assertEquals(VinMismatch(vin.takeLast(4), otherVin.takeLast(4)), fill(vehicleVin = otherVin).vinMismatch)
        // A vehicle with no VIN has nothing to compare.
        assertNull(fill(vehicleVin = null).vinMismatch)
        assertNull(fill(vehicleVin = "").vinMismatch)
    }

    @Test
    fun `the user's rule files a job, and a guess is only offered`() {
        val f = fill(rules = listOf(CatalogRule("front wheel alignment", "rotation")))
        assertEquals("rotation", f.jobs[1].filed)
        assertNull(f.jobs[0].filed)
        assertEquals(listOf("alignment"), fill().jobs[1].guesses)
    }

    @Test
    fun `a second job worded as an earlier one is left out and said so`() {
        val f = fill(maple(secondWording = "engine oil and filter service"))
        assertEquals(listOf("Engine Oil and Filter Service"), f.jobs.map { it.wording })
        assertEquals(listOf("engine oil and filter service"), f.leftOut)
    }

    @Test
    fun `each line says what it became`() {
        val reading = InvoiceReader.read(maple(labor1 = "$38.95"), today, emptyList(), DistanceUnit.MILES)
        val roles = InvoiceFills.lineRoles(reading)
        assertEquals(reading.lines.size, roles.size)
        fun at(text: String) = roles[reading.lines.indexOfFirst { text in it.text }]
        assertEquals(listOf(LineRole.Shop), at("Maple Street Garage"))
        assertTrue(LineRole.Date in at("10/02/2026"))
        assertEquals(listOf(LineRole.Vin(taken = true)), at("VIN:"))
        assertEquals(listOf(LineRole.Odometer(OdometerRole.OUT, 52_342, taken = true)), at("Odometer Out"))
        assertEquals(listOf(LineRole.Heading(InvoiceSection.SERVICES)), at("SERVICES"))
        assertTrue(LineRole.Work(0) in at("Engine Oil"))
        assertEquals(listOf(LineRole.Figure(InvoiceFigure.LABOR, 3_895, job = 0, taken = false)), at("Labor: $38.95"))
        assertEquals(listOf(LineRole.Figure(InvoiceFigure.LABOR, 12_995, job = 1, taken = true)), at("Labor: $129.95"))
        assertEquals(listOf(LineRole.Recommended), at("Coolant Flush"))
        // A totals-block figure only checks the others: neither taken nor left out.
        assertEquals(listOf(LineRole.Figure(InvoiceFigure.TAX, 1_232, job = null, taken = null)), at("Sales Tax"))
        assertEquals(emptyList<LineRole>(), at("12 Maple Street").filterNot { it == LineRole.Date })
    }
}
