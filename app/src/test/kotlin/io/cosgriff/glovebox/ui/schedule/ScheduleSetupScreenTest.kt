package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.ui.ScheduleSetupRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** First-time setup over a real database, through its ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class ScheduleSetupScreenTest {
    @get:Rule val compose = createComposeRule()
    private val world = ScheduleWorld()

    @After fun close() = world.close()

    private var done = 0
    private var lastDone: Long? = null
    private lateinit var vm: ScheduleSetupViewModel

    private fun show(vehicle: VehicleEntity): Long {
        val id = world.vehicle(vehicle)
        vm = world.viewModel(ScheduleSetupViewModel::class) {
            ScheduleSetupViewModel(routeHandle(ScheduleSetupRoute(id)), world.db.vehicles(), world.settings, world.makers)
        }
        compose.show { ScheduleSetupScreen(onDone = { done++ }, onEnterLastDone = { lastDone = it }, vm = vm) }
        compose.until { vm.items != null }
        return id
    }

    private fun track(count: Int) = compose.onNodeWithText(plural(R.plurals.setup_track, count, count.toString()))

    @Test
    fun `the typical list is tracked as shown, and the owner goes on to the last-done list when they said so`() {
        val id = show(ScheduleFixtures.hatch())
        compose.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_typical_note)).assertIsDisplayed()
        val items = vm.items!!
        compose.onNodeWithText(text(R.string.setup_selected, items.size.toString(), items.size.toString())).assertIsDisplayed()

        // Untick one from its row.
        compose.onNodeWithText(items.first().name).performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText(text(R.string.setup_selected, (items.size - 1).toString(), items.size.toString())).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.setup_last_done_as_i_go)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.setup_last_done_next)).performClick()
        track(items.size - 1).performScrollTo().performClick()
        compose.until { lastDone != null }
        assertEquals(id, lastDone)
        assertEquals(0, done)
        val rows = world.items(id)
        assertEquals(items.size, rows.size)
        assertEquals(1, rows.count { !it.tracked })
    }

    @Test
    fun `with nothing ticked nothing can be tracked, and skipping saves nothing`() {
        val id = show(ScheduleFixtures.hatch())
        val n = vm.items!!.size
        compose.onNodeWithText(text(R.string.clear_all)).performScrollTo().performClick()
        track(0).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.select_all)).performScrollTo().performClick()
        track(n).performScrollTo()

        compose.onNodeWithText(text(R.string.setup_skip)).performScrollTo().performClick()
        assertEquals(1, done)
        assertTrue(world.items(id).isEmpty())
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        assertEquals(2, done)
    }

    @Test
    fun `the maker's schedule is named, severe use switches it, and tracking ends on done`() {
        val id = show(ScheduleFixtures.fourRunner())
        val known = vm.known!!
        compose.onNodeWithText(text(R.string.details_schedule)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_known_note, known.source.note)).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.use_severe)).performScrollTo().performClick()
        compose.until { vm.severeUse }
        assertTrue(vm.items!!.all { it.useSevere })
        // A job with a different severe interval is marked as on it.
        assertTrue(compose.onAllNodesWithText(text(R.string.setup_severe_tag)).fetchSemanticsNodes().isNotEmpty())

        val count = vm.items!!.count { it.tracked && !it.dormant }
        track(count).performScrollTo().performClick()
        compose.until { done == 1 }
        assertTrue(world.items(id).all { it.knownList == known.id })
        assertTrue(runBlocking { world.db.milestones().count(id) } > 0)
    }

    @Test
    fun `the engine question picks the maker's list, and E85 is switched for a flex-fuel one`() {
        show(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Sequoia", createdOn = ScheduleFixtures.ADDED))
        compose.onNodeWithText(text(R.string.setup_engine_title)).assertIsDisplayed()
        // No flex-fuel list yet, so no E85 switch.
        compose.onNodeWithText(text(R.string.schedule_e85)).assertDoesNotExist()
        compose.onNodeWithText("5.7L V8 flex-fuel (FFV)").performScrollTo().performClick()
        compose.until { vm.known != null }
        compose.onNodeWithText(text(R.string.setup_engine_title)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.schedule_e85)).performScrollTo().performClick()
        compose.until { vm.e85Use }
        assertTrue(vm.items!!.filter { it.e85 != null }.all { it.useE85 })
        assertTrue(compose.onAllNodesWithText(text(R.string.setup_e85_tag)).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `"neither" keeps the typical list`() {
        show(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Tacoma", createdOn = ScheduleFixtures.ADDED))
        compose.onNodeWithText(text(R.string.setup_engine_neither)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.setup_engine_title)).assertDoesNotExist()
        assertNull(vm.known)
        compose.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
    }

    @Test
    fun `an item is edited in the editor, and one of the user's own is added`() {
        show(ScheduleFixtures.hatch())
        val first = vm.items!!.first()
        compose.onNodeWithContentDescription(text(R.string.edit_item, first.name)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.item_edit_title)).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.close)).performClick()
        compose.onNodeWithText(text(R.string.item_edit_title)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.schedule_add_own)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.item_add_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.documents_name)).performTextInput("Grease hitch")
        compose.onNodeWithText(text(R.string.item_months)).performTextInput("4")
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.onNodeWithText("Grease hitch").performScrollTo().assertIsDisplayed()
        assertEquals(4, vm.items!!.last().normal.months)

        // Stop tracking from the editor unticks it.
        compose.onNodeWithContentDescription(text(R.string.edit_item, "Grease hitch")).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.item_stop_tracking)).performScrollTo().performClick()
        compose.onNodeWithText("Grease hitch").performScrollTo().assertIsOff()
    }
}
