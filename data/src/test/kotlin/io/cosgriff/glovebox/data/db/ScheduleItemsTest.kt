package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.milestones.StartedFrom
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ImportedMilestone
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.KnownEngine
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSpecSource
import io.cosgriff.glovebox.domain.MilestoneImport
import io.cosgriff.glovebox.domain.ScheduleTemplateItem
import io.cosgriff.glovebox.domain.ScheduleTemplates
import io.cosgriff.glovebox.domain.TableJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * A vehicle's schedule items as [ScheduleItems] builds and writes them (#620), over an in-memory database through
 * Android's own driver, as [MigrationTest] explains. The maker's list is made up here, not read from the app's assets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScheduleItemsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val schedule = db.schedule()

    @After fun close() = db.close()

    /** A made-up list: an oil change with a severe interval in miles alone and an E85 one, a job with no severe figure, and a severe-only job. */
    private val list = KnownScheduleList(
        id = "madeup-truck-2020-2024",
        title = "2020–2024 Made-up Truck",
        make = "Madeup",
        model = "Truck",
        years = 2020..2024,
        source = KnownSpecSource("Made-up guide", null, "2026-10-24", "Made up for a test.", "Test data."),
        unit = DistanceUnit.MILES,
        milestones = listOf(ImportedMilestone(5_000, listOf(TableJob("Replace engine oil", "oil")))),
        intervals = listOf(
            ImportedScheduleItem("Replace engine oil", Interval(10_000, 12), severe = Interval(5_000, null), service = "oil"),
            ImportedScheduleItem("Rotate tires", Interval(5_000, 6), service = "rotation"),
            ImportedScheduleItem("Replace differential oil", Interval(15_000, null), severeOnly = true),
        ),
        e85 = mapOf(MilestoneImport.nameKey("Replace engine oil") to Interval(5_000, 6)),
        engine = KnownEngine("2.4L I4", 2.4),
    )

    private val makers = MakersSchedules({ listOf(list) }, db)
    private val items = ScheduleItems(makers, db)

    private fun truck() = testVehicle(make = "Madeup", model = "Truck", year = 2022).copy(engine = "2.4L I4")
    private fun hatch() = testVehicle(make = "Volkswagen", model = "Golf", year = 2008).copy(drivetrain = "FWD", fuel = "Gasoline")

    private suspend fun vehicle(id: Long) = db.vehicles().byId(id)!!
    private suspend fun rows(id: Long) = schedule.allFor(id)
    private fun typicalName(t: ScheduleTemplateItem) = "Typical ${t.key}"

    /** The typical list as setup saves it. */
    private suspend fun setUpTypical(id: Long, severe: Boolean = false) {
        val drafts = ScheduleItems.typicalDrafts(vehicle(id), ::typicalName).map { it.copy(useSevere = severe) }
        assertTrue(items.setUp(id, drafts, list = null))
    }

    @Test
    fun `setup saves each draft as its row, and a row reads back as the same draft`() = runBlocking {
        val car = db.vehicles().insert(hatch())
        val own = DraftItem(
            key = "custom-0", name = "Job", normal = Interval(10_000, 12), severe = Interval(5_000, 6), useSevere = true,
            rule = DueRule.TIME_ONLY, service = "oil", firstAt = 60_000, e85 = Interval(4_000, null), useE85 = true,
        )
        val template = ScheduleTemplates.generic("FWD", "Gasoline", DistanceUnit.MILES).first()
        // A typical item never carries a service onto its row, and no E85 interval is never on it, whatever the switch.
        val typical = template.toDraft("Typical").copy(service = "oil", useE85 = true, tracked = false)

        assertTrue(items.setUp(car, listOf(own, typical), list = null))

        val saved = rows(car).associateBy { it.name }
        assertEquals(own.copy(key = saved.getValue("Job").id.toString()), saved.getValue("Job").toDraft(null))
        val row = saved.getValue("Typical")
        assertEquals(template.key, row.templateKey)
        assertNull(row.service)
        assertFalse(row.useE85)
        assertFalse(row.tracked)
        assertNull(row.copy(intervalMiles = null, intervalMonths = null).toDraft(null))
        // The typical list brings no table.
        assertEquals(0, db.milestones().count(car))
    }

    @Test
    fun `setup on a maker's schedule saves its items as the owner's own, with its table`() = runBlocking {
        val car = db.vehicles().insert(truck())
        val drafts = ScheduleItems.drafts(list, vehicle(car))
        assertEquals(list.intervals.map { it.name }, drafts.map { it.name })
        assertTrue(drafts.all { it.own && it.knownList == list.id && it.template == null })

        assertTrue(items.setUp(car, drafts, list))

        assertTrue(rows(car).all { it.knownList == list.id && it.templateKey == null && it.tracked })
        assertEquals(1, db.milestones().count(car))
        assertEquals(StartedFrom("Made-up guide, 2020–2024 Made-up Truck"), items.startedFrom(rows(car)))
    }

    @Test
    fun `setup is refused for a vehicle that already has items, even two at once`() = runBlocking {
        val car = db.vehicles().insert(truck())
        val drafts = ScheduleItems.drafts(list, vehicle(car))

        // A double-tapped Save: both writes start before either has finished.
        val done = listOf(async { items.setUp(car, drafts, list) }, async { items.setUp(car, drafts, list) }).awaitAll()

        assertEquals(listOf(true, false), done.sortedDescending())
        assertEquals(drafts.size, rows(car).size)
        assertEquals(1, db.milestones().count(car))
        // A vehicle that already has items, however they got there, is never set up again.
        val other = db.vehicles().insert(hatch())
        items.add(other, DraftItem.blank("new", DistanceUnit.MILES).copy(name = "Own"))
        assertFalse(items.setUp(other, ScheduleItems.typicalDrafts(vehicle(other), ::typicalName), list = null))
        assertEquals(listOf("Own"), rows(other).map { it.name })
    }

    @Test
    fun `an edit writes what the editor shows, and keeps a snooze set after the item was read`() = runBlocking {
        val car = db.vehicles().insert(hatch())
        setUpTypical(car)
        val read = rows(car).first { it.templateKey == "rotation" }
        val draft = read.toDraft(null)!!
        // Set after the editor opened: a whole-row write from the copy would undo it.
        schedule.setSnooze(read.id, 9_000, LocalDate.of(2026, 6, 1))

        items.edit(read.id, draft.copy(name = "Rotate tyres", normal = Interval(6_000, 6), service = "oil", rule = DueRule.MILEAGE_ONLY))

        val row = rows(car).first { it.id == read.id }
        assertEquals("Rotate tyres", row.name)
        assertEquals(6_000 to 6, row.intervalMiles to row.intervalMonths)
        assertEquals(DueRule.MILEAGE_ONLY.name, row.ruleOverride)
        assertEquals(9_000, row.snoozedUntilOdometer)
        assertEquals("rotation", row.templateKey)
        // Never a service on a typical item: it answers through its template key.
        assertNull(row.service)
    }

    @Test
    fun `a new item of the owner's own is added with its service`() = runBlocking {
        val car = db.vehicles().insert(hatch())
        items.add(car, DraftItem.blank("new", DistanceUnit.MILES).copy(name = "Grease hitch", normal = Interval(3_000, null), service = "belt-check"))

        val row = rows(car).single()
        assertEquals("Grease hitch", row.name)
        assertEquals("belt-check", row.service)
        assertEquals(3_000, row.intervalMiles)
        assertNull(row.templateKey)
    }

    @Test
    fun `ticking, select all and clear all change only tracked, and only on this vehicle`() = runBlocking {
        val car = db.vehicles().insert(hatch())
        val other = db.vehicles().insert(hatch())
        setUpTypical(car)
        setUpTypical(other)
        val first = rows(car).first()
        schedule.setSnooze(first.id, 9_000, null)

        items.setTracked(first.id, false)
        assertEquals(listOf(first.id), rows(car).filter { !it.tracked }.map { it.id })
        items.setAllTracked(car, false)
        assertTrue(rows(car).none { it.tracked })
        assertEquals(9_000, rows(car).first { it.id == first.id }.snoozedUntilOdometer)
        items.setAllTracked(car, true)
        assertTrue(rows(car).all { it.tracked })
        assertTrue(rows(other).all { it.tracked })
    }

    @Test
    fun `severe use is set on every item of the vehicle, and E85 only on items with an E85 interval`() = runBlocking {
        val car = db.vehicles().insert(truck())
        val other = db.vehicles().insert(truck())
        items.setUp(car, ScheduleItems.drafts(list, vehicle(car)), list)
        items.setUp(other, ScheduleItems.drafts(list, vehicle(other)), list)

        items.setSevereUse(car, true)
        assertTrue(rows(car).all { it.useSevere })
        assertEquals(DrivingUse.SEVERE, ScheduleItems.use(rows(car).mapNotNull { it.toDraft(null) }))
        assertTrue(rows(other).none { it.useSevere })
        items.setSevereUse(car, false)
        assertTrue(rows(car).none { it.useSevere })

        items.setE85Use(car, true)
        val (fuelled, plain) = rows(car).partition { it.e85Miles != null || it.e85Months != null }
        assertEquals(listOf("Replace engine oil"), fuelled.map { it.name })
        assertTrue(fuelled.all { it.useE85 })
        assertTrue(plain.none { it.useE85 })
        assertEquals(true, ScheduleItems.e85(rows(car).mapNotNull { it.toDraft(null) }))
        items.setE85Use(car, false)
        assertTrue(rows(car).none { it.useE85 })
    }

    @Test
    fun `the missing typical items are added unticked, on severe use like the schedule, and only once`() = runBlocking {
        val car = db.vehicles().insert(hatch())
        // One of the owner's own already resets the oil: the typical oil change isn't offered again.
        items.add(car, DraftItem.blank("new", DistanceUnit.MILES).copy(name = "Synthetic oil", normal = Interval(10_000, 12), severe = Interval(5_000, 6), service = "oil"))
        items.setSevereUse(car, true)
        val missing = ScheduleItems.missing(vehicle(car), rows(car))
        assertTrue(missing.none { it.key == "oil" })
        assertTrue(missing.any { it.key == "rotation" })

        items.addMissing(car, ::typicalName)
        items.addMissing(car, ::typicalName)

        val added = rows(car).filter { it.templateKey != null }
        assertEquals(missing.map { it.key }.toSet(), added.map { it.templateKey }.toSet())
        assertEquals(missing.size, added.size)
        assertTrue(added.none { it.tracked })
        assertEquals(missing.map { "Typical ${it.key}" }.toSet(), added.map { it.name }.toSet())
        // Every one of them, whether or not severe use changes its interval (#620's open question).
        assertTrue(added.all { it.useSevere })
        assertEquals(emptyList<Any>(), ScheduleItems.missing(vehicle(car), rows(car)))
    }

    @Test
    fun `the maker's schedule is offered to a vehicle set up on the typical list, and taken up once`() = runBlocking {
        val car = db.vehicles().insert(truck())
        setUpTypical(car)
        val typicalOil = rows(car).first { it.templateKey == "oil" }
        val offer = items.offer(vehicle(car), rows(car))
        assertNotNull(offer)
        val (offered, plan) = offer!!
        assertEquals(list.id, offered.id)
        assertTrue(typicalOil.id in plan.doubles.map { it.id })
        assertNull(items.startedFrom(rows(car)))

        items.takeUpMakers(car, stop = setOf(typicalOil.id))
        items.takeUpMakers(car, stop = setOf(typicalOil.id))

        val taken = rows(car).filter { it.knownList == list.id }
        assertEquals(list.intervals.map { it.name }.toSet(), taken.map { it.name }.toSet())
        assertEquals(list.intervals.size, taken.size)
        assertTrue(taken.all { it.tracked && it.templateKey == null })
        // The double it stops had nothing logged against it, so it is gone; the rest stay.
        assertTrue(rows(car).none { it.id == typicalOil.id })
        assertTrue(rows(car).first { it.templateKey == "rotation" }.tracked)
        assertEquals(1, db.milestones().count(car))
        assertNull(items.offer(vehicle(car), rows(car)))
        assertEquals(StartedFrom("Made-up guide, 2020–2024 Made-up Truck"), items.startedFrom(rows(car)))
    }

    @Test
    fun `on severe use the maker's items are on severe use only where it changes something, and on E85 only where the schedule is`() = runBlocking {
        val car = db.vehicles().insert(truck())
        setUpTypical(car, severe = true)
        assertEquals(DrivingUse.SEVERE, ScheduleItems.use(rows(car).mapNotNull { it.toDraft(null) }))

        items.takeUpMakers(car, stop = emptySet())

        val taken = rows(car).filter { it.knownList == list.id }.associateBy { it.name }
        assertTrue(taken.getValue("Replace engine oil").useSevere)
        assertTrue(taken.getValue("Replace differential oil").useSevere)
        // No severe figure: left on normal (#620's open question; setup and the missing items set every one).
        assertFalse(taken.getValue("Rotate tires").useSevere)
        // Nothing on the schedule said E85, so nothing is assumed.
        assertTrue(taken.values.none { it.useE85 })
    }

    @Test
    fun `a list taken up that no longer ships is credited to the maker's guide, unnamed`() = runBlocking {
        val car = db.vehicles().insert(truck())
        schedule.insertAll(listOf(ImportedScheduleItem("Replace engine oil", Interval(5_000, 6)).toEntity(car, knownList = "madeup-gone")))

        assertEquals(StartedFrom(null), items.startedFrom(rows(car)))
    }

    @Test
    fun `an interval from a file becomes one of the owner's own items, tracked`() = runBlocking {
        val car = db.vehicles().insert(hatch())
        val imported = ImportedScheduleItem(
            name = "Made-up job", normal = Interval(30_000, 36), severe = Interval(15_000, 18), e85 = Interval(20_000, 24),
            rule = DueRule.WHICHEVER_LAST, service = "coolant", firstAt = 100_000, severeOnly = false,
        )
        schedule.insertAll(listOf(imported.toEntity(car)))

        val row = rows(car).single()
        assertEquals(listOf(30_000, 36, 15_000, 18, 20_000, 24), listOf(row.intervalMiles, row.intervalMonths, row.severeMiles, row.severeMonths, row.e85Miles, row.e85Months))
        assertEquals(listOf("WHICHEVER_LAST", "coolant", null, 100_000), listOf(row.ruleOverride, row.service, row.knownList, row.firstMiles))
        assertTrue(row.tracked)
        assertFalse(row.useSevere || row.useE85)
        assertNull(row.templateKey)
    }
}
