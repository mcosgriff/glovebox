package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.domain.DeleteAll
import io.cosgriff.glovebox.ui.ClearHistoryRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** Clear service history: imported records only by default, or all; typed DELETE; nothing else about the vehicle goes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ClearHistoryScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val today = LocalDate.now()
    private val cleared = mutableListOf<Int>()
    private lateinit var vm: ClearHistoryViewModel

    private fun record(id: Long, odometer: Int, imported: Boolean, photo: String? = null) = runBlocking {
        val r = g.db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = today.minusDays(odometer / 100L), odometer = odometer, diy = true, imported = imported),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Work")),
        )
        photo?.let { g.db.service().insertPhotos(listOf(PhotoEntity(recordId = r, fileName = it))) }
    }

    private fun show(id: Long) {
        vm = g.viewModel { ClearHistoryViewModel(routeHandle(ClearHistoryRoute(id)), g.db.vehicles(), g.db.service(), g.wipe) }
        compose.setContent { GloveboxTheme { ClearHistoryScreen(onCancel = {}, onCleared = { cleared += it }, vm = vm) } }
        compose.awaitThat { vm.data != null }
    }

    private fun records(n: Int) = g.context.resources.getQuantityString(R.plurals.records, n, n.toString())
    private fun button(n: Int) = compose.onNode(hasText(g.string(R.string.clear_history_delete, records(n))) and hasClickAction()).performScrollTo()
    private fun confirm() = compose.onNode(hasSetTextAction()).performScrollTo().performTextInput(DeleteAll.WORD)

    @Test
    fun `imported records only, by default, with their photos, and nothing else`() {
        val id = g.add(VehicleFixtures.wagon, firstReading = 90_000)
        val photo = File(g.crypto.photoDir, "receipt-imported.jpg").apply { writeText("x") }
        record(id, 80_000, imported = true, photo = photo.name)
        record(id, 85_000, imported = true)
        record(id, 88_000, imported = false)
        show(id)

        compose.onNode(hasText(g.string(R.string.clear_history_imported), substring = true) and hasClickAction()).assertIsSelected()
        compose.onNodeWithText(g.string(R.string.clear_history_deletes_with_photos, records(2), g.context.resources.getQuantityString(R.plurals.photos, 1, "1"))).assertExists()
        button(2).assertIsNotEnabled()
        confirm()
        button(2).assertIsEnabled().click()

        compose.awaitThat { cleared.isNotEmpty() }
        assertEquals(listOf(2), cleared)
        assertEquals(listOf(88_000), runBlocking { g.db.service().recordsFor(id) }.map { it.record.odometer })
        assertFalse(photo.exists())
        // The vehicle and its readings stay.
        assertEquals(listOf(90_000), runBlocking { g.db.odometer().readingsFor(id) }.map(OdometerReadingEntity::odometer))
    }

    @Test
    fun `every record, when chosen`() {
        val id = g.add(VehicleFixtures.wagon)
        record(id, 80_000, imported = true)
        record(id, 88_000, imported = false)
        show(id)
        compose.onNode(hasText(g.string(R.string.clear_history_all), substring = true) and hasClickAction()).click()
        compose.onNodeWithText(g.string(R.string.clear_history_deletes, records(2))).assertExists()
        confirm()
        button(2).click()
        compose.awaitThat { cleared.isNotEmpty() }
        assertEquals(listOf(2), cleared)
        assertTrue(runBlocking { g.db.service().recordsFor(id) }.isEmpty())
        // A second tap while it is clearing does nothing more.
        vm.clear(all = true) { cleared += it }
        compose.waitForIdle()
        assertEquals(listOf(2), cleared)
    }

    @Test
    fun `with nothing imported, every record is the choice`() {
        val own = g.add(VehicleFixtures.wagon)
        record(own, 88_000, imported = false)
        show(own)
        compose.onNode(hasText(g.string(R.string.clear_history_all), substring = true) and hasClickAction()).assertIsSelected()
        assertEquals(0, vm.data!!.imported)
        assertEquals(1, vm.data!!.all)
    }

    @Test
    fun `a vehicle with no records has nothing to delete`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        confirm()
        compose.onNode(hasText(g.string(R.string.clear_history_nothing)) and hasClickAction()).performScrollTo().assertIsNotEnabled()
    }
}
