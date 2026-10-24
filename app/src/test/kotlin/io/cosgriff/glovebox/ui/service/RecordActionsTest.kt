package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.nhtsa.toNotice
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.CatalogRules
import io.cosgriff.glovebox.domain.RecordMark
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class RecordActionsTest {
    private val t = ServiceTestDb()
    private val service = t.db.service()
    private val recalls = t.db.recalls()
    private val actions = RecordActions(service, recalls, t.db.vehicles(), t.photos)
    private val day = ServiceFixtures.DAY

    @After fun close() = t.close()

    private suspend fun nextEvent() = actions.events.first()

    /** Nothing more announced: the channel is buffered, so a sent event would be there at once. */
    private suspend fun noEvent() = assertNull(withTimeoutOrNull(100) { actions.events.first() })

    @Test
    fun `a new record is saved with its lines and photos, announced, and History is pointed at it`() = runBlocking {
        val car = t.vehicle()
        val receipt = t.photoFile("record-a.jpg")
        actions.add(ServiceRecordEntity(vehicleId = car, date = day, odometer = 41_000, diy = true), listOf(line("Oil and filter", "oil")), listOf(receipt))

        val saved = service.recordsFor(car).single()
        assertEquals(listOf("Oil and filter"), saved.items.map { it.label })
        assertEquals(listOf(receipt), saved.photos.map { it.fileName })
        assertEquals(RecordEvent.Saved(saved.record.id), nextEvent())
        assertEquals(HistoryFocus(car, saved.record.id, RecordMark.ADDED), actions.focus.value)

        actions.clearFocus()
        assertNull(actions.focus.value)
    }

    @Test
    fun `a bare last done is flagged, filed under the line that resets it, and announces nothing`() = runBlocking {
        val car = t.vehicle()
        t.db.schedule().applyImport(
            listOf(ScheduleItemEntity(vehicleId = car, name = "Engine oil", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6)),
            emptyList(), emptyList(),
        )
        val item = t.db.schedule().allFor(car).single()
        val itemId = item.id

        actions.addLastDone(car, item, day, 38_000)

        val saved = service.recordsFor(car).single()
        assertTrue(saved.record.enteredAsLastDone)
        assertEquals(38_000, saved.record.odometer)
        assertEquals(listOf(Triple(itemId, "Engine oil", "oil")), saved.items.map { Triple(it.scheduleItemId, it.label, it.catalogId) })
        assertNull(actions.focus.value)
        noEvent()
    }

    @Test
    fun `an edit keeps the removed photo's file for Undo, and Undo puts the record back as it was`() = runBlocking {
        val car = t.vehicle()
        val kept = t.photoFile("record-kept.jpg")
        val gone = t.photoFile("record-gone.jpg")
        val id = t.record(car, odometer = 40_000, lines = listOf(line("Wiper blades replaced")))
        service.insertPhotos(listOf(PhotoEntity(recordId = id, fileName = kept), PhotoEntity(recordId = id, fileName = gone)))
        val before = service.record(id)!!
        val added = t.photoFile("record-new.jpg")

        actions.replace(
            before, before.record.copy(odometer = 40_500), listOf(line("Wiper blades replaced"), line("Washer fluid", "washer")),
            addedPhotos = listOf(added), removedPhotoIds = setOf(before.photos.single { it.fileName == gone }.id),
        )

        val edited = service.record(id)!!
        assertEquals(40_500, edited.record.odometer)
        assertEquals(2, edited.items.size)
        assertEquals(setOf(kept, added), edited.photos.map { it.fileName }.toSet())
        // The row is gone, the file isn't: Undo may still want it.
        assertTrue(t.hasFile(gone))
        val event = nextEvent() as RecordEvent.Edited
        assertEquals(listOf(added), event.addedPhotos)
        assertEquals(listOf(gone), event.removedPhotos)

        assertTrue(actions.undo(event))
        val undone = service.record(id)!!
        assertEquals(40_000, undone.record.odometer)
        assertEquals(listOf("Wiper blades replaced"), undone.items.map { it.label })
        assertEquals(setOf(kept, gone), undone.photos.map { it.fileName }.toSet())
        // The photo added by the edit belonged to nothing once it was undone.
        assertFalse(t.hasFile(added))
        assertTrue(t.hasFile(gone))
    }

    @Test
    fun `an edit not undone lets the removed photo's file go, and only that one`() = runBlocking {
        val car = t.vehicle()
        val gone = t.photoFile("record-gone.jpg")
        val other = t.photoFile("record-other.jpg")
        actions.discard(RecordEvent.Edited(previous = recordWith(car), addedPhotos = listOf(other), removedPhotos = listOf(gone)))
        assertFalse(t.hasFile(gone))
        assertTrue(t.hasFile(other))
    }

    @Test
    fun `an edit can't be undone once the vehicle has gone`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car)
        val before = service.record(id)!!
        actions.replace(before, before.record.copy(odometer = 41_000), before.items)
        val event = nextEvent()
        deleteVehicle(car)
        assertFalse(actions.undo(event))
    }

    @Test
    fun `a delete leaves the recall done, unlinks it, and Undo brings the record, its photos and the link back`() = runBlocking {
        val car = t.vehicle()
        val photo = t.photoFile("record-receipt.jpg")
        val id = t.record(car, odometer = 42_000, lines = listOf(line("Fuel pump replaced")))
        service.insertPhotos(listOf(PhotoEntity(recordId = id, fileName = photo)))
        val recall = t.recall(car, completedOn = day, recordId = id)

        actions.delete(service.record(id)!!)

        assertNull(service.record(id))
        val afterDelete = recalls.byId(recall)!!
        assertEquals(day, afterDelete.completedOn)
        assertNull(afterDelete.serviceRecordId)
        assertTrue(t.hasFile(photo))
        assertEquals(HistoryFocus(car), actions.focus.value)
        val event = nextEvent() as RecordEvent.Deleted
        assertEquals(listOf(recall), event.recallIds)

        assertTrue(actions.undo(event))
        val back = service.recordsFor(car).single()
        assertEquals(42_000, back.record.odometer)
        assertEquals(listOf("Fuel pump replaced"), back.items.map { it.label })
        assertEquals(listOf(photo), back.photos.map { it.fileName })
        assertEquals(back.record.id, recalls.byId(recall)!!.serviceRecordId)
        assertEquals(HistoryFocus(car, back.record.id, RecordMark.RESTORED), actions.focus.value)
    }

    @Test
    fun `a delete not undone removes its photo files`() = runBlocking {
        val car = t.vehicle()
        val photo = t.photoFile("record-receipt.jpg")
        val id = t.record(car)
        service.insertPhotos(listOf(PhotoEntity(recordId = id, fileName = photo)))
        actions.delete(service.record(id)!!)
        actions.discard(nextEvent())
        assertFalse(t.hasFile(photo))
    }

    @Test
    fun `a delete can't be undone once the vehicle has gone`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car)
        actions.delete(service.record(id)!!)
        val event = nextEvent()
        deleteVehicle(car)
        assertFalse(actions.undo(event))
        assertEquals(HistoryFocus(car), actions.focus.value)
    }

    @Test
    fun `marking a recall done with a record adds the work as a line that resets nothing, once`() = runBlocking {
        val car = t.vehicle()
        val recall = t.recall(car)
        val record = ServiceRecordEntity(vehicleId = car, date = day, odometer = 43_000, diy = false, shopName = "Made-up Motors")

        actions.completeRecall(recall, day, record)

        val saved = service.recordsFor(car).single()
        val done = recalls.byId(recall)!!
        assertEquals(day, done.completedOn)
        assertEquals(saved.record.id, done.serviceRecordId)
        val line = saved.items.single()
        assertNull(line.scheduleItemId)
        assertNull(line.catalogId)
        assertEquals(recallServiceLine(done.toNotice()), line.label)
        assertEquals(RecordEvent.RecallDone(recall, saved.record.id), nextEvent())

        // Already done: a second mark adds no second record.
        actions.completeRecall(recall, day, record)
        assertEquals(1, service.recordsFor(car).size)
        noEvent()
    }

    @Test
    fun `a recall's record can join the visit it was done on`() = runBlocking {
        val car = t.vehicle()
        val other = t.record(car, odometer = 43_000)
        val recall = t.recall(car)
        actions.completeRecall(recall, day, ServiceRecordEntity(vehicleId = car, date = day, odometer = 43_000, diy = true), visitWith = other)
        val records = service.recordsFor(car).map { it.record }
        assertEquals(2, records.size)
        assertNotNull(records.first().visitId)
        assertEquals(1, records.map { it.visitId }.toSet().size)
    }

    @Test
    fun `undoing a recall marked done without a record opens it again`() = runBlocking {
        val car = t.vehicle()
        val recall = t.recall(car)
        actions.completeRecall(recall, day, record = null)
        val event = nextEvent()
        assertEquals(RecordEvent.RecallDone(recall, null), event)
        assertTrue(actions.undo(event))
        assertNull(recalls.byId(recall)!!.completedOn)
    }

    @Test
    fun `reopening a recall removes the record it added, and Undo puts both back`() = runBlocking {
        val car = t.vehicle()
        val recall = t.recall(car)
        actions.completeRecall(recall, day, ServiceRecordEntity(vehicleId = car, date = day, odometer = 43_000, diy = true))
        nextEvent()

        actions.reopenRecall(recall)
        assertTrue(service.recordsFor(car).isEmpty())
        assertNull(recalls.byId(recall)!!.completedOn)
        val event = nextEvent() as RecordEvent.RecallReopened

        assertTrue(actions.undo(event))
        val back = service.recordsFor(car).single()
        val again = recalls.byId(recall)!!
        assertEquals(day, again.completedOn)
        assertEquals(back.record.id, again.serviceRecordId)

        // An open recall has nothing to reopen.
        actions.reopenRecall(t.recall(car, campaign = "24V999000"))
        noEvent()
    }

    @Test
    fun `a reopened recall's record photos go when its Undo isn't taken`() = runBlocking {
        val car = t.vehicle()
        val photo = t.photoFile("record-recall.jpg")
        val recall = t.recall(car)
        actions.completeRecall(recall, day, ServiceRecordEntity(vehicleId = car, date = day, odometer = 43_000, diy = true))
        nextEvent()
        service.insertPhotos(listOf(PhotoEntity(recordId = recalls.byId(recall)!!.serviceRecordId!!, fileName = photo)))
        actions.reopenRecall(recall)
        actions.discard(nextEvent())
        assertFalse(t.hasFile(photo))
    }

    @Test
    fun `custom lines filed under a line keep their wording, and one Undo puts lines and rules back`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car, odometer = 40_000, lines = listOf(line("Hitch greased"), line("Roof rack fitted")))
        val lines = service.record(id)!!.items
        val hitch = lines.single { it.label == "Hitch greased" }
        t.db.catalogRules().put("Roof rack fitted", "washer", day)

        actions.fileCustom(
            listOf(hitch.id to "oil"),
            listOf(CatalogRule("Hitch greased", "oil"), CatalogRule("Roof rack fitted", "alignment")),
            day,
        )

        val filed = service.record(id)!!.items.single { it.id == hitch.id }
        assertEquals("Hitch greased", filed.label)
        assertEquals("oil", filed.catalogId)
        assertEquals(setOf("Hitch greased" to "oil", "Roof rack fitted" to "alignment"), t.db.catalogRules().all().map { it.text to it.catalogId }.toSet())
        val event = nextEvent() as RecordEvent.Filed
        assertEquals(listOf(hitch.id), event.lineIds)
        assertEquals(listOf("Roof rack fitted" to "washer"), event.replaced.map { it.text to it.catalogId })

        assertTrue(actions.undo(event))
        assertNull(service.record(id)!!.items.single { it.id == hitch.id }.catalogId)
        assertEquals(listOf("Roof rack fitted" to "washer"), t.db.catalogRules().all().map { it.text to it.catalogId })
        assertEquals(CatalogRules.key("Roof rack fitted"), t.db.catalogRules().all().single().textKey)
    }

    @Test
    fun `filing nothing changes nothing and announces nothing`() = runBlocking {
        actions.fileCustom(emptyList(), listOf(CatalogRule("Hitch greased", "oil")), day)
        assertTrue(t.db.catalogRules().all().isEmpty())
        noEvent()
    }

    @Test
    fun `two records link into one visit, and unlinking one of two leaves neither in a visit`() = runBlocking {
        val car = t.vehicle()
        val a = t.record(car, odometer = 40_000)
        val b = t.record(car, odometer = 40_010)
        actions.link(a, b)
        val visit = service.record(a)!!.record.visitId
        assertNotNull(visit)
        assertEquals(visit, service.record(b)!!.record.visitId)

        actions.unlink(a)
        assertNull(service.record(a)!!.record.visitId)
        assertNull(service.record(b)!!.record.visitId)

        // A record that isn't there links to nothing.
        actions.link(a, 9_999)
        assertNull(service.record(a)!!.record.visitId)
        actions.link(9_999, a)
        actions.unlink(9_999)
    }

    @Test
    fun `showIn points History at a vehicle without marking a record`() {
        actions.showIn(7)
        assertEquals(HistoryFocus(7), actions.focus.value)
    }

    @Test
    fun `a saved record has nothing to undo or discard`() = runBlocking {
        val photo = t.photoFile("record-x.jpg")
        assertTrue(actions.undo(RecordEvent.Saved(1)))
        actions.discard(RecordEvent.Saved(1))
        actions.discard(RecordEvent.RecallDone(1, null))
        actions.discard(RecordEvent.Filed(emptyList(), emptyList(), emptyList()))
        assertTrue(t.hasFile(photo))
    }

    @Test
    fun `an import adds and replaces in one go, announcing nothing`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car, odometer = 40_000, lines = listOf(line("Old wording")))
        val old = service.record(id)!!.record
        actions.applyImport(
            added = listOf(ServiceRecordEntity(vehicleId = car, date = day.minusDays(30), odometer = 39_000, diy = true, imported = true) to listOf(line("Tires rotated", "rotation"))),
            replaced = listOf(old.copy(notes = "From the file") to listOf(line("New wording"))),
        )
        val all = service.recordsFor(car)
        assertEquals(2, all.size)
        assertEquals("From the file", service.record(id)!!.record.notes)
        assertEquals(listOf("New wording"), service.record(id)!!.items.map { it.label })
        noEvent()
    }

    private suspend fun recordWith(car: Long) = service.record(t.record(car))!!

    private suspend fun deleteVehicle(id: Long) {
        t.db.vehicles().delete(t.db.vehicles().byId(id)!!)
    }
}
