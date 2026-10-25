package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
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
class VehicleDaoInteractionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val vehicles = db.vehicles()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private suspend fun add(model: String = "4Runner", createdOn: LocalDate = day) =
        vehicles.add(testVehicle(model = model, createdOn = createdOn), firstReading = null)

    private suspend fun row(id: Long) = vehicles.byId(id)!!

    @Test
    fun `the garage lists active vehicles in the order added, and sold ones apart, most recently sold first`() = runBlocking {
        val a = add("A", day.minusDays(30))
        val b = add("B", day.minusDays(20))
        val c = add("C", day.minusDays(10))
        val d = add("D", day)
        vehicles.setArchived(b, day.minusDays(5))
        vehicles.setArchived(c, day.minusDays(1))

        assertEquals(listOf(a, d), vehicles.observeActive().first().map { it.id })
        assertEquals(listOf(a, d), vehicles.active().map { it.id }.sorted())
        assertEquals(listOf(c, b), vehicles.observeArchived().first().map { it.id })
        assertEquals(listOf(a, d, c, b), vehicles.all().map { it.id })
        assertEquals("C", vehicles.observe(c).first()!!.model)
        assertNull(vehicles.observe(999).first())

        // Back to the garage.
        vehicles.setArchived(c, null)
        assertEquals(listOf(a, c, d), vehicles.observeActive().first().map { it.id })
    }

    @Test
    fun `a VIN looked up again fills only what is empty, and never what the owner has since typed`() = runBlocking {
        // A blank left by an older version counts as empty.
        val id = vehicles.add(testVehicle().copy(engine = "4.0L 6-cyl", tpms = "Indirect", fuel = " "), firstReading = null)

        vehicles.fillFromVin(
            id, bodyClass = "Sport Utility Vehicle (SUV)", engine = "3.5L 6-cyl", fuel = "Gasoline",
            transmission = "5-speed Automatic", engineCode = "1GR-FE", tpms = "Direct", series = "GRN280L/GRN285L",
        )
        // A second answer finds nothing empty to fill.
        vehicles.fillFromVin(id, bodyClass = "Pickup", engine = null, fuel = null, transmission = null, engineCode = "2GR-FKS", tpms = null, series = null)

        val v = row(id)
        assertEquals("4.0L 6-cyl", v.engine)
        assertEquals("Indirect", v.tpms)
        assertEquals("Sport Utility Vehicle (SUV)", v.bodyClass)
        // The drivetrain is the owner's pick, never filled from a lookup.
        assertNull(v.drivetrain)
        assertEquals("Gasoline", v.fuel)
        assertEquals("5-speed Automatic", v.transmission)
        assertEquals("1GR-FE", v.engineCode)
        assertEquals("GRN280L/GRN285L", v.series)
    }

    @Test
    fun `one-column writes change that column and leave a recall check that landed meanwhile alone`() = runBlocking {
        val id = add()
        val stale = row(id)
        vehicles.setRecallsChecked(id, day)

        vehicles.setVin(id, "1M8GDM9AXKP042788")
        vehicles.setNickname(id, "Made-up name")
        vehicles.setPlate(id, "MADE UP", "CO")
        vehicles.setMileageCheckIn(id, 0)
        vehicles.setPhotoFile(id, "vehicle.jpg")

        val now = row(id)
        assertEquals(day, now.recallsCheckedOn)
        assertEquals(
            stale.copy(vin = "1M8GDM9AXKP042788", nickname = "Made-up name", plate = "MADE UP", plateRegion = "CO", mileageCheckInDays = 0, photoFile = "vehicle.jpg", recallsCheckedOn = day),
            now,
        )
    }

    @Test
    fun `the engine and powertrain are filled in only where none is on record`() = runBlocking {
        val id = add()
        vehicles.setEngineIfBlank(id, "4.0L V6")
        vehicles.setEngineIfBlank(id, "2.7L I4")
        vehicles.setPowertrainIfBlank(id, "HYBRID")
        vehicles.setPowertrainIfBlank(id, "GAS")
        assertEquals("4.0L V6" to "HYBRID", row(id).engine to row(id).powertrain)

        val blank = vehicles.add(testVehicle().copy(engine = "  "), firstReading = null)
        vehicles.setEngineIfBlank(blank, "4.0L V6")
        assertEquals("4.0L V6", row(blank).engine)
    }

    private val takes = WhatItTakes(
        oilGrade = "0W-20", oilApproval = "API SP", oilCapacity = 6.2, oilCapacityUnit = VehicleSpecEntry.Volume.QUARTS, oilFilter = "MADE-UP-1",
        tireSize = "265/70R17", tireSizeRear = "275/70R17", tirePressureFront = 32, tirePressureRear = 35, tirePressureSpare = 60, tirePressureUnit = VehicleSpecEntry.Pressure.PSI,
        wiperDriver = 22, wiperPassenger = 20, wiperRear = 14, wiperNote = "Made-up blades",
        transmissionFluid = "Made-up ATF", differentialOil = "75W-85", transferCaseOil = "75W", coolant = "Made-up coolant", brakeFluid = "DOT 3",
        engineAirFilter = "MADE-UP-AIR", cabinAirFilter = "MADE-UP-CABIN", sparkPlug = "MADE-UP-PLUG", sparkPlugGap = "1.1 mm",
        batteryGroup = "24F", batteryCca = 585, bulbLow = "H11", bulbHigh = "9005", bulbFog = "H16",
    )

    @Test
    fun `a bundled list fills every row of What it takes and marks them to check, in one go`() = runBlocking {
        val id = add()
        vehicles.fillTakes(id, takes, TakesItem.entries, "made-up-list", "OIL,TIRES")

        val v = row(id)
        assertEquals(listOf("0W-20", "API SP", 6.2, "QUARTS", "MADE-UP-1"), listOf(v.oilSpec, v.oilApproval, v.oilCapacity, v.oilCapacityUnit, v.oilFilter))
        assertEquals(listOf("265/70R17", "275/70R17", 32, 35, 60, "PSI"), listOf(v.tireSize, v.tireSizeRear, v.tirePressureFront, v.tirePressureRear, v.tirePressureSpare, v.tirePressureUnit))
        assertEquals(listOf(22, 20, 14, "Made-up blades"), listOf(v.wiperDriverIn, v.wiperPassengerIn, v.wiperRearIn, v.wiperBlades))
        assertEquals(
            listOf("Made-up ATF", "75W-85", "75W", "Made-up coolant", "DOT 3", "MADE-UP-AIR", "MADE-UP-CABIN", "MADE-UP-PLUG", "1.1 mm", "24F", 585, "H11", "9005", "H16"),
            listOf(v.transmissionFluid, v.differentialOil, v.transferCaseOil, v.coolantType, v.brakeFluid, v.engineAirFilter, v.cabinAirFilter, v.sparkPlug, v.sparkPlugGap, v.batteryGroup, v.batteryCca, v.bulbLow, v.bulbHigh, v.bulbFog),
        )
        assertEquals("made-up-list" to "OIL,TIRES", v.takesKnownList to v.takesToCheck)

        vehicles.setTakesToCheck(id, null)
        assertNull(row(id).takesToCheck)
        assertEquals("made-up-list", row(id).takesKnownList)
    }

    @Test
    fun `a unit is never kept without its figure, nor a rear tire size without a front one`() = runBlocking {
        val id = add()
        val bare = WhatItTakes(oilGrade = "5W-30", oilCapacityUnit = VehicleSpecEntry.Volume.LITRES, tireSizeRear = "275/70R17", tirePressureUnit = VehicleSpecEntry.Pressure.KPA)

        vehicles.setTakes(id, bare, TakesItem.OIL)
        vehicles.setTakes(id, bare, TakesItem.TIRES)

        val v = row(id)
        assertEquals("5W-30", v.oilSpec)
        assertNull(v.oilCapacityUnit)
        assertNull(v.tireSizeRear)
        assertNull(v.tirePressureUnit)
    }

    @Test
    fun `setting the cluster swap drops snoozes that end at a reading, and keeps those that end on a date`() = runBlocking {
        val id = add()
        db.schedule().insertAll(
            listOf(
                ScheduleItemEntity(vehicleId = id, name = "Oil", intervalMiles = 5000, intervalMonths = 6, snoozedUntilOdometer = 61_000),
                ScheduleItemEntity(vehicleId = id, name = "Wipers", intervalMiles = null, intervalMonths = 12, snoozedUntilDate = day.plusDays(10)),
            ),
        )

        vehicles.setClusterSwap(id, offset = 40_000, on = day, startReading = 20_000)

        val v = row(id)
        assertEquals(Triple(40_000, day, 20_000), Triple(v.odometerOffset, v.clusterSwappedOn, v.clusterStartReading))
        val items = db.schedule().allFor(id).associateBy { it.name }
        assertNull(items.getValue("Oil").snoozedUntilOdometer)
        assertEquals(day.plusDays(10), items.getValue("Wipers").snoozedUntilDate)
    }

    @Test
    fun `a whole-row update writes every column`() = runBlocking {
        val id = add()
        val changed = row(id).copy(trim = "SR5", trimConfirmed = true, fuel = "Gasoline", drivetrain = "4WD", transmission = "5-speed Automatic", bodyClass = "SUV", enteredByHand = true)
        vehicles.update(changed)
        assertEquals(changed, row(id))
    }

    @Test
    fun `a vehicle with every column filled reads back whole through every query`() = runBlocking {
        val full = VehicleEntity(
            vin = "1M8GDM9AXKP042788", enteredByHand = true, year = 2019, make = "Toyota", model = "4Runner", trim = "SR5", trimConfirmed = true,
            engine = "4.0L V6", fuel = "Gasoline", powertrain = "GAS", drivetrain = "4WD", transmission = "5-speed Automatic", bodyClass = "SUV",
            nickname = "Made-up", plate = "MADE UP", plateRegion = "CO", oilSpec = "0W-20", oilFilter = "MADE-UP-1", oilApproval = "API SP",
            oilCapacity = 6.2, oilCapacityUnit = "QUARTS", tireSize = "265/70R17", tireSizeRear = "275/70R17", wiperBlades = "Made-up",
            wiperDriverIn = 22, wiperPassengerIn = 20, wiperRearIn = 14, tirePressureFront = 32, tirePressureRear = 35, tirePressureSpare = 60,
            tirePressureUnit = "PSI", transmissionFluid = "Made-up ATF", differentialOil = "75W-85", transferCaseOil = "75W", coolantType = "Made-up",
            brakeFluid = "DOT 3", engineAirFilter = "MADE-UP-AIR", cabinAirFilter = "MADE-UP-CABIN", sparkPlug = "MADE-UP-PLUG", sparkPlugGap = "1.1 mm",
            batteryGroup = "24F", batteryCca = 585, bulbLow = "H11", bulbHigh = "9005", bulbFog = "H16", photoFile = "vehicle.jpg", odometerOffset = 40_000,
            clusterSwappedOn = day.minusDays(100), clusterStartReading = 20_000, archivedOn = null, createdOn = day.minusDays(400), recallsCheckedOn = day,
            milestonesRepeatEveryMiles = 60_000, milestonesKnownList = "made-up", distanceUnit = "KILOMETERS", takesKnownList = "made-up", takesToCheck = "OIL",
            mileageCheckInDays = 30,
        )
        val id = vehicles.insert(full)
        val expected = full.copy(id = id)

        assertEquals(expected, vehicles.byId(id))
        assertEquals(expected, vehicles.observe(id).first())
        assertEquals(listOf(expected), vehicles.observeActive().first())
        assertEquals(listOf(expected), vehicles.active())
        assertEquals(listOf(expected), vehicles.all())
        assertEquals(expected, db.units().vehicle(id))

        vehicles.setArchived(id, day)
        assertEquals(listOf(expected.copy(archivedOn = day)), vehicles.observeArchived().first())
    }
}
