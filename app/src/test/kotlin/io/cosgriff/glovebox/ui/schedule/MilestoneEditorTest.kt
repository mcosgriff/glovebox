package io.cosgriff.glovebox.ui.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.TableJob
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.catalogLabel
import io.cosgriff.glovebox.ui.service.titleWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The editor for one milestone of the user's own table: the same rules as a table read from a file. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class MilestoneEditorTest {
    @get:Rule val compose = createComposeRule()

    private val saved = mutableListOf<ImportedMilestone>()
    private var deleted = 0
    private var closed = 0
    private val unit = DistanceUnit.MILES
    private val fresh = MilestoneDraft(id = null, atMiles = null, orMonths = null, jobs = emptyList())

    private fun open(draft: MilestoneDraft, others: List<Int> = emptyList(), repeats: Int? = null, startsTable: Boolean = false, endsTable: Boolean = false) = compose.show {
        MilestoneEditor(
            draft = draft, others = others, repeatsEveryMiles = repeats, unit = unit, startsTable = startsTable, endsTable = endsTable,
            onSave = { saved += it }, onDelete = { deleted++ }, onClose = { closed++ },
        )
    }

    private fun miles() = compose.onNode(hasSetTextAction() and hasText(unit.titleWord))
    private fun jobs() = compose.onAllNodes(hasSetTextAction() and hasText(text(R.string.milestone_job)))
    private fun save() = compose.onNodeWithText(text(R.string.save)).performClick()
    private fun problem(message: String) = compose.onNodeWithText(text(R.string.sentence, message)).performScrollTo().assertIsDisplayed()

    @Test
    fun `a new milestone needs a mileage and a named job, and then saves as typed`() {
        open(fresh, startsTable = true)
        compose.onNodeWithText(text(R.string.milestone_add_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.milestone_starts_table)).assertIsDisplayed()
        // A new one is opened with one empty job and nothing to delete.
        assertEquals(1, jobs().fetchSemanticsNodes().size)
        compose.onNodeWithText(text(R.string.milestone_delete)).assertDoesNotExist()

        save()
        assertTrue(saved.isEmpty())
        problem(text(R.string.milestone_miles_bad, "1,000,000"))
        compose.onNodeWithText(text(R.string.milestone_job_no_name), substring = true).performScrollTo().assertIsDisplayed()

        miles().performTextInput("30,000")
        compose.onNodeWithText(text(R.string.milestone_or_months)).performTextInput("36")
        jobs().onFirst().performTextInput("Inspect brake pads")
        save()
        assertEquals(ImportedMilestone(30_000, listOf(TableJob("Inspect brake pads", null)), 36), saved.single())
    }

    @Test
    fun `a mileage already in the table, past the cycle or months out of range are refused`() {
        open(fresh, others = listOf(30_000), repeats = 60_000)
        compose.onNodeWithText(text(R.string.milestone_due_note_repeats, unit.titleWord, "60,000 mi")).assertIsDisplayed()
        jobs().onFirst().performTextInput("Rotate tires")

        miles().performTextInput("30000")
        save()
        problem(text(R.string.milestone_miles_taken, "30,000 mi"))

        miles().performTextReplacement("70000")
        save()
        problem(text(R.string.milestone_miles_past_cycle, "60,000 mi"))

        miles().performTextReplacement("45000")
        compose.onNodeWithText(text(R.string.milestone_or_months)).performTextInput("9999")
        save()
        problem(text(R.string.milestone_months_bad, MilestoneImport.MAX_MONTHS.toString()))
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `jobs are added, moved, removed and tied to a service line, each once`() {
        open(fresh)
        miles().performTextInput("15000")
        jobs().onFirst().performTextInput("Replace cabin filter")
        compose.onNodeWithText(text(R.string.milestone_add_job)).performScrollTo().performClick()
        jobs().onLast().performTextInput("Replace cabin filter")
        save()
        compose.onNodeWithText(text(R.string.milestone_job_repeated), substring = true).performScrollTo().assertIsDisplayed()

        jobs().onLast().performTextReplacement("Rotate tires")
        // Both completed by the same line is refused too.
        compose.onAllNodesWithText(text(R.string.milestone_no_service)).onFirst().performScrollTo().performClick()
        compose.onAllNodesWithText(catalogLabel("rotation")).onFirst().performScrollTo().performClick()
        compose.onAllNodesWithText(text(R.string.milestone_no_service)).onFirst().performScrollTo().performClick()
        compose.onAllNodesWithText(catalogLabel("rotation")).onLast().performScrollTo().performClick()
        save()
        compose.onNodeWithText(text(R.string.milestone_job_service_taken, catalogLabel("rotation")), substring = true).performScrollTo().assertIsDisplayed()

        // The first job can't go up, nor the last down. Remove the first, add it back at the end and move it up.
        compose.onAllNodesWithContentDescription(text(R.string.move_up)).onFirst().assertIsNotEnabled()
        compose.onAllNodesWithContentDescription(text(R.string.move_down)).onLast().assertIsNotEnabled()
        compose.onAllNodesWithText(text(R.string.remove)).onFirst().performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.milestone_add_job)).performScrollTo().performClick()
        jobs().onLast().performTextInput("Replace cabin filter")
        compose.onAllNodesWithContentDescription(text(R.string.move_up)).onLast().performScrollTo().performClick()
        save()
        assertEquals(listOf(TableJob("Replace cabin filter", null), TableJob("Rotate tires", "rotation")), saved.single().work)
    }

    @Test
    fun `a milestone with no jobs left is refused, and at the most jobs no more can be added`() {
        open(MilestoneDraft(id = 4, atMiles = 60_000, orMonths = null, jobs = (1..MilestoneImport.MAX_WORK).map { TableJob("Job $it", null) }))
        compose.onNodeWithText(text(R.string.milestone_add_job)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.sentence, text(R.string.milestone_work_too_many, MilestoneImport.MAX_WORK.toString()))).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `removing the only job leaves nothing to save`() {
        open(MilestoneDraft(id = 4, atMiles = 60_000, orMonths = null, jobs = listOf(TableJob("Replace coolant", null))))
        compose.onAllNodesWithText(text(R.string.remove)).onFirst().performScrollTo().performClick()
        save()
        problem(text(R.string.milestone_work_none))
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `close with nothing changed just closes, and with changes asks first`() {
        open(fresh)
        compose.onNodeWithContentDescription(text(R.string.close)).performClick()
        assertEquals(1, closed)

        miles().performTextInput("5000")
        compose.onNodeWithContentDescription(text(R.string.close)).performClick()
        compose.onNodeWithText(text(R.string.milestone_discard_new)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.milestone_discard_new_body)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.keep_editing)).performClick()
        assertEquals(1, closed)
        compose.onNodeWithContentDescription(text(R.string.close)).performClick()
        compose.onNodeWithText(text(R.string.discard)).performClick()
        assertEquals(2, closed)
    }

    @Test
    fun `an existing milestone is deleted only once confirmed, and says when the table goes with it`() {
        open(MilestoneDraft(id = 4, atMiles = 60_000, orMonths = 72, jobs = listOf(TableJob("Replace coolant", null))), endsTable = true)
        compose.onNodeWithText(text(R.string.milestone_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.milestone_delete)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.milestone_delete_last)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(0, deleted)
        compose.onNodeWithText(text(R.string.milestone_delete)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.delete)).performClick()
        assertEquals(1, deleted)

        // A change to an existing one is "discard your changes".
        compose.onNodeWithText(text(R.string.milestone_or_months)).performTextReplacement("")
        compose.onNodeWithContentDescription(text(R.string.close)).performClick()
        compose.onNodeWithText(text(R.string.discard_changes)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.milestone_discard_body)).assertIsDisplayed()
    }

    @Test
    fun `deleting one of several says only the milestone goes`() {
        open(MilestoneDraft(id = 4, atMiles = 60_000, orMonths = null, jobs = listOf(TableJob("Replace coolant", null))), others = listOf(30_000))
        compose.onNodeWithText(text(R.string.milestone_delete)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.milestone_delete_body)).assertIsDisplayed()
    }
}
