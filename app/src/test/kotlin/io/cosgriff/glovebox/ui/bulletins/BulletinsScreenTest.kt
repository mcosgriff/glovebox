package io.cosgriff.glovebox.ui.bulletins

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.VehicleBulletins
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = "w411dp-h2000dp-xhdpi", sdk = [35])
class BulletinsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val opened = mutableListOf<String>()

    private fun show(state: BulletinsState) = compose.setContent { GloveboxTheme { BulletinsContent(state, onBack = {}, onOpenPdf = { opened += it }) } }

    @Test
    fun `each bulletin says its kind, the list says how current it is, and one found by its wording says so`() {
        show(Fixtures.bulletins)
        compose.onNodeWithText(app.getString(R.string.bulletins_as_of, LocalDate.of(2026, 10, 24).format(longDate))).assertExists()
        compose.onNodeWithText(app.getString(R.string.bulletins_kind_warranty)).assertExists()
        compose.onNodeWithText(app.getString(R.string.bulletins_kind_over_the_air)).assertExists()
        compose.onNodeWithText(app.getString(R.string.bulletins_kind_campaign)).assertExists()
        compose.onAllNodesWithText(app.getString(R.string.bulletins_by_wording)).assertCountEquals(1)
        compose.onNodeWithText("2017–2019, 2021 4RUNNER", substring = true).assertExists()
    }

    @Test
    fun `a bulletin whose summary says its program ended says so on its card, and the screen says programs end`() {
        show(Fixtures.bulletins)
        compose.onAllNodesWithText(app.getString(R.string.bulletins_ended, LocalDate.of(2019, 3, 31).format(longDate))).assertCountEquals(1)
        compose.onNodeWithText(app.getString(R.string.bulletins_end_note)).assertExists()
    }

    @Test
    fun `a bulletin's PDF opens from NHTSA, and one without a link offers none`() {
        show(Fixtures.bulletins)
        compose.onAllNodesWithText(app.getString(R.string.bulletins_open_pdf, "ZZ-25-07")).assertCountEquals(0)
        compose.onNodeWithText(app.getString(R.string.bulletins_open_pdf, "ZZ1")).assertExists()
        compose.onNodeWithText(app.getString(R.string.bulletins_open_pdf, "ZZ-26-01")).performScrollTo().performClick()
        assertEquals(listOf("https://static.nhtsa.gov/odi/tsbs/2026/MC-90000003-0001.pdf"), opened)
    }

    @Test
    fun `a covered year with none says so`() {
        show(BulletinsState("2019 Blue truck", VehicleBulletins(LocalDate.of(2026, 10, 24), emptyList())))
        compose.onNodeWithText(app.getString(R.string.bulletins_none)).assertExists()
    }

    @Test
    fun `a model Glovebox has no bulletins for says why`() {
        show(BulletinsState("2009 Wagon", null))
        compose.onNodeWithText(app.getString(R.string.bulletins_not_covered)).assertExists()
    }
}
