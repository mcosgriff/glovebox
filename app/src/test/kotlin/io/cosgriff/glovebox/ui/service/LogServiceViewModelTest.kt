package io.cosgriff.glovebox.ui.service

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
                    routeHandle(route), t.db.vehicles(), t.db.schedule(), t.db.service(), t.db.odometer(), t.settings,
                    t.db.catalogRules(), t.actions, t.picker,
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
