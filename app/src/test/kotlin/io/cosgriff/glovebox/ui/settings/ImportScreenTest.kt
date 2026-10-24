package io.cosgriff.glovebox.ui.settings

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.domain.CarfaxRefusal
import io.cosgriff.glovebox.domain.ImportError
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportKind
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.ui.ImportRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** Settings → Import as the user goes through it: pick, read the preview, choose, confirm, and where it leads. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class ImportScreenTest {
    @get:Rule(order = 1) val compose = createComposeRule()

    @get:Rule(order = 0) val g = TestGraph()
    private val picker = ImportPicker(g.context, g.appScope)

    init {
        runBlocking { g.resetSettings() }
    }


    private fun vm(route: ImportRoute = ImportRoute()) = ImportViewModel(
        g.db.vehicles(), g.settings, g.db.torque(), g.db.schedule(), g.db.service(), g.db.odometer(), g.db.milestones(),
        g.db.catalogRules(), g.actions, picker, g.documents, routeHandle(route),
    )

    private fun pick(name: String) {
        val file = File(g.context.cacheDir, "picked-${System.nanoTime()}.json").apply { writeText(File("../docs/import/examples/$name").readText()) }
        picker.onPicked(Uri.fromFile(file))
    }

    @Test
    fun `replace matching is chosen, confirmed against the list of what it overwrites, and then leads to the specs`() {
        val id = runBlocking {
            g.vehicle().also { g.db.torque().insert(TorqueSpecEntity(vehicleId = it, name = "Lug nuts", category = "WHEELS", newtonMeters = 123.0)) }
        }
        var opened: Long? = null
        var asked = 0
        picker.attach { asked++ }
        compose.show { ImportScreen(onBack = {}, onOpenTorqueSpecs = { opened = it }, onOpenSchedule = {}, onEnterLastDone = {}, onOpenMilestones = {}, onOpenHistory = { _, _ -> }, vm = vm()) }

        compose.waitForText(text(R.string.import_choose))
        compose.onNodeWithText(text(R.string.import_choose)).performClick()
        assertEquals(1, asked)

        pick("torque-specs.json")
        compose.waitForText(text(R.string.import_replace))
        // Add new only is the default, and imports the three new rows.
        compose.onNodeWithText(text(R.string.import_button, counted(R.plurals.plan_rows, 3, "3"))).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.import_replace)).performClick()
        val four = text(R.string.import_button, counted(R.plurals.plan_rows, 4, "4"))
        compose.onNodeWithText(four).performScrollTo().performClick()
        // The confirmation names the row it will overwrite before anything is written.
        compose.onNodeWithText(text(R.string.import_replace_title, counted(R.plurals.plan_rows, 1, "1"))).assertIsDisplayed()
        assertEquals(1, runBlocking { g.db.torque().specsFor(id) }.size)
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertEquals(1, runBlocking { g.db.torque().specsFor(id) }.size)

        compose.onNodeWithText(four).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.import_replace_and_import)).performClick()
        compose.waitForText(text(R.string.import_see_torque))
        compose.onNodeWithText(text(R.string.import_replaced, counted(R.plurals.plan_rows, 1, "1"))).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.import_see_torque)).performClick()
        assertEquals(id, opened)
        assertEquals(4, runBlocking { g.db.torque().specsFor(id) }.size)
    }

    @Test
    fun `after a history import, See history points History at the vehicle`() {
        val id = runBlocking { g.vehicle() }
        var opened: Pair<Long, Boolean>? = null
        compose.show { ImportScreen(onBack = {}, onOpenTorqueSpecs = {}, onOpenSchedule = {}, onEnterLastDone = {}, onOpenMilestones = {}, onOpenHistory = { v, a -> opened = v to a }, vm = vm(ImportRoute(id))) }
        compose.waitForText(text(R.string.import_choose))
        // The vehicle it was opened from is named, with nothing to choose.
        compose.onNodeWithText(text(R.string.import_into)).assertIsDisplayed()
        pick("service-history.json")
        val button = text(R.string.import_button, counted(R.plurals.records, 3, "3"))
        compose.waitForText(button)
        compose.onNodeWithText(button).performScrollTo().performClick()
        compose.waitForText(text(R.string.import_see_history))
        compose.onNodeWithText(text(R.string.import_see_history)).performClick()
        assertEquals(id to false, opened)
        assertEquals(id, g.actions.focus.value?.vehicleId)
    }

    @Test
    fun `several vehicles are offered to choose from, a sold one marked sold`() {
        runBlocking {
            g.vehicle(model = "4Runner")
            val sold = g.vehicle(make = "Volkswagen", model = "GTI")
            g.db.vehicles().setArchived(sold, LocalDate.of(2025, 1, 1))
        }
        compose.show { ImportScreen(onBack = {}, onOpenTorqueSpecs = {}, onOpenSchedule = {}, onEnterLastDone = {}, onOpenMilestones = {}, onOpenHistory = { _, _ -> }, vm = vm()) }
        compose.waitForText(text(R.string.import_sold))
        pick("torque-specs.json")
        compose.waitForText(text(R.string.import_needs_vehicle))
        compose.onNodeWithText("GTI", substring = true).performClick()
        compose.waitForText(text(R.string.import_sold_only_history))
    }

    private fun content(
        preview: ImportPreview? = null,
        result: ImportResult? = null,
        vehicles: Int = 1,
        pickedName: String? = "file.json",
        pickedPdf: Boolean = false,
        problem: String? = null,
        onImport: (ImportPreview.Ready, ImportMode, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
        onLastDone: (Long) -> Unit = {},
        onSchedule: (Long) -> Unit = {},
        onMilestones: (Long) -> Unit = {},
        onPick: () -> Unit = {},
    ) {
        val list = (1..vehicles).map { io.cosgriff.glovebox.data.db.VehicleEntity(id = it.toLong(), vin = null, year = 2019, make = "Toyota", model = "Model $it", createdOn = LocalDate.of(2023, 1, 1)) }
        compose.show {
            ImportContent(
                list, list.firstOrNull()?.id, vehicleFixed = false, pickedName = pickedName, pickedPdf = pickedPdf, problem = problem, preview = preview, result = result,
                onBack = {}, onPick = onPick, onChooseVehicle = {}, onInclude = { _, _ -> }, onImport = onImport,
                onOpenTorqueSpecs = {}, onOpenSchedule = onSchedule, onEnterLastDone = onLastDone, onOpenMilestones = onMilestones, onOpenHistory = { _, _ -> },
            )
        }
    }

    private fun result(kind: ImportKind, added: Int = 2, keptCopy: Boolean? = null, stopped: Int = 0) = ImportResult(
        kind = kind, noun = PlanNoun.ITEM, vehicleId = 7, vehicleName = "2019 4Runner", archived = false,
        added = added, filled = 1, replaced = 0, unchanged = 3, stopped = stopped, note = "A note about the file.", keptCopy = keptCopy,
    )

    @Test
    fun `with no vehicle there is nothing to import into`() {
        content(vehicles = 0)
        compose.onNodeWithText(text(R.string.import_no_vehicle)).assertIsDisplayed()
    }

    @Test
    fun `a schedule import offers to enter when its new items were last done, and the schedule`() {
        var lastDone: Long? = null
        var schedule: Long? = null
        var picked = 0
        content(result = result(ImportKind.INTERVAL_SCHEDULE, stopped = 2), onLastDone = { lastDone = it }, onSchedule = { schedule = it }, onPick = { picked++ })
        compose.onNodeWithText(text(R.string.import_filled, counted(R.plurals.plan_items, 1, "1"))).assertIsDisplayed()
        compose.onNodeWithText(counted(R.plurals.import_stopped, 2, "2")).assertIsDisplayed()
        compose.onNodeWithText("A note about the file.").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_enter_last_done)).performClick()
        compose.onNodeWithText(text(R.string.import_see_schedule)).performClick()
        compose.onNodeWithText(text(R.string.import_another)).performClick()
        assertEquals(7L, lastDone)
        assertEquals(7L, schedule)
        assertEquals(1, picked)
    }

    @Test
    fun `a schedule import with nothing new doesn't ask about last done`() {
        content(result = result(ImportKind.INTERVAL_SCHEDULE, added = 0))
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasText(text(R.string.home_enter_last_done))).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `a milestone import leads to the milestones`() {
        var opened: Long? = null
        content(result = result(ImportKind.MILESTONE_SCHEDULE), onMilestones = { opened = it })
        compose.onNodeWithText(text(R.string.import_see_milestones)).performClick()
        assertEquals(7L, opened)
    }

    @Test
    fun `a kept report is said to be kept, and one that couldn't be is said too`() {
        content(result = result(ImportKind.SERVICE_HISTORY, keptCopy = true))
        compose.onNodeWithText(text(R.string.import_kept_copy)).assertIsDisplayed()
    }

    @Test
    fun `a report that couldn't be kept says so`() {
        content(result = result(ImportKind.SERVICE_HISTORY, keptCopy = false))
        compose.onNodeWithText(text(R.string.import_copy_failed)).assertIsDisplayed()
    }

    @Test
    fun `a file the reader refuses lists each error with its path`() {
        val invalid = ImportFile.Invalid(listOf(ImportError("items[0].name", "must not start or end with a space")), more = 2)
        content(preview = ImportPreview.Invalid(invalid), problem = "Couldn't read that one")
        compose.onNodeWithText(text(R.string.import_cant)).assertIsDisplayed()
        compose.onNodeWithText("items[0].name").assertIsDisplayed()
        compose.onNodeWithText("Must not start or end with a space").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.and_more, "2")).assertIsDisplayed()
        compose.onNodeWithText("Couldn't read that one").assertIsDisplayed()
    }

    @Test
    fun `a PDF that isn't a report, or has no visits with a mileage, says which`() {
        content(preview = ImportPreview.Invalid(ImportFile.Invalid(emptyList(), carfax = CarfaxRefusal.NoVisits(withoutMileage = 3))))
        compose.onNodeWithText(counted(R.plurals.carfax_no_visits_without_mileage, 3, "3")).assertIsDisplayed()
    }

    @Test
    fun `a PDF that isn't a Carfax report is called that`() {
        content(preview = ImportPreview.Invalid(ImportFile.Invalid(emptyList(), carfax = CarfaxRefusal.NotAReport)))
        compose.onNodeWithText(text(R.string.carfax_not_a_report)).assertIsDisplayed()
    }

    @Test
    fun `a report with no visits at all says so`() {
        content(preview = ImportPreview.Invalid(ImportFile.Invalid(emptyList(), carfax = CarfaxRefusal.NoVisits(withoutMileage = 0))))
        compose.onNodeWithText(text(R.string.carfax_no_visits)).assertIsDisplayed()
    }

    private fun view(
        new: Int = 0,
        differs: Int = 0,
        doubles: List<String> = emptyList(),
        doubts: List<DoubtLine> = emptyList(),
        moves: Map<ImportMode, List<String>> = emptyMap(),
    ) = PlanView(
        kind = ImportKind.SERVICE_HISTORY,
        noun = PlanNoun.RECORD,
        total = new + differs + 1,
        new = (1..new).map { PlanLine("New $it", "Oil & filter change") },
        fills = emptyList(),
        differs = (1..differs).map { PlanLine("Differs $it", "From the file", yours = "Yours now") },
        same = 1,
        caution = "Check what you import.",
        doubles = doubles,
        moves = moves,
        doubts = doubts,
    )

    @Test
    fun `a long list is cut short with how many more, and a file that changes nothing says so`() {
        content(preview = ImportPreview.Ready(view(new = 14)) { _, _ -> })
        compose.onNodeWithText(text(R.string.and_more, "2")).assertIsDisplayed()
        compose.onNodeWithText("New 12").assertIsDisplayed()
    }

    @Test
    fun `nothing new and nothing different is said plainly`() {
        content(preview = ImportPreview.Ready(view()) { _, _ -> })
        compose.onNodeWithText(text(R.string.import_nothing)).assertIsDisplayed()
    }

    @Test
    fun `only differences, under Add new only, would change nothing`() {
        content(preview = ImportPreview.Ready(view(differs = 1)) { _, _ -> })
        compose.onNodeWithText(text(PlanNoun.RECORD.replaceOnly)).assertIsDisplayed()
    }

    @Test
    fun `a doubtful reading, a typical twin, the reminders that move and keeping a report are all the user's to tick`() {
        val key = LocalDate.of(2024, 9, 1) to 450_000
        var included: Boolean? = null
        var imported: Triple<ImportMode, Boolean, Boolean>? = null
        val ready = ImportPreview.Ready(
            view(
                new = 1,
                doubles = listOf("Oil change"),
                doubts = listOf(DoubtLine(key, "September 1, 2024 · 450,000 mi", "Far past the last reading", included = false)),
                moves = mapOf(ImportMode.ADD_NEW to listOf("Oil change now counts from March 1, 2024")),
            ),
        ) { _, _ -> }
        compose.show {
            ImportContent(
                emptyList<io.cosgriff.glovebox.data.db.VehicleEntity>().plus(io.cosgriff.glovebox.data.db.VehicleEntity(id = 1, vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = LocalDate.of(2023, 1, 1))),
                1L, vehicleFixed = false, pickedName = "report.pdf", pickedPdf = true, problem = null, preview = ready, result = null,
                onBack = {}, onPick = {}, onChooseVehicle = {}, onInclude = { _, yes -> included = yes }, onImport = { _, mode, stop, keep -> imported = Triple(mode, stop, keep) },
                onOpenTorqueSpecs = {}, onOpenSchedule = {}, onEnterLastDone = {}, onOpenMilestones = {}, onOpenHistory = { _, _ -> },
            )
        }
        compose.onNodeWithText(counted(R.plurals.import_left_out, 1, counted(R.plurals.records, 1, "1"))).assertIsDisplayed()
        compose.onNodeWithText("September 1, 2024 · 450,000 mi").performScrollTo().performClick()
        assertEquals(true, included)
        compose.onNodeWithText("Oil change now counts from March 1, 2024").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(counted(R.plurals.import_stop_typical, 1, "1")).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.import_keep_copy)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.import_button, counted(R.plurals.records, 1, "1"))).performScrollTo().performClick()
        assertEquals(Triple(ImportMode.ADD_NEW, true, true), imported)
    }
}
