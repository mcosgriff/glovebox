package io.cosgriff.glovebox.notify

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.RecallCadence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class NoticeSchedulerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scheduler = NoticeScheduler(app)
    private lateinit var work: WorkManager

    @Before fun init() {
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        work = WorkManager.getInstance(app)
    }

    @After fun close() = ShellTestApp.graph.close()

    /** The job under [name] that is waiting to run, if any. */
    private fun waiting(name: String): WorkInfo? =
        work.getWorkInfosForUniqueWork(name).get().singleOrNull { it.state == WorkInfo.State.ENQUEUED }


    @Test
    fun `one daily job runs everything when the mileage check-in has no time of its own`() {
        scheduler.sync(NoticePrefs(hour = 9), RecallCadence.WEEKLY)
        val daily = checkNotNull(waiting("daily-check"))
        assertEquals(TimeUnit.DAYS.toMillis(1), daily.periodicityInfo?.repeatIntervalMillis)
        assertTrue(daily.initialDelayMillis <= TimeUnit.DAYS.toMillis(1))
        assertEquals(null, waiting("mileage-check-in"))
    }

    @Test
    fun `a mileage check-in at a time of its own is a second job, running only that part`() {
        scheduler.sync(NoticePrefs(hour = 8, mileageHour = 18, mileageMinute = 30), RecallCadence.NEVER)
        checkNotNull(waiting("daily-check"))
        checkNotNull(waiting("mileage-check-in"))

        // Back to one time: the second job goes.
        scheduler.sync(NoticePrefs(hour = 8), RecallCadence.NEVER)
        assertEquals(null, waiting("mileage-check-in"))
    }

    @Test
    fun `with every daily notice off, neither daily job is kept`() {
        scheduler.sync(NoticePrefs(mileageHour = 18), RecallCadence.NEVER)
        scheduler.sync(NoticePrefs(serviceDue = false, overdue = false, mileage = false, mileageHour = 18), RecallCadence.NEVER)
        assertEquals(null, waiting("daily-check"))
        assertEquals(null, waiting("mileage-check-in"))
    }

    @Test
    fun `the recall check runs daily on a network, and stops when set to never`() {
        scheduler.sync(NoticePrefs(), RecallCadence.MONTHLY)
        val recalls = checkNotNull(waiting("recall-check"))
        assertEquals(NetworkType.CONNECTED, recalls.constraints.requiredNetworkType)
        assertEquals(TimeUnit.DAYS.toMillis(1), recalls.periodicityInfo?.repeatIntervalMillis)

        scheduler.sync(NoticePrefs(), RecallCadence.NEVER)
        assertEquals(null, waiting("recall-check"))
    }

    @Test
    fun `syncing again keeps one job under each name`() {
        repeat(3) { scheduler.sync(NoticePrefs(hour = 7 + it), RecallCadence.WEEKLY) }
        assertEquals(1, work.getWorkInfosForUniqueWork("daily-check").get().count { it.state == WorkInfo.State.ENQUEUED })
        assertEquals(1, work.getWorkInfosForUniqueWork("recall-check").get().count { it.state == WorkInfo.State.ENQUEUED })
    }
}
