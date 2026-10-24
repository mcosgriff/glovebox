package io.cosgriff.glovebox.ui.torque

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.domain.KnownSpecs
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueSpecs
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.domain.WrittenFile
import io.cosgriff.glovebox.ui.garage.UnconfinedMainRule
import io.cosgriff.glovebox.ui.garage.waitFor
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Tall enough that every row of the list is composed.
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = "w411dp-h2000dp-xhdpi", sdk = [35])
class TorqueSpecsScreenTest {
    @get:Rule val compose = createComposeRule()

    // Room's writes from viewModelScope finish only off Robolectric's paused main looper.
    @get:Rule val main = UnconfinedMainRule()

    private val h = TorqueHarness()
    private val events = mutableListOf<String>()
    private val saved = mutableListOf<SpecDraft>()

    @After fun close() = h.close()

    private fun str(@StringRes id: Int, vararg args: Any) = h.context.getString(id, *args)

    // Made-up figures, none from any manual.
    private fun spec(id: Long, name: String, category: TorqueCategory, nm: Double?, source: TorqueSource = TorqueSource.USER, knownList: String? = null, note: String? = null) =
        TorqueSpecEntity(id = id, vehicleId = 1, name = name, category = category.name, newtonMeters = nm, source = source.name, note = note, knownList = knownList, socketSize = "14 mm".takeIf { nm != null })

    private fun content(state: TorqueSpecsState) = compose.setContent {
        GloveboxTheme {
            TorqueSpecsContent(
                state, onBack = { events += "back" },
                onSave = { saved += it },
                onDelete = { events += "delete ${it.name}" },
                onAddCommon = { events += "common" },
                onAddKnown = { events += "known" },
                onWriteFile = { events += "write"; WrittenFile("{}", 1) },
                onSaveFile = { events += "save file" },
            )
        }
    }

    private val specs = listOf(
        spec(1, "Drain plug", TorqueCategory.ENGINE, 33.3, TorqueSource.FACTORY, note = "A made-up note"),
        spec(2, "Lug nuts", TorqueCategory.WHEELS, 111.1, TorqueSource.REFERENCE, knownList = "made-up-list"),
        spec(3, "Hitch bolt", TorqueCategory.OTHER, null),
        spec(4, "Skid plate", TorqueCategory.OTHER, 22.2),
    )

    @Test
    fun `an empty list offers the common fasteners and nothing else`() {
        content(TorqueSpecsState(Fixtures.truck, emptyList(), TorqueUnit.NEWTON_METER))
        compose.onNodeWithText(str(R.string.torque_empty_body)).assertExists()
        compose.onNodeWithText(str(R.string.torque_start_common)).performClick()
        assertEquals(listOf("common"), events)
        compose.onAllNodesWithText(str(R.string.torque_search)).assertCountEquals(0)
    }

    @Test
    fun `each row is tagged by where its value came from, and an unchecked reference value says DOUBLE-CHECK`() {
        content(TorqueSpecsState(Fixtures.truck, specs, TorqueUnit.NEWTON_METER, knownSources = mapOf("made-up-list" to "A made-up source")))
        compose.onNodeWithText(str(R.string.torque_tag_factory)).assertExists()
        compose.onNodeWithText(str(R.string.torque_tag_double_check)).assertExists()
        compose.onNodeWithText(str(R.string.torque_tag_needs_value)).assertExists()
        compose.onNodeWithText(str(R.string.torque_tag_yours)).assertExists()
        compose.onNodeWithText(str(R.string.torque_started_from, "A made-up source")).assertExists()
        compose.onNodeWithText("A made-up note").assertExists()
    }

