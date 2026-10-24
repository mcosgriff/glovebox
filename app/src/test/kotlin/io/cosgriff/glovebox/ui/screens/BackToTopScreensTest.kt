package io.cosgriff.glovebox.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageContent
import io.cosgriff.glovebox.ui.reminders.RemindersContent
import io.cosgriff.glovebox.ui.settings.SettingsContent
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import io.cosgriff.glovebox.ui.vehicle.TakesKnown
import io.cosgriff.glovebox.ui.vehicle.VehicleDetailsContent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Top button on the Reminders tab, the garage, vehicle details and settings, through each `…Content`
 * from made-up [Fixtures]: not there at the top, there once a screen or more down, and a tap goes back up.
 * On a short phone, so a page of ordinary length is more than two screens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = "w411dp-h480dp-xhdpi", sdk = [35])
class BackToTopScreensTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(id: Int) = context.getString(id)
    private val top get() = compose.onNodeWithContentDescription(string(R.string.scroll_back_to_top))

    /** Whether [text] is on screen: a lazy list drops what has gone by, a whole page only moves it off. */
    private fun showing(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().any { it.boundsInRoot.bottom > 0f }

    /** Shows [content], checks there is no Top button, flicks to the end and back to [first] with the button. */
    private fun check(first: String, content: @Composable () -> Unit) {
        compose.setContent { GloveboxTheme { content() } }
        compose.onNodeWithText(first).assertIsDisplayed()
        top.assertDoesNotExist()

        repeat(6) {
            compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        assertFalse(showing(first))
        top.assertIsDisplayed()

        top.performClick()
        compose.waitForIdle()
        compose.onNodeWithText(first).assertIsDisplayed()
        assertTrue(showing(first))
        top.assertDoesNotExist()
    }

    @Test
    fun reminders() = check(string(R.string.reminders_title)) {
        RemindersContent(
            Fixtures.garage, Fixtures.TODAY, notificationsAllowed = true,
            onLogIt = { _, _ -> }, onAddVehicle = {}, onOpenRules = {}, onOpenNotifications = {}, onSnooze = {}, onEndSnooze = {},
        )
    }

    @Test
    fun garage() {
        // Eight made-up trucks, to make the list long.
        val many = Garage(
            (1L..8L).map { id ->
                val g = Fixtures.garage.vehicles.first()
                g.copy(vehicle = g.vehicle.copy(id = id, nickname = "Truck $id"))
            },
        )
        check(string(R.string.garage_title)) {
            GarageContent(many, archived = 0, onAddVehicle = {}, onOpenVehicle = {}, onSeeReminders = {}, onOpenArchive = {}, onRestore = {})
        }
    }

    @Test
    fun vehicleDetails() = check(string(R.string.home_vehicle_details)) {
        VehicleDetailsContent(
            Fixtures.vehicleDetails, photo = null, photoFailed = false, known = TakesKnown(), makerDocs = null,
            nickname = Fixtures.truck.nickname.orEmpty(), noticePrefs = NoticePrefs(), onNicknameChange = {},
            onBack = {}, onOpenRecalls = {}, onOpenSchedule = {}, onDelete = {}, onOpenTorqueSpecs = {}, onOpenMoreTakes = {},
            onOpenReadings = {}, onOpenImport = {}, onOpenDocuments = {}, onClearHistory = {},
            onEditTakes = {}, onFillKnown = {}, onEditPlate = {}, onEditCheckIn = {}, onAddVin = {}, onSetOffset = {}, onChangeUnit = {},
            onSell = {}, onPickPhoto = {}, onRemovePhoto = {},
        )
    }

    @Test
    fun settings() = check(string(R.string.settings_title)) {
        SettingsContent(
            Fixtures.settings, Fixtures.settingsVehicles, canLock = true,
            onOpenRules = {}, onOpenNotifications = {}, onOpenBackup = {}, onOpenImport = {}, onDeleteAll = {}, onOpenAbout = {},
            onOpenSchedule = {}, onSetTheme = {}, onSetBiometric = {}, onSetLockDelay = {}, onSetHideInRecents = {},
            onSetScannerDetails = {}, onPick = {}, onExport = {},
        )
    }
}
