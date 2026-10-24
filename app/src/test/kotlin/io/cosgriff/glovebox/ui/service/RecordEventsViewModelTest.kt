package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The nav host's side of the snackbar: what it hears, and what Undo and the bar ending do. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class RecordEventsViewModelTest {
    private val t = ServiceTestDb()
    private val vm = t.owned(RecordEventsViewModel(t.actions))

    @Before fun main() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After fun close() {
        t.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a delete is heard, and Undo puts the record back and says so`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car)
        t.actions.delete(t.db.service().record(id)!!)
        val event = vm.events.first()
        var undone = false
        vm.undo(event) { undone = true }
        awaitUntil("the undo") { undone }
        assertEquals(1, t.db.service().recordsFor(car).size)
    }

    @Test
    fun `Undo says nothing when the vehicle has gone, and the bar ending lets the files go`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car)
        val photo = t.photoFile("record-receipt.jpg")
        t.db.service().insertPhotos(listOf(io.cosgriff.glovebox.data.db.PhotoEntity(recordId = id, fileName = photo)))
        t.actions.delete(t.db.service().record(id)!!)
        val event = vm.events.first()
        t.db.vehicles().delete(t.db.vehicles().byId(car)!!)
        var undone = false
        vm.undo(event) { undone = true }.join()
        assertFalse(undone)
        vm.discard(event).join()
        assertFalse(t.hasFile(photo))
    }
}
