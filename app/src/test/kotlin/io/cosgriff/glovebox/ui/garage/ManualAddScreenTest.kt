package io.cosgriff.glovebox.ui.garage

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ManualAddScreenTest {
    @get:Rule val compose = createComposeRule()

    // Room's writes from viewModelScope finish only off Robolectric's paused main looper.
    @get:Rule val main = UnconfinedMainRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = testDatabase()
    private val nhtsa = FakeNhtsa().apply {
        makes = listOf("TOYOTA", "VOLKSWAGEN")
        models = listOf("4Runner", "Tacoma")
    }
    private val events = mutableListOf<String>()

    @After fun close() = db.close()

    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(): ManualAddViewModel {
        val vm = ManualAddViewModel(nhtsa.client, db.vehicles(), testSettings())
        compose.setContent {
            GloveboxTheme { ManualAddScreen(onSaved = { events += "saved $it" }, onBack = { events += "back" }, vm = vm) }
        }
        return vm
    }

    private fun picker(@StringRes label: Int): SemanticsNodeInteraction =
        compose.onNode(hasText(str(label)) and hasClickAction() and !hasAnyAncestor(isDialog()))

    /** A row of the open picker, not the search box that may hold the same text. */
    private fun inDialog(text: String) = compose.onNode(hasText(text) and hasClickAction() and !hasSetTextAction() and hasAnyAncestor(isDialog()))

    private fun search(text: String) = compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput(text)

    @Test
    fun `the model waits for a year and a make, which are picked from NHTSA's lists`() {
        show()
        picker(R.string.add_vehicle_model).assertIsNotEnabled()
        compose.onNodeWithText(str(R.string.manual_add_year_make_first)).assertExists()

        picker(R.string.manual_add_year).performClick()
        search("2019")
        inDialog("2019").performClick()
        picker(R.string.add_vehicle_make).performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Toyota").fetchSemanticsNodes().isNotEmpty() }
        // NHTSA's capitals are shown as the make is written.
        inDialog("Toyota").performClick()

        picker(R.string.add_vehicle_model).assertIsEnabled().performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Tacoma").fetchSemanticsNodes().isNotEmpty() }
        search("ta")
        compose.onAllNodesWithText("4Runner").assertCountEquals(0)
        inDialog("Tacoma").performClick()
        compose.onAllNodesWithText(str(R.string.manual_add_year_make_first)).assertCountEquals(0)
        compose.onNodeWithText("Tacoma").assertExists()
    }

    @Test
    fun `what NHTSA doesn't list can be typed and used as it stands`() {
        val vm = show()
        picker(R.string.add_vehicle_make).performClick()
        search("Kit Car Co")
        inDialog(str(R.string.manual_add_use_typed, "Kit Car Co")).performClick()
        compose.waitForIdle()
        assertEquals("Kit Car Co", vm.make)
    }

    @Test
    fun `makes that couldn't be loaded can be asked for again`() {
        nhtsa.makes = null
        show()
        picker(R.string.add_vehicle_make).performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(str(R.string.manual_add_load_failed)).fetchSemanticsNodes().isNotEmpty() }
        nhtsa.makes = listOf("FORD")
        inDialog(str(R.string.retry)).performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Ford").fetchSemanticsNodes().isNotEmpty() }
        inDialog(str(R.string.cancel)).performClick()
        compose.onAllNodesWithText("Ford").assertCountEquals(0)
    }

    @Test
    fun `a make and year NHTSA has no models for say so`() {
        nhtsa.models = emptyList()
        val vm = show()
        vm.chooseYear(1931)
        vm.chooseMake("FORD")
        picker(R.string.add_vehicle_model).performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(str(R.string.manual_add_no_models, "1931", "Ford")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `the drivetrain starts unpicked, and tapping the picked one clears it`() {
        val vm = show()
        compose.onNodeWithText("AWD").performScrollTo().assertIsNotSelected().performClick()
        compose.onNodeWithText("AWD").assertIsSelected()
        assertEquals("AWD", vm.drivetrain)
        compose.onNodeWithText("AWD").performClick()
        compose.waitForIdle()
        assertEquals(null, vm.drivetrain)
    }

    @Test
    fun `Next saves the vehicle as given, and Back leaves`() {
        val vm = show()
        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        vm.chooseYear(LocalDate.now().year)
        vm.chooseMake("TOYOTA")
        vm.chooseModel("Tacoma")
        compose.onNode(hasSetTextAction() and hasText(str(R.string.manual_add_engine))).performTextInput("2.4L")
        compose.onNodeWithText(str(R.string.add_vehicle_next)).performScrollTo().performClick()
        compose.waitUntil(TIMEOUT) { events.any { it.startsWith("saved") } }
        val id = events.last().removePrefix("saved ").toLong()
        assertEquals("back", events.first())
        assertEquals("2.4L", runBlocking { db.vehicles().byId(id)!!.engine })
    }
}
