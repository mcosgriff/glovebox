package io.cosgriff.glovebox.widget

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.string
import io.cosgriff.glovebox.ui.garage.GarageOverview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class WidgetViewTest {
    private val graph get() = ShellTestApp.graph

    @After fun close() = graph.close()

    private fun view() = runBlocking { WidgetView.of(graph.get<GarageOverview>().garage.first()) }

    @Test
    fun `an empty garage has no rows`() {
        assertEquals(WidgetView(emptyList(), more = 0), view())
    }

    @Test
    fun `a row names the vehicle by year, make and model, with its nickname first`() {
        graph.addVehicle(make = "Toyota", model = "Tacoma", year = 2019)
        graph.addVehicle(make = "Ford", model = "F-150", year = 2008, nickname = "Big Red")
        assertEquals(listOf("2019 Toyota Tacoma", string(R.string.widget_name_nicknamed, "Big Red", "2008 Ford F-150")), view().rows.map { it.name })
    }

    @Test
    fun `a vehicle with nothing to count from has no chip`() {
        graph.addVehicle()
        assertNull(view().rows.single().pill)
    }

    @Test
    fun `an overdue vehicle has the solid chip, and its second line is what comes after`() {
        val (id, _) = graph.overdueVehicle()
        val row = view().rows.single()
        assertEquals(id, row.vehicleId)
        assertEquals(PillKind.OVERDUE, row.pill?.kind)
        // The overdue job is the chip's; nothing else is tracked.
        assertEquals(string(R.string.garage_nothing_else), row.next)
    }

    @Test
    fun `the second line names the next job and how far off it is`() {
        val id = graph.addVehicle(reading = 61_000)
        graph.logService(id, graph.addItem(id, name = "Tire rotation", everyMiles = 7_500, everyMonths = null), LocalDate.now().minusDays(20), 60_000)
        val row = view().rows.single()
        assertTrue(row.next, row.next.startsWith(string(R.string.garage_next_in, "Tire rotation", "").trimEnd()))
    }

    @Test
    fun `a vehicle with a job to log is grey, and one up to date is teal`() {
        val toLog = graph.addVehicle(model = "Corolla")
        graph.addItem(toLog)
        val fresh = graph.addVehicle(model = "Camry")
        graph.logService(fresh, graph.addItem(fresh), LocalDate.now().minusDays(10), 60_990)
        val kinds = view().rows.associate { it.vehicleId to it.pill?.kind }
        assertEquals(PillKind.SOON, kinds[toLog])
        assertEquals(PillKind.OK, kinds[fresh])
    }

    @Test
    fun `past five vehicles the rest are counted, and sold ones aren't there at all`() {
        val ids = (1..7).map { graph.addVehicle(model = "Model $it") }
        runBlocking { graph.db.vehicles().setArchived(ids.last(), LocalDate.now()) }
        val view = view()
        assertEquals(ids.take(5), view.rows.map { it.vehicleId })
        assertEquals(1, view.more)
    }
}
