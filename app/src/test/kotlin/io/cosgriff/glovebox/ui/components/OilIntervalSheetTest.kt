package io.cosgriff.glovebox.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.CountedInterval
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.titleWord
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The owner's own oil change interval (#517). */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class OilIntervalSheetTest {
    @get:Rule val compose = createComposeRule()

    private val maker = OilChange(CountedInterval(Interval(10_000, 12), monthsWorkedOut = true), OilSource.MAKER)
    private val saved = mutableListOf<Interval?>()

    private fun draw(own: Interval?) = compose.draw {
        OilIntervalSheet(maker, own, DistanceUnit.MILES, onSave = { saved += it }, onDismiss = {})
    }

    private fun months(n: Int) = Components.context.resources.getQuantityString(R.plurals.months, n, n.toString())

    @Test
    fun `months left blank follow the miles, and are saved as following`() {
        draw(own = null)
        compose.onNodeWithText(Components.string(R.string.oil_months_blank)).assertExists()
        compose.onNodeWithText(DistanceUnit.MILES.titleWord).performTextInput("7500")
        compose.onNodeWithText(Components.string(R.string.oil_months_follow, months(9))).assertExists()
        compose.onNodeWithText(Components.string(R.string.save)).performScrollTo().performClick()
        assertEquals(listOf(Interval(7_500, null)), saved)
    }

    @Test
    fun `typed months are saved as typed`() {
        draw(own = null)
        compose.onNodeWithText(DistanceUnit.MILES.titleWord).performTextInput("7500")
        compose.onNodeWithText(Components.string(R.string.item_months)).performTextInput("12")
        compose.onNodeWithText(Components.string(R.string.oil_months_typed)).assertExists()
        compose.onNodeWithText(Components.string(R.string.save)).performScrollTo().performClick()
        assertEquals(listOf(Interval(7_500, 12)), saved)
    }

    @Test
    fun `nothing typed is not saved`() {
        draw(own = null)
        compose.onNodeWithText(Components.string(R.string.save)).performScrollTo().performClick()
        assertEquals(emptyList<Interval?>(), saved)
        compose.onNodeWithText(Components.string(R.string.oil_use_maker)).assertDoesNotExist()
    }

    @Test
    fun `Use the maker's clears the owner's interval`() {
        draw(own = Interval(5_000, null))
        compose.onNodeWithText(Components.string(R.string.oil_use_maker)).performScrollTo().performClick()
        assertEquals(listOf<Interval?>(null), saved)
    }
}
