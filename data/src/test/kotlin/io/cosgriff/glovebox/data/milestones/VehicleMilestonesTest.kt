package io.cosgriff.glovebox.data.milestones

import io.cosgriff.glovebox.data.db.MilestoneEntity
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.data.db.MilestoneWithJobs
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.log.VehicleLog
import io.cosgriff.glovebox.data.log.log
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.Gearbox
import io.cosgriff.glovebox.domain.GearboxOnRecord
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSpecSource
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.MilestonePlace
import io.cosgriff.glovebox.domain.MilestoneStatus
import io.cosgriff.glovebox.domain.Milestones
import io.cosgriff.glovebox.domain.TableJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** A vehicle's milestones (#621), worked out from logs and tables built by hand. Every vehicle, list and record is made up. */
class VehicleMilestonesTest {
    private val day = LocalDate.of(2026, 5, 4)

    private fun vehicle(swapped: Boolean = false) = VehicleEntity(
        id = 1, vin = null, year = 2019, make = "Madeup", model = "Wagon", createdOn = day.minusYears(6),
        // A cluster put in on [day]: the old one read 150,000, the new one 20,000.
        clusterSwappedOn = day.takeIf { swapped }, odometerOffset = if (swapped) 130_000 else 0, clusterStartReading = 20_000.takeIf { swapped },
    )

    private fun item(id: Long, name: String, miles: Int?, months: Int? = null, firstMiles: Int? = null, knownList: String? = null, gearboxOnly: Gearbox? = null) =
        ScheduleItemEntity(
            id = id, vehicleId = 1, name = name, intervalMiles = miles, intervalMonths = months, firstMiles = firstMiles,
            knownList = knownList, gearboxOnly = gearboxOnly?.name,
        )

    private fun line(label: String, catalogId: String? = null, scheduleItemId: Long? = null) =
        ServiceRecordItemEntity(recordId = 0, scheduleItemId = scheduleItemId, label = label, catalogId = catalogId)

    private fun record(id: Long, date: LocalDate, odometer: Int, vararg lines: ServiceRecordItemEntity) =
        ServiceRecordWithItems(ServiceRecordEntity(id = id, vehicleId = 1, date = date, odometer = odometer, diy = true), lines.toList(), emptyList())

    private fun reading(date: LocalDate, odometer: Int) = OdometerReadingEntity(vehicleId = 1, date = date, odometer = odometer)

    private fun log(
        vehicle: VehicleEntity,
        items: List<ScheduleItemEntity> = emptyList(),
        records: List<ServiceRecordWithItems> = emptyList(),
        readings: List<OdometerReadingEntity> = emptyList(),
    ): VehicleLog = vehicle.log(items, records, readings, Settings())

    /** The user's table: each milestone's jobs in the order given. */
    private fun table(vararg rows: Pair<Int, List<TableJob>>) = rows.mapIndexed { i, (at, jobs) ->
        val id = i + 1L
        MilestoneWithJobs(MilestoneEntity(id = id, vehicleId = 1, atMiles = at), jobs.mapIndexed { p, j -> MilestoneJobEntity(milestoneId = id, name = j.name, service = j.service, position = p) })
    }

    private fun List<MilestoneStatus>.at(miles: Int) = first { it.milestone.atMiles == miles }

    private fun MilestoneStatus.names() = jobs.map { it.job.name }

    private val oil = TableJob("Change oil and filter", "oil")
    private val plugs = TableJob("Replace spark plugs")
    private val belt = TableJob("Replace timing belt")
    private val dsg = "Change DSG fluid"

