package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * An inspection report's findings and their colours (#572), on a made-up invoice laid out as a Shop-Ware health
 * report: each item a line with a round mark at its left, what was found indented under it, and any recommended work
 * further in with its price. Nothing here is a real shop, vehicle or invoice.
 */
class InspectionReportTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val red = 0xFC494D
    private val yellow = 0xFED037
    private val green = 0x5BB65B

    /** A pale ring drawn over every mark: grey, so no colour of its own. */
    private val ring = 0xD2E6E6

    private val item = 100
    private val found = 160
    private val work = 220

    private fun report(marks: Boolean = true) = invoiceWithMarks {
        page {
            row(item to "Cedar Hollow Auto & Truck")
            row(item to "SERVICES")
            numbered("1", 260 to "Brake Fluid Service", 1700 to "SUBTOTAL: $171.50")
            row(1700 to "Labor: $171.50")
            row(1700 to "GRAND TOTAL: $171.50")
            gap()
            row(item to "INSPECTION REPORT")
            row(item to "Vehicle Health Report")
            line(marks, green, item to "Horn")
            line(marks, red, item to "Axles (Driveshaft if equipped)")
            row(found to "Driver side axle boot torn")
            row(found to "Recommended replacement")
            row(work to "Replace CV Axle Assembly", 900 to "$574.20 +tax")
            row(1200 to "Written by Sam Okafor at 10/29/2026 08:16 PM")
            line(marks, red, item to "Lights")
            row(found to "Fog lights not working")
            row(work to "Electrical Diagnosis", 900 to "$189.00 +tax", 1300 to "Approved by phone, 1:44pm 10/29/26")
            line(marks, yellow, item to "Fluid Leaks (1-Seeping to 5-Dumping)")
            row(found to "Oil pan seeping")
            line(marks, green, item to "Wheels/Tires")
            row(found to "Front 10/32, rear 9/32")
            line(marks, yellow, item to "Battery")
            row(found to "Tested weak")
            row(work to "Battery Replacement")
            row(work + 20 to "$289.00 +tax")
            row(item to "Past Recommendations")
            row(item to "Wiper Blade Replacement", 900 to "$49.99 +tax")
        }
    }

    private fun InvoicePage.line(marked: Boolean, rgb: Int, vararg cells: Pair<Int, String>) = if (marked) marked(rgb, *cells) else row(*cells)

    private fun read(marks: Boolean = true): InvoiceReading {
        val (words, shapes) = report(marks)
        // Every mark wears a pale ring, as the owner's shop's reports draw them.
        val ringed = shapes + shapes.map { it.copy(rgb = ring) }
        return InvoiceReader.read(words, today, emptyList(), DistanceUnit.MILES, marks = ringed)
    }

    @Test
    fun `a mark reads by its hue, never its exact colour`() {
        assertEquals(FindingColour.RED, FindingColour.of(red))
        assertEquals(FindingColour.RED, FindingColour.of(0xB42318))
        assertEquals(FindingColour.AMBER, FindingColour.of(yellow))
        assertEquals(FindingColour.AMBER, FindingColour.of(0xF59E0B))
        assertEquals(FindingColour.GREEN, FindingColour.of(green))
        assertEquals(FindingColour.GREEN, FindingColour.of(0x0F766E))
        assertNull(FindingColour.of(ring))
        assertNull(FindingColour.of(0x000000))
        assertNull(FindingColour.of(0xFFFFFF))
        assertNull(FindingColour.of(0x2563EB))
    }

    @Test
    fun `work under a marked item carries the item, what was found and its colour`() {
        val r = read()
        val axle = r.recommendations.single { it.text == "Replace CV Axle Assembly" }
        assertEquals(57_420L, axle.cents)
        assertEquals("Axles", axle.area)
        assertEquals("Driver side axle boot torn. Recommended replacement", axle.finding)
        assertEquals(FindingColour.RED, axle.colour)
    }

    @Test
    fun `a red or yellow item with no work under it is a finding of its own, green ones and approved work are not`() {
        val r = read()
        assertEquals(listOf(InvoiceFinding(r.findings[0].line, "Fluid Leaks", "Oil pan seeping", FindingColour.AMBER)), r.findings)
        // Approved by phone: done on this visit, so neither it nor what it was for is open.
        assertTrue(r.recommendations.none { it.text == "Electrical Diagnosis" })
        assertTrue(r.findings.none { it.area == "Lights" || it.area == "Horn" || it.area == "Wheels/Tires" })
    }

    @Test
    fun `a title wrapped above its price is that price's work, under its item`() {
        val battery = read().recommendations.single { it.text == "Battery Replacement" }
        assertEquals(28_900L, battery.cents)
        assertEquals("Battery", battery.area)
        assertEquals(FindingColour.AMBER, battery.colour)
    }

    @Test
    fun `the report ends at the next heading, and what follows is read as before`() {
        val wipers = read().recommendations.single { it.text == "Wiper Blade Replacement" }
        assertNull(wipers.area)
        assertNull(wipers.colour)
    }

    @Test
    fun `the services are read as before, and the report is no job`() {
        assertEquals(listOf("Brake Fluid Service"), read().jobs.map { it.wording })
    }

    @Test
    fun `a report without marks gives its findings without a colour`() {
        val r = read(marks = false)
        assertTrue(r.findings.all { it.colour == null })
        assertEquals(listOf("Fluid Leaks" to "Oil pan seeping", "Wheels/Tires" to "Front 10/32, rear 9/32"), r.findings.map { it.area to it.text })
        assertNull(r.recommendations.single { it.text == "Replace CV Axle Assembly" }.colour)
    }
}
