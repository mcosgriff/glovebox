package io.cosgriff.glovebox.ui.recalls

import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.nhtsa.toNotice
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.ui.RecallsRoute
import io.cosgriff.glovebox.ui.garage.FakeNhtsa
import io.cosgriff.glovebox.ui.garage.TestViewModels
import io.cosgriff.glovebox.ui.garage.UnconfinedMainRule
import io.cosgriff.glovebox.ui.garage.awaitMatching
import io.cosgriff.glovebox.ui.garage.recallRow
import io.cosgriff.glovebox.ui.garage.routeHandle
import io.cosgriff.glovebox.ui.garage.testDatabase
import io.cosgriff.glovebox.ui.garage.testSettings
import io.cosgriff.glovebox.ui.garage.waitFor
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.RecordActions
import io.cosgriff.glovebox.ui.service.recallServiceLine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Builds what a recalls screen needs over one in-memory database. Used by the screen tests too. */
internal class RecallsHarness(cadence: RecallCadence = RecallCadence.WEEKLY) {
    val db = testDatabase()
    val settings = testSettings(cadence = cadence)
    val nhtsa = FakeNhtsa()
    val today: LocalDate = LocalDate.now()
    private val viewModels = TestViewModels()

    /** Stops the ViewModels, then closes the database. */
    fun close() {
        viewModels.clear()
        db.close()
    }

    fun addTruck(checkedOn: LocalDate? = today, make: String = "Toyota", year: Int = 2019): Long = runBlocking {
        db.vehicles().add(Fixtures.truck.copy(id = 0, recallsCheckedOn = checkedOn, make = make, year = year), firstReading = null)
    }

    fun recall(vehicleId: Long, campaign: String, component: String = "FUEL SYSTEM", done: LocalDate? = null, archived: LocalDate? = null, consequence: String? = null) =
        RecallEntity(
            vehicleId = vehicleId, campaign = campaign, component = component, summary = "A made-up summary of $campaign.",
            consequence = consequence, remedy = "A made-up remedy.", reportedOn = today.minusYears(1), firstSeenOn = today,
            completedOn = done, archivedOn = archived,
        )

    fun insert(vararg recalls: RecallEntity) = runBlocking { db.recalls().insertAll(recalls.toList()) }

