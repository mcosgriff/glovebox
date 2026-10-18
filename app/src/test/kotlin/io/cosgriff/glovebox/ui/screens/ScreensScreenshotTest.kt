package io.cosgriff.glovebox.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.OpenRecalls
import io.cosgriff.glovebox.domain.WrittenFile
import io.cosgriff.glovebox.ui.garage.Garage
import io.cosgriff.glovebox.ui.garage.GarageContent
import io.cosgriff.glovebox.ui.history.HistoryContent
import io.cosgriff.glovebox.ui.history.RecordDetailContent
import io.cosgriff.glovebox.ui.reminders.RemindersContent
import io.cosgriff.glovebox.ui.schedule.MilestonesContent
import io.cosgriff.glovebox.ui.schedule.ScheduleContent
import io.cosgriff.glovebox.ui.service.LogServiceContent
import io.cosgriff.glovebox.ui.settings.SettingsContent
import io.cosgriff.glovebox.ui.torque.TorqueSpecsContent
import io.cosgriff.glovebox.ui.vehicle.TakesKnown
import io.cosgriff.glovebox.ui.vehicle.UpdateMileageSheet
import io.cosgriff.glovebox.ui.vehicle.VehicleDetailsContent
import io.cosgriff.glovebox.ui.vehicle.VehicleHomeContent
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
    fun history() = compose.shoot("history", shade) {
        HistoryContent(
            Fixtures.history, chosenFilter = null, fresh = null, archived = false,
            onOpenRecord = {}, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {},
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
    fun vehicleDetails() = compose.shoot("vehicle_details", shade) {
        VehicleDetailsContent(
            Fixtures.vehicleDetails, photo = null, photoFailed = false, known = TakesKnown(), makerDocs = null,
            nickname = Fixtures.truck.nickname.orEmpty(), onNicknameChange = {},
            onBack = {}, onOpenRecalls = {}, onOpenSchedule = {}, onDelete = {}, onOpenTorqueSpecs = {}, onOpenMoreTakes = {},
            onOpenReadings = {}, onOpenImport = {}, onOpenDocuments = {}, onClearHistory = {},
            onEditTakes = {}, onFillKnown = {}, onEditPlate = {}, onAddVin = {}, onSetOffset = {}, onChangeUnit = {},
            onSell = {}, onPickPhoto = {}, onRemovePhoto = {},
        )
    }

    @Test
    fun schedule() = compose.shoot("schedule", shade) {
        ScheduleContent(
            Fixtures.schedule, hasMilestoneTable = false,
            onBack = {}, onSetUp = {}, onEnterLastDone = {}, onOpenMilestones = {},
            onSetTracked = { _, _ -> }, onEdit = {}, onAddOwn = {}, onAddKnown = {}, onAddMissing = {},
            onChooseSevereUse = {}, onSetAllTracked = {}, onWriteFile = { WrittenFile(null, 0) }, onSaveFile = {},
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
