package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ServiceDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val service = db.service()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun line(label: String, catalogId: String? = null, scheduleItemId: Long? = null) =
        ServiceRecordItemEntity(recordId = 0, scheduleItemId = scheduleItemId, label = label, catalogId = catalogId)

    private suspend fun record(vehicleId: Long, date: LocalDate, odometer: Int, shop: String? = null, imported: Boolean = false, vararg lines: ServiceRecordItemEntity) =
        service.insertWithItems(ServiceRecordEntity(vehicleId = vehicleId, date = date, odometer = odometer, diy = shop == null, shopName = shop, imported = imported), lines.toList())

    @Test
    fun `a vehicle's records come newest first, each with its lines and photos`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val other = db.vehicles().add(testVehicle(model = "GTI"), firstReading = null)
        val older = record(v, day.minusDays(30), 58_000, lines = arrayOf(line("Oil change", "oil_change")))
        val sameDayLower = record(v, day, 59_000)
        val newest = record(v, day, 60_000, shop = "Made-up Motors", lines = arrayOf(line("Tire rotation", "tire_rotation"), line("Wiper blades")))
        record(other, day, 10_000, shop = "Made-up Tires")
        service.insertPhotos(listOf(PhotoEntity(recordId = newest, fileName = "receipt.jpg")))

        val records = service.observeRecords(v).first()
        assertEquals(listOf(newest, sameDayLower, older), records.map { it.record.id })
        assertEquals(listOf("Tire rotation", "Wiper blades"), records.first().items.map { it.label }.sorted())
        assertEquals(listOf("receipt.jpg"), records.first().photos.map { it.fileName })

        assertEquals(4, service.observeAllRecords().first().size)
        assertEquals(3, service.recordsFor(v).size)
        assertEquals(newest, service.observeRecord(newest).first()!!.record.id)
        assertNull(service.observeRecord(999).first())
        assertEquals(1, service.photoCount(newest))
        assertEquals(setOf("Made-up Motors", "Made-up Tires"), service.observeShops().first().map { it.name }.toSet())
    }

    @Test
    fun `an edit replaces the record and its lines as one`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val id = record(v, day, 60_000, lines = arrayOf(line("Oil change", "oil_change"), line("Wipers")))
        val before = service.record(id)!!

        service.replaceWithItems(before.record.copy(odometer = 60_100, notes = "Made-up note"), before.items.take(1) + line("Cabin filter", "cabin_filter"))

        val after = service.record(id)!!
        assertEquals(60_100, after.record.odometer)
        assertEquals("Made-up note", after.record.notes)
        assertEquals(listOf("Cabin filter", "Oil change"), after.items.map { it.label }.sorted())
    }

    @Test
    fun `clearing history counts and takes imported records only, or every one`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val imported = record(v, day.minusDays(60), 50_000, imported = true)
        record(v, day, 60_000)
        service.insertPhotos(listOf(PhotoEntity(recordId = imported, fileName = "imported.jpg")))

        assertEquals(1, service.countRecords(v, all = false))
        assertEquals(2, service.countRecords(v, all = true))
        assertEquals(listOf("imported.jpg"), service.photoFilesOf(v, all = false))
        assertEquals(1, service.clearRecords(v, all = false))
        assertEquals(listOf(60_000), service.recordsFor(v).map { it.record.odometer })
    }

    @Test
    fun `an import adds and replaces records as one, and a replaced record keeps its id`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val kept = record(v, day, 60_000, lines = arrayOf(line("Oil change", "oil_change")))
        val existing = service.record(kept)!!.record

        service.applyImport(
            added = listOf(
                RecordRows(
                    ServiceRecordEntity(vehicleId = v, date = day.minusDays(90), odometer = 55_000, diy = false, imported = true),
                    listOf(line("Brake pads", "brake_pads_front").copy(jobId = 1)),
                    listOf(ServiceRecordJobEntity(id = 1, recordId = 0, laborCents = 9_000, partsCents = 6_000)),
                ),
            ),
            replaced = listOf(RecordRows(existing.copy(imported = true, shopName = "Made-up Motors"), listOf(line("Oil change", "oil_change"), line("Oil filter")))),
        )

        val records = service.recordsFor(v).associateBy { it.record.odometer }
        assertEquals(kept, records.getValue(60_000).record.id)
        assertEquals("Made-up Motors", records.getValue(60_000).record.shopName)
        assertEquals(2, records.getValue(60_000).items.size)
        assertEquals(listOf("Brake pads"), records.getValue(55_000).items.map { it.label })
        // An imported job is written with its record, and its line points at it.
        val job = records.getValue(55_000).jobs.single()
        assertEquals(9_000L to 6_000L, job.laborCents to job.partsCents)
        assertEquals(job.id, records.getValue(55_000).items.single().jobId)
    }

    @Test
    fun `photos are added, removed one by one or all at once`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val id = record(v, day, 60_000)
        service.insertPhotos(listOf(PhotoEntity(recordId = id, fileName = "a.jpg"), PhotoEntity(recordId = id, fileName = "b.jpg"), PhotoEntity(recordId = id, fileName = "c.jpg")))
        val a = service.record(id)!!.photos.single { it.fileName == "a.jpg" }

        service.deletePhotos(listOf(a.id))
        assertEquals(listOf("b.jpg", "c.jpg"), service.record(id)!!.photos.map { it.fileName }.sorted())
        service.clearPhotos(id)
        assertEquals(0, service.photoCount(id))
    }

    @Test
    fun `deleting a record takes its lines, and a recall it was the work for keeps its done mark`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val id = record(v, day, 60_000, lines = arrayOf(line("Recall work")))
        db.recalls().insertAll(listOf(RecallEntity(vehicleId = v, campaign = "99V000001", component = "Made-up", summary = "Made-up", firstSeenOn = day)))
        val recall = db.recalls().recallsFor(v).single()
        db.recalls().setDone(recall.id, day, id)
        assertEquals(listOf(recall.id), db.recalls().idsLinkedTo(id))

        service.deleteRecord(service.record(id)!!.record)

        assertNull(service.record(id))
        val after = db.recalls().byId(recall.id)!!
        assertEquals(day, after.completedOn)
        assertNull(after.serviceRecordId)
    }

    @Test
    fun `rules can be looked up by their wording`() = runBlocking {
        service.putRules(listOf(CatalogRuleEntity(text = "Lube oil filter", textKey = "lube oil filter", catalogId = "oil_change", createdOn = day)))
        assertEquals(listOf("oil_change"), service.rulesFor(listOf("lube oil filter", "unknown")).map { it.catalogId })
        service.deleteRules(listOf("lube oil filter"))
        assertEquals(emptyList<CatalogRuleEntity>(), service.rulesFor(listOf("lube oil filter")))
    }
}
