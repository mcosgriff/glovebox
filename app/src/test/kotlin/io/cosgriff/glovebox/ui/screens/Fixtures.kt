package io.cosgriff.glovebox.ui.screens

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.JobDone
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.LoggedReading
import io.cosgriff.glovebox.domain.MileageReadings
import io.cosgriff.glovebox.domain.Milestones
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.domain.ReminderPresentation
import io.cosgriff.glovebox.domain.ScheduleRef
import io.cosgriff.glovebox.domain.ScheduleSummary
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.ServiceEntry
import io.cosgriff.glovebox.domain.ServiceLinks
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.domain.categoryOf
import io.cosgriff.glovebox.domain.nextDueAfter
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageVehicle
import io.cosgriff.glovebox.ui.history.HistoryState
import io.cosgriff.glovebox.ui.history.RecordDetail
import io.cosgriff.glovebox.ui.history.WorkLine
import io.cosgriff.glovebox.ui.schedule.DraftItem
import io.cosgriff.glovebox.ui.schedule.ItemLog
import io.cosgriff.glovebox.ui.schedule.MilestonesState
import io.cosgriff.glovebox.ui.schedule.ScheduleState
import io.cosgriff.glovebox.ui.service.DueSuggestion
import io.cosgriff.glovebox.ui.service.LogServiceData
import io.cosgriff.glovebox.ui.service.LogServiceForm
import io.cosgriff.glovebox.ui.schedule.label
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.whatItTakes
import io.cosgriff.glovebox.ui.settings.ScheduleOwner
import io.cosgriff.glovebox.ui.torque.TorqueSpecsState
import io.cosgriff.glovebox.ui.vehicle.MileageReadingsState
import io.cosgriff.glovebox.ui.vehicle.VehicleDetails
import io.cosgriff.glovebox.ui.vehicle.VehicleHome
import java.time.LocalDate

/**
 * Made-up vehicles, records and readings for the screenshot tests. **Public repo: nothing here is
 * anyone's.** The VINs pass the check digit and belong to no vehicle anyone owns; every figure, shop
 * and note is invented.
 *
 * The truck's history is built the way the app builds it: the typical schedule, a few logged
 * services and a reading, put through `:domain`, so the screens show what the app would work out.
 */
object Fixtures {
    /** The day every screen is drawn on, so a reference doesn't change with the calendar. */
    val TODAY: LocalDate = LocalDate.of(2026, 6, 15)
    private val unit = DistanceUnit.MILES

    val truck = VehicleEntity(
        id = 1,
        vin = "JTEBU5JR7K5900001",
        year = 2019,
        make = "Toyota",
        model = "4Runner",
        trim = "SR5",
        nickname = "Blue truck",
        drivetrain = "4WD/4-Wheel Drive/4x4",
        transmission = "Automatic",
        engine = "4.0L V6",
        bodyClass = "Sport Utility Vehicle (SUV)",
        plate = "GBX 1234",
        plateRegion = "CO",
        createdOn = LocalDate.of(2024, 3, 2),
        oilSpec = "0W-20",
        oilApproval = "API SP",
        oilCapacity = 6.6,
        oilCapacityUnit = "QUARTS",
        tireSize = "265/70R17 113S",
        wiperDriverIn = 22,
        wiperPassengerIn = 20,
        wiperRearIn = 14,
    )

    val hatch = VehicleEntity(
        id = 2,
        vin = "WVWHV7AJ0DW900002",
        year = 2013,
        make = "Volkswagen",
        model = "GTI",
        nickname = "Hatch",
        createdOn = LocalDate.of(2025, 8, 20),
    )

    /** The typical schedule for the truck, with ids; the transmission and differential fluids untracked. */
    private val templates = ScheduleTemplates.generic(truck.drivetrain, null, unit)
    private val ids: Map<String, Long> = templates.mapIndexed { i, t -> t.key to 11L + i }.toMap()
    private val untracked = setOf("trans-fluid", "diff-fluid")
    private fun id(key: String) = ids.getValue(key)

    private val tracked: List<TrackedItem> = templates.filter { it.key !in untracked }
        .map { TrackedItem(id(it.key), it.label, it.normal, DueRule.WHICHEVER_FIRST) }

