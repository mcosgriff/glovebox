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

/**
 * Read an invoice (#484) fills Log service: made-up figures, through the form's own state. What the merge decides is
 * `LogDraftTest` (`:domain`); this is only what needs the app's locale, its fields written and read.
 */
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
    fun `the draft's merge lands on the form, and a figure that didn't read is outlined until typed in`() {
        val form = form()
        form.fill(fill(listOf(job("Front Wheel Alignment", null, 0, laborUnread = true)), unread = setOf(FilledFigure.TOTAL), total = null), notes = "")
        assertEquals(listOf("Front Wheel Alignment"), form.customItems)
        assertTrue("oil" in form.selected)
        val alignment = form.jobs.single { it.lines == listOf(customKey("Front Wheel Alignment")) }
        assertTrue(form.laborUnread(alignment))
        assertTrue(form.pricesShown)
        assertTrue(form.costUnread)
        assertFalse(form.odometerUnread)
        form.setLabor(alignment.id, "129.95")
        form.costText = "210.72"
        assertEquals(JobPrice(12_995, 0), form.price(form.jobs.single { it.id == alignment.id }))
        assertFalse(form.costUnread)
        // It fills a form once.
        form.fill(fill(), notes = "Recommended: coolant flush")
        assertEquals("", form.notes)
    }
}
