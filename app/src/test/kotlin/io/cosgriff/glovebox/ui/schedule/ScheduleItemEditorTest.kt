package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.domain.nextDueAfter
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.titleWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class ScheduleItemEditorTest {
    @get:Rule val compose = createComposeRule()

    private val saved = mutableListOf<DraftItem>()
    private var stopped = 0
    private var closed = 0
    private val unit = DistanceUnit.MILES
    private val oil = ScheduleTemplates.generic("FWD", "Gasoline", unit).first { it.key == "oil" }

    private fun open(item: DraftItem, isNew: Boolean = false, history: ItemHistory? = null) = compose.show {
        ScheduleItemEditor(
            item = item, isNew = isNew, defaultRule = DueRule.WHICHEVER_FIRST, unit = unit,
            onSave = { saved += it }, onStopTracking = { stopped++ }, onClose = { closed++ }, history = history,
        )
    }

    /** The distance boxes are both labelled with the unit: the interval's first, then the first time's. */
    private fun distanceBoxes(): SemanticsNodeInteractionCollection = compose.onAllNodes(hasSetTextAction() and hasText(unit.titleWord))

    private fun save() = compose.onNodeWithText(text(R.string.save)).performClick()

    @Test
    fun `a new item needs a name and an interval, and a first time only if it is a whole distance`() {
        open(DraftItem.blank("custom-0", unit), isNew = true)
        compose.onNodeWithText(text(R.string.item_add_title)).assertIsDisplayed()
        // The interval is the user's to say: the boxes start empty.
        distanceBoxes().onFirst().assert(hasText(""))
        compose.onNodeWithText(text(R.string.item_stop_tracking)).assertDoesNotExist()

        save()
        assertTrue(saved.isEmpty())
        compose.onNodeWithText(text(R.string.documents_give_name)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.item_interval_error, "miles"), substring = true).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.documents_name)).performTextInput("  Grease hitch ")
        compose.onNodeWithText(text(R.string.item_months)).performTextInput("4")
        distanceBoxes().onLast().performTextInput("abc")
        save()
        assertTrue(saved.isEmpty())
        compose.onNodeWithText(text(R.string.item_first_error, "miles"), substring = true).performScrollTo().assertIsDisplayed()

        distanceBoxes().onLast().performTextReplacement("12,000")
        save()
        val item = saved.single()
        assertEquals("Grease hitch", item.name)
        assertEquals(Interval(null, 4), item.normal)
        assertEquals(12_000, item.firstAt)
    }

    @Test
    fun `an item of the user's own can be pointed at one of Glovebox's service lines, and given its own rule`() {
        open(DraftItem.blank("custom-0", unit).copy(name = "Belts"), isNew = false)
        compose.onNodeWithText(text(R.string.item_service_pick)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.item_service_own)).performScrollTo().performClick()
        compose.onAllNodesWithText(catalogLabel("belt-check")).onFirst().performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.item_service_chosen, catalogLabel("belt-check"))).performScrollTo().assertIsDisplayed()

        compose.onNodeWithText(text(R.string.item_rule_default, text(R.string.rule_whichever_first_sentence))).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.rule_mileage_only)).performClick()

        save()
        val item = saved.single()
        assertEquals("belt-check", item.service)
        assertEquals(DueRule.MILEAGE_ONLY, item.rule)
        // Saved with the interval it opened with.
        assertEquals(Interval(5_000, 6), item.normal)

        compose.onNodeWithText(text(R.string.item_stop_tracking)).performScrollTo().performClick()
        assertEquals(1, stopped)
        compose.onNodeWithContentDescription(text(R.string.close)).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `a typical item's severe interval is edited in the same boxes, and can be reset to the suggestion`() {
        val item = DraftItem(key = "1", name = oil.label, normal = oil.normal, severe = oil.severe, template = oil)
        open(item)
        // A typical item answers through its own line, so no picker.
        compose.onNodeWithText(text(R.string.item_resets_it)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.item_severe_typical, oil.severe!!.text(unit))).performScrollTo().assertIsDisplayed()

        compose.onNodeWithText(text(R.string.item_severe_interval)).performScrollTo()
        compose.onAllNodes(androidx.compose.ui.test.isToggleable()).onFirst().performClick()
        // The boxes now show the severe interval.
        distanceBoxes().onFirst().assert(hasText("5,000"))
        distanceBoxes().onFirst().performTextReplacement("4,000")
        save()
        assertEquals(oil.normal, saved.last().normal)
        assertEquals(Interval(4_000, 6), saved.last().severe)
        assertTrue(saved.last().useSevere)

        compose.onNodeWithText(text(R.string.item_reset)).performScrollTo().performClick()
        distanceBoxes().onFirst().assert(hasText("5,000"))
        save()
        assertEquals(oil.severe, saved.last().severe)
    }

    @Test
    fun `a flex-fuel item's E85 interval is edited while it is on`() {
        val item = DraftItem(key = "2", name = "Replace engine oil", normal = Interval(10_000, 12), severe = null, own = true, e85 = Interval(5_000, 6))
        open(item)
        compose.onNodeWithText(text(R.string.item_e85_for)).performScrollTo().assertIsDisplayed()
        compose.onAllNodes(androidx.compose.ui.test.isToggleable()).onFirst().performClick()
        distanceBoxes().onFirst().assert(hasText("5,000"))
        distanceBoxes().onFirst().performTextReplacement("3,000")
        save()
        val out = saved.single()
        assertEquals(Interval(10_000, 12), out.normal)
        assertEquals(Interval(3_000, 6), out.e85)
        assertTrue(out.useE85)
    }

    @Test
    fun `a job only for severe use says so, and keeps whether it is on`() {
        val item = DraftItem(key = "3", name = "Lubricate propeller shaft", normal = Interval(5_000, 6), severe = null, own = true, severeOnly = true, useSevere = true)
        open(item)
        compose.onNodeWithText(text(R.string.item_severe_only_note)).performScrollTo().assertIsDisplayed()
        save()
        assertTrue(saved.single().useSevere)
    }

    @Test
    fun `an item never logged asks for its last done, and says nothing is due yet`() {
        var asked = 0
        open(
            DraftItem(key = "1", name = oil.label, normal = oil.normal, severe = oil.severe, template = oil),
            history = ItemHistory(lastDone = null, onOpenRecord = null, onEnterLastDone = { asked++ }, resetBy = listOf(catalogLabel("oil")), estimate = null),
        )
        compose.onNodeWithText(text(R.string.item_last_done)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.item_log_once)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(catalogLabel("oil")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.item_enter_it)).performScrollTo().performClick()
        assertEquals(1, asked)
    }

    @Test
    fun `an item with a record shows it, counts the next due from what is typed, and drops the estimate while editing`() {
        var opened = 0
        val last = LastDone(LocalDate.of(2026, 2, 1), 40_000)
        val estimate = LocalDate.of(2026, 6, 1)
        open(
            DraftItem(key = "4", name = "Synthetic oil", normal = Interval(10_000, 12), severe = null, own = true),
            history = ItemHistory(lastDone = last, onOpenRecord = { opened++ }, entered = true, resetBy = emptyList(), estimate = estimate),
        )
        compose.onNodeWithText(text(R.string.details_entered_by_you)).performScrollTo().performClick()
        assertEquals(1, opened)
        compose.onNodeWithText(text(R.string.item_service_own_log)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(Interval(10_000, 12).nextDueAfter(last).text(unit)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.item_estimate, estimate.format(longDate)), substring = true).performScrollTo().assertIsDisplayed()

        distanceBoxes().onFirst().performTextReplacement("8,000")
        compose.onNodeWithText(Interval(8_000, 12).nextDueAfter(last).text(unit)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.item_estimate, estimate.format(longDate)), substring = true).assertDoesNotExist()

        distanceBoxes().onFirst().performTextClearance()
        compose.onNodeWithText(text(R.string.item_months)).performTextClearance()
        compose.onNodeWithText(text(R.string.item_enter_interval)).performScrollTo().assertIsDisplayed()
        assertNull(saved.firstOrNull())
    }

    @Test
    fun `a record from the log is said to be from it`() {
        val last = LastDone(LocalDate.of(2026, 2, 1), 40_000)
        open(
            DraftItem(key = "5", name = "Tire rotation", normal = Interval(5_000, null), severe = null, own = true),
            history = ItemHistory(lastDone = last, onOpenRecord = {}, resetBy = listOf(catalogLabel("rotation"), catalogLabel("oil")), estimate = null),
        )
        compose.onNodeWithText(text(R.string.from_your_log)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(plural(R.plurals.item_resets, 2)).performScrollTo().assertIsDisplayed()
    }
}
