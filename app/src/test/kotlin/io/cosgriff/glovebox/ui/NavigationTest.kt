package io.cosgriff.glovebox.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import io.cosgriff.glovebox.ShellTestApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class NavigationTest {
    @get:Rule val compose = createComposeRule()

    @After fun close() = ShellTestApp.graph.close()

    class RouteReader(val handle: SavedStateHandle) : ViewModel()

    @Test
    fun `a screen's ViewModels are told the route it was opened with`() {
        lateinit var reader: RouteReader
        compose.setContent {
            ProvideRoute(VehicleHomeRoute(7, updateMileage = true)) { reader = viewModel() }
        }
        compose.waitForIdle()
        assertEquals(VehicleHomeRoute(7, updateMileage = true), reader.handle.route<VehicleHomeRoute>())
        assertEquals(VehicleHomeRoute(7, updateMileage = true), reader.handle.routeOrNull<VehicleHomeRoute>())
        // A ViewModel serving more than one route asks which.
        assertNull(reader.handle.routeOrNull<LogServiceRoute>())
        assertThrows(IllegalStateException::class.java) { reader.handle.route<LogServiceRoute>() }
    }

    @Test
    fun `a ViewModel made outside any screen has no route`() {
        assertNull(SavedStateHandle().routeOrNull<NavKey>())
    }

    @Test
    fun `results survive being saved and restored, each under its name`() {
        val dash = ResultKey<Int>("dashReading")
        val vin = ResultKey<String>("scannedVin")
        val results = mutableStateMapOf<String, Any>()
        results[dash] = 61_234
        results[vin] = "1HGCM82633A004352"
        val saved = with(ResultsSaver) { SaverScope { true }.save(results) }!!
        val restored = ResultsSaver.restore(saved)!!
        assertEquals(61_234, restored[dash])
        assertEquals("1HGCM82633A004352", restored[vin])

        restored.remove(dash)
        assertNull(restored[dash])
        assertEquals("1HGCM82633A004352", restored[vin])
    }

    @Test
    fun `trimming keeps the first entries and drops the rest`() {
        val stack = mutableListOf<NavKey>(GarageRoute, VehicleHomeRoute(1), VehicleDetailsRoute(1), RecallsRoute(1))
        stack.trimTo(2)
        assertEquals(listOf(GarageRoute, VehicleHomeRoute(1)), stack)
        stack.trimTo(5)
        assertEquals(2, stack.size)
    }

    @Test
    fun `a screen's state starts from its initial value and follows the flow while it is watched`() = runTest {
        val source = MutableStateFlow(1)
        val state = source.map { it * 10 }.screenState(backgroundScope, initial = 0)
        assertEquals(0, state.value)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        testScheduler.runCurrent()
        assertEquals(10, state.value)
        source.value = 2
        testScheduler.runCurrent()
        assertEquals(20, state.value)
    }
}
