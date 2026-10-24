package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.LastDoneRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.pluralWord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** "When were these last done?" over a real database, through its ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class LastDoneScreenTest {
    @get:Rule val compose = createComposeRule()
    private val world = ScheduleWorld()
    private val unit = DistanceUnit.MILES

    @After fun close() = world.close()

    private var done = 0
    private var opened: Long? = null
    private lateinit var vm: LastDoneViewModel

    private fun show(vehicleId: Long) {
        vm = world.viewModel(LastDoneViewModel::class) {
            LastDoneViewModel(routeHandle(LastDoneRoute(vehicleId)), world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(), world.settings, world.actions)
        }
        compose.show { LastDoneScreen(onDone = { done++ }, onOpenRecord = { opened = it }, vm = vm) }
        compose.until { vm.state.value != null }
    }

    private fun records(vehicleId: Long) = runBlocking { world.db.service().observeRecords(vehicleId).first() }

    @Test
    fun `a date and mileage is entered for a reminder never logged, and a logged one opens its record`() {
        val id = world.vehicle(ScheduleFixtures.hatch(), firstReading = 50_000)
        val oil = world.item(ScheduleItemEntity(vehicleId = id, name = "Change oil", intervalMiles = 5_000, intervalMonths = 6))
        world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        val record = world.record(id, LocalDate.of(2026, 1, 10), 49_000, line("Change oil", itemId = oil))
        show(id)

        compose.onNodeWithText(text(R.string.last_done_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.last_done_pending, "1", "2")).assertIsDisplayed()
        compose.onNodeWithText("Change oil").performClick()
        assertEquals(record, opened)

        compose.onNodeWithText("Rotate tires").performClick()
        compose.onNodeWithText(text(R.string.last_done_sheet_intro, text(R.string.last_done_tag))).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.odometer_note_latest, 50_000.asDistance(unit))).assertExists()
        // Nothing typed: refused, and said why.
        compose.onAllNodesWithText(text(R.string.save)).onLast().performClick()
        compose.onNodeWithText(text(R.string.odometer_note_missing, unit.pluralWord)).assertExists()
        // Above the latest reading: allowed, but worth a second look.
        compose.onNode(hasSetTextAction()).performTextInput("52000")
        compose.onNodeWithText(text(R.string.odometer_note_higher, 50_000.asDistance(unit))).assertExists()
        compose.onNode(hasSetTextAction()).performTextReplacement("48,000")
        compose.onAllNodesWithText(text(R.string.save)).onLast().performClick()

        compose.until { records(id).size == 2 }
        val entered = records(id).first { it.record.id != record }
        assertTrue(entered.record.enteredAsLastDone)
        assertEquals(48_000, entered.record.odometer)
        assertEquals(LocalDate.now(), entered.record.date)
        compose.waitFor(text(R.string.last_done_all, "2"))
        compose.onNodeWithText(text(R.string.details_entered_by_you)).assertExists()

        compose.onNodeWithText(text(R.string.done)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        assertEquals(2, done)
    }

    @Test
    fun `cancelling the sheet saves nothing`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        show(id)
        compose.onNodeWithText("Rotate tires").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("12000")
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(text(R.string.last_done_tag), substring = true).assertDoesNotExist()
        assertTrue(records(id).isEmpty())
    }

    @Test
    fun `with nothing tracked there is nothing to enter`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null, tracked = false))
        show(id)
        compose.onNodeWithText(text(R.string.home_none_tracked)).assertIsDisplayed()
    }
}