    /**
     * A made-up maker's list. Its table (what [makersTable] saves) runs to 60,000: oil at every 10,000, plugs at
     * every 30,000 and the belt once. The guide gives a dual-clutch fluid change first at 80,000, past the table.
     */
    private val list = KnownScheduleList(
        id = "madeup-wagon-2018-2022",
        title = "2018–2022 Made-up Wagon",
        make = "Madeup",
        model = "Wagon",
        years = 2018..2022,
        source = KnownSpecSource("Made-up guide", null, "2026-11-20", "Made up for a test.", "Test data."),
        unit = DistanceUnit.MILES,
        milestones = listOf(ImportedMilestone(10_000, listOf(oil))),
        intervals = listOf(ImportedScheduleItem(oil.name, Interval(10_000, 12), service = "oil")),
        additional = listOf(ImportedScheduleItem(dsg, Interval(40_000, null), firstAt = 80_000)),
        dsgOnly = setOf(MilestoneImport.nameKey(dsg)),
    )

    private val makersTable = table(
        *(1..6).map { i ->
            val at = i * 10_000
            at to listOfNotNull(oil, plugs.takeIf { at % 30_000 == 0 }, belt.takeIf { at == 40_000 })
        }.toTypedArray(),
    )

    @Test
    fun `with a replaced cluster the schedule's milestones are placed by actual mileage, and so is a first time`() {
        val swapped = vehicle(swapped = true)
        val items = listOf(item(11, "Change oil", 10_000), item(12, "Replace timing belt", 60_000, firstMiles = 105_000))
        val before = record(1, day.minusMonths(3), 148_000, line("Oil", scheduleItemId = 11)) // on the old cluster
        val after = record(2, day.plusMonths(2), 25_000, line("Oil", scheduleItemId = 11)) // on the new one
        val view = VehicleMilestones.of(log(swapped, items, listOf(before, after), listOf(reading(day.plusMonths(4), 30_000))), emptyList(), emptyList())

        assertEquals(160_000, view.current)
        assertEquals(swapped.odometerOffset, view.swap?.offset)
        assertFalse(view.fromTable)
        assertTrue(view.hasMileageItems)
        // The belt comes round from its own first time in miles really covered: 105,000, then 165,000.
        assertEquals(listOf("Replace timing belt"), view.milestones.at(165_000).names())
        assertEquals(MilestonePlace.NEXT, view.milestones.at(165_000).place)
        // Each record is placed by actual mileage, with what its dash read that day.
        val oldOil = view.milestones.at(150_000).jobs.single().done!!
        assertEquals(148_000 to 148_000, oldOil.actualMiles to oldOil.readMiles)
        val newOil = view.milestones.at(160_000).jobs.single().done!!
        assertEquals(155_000 to 25_000, newOil.actualMiles to newOil.readMiles)
        // It starts where the records do.
        assertEquals(150_000, view.milestones.first().milestone.atMiles)
    }

    @Test
    fun `a milestone built from the schedule is done by the item a line counts for, never by its name`() {
        val items = listOf(item(11, "Rotate tires", 5_000), item(12, "Brake fluid", null, months = 36))
        val byItem = record(1, day.minusMonths(6), 4_800, line("Tire rotation", scheduleItemId = 11))
        val byName = record(2, day.minusMonths(1), 9_900, line("Rotate tires"))
        val view = VehicleMilestones.of(log(vehicle(), items, listOf(byItem, byName)), emptyList(), emptyList())

        assertEquals(1L, view.milestones.at(5_000).jobs.single().done?.recordId)
        assertNull(view.milestones.at(10_000).jobs.single().done)
        // Brake fluid repeats by time only, so it is on no milestone.
        assertTrue(view.milestones.none { m -> m.names().contains("Brake fluid") })
    }

    @Test
    fun `with nothing repeating by mileage there is nothing to build from`() {
        val view = VehicleMilestones.of(log(vehicle(), listOf(item(12, "Brake fluid", null, months = 36))), emptyList(), emptyList())
        assertFalse(view.hasMileageItems)
        assertTrue(view.milestones.isEmpty())
    }

    @Test
    fun `"Show more" reaches further ahead`() {
        val logged = log(vehicle(), listOf(item(11, "Rotate tires", 5_000)), readings = listOf(reading(day, 12_000)))
        val first = VehicleMilestones.of(logged, emptyList(), emptyList())
        val more = VehicleMilestones.of(logged, emptyList(), emptyList(), Milestones.AHEAD + Milestones.MORE)
        assertEquals(40_000, first.milestones.last().milestone.atMiles)
        assertEquals(70_000, more.milestones.last().milestone.atMiles)
    }

