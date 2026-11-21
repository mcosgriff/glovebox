package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** An import planned at preview time and checked again inside its write (#645), on made-up torque rows. */
class ImportPlansTest {
    private val engine = TorqueCategory.ENGINE

    private fun item(name: String, nm: Double? = null) = ImportedTorqueSpec(name, engine, torque = nm?.let(::Torque))

    private fun saved(id: Long, name: String, nm: Double? = null) = SavedTorqueSpec(id, name, engine, null, nm?.let(::Torque), TorqueSource.USER, null)

    private val file = listOf(item("Blank", 10.0), item("Differs", 20.0), item("Same", 30.0), item("New", 40.0))
    private val before = listOf(saved(1, "Blank"), saved(2, "Differs", 25.0), saved(3, "Same", 30.0))
    private val preview = TorqueImport.plan(file, before)

    private fun ImportWrite<ImportedTorqueSpec, SavedTorqueSpec>.names() = added.map { it.name } to replaced.map { it.second.id }

    @Test
    fun `with nothing changed since the preview, the write is what the preview listed`() {
        assertEquals(file.map { it.name }.toSet(), preview.items.map { it.name }.toSet())
        val now = TorqueImport.plan(preview.items, before)
        assertEquals(listOf("New") to listOf(1L), ImportPlans.recheck(preview, now, ImportMode.ADD_NEW) { it.id }.names())
        assertEquals(listOf("New") to listOf(1L, 2L), ImportPlans.recheck(preview, now, ImportMode.REPLACE_MATCHING) { it.id }.names())
    }

    @Test
    fun `a row changed since the preview is written over only where the preview said it would be`() {
        // The blank row was given a figure, and the one that was the same was changed.
        val now = TorqueImport.plan(preview.items, listOf(saved(1, "Blank", 12.0), saved(2, "Differs", 25.0), saved(3, "Same", 35.0)))
        assertEquals("Add new only no longer fills the row the owner typed into", listOf("New") to emptyList<Long>(), ImportPlans.recheck(preview, now, ImportMode.ADD_NEW) { it.id }.names())
        assertEquals(
            "Replace matching writes over the rows it listed, never the one it called the same",
            listOf("New") to listOf(1L, 2L), ImportPlans.recheck(preview, now, ImportMode.REPLACE_MATCHING) { it.id }.names(),
        )
    }

    @Test
    fun `nothing is added twice, and a row gone since is not written`() {
        // "New" was typed in meanwhile, and the row that differed was deleted.
        val now = TorqueImport.plan(preview.items, listOf(saved(1, "Blank"), saved(3, "Same", 30.0), saved(4, "New")))
        val write = ImportPlans.recheck(preview, now, ImportMode.REPLACE_MATCHING) { it.id }
        // "Differs" is new now, but the preview said it would replace a row, not add one; the new "New" was never listed.
        assertEquals(emptyList<String>() to listOf(1L), write.names())
    }
}
