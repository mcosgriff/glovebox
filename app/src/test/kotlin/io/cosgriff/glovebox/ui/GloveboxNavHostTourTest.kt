package io.cosgriff.glovebox.ui

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.cosgriff.glovebox.MainActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.SHELL_PHONE
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.back
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.string
import io.cosgriff.glovebox.tap
import io.cosgriff.glovebox.tapDescription
import io.cosgriff.glovebox.waitForDescription
import io.cosgriff.glovebox.waitForNoText
import io.cosgriff.glovebox.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The app's screens reached the way the user reaches them, and left by their own Back or close: each of the
 * nav host's routes and the way back from it. Offline, with no camera and no file picker.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = SHELL_PHONE)
class GloveboxNavHostTourTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph
    private var controller: ActivityController<MainActivity>? = null
    private val today = LocalDate.now()

    @Before fun returning() = graph.settings { setWelcomeSeen(); setBiometricLock(false); setRecallCadence(RecallCadence.NEVER) }

    @After fun close() {
        controller?.pause()?.stop()?.destroy()
        graph.close()
    }

    private fun launch() {
        controller = Robolectric.buildActivity(MainActivity::class.java, Intent(app, MainActivity::class.java)).setup()
    }

    private fun openRecall(vehicle: Long) = runBlocking {
        graph.db.recalls().insertAll(
            listOf(RecallEntity(vehicleId = vehicle, campaign = "26V000000", component = "SERVICE BRAKES", summary = "Made up for a test.", firstSeenOn = today)),
        )
    }

    /** One vehicle, so the app opens on its home: an overdue oil change, a job never logged, an open recall. */
    private fun oneVehicle(): Long {
        val (id, _) = graph.overdueVehicle()
        graph.addItem(id, name = "Brake fluid", everyMiles = null, everyMonths = 36)
        openRecall(id)
        return id
    }

    private fun home() = compose.waitForDescription(string(R.string.home_vehicle_details))

    @Test
    fun `vehicle home leads to logging, last done, rules, recalls and back`() {
        oneVehicle()
        launch()
        home()

        compose.tap(string(R.string.log_service))
        compose.tapDescription(string(R.string.close))
        home()

        // A reminder row opens Log service with that job ticked.
        compose.tap("Oil change")
        compose.waitForText(string(R.string.log_service))
        compose.tapDescription(string(R.string.close))
        home()

        compose.tap(string(R.string.home_enter_last_done))
        compose.back()
        home()

        compose.tap(string(R.string.rule_whichever_first))
        compose.back()
        home()

        compose.tap(string(R.string.home_recalls_free))
        compose.waitForText(string(R.string.details_recalls))
        compose.back()
        home()

        // Garage from a vehicle's home is the list, with nothing above it.
        compose.tap(string(R.string.garage_title))
        compose.waitForDescription(string(R.string.garage_add_vehicle))
    }

    @Test
    fun `updating the mileage can read the dash, and the reading screen closes back to it`() {
        oneVehicle()
        launch()
        home()
        compose.tap(string(R.string.update_mileage_title))
        compose.tap(string(R.string.read_dash_title))
        compose.tapDescription(string(R.string.scan_close_scanner))
        compose.waitForText(string(R.string.read_dash_title))
    }

    @Test
    fun `vehicle details leads to every page about the vehicle and back`() {
        val id = oneVehicle()
        graph.logService(id, null, today.minusDays(40), 59_000, label = "Wiper refills")
        launch()
        home()
        compose.tapDescription(string(R.string.home_vehicle_details))
        compose.waitForText(string(R.string.details_torque))

        for (row in listOf(R.string.details_torque, R.string.details_schedule, R.string.details_recalls, R.string.readings_title, R.string.documents_title, R.string.details_import, R.string.fluids_title)) {
            compose.tap(string(row))
            compose.back()
            compose.waitForText(string(R.string.details_clear))
        }

        compose.tap(string(R.string.details_clear))
        compose.tapDescription(string(R.string.cancel))
        compose.tap(string(R.string.details_delete))
        compose.tapDescription(string(R.string.cancel))

        compose.back()
        home()
    }

    @Test
    fun `marking a vehicle sold goes back to the garage and offers Undo`() {
        val id = oneVehicle()
        launch()
        home()
        compose.tapDescription(string(R.string.home_vehicle_details))
        compose.tap(string(R.string.details_mark_sold))
        compose.tap(string(R.string.sold_archive))
        compose.waitForText(string(R.string.event_archived, "").trim(), substring = true)
        compose.tap(string(R.string.undo))
        compose.waitUntil(10_000) { runBlocking { graph.db.vehicles().byId(id) }?.archivedOn == null }
    }

    @Test
    fun `the schedule leads to milestones and last done`() {
        oneVehicle()
        launch()
        home()
        compose.tapDescription(string(R.string.home_vehicle_details))
        compose.tap(string(R.string.details_schedule))
        compose.tap(string(R.string.milestones_title))
        compose.back()
        compose.tap(string(R.string.schedule_enter_last_done))
        compose.back()
        compose.waitForText(string(R.string.details_schedule))
    }
}
