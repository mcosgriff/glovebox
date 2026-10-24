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
}
