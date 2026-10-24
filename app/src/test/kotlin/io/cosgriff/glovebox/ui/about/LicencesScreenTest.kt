package io.cosgriff.glovebox.ui.about

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Licences
import io.cosgriff.glovebox.ui.LicenceTextRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.licencesSummary
import io.cosgriff.glovebox.ui.settings.routeHandle
import io.cosgriff.glovebox.ui.settings.show
import io.cosgriff.glovebox.ui.settings.text
import io.cosgriff.glovebox.ui.settings.title
import io.cosgriff.glovebox.ui.settings.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The licences: the list built into the APK, read from the build's own record, and each licence's wording. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class LicencesScreenTest {
    @get:Rule val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val catalog = LicenceCatalog(app)

    @Test
    fun `the catalog groups every library under its licence, the fonts and Tesseract included`() {
        val groups = runBlocking { catalog.groups() }
        val ids = groups.flatMap { g -> g.libraries.map { it.id } }
        assertTrue("com.ibm:plex" in ids)
        assertTrue("cz.adaptech.tesseract4android:tesseract4android" in ids)
        val tesseract = groups.filter { g -> g.libraries.any { it.id == "cz.adaptech.tesseract4android:tesseract4android" } }.map { it.licence.id }
        assertEquals(setOf("Apache-2.0", "tesseract4android-bundled"), tesseract.toSet())
        assertEquals("SIL Open Font License 1.1", groups.single { it.licence.id == "OFL-1.1" }.title)
    }

    @Test
    fun `each licence has its wording, the app's own from the repository, a library's own from the app's copy`() {
        val app = runBlocking { catalog.document(Licences.APP_LICENCE_ID) }!!
        assertEquals(Licences.APP_LICENCE_NAME, app.name)
        assertTrue(app.text!!.contains("GNU GENERAL PUBLIC LICENSE"))

        val apache = runBlocking { catalog.document("Apache-2.0") }!!
        assertNotNull(apache.text)

        val fonts = runBlocking { catalog.document("OFL-1.1") }!!
        assertEquals("https://openfontlicense.org", fonts.url)
        assertTrue("the fonts' licence comes from the app's own copy", fonts.text!!.isNotBlank())

        assertNull(runBlocking { catalog.document("no-such-licence") })
    }

    @Test
    fun `every licence the build lists has its wording, from the app's own copies`() {
        // The build fetches no wording, so a dependency that brings a new licence needs its text in assets/licences.
        val groups = runBlocking { catalog.groups() }
        val missing = groups.map { it.licence.id }.filter { runBlocking { catalog.document(it) }?.text.isNullOrBlank() }
        assertEquals(emptyList<String>(), missing)
        assertEquals("MIT License", groups.single { it.licence.id == "MIT" }.title)
    }

    @Test
    fun `the list leads to each licence, the app's own first`() {
        val opened = mutableListOf<String>()
        val vm = LicencesViewModel(catalog)
        compose.show { LicencesScreen(onBack = {}, onOpenLicence = { opened += it }, vm = vm) }
        compose.waitForText(text(R.string.licences_gpl))
        compose.onNodeWithText(text(R.string.licences_built_with, licencesSummary(vm.groups!!))).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.licences_gpl)).performClick()
        compose.onNodeWithText(vm.groups!!.first().title).performClick()
        assertEquals(listOf(Licences.APP_LICENCE_ID, vm.groups!!.first().licence.id), opened)
    }
}

/** One licence's wording, a paragraph at a time, with its address when it has one. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class LicenceTextScreenTest {
    @get:Rule(order = 0) val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val catalog = LicenceCatalog(app)

    private fun show(id: String, onMessage: (String) -> Unit = {}): LicenceTextViewModel {
        val vm = LicenceTextViewModel(routeHandle(LicenceTextRoute(id)), catalog)
        compose.show { LicenceTextScreen(onBack = {}, onMessage = onMessage, vm = vm) }
        return vm
    }

    @Test
    fun `the app's licence is shown in paragraphs`() {
        val vm = show(Licences.APP_LICENCE_ID)
        compose.waitForText(Licences.APP_LICENCE_NAME)
        compose.waitForText("GNU GENERAL PUBLIC LICENSE", substring = true)
        assertTrue(!vm.loading)
    }

    @Test
    fun `a licence with an address links to it`() {
        show("OFL-1.1")
        compose.waitForText("https://openfontlicense.org")
        compose.onNodeWithText("https://openfontlicense.org").performClick()
        assertEquals("https://openfontlicense.org", org.robolectric.Shadows.shadowOf(app).nextStartedActivity.dataString)
    }

    @Test
    fun `a licence that isn't there says so plainly`() {
        val vm = show("no-such-licence")
        compose.waitForText(text(R.string.licences_no_text))
        assertNull(vm.document)
        compose.onNodeWithText(text(R.string.licences_licence)).assertIsDisplayed()
    }
}

