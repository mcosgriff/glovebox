package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.ImportedWork
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.SavedRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** How reminders would move if the log changed (#430), against a log built by hand. */
class WhatIfTest {
    private val day = LocalDate.of(2026, 5, 4)

    private fun vehicle(swapped: Boolean = false, archived: Boolean = false) = VehicleEntity(
        id = 1, vin = null, year = 2019, make = "Toyota", model = "Tacoma", createdOn = day.minusYears(5),
        // A cluster put in on [day]: the old one read 150,000, the new one 20,000.
        clusterSwappedOn = day.takeIf { swapped }, odometerOffset = if (swapped) 130_000 else 0, clusterStartReading = 20_000.takeIf { swapped },
        archivedOn = day.takeIf { archived },
    )

    // A typical oil item, the user's own synthetic oil answering to the same line, and a typical rotation.
    private val items = listOf(
        ScheduleItemEntity(id = 11, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = null),
        ScheduleItemEntity(id = 12, vehicleId = 1, name = "Synthetic oil", service = "oil", intervalMiles = 10_000, intervalMonths = null),
        ScheduleItemEntity(id = 13, vehicleId = 1, name = "Tire rotation", templateKey = "rotation", intervalMiles = 5_000, intervalMonths = null),
    )

    private fun line(label: String, catalogId: String? = null, scheduleItemId: Long? = null, id: Long = 0) =
        ServiceRecordItemEntity(id = id, recordId = 0, scheduleItemId = scheduleItemId, label = label, catalogId = catalogId)

    private fun record(id: Long, date: LocalDate, odometer: Int, vararg lines: ServiceRecordItemEntity) =
        ServiceRecordWithItems(ServiceRecordEntity(id = id, vehicleId = 1, date = date, odometer = odometer, diy = true), lines.toList(), emptyList())

    private val june = record(1, day.minusMonths(11), 30_000, line("Oil and filter", "oil", 11))
    private val november = record(2, day.minusMonths(6), 35_000, line("Oil and filter", "oil", 11))

    private fun log(vararg records: ServiceRecordWithItems, swapped: Boolean = false, archived: Boolean = false) =
        vehicle(swapped, archived).log(items, records.toList(), emptyList(), Settings())

    @Test
    fun `deleting the latest service moves its reminders back to the one before`() {
        val log = log(june, november)
        val changes = log.whatIf(listOf(june.toDraft())).changes

        assertEquals(listOf(11L, 12L), changes.map { it.item.id })
        assertEquals(LastDone(november.record.date, 35_000), changes.first().before)
        assertEquals(LastDone(june.record.date, 30_000), changes.first().after)
    }

    @Test
    fun `an edit to a record's reading moves what it resets`() {
        val log = log(june, november)
        val edited = november.toDraft().copy(odometer = 35_500)

        val changes = log.whatIf(listOf(june.toDraft(), edited)).changes
        assertEquals(listOf(11L, 12L), changes.map { it.item.id })
        assertEquals(LastDone(november.record.date, 35_500), changes.first().after)
    }

    @Test
    fun `a line filed under a catalog line counts for every item that answers to it`() {
        val custom = record(3, day.minusMonths(1), 39_000, line("Changed oil", id = 7))
        val log = log(june, custom)
        val filed = custom.toDraft().copy(lines = listOf(null to "oil"))

        val changes = log.whatIf(listOf(june.toDraft(), filed)).changes
        assertEquals(listOf(11L, 12L), changes.map { it.item.id })
        assertTrue(changes.all { it.after == LastDone(custom.record.date, 39_000) })
    }

    @Test
    fun `a reading typed on the old cluster is put in dash terms, before and after`() {
        val old = record(1, day.minusMonths(6), 140_000, line("Oil and filter", "oil", 11))
        val log = log(old, swapped = true)
        val after = log.whatIf(listOf(old.toDraft(), DraftRecord(-1, day.plusMonths(1), 24_000, listOf(11L to "oil"))))

        assertEquals(listOf(10_000, 24_000), after.entries.map { it.odometer })
        assertEquals(LastDone(old.record.date, 10_000), after.changes.first().before)
        assertEquals(LastDone(day.plusMonths(1), 24_000), after.changes.first().after)
    }

    @Test
    fun `a sold vehicle has no reminders to move`() {
        val log = log(june, november, archived = true)
        val after = log.whatIf(listOf(june.toDraft()))

        assertEquals(emptyList<Any>(), after.changes)
        assertEquals(listOf(1L), after.entries.map { it.id })
        assertEquals(listOf(11L, 12L, 13L), log.tracked.map { it.id })
    }

    private fun file(date: LocalDate = day, odometer: Int = 41_000, vararg work: ImportedWork = arrayOf(ImportedWork("Oil & filter change", "oil"))) =
        ImportedRecord(date, odometer, work.toList(), diy = true)

    private fun ServiceRecordWithItems.saved() = SavedRecord(
        record.id, record.date, record.odometer, items.map { it.label }, items.mapNotNull { it.scheduleItemId }.toSet(),
        diy = record.diy, shopName = null, costCents = null, notes = null, enteredAsLastDone = record.enteredAsLastDone,
    )

    private fun VehicleLog.plan(vararg file: ImportedRecord) = HistoryImport.plan(file.toList(), records.map { it.saved() }, items.map { it.toRef() })

    @Test
    fun `imported history moves reminders the way logged work does`() {
        val log = log()
        val change = log.whatIf(log.afterImport(log.plan(file()), ImportMode.ADD_NEW)).changes.first()

        assertNull(change.before)
        assertEquals(LastDone(day, 41_000), change.after)
    }

    @Test
    fun `an older imported record doesn't move a reminder a newer service already set`() {
        val log = log(november)
        assertTrue(log.whatIf(log.afterImport(log.plan(file(day.minusYears(1), 29_000)), ImportMode.ADD_NEW)).changes.isEmpty())
    }

    @Test
    fun `replacing a record changes which reminders it resets`() {
        val log = log(november)
        val both = log.plan(file(november.record.date, 35_000, ImportedWork("Oil and filter", "oil"), ImportedWork("Tire rotation", "rotation")))

        assertTrue(log.whatIf(log.afterImport(both, ImportMode.ADD_NEW)).changes.isEmpty())
        val moved = log.whatIf(log.afterImport(both, ImportMode.REPLACE_MATCHING)).changes
        assertEquals(listOf(13L), moved.map { it.item.id })
        assertEquals(LastDone(november.record.date, 35_000), moved.single().after)
    }

    @Test
    fun `a file's new records are numbered below zero in the file's order, so a doubt can be traced back`() {
        val log = log(june)
        val plan = log.plan(file(day.minusMonths(2), 38_000), file(day.minusMonths(1), 39_000))
        val after = log.whatIf(log.afterImport(plan, ImportMode.ADD_NEW)).entries

        assertEquals(listOf(1L, -1L, -2L), after.map { it.id })
        assertEquals(plan.new.map { it.odometer }, after.filter { it.id < 0 }.map { plan.new[newIndexOf(it.id)!!].odometer })
        assertNull(newIndexOf(1))
    }

    @Test
    fun `readings from before a cluster replacement are checked on the same footing as the file's`() {
        // Typed from the old cluster; the file's record after the swap reads far lower on the new one.
        val log = log(record(1, day.minusMonths(6), 140_000, line("Oil and filter", "oil", 11)), swapped = true)
        val after = log.whatIf(log.afterImport(log.plan(file(day.plusMonths(1), 21_000)), ImportMode.ADD_NEW)).entries

        assertTrue(HistoryImport.doubtful(after, log.updates, DistanceUnit.MILES).isEmpty())
    }
}
