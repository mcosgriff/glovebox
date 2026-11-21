package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.ImportClose
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobPartEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.ImportFile
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportedJob
import io.cosgriff.glovebox.domain.ImportedPartRow
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.InvoiceCharges
import io.cosgriff.glovebox.domain.ImportedWork
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.RecommendedItem
import io.cosgriff.glovebox.domain.RecordJob
import io.cosgriff.glovebox.domain.RecordPartRow
import io.cosgriff.glovebox.domain.ShopRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A service-history import planned against a log built by hand (#618): the doubts, the two modes, what a replaced
 * record keeps, the recommendations offered and closed, and the one write. Made-up records, shops and readings.
 */
class HistoryImportPlannerTest {
    private val oilItem = ScheduleItemEntity(id = 11, vehicleId = 1, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = null)

    private fun vehicle(archived: Boolean = false, swappedOn: LocalDate? = null, offset: Int = 0, startReading: Int? = null) = VehicleEntity(
        id = 1, vin = null, year = 2019, make = "Toyota", model = "Tacoma", createdOn = LocalDate.of(2020, 1, 1),
        archivedOn = LocalDate.of(2026, 1, 1).takeIf { archived },
        clusterSwappedOn = swappedOn, odometerOffset = offset, clusterStartReading = startReading,
    )

    private fun log(
        vararg records: ServiceRecordWithItems,
        readings: List<OdometerReadingEntity> = emptyList(),
        vehicle: VehicleEntity = vehicle(),
    ) = vehicle.log(listOf(oilItem), records.toList(), readings, Settings())

    private fun reading(date: LocalDate, odometer: Int) = OdometerReadingEntity(vehicleId = 1, date = date, odometer = odometer)

    private fun saved(
        id: Long,
        date: LocalDate,
        odometer: Int,
        vararg lines: ServiceRecordItemEntity,
        shop: String? = null,
        lastDone: Boolean = false,
        jobs: List<ServiceRecordJobEntity> = emptyList(),
        parts: List<ServiceRecordJobPartEntity> = emptyList(),
        taxCents: Long? = null,
    ) = ServiceRecordWithItems(
        ServiceRecordEntity(id = id, vehicleId = 1, date = date, odometer = odometer, diy = shop == null, shopName = shop, enteredAsLastDone = lastDone, taxCents = taxCents),
        lines.map { it.copy(recordId = id) }, emptyList(), jobs, parts,
    )

    private fun line(label: String, catalogId: String? = null, scheduleItemId: Long? = null, id: Long = 0, jobId: Long? = null) =
        ServiceRecordItemEntity(id = id, recordId = 0, scheduleItemId = scheduleItemId, label = label, catalogId = catalogId, jobId = jobId)

    private fun imported(date: LocalDate, odometer: Int, vararg work: ImportedWork) = ImportedRecord(date, odometer, work.toList(), diy = true)

    private val oil = ImportedWork("Oil & filter change", "oil")
    private val rotation = ImportedWork("Tire rotation", "rotation")

    private fun recommendation(id: Long, line: String, recordId: Long = 1, doneBy: Long? = null) = ShopRecommendation(
        id = id, recordId = recordId, date = LocalDate.of(2024, 1, 10), shop = "Made-up Motors", item = RecommendedItem("Due: $line", null),
        line = line, doneBy = doneBy,
    )

    private fun plan(
        log: VehicleLog,
        vararg records: ImportedRecord,
        unit: DistanceUnit = DistanceUnit.MILES,
        rules: List<CatalogRule> = emptyList(),
        recommendations: List<ShopRecommendation> = emptyList(),
        overWording: Boolean = false,
        choices: HistoryChoices = HistoryChoices(),
    ) = HistoryImportPlanner.plan(ImportFile.ServiceHistory(records.toList(), unit), log, rules, recommendations, overWording, choices)

    @Test
    fun `a reading that looks mistyped is a doubt, left out unless the owner keeps it`() {
        val log = log(readings = listOf(reading(LocalDate.of(2023, 1, 1), 30_000), reading(LocalDate.of(2025, 6, 1), 52_000)))
        val good = imported(LocalDate.of(2024, 3, 1), 41_000, oil)
        // A slipped digit: nowhere near the readings either side of it.
        val typo = imported(LocalDate.of(2024, 9, 1), 450_000, rotation)

        val first = plan(log, good, typo)
        val doubt = first.doubts.single()
        assertEquals(LocalDate.of(2024, 9, 1) to 450_000, doubt.key)
        assertFalse(doubt.kept)
        assertNull("out of line, not a jump from one reading", doubt.jump)
        assertEquals(listOf(good), first.matched.new)
        assertEquals(listOf(41_000), first.write(ImportMode.ADD_NEW).added.map { it.odometer })

        val kept = plan(log, good, typo, choices = HistoryChoices(kept = setOf(doubt.key)))
        assertTrue("still listed, now kept", kept.doubts.single().kept)
        assertEquals(listOf(41_000, 450_000), kept.write(ImportMode.ADD_NEW).added.map { it.odometer })
    }

    @Test
    fun `a jump far ahead of the reading before says how far, from the reading as the dash showed it`() {
        // The cluster was replaced in June 2025; the old one read 10,000 more than the new one would have.
        val swapped = vehicle(swappedOn = LocalDate.of(2025, 6, 1), offset = 10_000, startReading = 140_000)
        val log = log(readings = listOf(reading(LocalDate.of(2025, 1, 1), 100_000)), vehicle = swapped)

        val doubt = plan(log, imported(LocalDate.of(2025, 2, 1), 130_000, oil)).doubts.single()
        assertEquals(
            "typed on the old cluster, so as it read then",
            HistoryDoubt.Jump(OdometerReading(LocalDate.of(2025, 1, 1), 100_000), by = 30_000),
            doubt.jump,
        )
    }

    @Test
    fun `add new only leaves a differing record alone, and replace matching rewrites it under its own id`() {
        val log = log(
            saved(1, LocalDate.of(2023, 4, 2), 31_800, line("Cabin filter"), shop = "Somewhere"),
            // A bare last done: filled whatever the mode, since the file's record resets what it did.
            saved(2, LocalDate.of(2022, 1, 1), 20_000, line("Oil change", "oil", 11), lastDone = true),
        )
        val records = arrayOf(
            imported(LocalDate.of(2023, 4, 2), 31_800, ImportedWork("Cabin air filter")),
            imported(LocalDate.of(2022, 1, 1), 20_000, oil),
            imported(LocalDate.of(2024, 3, 1), 41_000, oil),
        )
        val planned = plan(log, *records)
        assertEquals(1, planned.matched.differs.size)
        assertEquals(1, planned.matched.fills.size)

        val addNew = planned.write(ImportMode.ADD_NEW)
        val added = addNew.added.single()
        assertEquals(41_000, added.odometer)
        assertEquals(0L, added.id)
        assertTrue(added.imported)
        assertEquals(11L, added.lines.single().scheduleItemId)
        val filled = addNew.replaced.single()
        assertEquals(2L, filled.id)
        assertFalse("no longer a bare last done", filled.enteredAsLastDone)

        val replace = planned.write(ImportMode.REPLACE_MATCHING)
        assertEquals(addNew.added, replace.added)
        val cabin = replace.replaced.single { it.id == 1L }
        assertTrue(cabin.diy)
        assertTrue(cabin.imported)
        assertEquals(listOf("Cabin air filter"), cabin.lines.map { it.label })
        assertEquals(listOf(2L, 1L), replace.replaced.map { it.id })
    }

    @Test
    fun `a record replaced from a file that names no prices or charges keeps its jobs, part rows and charges`() {
        val log = log(
            saved(
                1, LocalDate.of(2023, 4, 2), 31_800, line("Cabin air filter", id = 5, jobId = 7), shop = "Somewhere",
                jobs = listOf(ServiceRecordJobEntity(id = 7, recordId = 1, laborCents = 1_000)),
                parts = listOf(ServiceRecordJobPartEntity(jobId = 7, wording = "Made-up filter", totalCents = 1_899, itemId = 5)),
                taxCents = 152,
            ),
        )
        val replaced = plan(log, imported(LocalDate.of(2023, 4, 2), 31_800, ImportedWork("Cabin air filter")))
            .write(ImportMode.REPLACE_MATCHING).replaced.single()

        assertTrue("the file's own say", replaced.diy)
        assertEquals(listOf(RecordJob(null, 1_000, null, listOf(RecordPartRow("Made-up filter", null, null, 1_899, 0)))), replaced.jobs)
        assertEquals(0, replaced.lines.single().job)
        assertEquals(152L, replaced.charges.taxCents)
    }

    @Test
    fun `a new record comes with its jobs, their part rows on its lines, and its charges`() {
        val priced = ImportedRecord(
            LocalDate.of(2024, 6, 14), 45_200,
            listOf(rotation, ImportedWork("Spark plugs", "spark-plugs", job = 0), ImportedWork("Ignition coils", "ignition-coils", job = 0)),
            diy = false, shopName = "Made-up Motors", costCents = 49_731,
            jobs = listOf(
                ImportedJob(
                    "Replace plugs and coils", 18_000, null,
                    listOf(ImportedPartRow("Spark plug", 4_000, 1_250, 5_000, line = 0), ImportedPartRow("Ignition coil", totalCents = 37_000, line = 1), ImportedPartRow("Grease", totalCents = 600)),
                ),
            ),
            charges = InvoiceCharges(shopSuppliesCents = 1_850, taxCents = 4_845),
        )
        val added = plan(log(), priced).write(ImportMode.ADD_NEW).added.single()

        assertEquals(listOf(null, 0, 0), added.lines.map { it.job })
        val job = added.jobs.single()
        assertEquals(18_000L, job.laborCents)
        // A row's line is its place among all the record's lines, not only its job's.
        assertEquals(listOf(1, 2, null), job.rows.map { it.line })
        assertEquals(InvoiceCharges(shopSuppliesCents = 1_850, taxCents = 4_845), added.charges)
        assertEquals(49_731L, added.costCents)
    }

    @Test
    fun `recommendations are offered for each mode, and only the ticked ones close, by added place or saved id`() {
        val log = log(
            saved(1, LocalDate.of(2024, 1, 10), 35_000, line("Vehicle health report"), shop = "Made-up Motors"),
            saved(2, LocalDate.of(2024, 2, 1), 36_000, line("Checked lights")),
        )
        val recs = listOf(recommendation(101, "oil"), recommendation(102, "rotation"), recommendation(103, "wipers"))
        val planned = plan(
            log,
            // Before the visit that kept them: never offered for one.
            imported(LocalDate.of(2023, 12, 1), 34_000, oil),
            imported(LocalDate.of(2024, 3, 1), 37_000, oil),
            // Replaces record 2, so the rotation is offered only when it does.
            imported(LocalDate.of(2024, 2, 1), 36_000, rotation),
            imported(LocalDate.of(2024, 4, 1), 38_000, ImportedWork("Wiper blades", "wipers")),
            recommendations = recs,
            choices = HistoryChoices(unticked = setOf(103)),
        )

        val addNew = planned.offers.getValue(ImportMode.ADD_NEW)
        assertEquals(listOf(101L, 103L), addNew.map { it.recommendation.id })
        assertEquals(LocalDate.of(2024, 3, 1), addNew[0].record.date)
        assertEquals("Oil & filter change", addNew[0].line.label)
        assertEquals(listOf(true, false), addNew.map { it.ticked })
        assertEquals(listOf(101L, 102L, 103L), planned.offers.getValue(ImportMode.REPLACE_MATCHING).map { it.recommendation.id })

        val dates = planned.write(ImportMode.ADD_NEW).added.map { it.date }
        assertEquals(LocalDate.of(2024, 3, 1), dates[1])
        assertEquals(listOf(ImportClose(101, added = 1)), planned.write(ImportMode.ADD_NEW).done)
        assertEquals(
            "unticked: stays open",
            listOf(ImportClose(101, added = 1), ImportClose(102, savedId = 2)),
            planned.write(ImportMode.REPLACE_MATCHING).done,
        )
    }

    @Test
    fun `a replaced record that no longer looks like the work for what it closed opens it again`() {
        val log = log(saved(2, LocalDate.of(2024, 3, 1), 37_000, line("Oil & filter change", "oil", 11)))
        val closed = recommendation(101, "oil", doneBy = 2)
        val planned = plan(log, imported(LocalDate.of(2024, 3, 1), 37_000, rotation), recommendations = listOf(closed))

        assertTrue(planned.offers.values.all { it.isEmpty() })
        assertEquals(emptyList<Long>(), planned.write(ImportMode.ADD_NEW).reopened)
        assertEquals(listOf(101L), planned.write(ImportMode.REPLACE_MATCHING).reopened)
    }

    @Test
    fun `what moves is worked out for each mode, and nothing moves on a sold vehicle`() {
        val cabin = saved(1, LocalDate.of(2024, 1, 1), 30_000, line("Cabin filter"))
        val file = imported(LocalDate.of(2024, 1, 1), 30_000, oil)

        val moves = plan(log(cabin), file).moves
        assertEquals(emptyList<Any>(), moves.getValue(ImportMode.ADD_NEW))
        assertEquals("the replaced record now resets the oil", listOf(11L), moves.getValue(ImportMode.REPLACE_MATCHING).map { it.item.id })

        val sold = plan(log(cabin, vehicle = vehicle(archived = true)), file, imported(LocalDate.of(2024, 6, 1), 35_000, oil))
        assertTrue(sold.moves.values.all { it.isEmpty() })
        assertEquals("a sold vehicle still takes its history", 1, sold.write(ImportMode.ADD_NEW).added.size)
    }

    @Test
    fun `a file in kilometres is saved in the vehicle's miles, and the owner's rules file its lines`() {
        val lamp = ImportedWork("Replaced left tail lamp")
        val worded = ImportedWork("Tail lamp", "battery")
        val rules = listOf(CatalogRule("Replaced left tail lamp", "bulbs"), CatalogRule("Tail lamp", "bulbs"))
        val planned = plan(log(), imported(LocalDate.of(2024, 3, 1), 16_093, lamp, worded), unit = DistanceUnit.KILOMETERS, rules = rules)

        val added = planned.write(ImportMode.ADD_NEW).added.single()
        assertEquals(10_000, added.odometer)
        assertEquals("a rule fills only what the file leaves unsaid", listOf("bulbs", "battery"), added.lines.map { it.catalogId })
        assertEquals(1, planned.filing.lines)

        // A Carfax report's service is only Glovebox's reading of the wording: there the owner's rule wins.
        val carfax = plan(log(), imported(LocalDate.of(2024, 3, 1), 10_000, lamp, worded), rules = rules, overWording = true)
        assertEquals(listOf("bulbs", "bulbs"), carfax.write(ImportMode.ADD_NEW).added.single().lines.map { it.catalogId })
        assertEquals(2, carfax.filing.lines)
    }
}
