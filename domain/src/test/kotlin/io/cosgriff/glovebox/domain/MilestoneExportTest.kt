package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MilestoneExportTest {
    private val oil = TableJob("Change engine oil and filter", "oil")
    private val belts = TableJob("Inspect \"drive\" belts \\ hoses")
    private val table = listOf(
        SavedMilestone(7, 30_000, listOf(belts, oil), 36),
        SavedMilestone(3, 10_000, listOf(oil), 12),
        SavedMilestone(9, 65_000, listOf(TableJob("Replace spark plugs", "spark-plugs"))),
    )

    private fun back(text: String) = ImportFormat.parse(text) as ImportFile.MilestoneSchedule

    @Test
    fun `what is written out is read back as the same table`() {
        val file = back(MilestoneExport.file(table, 120_000))
        assertEquals(120_000, file.repeatsEveryMiles)
        // By mileage, with the jobs in the user's order.
        assertEquals(
            listOf(
                ImportedMilestone(10_000, listOf(oil), 12),
                ImportedMilestone(30_000, listOf(belts, oil), 36),
                ImportedMilestone(65_000, listOf(TableJob("Replace spark plugs", "spark-plugs"))),
            ),
            file.items,
        )
        // Imported over the table it came from, nothing is new and nothing differs.
        val plan = MilestoneImport.plan(file.items, table)
        assertEquals(3, plan.same.size)
        assertEquals(3, plan.total)
    }

    @Test
    fun `a table that ends says nothing about repeating`() {
        val text = MilestoneExport.file(table, null)
        assertFalse(MilestoneImport.REPEATS in text)
        assertEquals(null, back(text).repeatsEveryMiles)
    }

    @Test
    fun `a table at the format's limits still goes out and back`() {
        val full = (1..MilestoneImport.MAX_ITEMS).map { i ->
            SavedMilestone(i.toLong(), i * 5_000, (1..MilestoneImport.MAX_WORK).map { TableJob("x".repeat(MilestoneImport.MAX_NAME - 2) + "%02d".format(it)) }, MilestoneImport.MAX_MONTHS)
        }
        val text = MilestoneExport.file(full, MilestoneImport.MAX_MILES)
        assertTrue(text.toByteArray().size < ImportFormat.MAX_BYTES)
        assertEquals(full.map { ImportedMilestone(it.atMiles, it.work, it.orMonths) }, back(text).items)
    }

    @Test
    fun `whatever the editor accepts can be written out and read back`() {
        val typed = (MilestoneEdits.check("1,000,000", "600", listOf(TableJob("  Épaisseur des plaquettes — 3 mm  "), oil), emptyList(), null) as MilestoneEdit.Ok).milestone
        val text = MilestoneExport.file(listOf(SavedMilestone(1, typed.atMiles, typed.work, typed.orMonths)), 1_000_000)
        assertEquals(listOf(typed), back(text).items)
    }

    @Test
    fun `the file passes the published schema and names no vehicle`() {
        val text = MilestoneExport.file(table, 120_000)
        val schema = File("../docs/import/schema/milestone-schedule.schema.json").readText()
        val registry = com.networknt.schema.SchemaRegistry.withDefaultDialect(com.networknt.schema.SpecificationVersion.DRAFT_2020_12)
        assertTrue(registry.getSchema(schema).validate(text, com.networknt.schema.InputFormat.JSON).isEmpty())
        assertTrue(MilestoneExport.SCHEMA in schema)
        assertTrue(text.endsWith("}\n"))
    }

    @Test
    fun `the suggested name says which model the table is for`() {
        assertEquals("glovebox-milestones-2003-honda-accord.json", MilestoneExport.fileName("2003 Honda Accord"))
        assertEquals("glovebox-milestones-vehicle.json", MilestoneExport.fileName("  "))
    }
}
