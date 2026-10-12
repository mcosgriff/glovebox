package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleImportTest {
    private fun file(name: String, miles: Int? = 5_000, months: Int? = null, severe: Interval? = null, rule: DueRule? = null) =
        ImportedScheduleItem(name, Interval(miles, months), severe, rule)

    private fun own(id: Long, name: String, miles: Int? = 5_000, months: Int? = null, severe: Interval? = null, rule: DueRule? = null, tracked: Boolean = true) =
        SavedScheduleItem(id, name, Interval(miles, months), severe, rule, templateKey = null, tracked = tracked)

    private fun typical(id: Long, key: String, name: String, tracked: Boolean = true) =
        SavedScheduleItem(id, name, Interval(7_500, 12), null, null, templateKey = key, tracked = tracked)

    @Test
    fun `items are matched to the user's own by name, ignoring case`() {
        val plan = ScheduleImport.plan(
            items = listOf(file("Grease driveshaft", 15_000), file("Rotate tires", 5_000), file("Wash underbody", null, 6)),
            existing = listOf(own(1, "grease DRIVESHAFT", 15_000), own(2, "Rotate tires", 7_500)),
        )
        assertEquals(listOf("Wash underbody"), plan.new.map { it.name })
        assertEquals(listOf(1L), plan.same.map { it.second.id })
        assertEquals(listOf(2L), plan.differs.map { it.second.id })
        assertTrue(plan.fills.isEmpty())
    }

    @Test
    fun `a typical item is never matched, so an import can't change one`() {
        val existing = listOf(typical(1, "oil", "Engine oil & filter"))
        val plan = ScheduleImport.plan(listOf(file("Engine oil & filter", 5_000, 6)), existing)
        assertEquals(1, plan.new.size)
        assertTrue(plan.replaced(ImportMode.REPLACE_MATCHING).isEmpty())
    }

    @Test
    fun `a tracked typical item with the same name is pointed out as a double`() {
        val existing = listOf(
            typical(1, "oil", "Engine oil & filter"),
            typical(2, "rotation", "Tire rotation", tracked = false), // not reminding, so not a double
            own(3, "Brake fluid"),
        )
        val items = listOf(file("engine oil & FILTER"), file("Tire rotation"), file("Brake fluid"))
        assertEquals(listOf(1L), ScheduleImport.doubles(items, existing).map { it.id })
    }

    @Test
    fun `severe interval and rule count as differences`() {
        fun differs(item: ImportedScheduleItem, saved: SavedScheduleItem) = ScheduleImport.plan(listOf(item), listOf(saved)).differs.size == 1
        assertTrue(differs(file("Oil", severe = Interval(3_000, null)), own(1, "Oil")))
        assertTrue(differs(file("Oil", rule = DueRule.MILEAGE_ONLY), own(1, "Oil")))
        assertTrue(differs(file("Oil", 5_000, 6), own(1, "Oil", 5_000)))
        assertEquals(1, ScheduleImport.plan(listOf(file("Oil", rule = DueRule.TIME_ONLY)), listOf(own(1, "Oil", rule = DueRule.TIME_ONLY))).same.size)
    }

    @Test
    fun `add new only leaves a differing item alone and replace takes the file's`() {
        val plan = ScheduleImport.plan(listOf(file("Rotate tires", 5_000), file("Wash underbody")), listOf(own(2, "Rotate tires", 7_500)))
        assertEquals(1, plan.changes(ImportMode.ADD_NEW))
        assertEquals(2, plan.changes(ImportMode.REPLACE_MATCHING))
    }
}
