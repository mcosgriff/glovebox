package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorqueImportTest {
    private fun file(name: String, nm: Double? = null, category: TorqueCategory = TorqueCategory.WHEELS, socket: String? = null, note: String? = null) =
        ImportedTorqueSpec(name, category, socket, nm?.let(::Torque), TorqueSource.USER, note)

    private fun saved(id: Long, name: String, nm: Double? = null, category: TorqueCategory = TorqueCategory.WHEELS, socket: String? = null, note: String? = null) =
        SavedTorqueSpec(id, name, category, socket, nm?.let(::Torque), TorqueSource.USER, note)

    @Test
    fun `items are sorted into new, same, fills and differs`() {
        val plan = TorqueImport.plan(
            items = listOf(file("Lug nuts", 135.0), file("Caliper bolts", 107.0), file("Axle nut", 294.0), file("Drain plug", 40.0, TorqueCategory.ENGINE)),
            existing = listOf(
                saved(1, "lug NUTS", 135.0), // same, whatever the case
                saved(2, "Caliper bolts", 90.0), // differs
                saved(3, "Axle nut"), // only a name: filled in
            ),
        )
        assertEquals(listOf("Drain plug"), plan.new.map { it.name })
        assertEquals(listOf(1L), plan.same.map { it.second.id })
        assertEquals(listOf(3L), plan.fills.map { it.second.id })
        assertEquals(listOf(2L), plan.differs.map { it.second.id })
        assertEquals(4, plan.total)
    }

    @Test
    fun `add new only never writes over a row that says something`() {
        val plan = TorqueImport.plan(listOf(file("Caliper bolts", 107.0), file("Axle nut", 294.0)), listOf(saved(2, "Caliper bolts", 90.0), saved(3, "Axle nut")))
        assertEquals(listOf(3L), plan.replaced(ImportMode.ADD_NEW).map { it.second.id })
        assertEquals(1, plan.changes(ImportMode.ADD_NEW))
        assertEquals(setOf(2L, 3L), plan.replaced(ImportMode.REPLACE_MATCHING).map { it.second.id }.toSet())
        assertEquals(2, plan.changes(ImportMode.REPLACE_MATCHING))
    }

    @Test
    fun `the same name in another category is a different fastener`() {
        val plan = TorqueImport.plan(listOf(file("Drain plug", 40.0, TorqueCategory.ENGINE)), listOf(saved(1, "Drain plug", 27.0, TorqueCategory.DRIVELINE)))
        assertEquals(1, plan.new.size)
        assertTrue(plan.differs.isEmpty())
    }

    @Test
    fun `the same torque written in another unit is the same`() {
        val inFootPounds = ImportedTorqueSpec("Lug nuts", TorqueCategory.WHEELS, torque = Torque.of(100.0, TorqueUnit.FOOT_POUND))
        val plan = TorqueImport.plan(listOf(inFootPounds), listOf(saved(1, "Lug nuts", 135.58)))
        assertEquals(1, plan.same.size)
    }

    @Test
    fun `a different socket, note or source is a difference`() {
        fun differs(item: ImportedTorqueSpec, row: SavedTorqueSpec) = TorqueImport.plan(listOf(item), listOf(row)).differs.size == 1
        assertTrue(differs(file("Lug nuts", 135.0, socket = "21 mm"), saved(1, "Lug nuts", 135.0, socket = "19 mm")))
        assertTrue(differs(file("Lug nuts", 135.0, note = "Star pattern"), saved(1, "Lug nuts", 135.0)))
        assertTrue(differs(file("Lug nuts", 135.0).copy(source = TorqueSource.FACTORY), saved(1, "Lug nuts", 135.0)))
    }

    @Test
    fun `an import never removes a row the file doesn't mention`() {
        val plan = TorqueImport.plan(listOf(file("Lug nuts", 135.0)), listOf(saved(9, "Axle nut", 294.0)))
        assertEquals(listOf("Lug nuts"), plan.new.map { it.name })
        assertTrue(plan.replaced(ImportMode.REPLACE_MATCHING).isEmpty())
    }
}
