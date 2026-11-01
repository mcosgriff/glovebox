package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.domain.DistanceUnit
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

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ManualAddViewModelTest {
    @get:Rule val main = UnconfinedMainRule()

    private val db = testDatabase()
    private val nhtsa = FakeNhtsa().apply {
        makes = listOf("TOYOTA", " ", "toyota", "Volkswagen", "Ford")
        models = listOf("Tacoma", "4Runner", "4Runner")
    }

    @After fun close() = db.closeWhenIdle()

    private fun viewModel(unit: DistanceUnit = DistanceUnit.MILES) = ManualAddViewModel(nhtsa.client, db.vehicles(), testSettings(distance = unit))

    @Test
    fun `NHTSA's makes come tidied, and asking for them sends nothing about the vehicle`() {
        val vm = viewModel()
        waitFor("the makes") { vm.makes is PickList.Loaded }
        assertEquals(PickList.Loaded(listOf("Ford", "TOYOTA", "Volkswagen")), vm.makes)
        assertTrue(nhtsa.asked.all { it.encodedPath.contains("GetMakesForVehicleType") && it.parameters.names() == setOf("format") })
    }

    @Test
    fun `makes that can't be loaded are offered again`() {
        nhtsa.makes = null
        val vm = viewModel()
        waitFor("the failure") { vm.makes == PickList.Unavailable }
        nhtsa.makes = listOf("Ford")
        vm.loadMakes()
        waitFor("the makes") { vm.makes == PickList.Loaded(listOf("Ford")) }
    }

    @Test
    fun `models wait for a year and a make, and only those two are sent`() {
        val vm = viewModel()
        vm.chooseYear(2019)
        assertEquals(PickList.Loaded(emptyList()), vm.models)
        vm.chooseMake("TOYOTA")
        waitFor("the models") { vm.models == PickList.Loaded(listOf("4Runner", "Tacoma")) }
        val asked = nhtsa.asked.single { "GetModelsForMakeYear" in it.encodedPath }
        assertTrue(asked.encodedPath.endsWith("/make/TOYOTA/modelyear/2019"))

        vm.chooseModel("4Runner")
        // Another make: the model belonged to the old one.
        nhtsa.models = null
        vm.chooseMake("Ford")
        assertEquals("", vm.model)
        waitFor("the failure") { vm.models == PickList.Unavailable }
    }

    @Test
    fun `the same year or make chosen again asks nothing new`() {
        val vm = viewModel()
        vm.chooseYear(2019)
        vm.chooseMake("TOYOTA")
        waitFor("the models") { vm.models is PickList.Loaded && (vm.models as PickList.Loaded).names.isNotEmpty() }
        val before = nhtsa.asked.size
        vm.chooseYear(2019)
        vm.chooseMake("TOYOTA")
        assertEquals(before, nhtsa.asked.size)
    }

    @Test
    fun `a vehicle added by hand has no VIN, and keeps only what was given`() = runBlocking {
        val vm = viewModel(unit = DistanceUnit.KILOMETERS)
        waitFor("the settings unit") { vm.unit == DistanceUnit.KILOMETERS }
        assertFalse(vm.canSave)
        vm.chooseYear(1987)
        vm.chooseMake("Volkswagen")
        vm.chooseModel("  Golf  ")
        vm.trim = "  "
        vm.engine = " 1.8L "
        vm.drivetrain = "FWD"
        vm.onOdometerChange("212,400")
        assertTrue(vm.canSave)

        var saved: Long? = null
        vm.save { saved = it }
        waitFor("the save") { saved != null }
        val v = db.vehicles().byId(saved!!)!!
        assertNull(v.vin)
        assertTrue(v.enteredByHand)
        assertEquals(Triple(1987, "Volkswagen", "Golf"), Triple(v.year, v.make, v.model))
        assertNull(v.trim)
        assertEquals("1.8L", v.engine)
        assertEquals("FWD", v.drivetrain)
        assertEquals(DistanceUnit.KILOMETERS.name, v.distanceUnit)
        assertEquals(listOf(212_400), db.odometer().readingsFor(saved!!).map { it.odometer })
    }

    @Test
    fun `nothing is saved without a year`() {
        val vm = viewModel()
        vm.chooseMake("Ford")
        vm.chooseModel("F-150")
        var saved = false
        vm.save { saved = true }
        assertFalse(saved)
        assertFalse(vm.saving)
    }
}
