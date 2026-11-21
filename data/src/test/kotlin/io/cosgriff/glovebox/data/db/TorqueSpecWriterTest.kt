package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.KnownSpecList
import io.cosgriff.glovebox.domain.KnownSpecSource
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueImport
import io.cosgriff.glovebox.domain.TorqueSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A torque file and a bundled list written into a vehicle's rows (#645), through Android's own driver, as
 * [MigrationTest] explains. Every figure is made up: Glovebox has no factory torque values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TorqueSpecWriterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val torque = db.torque()
    private val writer = TorqueSpecWriter(db)

    @After fun close() = db.close()

    /** A made-up file: a fastener the vehicle has only a name for, one it has a figure for, and a new one. */
    private val file = listOf(
        ImportedTorqueSpec("Made-up drain plug", TorqueCategory.ENGINE, socket = "14 mm", torque = Torque(30.0), source = TorqueSource.FACTORY, note = "New washer"),
        ImportedTorqueSpec("Made-up wheel nuts", TorqueCategory.WHEELS, socket = "21 mm", torque = Torque(100.0), source = TorqueSource.FACTORY),
        ImportedTorqueSpec("Made-up bracket", TorqueCategory.OTHER, torque = Torque(20.0)),
    )

    private suspend fun vehicle(): Long {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        torque.insertAll(
            listOf(
                TorqueSpecEntity(vehicleId = v, name = "Made-up drain plug", category = "ENGINE"),
                TorqueSpecEntity(vehicleId = v, name = "Made-up wheel nuts", category = "WHEELS", newtonMeters = 90.0, note = "Mine", knownList = "made-up-list"),
            ),
        )
        return v
    }

    private suspend fun rows(v: Long) = torque.specsFor(v).associateBy { it.name }

    private suspend fun preview(v: Long) = TorqueImport.plan(file, torque.specsFor(v).map { it.toSaved() })

    @Test
    fun `Add new only adds the new rows and fills the blank ones, and leaves a typed figure as it is`() = runBlocking {
        val v = vehicle()

        writer.import(v, preview(v), ImportMode.ADD_NEW)

        val after = rows(v)
        assertEquals(setOf("Made-up drain plug", "Made-up wheel nuts", "Made-up bracket"), after.keys)
        val plug = after.getValue("Made-up drain plug")
        assertEquals(listOf<Any?>("14 mm", 30.0, "FACTORY", "New washer"), listOf(plug.socketSize, plug.newtonMeters, plug.source, plug.note))
        assertEquals(90.0, after.getValue("Made-up wheel nuts").newtonMeters!!, 0.0)
        assertEquals("USER", after.getValue("Made-up bracket").source)
    }

    @Test
    fun `an import planned before an edit writes only a fastener's setting over it`() = runBlocking {
        val v = vehicle()
        val preview = preview(v)
        val nuts = rows(v).getValue("Made-up wheel nuts").id
        assertEquals(listOf(nuts), preview.differs.map { it.second.id })

        // Between the preview and Import, the owner renames the row in the editor; it is still the same fastener.
        torque.edit(nuts, "MADE-UP WHEEL NUTS", "WHEELS", null, 90.0, "USER", "Mine")

        writer.import(v, preview, ImportMode.REPLACE_MATCHING)

        val row = torque.specsFor(v).single { it.id == nuts }
        assertEquals(listOf<Any?>("21 mm", 100.0, "FACTORY", null), listOf(row.socketSize, row.newtonMeters, row.source, row.note))
        assertEquals("the owner's name, which the file doesn't set, stays", "MADE-UP WHEEL NUTS", row.name)
        assertEquals("the list a row came from stays with it", "made-up-list", row.knownList)
    }

    @Test
    fun `a row given a figure since the preview is not filled, and a row added since is not added twice`() = runBlocking {
        val v = vehicle()
        val preview = preview(v)
        val plug = rows(v).getValue("Made-up drain plug").id
        torque.edit(plug, "Made-up drain plug", "ENGINE", "13 mm", 25.0, "USER", null)
        torque.insert(TorqueSpecEntity(vehicleId = v, name = "Made-up bracket", category = "OTHER", newtonMeters = 22.0))

        writer.import(v, preview, ImportMode.ADD_NEW)

        val after = torque.specsFor(v)
        assertEquals(3, after.size)
        assertEquals(25.0, after.single { it.id == plug }.newtonMeters!!, 0.0)
        assertEquals(22.0, after.single { it.name == "Made-up bracket" }.newtonMeters!!, 0.0)
    }

    @Test
    fun `a bundled list adds its values as references, marked with the list, and fills only rows that are a name`() = runBlocking {
        val v = vehicle()
        val list = KnownSpecList(
            id = "made-up-specs", title = "Made-up Truck", make = "Toyota", model = "4Runner", years = 2010..2024,
            source = KnownSpecSource("Made-up manual", null, "2026-11-21", "Made up for a test.", "Test data."), items = file,
        )

        writer.addKnown(v, list)

        val after = rows(v)
        val plug = after.getValue("Made-up drain plug")
        assertEquals(listOf<Any?>(30.0, "REFERENCE", "made-up-specs"), listOf(plug.newtonMeters, plug.source, plug.knownList))
        val nuts = after.getValue("Made-up wheel nuts")
        assertEquals("a row the owner put anything in is never touched", listOf<Any?>(90.0, "USER", "made-up-list"), listOf(nuts.newtonMeters, nuts.source, nuts.knownList))
        assertEquals("REFERENCE" to "made-up-specs", after.getValue("Made-up bracket").let { it.source to it.knownList })

        writer.addKnown(v, list)
        assertEquals("a second time adds nothing", 3, torque.specsFor(v).size)
        assertNull(after.getValue("Made-up bracket").socketSize)
    }
}
