package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.data.log.VehicleLogs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.VehicleHomeRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Vehicle home with its real ViewModel: the reminders, each one tap from Log service, and the Update mileage sheet. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class VehicleHomeScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val unit = DistanceUnit.MILES
    private val today = LocalDate.now()
    private val events = mutableListOf<String>()
    private val messages = mutableListOf<Pair<String, () -> Unit>>()
    private var dash by mutableStateOf<Int?>(null)

    private fun show(id: Long, updateMileage: Boolean = false, until: String) {
        val vm = g.viewModel {
            VehicleHomeViewModel(
                routeHandle(VehicleHomeRoute(id, updateMileage)), VehicleLogs(g.db.vehicles(), g.db.schedule(), g.db.service(), g.db.odometer(), g.settings),
                g.db.odometer(), g.db.recalls(), g.recallRepository, g.db.recommendations(),
            )
        }
        compose.setContent {
            GloveboxTheme {
                VehicleHomeScreen(
                    onBack = { events += "back" },
                    onLogService = { events += "log $it" },
                    onLogItem = { v, item -> events += "log $v item $item" },
                    onOpenRules = { events += "rules" },
                    onOpenRecalls = { events += "recalls $it" },
                    onOpenDetails = { events += "details $it" },
                    onEnterLastDone = { events += "last done $it" },
                    onReadDash = { last, u -> events += "dash $last $u" },
                    dashReading = dash,
                    onDashReadingUsed = { dash = null; events += "dash used" },
                    onMessage = { m, _, undo -> messages += m to undo },
                    vm = vm,
                )
            }
        }
        compose.awaitThat { compose.onAllNodes(hasText(until, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun key(text: String) = compose.onNode(hasText(text) and hasClickAction()).click()
    private fun type(digits: String) = digits.forEach { key(it.toString()) }
    private fun readings(id: Long) = runBlocking { g.db.odometer().readingsFor(id) }.map { it.odometer }.sorted()

    /** The wagon with an oil change every 5,000 mi logged at 58,000 and a reading of 60,000; the tire rotation never logged. */
    private fun wagon(): Pair<Long, Pair<Long, Long>> {
        val id = g.add(VehicleFixtures.wagon)
        return runBlocking {
            g.db.schedule().insertAll(
                listOf(
                    ScheduleItemEntity(vehicleId = id, name = "Oil change", intervalMiles = 5_000, intervalMonths = 12),
                    ScheduleItemEntity(vehicleId = id, name = "Tire rotation", intervalMiles = 5_000, intervalMonths = 6),
                ),
            )
            val items = g.db.schedule().allFor(id)
            val oil = items.first { it.name == "Oil change" }.id
            val rotation = items.first { it.name == "Tire rotation" }.id
            g.db.service().insertWithItems(
                ServiceRecordEntity(vehicleId = id, date = today.minusMonths(2), odometer = 58_000, diy = true),
                listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = oil, label = "Oil change")),
            )
            g.db.odometer().insert(OdometerReadingEntity(vehicleId = id, date = today.minusDays(3), odometer = 60_000))
            id to (oil to rotation)
        }
    }

    @Test
    fun `each reminder is one tap from Log service with its job ticked`() {
        val (id, items) = wagon()
        show(id, until = "Tire rotation")
        compose.onNodeWithText(g.string(R.string.due_not_logged)).assertExists()
        compose.onNode(hasText("Tire rotation", substring = true) and hasClickAction()).performScrollTo().click()
        compose.onNode(hasText("Oil change", substring = true) and hasClickAction()).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.home_enter_last_done)) and hasClickAction()).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.log_service)) and hasClickAction()).click()
        compose.onNode(hasContentDescription(g.string(R.string.home_vehicle_details))).click()
        compose.onNode(hasText(g.string(R.string.home_all_vehicles)) and hasClickAction()).click()
        assertEquals(
            listOf("log $id item ${items.second}", "log $id item ${items.first}", "last done $id", "log $id", "details $id", "back"),
            events,
        )
    }

    @Test
    fun `a new reading is saved dated today, and Undo takes it away`() {
        val (id, _) = wagon()
        show(id, until = "Tire rotation")
        compose.onNode(hasText(g.string(R.string.update_mileage_title)) and hasClickAction()).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.update_mileage_enter)) and hasClickAction()).assertExists()
        type("61250")
        compose.onNodeWithText("+" + 1_250.asDistance(unit), substring = true).assertExists()
        compose.onNode(hasText(g.string(R.string.update_mileage_save, 61_250.asDistance(unit))) and hasClickAction()).click()

        compose.awaitThat { messages.isNotEmpty() }
        assertEquals(g.string(R.string.home_mileage_updated, 61_250.asDistance(unit)), messages.single().first)
        assertEquals(listOf(60_000, 61_250), readings(id))
        assertEquals(today, runBlocking { g.db.odometer().readingsFor(id) }.first { it.odometer == 61_250 }.date)

        messages.single().second()
        compose.awaitThat { readings(id) == listOf(60_000) }
    }

    @Test
    fun `a reading far past the last asks before saving, and never blocks`() {
        val (id, _) = wagon()
        show(id, updateMileage = true, until = g.string(R.string.update_mileage_title))
        type("160000")
        compose.onNodeWithText(g.string(R.string.update_mileage_far, 100_000.asDistance(unit))).assertExists()
        compose.onNode(hasText(g.string(R.string.save_anyway)) and hasClickAction()).click()
        compose.awaitThat { readings(id) == listOf(60_000, 160_000) }
    }

    @Test
    fun `a reading below the last asks too, and the keys clear and take back digits`() {
        val (id, _) = wagon()
        show(id, updateMileage = true, until = g.string(R.string.update_mileage_title))
        type("5999")
        compose.onNodeWithText(g.string(R.string.update_mileage_below, 60_000.asDistance(unit))).assertExists()
        compose.onNode(hasText(g.string(R.string.save_anyway)) and hasClickAction()).assertExists()

        compose.onNode(hasContentDescription(g.string(R.string.clear))).click()
        type("600012")
        compose.onNode(hasText(g.string(R.string.save_anyway)) and hasClickAction()).assertExists()
        compose.onNode(hasContentDescription(g.string(R.string.update_mileage_delete_digit))).click()
        compose.onNode(hasText(g.string(R.string.update_mileage_save, 60_001.asDistance(unit))) and hasClickAction()).assertExists()
        compose.onNode(hasContentDescription(g.string(R.string.clear))).click()
        compose.onNode(hasText(g.string(R.string.update_mileage_enter)) and hasClickAction()).assertExists()
        assertEquals(listOf(60_000), readings(id))
    }

    @Test
    fun `the camera's reading only fills the field, and is said to need checking`() {
        val (id, _) = wagon()
        show(id, updateMileage = true, until = g.string(R.string.update_mileage_title))
        compose.onNode(hasText(g.string(R.string.read_dash_title)) and hasClickAction()).click()
        assertEquals("dash 60000 MILES", events.single())

        dash = 61_480
        compose.awaitThat { events.contains("dash used") }
        compose.onNodeWithText(g.string(R.string.update_mileage_from_dash)).assertExists()
        compose.onNode(hasText(g.string(R.string.update_mileage_save, 61_480.asDistance(unit))) and hasClickAction()).assertExists()
        // Nothing is saved until Save.
        assertEquals(listOf(60_000), readings(id))
        // Changed by hand, it is no longer the camera's.
        key("1")
        compose.onNodeWithText(g.string(R.string.update_mileage_from_dash)).assertDoesNotExist()
    }

    @Test
    fun `an open recall is shown on the vehicle's page and opens its list`() {
        val id = g.add(VehicleFixtures.wagon)
        runBlocking {
            g.db.recalls().insertAll(listOf(RecallEntity(vehicleId = id, campaign = "26V000000", component = "SEAT BELTS", summary = "Made up.", firstSeenOn = today)))
        }
        show(id, until = g.string(R.string.home_none_tracked))
        val banner = hasText(g.string(R.string.home_recalls_free)) and hasClickAction()
        compose.awaitThat { compose.onAllNodes(banner).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(banner).click()
        assertEquals(listOf("recalls $id"), events)
    }

    @Test
    fun `an open recall that says not to drive or to park outside says so on the vehicle's page`() {
        val id = g.add(VehicleFixtures.wagon)
        runBlocking {
            g.db.recalls().insertAll(
                listOf(
                    RecallEntity(vehicleId = id, campaign = "26V000000", component = "SEAT BELTS", summary = "Made up.", firstSeenOn = today, doNotDrive = true),
                    RecallEntity(vehicleId = id, campaign = "26V000001", component = "WIRING", summary = "Made up.", firstSeenOn = today, parkOutside = true),
                ),
            )
        }
        show(id, until = g.string(R.string.home_none_tracked))
        val banner = hasText(g.string(R.string.recalls_do_not_drive)) and hasClickAction()
        compose.awaitThat { compose.onAllNodes(banner).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(g.string(R.string.recalls_park_outside)) and hasClickAction()).assertExists()
    }

    @Test
    fun `a recall list that was never fetched is asked for once, and a refused check changes nothing`() {
        val id = g.add(VehicleFixtures.wagon.copy(recallsCheckedOn = null))
        show(id, until = g.string(R.string.home_none_tracked))
        compose.awaitThat { g.engine.requests.get() > 0 }
        assertEquals(null, g.vehicle(id)!!.recallsCheckedOn)
        assertTrue(runBlocking { g.db.recalls().recallsFor(id) }.isEmpty())
    }
}
