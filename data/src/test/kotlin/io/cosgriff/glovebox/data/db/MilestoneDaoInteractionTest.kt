package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.TableJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MilestoneDaoInteractionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val milestones = db.milestones()

    @After fun close() = db.close()

    private fun table(vehicleId: Long) = listOf(
        ImportedMilestone(atMiles = 30_000, orMonths = 36, work = listOf(TableJob("Made-up job B", "cabin_filter"), TableJob("Made-up job A", null))).toRows(vehicleId),
        ImportedMilestone(atMiles = 15_000, orMonths = null, work = listOf(TableJob("Made-up job C", null))).toRows(vehicleId),
    )

    @Test
    fun `a maker's table goes onto a vehicle with none, naming its list, and is listed by mileage`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)

        milestones.addKnown(v, "made-up-list", table(v))

        val shown = milestones.observe(v).first()
        assertEquals(listOf(15_000, 30_000), shown.map { it.milestone.atMiles })
        assertEquals(listOf("Made-up job B", "Made-up job A"), shown.last().ordered.map { it.name })
        assertEquals("cabin_filter", shown.last().ordered.first().service)
        assertEquals("made-up-list", db.vehicles().byId(v)!!.milestonesKnownList)
        assertNull(db.vehicles().byId(v)!!.milestonesRepeatEveryMiles)
    }

    @Test
    fun `a maker's table never writes over one that is there`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        milestones.save(MilestoneEntity(vehicleId = v, atMiles = 5_000), listOf(MilestoneJobEntity(milestoneId = 0, name = "Typed job")))

        milestones.addKnown(v, "made-up-list", table(v))

        assertEquals(listOf(5_000), milestones.tableFor(v).map { it.milestone.atMiles })
        assertNull(db.vehicles().byId(v)!!.milestonesKnownList)
    }

    @Test
    fun `deleting the table forgets where it came from and that it repeated`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        milestones.addKnown(v, "made-up-list", table(v))
        milestones.setRepeat(v, 30_000)

        milestones.deleteTable(v)

        assertEquals(0, milestones.count(v))
        val vehicle = db.vehicles().byId(v)!!
        assertNull(vehicle.milestonesKnownList)
        assertNull(vehicle.milestonesRepeatEveryMiles)
    }
}
