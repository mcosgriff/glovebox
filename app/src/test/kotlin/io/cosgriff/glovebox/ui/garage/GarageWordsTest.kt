package io.cosgriff.glovebox.ui.garage

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Attention
import io.cosgriff.glovebox.domain.ComingUp
import io.cosgriff.glovebox.domain.DecidedBy
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.DueState
import io.cosgriff.glovebox.domain.DueStatus
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.ItemStatus
import io.cosgriff.glovebox.domain.ReminderProgress
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.until
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class GarageWordsTest {
    private val res: Resources = ApplicationProvider.getApplicationContext<ScreenshotApp>().resources

    private fun status(name: String, state: DueState = DueState.OK) = ItemStatus(
        TrackedItem(1, name, Interval(5_000, 6), DueRule.WHICHEVER_FIRST),
        lastDone = null,
        status = DueStatus(state, null, null, null, null, null, DecidedBy.MILEAGE),
    )

    private fun next(headline: DueHeadline) = ComingUp(listOf(status("Oil & filter change") to ReminderProgress(headline, 0.5)), emptyList())

    @Test
    fun `the chip names a single reminder, counts several, and says how many are still to log`() {
        assertEquals(res.getString(R.string.item_overdue, "Tire rotation"), Attention(DueState.OVERDUE, 1, "Tire rotation").label(res))
        assertEquals(res.getQuantityString(R.plurals.count_overdue, 3, "3"), Attention(DueState.OVERDUE, 3, null).label(res))
        assertEquals(res.getString(R.string.item_due_soon, "Cabin air filter"), Attention(DueState.DUE_SOON, 1, "Cabin air filter").label(res))
        assertEquals(res.getQuantityString(R.plurals.count_due_soon, 2, "2"), Attention(DueState.DUE_SOON, 2, null).label(res))
        assertEquals(res.getString(R.string.up_to_date), Attention(DueState.OK, 0, null).label(res))
        // Jobs never logged outrank "Up to date": there is nothing to be up to date on yet.
        assertEquals(res.getQuantityString(R.plurals.count_to_log, 4, "4"), Attention(DueState.OK, 0, null, toLog = 4).label(res))
    }

    @Test
    fun `the next line is in the vehicle's own unit and says which way it is counting`() {
        val name = "Oil & filter change"
        assertEquals(
            res.getString(R.string.garage_next_in, name, 1_688.asDistance(DistanceUnit.KILOMETERS)),
            next(DueHeadline.Miles(1_688)).nextLine(res, DistanceUnit.KILOMETERS),
        )
        assertEquals(
            res.getString(R.string.garage_next_overdue, name, 40.asDistance(DistanceUnit.MILES)),
            next(DueHeadline.Miles(-40)).nextLine(res, DistanceUnit.MILES),
        )
        assertEquals(res.getString(R.string.garage_next_due_now, name), next(DueHeadline.Miles(0)).nextLine(res, DistanceUnit.MILES))
        assertEquals(res.getString(R.string.garage_next_due_today, name), next(DueHeadline.Days(0)).nextLine(res, DistanceUnit.MILES))
        val snooze = Snooze(untilOdometer = 64_000)
        assertEquals(
            res.getString(R.string.garage_next_snoozed, name, snooze.until(DistanceUnit.MILES)),
            next(DueHeadline.Snoozed(snooze)).nextLine(res, DistanceUnit.MILES),
        )
        assertEquals(res.getString(R.string.garage_next_not_logged, name), next(DueHeadline.NotLogged).nextLine(res, DistanceUnit.MILES))
    }

    @Test
    fun `with nothing coming up the line says why`() {
        // Only overdue reminders: nothing is "next".
        val overdue = ComingUp(listOf(status("Brake fluid", DueState.OVERDUE) to ReminderProgress(DueHeadline.Days(-3), 1.0)), emptyList())
        assertEquals(res.getString(R.string.garage_nothing_else), overdue.nextLine(res, DistanceUnit.MILES))
        val toLog = ComingUp(emptyList(), listOf(status("Coolant"), status("Spark plugs")))
        assertEquals(res.getQuantityString(R.plurals.garage_not_logged, 2, "2"), toLog.nextLine(res, DistanceUnit.MILES))
        assertEquals(res.getString(R.string.garage_none_tracked), ComingUp(emptyList(), emptyList()).nextLine(res, DistanceUnit.MILES))
    }
}
