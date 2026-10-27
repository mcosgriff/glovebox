package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.data.db.ScheduleWriter
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.ui.ScheduleSetupRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ScheduleSetupViewModelTest {
    private val world = ScheduleWorld()

    @After fun close() = world.close()

    private fun setup(vehicle: VehicleEntity): Pair<Long, ScheduleSetupViewModel> {
        val id = world.vehicle(vehicle)
        val vm = world.viewModel(ScheduleSetupViewModel::class) {
            ScheduleSetupViewModel(routeHandle(ScheduleSetupRoute(id)), world.db.vehicles(), world.settings, world.catalog, ScheduleWriter(world.db))
        }
        eventually { vm.items != null }
        return id to vm
    }

    private fun ScheduleSetupViewModel.saved(): Long {
        var done: Long? = null
        save { done = it }
        eventually { done != null }
        return done!!
    }

    @Test
    fun `a model with no maker's list starts from the typical list, saved as Glovebox's own items`() {
        val (id, vm) = setup(ScheduleFixtures.hatch())
        assertNull(vm.known)
        assertEquals(emptyList<Any>(), vm.engineChoices)
        val items = vm.items!!
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.template != null && !it.own && it.knownList == null })

        assertEquals(id, vm.saved())
        val rows = world.items(id)
        assertEquals(items.map { it.template!!.key }.toSet(), rows.map { it.templateKey }.toSet())
        // A typical list brings no milestone table.
        assertEquals(0, runBlocking { world.db.milestones().count(id) })
    }

    @Test
    fun `a bundled maker's schedule is the schedule, its items the user's own, and its table the milestones`() {
        val (id, vm) = setup(ScheduleFixtures.fourRunner())
        val known = assertNotNull(vm.known).let { vm.known!! }
        assertEquals("toyota-4runner-2017-2024", known.id)
        val items = vm.items!!
        assertTrue(items.all { it.own && it.template == null && it.knownList == known.id })

        vm.saved()
        val rows = world.items(id)
        assertEquals(items.size, rows.size)
        assertTrue(rows.all { it.templateKey == null && it.knownList == known.id })
        assertTrue(runBlocking { world.db.milestones().count(id) } > 0)
        assertEquals(known.id, runBlocking { world.db.vehicles().byId(id) }!!.milestonesKnownList)
    }

    @Test
    fun `with the engine not on record the owner is asked, and the answer is written to the vehicle`() {
        val (id, vm) = setup(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Tacoma", createdOn = ScheduleFixtures.ADDED))
        assertNull(vm.known)
        val choices = vm.engineChoices
        assertEquals(setOf("2.7L 4-cylinder", "3.5L V6"), choices.map { it.engine?.label }.toSet())
        // Until the owner answers, the typical list stands.
        assertTrue(vm.items!!.all { it.template != null })

        val v6 = choices.first { it.engine?.label == "3.5L V6" }
        vm.chooseEngine(v6)
        assertEquals(v6, vm.known)
        assertEquals(emptyList<Any>(), vm.engineChoices)
        assertTrue(vm.items!!.all { it.knownList == v6.id })

        vm.saved()
        val vehicle = runBlocking { world.db.vehicles().byId(id) }!!
        assertEquals("3.5L V6", vehicle.engine)
        // The Tacoma list names no powertrain, so none is written.
        assertNull(vehicle.powertrain)
    }

    @Test
    fun `with the body not on record the owner is asked, and the answer is written to the vehicle`() {
        val (id, vm) = setup(VehicleEntity(vin = null, year = 2026, make = "Toyota", model = "Corolla", engine = "2.0L 4-cyl", createdOn = ScheduleFixtures.ADDED))
        assertNull(vm.known)
        assertEquals(setOf(BodyStyle.SEDAN, BodyStyle.HATCHBACK), vm.engineChoices.map { it.body }.toSet())

        val hatch = vm.engineChoices.first { it.body == BodyStyle.HATCHBACK }
        vm.chooseEngine(hatch)
        assertEquals(hatch, vm.known)
        vm.saved()
        val vehicle = runBlocking { world.db.vehicles().byId(id) }!!
        assertEquals(BodyStyle.HATCHBACK.name, vehicle.bodyStyle)
        // The engine on record stays as it was.
        assertEquals("2.0L 4-cyl", vehicle.engine)
    }

    @Test
    fun `a body on record picks its own list`() {
        val (_, vm) = setup(
            VehicleEntity(vin = null, year = 2026, make = "Toyota", model = "Corolla", engine = "2.0L 4-cyl", bodyStyle = BodyStyle.SEDAN.name, createdOn = ScheduleFixtures.ADDED),
        )
        assertEquals("toyota-corolla-2026-sedan", vm.known?.id)
        assertEquals(emptyList<Any>(), vm.engineChoices)
    }

    @Test
    fun `"neither of these" keeps the typical list and stops asking`() {
        val (id, vm) = setup(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Tacoma", createdOn = ScheduleFixtures.ADDED))
        vm.declineEngines()
        assertEquals(emptyList<Any>(), vm.engineChoices)
        assertNull(vm.known)
        vm.saved()
        assertNull(runBlocking { world.db.vehicles().byId(id) }!!.engine)
        assertTrue(world.items(id).all { it.templateKey != null })
    }

    @Test
    fun `a flex-fuel list writes the powertrain, and E85 intervals apply only when the owner says so`() {
        val (id, vm) = setup(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Sequoia", createdOn = ScheduleFixtures.ADDED))
        val ffv = vm.engineChoices.first { it.engine?.powertrain == Powertrain.FLEX_FUEL }
        vm.chooseEngine(ffv)
        val withE85 = vm.items!!.filter { it.e85 != null }
        assertTrue(withE85.isNotEmpty())
        // Nothing is assumed: off until chosen.
        assertFalse(vm.e85Use)
        assertTrue(vm.items!!.none { it.useE85 || it.showsE85 })

        vm.chooseE85Use(true)
        assertTrue(vm.e85Use)
        vm.items!!.forEach { assertEquals(it.e85 != null, it.useE85) }
        withE85.forEach { item -> assertEquals(item.e85, vm.items!!.first { it.key == item.key }.active) }

        vm.saved()
        val rows = world.items(id)
        assertTrue(rows.filter { it.e85Miles != null || it.e85Months != null }.all { it.useE85 })
        assertTrue(rows.filter { it.e85Miles == null && it.e85Months == null }.none { it.useE85 })
        assertEquals(Powertrain.FLEX_FUEL.name, runBlocking { world.db.vehicles().byId(id) }!!.powertrain)

        // Choosing another engine starts the switches again.
        vm.chooseE85Use(false)
        assertTrue(vm.items!!.none { it.useE85 })
    }

    @Test
    fun `severe use switches every item to its severe interval, and choosing an engine resets it`() {
        val (_, vm) = setup(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Tacoma", createdOn = ScheduleFixtures.ADDED))
        vm.chooseSevereUse(true)
        assertTrue(vm.severeUse)
        assertTrue(vm.items!!.all { it.useSevere })
        vm.items!!.filter { it.severe != null }.forEach { assertEquals(it.severe, it.active) }

        vm.chooseEngine(vm.engineChoices.first())
        assertFalse(vm.severeUse)
        assertTrue(vm.items!!.none { it.useSevere })
        // A job only for severe use is listed but dormant on normal use.
        vm.items!!.filter { it.severeOnly }.forEach { assertTrue(it.dormant) }
    }

    @Test
    fun `unticked items are kept as untracked rows`() {
        val (id, vm) = setup(ScheduleFixtures.hatch())
        val first = vm.items!!.first()
        vm.toggle(first.key)
        assertFalse(vm.items!!.first().tracked)
        vm.saved()
        val rows = world.items(id)
        assertEquals(vm.items!!.size, rows.size)
        assertFalse(rows.first { it.templateKey == first.template!!.key }.tracked)
    }

    @Test
    fun `select all ticks everything, and with everything ticked clears it`() {
        val (_, vm) = setup(ScheduleFixtures.hatch())
        vm.toggleAll()
        assertTrue(vm.items!!.none { it.tracked })
        vm.toggleAll()
        assertTrue(vm.items!!.all { it.tracked })
        vm.toggle(vm.items!!.first().key)
        vm.toggleAll()
        assertTrue(vm.items!!.all { it.tracked })
    }

    @Test
    fun `an item of the user's own is added through the editor and saved with its service`() {
        val (id, vm) = setup(ScheduleFixtures.hatch())
        vm.addOwn()
        val blank = vm.editing!!
        assertTrue(vm.isNew(blank))
        assertTrue(blank.own)
        assertEquals(Interval(5_000, 6), blank.normal)

        val count = vm.items!!.size
        vm.saveEdit(blank.copy(name = "Grease hitch", normal = Interval(3_000, null), service = "belt-check", rule = DueRule.MILEAGE_ONLY))
        assertNull(vm.editing)
        assertEquals(count + 1, vm.items!!.size)
        assertFalse(vm.isNew(vm.items!!.last()))

        vm.saved()
        val row = world.items(id).first { it.name == "Grease hitch" }
        assertEquals(3_000, row.intervalMiles)
        assertNull(row.intervalMonths)
        assertNull(row.templateKey)
        assertEquals("belt-check", row.service)
        assertEquals(DueRule.MILEAGE_ONLY.name, row.ruleOverride)
    }

    @Test
    fun `editing a typical item changes it in place and never keeps a service on it`() {
        val (id, vm) = setup(ScheduleFixtures.hatch())
        val first = vm.items!!.first()
        vm.edit(first.key)
        assertEquals(first, vm.editing)
        vm.saveEdit(first.copy(normal = Interval(7_500, 9), service = "oil"))
        assertEquals(Interval(7_500, 9), vm.items!!.first().normal)

        vm.saved()
        val row = world.items(id).first { it.templateKey == first.template!!.key }
        assertEquals(7_500, row.intervalMiles)
        assertNull(row.service)
    }

    @Test
    fun `stop tracking in the editor unticks the item and closes it, and close leaves it alone`() {
        val (_, vm) = setup(ScheduleFixtures.hatch())
        val first = vm.items!!.first()
        vm.edit(first.key)
        vm.stopTracking(first.key)
        assertNull(vm.editing)
        assertFalse(vm.items!!.first().tracked)

        vm.edit(first.key)
        vm.closeEditor()
        assertNull(vm.editing)
        assertFalse(vm.items!!.first().tracked)
    }

    @Test
    fun `a draft and the row it is saved as agree, and a row with no interval has no draft`() {
        val draft = DraftItem(
            key = "k", name = "Job", normal = Interval(10_000, 12), severe = Interval(5_000, 6), useSevere = true,
            rule = DueRule.TIME_ONLY, service = "oil", firstAt = 60_000, e85 = Interval(4_000, null), useE85 = true,
        )
        val row = draft.toEntity(vehicleId = 3)
        assertEquals("oil", row.service)
        assertTrue(row.useE85)
        val back = row.copy(id = 9).toDraft(null)!!
        assertEquals(draft.copy(key = "9"), back)

        // A typical item's draft never carries a service onto its row.
        assertNull(draft.copy(own = false).toEntity(3).service)
        // No E85 interval: never on it, whatever the switch.
        assertFalse(draft.copy(e85 = null).toEntity(3).useE85)
        assertNull(row.copy(intervalMiles = null, intervalMonths = null).toDraft(null))
    }
}
