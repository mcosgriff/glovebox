package io.cosgriff.glovebox.ui.vehicle

import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.LicensePlates
import io.cosgriff.glovebox.domain.MakerDocuments
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.ui.VehicleDetailsRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Vehicle details with its real ViewModel over a database in memory: what each editor writes, one column at a time. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class VehicleDetailsScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val opened = mutableListOf<String>()
    private val archived = mutableListOf<Pair<String, () -> Unit>>()

    private fun viewModel(id: Long) = g.viewModel {
        VehicleDetailsViewModel(
        routeHandle(VehicleDetailsRoute(id)), g.db.vehicles(), g.db.schedule(), g.db.recalls(), g.db.service(), g.db.odometer(),
        g.db.torque(), g.db.documents(), g.vehiclePhotos, g.photoPicker, g.archiver, g.db.units(), g.knownTakes, g.makerDocuments, g.settings,
        )
    }

    private fun show(id: Long): VehicleDetailsViewModel {
        val vm = viewModel(id)
        compose.setContent {
            GloveboxTheme {
                VehicleDetailsScreen(
                    onBack = { opened += "back" },
                    onOpenRecalls = { opened += "recalls $it" },
                    onOpenSchedule = { opened += "schedule $it" },
                    onArchived = { name, undo -> archived += name to undo },
                    onDelete = { opened += "delete $it" },
                    onOpenTorqueSpecs = { opened += "torque $it" },
                    onOpenMoreTakes = { opened += "takes $it" },
                    onOpenReadings = { opened += "readings $it" },
                    onOpenImport = { opened += "import $it" },
                    onOpenDocuments = { opened += "documents $it" },
                    onClearHistory = { opened += "clear $it" },
                    vm = vm,
                )
            }
        }
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.details_nickname))).fetchSemanticsNodes().isNotEmpty() }
        return vm
    }

    private fun text(id: Int, vararg args: Any) = compose.onNodeWithText(g.string(id, *args))
    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))
    private fun clickLabelled(label: String) = SemanticsMatcher("clicks to $label") { it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label }
    private fun longClickLabelled(label: String) = SemanticsMatcher("long-clicks to $label") { it.config.getOrElseNullable(SemanticsActions.OnLongClick) { null }?.label == label }
    private fun tap(label: String) = compose.onNode(hasText(label) and hasClickAction()).performScrollTo().click()

    @Test
    fun `the nickname is saved as it is typed, cleaned`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        field(g.string(R.string.details_nickname)).performTextInput("  Old   faithful ")
        compose.awaitThat { g.db.vehicles().byId(id)?.nickname == "Old faithful" }
    }

    @Test
    fun `a plate and its region are saved together, and only those`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(g.string(R.string.details_plate))
        field(g.string(R.string.details_plate_field)).performTextInput("gbx 9876")
        text(R.string.not_set).click()
        val region = LicensePlates.regions.first()
        text(R.string.details_plate_region_item, region.name, region.code).click()
        text(R.string.save).click()
        compose.awaitThat { g.db.vehicles().byId(id)?.plate != null }
        val v = g.vehicle(id)!!
        assertEquals(LicensePlates.clean("GBX 9876"), v.plate)
        assertEquals(region.code, v.plateRegion)
        assertEquals(VehicleFixtures.wagon.model, v.model)
    }

    @Test
    fun `a VIN added later is checked as it is typed and changes nothing else`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(g.string(R.string.details_vin))
        val vin = field(g.string(R.string.details_vin_or_serial))
        vin.performTextInput("ab!")
        text(R.string.details_vin_letters).assertExists()
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).assertIsNotEnabled()

        vin.performTextClearance()
        // A made-up VIN whose check digit is wrong: offered, with a warning.
        vin.performTextInput("JTEBU5JR0K5900001")
        text(R.string.details_vin_check_digit).assertExists()
        vin.performTextClearance()
        vin.performTextInput("jtebu5jr7k5900001")
        text(R.string.add_vehicle_vin_valid).assertExists()
        text(R.string.save).click()

        compose.awaitThat { g.db.vehicles().byId(id)?.vin != null }
        val v = g.vehicle(id)!!
        assertEquals("JTEBU5JR7K5900001", v.vin)
        assertEquals(1998, v.year)
        assertEquals("Volvo", v.make)
        assertEquals("V70", v.model)
        assertTrue(v.enteredByHand)
    }

    @Test
    fun `engine oil is saved in its standard form, in the unit picked`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(TakesItem.OIL.label)
        compose.onNode(hasText("5W-30") and hasClickAction()).click()
        field(g.string(R.string.oil_capacity)).performTextInput("5.5")
        compose.onNode(hasText(g.string(R.string.unit_litre)) and hasClickAction()).click()
        field(g.string(R.string.oil_filter)).performTextInput("90915-YZZD3")
        text(R.string.save).click()

        compose.awaitThat { g.db.vehicles().byId(id)?.oilSpec != null }
        val v = g.vehicle(id)!!
        assertEquals("5W-30", v.oilSpec)
        assertEquals(5.5, v.oilCapacity!!, 0.0)
        assertEquals(VehicleSpecEntry.Volume.LITRES.name, v.oilCapacityUnit)
        assertEquals(VehicleSpecEntry.partNumbers("90915-YZZD3"), v.oilFilter)
    }

    @Test
    fun `an oil grade that isn't one can't be saved`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(TakesItem.OIL.label)
        field(g.string(R.string.oil_grade)).performTextInput("thick")
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).assertIsNotEnabled()
        text(R.string.cancel).click()
        assertNull(g.vehicle(id)!!.oilSpec)
    }

    @Test
    fun `tires take a rear size of their own and pressures in kPa`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(TakesItem.TIRES.label)
        field(g.string(R.string.tires_size)).performTextInput("205/55R16")
        compose.onNode(hasText(g.string(R.string.tires_rear_differs)) and hasClickAction()).click()
        // With the rear differing, the sizes and the pressures are each labelled Front and Rear: sizes first.
        compose.onAllNodes(hasSetTextAction() and hasText(g.string(R.string.rear)))[0].performTextInput("225/45R17")
        compose.onNode(hasText(g.string(R.string.unit_kpa)) and hasClickAction()).click()
        compose.onAllNodes(hasSetTextAction() and hasText(g.string(R.string.front)))[1].performTextInput("230")
        field(g.string(R.string.tires_spare)).performTextInput("300")
        text(R.string.save).click()

        compose.awaitThat { g.db.vehicles().byId(id)?.tireSize != null }
        val v = g.vehicle(id)!!
        assertEquals(VehicleSpecEntry.tireSize("205/55R16"), v.tireSize)
        assertEquals(VehicleSpecEntry.tireSize("225/45R17"), v.tireSizeRear)
        assertEquals(230, v.tirePressureFront)
        assertNull(v.tirePressureRear)
        assertEquals(300, v.tirePressureSpare)
        assertEquals(VehicleSpecEntry.Pressure.KPA.name, v.tirePressureUnit)
    }

    @Test
    fun `wiper lengths and a note are saved`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(TakesItem.WIPERS.label)
        field(g.string(R.string.wipers_driver)).performTextInput("22")
        field(g.string(R.string.wipers_passenger)).performTextInput("19")
        field(g.string(R.string.wipers_parts)).performTextInput("  beam blades ")
        text(R.string.save).click()

        compose.awaitThat { g.db.vehicles().byId(id)?.wiperDriverIn != null }
        val v = g.vehicle(id)!!
        assertEquals(22, v.wiperDriverIn)
        assertEquals(19, v.wiperPassengerIn)
        assertNull(v.wiperRearIn)
        assertEquals("beam blades", v.wiperBlades)
    }

    @Test
    fun `a maker's list fills only empty rows, tagged to check, and the tag goes once the owner says so`() {
        val id = g.add(VehicleFixtures.truck.copy(wiperDriverIn = 24))
        show(id)
        val fill = compose.onNode(hasText(g.string(R.string.known_takes_fill, "").trim(), substring = true) and hasClickAction())
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.known_takes_fill, "").trim(), substring = true)).fetchSemanticsNodes().isNotEmpty() }
        fill.performScrollTo().click()

        compose.awaitThat { g.db.vehicles().byId(id)?.takesToCheck != null }
        val filled = g.vehicle(id)!!
        assertNotNull(filled.takesKnownList)
        assertTrue(TakesItem.OIL in filled.takesToCheck())
        // The owner's own figure is never written over.
        assertEquals(24, filled.wiperDriverIn)

        // The oil row's editor asks whether it was checked; saying so settles the tag.
        tap(TakesItem.OIL.label)
        compose.onNode(hasText(g.string(R.string.known_takes_checked)) and hasClickAction()).click()
        text(R.string.save).click()
        compose.awaitThat { TakesItem.OIL !in (g.db.vehicles().byId(id)?.takesToCheck() ?: setOf(TakesItem.OIL)) }
    }

    @Test
    fun `changing the unit converts every reading, after asking`() {
        val id = g.add(VehicleFixtures.wagon, firstReading = 100_000)
        show(id)
        tap(g.string(R.string.details_units))
        text(R.string.cancel).click()
        assertEquals(DistanceUnit.MILES.name, g.vehicle(id)!!.distanceUnit)

        tap(g.string(R.string.details_units))
        text(R.string.details_unit_convert, DistanceUnit.KILOMETERS.pluralWord).click()
        compose.awaitThat { g.db.vehicles().byId(id)?.distanceUnit == DistanceUnit.KILOMETERS.name }
        assertEquals(listOf(160_934), runBlocking { g.db.odometer().readingsFor(id) }.map(OdometerReadingEntity::odometer))
    }

    @Test
    fun `an odometer offset is worked out from what each cluster read, and can be removed`() {
        val id = g.add(VehicleFixtures.wagon, firstReading = 150_000)
        show(id)
        tap(g.string(R.string.offset_title))
        field(g.string(R.string.offset_old_read, DistanceUnit.MILES.symbolWord)).performTextInput("152000")
        field(g.string(R.string.offset_new_read, DistanceUnit.MILES.symbolWord)).performTextInput("152000")
        text(R.string.offset_same).assertExists()
        field(g.string(R.string.offset_new_read, DistanceUnit.MILES.symbolWord)).performTextClearance()
        field(g.string(R.string.offset_new_read, DistanceUnit.MILES.symbolWord)).performTextInput("0")
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).performScrollTo().click()

        compose.awaitThat { g.db.vehicles().byId(id)?.odometerOffset != 0 }
        val v = g.vehicle(id)!!
        assertEquals(152_000, v.odometerOffset)
        assertEquals(0, v.clusterStartReading)
        assertEquals(LocalDate.now(), v.clusterSwappedOn)

        tap(g.string(R.string.offset_title))
        compose.onNode(hasText(g.string(R.string.offset_remove)) and hasClickAction()).performScrollTo().click()
        compose.awaitThat { g.db.vehicles().byId(id)?.odometerOffset == 0 }
        assertNull(g.vehicle(id)!!.clusterSwappedOn)
    }

    @Test
    fun `an offset with no old reading asks for one and saves nothing`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(g.string(R.string.offset_title))
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).performScrollTo().click()
        text(R.string.offset_old_missing, DistanceUnit.MILES.pluralWord).assertExists()
        assertEquals(0, g.vehicle(id)!!.odometerOffset)
    }

    @Test
    fun `marking as sold archives the vehicle with its final reading, and Undo puts both back`() {
        val id = g.add(VehicleFixtures.wagon, firstReading = 180_000)
        show(id)
        tap(g.string(R.string.details_mark_sold))
        val odometer = field(g.string(R.string.sold_final_odometer, DistanceUnit.MILES.symbolWord))
        odometer.performTextClearance()
        odometer.performTextInput("170000")
        compose.onNode(hasText(g.string(R.string.sold_archive)) and hasClickAction()).performScrollTo().assertIsNotEnabled()
        odometer.performTextClearance()
        odometer.performTextInput("181250")
        compose.onNode(hasText(g.string(R.string.sold_archive)) and hasClickAction()).performScrollTo().assertIsEnabled().click()

        compose.awaitThat { archived.isNotEmpty() }
        assertEquals("V70", archived.single().first)
        assertEquals(LocalDate.now(), g.vehicle(id)!!.archivedOn)
        assertEquals(listOf(180_000, 181_250), runBlocking { g.db.odometer().readingsFor(id) }.map { it.odometer }.sorted())

        archived.single().second()
        compose.awaitThat { g.db.vehicles().byId(id)?.archivedOn == null && g.db.odometer().readingsFor(id).size == 1 }
    }

    @Test
    fun `each row opens its own page for this vehicle`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        listOf(
            R.string.details_recalls, R.string.details_schedule, R.string.readings_title, R.string.documents_title,
            R.string.details_import, R.string.details_clear, R.string.details_delete,
        ).forEach { tap(g.string(it)) }
        tap(g.string(R.string.fluids_title))
        compose.onNode(hasText(g.string(R.string.details_torque)) and hasClickAction()).performScrollTo().click()
        assertEquals(
            listOf("recalls $id", "schedule $id", "readings $id", "documents $id", "import $id", "clear $id", "delete $id", "takes $id", "torque $id"),
            opened,
        )
    }

    @Test
    fun `a long press copies the VIN, marked sensitive`() {
        val id = g.add(VehicleFixtures.truck)
        show(id)
        compose.onNode(longClickLabelled(g.string(R.string.details_copy_vin))).performScrollTo().performSemanticsAction(SemanticsActions.OnLongClick)
        val clip = g.context.getSystemService(ClipboardManager::class.java).primaryClip!!
        assertEquals(VehicleFixtures.truck.vin, clip.getItemAt(0).text.toString())
        assertTrue(clip.description.extras.getBoolean("android.content.extra.IS_SENSITIVE"))
    }

    @Test
    fun `the maker's VIN lookup copies the VIN and never puts it in the link`() {
        val id = g.add(VehicleFixtures.hatch)
        show(id)
        val lookup = g.string(R.string.maker_doc_vin_lookup, "Volkswagen")
        compose.awaitThat { compose.onAllNodes(hasText(lookup)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(lookup) and hasClickAction()).performScrollTo().click()

        val started: Intent = checkNotNull(shadowOf(g.context).nextStartedActivity) { "No page was opened" }
        assertEquals(Intent.ACTION_VIEW, started.action)
        val url = started.dataString!!
        assertFalse(url.contains(VehicleFixtures.hatch.vin!!))
        assertTrue(MakerDocuments.hosts.any { url.contains(it) })
        val clip = g.context.getSystemService(ClipboardManager::class.java).primaryClip!!
        assertEquals(VehicleFixtures.hatch.vin, clip.getItemAt(0).text.toString())
    }

    @Test
    fun `the mileage check-in is set for this vehicle alone`() {
        val id = g.add(VehicleFixtures.wagon)
        show(id)
        tap(g.string(R.string.channel_mileage))
        compose.onNode(hasText(g.string(R.string.checkin_off)) and hasClickAction()).click()
        text(R.string.save).click()
        compose.awaitThat { g.db.vehicles().byId(id)?.mileageCheckInDays != null }
        text(R.string.details_checkin_off).assertExists()
    }

    @Test
    fun `a photo that can't be read is offered to change or remove, and Remove clears it`() {
        val id = g.add(VehicleFixtures.wagon.copy(photoFile = "vehicle-gone.jpg"))
        val vm = show(id)
        val photo = compose.onNode(clickLabelled(g.string(R.string.details_change_photo)))
        photo.click()
        text(R.string.details_choose_photo).click()
        // No picker is attached outside the activity: nothing opens, and nothing is said to have failed.
        assertFalse(vm.photoFailed.value)
        photo.click()
        text(R.string.details_remove_photo).click()
        compose.awaitThat { g.db.vehicles().byId(id)?.photoFile == null }
        text(R.string.details_add_photo, "V70").assertExists()
    }

    @Test
    fun `the cluster swap and check-in are written through the ViewModel one column at a time`() {
        val id = g.add(VehicleFixtures.wagon.copy(nickname = "Keep me", oilSpec = "5W-30"))
        val vm = show(id)
        vm.setClusterSwap(ClusterSwap(on = LocalDate.of(2025, 1, 2), offset = 1_000, startReading = 5))
        compose.awaitThat { g.db.vehicles().byId(id)?.odometerOffset == 1_000 }
        val v = g.vehicle(id)!!
        assertEquals("Keep me", v.nickname)
        assertEquals("5W-30", v.oilSpec)
        assertEquals(5, v.clusterStartReading)
    }
}

/** Clicks through the node's semantics, so a sheet only partly on screen doesn't swallow the tap. */
internal fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction = performSemanticsAction(SemanticsActions.OnClick)
