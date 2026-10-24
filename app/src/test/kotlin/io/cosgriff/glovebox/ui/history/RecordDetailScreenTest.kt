package io.cosgriff.glovebox.ui.history

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.RecordDetailRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.ServiceTestDb
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.catalogCategoryLabel
import io.cosgriff.glovebox.ui.service.forgetFileProviderRoots
import io.cosgriff.glovebox.ui.service.line
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.routeHandle
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** A service record through its ViewModel on a real database: what it says, and what Edit, Delete, photos and visits do. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class RecordDetailScreenTest {
    @get:Rule val compose = createComposeRule()
    private val t = ServiceTestDb()
    private val today: LocalDate = LocalDate.now()

    private val edits = mutableListOf<Pair<Long, Long>>()
    private val recordsOpened = mutableListOf<Long>()
    private var deleted = 0
    private var confirmShown = 0

    @Before fun roots() = forgetFileProviderRoots()

    @After fun close() = t.close()

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = t.context.resources.getQuantityString(id, n, *args)

    private fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForNoText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty() }

    private fun detail(recordId: Long) = t.owned(
        RecordDetailViewModel(routeHandle(RecordDetailRoute(recordId)), t.db.vehicles(), t.db.schedule(), t.db.service(), t.settings, t.actions, t.picker),
    )

    /**
     * Records here have no photos: a photo tile fetches its loader with `hiltViewModel()`, which needs a Hilt
     * activity. `PhotoBitsTest` draws the tiles with a loader of its own.
     */
    private fun show(vm: RecordDetailViewModel, confirmDelete: Boolean = false) {
        compose.setContent {
            GloveboxTheme {
                RecordDetailScreen(
                    onBack = {}, onDeleted = { deleted++ }, confirmDelete = confirmDelete, onConfirmDeleteShown = { confirmShown++ },
                    onEdit = { v, r -> edits += v to r }, onOpenPhoto = { _, _ -> }, onOpenRecord = { recordsOpened += it },
                    vm = vm,
                )
            }
        }
    }

    /** An oil change tracked every 5,000 mi, logged twice: the later record is the one its reminder counts from. */
    private suspend fun oilTwice(): Triple<Long, Long, Long> {
        val car = t.vehicle(nickname = "Blue car")
        t.db.schedule().applyImport(
            listOf(ScheduleItemEntity(vehicleId = car, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = null)),
            emptyList(), emptyList(),
        )
        val oil = t.db.schedule().allFor(car).single().id
        val older = t.record(car, date = today.minusDays(200), odometer = 40_000, lines = listOf(line("Oil and filter", "oil", oil)))
        val newer = t.record(
            car, date = today.minusDays(20), odometer = 45_000,
            lines = listOf(line("Oil and filter", "oil", oil), line("Hitch greased")), costCents = 7_450, notes = "Made-up note",
        )
        return Triple(car, older, newer)
    }

    @Test
    fun `a record says what was done, its next due, and opens for editing`() = runBlocking<Unit> {
        val (car, _, newer) = oilTwice()
        show(detail(newer))
        waitForText("Made-up note")
        compose.onNodeWithText(today.minusDays(20).format(longDate)).assertExists()
        compose.onNodeWithText(45_000.asDistance(DistanceUnit.MILES)).assertExists()
        compose.onNodeWithText(Money.format(7_450)).assertExists()
        compose.onNodeWithText(catalogCategoryLabel("engine")).assertExists()
        compose.onNodeWithText(string(R.string.log_custom)).assertExists()
        compose.onNodeWithText(string(R.string.record_next_due, 50_000.asDistance(DistanceUnit.MILES))).assertExists()
        compose.onNodeWithText(plural(R.plurals.record_work_done, 2, 2.asNumber())).assertExists()

        compose.onNodeWithText(string(R.string.edit)).performClick()
        assertEquals(listOf(car to newer), edits)
    }

    @Test
    fun `an older record of the same job says it was done again since, and has no cost`() = runBlocking<Unit> {
        val (_, older, _) = oilTwice()
        show(detail(older))
        waitForText(string(R.string.record_done_again))
        compose.onNodeWithText(string(R.string.record_not_entered)).assertExists()
    }

    @Test
    fun `deleting asks first, says which reminders go back, and only then deletes`() = runBlocking<Unit> {
        val (_, older, newer) = oilTwice()
        show(detail(newer))
        waitForText("Made-up note")

        compose.onNodeWithText(string(R.string.record_delete)).performScrollTo().performClick()
        waitForText(string(R.string.record_delete_title))
        compose.onNodeWithText(string(R.string.record_go_back_to)).assertExists()
        compose.onNodeWithText(string(R.string.reminder_change_next_due, "Oil change", 45_000.asDistance(DistanceUnit.MILES)), substring = true).assertExists()
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        waitForNoText(string(R.string.record_delete_title))
        assertEquals(2, t.db.service().recordsFor(t.db.service().record(newer)!!.record.vehicleId).size)

        compose.onNodeWithText(string(R.string.record_delete)).performScrollTo().performClick()
        waitForText(string(R.string.record_delete_title))
        compose.onNodeWithText(string(R.string.delete)).performClick()
        compose.waitForIdle()
        compose.waitUntil(5_000) { deleted == 1 }
        assertNull(t.db.service().record(newer))
        assertEquals(listOf(older), t.db.service().recordsFor(t.db.service().record(older)!!.record.vehicleId).map { it.record.id })
    }

    @Test
    fun `asked for from the edit form, the delete question opens by itself and says nothing changes`() = runBlocking<Unit> {
        val car = t.vehicle()
        val id = t.record(car)
        show(detail(id), confirmDelete = true)
        waitForText(string(R.string.record_delete_title))
        assertEquals(1, confirmShown)
        compose.onNodeWithText(string(R.string.record_no_change)).assertExists()
        compose.onNodeWithText(string(R.string.record_photos_go_too)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.record_undo_for, 10.asNumber())).assertExists()
    }

    @Test
    fun `another photo can be chosen from the Add tile`() = runBlocking<Unit> {
        val car = t.vehicle()
        val id = t.record(car)
        var picks = 0
        t.picker.attach({ picks++ }, {})
        show(detail(id))
        waitForText(string(R.string.record_add_photo))
        compose.onNodeWithText(string(R.string.record_photos_note)).assertExists()
        compose.onNodeWithText(string(R.string.record_add_photo)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.photo_choose)).performClick()
        assertEquals(1, picks)
    }

    @Test
    fun `with the camera allowed, Take a photo opens the camera for this record`() = runBlocking<Unit> {
        val car = t.vehicle()
        val id = t.record(car)
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.CAMERA)
        val shots = mutableListOf<android.net.Uri>()
        t.picker.attach({}, { shots += it })
        show(detail(id))
        waitForText(string(R.string.record_add_photo))
        compose.onNodeWithText(string(R.string.record_add_photo)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.documents_take_photo)).performClick()
        assertEquals(1, shots.size)
    }

    @Test
    fun `a record that looks like the same visit is offered, and Link links them`() = runBlocking<Unit> {
        val car = t.vehicle()
        val id = t.record(car, date = today.minusDays(3), odometer = 50_000, lines = listOf(line("Oil and filter", "oil")))
        val other = t.record(car, date = today.minusDays(3), odometer = 50_000, lines = listOf(line("Tires rotated", "rotation")))
        show(detail(id))
        waitForText(string(R.string.record_visit_offer_title))
        compose.onNodeWithText("Tires rotated").performClick()
        assertEquals(listOf(other), recordsOpened)

        compose.onNodeWithText(string(R.string.record_visit_link)).performClick()
        waitForText(string(R.string.record_visit_title))
        val visit = t.db.service().record(id)!!.record.visitId
        assertEquals(visit, t.db.service().record(other)!!.record.visitId)

        compose.onNodeWithText(string(R.string.record_visit_unlink)).performClick()
        waitForText(string(R.string.record_visit_offer_title))
        assertNull(t.db.service().record(id)!!.record.visitId)
    }

    @Test
    fun `any other record can be picked as the same visit`() = runBlocking<Unit> {
        val car = t.vehicle()
        val id = t.record(car, date = today.minusDays(3), odometer = 50_000, lines = listOf(line("Oil and filter", "oil")))
        val far = t.record(car, date = today.minusDays(300), odometer = 41_000, lines = listOf(line("Wipers")))
        show(detail(id))
        waitForText(string(R.string.record_visit_choose))
        compose.onNodeWithText(string(R.string.record_visit_choose)).performScrollTo().performClick()
        waitForText(string(R.string.record_visit_choose_note))
        compose.onNodeWithText("Wipers").performClick()
        compose.waitForIdle()
        compose.waitUntil(5_000) { runBlocking { t.db.service().record(far)!!.record.visitId } != null }
        assertEquals(t.db.service().record(far)!!.record.visitId, t.db.service().record(id)!!.record.visitId)
    }

    @Test
    fun `an archived vehicle's record can be read but not changed`() = runBlocking<Unit> {
        val car = t.vehicle()
        t.db.vehicles().setArchived(car, today)
        val id = t.record(car)
        show(detail(id), confirmDelete = true)
        waitForText(string(R.string.record_archived))
        compose.onNodeWithText(string(R.string.edit)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.record_delete)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.record_add_photo)).assertDoesNotExist()
        // Asked for from elsewhere, the delete question still doesn't open.
        compose.onNodeWithText(string(R.string.record_delete_title)).assertDoesNotExist()
        assertEquals(0, confirmShown)
    }

    @Test
    fun `a record deleted elsewhere leaves nothing to show`() = runBlocking<Unit> {
        val car = t.vehicle()
        val id = t.record(car, lines = listOf(line("Gone soon")))
        val vm = detail(id)
        show(vm)
        waitForText("Gone soon")
        t.db.service().deleteRecord(t.db.service().record(id)!!.record)
        waitForNoText("Gone soon")
        assertNull(vm.detail.value)
        assertTrue(edits.isEmpty())
    }
}
