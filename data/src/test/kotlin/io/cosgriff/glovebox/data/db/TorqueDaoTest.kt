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

        torque.update(spec.copy(newtonMeters = 12.5))
        assertEquals(12.5, torque.specsFor(v).single().newtonMeters!!, 0.0)

        torque.delete(torque.specsFor(v).single())
        assertEquals(emptyList<TorqueSpecEntity>(), torque.specsFor(v))
    }

    @Test
    fun `an import adds and replaces, and deletes nothing`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        torque.insertAll(listOf(TorqueSpecEntity(vehicleId = v, name = "A", category = "ENGINE"), TorqueSpecEntity(vehicleId = v, name = "B", category = "ENGINE")))
        val a = torque.specsFor(v).single { it.name == "A" }

        torque.applyImport(added = listOf(TorqueSpecEntity(vehicleId = v, name = "C", category = "ENGINE", newtonMeters = 5.0)), replaced = listOf(a.copy(newtonMeters = 7.0, knownList = "made-up")))

        val after = torque.specsFor(v).associateBy { it.name }
        assertEquals(setOf("A", "B", "C"), after.keys)
        assertEquals(7.0, after.getValue("A").newtonMeters!!, 0.0)
        assertEquals("made-up", after.getValue("A").knownList)
    }
}
