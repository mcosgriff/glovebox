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
class RecallDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val recalls = db.recalls()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun recall(vehicleId: Long, campaign: String) = RecallEntity(vehicleId = vehicleId, campaign = campaign, component = "Made-up", summary = "Made-up", firstSeenOn = day)

    @Test
    fun `recalls are listed per vehicle and for the garage`() = runBlocking {
        val a = db.vehicles().add(testVehicle(), firstReading = null)
        val b = db.vehicles().add(testVehicle(model = "GTI"), firstReading = null)
        recalls.insertAll(listOf(recall(a, "99V000001"), recall(a, "99V000002"), recall(b, "99V000003")))

        assertEquals(setOf("99V000001", "99V000002"), recalls.observe(a).first().map { it.campaign }.toSet())
        assertEquals(3, recalls.observeAll().first().size)
    }

    @Test
    fun `marks are set and taken back`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        recalls.insertAll(listOf(recall(v, "99V000001"), recall(v, "99V000002")))
        val (one, two) = recalls.recallsFor(v).sortedBy { it.campaign }
        val record = db.service().insertWithItems(ServiceRecordEntity(vehicleId = v, date = day, odometer = 60_000, diy = false), emptyList())

        recalls.setArchived(one.id, day)
        recalls.linkRecord(listOf(one.id, two.id), record)
        assertEquals(day, recalls.byId(one.id)!!.archivedOn)
        assertEquals(setOf(one.id, two.id), recalls.idsLinkedTo(record).toSet())

        recalls.setArchived(one.id, null)
        recalls.setDone(two.id, null, null)
        assertNull(recalls.byId(one.id)!!.archivedOn)
        assertNull(recalls.byId(two.id)!!.serviceRecordId)
    }

    @Test
    fun `a refresh adds, updates and withdraws as one`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        recalls.insertAll(listOf(recall(v, "99V000001"), recall(v, "99V000002")))
        val (one, two) = recalls.recallsFor(v).sortedBy { it.campaign }

        recalls.apply(added = listOf(recall(v, "99V000003")), updated = listOf(one.copy(summary = "Reworded")), withdrawn = listOf(two.id))

        val after = recalls.recallsFor(v).associateBy { it.campaign }
        assertEquals(setOf("99V000001", "99V000003"), after.keys)
        assertEquals("Reworded", after.getValue("99V000001").summary)
    }
}
