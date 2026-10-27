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
}
