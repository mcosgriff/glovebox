package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.data.log.VehicleLogs
import android.Manifest
import android.app.Application
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.LogServiceRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Log service form through its ViewModel, on a real database: what the user taps and types, and what is saved. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class LogServiceScreenTest {
    @get:Rule val compose = createComposeRule()
    private val t = ServiceTestDb()
    private val today: LocalDate = LocalDate.now()
    private var done = 0
    private var added = 0
    private var deleteInstead = 0

    @Before fun roots() = forgetFileProviderRoots()

    @After fun close() = t.close()

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = t.context.resources.getQuantityString(id, n, *args)

    private fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }

    private fun checkbox(label: String) = compose.onNode(isToggleable() and hasText(label))

    /** A truck with an oil change two years overdue, a rotation never logged, and a reading three days ago. */
    private suspend fun truck(): Long {
        val car = t.vehicle(model = "Tacoma", nickname = "Blue truck")
        t.db.schedule().applyImport(
            listOf(
                ScheduleItemEntity(vehicleId = car, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6),
                ScheduleItemEntity(vehicleId = car, name = "Tire rotation", templateKey = "rotation", intervalMiles = 5_000, intervalMonths = 6),
            ),
            emptyList(), emptyList(),
        )
        val oil = t.db.schedule().allFor(car).single { it.templateKey == "oil" }.id
        t.record(car, date = today.minusYears(2), odometer = 31_000, lines = listOf(line("Oil changed", "oil", oil)), shop = "Made-up Motors")
        t.db.odometer().insert(OdometerReadingEntity(vehicleId = car, date = today.minusDays(3), odometer = 52_500))
        return car
    }

    private fun open(route: LogServiceRoute): LogServiceViewModel {
        val vm = t.owned(
            LogServiceViewModel(
                routeHandle(route), VehicleLogs(t.db.vehicles(), t.db.schedule(), t.db.service(), t.db.odometer(), t.settings), t.db.service(), t.db.catalogRules(), t.actions, t.picker, t.invoices,
            ),
        )
        compose.setContent {
            GloveboxTheme { LogServiceScreen(onDone = { done++ }, onAdded = { added++ }, onDeleteInstead = { deleteInstead++ }, vm = vm) }
        }
        waitForText(string(R.string.log_what_section))
        return vm
    }

    @Test
    fun `the reading is only filled in on a tap, a due chip ticks its job, and the service is saved`() = runBlocking<Unit> {
        val car = truck()
        val vm = open(LogServiceRoute(car))
        compose.onNodeWithText(string(R.string.log_service)).assertExists()
        compose.onNodeWithText("2019 Blue truck").assertExists()
        assertEquals("", vm.form.odometerText)

        compose.onNodeWithText(string(R.string.log_use_current)).performClick()
        assertEquals(52_500.asNumber(), vm.form.odometerText)
        compose.onNodeWithText(string(R.string.log_use_current)).assertDoesNotExist()

        val chip = string(R.string.log_due_overdue, catalogLabel("oil"))
        compose.onNodeWithText(string(R.string.log_chip_off, chip)).performClick()
        compose.onNodeWithText(string(R.string.log_chip_on, chip)).assertExists()
        assertEquals(setOf("oil"), vm.form.selected)
        assertEquals("engine", vm.form.categoryId)

        // The tires tab opens its own lines; ticking one counts on the tab.
        compose.onNodeWithText(catalogCategoryLabel("tires")).performClick()
        checkbox(catalogLabel("rotation")).performClick()
        assertEquals(setOf("oil", "rotation"), vm.form.selected)
        compose.onNodeWithText(plural(R.plurals.log_items, 2, 2.asNumber()), substring = true).assertExists()

        compose.onNodeWithText(string(R.string.log_save)).performScrollTo().performClick()
        compose.settle("the save") { added == 1 }
        val saved = t.db.service().recordsFor(car).maxBy { it.record.date }
        assertEquals(52_500, saved.record.odometer)
        assertEquals(setOf("oil", "rotation"), saved.items.mapNotNull { it.catalogId }.toSet())
        assertEquals(0, done)
    }

    @Test
    fun `saving with nothing filled in says what is missing, and a bad cost says how to write one`() = runBlocking<Unit> {
        val car = truck()
        val vm = open(LogServiceRoute(car))
        compose.onNodeWithText(string(R.string.log_save)).performScrollTo().performClick()
        val missing = andList(listOf(string(R.string.log_missing_odometer), string(R.string.log_missing_item)))
        waitForText(string(R.string.log_to_save, missing))
        assertTrue(vm.form.showErrors)

        compose.onNodeWithContentDescription(string(R.string.log_cost_optional)).performScrollTo().performTextInput("12.3.4")
        compose.onNodeWithText(string(R.string.log_cost_error)).assertExists()
        compose.onNodeWithText(string(R.string.log_to_save, andList(listOf(string(R.string.log_missing_odometer), string(R.string.log_missing_item), string(R.string.log_missing_cost))))).assertExists()
        compose.onNodeWithContentDescription(string(R.string.log_cost_optional)).performTextClearance()
        compose.onNodeWithText(string(R.string.log_cost_error)).assertDoesNotExist()
        compose.waitForIdle()
        assertEquals(0, added)
        assertEquals(1, t.db.service().recordsFor(car).size)
    }

    @Test
    fun `a shop's name is offered from earlier records and saved with notes`() = runBlocking<Unit> {
        val car = truck()
        val vm = open(LogServiceRoute(car))
        vm.form.odometerText = "53000"
        vm.form.toggle("oil")
        compose.onNodeWithText(string(R.string.log_a_shop)).performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText(string(R.string.log_shop_name))).performScrollTo().performTextInput("Made")
        waitForText("Made-up Motors")
        compose.onNode(hasText("Made-up Motors") and hasClickAction() and !hasSetTextAction()).performClick()
        assertEquals("Made-up Motors", vm.form.shopName)
        compose.onNodeWithContentDescription(string(R.string.log_notes)).performScrollTo().performTextInput("Made-up note")

        compose.onNodeWithText(string(R.string.log_save)).performScrollTo().performClick()
        compose.settle("the save") { added == 1 }
        val saved = t.db.service().recordsFor(car).maxBy { it.record.date }.record
        assertEquals("Made-up Motors", saved.shopName)
        assertEquals("Made-up note", saved.notes)
        assertFalse(saved.diy)
    }

    /**
     * The "Add custom item" dialog isn't opened here: under Robolectric, its text field in a dialog over the form
     * never lets the test go idle and runs the JVM out of memory. The item is added the way its Add does, through
     * the form; what it offers as it is typed is `LineMatchesTest`'s.
     */
    @Test
    fun `a custom item can be filed under a line, keeping its words, or taken off`() = runBlocking<Unit> {
        val car = truck()
        val vm = open(LogServiceRoute(car))
        compose.onNodeWithText(string(R.string.log_add_custom_button)).performScrollTo().assertExists()
        vm.form.addCustom("  Hitch greased ")
        vm.form.addCustom("Hitch greased")
        vm.form.addCustom("Roof rack fitted")
        assertEquals(listOf("Hitch greased", "Roof rack fitted"), vm.form.customItems)
        checkbox("Hitch greased").performScrollTo().assertExists()

        compose.onAllNodes(hasText(string(R.string.log_file_under_button)))[0].performScrollTo().performClick()
        waitForText(string(R.string.log_file_under_body, "Hitch greased"))
        // One category opened, not all.
        compose.onNode(hasText(catalogCategoryLabel("tires")) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText(catalogLabel("alignment")) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        assertEquals(setOf("alignment"), vm.form.selected)
        assertEquals(mapOf("alignment" to "Hitch greased"), vm.form.keptLabels)
        assertEquals("tires", vm.form.categoryId)
        compose.onNodeWithText(string(R.string.log_kept_wording, "Hitch greased")).assertExists()

        // Its tick box takes a custom item off the form.
        checkbox("Roof rack fitted").performScrollTo().performClick()
        assertTrue(vm.form.customItems.isEmpty())

        // Backing out of filing leaves it under Custom.
        vm.form.addCustom("Mud flaps fitted")
        compose.onNodeWithText(string(R.string.log_file_under_button)).performScrollTo().performClick()
        waitForText(string(R.string.log_file_under_body, "Mud flaps fitted"))
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        assertEquals(listOf("Mud flaps fitted"), vm.form.customItems)
    }

    @Test
    fun `a day picked in the date picker goes on the form`() = runBlocking<Unit> {
        val car = truck()
        val vm = open(LogServiceRoute(car))
        compose.onNodeWithText(today.format(longDate)).performClick()
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        assertEquals(today, vm.form.date)

        compose.onNodeWithText(today.format(longDate)).performClick()
        // Material's day cells are named by the whole date.
        val first = today.withDayOfMonth(1)
        val name = "${first.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)} 1, ${first.year}"
        compose.onNode(hasText(name, substring = true) and hasClickAction()).performClick()
        compose.onNodeWithText(string(R.string.ok)).performClick()
        assertEquals(today.withDayOfMonth(1), vm.form.date)
    }

    @Test
    fun `photos are chosen through the attach menu, and the camera opens once it is allowed`() = runBlocking<Unit> {
        val car = truck()
        var picks = 0
        val shots = mutableListOf<android.net.Uri>()
        t.picker.attach({ picks++ }, { shots += it })
        open(LogServiceRoute(car))
        compose.onNodeWithText(string(R.string.log_photos_note)).performScrollTo()
        compose.onNodeWithText(string(R.string.log_attach)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.photo_choose)).performClick()
        assertEquals(1, picks)

        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.CAMERA)
        compose.onNodeWithText(string(R.string.log_attach)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.documents_take_photo)).performClick()
        assertEquals(1, shots.size)
    }

    @Test
    fun `an edit says what it changes, saves back, and can hand over to delete`() = runBlocking<Unit> {
        val car = truck()
        val id = t.db.service().recordsFor(car).single().record.id
        val vm = open(LogServiceRoute(car, recordId = id))
        compose.onNodeWithText(string(R.string.log_edit_title)).assertExists()
        compose.onNodeWithText(string(R.string.log_use_current)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.log_changes_section)).performScrollTo()
        compose.onNodeWithText(string(R.string.log_no_changes)).assertExists()

        vm.form.odometerText = "31,500"
        waitForText(string(R.string.reminder_change_next_due, "Oil change", 36_500.asDistance(DistanceUnit.MILES)), substring = true)

        compose.onNodeWithText(string(R.string.log_delete)).performScrollTo().performClick()
        assertEquals(1, deleteInstead)

        compose.onNodeWithText(string(R.string.log_save_changes)).performScrollTo().performClick()
        compose.settle("the save") { done == 1 }
        assertEquals(31_500, t.db.service().record(id)!!.record.odometer)
        assertEquals(0, added)
    }

    @Test
    fun `Close leaves without saving`() = runBlocking<Unit> {
        val car = truck()
        open(LogServiceRoute(car))
        compose.onNodeWithContentDescription(string(R.string.close)).performClick()
        assertEquals(1, done)
        assertEquals(1, t.db.service().recordsFor(car).size)
    }
}
