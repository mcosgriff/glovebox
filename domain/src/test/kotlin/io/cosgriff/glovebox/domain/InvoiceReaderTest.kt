package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Made-up invoices in two layouts: a Shop-Ware one, as #482 described the owner's shop's (numbered services with
 * their figures on the right, parts tables, a totals block beside the terms, an inspection report after), and a
 * dealer's repair order (a table of lines with labor, parts and total columns, recommended work before the
 * totals). Nothing here is a real shop, vehicle or invoice.
 */
class InvoiceReaderTest {
    private val today = LocalDate.of(2026, 10, 26)
    private val gti = madeUpVin("WVWHV7AJ?BW251846")
    private val civic = madeUpVin("1HGFC2F5?KH512094")

    private fun read(words: List<InvoiceWord>, readings: List<OdometerReading> = emptyList(), rules: List<CatalogRule> = emptyList()) =
        InvoiceReader.read(words, today, readings, DistanceUnit.MILES, rules)

    // ---- Shop-Ware -----------------------------------------------------------------------------------------------

    private val left = 150
    private val mid = 900
    private val right = 1700

    private fun InvoicePage.header(page: Int, of: Int) {
        row(left to "Cedar Hollow Auto & Truck", right to "Invoice #318204")
        row(left to "4471 Quarry Mill Road", right to "Page $page of $of")
        row(left to "Fernbrook, MI 49000", right to "03/18/2026 2:15 PM")
        row(left to "www.cedarhollowauto.example")
        row(left to "(555) 010-4477")
        row(left to "service@cedarhollowauto.example")
        gap()
    }

    /** The invoice, with any figure or field swapped for a misread one. */
    private fun shopWare(
        vin: String = "VIN: $gti",
        odometerIn: String = "129562",
        odometerOut: String = "129565",
        topTotal: String = "$1,039.97",
        subtotal1: String = "$739.96",
        labor4: String = "$129.95",
        fees2: String? = "$6.00",
    ) = invoice {
        page {
            header(1, 4)
            row(left to "Jordan Pell", mid to "2011 Volkswagen GTI")
            row(left to "88 Birchwood Ct", mid to vin)
            row(left to "Fernbrook, MI 49000", mid to "Engine: 2.0L L4 Turbo")
            row(left to "jpell@mail.example", mid to "Plate: KTR 4410")
            row(left to "(555) 010-2231", mid to "Fleet #:")
            row(mid to "Odometer In: $odometerIn")
            row(mid to "Odometer Out: $odometerOut")
            row(left to "Advisor: Dana Ruiz", mid to "Technician: Sam Okafor", right to "GRAND TOTAL: $topTotal")
            row(right to "BALANCE DUE: $1,039.97")
            gap()
            row(left to "Reason for Customer's Visit")
            row(left to "1. Check engine light flashing at idle, rough running")
            gap()
            row(left to "SERVICES")
            numbered("1", 260 to "Remove & Replace Spark Plugs and Ignition Coils", right to "SUBTOTAL: $subtotal1")
            row(right to "Labor: $260.00")
            row(left to "Found misfire codes on cylinders 2 and 3. Battery tested", right to "Parts: $479.96")
            row(left to "at 12.61 VOLTS, charging at 13.99 VOLTS.", right to "Fees: $0.00")
            row(right to "Sublet: $0.00")
            row(left to "LABOR")
            row(left to "Remove and replace spark plugs and ignition coils")
            row(left to "PARTS")
            row(mid to "QTY", 1100 to "EACH", 1350 to "TOTAL")
            row(left to "Spark Plug PFR7S8EG", mid to "4.0", 1100 to "$44.49", 1350 to "$177.96")
        }
        page {
            header(2, 4)
            // Service 1's parts run onto this page.
            row(left to "Ignition Coil 06H905110G", mid to "4.0", 1100 to "$75.50", 1350 to "$302.00")
            gap()
            numbered("2", 260 to "Engine Oil and Filter Service", right to "SUBTOTAL: $118.41")
            row(right to "Labor: $39.95")
            row(right to "Parts: $72.46")
            fees2?.let { row(right to "Fees: $it") }
            row(left to "LABOR", right to "Sublet: $0.00")
            row(left to "Drain engine oil, replace filter, refill")
            row(left to "PARTS")
            row(mid to "QTY", 1100 to "EACH", 1350 to "TOTAL")
            row(left to "Synthetic Oil 5W-40", mid to "5.5", 1100 to "$9.99", 1350 to "$54.95")
            row(left to "Oil Filter HU719/7X", mid to "1.0", 1100 to "$17.51", 1350 to "$17.51")
            gap()
            numbered("3", 260 to "Vehicle Health Report", right to "SUBTOTAL: $0.00")
            row(left to "Courtesy inspection completed, see report.", right to "Labor: $0.00")
            row(right to "Parts: $0.00")
            row(right to "Fees: $0.00")
            row(right to "Sublet: $0.00")
        }
        page {
            header(3, 4)
            numbered("4", 260 to "Front Wheel Alignment", right to "SUBTOTAL: $129.95")
            row(right to "Labor: $labor4")
            row(right to "Parts: $0.00")
            row(right to "Fees: $0.00")
            row(right to "Sublet: $0.00")
            row(left to "LABOR")
            row(left to "Set toe and camber to spec, 2 tie rods adjusted")
            gap()
            row(left to "Terms of Service", right to "GRAND TOTAL: $1,039.97")
            row(left to "1. Parts remain the property of the shop until paid.", right to "Total Labor: $429.90")
            row(left to "2. Storage of $25.00 per day after 3 days.", right to "Total Parts: $552.42")
            row(left to "3. Not responsible for items left in vehicle.", right to "Total Fees: $6.00")
            row(right to "Total Sublets: $0.00")
            row(right to "Shop Supplies: $18.50")
            row(right to "Sales Tax: $33.15")
            row(right to "BALANCE DUE: $1,039.97")
            gap()
            row(left to "Authorization:")
            row(left to "Jordan Pell 03/18/2026")
        }
        page {
            header(4, 4)
            row(left to "Vehicle Inspection Report")
            row(left to "Tires: left front 6/32, right front 6/32", right to "Good")
            row(left to "Front brake pads 4mm", right to "Needs attention")
            row(left to "Synthetic Oil Change", right to "$141.99 +tax")
            row(left to "Front Brake Pads and Rotors", right to "$489.00 +tax")
            row(left to "Past Recommendations")
            row(left to "Coolant Flush", right to "$159.99 +tax")
        }
    }

