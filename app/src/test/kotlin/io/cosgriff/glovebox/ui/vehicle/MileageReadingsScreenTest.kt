package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.MileageReadingsRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Mileage readings with its real ViewModel: a standalone reading is corrected or deleted here, with Undo; a record's opens the record. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class MileageReadingsScreenTest {
    private val g = VehicleTestGarage()
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(g).around(compose)

    private val unit = DistanceUnit.MILES
    private val messages = mutableListOf<Triple<String, String, () -> Unit>>()
    private val openedRecords = mutableListOf<Long>()
    private var vehicleId = 0L
    private var readingId = 0L
    private var recordId = 0L

    private fun garage() {
        vehicleId = g.add(VehicleFixtures.wagon)
        runBlocking {
            readingId = g.db.odometer().insert(OdometerReadingEntity(vehicleId = vehicleId, date = LocalDate.now().minusDays(20), odometer = 61_000))
            recordId = g.db.service().insertWithItems(
                ServiceRecordEntity(vehicleId = vehicleId, date = LocalDate.now().minusDays(40), odometer = 60_200, diy = true),
                listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Wiper blades")),
            )
        }
    }

    /** Shows the screen, once [until] is on it. */
    private fun show(until: String = 61_000.asDistance(unit)) {
        val vm = g.viewModel { MileageReadingsViewModel(routeHandle(MileageReadingsRoute(vehicleId)), g.logs, g.db.odometer(), g.readingUndo) }
        compose.setContent {
            GloveboxTheme {
                MileageReadingsScreen(onBack = {}, onOpenRecord = { openedRecords += it }, onMessage = { m, a, undo -> messages += Triple(m, a, undo) }, vm = vm)
            }
        }
        compose.awaitThat { compose.onAllNodes(hasText(until)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun row(miles: Int) = compose.onNode(hasText(miles.asDistance(unit)) and hasClickAction())
    private fun odometerField() = compose.onNode(hasSetTextAction() and hasText(g.string(R.string.odometer_in, unit.symbolWord)))
    private fun readings() = runBlocking { g.db.odometer().readingsFor(vehicleId) }

    @Test
    fun `a reading logged with a service opens its record`() {
        garage()
        show()
        row(60_200).performScrollTo().click()
        assertEquals(listOf(recordId), openedRecords)
        compose.onNodeWithText(g.string(R.string.readings_correct_title)).assertDoesNotExist()
    }

    @Test
    fun `a corrected reading is saved as typed, and Undo puts the old one back`() {
        garage()
        show()
        row(61_000).performScrollTo().click()
        odometerField().performTextClearance()
        odometerField().performTextInput("61900")
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).click()

        compose.awaitThat { messages.isNotEmpty() }
        assertEquals(listOf(61_900), readings().map { it.odometer })
        val (message, action, undo) = messages.single()
        assertEquals(g.string(R.string.readings_corrected, 61_900.asDistance(unit)), message)
        assertEquals(g.string(R.string.undo), action)

        undo()
        compose.awaitThat { readings().map { it.odometer } == listOf(61_000) }
    }

    @Test
    fun `saving a reading unchanged writes nothing and offers no Undo`() {
        garage()
        show()
        row(61_000).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).click()
        compose.waitForIdle()
        assertTrue(messages.isEmpty())
        assertEquals(listOf(61_000), readings().map { it.odometer })
    }

    @Test
    fun `an empty reading isn't saved, and says why`() {
        garage()
        show()
        row(61_000).performScrollTo().click()
        odometerField().performTextClearance()
        compose.onNode(hasText(g.string(R.string.save)) and hasClickAction()).click()
        compose.onNodeWithText(g.string(R.string.readings_problem_not_a_reading)).assertExists()
        assertTrue(messages.isEmpty())
        assertEquals(listOf(61_000), readings().map { it.odometer })
    }

    @Test
    fun `a deleted reading goes at once, and Undo puts it back`() {
        garage()
        show()
        row(61_000).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.readings_delete)) and hasClickAction()).click()

        compose.awaitThat { messages.isNotEmpty() }
        assertTrue(readings().isEmpty())
        assertEquals(g.string(R.string.readings_deleted, 61_000.asDistance(unit)), messages.single().first)
        messages.single().third()
        compose.awaitThat { readings().map { it.id to it.odometer } == listOf(readingId to 61_000) }
    }

    @Test
    fun `Undo after the vehicle has been deleted puts nothing back`() {
        garage()
        show()
        row(61_000).performScrollTo().click()
        compose.onNode(hasText(g.string(R.string.readings_delete)) and hasClickAction()).click()
        compose.awaitThat { messages.isNotEmpty() }

        val reading = OdometerReadingEntity(id = readingId, vehicleId = vehicleId, date = LocalDate.now().minusDays(20), odometer = 61_000)
        runBlocking { g.db.vehicles().delete(g.db.vehicles().byId(vehicleId)!!) }
        g.readingUndo.restore(reading)
        Thread.sleep(200)
        assertTrue(readings().isEmpty())
    }

    @Test
    fun `a vehicle with no readings says so`() {
        vehicleId = g.add(VehicleFixtures.wagon)
        show(until = g.string(R.string.readings_none))
    }

}
