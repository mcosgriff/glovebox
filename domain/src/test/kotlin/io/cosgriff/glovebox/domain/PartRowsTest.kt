package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PartRowsTest {
    private val parse: (PartField, String) -> Long? = { f, t -> if (f == PartField.QUANTITY) PartQuantity.parseMilli(t) else Money.parseCents(t) }
    private val format: (PartField, Long) -> String = { f, v ->
        if (f == PartField.QUANTITY) (v / 1000.0).toString().removeSuffix(".0") else "%d.%02d".format(v / 100, v % 100)
    }

    private fun type(row: PartRowDraft<String>, field: PartField, text: String) = PartRowDrafts.type(row, field, text, parse, format)

    @Test
    fun `two figures fill the third when it comes out exact`() {
        assertEquals(PartCheck.Fills(PartField.TOTAL, 5_000), PartRows.check(PartAmounts(4_000, 1_250, null)))
        assertEquals(PartCheck.Fills(PartField.EACH, 9_250), PartRows.check(PartAmounts(4_000, null, 37_000)))
        assertEquals(PartCheck.Fills(PartField.QUANTITY, 4_000), PartRows.check(PartAmounts(null, 9_250, 37_000)))
    }

    @Test
    fun `a figure that isn't exact is left blank, never rounded`() {
        // $20.00 for 3: each would be $6.666…
        assertEquals(PartCheck.LeftBlank(PartField.EACH), PartRows.check(PartAmounts(3_000, null, 2_000)))
        // 5.7 qt at $8.99 is $51.243.
        assertEquals(PartCheck.LeftBlank(PartField.TOTAL), PartRows.check(PartAmounts(5_700, 899, null)))
        // $10.00 at $3.00 each is 3.333… of them.
        assertEquals(PartCheck.LeftBlank(PartField.QUANTITY), PartRows.check(PartAmounts(null, 300, 1_000)))
    }

    @Test
    fun `a decimal quantity works out when the cents do`() {
        // 4.5 qt at $8.00 is $36.00, and $36.00 at $8.00 is 4.5.
        assertEquals(PartCheck.Fills(PartField.TOTAL, 3_600), PartRows.check(PartAmounts(4_500, 800, null)))
        assertEquals(PartCheck.Fills(PartField.QUANTITY, 4_500), PartRows.check(PartAmounts(null, 800, 3_600)))
    }

    @Test
    fun `nothing divides by zero`() {
        assertEquals(PartCheck.LeftBlank(PartField.EACH), PartRows.check(PartAmounts(0, null, 1_000)))
        assertEquals(PartCheck.LeftBlank(PartField.QUANTITY), PartRows.check(PartAmounts(null, 0, 1_000)))
        assertEquals(PartCheck.Fills(PartField.TOTAL, 0), PartRows.check(PartAmounts(0, 1_000, null)))
    }

    @Test
    fun `three figures that disagree say what they make, and the total counts`() {
        // 2 × $7.50 is $15.00, not $14.00.
        assertEquals(PartCheck.Mismatch(1_500), PartRows.check(PartAmounts(2_000, 750, 1_400)))
        assertEquals(PartCheck.Agrees, PartRows.check(PartAmounts(2_000, 750, 1_500)))
        assertEquals(1_400L, PartRows.partsCents(listOf(PartAmounts(2_000, 750, 1_400))))
    }

    @Test
    fun `fewer than two figures work nothing out`() {
        assertEquals(PartCheck.Incomplete, PartRows.check(PartAmounts()))
        assertEquals(PartCheck.Incomplete, PartRows.check(PartAmounts(eachCents = 600)))
    }

    @Test
    fun `figures too big to multiply are not worked out`() {
        assertEquals(PartCheck.LeftBlank(PartField.TOTAL), PartRows.check(PartAmounts(Long.MAX_VALUE / 2, 999_999_999_999, null)))
    }

    @Test
    fun `parts come from the rows' totals, leaving out a row without one`() {
        val rows = listOf(PartAmounts(4_000, 1_250, 5_000), PartAmounts(4_000, 9_250, 37_000), PartAmounts(null, 600, null))
        assertEquals(42_000L, PartRows.partsCents(rows))
        assertEquals(1, PartRows.missingTotals(rows))
        // No quantity is assumed: $6.00 each with no quantity is no total.
        assertNull(PartRows.partsCents(listOf(PartAmounts(null, 600, null))))
        assertNull(PartRows.partsCents(emptyList()))
    }

    @Test
    fun `quantities parse to thousandths`() {
        assertEquals(4_000L, PartQuantity.parseMilli("4"))
        assertEquals(5_700L, PartQuantity.parseMilli("5.7"))
        assertEquals(1_250L, PartQuantity.parseMilli("1.25"))
        assertEquals(1_000_000L, PartQuantity.parseMilli("1,000"))
        assertNull(PartQuantity.parseMilli("1.2345"))
        assertNull(PartQuantity.parseMilli("-1"))
        assertNull(PartQuantity.parseMilli("four"))
        assertNull(PartQuantity.parseMilli(""))
    }

    @Test
    fun `typing the second figure fills in the third`() {
        val row = type(type(PartRowDraft(1), PartField.QUANTITY, "4"), PartField.EACH, "12.50")
        assertEquals("50.00", row.total)
        assertEquals(PartField.TOTAL, row.filled)
    }

    @Test
    fun `changing a typed figure works the filled one out again`() {
        val row = type(type(PartRowDraft<String>(1), PartField.QUANTITY, "4"), PartField.EACH, "12.50")
        val changed = type(row, PartField.QUANTITY, "2")
        assertEquals("25.00", changed.total)
        assertEquals(PartField.TOTAL, changed.filled)
    }

    @Test
    fun `clearing a typed figure takes the filled one with it`() {
        val row = type(type(PartRowDraft<String>(1), PartField.QUANTITY, "4"), PartField.EACH, "12.50")
        val cleared = type(row, PartField.EACH, "")
        assertEquals("", cleared.total)
        assertNull(cleared.filled)
    }

    @Test
    fun `typing over a filled figure makes it the user's own`() {
        val row = type(type(PartRowDraft<String>(1), PartField.QUANTITY, "4"), PartField.EACH, "12.50")
        val typed = type(row, PartField.TOTAL, "48.00")
        assertEquals("48.00", typed.total)
        assertNull(typed.filled)
        // Now all three are the user's, so changing one fills nothing in.
        assertEquals("48.00", type(typed, PartField.QUANTITY, "3").total)
    }

    @Test
    fun `an inexact figure stays blank on the form`() {
        val row = type(type(PartRowDraft<String>(1), PartField.QUANTITY, "3"), PartField.TOTAL, "20.00")
        assertEquals("", row.each)
        assertNull(row.filled)
    }

    @Test
    fun `text that isn't a figure is never written over`() {
        val row = type(type(PartRowDraft<String>(1, total = "abc"), PartField.QUANTITY, "4"), PartField.EACH, "12.50")
        assertEquals("abc", row.total)
        assertNull(row.filled)
    }

    @Test
    fun `itemizing keeps a typed parts figure as the first row's total`() {
        val job = JobDraft(1, listOf("plugs", "coils"), parts = "420.00")
        val itemized = PartRowDrafts.itemize(job, rowId = 7)
        assertEquals("", itemized.parts)
        assertEquals(listOf(PartRowDraft<String>(7, total = "420.00")), itemized.rows)
        // Already itemized: nothing changes.
        assertEquals(itemized, PartRowDrafts.itemize(itemized, rowId = 8))
    }

    @Test
    fun `removing the last row makes parts one figure again`() {
        val job = PartRowDrafts.add(PartRowDrafts.itemize(JobDraft(1, listOf("plugs")), rowId = 1), rowId = 2)
        assertEquals(listOf(1, 2), job.rows!!.map { it.id })
        val one = PartRowDrafts.remove(job, 1)
        assertEquals(listOf(2), one.rows!!.map { it.id })
        assertNull(PartRowDrafts.remove(one, 2).rows)
    }

    @Test
    fun `a row tagged to a line that is unticked is for the whole job`() {
        val job = JobDraft(1, listOf("plugs", "coils"), rows = listOf(PartRowDraft(1, line = "plugs"), PartRowDraft(2, line = "coils")))
        val synced = JobGroups.sync(listOf(job), listOf("plugs"))
        assertEquals(listOf("plugs", null), synced.single().rows!!.map { it.line })
    }

    @Test
    fun `editing touches only its row`() {
        val job = JobDraft(1, listOf("plugs"), rows = listOf(PartRowDraft(1), PartRowDraft(2)))
        val edited = PartRowDrafts.edit(job, 2) { it.copy(wording = "Coil boot") }
        assertEquals(listOf("", "Coil boot"), edited.rows!!.map { it.wording })
    }
}
