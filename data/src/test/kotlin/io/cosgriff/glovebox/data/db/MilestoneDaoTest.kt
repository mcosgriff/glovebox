package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Through Android's own driver, as [MigrationTest] explains. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MilestoneDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    private val table = db.milestones()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun jobs(vararg names: String) = names.map { MilestoneJobEntity(milestoneId = 0, name = it) }

    @Test
    fun `a milestone is saved with its jobs in order, and an edit keeps its row`() = runBlocking {
        val id = db.vehicles().insert(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day))
        table.save(MilestoneEntity(vehicleId = id, atMiles = 15_000), jobs("Oil", "Rotate"))
        val saved = table.tableFor(id).single()
        assertEquals(listOf("Oil", "Rotate"), saved.ordered.map { it.name })

        table.save(saved.milestone.copy(atMiles = 20_000, orMonths = 12), jobs("Rotate", "Oil"))
        val after = table.tableFor(id).single()
        assertEquals(saved.milestone.id, after.milestone.id)
        assertEquals(20_000 to 12, after.milestone.atMiles to after.milestone.orMonths)
        assertEquals(listOf("Rotate", "Oil"), after.ordered.map { it.name })
    }

    @Test
    fun `deleting the last milestone takes the table, and that it repeats, with it`() = runBlocking {
        val id = db.vehicles().insert(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day))
        table.addKnown(id, "toyota-4runner", listOf(MilestoneEntity(vehicleId = id, atMiles = 5_000) to jobs("Oil")))
        table.setRepeat(id, 5_000)
        table.deleteMilestone(id, table.tableFor(id).single().milestone.id)
        val vehicle = db.vehicles().byId(id)!!
        assertNull(vehicle.milestonesRepeatEveryMiles)
        assertNull(vehicle.milestonesKnownList)
    }
}