    @Test
    fun `a Shop-Ware invoice gives its visit, its jobs and its total`() {
        val r = read(shopWare(), listOf(OdometerReading(LocalDate.of(2025, 9, 2), 121_400)))
        assertEquals(LocalDate.of(2026, 3, 18), r.date)
        assertEquals("Cedar Hollow Auto & Truck", r.shop)
        assertEquals(gti, r.vin)
        assertEquals(129_562, r.odometerIn)
        assertEquals(129_565, r.odometerOut)
        assertEquals(129_565, r.odometer)
        assertEquals(129_562, r.odometerInForNotes)
        assertEquals(103_997L, r.grandTotalCents)

        assertEquals(
            listOf("Remove & Replace Spark Plugs and Ignition Coils", "Engine Oil and Filter Service", "Vehicle Health Report", "Front Wheel Alignment"),
            r.jobs.map { it.wording },
        )
        assertEquals(listOf(1, 2, 3, 4), r.jobs.map { it.number })
        assertEquals(listOf(JobPrice(26_000, 47_996), JobPrice(3_995, 7_246), JobPrice(0, 0), JobPrice(12_995, 0)), r.jobs.map { it.price })
        assertEquals(600L, r.jobs[1].feesCents)
        assertEquals(73_996L, r.jobs[0].subtotalCents)
        assertEquals("Remove and replace spark plugs and ignition coils", r.jobs[0].laborWording)
        // The description's own numbers ("13.99 VOLTS") are text, not figures.
        assertTrue(r.jobs[0].description!!.contains("13.99 VOLTS"))
    }

    @Test
    fun `a service can be more than one line, and every line is a guess`() {
        val r = read(shopWare())
        assertEquals(listOf("spark-plugs", "ignition-coils"), r.jobs[0].guesses)
        assertEquals(listOf("alignment"), r.jobs[3].guesses)
        // A report that says nothing of checking is no check line.
        assertEquals(emptyList<String>(), r.jobs[2].guesses)
        assertTrue(r.jobs.all { it.filed == null })
    }

