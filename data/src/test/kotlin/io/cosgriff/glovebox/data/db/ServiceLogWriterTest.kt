package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Through Android's own driver, as [MigrationTest] explains. What each write announces is `RecordActionsTest`'s. */
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

    private fun record(vehicleId: Long) = ServiceRecordEntity(vehicleId = vehicleId, date = day, odometer = 41_000, diy = true)
    private val line = ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Work")

    private suspend fun recall(vehicleId: Long) = recalls.insertAll(
        listOf(RecallEntity(vehicleId = vehicleId, campaign = "26V000000", component = "Made up", summary = "", firstSeenOn = day)),
    ).let { recalls.recallsFor(vehicleId).single() }

    @Test
    fun `DAO calls inside one write are kept together or not at all`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val failed = runCatching {
            writer.transaction {
                service.insertWithItems(record(car), listOf(line))
                error("the process died here")
            }
        }
        assertTrue(failed.isFailure)
        assertEquals(emptyList<ServiceRecordWithItems>(), service.recordsFor(car))
    }

    @Test
    fun `a recall done with a record is done, linked to it, and a second time adds nothing`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val recall = recall(car)
        val id = writer.completeRecall(recall.id, day, record(car), listOf(line))!!.recordId
        val done = recalls.byId(recall.id)!!
        assertEquals(day, done.completedOn)
        assertEquals(id, done.serviceRecordId)

        assertNull(writer.completeRecall(recall.id, day, record(car), listOf(line)))
        assertEquals(1, service.recordsFor(car).size)
    }

    private fun kept(wording: String, catalogId: String? = null) =
        ShopRecommendationEntity(vehicleId = 0, recordId = 0, wording = wording, cents = 22_999, catalogId = catalogId)

    private suspend fun open(car: Long) = db.recommendations().all(car).map { it.toModel() }.filter { it.open }

    @Test
    fun `a record keeps the shop's recommendations, replaces the shop's older one and closes what it did`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val shop = record(car).copy(diy = false, shopName = "Cedar Hollow Auto & Truck")
        val first = writer.add(shop, listOf(line), kept = listOf(KeptRecommendation(kept("Coolant System Service", "coolant")), KeptRecommendation(kept("Battery Replacement", "battery"))))
        val older = open(car)
        assertEquals(listOf("Coolant System Service", "Battery Replacement"), older.map { it.item.text })
        assertEquals("Cedar Hollow Auto & Truck", older[0].shop)
        assertEquals(day, older[0].date)

        // A later visit: the shop recommends the coolant again, and the owner did the battery.
        val coolant = older.single { it.line == "coolant" }
        val battery = older.single { it.line == "battery" }
        val second = writer.add(
            shop.copy(date = day.plusDays(30)),
            listOf(line),
            kept = listOf(KeptRecommendation(kept("Coolant System Service", "coolant").copy(cents = 23_999), replaces = coolant.id)),
            done = listOf(battery.id),
        )
        val now = db.recommendations().all(car).map { it.toModel() }
        assertEquals(listOf(23_999L), now.filter { it.open }.map { it.item.cents })
        assertEquals(day.plusDays(30), now.single { it.id == battery.id }.doneOn)
        assertTrue(now.single { it.id == coolant.id }.replacedBy != null)
        assertTrue(first != second)
    }

    @Test
    fun `deleting a record takes what it kept and opens what it closed, and its restore puts both back`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val shop = record(car).copy(diy = false, shopName = "Cedar Hollow Auto & Truck")
        writer.add(shop, listOf(line), kept = listOf(KeptRecommendation(kept("Coolant System Service", "coolant")), KeptRecommendation(kept("Battery Replacement", "battery"))))
        val (coolant, battery) = open(car)
        val id = writer.add(
            shop.copy(date = day.plusDays(30)),
            listOf(line),
            kept = listOf(KeptRecommendation(kept("Coolant System Service", "coolant"), replaces = coolant.id)),
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
        writer.add(shop, listOf(line), kept = listOf(KeptRecommendation(kept("Coolant System Service", "coolant")), KeptRecommendation(kept("Battery Replacement", "battery"))))
        val (coolant, battery) = open(car)
        val id = writer.add(record(car).copy(date = day.plusDays(30)), listOf(line), done = listOf(coolant.id))
        val before = service.record(id)!!

        val change = io.cosgriff.glovebox.domain.DoneChange(close = listOf(battery.id), reopen = listOf(coolant.id))
        writer.replace(before.record.copy(odometer = 42_000), before.items.map { it.copy(id = 0) }, emptyList(), emptySet(), done = change)
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
        writer.add(shop, listOf(line), kept = listOf(KeptRecommendation(kept("Coolant System Service", "coolant")), KeptRecommendation(kept("Battery Replacement", "battery")), KeptRecommendation(kept("Wipers", "wipers"))))
        val (coolant, battery, wipers) = open(car)
        val saved = writer.add(record(car).copy(date = day.plusDays(10)), listOf(line), done = listOf(wipers.id))
        val replacing = service.record(saved)!!

        writer.applyImport(
            added = listOf(RecordRows(record(car).copy(date = day.plusDays(20), imported = true), listOf(line), emptyList(), emptyList())),
            replaced = listOf(RecordRows(replacing.record.copy(imported = true), listOf(line), emptyList(), emptyList())),
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
        writer.add(record(car), listOf(line), kept = listOf(KeptRecommendation(kept("Wiper Blade Replacement"))))
        db.vehicles().delete(db.vehicles().byId(car)!!)
        assertEquals(emptyList<ShopRecommendationRow>(), db.recommendations().all(car))
    }

    @Test
    fun `a record isn't put back on a vehicle that has gone`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val id = writer.add(record(car), listOf(line), listOf("receipt.jpg"))
        val saved = service.record(id)!!
        db.vehicles().delete(db.vehicles().byId(car)!!)

        assertNull(writer.restore(saved, recallIds = emptyList()))
    }

    @Test
    fun `jobs are written with their lines, and an edit and its revert carry them`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val plugs = ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Spark plugs", catalogId = "spark_plugs", jobId = 7)
        val coils = ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Ignition coils", jobId = 7)
        val rotation = ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Tire rotation")
        val job = ServiceRecordJobEntity(id = 7, recordId = 0, wording = "Replace spark plugs and ignition coils", laborCents = 30_000, partsCents = 42_000)
        val id = writer.add(record(car), listOf(plugs, coils, rotation), jobs = listOf(job))

        val saved = service.record(id)!!
        val written = saved.jobs.single()
        assertEquals(job.copy(id = written.id, recordId = id), written)
        assertEquals(listOf(written.id, written.id, null), saved.items.map { it.jobId })

        // An edit that prices nothing leaves no job behind.
        writer.replace(saved.record, saved.items.map { it.copy(jobId = null) }, emptyList(), emptySet())
        assertEquals(emptyList<ServiceRecordJobEntity>(), service.record(id)!!.jobs)

        assertTrue(writer.revertEdit(saved))
        val back = service.record(id)!!
        assertEquals(1, back.jobs.size)
        assertEquals(listOf(back.jobs.single().id, back.jobs.single().id, null), back.items.map { it.jobId })
    }

    @Test
    fun `a deleted record comes back with its jobs`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val id = writer.add(
            record(car),
            listOf(line.copy(jobId = 1)),
            jobs = listOf(ServiceRecordJobEntity(id = 1, recordId = 0, laborCents = 18_000)),
        )
        val saved = service.record(id)!!
        service.deleteRecord(saved.record)

        val back = service.record(writer.restore(saved, recallIds = emptyList())!!)!!
        assertEquals(18_000L, back.jobs.single().laborCents)
        assertEquals(back.jobs.single().id, back.items.single().jobId)
    }

    @Test
    fun `part rows and invoice charges are written, edited, reverted and restored with their job`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        // Lines given ids of their own only so the rows can say which one they are for.
        val plugs = ServiceRecordItemEntity(id = 1, recordId = 0, scheduleItemId = null, label = "Spark plugs", jobId = 7)
        val coils = ServiceRecordItemEntity(id = 2, recordId = 0, scheduleItemId = null, label = "Ignition coils", jobId = 7)
        val job = ServiceRecordJobEntity(id = 7, recordId = 0, wording = "Remove & replace spark plugs and coils", laborCents = 30_000)
        val rows = listOf(
            ServiceRecordJobPartEntity(jobId = 7, wording = "Spark plug, iridium", quantityMilli = 4_000, eachCents = 1_250, totalCents = 5_000, itemId = 1, position = 0),
            ServiceRecordJobPartEntity(jobId = 7, wording = "Ignition coil", quantityMilli = 4_000, eachCents = 9_250, totalCents = 37_000, itemId = 2, position = 1),
            ServiceRecordJobPartEntity(jobId = 7, wording = "Dielectric grease", quantityMilli = 3_000, totalCents = 2_000, position = 2),
        )
        val charged = record(car).copy(costCents = 155_260, shopSuppliesCents = 3_850, taxCents = 5_210, feesCents = 1_200)
        val id = writer.add(charged, listOf(plugs, coils), jobs = listOf(job), parts = rows)

        val saved = service.record(id)!!
        assertEquals(listOf(3_850L, 5_210L, 1_200L), saved.record.let { listOf(it.shopSuppliesCents, it.taxCents, it.feesCents) })
        val jobId = saved.jobs.single().id
        val byLabel = saved.items.associate { it.label to it.id }
        val parts = saved.parts.sortedBy { it.position }
        assertEquals(listOf(jobId, jobId, jobId), parts.map { it.jobId })
        // Each row still names its line under the line's new id; the untagged one is for the whole job.
        assertEquals(listOf(byLabel["Spark plugs"], byLabel["Ignition coils"], null), parts.map { it.itemId })
        assertEquals(rows.map { it.copy(id = 0, jobId = 0, itemId = null) }, parts.map { it.copy(id = 0, jobId = 0, itemId = null) })

        // An edit that drops the rows leaves none behind.
        writer.replace(saved.record, saved.items, emptyList(), emptySet(), jobs = saved.jobs)
        assertEquals(emptyList<ServiceRecordJobPartEntity>(), service.record(id)!!.parts)

        assertTrue(writer.revertEdit(saved))
        val back = service.record(id)!!
        val backLabels = back.items.associate { it.id to it.label }
        assertEquals(listOf("Spark plugs", "Ignition coils", null), back.parts.sortedBy { it.position }.map { it.itemId?.let(backLabels::get) })

        service.deleteRecord(back.record)
        // Gone with its record, by cascade through the job.
        assertEquals(emptyList<ServiceRecordJobPartEntity>(), service.record(id)?.parts.orEmpty())
        val restored = service.record(writer.restore(back, recallIds = emptyList())!!)!!
        assertEquals(3, restored.parts.size)
        assertEquals(restored.jobs.single().id, restored.parts.map { it.jobId }.distinct().single())
        assertEquals(5_210L, restored.record.taxCents)
    }
}
