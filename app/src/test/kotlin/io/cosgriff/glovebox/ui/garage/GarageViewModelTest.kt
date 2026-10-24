package io.cosgriff.glovebox.ui.garage

import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.Snoozing
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class GarageViewModelTest {
    @get:Rule val main = UnconfinedMainRule()

    private val db = testDatabase()
    private val settings = testSettings()
    private val nhtsa = FakeNhtsa()
    private val today = LocalDate.now()

    @After fun close() = db.close()

    private fun overview() = GarageOverview(VehicleLogs(db.vehicles(), db.schedule(), db.service(), db.odometer(), settings), db.recalls())

    private fun viewModel() = GarageViewModel(overview(), nhtsa.repository(db, settings), db.vehicles())

    private suspend fun add(v: VehicleEntity, reading: Int? = null) = db.vehicles().add(v.copy(id = 0), firstReading = reading)

    @Test
    fun `a sold vehicle leaves the garage and is only counted in the archive`() = runBlocking {
        val truck = add(Fixtures.truck.copy(recallsCheckedOn = today))
        val hatch = add(Fixtures.hatch.copy(recallsCheckedOn = today))
        db.vehicles().setArchived(hatch, today)

        val vm = viewModel()
        val garage = vm.garage.awaitMatching { it != null }!!
        assertEquals(listOf(truck), garage.vehicles.map { it.vehicle.id })
        assertEquals(1, vm.archived.awaitMatching { it == 1 })
    }

    @Test
    fun `reminders are worked out from the log, with the oil change picked out for snoozing`() = runBlocking {
        val truck = add(Fixtures.truck.copy(recallsCheckedOn = today))
        db.schedule().insertAll(
            listOf(
                ScheduleItemEntity(vehicleId = truck, name = "Oil & filter change", templateKey = Snoozing.OIL_TEMPLATE_KEY, intervalMiles = 5_000, intervalMonths = 6),
                ScheduleItemEntity(vehicleId = truck, name = "Tire rotation", templateKey = "rotation", intervalMiles = 5_000, intervalMonths = 6),
            ),
        )
        val oil = db.schedule().observeTracked(truck).awaitMatching { it.size == 2 }.first { it.templateKey == Snoozing.OIL_TEMPLATE_KEY }
        // The oil was changed 7,000 miles ago: overdue. The rotation was never logged.
        db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = truck, date = today.minusMonths(2), odometer = 50_000, diy = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = oil.id, label = oil.name)),
        )
        db.odometer().insert(OdometerReadingEntity(vehicleId = truck, date = today, odometer = 57_000))

        val garage = overview().garage.awaitMatching { g -> g.vehicles.singleOrNull()?.odometer == 57_000 }
        val g = garage.vehicles.single()
        assertEquals(oil.id, g.oil?.item?.id)
        assertEquals(DueState.OVERDUE, g.oil?.status?.state)
        assertEquals(1, garage.overdue)
        assertEquals(1, g.comingUp.notLogged.size)
        // One vehicle: nothing to look across.
        assertEquals(emptyList<Any>(), garage.urgent)
    }

    @Test
    fun `"No open recalls" only once NHTSA has answered for that vehicle`() = runBlocking {
        val checked = add(Fixtures.truck.copy(recallsCheckedOn = today))
        add(Fixtures.hatch.copy(recallsCheckedOn = null))
        db.recalls().insertAll(
            listOf(
                RecallEntity(vehicleId = checked, campaign = "26V100000", component = "FUEL SYSTEM", summary = "Made up.", firstSeenOn = today),
                RecallEntity(vehicleId = checked, campaign = "26V200000", component = "AIR BAGS", summary = "Made up.", firstSeenOn = today, completedOn = today),
                RecallEntity(vehicleId = checked, campaign = "26V300000", component = "LIGHTING", summary = "Made up.", firstSeenOn = today, archivedOn = today),
            ),
        )

        val garage = overview().garage.awaitMatching { g -> g.vehicles.size == 2 && g.vehicles.any { it.recalls != null } }
        val byId = garage.vehicles.associateBy { it.vehicle.id }
        // Done and "doesn't apply" aren't open.
        assertEquals(1, byId.getValue(checked).recalls?.count)
        assertNull(garage.vehicles.first { it.vehicle.id != checked }.recalls)
        assertEquals(1, garage.openRecalls)
    }

    @Test
    fun `opening the garage re-checks a vehicle whose recalls were never fetched, sending only year, make and model`() = runBlocking {
        val hatch = add(Fixtures.hatch.copy(recallsCheckedOn = null))
        nhtsa.recalls = listOf(mapOf("NHTSACampaignNumber" to "26V400000", "Component" to "STEERING", "Summary" to "Made up."))

        viewModel()
        val stored = db.recalls().observe(hatch).awaitMatching { it.isNotEmpty() }
        assertEquals(listOf("26V400000"), stored.map { it.campaign })
        assertEquals(today, db.vehicles().observe(hatch).awaitMatching { it?.recallsCheckedOn != null }?.recallsCheckedOn)

        val asked = nhtsa.asked.single()
        assertEquals(mapOf("make" to "Volkswagen", "model" to "GTI", "modelYear" to "2013"), asked.parameters.names().associateWith { asked.parameters[it] })
        assertTrue(Fixtures.hatch.vin!! !in asked.toString())
    }
}
