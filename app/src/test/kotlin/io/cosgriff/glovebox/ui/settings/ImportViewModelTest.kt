package io.cosgriff.glovebox.ui.settings

import android.net.Uri
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.ImportRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Settings → Import against a real database: what the preview says before anything is written, and what an
 * import writes. "Add new only" is the default; "Replace matching" lists what it overwrites; readings that look
 * mistyped are left out unless ticked; an import never deletes and only writes the user's own rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ImportViewModelTest {
    @get:Rule(order = 1) val main = MainDispatcherRule()

    @get:Rule(order = 0) val g = TestGraph()
    private val picker = ImportPicker(g.context, g.appScope)
    private val res get() = AppText.res

    @Before fun reset() = runBlocking { g.resetSettings() }


    private fun vm(route: ImportRoute = ImportRoute()) = ImportViewModel(
        g.db.vehicles(), g.settings, g.db.torque(), g.db.schedule(), g.db.service(), g.db.odometer(), g.db.milestones(),
        g.db.catalogRules(), g.actions, picker, g.documents, routeHandle(route),
    )

    private fun example(name: String) = File("../docs/import/examples/$name").readText()

    /** Picks [text] as a file, the way Android's picker hands one over, and waits until it has been read. */
    private suspend fun pick(text: String) {
        val file = File(g.context.cacheDir, "picked-${System.nanoTime()}.json").apply { writeText(text) }
        picker.onPicked(Uri.fromFile(file))
        picker.picked.awaitValue { it != null }
    }

    private suspend fun ImportViewModel.ready(): ImportPreview.Ready = preview.awaitValue { it is ImportPreview.Ready } as ImportPreview.Ready

    private suspend fun ImportViewModel.importAndWait(ready: ImportPreview.Ready, mode: ImportMode, stopDoubles: Boolean = false): ImportResult {
        import(ready, mode, stopDoubles, keepCopy = false)
        return result.awaitValue { it != null }!!
    }

    @Test
    fun `torque specs are previewed, then only the new rows and the blank ones are written by default`() = runTest {
        val id = g.vehicle()
        // A row that is only a name is filled; one the user typed differently is left as it is unless they choose to replace.
        g.db.torque().insert(TorqueSpecEntity(vehicleId = id, name = "Oil drain plug", category = "ENGINE"))
        g.db.torque().insert(TorqueSpecEntity(vehicleId = id, name = "Lug nuts", category = "WHEELS", newtonMeters = 123.0))
        val vm = vm()
        pick(example("torque-specs.json"))

        val view = vm.ready().view
        assertEquals(ImportKind.TORQUE_SPECS, view.kind)
        assertEquals(4, view.total)
        assertEquals(listOf("Valve cover bolts", "Skid plate bolts"), view.new.map { it.name })
        assertEquals(listOf("Oil drain plug"), view.fills.map { it.name })
        assertEquals(listOf("Lug nuts"), view.differs.map { it.name })
        assertNotNull("a line that would be replaced says what is saved now", view.differs.single().yours)
        assertEquals(3, view.changes(ImportMode.ADD_NEW))
        assertEquals(4, view.changes(ImportMode.REPLACE_MATCHING))
        assertEquals(res.getString(R.string.import_caution_torque), view.caution)
        assertEquals(res.getQuantityString(R.plurals.plan_rows, 3, "3"), view.count(3))

        val result = vm.importAndWait(vm.ready(), ImportMode.ADD_NEW)
        assertEquals(2, result.added)
        assertEquals(1, result.filled)
        assertEquals(0, result.replaced)
        assertEquals(1, result.unchanged)
        assertEquals(id, result.vehicleId)
        assertEquals("2019 4Runner", result.vehicleName)
        assertFalse(result.archived)

        val rows = g.db.torque().specsFor(id).associateBy { it.name }
        assertEquals(4, rows.size)
        assertEquals("the user's own figure stays under Add new only", 123.0, rows.getValue("Lug nuts").newtonMeters!!, 0.0)
        assertEquals("14 mm", rows.getValue("Oil drain plug").socketSize)
        assertNull("a finished import lets go of the file", picker.picked.value)
    }

    @Test
    fun `replace matching overwrites the rows it listed and keeps their names`() = runTest {
        val id = g.vehicle()
        val lug = g.db.torque().insert(TorqueSpecEntity(vehicleId = id, name = "LUG NUTS", category = "WHEELS", newtonMeters = 123.0, note = "Mine"))
        val vm = vm()
        pick(example("torque-specs.json"))

        val result = vm.importAndWait(vm.ready(), ImportMode.REPLACE_MATCHING)
        assertEquals(1, result.replaced)
        assertEquals(0, result.unchanged)
        val row = g.db.torque().specsFor(id).single { it.id == lug }
        assertEquals("LUG NUTS", row.name)
        assertEquals("21 mm", row.socketSize)
        assertEquals("Star pattern, re-check after 50 mi", row.note)
        assertEquals("FACTORY", row.source)
    }

    @Test
    fun `with several vehicles nothing is planned until one is chosen`() = runTest {
        g.vehicle(model = "4Runner")
        val second = g.vehicle(make = "Volkswagen", model = "GTI")
        val vm = vm()
        assertFalse(vm.vehicleFixed)
        pick(example("torque-specs.json"))
        assertEquals(ImportPreview.NeedsVehicle, vm.preview.awaitValue { it != null })

        vm.chooseVehicle(second)
        assertEquals(second, vm.vehicleId.awaitValue { it != null })
        assertEquals(4, vm.ready().view.new.size)
    }

    @Test
    fun `opened from a vehicle, that vehicle is the one imported into`() = runTest {
        g.vehicle(model = "4Runner")
        val second = g.vehicle(make = "Volkswagen", model = "GTI")
        val vm = vm(ImportRoute(second))
        assertTrue(vm.vehicleFixed)
        assertEquals(second, vm.vehicleId.awaitValue { it != null })
    }

    @Test
    fun `a file that isn't valid is shown with its errors and plans nothing`() = runTest {
        g.vehicle()
        val vm = vm()
        pick("""{"glovebox": "import", "version": 1, "kind": "torque-specs", "items": [{"name": " Lug nuts", "category": "wheels"}]}""")
        val preview = vm.preview.awaitValue { it != null }
        assertTrue(preview is ImportPreview.Invalid)
        assertTrue((preview as ImportPreview.Invalid).file.errors.isNotEmpty())
    }

    @Test
    fun `a sold vehicle takes service history but nothing else`() = runTest {
        val id = g.vehicle()
        g.db.vehicles().setArchived(id, LocalDate.of(2025, 1, 1))
        val vm = vm()
        pick(example("torque-specs.json"))
        assertEquals(ImportPreview.Refused(res.getString(R.string.import_sold_only_history)), vm.preview.awaitValue { it is ImportPreview.Refused })

        pick(example("service-history.json"))
        val view = vm.ready().view
        assertEquals(ImportKind.SERVICE_HISTORY, view.kind)
        assertTrue("a sold vehicle has no reminders to move", view.moves.values.all { it.isEmpty() })
        val result = vm.importAndWait(vm.ready(), ImportMode.ADD_NEW)
        assertTrue(result.archived)
        assertEquals(3, result.added)
    }

    @Test
    fun `service history comes in as imported records, and Add new only leaves a matching record alone`() = runTest {
        val id = g.vehicle()
        g.db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = id, name = "Oil change", templateKey = "oil", intervalMiles = 7_500, intervalMonths = 12)))
        // The same day and reading as one of the file's records, written differently.
        g.db.service().insertWithItems(
            io.cosgriff.glovebox.data.db.ServiceRecordEntity(vehicleId = id, date = LocalDate.of(2023, 4, 2), odometer = 31_800, diy = false, shopName = "Somewhere"),
            listOf(io.cosgriff.glovebox.data.db.ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Cabin filter")),
        )
        val vm = vm()
        pick(example("service-history.json"))

        val view = vm.ready().view
        assertEquals(2, view.new.size)
        assertEquals(1, view.differs.size)
        assertTrue("the oil change moves the oil reminder", view.moves.getValue(ImportMode.ADD_NEW).isNotEmpty())
        assertEquals(res.getString(R.string.import_caution_history), view.caution)

        val result = vm.importAndWait(vm.ready(), ImportMode.ADD_NEW)
        assertEquals(2, result.added)
        assertEquals(1, result.unchanged)
        val records = g.db.service().recordsFor(id)
        assertEquals(3, records.size)
        assertEquals(2, records.count { it.record.imported })
        val mine = records.single { it.record.date == LocalDate.of(2023, 4, 2) }
        assertEquals("Somewhere", mine.record.shopName)
        assertFalse(mine.record.imported)
        // The oil line is saved against the reminder it resets.
        val march = records.single { it.record.date == LocalDate.of(2024, 3, 1) }
        assertTrue(march.items.any { it.label == "Oil & filter change" && it.scheduleItemId != null })
    }

    @Test
    fun `replace matching rewrites a matching record in place`() = runTest {
        val id = g.vehicle()
        val recordId = g.db.service().insertWithItems(
            io.cosgriff.glovebox.data.db.ServiceRecordEntity(vehicleId = id, date = LocalDate.of(2023, 4, 2), odometer = 31_800, diy = false, shopName = "Somewhere"),
            listOf(io.cosgriff.glovebox.data.db.ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Cabin filter")),
        )
        val vm = vm()
        pick(example("service-history.json"))
        val result = vm.importAndWait(vm.ready(), ImportMode.REPLACE_MATCHING)
        assertEquals(1, result.replaced)
        val record = g.db.service().recordsFor(id).single { it.record.id == recordId }
        assertTrue(record.record.diy)
        assertTrue(record.record.imported)
        assertEquals(listOf("Cabin air filter"), record.items.map { it.label })
    }

    @Test
    fun `a reading that looks mistyped is left out unless the user ticks it`() = runTest {
        val id = g.vehicle(firstReading = 30_000)
        g.db.odometer().insert(OdometerReadingEntity(vehicleId = id, date = LocalDate.of(2025, 6, 1), odometer = 52_000))
        val file = """
            {"glovebox": "import", "version": 1, "kind": "service-history", "items": [
              {"date": "2024-03-01", "odometer": 41000, "work": [{"label": "Oil & filter change", "service": "oil"}], "doneBy": {"type": "diy"}},
              {"date": "2024-09-01", "odometer": 450000, "work": [{"label": "Tire rotation", "service": "rotation"}], "doneBy": {"type": "diy"}}
            ]}
        """.trimIndent()
        val vm = vm()
        pick(file)

        val first = vm.ready().view
        val doubt = first.doubts.single()
        assertEquals(LocalDate.of(2024, 9, 1) to 450_000, doubt.key)
        assertFalse("left out by default", doubt.included)
        assertEquals(1, first.new.size)
        assertTrue(doubt.why.isNotEmpty())

        vm.include(doubt.key, true)
        val ticked = vm.preview.awaitValue { (it as? ImportPreview.Ready)?.view?.doubts?.singleOrNull()?.included == true } as ImportPreview.Ready
        assertEquals(2, ticked.view.new.size)

        vm.include(doubt.key, false)
        val unticked = vm.preview.awaitValue { (it as? ImportPreview.Ready)?.view?.doubts?.singleOrNull()?.included == false } as ImportPreview.Ready
        assertEquals(1, unticked.view.new.size)
        val result = vm.importAndWait(unticked, ImportMode.ADD_NEW)
        assertEquals(1, result.added)
        assertEquals(listOf(41_000), g.db.service().recordsFor(id).map { it.record.odometer })
    }

    @Test
    fun `picking another file starts again with nothing ticked and no result`() = runTest {
        g.vehicle()
        val vm = vm()
        pick(example("torque-specs.json"))
        vm.importAndWait(vm.ready(), ImportMode.ADD_NEW)
        vm.pick()
        assertNull(vm.result.value)
    }

    @Test
    fun `an interval schedule adds the user's own items and can stop a typical twin`() = runTest {
        val id = g.vehicle()
        g.db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = id, name = "Oil change", templateKey = "oil", intervalMiles = 7_500, intervalMonths = 12)))
        val vm = vm()
        pick(example("interval-schedule.json"))

        val view = vm.ready().view
        assertEquals(ImportKind.INTERVAL_SCHEDULE, view.kind)
        assertEquals(listOf("Oil change"), view.doubles)
        assertTrue(view.new.any { it.name == "Rotate tires" })

        val result = vm.importAndWait(vm.ready(), ImportMode.ADD_NEW, stopDoubles = true)
        assertEquals(1, result.stopped)
        val items = g.db.schedule().allFor(id)
        assertFalse("the typical twin is untracked, never deleted", items.single { it.templateKey == "oil" }.tracked)
        val imported = items.filter { it.templateKey == null }
        assertEquals(view.new.size, imported.size)
        assertTrue(imported.all { it.tracked })
    }

    @Test
    fun `a schedule in kilometres says it was converted for a vehicle in miles`() = runTest {
        g.vehicle()
        val vm = vm()
        pick(example("interval-schedule-km.json"))
        assertEquals(convertedNote(DistanceUnit.KILOMETERS, DistanceUnit.MILES), vm.ready().view.note)
        assertNotNull(convertedNote(DistanceUnit.KILOMETERS, DistanceUnit.MILES))
    }

    @Test
    fun `a milestone table is written in one go, and a file that repeats differently is refused`() = runTest {
        val id = g.vehicle()
        val vm = vm()
        pick(example("milestone-schedule.json"))
        val view = vm.ready().view
        assertEquals(ImportKind.MILESTONE_SCHEDULE, view.kind)
        val result = vm.importAndWait(vm.ready(), ImportMode.ADD_NEW)
        assertEquals(view.new.size, result.added)
        assertEquals(view.new.size, g.db.milestones().tableFor(id).size)
        assertEquals(120_000, g.db.vehicles().byId(id)!!.milestonesRepeatEveryMiles)

        val other = example("milestone-schedule.json").replace("\"repeatsEveryMiles\": 120000", "\"repeatsEveryMiles\": 150000")
        pick(other)
        val refused = vm.preview.awaitValue { it is ImportPreview.Refused } as ImportPreview.Refused
        assertTrue(refused.why.isNotEmpty())
    }

    @Test
    fun `a category rule files a line the file leaves unsaid, and the preview says so`() = runTest {
        val id = g.vehicle()
        g.db.catalogRules().put("Replaced left tail lamp", "bulbs", LocalDate.of(2026, 1, 1))
        val vm = vm()
        pick(example("service-history.json"))
        val view = vm.ready().view
        assertNotNull(view.note)
        assertTrue(view.note!!.contains("Replaced left tail lamp"))
        vm.importAndWait(vm.ready(), ImportMode.ADD_NEW)
        val line = g.db.service().recordsFor(id).flatMap { it.items }.single { it.label == "Replaced left tail lamp" }
        assertEquals("the wording is kept as written", "bulbs", line.catalogId)
    }

    @Test
    fun `history goes to the vehicle it was imported into`() = runTest {
        val id = g.vehicle()
        val vm = vm()
        vm.showHistory(id)
        assertEquals(id, g.actions.focus.value?.vehicleId)
    }
}
