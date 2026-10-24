package io.cosgriff.glovebox.ui

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.MainActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.SHELL_PHONE
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.notify.PendingDestination
import io.cosgriff.glovebox.notify.ShortcutTask
import io.cosgriff.glovebox.string
import io.cosgriff.glovebox.waitForDescription
import io.cosgriff.glovebox.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Where a tapped notification, widget row or launcher shortcut lands, once the screens are up. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = SHELL_PHONE)
class GloveboxNavHostDestinationsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph
    private var controller: ActivityController<MainActivity>? = null

    @Before fun returning() = graph.settings { setWelcomeSeen(); setBiometricLock(false) }

    @After fun close() {
        controller?.pause()?.stop()?.destroy()
        graph.close()
    }

    private fun open(to: Destination?) {
        val intent = Intent(app, MainActivity::class.java).also { to?.putInto(it) }
        controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
    }

    private fun arrive(to: Destination) {
        controller!!.newIntent(Intent(app, MainActivity::class.java).also(to::putInto))
    }

    private fun gone() = compose.waitUntil(10_000) { graph.get<PendingDestination>().destination.value == null }

    @Test
    fun `the recalls notification opens that vehicle's recalls`() {
        val id = graph.addVehicle()
        open(Destination.Recalls(id))
        compose.waitForText(string(R.string.details_recalls))
    }

    @Test
    fun `the mileage check-in opens the vehicle with Update mileage up`() {
        val id = graph.addVehicle()
        open(Destination.UpdateMileage(id))
        compose.waitForText(string(R.string.update_mileage_title))
    }

    @Test
    fun `the backup reminder opens Encrypted backup, once however often it is tapped`() {
        graph.addVehicle()
        open(Destination.Backup)
        compose.waitForText(string(R.string.settings_backup))
        arrive(Destination.Backup)
        gone()
        // One Back leaves it: it wasn't stacked on itself.
        controller!!.get().onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasText(string(R.string.settings_backup))).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun `a widget row opens its vehicle, and a sold one's tap goes nowhere`() {
        val tacoma = graph.addVehicle(model = "Tacoma")
        graph.addVehicle(model = "Corolla")
        val sold = graph.addVehicle(model = "Sequoia")
        runBlocking { graph.db.vehicles().setArchived(sold, LocalDate.now()) }
        open(Destination.VehicleHome(sold))
        gone()
        // Still the garage list, with its add button.
        compose.waitForDescription(string(R.string.garage_add_vehicle))
        arrive(Destination.VehicleHome(tacoma))
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(string(R.string.garage_add_vehicle))).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun `the welcome's first button opens Add vehicle`() {
        open(Destination.AddVehicle)
        compose.waitForText(string(R.string.add_vehicle_title))
    }

    @Test
    fun `a shortcut with no vehicle to do it for lands on the garage`() {
        open(Destination.Shortcut(ShortcutTask.LOG_SERVICE))
        gone()
        compose.waitForText(string(R.string.garage_empty_title))
    }

    @Test
    fun `a shortcut with one vehicle goes straight to it`() {
        graph.addVehicle()
        open(Destination.Shortcut(ShortcutTask.LOG_SERVICE))
        compose.waitForText(string(R.string.log_service))
    }

    @Test
    fun `the mileage shortcut with one vehicle opens Update mileage`() {
        graph.addVehicle()
        open(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE))
        compose.waitForText(string(R.string.update_mileage_title))
    }

    @Test
    fun `a shortcut with several vehicles asks which, and does it for the one chosen`() {
        graph.addVehicle(make = "Toyota", model = "Tacoma", year = 2019)
        graph.addVehicle(make = "Honda", model = "Civic", year = 2016)
        open(Destination.Shortcut(ShortcutTask.LOG_SERVICE))
        compose.waitForText(string(R.string.choose_vehicle_log))
        compose.onNodeWithText("2016 Honda Civic").performClick()
        compose.waitForText(string(R.string.log_service))
        compose.onNodeWithText(string(R.string.choose_vehicle_log)).assertDoesNotExist()
    }

    @Test
    fun `asking which vehicle can be called off`() {
        graph.addVehicle(model = "Tacoma")
        graph.addVehicle(model = "Civic")
        open(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE))
        compose.waitForText(string(R.string.choose_vehicle_mileage))
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasText(string(R.string.choose_vehicle_mileage))).fetchSemanticsNodes().isEmpty() }
        assertNull(graph.get<PendingDestination>().destination.value)
    }
}
