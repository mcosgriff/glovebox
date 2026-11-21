package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.DoneChange
import io.cosgriff.glovebox.domain.InvoiceCharges
import io.cosgriff.glovebox.domain.RecommendedItem
import io.cosgriff.glovebox.domain.RecordJob
import io.cosgriff.glovebox.domain.RecordLine
import io.cosgriff.glovebox.domain.RecordPartRow
import io.cosgriff.glovebox.domain.ServiceRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Through Android's own driver, as [MigrationTest] explains. Every record goes in and comes back as a [ServiceRecord]
 * (#617); what each write announces, Undo's events and the photo files are `RecordActionsTest`'s.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ServiceLogWriterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val service = db.service()
    private val recalls = db.recalls()
    private val writer = ServiceLogWriter(db)
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun record(vehicleId: Long, vararg lines: RecordLine = arrayOf(RecordLine("Work"))) =
        ServiceRecord(vehicleId = vehicleId, date = day, odometer = 41_000, diy = true, lines = lines.toList())

    private suspend fun saved(id: Long): ServiceRecord = service.record(id)!!.toModel()

    /** Made-up shop, work and figures: plugs and coils as one job with its part rows, and a rotation alone. */
    private fun invoice(vehicleId: Long) = ServiceRecord(
        vehicleId = vehicleId, date = day, odometer = 41_000, diy = false, shopName = "Made-up Motors", costCents = 155_260,
        notes = "Odometer in 40,998",
        lines = listOf(RecordLine("Spark plugs", "spark_plugs", job = 0), RecordLine("Ignition coils", job = 0), RecordLine("Tire rotation", "rotation")),
        jobs = listOf(
            RecordJob(
                "Remove & replace spark plugs and coils", laborCents = 30_000,
                rows = listOf(
                    RecordPartRow("Spark plug, iridium", 4_000, 1_250, 5_000, line = 0),
                    RecordPartRow("Ignition coil", 4_000, 9_250, 37_000, line = 1),
                    RecordPartRow("Dielectric grease", 3_000, totalCents = 2_000),
                ),
            ),
        ),
        charges = InvoiceCharges(3_850, 5_210, 1_200),
    )

    private suspend fun recall(vehicleId: Long, campaign: String = "26V000000") = recalls.insertAll(
        listOf(RecallEntity(vehicleId = vehicleId, campaign = campaign, component = "Made up", summary = "", firstSeenOn = day)),
    ).let { recalls.recallsFor(vehicleId).single { it.campaign == campaign } }

    @Test
    fun `DAO calls inside one write are kept together or not at all`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val failed = runCatching {
            writer.transaction {
                service.insertWithItems(ServiceRecordEntity(vehicleId = car, date = day, odometer = 41_000, diy = true), emptyList())
                error("the process died here")
            }
        }
        assertTrue(failed.isFailure)
        assertEquals(emptyList<ServiceRecordWithItems>(), service.recordsFor(car))
    }

    @Test
    fun `a new record is written whole and reads back as it was given, with its photos`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val id = writer.add(invoice(car), listOf("receipt.jpg"))
        assertEquals(invoice(car).copy(id = id), saved(id))
        assertEquals(listOf("receipt.jpg"), service.record(id)!!.photos.map { it.fileName })
    }

    @Test
    fun `a job that says nothing isn't written on a save or an edit, but an import writes the file's as given`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        // One line alone in a job of its own, unpriced and unworded: a line as it always was.
        val bare = record(car, RecordLine("Tire rotation", "rotation", job = 0)).copy(jobs = listOf(RecordJob()))
        val id = writer.add(bare)
        assertEquals(emptyList<RecordJob>(), saved(id).jobs)
        assertEquals(listOf<Int?>(null), saved(id).lines.map { it.job })

        writer.add(invoice(car)).let { priced ->
            // Its prices and part rows taken off, and the coils priced on their own again: the plugs' job says nothing.
            writer.replace(saved(priced).let { r -> r.copy(lines = r.lines.mapIndexed { i, line -> line.copy(job = 0.takeIf { i == 0 }) }, jobs = listOf(RecordJob())) })
            assertEquals(emptyList<RecordJob>(), saved(priced).jobs)
        }

        writer.applyImport(added = listOf(bare.copy(date = day.minusDays(1), imported = true)), replaced = emptyList())
        val imported = service.recordsFor(car).single { it.record.date == day.minusDays(1) }.toModel()
        assertEquals(listOf(RecordJob()), imported.jobs)
    }

    @Test
    fun `an edit is written in the record's place, and its revert puts it back as it was, photos included`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val id = writer.add(invoice(car), listOf("receipt.jpg", "page.jpg"))
        val before = service.record(id)!!
        val page = before.photos.single { it.fileName == "page.jpg" }

        // The rows dropped, the rotation priced on its own, a reading corrected and one photo swapped for another.
        val edit = saved(id).let { r ->
            r.copy(
                odometer = 41_100,
                lines = r.lines.mapIndexed { i, line -> if (i == 2) line.copy(job = 1) else line },
                jobs = listOf(r.jobs.single().copy(rows = emptyList(), partsCents = 44_000, itemized = false), RecordJob(laborCents = 2_500)),
            )
        }
        writer.replace(edit, addedPhotos = listOf("new.jpg"), removedPhotoIds = setOf(page.id))
        assertEquals(edit, saved(id))
        assertEquals(setOf("receipt.jpg", "new.jpg"), service.record(id)!!.photos.map { it.fileName }.toSet())

        assertTrue(writer.revertEdit(before))
        assertEquals(invoice(car).copy(id = id), saved(id))
        assertEquals(setOf("receipt.jpg", "page.jpg"), service.record(id)!!.photos.map { it.fileName }.toSet())
    }

    @Test
    fun `a deleted record comes back whole, and not on a vehicle that has gone`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val id = writer.add(invoice(car), listOf("receipt.jpg"))
        val before = service.record(id)!!
        writer.delete(before.record)
        assertNull(service.record(id))

        val back = writer.restore(before, recallIds = emptyList())!!
        assertEquals(invoice(car).copy(id = back), saved(back))
        assertEquals(listOf("receipt.jpg"), service.record(back)!!.photos.map { it.fileName })

        db.vehicles().delete(db.vehicles().byId(car)!!)
        assertNull(writer.restore(before, recallIds = emptyList()))
    }

    @Test
    fun `a bare last done is one line, flagged, filed under the catalog line that resets its reminder`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        db.schedule().applyImport(
            listOf(ScheduleItemEntity(vehicleId = car, name = "Engine oil", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6)),
            emptyList(), emptyList(),
        )
        val item = db.schedule().allFor(car).single()

        val id = writer.addLastDone(car, item, day, 38_000)

        assertEquals(
            ServiceRecord(id = id, vehicleId = car, date = day, odometer = 38_000, diy = true, enteredAsLastDone = true, lines = listOf(RecordLine("Engine oil", "oil", item.id))),
            saved(id),
        )
    }

    @Test
    fun `a recall done with its work is one record that day, its line the recall's and resetting nothing, and a second time adds nothing`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val recall = recall(car)
        val words = { notice: io.cosgriff.glovebox.domain.RecallNotice -> "Recall ${notice.campaign}: ${notice.component}" }

        val id = writer.completeRecall(recall.id, day, RecallWork(43_000, "Made-up Motors"), line = words)!!.recordId!!

        val done = recalls.byId(recall.id)!!
        assertEquals(day, done.completedOn)
        assertEquals(id, done.serviceRecordId)
        assertEquals(
            ServiceRecord(id = id, vehicleId = car, date = day, odometer = 43_000, diy = false, shopName = "Made-up Motors", lines = listOf(RecordLine("Recall 26V000000: Made up"))),
            saved(id),
        )

        assertNull(writer.completeRecall(recall.id, day, RecallWork(43_000), line = words))
        assertEquals(1, service.recordsFor(car).size)
    }

    @Test
    fun `a recall done without its work adds no record, and one with it can join the visit it was done on`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        assertNull(writer.completeRecall(recall(car).id, day, work = null) { "never asked" }!!.recordId)
        assertTrue(service.recordsFor(car).isEmpty())

        val other = writer.add(record(car))
        writer.completeRecall(recall(car, "26V000111").id, day, RecallWork(41_000), visitWith = other) { it.campaign }
        val records = service.recordsFor(car).map { it.record }
        assertEquals(2, records.size)
        assertNotNull(records.first().visitId)
        assertEquals(1, records.map { it.visitId }.toSet().size)
    }

    @Test
    fun `a recall opened again loses the record marking it done added, and its redo puts both back`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val recall = recall(car)
        writer.completeRecall(recall.id, day, RecallWork(43_000)) { it.campaign }

        val reopened = writer.reopenRecall(recall.id)!!
        assertTrue(service.recordsFor(car).isEmpty())
        assertNull(recalls.byId(recall.id)!!.completedOn)
        assertNull(writer.reopenRecall(recall.id))

        writer.redoRecall(reopened)
        val back = service.recordsFor(car).single()
        assertEquals(back.record.id, recalls.byId(recall.id)!!.serviceRecordId)
        assertEquals(listOf("26V000000"), back.items.map { it.label })
    }

    @Test
    fun `an import adds and replaces in one go, a replaced record keeping its id and photos`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val id = writer.add(record(car, RecordLine("Old wording")), listOf("receipt.jpg"))
        val file = record(car, RecordLine("Tires rotated", "rotation")).copy(date = day.minusDays(30), odometer = 39_000, imported = true)

        writer.applyImport(added = listOf(file), replaced = listOf(saved(id).copy(notes = "From the file", imported = true, lines = listOf(RecordLine("New wording")))))

        val all = service.recordsFor(car)
        assertEquals(2, all.size)
        assertEquals(file.copy(id = all.single { it.record.id != id }.record.id), all.single { it.record.id != id }.toModel())
        assertEquals("From the file", saved(id).notes)
        assertEquals(listOf("New wording"), saved(id).lines.map { it.label })
        assertEquals(listOf("receipt.jpg"), service.record(id)!!.photos.map { it.fileName })
    }

    private fun kept(wording: String, catalogId: String? = null, cents: Long = 22_999, replaces: Long? = null) =
        KeptRecommendation(RecommendedItem(wording, cents), catalogId, replaces)

    private suspend fun open(car: Long) = db.recommendations().all(car).map { it.toModel() }.filter { it.open }

    @Test
    fun `a record keeps the shop's recommendations, replaces the shop's older one and closes what it did`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val shop = record(car).copy(diy = false, shopName = "Cedar Hollow Auto & Truck")
        val first = writer.add(shop, kept = listOf(kept("Coolant System Service", "coolant"), kept("Battery Replacement", "battery")))
        val older = open(car)
        assertEquals(listOf("Coolant System Service", "Battery Replacement"), older.map { it.item.text })
        assertEquals("Cedar Hollow Auto & Truck", older[0].shop)
        assertEquals(day, older[0].date)

        // A later visit: the shop recommends the coolant again, and the owner did the battery.
        val coolant = older.single { it.line == "coolant" }
        val battery = older.single { it.line == "battery" }
        val second = writer.add(
            shop.copy(date = day.plusDays(30)),
            kept = listOf(kept("Coolant System Service", "coolant", cents = 23_999, replaces = coolant.id)),
            done = listOf(battery.id),
        )
        val now = db.recommendations().all(car).map { it.toModel() }
        assertEquals(listOf(23_999L), now.filter { it.open }.map { it.item.cents })
        assertEquals(day.plusDays(30), now.single { it.id == battery.id }.doneOn)
        assertTrue(now.single { it.id == coolant.id }.replacedBy != null)
        assertTrue(first != second)
    }

    @Test
    fun `a kept finding keeps its area, what was found and its colour`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val finding = RecommendedItem("Oil pan seeping", null, "Fluid Leaks", "Seeping at the gasket", io.cosgriff.glovebox.domain.FindingColour.AMBER)
        writer.add(record(car), kept = listOf(KeptRecommendation(finding)))
        val kept = open(car).single()
        assertEquals(finding, kept.item)
        assertNull(kept.line)
    }

    @Test
    fun `deleting a record takes what it kept and opens what it closed, and its restore puts both back`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val shop = record(car).copy(diy = false, shopName = "Cedar Hollow Auto & Truck")
        writer.add(shop, kept = listOf(kept("Coolant System Service", "coolant"), kept("Battery Replacement", "battery")))
        val (coolant, battery) = open(car)
        val id = writer.add(
            shop.copy(date = day.plusDays(30)),
            kept = listOf(kept("Coolant System Service", "coolant", replaces = coolant.id)),
            done = listOf(battery.id),
        )
        val saved = service.record(id)!!

        val was = writer.delete(saved.record)
        assertEquals(listOf(coolant.id, battery.id), open(car).map { it.id })

        writer.restore(saved, emptyList(), was)
        val back = db.recommendations().all(car).map { it.toModel() }
        assertEquals(listOf("Coolant System Service"), back.filter { it.open }.map { it.item.text })
        assertTrue(back.filter { it.open }.single().recordId == id)
        assertEquals(id, back.single { it.id == battery.id }.doneBy)
        assertTrue(back.single { it.id == coolant.id }.replacedBy != null)
    }

    @Test
    fun `an edit closes and opens again what it says, and its revert undoes both`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val shop = record(car).copy(diy = false, shopName = "Cedar Hollow Auto & Truck")
        writer.add(shop, kept = listOf(kept("Coolant System Service", "coolant"), kept("Battery Replacement", "battery")))
        val (coolant, battery) = open(car)
        val id = writer.add(record(car).copy(date = day.plusDays(30)), done = listOf(coolant.id))
        val before = service.record(id)!!

        val change = DoneChange(close = listOf(battery.id), reopen = listOf(coolant.id))
        writer.replace(before.toModel().copy(odometer = 42_000), done = change)
        assertEquals(listOf(coolant.id), open(car).map { it.id })
        assertEquals(id, db.recommendations().all(car).map { it.toModel() }.single { it.id == battery.id }.doneBy)

        writer.revertEdit(before, change)
        assertEquals(listOf(battery.id), open(car).map { it.id })
        assertEquals(id, db.recommendations().all(car).map { it.toModel() }.single { it.id == coolant.id }.doneBy)
    }

    @Test
    fun `an import closes by the record each was offered for, added or replaced, and opens what a replaced one no longer did`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val shop = record(car).copy(diy = false, shopName = "Cedar Hollow Auto & Truck")
        writer.add(shop, kept = listOf(kept("Coolant System Service", "coolant"), kept("Battery Replacement", "battery"), kept("Wipers", "wipers")))
        val (coolant, battery, wipers) = open(car)
        val saved = writer.add(record(car).copy(date = day.plusDays(10)), done = listOf(wipers.id))

        writer.applyImport(
            added = listOf(record(car).copy(date = day.plusDays(20), imported = true)),
            replaced = listOf(saved(saved).copy(imported = true)),
            done = listOf(ImportClose(coolant.id, added = 0), ImportClose(battery.id, savedId = saved)),
            reopened = listOf(wipers.id),
        )
        val now = db.recommendations().all(car).map { it.toModel() }.associateBy { it.id }
        val added = service.recordsFor(car).single { it.record.date == day.plusDays(20) }.record.id
        assertEquals(added, now.getValue(coolant.id).doneBy)
        assertEquals(saved, now.getValue(battery.id).doneBy)
        assertTrue(now.getValue(wipers.id).open)
    }

    @Test
    fun `deleting the vehicle takes its recommendations`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        writer.add(record(car), kept = listOf(kept("Wiper Blade Replacement")))
        db.vehicles().delete(db.vehicles().byId(car)!!)
        assertEquals(emptyList<ShopRecommendationRow>(), db.recommendations().all(car))
    }

    @Test
    fun `two records link into one visit, and unlinking one of two leaves neither in a visit`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val a = writer.add(record(car))
        val b = writer.add(record(car).copy(odometer = 41_010))
        writer.link(a, b)
        val visit = saved(a).visitId
        assertNotNull(visit)
        assertEquals(visit, saved(b).visitId)

        writer.unlink(a)
        assertNull(saved(a).visitId)
        assertNull(saved(b).visitId)

        // A record that isn't there links to nothing.
        writer.link(a, 9_999)
        assertNull(saved(a).visitId)
        writer.link(9_999, a)
        writer.unlink(9_999)
    }
}
