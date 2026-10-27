package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.ui.components.DecodedField
import io.cosgriff.glovebox.domain.DecodeQuality
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class AddVehicleViewModelTest {
    @get:Rule val main = UnconfinedMainRule()

    private val db = testDatabase()
    private val nhtsa = FakeNhtsa().apply { decode = truckDecode }
    private val vin = Fixtures.truck.vin!!

    @After fun close() = db.close()

    private fun viewModel(unit: DistanceUnit = DistanceUnit.MILES): AddVehicleViewModel {
        val settings = testSettings(distance = unit)
        return AddVehicleViewModel(nhtsa.client, db.vehicles(), nhtsa.repository(db, settings), settings)
    }

    private fun AddVehicleViewModel.decoded(): AddVehicleViewModel {
        decode()
        waitFor("the decode") { decoded != null || failed }
        return this
    }

    @Test
    fun `a typed VIN is uppercased, stripped and stopped at 17, and only a right check digit passes`() {
        val vm = viewModel()
        vm.onVinChange(" ${vin.lowercase()}X")
        assertEquals(vin, vm.vin)
        assertTrue(vm.vinValid)
        // One character out: the check digit no longer matches.
        vm.onVinChange(vin.dropLast(1) + "2")
        assertFalse(vm.vinValid)
    }

    @Test
    fun `the decode fills what vPIC said plainly, and only the VIN is sent for it`() {
        nhtsa.recalls = listOf(recallRow("26V500000"), recallRow("26V500000"), recallRow("26V600000", "AIR BAGS"))
        val vm = viewModel()
        vm.onVinChange(vin)
        vm.decoded()

        assertEquals(DecodeQuality.CLEAN, vm.quality)
        assertEquals(Triple(2019, "TOYOTA", "4Runner"), Triple(vm.year, vm.make, vm.model))
        assertEquals(
            mapOf(
                DecodedField.BODY to "Sport Utility Vehicle (SUV)",
                DecodedField.DRIVE to "4WD/4-Wheel Drive/4x4",
                DecodedField.ENGINE to "4.0L 6-cyl",
                DecodedField.ENGINE_CODE to "1GR-XX",
                DecodedField.FUEL to "Gasoline",
                DecodedField.TRANSMISSION to "5-speed Automatic",
                DecodedField.TPMS to "Direct",
                DecodedField.SERIES to "XYZ100L",
            ),
            vm.fields,
        )
        // Petrol is never assumed from vPIC's plain "Gasoline".
        assertNull(vm.powertrain)
        // An SUV is neither body a maker's schedule is told apart by.
        assertNull(vm.bodyStyle)
        // A single trim is filled in, but it is vPIC's, not the owner's.
        assertEquals("SR5", vm.trim)
        assertFalse(vm.trimConfirmed)

        waitFor("the recall lookup") { vm.recalls is RecallLookup.Found }
        assertEquals(3, (vm.recalls as RecallLookup.Found).notices.size)
        val (decodeUrl, recallUrl) = nhtsa.asked.partition { "DecodeVinValues" in it.encodedPath }
        assertTrue(decodeUrl.single().encodedPath.endsWith("/$vin"))
        assertTrue(recallUrl.none { vin in it.toString() })
        assertEquals("2019", recallUrl.single().parameters["modelYear"])
    }

    @Test
    fun `NHTSA out of reach says so and keeps the VIN`() {
        nhtsa.decode = null
        val vm = viewModel()
        vm.onVinChange(vin)
        vm.decoded()
        assertTrue(vm.failed)
        assertNull(vm.decoded)
        assertEquals(vin, vm.vin)
        // Typing again clears the message.
        vm.onVinChange(vin.dropLast(1))
        assertFalse(vm.failed)
    }

    @Test
    fun `an unidentified VIN can't be saved until year, make and model are given`() {
        nhtsa.decode = mapOf("ErrorCode" to "8", "ErrorText" to "8 - No detailed data available")
        val vm = viewModel()
        vm.onVinChange(vin)
        vm.decoded()
        assertEquals(DecodeQuality.UNIDENTIFIED, vm.quality)
        assertFalse(vm.canSave)
        assertEquals(RecallLookup.NotAsked, vm.recalls)

        vm.correctVehicle(2019, "Toyota", "4Runner")
        assertTrue(vm.canSave)
        // With a vehicle to ask about, the recalls are looked up.
        waitFor("the recall lookup") { vm.recalls is RecallLookup.Found }
    }

    @Test
    fun `a scanned VIN is decoded straight away, and Back to the VIN forgets the lookup`() {
        val vm = viewModel()
        vm.useScanned(vin)
        waitFor("the decode") { vm.decoded != null }
        vm.backToVin()
        assertNull(vm.decoded)
        assertEquals(RecallLookup.NotAsked, vm.recalls)
        assertEquals(vin, vm.vin)
    }

    @Test
    fun `corrections replace what was decoded, and an emptied one clears it`() {
        val vm = viewModel()
        vm.onVinChange(vin)
        vm.decoded()
        vm.correct(DecodedField.ENGINE, "  4.0L V6  ")
        vm.correct(DecodedField.FUEL, "   ")
        assertEquals("4.0L V6", vm.fields[DecodedField.ENGINE])
        assertFalse(DecodedField.FUEL in vm.fields)
    }

    @Test
    fun `saving keeps the corrections, the unit, the first reading and the recalls already found`() = runBlocking {
        nhtsa.decode = truckDecode + mapOf(
            "Trim" to "SR5/SR5 Premium", "ElectrificationLevel" to "Strong HEV (Hybrid Electric Vehicle)", "BodyClass" to "Hatchback/Liftback/Notchback",
        )
        nhtsa.recalls = listOf(recallRow("26V700000"))
        val vm = viewModel(unit = DistanceUnit.KILOMETERS)
        waitFor("the settings unit") { vm.unit == DistanceUnit.KILOMETERS }
        vm.onVinChange(vin)
        vm.decoded()
        // Two trims: neither is chosen for the owner.
        assertEquals(listOf("SR5", "SR5 Premium"), vm.trimChoices)
        assertEquals("", vm.trim)
        assertEquals(Powertrain.HYBRID, vm.powertrain)
        assertEquals(BodyStyle.HATCHBACK, vm.bodyStyle)
        vm.onTrimChange("SR5 Premium")
        vm.correct(DecodedField.ENGINE_CODE, "1GR-YY")
        vm.correct(DecodedField.TPMS, "")
        vm.onOdometerChange("61,250")
        waitFor("the recall lookup") { vm.recalls is RecallLookup.Found }

        var saved: Long? = null
        vm.save { saved = it }
        waitFor("the save") { saved != null }

        val v = db.vehicles().byId(saved!!)!!
        assertEquals(vin, v.vin)
        assertEquals("SR5 Premium", v.trim)
        assertTrue(v.trimConfirmed)
        assertEquals(Powertrain.HYBRID.name, v.powertrain)
        assertEquals(BodyStyle.HATCHBACK.name, v.bodyStyle)
        assertEquals("4.0L 6-cyl", v.engine)
        // Corrected on the confirm step, and cleared there.
        assertEquals("1GR-YY", v.engineCode)
        assertNull(v.tpms)
        assertEquals("XYZ100L", v.series)
        assertEquals(DistanceUnit.KILOMETERS.name, v.distanceUnit)
        assertEquals(listOf(61_250), db.odometer().readingsFor(saved!!).map { it.odometer })
        assertEquals(listOf("26V700000"), db.recalls().recallsFor(saved!!).map { it.campaign })
        assertEquals(LocalDate.now(), v.recallsCheckedOn)
    }

    @Test
    fun `a failed recall lookup never blocks adding the vehicle`() = runBlocking {
        nhtsa.recalls = null
        val vm = viewModel()
        vm.onVinChange(vin)
        vm.decoded()
        waitFor("the recall lookup") { vm.recalls == RecallLookup.Failed }
        var saved: Long? = null
        vm.save { saved = it }
        waitFor("the save") { saved != null }
        assertEquals(emptyList<Any>(), db.recalls().recallsFor(saved!!))
        // Never checked, so the garage asks again later.
        assertNull(db.vehicles().byId(saved!!)!!.recallsCheckedOn)
    }

    @Test
    fun `nothing is saved before a decode`() {
        val vm = viewModel()
        vm.onVinChange(vin)
        var saved = false
        vm.save { saved = true }
        assertFalse(vm.saving)
        assertFalse(saved)
    }
}
