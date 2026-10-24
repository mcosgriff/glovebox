package io.cosgriff.glovebox.ui.settings

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.MileageCheckIns
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.notify.Notifier
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Settings → Notifications: what Android allows, each kind's switch, and when they come. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class NotificationsScreenTest {
    @get:Rule(order = 1) val compose = createComposeRule()

    @get:Rule(order = 0) val g = TestGraph()

    init {
        runBlocking { g.resetSettings() }
    }


    private fun saved(): Settings = runBlocking { g.settings.settings.first() }

    private fun show(onOpenRules: () -> Unit = {}) {
        val vm = NotificationsViewModel(g.settings, Notifier(g.context))
        compose.show { NotificationsScreen(onBack = {}, onOpenRules = onOpenRules, vm = vm) }
    }

    private fun switch(title: String) = compose.onNode(isToggleable() and hasText(title)).performScrollTo()

    @Test
    fun `refused by Android, the screen says so and offers to ask`() {
        shadowOf(g.context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        show()
        compose.waitForText(text(R.string.notifications_blocked))
        compose.onNodeWithText(text(R.string.notifications_allow)).assertIsDisplayed().performClick()
    }

    @Test
    fun `each kind has its own switch, and the widget is offered once it is on`() {
        shadowOf(g.context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        var rules = 0
        show(onOpenRules = { rules++ })
        compose.waitForText(text(R.string.notifications_allowed))

        switch(text(R.string.channel_due)).performClick()
        compose.waitFor { !saved().notices.serviceDue }
        switch(text(R.string.channel_overdue)).performClick()
        compose.waitFor { !saved().notices.overdue }
        switch(text(R.string.channel_recalls)).performClick()
        compose.waitFor { !saved().notices.recalls }
        compose.onNode(isToggleable() and hasText(text(R.string.notifications_mileage_note))).performScrollTo().performClick()
        compose.waitFor { !saved().notices.mileage }
        switch(text(R.string.notifications_widget)).performClick()
        compose.waitFor { saved().homeWidget }
        // Robolectric's launcher takes no pin requests, so the screen says where to find the widget.
        compose.waitForText(text(R.string.notifications_widget_from_launcher))

        compose.onNodeWithText(text(R.string.notifications_how_early)).performScrollTo().performClick()
        assertEquals(1, rules)
    }

    @Test
    fun `with recall checks off the recalls switch says they aren't checked`() {
        runBlocking { g.settings.setRecallCadence(RecallCadence.NEVER) }
        show()
        compose.waitForText(text(R.string.notifications_recalls_off))
    }

    @Test
    fun `the check-in cadence and its own time of day are chosen in dialogs, and the time can go back to the rest's`() {
        show()
        compose.waitForText(text(R.string.notifications_time))

        compose.onNodeWithText(checkInLabel(MileageCheckIns.DEFAULT_DAYS)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.cadence_every_week)).performClick()
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.waitFor { saved().notices.mileageEveryDays == 7 }

        val mileageTime = text(R.string.notifications_mileage_time)
        compose.onNodeWithText(mileageTime).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.notifications_mileage_time_note)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ok)).performClick()
        compose.waitFor { saved().notices.mileageHour == saved().notices.hour }

        compose.onNodeWithText(mileageTime).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.notifications_mileage_time_same)).performClick()
        compose.waitFor { saved().notices.mileageHour == null }

        compose.onNodeWithText(text(R.string.notifications_time)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.onNodeWithText(text(R.string.notifications_time)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.ok)).performClick()
        compose.waitFor { compose.onAllNodes(hasText(text(R.string.ok))).fetchSemanticsNodes().isEmpty() }
        assertEquals(8, saved().notices.hour)
    }
}
