package io.cosgriff.glovebox.ui.screens

import io.cosgriff.glovebox.ui.bulletins.BulletinsContent
import io.cosgriff.glovebox.ui.settings.ImportContent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.HistoryOrder
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.VehicleCheckIn
import io.cosgriff.glovebox.domain.WrittenFile
import io.cosgriff.glovebox.notify.ShortcutTask
import io.cosgriff.glovebox.ui.components.ChooseVehicleDialog
import io.cosgriff.glovebox.ui.components.asChoice
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageContent
import io.cosgriff.glovebox.ui.history.HistoryContent
import io.cosgriff.glovebox.ui.history.HistoryView
import io.cosgriff.glovebox.ui.history.RecordDetailContent
import io.cosgriff.glovebox.ui.recalls.MarkDoneSheet
import io.cosgriff.glovebox.ui.recalls.NameChoice
import io.cosgriff.glovebox.ui.recalls.RecallNamesSheet
import io.cosgriff.glovebox.ui.reminders.RemindersContent
import io.cosgriff.glovebox.ui.schedule.MilestonesContent
import io.cosgriff.glovebox.ui.schedule.ScheduleContent
import io.cosgriff.glovebox.ui.history.SortCustomContent
import io.cosgriff.glovebox.ui.service.LineMatches
import io.cosgriff.glovebox.ui.service.LinePicker
import io.cosgriff.glovebox.ui.service.LineChoice
import io.cosgriff.glovebox.ui.service.LogServiceContent
import io.cosgriff.glovebox.ui.service.WorkAndPrices
import io.cosgriff.glovebox.ui.service.RecommendedByShop
import io.cosgriff.glovebox.ui.service.DoneWithThese
import io.cosgriff.glovebox.ui.service.ShopRecommendedContent
import io.cosgriff.glovebox.domain.OpenRecommendations
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import io.cosgriff.glovebox.ui.service.InvoiceCard
import io.cosgriff.glovebox.ui.service.InvoicePages
import io.cosgriff.glovebox.ui.service.WhatWasRead
import io.cosgriff.glovebox.ui.service.CostCard
import io.cosgriff.glovebox.ui.settings.CategoryRulesContent
import io.cosgriff.glovebox.ui.settings.SettingsContent
import io.cosgriff.glovebox.ui.torque.TorqueSpecsContent
import io.cosgriff.glovebox.ui.vehicle.MileageReadingsContent
import io.cosgriff.glovebox.ui.vehicle.TakesKnown
import io.cosgriff.glovebox.ui.vehicle.UpdateMileageSheet
import io.cosgriff.glovebox.ui.settings.CheckInDialog
import io.cosgriff.glovebox.ui.vehicle.VehicleDetailsContent
import io.cosgriff.glovebox.ui.vehicle.VehicleHomeContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.ExportOptions
import io.cosgriff.glovebox.domain.ExportText
import io.cosgriff.glovebox.domain.Exports
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.components.OilIntervalSheet
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.ui.export.AppExportWords
import io.cosgriff.glovebox.ui.export.Exportable
import io.cosgriff.glovebox.ui.export.ExportPreview
import io.cosgriff.glovebox.ui.export.ExportSheetContent
import io.cosgriff.glovebox.ui.export.ReportFonts
import io.cosgriff.glovebox.ui.export.ReportRenderer
import io.cosgriff.glovebox.ui.export.SampleReport
import org.junit.Rule
import org.robolectric.RuntimeEnvironment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The main screens in light and dark (#233), drawn from [Fixtures] with no database or ViewModel, and
 * compared with the references in `src/test/screenshots`. `./gradlew :app:recordRoborazziDebug` writes
 * new references after a change meant to alter how a screen looks; look at every image it changes.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class ScreensScreenshotTest(private val shade: Shade) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun garage() = compose.shoot("garage", shade) {
        GarageContent(Fixtures.garage, archived = 1, onAddVehicle = {}, onOpenVehicle = {}, onSeeReminders = {}, onOpenArchive = {}, onRestore = {})
    }

    @Test
    fun emptyGarage() = compose.shoot("garage_empty", shade) {
        GarageContent(Garage(emptyList()), archived = 0, onAddVehicle = {}, onOpenVehicle = {}, onSeeReminders = {}, onOpenArchive = {}, onRestore = {})
    }

    /** The truck's open recall says not to drive it, the hatch's to park outside (#461). */
    @Test
    fun garageDoNotDrive() = compose.shoot("garage_do_not_drive", shade) {
        val (truck, hatch) = Fixtures.garage.vehicles
        GarageContent(
            Garage(listOf(truck.copy(recalls = OpenRecalls(1, "Fuel pump", doNotDrive = true)), hatch.copy(recalls = OpenRecalls(1, null, parkOutside = true)))),
            archived = 1, onAddVehicle = {}, onOpenVehicle = {}, onSeeReminders = {}, onOpenArchive = {}, onRestore = {},
        )
    }

    @Test
    fun vehicleHome() = compose.shoot("vehicle_home", shade) {
        VehicleHomeContent(
            Fixtures.vehicleHome, OpenRecalls(1, "Fuel pump"),
            onBack = {}, onLogService = {}, onLogItem = { _, _ -> }, onOpenRules = {}, onOpenRecalls = {}, onOpenDetails = {},
            onEnterLastDone = {}, onUpdateMileage = {},
        )
    }

    /** An open recall whose maker says not to drive it, or to park it outside (#461). */
    @Test
    fun vehicleHomeDoNotDrive() = compose.shoot("vehicle_home_do_not_drive", shade) {
        VehicleHomeContent(
            Fixtures.vehicleHome, OpenRecalls(2, null, doNotDrive = true, parkOutside = true),
            onBack = {}, onLogService = {}, onLogItem = { _, _ -> }, onOpenRules = {}, onOpenRecalls = {}, onOpenDetails = {},
            onEnterLastDone = {}, onUpdateMileage = {},
        )
    }

    @Test
    fun updateMileageSheet() = compose.shootScreen("update_mileage", shade) {
        UpdateMileageSheet(
            vehicleName = "2019 Blue truck",
            unit = DistanceUnit.MILES,
            preview = { miles -> MileageUpdate.preview(Fixtures.vehicleHome.inputs, miles, Fixtures.TODAY, DistanceUnit.MILES) },
            onSave = {},
            onDismiss = {},
            onReadDash = {},
            dashReading = null,
            onDashReadingUsed = {},
        )
    }

    // A launcher shortcut with several vehicles (#235); the third has no nickname.
    @Test
    fun markDoneVisit() = compose.shootScreen("mark_done_visit", shade) {
        // No reading to start from: with one, the field's label sits on its border, and the contrast check takes the
        // whole field for text in the border's colour (1.8:1). The label and figures themselves are well above 4.5:1.
        MarkDoneSheet(
            Fixtures.recall, odometer = null, unit = DistanceUnit.MILES, makeName = "Toyota",
            visitsOn = { Fixtures.markDoneVisits }, onConfirm = {}, onDismiss = {}, today = Fixtures.TODAY,
        )
    }

    @Test
    fun recallNames() = compose.shootScreen("recall_names", shade) {
        // Made-up names, as NHTSA would list them for a model.
        RecallNamesSheet(
            NameChoice.Ready(listOf("4RUNNER LIMITED MADE-UP", "4RUNNER TRAIL MADE-UP")), model = Fixtures.truck.model,
            chosen = listOf("4RUNNER TRAIL MADE-UP"), onSave = {}, onRetry = {}, onDismiss = {},
        )
    }

    @Test
    fun chooseVehicle() = compose.shootScreen("choose_vehicle", shade) {
        ChooseVehicleDialog(
            ShortcutTask.LOG_SERVICE,
            listOf(Fixtures.truck, Fixtures.hatch, Fixtures.truck.copy(id = 3, year = 2021, nickname = null)).map { it.asChoice() },
            onChoose = {},
            onDismiss = {},
        )
    }

    @Test
    fun reminders() = compose.shoot("reminders", shade) {
        RemindersContent(
            Fixtures.garage, Fixtures.TODAY, notificationsAllowed = true,
            onLogIt = { _, _ -> }, onAddVehicle = {}, onOpenRules = {}, onOpenNotifications = {}, onSnooze = {}, onEndSnooze = {},
        )
    }

    @Test
    fun logService() = compose.shoot("log_service", shade) {
        LogServiceContent(
            Fixtures.logService(), newPhotos = emptyList(), photoProblem = null,
            onDone = {}, onSave = {}, onDeleteInstead = {}, onTakePhoto = {}, onChoosePhotos = {}, onRemoveNewPhoto = {},
        )
    }

    /** Read an invoice (#484): what it filled, the VIN question and a figure that didn't read. */
    @Test
    fun logServiceInvoice() {
        val form = Fixtures.logServiceFromInvoice()
        compose.shoot("log_service_invoice", shade) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                InvoiceCard(form, "2019 Blue truck", onRead = {}, InvoicePages(), Fixtures.pageThumb)
                WorkAndPrices(form)
            }
        }
    }

    /** Photographing a paper invoice (#513): the page strip while a page is still being read. */
    @Test
    fun logServiceInvoicePages() {
        val form = Fixtures.logService()
        compose.shoot("log_service_invoice_pages", shade) {
            Column(Modifier.padding(20.dp)) { InvoiceCard(form, "2019 Blue truck", onRead = {}, InvoicePages(Fixtures.invoicePagesReading), Fixtures.pageThumb) }
        }
    }

    /** Every page read: the form can be filled from them. */
    @Test
    fun logServiceInvoicePagesRead() {
        val form = Fixtures.logService()
        compose.shoot("log_service_invoice_pages_read", shade) {
            Column(Modifier.padding(20.dp)) { InvoiceCard(form, "2019 Blue truck", onRead = {}, InvoicePages(Fixtures.invoicePagesRead), Fixtures.pageThumb) }
        }
    }

    @Test
    fun logServiceInvoiceCost() {
        val form = Fixtures.logServiceFromInvoice()
        compose.shoot("log_service_invoice_cost", shade) {
            Column(Modifier.padding(20.dp)) { CostCard(form) }
        }
    }

    @Test
    fun whatWasRead() = compose.shootScreen("invoice_what_was_read", shade) {
        WhatWasRead(Fixtures.readInvoice, onDismiss = {})
    }

    @Test
    fun logServicePrices() {
        // Made once, as below. Only the two price sections, each in a shot of its own: together they are taller
        // than the window.
        val form = Fixtures.logServicePriced()
        compose.shoot("log_service_prices", shade) {
            Column(Modifier.padding(20.dp)) { WorkAndPrices(form) }
        }
    }

    @Test
    fun logServiceCost() {
        val form = Fixtures.logServicePriced()
        compose.shoot("log_service_cost", shade) {
            Column(Modifier.padding(20.dp)) { CostCard(form) }
        }
    }

    /** Part rows open and closed, a figure filled in and one left blank (#502). */
    @Test
    fun logServicePartRows() {
        val form = Fixtures.logServiceItemized()
        compose.shoot("log_service_part_rows", shade) {
            Column(Modifier.padding(20.dp)) { WorkAndPrices(form) }
        }
    }

    /** The invoice's charges, and everything adding up to the total (#502). */
    @Test
    fun logServiceCharges() {
        val form = Fixtures.logServiceItemized()
        compose.shoot("log_service_charges", shade) {
            Column(Modifier.padding(20.dp)) { CostCard(form) }
        }
    }

    /** What the shop recommended, chosen row by row (#572, board RecommendedLog). */
    @Test
    fun logServiceRecommended() {
        val form = Fixtures.logServiceRecommended()
        compose.shoot("log_service_recommended", shade) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) { RecommendedByShop(form, onChangeLine = {}) }
        }
    }

    /** Ticked lines that look like the work for open recommendations: done with these? (#572, board RecommendedLogDone). */
    @Test
    fun logServiceDone() {
        val form = Fixtures.logServiceDone()
        compose.shoot("log_service_done", shade) {
            Column(Modifier.padding(20.dp)) { DoneWithThese(form) }
        }
    }

    /** The vehicle's kept recommendations (#572, board RecommendedList). */
    @Test
    fun shopRecommended() = compose.shoot("shop_recommended", shade) {
        ShopRecommendedContent(Fixtures.shopRecommended, onBack = {}, onClose = {})
    }

    /** Vehicle home's row for them (#572, board RecommendedHome). */
    @Test
    fun vehicleHomeRecommendations() = compose.shoot("vehicle_home_recommendations", shade) {
        VehicleHomeContent(
            Fixtures.vehicleHome, OpenRecalls(0, null),
            onBack = {}, onLogService = {}, onLogItem = { _, _ -> }, onOpenRules = {}, onOpenRecalls = {}, onOpenDetails = {},
            onEnterLastDone = {}, onUpdateMileage = {}, recommendations = OpenRecommendations(5, red = 2, amber = 1),
        )
    }

    @Test
    fun logServiceAddPrices() {
        val form = Fixtures.logService()
        compose.shoot("log_service_add_prices", shade) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                WorkAndPrices(form)
                CostCard(form)
            }
        }
    }

    @Test
    fun customItemMatches() {
        // Made once: building the form writes its state, which inside a composition would compose again for ever.
        val categories = Fixtures.logService().data!!.categories
        val lines = categories.flatMap { c -> c.options.map { LineChoice(it, c) } }
        compose.shoot("custom_item_matches", shade) {
            // A made-up rule files this wording under the radiator, so that line comes first, said to be the user's.
            Column(Modifier.padding(16.dp)) { LineMatches("Coolant hoses replaced", lines, onPick = {}, rules = listOf(CatalogRule("Coolant hoses replaced", "radiator"))) }
        }
    }

    @Test
    fun fileUnderLine() {
        // Made once, as above. Two categories, so no row is cut off at the foot of the window (the check would flag it).
        val categories = Fixtures.logService().data!!.categories.filter { it.id in setOf("fluids", "cooling") }
        val lines = categories.flatMap { c -> c.options.map { LineChoice(it, c) } }
        compose.shoot("file_under_line", shade) {
            LinePicker("Water pump gasket replaced", lines, onPick = {}, Modifier.padding(16.dp))
        }
    }

    @Test
    fun categoryRules() = compose.shoot("category_rules", shade) {
        CategoryRulesContent(Fixtures.categoryRules, onSetLine = { _, _ -> }, onDelete = {}, onBack = {})
    }

    @Test
    fun history() = compose.shoot("history", shade) {
        HistoryContent(
            Fixtures.history, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            onSortCustom = {}, view = HistoryView.LIST,
        )
    }

    /** Inside Oil & engine: the categories give way to a back arrow, the category and its jobs, the oil change chosen. */
    @Test
    fun historyJob() = compose.shoot("history_job", shade) {
        HistoryContent(
            Fixtures.historyJobs, chosenFilter = "engine", order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            chosenJob = "oil", view = HistoryView.LIST,
        )
    }

    /** Well down a long history: the filters have scrolled away and the Top button is there to bring them back. */
    @Test
    fun historyTop() = compose.shoot("history_top", shade) {
        HistoryContent(
            Fixtures.longHistory, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            view = HistoryView.LIST, listState = rememberLazyListState(initialFirstVisibleItemIndex = 12),
        )
    }

    /** 6e · History — timeline view: the next reminder, the records under their years with the time between, and the key. */
    @Test
    fun historyTimeline() = compose.shoot("history_timeline", shade) {
        HistoryContent(
            Fixtures.historyTimeline, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            onSortCustom = {}, view = HistoryView.TIMELINE, onLogIt = { _, _ -> },
        )
    }

    /** A visit on the timeline is one entry, with each of its records. */
    @Test
    fun historyTimelineVisit() = compose.shoot("history_timeline_visit", shade) {
        HistoryContent(
            Fixtures.historyWithVisit, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            view = HistoryView.TIMELINE,
        )
    }

    @Test
    fun sortCustom() = compose.shoot("sort_custom", shade) {
        SortCustomContent(
            Fixtures.customGroups, chosen = emptyMap(), remember = emptyMap(), preview = Fixtures.sortPreview,
            onChoose = { _, _ -> }, onRemember = { _, _ -> }, onSave = {}, onBack = {},
        )
    }

    /** A search by a line's name: the spending line goes, since it would count only what matched. */
    @Test
    fun historySearch() = compose.shoot("history_search", shade) {
        HistoryContent(
            Fixtures.history, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            search = "rotation", view = HistoryView.LIST,
        )
    }

    @Test
    fun historyNoMatch() = compose.shoot("history_no_match", shade) {
        HistoryContent(
            Fixtures.history, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            search = "serpentine", view = HistoryView.LIST,
        )
    }

    /** Records with line prices: labor and parts under the spending line, over the records that have them (#485). */
    @Test
    fun historyPriced() = compose.shoot("history_priced", shade) {
        HistoryContent(
            Fixtures.historyPriced, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            onSortCustom = {}, view = HistoryView.LIST,
        )
    }

    @Test
    fun historyVisit() = compose.shoot("history_visit", shade) {
        HistoryContent(
            Fixtures.historyWithVisit, chosenFilter = null, order = HistoryOrder(), fresh = null, archived = false, today = Fixtures.TODAY,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
            onSortCustom = {}, view = HistoryView.LIST,
        )
    }

    @Test
    fun importPreview() = compose.shoot("import_history", shade) {
        ImportContent(
            vehicles = listOf(Fixtures.history.vehicles.first()), vehicleId = Fixtures.history.vehicles.first().id, vehicleFixed = true,
            pickedName = "history.json", pickedPdf = false, problem = null, preview = Fixtures.importPreview, result = null,
            onBack = {}, onPick = {}, onChooseVehicle = {}, onInclude = { _, _ -> }, onImport = { _, _, _, _ -> },
            onOpenTorqueSpecs = {}, onOpenSchedule = {}, onEnterLastDone = {}, onOpenMilestones = {}, onOpenHistory = { _, _ -> },
        )
    }

    @Test
    fun record() = compose.shoot("record", shade) {
        RecordDetailContent(
            Fixtures.recordDetail, photoProblem = null,
            onBack = {}, onEdit = { _, _ -> }, onOpenPhoto = { _, _ -> }, onTakePhoto = {}, onChoosePhotos = {}, onDelete = {},
        )
    }

    @Test
    fun recordPriced() = compose.shoot("record_priced", shade) {
        RecordDetailContent(
            Fixtures.recordPriced, photoProblem = null,
            onBack = {}, onEdit = { _, _ -> }, onOpenPhoto = { _, _ -> }, onTakePhoto = {}, onChoosePhotos = {}, onDelete = {},
        )
    }

    @Test
    fun recordPartRows() = compose.shoot("record_part_rows", shade) {
        RecordDetailContent(
            Fixtures.recordItemized, photoProblem = null,
            onBack = {}, onEdit = { _, _ -> }, onOpenPhoto = { _, _ -> }, onTakePhoto = {}, onChoosePhotos = {}, onDelete = {},
        )
    }

    @Test
    fun recordInVisit() = compose.shoot("record_in_visit", shade) {
        RecordDetailContent(
            Fixtures.recordInVisit, photoProblem = null,
            onBack = {}, onEdit = { _, _ -> }, onOpenPhoto = { _, _ -> }, onTakePhoto = {}, onChoosePhotos = {}, onDelete = {},
        )
    }

    @Test
    fun recordVisitOffer() = compose.shoot("record_visit_offer", shade) {
        RecordDetailContent(
            Fixtures.recordOffer, photoProblem = null,
            onBack = {}, onEdit = { _, _ -> }, onOpenPhoto = { _, _ -> }, onTakePhoto = {}, onChoosePhotos = {}, onDelete = {},
        )
    }

    /** The owner's own oil change interval, not set yet: the schedule's 7,500 mi with Glovebox's 9 months beside it (#517). */
    @Test
    fun oilInterval() = compose.shootScreen("oil_interval", shade) {
        OilIntervalSheet(Fixtures.oilChange, own = null, unit = DistanceUnit.MILES, onSave = {}, onDismiss = {})
    }

    /** The owner's 5,000 mi with months left to follow it (6, Glovebox's), and Use the typical. */
    @Test
    fun oilIntervalYours() = compose.shootScreen("oil_interval_yours", shade) {
        OilIntervalSheet(Fixtures.oilChange, own = Interval(5_000, null), unit = DistanceUnit.MILES, onSave = {}, onDismiss = {})
    }

    @Test
    fun checkIn() = compose.shootScreen("check_in", shade) {
        CheckInDialog(VehicleCheckIn.Default, default = 14, onSave = {}, onDismiss = {})
    }

    @Test
    fun bulletins() = compose.shoot("bulletins", shade) {
        BulletinsContent(Fixtures.bulletins, onBack = {}, onOpenPdf = {})
    }

    @Test
    fun vehicleDetails() = compose.shoot("vehicle_details", shade) {
        VehicleDetailsContent(
            Fixtures.vehicleDetails, photo = null, photoFailed = false, known = TakesKnown(), makerDocs = null,
            nickname = Fixtures.truck.nickname.orEmpty(), noticePrefs = NoticePrefs(), onNicknameChange = {},
            onBack = {}, onOpenRecalls = {}, onOpenSchedule = {}, onDelete = {}, onOpenTorqueSpecs = {}, onOpenMoreTakes = {},
            onOpenReadings = {}, onOpenImport = {}, onOpenDocuments = {}, onClearHistory = {},
            onEditTakes = {}, onFillKnown = {}, onEditPlate = {}, onEditCheckIn = {}, onEditVin = {}, onLookUpVin = {}, onSetOffset = {}, onSetOil = {}, onChangeUnit = {},
            onSell = {}, onPickPhoto = {}, onRemovePhoto = {},
        )
    }

    @Test
    fun mileageReadings() = compose.shoot("mileage_readings", shade) {
        MileageReadingsContent(
            Fixtures.mileageReadings, Fixtures.TODAY, years = null, selected = "record:102",
            onBack = {}, onYears = {}, onSelect = {}, onOpen = {},
        )
    }

    /** The slider part way: the past two years of three. */
    @Test
    fun mileageReadingsTwoYears() = compose.shoot("mileage_readings_two_years", shade) {
        MileageReadingsContent(
            Fixtures.mileageReadings, Fixtures.TODAY, years = 2, selected = null,
            onBack = {}, onYears = {}, onSelect = {}, onOpen = {},
        )
    }

    /** A reading a week for six years: services and the odometer marked, the updates only a line. */
    @Test
    fun mileageReadingsWeekly() = compose.shoot("mileage_readings_weekly", shade) {
        MileageReadingsContent(
            Fixtures.weeklyReadings, Fixtures.TODAY, years = null, selected = null,
            onBack = {}, onYears = {}, onSelect = {}, onOpen = {},
        )
    }

    @Test
    fun schedule() = compose.shoot("schedule", shade) {
        ScheduleContent(
            Fixtures.schedule, hasMilestoneTable = false,
            onBack = {}, onSetUp = {}, onEnterLastDone = {}, onOpenMilestones = {},
            onSetTracked = { _, _ -> }, onEdit = {}, onAddOwn = {}, onAddKnown = {}, onAddMissing = {},
            onChooseSevereUse = {}, onChooseE85Use = {}, onSetAllTracked = {}, onWriteFile = { WrittenFile(null, 0) }, onSaveFile = {},
        )
    }

    @Test
    fun scheduleE85() = compose.shoot("schedule_e85", shade) {
        ScheduleContent(
            Fixtures.flexSchedule, hasMilestoneTable = false,
            onBack = {}, onSetUp = {}, onEnterLastDone = {}, onOpenMilestones = {},
            onSetTracked = { _, _ -> }, onEdit = {}, onAddOwn = {}, onAddKnown = {}, onAddMissing = {},
            onChooseSevereUse = {}, onChooseE85Use = {}, onSetAllTracked = {}, onWriteFile = { WrittenFile(null, 0) }, onSaveFile = {},
        )
    }

    @Test
    fun milestones() = compose.shoot("milestones", shade) {
        MilestonesContent(
            Fixtures.milestones, onBack = {}, onOpenRecord = {},
            onEdit = {}, onShowMore = {}, onAddMilestone = {}, onChangeRepeat = {}, onSaveFile = {}, onRemove = {}, onAddKnown = {},
        )
    }

    @Test
    fun milestonesContinued() = compose.shoot("milestones_continued", shade) {
        MilestonesContent(
            Fixtures.milestonesContinued, onBack = {}, onOpenRecord = {},
            onEdit = {}, onShowMore = {}, onAddMilestone = {}, onChangeRepeat = {}, onSaveFile = {}, onRemove = {}, onAddKnown = {},
        )
    }

    @Test
    fun settings() = compose.shoot("settings", shade) {
        SettingsContent(
            Fixtures.settings, Fixtures.settingsVehicles, canLock = true,
            onOpenRules = {}, onOpenNotifications = {}, onOpenBackup = {}, onOpenImport = {}, onDeleteAll = {}, onOpenAbout = {},
            onOpenSchedule = {}, onSetTheme = {}, onSetBiometric = {}, onSetLockDelay = {}, onSetHideInRecents = {},
            onSetScannerDetails = {}, onPick = {}, onExport = {},
        )
    }

    @Test
    fun torqueSpecs() = compose.shoot("torque_specs", shade) {
        TorqueSpecsContent(
            Fixtures.torque, onBack = {}, onSave = {}, onDelete = {}, onAddCommon = {}, onAddKnown = {},
            onWriteFile = { WrittenFile(null, 0) }, onSaveFile = {},
        )
    }

    @Test
    fun exportSheetPdf() = exportSheet("export_pdf", ExportFormat.PDF)

    @Test
    fun exportSheetMarkdown() = exportSheet("export_markdown", ExportFormat.MARKDOWN)

    /** The export sheet with its preview drawn from the made-up sample report. */
    private fun exportSheet(name: String, format: ExportFormat) {
        val options = ExportOptions(format = format)
        val vehicle = SampleReport.vehicle(Fixtures.TODAY)
        val doc = Exports.document(listOf(vehicle), options, Fixtures.TODAY)
        val renderer = ReportRenderer(ReportFonts(RuntimeEnvironment.getApplication()))
        val preview = when (format) {
            ExportFormat.PDF -> renderer.layout(doc).let { pages -> ExportPreview.Pdf(pages.take(2).mapIndexed { i, p -> renderer.bitmap(p, i + 1, pages.size, 270).asImageBitmap() }, pages.size) }
            ExportFormat.MARKDOWN -> ExportPreview.Markdown(ExportText(AppExportWords).markdown(doc).lines().take(14).joinToString("\n"))
        }
        compose.shootScreen(name, shade) {
            GloveboxSheet(onDismiss = {}) {
                ExportSheetContent(
                    all = listOf(Exportable(1, "Blue truck", archived = false, vehicle = vehicle)), vehicleId = 1, options = options, document = doc,
                    fileName = Exports.fileName("Blue truck", Fixtures.TODAY, format), working = false, preview = preview, today = Fixtures.TODAY,
                    onChange = {}, onChooseVehicle = {}, onPickDate = {}, onViewPage = {}, onExport = {},
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun shades() = Shade.entries.map { arrayOf(it) }
    }
}
