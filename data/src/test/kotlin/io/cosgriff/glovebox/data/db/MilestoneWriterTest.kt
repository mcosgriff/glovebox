package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.TableJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A made-up `milestone-schedule` file written into the owner's table (#645), through Android's own driver, as [MigrationTest] explains. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MilestoneWriterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val table = db.milestones()
    private val writer = MilestoneWriter(db)

    @After fun close() = db.close()

    private fun jobs(vararg names: String) = names.map { TableJob(it, null) }

    private val file = listOf(
        ImportedMilestone(15_000, jobs("Rotate", "Oil"), orMonths = 12),
        ImportedMilestone(30_000, jobs("Oil", "Cabin filter", "Rotate")),
    )

    private suspend fun vehicle(): Long {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        table.save(MilestoneEntity(vehicleId = v, atMiles = 15_000), listOf(MilestoneJobEntity(milestoneId = 0, name = "Oil")))
        table.setRepeat(v, 30_000)
        return v
    }

    private suspend fun preview(v: Long) = MilestoneImport.plan(file, table.tableFor(v).map { it.toSaved() })

    @Test
    fun `an import adds milestones and replaces a matched one's months and jobs, in the file's order, on its own row`() = runBlocking {
        val v = vehicle()
        val before = table.tableFor(v).single().milestone.id

        assertTrue(writer.import(v, 30_000, preview(v), ImportMode.REPLACE_MATCHING))

        val after = table.tableFor(v)
        assertEquals(listOf(15_000, 30_000), after.map { it.milestone.atMiles })
        assertEquals(before, after[0].milestone.id)
        assertEquals(12, after[0].milestone.orMonths)
        assertEquals(listOf("Rotate", "Oil"), after[0].ordered.map { it.name })
        assertEquals(listOf("Oil", "Cabin filter", "Rotate"), after[1].ordered.map { it.name })
        assertEquals(30_000, db.vehicles().byId(v)!!.milestonesRepeatEveryMiles)
    }

    @Test
    fun `a milestone moved since the preview is not written over, and one added since is not added twice`() = runBlocking {
        val v = vehicle()
        val preview = preview(v)
        // Between the preview and Import, the owner moves the matched milestone and types in the new one.
        val moved = table.tableFor(v).single().milestone
        table.save(moved.copy(atMiles = 20_000), listOf(MilestoneJobEntity(milestoneId = 0, name = "Oil")))
        table.save(MilestoneEntity(vehicleId = v, atMiles = 30_000), listOf(MilestoneJobEntity(milestoneId = 0, name = "Own job")))

        assertTrue(writer.import(v, 30_000, preview, ImportMode.REPLACE_MATCHING))

        val after = table.tableFor(v)
        assertEquals(listOf(20_000, 30_000), after.map { it.milestone.atMiles })
        assertEquals(listOf("Oil"), after[0].ordered.map { it.name })
        assertEquals(listOf("Own job"), after[1].ordered.map { it.name })
    }

    @Test
    fun `nothing is written when the table has come to repeat another way since the preview`() = runBlocking {
        val v = vehicle()
        val preview = preview(v)
        table.setRepeat(v, 45_000)

        assertFalse(writer.import(v, 30_000, preview, ImportMode.REPLACE_MATCHING))

        assertEquals(listOf(15_000), table.tableFor(v).map { it.milestone.atMiles })
        assertEquals(45_000, db.vehicles().byId(v)!!.milestonesRepeatEveryMiles)
    }
}
