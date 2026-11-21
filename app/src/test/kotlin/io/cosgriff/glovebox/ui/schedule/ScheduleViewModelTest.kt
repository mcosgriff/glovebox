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
                world.scheduleItems, world.db.vehicles(), world.actions, world.db.milestones(), world.saver,
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
    private fun typical(vehicleId: Long, vehicle: VehicleEntity): Map<String, Long> =
        ScheduleTemplates.generic(vehicle.drivetrain, vehicle.fuel, DistanceUnit.MILES).associate {
            it.key to world.item(
                ScheduleItemEntity(
                    vehicleId = vehicleId, name = it.label, templateKey = it.key, intervalMiles = it.normal.miles, intervalMonths = it.normal.months,
                    severeMiles = it.severe?.miles, severeMonths = it.severe?.months,
                ),
            )
        }

    // What each write does to the rows is ScheduleItemsTest's (:data); these check the screen hands it on and shows the result.

    @Test
    fun `the maker's schedule is offered, and taking it up stops the doubles the owner picked and names its guide`() {
        val truck = ScheduleFixtures.fourRunner()
        val id = world.vehicle(truck, firstReading = 40_000)
        val typical = typical(id, truck)
        val vm = open(id)

        val (list, plan) = vm.until { it.known != null }.known!!
        assertEquals("toyota-4runner-2017-2024", list.id)
        assertNull(vm.state.value!!.startedFrom)
        val oil = typical.getValue("oil")
        assertTrue(oil in plan.doubles.map { it.id })

        vm.addKnown(setOf(oil))
        // Once everything is added, it is no longer offered, and the guide is named once.
        val after = vm.until { it.known == null && it.startedFrom != null }
        assertTrue(after.startedFrom!!.contains("4Runner"))
        assertEquals(plan.new.size, world.items(id).count { it.knownList == list.id })
        assertTrue(world.items(id).none { it.id == oil })
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
        val after = vm.until { it.missing.isEmpty() }
        assertTrue(after.items.none { it.tracked })
        // Worded as the app words the typical items.
        assertEquals(s.missing.map { it.label }.toSet(), after.items.map { it.name }.toSet())
    }

    @Test
    fun `the editor saves over a saved item and adds a new one`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        val rotation = typical(id, hatch).getValue("rotation")
        val vm = open(id)
        val draft = vm.until { it.items.isNotEmpty() }.items.first { it.key == rotation.toString() }
        vm.edit(draft)
        assertEquals(draft, vm.editing)
        assertFalse(vm.isNew(draft))

        vm.saveEdit(draft.copy(name = "Rotate tyres", normal = Interval(6_000, 6)))
        assertNull(vm.editing)
        vm.until { s -> s.items.any { it.key == rotation.toString() && it.name == "Rotate tyres" } }

        vm.addOwn()
        val blank = vm.editing!!
        assertTrue(vm.isNew(blank))
        val count = vm.state.value!!.items.size
        vm.saveEdit(blank.copy(name = "Grease hitch", normal = Interval(3_000, null), service = "belt-check"))
        val row = vm.until { it.items.size == count + 1 }.items.first { it.name == "Grease hitch" }
        assertEquals("belt-check", row.service)
    }

    @Test
    fun `ticking, select all and clear all are shown as they are written`() {
        val hatch = ScheduleFixtures.hatch()
        val id = world.vehicle(hatch)
        typical(id, hatch)
        val vm = open(id)
        val items = vm.until { it.items.isNotEmpty() }.items
        vm.edit(items.first())
        vm.setTracked(items.first(), false)
        assertNull(vm.editing)
        vm.until { s -> s.items.count { !it.tracked } == 1 }

        vm.setAllTracked(false)
        vm.until { s -> s.items.none { it.tracked } }
        vm.setAllTracked(true)
        vm.until { s -> s.items.all { it.tracked } }
        vm.closeEditor()
        assertNull(vm.editing)
    }

    @Test
    fun `normal or severe use, and E85, are the schedule's switches`() {
        val id = world.vehicle(VehicleEntity(vin = null, year = 2016, make = "Toyota", model = "Sequoia", createdOn = ScheduleFixtures.ADDED))
        world.item(ScheduleItemEntity(vehicleId = id, name = "Oil and filter", intervalMiles = 10_000, intervalMonths = 12, severeMiles = 5_000, severeMonths = 6, e85Miles = 5_000, e85Months = 6))
        world.item(ScheduleItemEntity(vehicleId = id, name = "Rotate tires", intervalMiles = 5_000, intervalMonths = 6))
        val vm = open(id)
        val s = vm.until { it.items.size == 2 }
        assertEquals(DrivingUse.NORMAL, s.use)
        assertEquals(false, s.e85)

        vm.chooseSevereUse(true)
        vm.until { it.use == DrivingUse.SEVERE }
        vm.chooseSevereUse(false)
        vm.until { it.use == DrivingUse.NORMAL }
        vm.chooseE85Use(true)
        vm.until { it.e85 == true }
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
                world.scheduleItems, world.db.vehicles(), world.actions, world.db.milestones(), world.saver,
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
