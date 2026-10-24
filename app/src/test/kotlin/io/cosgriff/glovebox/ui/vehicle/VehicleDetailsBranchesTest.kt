package io.cosgriff.glovebox.ui.vehicle

import android.app.Application
import android.content.ClipboardManager
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.MakerDocument
import io.cosgriff.glovebox.domain.MakerDocumentKind
import io.cosgriff.glovebox.domain.MakerDocumentList
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.settings.checkInLabel
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Vehicle details through [VehicleDetailsContent], from made-up vehicles: what each row says in the cases the
 * ViewModel-driven [VehicleDetailsScreenTest] doesn't reach (a photo that failed, a vehicle with no usable name,
 * a powertrain, a confirmed trim, the maker's documents in each form, and the mileage check-in's wording).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class VehicleDetailsBranchesTest {
    @get:Rule val compose = createComposeRule()

    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun string(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
    private fun text(value: String) = compose.onNodeWithText(value)

    private data class Shown(val vehicle: VehicleEntity, val photoFailed: Boolean, val makerDocs: MakerDocumentList?, val noticePrefs: NoticePrefs)

    /** What is on screen; the content is set once, and a later [show] changes what it is given. */
    private val shown = mutableStateOf<Shown?>(null)

    private fun show(
        vehicle: VehicleEntity,
        photoFailed: Boolean = false,
        makerDocs: MakerDocumentList? = null,
        noticePrefs: NoticePrefs = NoticePrefs(),
    ) {
        val first = shown.value == null
        shown.value = Shown(vehicle, photoFailed, makerDocs, noticePrefs)
        if (first) {
            compose.setContent {
                val s = shown.value ?: return@setContent
                GloveboxTheme {
                    VehicleDetailsContent(
                        VehicleDetails(s.vehicle, schedule = null, recalls = null, torqueSpecs = 0), photo = null, photoFailed = s.photoFailed,
                        known = TakesKnown(), makerDocs = s.makerDocs, nickname = s.vehicle.nickname.orEmpty(), noticePrefs = s.noticePrefs,
                        onNicknameChange = {}, onBack = {}, onOpenRecalls = {}, onOpenSchedule = {}, onDelete = {}, onOpenTorqueSpecs = {},
                        onOpenMoreTakes = {}, onOpenReadings = {}, onOpenImport = {}, onOpenDocuments = {}, onClearHistory = {},
                        onEditTakes = {}, onFillKnown = {}, onEditPlate = {}, onEditCheckIn = {}, onAddVin = {}, onSetOffset = {},
                        onChangeUnit = {}, onSell = {}, onPickPhoto = {}, onRemovePhoto = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /** The detail line of the mileage check-in row. */
    private fun checkIn(detail: String) = compose.onNode(hasText(string(R.string.channel_mileage)) and hasText(detail) and hasClickAction())

    @Test
    fun `a photo that failed to be read says so, and only then`() {
        show(VehicleFixtures.wagon, photoFailed = true)
        text(string(R.string.details_photo_unreadable)).assertExists()

        show(VehicleFixtures.wagon, photoFailed = false)
        text(string(R.string.details_photo_unreadable)).assertDoesNotExist()
    }

    @Test
    fun `a vehicle with no year, make or model to show is an unknown vehicle`() {
        show(VehicleFixtures.wagon.copy(year = 0, make = "Unknown", model = " ", nickname = "Mystery"))
        text(string(R.string.unknown_vehicle)).assertExists()
    }

    @Test
    fun `a known vehicle is titled by year, make, model and trim`() {
        show(VehicleFixtures.truck)
        text("2019 Toyota 4Runner SR5").assertExists()
        text(string(R.string.unknown_vehicle)).assertDoesNotExist()
    }

    @Test
    fun `the powertrain is shown when one is on record, and its row is left out otherwise`() {
        show(VehicleFixtures.truck.copy(powertrain = Powertrain.HYBRID.name))
        text(string(R.string.add_vehicle_powertrain)).assertExists()
        text(string(R.string.powertrain_hybrid)).assertExists()

        show(VehicleFixtures.truck.copy(powertrain = null))
        text(string(R.string.add_vehicle_powertrain)).assertDoesNotExist()

        // A name a later version dropped is nothing on record, not a dead row.
        show(VehicleFixtures.truck.copy(powertrain = "STEAM"))
        text(string(R.string.add_vehicle_powertrain)).assertDoesNotExist()
    }

    @Test
    fun `the source says when the trim was confirmed`() {
        show(VehicleFixtures.truck.copy(trimConfirmed = true))
        text(string(R.string.details_source_trim_confirmed)).assertExists()
        text(string(R.string.details_source)).assertDoesNotExist()

        show(VehicleFixtures.truck.copy(trimConfirmed = false))
        text(string(R.string.details_source)).assertExists()
        text(string(R.string.details_source_trim_confirmed)).assertDoesNotExist()

        // Entered by hand wins over a confirmed trim: nothing was decoded.
        show(VehicleFixtures.wagon.copy(trimConfirmed = true))
        text(string(R.string.details_entered_by_you)).assertExists()
        text(string(R.string.details_source_trim_confirmed)).assertDoesNotExist()
    }

    @Test
    fun `each maker's document says where it is and which edition`() {
        show(VehicleFixtures.hatch, makerDocs = docs)
        text(string(R.string.maker_doc_vin_lookup, MAKE)).assertExists()
        text(string(R.string.maker_doc_from, MAKE, string(R.string.maker_doc_code_edition, EDITION_CODE, EDITION))).assertExists()
        text(string(R.string.maker_doc_from, MAKE, PLAIN_CODE)).assertExists()
        text(string(R.string.maker_doc_from_nhtsa, MAKE, NHTSA_CODE)).assertExists()
        // The edition is only on the document that has one.
        text(string(R.string.maker_doc_code_edition, PLAIN_CODE, EDITION)).assertDoesNotExist()
    }

    @Test
    fun `without a VIN the lookup row doesn't promise to copy one, and tapping it copies nothing`() {
        show(VehicleFixtures.wagon, makerDocs = docs)
        val none = string(R.string.maker_doc_vin_lookup_none, MAKE)
        text(none).assertExists()
        text(string(R.string.maker_doc_vin_lookup, MAKE)).assertDoesNotExist()

        compose.onNode(hasText(none) and hasClickAction()).performScrollTo().click()
        assertEquals(VIN_LOOKUP_URL, checkNotNull(shadowOf(context).nextStartedActivity) { "No page was opened" }.dataString)
        assertNull(context.getSystemService(ClipboardManager::class.java).primaryClip)
    }

    @Test
    fun `tapping the VIN lookup copies the VIN marked sensitive and opens the maker's page without it`() {
        show(VehicleFixtures.hatch, makerDocs = docs)
        compose.onNode(hasText(string(R.string.maker_doc_vin_lookup, MAKE)) and hasClickAction()).performScrollTo().click()

        val started: Intent = checkNotNull(shadowOf(context).nextStartedActivity) { "No page was opened" }
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(VIN_LOOKUP_URL, started.dataString)
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip!!
        assertEquals(VehicleFixtures.hatch.vin, clip.getItemAt(0).text.toString())
        assertTrue(clip.description.extras.getBoolean("android.content.extra.IS_SENSITIVE"))
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `a document with no browser to open it says so`() {
        shadowOf(context).checkActivities(true)
        show(VehicleFixtures.hatch, makerDocs = docs)
        compose.onNode(hasText(string(R.string.maker_doc_from, MAKE, PLAIN_CODE)) and hasClickAction()).performScrollTo().click()
        compose.waitForIdle()
        assertEquals(string(R.string.licences_no_browser, PLAIN_URL), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `the check-in says it is off everywhere when mileage notices are off`() {
        show(VehicleFixtures.wagon.copy(mileageCheckInDays = 10), noticePrefs = NoticePrefs(mileage = false))
        checkIn(string(R.string.details_checkin_all_off)).assertExists()
        text(checkInLabel(10)).assertDoesNotExist()
    }

    @Test
    fun `the check-in says when this vehicle alone isn't asked`() {
        show(VehicleFixtures.wagon.copy(mileageCheckInDays = 0))
        checkIn(string(R.string.details_checkin_off)).assertExists()
    }

    @Test
    fun `a vehicle's own check-in is worded as its cadence`() {
        show(VehicleFixtures.wagon.copy(mileageCheckInDays = 7))
        checkIn(string(R.string.cadence_every_week)).assertExists()

        show(VehicleFixtures.wagon.copy(mileageCheckInDays = 10))
        val tenDays = checkInLabel(10)
        assertEquals(context.resources.getQuantityString(R.plurals.cadence_every_days, 10, "10"), tenDays)
        checkIn(tenDays).assertExists()
    }

    @Test
    fun `a vehicle following the default says so, with the default's cadence`() {
        show(VehicleFixtures.wagon.copy(mileageCheckInDays = null), noticePrefs = NoticePrefs(mileageEveryDays = 14))
        checkIn(string(R.string.checkin_as_default, string(R.string.cadence_every_two_weeks))).assertExists()
    }

    private companion object {
        /** Made-up documents and codes; the links point at the makers' allowed hosts, at pages that don't exist. */
        const val MAKE = "Volkswagen"
        const val VIN_LOOKUP_URL = "https://www.vw.com/en/made-up-manuals.html"
        const val PLAIN_URL = "https://ownersliterature.vw.com/made-up-warranty.pdf"
        const val EDITION_CODE = "MADEUP01"
        const val EDITION = "through made-up production"
        const val PLAIN_CODE = "MADEUP02"
        const val NHTSA_CODE = "MADEUP03"

        val docs = MakerDocumentList(
            MAKE, "GTI",
            listOf(
                MakerDocument(MakerDocumentKind.OWNERS_MANUAL, 2013..2013, VIN_LOOKUP_URL, code = null, vinLookup = true),
                MakerDocument(MakerDocumentKind.MAINTENANCE_GUIDE, 2013..2013, "https://ownersliterature.vw.com/made-up-guide.pdf", EDITION_CODE, edition = EDITION),
                MakerDocument(MakerDocumentKind.WARRANTY, 2013..2013, PLAIN_URL, PLAIN_CODE),
                MakerDocument(MakerDocumentKind.FLUID_CHART, 2013..2013, "https://static.nhtsa.gov/odi/tsbs/made-up.pdf", NHTSA_CODE),
            ),
        )
    }
}
