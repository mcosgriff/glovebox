package io.cosgriff.glovebox.ui.torque

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.ImportFiles
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueExport
import io.cosgriff.glovebox.domain.TorqueImport
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueSpecs
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.TorqueSpecsRoute
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.garage.TestViewModels
import io.cosgriff.glovebox.ui.garage.UnconfinedMainRule
import io.cosgriff.glovebox.ui.garage.awaitMatching
import io.cosgriff.glovebox.ui.garage.routeHandle
import io.cosgriff.glovebox.ui.garage.testDatabase
import io.cosgriff.glovebox.ui.garage.testSettings
import io.cosgriff.glovebox.ui.garage.waitFor
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What a torque screen needs over one in-memory database. Used by the screen tests too. */
internal class TorqueHarness(unit: TorqueUnit = TorqueUnit.NEWTON_METER) {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db = testDatabase(context)
    val settings = testSettings(torque = unit)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val viewModels = TestViewModels()
    val saver = ExportSaver(context, scope)

    /** The screenshot truck: a 2019 4Runner, which a bundled list is for. */
    fun addTruck(model: String = "4Runner"): Long = runBlocking { db.vehicles().add(Fixtures.truck.copy(id = 0, model = model), firstReading = null) }

    fun spec(vehicleId: Long, name: String, category: TorqueCategory = TorqueCategory.ENGINE, nm: Double? = null, source: TorqueSource = TorqueSource.USER, knownList: String? = null) =
        TorqueSpecEntity(vehicleId = vehicleId, name = name, category = category.name, newtonMeters = nm, source = source.name, knownList = knownList)

    fun insert(vararg specs: TorqueSpecEntity) = runBlocking { db.torque().insertAll(specs.toList()) }

    fun specs(vehicleId: Long) = runBlocking { db.torque().specsFor(vehicleId) }

    fun viewModel(vehicleId: Long) = viewModels.make {
        TorqueSpecsViewModel(
            routeHandle(TorqueSpecsRoute(vehicleId)), db.vehicles(), db.torque(), settings,
            TorqueSpecUndo(db.torque(), db.vehicles(), scope), saver, KnownSpecCatalog(context),
        )
    }

    fun close() {
        viewModels.clear()
        scope.cancel()
        db.close()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class TorqueSpecsViewModelTest {
    @get:Rule val main = UnconfinedMainRule()

    private val h = TorqueHarness()

    @After fun close() = h.close()

    @Test
    fun `specs are grouped by category, then by name, and the bundled list is offered for its model`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.spec(truck, "wheel bearing nut", TorqueCategory.WHEELS), h.spec(truck, "Spark plugs"), h.spec(truck, "Axle nut", TorqueCategory.WHEELS))
        val state = h.viewModel(truck).state.awaitMatching { it?.specs?.size == 3 && it.known != null }!!
        assertEquals(listOf("Spark plugs", "Axle nut", "wheel bearing nut"), state.specs.map { it.name })
        assertEquals(listOf(TorqueCategory.ENGINE, TorqueCategory.WHEELS), state.categories)
        assertEquals(TorqueUnit.NEWTON_METER, state.settingsUnit)
        val (list, plan) = state.known!!
        assertEquals("toyota-4runner-2017-2024", list.id)
        assertEquals(3, plan.changes)
        assertTrue(state.knownSources.getValue(list.id).isNotBlank())
    }

    @Test
    fun `no list is offered for a model none is bundled for`() = runBlocking {
        val car = h.addTruck(model = "Made-up Model")
        val state = h.viewModel(car).state.awaitMatching { it != null }!!
        assertNull(state.known)
    }

    @Test
    fun `a value is stored in N·m whatever unit it was typed in, and an edit keeps where the row came from`() = runBlocking {
        val truck = h.addTruck()
        val vm = h.viewModel(truck)
        vm.state.awaitMatching { it != null }
        vm.save(SpecDraft(0, "Drain plug", TorqueCategory.ENGINE, "14 mm", Torque.of(30.0, TorqueUnit.FOOT_POUND), TorqueSource.USER, "New washer"))
        val saved = vm.state.awaitMatching { it?.specs?.size == 1 }!!.specs.single()
        assertEquals(30.0 * TorqueUnit.FOOT_POUND.newtonMetersPerUnit, saved.newtonMeters!!, 1e-9)
        assertEquals("14 mm", saved.socketSize)

        h.insert(h.spec(truck, "Spare tire carrier", TorqueCategory.OTHER, 50.0, TorqueSource.REFERENCE, knownList = "toyota-4runner-2017-2024"))
        val listed = vm.state.awaitMatching { it?.specs?.size == 2 }!!.specs.first { it.knownList != null }
        vm.save(SpecDraft(listed.id, "Spare tire carrier", TorqueCategory.OTHER, null, Torque(51.0), TorqueSource.USER, null))
        val edited = vm.state.awaitMatching { s -> s?.specs?.any { it.newtonMeters == 51.0 } == true }!!.specs.first { it.id == listed.id }
        assertEquals("toyota-4runner-2017-2024", edited.knownList)
        assertEquals(TorqueSource.USER.name, edited.source)
    }

