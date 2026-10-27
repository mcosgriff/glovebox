package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.ui.components.DecodedField
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class AddVehicleScreenTest {
    @get:Rule val compose = createComposeRule()
    // Room's writes from viewModelScope finish only off Robolectric's paused main looper, as in the ViewModel tests.
    @get:Rule val main = UnconfinedMainRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = testDatabase()
    private val settings = testSettings()
    private val nhtsa = FakeNhtsa().apply { decode = truckDecode }
    private val vin = Fixtures.truck.vin!!
    private val events = mutableListOf<String>()

    @After fun close() = db.close()

    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(scanned: String? = null): AddVehicleViewModel {
        val vm = AddVehicleViewModel(nhtsa.client, db.vehicles(), nhtsa.repository(db, settings), settings)
        compose.setContent {
            GloveboxTheme {
                AddVehicleScreen(
                    onSaved = { events += "saved $it" },
                    onBack = { events += "back" },
                    onNoVin = { events += "no vin" },
                    onScan = { events += "scan" },
                    scannedVin = scanned,
                    onScannedVinUsed = { events += "scan used" },
                    vm = vm,
                )
            }
        }
        return vm
    }

    private fun field(@StringRes label: Int): SemanticsNodeInteraction = compose.onNode(hasSetTextAction() and hasText(str(label)))

    private fun text(value: String) = compose.onNodeWithText(value).performScrollTo()

    /** Types the VIN, decodes it and waits for the confirm step. */
    private fun decode() {
        field(R.string.add_vehicle_vin_label).performTextInput(vin)
        compose.onNodeWithText(str(R.string.add_vehicle_decode)).performScrollTo().performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(str(R.string.add_vehicle_confirm_title)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `the VIN is counted to 17, its check digit checked, and Decode waits for both`() {
        show()
        compose.onNodeWithText(str(R.string.add_vehicle_vin_count, 0)).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_decode)).assertIsNotEnabled()

        field(R.string.add_vehicle_vin_label).performTextInput(vin.dropLast(1) + "2")
        compose.onNodeWithText(str(R.string.add_vehicle_vin_check_digit_wrong)).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_vin_wrong_character)).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_decode)).assertIsNotEnabled()

        field(R.string.add_vehicle_vin_label).performTextReplacement(vin)
        compose.onNodeWithText(str(R.string.add_vehicle_vin_valid)).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_decode)).assertIsEnabled()
    }

    @Test
    fun `scan, no VIN and Back each go their own way`() {
        show()
        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_scan)).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_no_vin)).performScrollTo().performClick()
        assertEquals(listOf("back", "scan", "no vin"), events)
    }

    @Test
    fun `NHTSA out of reach is said under the button`() {
        nhtsa.decode = null
        show()
        field(R.string.add_vehicle_vin_label).performTextInput(vin)
        compose.onNodeWithText(str(R.string.add_vehicle_decode)).performScrollTo().performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(str(R.string.add_vehicle_nhtsa_unreachable)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `the confirm step shows vPIC's answer, the recalls, and Back returns to the VIN`() {
        nhtsa.recalls = listOf(recallRow("26V100000"), recallRow("26V200000"))
        show()
        decode()
        compose.onNodeWithText(vin).assertExists()
        compose.onNodeWithText(VehicleText.title(2019, "TOYOTA", "4Runner", trim = null)!!).assertExists()
        text("4.0L 6-cyl · ${str(R.string.add_vehicle_horsepower, "270")}").assertExists()
        // The engine code, the tire pressure sensors and the chassis code are rows of their own, kept and correctable.
        text("1GR-XX").assertExists()
        text("Direct").assertExists()
        text("XYZ100L").assertExists()
        text("Class 1D").assertExists()
        text("Testville, Nowhere").assertExists()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(str(R.string.add_vehicle_recalls_found, "2")).fetchSemanticsNodes().isNotEmpty() }
        // One trim from vPIC fills the field; there is nothing to pick between.
        compose.onAllNodesWithText(str(R.string.add_vehicle_pick_trim)).assertCountEquals(0)

        compose.onNodeWithContentDescription(str(R.string.back)).performScrollTo().performClick()
        field(R.string.add_vehicle_vin_label).assertExists()
        compose.onNodeWithText(vin).assertExists()
    }

    @Test
    fun `an unidentified VIN asks for year, make and model, and takes them from the dialog`() {
        nhtsa.decode = mapOf("ErrorCode" to "8", "ErrorText" to "8 - Made-up error")
        show()
        decode()
        compose.onNodeWithText(str(R.string.add_vehicle_unidentified_title)).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_nhtsa_said, "8 - Made-up error")).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_no_trim)).performScrollTo().assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_next)).performScrollTo().assertIsNotEnabled()

        compose.onNodeWithText(str(R.string.add_vehicle_enter_ymm)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.save)).assertIsNotEnabled()
        field(R.string.add_vehicle_model_year).performTextInput("2019")
        field(R.string.add_vehicle_make).performTextInput("Toyota")
        field(R.string.add_vehicle_model).performTextInput("4Runner")
        compose.onNodeWithText(str(R.string.save)).performClick()

        compose.onNodeWithText(VehicleText.title(2019, "Toyota", "4Runner", trim = null)!!).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_next)).performScrollTo().assertIsEnabled()
    }

    @Test
    fun `a flagged decode says so but can still be saved`() {
        nhtsa.decode = truckDecode + mapOf("ErrorCode" to "1", "ErrorText" to "1 - Made-up warning")
        show()
        decode()
        compose.onNodeWithText(str(R.string.add_vehicle_partial_title)).assertExists()
        compose.onNodeWithText(str(R.string.add_vehicle_next)).performScrollTo().assertIsEnabled()
    }

    @Test
    fun `a decoded value is corrected in its dialog`() {
        val vm = show()
        decode()
        text(str(R.string.add_vehicle_fuel)).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_field_replaces)).assertExists()
        compose.onNode(hasSetTextAction() and hasText("Gasoline")).performTextReplacement("Diesel")
        compose.onNodeWithText(str(R.string.save)).performClick()
        compose.waitForIdle()
        assertEquals("Diesel", vm.fields[DecodedField.FUEL])

        // Cancel changes nothing.
        text(str(R.string.add_vehicle_transmission)).performClick()
        compose.onNodeWithText(str(R.string.cancel)).performClick()
        compose.waitForIdle()
        assertEquals("5-speed Automatic", vm.fields[DecodedField.TRANSMISSION])
    }

    @Test
    fun `the drive type keeps vPIC's wording unless one is picked, and a pick replaces it`() {
        nhtsa.decode = truckDecode + mapOf("DriveType" to "Part-time 4x4")
        val vm = show()
        decode()
        text(str(R.string.add_vehicle_drive_type)).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_drive_kept, "Part-time 4x4")).assertExists()
        // Saved untouched: the decoded wording stays as it was.
        compose.onNodeWithText(str(R.string.save)).performClick()
        compose.waitForIdle()
        assertEquals("Part-time 4x4", vm.fields[DecodedField.DRIVE])

        text(str(R.string.add_vehicle_drive_type)).performClick()
        compose.onNode(hasText("4WD") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_drive_replaces)).assertExists()
        compose.onNodeWithText(str(R.string.save)).performClick()
        compose.waitForIdle()
        assertEquals("4WD", vm.fields[DecodedField.DRIVE])
    }

    @Test
    fun `a drive type picked again is cleared, and nothing is left`() {
        val vm = show()
        decode()
        // vPIC's "4WD/4-Wheel Drive/4x4" starts as 4WD; tapping it clears the choice.
        text(str(R.string.add_vehicle_drive_type)).performClick()
        compose.onNode(hasText("4WD") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText(str(R.string.save)).performClick()
        compose.waitForIdle()
        assertEquals(null, vm.fields[DecodedField.DRIVE])
    }

    @Test
    fun `the powertrain is only what the owner picks`() {
        val vm = show()
        decode()
        text(str(R.string.add_vehicle_powertrain)).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_powertrain_note)).assertExists()
        compose.onNodeWithText(str(R.string.powertrain_hybrid)).performClick()
        compose.waitForIdle()
        assertEquals(Powertrain.HYBRID, vm.powertrain)
    }

    @Test
    fun `the body style is NHTSA's only where it says so plainly, then what the owner picks`() {
        val vm = show()
        decode()
        assertEquals(null, vm.bodyStyle)
        text(str(R.string.add_vehicle_body_style)).performClick()
        compose.onNodeWithText(str(R.string.add_vehicle_body_style_note)).assertExists()
        compose.onNodeWithText(str(R.string.body_style_hatchback)).performClick()
        compose.waitForIdle()
        assertEquals(BodyStyle.HATCHBACK, vm.bodyStyle)
    }

    @Test
    fun `two trims are offered and the owner's pick is saved with the reading and unit`() {
        nhtsa.decode = truckDecode + mapOf("Trim" to "SR5/Limited")
        show()
        decode()
        compose.onNodeWithText(str(R.string.add_vehicle_trim_confirm)).assertExists()
        compose.onNodeWithText("Limited").performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.unit_kilometres_title)).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText(str(R.string.odometer_in, DistanceUnit.KILOMETERS.symbolWord))).performTextInput("98000")
        compose.onNodeWithText(str(R.string.add_vehicle_next)).performScrollTo().performClick()
        compose.waitUntil(TIMEOUT) { events.any { it.startsWith("saved") } }

        val id = events.single { it.startsWith("saved") }.removePrefix("saved ").toLong()
        val v = runBlocking { db.vehicles().byId(id)!! }
        assertEquals("Limited", v.trim)
        assertEquals(DistanceUnit.KILOMETERS.name, v.distanceUnit)
        assertEquals(listOf(98_000), runBlocking { db.odometer().readingsFor(id) }.map { it.odometer })
    }

    @Test
    fun `a VIN handed back by the scanner is decoded at once`() {
        show(scanned = vin)
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(str(R.string.add_vehicle_confirm_title)).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("scan used"), events)
    }
}