    @Test
    fun `the user's rule files a job before any guess`() {
        val r = read(shopWare(), rules = listOf(CatalogRule("vehicle health report", "multi-point")))
        assertEquals("multi-point", r.jobs[2].filed)
        assertEquals(emptyList<String>(), r.jobs[2].guesses)
        assertTrue(r.trace.contains(InvoiceStep.JobFiled(2, "multi-point")))
    }

    @Test
    fun `the inspection report's priced work is offered for notes and never a job`() {
        val r = read(shopWare())
        assertEquals(
            listOf("Synthetic Oil Change" to 14_199L, "Front Brake Pads and Rotors" to 48_900L, "Coolant Flush" to 15_999L),
            r.recommendations.map { it.text to it.cents },
        )
        assertTrue(r.jobs.none { "Coolant" in it.wording || "Brake" in it.wording })
    }

    @Test
    fun `a service's large number set a little below its title is still that title's`() {
        val r = read(
            invoice {
                page {
                    header(1, 1)
                    row(left to "SERVICES")
                    numbered("1", 260 to "Front Wheel Alignment", drop = 0.6f)
                    row(right to "SUBTOTAL: $129.95")
                    row(right to "Labor: $129.95")
                    row(right to "Parts: $0.00")
                    numbered("2", 260 to "Fluid Leak Dye Test", drop = 0.6f)
                    row(right to "SUBTOTAL: $90.00")
                    row(right to "Labor: $90.00")
                    row(right to "Parts: $0.00")
                    row(left to "Terms of Service", right to "GRAND TOTAL: $219.95")
                    row(right to "Total Labor: $219.95")
                    row(right to "Total Parts: $0.00")
                }
            },
        )
        assertEquals(listOf(1, 2), r.jobs.map { it.number })
        assertEquals(listOf("Front Wheel Alignment", "Fluid Leak Dye Test"), r.jobs.map { it.wording })
        assertEquals(listOf(JobPrice(12_995, 0), JobPrice(9_000, 0)), r.jobs.map { it.price })
    }

    @Test
    fun `words printed large with nothing beside them are lines of their own`() {
        val r = read(
            invoice {
                page {
                    numbered("INVOICE")
                    numbered("PAID")
                    header(1, 1)
                }
            },
        )
        assertEquals(listOf("INVOICE", "PAID"), r.lines.take(2).map { it.text })
    }

    @Test
    fun `a recommendation's "+taxes and fees" is not its wording, even wrapped onto a line of its own`() {
        val r = read(
            invoice {
                page {
                    header(1, 1)
                    row(left to "SERVICES")
                    numbered("1", 260 to "Front Wheel Alignment", right to "SUBTOTAL: $129.95")
                    row(right to "Labor: $129.95")
                    row(right to "Parts: $0.00")
                    row(left to "Terms of Service", right to "GRAND TOTAL: $129.95")
                    row(right to "Total Labor: $129.95")
                    row(right to "Total Parts: $0.00")
                    row(left to "Past Recommendations")
                    row(left to "Brake Inspection", 1300 to "$58.33 +taxes and fees")
                    row(left to "Transmission Fluid Exchange Service", 1300 to "$404.99 +taxes")
                    row(1300 to "and fees")
                    row(left to "Coolant Flush", 1300 to "$159.99 plus tax & fees")
                    row(left to "Wiper blades and fees waived", 1300 to "$24.00")
                }
            },
        )
        assertEquals(
            listOf(
                "Brake Inspection" to 5_833L,
                "Transmission Fluid Exchange Service" to 40_499L,
                "Coolant Flush" to 15_999L,
                "Wiper blades and fees waived" to 2_400L,
            ),
            r.recommendations.map { it.text to it.cents },
        )
    }

