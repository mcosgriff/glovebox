package io.cosgriff.glovebox.ui.torque

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertCountEquals
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueSource
import io.cosgriff.glovebox.domain.TorqueSpecs
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = "w411dp-h2000dp-xhdpi", sdk = [35])
class SpecEditorSheetTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val saved = mutableListOf<SpecDraft>()
    private var deleted = 0
    private var dismissed = 0

    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private fun show(spec: TorqueSpecEntity?, unit: TorqueUnit = TorqueUnit.NEWTON_METER, listSource: String? = null) = compose.setContent {
        GloveboxTheme {
            SpecEditorSheet(spec, listSource, unit, TorqueCategory.WHEELS, onSave = { saved += it }, onDelete = { deleted++ }, onDismiss = { dismissed++ })
        }
    }

    private fun field(@StringRes label: Int, vararg args: Any) = compose.onNode(hasSetTextAction() and hasText(str(label, *args)))

    private fun torqueField(unit: TorqueUnit) = field(R.string.torque_in, unit.symbolWord)

    private fun save() = compose.onNodeWithText(str(R.string.save)).performScrollTo().performClick()

    @Test
    fun `a value typed in ft-lb is saved in N·m, and nothing is chosen for a new spec but the category it starts in`() {
        show(null, TorqueUnit.FOOT_POUND)
        compose.onNodeWithText(str(R.string.torque_add_title)).assertExists()
        compose.onNodeWithText(str(R.string.save)).assertIsNotEnabled()
        field(R.string.torque_fastener).performTextInput("  Made-up bolt  ")
        torqueField(TorqueUnit.FOOT_POUND).performTextInput("50")
        compose.onNodeWithText(str(R.string.torque_equals, "${Torque.of(50.0, TorqueUnit.FOOT_POUND).text(TorqueUnit.NEWTON_METER)} · ${Torque.of(50.0, TorqueUnit.FOOT_POUND).text(TorqueUnit.INCH_POUND)}")).assertExists()
        save()
        val draft = saved.single()
        assertEquals("Made-up bolt", draft.name)
        assertEquals(TorqueCategory.WHEELS, draft.category)
        assertEquals(50 * TorqueUnit.FOOT_POUND.newtonMetersPerUnit, draft.torque!!.newtonMeters, 1e-9)
        assertEquals(TorqueSource.USER, draft.source)
        assertNull(draft.socketSize)
        // A new spec has nothing to delete.
        compose.onAllNodesWithText(str(R.string.torque_delete)).assertCountEquals(0)
    }

    @Test
    fun `more than 1,000 N·m is refused, in whatever unit it is typed`() {
        show(null)
        field(R.string.torque_fastener).performTextInput("Made-up bolt")
        torqueField(TorqueUnit.NEWTON_METER).performTextInput("1001")
        compose.onNodeWithText(str(R.string.torque_bad, Torque(TorqueSpecs.MAX_NEWTON_METERS).text(TorqueUnit.NEWTON_METER))).assertExists()
        compose.onNodeWithText(str(R.string.save)).assertIsNotEnabled()

        // The switch says what the number means; it doesn't convert it. 1001 in-lb is about 113 N·m.
        compose.onNodeWithText(str(R.string.torque_unit_in_lb)).performClick()
        compose.onNodeWithText(str(R.string.save)).assertIsEnabled()
        save()
        assertEquals(1001 * TorqueUnit.INCH_POUND.newtonMetersPerUnit, saved.single().torque!!.newtonMeters, 1e-9)
    }

    @Test
    fun `a value left empty is saved for later, with the details given`() {
        show(null)
        field(R.string.torque_fastener).performTextInput("Made-up bolt")
        compose.onNodeWithText(str(R.string.torque_leave_empty)).assertExists()
        compose.onNodeWithText(TorqueCategory.DRIVELINE.label).performClick()
        field(R.string.torque_socket).performTextInput("19 mm")
        field(R.string.torque_note_label).performTextInput("Made-up note")
        // "From the factory manual" means nothing without a value.
        compose.onNodeWithText(str(R.string.torque_factory)).performScrollTo().performClick()
        save()
        val draft = saved.single()
        assertEquals(TorqueCategory.DRIVELINE, draft.category)
        assertEquals("19 mm", draft.socketSize)
        assertEquals("Made-up note", draft.note)
        assertNull(draft.torque)
        assertEquals(TorqueSource.USER, draft.source)
    }

    private val reference = TorqueSpecEntity(
        id = 7, vehicleId = 1, name = "Made-up nut", category = TorqueCategory.WHEELS.name,
        newtonMeters = 111.1, source = TorqueSource.REFERENCE.name, knownList = "made-up-list",
    )

    @Test
    fun `a reference value stays one until it is checked or changed`() {
        show(reference, listSource = "A made-up source")
        compose.onNodeWithText(str(R.string.torque_edit_title)).assertExists()
        compose.onNodeWithText(str(R.string.torque_checked_note, "A made-up source")).assertExists()
        save()
        assertEquals(TorqueSource.REFERENCE, saved.last().source)
        assertEquals(7L, saved.last().id)
    }

    @Test
    fun `checking a reference value makes it the user's`() {
        show(reference, listSource = "A made-up source")
        compose.onNodeWithText(str(R.string.torque_checked)).performScrollTo().performClick()
        save()
        assertEquals(TorqueSource.USER, saved.single().source)
    }

    @Test
    fun `changing a reference value makes it the user's, and from the factory manual makes it FACTORY`() {
        show(reference, listSource = "A made-up source")
        torqueField(TorqueUnit.NEWTON_METER).performTextReplacement("120")
        compose.onNodeWithText(str(R.string.torque_factory)).performScrollTo().performClick()
        save()
        assertEquals(TorqueSource.FACTORY, saved.single().source)
        assertEquals(120.0, saved.single().torque!!.newtonMeters, 1e-9)
    }

    @Test
    fun `a saved spec can be deleted`() {
        show(reference.copy(source = TorqueSource.USER.name, knownList = null))
        compose.onAllNodesWithText(str(R.string.torque_checked)).assertCountEquals(0)
        compose.onNodeWithText(str(R.string.torque_delete)).performScrollTo().performClick()
        assertEquals(1, deleted)
    }
}