    @Test
    fun `values are shown in the unit picked here, starting from the Settings one`() {
        content(TorqueSpecsState(Fixtures.truck, specs, TorqueUnit.NEWTON_METER))
        val drain = Torque(33.3)
        compose.onNodeWithText(drain.text(TorqueUnit.NEWTON_METER)).assertExists()
        compose.onNodeWithText(str(R.string.torque_unit_ft_lb)).performClick()
        compose.onNodeWithText(drain.text(TorqueUnit.FOOT_POUND)).assertExists()
        // The others underneath, in the units not picked.
        compose.onNodeWithText("${drain.text(TorqueUnit.NEWTON_METER)} · ${drain.text(TorqueUnit.INCH_POUND)}").assertExists()
    }

    @Test
    fun `search and the category chips narrow the list`() {
        content(TorqueSpecsState(Fixtures.truck, specs, TorqueUnit.NEWTON_METER))
        compose.onNodeWithText(TorqueCategory.WHEELS.label).performClick()
        compose.onAllNodesWithText("Drain plug").assertCountEquals(0)
        compose.onNodeWithText("Lug nuts").assertExists()
        compose.onNodeWithText(str(R.string.history_all)).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("zzz")
        compose.onNodeWithText(str(R.string.torque_nothing_matches)).assertExists()
    }

    @Test
    fun `a bundled list is offered with its source, and the missing common fasteners can be added`() {
        val list = runBlocking { KnownSpecCatalog(h.context).lists() }.first { it.id == "toyota-4runner-2017-2024" }
        val plan = KnownSpecs.plan(list, emptyList())
        content(TorqueSpecsState(Fixtures.truck, specs, TorqueUnit.NEWTON_METER, known = list to plan))
        compose.onNodeWithText(str(R.string.torque_add_known, list.title)).performClick()
        compose.onNodeWithText(offer(list, plan)).assertExists()
        compose.onNodeWithText(str(R.string.torque_add_missing)).performClick()
        assertEquals(listOf("known", "common"), events)
    }

    @Test
    fun `a row opens in the editor, and a new spec starts empty`() {
        content(TorqueSpecsState(Fixtures.truck, specs, TorqueUnit.NEWTON_METER))
        compose.onNodeWithText("Skid plate").performClick()
        compose.onNodeWithText(str(R.string.torque_edit_title)).assertExists()
        compose.onNodeWithText(str(R.string.torque_delete)).performClick()
        assertEquals(listOf("delete Skid plate"), events)

        compose.onNodeWithText(str(R.string.torque_add)).performClick()
        compose.onNodeWithText(str(R.string.torque_add_title)).assertExists()
        compose.onNode(hasSetTextAction() and hasText(str(R.string.torque_fastener))).performTextInput("Made-up bolt")
        compose.onNodeWithText(str(R.string.save)).performClick()
        assertEquals("Made-up bolt", saved.single().name)
        assertEquals(0L, saved.single().id)
    }

    @Test
    fun `a deleted spec comes back from the message's Undo`() {
        val truck = h.addTruck()
        h.insert(h.spec(truck, "Drain plug", nm = 33.3))
        val vm = h.viewModel(truck)
        var undo: (() -> Unit)? = null
        compose.setContent {
            GloveboxTheme {
                TorqueSpecsScreen(onBack = {}, onMessage = { message, action, onAction -> events += "$message|$action"; undo = onAction }, vm = vm)
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Drain plug").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Drain plug").performClick()
        compose.onNodeWithText(str(R.string.torque_delete)).performClick()
        compose.waitUntil(5_000) { undo != null }
        assertEquals(listOf("${str(R.string.torque_deleted, "Drain plug")}|${str(R.string.undo)}"), events)
        assertTrue(h.specs(truck).isEmpty())
        undo!!()
        waitFor("the undo") { h.specs(truck).isNotEmpty() }

        // The common fasteners from the screen are added through the ViewModel.
        compose.waitUntil(5_000) { compose.onAllNodesWithText(str(R.string.torque_add_missing)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(str(R.string.torque_add_missing)).performClick()
        waitFor("the common fasteners") { h.specs(truck).size == 1 + TorqueSpecs.common.size }
    }
}
