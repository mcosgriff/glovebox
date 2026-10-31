package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Archived vehicles: sold ones with their history kept; moved back to the garage with an Undo that keeps the sold date. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class ArchivedVehiclesScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val soldOn = LocalDate.of(2026, 3, 9)
    private val events = mutableListOf<String>()
    private val messages = mutableListOf<Pair<String, () -> Unit>>()

    private fun show(until: String) {
        val vm = g.viewModel { ArchivedVehiclesViewModel(g.logs, g.archiver) }
        compose.setContent {
            GloveboxTheme {
                ArchivedVehiclesScreen(
                    onBack = { events += "back" },
                    onViewHistory = { events += "history $it" },
                    onDelete = { events += "delete $it" },
                    onMessage = { m, _, undo -> messages += m to undo },
                    vm = vm,
                )
            }
        }
        compose.awaitThat { compose.onAllNodes(hasText(until, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `nothing archived says how a vehicle gets here`() {
        g.add(VehicleFixtures.wagon)
        show(until = g.string(R.string.archived_empty))
        compose.onNodeWithText("V70", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a sold vehicle shows its sale, final reading and history, and its buttons act on it`() {
        val id = g.add(VehicleFixtures.wagon.copy(nickname = "Old Faithful", archivedOn = soldOn), firstReading = 210_000)
        runBlocking {
            val r = g.db.service().insertWithItems(
                ServiceRecordEntity(vehicleId = id, date = soldOn.minusMonths(2), odometer = 209_000, diy = true),
                listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Work")),
            )
            g.db.service().insertPhotos(listOf(PhotoEntity(recordId = r, fileName = "a.jpg"), PhotoEntity(recordId = r, fileName = "b.jpg")))
        }
        // One still in the garage isn't listed.
        g.add(VehicleFixtures.truck)
        show(until = g.string(R.string.archived_sold_on, soldOn.format(longDate)))

        compose.onNodeWithText(g.string(R.string.archived_final, 210_000.asDistance(DistanceUnit.MILES)), substring = true).assertExists()
        compose.onNodeWithText(g.context.resources.getQuantityString(R.plurals.photos, 2, "2"), substring = true).assertExists()
        compose.onNodeWithText("4Runner", substring = true).assertDoesNotExist()

        compose.onNode(hasText(g.string(R.string.archived_view_history)) and hasClickAction()).click()
        compose.onNode(hasText(g.string(R.string.delete)) and hasClickAction()).click()
        assertEquals(listOf("history $id", "delete $id"), events)
    }

    @Test
    fun `moving back to the garage keeps the final reading, and Undo archives it again on the same day`() {
        val id = g.add(VehicleFixtures.wagon.copy(nickname = "Old Faithful", archivedOn = soldOn), firstReading = 210_000)
        show(until = "Old Faithful")
        compose.onNode(hasText(g.string(R.string.archived_move_back)) and hasClickAction()).click()

        compose.awaitThat { messages.isNotEmpty() }
        assertEquals(g.string(R.string.archived_moved_back, "Old Faithful"), messages.single().first)
        assertNull(g.vehicle(id)!!.archivedOn)
        assertEquals(listOf(210_000), runBlocking { g.db.odometer().readingsFor(id) }.map(OdometerReadingEntity::odometer))
        compose.awaitThat { compose.onAllNodes(hasText(g.string(R.string.archived_empty))).fetchSemanticsNodes().isNotEmpty() }

        messages.single().second()
        compose.awaitThat { g.db.vehicles().byId(id)?.archivedOn == soldOn }
    }

    @Test
    fun `the archiver logs a higher final odometer as a reading, and its Undo takes only that away`() = runBlocking {
        val id = g.add(VehicleFixtures.wagon, firstReading = 150_000)
        val receipt = g.archiver.archive(id, Sale(soldOn, 151_200), lastOdometer = 150_000)
        assertEquals(soldOn, g.vehicle(id)!!.archivedOn)
        assertEquals(listOf(150_000, 151_200), g.db.odometer().readingsFor(id).map { it.odometer }.sorted())

        g.archiver.undo(receipt)
        eventually { g.db.vehicles().byId(id)?.archivedOn == null && g.db.odometer().readingsFor(id).map { it.odometer } == listOf(150_000) }

        // A sale at the last reading logs nothing new.
        val same = g.archiver.archive(id, Sale(soldOn, 150_000), lastOdometer = 150_000)
        assertNull(same.readingId)
    }
}
