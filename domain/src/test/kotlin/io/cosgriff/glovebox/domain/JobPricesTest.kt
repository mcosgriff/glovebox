package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobPricesTest {
    private val plugsAndCoils = JobPrice(30_000, 42_000)
    private val axle = JobPrice(29_000, 26_000)
    private val diagnosis = JobPrice(18_000, null)

    @Test
    fun `a total above the line prices leaves tax and fees`() {
        val b = JobPrices.breakdown(listOf(plugsAndCoils, axle, diagnosis), 159_840)!!
        assertEquals(77_000L, b.laborCents)
        assertEquals(68_000L, b.partsCents)
        assertEquals(145_000L, b.linesCents)
        assertEquals(14_840L, b.gapCents)
        // As the board shows them: 48 %, 43 %, 9 %.
        assertEquals(CostBreakdown.Shares(48, 43, 9), b.shares)
    }

    @Test
    fun `a total below the line prices leaves a negative gap and no share for it`() {
        val b = JobPrices.breakdown(listOf(plugsAndCoils, axle, diagnosis), 142_500)!!
        assertEquals(-2_500L, b.gapCents)
        assertEquals(0, b.shares.gap)
        assertEquals(100, b.shares.labor + b.shares.parts)
    }

    @Test
    fun `a total equal to the line prices leaves no gap`() {
        assertEquals(0L, JobPrices.breakdown(listOf(axle), 55_000)!!.gapCents)
    }

    @Test
    fun `no total means no gap, and the total is never worked out`() {
        val b = JobPrices.breakdown(listOf(axle), null)!!
        assertNull(b.gapCents)
        assertNull(b.totalCents)
        assertEquals(55_000L, b.linesCents)
    }

    @Test
    fun `no prices means no breakdown`() {
        assertNull(JobPrices.breakdown(emptyList(), 10_000))
        assertNull(JobPrices.breakdown(listOf(JobPrice(null, null)), 10_000))
    }

    @Test
    fun `labor only and parts only`() {
        assertEquals(CostBreakdown(18_000, 0, 20_000), JobPrices.breakdown(listOf(diagnosis), 20_000))
        assertEquals(CostBreakdown(0, 4_500, 4_500), JobPrices.breakdown(listOf(JobPrice(null, 4_500)), 4_500))
    }

    @Test
    fun `zero is a price and null is not`() {
        assertEquals(true, JobPrice(0, null).priced)
        assertEquals(0L, JobPrice(0, null).totalCents)
        assertNull(JobPrice(null, null).totalCents)
    }

    @Test
    fun `a job of two lines is priced once`() {
        // Spark plugs and ignition coils, one labor and one parts figure for both: counted once, not per line.
        val b = JobPrices.breakdown(listOf(plugsAndCoils), 72_000)!!
        assertEquals(72_000L, b.linesCents)
        assertEquals(0L, b.gapCents)
    }

    @Test
    fun `shares always add up to 100`() {
        val b = CostBreakdown(1, 1, 3)
        assertEquals(100, b.shares.let { it.labor + it.parts + it.gap })
        assertEquals(CostBreakdown.Shares(0, 0, 0), CostBreakdown(0, 0, 0).shares)
    }

    @Test
    fun `the invoice's charges count against the total`() {
        val charges = InvoiceCharges(shopSuppliesCents = 3_850, taxCents = 5_210, feesCents = 1_200)
        val b = JobPrices.breakdown(listOf(plugsAndCoils, axle, diagnosis), 155_260, charges)!!
        assertEquals(145_000L, b.linesCents)
        assertEquals(155_260L, b.enteredCents)
        assertEquals(0L, b.gapCents)
        assertNull(b.gapKind)
    }

    @Test
    fun `what is left is tax and fees only while neither is entered`() {
        val jobs = listOf(plugsAndCoils, axle, diagnosis)
        assertEquals(GapKind.TAX_AND_FEES, JobPrices.breakdown(jobs, 159_840)!!.gapKind)
        assertEquals(GapKind.TAX_AND_FEES, JobPrices.breakdown(jobs, 159_840, InvoiceCharges(shopSuppliesCents = 3_850))!!.gapKind)
        assertEquals(GapKind.OTHER_CHARGES, JobPrices.breakdown(jobs, 159_840, InvoiceCharges(taxCents = 5_210))!!.gapKind)
        assertEquals(GapKind.OTHER_CHARGES, JobPrices.breakdown(jobs, 159_840, InvoiceCharges(feesCents = 1_200))!!.gapKind)
        assertEquals(GapKind.BELOW, JobPrices.breakdown(jobs, 142_500, InvoiceCharges(taxCents = 5_210))!!.gapKind)
    }

    @Test
    fun `a zero charge is entered`() {
        val b = JobPrices.breakdown(listOf(axle), 60_000, InvoiceCharges(taxCents = 0))!!
        assertEquals(GapKind.OTHER_CHARGES, b.gapKind)
    }

    @Test
    fun `charges alone make a breakdown, and no total still means no gap`() {
        val b = JobPrices.breakdown(emptyList(), null, InvoiceCharges(shopSuppliesCents = 1_500))!!
        assertEquals(1_500L, b.enteredCents)
        assertNull(b.gapCents)
        assertNull(b.gapKind)
        assertNull(JobPrices.breakdown(emptyList(), 10_000, InvoiceCharges.NONE))
    }

    @Test
    fun `shares cover every charge and add up to 100`() {
        val charges = InvoiceCharges(shopSuppliesCents = 3_850, taxCents = 5_210, feesCents = 1_200)
        val s = JobPrices.breakdown(listOf(plugsAndCoils, axle, diagnosis), 159_840, charges)!!.shares
        assertEquals(100, s.labor + s.parts + s.gap + s.shopSupplies + s.tax + s.fees)
        assertEquals(CostBreakdown.Shares(48, 43, 3, 2, 3, 1), s)
    }

    @Test
    fun `ticking lines gives each a job of its own, and unticking takes it out`() {
        val jobs = JobGroups.sync(emptyList(), listOf("plugs", "coils"))
        assertEquals(listOf(listOf("plugs"), listOf("coils")), jobs.map { it.lines })
        val joined = JobGroups.join(jobs, from = jobs[1].id, into = jobs[0].id)
        assertEquals(listOf(listOf("plugs", "coils")), joined.map { it.lines })
        val fewer = JobGroups.sync(joined, listOf("plugs", "axle"))
        assertEquals(listOf(listOf("plugs"), listOf("axle")), fewer.map { it.lines })
        assertEquals(emptyList<List<String>>(), JobGroups.sync(fewer, emptyList()).map { it.lines })
    }

    @Test
    fun `joining keeps the target's prices and wording`() {
        val jobs = listOf(
            JobDraft(1, listOf("plugs"), wording = "Replace spark plugs and ignition coils", labor = "300.00", parts = "420.00"),
            JobDraft(2, listOf("coils"), labor = "99"),
        )
        assertEquals(
            listOf(JobDraft(1, listOf("plugs", "coils"), "Replace spark plugs and ignition coils", "300.00", "420.00")),
            JobGroups.join(jobs, from = 2, into = 1),
        )
        // Unknown or the same job: nothing changes.
        assertEquals(jobs, JobGroups.join(jobs, from = 2, into = 2))
        assertEquals(jobs, JobGroups.join(jobs, from = 9, into = 1))
    }

    @Test
    fun `pricing separately gives each line its own unpriced job in place`() {
        val jobs = listOf(
            JobDraft(1, listOf("plugs", "coils"), wording = "Replace spark plugs and ignition coils", labor = "300.00"),
            JobDraft(2, listOf("axle"), labor = "290.00"),
        )
        val split = JobGroups.split(jobs, 1)
        assertEquals(listOf(listOf("plugs"), listOf("coils"), listOf("axle")), split.map { it.lines })
        assertEquals(listOf(null, null, null), split.map { it.wording })
        assertEquals(listOf("", "", "290.00"), split.map { it.labor })
        assertEquals(split.size, split.map { it.id }.toSet().size)
    }
}
