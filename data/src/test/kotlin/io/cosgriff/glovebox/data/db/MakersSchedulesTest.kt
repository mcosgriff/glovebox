package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.KnownEngine
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import io.cosgriff.glovebox.domain.KnownSpecSource
import io.cosgriff.glovebox.domain.KnownTrim
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.domain.TableJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Which maker's schedule a vehicle is on (#576), and taking one up (#429), through Android's own driver as
 * [MigrationTest] explains. The lists are made up here, not read from the app's assets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MakersSchedulesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val schedule = db.schedule()
    private val milestones = db.milestones()

    @After fun close() = db.close()

    /** A made-up list: one reminder and a two-line table, one line of it for four-wheel drive only. */
    private val list = KnownScheduleList(
        id = "madeup-truck-2020-2024",
        title = "2020–2024 Made-up Truck",
        make = "Madeup",
        model = "Truck",
        years = 2020..2024,
        source = KnownSpecSource("Made-up guide", null, "2026-10-24", "Made up for a test.", "Test data."),
        unit = DistanceUnit.MILES,
        milestones = listOf(
            ImportedMilestone(5_000, listOf(TableJob("Replace engine oil", "oil-change"))),
            ImportedMilestone(30_000, listOf(TableJob("Replace engine oil", "oil-change"), TableJob("Replace transfer case oil"))),
        ),
        intervals = listOf(ImportedScheduleItem("Replace engine oil", Interval(5_000, 6), service = "oil-change")),
        fourWheelDriveOnly = setOf("replace transfer case oil"),
        engine = KnownEngine("2.4L I4", 2.4),
    )

    /** The same made-up truck's other engine, with a list of its own. */
    private val v6 = list.copy(id = "madeup-truck-v6-2020-2024", title = "2020–2024 Made-up Truck V6", engine = KnownEngine("4.0L V6", 4.0))

    private val writer = MakersSchedules({ listOf(list, v6) }, db)

    private fun truck(engine: String?) = testVehicle(make = "Madeup", model = "Truck", year = 2022).copy(engine = engine)

    private suspend fun vehicle(id: Long) = db.vehicles().byId(id)!!

    private fun items(vehicleId: Long) = list.intervals.map { it.toEntity(vehicleId, knownList = list.id) }

    @Test
    fun `a maker's schedule is taken up with its table`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        writer.adopt(car, items(car), list)

        assertEquals(listOf("Replace engine oil"), schedule.allFor(car).map { it.name })
        assertEquals(listOf(5_000, 30_000), milestones.tableFor(car).map { it.milestone.atMiles })
        assertEquals(list.id, db.vehicles().byId(car)!!.milestonesKnownList)
    }

    @Test
    fun `DAO calls inside one write are kept together or not at all, their own transactions included`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        val failed = runCatching {
            writer.transaction {
                schedule.applyKnown(items(car), emptyList())
                milestones.addKnown(car, list.id, list.milestones.map { it.toRows(car) })
                error("the process died here")
            }
        }
        assertTrue(failed.isFailure)
        assertEquals(emptyList<ScheduleItemEntity>(), schedule.allFor(car))
        assertEquals(emptyList<MilestoneWithJobs>(), milestones.tableFor(car))
        assertNull(db.vehicles().byId(car)!!.milestonesKnownList)
    }

    @Test
    fun `the table leaves out what isn't for the vehicle's drive`() = runBlocking {
        val car = db.vehicles().insert(testVehicle().copy(drivetrain = "4x2"))
        writer.adopt(car, items(car), list)

        assertEquals(listOf("Replace engine oil"), milestones.tableFor(car).last().ordered.map { it.name })
    }

    @Test
    fun `a table the vehicle already has is never written over`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        milestones.save(MilestoneEntity(vehicleId = car, atMiles = 7_500), listOf(MilestoneJobEntity(milestoneId = 0, name = "Own job")))
        writer.adopt(car, items(car), list)

        assertEquals(listOf(7_500), milestones.tableFor(car).map { it.milestone.atMiles })
        assertEquals(listOf("Replace engine oil"), schedule.allFor(car).map { it.name })
    }

    @Test
    fun `the typical list has no table`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        writer.adopt(car, items(car))

        assertEquals(emptyList<MilestoneWithJobs>(), milestones.tableFor(car))
        assertEquals(1, schedule.allFor(car).size)
    }

    @Test
    fun `a typical item it doubles is removed, or kept untracked once logged against`() = runBlocking {
        val car = db.vehicles().insert(testVehicle())
        fun typical(name: String) = ScheduleItemEntity(vehicleId = car, name = name, intervalMiles = 5_000, intervalMonths = null, templateKey = name, tracked = true)
        schedule.insertAll(listOf(typical("oil"), typical("oil-logged")))
        val (unlogged, logged) = schedule.allFor(car).sortedBy { it.name }
        db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = car, date = LocalDate.of(2026, 5, 4), odometer = 41_000, diy = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = logged.id, label = "Oil")),
        )

        writer.adopt(car, items(car), list, replaced = listOf(unlogged.id, logged.id))

        val left = schedule.allFor(car).associateBy { it.name }
        assertEquals(setOf("Replace engine oil", "oil-logged"), left.keys)
        assertFalse(left.getValue("oil-logged").tracked)
    }

    @Test
    fun `the engine chosen is written only where the vehicle has none`() = runBlocking {
        val blank = db.vehicles().insert(testVehicle())
        val known = db.vehicles().insert(testVehicle().copy(engine = "4.0L V6", powertrain = Powertrain.GAS.name, bodyStyle = BodyStyle.SEDAN.name))
        val engine = KnownEngine("2.4L I4 hybrid", 2.4, powertrain = Powertrain.HYBRID)

        writer.adopt(blank, items(blank), list, engine = engine, body = BodyStyle.HATCHBACK)
        writer.adopt(known, items(known), list, engine = engine, body = BodyStyle.HATCHBACK)

        val filled = db.vehicles().byId(blank)!!
        assertEquals("2.4L I4 hybrid", filled.engine)
        assertEquals(Powertrain.HYBRID.name, filled.powertrain)
        assertEquals(BodyStyle.HATCHBACK.name, filled.bodyStyle)
        val kept = db.vehicles().byId(known)!!
        assertEquals("4.0L V6", kept.engine)
        assertEquals(Powertrain.GAS.name, kept.powertrain)
        assertEquals(BodyStyle.SEDAN.name, kept.bodyStyle)
    }

    @Test
    fun `a vehicle with nothing taken up is on the list that covers it as it is on record`() = runBlocking {
        val car = db.vehicles().insert(truck("2.4L I4"))
        val blank = db.vehicles().insert(truck(null))

        assertEquals(list.id, writer.forVehicle(vehicle(car))?.id)
        assertNull(writer.forVehicle(vehicle(blank)))
        assertEquals(listOf(list.id, v6.id), writer.choices(vehicle(blank)).map { it.id })
    }

    @Test
    fun `a vehicle's trim on record picks between lists of one engine that name it (#569)`() = runBlocking {
        val sport = list.copy(id = "madeup-truck-sport-2020-2024", trim = KnownTrim("Sport", named = true))
        val plain = list.copy(trim = KnownTrim("Sport", named = false))
        val byTrim = MakersSchedules({ listOf(plain, sport) }, db)
        val sporty = db.vehicles().insert(truck("2.4L I4").copy(trim = "Sport, Sport Plus"))
        val base = db.vehicles().insert(truck("2.4L I4").copy(trim = "Base"))
        val unknown = db.vehicles().insert(truck("2.4L I4"))

        assertEquals(sport.id, byTrim.forVehicle(vehicle(sporty))?.id)
        assertEquals(plain.id, byTrim.forVehicle(vehicle(base))?.id)
        assertNull(byTrim.forVehicle(vehicle(unknown)))
        assertEquals(listOf(plain.id, sport.id), byTrim.choices(vehicle(unknown)).map { it.id })
    }

    @Test
    fun `a vehicle stays on the list it took up after its engine text changes`() = runBlocking {
        val car = db.vehicles().insert(truck("2.4L I4"))
        writer.adopt(car, items(car), list)
        db.vehicles().update(vehicle(car).copy(engine = "4.0L V6"))

        assertEquals(list.id, writer.forVehicle(vehicle(car))?.id)
        db.vehicles().update(vehicle(car).copy(engine = "something none of them is for"))
        assertEquals(list.id, writer.forVehicle(vehicle(car))?.id)
    }

    @Test
    fun `a list taken up that no longer ships gives way to the one that covers the vehicle`() = runBlocking {
        val car = db.vehicles().insert(truck("4.0L V6"))
        schedule.insertAll(listOf(ScheduleItemEntity(vehicleId = car, name = "Replace engine oil", intervalMiles = 5_000, intervalMonths = 6, knownList = "madeup-gone")))

        assertEquals(v6.id, writer.forVehicle(vehicle(car))?.id)
    }

    @Test
    fun `taking up a list's table on its own writes it, in the vehicle's unit and for its drive`() = runBlocking {
        val car = db.vehicles().insert(truck("2.4L I4").copy(drivetrain = "4x2", distanceUnit = DistanceUnit.KILOMETERS.name))
        writer.addTable(car, list)

        val table = milestones.tableFor(car)
        val inKm = KnownSchedules.milestonesIn(list, DistanceUnit.KILOMETERS, "4x2").map { it.atMiles }
        assertEquals(listOf(5_000, 30_000), list.milestones.map { it.atMiles })
        assertEquals(inKm, table.map { it.milestone.atMiles })
        assertEquals(listOf("Replace engine oil"), table.last().ordered.map { it.name })
        assertEquals(list.id, vehicle(car).milestonesKnownList)
        assertEquals(emptyList<ScheduleItemEntity>(), schedule.allFor(car))
    }
}
