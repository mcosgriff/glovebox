package io.cosgriff.glovebox.notify

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.domain.RecallCadence
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The tests are offline (see ShellTestSupport): NHTSA never answers, as on a phone with no signal. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class RecallCheckWorkerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val graph get() = ShellTestApp.graph
    private val today = LocalDate.now()

    @After fun close() = graph.close()

    private fun run(): ListenableWorker.Result = runBlocking { TestListenableWorkerBuilder<RecallCheckWorker>(app).build().doWork() }
    private fun checkedOn(id: Long) = runBlocking { graph.db.vehicles().byId(id)?.recallsCheckedOn }

    @Test
    fun `a failed check is never announced, WorkManager is asked to try again`() {
        val id = graph.addVehicle()
        graph.settings { setRecallCadence(RecallCadence.WEEKLY) }
        assertEquals(ListenableWorker.Result.retry(), run())
        // The stored list is left as it was.
        assertNull(checkedOn(id))
    }

    @Test
    fun `a vehicle whose list is fresh isn't asked about`() {
        val id = graph.addVehicle()
        runBlocking { graph.db.vehicles().setRecallsChecked(id, today.minusDays(2)) }
        graph.settings { setRecallCadence(RecallCadence.WEEKLY) }
        assertEquals(ListenableWorker.Result.success(), run())
        assertEquals(today.minusDays(2), checkedOn(id))
    }

    @Test
    fun `sold vehicles, and ones NHTSA can't be asked about, are left out`() {
        val sold = graph.addVehicle()
        runBlocking { graph.db.vehicles().setArchived(sold, today) }
        graph.addVehicle(make = "", model = "")
        graph.settings { setRecallCadence(RecallCadence.WEEKLY) }
        assertEquals(ListenableWorker.Result.success(), run())
    }

    @Test
    fun `with checks set to never, nothing is asked`() {
        graph.addVehicle()
        graph.settings { setRecallCadence(RecallCadence.NEVER) }
        assertEquals(ListenableWorker.Result.success(), run())
    }
}
