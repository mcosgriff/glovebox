package io.cosgriff.glovebox.ui.reminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.LeadTimeInput
import io.cosgriff.glovebox.ui.schedule.ScheduleFixtures
import io.cosgriff.glovebox.ui.schedule.ScheduleWorld
import io.cosgriff.glovebox.ui.schedule.eventually
import io.cosgriff.glovebox.ui.schedule.line
import io.cosgriff.glovebox.ui.schedule.plural
import io.cosgriff.glovebox.ui.schedule.show
import io.cosgriff.glovebox.ui.schedule.text
import io.cosgriff.glovebox.ui.schedule.until
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.ruleInSentence
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.service.titleWord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import io.cosgriff.glovebox.data.log.VehicleLogs

/** Reminder rules over a real database and the app's settings, through its ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class ReminderRulesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val world = ScheduleWorld()
    private val today = LocalDate.now()
    private val unit = DistanceUnit.MILES

    @After fun close() {
        // The settings store outlives the test; put back what the others expect.
        runBlocking {
            world.settings.setDefaultRule(DueRule.WHICHEVER_FIRST)
            world.settings.setLeadTime(LeadTime())
        }
        world.close()
    }

    private lateinit var vm: ReminderRulesViewModel

    private fun open(): ReminderRulesViewModel {
        vm = world.viewModel(ReminderRulesViewModel::class) {
            ReminderRulesViewModel(VehicleLogs(world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(), world.settings), world.db.schedule(), world.settings)
        }
        return vm
    }

    private fun show() {
        open()
        compose.show { ReminderRulesScreen(onBack = {}, vm = vm) }
        compose.until { vm.rules.value != null }
    }

    private fun pick(rule: DueRule) {
        compose.onNodeWithText(rule.ruleLabel()).performScrollTo().performClick()
        compose.until { vm.rules.value?.defaultRule == rule }
    }

    /** The words of [id] before its first argument: enough to know which line is shown when the rest is a date. */
    private fun opening(id: Int) = world.context.resources.getString(id).substringBefore("%").trim()

    private fun shows(text: String, substring: Boolean = false) = compose.onNodeWithText(text, substring = substring).performScrollTo().assertIsDisplayed()

    /** An oil change every 5,000 mi or 6 months, done [daysAgo] at 40,000, with the odometer at [now] today. */
    private fun oilChange(daysAgo: Long, now: Int?, vehicle: VehicleEntity = ScheduleFixtures.hatch()): Long {
        val id = world.vehicle(vehicle)
        val oil = world.item(ScheduleItemEntity(vehicleId = id, name = "Change oil", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6))
        world.record(id, today.minusDays(daysAgo), 40_000, line("Change oil", itemId = oil))
        now?.let { world.reading(id, today, it) }
        return id
    }

    @Test
    fun `with nothing logged there is no preview, and no sample is shown in its place`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        // Logged, but with only a mileage interval: not a worked example.
        val rotate = world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = null))
        world.record(id, today.minusDays(10), 10_000, line("Rotate tires", itemId = rotate))
        // Both intervals, but never logged.
        world.item(ScheduleItemEntity(vehicleId = id, name = "Change oil", intervalMiles = 5_000, intervalMonths = 6))
        show()
        assertNull(vm.rules.value!!.preview)
        shows(text(R.string.rules_preview_none))
        shows(text(R.string.rules_every_service))
    }

    @Test
    fun `the oil change is the example when it qualifies, named with its vehicle when there are several`() {
        val truck = VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", nickname = "Truck", createdOn = ScheduleFixtures.ADDED)
        val t = world.vehicle(truck)
        val brakes = world.item(ScheduleItemEntity(vehicleId = t, name = "Inspect brakes", intervalMiles = 15_000, intervalMonths = 12))
        world.record(t, today.minusDays(5), 20_000, line("Inspect brakes", itemId = brakes))
        oilChange(daysAgo = 30, now = 41_000)
        // A sold vehicle's reminders aren't listed.
        val sold = world.vehicle(VehicleEntity(vin = null, year = 2001, make = "Honda", model = "Civic", createdOn = ScheduleFixtures.ADDED))
        world.item(ScheduleItemEntity(vehicleId = sold, name = "Sold oil", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6))
        runBlocking { world.db.vehicles().setArchived(sold, today) }
        open()
        world.keep(vm.rules)
        eventually { vm.rules.value != null }
        val rules = vm.rules.value!!
        assertEquals("Change oil · ${ScheduleFixtures.hatch().shortName()}", rules.preview!!.title)
        assertEquals(41_000, rules.preview!!.odometer)
        assertEquals(setOf("Inspect brakes · ${truck.shortName()}", "Change oil · ${ScheduleFixtures.hatch().shortName()}"), rules.items.map { it.label }.toSet())
    }

    @Test
    fun `after a cluster replacement the example counts in the new dash and reads back as the old one showed`() {
        // The old cluster read 40,500 when it came out 20 days ago; the new one started at 100.
        val swapped = ScheduleFixtures.hatch().copy(clusterSwappedOn = today.minusDays(20), clusterStartReading = 100, odometerOffset = 40_400)
        oilChange(daysAgo = 30, now = 600, vehicle = swapped)
        open()
        world.keep(vm.rules)
        eventually { vm.rules.value != null }
        val preview = vm.rules.value!!.preview!!
        assertEquals(40_000 - 40_400, preview.last.odometer)
        assertEquals(40_000, preview.lastRead.odometer)
        assertEquals(600, preview.odometer)
    }

    /** What the preview's example counts by, for an oil change every 10,000 mi and [intervalMonths] on [vehicle]. */
    private fun previewOf(vehicle: VehicleEntity, intervalMonths: Int?): PreviewSource {
        val id = world.vehicle(vehicle)
        val oil = world.item(ScheduleItemEntity(vehicleId = id, name = "Change oil", templateKey = "oil", intervalMiles = 10_000, intervalMonths = intervalMonths))
        world.record(id, today.minusDays(30), 40_000, line("Change oil", itemId = oil))
        world.reading(id, today, 41_000)
        open()
        world.keep(vm.rules)
        eventually { vm.rules.value?.preview != null }
        return vm.rules.value!!.preview!!
    }

    @Test
    fun `the example counts by the owner's own oil interval, as the reminder does`() {
        val preview = previewOf(ScheduleFixtures.hatch().copy(ownOilMiles = 6_000, ownOilMonths = 6), intervalMonths = 12)
        assertEquals(Interval(6_000, 6), preview.interval)
        assertEquals(false, preview.monthsWorkedOut)
    }

    @Test
    fun `an oil change in miles alone is an example, with Glovebox's months said to be theirs`() {
        val preview = previewOf(ScheduleFixtures.hatch(), intervalMonths = null)
        assertEquals(Interval(10_000, 12), preview.interval)
        assertEquals(true, preview.monthsWorkedOut)
        compose.show { ReminderRulesScreen(onBack = {}, vm = vm) }
        shows(text(R.string.interval_months_ours, plural(R.plurals.months, 12, "12")), substring = true)
    }

    @Test
    fun `for a heavy driver mileage comes first, and each rule says when and why`() {
        oilChange(daysAgo = 30, now = 44_000)
        show()
        shows(text(R.string.rules_preview_of, "CHANGE OIL"))
        shows(text(R.string.rules_bar_mileage, 45_000.asDistance(unit)))

        pick(DueRule.WHICHEVER_FIRST)
        shows(text(R.string.rules_mileage_first, 45_000.asDistance(unit)))
        pick(DueRule.WHICHEVER_LAST)
        shows(opening(R.string.rules_time_last_est), substring = true)
        shows(text(R.string.rules_can_exceed))
        pick(DueRule.MILEAGE_ONLY)
        shows(opening(R.string.rules_est_around), substring = true)
        pick(DueRule.TIME_ONLY)
        shows(plural(R.plurals.rules_after_last, 6, "6"))
        shows(opening(R.string.rules_pace), substring = true)
    }

    @Test
    fun `for a light driver time comes first`() {
        oilChange(daysAgo = 150, now = 40_500)
        show()
        pick(DueRule.WHICHEVER_FIRST)
        shows(opening(R.string.rules_time_first), substring = true)
        pick(DueRule.WHICHEVER_LAST)
        shows(text(R.string.rules_mileage_last, 45_000.asDistance(unit)))
    }

    @Test
    fun `with the mileage passed it says so`() {
        oilChange(daysAgo = 30, now = 46_000)
        show()
        shows(text(R.string.rules_reached))
        pick(DueRule.MILEAGE_ONLY)
        shows(text(R.string.rules_mileage_passed))
        pick(DueRule.WHICHEVER_FIRST)
        shows(text(R.string.due_now))
    }

    @Test
    fun `with no driving pace yet nothing is estimated`() {
        oilChange(daysAgo = 30, now = null)
        show()
        shows(text(R.string.rules_no_pace))
        shows(text(R.string.rules_no_estimate))
        pick(DueRule.WHICHEVER_FIRST)
        shows(text(R.string.rules_first_no_pace))
        pick(DueRule.WHICHEVER_LAST)
        shows(text(R.string.rules_last_no_pace))
        pick(DueRule.MILEAGE_ONLY)
        shows(text(R.string.rules_no_date))
    }

    @Test
    fun `remind early is saved once both boxes hold whole numbers within the limits`() {
        show()
        val distance = compose.onNode(hasSetTextAction() and hasText(text(R.string.rules_distance_before, unit.titleWord)))
        distance.performScrollTo().performTextReplacement("99999")
        shows(text(R.string.rules_lead_error, LeadTimeInput.MAX_MILES.asDistance(unit), LeadTimeInput.MAX_DAYS.toString()))
        distance.performTextReplacement("800")
        compose.onNode(hasSetTextAction() and hasText(text(R.string.rules_days_before))).performTextReplacement("21")
        compose.until { runBlocking { world.settings.settings.first() }.lead == LeadTime(800, 21) }
        shows(text(R.string.rules_lead_note))
    }

    @Test
    fun `a service is given a rule of its own, and put back on the default`() {
        val id = oilChange(daysAgo = 30, now = 41_000)
        val brakes = world.item(ScheduleItemEntity(vehicleId = id, name = "Inspect brakes", intervalMiles = 15_000, intervalMonths = 12))
        show()
        compose.onNodeWithText(text(R.string.rules_add_override)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.rules_override_which)).assertIsDisplayed()
        compose.onNodeWithText("Inspect brakes").performClick()
        // The dialog's choice, over the default rule's own button.
        compose.onAllNodesWithText(DueRule.TIME_ONLY.ruleLabel()).onLast().performClick()
        compose.until { world.items(id).first { it.id == brakes }.ruleOverride == DueRule.TIME_ONLY.name }

        shows(text(R.string.rules_everything_else))
        compose.onNodeWithText(text(R.string.rules_override_value, DueRule.TIME_ONLY.ruleLabel())).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.rules_use_default, DueRule.WHICHEVER_FIRST.ruleInSentence())).performClick()
        compose.until { world.items(id).first { it.id == brakes }.ruleOverride == null }
        compose.onNodeWithContentDescription(text(R.string.back)).assertIsDisplayed()
    }
}
