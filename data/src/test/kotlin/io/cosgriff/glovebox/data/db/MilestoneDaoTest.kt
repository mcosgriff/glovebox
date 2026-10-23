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
    fun `an import adds and replaces milestones, each with its jobs in order`() = runBlocking {
        val id = db.vehicles().insert(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = day))
        table.applyImport(id, 30_000, listOf(MilestoneEntity(vehicleId = id, atMiles = 15_000) to jobs("Oil", "Rotate")), emptyList())
        val saved = table.tableFor(id).single()
        assertEquals(listOf("Oil", "Rotate"), saved.ordered.map { it.name })

        table.applyImport(
            id, 30_000,
            added = listOf(MilestoneEntity(vehicleId = id, atMiles = 30_000) to jobs("Oil", "Cabin filter", "Rotate")),
            replaced = listOf(saved.milestone.copy(orMonths = 12) to jobs("Rotate", "Oil")),
        )
        val after = table.tableFor(id)
        assertEquals(listOf(15_000, 30_000), after.map { it.milestone.atMiles })
        // The replaced one keeps its row, with the new jobs in the file's order.
        assertEquals(saved.milestone.id, after[0].milestone.id)
        assertEquals(12, after[0].milestone.orMonths)
        assertEquals(listOf("Rotate", "Oil"), after[0].ordered.map { it.name })
        assertEquals(listOf("Oil", "Cabin filter", "Rotate"), after[1].ordered.map { it.name })
        assertEquals(30_000, db.vehicles().byId(id)!!.milestonesRepeatEveryMiles)
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