    private fun record(id: Long, monthsAgo: Long, odometer: Int, keys: List<String>, shop: String? = null, cents: Long? = null, notes: String? = null) =
        ServiceRecordWithItems(
            ServiceRecordEntity(id, truck.id, TODAY.minusMonths(monthsAgo), odometer, diy = shop == null, shopName = shop, costCents = cents, notes = notes),
            keys.mapIndexed { i, k -> ServiceRecordItemEntity(id * 10 + i, id, id(k), templates.first { it.key == k }.label, k) },
            emptyList(),
        )

    /** Newest first, as History lists them. */
    val truckRecords: List<ServiceRecordWithItems> = listOf(
        record(103, 3, 62_100, listOf("cabin-air"), shop = "Main Street Auto", cents = 4_850),
        record(102, 4, 61_400, listOf("rotation", "brake-inspect"), notes = "Pads at about half. Rear left rotor has a light lip."),
        record(101, 13, 55_100, listOf("oil", "rotation"), shop = "Main Street Auto", cents = 8_995),
    )

    /**
     * The truck's odometer readings: three years of mileage updates and its service records, with one
     * slipped digit (65,800 where it read about 60,500) for the chart and the list to point at.
     */
    val mileageReadings: MileageReadingsState = MileageReadingsState(
        vehicleName = "2019 Blue truck",
        rows = MileageReadings.rows(
            listOf(
                LoggedReading(1, TODAY.minusMonths(34), 31_200),
                LoggedReading(2, TODAY.minusMonths(26), 38_950),
                LoggedReading(3, TODAY.minusMonths(18), 49_800),
                LoggedReading(4, TODAY.minusMonths(9), 59_800),
                LoggedReading(5, TODAY.minusMonths(7), 65_800),
                LoggedReading(6, TODAY.minusDays(3), 63_480),
            ) + truckRecords.map { LoggedReading(it.record.id, it.record.date, it.record.odometer, fromRecord = true) },
            null,
            unit,
        ),
        swap = null,
        unit = unit,
    )

    private val truckEntries = truckRecords.map { r -> ServiceEntry(r.record.id, r.record.date, r.record.odometer, r.items.mapNotNull { it.scheduleItemId }.toSet()) }
    private val truckReadings = listOf(OdometerReading(TODAY.minusDays(3), 63_480))
    private val truckOdometer = 63_480

    /** The oil change overdue, the rest counting down, and two jobs not logged yet. */
    val truckComingUp: ComingUp = ReminderPresentation.comingUp(VehicleReminders.statuses(tracked, truckEntries, truckReadings, TODAY))

    /** The hatch: everything logged and in hand. */
    val hatchComingUp: ComingUp = run {
        val oil = TrackedItem(51, "Oil & filter change", Interval(10_000, 12), DueRule.WHICHEVER_FIRST)
        ReminderPresentation.comingUp(
            VehicleReminders.statuses(
                listOf(oil),
                listOf(ServiceEntry(201, TODAY.minusMonths(1), 88_900, setOf(oil.id))),
                listOf(OdometerReading(TODAY.minusDays(1), 89_650)),
                TODAY,
            ),
        )
    }

    val garage = Garage(
        listOf(
            GarageVehicle(truck, truckOdometer, truckComingUp, oil = null, recalls = OpenRecalls(1, "Fuel pump")),
            GarageVehicle(hatch, 89_650, hatchComingUp, oil = null),
        ),
    )

    val vehicleHome = VehicleHome(truck, truckOdometer, DueRule.WHICHEVER_FIRST, truckComingUp, ReminderInputs(tracked, truckEntries, truckReadings))

    val history = HistoryState(listOf(truck, hatch), truck, truckRecords)

