package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OdometerDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val odometer = db.odometer()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    @Test
    fun `readings are listed by date, per vehicle and for the garage`() = runBlocking {
        val a = db.vehicles().add(testVehicle(), firstReading = null)
        val b = db.vehicles().add(testVehicle(model = "GTI"), firstReading = null)
        odometer.insert(OdometerReadingEntity(vehicleId = a, date = day, odometer = 61_000))
        odometer.insert(OdometerReadingEntity(vehicleId = a, date = day.minusDays(30), odometer = 60_000))
        odometer.insert(OdometerReadingEntity(vehicleId = b, date = day.minusDays(10), odometer = 10_000))

        assertEquals(listOf(60_000, 61_000), odometer.observe(a).first().map { it.odometer })
        assertEquals(listOf(60_000, 10_000, 61_000), odometer.observeAll().first().map { it.odometer })
    }

    @Test
    fun `a reading is corrected, deleted, and put back as it was by Undo`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val id = odometer.insert(OdometerReadingEntity(vehicleId = v, date = day, odometer = 610_000))
        val typed = odometer.readingsFor(v).single()

        odometer.correct(id, day.minusDays(1), 61_000)
        assertEquals(listOf(day.minusDays(1) to 61_000), odometer.readingsFor(v).map { it.date to it.odometer })

        // Undo of the correction.
        odometer.put(typed)
        assertEquals(listOf(typed), odometer.readingsFor(v))

        odometer.delete(id)
        assertEquals(emptyList<OdometerReadingEntity>(), odometer.readingsFor(v))
        // Undo of the delete: back under its own id.
        odometer.put(typed)
        assertEquals(listOf(typed), odometer.readingsFor(v))
    }
}
