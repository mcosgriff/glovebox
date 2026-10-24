package io.cosgriff.glovebox.ui.settings

import android.net.Uri
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.backup.BackupRestorer
import io.cosgriff.glovebox.data.backup.BackupSummary
import io.cosgriff.glovebox.data.backup.BackupWriter
import io.cosgriff.glovebox.data.settings.NoticeLogStore
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
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
 * Restoring a backup: the password is proved and the contents read before anything changes, every failure is
 * said in words and changes nothing, and a restore replaces everything in one go.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class RestoreViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val g = TestGraph()
    private val picker = RestorePicker(g.context, g.appScope)
    private val restorer = BackupRestorer(g.db, g.crypto, g.settings, NoticeLogStore(g.context))
    private val res get() = AppText.res
    private val day = LocalDate.of(2026, 3, 14)

    @Before fun reset() = runBlocking { g.resetSettings() }

    @After fun close() = g.close()

    /** A backup of what is on the phone now, made with [password], as a file the user could pick. */
    private suspend fun backup(password: String): File {
        val file = File(g.context.filesDir, "made-${System.nanoTime()}.glovebox")
        file.outputStream().use { BackupWriter(g.db, g.crypto, g.settings).write(it, password.toCharArray(), day) }
        return file
    }

    private suspend fun picked(file: File) {
        picker.onPicked(Uri.fromFile(file))
        picker.picked.awaitValue { it != null }
    }

    private suspend fun RestoreViewModel.opened() = eventually { !checking }

    @Test
    fun `the picked file is copied into the cache, still encrypted, and gone once closed`() = runTest {
        g.vehicle()
        val source = backup("pw")
        picked(source)
        val copy = picker.picked.value!!
        assertEquals(res.getString(R.string.restore_backup_file), copy.name)
        assertTrue(copy.file.readBytes().contentEquals(source.readBytes()))

        val vm = RestoreViewModel(picker, restorer)
        vm.password = "pw"
        vm.close()
        assertEquals("", vm.password)
        assertNull(picker.picked.value)
        assertFalse(copy.file.exists())
    }

    @Test
    fun `a file that can't be read is said so`() = runTest {
        picker.onPicked(Uri.fromFile(File(g.context.filesDir, "missing.glovebox")))
        assertEquals(res.getString(R.string.document_problem_unreadable), picker.problem.awaitValue { it != null })
        assertNull(picker.picked.value)
        picker.onPicked(null)
        assertNull(picker.picked.value)
    }

    @Test
    fun `the right password opens the file and says what it holds, and that it replaces what is here`() = runTest {
        g.vehicle()
        picked(backup("correct horse"))
        val vm = RestoreViewModel(picker, restorer)

        vm.password = "wrong"
        vm.open()
        vm.opened()
        assertEquals(res.getString(R.string.restore_wrong_password), vm.error)
        assertNull(vm.summary)

        vm.password = "correct horse"
        vm.open()
        vm.opened()
        assertNull(vm.error)
        val summary = vm.summary!!
        assertEquals(1, summary.vehicles)
        assertEquals(day, summary.createdOn)
        assertTrue("there is a vehicle here for it to replace", vm.replaces)
    }

    @Test
    fun `something that isn't a backup is called that`() = runTest {
        picked(File(g.context.filesDir, "notes.txt").apply { writeText("Just some notes, nothing to do with Glovebox.") })
        val vm = RestoreViewModel(picker, restorer)
        vm.password = "pw"
        vm.open()
        vm.opened()
        assertEquals(res.getString(R.string.restore_not_a_backup), vm.error)
    }

    @Test
    fun `a backup cut short is never restored, and what is here stays`() = runTest {
        val kept = g.vehicle(model = "Kept")
        val whole = backup("pw").readBytes()
        picked(File(g.context.filesDir, "short.glovebox").apply { writeBytes(whole.copyOf(whole.size - 40)) })
        val vm = RestoreViewModel(picker, restorer)
        vm.password = "pw"
        var done: BackupSummary? = null
        vm.restore { done = it }
        eventually { vm.progress == null }
        assertNull(done)
        assertEquals(res.getString(R.string.restore_nothing_changed, res.getString(R.string.restore_incomplete)), vm.error)
        assertEquals(listOf(kept), g.db.vehicles().all().map { it.id })
    }

    @Test
    fun `restoring replaces everything with the backup and lets go of the file`() = runTest {
        g.vehicle(model = "In the backup")
        val file = backup("pw")
        g.vehicle(model = "Added since")
        picked(file)
        val vm = RestoreViewModel(picker, restorer)
        vm.password = "pw"
        vm.open()
        vm.opened()
        assertEquals(1, vm.summary!!.vehicles)

        var done: BackupSummary? = null
        vm.restore { done = it }
        eventually { done != null }
        assertEquals(1, done!!.vehicles)
        assertEquals(listOf("In the backup"), g.db.vehicles().all().map { it.model })
        assertNull(vm.progress)
        assertNull(vm.summary)
        assertEquals("", vm.password)
        assertNull(picker.picked.value)
    }

    @Test
    fun `nothing is opened or restored with no file picked`() = runTest {
        val vm = RestoreViewModel(picker, restorer)
        vm.open()
        assertFalse(vm.checking)
        var called = false
        vm.restore { called = true }
        assertNull(vm.progress)
        assertFalse(called)
    }

    @Test
    fun `picking asks the activity's launcher and clears an earlier problem`() = runTest {
        picker.onPicked(Uri.fromFile(File(g.context.filesDir, "missing.glovebox")))
        picker.problem.awaitValue { it != null }
        var asked = 0
        val launcher: () -> Unit = { asked++ }
        picker.attach(launcher)
        RestoreViewModel(picker, restorer).pick()
        assertEquals(1, asked)
        assertNull(picker.problem.value)
        picker.detach(launcher)
        picker.pick()
        assertEquals(1, asked)
    }
}
