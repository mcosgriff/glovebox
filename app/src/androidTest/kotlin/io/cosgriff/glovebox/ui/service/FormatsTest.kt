package io.cosgriff.glovebox.ui.service

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.NextDue
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.ui.inAppLocale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

/**
 * The shared formatters read their words from resources (#236). These are the English they wrote
 * before, word for word, which `EndToEndTest` and the baseline profile journeys depend on. It runs
 * on a device because the words, plural rules and date patterns come from Android.
 */
@RunWith(AndroidJUnit4::class)
class FormatsTest {
    private val mi = DistanceUnit.MILES
    private val km = DistanceUnit.KILOMETERS

    @Test
    fun distancesAndDates() {
        assertEquals("12,345 mi", 12_345.asDistance(mi))
        assertEquals("1,234,567 km", 1_234_567.asDistance(km))
        assertEquals("Oct 17, 2026", LocalDate.of(2026, 10, 17).format(longDate))
        assertEquals("Apr 3", LocalDate.of(2026, 4, 3).format(shortDate))
        assertEquals("Apr 2027", LocalDate.of(2027, 4, 3).format(monthYear))
        assertEquals("Miles", mi.titleWord)
        assertEquals("kilometres", km.pluralWord)
        assertEquals("km", km.symbolWord)
    }

    @Test
    fun intervals() {
        assertEquals("7,500 mi / 12 mo", Interval(7_500, 12).text(mi))
        assertEquals("30,000 mi", Interval(30_000, null).text(mi))
        assertEquals("36 mo", Interval(null, 36).text(mi))
        assertEquals("First at 100,000 mi, then every 50,000 mi / 60 mo", Interval(50_000, 60).text(mi, firstAt = 100_000))
        assertEquals("Every 7,500 mi or 12 mo", Interval(7_500, 12).every(mi))
        assertEquals("Every 36 mo", Interval(null, 36).every(mi))
    }

    @Test
    fun dueHeadlines() {
        assertEquals("Not logged yet", DueHeadline.NotLogged.text(mi))
        assertEquals("In 1,688 mi", DueHeadline.Miles(1_688).text(mi))
        assertEquals("Due now", DueHeadline.Miles(0).text(mi))
        assertEquals("Overdue 412 mi", DueHeadline.Miles(-412).text(mi))
        assertEquals("In 1 day", DueHeadline.Days(1).text(mi))
        assertEquals("Overdue 21 days", DueHeadline.Days(-21).text(mi))
        assertEquals("Due today", DueHeadline.Days(0).text(mi))
        assertEquals("In 5 months", DueHeadline.Days(151).text(mi))
        val nextYear = LocalDate.now().year + 1
        assertEquals(
            "Snoozed until 70,100 mi or Apr 10, $nextYear",
            DueHeadline.Snoozed(Snooze(70_100, LocalDate.of(nextYear, 4, 10))).text(mi),
        )
    }

    @Test
    fun nextDueAndChanges() {
        assertEquals("73,412 mi or Apr 10, 2027", NextDue(73_412, LocalDate.of(2027, 4, 10)).text(mi))
        val oil = TrackedItem(1, "Oil & filter", Interval(5_000, 6), DueRule.WHICHEVER_FIRST)
        assertEquals(
            "Oil & filter — next due 55,000 mi or Apr 10, 2027",
            ReminderChange(oil, null, LastDone(LocalDate.of(2026, 10, 10), 50_000)).text(mi),
        )
        assertEquals("Oil & filter — back to not logged yet", ReminderChange(oil, LastDone(LocalDate.of(2026, 10, 10), 50_000), null).text(mi))
        assertEquals("Whichever comes first", DueRule.WHICHEVER_FIRST.ruleLabel())
    }

    @Test
    fun englishOnAnotherPhoneKeepsEnglishPluralsAndFigures() {
        // A Russian phone gets the English text; Russian plural rules would make that "21 day".
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val russian = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru-RU")) })
        val res = russian.inAppLocale().resources
        assertEquals("21 days", res.getQuantityString(R.plurals.days, 21, "21"))
        assertEquals(Locale.forLanguageTag("en-US"), res.configuration.locales[0])
    }
}
