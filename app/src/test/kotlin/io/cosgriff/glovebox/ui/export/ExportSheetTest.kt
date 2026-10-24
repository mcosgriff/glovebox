package io.cosgriff.glovebox.ui.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.domain.ExportDates
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.TestGraph
import io.cosgriff.glovebox.ui.settings.show
import io.cosgriff.glovebox.ui.settings.text
import io.cosgriff.glovebox.ui.settings.waitFor
import io.cosgriff.glovebox.ui.settings.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The export sheet: choose the vehicle, the dates and what goes in, see the file's name, and export. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35], shadows = [ShadowPdfDocument::class])
class ExportSheetTest {
    @get:Rule(order = 0) val g = TestGraph()
    @get:Rule(order = 1) val compose = createComposeRule()

    private val saver = ExportSaver(g.context, g.appScope)

    private fun vm() = ExportViewModel(g.db.vehicles(), g.db.service(), g.db.schedule(), g.db.recalls(), g.db.odometer(), g.db.documents(), g.settings, g.photos, saver)

    private fun record(id: Long, date: LocalDate) = runBlocking {
        g.db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = date, odometer = 41_000, diy = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Oil & filter change")),
        )
    }

    @Test
    fun `with no vehicle there is nothing to export`() {
        compose.show { ExportSheet(vehicleId = null, onDismiss = {}, vm = vm()) }
        compose.waitForText(text(R.string.export_no_vehicle))
    }

    @Test
    fun `a vehicle with nothing on it can't be exported, and says so`() {
        runBlocking { g.vehicle() }
        compose.show { ExportSheet(vehicleId = null, onDismiss = {}, vm = vm()) }
        compose.waitForText(text(R.string.export_nothing))
        compose.onNodeWithText(text(R.string.export_button, AppExportWords.records(0))).assertIsNotEnabled()
    }

    @Test
    fun `the choices change what is exported, and Export hands the file to save as and closes the sheet`() {
        val id = runBlocking { g.vehicle() }
        val other = runBlocking { g.vehicle(make = "Volkswagen", model = "GTI") }
        record(id, LocalDate.of(2020, 6, 1))
        record(id, LocalDate.now())
        val asked = mutableListOf<Pair<String, String>>()
        saver.attach { mime, name -> asked += mime to name }
        var dismissed = 0
        val vm = vm()
        compose.show { ExportSheet(vehicleId = id, onDismiss = { dismissed++ }, format = ExportFormat.PDF, vm = vm) }
        compose.waitForText(text(R.string.export_button, AppExportWords.records(2)))

        // Photos go only in a PDF; the choice is kept while Markdown is picked.
        compose.onNodeWithText(text(R.string.export_markdown)).performClick()
        compose.onNode(isToggleable() and hasText(text(R.string.export_photos_pdf_only))).performScrollTo().assertIsOff().assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.export_pdf)).performClick()
        compose.onNode(isToggleable() and hasText(text(R.string.export_photos))).performScrollTo().assertIsOn().performClick()
        assertTrue(!vm.options.photos)

        compose.onNode(isToggleable() and hasText(text(R.string.export_costs))).performScrollTo().performClick()
        compose.onNode(isToggleable() and hasText(text(R.string.export_schedule))).performScrollTo().performClick()
        compose.onNode(isToggleable() and hasText(text(R.string.export_takes))).performScrollTo().performClick()
        compose.onNode(isToggleable() and hasText(text(R.string.export_documents))).performScrollTo().performClick()
        assertTrue(!vm.options.costsAndNotes && !vm.options.scheduleAndRecalls && !vm.options.whatItTakes && !vm.options.documents)

        // This year only: the 2020 record drops out.
        compose.onNodeWithText(text(R.string.export_all_time)).performClick()
        compose.onNodeWithText(text(R.string.export_this_year)).performClick()
        assertEquals(ExportDates.ThisYear, vm.options.dates)
        compose.onNodeWithText(text(R.string.export_button, AppExportWords.records(1))).assertIsDisplayed()

        // Every vehicle, then the other one.
        compose.onNodeWithText("4Runner", substring = true).performClick()
        compose.onNodeWithText(text(R.string.home_all_vehicles)).performClick()
        assertEquals(null, vm.vehicleId)
        compose.onNodeWithText(text(R.string.home_all_vehicles)).performClick()
        compose.onNodeWithText("GTI", substring = true).performClick()
        assertEquals(other, vm.vehicleId)
        compose.onNodeWithText(text(R.string.export_nothing)).assertExists()
        compose.onNodeWithText("GTI", substring = true).performClick()
        compose.onNodeWithText("4Runner", substring = true).performClick()

        compose.onNodeWithText(vm.fileName).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.export_button, AppExportWords.records(1))).performScrollTo().performClick()
        compose.waitFor { dismissed == 1 }
        assertEquals(listOf(ExportFormat.PDF.mimeType to vm.fileName), asked)
    }

    @Test
    fun `a custom range is picked a date at a time and kept the right way round`() {
        val id = runBlocking { g.vehicle() }
        record(id, LocalDate.now())
        val vm = vm()
        compose.show { ExportSheet(vehicleId = id, onDismiss = {}, format = ExportFormat.MARKDOWN, vm = vm) }
        compose.waitForText(text(R.string.export_all_time))
        compose.onNodeWithText(text(R.string.export_all_time)).performClick()
        compose.onNodeWithText(text(R.string.export_custom_pick)).performClick()
        val range = vm.options.dates as ExportDates.Custom
        assertEquals(LocalDate.now().withDayOfYear(1), range.from)
        compose.onNodeWithText(text(R.string.export_custom)).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.export_from, range.from.format(io.cosgriff.glovebox.ui.service.longDate))).performClick()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(text(R.string.export_to, range.to.format(io.cosgriff.glovebox.ui.service.longDate))).performClick()
        compose.onNodeWithText(text(R.string.ok)).performClick()
        val after = vm.options.dates as ExportDates.Custom
        assertTrue(!after.from.isAfter(after.to))
    }
}
