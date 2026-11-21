package io.cosgriff.glovebox.data.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.domain.ImportPlans
import io.cosgriff.glovebox.domain.ImportedTorqueSpec
import io.cosgriff.glovebox.domain.KnownSpecList
import io.cosgriff.glovebox.domain.KnownSpecs
import io.cosgriff.glovebox.domain.SavedTorqueSpec
import io.cosgriff.glovebox.domain.Torque
import io.cosgriff.glovebox.domain.TorqueCategory
import io.cosgriff.glovebox.domain.TorqueImport
import io.cosgriff.glovebox.domain.TorqueSource
import javax.inject.Inject
import javax.inject.Singleton

/** A torque row as an import, a bundled list or a file written from the vehicle sees it. */
fun TorqueSpecEntity.toSaved() = SavedTorqueSpec(
    id = id,
    name = name,
    category = TorqueCategory.of(category),
    socket = socketSize,
    torque = newtonMeters?.let(::Torque),
    source = TorqueSource.of(source),
    note = note,
)

/**
 * The writes to a vehicle's torque rows that take more than one row (#645): a `torque-specs` file and a bundled
 * list. Each is one transaction that plans again from the rows as they stand inside it, and writes only a
 * fastener's setting onto a row it matched ([TorqueDao.setSpec]), never a whole row from a copy read earlier: a
 * row's name, category and the list it came from stay as they are.
 */
@Singleton
class TorqueSpecWriter @Inject constructor(private val db: GloveboxDatabase) {
    private val torque = db.torque()

    /**
     * Writes a file as its [preview] planned it: nothing is added twice, and no row changed since is written over
     * unless the preview listed it ([ImportPlans.recheck]). Nothing is deleted.
     */
    suspend fun import(vehicleId: Long, preview: ImportPlan<ImportedTorqueSpec, SavedTorqueSpec>, mode: ImportMode) = transaction {
        val now = TorqueImport.plan(preview.items, torque.specsFor(vehicleId).map { it.toSaved() })
        val write = ImportPlans.recheck(preview, now, mode) { it.id }
        torque.insertAll(write.added.map { it.toEntity(vehicleId, it.source) })
        write.replaced.forEach { (item, saved) -> torque.setSpec(saved.id, item.socket, item.torque?.newtonMeters, item.source.name, item.note) }
    }

    /**
     * Adds a bundled [list]'s values: new rows, and rows that are only a name. Every value arrives as a reference to
     * double-check, marked for good with the list it came from. A row the owner has put anything in is never touched.
     */
    suspend fun addKnown(vehicleId: Long, list: KnownSpecList) = transaction {
        val plan = KnownSpecs.plan(list, torque.specsFor(vehicleId).map { it.toSaved() })
        torque.insertAll(plan.new.map { it.toEntity(vehicleId, TorqueSource.REFERENCE).copy(knownList = list.id) })
        plan.fills.forEach { (item, saved) ->
            torque.setSpec(saved.id, item.socket, item.torque?.newtonMeters, TorqueSource.REFERENCE.name, item.note)
            torque.setKnownList(saved.id, list.id)
        }
    }

    private fun ImportedTorqueSpec.toEntity(vehicleId: Long, source: TorqueSource) = TorqueSpecEntity(
        vehicleId = vehicleId,
        name = name,
        category = category.name,
        socketSize = socket,
        newtonMeters = torque?.newtonMeters,
        source = source.name,
        note = note,
    )

    /** The DAO calls in [block] are kept together or not at all. */
    private suspend fun <T> transaction(block: suspend () -> T): T =
        db.useWriterConnection { it.immediateTransaction { block() } }
}
