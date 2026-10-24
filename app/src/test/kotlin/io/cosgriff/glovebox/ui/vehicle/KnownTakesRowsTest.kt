package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.domain.MakerDocumentKind
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.whatItTakes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The bundled "What it takes" lists and makers' documents, read from the app's own assets, and what filling from one writes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class KnownTakesRowsTest {
    @get:Rule val g = VehicleTestGarage()

    @Test
    fun `every bundled list is read, and each has a maker's source`() = runBlocking {
        val lists = KnownTakesCatalog(g.context).lists()
        assertTrue(lists.isNotEmpty())
        assertTrue(lists.all { it.source.name.isNotBlank() })
        val docs = MakerDocumentCatalog(g.context).lists()
        assertTrue(docs.any { it.make == "Toyota" })
        assertTrue(docs.flatMap { it.documents }.any { it.kind == MakerDocumentKind.OWNERS_MANUAL })
    }

    @Test
    fun `a covered vehicle is offered its list, and a vehicle nothing covers is offered nothing`() = runBlocking {
        val truck = g.add(VehicleFixtures.truck)
        val offered = g.knownTakes.observe(g.db.vehicles().observe(truck)).first()
        assertNotNull(offered.list)
        assertTrue(offered.offer!!.filled.contains(TakesItem.OIL))
        assertTrue(offered.toCheck.isEmpty())
        assertNull(offered.toCheckFrom)

        val wagon = g.add(VehicleFixtures.wagon)
        assertEquals(TakesKnown(), g.knownTakes.observe(g.db.vehicles().observe(wagon)).first())
        // A vehicle that has gone has nothing to offer either.
        assertEquals(TakesKnown(), g.knownTakes.observe(g.db.vehicles().observe(999)).first())
    }

    @Test
    fun `filling writes only the empty rows and tags each to check`() = runBlocking {
        val id = g.add(VehicleFixtures.truck.copy(oilSpec = "5W-30"))
        g.knownTakes.fill(g.vehicle(id)!!)
        val v = g.vehicle(id)!!
        assertEquals("5W-30", v.oilSpec)
        assertTrue(TakesItem.OIL !in v.takesToCheck())
        assertTrue(v.takesToCheck().isNotEmpty())
        val after = g.knownTakes.observe(g.db.vehicles().observe(id)).first()
        assertEquals(after.list, after.toCheckFrom)
        assertEquals(v.takesToCheck(), after.toCheck)

        // Filling again finds nothing left to fill and changes nothing.
        g.knownTakes.fill(v)
        assertEquals(v, g.vehicle(id))
        // Nor does filling a vehicle no list covers.
        val wagon = g.add(VehicleFixtures.wagon)
        g.knownTakes.fill(g.vehicle(wagon)!!)
        assertNull(g.vehicle(wagon)!!.takesToCheck)
    }

    @Test
    fun `a filled row keeps its tag until it is checked or changed`() = runBlocking {
        val id = g.add(VehicleFixtures.truck)
        g.knownTakes.fill(g.vehicle(id)!!)
        val filled = g.vehicle(id)!!
        val takes = filled.whatItTakes()

        // Saved as it was, unchecked: still to check.
        g.knownTakes.save(filled, TakesItem.TIRES, takes, checked = false)
        assertTrue(TakesItem.TIRES in g.vehicle(id)!!.takesToCheck())
        // Saved as it was, checked: settled.
        g.knownTakes.save(g.vehicle(id)!!, TakesItem.TIRES, takes, checked = true)
        assertTrue(TakesItem.TIRES !in g.vehicle(id)!!.takesToCheck())
        // Changed: the owner's own now, so settled too.
        g.knownTakes.save(g.vehicle(id)!!, TakesItem.OIL, takes.copy(oilGrade = "0W-16"), checked = false)
        val v = g.vehicle(id)!!
        assertTrue(TakesItem.OIL !in v.takesToCheck())
        assertEquals("0W-16", v.oilSpec)
    }
}
