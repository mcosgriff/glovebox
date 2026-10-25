package io.cosgriff.glovebox.ui.screens

import io.cosgriff.glovebox.ui.bulletins.BulletinsState
import io.cosgriff.glovebox.domain.VehicleBulletins
import io.cosgriff.glovebox.domain.BulletinProduct
import io.cosgriff.glovebox.domain.BulletinKind
import io.cosgriff.glovebox.domain.Bulletin
import io.cosgriff.glovebox.ui.settings.ImportPreview
import io.cosgriff.glovebox.ui.settings.PlanView
import io.cosgriff.glovebox.ui.settings.PlanLine
import io.cosgriff.glovebox.ui.settings.PlanNoun
import io.cosgriff.glovebox.ui.settings.DoubtLine
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.CatalogRuleEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.toEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.CustomGroup
import io.cosgriff.glovebox.domain.CustomLines
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
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.ReminderInputs
import io.cosgriff.glovebox.data.log.VehicleLog
import io.cosgriff.glovebox.domain.SavedLine
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
import io.cosgriff.glovebox.ui.history.visitRow
import io.cosgriff.glovebox.ui.history.VisitRow
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.ui.recalls.RecallsState
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.CustomCount
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageVehicle
import io.cosgriff.glovebox.ui.history.HistoryState
import io.cosgriff.glovebox.ui.history.SortPreview
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
        engineCode = "1GR-XX",
        tpms = "Direct",
        series = "XYZ100L",
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

    /** Six years of a reading a week, a service every six months among them: the chart has to stay readable. */
    val weeklyReadings: MileageReadingsState = MileageReadingsState(
        vehicleName = "2019 Blue truck",
        rows = MileageReadings.rows(
            // About 230 a week, unevenly: more in summer.
            (0 until 6 * 52).runningFold(9_000) { miles, w -> miles + if (w % 52 in 20..32) 320 else 200 }.drop(1).mapIndexed { w, miles ->
                LoggedReading(1_000L + w, TODAY.minusWeeks(6L * 52 - 1 - w), miles, fromRecord = w % 26 == 0)
            },
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

    val history = HistoryState(listOf(truck, hatch), truck, truckRecords, customLines = CustomCount(lines = 4, wordings = 3))

    /** Custom lines as a report might word them: one Glovebox recognises, one the user has a rule for, one neither. */
    val customGroups: List<CustomGroup> = CustomLines.groups(
        listOf(
            SavedLine(1, 101, LocalDate.of(2023, 3, 14), "Water pump gasket replaced", null, null),
            SavedLine(2, 102, LocalDate.of(2025, 6, 2), "Water pump gasket replaced", null, null),
            SavedLine(3, 103, LocalDate.of(2024, 9, 20), "Brakes done", null, null),
            SavedLine(4, 104, LocalDate.of(2024, 9, 20), "Hitch pin replaced", null, null),
        ),
        rules = listOf(CatalogRule("Brakes done", "brake-service")),
    )

    /** Made-up rules, by wording; the last names a line no version has, as one a later version dropped would. */
    val categoryRules: List<CatalogRuleEntity> = listOf(
        CatalogRule("Brakes done", "brake-service"),
        CatalogRule("Tire(s) mounted", "new-tires"),
        CatalogRule("Water pump gasket replaced", "water-pump"),
        CatalogRule("Flux capacitor serviced", "flux-capacitor"),
    ).mapIndexed { i, r -> r.toEntity(TODAY).copy(id = i + 1L) }

    /** Filing those: a made-up water pump reminder starts counting from the later line, and a job on the user's table. */
    val sortPreview = SortPreview(
        lines = 3,
        reminders = listOf(
            ReminderChange(TrackedItem(9, "Water pump", Interval(100_000, null), DueRule.MILEAGE_ONLY), before = null, after = LastDone(LocalDate.of(2025, 6, 2), 58_200)),
        ),
        tableJobs = listOf("Water pump"),
        unit = DistanceUnit.MILES,
    )

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

    /** A recall fixed at the shop the day the cabin filter was changed: a made-up campaign. */
    private val recallRecord = ServiceRecordWithItems(
        ServiceRecordEntity(104, truck.id, truckRecords[0].record.date, 62_100, diy = false, shopName = "Main Street Auto"),
        listOf(ServiceRecordItemEntity(1040, 104, null, AppText.res.getString(R.string.recall_service_line, Recalls.campaignLabel("26V100000"), "Fuel pump"))),
        emptyList(),
    )

    /** The cabin filter as imported from a report, with that recall's record linked as one visit (#307). */
    val recordInVisit: RecordDetail = run {
        val r = truckRecords[0].let { it.copy(record = it.record.copy(imported = true, visitId = 1)) }
        RecordDetail(
            record = r,
            vehicleName = "2019 Blue truck",
            unit = unit,
            readOnly = false,
            lines = listOf(WorkLine(r.items[0].label, ServiceCatalog.categoryOf(r.items[0].catalogId)?.label, null, current = false)),
            deleteImpact = emptyList(),
            visit = listOf(recallRecord.visitRow(unit)),
            choices = listOf(truckRecords[1].visitRow(unit)),
        )
    }

    /** The same two not linked yet: the record screen offers it. */
    val recordOffer: RecordDetail = recordInVisit.copy(
        record = recordInVisit.record.let { it.copy(record = it.record.copy(visitId = null)) },
        visit = emptyList(),
        offers = listOf(recallRecord.visitRow(unit)),
        choices = listOf(recallRecord.visitRow(unit), truckRecords[1].visitRow(unit)),
    )

    /**
     * The truck's history for the timeline, with its next reminder and one more, older record: brake pads and a
     * Custom line, three weeks before the oil change.
     */
    val historyTimeline: HistoryState = history.copy(
        records = truckRecords + ServiceRecordWithItems(
            ServiceRecordEntity(100, truck.id, TODAY.minusMonths(13).minusDays(20), 54_600, diy = true),
            listOf(
                ServiceRecordItemEntity(1_000, 100, null, "Front brake pads", "front-pads"),
                ServiceRecordItemEntity(1_001, 100, null, "Hitch greased", null),
                ServiceRecordItemEntity(1_002, 100, null, "Sway bar links", "sway-bar-links"),
                ServiceRecordItemEntity(1_003, 100, null, "Fuel filter", "fuel-filter"),
            ),
            emptyList(),
        ),
        next = truckComingUp.next,
    )

    /**
     * Years of made-up records on the truck, more than a screen of them: oil changes every six months with a
     * rotation every other one, for a list long enough to scroll the filters away and bring up Top.
     */
    val longHistory: HistoryState = history.copy(
        records = (0L until 24L).map { i ->
            record(200 + i, 3 + 6 * i, 62_000 - 3_000 * i.toInt(), if (i % 2 == 0L) listOf("oil", "rotation") else listOf("oil"),
                shop = "Main Street Auto".takeIf { i % 3 != 0L }, cents = 6_500L + 100 * i)
        },
        next = truckComingUp.next,
    )

    /** History with the imported cabin filter and the recall's record linked as one visit. */
    val historyWithVisit: HistoryState = history.copy(
        records = listOf(
            truckRecords[0].let { it.copy(record = it.record.copy(imported = true, visitId = 1)) },
            recallRecord.copy(record = recallRecord.record.copy(visitId = 1)),
        ) + truckRecords.drop(1),
    )

    /** A made-up recall on the truck, being marked done on a day it already has a record from the shop. */
    val recall = RecallEntity(
        id = 1, vehicleId = truck.id, campaign = "26V100000", component = "FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP",
        summary = "A made-up summary.", firstSeenOn = TODAY.minusMonths(2),
    )

    /** What the sheet offers when the cabin filter was changed at the shop the same day, as `RecallsState` words it. */
    val markDoneVisits: List<VisitRow> = RecallsState(
        truck, emptyList(), odometer = null,
        records = listOf(truckRecords[0].let { it.copy(record = it.record.copy(date = TODAY)) }),
    ).visitsOn(TODAY)

    /**
     * A made-up service-history file for the truck: two new visits, one that differs from a saved record, one
     * whose reading looks mistyped (left out until ticked), and the reminders an import would move.
     */
    val importPreview: ImportPreview.Ready = ImportPreview.Ready(
        PlanView(
            kind = ImportKind.SERVICE_HISTORY,
            noun = PlanNoun.RECORD,
            total = 4,
            new = listOf(
                PlanLine("Jan 12, 2026 · 60,900 mi", "Oil & filter change · Main Street Auto · $52.00"),
                PlanLine("Aug 3, 2025 · 57,600 mi", "Tire rotation · Main Street Auto"),
            ),
            fills = emptyList(),
            differs = listOf(PlanLine("May 15, 2025 · 55,100 mi", "Oil & filter change, Tire rotation · Main Street Auto · $89.95", "Engine oil & filter, Tire rotation · Main Street Auto · $89.95")),
            same = 0,
            caution = AppText.res.getString(R.string.import_caution_history),
            moves = mapOf(ImportMode.ADD_NEW to listOf("Oil & filter change — next due 70,900 mi")),
            doubts = listOf(DoubtLine(LocalDate.of(2025, 11, 2) to 95_800, "Nov 2, 2025 · 95,800 mi", "35,000 mi above the reading before it", included = false)),
        ),
    ) { _, _ -> }

    /** Made-up bulletins: invented numbers, summaries and links, one of each kind and one found by its wording. */
    val bulletins = BulletinsState(
        "2019 Blue truck",
        VehicleBulletins(
            LocalDate.of(2026, 10, 24),
            listOf(
                Bulletin(
                    "90000003", "ZZ-26-01", LocalDate.of(2026, 3, 2), BulletinKind.WARRANTY_EXTENSION, byWording = false,
                    summary = "A made-up warranty extension covering the made-up water pump for 10 years or 150,000 miles.",
                    products = listOf(BulletinProduct("4RUNNER", listOf(2017, 2018, 2019, 2021))), components = listOf("ENGINE"),
                    pdf = "https://static.nhtsa.gov/odi/tsbs/2026/MC-90000003-0001.pdf",
                ),
                Bulletin(
                    "90000002", "ZZ-25-07", LocalDate.of(2025, 8, 14), BulletinKind.OVER_THE_AIR, byWording = false,
                    summary = "A made-up software update for the made-up infotainment unit, sent over the air.",
                    products = listOf(BulletinProduct("4RUNNER", listOf(2019))), components = listOf("ELECTRICAL"), pdf = null,
                ),
                Bulletin(
                    "90000001", "ZZ1", LocalDate.of(2018, 5, 9), BulletinKind.SERVICE_CAMPAIGN, byWording = true,
                    summary = "Technical instructions for made-up limited service campaign ZZ1: replace the made-up bracket.",
                    products = listOf(BulletinProduct("4RUNNER", listOf(2018, 2019))), components = listOf("STRUCTURE"),
                    pdf = "https://static.nhtsa.gov/odi/tsbs/2018/MC-90000001-9999.pdf",
                ),
            ),
        ),
    )

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

    /**
     * A made-up flex-fuel schedule, to draw "Runs mostly on E85": the oil on its E85 interval, a job only for
     * severe use, and two ordinary ones. No maker is named.
     */
    val flexSchedule: ScheduleState = run {
        val items = listOf(
            DraftItem(key = "1", name = "Replace engine oil and filter", normal = Interval(5_000, 6), severe = null, own = true, service = "oil", e85 = Interval(2_500, 3), useE85 = true),
            DraftItem(key = "2", name = "Rotate tires", normal = Interval(5_000, 6), severe = null, own = true, service = "rotation"),
            DraftItem(key = "3", name = "Replace engine air filter", normal = Interval(30_000, 36), severe = null, own = true, service = "engine-air"),
            DraftItem(key = "4", name = "Replace rear differential oil · towing or heavy loads", normal = Interval(30_000, 36), severe = null, own = true, service = "rear-diff", severeOnly = true),
        )
        ScheduleState(
            vehicle = truck,
            items = items,
            templateKeys = items.associate { it.key to null },
            logs = emptyMap(),
            defaultRule = DueRule.WHICHEVER_FIRST,
            missing = emptyList(),
            odometer = truckOdometer,
            notLogged = 0,
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
        return LogServiceForm(editing = false, today = { TODAY }).apply {
            data = LogServiceData(
                vehicleName = "2019 Blue truck",
                categories = categories,
                due = listOf(DueSuggestion(oil.id, oil.label, overdue = true)),
                log = VehicleLog(
                    vehicle = truck, items = emptyList(), records = truckRecords, swap = null, entries = truckEntries, updates = truckReadings,
                    tracked = tracked, lead = settings.leadFor(unit), defaultRule = settings.defaultRule, answering = ServiceLinks.answering(refs),
                ),
                readings = ServiceLog.readings(truckEntries, truckReadings),
                lastOdometer = truckOdometer,
                specs = truck.whatItTakes(),
                original = null,
            )
            categoryId = categories.first { c -> c.options.any { it.id == oil.id } }.id
            toggle(oil.id)
        }
    }
}