    /** The rotation and brake check: the brake check's reminder now counts from it, the rotation has been done again since. */
    val recordDetail: RecordDetail = run {
        val r = truckRecords[1]
        val nextDue = Interval(15_000, 12).nextDueAfter(LastDone(r.record.date, r.record.odometer)).text(unit)
        RecordDetail(
            record = r,
            vehicleName = "2019 Blue truck",
            unit = unit,
            readOnly = false,
            lines = listOf(
                WorkLine(r.items[0].label, ServiceCatalog.categoryOf(r.items[0].catalogId)?.label, AppText.res.getString(R.string.record_done_again), current = false),
                WorkLine(r.items[1].label, ServiceCatalog.categoryOf(r.items[1].catalogId)?.label, AppText.res.getString(R.string.record_next_due, nextDue), current = true),
            ),
            deleteImpact = emptyList(),
        )
    }

    val vehicleDetails = VehicleDetails(
        vehicle = truck,
        schedule = ScheduleSummary(tracked = tracked.size, severe = 0),
        recalls = OpenRecalls(1, "Fuel pump"),
        torqueSpecs = 3,
        documentCount = 2,
        documentBytes = 1_450_000,
    )

    val schedule: ScheduleState = run {
        ScheduleState(
            vehicle = truck,
            items = templates.map { t ->
                DraftItem(key = t.key, name = t.label, normal = t.normal, severe = t.severe, template = t, tracked = t.key !in untracked)
            },
            templateKeys = templates.associate { it.key to it.key },
            logs = templates.associate { t ->
                val done = ServiceLog.lastDone(id(t.key), truckEntries)
                t.key to ItemLog(done, done, truckRecords.firstOrNull { r -> r.items.any { it.scheduleItemId == id(t.key) } }?.record?.id, entered = false, estimate = null)
            },
            defaultRule = DueRule.WHICHEVER_FIRST,
            missing = emptyList(),
            odometer = truckOdometer,
            notLogged = truckComingUp.notLogged.size,
        )
    }

    val milestones: MilestonesState = run {
        val done = truckEntries.flatMap { e -> e.itemIds.map { JobDone(it.toString(), e.date, e.odometer, e.odometer, e.id) } }
        val list = Milestones.generate(tracked, Milestones.startFor(done, truckOdometer), truckOdometer)
        MilestonesState(
            vehicleName = "2019 Blue truck",
            milestones = Milestones.status(list, done, truckOdometer, continues = true),
            current = truckOdometer,
            swap = null,
            unit = unit,
            hasMileageItems = true,
            fromTable = false,
        )
    }

    val settings = Settings()
    val settingsVehicles = listOf(ScheduleOwner(1, "2019 Blue truck"), ScheduleOwner(2, "2013 Hatch"))

    val torque = TorqueSpecsState(
        vehicle = truck,
        specs = listOf(
            // Invented figures, not any vehicle's: a reference image is no place for a real torque value.
            TorqueSpecEntity(31, truck.id, "Skid plate bolts", TorqueCategory.ENGINE.name, socketSize = "12 mm", newtonMeters = 37.0),
            TorqueSpecEntity(32, truck.id, "Step bar brackets", TorqueCategory.WHEELS.name, socketSize = "17 mm", newtonMeters = 64.0, source = TorqueSource.FACTORY.name),
            TorqueSpecEntity(33, truck.id, "Spare tire carrier", TorqueCategory.WHEELS.name, note = "Look this one up before the next tire change"),
        ),
        settingsUnit = TorqueUnit.FOOT_POUND,
    )

    /** A new service, opened from the overdue oil change, with the reading still to type. */
    fun logService(): LogServiceForm {
        val refs = templates.map { ScheduleRef(id(it.key), it.label, it.key, tracked = it.key !in untracked) }
        val categories = ServiceCatalog.forVehicle(refs)
        val oil = categories.flatMap { it.options }.first { it.id == "oil" }
        return LogServiceForm(editing = false, today = TODAY).apply {
            data = LogServiceData(
                vehicleName = "2019 Blue truck",
                categories = categories,
                due = listOf(DueSuggestion(oil.id, oil.label, overdue = true)),
                tracked = tracked,
                entries = truckEntries,
                readings = ServiceLog.readings(truckEntries, truckReadings),
                lastOdometer = truckOdometer,
                swap = null,
                unit = unit,
                answering = ServiceLinks.answering(refs),
                specs = truck.whatItTakes(),
                original = null,
            )
            categoryId = categories.first { c -> c.options.any { it.id == oil.id } }.id
            toggle(oil.id)
        }
    }
}
