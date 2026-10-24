package io.cosgriff.glovebox.ui.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DocumentProblem
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.DoneBy
import io.cosgriff.glovebox.domain.DueHeadline
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.NextDue
import io.cosgriff.glovebox.domain.OdometerNote
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.RecordMark
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.Who
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The shared formatters and mappers: every distance in the vehicle's own unit, every reading through its cluster swap. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class MappersTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)
    private val mi = DistanceUnit.MILES
    private val km = DistanceUnit.KILOMETERS

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = context.resources.getQuantityString(id, n, *args)

    /** A cluster put in on [day]: the old one read 150,000, the new one 20,000. */
    private val swap = ClusterSwap(day, offset = 130_000, startReading = 20_000)

    private fun vehicle(unit: String = "MILES", swapped: Boolean = false) = VehicleEntity(
        id = 1, vin = null, year = 2019, make = "Toyota", model = "Tacoma", createdOn = day, distanceUnit = unit,
        clusterSwappedOn = day.takeIf { swapped }, odometerOffset = if (swapped) 130_000 else 0, clusterStartReading = 20_000.takeIf { swapped },
    )

    private fun record(id: Long, date: LocalDate, odometer: Int, vararg lines: io.cosgriff.glovebox.data.db.ServiceRecordItemEntity) =
        ServiceRecordWithItems(ServiceRecordEntity(id = id, vehicleId = 1, date = date, odometer = odometer, diy = true), lines.toList(), emptyList())

    @Test
    fun `a distance always says its unit, and figures are grouped`() {
        assertEquals(string(R.string.distance_mi, "12,345"), 12_345.asDistance(mi))
        assertEquals(string(R.string.distance_km, "12,345"), 12_345.asDistance(km))
        assertEquals("12,345", 12_345.asNumber())
        assertEquals(string(R.string.unit_km), km.symbolWord)
        assertEquals(string(R.string.unit_mi), mi.symbolWord)
        assertEquals(string(R.string.unit_kilometres_title), km.titleWord)
        assertEquals(string(R.string.unit_miles_title), mi.titleWord)
        assertEquals(string(R.string.unit_kilometres), km.pluralWord)
        assertEquals(string(R.string.unit_miles), mi.pluralWord)
        assertEquals(km, vehicle(unit = "KILOMETERS").unit)
    }

    @Test
    fun `an interval reads in the vehicle's unit, with either part or both`() {
        val months = string(R.string.months_short, "12")
        val both = Interval(7_500, 12)
        assertEquals(string(R.string.interval_slash, 7_500.asDistance(km), months), both.text(km))
        assertEquals(string(R.string.either_or, 7_500.asDistance(mi), months), both.orText(mi))
        assertEquals(string(R.string.interval_every, both.orText(mi)), both.every(mi))
        assertEquals(30_000.asDistance(mi), Interval(30_000, null).text(mi))
        assertEquals(string(R.string.months_short, "36"), Interval(null, 36).text(mi))
        assertEquals(Interval(50_000, null).text(mi), Interval(50_000, null).text(mi, firstAt = null))
        assertEquals(
            string(R.string.interval_first_then_every, 100_000.asDistance(mi), 50_000.asDistance(mi)),
            Interval(50_000, null).text(mi, firstAt = 100_000),
        )
    }

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
    fun `a vehicle's log is in dash terms with the lead in its own unit`() {
        val v = vehicle(swapped = true)
        val items = listOf(
            ScheduleItemEntity(id = 11, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = null),
            ScheduleItemEntity(id = 12, vehicleId = 1, name = "Untracked", intervalMiles = 5_000, intervalMonths = null, tracked = false),
        )
        val log = v.log(items, listOf(record(1, day.plusDays(1), 21_000, line("Oil", "oil", 11))), emptyList(), Settings())
        assertEquals(swap, log.swap)
        assertEquals(listOf(11L), log.tracked.map { it.id })
        assertEquals(21_000, log.odometer)
        assertEquals(Settings().leadFor(mi), log.lead)
        assertEquals(1, log.statuses(day.plusDays(2)).size)
        assertEquals(log.tracked, log.inputs.items)
    }

    @Test
    fun `a tracked job counts from the new cluster's zero, and a severe-only job is off on normal use`() {
        val first = ScheduleItemEntity(id = 1, vehicleId = 1, name = "Timing belt", intervalMiles = 50_000, intervalMonths = null, firstMiles = 100_000)
        val tracked = first.toTracked(DueRule.WHICHEVER_FIRST, swap)!!
        assertEquals(-130_000, tracked.zeroAt)
        assertEquals(-30_000, tracked.firstAt)
        assertEquals(100_000, first.toTracked(DueRule.WHICHEVER_FIRST, null)!!.firstAt)

        val severeOnly = ScheduleItemEntity(id = 2, vehicleId = 1, name = "Dusty filter", intervalMiles = null, intervalMonths = null, severeMiles = 5_000, severeMonths = null, severeOnly = true)
        assertNull(severeOnly.toTracked(DueRule.WHICHEVER_FIRST, null))
        assertEquals(Interval(5_000, null), severeOnly.copy(useSevere = true).toTracked(DueRule.WHICHEVER_FIRST, null)!!.interval)

        val snoozed = ScheduleItemEntity(id = 3, vehicleId = 1, name = "Wipers", intervalMiles = null, intervalMonths = 12, ruleOverride = DueRule.TIME_ONLY.name, snoozedUntilDate = day)
        val s = snoozed.toTracked(DueRule.WHICHEVER_FIRST, null)!!
        assertEquals(DueRule.TIME_ONLY, s.rule)
        assertEquals(Snooze(untilDate = day), s.snooze)
        assertNull(ScheduleItemEntity(id = 4, vehicleId = 1, name = "Nothing", intervalMiles = null, intervalMonths = null).toTracked(DueRule.WHICHEVER_FIRST, null))
    }

    @Test
    fun `due headlines and amounts say how far, in the vehicle's unit or in days and months`() {
        assertEquals(string(R.string.due_not_logged), DueHeadline.NotLogged.text(mi))
        assertEquals(string(R.string.due_in, 1_688.asDistance(km)), DueHeadline.Miles(1_688).text(km))
        assertEquals(string(R.string.due_overdue_by, 12.asDistance(mi)), DueHeadline.Miles(-12).text(mi))
        assertEquals(string(R.string.due_now), DueHeadline.Miles(0).text(mi))
        assertEquals(string(R.string.due_in, plural(R.plurals.days, 21, "21")), DueHeadline.Days(21).text(mi))
        assertEquals(string(R.string.due_overdue_by, plural(R.plurals.months, 5, "5")), DueHeadline.Days(-151).text(mi))
        assertEquals(string(R.string.due_today), DueHeadline.Days(0).text(mi))
        val snooze = Snooze(untilOdometer = 70_100)
        assertEquals(string(R.string.due_snoozed_until, 70_100.asDistance(mi)), DueHeadline.Snoozed(snooze).text(mi))

        assertEquals(1_688.asDistance(km), DueHeadline.Miles(-1_688).amount(km))
        assertNull(DueHeadline.Miles(0).amount(mi))
        assertEquals(plural(R.plurals.days, 3, "3"), DueHeadline.Days(3).amount(mi))
        assertNull(DueHeadline.Days(0).amount(mi))
        assertNull(DueHeadline.NotLogged.amount(mi))
        assertEquals(true, DueHeadline.Miles(5).ahead)
        assertEquals(false, DueHeadline.Days(-5).ahead)
        assertNull(DueHeadline.Days(0).ahead)
        assertNull(DueHeadline.Miles(0).ahead)
        assertNull(DueHeadline.NotLogged.ahead)
    }

    @Test
    fun `a snooze says its date without the year only in the current year`() {
        val today = LocalDate.of(2026, 10, 1)
        assertEquals(LocalDate.of(2026, 10, 24).format(shortDate), Snooze(untilDate = LocalDate.of(2026, 10, 24)).until(mi, today))
        assertEquals(LocalDate.of(2027, 4, 10).format(longDate), Snooze(untilDate = LocalDate.of(2027, 4, 10)).until(mi, today))
        assertEquals(
            string(R.string.either_or, 70_100.asDistance(km), LocalDate.of(2027, 4, 10).format(longDate)),
            Snooze(70_100, LocalDate.of(2027, 4, 10)).until(km, today),
        )
    }

    @Test
    fun `next due and a reminder's change are worded in the vehicle's unit`() {
        assertEquals(string(R.string.either_or, 73_412.asDistance(km), day.format(longDate)), NextDue(73_412, day).text(km))
        assertEquals(73_412.asDistance(mi), NextDue(73_412, null).text(mi))
        assertEquals(day.format(longDate), NextDue(null, day).text(mi))
        val item = TrackedItem(1, "Oil change", Interval(5_000, null), DueRule.MILEAGE_ONLY)
        assertEquals(
            string(R.string.reminder_change_next_due, "Oil change", 45_000.asDistance(km)),
            ReminderChange(item, before = null, after = LastDone(day, 40_000)).text(km),
        )
        assertEquals(string(R.string.reminder_change_not_logged, "Oil change"), ReminderChange(item, LastDone(day, 40_000), null).text(mi))
    }

    @Test
    fun `rules, offsets, sizes, notes and who did it are worded`() {
        DueRule.entries.forEach { assertTrue(it.ruleInSentence().isNotBlank() && it.ruleLabel().isNotBlank()) }
        assertEquals(string(R.string.rule_time_only), DueRule.TIME_ONLY.ruleLabel())
        assertEquals(string(R.string.rule_whichever_last_sentence), DueRule.WHICHEVER_LAST.ruleInSentence())
        assertEquals(string(R.string.offset_plus, 120_000.asDistance(km)), offsetLabel(120_000, km))
        assertEquals(string(R.string.offset_minus, 2_500.asDistance(mi)), offsetLabel(-2_500, mi))
        assertEquals(string(R.string.size_kb, "1"), fileSize(10))
        assertEquals(string(R.string.size_kb, "340"), fileSize(340L * 1024))
        assertEquals(string(R.string.size_mb, "4.2"), fileSize((4.2 * 1024 * 1024).toLong()))
        DocumentType.entries.forEach { assertTrue(it.label.isNotBlank()) }
        DocumentProblem.entries.forEach { assertTrue(it.text.isNotBlank()) }

        assertEquals(string(R.string.odometer_note_missing, km.pluralWord), OdometerNote.Missing.text(km))
        assertEquals(string(R.string.odometer_note_lower, "61,000"), OdometerNote.LowerThanEarlier(61_000).text(mi))
        assertEquals(string(R.string.odometer_note_was, "61,000"), OdometerNote.Was(61_000).text(mi))
        assertEquals(string(R.string.odometer_note_unchanged), OdometerNote.Unchanged.text(mi))
        assertEquals(string(R.string.odometer_note_last_recorded, 61_000.asDistance(km)), OdometerNote.LastRecorded(61_000).text(km))
        assertEquals(string(R.string.odometer_note_higher, 61_000.asDistance(km)), OdometerNote.HigherThanLatest(61_000).text(km))
        assertEquals(string(R.string.odometer_note_latest, 61_000.asDistance(km)), OdometerNote.Latest(61_000).text(km))

        assertEquals(string(R.string.who_diy), Who(DoneBy.Diy, imported = false, mark = null).text)
        assertEquals(string(R.string.who_shop), Who(DoneBy.Shop(null), imported = false, mark = null).text)
        assertEquals(string(R.string.who_imported, "Made-up Motors"), Who(DoneBy.Shop("Made-up Motors"), imported = true, mark = null).text)
        assertEquals(string(R.string.who_just_added, string(R.string.last_done_tag)), Who(DoneBy.EnteredAsLastDone, false, RecordMark.ADDED).text)
        assertEquals(string(R.string.who_restored, string(R.string.who_diy)), Who(DoneBy.Diy, false, RecordMark.RESTORED).text)
    }

    @Test
    fun `small phrases and figures as typed`() {
        assertNull(photoCount(0))
        assertEquals(plural(R.plurals.photos, 2, "2"), photoCount(2))
        assertEquals("", andList(emptyList()))
        assertEquals("A", andList(listOf("A")))
        assertEquals(string(R.string.list_and, string(R.string.list_and, "A", "B"), "C"), andList(listOf("A", "B", "C")))
        assertEquals(string(R.string.recall), recallComponent(""))
        assertEquals("Fuel pump", recallComponent("FUEL SYSTEM: FUEL PUMP"))
        assertEquals(string(R.string.recall_service_line, "20V-682", "Fuel pump"), recallServiceLine(RecallNotice("20V682000", "FUEL PUMP", "", null, null, null)))
        assertEquals("12345", "12,345".typedNumber())
        assertEquals("45.99", "45.99".typedNumber())
        assertEquals("6.2", "6.2".typedDecimal())
        assertEquals("6.2", 6.2.editableDecimal())
        assertEquals("5", 5.0.editableDecimal())
        assertEquals("45.09", editableCost(4_509))
    }

    @Test
    fun `saved rows are read back as the import and export see them`() {
        val torque = TorqueSpecEntity(id = 3, vehicleId = 1, name = "Made-up bolt", category = "WHEELS", newtonMeters = 100.0, source = "USER")
        assertEquals("Made-up bolt", torque.toSaved().name)
        assertEquals(100.0, torque.toSaved().torque!!.newtonMeters, 0.0)
        assertNull(torque.copy(newtonMeters = null).toSaved().torque)

        val item = ScheduleItemEntity(id = 4, vehicleId = 1, name = "Oil change", intervalMiles = 5_000, intervalMonths = 6, severeMiles = null, severeMonths = 3, firstMiles = 10_000)
        val saved = item.toSaved()
        assertEquals(Interval(5_000, 6), saved.normal)
        assertEquals(Interval(null, 3), saved.severe)
        assertEquals(10_000, saved.firstAt)
        assertNull(item.copy(intervalMiles = null, intervalMonths = null).toSaved().normal)

        val v = vehicle().copy(nickname = "  ", engine = "2.7L", oilSpec = "0W-20", oilCapacity = 6.6, oilCapacityUnit = "QUARTS", tirePressureFront = 35, tirePressureUnit = "PSI")
        assertEquals("Tacoma", v.nicknameOrModel())
        assertEquals("Blue truck", v.copy(nickname = "Blue truck").nicknameOrModel())
        assertEquals(string(R.string.vehicle_short_name, "2019", "Tacoma"), v.shortName())
        assertEquals("${v.shortName()} · 2.7L", v.subtitle())
        assertEquals("2019 Toyota Tacoma", v.fileStem())
        assertEquals("0W-20", v.whatItTakes().oilGrade)
        assertEquals(6.6, v.whatItTakes().oilCapacity!!, 0.0)
    }
}
