package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.Interval
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScheduleDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val schedule = db.schedule()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun item(vehicleId: Long, name: String, templateKey: String? = null, tracked: Boolean = true) =
        ScheduleItemEntity(vehicleId = vehicleId, name = name, templateKey = templateKey, intervalMiles = 5000, intervalMonths = 6, tracked = tracked)

    private suspend fun byName(vehicleId: Long) = schedule.allFor(vehicleId).associateBy { it.name }

    @Test
    fun `tracked items are listed by name, per vehicle and for the whole garage`() = runBlocking {
        val a = db.vehicles().add(testVehicle(), firstReading = null)
        val b = db.vehicles().add(testVehicle(model = "GTI"), firstReading = null)
        schedule.insertAll(listOf(item(a, "Tires"), item(a, "Brakes", tracked = false), item(a, "Air filter"), item(b, "Coolant")))

        assertEquals(listOf("Air filter", "Tires"), schedule.observeTracked(a).first().map { it.name })
        assertEquals(listOf("Air filter", "Brakes", "Tires"), schedule.observeAll(a).first().map { it.name })
        assertEquals(listOf("Air filter", "Brakes", "Tires"), schedule.allFor(a).map { it.name })
        assertEquals(listOf("Air filter", "Coolant", "Tires"), schedule.observeAllTracked().first().map { it.name })
    }

    @Test
    fun `an item's own rule and snooze are set and cleared`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        schedule.insertAll(listOf(item(v, "Oil")))
        val id = schedule.allFor(v).single().id

        schedule.setRuleOverride(id, DueRule.MILEAGE_ONLY.name)
        schedule.setSnooze(id, 61_000, day)
        var oil = schedule.allFor(v).single()
        assertEquals(DueRule.MILEAGE_ONLY, oil.rule)
        assertEquals(61_000 to day, oil.snoozedUntilOdometer to oil.snoozedUntilDate)

        schedule.setRuleOverride(id, null)
        schedule.setSnooze(id, null, null)
        oil = schedule.allFor(v).single()
        assertNull(oil.rule)
        assertNull(oil.snoozedUntilOdometer)

        schedule.setRuleOverride(id, "A_RULE_FROM_LATER")
        assertNull(schedule.allFor(v).single().rule)
    }

    @Test
    fun `a maker's schedule removes the typical items it replaces, unless a service was logged against one`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        schedule.insertAll(listOf(item(v, "Oil (typical)", templateKey = "oil"), item(v, "Tires (typical)", templateKey = "tires"), item(v, "Own job")))
        val before = byName(v)
        db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = v, date = day, odometer = 60_000, diy = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = before.getValue("Tires (typical)").id, label = "Tires")),
        )

        schedule.applyKnown(
            added = listOf(ScheduleItemEntity(vehicleId = v, name = "Oil (maker)", intervalMiles = 10_000, intervalMonths = 12, knownList = "made-up")),
            replaced = listOf(before.getValue("Oil (typical)").id, before.getValue("Tires (typical)").id, before.getValue("Own job").id),
        )

        val after = byName(v)
        assertEquals(setOf("Oil (maker)", "Tires (typical)", "Own job"), after.keys)
        assertFalse(after.getValue("Tires (typical)").tracked)
        // Only typical items are ever removed; one of the user's own is only stopped.
        assertFalse(after.getValue("Own job").tracked)
        assertEquals("made-up", after.getValue("Oil (maker)").knownList)
    }

    @Test
    fun `an import adds, replaces and stops tracking, and deletes nothing`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        schedule.insertAll(listOf(item(v, "Oil"), item(v, "Tires (typical)", templateKey = "tires")))
        val before = byName(v)

        schedule.applyImport(
            added = listOf(item(v, "Coolant")),
            replaced = listOf(before.getValue("Oil").copy(intervalMiles = 7500)),
            stopped = listOf(before.getValue("Tires (typical)").id),
        )

        val after = byName(v)
        assertEquals(setOf("Oil", "Tires (typical)", "Coolant"), after.keys)
        assertEquals(7500, after.getValue("Oil").intervalMiles)
        assertFalse(after.getValue("Tires (typical)").tracked)

        schedule.update(after.getValue("Coolant").copy(intervalMonths = 60))
        assertEquals(60, byName(v).getValue("Coolant").intervalMonths)
    }

    @Test
    fun `an interval from a file or a maker's list becomes one of the user's own items`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val imported = ImportedScheduleItem(
            name = "Made-up job", normal = Interval(30_000, 36), severe = Interval(15_000, 18), e85 = Interval(20_000, 24),
            rule = DueRule.WHICHEVER_LAST, service = "coolant", firstAt = 100_000, severeOnly = false,
        )

        schedule.insertAll(listOf(imported.toEntity(v, knownList = "made-up", useSevere = true, useE85 = true), imported.copy(e85 = null).toEntity(v, useE85 = true)))

        val all = schedule.allFor(v)
        val a = all.single { it.knownList == "made-up" }
        val b = all.single { it.knownList == null }
        assertEquals(listOf(30_000, 36, 15_000, 18, 20_000, 24), listOf(a.intervalMiles, a.intervalMonths, a.severeMiles, a.severeMonths, a.e85Miles, a.e85Months))
        assertTrue(a.useSevere && a.useE85 && a.tracked)
        assertEquals(listOf("WHICHEVER_LAST", "coolant", "made-up", 100_000), listOf(a.ruleOverride, a.service, a.knownList, a.firstMiles))
        assertFalse(b.useE85)
        assertNull(b.templateKey)
    }
}
