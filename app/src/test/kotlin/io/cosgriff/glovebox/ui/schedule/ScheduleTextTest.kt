package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.JobProblem
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.RepeatConflict
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The wording `:domain`'s typed results are given here, each figure in the vehicle's own unit. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ScheduleTextTest {
    private val km = DistanceUnit.KILOMETERS

    @Test
    fun `a repeat conflict says what the vehicle's table does and what the file's does`() {
        assertEquals(
            text(R.string.repeat_conflict, text(R.string.repeat_says_every, 48_000.asDistance(km)), text(R.string.repeat_says_not)),
            RepeatConflict(saved = 48_000, file = null).text(km),
        )
        assertEquals(
            text(R.string.repeat_conflict, text(R.string.repeat_says_not), text(R.string.repeat_says_every, 96_000.asDistance(km))),
            RepeatConflict(saved = null, file = 96_000).text(km),
        )
    }

    @Test
    fun `the import page's note says whether the table repeats`() {
        assertEquals(text(R.string.repeat_note_every, 48_000.asDistance(km)), repeatNote(48_000, km))
        assertEquals(text(R.string.repeat_note_ends), repeatNote(null, km))
    }

    @Test
    fun `a job's problems are each worded with the limit they break`() {
        assertEquals(text(R.string.milestone_job_too_long, MilestoneImport.MAX_NAME.toString()), JobProblem.TooLong.text)
        assertEquals(text(R.string.milestone_job_one_line), JobProblem.NotOneLine.text)
    }

    @Test
    fun `with no credit a maker's list is still said to be the maker's guide`() {
        assertEquals(text(R.string.known_started_from, text(R.string.known_makers_guide)), startedFrom(null))
    }
}
