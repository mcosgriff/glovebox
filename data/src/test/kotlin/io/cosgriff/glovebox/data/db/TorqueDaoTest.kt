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

/** Torque rows with names only and made-up figures: Glovebox has no factory torque values. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TorqueDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val torque = db.torque()

    @After fun close() = db.close()

    @Test
    fun `specs are listed by category, then name`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        torque.insertAll(
            listOf(
                TorqueSpecEntity(vehicleId = v, name = "Lug nuts", category = "WHEELS"),
                TorqueSpecEntity(vehicleId = v, name = "Drain plug", category = "ENGINE"),
                TorqueSpecEntity(vehicleId = v, name = "Caliper bracket", category = "BRAKES"),
            ),
        )
        assertEquals(listOf("Caliper bracket", "Drain plug", "Lug nuts"), torque.observe(v).first().map { it.name })
    }

    @Test
    fun `a spec is written in newton-metres, edited and deleted`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val id = torque.insert(TorqueSpecEntity(vehicleId = v, name = "Made-up bolt", category = "ENGINE", socketSize = "14 mm", newtonMeters = 10.0, note = "made-up"))
        val spec = torque.specsFor(v).single()
        assertEquals(id, spec.id)
        assertEquals("USER", spec.source)

        torque.setKnownList(id, "made-up")
        torque.edit(id, "Made-up bolt", "ENGINE", "14 mm", 12.5, "FACTORY", null)
        val edited = torque.specsFor(v).single()
        assertEquals(12.5, edited.newtonMeters!!, 0.0)
        assertEquals("FACTORY" to null, edited.source to edited.note)
        assertEquals("the list a row came from stays with it", "made-up", edited.knownList)

        torque.delete(torque.specsFor(v).single())
        assertEquals(emptyList<TorqueSpecEntity>(), torque.specsFor(v))
    }
}
