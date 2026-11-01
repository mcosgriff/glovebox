package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.data.log.VehicleLogs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.RecordMark
import io.cosgriff.glovebox.ui.LogServiceRoute
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.cosgriff.glovebox.domain.PartField
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class LogServiceViewModelTest {
    private val t = ServiceTestDb()
    private val today: LocalDate = LocalDate.now()
    private val store = ViewModelStore()

    @Before fun main() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After fun close() {
        store.clear()
        Dispatchers.resetMain()
        t.close()
    }

    private fun open(route: LogServiceRoute): LogServiceViewModel {
        val vm = ViewModelProvider.create(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LogServiceViewModel(
                    routeHandle(route), VehicleLogs(t.db.vehicles(), t.db.schedule(), t.db.service(), t.db.odometer(), t.settings), t.db.service(),
                    t.db.catalogRules(), t.actions, t.picker, t.invoices, t.invoicePhotos, t.db.recommendations(),
                ) as T
            },
        )[route.toString(), LogServiceViewModel::class]
        return vm
    }

    private fun loaded(route: LogServiceRoute) = open(route).also { vm -> awaitUntil("the form to load") { vm.form.data != null } }

    /** A vehicle with an oil change logged two years ago (overdue now) and a rotation never logged. */
    private suspend fun truck(unit: String = "MILES"): Triple<Long, Long, Long> {
        val car = t.vehicle(model = "Tacoma", unit = unit, firstReading = 30_000, nickname = "Blue truck")
        t.db.schedule().applyImport(
            listOf(
                ScheduleItemEntity(vehicleId = car, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6),
                ScheduleItemEntity(vehicleId = car, name = "Tire rotation", templateKey = "rotation", intervalMiles = 5_000, intervalMonths = 6),
            ),
            emptyList(), emptyList(),
        )
        val items = t.db.schedule().allFor(car)
        val oil = items.single { it.templateKey == "oil" }.id
        val rotation = items.single { it.templateKey == "rotation" }.id
        t.record(car, date = today.minusYears(2), odometer = 31_000, lines = listOf(line("Oil changed", "oil", oil)))
        t.db.odometer().insert(io.cosgriff.glovebox.data.db.OdometerReadingEntity(vehicleId = car, date = today.minusDays(3), odometer = 52_500))
        return Triple(car, oil, rotation)
    }

    @Test
    fun `a new form never fills the odometer in, and offers only the due job that has a log entry`() = runBlocking {
        val (car, _, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        val form = vm.form
        val data = form.data!!

        assertFalse(vm.editing)
        assertEquals("", form.odometerText)
        assertNull(form.odometer)
        assertEquals(today, form.date)
        assertEquals(52_500, data.lastOdometer)
        assertEquals(DistanceUnit.MILES, data.unit)
        assertEquals(listOf(DueSuggestion("oil", catalogLabel("oil"), overdue = true)), data.due)
        assertTrue(form.selected.isEmpty())
        assertEquals(data.categories.first().id, form.categoryId)
        assertTrue(form.vehicleName().contains("Blue truck"))
    }

    @Test
    fun `opened from a reminder, that job is ticked and its category open`() = runBlocking {
        val (car, _, rotation) = truck()
        val form = loaded(LogServiceRoute(car, scheduleItemId = rotation)).form
        assertEquals(setOf("rotation"), form.selected)
        assertEquals("tires", form.categoryId)
    }

    @Test
    fun `a saved service is written as typed, with History pointed at it`() = runBlocking {
        val (car, oil, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        val form = vm.form
        form.odometerText = "53,000"
        form.toggle("oil")
        form.addCustom("  Cabin light bulb  ")
        form.diy = false
        form.shopName = "  Made-up Motors "
        form.costText = "84.50"
        form.notes = " Synthetic. "
        var done = false
        vm.save { done = true }
        awaitUntil("the save") { done }

        val saved = t.db.service().recordsFor(car).maxBy { it.record.date }
        assertEquals(today, saved.record.date)
        assertEquals(53_000, saved.record.odometer)
        assertFalse(saved.record.diy)
        assertEquals("Made-up Motors", saved.record.shopName)
        assertEquals(8_450L, saved.record.costCents)
        assertEquals("Synthetic.", saved.record.notes)
        assertFalse(saved.record.enteredAsLastDone)
        assertEquals(
            setOf(Triple(oil, catalogLabel("oil"), "oil"), Triple(null, "Cabin light bulb", null)),
            saved.items.map { Triple(it.scheduleItemId, it.label, it.catalogId) }.toSet(),
        )
        assertEquals(HistoryFocus(car, saved.record.id, RecordMark.ADDED), t.actions.focus.value)
    }

    /** #572: made-up shop, work and prices. */
    @Test
    fun `an invoice's recommendations are kept, noted or let go on save, and logged work closes an open one when left ticked`() = runBlocking {
        val (car, _, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        val form = vm.form
        form.fill(
            io.cosgriff.glovebox.domain.InvoiceFill(
                date = today, odometer = 53_000, shop = "Made-up Motors", totalCents = null, jobs = emptyList(),
                recommendations = listOf(
                    io.cosgriff.glovebox.domain.RecommendedItem("Battery Replacement", 28_900),
                    io.cosgriff.glovebox.domain.RecommendedItem("Oil pan seeping", null, "Fluid Leaks", colour = io.cosgriff.glovebox.domain.FindingColour.AMBER),
                    io.cosgriff.glovebox.domain.RecommendedItem("Wiper Blade Replacement", 4_999),
                ),
                odometerIn = null, vinMismatch = null, unread = emptySet(), leftOut = emptyList(),
            ),
            notes = "",
        )
        form.toggle("oil")
        form.toggleKeep(1)
        form.toggleNote(1)
        form.toggleKeep(2)
        var done = false
        vm.save { done = true }
        awaitUntil("the save") { done }

        val saved = t.db.service().recordsFor(car).maxBy { it.record.date }
        assertEquals(t.recommendations(car).map { it.item.text }, listOf("Battery Replacement"))
        assertEquals("battery", t.recommendations(car).single().line)
        assertTrue(saved.record.notes!!.contains("Oil pan seeping"))
        assertFalse(saved.record.notes!!.contains("Wiper"))

        // The owner replaces the battery: the form asks, and saving with it ticked closes it.
        store.clear()
        val again = loaded(LogServiceRoute(car))
        again.form.odometerText = "53,100"
        again.form.toggle("battery")
        assertEquals(listOf("Battery Replacement"), again.form.doneMatches.map { it.recommendation.item.text })
        var saved2 = false
        again.save { saved2 = true }
        awaitUntil("the second save") { saved2 }
        assertTrue(t.recommendations(car).none { it.open })
    }

    @Test
    fun `a form missing what it needs, or dated in the future, saves nothing and says so`() = runBlocking {
        val (car, _, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        val form = vm.form
        var done = false

        vm.save { done = true }
        assertTrue(form.showErrors)

        form.odometerText = "53000"
        form.toggle("oil")
        form.costText = "12.3.4"
        assertFalse(form.costValid)
        vm.save { done = true }

        form.costText = ""
        form.date = today.plusDays(1)
        assertTrue(form.dateInFuture)
        vm.save { done = true }

        Thread.sleep(100)
        assertFalse(done)
        assertEquals(1, t.db.service().recordsFor(car).size)
    }

    @Test
    fun `a shop is only saved for a shop job, and a blank one not at all`() = runBlocking {
        val (car, _, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        vm.form.odometerText = "53000"
        vm.form.toggle("oil")
        vm.form.shopName = "Made-up Motors"
        vm.form.diy = true
        var done = false
        vm.save { done = true }
        awaitUntil("the save") { done }
        assertNull(t.db.service().recordsFor(car).maxBy { it.record.date }.record.shopName)
    }

    @Test
    fun `jobs are saved with their prices, an edit opens on them, and an unpriced line alone has no job`() = runBlocking {
        val (car, _, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        val form = vm.form
        form.odometerText = "53,000"
        form.toggle("oil")
        form.toggle("rotation")
        form.addCustom("Wiper fluid")
        form.diy = false
        form.costText = "120.00"
        // Made-up figures: the oil change and the rotation priced as one job, the fluid not at all.
        val byLine = form.jobs.associateBy { it.lines.single() }
        form.join(byLine.getValue("rotation").id, byLine.getValue("oil").id)
        form.setLabor(byLine.getValue("oil").id, "40")
        form.setParts(byLine.getValue("oil").id, "12.3.4")
        assertFalse(form.pricesValid)
        var done = false
        vm.save { done = true }
        Thread.sleep(100)
        assertFalse(done)

        form.setParts(byLine.getValue("oil").id, "55.50")
        assertEquals(2_450L, form.breakdown!!.gapCents)
        vm.save { done = true }
        awaitUntil("the save") { done }

        val saved = t.db.service().recordsFor(car).maxBy { it.record.date }
        val job = saved.jobs.single()
        assertEquals(4_000L, job.laborCents)
        assertEquals(5_550L, job.partsCents)
        assertNull(job.wording)
        // The total stays as typed; it is never worked out from the prices.
        assertEquals(12_000L, saved.record.costCents)
        assertEquals(
            mapOf("oil" to job.id, "rotation" to job.id, null to null),
            saved.items.associate { it.catalogId to it.jobId },
        )

        val edit = loaded(LogServiceRoute(car, recordId = saved.record.id))
        awaitUntil("the edit to load") { edit.form.data != null }
        assertTrue(edit.form.pricesShown)
        assertEquals(
            listOf(listOf("oil", "rotation") to ("40.00" to "55.50"), listOf(customKey("Wiper fluid")) to ("" to "")),
            edit.form.jobs.map { it.lines to (it.labor to it.parts) },
        )
    }

    @Test
    fun `part rows and charges are saved as typed, and an edit opens on them`() = runBlocking {
        val (car, _, _) = truck()
        val vm = loaded(LogServiceRoute(car))
        val form = vm.form
        form.odometerText = "53,000"
        form.toggle("oil")
        form.toggle("rotation")
        form.diy = false
        // Made-up figures: the oil change and the rotation as one job, its parts itemized.
        val byLine = form.jobs.associateBy { it.lines.single() }
        form.join(byLine.getValue("rotation").id, byLine.getValue("oil").id)
        val job = byLine.getValue("oil").id
        form.setLabor(job, "40")
        form.setParts(job, "36.00")
        form.itemize(job)
        val oil = form.jobs.single { it.id == job }.rows!!.single().id
        // The parts figure typed before became the first row's total; two figures fill the third when exact.
        form.nameRow(job, oil, "Synthetic oil")
        form.typeRow(job, oil, PartField.QUANTITY, "4.5")
        assertEquals("8.00", form.jobs.single { it.id == job }.rows!!.single().each)
        form.tagRow(job, oil, "oil")
        form.addRow(job)
        val filter = form.jobs.single { it.id == job }.rows!!.last().id
        form.nameRow(job, filter, "Oil filter")
        form.typeRow(job, filter, PartField.EACH, "11.50")
        // Picked from the job's lines: named and tagged by the line. Typing a line's own name picks it too.
        form.addRow(job)
        val picked = form.jobs.single { it.id == job }.rows!!.last().id
        form.pickPart(job, picked, "rotation")
        assertEquals(form.lineName("rotation"), form.partText(form.jobs.single { it.id == job }.rows!!.last()))
        form.nameRow(job, picked, "")
        assertEquals(null to "", form.jobs.single { it.id == job }.rows!!.last().let { it.line to it.wording })
        form.nameRow(job, picked, form.lineName("rotation"))
        assertEquals("rotation" to "", form.jobs.single { it.id == job }.rows!!.last().let { it.line to it.wording })
        form.shopSuppliesText = "4.25"
        form.costText = "80.25"
        // A row with no total is left out of the parts figure: labor, oil and supplies make the total.
        assertEquals(0L, form.breakdown!!.gapCents)
        var done = false
        vm.save { done = true }
        awaitUntil("the save") { done }

        val saved = t.db.service().recordsFor(car).maxBy { it.record.date }
        assertEquals(425L, saved.record.shopSuppliesCents)
        assertNull(saved.record.taxCents)
        val written = saved.jobs.single()
        // The rows make the parts figure, so none is stored.
        assertNull(written.partsCents)
        val rows = saved.parts.sortedBy { it.position }
        // The picked row has no wording of its own: its line names it.
        assertEquals(listOf("Synthetic oil", "Oil filter", null), rows.map { it.wording })
        assertEquals(listOf(4_500L, null, null), rows.map { it.quantityMilli })
        assertEquals(listOf(800L, 1_150L, null), rows.map { it.eachCents })
        assertEquals(listOf(3_600L, null, null), rows.map { it.totalCents })
        assertEquals(listOf("oil", null, "rotation"), rows.map { r -> saved.items.firstOrNull { it.id == r.itemId }?.catalogId })

        val edit = loaded(LogServiceRoute(car, recordId = saved.record.id))
        awaitUntil("the edit to load") { edit.form.data != null }
        assertEquals("4.25", edit.form.shopSuppliesText)
        val opened = edit.form.jobs.single { it.rows != null }.rows!!
        assertEquals(listOf("4.5" to "oil", "" to null, "" to "rotation"), opened.map { it.quantity to it.line })
        // Nothing is filled in on opening: the filter's total stays blank.
        assertEquals("", opened[1].total)
    }

    @Test
    fun `an edit opens on the record as saved, keeps its wording and flags, and offers no due chips`() = runBlocking {
        val (car, oil, _) = truck()
        val id = t.db.service().insertWithItems(
            ServiceRecordEntity(
                vehicleId = car, date = today.minusDays(10), odometer = 52_000, diy = false, shopName = "Made-up Motors",
                costCents = 12_345, notes = "Imported note", enteredAsLastDone = true, imported = true,
            ),
            listOf(line("Oil and filter changed", "oil", oil), line("Hitch greased")),
        )
        val photo = t.photoFile("record-kept.jpg")
        t.db.service().insertPhotos(listOf(PhotoEntity(recordId = id, fileName = photo)))
        val other = t.record(car, date = today.minusDays(10), odometer = 52_000)
        t.actions.link(id, other)
        val visit = t.db.service().record(id)!!.record.visitId

        val vm = loaded(LogServiceRoute(car, recordId = id))
        val form = vm.form
        assertTrue(vm.editing)
        assertEquals(52_000.asNumber(), form.odometerText)
        assertEquals(today.minusDays(10), form.date)
        assertFalse(form.diy)
        assertEquals("Made-up Motors", form.shopName)
        assertEquals(editableCost(12_345), form.costText)
        assertEquals("Imported note", form.notes)
        assertEquals(setOf("oil"), form.selected)
        assertEquals(mapOf("oil" to "Oil and filter changed"), form.keptLabels)
        assertEquals(listOf("Hitch greased"), form.customItems)
        assertEquals(listOf(photo), form.keptPhotos.map { it.fileName })
        assertEquals("engine", form.categoryId)
        assertTrue(form.data!!.due.isEmpty())
        // The record being edited is left out of what its reading is checked against.
        assertEquals(52_500, form.data!!.lastOdometer)
        assertTrue(form.reminderChanges.isEmpty())

        form.odometerText = "52,100"
        assertEquals(1, form.reminderChanges.size)
        form.removeKeptPhoto(form.keptPhotos.single())
        var done = false
        vm.save { done = true }
        awaitUntil("the save") { done }

        val saved = t.db.service().record(id)!!
        assertEquals(52_100, saved.record.odometer)
        assertTrue(saved.record.enteredAsLastDone)
        assertTrue(saved.record.imported)
        assertEquals(visit, saved.record.visitId)
        assertEquals(setOf("Oil and filter changed", "Hitch greased"), saved.items.map { it.label }.toSet())
        assertTrue(saved.photos.isEmpty())
        // Kept until the Undo bar ends.
        assertTrue(t.hasFile(photo))
    }

    @Test
    fun `a kilometre vehicle's form is in kilometres`() = runBlocking {
        val (car, _, _) = truck(unit = "KILOMETERS")
        assertEquals(DistanceUnit.KILOMETERS, loaded(LogServiceRoute(car)).form.data!!.unit)
    }

    @Test
    fun `a form for a vehicle that has gone stays empty`() {
        val vm = open(LogServiceRoute(vehicleId = 404))
        Thread.sleep(200)
        assertNull(vm.form.data)
        var done = false
        vm.save { done = true }
        assertFalse(done)
    }

    @Test
    fun `a full record has no room for photos, and the picker is told`() = runBlocking {
        val (car, _, _) = truck()
        val id = t.record(car, date = today.minusDays(1), odometer = 52_400)
        t.db.service().insertPhotos((1..io.cosgriff.glovebox.domain.RecordPhotos.MAX_PER_RECORD).map { PhotoEntity(recordId = id, fileName = "record-$it.jpg") })
        val vm = loaded(LogServiceRoute(car, recordId = id))
        assertEquals(0, vm.photoRoom)
        vm.takePhoto()
        vm.choosePhotos()
        // No activity has attached the launchers here, so nothing is opened and nothing said.
        assertNull(vm.photoProblem.value)
        assertTrue(vm.newPhotos.value.isEmpty())
    }

    private fun LogServiceForm.vehicleName() = data!!.vehicleName
}