    @Test
    fun `while there is no table the maker's schedule the vehicle is on is offered, the one its items came from first`() {
        val other = list.copy(id = "madeup-coupe-2018-2022", title = "2018–2022 Made-up Coupe", model = "Coupe")
        val lists = listOf(list, other)
        // Matched by year, make and model.
        assertEquals(list.id, VehicleMilestones.of(log(vehicle()), emptyList(), lists).known?.id)
        // The list the schedule was taken up from wins over the match.
        val takenUp = log(vehicle(), listOf(item(11, "Change oil", 10_000, knownList = other.id)))
        assertEquals(other.id, VehicleMilestones.of(takenUp, emptyList(), lists).known?.id)
        // With a table, nothing is offered.
        assertNull(VehicleMilestones.of(takenUp, table(30_000 to listOf(plugs)), lists).known)
    }

    @Test
    fun `the generated view leaves out a one-gearbox job on "Don't know", and says what the vehicle is on record as`() {
        val items = listOf(item(11, "Change oil", 10_000), item(12, dsg, 40_000, gearboxOnly = Gearbox.DUAL_CLUTCH))
        val unsure = VehicleMilestones.of(log(vehicle().copy(drivetrain = "FWD", transmission = "Automated Manual"), items), emptyList(), emptyList())
        assertTrue(unsure.milestones.none { m -> m.names().contains(dsg) })
        assertEquals("FWD", unsure.drivetrain)
        assertEquals(GearboxOnRecord(null, "Automated Manual"), unsure.gearbox)

        val told = VehicleMilestones.of(log(vehicle().copy(gearbox = Gearbox.DUAL_CLUTCH.name), items), emptyList(), emptyList())
        assertEquals(setOf("Change oil", dsg), told.milestones.at(40_000).names().toSet())
    }

    @Test
    fun `a typed table is shown as it is, its jobs done by a line's catalog line or its name, by actual mileage`() {
        val swapped = vehicle(swapped = true)
        val rows = table(140_000 to listOf(TableJob("Replace cabin filter")), 160_000 to listOf(oil, plugs))
        val cabin = record(1, day.minusMonths(2), 139_000, line("replace cabin filter"))
        val oilChange = record(2, day.plusMonths(2), 28_000, line("Oil and filter", catalogId = "oil"))
        // Logged against the schedule's plugs item under another name: a table's job isn't done by an item.
        val plugsItem = record(3, day.plusMonths(2), 28_500, line("Plugs", scheduleItemId = 11))
        val logged = log(swapped, listOf(item(11, plugs.name, 30_000)), listOf(cabin, oilChange, plugsItem), listOf(reading(day.plusMonths(3), 29_000)))
        val view = VehicleMilestones.of(logged, rows, listOf(list))

        assertTrue(view.fromTable)
        assertTrue(view.hasMileageItems)
        assertEquals(159_000, view.current)
        assertEquals(listOf(140_000, 160_000), view.milestones.map { it.milestone.atMiles })
        assertEquals(listOf(140_000, 160_000), view.table.map { it.atMiles })
        assertEquals(listOf(oil, plugs), view.table[1].work)
        assertEquals(1L, view.milestones.at(140_000).jobs.single().done?.recordId)
        val done = view.milestones.at(160_000).jobs.first { it.job.name == oil.name }.done!!
        assertEquals(158_000 to 28_000, done.actualMiles to done.readMiles)
        assertNull(view.milestones.at(160_000).jobs.first { it.job.name == plugs.name }.done)
        // A table the owner typed ends where it ends, and says no list.
        assertNull(view.continuedAfter)
        assertNull(view.startedFrom)
        assertNull(view.repeatsEveryMiles)
    }

