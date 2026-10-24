package io.cosgriff.glovebox.ui

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.MainActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.SHELL_PHONE
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.idleMainUntil
import io.cosgriff.glovebox.string
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** The nav host's snackbar for service-log changes: what each change says, and what its button does. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35], qualifiers = SHELL_PHONE)
class GloveboxNavHostEventsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ShellTestApp.current
    private val graph get() = ShellTestApp.graph
    private val actions get() = graph.get<RecordActions>()
    private lateinit var controller: ActivityController<MainActivity>
    private var vehicle = 0L
    private val today = LocalDate.now()

    @Before fun open() {
        graph.settings { setWelcomeSeen(); setBiometricLock(false) }
        vehicle = graph.addVehicle()
        controller = Robolectric.buildActivity(MainActivity::class.java, Intent(app, MainActivity::class.java)).setup()
        compose.waitForText(string(R.string.garage_title))
    }

    @After fun close() {
        controller.pause().stop().destroy()
        graph.close()
    }

    private fun record(id: Long) = runBlocking { graph.db.service().record(id) }

    private fun newRecord(): ServiceRecordEntity = ServiceRecordEntity(vehicleId = vehicle, date = today.minusDays(3), odometer = 61_100, diy = true)

    private fun recall(done: LocalDate? = null, recordId: Long? = null): Long = runBlocking {
        graph.db.recalls().insertAll(
            listOf(RecallEntity(vehicleId = vehicle, campaign = "26V000000", component = "SERVICE BRAKES", summary = "Made up for a test.", firstSeenOn = today, completedOn = done, serviceRecordId = recordId)),
        )
        graph.db.recalls().recallsFor(vehicle).last().id
    }

    private fun tap(label: String) {
        compose.waitForText(label)
        compose.onNodeWithText(label).performClick()
    }

    @Test
    fun `a saved record offers to view it, which opens the record`() {
        runBlocking { actions.add(newRecord(), listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Wiper blades"))) }
        compose.waitForText(string(R.string.event_saved))
        tap(string(R.string.view))
        compose.waitForText("Wiper blades")
    }

    @Test
    fun `a deleted record comes back with Undo, which says so`() {
        val id = graph.logService(vehicle, null, today.minusDays(5), 61_050, label = "Cabin air filter")
        runBlocking { actions.delete(checkNotNull(record(id))) }
        compose.waitForText(string(R.string.event_deleted))
        assertNull(record(id))
        tap(string(R.string.undo))
        compose.waitForText(string(R.string.event_restored))
        assertEquals(1, runBlocking { graph.db.service().recordsFor(vehicle) }.size)
    }

    @Test
    fun `an edit is undone with Undo`() {
        val id = graph.logService(vehicle, null, today.minusDays(5), 61_050, label = "Cabin air filter")
        val before = checkNotNull(record(id))
        runBlocking { actions.replace(before, before.record.copy(odometer = 61_070), before.items) }
        compose.waitForText(string(R.string.event_edited))
        tap(string(R.string.undo))
        compose.waitForText(string(R.string.event_undone))
        assertEquals(61_050, record(id)?.record?.odometer)
    }

    @Test
    fun `filed lines go back with Undo`() {
        val id = graph.logService(vehicle, null, today.minusDays(5), 61_050, label = "Wiper refills")
        val line = checkNotNull(record(id)).items.single().id
        runBlocking { actions.fileCustom(listOf(line to "wipers"), emptyList(), today) }
        compose.waitForText(app.resources.getQuantityString(R.plurals.event_lines_filed, 1, "1"))
        tap(string(R.string.undo))
        compose.waitForText(string(R.string.event_undone))
        assertNull(checkNotNull(record(id)).items.single().catalogId)
    }

    @Test
    fun `a recall marked done without a record is reopened with Undo`() {
        val id = recall()
        runBlocking { actions.completeRecall(id, today, record = null) }
        compose.waitForText(string(R.string.event_recall_done))
        tap(string(R.string.undo))
        idleMainUntil { runBlocking { graph.db.recalls().byId(id) }?.completedOn == null }
    }

    @Test
    fun `a recall marked done with a record offers to view the record`() {
        val id = recall()
        runBlocking { actions.completeRecall(id, today, record = newRecord()) }
        compose.waitForText(string(R.string.event_recall_done_logged))
        tap(string(R.string.view))
        val recordId = checkNotNull(runBlocking { graph.db.recalls().byId(id) }?.serviceRecordId)
        assertNotNull(record(recordId))
    }

    @Test
    fun `a reopened recall is marked done again with Undo, its record put back`() {
        val recordId = graph.logService(vehicle, null, today.minusDays(2), 61_080, label = "Recall repair")
        val id = recall(done = today.minusDays(2), recordId = recordId)
        runBlocking { actions.reopenRecall(id) }
        compose.waitForText(string(R.string.event_recall_open_removed))
        tap(string(R.string.undo))
        idleMainUntil { runBlocking { graph.db.recalls().byId(id) }?.completedOn == today.minusDays(2) }
        assertNotNull(runBlocking { graph.db.recalls().byId(id) }?.serviceRecordId)
    }

    @Test
    fun `reopening a recall that had no record says only that`() {
        val id = recall(done = today.minusDays(2))
        runBlocking { actions.reopenRecall(id) }
        compose.waitForText(string(R.string.event_recall_open))
    }

    @Test
    fun `an Undo not taken goes after its set time`() {
        val id = graph.logService(vehicle, null, today.minusDays(5), 61_050)
        runBlocking { actions.delete(checkNotNull(record(id))) }
        compose.waitForText(string(R.string.event_deleted))
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
        compose.waitUntil(10_000) { compose.onAllNodesWithTextCount(string(R.string.event_deleted)) == 0 }
        assertNull(record(id))
    }

    @Test
    fun `the cross dismisses an Undo sooner, and the record stays deleted`() {
        val id = graph.logService(vehicle, null, today.minusDays(5), 61_050)
        runBlocking { actions.delete(checkNotNull(record(id))) }
        compose.waitForText(string(R.string.event_deleted))
        compose.onNodeWithContentDescription(DISMISS).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTextCount(string(R.string.event_deleted)) == 0 }
        assertNull(record(id))
    }

    private companion object {
        /** Material's own label for a snackbar's ✕. */
        const val DISMISS = "Dismiss"
    }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextCount(text: String) =
    onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
