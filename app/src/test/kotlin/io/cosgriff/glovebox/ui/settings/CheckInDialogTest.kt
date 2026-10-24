package io.cosgriff.glovebox.ui.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.VehicleCheckIn
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** How often to ask for a reading: the default's choices, or one vehicle's, which can follow the default or be off. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class CheckInDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `a vehicle can follow the default, be off, or ask every so many days`() {
        var saved: VehicleCheckIn? = null
        compose.show { CheckInDialog(current = VehicleCheckIn.Default, default = 14, onSave = { saved = it }, onDismiss = {}) }
        compose.onNodeWithText(text(R.string.checkin_vehicle_note)).assertExists()
        compose.onNodeWithText(text(R.string.checkin_as_default, checkInLabel(14))).assertIsSelected()

        compose.onNodeWithText(text(R.string.checkin_off)).performClick()
        compose.onNodeWithText(text(R.string.save)).performClick()
        assertEquals(VehicleCheckIn.Off, saved)

        compose.onNodeWithText(text(R.string.checkin_as_default, checkInLabel(14))).performClick()
        compose.onNodeWithText(text(R.string.save)).performClick()
        assertEquals(VehicleCheckIn.Default, saved)

        compose.onNodeWithText(checkInLabel(30)).performClick()
        compose.onNodeWithText(text(R.string.save)).performClick()
        assertEquals(VehicleCheckIn.Every(30), saved)
    }

    @Test
    fun `a custom number of days must be in range before it can be saved`() {
        var saved: VehicleCheckIn? = null
        compose.show { CheckInDialog(current = VehicleCheckIn.Every(14), default = null, onSave = { saved = it }, onDismiss = {}) }
        compose.onNodeWithText(text(R.string.checkin_custom)).performClick()
        val save = compose.onNodeWithText(text(R.string.save))
        save.assertIsNotEnabled()
        val days = compose.onNode(hasSetTextAction())
        days.performTextInput("0")
        save.assertIsNotEnabled()
        days.performTextReplacement("4x5")
        save.assertIsEnabled().performClick()
        assertEquals("only digits are kept", VehicleCheckIn.Every(45), saved)
    }

    @Test
    fun `a custom figure saved earlier opens as custom, and Cancel saves nothing`() {
        var saved: VehicleCheckIn? = null
        var dismissed = 0
        compose.show { CheckInDialog(current = VehicleCheckIn.Every(45), default = null, onSave = { saved = it }, onDismiss = { dismissed++ }) }
        compose.onNodeWithText(text(R.string.checkin_custom)).assertIsSelected()
        compose.onNodeWithText("45").assertExists()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(1, dismissed)
        assertNull(saved)
    }
}
