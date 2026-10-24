package io.cosgriff.glovebox.ui.export

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.domain.ExportDates
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.Exports
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.MainDispatcherRule
import io.cosgriff.glovebox.ui.settings.TestGraph
import io.cosgriff.glovebox.ui.settings.awaitValue
import io.cosgriff.glovebox.ui.settings.eventually
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/**
 * The export: everything about each vehicle read once, filtered by the choices, written to a working file in the
 * clear and handed to "save as", which is the only place it stays.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35], shadows = [ShadowPdfDocument::class])
class ExportViewModelTest {
    @get:Rule(order = 0) val g = TestGraph()
    @get:Rule(order = 1) val main = MainDispatcherRule()

    private val saver = ExportSaver(g.context, g.appScope)
    private val asked = mutableListOf<Pair<String, String>>()
    private val day = LocalDate.of(2025, 5, 4)

    private fun vm() = ExportViewModel(
        g.db.vehicles(), g.db.service(), g.db.schedule(), g.db.recalls(), g.db.odometer(), g.db.documents(), g.settings, g.photos, saver,
    )

    /** A made-up vehicle with a record, a schedule item, a recall and a document on file. */
    private suspend fun fullVehicle(model: String = "4Runner", photo: String? = null): Long {
        val id = g.vehicle(model = model, nickname = "Blue")
        g.db.vehicles().setVin(id, "JTEBU5JR7K5900001")
        val oil = ScheduleItemEntity(vehicleId = id, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6)
        g.db.schedule().insertAll(listOf(oil))
        val itemId = g.db.schedule().allFor(id).single().id
        val record = g.db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = day, odometer = 41_000, diy = false, shopName = "Corner Garage", costCents = 6_250, notes = "Made-up note"),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = itemId, label = "Oil & filter change")),
        )
        photo?.let { g.db.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = it))) }
        g.db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = day.minusYears(2), odometer = 20_000, diy = true, enteredAsLastDone = true, imported = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Tire rotation")),
        )
        g.db.recalls().insertAll(listOf(RecallEntity(vehicleId = id, campaign = "99V999000", component = "MADE UP: COMPONENT", summary = "Invented.", firstSeenOn = day)))
        g.db.documents().insert(DocumentEntity(vehicleId = id, type = "TITLE", name = "Title", fileName = "doc.enc", format = "PDF", sizeBytes = 10, addedOn = day))
        return id
    }

    private suspend fun ExportViewModel.loaded() = eventually { all != null }

    @Test
    fun `opening reads every vehicle, garage and sold, and starts on the one asked for`() = runTest {
        val first = fullVehicle()
        val sold = g.vehicle(make = "Volkswagen", model = "GTI")
        g.db.vehicles().setArchived(sold, day)
        val vm = vm()
        vm.open(sold, ExportFormat.MARKDOWN)
        vm.loaded()
        assertEquals(sold, vm.vehicleId)
        assertEquals(ExportFormat.MARKDOWN, vm.options.format)
        assertEquals(setOf(first to false, sold to true), vm.all!!.map { it.id to it.archived }.toSet())
        assertEquals(Exports.fileName("GTI", LocalDate.now(), ExportFormat.MARKDOWN), vm.fileName)

        val exported = vm.all!!.single { it.id == first }.vehicle
        assertEquals("Blue", exported.nickname)
        assertEquals("JTEBU5JR7K5900001", exported.vin)
        assertEquals(2, exported.records.size)
        assertEquals("Corner Garage", exported.records.first { !it.enteredAsLastDone }.doneBy)
        assertEquals(AppText.res.getString(R.string.who_diy), exported.records.first { it.enteredAsLastDone }.doneBy)
        assertEquals(listOf("Oil change"), exported.schedule.map { it.name })
        assertEquals(1, exported.recalls.size)
        assertEquals(listOf("Title"), exported.documents.map { it.name })

        vm.chooseVehicle(null)
        assertEquals(2, vm.document!!.vehicles.size)
        vm.chooseVehicle(first)
        assertEquals(1, vm.document!!.vehicles.size)
    }

    @Test
    fun `a vehicle that isn't there any more starts the sheet on the first one`() = runTest {
        val id = g.vehicle()
        val vm = vm()
        vm.open(999, ExportFormat.PDF)
        vm.loaded()
        assertEquals(id, vm.vehicleId)
    }

    @Test
    fun `the options are applied to what would be written`() = runTest {
        fullVehicle()
        val vm = vm()
        vm.open(null, ExportFormat.PDF)
        vm.loaded()
        assertEquals(2, vm.document!!.recordCount)
        vm.change(ExportOptions(format = ExportFormat.PDF, dates = ExportDates.Custom(day, day)))
        assertEquals(1, vm.document!!.recordCount)
        vm.change(vm.options.copy(costsAndNotes = false, scheduleAndRecalls = false))
        val v = vm.document!!.vehicles.single()
        assertNull(v.records.single().costCents)
        assertTrue(v.schedule.isEmpty())
        assertTrue(v.recalls.isEmpty())
    }

    @Test
    fun `a Markdown export is written to a working file, offered to save as, and gone once copied out`() = runTest {
        fullVehicle()
        saver.attach { mime, name -> asked += mime to name }
        val vm = vm()
        vm.open(null, ExportFormat.MARKDOWN)
        vm.loaded()
        var started = false
        vm.export { started = true }
        eventually { started }
        assertFalse(vm.working)
        assertEquals(listOf(ExportFormat.MARKDOWN.mimeType to vm.fileName), asked)

        val out = File(g.context.filesDir, "out.md")
        saver.onDestination(Uri.fromFile(out))
        assertEquals(AppText.res.getString(R.string.export_saved, vm.fileName), saver.results.awaitValue { true })
        val text = out.readText()
        assertTrue(text.contains("JTEBU5JR7K5900001"))
        assertTrue(text.contains("Oil & filter change"))
        assertTrue(text.contains("Corner Garage"))
        assertTrue("the working copy in the clear is gone", File(g.context.cacheDir, "export").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `a PDF export draws each record with its photos, through the encrypted photo store`() = runTest {
        val jpeg = ByteArrayOutputStream().also { Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        g.crypto.encryptTo("photo-1.enc", jpeg.inputStream())
        fullVehicle(photo = "photo-1.enc")
        saver.attach { mime, name -> asked += mime to name }
        val vm = vm()
        vm.open(null, ExportFormat.PDF)
        vm.loaded()
        assertEquals(listOf("photo-1.enc"), vm.document!!.photoFiles)
        var started = false
        vm.export { started = true }
        eventually { started }
        assertEquals(listOf(ExportFormat.PDF.mimeType to vm.fileName), asked)
        val out = File(g.context.filesDir, "out.pdf")
        saver.onDestination(Uri.fromFile(out))
        saver.results.awaitValue { true }

        val drawn = pages(out.readBytes()).flatten()
        assertTrue(drawn.contains("JTEBU5JR7K5900001"))
        assertTrue(drawn.toString(), drawn.any { it.startsWith("[picture") })
        assertTrue(drawn.contains(io.cosgriff.glovebox.domain.Recalls.campaignLabel("99V999000")))
    }

    @Test
    fun `nothing is exported with nothing to export, and a backed-out save leaves nothing behind`() = runTest {
        val vm = vm()
        vm.export {}
        assertFalse(vm.working)
        g.vehicle()
        saver.attach { mime, name -> asked += mime to name }
        vm.open(null, ExportFormat.MARKDOWN)
        vm.loaded()
        var started = false
        vm.export { started = true }
        eventually { started }
        saver.onDestination(null)
        eventually { File(g.context.cacheDir, "export").listFiles().orEmpty().isEmpty() }
        // A second answer to the same request does nothing.
        saver.onDestination(null)
    }

    @Test
    fun `a destination that can't be written is said so`() = runTest {
        g.vehicle()
        saver.attach { mime, name -> asked += mime to name }
        val vm = vm()
        vm.open(null, ExportFormat.MARKDOWN)
        vm.loaded()
        var started = false
        vm.export { started = true }
        eventually { started }
        saver.onDestination(Uri.fromFile(File(g.context.filesDir, "no-such-folder/out.md")))
        assertEquals(AppText.res.getString(R.string.export_couldnt_save, vm.fileName), saver.results.awaitValue { true })
    }
}
