package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.KeptRecommendation
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.domain.DoneChange
import io.cosgriff.glovebox.domain.KeptLine
import io.cosgriff.glovebox.domain.RecommendedChoice
import io.cosgriff.glovebox.domain.RecommendedItem
import io.cosgriff.glovebox.domain.ShopRecommendation
import io.cosgriff.glovebox.domain.WorkOption
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The form as plain state, without Hilt or a database: what it holds, and what saving it writes ([LogServiceForm.toSave]). */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class LogServiceFormTest {
    // One of the user's own lines: it carries its name, so nothing here needs the app's resources.
    private val hitch = WorkOption("own-hitch", "Grease hitch", 7)

    private fun form(vararg custom: String) = LogServiceForm(editing = true).apply { customItems = custom.toList() }

    @Test
    fun `a custom line filed under a line is ticked there and keeps its wording`() {
        val form = form("Hitch greased, ball and pin", "Roof rack fitted")
        form.fileCustom("Hitch greased, ball and pin", hitch)
        assertEquals(setOf(hitch.id), form.selected)
        assertEquals(listOf("Roof rack fitted"), form.customItems)
        assertEquals("Hitch greased, ball and pin", form.labelOf(hitch))
        // Still one line on the form, not two.
        assertEquals(2, form.itemCount)
    }

    @Test
    fun `wording that is the line's own name isn't kept as anything else`() {
        val form = form("Grease hitch")
        form.fileCustom("Grease hitch", hitch)
        assertEquals(emptyMap<String, String>(), form.keptLabels)
        assertEquals("Grease hitch", form.labelOf(hitch))
    }

    @Test
    fun `a line already ticked isn't filed onto`() {
        val form = form("Hitch greased")
        form.toggle(hitch.id)
        form.fileCustom("Hitch greased", hitch)
        assertEquals(listOf("Hitch greased"), form.customItems)
        assertEquals("Grease hitch", form.labelOf(hitch))
    }

    @Test
    fun `unticking the line lets its kept wording go`() {
        val form = form("Hitch greased")
        form.fileCustom("Hitch greased", hitch)
        form.toggle(hitch.id)
        assertEquals(emptySet<String>(), form.selected)
        assertEquals(emptyMap<String, String>(), form.keptLabels)
        // Ticked again, it is the line's own name.
        form.toggle(hitch.id)
        assertEquals("Grease hitch", form.labelOf(hitch))
    }

    // Made-up shop, work and prices (#572).
    private val battery = RecommendedItem("Battery Replacement", 28_900)
    private val seep = RecommendedItem("Oil pan seeping", null, "Fluid Leaks")
    private val wipers = RecommendedItem("Wiper Blade Replacement", 4_999)
    private val choices = listOf(
        RecommendedChoice(battery, keep = true, note = false, line = KeptLine("battery", guessed = true)),
        RecommendedChoice(seep, keep = false, note = true, line = null),
        RecommendedChoice(wipers, keep = false, note = false, line = null),
    )

    /** The shop's battery recommendation from an earlier visit, still open. */
    private val older = ShopRecommendation(41, 9, Fixtures.TODAY.minusMonths(3), "Made-up Motors", battery.copy(cents = 26_500), "battery")

    @Test
    fun `nothing is saved without a reading`() {
        assertNull(Fixtures.logService().toSave())
    }

    @Test
    fun `a new record saves the form as typed, the noted recommendations after the notes, and the kept ones`() {
        val form = Fixtures.logService().apply {
            data = data!!.copy(openRecommendations = listOf(older))
            odometerText = "53,000"
            diy = false
            shopName = "  Made-up Motors "
            costText = "84.50"
            notes = " Synthetic. "
            recommended = choices
        }
        val save = form.toSave() as LogSave.New
        val record = save.record
        assertEquals(Fixtures.truck.id, record.vehicleId)
        assertEquals(Fixtures.TODAY, record.date)
        assertEquals(53_000, record.odometer)
        assertEquals("Made-up Motors", record.shopName)
        assertEquals(8_450L, record.costCents)
        assertEquals(listOf("oil"), record.lines.map { it.catalogId })
        // Each noted item on its own line, after the user's own notes; what was let go is nowhere.
        assertEquals("Synthetic.\n\n" + recommendedNotes(listOf(choices[1])), record.notes)
        assertFalse(record.notes!!.contains("Wiper"))
        // Kept for later under its line, replacing the shop's open one for the same work.
        assertEquals(listOf(KeptRecommendation(battery, "battery", replaces = older.id)), save.kept)
        assertEquals(emptyList<Long>(), save.done)
        assertTrue(save.keepInvoice)
        assertEquals(AppText.res.getString(R.string.invoice_document_name, "Made-up Motors"), save.invoiceName)
    }

    @Test
    fun `work done by the owner keeps no shop, and its invoice is named plainly`() {
        val form = Fixtures.logService().apply {
            odometerText = "53,000"
            shopName = "Made-up Motors"
            keepInvoice = false
        }
        val save = form.toSave() as LogSave.New
        assertNull(save.record.shopName)
        assertNull(save.record.notes)
        assertFalse(save.keepInvoice)
        assertEquals(AppText.res.getString(R.string.invoice_document_name_plain), save.invoiceName)
    }

    @Test
    fun `an edit saves in the original's place, keeping what the form doesn't show, and adds nothing to the notes`() {
        val fixture = Fixtures.logService()
        val photo = PhotoEntity(id = 5, recordId = 12, fileName = "receipt.jpg")
        val original = ServiceRecordWithItems(
            ServiceRecordEntity(id = 12, vehicleId = Fixtures.truck.id, date = Fixtures.TODAY.minusDays(9), odometer = 52_000, diy = true, enteredAsLastDone = true, imported = true, visitId = 3),
            listOf(ServiceRecordItemEntity(id = 1, recordId = 12, scheduleItemId = null, label = "Oil and filter", catalogId = "oil")),
            listOf(photo),
        )
        val form = LogServiceForm(editing = true, today = { Fixtures.TODAY }).apply {
            data = fixture.data!!.copy(original = original)
            date = Fixtures.TODAY.minusDays(9)
            odometerText = "52,100"
            notes = "Synthetic."
            toggle("oil")
            recommended = choices
            removeKeptPhoto(photo)
        }
        val save = form.toSave() as LogSave.Edit
        assertEquals(original, save.original)
        assertEquals(setOf(5L), save.removedPhotoIds)
        assertEquals(DoneChange(), save.done)
        val record = save.record
        assertEquals(listOf(12L, 3L), listOf(record.id, record.visitId))
        assertTrue(record.enteredAsLastDone)
        assertTrue(record.imported)
        assertEquals(52_100, record.odometer)
        assertEquals("Synthetic.", record.notes)
    }
}
