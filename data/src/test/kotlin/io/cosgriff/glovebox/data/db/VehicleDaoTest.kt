package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Through Android's own driver, as [MigrationTest] explains. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VehicleDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    @Test
    fun `a new vehicle comes with its first reading, dated the day it was added`() = runBlocking {
        val id = db.vehicles().add(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day), firstReading = 61_000)
        assertEquals(listOf(day to 61_000), db.odometer().readingsFor(id).map { it.date to it.odometer })

        val none = db.vehicles().add(VehicleEntity(vin = null, year = 2013, make = "Volkswagen", model = "GTI", createdOn = day), firstReading = null)
        assertEquals(emptyList<OdometerReadingEntity>(), db.odometer().readingsFor(none))
    }

    @Test
    fun `every encrypted file is found by the one query, photos, vehicle photos and documents alike`() = runBlocking {
        val id = db.vehicles().add(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day, photoFile = "vehicle.enc"), firstReading = null)
        val record = db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = day, odometer = 60_000, diy = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Work")),
        )
        db.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = "receipt.enc")))
        db.documents().insert(DocumentEntity(vehicleId = id, type = "TITLE", name = "Title", fileName = "document-title.enc", format = "PDF", sizeBytes = 1, addedOn = day))
        assertEquals(setOf("vehicle.enc", "receipt.enc", "document-title.enc"), db.documents().allEncryptedFiles().toSet())
    }
}
