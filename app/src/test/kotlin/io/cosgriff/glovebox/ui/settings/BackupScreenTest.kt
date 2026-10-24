package io.cosgriff.glovebox.ui.settings

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.backup.BackupRestorer
import io.cosgriff.glovebox.data.backup.BackupSummary
import io.cosgriff.glovebox.data.backup.BackupWriter
import io.cosgriff.glovebox.data.settings.NoticeLogStore
import io.cosgriff.glovebox.domain.BackupRules
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** The backup screen and the restore sheet, driven as the user drives them over a real database. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class BackupScreenTest {
    @get:Rule val compose = createComposeRule()

    private val g = TestGraph()
    private val saver = ExportSaver(g.context, g.appScope)

    init {
        runBlocking { g.resetSettings() }
    }

    @After fun close() = g.close()

    private fun field(i: Int) = compose.onAllNodes(hasSetTextAction())[i]

    @Test
    fun `a backup is made with a password typed twice, and the screen says how strong it is`() {
        runBlocking { g.vehicle() }
        val asked = mutableListOf<String>()
        saver.attach { _, name -> asked += name }
        var restore = 0
        compose.show { BackupScreen(onBack = {}, onRestore = { restore++ }, vm = BackupViewModel(g.settings, BackupWriter(g.db, g.crypto, g.settings), saver)) }

        compose.waitForText(text(R.string.backup_last, text(R.string.backup_never)))
        compose.waitForText(backupContents(1, 0, 0))
        compose.onNodeWithText(text(R.string.backup_strength_hint)).assertIsDisplayed()
        val create = compose.onNodeWithText(text(R.string.backup_create))
        create.assertIsNotEnabled()

        field(0).performTextInput("a")
        compose.onNodeWithText(text(R.string.backup_strength_weak)).assertIsDisplayed()
        field(1).performTextInput("b")
        compose.onNodeWithText(text(R.string.backup_mismatch)).assertIsDisplayed()
        create.assertIsNotEnabled()

        // Shown in the clear on request, both fields at once.
        compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(text(R.string.backup_show_passwords)))[0].performClick()
        compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(text(R.string.backup_hide_passwords)))[0].assertIsDisplayed()

        field(0).performTextInput("bcdefghijklmnop")
        field(1).performTextReplacement("abcdefghijklmnop")
        compose.onNodeWithText(text(R.string.backup_cant_recover_store)).assertExists()
        create.performScrollTo().assertIsEnabled().performClick()
        compose.waitFor { asked.isNotEmpty() }
        assertEquals(listOf(BackupRules.fileName(LocalDate.now(), "glovebox")), asked)

        compose.onNodeWithText(text(R.string.backup_restore_from)).performScrollTo().performClick()
        assertEquals(1, restore)
    }

    @Test
    fun `with nothing to back up the screen says so, and the reminder switch is the setting`() {
        runBlocking { g.settings.setLastBackup(LocalDate.of(2026, 1, 2)) }
        compose.show { BackupScreen(onBack = {}, onRestore = {}, vm = BackupViewModel(g.settings, BackupWriter(g.db, g.crypto, g.settings), saver)) }
        compose.waitForText(text(R.string.backup_nothing))
        compose.waitForText("2026", substring = true)
        val reminder = compose.onNode(isToggleable() and androidx.compose.ui.test.hasText(text(R.string.backup_reminder), substring = true))
        reminder.performScrollTo().assertIsOn().performClick()
        compose.waitFor { runBlocking { !g.settings.settings.first().backupReminder } }
        reminder.assertIsOff()
    }

    @Test
    fun `restoring needs the password, then REPLACE typed when it would replace what is here`() {
        runBlocking { g.vehicle(model = "In the backup") }
        val file = File(g.context.filesDir, "made.glovebox")
        runBlocking { file.outputStream().use { BackupWriter(g.db, g.crypto, g.settings).write(it, "pw".toCharArray(), LocalDate.of(2026, 3, 14)) } }
        runBlocking { g.vehicle(model = "Added since") }
        val picker = RestorePicker(g.context, g.appScope)
        picker.onPicked(Uri.fromFile(file))
        compose.waitFor { picker.picked.value != null }

        var restored: BackupSummary? = null
        val vm = RestoreViewModel(picker, BackupRestorer(g.db, g.crypto, g.settings, NoticeLogStore(g.context)))
        compose.show { RestoreSheet(onRestored = { restored = it }, vm = vm) }
        compose.waitForText(text(R.string.restore_title))
        compose.onNodeWithText(text(R.string.restore_cant_recover)).assertIsDisplayed()
        val open = compose.onNodeWithText(text(R.string.restore_open))
        open.assertIsNotEnabled()

        field(0).performTextInput("wrong")
        compose.onNodeWithContentDescription(text(R.string.restore_show_password)).performClick()
        open.performClick()
        compose.waitForText(text(R.string.restore_wrong_password))

        vm.password = "pw"
        open.performClick()
        compose.waitForText(text(R.string.restore_replaces))
        val replace = compose.onNodeWithText(text(R.string.restore_replace))
        replace.assertIsNotEnabled()
        // The password field is fixed once the file is open; the one left to type in is the confirmation.
        field(0).performTextInput("REPLAC")
        replace.assertIsNotEnabled()
        field(0).performTextInput("E")
        replace.assertIsEnabled().performClick()
        compose.waitFor { restored != null }
        assertEquals(1, restored!!.vehicles)
        assertEquals(listOf("In the backup"), runBlocking { g.db.vehicles().all() }.map { it.model })
        assertNull(picker.picked.value)
    }

    @Test
    fun `onto an empty phone a restore needs no typed word, and Cancel lets go of the file`() {
        val file = File(g.context.filesDir, "empty.glovebox")
        runBlocking { file.outputStream().use { BackupWriter(g.db, g.crypto, g.settings).write(it, "pw".toCharArray(), LocalDate.of(2026, 3, 14)) } }
        val picker = RestorePicker(g.context, g.appScope)
        picker.onPicked(Uri.fromFile(file))
        compose.waitFor { picker.picked.value != null }
        val vm = RestoreViewModel(picker, BackupRestorer(g.db, g.crypto, g.settings, NoticeLogStore(g.context)))
        compose.show { RestoreSheet(onRestored = {}, vm = vm) }
        compose.waitForText(text(R.string.restore_open))
        field(0).performTextInput("pw")
        compose.onNodeWithText(text(R.string.restore_open)).performClick()
        compose.waitForText(text(R.string.restore))
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasText(text(R.string.restore_replaces))).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(text(R.string.restore)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.waitFor { picker.picked.value == null }
    }
}
