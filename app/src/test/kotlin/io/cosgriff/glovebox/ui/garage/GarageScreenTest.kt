package io.cosgriff.glovebox.ui.garage

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import io.cosgriff.glovebox.data.log.VehicleLogs
import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class GarageScreenTest {
    @get:Rule val compose = createComposeRule()

    // Room's and DataStore's flows reach viewModelScope only off Robolectric's paused main looper.
    @get:Rule val main = UnconfinedMainRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = testDatabase()
    private val settings = testSettings()

    @After fun close() = db.close()

    private val opened = mutableListOf<String>()

    private fun content(garage: Garage, archived: Int) = compose.setContent {
        GloveboxTheme {
            GarageContent(
                garage, archived,
                onAddVehicle = { opened += "add" },
                onOpenVehicle = { opened += "vehicle $it" },
                onSeeReminders = { opened += "reminders" },
                onOpenArchive = { opened += "archive" },
                onRestore = { opened += "restore" },
            )
        }
    }

    @Test
    fun `each part of the garage leads where it says`() {
        content(Fixtures.garage, archived = 2)
        compose.onNodeWithContentDescription(context.getString(R.string.garage_add_vehicle)).performClick()
        compose.onNodeWithText("Hatch", substring = true).performClick()
        val list = compose.onNode(hasScrollAction())
        list.performScrollToNode(hasText(context.getString(R.string.garage_see_all_reminders)))
        compose.onNodeWithText(context.getString(R.string.garage_see_all_reminders)).performClick()
        list.performScrollToNode(hasText(context.getString(R.string.garage_archived, "2")))
        compose.onNodeWithText(context.getString(R.string.garage_archived, "2")).performClick()
        assertEquals(listOf("add", "vehicle ${Fixtures.hatch.id}", "reminders", "archive"), opened)
    }

    @Test
    fun `two vehicles are looked across as both`() {
        content(Fixtures.garage, archived = 0)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(context.getString(R.string.garage_across_both)))
        compose.onNodeWithText(context.getString(R.string.garage_across_both)).assertExists()
        // No archive, no row for it.
        compose.onAllNodesWithText(context.getString(R.string.garage_archived, "0")).assertCountEquals(0)
    }

    @Test
    fun `an empty garage offers to add, to restore, and the way back to sold vehicles`() {
        content(Garage(emptyList()), archived = 1)
        compose.onNodeWithText(context.getString(R.string.garage_empty_add)).performClick()
        compose.onNodeWithText(context.getString(R.string.welcome_restore)).performClick()
        compose.onNodeWithText(context.getString(R.string.garage_archived, "1")).performClick()
        assertEquals(listOf("add", "restore", "archive"), opened)
    }

    @Test
    fun `a vehicle NHTSA has answered for with nothing open says so, and one with no reading says that`() {
        val quiet = GarageVehicle(Fixtures.hatch, odometer = null, comingUp = Fixtures.hatchComingUp, oil = null, recalls = OpenRecalls(0, null))
        content(Garage(listOf(quiet)), archived = 0)
        compose.onNodeWithText(context.getString(R.string.no_open_recalls)).assertExists()
        compose.onNodeWithText(context.getString(R.string.garage_no_mileage)).assertExists()
    }

    @Test
    fun `a vehicle with an open recall that says not to drive, or to park outside, says so on its card`() {
        val stop = GarageVehicle(Fixtures.hatch, odometer = null, comingUp = Fixtures.hatchComingUp, oil = null, recalls = OpenRecalls(2, null, doNotDrive = true, parkOutside = true))
        var recalls by mutableStateOf(stop.recalls)
        compose.setContent { GloveboxTheme { GarageContent(Garage(listOf(stop.copy(recalls = recalls))), 0, {}, {}, {}, {}, {}) } }
        // Not driving it is the one to act on; it covers parking outside too.
        compose.onNodeWithText(context.getString(R.string.garage_recall_do_not_drive)).assertExists()
        compose.onNodeWithText(context.getString(R.string.garage_recall_park_outside)).assertDoesNotExist()

        recalls = OpenRecalls(1, null, parkOutside = true)
        compose.onNodeWithText(context.getString(R.string.garage_recall_park_outside)).assertExists()
    }

    private fun viewModel() = GarageViewModel(
        GarageOverview(VehicleLogs(db.vehicles(), db.schedule(), db.service(), db.odometer(), settings), db.recalls()),
        FakeNhtsa().repository(db, settings),
        db.vehicles(),
    )

    @Test
    fun `with one vehicle the garage opens straight onto it, once`() {
        val id = runBlocking { db.vehicles().add(Fixtures.truck.copy(id = 0, recallsCheckedOn = LocalDate.now()), firstReading = 61_000) }
        val vm = viewModel()
        compose.setContent {
            GloveboxTheme {
                GarageScreen(true, {}, onOpenVehicle = { opened += "vehicle $it" }, {}, {}, {}, vm)
            }
        }
        compose.waitUntil(TIMEOUT) { opened.isNotEmpty() }
        compose.waitForIdle()
        assertEquals(listOf("vehicle $id"), opened)
        // The list isn't drawn on its way out.
        compose.onAllNodesWithText(context.getString(R.string.garage_title)).assertCountEquals(0)
    }

    @Test
    fun `while another screen is over it, a single vehicle's garage stays a list`() {
        runBlocking {
            db.vehicles().add(Fixtures.truck.copy(id = 0, recallsCheckedOn = LocalDate.now()), firstReading = null)
        }
        val vm = viewModel()
        compose.setContent {
            GloveboxTheme {
                // Not showing: a notification opened something over it on a cold start.
                GarageScreen(false, {}, onOpenVehicle = { opened += "vehicle $it" }, {}, {}, {}, vm)
            }
        }
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(context.getString(R.string.garage_title)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(Fixtures.truck.nickname!!, substring = true).onFirst().assertExists()
        assertEquals(emptyList<String>(), opened)
    }
}
