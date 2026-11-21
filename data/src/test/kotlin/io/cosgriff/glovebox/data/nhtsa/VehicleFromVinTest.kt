package io.cosgriff.glovebox.data.nhtsa

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.Powertrain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** A vPIC decode into a vehicle's columns, at [VehicleFromVin]'s interface: NHTSA answered in the test, the database in memory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VehicleFromVinTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val vehicles = db.vehicles()
    private val nhtsa = FakeNhtsa()
    private val fromVin = VehicleFromVin(nhtsa.client, vehicles)

    @After fun close() = db.close()

    /** Made up, with a check digit that works out; it belongs to no one. */
    private val vin = "1M8GDM9AXKP042788"

    /** A clean decode of [vin] as a 2019 Toyota; every value invented. */
    private val decode = mapOf(
        "ModelYear" to "2019", "Make" to "TOYOTA", "Model" to "4Runner", "BodyClass" to "Sport Utility Vehicle (SUV)",
        "DriveType" to "4WD/4-Wheel Drive/4x4", "DisplacementL" to "3.956", "EngineCylinders" to "6", "EngineModel" to "1GR-XX",
        "FuelTypePrimary" to "Gasoline", "TransmissionSpeeds" to "5", "TransmissionStyle" to "Automatic", "TPMS" to "Direct",
        "Series" to "XYZ100L", "ErrorCode" to "0",
    )

    private fun answer(fields: Map<String, String>) {
        nhtsa.answer = { json(decodeJson(*fields.toList().toTypedArray())) }
    }

    private suspend fun saved(vehicle: VehicleEntity) = vehicles.byId(vehicles.add(vehicle, firstReading = null))!!

    @Test
    fun `a decode starts a new vehicle at what vPIC said plainly, and saves each field in its own column`() = runBlocking {
        answer(decode + mapOf("Trim" to "", "ElectrificationLevel" to "Strong HEV (Hybrid Electric Vehicle)", "BodyClass" to "Hatchback/Liftback/Notchback"))
        val start = VehicleFromVin.start(nhtsa.client.decodeVin(vin))

        assertEquals(
            mapOf(
                DecodedField.BODY to "Hatchback/Liftback/Notchback",
                DecodedField.DRIVE to "4WD/4-Wheel Drive/4x4",
                DecodedField.ENGINE to "4.0L 6-cyl",
                DecodedField.ENGINE_CODE to "1GR-XX",
                DecodedField.FUEL to "Gasoline",
                DecodedField.TRANSMISSION to "5-speed Automatic",
                DecodedField.TPMS to "Direct",
                DecodedField.SERIES to "XYZ100L",
            ),
            start.fields,
        )
        assertEquals(Powertrain.HYBRID, start.powertrain)
        assertEquals(BodyStyle.HATCHBACK, start.bodyStyle)

        // As adding a vehicle saves it, with the body style and powertrain the owner kept.
        val v = saved(testVehicle(vin = vin).withDecoded(start.fields).copy(bodyStyle = start.bodyStyle?.name, powertrain = start.powertrain?.name))
        assertEquals(
            listOf("Hatchback/Liftback/Notchback", "4WD/4-Wheel Drive/4x4", "4.0L 6-cyl", "1GR-XX", "Gasoline", "5-speed Automatic", "Direct", "XYZ100L"),
            listOf(v.bodyClass, v.drivetrain, v.engine, v.engineCode, v.fuel, v.transmission, v.tpms, v.series),
        )
        // The same decode, looked up again, has nothing left to add.
        assertEquals(VinLookupResult.Found(emptyMap()), fromVin.lookUp(v))
    }

    @Test
    fun `a field vPIC left blank is missing, and petrol or an SUV are never a body style or powertrain`() = runBlocking {
        answer(mapOf("ModelYear" to "2019", "Make" to "TOYOTA", "BodyClass" to "Sport Utility Vehicle (SUV)", "FuelTypePrimary" to "Gasoline", "EngineCylinders" to "6"))
        val start = VehicleFromVin.start(nhtsa.client.decodeVin(vin))

        assertEquals(
            mapOf(DecodedField.BODY to "Sport Utility Vehicle (SUV)", DecodedField.ENGINE to "6-cyl", DecodedField.FUEL to "Gasoline"),
            start.fields,
        )
        assertNull(start.powertrain)
        assertNull(start.bodyStyle)
    }

    @Test
    fun `looking a VIN up again sends the VIN alone and offers only what is empty here, never the drivetrain`() = runBlocking {
        answer(decode)
        // A blank left by an older version counts as empty; the engine is the owner's and reads differently from vPIC's.
        val v = saved(testVehicle(vin = vin).copy(engine = "4.0L V6", fuel = " "))

        val found = fromVin.lookUp(v)

        val url = nhtsa.requests.single().url
        assertTrue(url.encodedPath.endsWith("/DecodeVinValues/$vin"))
        // No year goes with it: vPIC answers with whatever year it is sent, which would hide a VIN that is someone else's.
        assertEquals(listOf("format"), url.parameters.names().toList())
        assertEquals(
            VinLookupResult.Found(
                mapOf(
                    DecodedField.BODY to "Sport Utility Vehicle (SUV)",
                    DecodedField.ENGINE_CODE to "1GR-XX",
                    DecodedField.FUEL to "Gasoline",
                    DecodedField.TRANSMISSION to "5-speed Automatic",
                    DecodedField.TPMS to "Direct",
                    DecodedField.SERIES to "XYZ100L",
                ),
            ),
            found,
        )
    }

    @Test
    fun `a VIN that decodes as another year or make offers nothing, and says what it is`() = runBlocking {
        val v = saved(testVehicle(vin = vin))

        answer(decode + mapOf("Make" to "FORD", "Model" to "F-150"))
        assertEquals(VinLookupResult.NotThisVehicle(2019, "FORD", "F-150"), fromVin.lookUp(v))
        answer(decode + ("ModelYear" to "2018"))
        assertEquals(VinLookupResult.NotThisVehicle(2018, "TOYOTA", "4Runner"), fromVin.lookUp(v))
        answer(mapOf("ErrorCode" to "8"))
        assertEquals(VinLookupResult.NotThisVehicle(null, null, null), fromVin.lookUp(v))
    }

    @Test
    fun `NHTSA out of reach is a failure, and a vehicle without a VIN asks nothing`() = runBlocking {
        nhtsa.answer = { throw IOException("offline") }
        assertEquals(VinLookupResult.Failed, fromVin.lookUp(saved(testVehicle(vin = vin))))

        nhtsa.requests.clear()
        assertEquals(VinLookupResult.Failed, fromVin.lookUp(saved(testVehicle(vin = null))))
        assertTrue(nhtsa.requests.isEmpty())
    }

    @Test
    fun `filling writes only what is still empty, so an edit saved while the lookup was out stands`() = runBlocking {
        answer(decode + mapOf("BodyClass" to "Hatchback/Liftback/Notchback", "ElectrificationLevel" to "Strong HEV (Hybrid Electric Vehicle)"))
        // A blank left by an older version counts as empty here too.
        val v = saved(testVehicle(vin = vin).copy(fuel = " "))
        val found = fromVin.lookUp(v) as VinLookupResult.Found
        assertEquals("4.0L 6-cyl", found.additions[DecodedField.ENGINE])

        // Setup writes the owner's pick of engine while the dialog is open.
        vehicles.setEngineIfBlank(v.id, "4.0L V6")
        fromVin.fill(v.id, found)
        // A second fill finds nothing empty to write.
        fromVin.fill(
            v.id,
            VinLookupResult.Found(
                mapOf(DecodedField.BODY to "Pickup", DecodedField.TPMS to "Indirect", DecodedField.SERIES to "OTHER"),
                bodyStyle = BodyStyle.SEDAN,
                powertrain = Powertrain.PLUG_IN,
            ),
        )

        val after = vehicles.byId(v.id)!!
        assertEquals("4.0L V6", after.engine)
        assertEquals("Hatchback/Liftback/Notchback", after.bodyClass)
        assertEquals("1GR-XX", after.engineCode)
        assertEquals("Gasoline", after.fuel)
        assertEquals("5-speed Automatic", after.transmission)
        assertEquals("Direct", after.tpms)
        assertEquals("XYZ100L", after.series)
        // The body style and powertrain vPIC says plainly, as adding a vehicle sets them (#644).
        assertEquals(BodyStyle.HATCHBACK.name, after.bodyStyle)
        assertEquals(Powertrain.HYBRID.name, after.powertrain)
        // The drivetrain is the owner's pick, never filled from a lookup, even when handed one.
        fromVin.fill(v.id, VinLookupResult.Found(mapOf(DecodedField.DRIVE to "4WD/4-Wheel Drive/4x4")))
        assertNull(vehicles.byId(v.id)!!.drivetrain)
    }

    @Test
    fun `a blank body style or powertrain is offered from the decode even when the body class is already here`() = runBlocking {
        answer(decode + mapOf("BodyClass" to "Convertible/Cabriolet", "FuelTypePrimary" to "Gasoline", "FuelTypeSecondary" to "Ethanol (E85)"))
        val v = saved(testVehicle(vin = vin).copy(bodyClass = "Convertible/Cabriolet", fuel = "Gasoline"))

        val found = fromVin.lookUp(v) as VinLookupResult.Found
        assertNull(found.additions[DecodedField.BODY])
        assertEquals(BodyStyle.CONVERTIBLE, found.bodyStyle)
        assertEquals(Powertrain.FLEX_FUEL, found.powertrain)

        fromVin.fill(v.id, found)
        val after = vehicles.byId(v.id)!!
        assertEquals(BodyStyle.CONVERTIBLE.name, after.bodyStyle)
        assertEquals(Powertrain.FLEX_FUEL.name, after.powertrain)
        // Then there is nothing left to offer.
        val again = fromVin.lookUp(after) as VinLookupResult.Found
        assertNull(again.bodyStyle)
        assertNull(again.powertrain)
    }

    @Test
    fun `the owner's body style and powertrain are never offered over, nor replaced by a fill`() = runBlocking {
        answer(decode + mapOf("BodyClass" to "Hatchback/Liftback/Notchback", "ElectrificationLevel" to "Strong HEV (Hybrid Electric Vehicle)"))
        // Picked when the vehicle was added: not what vPIC says.
        val picked = saved(testVehicle(vin = vin).copy(bodyStyle = BodyStyle.SEDAN.name, powertrain = Powertrain.GAS.name))
        val offered = fromVin.lookUp(picked) as VinLookupResult.Found
        assertNull(offered.bodyStyle)
        assertNull(offered.powertrain)

        // Blank when looked up; setup writes the owner's pick while the dialog is open, and the fill leaves it.
        val blank = saved(testVehicle(vin = vin))
        val found = fromVin.lookUp(blank) as VinLookupResult.Found
        assertEquals(BodyStyle.HATCHBACK, found.bodyStyle)
        assertEquals(Powertrain.HYBRID, found.powertrain)
        vehicles.setBodyStyleIfBlank(blank.id, BodyStyle.SEDAN.name)
        vehicles.setPowertrainIfBlank(blank.id, Powertrain.PLUG_IN.name)
        fromVin.fill(blank.id, found)

        val after = vehicles.byId(blank.id)!!
        assertEquals(BodyStyle.SEDAN.name, after.bodyStyle)
        assertEquals(Powertrain.PLUG_IN.name, after.powertrain)
    }
}
