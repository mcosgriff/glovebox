package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.MilestoneEntity
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.MilestoneExport
import io.cosgriff.glovebox.ui.MilestonesRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.titleWord
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
import java.time.LocalDate

/**
 * The Milestones screen over a real database, through its ViewModel. On the default screen size, since the
 * repeat dialog's text field never settles on the tall one under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class MilestonesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val world = ScheduleWorld()
    private val unit = DistanceUnit.MILES

    @After fun close() = world.close()

    private var opened: Long? = null
    private lateinit var vm: MilestonesViewModel

    private fun show(vehicleId: Long, loads: Boolean = true) {
        vm = world.viewModel(MilestonesViewModel::class) {
            MilestonesViewModel(
                routeHandle(MilestonesRoute(vehicleId)), world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(),
                world.settings, world.db.milestones(), world.saver, world.catalog,
            )
        }
        compose.show { MilestonesScreen(onBack = {}, onOpenRecord = { opened = it }, vm = vm) }
        if (loads) compose.until { vm.state.value != null }
    }

    private fun scrollTo(matcher: SemanticsMatcher) = compose.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)

    private fun scrollTo(text: String) = scrollTo(hasText(text))

    private fun table(vehicleId: Long, vararg rows: Pair<Int, List<String>>) = runBlocking {
        rows.forEach { (at, jobs) ->
            world.db.milestones().save(MilestoneEntity(vehicleId = vehicleId, atMiles = at), jobs.map { MilestoneJobEntity(milestoneId = 0, name = it) })
        }
    }

    private fun tableRows(vehicleId: Long) = runBlocking { world.db.milestones().tableFor(vehicleId) }

    @Test
    fun `the schedule laid out by mileage shows what was done, folds the empty stretch and goes further on request`() {
        val id = world.vehicle(ScheduleFixtures.hatch(), firstReading = 40_000)
        val rotation = world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        world.item(ScheduleItemEntity(vehicleId = id, name = "Change oil", intervalMiles = 10_000, intervalMonths = 12))
        val done = LocalDate.of(2024, 5, 1)
        val record = world.record(id, done, 5_000, line("Rotate tires", itemId = rotation))
        show(id)

        // It opens on the next milestone, with the past above it.
        scrollTo(text(R.string.milestones_intro_generated))
        compose.onNodeWithText(text(R.string.milestones_next_in, 5_000.asDistance(unit).uppercase())).assertExists()
        // 10,000 to 40,000 had nothing logged: one line, not seven cards.
        val folded = text(R.string.milestones_folded, "10,000", 40_000.asDistance(unit), "7")
        scrollTo(folded)
        compose.onNodeWithText(folded).performClick()
        scrollTo(20_000.asDistance(unit))

        // The 5,000 card is closed; opening it shows the record, which opens.
        scrollTo(5_000.asDistance(unit))
        compose.onNodeWithText(5_000.asDistance(unit)).performClick()
        val completed = text(R.string.milestones_completed, done.format(longDate), 5_000.asDistance(unit))
        scrollTo(completed)
        compose.onNodeWithText(completed).performClick()
        assertEquals(record, opened)
        // Closing it again hides the job.
        compose.onNodeWithText(5_000.asDistance(unit)).performClick()
        compose.onNodeWithText(completed).assertDoesNotExist()

        scrollTo(text(R.string.expand_all))
        compose.onNodeWithText(text(R.string.expand_all)).performClick()
        scrollTo(completed)
        compose.onNodeWithText(text(R.string.collapse_all)).performClick()
        compose.onNodeWithText(completed).assertDoesNotExist()

        val before = vm.state.value!!.milestones.size
        scrollTo(text(R.string.milestones_further))
        compose.onNodeWithText(text(R.string.milestones_further)).performClick()
        compose.until { vm.state.value!!.milestones.size > before }
    }

    @Test
    fun `with nothing repeating by mileage there are no milestones to build`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        world.item(ScheduleItemEntity(vehicleId = id, name = "Brake fluid", intervalMiles = null, intervalMonths = 36))
        show(id)
        compose.waitFor(text(R.string.milestones_none))
        compose.onNodeWithText(text(R.string.expand_all)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.milestones_further)).assertDoesNotExist()
    }

    @Test
    fun `a table of the user's own starts from an empty editor and is then shown in place of the schedule's`() {
        val id = world.vehicle(ScheduleFixtures.hatch(), firstReading = 1_000)
        world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        show(id)
        scrollTo(text(R.string.milestones_make_own))
        compose.onNodeWithText(text(R.string.milestones_make_own)).performClick()
        compose.onNodeWithText(text(R.string.milestone_add_title)).assertIsDisplayed()
        // Empty, never a copy of the generated view.
        compose.onNode(hasSetTextAction() and hasText(unit.titleWord)).assert(hasText(""))
        compose.onNode(hasSetTextAction() and hasText(unit.titleWord)).performTextInput("30000")
        compose.onNode(hasSetTextAction() and hasText(text(R.string.milestone_job))).performTextInput("Inspect brakes")
        compose.onNodeWithText(text(R.string.save)).performClick()

        compose.until { tableRows(id).isNotEmpty() }
        assertEquals(30_000, tableRows(id).single().milestone.atMiles)
        compose.until { vm.state.value?.fromTable == true }
        scrollTo(text(R.string.milestones_intro_table))
    }

    @Test
    fun `the user's table is edited, repeated, saved as a file and removed`() {
        val id = world.vehicle(ScheduleFixtures.hatch(), firstReading = 12_000)
        table(id, 15_000 to listOf("Replace cabin filter"), 30_000 to listOf("Inspect brakes", "Replace engine air filter"))
        // Logged under the job's own name, so it completes the job.
        val record = world.record(id, LocalDate.of(2025, 3, 1), 14_000, line("Replace cabin filter"))
        show(id)
        scrollTo(text(R.string.milestones_intro_table))

        // The next milestone is open: its job is done, from that record.
        compose.onNodeWithContentDescription(text(R.string.milestones_all_completed)).assertExists()
        compose.onAllNodesWithText(text(R.string.milestones_edit)).onFirst().performClick()
        compose.onNodeWithText(text(R.string.milestone_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.milestone_or_months)).performTextInput("12")
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.until { tableRows(id).first { it.milestone.atMiles == 15_000 }.milestone.orMonths == 12 }
        assertNull(vm.editing)

        // Repeat every 30,000.
        scrollTo(text(R.string.milestones_no_repeat))
        compose.onNodeWithText(text(R.string.milestones_no_repeat)).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("30000")
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.until { runBlocking { world.db.vehicles().byId(id) }!!.milestonesRepeatEveryMiles == 30_000 }
        scrollTo(text(R.string.milestones_intro_table_repeats, 30_000.asDistance(unit)))
        // The second round says what it repeats.
        compose.onNodeWithText(text(R.string.expand_all)).performClick()
        scrollTo(text(R.string.milestones_edit_repeat, 15_000.asDistance(unit)))

        // Saved as a file under the table's name: nothing in it is left out, so with no question first.
        var asked: String? = null
        world.saver.attach { _, name -> asked = name }
        scrollTo(text(R.string.save_file_button))
        compose.onNodeWithText(text(R.string.save_file_button)).performClick()
        compose.until { asked != null }
        assertEquals(MilestoneExport.fileName("2008-volkswagen-golf"), asked)

        // A milestone is deleted from its editor.
        scrollTo(text(R.string.milestones_edit_repeat, 15_000.asDistance(unit)))
        compose.onAllNodesWithText(text(R.string.milestones_edit)).onFirst().performClick()
        compose.onNodeWithText(text(R.string.milestone_delete)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.delete)).performClick()
        compose.until { tableRows(id).size == 1 }

        // And the table goes, once confirmed.
        scrollTo(text(R.string.milestones_remove))
        compose.onNodeWithText(text(R.string.milestones_remove)).performClick()
        compose.onNodeWithText(text(R.string.milestones_remove_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(1, tableRows(id).size)
        compose.onNodeWithText(text(R.string.milestones_remove)).performClick()
        compose.onNodeWithText(text(R.string.remove)).performClick()
        compose.until { tableRows(id).isEmpty() }
        compose.until { vm.state.value?.fromTable == false }
        scrollTo(text(R.string.milestones_intro_generated))
        assertTrue(record > 0)
    }

    @Test
    fun `the maker's table is offered while there is none, and is then named beside it`() {
        val id = world.vehicle(ScheduleFixtures.fourRunner(), firstReading = 20_000)
        show(id)
        val known = world.catalog.let { runBlocking { it.lists() } }.first { it.id == "toyota-4runner-2017-2024" }
        val offer = text(R.string.milestones_use_known, known.title)
        scrollTo(offer)
        compose.onNodeWithText(offer).performClick()
        compose.until { tableRows(id).isNotEmpty() }
        assertEquals(known.milestones.size, tableRows(id).size)
        compose.until { vm.state.value?.fromTable == true }
        scrollTo(text(R.string.milestones_intro_known, startedFrom("${known.source.name}, ${known.title}")))
    }

    @Test
    fun `with a replaced cluster the figures are actual mileage, with what the dash will read`() {
        val swapped = LocalDate.of(2025, 1, 1)
        val id = world.vehicle(
            VehicleEntity(vin = null, year = 2013, make = "Volkswagen", model = "Golf", createdOn = ScheduleFixtures.ADDED, odometerOffset = 10_000, clusterSwappedOn = swapped, clusterStartReading = 0),
        )
        world.reading(id, LocalDate.of(2026, 2, 1), 25_000)
        table(id, 40_000 to listOf("Replace spark plugs"))
        show(id)
        compose.waitFor(text(R.string.milestones_actual, 35_000.asDistance(unit)), substring = true)
        compose.onNodeWithText(text(R.string.milestones_on_dash, 30_000.asDistance(unit)), substring = true).assertExists()
    }

    @Test
    fun `a vehicle that no longer exists shows nothing`() {
        show(404, loads = false)
        repeat(10) { compose.waitForIdle() }
        assertNull(vm.state.value)
    }
}
