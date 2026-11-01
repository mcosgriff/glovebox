package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.ScheduleExport
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.ui.ScheduleRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ScheduleViewModelTest {
    private val world = ScheduleWorld()
    private val day = LocalDate.of(2026, 3, 2)

    @After fun close() = world.close()

    private fun open(vehicleId: Long): ScheduleViewModel {
        val vm = world.viewModel(ScheduleViewModel::class) {
            ScheduleViewModel(
                routeHandle(ScheduleRoute(vehicleId)), VehicleLogs(world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(), world.settings),
                world.db.schedule(), world.db.vehicles(), world.actions, world.db.milestones(), world.makers, world.saver,
            )
        }
        world.keep(vm.state)
        world.keep(vm.hasMilestoneTable)
        eventually { vm.state.value != null }
        return vm
    }

    /** Waits until the screen's state shows what was just written. */
    private fun ScheduleViewModel.until(check: (ScheduleState) -> Boolean): ScheduleState {
        eventually { state.value?.let(check) == true }
        return state.value!!
    }

    /** The typical list as setup saves it, for a vehicle set up before a maker's list shipped. */
    private fun typical(vehicleId: Long, vehicle: VehicleEntity, tracked: Boolean = true, useSevere: Boolean = false): Map<String, Long> =
        ScheduleTemplates.generic(vehicle.drivetrain, vehicle.fuel, DistanceUnit.MILES).associate {
            it.key to world.item(
                ScheduleItemEntity(
                    vehicleId = vehicleId, name = it.label, templateKey = it.key, intervalMiles = it.normal.miles, intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles, severeMonths = it.severe?.months, tracked = tracked, useSevere = useSevere && it.severe != null,
                ),
            )
        }

    @Test
    fun `using the maker's schedule deletes a replaced typical item, but keeps one with a logged service untracked`() {
        val truck = ScheduleFixtures.fourRunner()
        val id = world.vehicle(truck, firstReading = 40_000)
        val typical = typical(id, truck)
        // An oil change was logged against the typical item: that record keeps its link.
        world.record(id, day, 41_000, line("Oil change", itemId = typical.getValue("oil")))
        val vm = open(id)

        val s = vm.until { it.known != null }
        val (list, plan) = s.known!!
        assertEquals("toyota-4runner-2017-2024", list.id)
        val doubles = plan.doubles.map { it.id }.toSet()
        assertTrue(typical.getValue("oil") in doubles)
        assertTrue(doubles.size > 1)

        vm.addKnown(doubles)
        eventually { world.items(id).any { it.knownList == list.id } }
        val rows = world.items(id)
        val oil = rows.first { it.id == typical.getValue("oil") }
        assertFalse(oil.tracked)
        // Every other double had nothing logged against it, so it is gone.
        (doubles - typical.getValue("oil")).forEach { gone -> assertTrue(rows.none { it.id == gone }) }
        assertEquals(plan.new.size, rows.count { it.knownList == list.id })
        assertTrue(rows.filter { it.knownList == list.id }.all { it.tracked && it.templateKey == null })
        // Its table comes too, as the vehicle had none.
        eventually { runBlocking { world.db.milestones().count(id) } > 0 }
        // Once everything is added, it is no longer offered.
        vm.until { it.known == null && it.startedFrom != null }
    }

    @Test
    fun `typical items the user chose to keep stay tracked beside the maker's`() {
        val truck = ScheduleFixtures.fourRunner()
        val id = world.vehicle(truck)
        val typical = typical(id, truck)
        val vm = open(id)
        vm.until { it.known != null }

        vm.addKnown(emptySet())
        eventually { world.items(id).any { it.knownList != null } }
        val rows = world.items(id)
        typical.values.forEach { kept -> assertTrue(rows.first { it.id == kept }.tracked) }
    }

    @Test
    fun `a schedule on severe use adds the maker's items on severe use too`() {
        val truck = ScheduleFixtures.fourRunner()
        val id = world.vehicle(truck)
        typical(id, truck, useSevere = true)
        val vm = open(id)
        val s = vm.until { it.known != null }
        assertEquals(DrivingUse.SEVERE, s.use)

        vm.addKnown(emptySet())
        eventually { world.items(id).any { it.knownList != null } }
        val added = world.items(id).filter { it.knownList != null }
        assertTrue(added.filter { it.severeMiles != null || it.severeMonths != null || it.severeOnly }.all { it.useSevere })
        assertTrue(added.filter { it.severeMiles == null && it.severeMonths == null && !it.severeOnly }.none { it.useSevere })
    }

    @Test
    fun `a vehicle whose setup was skipped has nothing, and the typical items can be added unticked`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        val vm = open(id)
        val s = vm.until { it.missing.isNotEmpty() }
        assertTrue(s.items.isEmpty())
        assertNull(s.known)
        assertEquals(ScheduleTemplates.generic(hatch.drivetrain, hatch.fuel, DistanceUnit.MILES).map { it.key }, s.missing.map { it.key })

        vm.addMissing()
        eventually { world.items(id).isNotEmpty() }
        assertTrue(world.items(id).none { it.tracked })
        vm.until { it.missing.isEmpty() }
    }

    @Test
    fun `a typical job one of the user's own items already resets isn't offered again`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        world.item(ScheduleItemEntity(vehicleId = id, name = "Synthetic oil", intervalMiles = 10_000, intervalMonths = 12, service = "oil"))
        val vm = open(id)
        val s = vm.until { it.items.isNotEmpty() }
        assertTrue(s.missing.none { it.key == "oil" })
        assertTrue(s.missing.any { it.key == "rotation" })
    }

    @Test
    fun `saving an edit keeps what the editor doesn't touch, and never puts a service on a typical item`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        val typical = typical(id, hatch)
        val rotation = typical.getValue("rotation")
        runBlocking { world.db.schedule().setSnooze(rotation, 9_000, null) }
        val vm = open(id)
        val draft = vm.until { it.items.isNotEmpty() }.items.first { it.key == rotation.toString() }
        vm.edit(draft)
        assertEquals(draft, vm.editing)
        assertFalse(vm.isNew(draft))

        vm.saveEdit(draft.copy(name = "Rotate tyres", normal = Interval(6_000, 6), service = "oil"))
        assertNull(vm.editing)
        eventually { world.items(id).first { it.id == rotation }.name == "Rotate tyres" }
        val row = world.items(id).first { it.id == rotation }
        assertEquals(6_000, row.intervalMiles)
        assertEquals(9_000, row.snoozedUntilOdometer)
        assertNull(row.service)
    }

    @Test
    fun `a new item of the user's own is inserted with its service`() {
        val id = world.vehicle(ScheduleFixtures.hatch())
        val vm = open(id)
        vm.addOwn()
        val blank = vm.editing!!
        assertTrue(vm.isNew(blank))
        vm.saveEdit(blank.copy(name = "Grease hitch", normal = Interval(3_000, null), service = "belt-check"))
        eventually { world.items(id).isNotEmpty() }
        val row = world.items(id).single()
        assertEquals("Grease hitch", row.name)
        assertEquals("belt-check", row.service)
        assertEquals(3_000, row.intervalMiles)
    }

    @Test
    fun `ticking, select all and clear all change only tracked`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        typical(id, hatch)
        val vm = open(id)
        val items = vm.until { it.items.isNotEmpty() }.items
        vm.edit(items.first())
        vm.setTracked(items.first(), false)
        assertNull(vm.editing)
        eventually { world.items(id).count { !it.tracked } == 1 }

        vm.until { s -> s.items.count { !it.tracked } == 1 }
        vm.setAllTracked(false)
        eventually { world.items(id).none { it.tracked } }
        vm.until { s -> s.items.none { it.tracked } }
        vm.setAllTracked(true)
        eventually { world.items(id).all { it.tracked } }
        vm.closeEditor()
        assertNull(vm.editing)
    }

    @Test
    fun `normal or severe use is set on every item with a severe interval`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        typical(id, hatch)
        val vm = open(id)
        assertEquals(DrivingUse.NORMAL, vm.until { it.items.isNotEmpty() }.use)
        vm.chooseSevereUse(true)
        vm.until { it.use == DrivingUse.SEVERE }
        vm.chooseSevereUse(false)
        vm.until { it.use == DrivingUse.NORMAL }
    }

    @Test
    fun `runs mostly on E85 changes only items with an E85 interval`() {
        val id = world.vehicle(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Sequoia", createdOn = ScheduleFixtures.ADDED))
        val flex = world.item(ScheduleItemEntity(vehicleId = id, name = "Oil and filter", intervalMiles = 10_000, intervalMonths = 12, e85Miles = 5_000, e85Months = 6))
        val other = world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = 6))
        val vm = open(id)
        assertEquals(false, vm.until { it.items.size == 2 }.e85)

        vm.chooseE85Use(true)
        vm.until { it.e85 == true }
        assertTrue(world.items(id).first { it.id == flex }.useE85)
        assertFalse(world.items(id).first { it.id == other }.useE85)
        vm.chooseE85Use(false)
        vm.until { it.e85 == false }
    }

    @Test
    fun `a date and mileage entered for an item is a record marked entered, and counts as its last done`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch, firstReading = 50_000)
        val typical = typical(id, hatch)
        val vm = open(id)
        val before = vm.until { it.items.isNotEmpty() }
        // A job the vehicle hasn't reached yet counts down to its first mileage; the rest have nothing to count from.
        assertTrue(before.notLogged in 1 until before.items.size)
        val rotation = before.items.first { it.key == typical.getValue("rotation").toString() }
        assertNull(before.logs.getValue(rotation.key).lastDone)

        vm.enterLastDone(rotation, day, 48_000)
        val after = vm.until { it.logs.getValue(rotation.key).lastDone != null }
        val log = after.logs.getValue(rotation.key)
        assertEquals(day, log.lastDone!!.date)
        assertEquals(48_000, log.lastDone.odometer)
        assertTrue(log.entered)
        assertNotNull(log.recordId)
        assertEquals(before.notLogged - 1, after.notLogged)
        val record = runBlocking { world.db.service().observeRecords(id).first() }.single()
        assertTrue(record.record.enteredAsLastDone)
    }

    @Test
    fun `the schedule is written as an import file of its items, and handed to save as under its model's name`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        typical(id, hatch)
        val vm = open(id)
        vm.until { it.items.isNotEmpty() }
        val file = vm.written()
        assertNotNull(file.text)
        assertTrue(file.text!!.contains(ScheduleTemplates.generic(hatch.drivetrain, hatch.fuel, DistanceUnit.MILES).first().label))

        var asked: Pair<String, String>? = null
        world.saver.attach { mime, name -> asked = mime to name }
        vm.saveAsFile(file.text!!, "2008-volkswagen-golf")
        eventually { asked != null }
        assertEquals(ScheduleExport.fileName("2008-volkswagen-golf"), asked!!.second)
    }

    @Test
    fun `a maker's item names its guide once, and a table of the user's own is noticed`() {
        val truck = ScheduleFixtures.fourRunner()
        val id = world.vehicle(truck)
        world.item(ScheduleItemEntity(vehicleId = id, name = "Replace engine oil", intervalMiles = 10_000, intervalMonths = 12, knownList = "toyota-4runner-2017-2024"))
        val vm = open(id)
        val s = vm.until { it.items.isNotEmpty() }
        assertTrue(s.startedFrom!!.contains("4Runner"))
        assertFalse(vm.hasMilestoneTable.value)

        runBlocking { world.db.milestones().addKnown(id, "mine", listOf(io.cosgriff.glovebox.data.db.MilestoneEntity(vehicleId = id, atMiles = 30_000, orMonths = null) to listOf(io.cosgriff.glovebox.data.db.MilestoneJobEntity(milestoneId = 0, name = "Inspect brakes", service = null)))) }
        eventually { vm.hasMilestoneTable.value }
    }

    @Test
    fun `a vehicle that no longer exists has no state`() {
        val vm = world.viewModel(ScheduleViewModel::class) {
            ScheduleViewModel(
                routeHandle(ScheduleRoute(404)), VehicleLogs(world.db.vehicles(), world.db.schedule(), world.db.service(), world.db.odometer(), world.settings),
                world.db.schedule(), world.db.vehicles(), world.actions, world.db.milestones(), world.makers, world.saver,
            )
        }
        world.keep(vm.state)
        repeat(20) {
            idle()
            Thread.sleep(5)
        }
        assertNull(vm.state.value)
    }
}
