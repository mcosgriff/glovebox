package io.cosgriff.glovebox.ui.history

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.MilestoneEntity
import io.cosgriff.glovebox.data.db.MilestoneJobEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.categoryOf
import io.cosgriff.glovebox.ui.SortCustomRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.ServiceTestDb
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.catalogCategoryLabel
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.line
import io.cosgriff.glovebox.ui.service.routeHandle
import io.cosgriff.glovebox.ui.service.settle
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

/** Sorting Custom lines (#310) through its ViewModel, on a real database. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class SortCustomScreenTest {
    @get:Rule val compose = createComposeRule()
    private val t = ServiceTestDb()
    private val today: LocalDate = LocalDate.now()
    private var backs = 0

    @After fun close() = t.close()

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = t.context.resources.getQuantityString(id, n, *args)
    private fun lineName(id: String) = string(R.string.sort_custom_line, catalogLabel(id), catalogCategoryLabel(ServiceCatalog.categoryOf(id)!!.id))

    private fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    private fun sorter(car: Long) = t.owned(
        SortCustomViewModel(
            routeHandle(SortCustomRoute(car)), t.db.vehicles(), t.db.service(), t.db.recalls(), t.db.catalogRules(), t.db.schedule(),
            t.db.milestones(), t.settings, t.actions,
        ),
    )

    private fun show(vm: SortCustomViewModel) = compose.setContent { GloveboxTheme { SortCustomScreen(onBack = { backs++ }, vm = vm) } }

    /**
     * A car whose oil change is tracked and in its own milestone table, with four Custom wordings: one Glovebox
     * recognises, one the user has a rule for, one it can only guess, and one it has nothing for.
     */
    private suspend fun car(): Long {
        val car = t.vehicle()
        t.db.schedule().applyImport(
            listOf(ScheduleItemEntity(vehicleId = car, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = null)),
            emptyList(), emptyList(),
        )
        t.db.milestones().save(MilestoneEntity(vehicleId = car, atMiles = 60_000), listOf(MilestoneJobEntity(milestoneId = 0, name = "Engine oil", service = "oil")))
        t.db.catalogRules().put("Roof rack fitted", "washer", today)
        t.record(car, date = today.minusDays(90), odometer = 40_000, lines = listOf(line("Oil and filter changed"), line("Hitch greased")))
        t.record(car, date = today.minusDays(30), odometer = 42_000, lines = listOf(line("Hitch greased"), line("Roof rack fitted"), line("Watr pump replaced")))
        return car
    }

    @Test
    fun `each wording is a row with what it looks like, and saving files what is picked`() = runBlocking<Unit> {
        val car = car()
        show(sorter(car))
        waitForText("Hitch greased")
        compose.onNodeWithText(plural(R.plurals.sort_custom_count, 4, 5.asNumber(), 4.asNumber())).assertExists()
        // Recognised wording and the user's rule are picked already; the guess and the unknown wait.
        compose.onNodeWithText(string(R.string.sort_custom_from_wording)).assertExists()
        compose.onNodeWithText(string(R.string.sort_custom_from_rule)).assertExists()
        compose.onNodeWithText(string(R.string.sort_custom_looks_like, lineName("water-pump"))).assertExists()
        // What saving would do: the oil change counts, so its reminder and the table's job move.
        waitForText(string(R.string.sort_custom_table_jobs, "Engine oil"))
        compose.onNodeWithText(string(R.string.reminder_change_next_due, "Oil change", 45_000.asDistance(DistanceUnit.MILES))).assertExists()
        compose.onNodeWithText(plural(R.plurals.sort_custom_save, 2, 2.asNumber())).assertExists()

        // The guess is only taken on a tap.
        compose.onNodeWithContentDescription(string(R.string.sort_custom_use_for, "Watr pump replaced", catalogLabel("water-pump"))).performScrollTo().performClick()
        waitForText(plural(R.plurals.sort_custom_save, 3, 3.asNumber()))
        // Don't remember the guessed wording as a rule.
        compose.onNodeWithContentDescription(string(R.string.sort_custom_remember_for, "Watr pump replaced")).performScrollTo().performClick()

        compose.onNodeWithText(plural(R.plurals.sort_custom_save, 3, 3.asNumber())).performClick()
        compose.settle("the save") { backs == 1 }

        val lines = t.db.service().recordsFor(car).flatMap { it.items }.associate { it.label to it.catalogId }
        assertEquals("oil", lines["Oil and filter changed"])
        assertEquals("washer", lines["Roof rack fitted"])
        assertEquals("water-pump", lines["Watr pump replaced"])
        assertNull(lines["Hitch greased"])
        // The recognised wording is remembered; the user's own rule was there already; the guess was unticked.
        assertEquals(
            setOf("Roof rack fitted" to "washer", "Oil and filter changed" to "oil"),
            t.db.catalogRules().all().map { it.text to it.catalogId }.toSet(),
        )
    }

    @Test
    fun `a wording can be left under Custom, or filed under any line picked from the list`() = runBlocking<Unit> {
        val car = car()
        val vm = sorter(car)
        show(vm)
        waitForText("Hitch greased")

        compose.onNodeWithContentDescription(string(R.string.sort_custom_leave_for, "Oil and filter changed")).performClick()
        waitForText(plural(R.plurals.sort_custom_save, 1, 1.asNumber()))
        assertEquals(mapOf<String, String?>(vm.chosen.keys.single() to null), vm.chosen.toMap())

        compose.onNodeWithContentDescription(string(R.string.sort_custom_pick_for, "Hitch greased")).performScrollTo().performClick()
        waitForText(string(R.string.log_file_under_body, "Hitch greased"))
        compose.onNodeWithText(string(R.string.expand_all)).performClick()
        compose.onNodeWithText(catalogLabel("alignment")).performScrollTo().performClick()
        waitForText(lineName("alignment"))
        compose.onNodeWithText(string(R.string.sort_custom_from_you)).assertExists()
        waitForText(plural(R.plurals.sort_custom_save, 3, 3.asNumber()))

        // Changed again, then back out of the list.
        compose.onNodeWithContentDescription(string(R.string.sort_custom_change_for, "Hitch greased")).performScrollTo().performClick()
        waitForText(string(R.string.log_file_under_body, "Hitch greased"))
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        compose.onNodeWithText(lineName("alignment")).assertExists()
    }

    @Test
    fun `a vehicle with nothing under Custom says so and offers no save`() = runBlocking<Unit> {
        val car = t.vehicle()
        t.record(car, lines = listOf(line("Oil and filter", "oil")))
        show(sorter(car))
        waitForText(string(R.string.sort_custom_none))
        compose.onNodeWithText(string(R.string.sort_custom_save_none)).assertDoesNotExist()
    }

    @Test
    fun `with nothing picked there is nothing to save`() = runBlocking<Unit> {
        val car = t.vehicle()
        t.record(car, lines = listOf(line("Hitch greased")))
        val vm = sorter(car)
        show(vm)
        waitForText(string(R.string.sort_custom_save_none))
        vm.save(vm.state.value!!) { backs++ }
        compose.waitForIdle()
        assertEquals(0, backs)
    }

    @Test
    fun `a vehicle that has gone shows nothing`() {
        val vm = sorter(404)
        show(vm)
        compose.waitForIdle()
        assertNull(vm.state.value)
        compose.onNodeWithText(string(R.string.sort_custom_title)).assertDoesNotExist()
    }
}
