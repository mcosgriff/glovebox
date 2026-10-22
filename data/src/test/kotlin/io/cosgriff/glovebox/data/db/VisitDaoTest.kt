package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Through Android's own driver, as [MigrationTest] explains. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VisitDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    private val service = db.service()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private suspend fun record(vehicleId: Long, odometer: Int = 60_000) = service.insertWithItems(
        ServiceRecordEntity(vehicleId = vehicleId, date = day, odometer = odometer, diy = false, shopName = "Shop"),
        listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Work")),
    )

    private suspend fun visitOf(id: Long) = service.record(id)!!.record.visitId

    @Test
    fun `linked records share a visit, and a new visit never takes an id still held`() = runBlocking {
        val vehicleId = db.vehicles().insert(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day))
        val (a, b, c, d) = List(4) { record(vehicleId, 60_000 + it) }
        service.link(listOf(a, b))
        val first = visitOf(a)
        assertEquals(first, visitOf(b))
        service.link(listOf(c, d))
        assertNotEquals(first, visitOf(c))
        assertEquals(visitOf(c), visitOf(d))

        // Only the visit id changes.
        assertEquals(60_000, service.record(a)!!.record.odometer)
        assertEquals("Shop", service.record(a)!!.record.shopName)

        service.unlink(listOf(a, b))
        assertEquals(null, visitOf(a))
        assertEquals(null, visitOf(b))
    }

    @Test
    fun `an edit keeps the visit, and a deleted record put back by Undo rejoins it`() = runBlocking {
        val vehicleId = db.vehicles().insert(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day))
        val (a, b, c) = List(3) { record(vehicleId) }
        service.link(listOf(a, b, c))
        val visit = visitOf(a)

        val edited = service.record(a)!!
        service.replaceWithItems(edited.record.copy(odometer = 60_100), edited.items)
        assertEquals(visit, visitOf(a))

        // Undo of a delete inserts the record as it was, under a new id.
        val gone = service.record(b)!!
        service.deleteRecord(gone.record)
        assertEquals(visit, visitOf(c))
        val back = service.insertWithItems(gone.record.copy(id = 0), gone.items.map { it.copy(id = 0) })
        assertEquals(visit, visitOf(back))
    }
}
