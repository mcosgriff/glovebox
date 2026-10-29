package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.domain.FilledFigure
import io.cosgriff.glovebox.domain.FilledJob
import io.cosgriff.glovebox.domain.InvoiceFill
import io.cosgriff.glovebox.domain.JobPrice
import io.cosgriff.glovebox.domain.ShopUse
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Read an invoice (#484) fills Log service: made-up figures, through the form's own state. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class InvoiceFormFillTest {
    private fun job(wording: String, labor: Long?, parts: Long?, filed: String? = null, laborUnread: Boolean = false) =
        FilledJob(wording, filed, emptyList(), labor, parts, laborUnread, partsUnread = false)

    private fun fill(
        jobs: List<FilledJob> = listOf(job("Front Wheel Alignment", 12_995, 0)),
        unread: Set<FilledFigure> = emptySet(),
        odometer: Int? = 62_410,
        total: Long? = 21_072,
    ) = InvoiceFill(
        date = LocalDate.of(2026, 6, 2),
        odometer = odometer,
        shop = "main street auto",
        totalCents = total,
        jobs = jobs,
        recommendations = emptyList(),
        odometerIn = null,
        vinMismatch = null,
        unread = unread,
        leftOut = emptyList(),
    )

    private fun form() = Fixtures.logService().apply { shops = listOf(ShopUse("Main Street Auto", LocalDate.of(2026, 3, 1), 1)) }

    @Test
    fun `the visit fills from the invoice, the shop by its saved spelling`() {
        val form = form()
        form.fill(fill(), notes = "Recommended: coolant flush")
        assertEquals(LocalDate.of(2026, 6, 2), form.date)
        assertEquals(62_410, form.odometer)
        assertFalse(form.diy)
        assertEquals("Main Street Auto", form.shopName)
        assertEquals(21_072L, form.costCents)
        assertEquals("Recommended: coolant flush", form.notes)
        assertTrue(form.invoiceRead)
    }

    @Test
    fun `a service is a custom line of its wording, priced, beside what was ticked`() {
        val form = form()
        form.fill(fill(), notes = "")
        assertEquals(listOf("Front Wheel Alignment"), form.customItems)
        // The oil change ticked from its reminder stays, unpriced.
        assertTrue("oil" in form.selected)
        val alignment = form.jobs.single { it.lines == listOf(customKey("Front Wheel Alignment")) }
        assertEquals(JobPrice(12_995, 0), form.price(alignment))
        assertEquals(JobPrice(null, null), form.price(form.jobs.single { it.lines == listOf("oil") }))
        assertTrue(form.pricesShown)
    }

    @Test
    fun `a service the user's rule files ticks that line and keeps the shop's wording`() {
        val form = form()
        form.fill(fill(listOf(job("Rotate & Balance", 4_000, null, filed = "rotation"))), notes = "")
        assertTrue("rotation" in form.selected)
        assertEquals(emptyList<String>(), form.customItems)
        assertEquals("Rotate & Balance", form.lineName("rotation"))
        assertEquals(JobPrice(4_000, null), form.price(form.jobs.single { it.lines == listOf("rotation") }))
    }

    @Test
    fun `a figure that didn't read is outlined until something is typed in it`() {
        val form = form()
        form.fill(fill(listOf(job("Front Wheel Alignment", null, 0, laborUnread = true)), unread = setOf(FilledFigure.TOTAL), total = null), notes = "")
        val alignment = form.jobs.single { it.lines == listOf(customKey("Front Wheel Alignment")) }
        assertTrue(form.laborUnread(alignment))
        assertFalse(form.partsUnread(alignment))
        assertTrue(form.costUnread)
        assertFalse(form.odometerUnread)
        form.setLabor(alignment.id, "129.95")
        form.costText = "210.72"
        assertFalse(form.laborUnread(form.jobs.single { it.id == alignment.id }))
        assertFalse(form.costUnread)
    }

    @Test
    fun `filing a priced custom line keeps its prices`() {
        val form = form()
        form.fill(fill(listOf(job("Rotate & Balance", 4_000, 1_500))), notes = "")
        val rotation = form.data!!.categories.flatMap { it.options }.first { it.id == "rotation" }
        form.fileCustom("Rotate & Balance", rotation)
        assertEquals(JobPrice(4_000, 1_500), form.price(form.jobs.single { it.lines == listOf("rotation") }))
    }

    @Test
    fun `two services filed under one line keep both prices, the second as a line of its own`() {
        val form = form()
        form.fill(fill(listOf(job("Rotate & Balance", 4_000, null, filed = "rotation"), job("Tire rotation, rear", 2_500, null, filed = "rotation"))), notes = "")
        assertEquals(JobPrice(4_000, null), form.price(form.jobs.single { it.lines == listOf("rotation") }))
        assertEquals(JobPrice(2_500, null), form.price(form.jobs.single { it.lines == listOf(customKey("Tire rotation, rear")) }))
    }

    @Test
    fun `a line the user already priced keeps their price, and the invoice's service is a line of its own`() {
        val form = form()
        form.setLabor(form.jobs.single { it.lines == listOf("oil") }.id, "50.00")
        form.fill(fill(listOf(job("Oil Service", 3_995, 2_850, filed = "oil"))), notes = "")
        assertEquals(JobPrice(5_000, null), form.price(form.jobs.single { it.lines == listOf("oil") }))
        assertEquals(JobPrice(3_995, 2_850), form.price(form.jobs.single { it.lines == listOf(customKey("Oil Service")) }))
    }

    @Test
    fun `a service with nowhere of its own to go is said to be left out`() {
        val form = form()
        form.addCustom("Front Wheel Alignment")
        form.setLabor(form.jobs.single { it.lines == listOf(customKey("Front Wheel Alignment")) }.id, "10.00")
        form.fill(fill(), notes = "")
        assertEquals(listOf("Front Wheel Alignment"), form.invoiceLeftOut)
        assertEquals(JobPrice(1_000, null), form.price(form.jobs.single { it.lines == listOf(customKey("Front Wheel Alignment")) }))
    }

    @Test
    fun `a line filed while its labor couldn't be read stays outlined`() {
        val form = form()
        form.fill(fill(listOf(job("Rotate & Balance", null, 0, laborUnread = true))), notes = "")
        val rotation = form.data!!.categories.flatMap { it.options }.first { it.id == "rotation" }
        form.fileCustom("Rotate & Balance", rotation)
        assertTrue(form.laborUnread(form.jobs.single { it.lines == listOf("rotation") }))
    }
}
