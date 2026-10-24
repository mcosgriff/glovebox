package io.cosgriff.glovebox.ui.reminders

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.notify.Notifier
import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.ui.schedule.ScheduleFixtures
import io.cosgriff.glovebox.ui.schedule.ScheduleWorld
import io.cosgriff.glovebox.ui.schedule.line
import io.cosgriff.glovebox.ui.schedule.show
import io.cosgriff.glovebox.ui.schedule.text
import io.cosgriff.glovebox.ui.schedule.until
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.ui.service.shortName
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Reminders tab across the garage, over a real database, through its ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class RemindersScreenTest {
    @get:Rule val compose = createComposeRule()
    private val world = ScheduleWorld()
    private val today = LocalDate.now()
    private val unit = DistanceUnit.MILES

    @After fun close() = world.close()

    private val logged = mutableListOf<Pair<Long, Long>>()
    private var added = 0
    private var rules = 0
    private var notifications = 0
    private val messages = mutableListOf<Pair<String, () -> Unit>>()
    private lateinit var vm: RemindersViewModel

    private fun show() {
        vm = world.viewModel(RemindersViewModel::class) {
            RemindersViewModel(
                GarageOverview(world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(), world.settings, world.db.recalls()),
                world.db.schedule(), Notifier(world.context),
            )
        }
        compose.show {
            RemindersScreen(
                onLogIt = { v, i -> logged += v to i }, onAddVehicle = { added++ }, onOpenRules = { rules++ },
                onOpenNotifications = { notifications++ }, onMessage = { message, _, action -> messages += message to action }, vm = vm,
            )
        }
        compose.until { vm.garage.value != null }
    }

    private fun scrollTo(matcher: SemanticsMatcher) = compose.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)

    private fun item(vehicleId: Long) = { id: Long -> world.items(vehicleId).first { it.id == id } }

    @Test
    fun `with no vehicle the tab says to add one`() {
        show()
        compose.onNodeWithText(text(R.string.reminders_empty_no_vehicle)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.garage_empty_add)).performClick()
        assertEquals(1, added)
        compose.onNodeWithText(text(R.string.reminders_rules)).performClick()
        assertEquals(1, rules)
    }

    @Test
    fun `with nothing tracked there is nothing to show`() {
        world.vehicle(ScheduleFixtures.hatch(), firstReading = 1_000)
        show()
        compose.onNodeWithText(text(R.string.reminders_empty_none_tracked)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.garage_empty_add)).assertDoesNotExist()
    }

    @Test
    fun `reminders are grouped across vehicles, filtered by vehicle, and one tap from Log service`() {
        val hatch = ScheduleFixtures.hatch()
        val a = world.vehicle(hatch, firstReading = 50_000)
        val rotate = world.item(ScheduleItemEntity(vehicleId = a, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        world.record(a, today.minusDays(200), 44_000, line("Rotate tires", itemId = rotate))
        world.item(ScheduleItemEntity(vehicleId = a, name = "Inspect wipers", intervalMiles = null, intervalMonths = 12))
        val truck = VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", nickname = "Truck", createdOn = ScheduleFixtures.ADDED)
        val b = world.vehicle(truck, firstReading = 30_000)
        val fluid = world.item(ScheduleItemEntity(vehicleId = b, name = "Replace brake fluid", intervalMiles = null, intervalMonths = 36))
        world.record(b, today.minusMonths(36).plusDays(10), 10_000, line("Replace brake fluid", itemId = fluid))
        show()

        compose.onNodeWithText(text(R.string.reminders_group_overdue)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.reminders_over, 1_000.asDistance(unit))).assertIsDisplayed()
        scrollTo(hasText(text(R.string.reminders_group_next_30)))
        scrollTo(hasText(text(R.string.reminders_group_not_logged)))
        scrollTo(hasText(text(R.string.item_log_once)))
        scrollTo(hasText("Inspect wipers"))

        // Notifications are off in the test, so the tab says so once there is something to tell.
        scrollTo(hasText(text(R.string.reminders_notifications_off)))
        compose.onNodeWithText(text(R.string.reminders_notifications_off)).performClick()
        assertEquals(1, notifications)

        scrollTo(hasText("Rotate tires"))
        compose.onNodeWithContentDescription(text(R.string.reminders_log_it_for, "Rotate tires", hatch.shortName())).performClick()
        assertEquals(listOf(a to rotate), logged)

        // Only the truck.
        compose.onNodeWithText("Truck").performClick()
        compose.onNodeWithText("Truck").assertIsSelected()
        compose.onNodeWithText("Rotate tires").assertDoesNotExist()
        scrollTo(hasText("Replace brake fluid"))
        compose.onNodeWithText(text(R.string.home_all_vehicles)).performClick()
        scrollTo(hasText("Rotate tires"))
    }

    @Test
    fun `a due reminder is snoozed from its sheet, and the snackbar's Undo puts it back`() {
        val hatch = ScheduleFixtures.hatch()
        val a = world.vehicle(hatch, firstReading = 50_000)
        val rotate = world.item(ScheduleItemEntity(vehicleId = a, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        world.record(a, today.minusDays(200), 44_000, line("Rotate tires", itemId = rotate))
        // An oil change logged recently, and not due: the reminder can wait for it.
        val oil = world.item(ScheduleItemEntity(vehicleId = a, name = "Change oil", templateKey = "oil", intervalMiles = 7_500, intervalMonths = 12))
        world.record(a, today.minusDays(30), 48_000, line("Change oil", itemId = oil))
        val read = item(a)
        show()

        compose.onNodeWithContentDescription(text(R.string.reminders_snooze_for, "Rotate tires", hatch.shortName())).performClick()
        compose.onNodeWithText(text(R.string.snooze_title, "Rotate tires")).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.snooze_two_weeks)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.snooze_one_month)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.snooze_with_oil)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.snooze_distance, "1,000", unit.pluralWord)).performClick()
        compose.onNodeWithText(text(R.string.snooze)).performClick()

        compose.until { read(rotate).snoozedUntilOdometer == 51_000 }
        assertEquals(text(R.string.reminders_snoozed_until, "Rotate tires", 51_000.asDistance(unit)), messages.last().first)
        messages.last().second()
        compose.until { read(rotate).snoozedUntilOdometer == null }

        // Snoozed again, it can be woken from its card, and that too can be undone.
        val snoozeIt = text(R.string.reminders_snooze_for, "Rotate tires", hatch.shortName())
        compose.until { compose.onAllNodesWithContentDescription(snoozeIt).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(text(R.string.reminders_snooze_for, "Rotate tires", hatch.shortName())).performClick()
        compose.onNodeWithText(text(R.string.snooze_with_oil)).performClick()
        compose.onNodeWithText(text(R.string.snooze)).performClick()
        compose.until { read(rotate).snoozedUntilOdometer != null }
        val withOil = read(rotate).snoozedUntilOdometer
        compose.until { compose.onAllNodes(hasText(text(R.string.reminders_end_snooze))).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(text(R.string.reminders_end_snooze_for, "Rotate tires", hatch.shortName())).performClick()
        compose.until { read(rotate).snoozedUntilOdometer == null }
        assertEquals(text(R.string.reminders_unsnoozed, "Rotate tires"), messages.last().first)
        messages.last().second()
        compose.until { read(rotate).snoozedUntilOdometer == withOil }
    }

    @Test
    fun `skip waits for the next interval, and already done goes to Log service`() {
        val hatch = ScheduleFixtures.hatch()
        val a = world.vehicle(hatch, firstReading = 50_000)
        val rotate = world.item(ScheduleItemEntity(vehicleId = a, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        world.record(a, today.minusDays(200), 44_000, line("Rotate tires", itemId = rotate))
        val read = item(a)
        show()

        compose.onNodeWithContentDescription(text(R.string.reminders_snooze_for, "Rotate tires", hatch.shortName())).performClick()
        compose.onNodeWithText(text(R.string.snooze_with_oil)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.snooze_skip, 54_000.asDistance(unit))).performClick()
        compose.until { read(rotate).snoozedUntilOdometer == 54_000 }
        assertEquals(text(R.string.reminders_skipped_until, "Rotate tires", 54_000.asDistance(unit)), messages.last().first)
        messages.last().second()
        compose.until { read(rotate).snoozedUntilOdometer == null }

        // The sheet is still closing after the Undo; wait until the row's snooze button can be tapped again.
        val snoozeRotate = text(R.string.reminders_snooze_for, "Rotate tires", hatch.shortName())
        compose.until { compose.onAllNodesWithContentDescription(snoozeRotate).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(snoozeRotate).performClick()
        compose.onNodeWithText(text(R.string.snooze_already_done)).performClick()
        assertEquals(listOf(a to rotate), logged)
        compose.onNodeWithText(text(R.string.snooze_title, "Rotate tires")).assertDoesNotExist()
        assertNull(read(rotate).snoozedUntilDate)
    }
}
