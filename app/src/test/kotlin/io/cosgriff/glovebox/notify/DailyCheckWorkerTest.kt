package io.cosgriff.glovebox.notify

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.data.settings.NoticeLogStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.NoticeRun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class DailyCheckWorkerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val graph get() = ShellTestApp.graph
    private val system = app.getSystemService(NotificationManager::class.java)
    private val today = LocalDate.now()

    @Before fun allow() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        graph.get<Notifier>().createChannels()
        // Only what each test turns on.
        graph.settings { setBackupReminder(false) }
    }

    @After fun close() = graph.close()

    private fun run(vararg runs: NoticeRun): ListenableWorker.Result = runBlocking {
        val worker = TestListenableWorkerBuilder<DailyCheckWorker>(app)
            .apply { if (runs.isNotEmpty()) setInputData(workDataOf(DailyCheckWorker.RUNS to runs.map { it.name }.toTypedArray())) }
            .build()
        worker.doWork()
    }

    private val posted: List<Notification> get() = shadowOf(system).allNotifications
    private fun titles() = posted.map { it.extras.getCharSequence(Notification.EXTRA_TITLE).toString() }
    private fun log() = runBlocking { graph.get<NoticeLogStore>().read() }

    @Test
    fun `an overdue service is nudged, and the nudge is logged so it isn't repeated tomorrow`() {
        val (_, item) = graph.overdueVehicle()
        assertEquals(ListenableWorker.Result.success(), run())
        assertTrue(titles().toString(), titles().contains(app.getString(R.string.notify_overdue_title, "2019 Tacoma")))
        assertEquals(today, log().overdueNudged[item])

        system.cancelAll()
        run()
        assertTrue(titles().none { it == app.getString(R.string.notify_overdue_title, "2019 Tacoma") })
    }

    @Test
    fun `the log is kept with notifications off, so turning them on later releases no backlog`() {
        val (vehicle, item) = graph.overdueVehicle()
        shadowOf(system).setNotificationsEnabled(false)
        run()
        assertTrue(posted.isEmpty())
        assertEquals(today, log().overdueNudged[item])
        // The reading is a month old, older than the two-week default: the check-in was "asked" too.
        assertEquals(today, log().mileageAsked[vehicle])

        shadowOf(system).setNotificationsEnabled(true)
        run()
        assertTrue(titles().toString(), posted.isEmpty())
    }

    @Test
    fun `a sold vehicle gets no reminders of any kind`() {
        val (vehicle, item) = graph.overdueVehicle()
        runBlocking { graph.db.vehicles().setArchived(vehicle, today.minusDays(3)) }
        run()
        assertTrue(titles().toString(), posted.isEmpty())
        assertNull(log().overdueNudged[item])
        assertNull(log().mileageAsked[vehicle])
    }

    @Test
    fun `the mileage check-in run asks only for a reading, and leaves the services' log alone`() {
        val (vehicle, item) = graph.overdueVehicle()
        graph.settings { setBackupReminder(true) }
        assertEquals(ListenableWorker.Result.success(), run(NoticeRun.MILEAGE))
        assertEquals(listOf(app.getString(R.string.notify_mileage_title, "2019 Tacoma")), titles())
        assertEquals(today, log().mileageAsked[vehicle])
        assertNull(log().overdueNudged[item])
    }

    @Test
    fun `the services run nudges overdue work but doesn't ask for a reading`() {
        val (vehicle, _) = graph.overdueVehicle()
        run(NoticeRun.SERVICES)
        assertEquals(listOf(app.getString(R.string.notify_overdue_title, "2019 Tacoma")), titles())
        assertNull(log().mileageAsked[vehicle])
    }

    @Test
    fun `a phone with data and no backup is reminded once, and the day is noted`() {
        graph.addVehicle()
        graph.settings { setBackupReminder(true); setHomeWidget(true) }
        run()
        assertEquals(listOf(app.getString(R.string.notify_backup_never)), titles())
        assertEquals(today, runBlocking { graph.get<SettingsRepository>().settings.first().backupRemindedOn })

        system.cancelAll()
        run()
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `a sold vehicle's history still counts as data worth backing up`() {
        val id = graph.addVehicle()
        runBlocking { graph.db.vehicles().setArchived(id, today) }
        graph.settings { setBackupReminder(true); setLastBackup(today.minusDays(90)) }
        run()
        assertEquals(listOf(app.getString(R.string.notify_backup_fresh)), titles())
    }

    @Test
    fun `with the backup reminder off, nothing is said about backups`() {
        graph.addVehicle()
        run()
        assertTrue(posted.isEmpty())
        assertNull(runBlocking { graph.get<SettingsRepository>().settings.first().backupRemindedOn })
    }
}