    @Test
    fun `a table that repeats comes round by its own cycle, and a cycle shorter than the table is not a repeat`() {
        val rows = table(10_000 to listOf(TableJob("Rotate tires")), 20_000 to listOf(TableJob("Rotate tires"), plugs))
        val reads = listOf(reading(day, 45_000))
        val repeats = VehicleMilestones.of(log(vehicle().copy(milestonesRepeatEveryMiles = 20_000), readings = reads), rows, emptyList())
        assertEquals(20_000, repeats.repeatsEveryMiles)
        assertEquals(20_000, repeats.milestones.at(40_000).milestone.repeatOf)
        assertEquals(listOf("Rotate tires", plugs.name), repeats.milestones.at(60_000).names())
        assertTrue(repeats.milestones.size > rows.size)
        // "Show more" carries a repeating table further.
        val further = VehicleMilestones.of(log(vehicle().copy(milestonesRepeatEveryMiles = 20_000), readings = reads), rows, emptyList(), Milestones.AHEAD + Milestones.MORE)
        assertTrue(further.milestones.size > repeats.milestones.size)

        val short = VehicleMilestones.of(log(vehicle().copy(milestonesRepeatEveryMiles = 15_000), readings = reads), rows, emptyList())
        assertNull(short.repeatsEveryMiles)
        assertEquals(listOf(10_000, 20_000), short.milestones.map { it.milestone.atMiles })
    }

    @Test
    fun `a maker's table is continued past its last column, and names the list it started from (#568)`() {
        val fromList = vehicle().copy(milestonesKnownList = list.id)
        val view = VehicleMilestones.of(log(fromList, readings = listOf(reading(day, 55_000))), makersTable, listOf(list))

        assertEquals(StartedFrom("Made-up guide, 2018–2022 Made-up Wagon"), view.startedFrom)
        assertEquals(60_000, view.continuedAfter)
        assertEquals(listOf(belt.name), view.notContinued)
        val continued = view.milestones.filter { it.milestone.continued }
        // To the vehicle's mileage plus the table's length.
        assertEquals((7..11).map { it * 10_000 }, continued.map { it.milestone.atMiles })
        assertEquals(listOf(oil.name, plugs.name), continued.first { it.milestone.atMiles == 90_000 }.names())
        // Nothing for the maker's list is ever offered beside a table.
        assertNull(view.known)
    }

    @Test
    fun `a continued table carries a one-gearbox job on "Don't know", as the table shows it, but not for another gearbox (#549)`() {
        val fromList = vehicle().copy(milestonesKnownList = list.id)
        val reads = listOf(reading(day, 55_000))
        val unsure = VehicleMilestones.of(log(fromList, readings = reads), makersTable, listOf(list))
        assertEquals(setOf(oil.name, dsg), unsure.milestones.at(80_000).names().toSet())
        assertTrue(unsure.milestones.at(80_000).milestone.continued)

        val manual = VehicleMilestones.of(log(fromList.copy(gearbox = Gearbox.MANUAL.name), readings = reads), makersTable, listOf(list))
        assertEquals(listOf(oil.name), manual.milestones.at(80_000).names())
        val automatic = VehicleMilestones.of(log(fromList.copy(gearbox = Gearbox.AUTOMATIC.name), readings = reads), makersTable, listOf(list))
        assertEquals(listOf(oil.name), automatic.milestones.at(80_000).names())
    }

    @Test
    fun `a maker's table whose list no longer ships still says it came from a maker, and a repeat wins over continuing it`() {
        val reads = listOf(reading(day, 55_000))
        val gone = VehicleMilestones.of(log(vehicle().copy(milestonesKnownList = "withdrawn-list"), readings = reads), makersTable, listOf(list))
        assertEquals(StartedFrom(null), gone.startedFrom)
        // Still a maker's table, carried on from its own rows.
        assertEquals(60_000, gone.continuedAfter)

        val repeating = VehicleMilestones.of(log(vehicle().copy(milestonesKnownList = list.id, milestonesRepeatEveryMiles = 60_000), readings = reads), makersTable, listOf(list))
        assertNull(repeating.continuedAfter)
        assertTrue(repeating.notContinued.isEmpty())
        assertEquals(10_000, repeating.milestones.at(70_000).milestone.repeatOf)
    }
}