    @Test
    fun `the common fasteners are added as names only, skipping any already there`() = runBlocking {
        val truck = h.addTruck()
        val vm = h.viewModel(truck)
        val lug = TorqueSpecs.common.first { it.key == "lug-nuts" }.label
        h.insert(h.spec(truck, lug.uppercase(), TorqueCategory.WHEELS, 100.0))
        vm.state.awaitMatching { it?.specs?.size == 1 }
        vm.addCommon()
        val specs = vm.state.awaitMatching { it?.specs?.size == TorqueSpecs.common.size }!!.specs
        assertEquals(1, specs.count { it.name.equals(lug, ignoreCase = true) })
        // Glovebox has no values of its own: every added row is empty.
        assertTrue(specs.filter { it.name != lug.uppercase() }.all { it.newtonMeters == null && it.socketSize == null })
    }

    @Test
    fun `the bundled list fills new and name-only rows, tagged to double-check, and never touches the user's own`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.spec(truck, "Lug nuts, steel wheels", TorqueCategory.WHEELS, 99.9), h.spec(truck, "Spare tire carrier", TorqueCategory.WHEELS))
        val vm = h.viewModel(truck)
        vm.state.awaitMatching { it?.known != null }
        vm.addKnown()
        val specs = vm.state.awaitMatching { it?.specs?.size == 3 && it.known == null }!!.specs.associateBy { it.name }

        val mine = specs.getValue("Lug nuts, steel wheels")
        assertEquals(99.9, mine.newtonMeters!!, 0.0)
        assertNull(mine.knownList)
        for (name in listOf("Lug nuts, aluminum wheels", "Spare tire carrier")) {
            val row = specs.getValue(name)
            assertNotNull(row.newtonMeters)
            assertEquals(TorqueSource.REFERENCE.name, row.source)
            assertEquals("toyota-4runner-2017-2024", row.knownList)
        }
    }

    @Test
    fun `a deleted spec comes back with Undo, unless the vehicle has gone since`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.spec(truck, "Drain plug", nm = 33.3))
        val vm = h.viewModel(truck)
        val spec = vm.state.awaitMatching { it?.specs?.size == 1 }!!.specs.single()
        var undo: (() -> Unit)? = null
        vm.delete(spec) { undo = it }
        waitFor("the delete") { undo != null }
        assertEquals(emptyList<Any>(), h.specs(truck))
        undo!!()
        waitFor("the undo") { h.specs(truck).isNotEmpty() }
        assertEquals(spec, h.specs(truck).single())

        val again = h.specs(truck).single()
        vm.delete(again) { undo = it }
        waitFor("the delete") { h.specs(truck).isEmpty() }
        h.db.vehicles().delete(h.db.vehicles().byId(truck)!!)
        undo!!()
        Thread.sleep(200)
        assertEquals(emptyList<Any>(), h.specs(truck))
    }

    @Test
    fun `the import file is written in the unit the user works in`() = runBlocking {
        h.close()
        val ftLb = TorqueHarness(TorqueUnit.FOOT_POUND)
        try {
            val truck = ftLb.addTruck()
            val vm = ftLb.viewModel(truck)
            assertNull(vm.written().text)
            ftLb.insert(ftLb.spec(truck, "Drain plug", nm = 40.0))
            vm.state.awaitMatching { it?.specs?.size == 1 && it.settingsUnit == TorqueUnit.FOOT_POUND }
            val file = vm.written()
            assertEquals(1, file.count)
            val torque = Json.parseToJsonElement(file.text!!).jsonObject.getValue("items").jsonArray.single().jsonObject.getValue("torque").jsonObject
            assertEquals(TorqueImport.unitNames.getValue(TorqueUnit.FOOT_POUND), torque.getValue("unit").jsonPrimitive.content)
        } finally {
            ftLb.close()
        }
    }

    @Test
    fun `the rows are saved as a file named for the model, never the VIN, after asking where`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.spec(truck, "Drain plug", nm = 40.0))
        val asked = mutableListOf<Pair<String, String>>()
        h.saver.attach { mime, name -> asked += mime to name }
        val vm = h.viewModel(truck)
        vm.state.awaitMatching { it?.specs?.size == 1 }
        vm.saveAsFile(vm.written().text!!)
        waitFor("the question") { asked.isNotEmpty() }
        val (mime, name) = asked.single()
        assertEquals(ImportFiles.MIME_TYPE, mime)
        assertEquals(TorqueExport.fileName("2019 Toyota 4Runner"), name)
        assertTrue(Fixtures.truck.vin!! !in name)
    }
}
