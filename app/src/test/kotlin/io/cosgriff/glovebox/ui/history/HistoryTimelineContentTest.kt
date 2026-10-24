package io.cosgriff.glovebox.ui.history

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.HistoryOrder
import io.cosgriff.glovebox.domain.Money
import io.cosgriff.glovebox.domain.format
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** History's timeline through [HistoryContent], from made-up [Fixtures]: what it shows and where a tap goes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class HistoryTimelineContentTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = context.resources.getQuantityString(id, n, *args)

    private val opened = mutableListOf<Long>()
    private val logged = mutableListOf<Pair<Long, Long>>()
    private val views = mutableListOf<HistoryView>()

    private fun show(state: HistoryState = Fixtures.historyTimeline, start: HistoryView = HistoryView.TIMELINE, filter: String? = null, archived: Boolean = false) {
        compose.setContent {
            var view by remember { mutableStateOf(start) }
            GloveboxTheme {
                HistoryContent(
                    state, chosenFilter = filter, order = HistoryOrder(), fresh = null, archived = archived, today = Fixtures.TODAY,
                    onOpenRecord = { opened += it }, onBack = null, onExport = {}, onChooseVehicle = {}, onChooseFilter = {}, onChooseOrder = {},
                    view = view, onChooseView = { views += it; view = it }, onLogIt = { v, item -> logged += v to item },
                )
            }
        }
    }

    @Test
    fun `the switch goes between the list and the timeline, and the list's spending line and sorting stay with the list`() {
        show(start = HistoryView.LIST)
        val total = Money.format(Fixtures.historyTimeline.records.sumOf { it.record.costCents ?: 0 })
        compose.onNodeWithText(total, substring = true).assertExists()
        compose.onNodeWithContentDescription(string(R.string.history_sort_by)).assertExists()
        compose.onNodeWithText(string(R.string.history_view_list)).assertIsSelected()

        compose.onNodeWithText(string(R.string.history_view_timeline)).performClick()
        assertEquals(listOf(HistoryView.TIMELINE), views)
        compose.onNodeWithText(string(R.string.history_view_timeline)).assertIsSelected()
        compose.onNodeWithText(string(R.string.history_view_list)).assertIsNotSelected()
        compose.onNodeWithText(total, substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription(string(R.string.history_sort_by)).assertDoesNotExist()
    }

    @Test
    fun `the next reminder is the garage's next, and a tap on it is Log service with that job`() {
        show()
        val (status, progress) = Fixtures.historyTimeline.next!!
        compose.onNodeWithText(progress.headline.text(Fixtures.truck.unit), substring = true).assertExists()
        // The job's name is on records too; the card is the one that says it is coming up.
        compose.onNode(hasText(string(R.string.history_timeline_coming_up)) and hasText(status.item.name)).performClick()
        assertEquals(listOf(Fixtures.truck.id to status.item.id), logged)
    }

    @Test
    fun `gaps say the whole months between records, and under a month when there isn't one`() {
        show()
        // Mar 15 and Feb 15, 2026; then May 15, 2025, and three weeks before it.
        compose.onNodeWithText(plural(R.plurals.history_timeline_apart, 1, 1.asNumber())).assertExists()
        compose.onNodeWithText(plural(R.plurals.history_timeline_apart, 9, 9.asNumber())).assertExists()
    }

    @Test
    fun `a tapped card opens its record`() {
        show()
        val newest = Fixtures.historyTimeline.records.first()
        compose.onNodeWithText(newest.items.first().label).performClick()
        assertEquals(listOf(newest.record.id), opened)
    }

    @Test
    fun `a category filter keeps only matching records and leaves the next reminder out`() {
        show(filter = "brakes")
        compose.onNodeWithText(string(R.string.history_timeline_coming_up)).assertDoesNotExist()
        compose.onNodeWithText(Fixtures.historyTimeline.records.first().items.first().label).assertDoesNotExist()
    }

    @Test
    fun `with no records there is no switch`() {
        show(state = Fixtures.historyTimeline.copy(records = emptyList()))
        compose.onNodeWithText(string(R.string.history_view_timeline)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.history_none)).assertExists()
    }
}