    @Test
    fun `a service's parts header on one line with its columns is its parts table, not a table of work`() {
        val r = read(
            invoice {
                page {
                    header(1, 1)
                    row(left to "SERVICES")
                    numbered("1", 260 to "Replace Front Brake Pads & Rotors", right to "SUBTOTAL: $301.00")
                    row(left to "LABOR", right to "Labor: $120.00")
                    row(left to "Replace front brake pads and rotors", right to "Parts: $181.00")
                    row(right to "Fees: $0.00")
                    row(right to "Sublet: $0.00")
                    row(left to "PARTS", mid to "QTY", 1100 to "EACH", 1350 to "TOTAL")
                    row(left to "Disc Brake Pad Set", mid to "1.0", 1100 to "$61.00", 1350 to "$61.00")
                    row(left to "Disc Brake Rotor", mid to "2.0", 1100 to "$60.00", 1350 to "$120.00")
                    gap()
                    numbered("2", 260 to "Brake Fluid Service", right to "SUBTOTAL: $95.00")
                    row(left to "LABOR", right to "Labor: $80.00")
                    row(left to "Perform brake fluid service", right to "Parts: $15.00")
                    row(right to "Fees: $0.00")
                    row(right to "Sublet: $0.00")
                    row(left to "PARTS", mid to "QTY", 1100 to "EACH", 1350 to "TOTAL")
                    row(left to "Brake Fluid DOT 4", mid to "1.0", 1100 to "$15.00", 1350 to "$15.00")
                    gap()
                    row(left to "Terms of Service", right to "GRAND TOTAL: $396.00")
                    row(right to "Total Labor: $200.00")
                    row(right to "Total Parts: $196.00")
                }
            },
        )
        assertEquals(listOf("Replace Front Brake Pads & Rotors", "Brake Fluid Service"), r.jobs.map { it.wording })
        assertEquals(listOf(JobPrice(12_000, 18_100), JobPrice(8_000, 1_500)), r.jobs.map { it.price })
        assertEquals(listOf(6_100L, 12_000L, 1_500L), r.trace.filterIsInstance<InvoiceStep.PartRow>().map { it.totalCents })
    }

    @Test
    fun `the header every page repeats is skipped, and a service's parts join up across the page`() {
        val r = read(shopWare())
        val skipped = r.trace.filterIsInstance<InvoiceStep.RepeatedLineSkipped>().map { r.lines[it.line] }
        assertEquals(setOf(2, 3, 4), skipped.map { it.page }.toSet())
        assertTrue(skipped.none { it.page == 1 })
        assertEquals(18, skipped.size)
        val rows = r.trace.filterIsInstance<InvoiceStep.PartRow>().filter { it.job == 0 }
        assertEquals(listOf(17_796L, 30_200L), rows.map { it.totalCents })
        val parts = r.trace.filterIsInstance<InvoiceStep.SumChecked>().single { it.check == InvoiceCheck.JOB_PARTS && it.job == 0 }
        assertTrue(parts.agrees)
        assertEquals(setOf(1, 2), parts.lines.map { r.lines[it].page }.toSet())
    }

    @Test
    fun `terms, the authorization and the shop's storage price are not read`() {
        val r = read(shopWare())
        assertTrue(r.recommendations.none { "Storage" in it.text })
        assertTrue(r.trace.filterIsInstance<InvoiceStep.FigureRead>().none { it.cents == 2_500L })
    }

    @Test
    fun `a subtotal read a digit off is left out, and the sums still vouch for its labor and parts`() {
        // #482's real misread: $739.96 as $730.98.
        val r = read(shopWare(subtotal1 = "$730.98"))
        assertNull(r.jobs[0].subtotalCents)
        assertEquals(JobPrice(26_000, 47_996), r.jobs[0].price)
        assertTrue(r.trace.contains(InvoiceStep.FigureNotTaken(InvoiceFigure.SUBTOTAL, 0, 73_098, FigureLeftOut.NO_SUM_AGREES)))
        assertTrue(r.trace.any { it is InvoiceStep.SumChecked && it.check == InvoiceCheck.JOB_SUBTOTAL && it.job == 0 && !it.agrees && it.sumCents == 73_996L })
        assertEquals(103_997L, r.grandTotalCents)
    }

    @Test
    fun `a labor figure a digit off is left out, never corrected`() {
        val r = read(shopWare(labor4 = "$128.95"))
        assertNull(r.jobs[3].laborCents)
        assertEquals(0L, r.jobs[3].partsCents)
        // The other services' labor is vouched by their own subtotals.
        assertEquals(listOf(26_000L, 3_995L, 0L), r.jobs.take(3).map { it.laborCents })
        assertEquals(103_997L, r.grandTotalCents)
    }

