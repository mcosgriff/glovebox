package io.cosgriff.glovebox.ui.components

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.titleWord
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class DistanceUnitSwitchTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the vehicle's unit is shown chosen, and the other is one tap away`() {
        val picked = mutableListOf<DistanceUnit>()
        compose.draw { DistanceUnitSwitch(DistanceUnit.MILES, onPick = { picked += it }) }
        compose.onNodeWithText(DistanceUnit.MILES.titleWord).assertIsSelected()
        compose.onNodeWithText(DistanceUnit.KILOMETERS.titleWord).performClick()
        assertEquals(listOf(DistanceUnit.KILOMETERS), picked)
    }
}
