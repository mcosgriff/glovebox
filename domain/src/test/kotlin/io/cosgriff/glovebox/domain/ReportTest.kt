package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportTest {
    private val today = LocalDate.of(2026, 10, 24)
    private val lead = LeadTime(miles = 1_500)

    // Made-up schedule and records.
    private val oilItem = TrackedItem(1, "Oil & filter", Interval(10_000, 12), DueRule.WHICHEVER_FIRST)
    private val rotation = TrackedItem(2, "Tire rotation", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
    private val brakeCheck = TrackedItem(3, "Brake inspection", Interval(15_000, null), DueRule.MILEAGE_ONLY)
    private val diffFluid = TrackedItem(4, "Diff fluid", Interval(30_000, null), DueRule.MILEAGE_ONLY)
    private val cabin = TrackedItem(5, "Cabin air filter", Interval(30_000, null), DueRule.MILEAGE_ONLY)
    private val categories = mapOf(
        1L to ServiceGroup.OIL_ENGINE,
        2L to ServiceGroup.TIRES,
        3L to ServiceGroup.BRAKES,
        4L to ServiceGroup.FLUIDS_4WD,
        5L to ServiceGroup.OTHER,
    )

    private val fluids = record(LocalDate.of(2025, 10, 18), 50_200, diy = false, ServiceGroup.FLUIDS_4WD)
    private val brakes = record(LocalDate.of(2026, 2, 2), 54_800, diy = true, ServiceGroup.BRAKES)
    private val june = record(LocalDate.of(2026, 6, 14), 60_100, diy = false, ServiceGroup.OIL_ENGINE, ServiceGroup.TIRES)
    private val october = record(LocalDate.of(2026, 10, 10), 68_412, diy = true, ServiceGroup.OIL_ENGINE, ServiceGroup.TIRES)
    private val records = listOf(fluids, brakes, june, october)

    private val entries = listOf(
        ServiceEntry(1, fluids.date, fluids.odometer, setOf(4)),
        ServiceEntry(2, brakes.date, brakes.odometer, setOf(3)),
        ServiceEntry(3, june.date, june.odometer, setOf(1, 2)),
        ServiceEntry(4, october.date, october.odometer, setOf(1, 2)),
        ServiceEntry(5, LocalDate.of(2025, 6, 1), 45_000, setOf(5)),
    )

    private fun record(date: LocalDate, odometer: Int, diy: Boolean, vararg categories: ServiceGroup) =
        ExportRecord(date, odometer, summary(*categories.map { it.name }.toTypedArray(), groups = categories.toList()), if (diy) "DIY" else "Shop", null, diy = diy)

    private fun vehicle(
        records: List<ExportRecord> = this.records,
        items: List<TrackedItem> = listOf(oilItem, rotation, brakeCheck, diffFluid, cabin),
        recalls: List<ExportRecall> = listOf(ExportRecall("19V-670", "Suspension", LocalDate.of(2026, 1, 3)), ExportRecall("20V-012", "Fuel pump", null)),
    ): ExportVehicle {
        val readings = records.map { OdometerReading(it.date, it.odometer) }
        return ExportVehicle(
            title = "2019 Toyota 4Runner SR5",
            nickname = null,
            vin = null,
            unit = DistanceUnit.MILES,
            odometer = readings.maxOfOrNull { it.odometer },
            soldOn = null,
            records = records,
            schedule = items.map { ExportScheduleLine(it.name, "every", ServiceLog.lastDone(it.id, entries), it.id) },
            recalls = recalls,
            report = ReportFacts(VehicleReminders.statuses(items, entries, emptyList(), today, lead), categories, readings),
        )
    }

    private fun model(v: ExportVehicle = vehicle(), options: ExportOptions = ExportOptions()) =
        Reports.build(Exports.document(listOf(v), options, today)).single()

    @Test
    fun `stat tiles count the export's records, the distance since the first, and the recalls marked done`() {
        val stats = model().stats
        assertEquals(68_412, stats.odometer)
        assertEquals(4, stats.services)
        assertEquals(18_212, stats.sinceFirst)
        assertEquals(RecallTally(1, 2), stats.recalls)
    }

    @Test
    fun `a recall marked as not applying isn't counted, and with no recalls there is no tally`() {
        val archived = ExportRecall("21V-001", "Airbag", null, archivedOn = LocalDate.of(2026, 3, 1))
        assertEquals(RecallTally(0, 1), model(vehicle(recalls = listOf(ExportRecall("20V-012", "Fuel pump", null), archived))).stats.recalls)
        assertNull(model(vehicle(recalls = emptyList())).stats.recalls)
    }

    @Test
    fun `maintenance is current only when nothing is overdue and every job has something to count from`() {
        assertEquals(ReportStatus.Current, model().status)

        val overdue = TrackedItem(6, "Coolant", Interval(20_000, null), DueRule.MILEAGE_ONLY)
        val withOverdue = vehicle().let { v ->
            val e = entries + ServiceEntry(6, LocalDate.of(2024, 1, 1), 40_000, setOf(6))
            v.copy(report = v.report!!.copy(statuses = VehicleReminders.statuses(listOf(oilItem, overdue), e, emptyList(), today)))
        }
        assertEquals(ReportStatus.Overdue(1), model(withOverdue).status)

        val neverLogged = TrackedItem(7, "Brake fluid", Interval(null, 36), DueRule.TIME_ONLY)
        val withToLog = vehicle().let { v -> v.copy(report = v.report!!.copy(statuses = VehicleReminders.statuses(listOf(oilItem, neverLogged), entries, emptyList(), today))) }
        assertEquals(ReportStatus.ToLog(1), model(withToLog).status)
    }

    @Test
    fun `leaving the schedule out leaves out everything about what is due`() {
        val m = model(options = ExportOptions(scheduleAndRecalls = false))
        assertNull(m.status)
        assertTrue(m.dueNext.isEmpty())
        assertTrue(m.schedule.isEmpty())
        assertTrue(m.chart!!.lanes.all { it.nextDue == null })
    }

    @Test
    fun `lanes hold each group's services and its soonest next-due mileage`() {
        val chart = model().chart!!
        // Only the groups with work done or due get a lane.
        assertEquals(listOf(ServiceGroup.OIL_ENGINE, ServiceGroup.TIRES, ServiceGroup.BRAKES, ServiceGroup.FLUIDS_4WD, ServiceGroup.OTHER), chart.lanes.map { it.category })
        val byCategory = chart.lanes.associateBy { it.category }
        assertEquals(listOf(60_100, 68_412), byCategory.getValue(ServiceGroup.OIL_ENGINE).done)
        assertEquals(78_412, byCategory.getValue(ServiceGroup.OIL_ENGINE).nextDue)
        assertEquals(73_412, byCategory.getValue(ServiceGroup.TIRES).nextDue)
        assertEquals(69_800, byCategory.getValue(ServiceGroup.BRAKES).nextDue)
        assertEquals(80_200, byCategory.getValue(ServiceGroup.FLUIDS_4WD).nextDue)
        // Cabin filter: nothing done in the export, but a job coming due.
        assertEquals(emptyList<Int>(), byCategory.getValue(ServiceGroup.OTHER).done)
        assertEquals(75_000, byCategory.getValue(ServiceGroup.OTHER).nextDue)
        assertEquals(68_412, chart.now)
        assertEquals(records.map { it.date }, chart.visits.map { it.date })
    }

    @Test
    fun `the axis runs a little past the data with round ticks inside it`() {
        val chart = model().chart!!
        assertEquals(listOf(50_000, 60_000, 70_000, 80_000), chart.ticks)
        assertTrue(chart.low < 50_200 && chart.high > 80_200)
        assertTrue(chart.ticks.all { it in chart.low..chart.high })

        val (low, high, ticks) = Reports.axis(1_200, 1_900)
        assertEquals(listOf(1_200, 1_400, 1_600, 1_800), ticks.filter { it >= 1_200 })
        assertTrue(low < 1_200 && high > 1_900)
        val (oneLow, oneHigh, oneTicks) = Reports.axis(68_412, 68_412)
        assertTrue(oneLow < 68_412 && oneHigh > 68_412 && oneTicks.size >= 2)
    }

    @Test
    fun `nice steps are one, two, two and a half or five times a power of ten`() {
        assertEquals(10_000.0, Reports.niceStep(8_500.0), 0.0)
        assertEquals(2_500.0, Reports.niceStep(2_100.0), 0.0)
        assertEquals(500.0, Reports.niceStep(500.0), 0.0)
        assertEquals(1.0, Reports.niceStep(0.3), 0.0)
    }

    @Test
    fun `due next lists the furthest through their interval first, with what was just done last`() {
        val rows = model().dueNext
        assertEquals(listOf("Brake inspection", "Cabin air filter", "Diff fluid", "Tire rotation", "Oil & filter"), rows.map { it.name })
        assertEquals(DueNextState.DUE_SOON, rows[0].state)
        assertEquals(69_800, rows[0].dueAt)
        assertEquals(ServiceGroup.BRAKES, rows[0].category)
        assertEquals(DueNextState.OK, rows[1].state)
        assertEquals(DueNextState.JUST_DONE, rows[3].state)
        assertEquals(DueNextState.JUST_DONE, rows[4].state)
        assertEquals(78_412, rows[4].dueAt)
    }

    @Test
    fun `due next never shows more than six, and a job never logged gets no row`() {
        val many = (10L..20L).map { TrackedItem(it, "Job $it", Interval(10_000, null), DueRule.MILEAGE_ONLY) }
        val e = many.map { ServiceEntry(it.id, LocalDate.of(2026, 1, 1), 60_000 + it.id.toInt() * 100, setOf(it.id)) }
        val statuses = VehicleReminders.statuses(many + TrackedItem(99, "Never", Interval(null, 12), DueRule.TIME_ONLY), e, emptyList(), today)
        val rows = Reports.dueNext(statuses, emptyMap())
        assertEquals(Reports.DUE_NEXT, rows.size)
        assertTrue(rows.none { it.name == "Never" })
        assertTrue(rows.all { it.category == ServiceGroup.OTHER })
    }

    @Test
    fun `the date range narrows the log, the tiles and the chart`() {
        val m = model(options = ExportOptions(dates = ExportDates.Custom(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1))))
        assertEquals(listOf(june.date, brakes.date), m.log.map { it.single().date })
        assertEquals(2, m.stats.services)
        assertEquals(68_412 - 54_800, m.stats.sinceFirst)
        assertEquals(listOf(brakes.date, june.date), m.chart!!.visits.map { it.date })
        assertEquals(listOf(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 6, 14)), m.monthly!!.points.map { it.date })
    }

    @Test
    fun `who did the work leaves out a bare last done`() {
        val lastDone = ExportRecord(LocalDate.of(2025, 1, 1), 40_000, summary("Oil"), "", null, enteredAsLastDone = true)
        val m = model(vehicle(records = records + lastDone))
        assertEquals(2, m.diy)
        assertEquals(2, m.shop)
        assertEquals(4, m.stats.services)
    }

    @Test
    fun `miles per month are averaged over the readings and rounded`() {
        val monthly = model().monthly!!
        // 18,212 miles over 357 days.
        assertEquals(1_600, monthly.perMonth)
        assertEquals(fluids.date, monthly.from)
        assertEquals(october.date, monthly.to)
        assertEquals(1_500, Reports.roundAverage(1_549.0))
        assertEquals(430, Reports.roundAverage(432.0))
    }

    @Test
    fun `the schedule table gives each job's last done and next due`() {
        val rows = model().schedule.associateBy { it.name }
        assertEquals(68_412, rows.getValue("Oil & filter").lastDone!!.odometer)
        assertEquals(78_412, rows.getValue("Oil & filter").nextDue)
        assertEquals(ServiceGroup.FLUIDS_4WD, rows.getValue("Diff fluid").category)
        assertEquals(75_000, rows.getValue("Cabin air filter").nextDue)
    }

    @Test
    fun `an empty history has no chart, no average and no log`() {
        val m = model(vehicle(records = emptyList(), items = emptyList(), recalls = emptyList()))
        assertNull(m.chart)
        assertNull(m.monthly)
        assertNull(m.status)
        assertNull(m.stats.sinceFirst)
        assertEquals(0, m.stats.services)
        assertTrue(m.log.isEmpty() && m.dueNext.isEmpty() && m.schedule.isEmpty())
    }

    @Test
    fun `a replaced cluster puts every mark in dash terms`() {
        val swap = ClusterSwap(LocalDate.of(2026, 3, 1), offset = 50_000, startReading = 5_000)
        val v = vehicle().copy(clusterSwap = swap, odometer = 68_412 - 50_000)
        val m = model(v)
        // Records before the swap were read on the old cluster: 50,000 more than the dash now shows.
        assertEquals(listOf(200, 4_800), m.chart!!.visits.take(2).map { it.odometer })
        assertEquals(68_412 - 50_000 - 200, m.stats.sinceFirst)
    }

    @Test
    fun `a job counted by time alone is due by a date, and a line with no item or facts has no next due`() {
        val wipers = TrackedItem(6, "Wiper blades", Interval(null, 12), DueRule.TIME_ONLY)
        val withWipers = entries + ServiceEntry(6, LocalDate.of(2026, 3, 1), 56_000, setOf(6))
        val base = vehicle()
        val v = base.copy(
            schedule = base.schedule + ExportScheduleLine("Wiper blades", "every", ServiceLog.lastDone(6, withWipers), 6) + ExportScheduleLine("Made-up job", "every", null),
            report = base.report!!.copy(statuses = VehicleReminders.statuses(listOf(oilItem, rotation, brakeCheck, diffFluid, cabin, wipers), withWipers, emptyList(), today, lead)),
        )
        val m = model(v)
        val wiperRow = m.schedule.single { it.name == "Wiper blades" }
        assertEquals(LocalDate.of(2027, 3, 1), wiperRow.nextBy)
        assertNull(wiperRow.nextDue)
        assertEquals(ServiceGroup.OTHER, wiperRow.category)
        assertEquals(LocalDate.of(2027, 3, 1), m.dueNext.single { it.name == "Wiper blades" }.dueBy)
        val loose = m.schedule.single { it.name == "Made-up job" }
        assertNull(loose.nextDue)
        assertNull(loose.nextBy)

        // A vehicle without the report's facts still gets its log and tiles, with nothing said about what is due.
        val bare = model(base.copy(report = null, odometer = null))
        assertTrue(bare.dueNext.isEmpty())
        assertNull(bare.stats.sinceFirst)
        assertEquals(4, bare.log.size)
    }

    @Test
    fun `two readings on one day are one point, the higher`() {
        val sameDay = vehicle().let { it.copy(report = it.report!!.copy(readings = it.report!!.readings + OdometerReading(october.date, 68_000))) }
        val monthly = model(sameDay).monthly!!
        assertEquals(68_412, monthly.points.last().odometer)
        assertEquals(1, monthly.points.count { it.date == october.date })
    }
}
