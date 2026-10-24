package io.cosgriff.glovebox.notify

import io.cosgriff.glovebox.ShellTestApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class DestinationViewModelTest {
    private val graph get() = ShellTestApp.graph
    private val holder = PendingDestination()
    private val model by lazy { DestinationViewModel(holder, graph.db.vehicles()) }

    @After fun close() = graph.close()

    private fun sell(id: Long) = runBlocking { graph.db.vehicles().setArchived(id, LocalDate.now()) }

    @Test
    fun `nothing waiting is nothing to do`() = runBlocking {
        assertNull(model.take())
    }

    @Test
    fun `a destination without a vehicle is handed over and cleared`() = runBlocking {
        for (to in listOf(Destination.Reminders, Destination.Backup, Destination.AddVehicle)) {
            holder.set(to)
            assertEquals(Arrival(to), model.take())
            assertNull(holder.destination.value)
        }
    }

    @Test
    fun `a destination for a vehicle in the garage is handed over`() = runBlocking {
        val id = graph.addVehicle()
        for (to in listOf(Destination.Recalls(id), Destination.UpdateMileage(id), Destination.VehicleHome(id))) {
            holder.set(to)
            assertEquals(Arrival(to), model.take())
        }
    }

    @Test
    fun `a destination for a vehicle that is gone or sold is dropped, and cleared all the same`() = runBlocking {
        val sold = graph.addVehicle(model = "GTI", make = "Volkswagen")
        sell(sold)
        for (to in listOf(Destination.Recalls(sold), Destination.UpdateMileage(404), Destination.VehicleHome(sold))) {
            holder.set(to)
            assertNull(model.take())
            assertNull(holder.destination.value)
        }
    }

    @Test
    fun `a shortcut comes with the vehicles it could be for, sold ones left out`() = runBlocking {
        val first = graph.addVehicle()
        val second = graph.addVehicle(make = "Honda", model = "Civic")
        sell(graph.addVehicle(make = "Volkswagen", model = "GTI"))
        val to = Destination.Shortcut(ShortcutTask.LOG_SERVICE)
        holder.set(to)
        assertEquals(Arrival(to, listOf(first, second)), model.take()?.let { it.copy(activeVehicleIds = it.activeVehicleIds.sorted()) })
        assertEquals(listOf(first, second), model.activeVehicles.first().map { it.id }.sorted())
    }

    @Test
    fun `the destination left waiting is what is handed over`() = runBlocking {
        holder.set(Destination.Backup)
        assertEquals(Destination.Backup, model.pending.value)
    }
}
