package io.cosgriff.glovebox.ui

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.cosgriff.glovebox.MainActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.SHELL_PHONE
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.back
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.string
import io.cosgriff.glovebox.tab
import io.cosgriff.glovebox.tap
import io.cosgriff.glovebox.tapDescription
import io.cosgriff.glovebox.waitForDescription
import io.cosgriff.glovebox.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

/** The tabs, the garage list, History and Settings, and the pages each leads to, through the nav host. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = SHELL_PHONE)
class GloveboxNavHostTabsTourTest {
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

    private fun systemBack() = controller!!.get().onBackPressedDispatcher.onBackPressed()

    private fun garageList() = compose.waitForDescription(string(R.string.garage_add_vehicle))

    @Test
    fun `settings leads to every page it lists, and back`() {
        graph.addVehicle()
        runBlocking { graph.db.catalogRules().put("Wiper refills", "wipers", today) }
        launch()
        compose.tab(string(R.string.settings_title))

        compose.tap(string(R.string.settings_reminder_rule))
        compose.back()
        compose.tap(string(R.string.details_schedule))
        compose.waitForText(string(R.string.details_schedule))
        compose.back()
        compose.tap(string(R.string.notifications_title))
        compose.tap(string(R.string.notifications_how_early))
        compose.back()
        compose.back()
        compose.tap(string(R.string.settings_backup))
        compose.tap(string(R.string.backup_restore_from))
        compose.back()
        compose.tap(string(R.string.settings_import))
        compose.back()
        compose.tap(string(R.string.settings_rules))
        compose.waitForText("Wiper refills")
        compose.back()
        compose.tap(string(R.string.settings_delete_all))
        compose.tap(string(R.string.delete_all_backup_first))
        compose.waitForText(string(R.string.backup_restore_from))
        compose.back()
        compose.tapDescription(string(R.string.cancel))
        compose.tap(string(R.string.about_title))
        compose.tap(string(R.string.privacy))
        compose.back()
        compose.tap(string(R.string.licences_title))
        compose.tap(string(R.string.licences_gpl))
        compose.back()
        compose.back()
        compose.back()
        compose.waitForText(string(R.string.settings_delete_all))
    }

    @Test
    fun `a tab comes back as it was left, and Back from a tab's first page goes to the garage list`() {
        graph.addVehicle(model = "Tacoma")
        graph.addVehicle(model = "Civic")
        launch()
        garageList()
        compose.tap("Civic", substring = true)
        compose.waitForDescription(string(R.string.home_vehicle_details))
        compose.tab(string(R.string.settings_title))
        compose.waitForText(string(R.string.settings_delete_all))
        compose.tab(string(R.string.garage_title))
        compose.waitForDescription(string(R.string.home_vehicle_details))

        compose.tab(string(R.string.history_title))
        systemBack()
        garageList()
    }

    @Test
    fun `history opens a record, whose edit can delete it instead, ending on History`() {
        val id = graph.addVehicle()
        val record = graph.logService(id, null, today.minusDays(9), 60_500, label = "Wiper refills", shop = "Made-up Motors")
        launch()
        compose.tab(string(R.string.history_title))
        compose.tap("Wiper refills")
        compose.waitForText(string(R.string.record_title))
        compose.tap(string(R.string.edit))
        compose.tap(string(R.string.log_delete))
        compose.waitForText(string(R.string.record_delete_title))
        compose.tap(string(R.string.delete))
        compose.waitForText(string(R.string.event_deleted))
        compose.waitUntil(10_000) { runBlocking { graph.db.service().record(record) } == null }
    }

    @Test
    fun `a record's edit can be closed without saving`() {
        val id = graph.addVehicle()
        graph.logService(id, null, today.minusDays(9), 60_500, label = "Wiper refills")
        launch()
        compose.tab(string(R.string.history_title))
        compose.tap("Wiper refills")
        compose.tap(string(R.string.edit))
        compose.tapDescription(string(R.string.close))
        compose.waitForText(string(R.string.record_title))
        compose.back()
        compose.waitForText("Wiper refills")
    }

    @Test
    fun `history's custom lines open the sort page`() {
        val id = graph.addVehicle()
        graph.logService(id, null, today.minusDays(9), 60_500, label = "Wiper refills")
        launch()
        compose.tab(string(R.string.history_title))
        compose.tap(app.resources.getQuantityString(R.plurals.history_custom_lines, 1, "1"))
        compose.waitForText(string(R.string.sort_custom_title))
        compose.back()
    }

    @Test
    fun `a reading logged with a service opens its record`() {
        val id = graph.addVehicle()
        graph.logService(id, null, today.minusDays(9), 60_500, label = "Wiper refills")
        launch()
        compose.waitForDescription(string(R.string.home_vehicle_details))
        compose.tapDescription(string(R.string.home_vehicle_details))
        compose.tap(string(R.string.readings_title))
        compose.tap(string(R.string.readings_from_service, "").trim(), substring = true)
        compose.waitForText(string(R.string.record_title))
    }

    @Test
    fun `the garage list leads to adding, the archive and a vehicle`() {
        graph.addVehicle(model = "Tacoma")
        graph.addVehicle(model = "Civic")
        val sold = graph.addVehicle(model = "Sequoia")
        runBlocking { graph.db.vehicles().setArchived(sold, today.minusDays(1)) }
        launch()
        garageList()

        compose.tapDescription(string(R.string.garage_add_vehicle))
        compose.tap(string(R.string.add_vehicle_scan))
        compose.tap(string(R.string.scan_vin_type_instead))
        compose.tap(string(R.string.add_vehicle_no_vin))
        compose.waitForText(string(R.string.manual_add_title))
        compose.back()
        compose.back()
        garageList()

        compose.tap(string(R.string.garage_archived, "1"))
        compose.tap(string(R.string.archived_view_history))
        compose.back()
        compose.tap(string(R.string.delete))
        compose.tapDescription(string(R.string.cancel))
        compose.tap(string(R.string.archived_move_back))
        compose.waitUntil(10_000) { runBlocking { graph.db.vehicles().byId(sold) }?.archivedOn == null }
        compose.back()

        compose.tap("Civic", substring = true)
        compose.waitForDescription(string(R.string.home_vehicle_details))
        compose.tap(string(R.string.home_all_vehicles))
        garageList()
    }

    @Test
    fun `the garage's urgent work leads to Reminders, whose rows log it`() {
        graph.overdueVehicle(model = "Tacoma")
        graph.addVehicle(model = "Civic")
        launch()
        compose.tap(string(R.string.garage_see_all_reminders))
        compose.waitForText(string(R.string.reminders_rules))
        compose.tap(string(R.string.log_it))
        compose.waitForText(string(R.string.log_service))
        compose.tapDescription(string(R.string.close))
        compose.tap(string(R.string.reminders_rules))
        compose.back()
    }

    @Test
    fun `reminders with notifications off lead to the notification settings`() {
        graph.overdueVehicle()
        launch()
        compose.tab(string(R.string.reminders_title))
        compose.tap(string(R.string.reminders_turn_on))
        compose.waitForText(string(R.string.notifications_how_early))
        compose.back()
    }

    @Test
    fun `with no vehicles, Reminders and the garage both lead to adding one`() {
        launch()
        compose.tap(string(R.string.garage_empty_add))
        compose.waitForText(string(R.string.add_vehicle_title))
        compose.back()
        compose.tab(string(R.string.reminders_title))
        compose.tap(string(R.string.garage_empty_add))
        compose.waitForText(string(R.string.add_vehicle_title))
        compose.back()
        compose.tab(string(R.string.garage_title))
        compose.tap(string(R.string.welcome_restore))
        assertNull(graph.get<io.cosgriff.glovebox.notify.PendingDestination>().destination.value)
    }

    @Test
    fun `a vehicle with no schedule is set up from its schedule page, going on to last done`() {
        // Not a model any bundled list covers, so the typical list is offered.
        graph.addVehicle(make = "Madeup", model = "Roadster", year = 2011)
        launch()
        compose.tapDescription(string(R.string.home_vehicle_details))
        compose.tap(string(R.string.details_schedule))
        compose.tap(string(R.string.schedule_start_typical))
        compose.tap(string(R.string.setup_last_done_label))
        compose.tap(string(R.string.setup_last_done_next))
        compose.tap(app.resources.getQuantityString(R.plurals.setup_track, 2, "").trim().substringBefore(" "), substring = true)
        compose.waitForText(string(R.string.done))
        // Setup is gone from under it: Back lands on the schedule.
        compose.back()
        compose.waitForText(string(R.string.details_schedule))
    }
}