    fun viewModel(vehicleId: Long): RecallsViewModel {
        val context = ApplicationProvider.getApplicationContext<ScreenshotApp>()
        val photos = RecordPhotoStore(context, PhotoCrypto(context), db.service(), db.vehicles(), db.documents())
        return viewModels.make {
            RecallsViewModel(
                routeHandle(RecallsRoute(vehicleId)), db.vehicles(), db.recalls(), db.service(), db.odometer(),
                nhtsa.repository(db, settings), RecordActions(db.service(), db.recalls(), db.vehicles(), photos),
            )
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class RecallsViewModelTest {
    @get:Rule val main = UnconfinedMainRule()

    private val h = RecallsHarness()
    private val db = h.db

    @After fun close() = h.close()

    @Test
    fun `open recalls come first, and the odometer is the highest reading on record`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000", done = h.today), h.recall(truck, "26V200000"), h.recall(truck, "26V300000", archived = h.today))
        db.odometer().insert(OdometerReadingEntity(vehicleId = truck, date = h.today, odometer = 63_480))
        db.service().insertWithItems(ServiceRecordEntity(vehicleId = truck, date = h.today.minusDays(9), odometer = 63_000, diy = true), emptyList())

        val state = h.viewModel(truck).state.awaitMatching { it != null && it.recalls.size == 3 && it.odometer == 63_480 }!!
        assertEquals("26V200000", state.recalls.first().campaign)
        assertEquals(1, state.open)
        assertTrue(state.canCheck)
        assertEquals("Toyota", state.makeName)
    }

    @Test
    fun `a vehicle that no longer exists shows nothing`() = runBlocking {
        val vm = h.viewModel(404)
        waitFor("the stale check") { !vm.refreshing }
        assertNull(vm.state.awaitMatching { true })
    }

    @Test
    fun `a list a week old is re-checked on the way in, a fresh one isn't`() {
        val fresh = h.addTruck(checkedOn = h.today)
        h.viewModel(fresh)
        val stale = h.addTruck(checkedOn = h.today.minusDays(8))
        h.nhtsa.recalls = listOf(recallRow("26V400000"))
        val vm = h.viewModel(stale)
        waitFor("the check") { !vm.refreshing }
        assertEquals(1, h.nhtsa.asked.size)
        assertFalse(vm.failed)
        assertEquals(listOf("26V400000"), runBlocking { db.recalls().recallsFor(stale) }.map { it.campaign })
    }

    @Test
    fun `a check that doesn't get through says so and keeps the stored list`() {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        h.nhtsa.recalls = null
        val vm = h.viewModel(truck)
        // A Refresh while the check on the way in is still going is the same check.
        waitFor("the check on the way in") { !vm.refreshing }
        vm.refresh()
        waitFor("the check") { !vm.refreshing && vm.failed }
        assertEquals(listOf("26V100000"), runBlocking { db.recalls().recallsFor(truck) }.map { it.campaign })
    }

    @Test
    fun `marked done with an odometer, the repair goes in the service log, and marked open again it comes out`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        val vm = h.viewModel(truck)
        val recall = vm.state.awaitMatching { it?.recalls?.isNotEmpty() == true }!!.recalls.single()

        vm.markDone(recall, RecallCompletion(h.today.minusDays(2), 62_000, "Main Street Toyota"))
        val done = vm.state.awaitMatching { it?.recalls?.single()?.completedOn != null }!!.recalls.single()
        assertEquals(h.today.minusDays(2), done.completedOn)
        val record = db.service().record(done.serviceRecordId!!)!!
        assertEquals(62_000, record.record.odometer)
        assertEquals("Main Street Toyota", record.record.shopName)
        assertEquals(listOf(recallServiceLine(recall.toNotice())), record.items.map { it.label })

        vm.markOpen(done)
        val open = vm.state.awaitMatching { it?.recalls?.single()?.completedOn == null }!!.recalls.single()
        assertNull(open.serviceRecordId)
        assertNull(db.service().record(record.record.id))
    }

    @Test
    fun `marked done without a record, nothing is added to the log`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        val vm = h.viewModel(truck)
        val recall = vm.state.awaitMatching { it?.recalls?.isNotEmpty() == true }!!.recalls.single()
        vm.markDone(recall, RecallCompletion(h.today, null, null))
        val done = vm.state.awaitMatching { it?.recalls?.single()?.completedOn != null }!!.recalls.single()
        assertNull(done.serviceRecordId)
        assertEquals(emptyList<Any>(), vm.state.value!!.records)
    }

    @Test
    fun `a recall done at a visit joins it`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        val visitDay = h.today.minusDays(5)
        val oil = db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = truck, date = visitDay, odometer = 61_000, diy = false, shopName = "Main Street Toyota"),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Oil change")),
        )
        val vm = h.viewModel(truck)
        val state = vm.state.awaitMatching { it?.records?.isNotEmpty() == true && it.recalls.isNotEmpty() }!!
        val offered = state.visitsOn(visitDay).single()
        assertEquals(oil, offered.id)
        assertEquals("Oil change", offered.work)
        assertEquals(emptyList<Any>(), state.visitsOn(h.today))

        vm.markDone(state.recalls.single(), RecallCompletion(visitDay, 61_000, null, visitWith = oil))
        // The record and its visit link are written one after the other; wait for both.
        val done = vm.state.awaitMatching { s -> s?.recalls?.single()?.completedOn != null && s.records.size == 2 && s.records.all { it.record.visitId != null } }!!
        val visits = done.records.map { it.record.visitId }.toSet()
        assertEquals(1, visits.size)
        assertNotNull(visits.single())
    }

    @Test
    fun `archiving keeps the recall and moving it back reopens it`() = runBlocking {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        val vm = h.viewModel(truck)
        val recall = vm.state.awaitMatching { it?.recalls?.isNotEmpty() == true }!!.recalls.single()
        vm.archive(recall)
        val archived = vm.state.awaitMatching { it?.recalls?.single()?.archivedOn != null }!!
        assertEquals(0, archived.open)
        assertEquals(1, archived.recalls.size)
        vm.unarchive(archived.recalls.single())
        assertEquals(1, vm.state.awaitMatching { it?.recalls?.single()?.archivedOn == null }!!.open)
    }
}
