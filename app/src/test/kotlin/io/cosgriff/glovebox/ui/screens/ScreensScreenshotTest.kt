package io.cosgriff.glovebox.ui.screens

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
import io.cosgriff.glovebox.ui.reminders.RemindersContent
import io.cosgriff.glovebox.ui.schedule.MilestonesContent
import io.cosgriff.glovebox.ui.schedule.ScheduleContent
import io.cosgriff.glovebox.ui.history.SortCustomContent
import io.cosgriff.glovebox.ui.service.LineMatches
import io.cosgriff.glovebox.ui.service.LinePicker
import io.cosgriff.glovebox.ui.service.LineChoice
import io.cosgriff.glovebox.ui.service.LogServiceContent
import io.cosgriff.glovebox.ui.settings.CategoryRulesContent
import io.cosgriff.glovebox.ui.settings.SettingsContent
import io.cosgriff.glovebox.ui.torque.TorqueSpecsContent
import io.cosgriff.glovebox.ui.vehicle.MileageReadingsContent
import io.cosgriff.glovebox.ui.vehicle.TakesKnown
import io.cosgriff.glovebox.ui.vehicle.UpdateMileageSheet
import io.cosgriff.glovebox.ui.settings.CheckInDialog
import io.cosgriff.glovebox.ui.vehicle.VehicleDetailsContent
import io.cosgriff.glovebox.ui.vehicle.VehicleHomeContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
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

    @Test
    fun vehicleHome() = compose.shoot("vehicle_home", shade) {
        VehicleHomeContent(
            Fixtures.vehicleHome, OpenRecalls(1, "Fuel pump"),
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

    @Test
    fun checkIn() = compose.shootScreen("check_in", shade) {
        CheckInDialog(VehicleCheckIn.Default, default = 14, onSave = {}, onDismiss = {})
    }

    @Test
    fun vehicleDetails() = compose.shoot("vehicle_details", shade) {
        VehicleDetailsContent(
            Fixtures.vehicleDetails, photo = null, photoFailed = false, known = TakesKnown(), makerDocs = null,
            nickname = Fixtures.truck.nickname.orEmpty(), noticePrefs = NoticePrefs(), onNicknameChange = {},
            onBack = {}, onOpenRecalls = {}, onOpenSchedule = {}, onDelete = {}, onOpenTorqueSpecs = {}, onOpenMoreTakes = {},
            onOpenReadings = {}, onOpenImport = {}, onOpenDocuments = {}, onClearHistory = {},
            onEditTakes = {}, onFillKnown = {}, onEditPlate = {}, onEditCheckIn = {}, onAddVin = {}, onSetOffset = {}, onChangeUnit = {},
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

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun shades() = Shade.entries.map { arrayOf(it) }
    }
}