    @Test
    fun `a misread grand total at the top is left out, and the one the totals block vouches for is taken`() {
        val r = read(shopWare(topTotal = "$1,039.37"))
        assertEquals(103_997L, r.grandTotalCents)
        assertTrue(r.trace.contains(InvoiceStep.FigureNotTaken(InvoiceFigure.GRAND_TOTAL, null, 103_937, FigureLeftOut.NO_SUM_AGREES)))
    }

    @Test
    fun `a line that wasn't read at all only costs the sums it is in`() {
        val r = read(shopWare(fees2 = null))
        // Service 2 has no fees line, so its own sum and the fees total don't add up; the subtotals still add up to
        // the grand total, and labor and parts are vouched by their totals.
        assertNull(r.jobs[1].feesCents)
        assertEquals(11_841L, r.jobs[1].subtotalCents)
        assertEquals(JobPrice(3_995, 7_246), r.jobs[1].price)
        assertEquals(103_997L, r.grandTotalCents)
    }

    @Test
    fun `a VIN with S read for 5 or 8 for B is folded back only where the check digit passes`() {
        val withS = gti.replaceFirst('5', 'S').also { check(it != gti) }
        val r = read(shopWare(vin = "VIN: $withS"))
        assertEquals(gti, r.vin)
        val step = r.trace.filterIsInstance<InvoiceStep.VinTaken>().single()
        assertEquals(listOf(VinFold(gti.indexOf('5') + 1, 'S', '5')), step.folds)

        val with8 = gti.replace("BW", "8W")
        assertEquals(gti, read(shopWare(vin = "VIN: $with8")).vin)
    }

    @Test
    fun `a VIN that fails its check digit is left out`() {
        val wrong = gti.replace("251846", "251847")
        val r = read(shopWare(vin = "VIN: $wrong"))
        assertNull(r.vin)
        assertTrue(r.trace.any { it is InvoiceStep.VinNotTaken })
    }

    @Test
    fun `the odometer follows Read dash's range, from the last reading before the invoice`() {
        val far = read(shopWare(), listOf(OdometerReading(LocalDate.of(2025, 1, 1), 131_000)))
        assertNull(far.odometer)
        assertTrue(far.trace.contains(InvoiceStep.OdometerNotTaken(far.trace.filterIsInstance<InvoiceStep.OdometerNotTaken>().first().line, OdometerRole.IN, 129_562, OdometerLeftOut.OutOfRange(131_000, 331_000))))
        // A reading after the invoice's date doesn't count against it: the invoice is entered late.
        val late = read(shopWare(), listOf(OdometerReading(LocalDate.of(2026, 9, 1), 135_000)))
        assertEquals(129_565, late.odometer)
    }

    @Test
    fun `out below in, or far past it, leaves both out`() {
        assertNull(read(shopWare(odometerOut = "129462")).odometer)
        assertNull(read(shopWare(odometerOut = "139565")).odometer)
        // A letter in the number is no number.
        val r = read(shopWare(odometerOut = "129S65"))
        assertEquals(129_562, r.odometer)
        assertNull(r.odometerInForNotes)
    }

    @Test
    fun `a date after today is left out`() {
        val words = shopWare().map { if (it.text == "03/18/2026") it.copy(text = "03/18/2027") else it }
        val r = read(words)
        assertNull(r.date)
        assertTrue(r.trace.any { it is InvoiceStep.DateNotTaken })
    }

    @Test
    fun `the same invoice from a PDF's text layer reads the same`() {
        val pdf = shopWare().map { it.copy(confidence = null) }
        val r = read(pdf)
        assertEquals(103_997L, r.grandTotalCents)
        assertEquals(4, r.jobs.size)
        assertEquals("Cedar Hollow Auto & Truck", r.shop)
    }

    @Test
    fun `words round-trip through JSON`() {
        val words = shopWare() + InvoiceWord("x", 9, 1f, 2f, 3f, 4f, null)
        assertEquals(words, InvoiceWords.fromJson(InvoiceWords.toJson(words)))
    }

    // ---- A dealer's repair order -----------------------------------------------------------------------------------

