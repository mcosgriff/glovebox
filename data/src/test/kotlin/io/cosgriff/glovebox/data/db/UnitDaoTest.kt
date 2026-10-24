package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.DistanceConversion
import io.cosgriff.glovebox.domain.DistanceKind
import io.cosgriff.glovebox.domain.DistanceUnit
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
class UnitDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun measured(miles: Int) = DistanceConversion.convert(miles, DistanceUnit.MILES, DistanceUnit.KILOMETERS, DistanceKind.MEASURED)
    private fun scheduled(miles: Int) = DistanceConversion.convert(miles, DistanceUnit.MILES, DistanceUnit.KILOMETERS, DistanceKind.SCHEDULED)

    /** A miles vehicle with something stored in every distance column. */
    private suspend fun filled(): Long {
        val v = db.vehicles().add(testVehicle(), firstReading = 61_000)
        db.vehicles().writeClusterSwap(v, offset = 40_000, on = day.minusDays(100), startReading = 20_000)
        db.service().insertWithItems(ServiceRecordEntity(vehicleId = v, date = day, odometer = 60_000, diy = true), emptyList())
        db.schedule().insertAll(
            listOf(
                ScheduleItemEntity(vehicleId = v, name = "Oil", intervalMiles = 5_000, intervalMonths = 6, severeMiles = 3_000, e85Miles = 4_000, snoozedUntilOdometer = 61_500, firstMiles = 10_000),
                ScheduleItemEntity(vehicleId = v, name = "Wipers", intervalMiles = null, intervalMonths = 12),
            ),
        )
        db.milestones().applyImport(v, 30_000, listOf(MilestoneEntity(vehicleId = v, atMiles = 15_000) to emptyList(), MilestoneEntity(vehicleId = v, atMiles = 30_000) to emptyList()), emptyList())
        return v
    }

    @Test
    fun `changing a vehicle to kilometres converts measurements exactly and schedule figures as manuals print them`() = runBlocking {
        val v = filled()

        db.units().convert(v, DistanceUnit.KILOMETERS)

        val vehicle = db.vehicles().byId(v)!!
        assertEquals("KILOMETERS", vehicle.distanceUnit)
        assertEquals(measured(40_000), vehicle.odometerOffset)
        assertEquals(measured(20_000), vehicle.clusterStartReading)
        assertEquals(scheduled(30_000), vehicle.milestonesRepeatEveryMiles)
        assertEquals(listOf(measured(61_000)), db.odometer().readingsFor(v).map { it.odometer })
        assertEquals(listOf(measured(60_000)), db.service().recordsFor(v).map { it.record.odometer })
        val oil = db.schedule().allFor(v).single { it.name == "Oil" }
        assertEquals(
            listOf(scheduled(5_000), scheduled(3_000), scheduled(4_000), measured(61_500), scheduled(10_000)),
            listOf(oil.intervalMiles, oil.severeMiles, oil.e85Miles, oil.snoozedUntilOdometer, oil.firstMiles),
        )
        assertEquals(null, db.schedule().allFor(v).single { it.name == "Wipers" }.intervalMiles)
        assertEquals(listOf(scheduled(15_000), scheduled(30_000)), db.milestones().tableFor(v).map { it.milestone.atMiles })
    }

    @Test
    fun `changing back takes milestones down without two landing on one figure`() = runBlocking {
        val v = filled()
        db.units().convert(v, DistanceUnit.KILOMETERS)
        db.units().convert(v, DistanceUnit.MILES)

        val vehicle = db.vehicles().byId(v)!!
        assertEquals("MILES", vehicle.distanceUnit)
        assertEquals(listOf(15_000, 30_000), db.milestones().tableFor(v).map { it.milestone.atMiles })
        assertEquals(listOf(5_000), db.schedule().allFor(v).filter { it.name == "Oil" }.map { it.intervalMiles })
    }

    @Test
    fun `the unit it already counts in, or a vehicle that is gone, changes nothing`() = runBlocking {
        val v = filled()
        val before = db.odometer().readingsFor(v)
        db.units().convert(v, DistanceUnit.MILES)
        db.units().convert(999, DistanceUnit.KILOMETERS)
        assertEquals(before, db.odometer().readingsFor(v))
        assertEquals("MILES", db.vehicles().byId(v)!!.distanceUnit)
    }
}
