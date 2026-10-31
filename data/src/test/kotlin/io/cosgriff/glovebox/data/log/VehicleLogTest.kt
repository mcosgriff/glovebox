package io.cosgriff.glovebox.data.log

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.Snooze
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** A vehicle's log: every reading through its cluster swap, and read the one way through [VehicleLogs]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VehicleLogTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)

    /** A cluster put in on [day]: the old one read 150,000, the new one 20,000. */
    private val swap = ClusterSwap(day, offset = 130_000, startReading = 20_000)

    private fun vehicle(swapped: Boolean = false) = VehicleEntity(
        id = 1, vin = null, year = 2019, make = "Toyota", model = "Tacoma", createdOn = day,
        clusterSwappedOn = day.takeIf { swapped }, odometerOffset = if (swapped) 130_000 else 0, clusterStartReading = 20_000.takeIf { swapped },
    )

    private fun line(label: String, catalogId: String? = null, scheduleItemId: Long? = null) =
        ServiceRecordItemEntity(recordId = 0, scheduleItemId = scheduleItemId, label = label, catalogId = catalogId)

    private fun record(id: Long, date: LocalDate, odometer: Int, vararg lines: ServiceRecordItemEntity) =
        ServiceRecordWithItems(ServiceRecordEntity(id = id, vehicleId = 1, date = date, odometer = odometer, diy = true), lines.toList(), emptyList())

    @Test
    fun `a reading from before the swap is put in dash terms, and one after is left as typed`() {
        val before = record(1, day.minusDays(10), 140_000)
        val after = record(2, day.plusDays(10), 25_000)
        assertEquals(10_000, before.toEntry(swap, NoLinks).odometer)
        assertEquals(25_000, after.toEntry(swap, NoLinks).odometer)
        assertEquals(140_000, before.toEntry(null, NoLinks).odometer)

        val readings = listOf(OdometerReadingEntity(vehicleId = 1, date = day.minusDays(5), odometer = 149_000))
        assertEquals(listOf(OdometerReading(day.minusDays(5), 19_000), OdometerReading(day, 20_000)), readings.toReadings(swap))
        assertEquals(listOf(OdometerReading(day.minusDays(5), 149_000)), readings.toReadings(null))
    }

    @Test
    fun `the highest reading is in dash terms, so an old cluster's big figure doesn't win`() {
        val v = vehicle(swapped = true)
        val records = listOf(record(1, day.minusDays(10), 148_000), record(2, day.plusDays(3), 21_000))
        assertEquals(21_000, v.highestReading(records, emptyList()))
        assertEquals(swap, v.clusterSwap())
        assertNull(vehicle().clusterSwap())
        assertEquals(148_000, vehicle().highestReading(records, emptyList()))
    }

    @Test
    fun `a line counts for the item it was saved against and every item answering to its catalog line`() {
        val items = listOf(
            ScheduleItemEntity(id = 11, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6),
            ScheduleItemEntity(id = 12, vehicleId = 1, name = "Synthetic oil", service = "oil", intervalMiles = 10_000, intervalMonths = 12),
        )
        val r = record(1, day, 40_000, line("Oil and filter", "oil"))
        assertEquals(setOf(11L, 12L), r.toEntry(null, items.answering()).itemIds)
        assertEquals(emptySet<Long>(), r.toEntry(null, NoLinks).itemIds)
    }

    @Test
    fun `a vehicle's log is in dash terms, keeps every item, and leads in its own unit`() {
        val v = vehicle(swapped = true)
        val items = listOf(
            ScheduleItemEntity(id = 11, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = null),
            ScheduleItemEntity(id = 12, vehicleId = 1, name = "Untracked", intervalMiles = 5_000, intervalMonths = null, tracked = false),
        )
        val records = listOf(record(1, day.plusDays(1), 21_000, line("Oil", "oil", 11)))
        val log = v.log(items, records, emptyList(), Settings())
        assertEquals(v, log.vehicle)
        assertEquals(items, log.items)
        assertEquals(records, log.records)
        assertEquals(swap, log.swap)
        assertEquals(listOf(11L), log.tracked.map { it.id })
        assertEquals(21_000, log.odometer)
        assertEquals(Settings().leadFor(DistanceUnit.MILES), log.lead)
        assertEquals(Settings().defaultRule, log.defaultRule)
        assertEquals(1, log.statuses(day.plusDays(2)).size)
        assertEquals(log.tracked, log.inputs.items)
    }

    /** This item as its reminder on [on], through [VehicleEntity.tracked]: the one way an item becomes a reminder. */
    private fun ScheduleItemEntity.reminder(on: VehicleEntity = vehicle(), swap: ClusterSwap? = null) =
        on.tracked(listOf(this), DueRule.WHICHEVER_FIRST, swap).single()

    @Test
    fun `an item's own rule, snooze and swap are passed to its reminder`() {
        val snoozed = ScheduleItemEntity(id = 3, vehicleId = 1, name = "Wipers", intervalMiles = null, intervalMonths = 12, ruleOverride = DueRule.TIME_ONLY.name, snoozedUntilDate = day)
        val s = snoozed.reminder(swap = swap)
        assertEquals(DueRule.TIME_ONLY, s.rule)
        assertEquals(Snooze(untilDate = day), s.snooze)
        assertEquals(-130_000, s.zeroAt)
        assertEquals(DueRule.WHICHEVER_FIRST, snoozed.copy(ruleOverride = null).reminder().rule)
        // Untracked items, and a severe-only job off the severe schedule, are no reminder.
        assertTrue(vehicle().tracked(listOf(snoozed.copy(tracked = false), snoozed.copy(severeOnly = true)), DueRule.WHICHEVER_FIRST).isEmpty())
    }

    @Test
    fun `a first time reads back as typed, though the reminder counts it in dash terms`() {
        val belt = ScheduleItemEntity(id = 7, vehicleId = 1, name = "Timing belt", intervalMiles = 50_000, intervalMonths = null, firstMiles = 100_000)
        val t = belt.reminder(swap = swap)
        assertEquals(100_000 - 130_000, t.firstAt)
        assertEquals(100_000, t.firstMiles)
        assertNull(belt.copy(firstMiles = null).reminder(swap = swap).firstMiles)
    }

    @Test
    fun `an oil change in miles alone counts by Glovebox's months, and says so, while nothing is saved`() {
        val typical = ScheduleItemEntity(id = 4, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 10_000, intervalMonths = null)
        val known = ScheduleItemEntity(id = 5, vehicleId = 1, name = "Change engine oil and filter", service = "oil", intervalMiles = 10_000, intervalMonths = null)
        val other = ScheduleItemEntity(id = 6, vehicleId = 1, name = "Spark plugs", service = "spark-plugs", intervalMiles = 40_000, intervalMonths = null)
        listOf(typical, known).forEach { item ->
            val t = item.reminder()
            assertEquals(Interval(10_000, 12), t.interval)
            assertTrue(t.monthsWorkedOut)
            assertNull(item.intervalMonths)
            // The schedule's own figure, as Vehicle details puts it beside the owner's, has them too.
            assertEquals(Interval(10_000, 12), item.scheduledOil(DistanceUnit.MILES)!!.interval)
        }
        val plugs = other.reminder()
        assertEquals(Interval(40_000, null), plugs.interval)
        assertFalse(plugs.monthsWorkedOut)
        // A kilometre vehicle's 16,000 km is a manual's 10,000 mi.
        val km = vehicle().copy(distanceUnit = DistanceUnit.KILOMETERS.name)
        assertEquals(Interval(16_000, 12), typical.copy(intervalMiles = 16_000).reminder(on = km).interval)
    }

    @Test
    fun `the owner's own oil interval is what every oil change on the vehicle counts by, Severe or not`() {
        val severe = ScheduleItemEntity(id = 4, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 10_000, intervalMonths = 12, severeMiles = 5_000, severeMonths = 6, useSevere = true)
        val plugs = ScheduleItemEntity(id = 6, vehicleId = 1, name = "Spark plugs", service = "spark-plugs", intervalMiles = 40_000, intervalMonths = null)
        val owner = vehicle(swapped = false).copy(ownOilMiles = 7_500)
        val t = severe.reminder(on = owner)
        // Months follow the owner's miles until typed: Glovebox's, and said so.
        assertEquals(Interval(7_500, 9), t.interval)
        assertTrue(t.monthsWorkedOut)
        assertEquals(Interval(7_500, 9), severe.copy(useSevere = false).reminder(on = owner).interval)
        assertEquals(Interval(7_500, 12), severe.reminder(on = owner.copy(ownOilMonths = 12)).interval)
        assertEquals(Interval(40_000, null), plugs.reminder(on = owner).interval)
        // The schedule's own figure stays the item's.
        assertEquals(Interval(5_000, 6), severe.scheduledOil(DistanceUnit.MILES)!!.interval)
        // Use the maker's: none of the owner's, and the item's own figures count again.
        assertNull(vehicle(swapped = false).ownOil())
        assertEquals(Interval(5_000, 6), severe.reminder().interval)
    }

    @Test
    fun `a vehicle's log counts its oil change by the owner's interval`() {
        val v = vehicle(swapped = false).copy(ownOilMiles = 6_000, ownOilMonths = 6)
        val items = listOf(ScheduleItemEntity(id = 4, vehicleId = v.id, name = "Oil change", templateKey = "oil", intervalMiles = 10_000, intervalMonths = null))
        assertEquals(Interval(6_000, 6), v.log(items, emptyList(), emptyList(), Settings()).tracked.single().interval)
    }

    /**
     * #575: what an item counts by has one answer, [VehicleLog.tracked]. The reminder ([VehicleLog.statuses]) and the
     * rule preview (which reads `log.tracked(itemId)`) see the same interval, first time and whose months they are.
     */
    @Test
    fun `an oil change with the owner's own interval counts by it in the reminder and in what one item reads`() {
        val v = vehicle(swapped = false).copy(ownOilMiles = 6_000, ownOilMonths = 6)
        val items = listOf(
            ScheduleItemEntity(id = 4, vehicleId = v.id, name = "Oil change", templateKey = "oil", intervalMiles = 10_000, intervalMonths = 12, severeMiles = 5_000, useSevere = true),
            ScheduleItemEntity(id = 8, vehicleId = v.id, name = "Untracked", intervalMiles = 5_000, intervalMonths = 6, tracked = false),
        )
        val log = v.log(items, listOf(record(1, day, 40_000, line("Oil", "oil", 4))), emptyList(), Settings())
        val oil = log.tracked(4)!!
        assertEquals(Interval(6_000, 6), oil.interval)
        assertFalse(oil.monthsWorkedOut)
        assertEquals(oil, log.statuses(day).single { it.item.id == 4L }.item)
        assertNull(log.tracked(8))
        assertNull(log.tracked(99))
    }

    @Test
    fun `an oil change in miles alone counts by Glovebox's months in the reminder and in what one item reads`() {
        val v = vehicle(swapped = false)
        val items = listOf(ScheduleItemEntity(id = 4, vehicleId = v.id, name = "Oil change", templateKey = "oil", intervalMiles = 7_500, intervalMonths = null))
        val log = v.log(items, listOf(record(1, day, 40_000, line("Oil", "oil", 4))), emptyList(), Settings())
        val oil = log.tracked(4)!!
        // Both limits, so the rule preview takes it as its example, with the months said to be Glovebox's.
        assertEquals(Interval(7_500, 9), oil.interval)
        assertTrue(oil.monthsWorkedOut)
        assertEquals(oil, log.statuses(day).single().item)
        // The owner's own miles alone: their months are worked out the same way.
        val owners = v.copy(ownOilMiles = 5_000).log(items, emptyList(), emptyList(), Settings()).tracked(4)!!
        assertEquals(Interval(5_000, 6), owners.interval)
        assertTrue(owners.monthsWorkedOut)
    }

    @Test
    fun `statuses as read give last done as the cluster in the dash that day showed it`() {
        val v = vehicle(swapped = true)
        val items = listOf(ScheduleItemEntity(id = 11, vehicleId = 1, name = "Oil change", intervalMiles = 5_000, intervalMonths = null))
        val log = v.log(items, listOf(record(1, day.minusDays(10), 148_000, line("Oil", scheduleItemId = 11))), emptyList(), Settings())
        assertEquals(LastDone(day.minusDays(10), 18_000), log.statuses(day).single().lastDone)
        assertEquals(LastDone(day.minusDays(10), 148_000), log.statusesAsRead(day).single().lastDone)
    }

    @Test
    fun `the source reads a vehicle's log afresh when a record is added, and none once it is gone`() = runBlocking {
        resetDataStores()
        val db = inMemoryDatabase(context)
        try {
            val logs = VehicleLogs(db.vehicles(), db.schedule(), db.service(), db.odometer(), SettingsRepository(context))
            val car = db.vehicles().insert(testVehicle())
            db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = car, name = "Oil change", intervalMiles = 5_000, intervalMonths = null)))
            assertEquals(null, withTimeout(5_000) { logs.observe(car).filterNotNull().first() }.odometer)

            db.service().insertWithItems(ServiceRecordEntity(vehicleId = car, date = day, odometer = 41_000, diy = true), listOf(line("Oil")))
            assertEquals(41_000, withTimeout(5_000) { logs.observe(car).filterNotNull().first { it.odometer != null } }.odometer)
            assertEquals(41_000, logs.load(car)!!.odometer)
            assertEquals(listOf(car), logs.observeGarage().first().map { it.vehicle.id })

            db.vehicles().delete(db.vehicles().byId(car)!!)
            assertNull(logs.load(car))
            assertNull(logs.observe(car).first())
        } finally {
            db.close()
        }
    }

    @After fun forget() = resetDataStores()
}
