package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.ui.DeleteVehicleRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.shortName
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** Delete vehicle: what it takes with it, counted; confirmed by typing the name; no Undo, so the files go too. Or archive instead. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class DeleteVehicleScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val today = LocalDate.now()
    private val deleted = mutableListOf<String>()
    private val archived = mutableListOf<Pair<String, () -> Unit>>()
    private lateinit var vm: DeleteVehicleViewModel

    /** The wagon with a record and its receipt photo, an item, a torque row, a recall, a reading and a document. */
    private fun wagon(): Pair<Long, List<File>> {
        val id = g.add(VehicleFixtures.wagon.copy(nickname = "Old Faithful", photoFile = "vehicle-1.jpg"), firstReading = 201_000)
        val files = listOf("vehicle-1.jpg", "receipt-1.jpg", "document-1.pdf").map { File(g.crypto.photoDir, it).apply { writeText("not really encrypted") } }
        runBlocking {
            val record = g.db.service().insertWithItems(
                ServiceRecordEntity(vehicleId = id, date = today.minusMonths(1), odometer = 200_500, diy = true),
                listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Brake pads")),
            )
            g.db.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = "receipt-1.jpg")))
            g.db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = id, name = "Oil change", intervalMiles = 5_000, intervalMonths = 6)))
            g.db.torque().insert(TorqueSpecEntity(vehicleId = id, name = "Lug nuts", category = "WHEELS"))
            g.db.recalls().insertAll(listOf(RecallEntity(vehicleId = id, campaign = "26V000001", component = "BRAKES", summary = "Made up.", firstSeenOn = today)))
            g.db.documents().insert(DocumentEntity(vehicleId = id, type = "TITLE", name = "Title", fileName = "document-1.pdf", format = "PDF", sizeBytes = 10, addedOn = today))
        }
        return id to files
    }

    private fun show(id: Long, loaded: Boolean = true) {
        vm = g.viewModel {
            DeleteVehicleViewModel(
                routeHandle(DeleteVehicleRoute(id)), g.db.vehicles(), g.db.service(), g.db.schedule(), g.db.torque(), g.db.recalls(),
                g.db.odometer(), g.db.documents(), g.wipe, g.archiver,
            )
        }
        compose.setContent {
            GloveboxTheme { DeleteVehicleScreen(onCancel = {}, onDeleted = { deleted += it }, onArchived = { name, undo -> archived += name to undo }, vm = vm) }
        }
        if (loaded) compose.awaitThat { vm.data != null }
    }

    private fun deleteButton() = compose.onNode(hasText(g.string(R.string.delete_vehicle_button)) and hasClickAction()).performScrollTo()

    @Test
    fun `everything the vehicle has is counted before it goes`() {
        val (id, _) = wagon()
        show(id)
        val d = vm.data!!
        assertEquals(listOf(1, 1, 1, 1, 1, 1, 1), listOf(d.records, d.photos, d.scheduleItems, d.torqueSpecs, d.recalls, d.mileageReadings, d.documents))
        assertEquals(201_000, d.lastOdometer)
        assertEquals("Old Faithful", d.name)
        compose.onNodeWithText(g.string(R.string.delete_vehicle_title, g.vehicle(id)!!.shortName())).assertExists()
    }

    @Test
    fun `only the vehicle's name, typed, deletes it, and its files go with it`() {
        val (id, files) = wagon()
        show(id)
        deleteButton().assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Old")
        deleteButton().assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput(" Faithful")
        deleteButton().assertIsEnabled().click()

        compose.awaitThat { deleted.isNotEmpty() }
        assertEquals(listOf("Old Faithful"), deleted)
        assertNull(g.vehicle(id))
        assertTrue(runBlocking { g.db.service().recordsFor(id) }.isEmpty())
        assertTrue(runBlocking { g.db.documents().documentsFor(id) }.isEmpty())
        files.forEach { assertFalse("${it.name} is still there", it.exists()) }
        // A second tap while it was deleting does nothing more.
        vm.delete { deleted += it }
        compose.waitForIdle()
        assertEquals(1, deleted.size)
    }

    @Test
    fun `archive instead keeps everything, with an Undo`() {
        val (id, files) = wagon()
        show(id)
        compose.onNode(hasText(g.string(R.string.delete_vehicle_archive_instead)) and hasClickAction()).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.sold_archive)) and hasClickAction()).click()

        compose.awaitThat { archived.isNotEmpty() }
        assertEquals("Old Faithful", archived.single().first)
        assertEquals(today, g.vehicle(id)!!.archivedOn)
        files.forEach { assertTrue(it.exists()) }
        archived.single().second()
        compose.awaitThat { g.db.vehicles().byId(id)?.archivedOn == null }
    }

    @Test
    fun `an archived vehicle has no archive to offer`() {
        val id = g.add(VehicleFixtures.wagon.copy(archivedOn = today.minusDays(5)))
        show(id)
        compose.onNodeWithText(g.string(R.string.delete_vehicle_archive_instead)).assertDoesNotExist()
        compose.onNodeWithText(g.string(R.string.delete_vehicle_no_photo)).assertExists()
    }

    @Test
    fun `a vehicle that has gone shows nothing and deletes nothing`() {
        show(404, loaded = false)
        compose.waitForIdle()
        assertNull(vm.data)
        vm.delete { deleted += it }
        compose.waitForIdle()
        assertTrue(deleted.isEmpty())
        compose.onNode(hasContentDescription(g.string(R.string.cancel))).assertDoesNotExist()
    }

}
