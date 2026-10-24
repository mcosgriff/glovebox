package io.cosgriff.glovebox.ui.settings

import android.net.Uri
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.backup.BackupFormat
import io.cosgriff.glovebox.data.backup.BackupWriter
import io.cosgriff.glovebox.domain.BackupRules
import io.cosgriff.glovebox.domain.PasswordStrength
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Making a backup: any password the user chooses, typed twice; the file is written encrypted and handed to
 * "save as"; the password is never kept; the last-backup date is recorded only once Android has written the file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class BackupViewModelTest {
    @get:Rule(order = 1) val main = MainDispatcherRule()

    @get:Rule(order = 0) val g = TestGraph()
    private val saver = ExportSaver(g.context, g.appScope)
    private val writer = BackupWriter(g.db, g.crypto, g.settings)

    /** What the activity's "save as" launcher was asked for. */
    private val asked = mutableListOf<Pair<String, String>>()
    private val launcher: (String, String) -> Unit = { mime, name -> asked += mime to name }

    @Before fun reset() = runBlocking { g.resetSettings() }


    private suspend fun vm(): BackupViewModel = BackupViewModel(g.settings, writer, saver).also { vm -> eventually { vm.contents != null } }

    @Test
    fun `nothing to back up until there is a vehicle`() = runTest {
        val vm = vm()
        assertEquals(0, vm.contents!!.vehicles)
        vm.password = "correct horse"
        vm.confirm = "correct horse"
        assertFalse(vm.canCreate)
    }

    @Test
    fun `a backup needs a password typed the same twice, however weak`() = runTest {
        g.vehicle()
        val vm = vm()
        assertEquals(1, vm.contents!!.vehicles)
        assertFalse("an empty password protects nothing", vm.canCreate)

        vm.password = "a"
        assertEquals(PasswordStrength.WEAK, vm.strength)
        assertFalse(vm.matches)
        assertFalse(vm.canCreate)

        vm.confirm = "a"
        assertTrue(vm.matches)
        assertTrue("a weak password is advice, not a gate", vm.canCreate)
        assertEquals(BackupRules.strength("a"), vm.strength)
    }

    @Test
    fun `the backup is written, offered to save as, and dated only once it has been saved`() = runTest {
        g.vehicle()
        saver.attach(launcher)
        val vm = vm()
        vm.password = "correct horse battery"
        vm.confirm = "correct horse battery"
        vm.create()
        eventually { asked.isNotEmpty() }

        val today = LocalDate.now()
        assertEquals(listOf(BackupFormat.MIME_TYPE to BackupRules.fileName(today, BackupFormat.FILE_EXTENSION)), asked)
        assertEquals("the password is let go once the file is written", "", vm.password)
        assertEquals("", vm.confirm)
        assertNull(vm.progress)
        assertFalse(vm.failed)
        assertNull("not saved yet, so not dated", g.settings.settings.first().lastBackupOn)

        val destination = File(g.context.filesDir, "saved.glovebox")
        saver.onDestination(Uri.fromFile(destination))
        assertEquals(AppText.res.getString(R.string.backup_saved), saver.results.awaitValue { true })
        assertEquals(today, g.settings.settings.first().lastBackupOn)
        assertTrue(destination.length() > 0)
        val text = destination.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse("the password is nowhere in the file", text.contains("correct horse battery"))
        assertFalse("nor is anything in the clear", text.contains("4Runner"))
    }

    @Test
    fun `backing out of save as records no backup`() = runTest {
        g.vehicle()
        saver.attach(launcher)
        val vm = vm()
        vm.password = "pw"
        vm.confirm = "pw"
        vm.create()
        eventually { asked.isNotEmpty() }
        saver.onDestination(null)
        // Nothing is said and nothing is dated.
        eventually { File(g.context.cacheDir, "export").listFiles().orEmpty().isEmpty() }
        assertNull(g.settings.settings.first().lastBackupOn)
    }

    @Test
    fun `with no save as to hand the file to, it is deleted and nothing is dated`() = runTest {
        g.vehicle()
        val vm = vm()
        vm.password = "pw"
        vm.confirm = "pw"
        vm.create()
        eventually { vm.progress == null && vm.password.isEmpty() }
        assertTrue(File(g.context.cacheDir, "export").listFiles().orEmpty().isEmpty())
        assertNull(g.settings.settings.first().lastBackupOn)
    }

    @Test
    fun `create does nothing while it can't`() = runTest {
        saver.attach(launcher)
        val vm = vm()
        vm.create()
        assertNull(vm.progress)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun `the reminder switch is the setting`() = runTest {
        val vm = vm()
        assertTrue(vm.settings.awaitValue { it != null }!!.backupReminder)
        vm.setReminder(false)
        assertFalse(vm.settings.awaitValue { it?.backupReminder == false }!!.backupReminder)
    }
}
