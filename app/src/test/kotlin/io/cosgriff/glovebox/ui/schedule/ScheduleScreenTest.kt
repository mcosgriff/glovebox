package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.ui.ScheduleRoute
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Maintenance schedule screen over a real database, through its ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class ScheduleScreenTest {
    @get:Rule val compose = createComposeRule()
    private val world = ScheduleWorld()

    @After fun close() = world.close()

    private var setUp: Long? = null
    private var lastDone: Long? = null
    private var milestones: Long? = null
    private var opened: Long? = null

    private lateinit var vm: ScheduleViewModel

    private fun show(vehicleId: Long) {
        vm = world.viewModel(ScheduleViewModel::class) {
            ScheduleViewModel(
                routeHandle(ScheduleRoute(vehicleId)), world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(),
                world.settings, world.actions, world.db.milestones(), world.saver, world.catalog,
            )
        }
        compose.show {
            ScheduleScreen(
                onBack = {}, onOpenRecord = { opened = it }, onSetUp = { setUp = it }, onEnterLastDone = { lastDone = it },
                onOpenMilestones = { milestones = it }, vm = vm,
            )
        }
    }

    private fun typical(vehicleId: Long, vehicle: VehicleEntity): Map<String, Long> =
        ScheduleTemplates.generic(vehicle.drivetrain, vehicle.fuel, DistanceUnit.MILES).associate {
            it.key to world.item(
                ScheduleItemEntity(
                    vehicleId = vehicleId, name = it.label, templateKey = it.key, intervalMiles = it.normal.miles, intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles, severeMonths = it.severe?.months,
                ),
            )
        }

    @Test
    fun `the maker's schedule is offered, and a typical item unticked in the dialog is kept`() {
        val truck = ScheduleFixtures.fourRunner()
        val id = world.vehicle(truck, firstReading = 40_000)
        val typical = typical(id, truck)
        show(id)

        compose.waitFor(text(R.string.schedule_use_known))
        compose.onNodeWithText(text(R.string.schedule_use_known)).performScrollTo().performClick()
        val rotation = world.items(id).first { it.id == typical.getValue("rotation") }
        // Every double starts ticked to be replaced; the owner keeps the rotation.
        compose.onNodeWithText(text(R.string.schedule_replace, rotation.name)).performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText(text(R.string.schedule_known_added)).assertExists()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(text(R.string.schedule_known_added)).assertDoesNotExist()
        assertTrue(world.items(id).none { it.knownList != null })

        compose.onNodeWithText(text(R.string.schedule_use_known)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.schedule_replace, rotation.name)).performScrollTo().performClick()
        compose.onNode(androidx.compose.ui.test.hasText(text(R.string.schedule_known_title, "Toyota Warranty and Maintenance Guide"), substring = true)).assertIsDisplayed()
        val new = vm.state.value!!.known!!.second.new.size
        compose.onNodeWithText(plural(R.plurals.schedule_add_reminders, new, new.toString())).performClick()

        compose.until { world.items(id).any { it.knownList != null } }
        val rows = world.items(id)
        assertTrue(rows.first { it.id == typical.getValue("rotation") }.tracked)
        assertTrue(rows.none { it.id == typical.getValue("oil") })
    }

    @Test
    fun `an empty schedule for a model with a maker's list starts setup from it`() {
        val id = world.vehicle(ScheduleFixtures.fourRunner())
        show(id)
        compose.waitFor(text(R.string.schedule_empty_known))
        compose.onNodeWithText(text(R.string.schedule_start_known)).performClick()
        assertEquals(id, setUp)
        // Setup is offered only while there is nothing to insert twice.
        compose.onNodeWithText(text(R.string.schedule_use_known)).assertDoesNotExist()
    }

    @Test
    fun `an empty schedule with no maker's list starts from the typical one, and a table of the user's own is still reachable`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        runBlocking {
            world.db.milestones().addKnown(
                id, "mine",
                listOf(io.cosgriff.glovebox.data.db.MilestoneEntity(vehicleId = id, atMiles = 30_000, orMonths = null) to listOf(io.cosgriff.glovebox.data.db.MilestoneJobEntity(milestoneId = 0, name = "Inspect brakes", service = null))),
            )
        }
        show(id)
        compose.waitFor(text(R.string.schedule_empty))
        compose.onNodeWithText(text(R.string.schedule_start_typical)).performClick()
        assertEquals(id, setUp)
        compose.waitFor(text(R.string.milestones_title))
        compose.onNodeWithText(text(R.string.milestones_title)).performClick()
        assertEquals(id, milestones)
    }

    @Test
    fun `items are ticked and unticked from the list, and the driving use switch sets every item`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        val typical = typical(id, hatch)
        show(id)
        val rotation = world.items(id).first { it.id == typical.getValue("rotation") }
        compose.waitFor(rotation.name)
        compose.onNodeWithText(rotation.name).performScrollTo().performClick()
        compose.until { !world.items(id).first { it.id == rotation.id }.tracked }

        compose.onNodeWithText(text(R.string.use_severe)).performScrollTo().performClick()
        compose.until { world.items(id).filter { it.severeMiles != null || it.severeMonths != null }.all { it.useSevere } }
        // Some on severe and one not would read as mixed; here everything with a severe interval is on it.
        compose.onNodeWithText(text(R.string.schedule_mixed)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.select_all)).performScrollTo().performClick()
        compose.until { world.items(id).all { it.tracked } }
        compose.waitFor(text(R.string.clear_all))
        compose.onNodeWithText(text(R.string.clear_all)).performScrollTo().performClick()
        compose.until { world.items(id).none { it.tracked } }
    }

    @Test
    fun `items never logged lead to the last-done list, and the milestones row opens milestones`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch, firstReading = 20_000)
        typical(id, hatch)
        show(id)
        compose.waitFor(text(R.string.schedule_enter_last_done))
        compose.onNodeWithText(text(R.string.schedule_enter_last_done)).performScrollTo().performClick()
        assertEquals(id, lastDone)
        compose.onNodeWithText(text(R.string.milestones_title)).performScrollTo().performClick()
        assertEquals(id, milestones)
    }

    @Test
    fun `typical items a schedule lacks are added from the button under it`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        world.item(ScheduleItemEntity(vehicleId = id, name = "Grease hitch", intervalMiles = 3_000, intervalMonths = null))
        show(id)
        compose.waitFor("Grease hitch")
        val missing = ScheduleTemplates.generic(ScheduleFixtures.hatch().drivetrain, ScheduleFixtures.hatch().fuel, DistanceUnit.MILES).size
        compose.onNodeWithText(plural(R.plurals.schedule_add_typical, missing, missing.toString())).performScrollTo().performClick()
        compose.until { world.items(id).size == missing + 1 }
        assertTrue(world.items(id).filter { it.templateKey != null }.none { it.tracked })
    }

    @Test
    fun `an item is edited, and a last done entered from its editor is shown as entered by you`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch, firstReading = 20_000)
        val typical = typical(id, hatch)
        val rotation = world.items(id).first { it.id == typical.getValue("rotation") }
        show(id)
        compose.waitFor(rotation.name)
        compose.onNodeWithContentDescription(text(R.string.edit_item, rotation.name)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.item_edit_title)).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.item_enter_it)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.last_done_odometer, DistanceUnit.MILES.symbolWord)).performTextInput("18000")
        // The sheet's Save, over the editor's.
        compose.onAllNodesWithText(text(R.string.save)).onLast().performClick()
        compose.until { runBlocking { world.db.service().observeRecords(id).first() }.isNotEmpty() }
        val record = runBlocking { world.db.service().observeRecords(id).first() }.single()
        assertTrue(record.record.enteredAsLastDone)
        assertEquals(18_000, record.record.odometer)
        assertEquals(LocalDate.now(), record.record.date)

        compose.waitFor(text(R.string.details_entered_by_you))
        compose.onNodeWithText(text(R.string.details_entered_by_you)).performScrollTo().performClick()
        assertEquals(record.record.id, opened)

        // Stop tracking closes the editor and unticks the item.
        compose.onNodeWithText(text(R.string.item_stop_tracking)).performScrollTo().performClick()
        compose.until { !world.items(id).first { it.id == rotation.id }.tracked }
        compose.onNodeWithText(text(R.string.item_edit_title)).assertDoesNotExist()
    }

    @Test
    fun `an item of the user's own is added from the list`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        world.item(ScheduleItemEntity(vehicleId = id, name = "Grease hitch", intervalMiles = 3_000, intervalMonths = null))
        show(id)
        compose.waitFor(text(R.string.schedule_add_own))
        compose.onNodeWithText(text(R.string.schedule_add_own)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.item_add_title)).assertIsDisplayed()
        // No history for an item not saved yet.
        compose.onNodeWithText(text(R.string.item_enter_it)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.documents_name)).performTextInput("Wash underbody")
        compose.onNodeWithText(text(R.string.item_months)).performTextInput("3")
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.until { world.items(id).any { it.name == "Wash underbody" } }
        val row = world.items(id).first { it.name == "Wash underbody" }
        assertEquals(3, row.intervalMonths)
        assertEquals(null, row.intervalMiles)
        assertFalse(row.templateKey != null)
    }
}
