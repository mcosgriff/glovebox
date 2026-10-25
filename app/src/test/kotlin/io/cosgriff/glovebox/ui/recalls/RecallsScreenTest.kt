package io.cosgriff.glovebox.ui.recalls

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.garage.TIMEOUT
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.ui.garage.UnconfinedMainRule
import io.cosgriff.glovebox.ui.garage.recallRow
import io.cosgriff.glovebox.ui.screens.Fixtures
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
// Tall enough that every card of the list is composed.
@Config(application = ScreenshotApp::class, qualifiers = "w411dp-h2000dp-xhdpi", sdk = [35])
class RecallsScreenTest {
    @get:Rule val compose = createComposeRule()

    // Room's writes from viewModelScope finish only off Robolectric's paused main looper.
    @get:Rule val main = UnconfinedMainRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    // Never re-checked on the way in, so each test decides what NHTSA is asked.
    private val h = RecallsHarness(cadence = RecallCadence.NEVER)
    private val vin = Fixtures.truck.vin!!
    private var backs = 0

    @After fun close() = h.close()

    private fun str(@StringRes id: Int, vararg args: Any) = app.getString(id, *args)

    private fun show(vehicleId: Long): RecallsViewModel {
        val vm = h.viewModel(vehicleId)
        compose.setContent { GloveboxTheme { RecallsScreen(onBack = { backs++ }, vm = vm) } }
        compose.waitUntil(TIMEOUT) { vm.state.value != null && !vm.refreshing }
        return vm
    }

