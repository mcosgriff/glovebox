package io.cosgriff.glovebox.ui.vehicle

import android.content.ClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasScrollToNodeAction
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.ui.FluidsPartsRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fluids, filters & parts: a card per row past oil, tires and wipers, each with its own checked editor. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class FluidsPartsScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private fun show(id: Long) {
        val vm = g.viewModel { FluidsPartsViewModel(routeHandle(FluidsPartsRoute(id)), g.db.vehicles(), g.knownTakes) }
        compose.setContent { GloveboxTheme { FluidsPartsScreen(onBack = {}, vm = vm) } }
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.fluids_title))).fetchSemanticsNodes().isNotEmpty() }
    }

    /** The card for [item], scrolled to in the list and opened. */
    private fun open(item: TakesItem) {
        val card = hasText(item.label) and hasClickAction()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(card)
        compose.onNode(card).click()
    }

    private fun field(label: Int) = compose.onNode(hasSetTextAction() and hasText(g.string(label)))
    private fun save() = compose.onNode(hasText(g.string(R.string.save)) and hasClickAction())

    @Test
    fun `a vehicle with nothing entered offers every row to add`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        compose.onNodeWithText(g.string(R.string.fluids_empty_body)).assertExists()
        compose.onNodeWithText(g.string(R.string.fluids_add_section)).assertExists()
    }

    @Test
    fun `a battery is saved in its standard form, and a figure out of range can't be`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        open(TakesItem.BATTERY)
        field(R.string.fluids_group).performTextInput("h6")
        field(R.string.fluids_cca).performTextInput("50")
        compose.onNodeWithText(g.string(R.string.fluids_cca_error)).assertExists()
        save().assertIsNotEnabled()
        field(R.string.fluids_cca).performTextClearance()
        field(R.string.fluids_cca).performTextInput("760")
        save().click()

        compose.awaitThat { g.db.vehicles().byId(id)?.batteryCca != null }
        val v = g.vehicle(id)!!
        assertEquals(VehicleSpecEntry.batteryGroup("h6"), v.batteryGroup)
        assertEquals(760, v.batteryCca)
        // Nothing else was written.
        assertNull(v.bulbLow)
        assertNull(v.oilSpec)
    }

    @Test
    fun `each row past oil, tires and wipers has its own editor that writes only its own columns`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        open(TakesItem.BULBS)
        field(R.string.fluids_low_beam).performTextInput("H7")
        field(R.string.fluids_fog).performTextInput("H11")
        save().click()
        compose.awaitThat { g.db.vehicles().byId(id)?.bulbLow != null }

        open(TakesItem.AIR_FILTERS)
        field(R.string.fluids_engine_air).performTextInput("17801-0P051")
        save().click()
        compose.awaitThat { g.db.vehicles().byId(id)?.engineAirFilter != null }

        open(TakesItem.SPARK_PLUGS)
        field(R.string.fluids_part_number).performTextInput("ILKAR7B11")
        save().click()
        compose.awaitThat { g.db.vehicles().byId(id)?.sparkPlug != null }

        val v = g.vehicle(id)!!
        assertEquals(VehicleSpecEntry.bulbCode("H7"), v.bulbLow)
        assertNull(v.bulbHigh)
        assertEquals(VehicleSpecEntry.bulbCode("H11"), v.bulbFog)
        assertEquals(VehicleSpecEntry.partNumbers("17801-0P051"), v.engineAirFilter)
        assertNull(v.cabinAirFilter)
        assertEquals(VehicleSpecEntry.partNumbers("ILKAR7B11"), v.sparkPlug)
    }

    @Test
    fun `cancelling an editor writes nothing`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        open(TakesItem.COOLANT)
        field(R.string.takes_coolant).performTextInput("Pink")
        compose.onNode(hasText(g.string(R.string.cancel)) and hasClickAction()).click()
        compose.waitForIdle()
        assertNull(g.vehicle(id)!!.coolantType)
    }

    @Test
    fun `a long press copies a row's part numbers, not marked sensitive`() {
        val id = g.add(VehicleFixtures.wagon.copy(cabinAirFilter = "87139-0N010"))
        show(id)
        val card = hasText(TakesItem.AIR_FILTERS.label) and hasClickAction()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(card)
        compose.onNode(card).performSemanticsAction(SemanticsActions.OnLongClick)
        val clip = g.context.getSystemService(ClipboardManager::class.java).primaryClip!!
        assertTrue(clip.getItemAt(0).text.contains("87139-0N010"))
        assertFalse(clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") ?: false)
    }

    @Test
    fun `a maker's list fills the empty rows, tagged until the owner says they checked`() {
        val id = g.add(VehicleFixtures.truck)
        show(id)
        val fill = hasText(g.string(R.string.known_takes_fill, "").trim(), substring = true) and hasClickAction()
        compose.awaitThat { compose.onAllNodes(fill).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(fill)
        compose.onNode(fill).click()
        compose.awaitThat { TakesItem.COOLANT in (g.db.vehicles().byId(id)?.takesToCheck() ?: emptySet()) }
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.torque_tag_double_check))).fetchSemanticsNodes().isNotEmpty() }

        open(TakesItem.COOLANT)
        compose.onNode(hasText(g.string(R.string.known_takes_checked)) and hasClickAction()).click()
        save().click()
        compose.awaitThat { TakesItem.COOLANT !in (g.db.vehicles().byId(id)?.takesToCheck() ?: setOf(TakesItem.COOLANT)) }
        // The others are still to check.
        assertTrue(g.vehicle(id)!!.takesToCheck().isNotEmpty())
    }
}
