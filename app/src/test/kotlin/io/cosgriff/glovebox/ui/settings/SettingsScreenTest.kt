package io.cosgriff.glovebox.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.LockDelay
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Settings: each row does what it says, and rows with nowhere to lead are left out. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class SettingsScreenTest {
    @get:Rule(order = 1) val compose = createComposeRule()

    @get:Rule(order = 0) val g = TestGraph()

    init {
        runBlocking { g.resetSettings() }
    }


    private fun saved(): Settings = runBlocking { g.settings.settings.first() }

    private fun row(id: Int) = compose.onNodeWithText(text(id)).performScrollTo()

    @Test
    fun `each choice is saved as the setting, and each row leads where it says`() {
        val (first, second) = runBlocking {
            g.vehicle(model = "4Runner") to g.vehicle(make = "Volkswagen", model = "GTI").also { g.db.catalogRules().put("Watr pump", "water-pump", LocalDate.of(2026, 1, 1)) }
        }
        val opened = mutableListOf<String>()
        var schedule: Long? = null
        val vm = SettingsViewModel(g.settings, g.db.vehicles(), g.db.catalogRules())
        compose.show {
            SettingsScreen(
                onOpenRules = { opened += "rules" },
                onOpenNotifications = { opened += "notifications" },
                onOpenBackup = { opened += "backup" },
                onOpenImport = { opened += "import" },
                onDeleteAll = { opened += "delete" },
                onOpenAbout = { opened += "about" },
                onOpenSchedule = { schedule = it },
                onOpenCategoryRules = { opened += "categories" },
                vm = vm,
            )
        }
        compose.waitForText(counted(R.plurals.settings_rules_note, 1, "1"))

        compose.onNodeWithText(text(R.string.theme_dark_title)).performClick()
        compose.waitFor { saved().theme == ThemeMode.DARK }

        row(R.string.details_units).performClick()
        compose.onNodeWithText(text(R.string.unit_kilometres_title)).performClick()
        compose.waitFor { saved().distanceUnit == DistanceUnit.KILOMETERS }

        row(R.string.settings_torque_units).performClick()
        compose.onNodeWithText(text(R.string.torque_unit_nm)).performClick()
        compose.waitFor { saved().torqueUnit == TorqueUnit.NEWTON_METER }

        row(R.string.settings_recall_checks).performClick()
        compose.onNodeWithText(text(R.string.recall_cadence_monthly)).performClick()
        compose.waitFor { saved().recallCadence == RecallCadence.MONTHLY }

        compose.onNode(isToggleable() and hasText(text(R.string.settings_hide_recents))).performScrollTo().performClick()
        compose.waitFor { !saved().hideInRecents }
        compose.onNode(isToggleable() and hasText(text(R.string.settings_scanner_details))).performScrollTo().performClick()
        compose.waitFor { saved().scannerDetails }

        // Two vehicles: the schedule row asks which.
        compose.onNodeWithText(counted(R.plurals.garage_vehicles, 2, "2")).performScrollTo().performClick()
        compose.onNodeWithText("GTI", substring = true).performClick()
        assertEquals(second, schedule)
        assertTrue(first != second)

        row(R.string.settings_reminder_rule).performClick()
        row(R.string.notifications_title).performClick()
        row(R.string.settings_backup).performClick()
        row(R.string.settings_import).performClick()
        row(R.string.settings_rules).performClick()
        row(R.string.settings_delete_all).performClick()
        row(R.string.about_title).performClick()
        assertEquals(listOf("rules", "notifications", "backup", "import", "categories", "delete", "about"), opened)
    }

    @Test
    fun `the app lock and when it locks are saved, and rows with nowhere to lead are left out`() {
        val vm = SettingsViewModel(g.settings, g.db.vehicles(), g.db.catalogRules())
        compose.show { SettingsScreen({}, {}, {}, {}, {}, {}, {}, {}, vm = vm) }
        // Robolectric's phone reports a screen lock, so the lock can be set as on a phone that has one.
        compose.waitForText(text(R.string.settings_lock_the_app))
        compose.onNodeWithText(LockDelay.ONE_MINUTE.label).performClick()
        compose.waitFor { saved().lockAfterSeconds == LockDelay.ONE_MINUTE.seconds }
        compose.onNode(isToggleable() and hasText(text(R.string.settings_biometrics))).performClick()
        compose.waitFor { !saved().biometricLock }
        compose.waitFor { compose.onAllNodes(hasText(text(R.string.settings_lock_the_app))).fetchSemanticsNodes().isEmpty() }
        // No vehicle: nothing to import into and no schedule to open; no rules, no rules row.
        assertTrue(compose.onAllNodes(hasText(text(R.string.settings_import))).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodes(hasText(text(R.string.details_schedule))).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodes(hasText(text(R.string.settings_rules))).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `with no screen lock the app lock can't be turned on, and says why`() {
        content(Settings(biometricLock = true), canLock = false)
        compose.onNodeWithText(text(R.string.settings_no_screen_lock)).assertIsDisplayed()
        compose.onNode(isToggleable() and hasText(text(R.string.settings_biometrics))).assertIsOff().assertIsNotEnabled()
        assertTrue(compose.onAllNodes(hasText(text(R.string.settings_lock_the_app))).fetchSemanticsNodes().isEmpty())
    }

    private fun content(
        s: Settings = Settings(),
        vehicles: List<ScheduleOwner> = listOf(ScheduleOwner(4, "2019 4Runner")),
        canLock: Boolean = true,
        onSchedule: (Long) -> Unit = {},
        onBiometric: (Boolean) -> Unit = {},
        onDelay: (LockDelay) -> Unit = {},
        onExport: (ExportFormat) -> Unit = {},
        onTheme: (ThemeMode) -> Unit = {},
    ) = compose.show {
        SettingsContent(
            s, vehicles, canLock, {}, {}, {}, {}, {}, {}, onSchedule,
            onSetTheme = onTheme, onSetBiometric = onBiometric, onSetLockDelay = onDelay, onSetHideInRecents = {}, onSetScannerDetails = {},
            onPick = {}, onExport = onExport,
        )
    }

    @Test
    fun `with a screen lock and the app lock on, when it locks can be chosen`() {
        var delay: LockDelay? = null
        var biometric: Boolean? = null
        content(Settings(biometricLock = true, lockAfterSeconds = 0), onDelay = { delay = it }, onBiometric = { biometric = it })
        compose.onNodeWithText(text(R.string.settings_lock_the_app)).assertIsDisplayed()
        compose.onNodeWithText(LockDelay.FIVE_MINUTES.label).performClick()
        assertEquals(LockDelay.FIVE_MINUTES, delay)
        compose.onNode(isToggleable() and hasText(text(R.string.settings_biometrics))).performClick()
        assertEquals(false, biometric)
    }

    @Test
    fun `with the app lock off there is no delay to choose`() {
        content(Settings(biometricLock = false))
        assertTrue(compose.onAllNodes(hasText(text(R.string.settings_lock_the_app))).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `one vehicle's schedule opens at once, and the export buttons say which format`() {
        var schedule: Long? = null
        val exports = mutableListOf<ExportFormat>()
        content(onSchedule = { schedule = it }, onExport = { exports += it })
        compose.onNodeWithText("2019 4Runner").performScrollTo().performClick()
        assertEquals(4L, schedule)
        compose.onNodeWithText(text(R.string.export_pdf)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.export_markdown)).performScrollTo().performClick()
        assertEquals(listOf(ExportFormat.PDF, ExportFormat.MARKDOWN), exports)
    }

    @Test
    fun `the backup row says when the last one was, and notifications say how many kinds are on`() {
        val notices = NoticePrefs().copy(serviceDue = true, overdue = false, recalls = false, mileage = false)
        content(Settings(lastBackupOn = LocalDate.of(2026, 2, 3), notices = notices, theme = ThemeMode.LIGHT))
        compose.onNodeWithText("2026", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(notificationsText(notices)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(themeText(followsPhone = false, dark = false)).assertIsDisplayed()
    }
}