    private fun dealer(partsA: String = "28.40", total: String = "148.03") = invoice {
        page {
            row(60 to "Riverside Motors Service", 1500 to "REPAIR ORDER 88231")
            row(60 to "1200 Commerce Blvd", 1500 to "Date: 05/02/2025")
            row(60 to "Brookfield, OH 44000")
            gap()
            row(60 to "Customer: Avery Lind", 900 to "VIN $civic")
            row(60 to "Year/Make/Model: 2019 Honda Civic", 900 to "Mileage In: 61,204", 1600 to "Mileage Out: 61,206")
            gap()
            row(60 to "LINE DESCRIPTION", 1300 to "LABOR", 1600 to "PARTS", 1900 to "TOTAL")
            row(60 to "A Lube oil and filter", 1300 to "39.95", 1600 to partsA, 1900 to "68.35")
            row(60 to "B Rotate tires", 1300 to "24.95", 1600 to "0.00", 1900 to "24.95")
            row(60 to "C Replace cabin air filter", 1300 to "15.00", 1600 to "32.10", 1900 to "47.10")
            gap()
            row(60 to "RECOMMENDED")
            row(60 to "Front brake pads at 3mm", 1900 to "$289.00")
            row(60 to "Coolant exchange", 1900 to "$159.95")
            gap()
            row(1500 to "Total Labor", 1900 to "79.90")
            row(1500 to "Total Parts", 1900 to "60.50")
            row(1500 to "Shop Supplies", 1900 to "4.00")
            row(1500 to "Sales Tax", 1900 to "3.63")
            row(1500 to "INVOICE TOTAL", 1900 to total)
            gap()
            row(60 to "Thank you for your business!")
        }
    }

    @Test
    fun `a dealer's table of lines reads by its columns`() {
        val r = read(dealer())
        assertEquals(LocalDate.of(2025, 5, 2), r.date)
        assertEquals("Riverside Motors Service", r.shop)
        assertEquals(civic, r.vin)
        assertEquals(61_204, r.odometerIn)
        assertEquals(61_206, r.odometerOut)
        assertEquals(14_803L, r.grandTotalCents)
        assertEquals(listOf("Lube oil and filter", "Rotate tires", "Replace cabin air filter"), r.jobs.map { it.wording })
        assertEquals(listOf(JobPrice(3_995, 2_840), JobPrice(2_495, 0), JobPrice(1_500, 3_210)), r.jobs.map { it.price })
        assertEquals(listOf(listOf("oil"), listOf("rotation"), listOf("cabin-air")), r.jobs.map { it.guesses })
        assertEquals(listOf("Front brake pads at 3mm" to 28_900L, "Coolant exchange" to 15_995L), r.recommendations.map { it.text to it.cents })
    }

    @Test
    fun `a dealer's misread parts figure is left out`() {
        val r = read(dealer(partsA = "26.40"))
        assertNull(r.jobs[0].partsCents)
        assertEquals(3_995L, r.jobs[0].laborCents)
        assertEquals(14_803L, r.grandTotalCents)
    }

    @Test
    fun `a grand total no sum agrees with is left out`() {
        assertNull(read(dealer(total = "148.08")).grandTotalCents)
    }

    @Test
    fun `recommendations above the services are notes, and the services after them still read`() {
        val r = read(
            invoice {
                page {
                    row(60 to "Oakfield Service Co.")
                    row(60 to "RECOMMENDED")
                    row(60 to "Timing belt due soon", 1800 to "$650.00")
                    row(60 to "SERVICES")
                    numbered("1", 260 to "Tire Rotation", 1700 to "SUBTOTAL: $30.00")
                    row(1700 to "Labor: $30.00")
                    row(1700 to "GRAND TOTAL: $30.00")
                }
            },
        )
        assertEquals(listOf("Timing belt due soon"), r.recommendations.map { it.text })
        assertEquals(listOf("Tire Rotation"), r.jobs.map { it.wording })
        assertEquals(JobPrice(3_000, null), r.jobs.single().price)
        assertEquals(3_000L, r.grandTotalCents)
    }

    @Test
    fun `words with no services give no jobs and say so`() {
        val r = read(invoice { page { row(60 to "Riverside Motors"); row(60 to "Thank you") } })
        assertEquals(emptyList<InvoiceJob>(), r.jobs)
        assertTrue(r.trace.contains(InvoiceStep.NoServices))
    }
}