    private fun shows(text: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun startedUrl(): String = shadowOf(app).nextStartedActivity.let { assertEquals(Intent.ACTION_VIEW, it.action); it.dataString!! }

    @Test
    fun `"No open recalls" waits for NHTSA's answer, and the maker's lookup never carries the VIN`() {
        val truck = h.addTruck(checkedOn = null)
        val vm = show(truck)
        shows(str(R.string.details_recalls_not_checked))
        shows(str(R.string.recalls_refresh_to_ask, "2019 Toyota 4Runner"))
        compose.onAllNodesWithText(str(R.string.recalls_all_clear)).assertCountEquals(0)
        // Only the end of the VIN is on screen.
        compose.onAllNodesWithText(vin).assertCountEquals(0)
        shows(str(R.string.recalls_vin_end, vin.takeLast(6)))

        compose.onNodeWithText(str(R.string.recalls_verify_with, "Toyota")).performClick()
        val url = startedUrl()
        assertEquals(Recalls.manufacturerSite("Toyota")!!.recallUrl, url)
        assertFalse(vin in url)

        compose.onNodeWithText(str(R.string.refresh)).performClick()
        shows(str(R.string.recalls_all_clear))
        shows(str(R.string.recalls_none_listed, "2019 Toyota 4Runner") + " " + str(R.string.recalls_checked_today))
        assertFalse(vm.failed)
        assertTrue(h.nhtsa.asked.none { vin in it.toString() })
    }

    @Test
    fun `a make with no lookup page of its own goes to NHTSA's, and a dealer search names only the make`() {
        val truck = h.addTruck(make = "Kit Car Co")
        h.insert(h.recall(truck, "26V100000"))
        show(truck)
        compose.onNodeWithText(str(R.string.recalls_check_nhtsa)).performClick()
        assertEquals(Recalls.NHTSA_LOOKUP_URL, startedUrl())

        compose.onNodeWithText(str(R.string.recalls_find_dealer)).performClick()
        val dealer = startedUrl()
        assertTrue(dealer.startsWith("geo:0,0?q="))
        assertFalse(vin in dealer)
    }

    @Test
    fun `a check that fails keeps the list on file and says from when`() {
        val truck = h.addTruck(checkedOn = LocalDate.now().minusDays(1))
        h.insert(h.recall(truck, "26V100000"), h.recall(truck, "26V200000"))
        show(truck)
        shows(app.resources.getQuantityString(R.plurals.recalls_on_file, 2, "2") + " " + str(R.string.recalls_checked_yesterday))
        h.nhtsa.recalls = null
        compose.onNodeWithText(str(R.string.refresh)).performClick()
        shows(str(R.string.recalls_failed_list_from, app.resources.getQuantityString(R.plurals.recalls_on_file, 2, "2"), LocalDate.now().minusDays(1).format(longDate)))
    }

    @Test
    fun `an open recall says do not drive or park outside, a done one doesn't, and an over-the-air fix is said either way`() {
        val truck = h.addTruck(checkedOn = LocalDate.now())
        h.insert(
            h.recall(truck, "26V100000").copy(doNotDrive = true, parkOutside = true),
            h.recall(truck, "26V200000", done = LocalDate.now()).copy(doNotDrive = true, parkOutside = true, overTheAir = true),
        )
        show(truck)
        shows(str(R.string.recalls_over_the_air))
        compose.onAllNodesWithText(str(R.string.recalls_do_not_drive)).assertCountEquals(1)
        compose.onAllNodesWithText(str(R.string.recalls_park_outside)).assertCountEquals(1)
        compose.onAllNodesWithText(str(R.string.recalls_over_the_air)).assertCountEquals(1)
    }

    @Test
    fun `a vehicle without a model can't be checked, and says so`() {
        val truck = h.addTruck(checkedOn = null, year = 0)
        show(truck)
        shows(str(R.string.recalls_cant_check))
        shows(str(R.string.recalls_missing_ymm))
        compose.onAllNodesWithText(str(R.string.refresh)).assertCountEquals(0)
    }

    @Test
    fun `marking a recall done opens the sheet and records the repair`() {
        val truck = h.addTruck(checkedOn = LocalDate.now().minusDays(20))
        h.insert(h.recall(truck, "26V100000"))
        show(truck)
        shows(str(R.string.recalls_checked_on, LocalDate.now().minusDays(20).format(longDate)))
        compose.onNodeWithText(str(R.string.recalls_mark_done)).performClick()
        shows(str(R.string.recalls_mark_done_title))
        // A record needs an odometer, so without one only turning the record off lets it through.
        compose.onNodeWithText(str(R.string.recalls_add_to_history)).performClick()
        compose.onAllNodesWithText(str(R.string.recalls_mark_done))[1].performClick()

        compose.waitUntil(TIMEOUT) { runBlocking { h.db.recalls().recallsFor(truck) }.single().completedOn != null }
        assertNull(runBlocking { h.db.recalls().recallsFor(truck) }.single().serviceRecordId)
        shows(str(R.string.recalls_pill_completed))
        compose.onNodeWithText(str(R.string.recalls_mark_open)).performClick()
        compose.waitUntil(TIMEOUT) { runBlocking { h.db.recalls().recallsFor(truck) }.single().completedOn == null }
    }

    @Test
    fun `archived recalls get a tab of their own and can be moved back`() {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"), h.recall(truck, "26V200000", component = "AIR BAGS", done = LocalDate.now()))
        show(truck)
        compose.onAllNodesWithText(str(R.string.recalls_tab_archived)).assertCountEquals(0)
        compose.onNodeWithText(str(R.string.recalls_archive)).performClick()
        shows(str(R.string.recalls_tab_archived))

        compose.onNodeWithText(str(R.string.recalls_tab_open)).performClick()
        shows(str(R.string.recalls_none_open))
        compose.onNodeWithText(str(R.string.recalls_tab_done)).performClick()
        shows(str(R.string.recalls_pill_completed))
        compose.onNodeWithText(str(R.string.recalls_tab_archived)).performClick()
        shows(str(R.string.recalls_pill_archived))
        compose.onNodeWithText(str(R.string.recalls_move_back)).performClick()
        compose.waitUntil(TIMEOUT) { runBlocking { h.db.recalls().recallsFor(truck) }.all { it.archivedOn == null } }
        shows(str(R.string.recalls_none_archived))
    }

    @Test
    fun `archiving a recall that says not to drive asks first, and Cancel leaves it open`() {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000").copy(doNotDrive = true))
        show(truck)
        compose.onNodeWithText(str(R.string.recalls_archive)).performClick()
        shows(str(R.string.recalls_archive_urgent_title))
        compose.onNodeWithText(str(R.string.cancel)).performClick()
        compose.waitForIdle()
        assertNull(runBlocking { h.db.recalls().recallsFor(truck) }.single().archivedOn)

        compose.onNodeWithText(str(R.string.recalls_archive)).performClick()
        compose.onNodeWithText(str(R.string.recalls_archive_anyway)).performClick()
        compose.waitUntil(TIMEOUT) { runBlocking { h.db.recalls().recallsFor(truck) }.single().archivedOn != null }
    }

    @Test
    fun `a recall's risk is only in the full notice`() {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000", consequence = "A made-up risk."))
        show(truck)
        compose.onAllNodesWithText("A made-up risk.", substring = true).assertCountEquals(0)
        compose.onNodeWithText(str(R.string.recalls_show_full)).performClick()
        shows("A made-up risk.")
        compose.onNodeWithText(str(R.string.recalls_show_less)).performClick()
        compose.onAllNodesWithText("A made-up risk.", substring = true).assertCountEquals(0)
    }

    @Test
    fun `Back leaves, and new recalls found by Refresh are listed open`() {
        val truck = h.addTruck()
        show(truck)
        h.nhtsa.recalls = listOf(recallRow("26V900000", "STEERING"))
        compose.onNodeWithText(str(R.string.refresh)).performClick()
        shows(str(R.string.recalls_pill_open))
        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        assertEquals(1, backs)
        assertNotNull(runBlocking { h.db.vehicles().byId(truck) }!!.recallsCheckedOn)
    }

    @Test
    fun `nothing done yet says so under Completed`() {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        show(truck)
        compose.onNodeWithText(str(R.string.recalls_tab_done)).performClick()
        shows(str(R.string.recalls_none_done))
    }

    @Test
    fun `without a maps app, Find a dealer opens the maker's page, and without a browser nothing happens`() {
        val truck = h.addTruck()
        h.insert(h.recall(truck, "26V100000"))
        show(truck)
        // Only intents something on the phone answers are started.
        shadowOf(app).checkActivities(true)
        compose.onNodeWithText(str(R.string.recalls_find_dealer)).performClick()
        assertNull(shadowOf(app).nextStartedActivity)

        val site = Recalls.manufacturerSite("Toyota")!!.recallUrl
        val browser = ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = "made.up.browser"; name = "Browser" } }
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, Uri.parse(site)), browser)
        compose.onNodeWithText(str(R.string.recalls_find_dealer)).performClick()
        assertEquals(site, startedUrl())
    }
}
