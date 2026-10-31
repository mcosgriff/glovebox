package io.cosgriff.glovebox.ui.history

import io.cosgriff.glovebox.data.log.VehicleLogs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasContentDescription
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.service.asDistance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobEntity
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.ui.garage.GarageOverview
import io.cosgriff.glovebox.ui.service.monthYear
import io.cosgriff.glovebox.domain.HistoryOrder
import io.cosgriff.glovebox.domain.HistorySort
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.ArchivedHistoryRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.ServiceTestDb
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.line
import io.cosgriff.glovebox.ui.service.routeHandle
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** History through its ViewModel, on a real database: what the tab shows as records come and go and the user picks. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private val t = ServiceTestDb()
    private val today: LocalDate = LocalDate.now()

    @After fun close() = t.close()

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = t.context.resources.getQuantityString(id, n, *args)

    private fun SemanticsNodeInteractionsProvider.waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(5_000) { onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    private fun SemanticsNodeInteractionsProvider.waitForNoText(text: String, substring: Boolean = false) =
        compose.waitUntil(5_000) { onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().isEmpty() }

    private fun show(vm: HistoryViewModel, opened: MutableList<Long> = mutableListOf(), sorting: MutableList<Long> = mutableListOf()) {
        compose.setContent {
            GloveboxTheme { HistoryScreen(onOpenRecord = { opened += it }, onSortCustom = { sorting += it }, vm = vm) }
        }
    }

    private fun logs() = VehicleLogs(t.db.vehicles(), t.db.schedule(), t.db.service(), t.db.odometer(), t.settings)

    private fun overview() = GarageOverview(logs(), t.db.recalls())

    /** History opens on the timeline; these tests read the list unless they say so. */
    private fun listHandle() = SavedStateHandle(mapOf("view" to HistoryView.LIST.name))

    private fun tab(handle: SavedStateHandle = listHandle()) = t.owned(HistoryViewModel(handle, t.db.vehicles(), t.db.recalls(), t.actions, overview(), logs()))

    @Test
    fun `the first vehicle shows, another is one tap away, and the choice is kept`() = runBlocking {
        t.vehicle(model = "Corolla")
        val truck = t.vehicle(model = "Tacoma")
        t.record(t.db.vehicles().all().first { it.model == "Corolla" }.id, lines = listOf(line("Corolla wipers")))
        t.record(truck, lines = listOf(line("Tacoma wipers")))
        val handle = listHandle()
        val vm = tab(handle)
        show(vm)

        compose.waitForText("Corolla wipers")
        compose.onNodeWithText("Tacoma wipers").assertDoesNotExist()
        compose.onNodeWithText("2019 Tacoma").performClick()
        compose.waitForText("Tacoma wipers")
        compose.onNodeWithText("2019 Tacoma").assertIsSelected()
        assertEquals(truck, handle.get<Long>("chosenVehicle"))
    }

    @Test
    fun `a category filter or a search hides the spending line, and a search with no match offers every record again`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(40), odometer = 40_000, lines = listOf(line("Oil and filter", "oil")), costCents = 5_000)
        t.record(car, date = today.minusDays(20), odometer = 41_000, lines = listOf(line("Tires rotated", "rotation")), costCents = 3_000, shop = "Made-up Tire")
        val handle = listHandle()
        val vm = tab(handle)
        show(vm)

        val total = Money.format(8_000)
        compose.waitForText(total, substring = true)

        compose.onNodeWithText(io.cosgriff.glovebox.ui.service.catalogCategoryLabel("tires")).performClick()
        compose.waitForNoText(total, substring = true)
        compose.onNodeWithText("Oil and filter").assertDoesNotExist()
        compose.onNodeWithText("Tires rotated").assertExists()
        assertEquals("tires", handle.get<String>("filter"))

        compose.onNodeWithText(string(R.string.history_all)).performClick()
        compose.waitForText(total, substring = true)

        compose.onNode(hasSetTextAction()).performTextInput("tire")
        compose.waitForNoText(total, substring = true)
        compose.onNodeWithText("Oil and filter").assertDoesNotExist()
        assertEquals("tire", handle.get<String>("search"))

        compose.onNodeWithContentDescription(string(R.string.history_search_clear)).performClick()
        compose.waitForText("Oil and filter")

        compose.onNode(hasSetTextAction()).performTextInput("serpentine")
        compose.waitForText(string(R.string.history_no_match, "serpentine"))
        compose.onNodeWithText(string(R.string.history_search_show_all)).performClick()
        compose.waitForText("Oil and filter")
        assertEquals("", vm.search)
    }

    @Test
    fun `a category with two jobs swaps the categories for its jobs, one shows alone, and back goes to All`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(60), odometer = 39_000, lines = listOf(line("Oil and filter", "oil")))
        t.record(car, date = today.minusDays(40), odometer = 40_000, lines = listOf(line("Air filter", "engine-air")))
        t.record(car, date = today.minusDays(20), odometer = 41_000, lines = listOf(line("Tires rotated", "rotation")))
        val handle = listHandle()
        val vm = tab(handle)
        show(vm)
        val oil = io.cosgriff.glovebox.ui.service.catalogLabel("oil")
        val engine = io.cosgriff.glovebox.ui.service.catalogCategoryLabel("engine")
        val tires = io.cosgriff.glovebox.ui.service.catalogCategoryLabel("tires")
        compose.waitForText("Oil and filter")
        compose.onNodeWithText(oil).assertDoesNotExist()

        compose.onNodeWithText(engine).performClick()
        compose.waitForText(oil)
        compose.onNodeWithText(tires).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.history_all)).assertDoesNotExist()
        compose.onNodeWithText(engine).assertIsSelected()
        compose.onNodeWithText("Tires rotated").assertDoesNotExist()
        compose.onNodeWithText("Air filter").assertExists()

        compose.onNodeWithText(oil).performClick()
        compose.waitForNoText("Air filter")
        compose.onNodeWithText("Oil and filter").assertExists()
        compose.onNodeWithText(oil).assertIsSelected()
        assertEquals("oil", handle.get<String>("job"))

        // A second tap, or the category's own chip, is the whole category again.
        compose.onNodeWithText(oil).performClick()
        compose.waitForText("Air filter")
        assertNull(vm.job)
        compose.onNodeWithText(oil).performClick()
        compose.waitForNoText("Air filter")
        compose.onNodeWithText(engine).performClick()
        compose.waitForText("Air filter")
        assertNull(vm.job)

        compose.onNodeWithText(oil).performClick()
        compose.waitForNoText("Air filter")
        compose.onNodeWithContentDescription(string(R.string.history_filter_back)).performClick()
        compose.waitForText("Tires rotated")
        compose.onNodeWithText(oil).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.history_all)).assertIsSelected()
        assertNull(handle.get<String>("filter"))
        assertNull(handle.get<String>("job"))

        // Tires has one job, so it is chosen in place, with nothing to step into.
        compose.onNodeWithText(tires).performClick()
        compose.waitForNoText("Air filter")
        compose.onNodeWithText(tires).assertIsSelected()
        compose.onNodeWithContentDescription(string(R.string.history_filter_back)).assertDoesNotExist()
    }

    @Test
    fun `labor and parts show under the spending line from the priced records, and go with it while a filter is on`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(40), odometer = 40_000, lines = listOf(line("Oil and filter", "oil")), costCents = 5_000)
        t.db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = car, date = today.minusDays(20), odometer = 41_000, diy = false, shopName = "Made-up Tire", costCents = 3_000),
            listOf(line("Tires rotated", "rotation").copy(jobId = 1)),
            listOf(ServiceRecordJobEntity(id = 1, recordId = 0, laborCents = 2_500)),
        )
        show(tab())

        // Parts no record entered are left out, not shown as nothing spent.
        val split = plural(R.plurals.history_spending_split_labor, 2, 1.asNumber(), 2.asNumber(), Money.format(2_500))
        compose.waitForText(split)

        compose.onNodeWithText(io.cosgriff.glovebox.ui.service.catalogCategoryLabel("tires")).performClick()
        compose.waitForNoText(split)
    }

    @Test
    fun `the order can be turned round and set to mileage, and is kept`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(30), odometer = 45_000)
        t.record(car, date = today.minusDays(10), odometer = 44_000)
        val handle = listHandle()
        val vm = tab(handle)
        show(vm)
        compose.waitForText(plural(R.plurals.records, 2, 2.asNumber()), substring = true)

        compose.onNodeWithContentDescription(string(R.string.history_reverse)).performClick()
        compose.waitForIdle()
        assertEquals(HistoryOrder(HistorySort.DATE, descending = false), vm.order)

        compose.onNodeWithContentDescription(string(R.string.history_sort_by)).performClick()
        compose.onNodeWithText(string(R.string.history_sort_mileage)).performClick()
        compose.waitForIdle()
        assertEquals(HistoryOrder(HistorySort.MILEAGE, descending = false), vm.order)
        assertEquals("MILEAGE", handle.get<String>("sortBy"))
        assertEquals(false, handle.get<Boolean>("descending"))

        // Picking what is already chosen changes nothing.
        compose.onNodeWithContentDescription(string(R.string.history_sort_by)).performClick()
        compose.onNodeWithText(string(R.string.history_sort_mileage)).performClick()
        assertEquals(HistoryOrder(HistorySort.MILEAGE, descending = false), vm.order)

        // A ViewModel made again from the saved state reads the same way.
        assertEquals(HistoryOrder(HistorySort.MILEAGE, descending = false), tab(handle).order)
    }

    @Test
    fun `a new record anywhere brings its vehicle up with the search cleared, and leaving History clears the mark`() = runBlocking {
        t.vehicle(model = "Corolla")
        val truck = t.vehicle(model = "Tacoma")
        val vm = tab()
        var showing by mutableStateOf(true)
        compose.setContent { GloveboxTheme { if (showing) HistoryScreen(onOpenRecord = {}, vm = vm) } }
        compose.waitForText(string(R.string.history_none))
        vm.search("wipers")

        t.actions.add(ServiceRecordEntity(vehicleId = truck, date = today, odometer = 30_000, diy = true), listOf(line("Brake fluid flushed")))
        compose.waitForText("Brake fluid flushed")
        assertEquals("", vm.search)
        assertEquals(truck, vm.fresh.value?.vehicleId)

        showing = false
        compose.waitForIdle()
        assertNull(t.actions.focus.value)
    }

    @Test
    fun `an opened record is the one tapped`() = runBlocking {
        val car = t.vehicle()
        val id = t.record(car, lines = listOf(line("Cabin filter")))
        val opened = mutableListOf<Long>()
        show(tab(), opened)
        compose.waitForText("Cabin filter")
        compose.onNodeWithText("Cabin filter").performClick()
        assertEquals(listOf(id), opened)
    }

    @Test
    fun `Custom lines are counted and open the sort screen for that vehicle`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(5), lines = listOf(line("Hitch greased"), line("Roof rack fitted")))
        t.record(car, date = today.minusDays(3), lines = listOf(line("Hitch greased")))
        val sorting = mutableListOf<Long>()
        show(tab(), sorting = sorting)
        val label = plural(R.plurals.history_custom_ways, 2, 3.asNumber(), 2.asNumber())
        compose.waitForText(label)
        compose.onNodeWithText(label).performClick()
        assertEquals(listOf(car), sorting)
    }

    @Test
    fun `records linked as a visit are one card with each record and the visit's total`() = runBlocking {
        val car = t.vehicle()
        val a = t.record(car, date = today.minusDays(5), odometer = 50_000, lines = listOf(line("Oil and filter", "oil")), costCents = 6_000)
        val b = t.record(car, date = today.minusDays(5), odometer = 50_000, lines = listOf(line("Tires rotated", "rotation")), costCents = 2_500, shop = "Made-up Tire")
        t.actions.link(a, b)
        val opened = mutableListOf<Long>()
        show(tab(), opened)

        compose.waitForText(plural(R.plurals.history_visit, 2, 2.asNumber()))
        compose.onNodeWithText(string(R.string.history_visit_total)).assertExists()
        compose.onNodeWithText(Money.format(8_500)).assertExists()
        compose.onNodeWithText("Tires rotated").performClick()
        assertEquals(listOf(b), opened)
    }

    @Test
    fun `an archived vehicle's history is read only and offers no sorting`() = runBlocking {
        val car = t.vehicle()
        t.db.vehicles().setArchived(car, today)
        t.record(car, lines = listOf(line("Hitch greased")))
        val vm = t.owned(HistoryViewModel(routeHandle(ArchivedHistoryRoute(car)), t.db.vehicles(), t.db.recalls(), t.actions, overview(), logs()))
        show(vm)
        compose.waitForText(string(R.string.history_subtitle_archived, "2019 Corolla", plural(R.plurals.records, 1, 1.asNumber())))
        compose.onNodeWithText(plural(R.plurals.history_custom_lines, 1, 1.asNumber())).assertDoesNotExist()
        assertEquals(true, vm.archived)
    }

    private fun waitForOdometer(miles: Int) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasContentDescription(string(R.string.history_odometer_description, miles.asDistance(DistanceUnit.MILES)))).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `the vehicle's odometer shows under it`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(20), odometer = 41_000, lines = listOf(line("Oil and filter", "oil")))
        show(tab())
        waitForOdometer(41_000)
    }

    @Test
    fun `an archived vehicle's odometer shows under it too`() = runBlocking {
        val car = t.vehicle()
        t.record(car, date = today.minusDays(20), odometer = 41_000, lines = listOf(line("Oil and filter", "oil")))
        t.db.vehicles().setArchived(car, today)
        show(t.owned(HistoryViewModel(routeHandle(ArchivedHistoryRoute(car)), t.db.vehicles(), t.db.recalls(), t.actions, overview(), logs())))
        waitForOdometer(41_000)
    }

    @Test
    fun `an archived vehicle with no records says none were logged`() = runBlocking {
        val car = t.vehicle()
        t.db.vehicles().setArchived(car, today)
        show(t.owned(HistoryViewModel(routeHandle(ArchivedHistoryRoute(car)), t.db.vehicles(), t.db.recalls(), t.actions, overview(), logs())))
        compose.waitForText(string(R.string.history_none_archived))
    }

    @Test
    fun `the timeline is kept in the saved state, lays the records out by date, and its next reminder opens Log service`() = runBlocking {
        val car = t.vehicle()
        // By distance only, so the dates below never make it overdue (an oil change would get Glovebox's months, #517).
        t.db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = car, name = "Tire rotation", templateKey = "rotation", intervalMiles = 5_000, intervalMonths = null)))
        val oil = t.db.schedule().allFor(car).single().id
        t.record(car, date = LocalDate.of(2025, 10, 18), odometer = 50_200, lines = listOf(line("Rotated the tires", "rotation")))
        t.record(car, date = LocalDate.of(2026, 2, 2), odometer = 54_800, lines = listOf(line("Front brake pads", "front-pads")))
        val handle = SavedStateHandle()
        val vm = tab(handle)
        val opened = mutableListOf<Long>()
        val logged = mutableListOf<Pair<Long, Long>>()
        compose.setContent {
            GloveboxTheme { HistoryScreen(onOpenRecord = { opened += it }, onLogIt = { v, item -> logged += v to item }, vm = vm) }
        }
        // With nothing chosen yet, History opens on the timeline.
        compose.waitForText(string(R.string.history_timeline_coming_up))
        compose.onNodeWithContentDescription(string(R.string.history_view_timeline)).assertIsSelected()
        assertEquals(HistoryView.TIMELINE, vm.view)

        compose.onNodeWithContentDescription(string(R.string.history_view_list)).performClick()
        compose.waitForNoText(string(R.string.history_timeline_coming_up))
        assertEquals("LIST", handle.get<String>("view"))
        assertEquals(HistoryView.LIST, tab(handle).view)

        compose.onNodeWithContentDescription(string(R.string.history_view_timeline)).performClick()
        compose.waitForText(string(R.string.history_timeline_coming_up))
        assertEquals("TIMELINE", handle.get<String>("view"))
        assertEquals(HistoryView.TIMELINE, tab(handle).view)
        // Newest first, gaps between them, and the oldest record's month at the end.
        compose.onNodeWithText(plural(R.plurals.history_timeline_apart, 3, 3.asNumber())).assertExists()
        compose.onNodeWithText(string(R.string.history_timeline_since, LocalDate.of(2025, 10, 1).format(monthYear))).performScrollTo().assertExists()

        compose.onNodeWithText("Tire rotation").performClick()
        assertEquals(listOf(car to oil), logged)
        compose.onNodeWithText("Front brake pads").performScrollTo().performClick()
        assertEquals(1, opened.size)
    }

    @Test
    fun `an empty garage says to add a vehicle`() {
        show(tab())
        compose.waitForText(string(R.string.history_no_vehicle))
        compose.onNodeWithText(string(R.string.history_export)).assertDoesNotExist()
    }
}
