package io.cosgriff.glovebox.ui.scan

import android.graphics.Point
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.BarcodeKind
import io.cosgriff.glovebox.ui.garage.UnconfinedMainRule
import io.cosgriff.glovebox.ui.garage.awaitMatching
import io.cosgriff.glovebox.ui.garage.testSettings
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The VIN scanner around its readers. With the camera allowed the scanner starts zxing-cpp, whose native library
 * can't load on the JVM, so only the screen without the camera, the setting and the panel's picture are tested here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ScanVinScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    @get:Rule val main = UnconfinedMainRule()

    @Test
    fun `"Show what the scanner sees" follows the setting`() = runBlocking {
        val settings = testSettings(scannerDetails = false)
        val vm = ScanVinViewModel(settings)
        assertFalse(vm.details.awaitMatching { true })
        settings.setScannerDetails(true)
        assertTrue(vm.details.awaitMatching { it })
    }

    @Test
    fun `without the camera it says what it would read and offers typing the VIN`() {
        var closes = 0
        compose.setContent { ScanVinScreen(onClose = { closes++ }, onUseVin = {}, vm = ScanVinViewModel(testSettings())) }
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.scan_vin_camera_why)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.scan_vin_type_instead)).performClick()
        assertEquals(1, closes)
    }

    @Test
    fun `the panel's picture is a small grey copy of the band, with the barcode outlined in fractions of it`() {
        // 720 x 4, every pixel its column's brightness.
        val band = ByteArray(720 * 4) { (it % 720 / 3).toByte() }
        val sighting = VinBarcodeReader.Sighting(BarcodeKind.CODE_39, null, listOf(Point(72, 0), Point(648, 0), Point(648, 4), Point(72, 4)))
        val view = BandView.of(band, 720, 4, sighting)
        // Halved to fit the panel.
        assertEquals(360 to 2, view.image.width to view.image.height)
        assertEquals(180f, view.aspect)
        assertEquals(listOf(Offset(0.1f, 0f), Offset(0.9f, 0f), Offset(0.9f, 1f), Offset(0.1f, 1f)), view.corners)
        // Found but not read: drawn in the guide's amber, not as read.
        assertFalse(view.read)

        val plain = BandView.of(band, 720, 4, null)
        assertNull(plain.corners)
        assertTrue(BandView.of(band, 720, 4, sighting.copy(text = "a VIN")).read)
    }
}
