package io.cosgriff.glovebox.ui.about

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.schedule.KnownScheduleCatalog
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.show
import io.cosgriff.glovebox.ui.settings.text
import io.cosgriff.glovebox.ui.settings.waitFor
import io.cosgriff.glovebox.ui.settings.waitForText
import io.cosgriff.glovebox.ui.torque.KnownSpecCatalog
import io.cosgriff.glovebox.ui.vehicle.KnownTakesCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** About: where the app's data comes from, every bundled list credited by name, and the way to the source and licences. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class AboutScreenTest {
    @get:Rule val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun vm() = KnownListsViewModel(KnownSpecCatalog(app), KnownScheduleCatalog(app), KnownTakesCatalog(app))

    @Test
    fun `every bundled list is credited to the maker it came from`() {
        val schedules = runBlocking { KnownScheduleCatalog(app).lists() }
        assertTrue("the app ships makers' schedules", schedules.isNotEmpty())
        val opened = mutableListOf<String>()
        val screenVm = vm()
        compose.show { AboutScreen(onBack = {}, onOpenPrivacy = { opened += "privacy" }, onOpenLicences = { opened += "licences" }, onMessage = {}, vm = screenVm) }
        compose.onNodeWithText(text(R.string.about_vpic)).assertIsDisplayed()
        val first = schedules.first()
        compose.waitForText(text(R.string.about_known_schedule, first.title, first.source.note))

        compose.onNodeWithText(text(R.string.privacy)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.licences_title)).performScrollTo().performClick()
        assertEquals(listOf("privacy", "licences"), opened)
    }

    @Test
    fun `the source opens in the browser, or the screen says there is none`() {
        val messages = mutableListOf<String>()
        val screenVm = vm()
        compose.show { AboutScreen(onBack = {}, onOpenPrivacy = {}, onOpenLicences = {}, onMessage = { messages += it }, vm = screenVm) }
        compose.onNodeWithText(text(R.string.about_source)).performScrollTo().performClick()
        val opened = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertEquals(SOURCE_URL, opened.dataString)
        assertTrue(messages.isEmpty())

        shadowOf(app).checkActivities(true)
        compose.onNodeWithText(text(R.string.about_source)).performScrollTo().performClick()
        compose.waitFor { messages.isNotEmpty() }
        assertEquals(listOf(text(R.string.licences_no_browser, SOURCE_URL)), messages)
    }

    @Test
    fun `the privacy screen lists every heading with its lines`() {
        compose.show { PrivacyScreen(onBack = {}) }
        listOf(
            R.string.privacy_stays to R.array.privacy_stays,
            R.string.privacy_sent to R.array.privacy_sent,
            R.string.privacy_never to R.array.privacy_never,
            R.string.privacy_saved to R.array.privacy_saved,
            R.string.privacy_imported to R.array.privacy_imported,
            R.string.privacy_leaving to R.array.privacy_leaving,
            R.string.privacy_permissions to R.array.privacy_permissions,
            R.string.privacy_updates to R.array.privacy_updates,
        ).forEach { (heading, lines) ->
            compose.onNodeWithText(text(heading)).performScrollTo().assertIsDisplayed()
            val each = app.resources.getStringArray(lines)
            assertTrue("${app.resources.getResourceEntryName(lines)} has lines", each.isNotEmpty())
            each.forEach { compose.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        }
    }
}
