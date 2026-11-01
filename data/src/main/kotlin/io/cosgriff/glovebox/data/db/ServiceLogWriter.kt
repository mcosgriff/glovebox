package io.cosgriff.glovebox.data.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.domain.DoneChange
import io.cosgriff.glovebox.domain.VisitRecord
import io.cosgriff.glovebox.domain.Visits
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A recommendation a new record keeps for later (#572): its row, and the open one from the same shop it replaces. */
data class KeptRecommendation(val row: ShopRecommendationEntity, val replaces: Long? = null)

/**
 * A recommendation an import closes (#593): by the record it was offered for, one of the import's [added] records by
 * its place, or a saved record it replaces, by its id ([savedId]).
 */
data class ImportClose(val recommendationId: Long, val added: Int? = null, val savedId: Long? = null)

/** What a deleted record had kept, closed and replaced (#572), for its Undo to put back. */
data class RecordRecommendations(
    val kept: List<ShopRecommendationEntity> = emptyList(),
    val done: List<Long> = emptyList(),
    val replaced: List<ReplacedLink> = emptyList(),
)

/** A record as `Visits` sees it (#307). */
fun ServiceRecordEntity.toVisit(): VisitRecord = VisitRecord(id, visitId, date, odometer, shopName.takeIf { !diy })

/**
 * Every write to the service log that takes more than one step, each in one transaction (#419), so a
 * process that dies part-way leaves the log as it was: never a record without its photos, or a recall's
 * record while the recall is still open. Saying what changed, Undo and the photo files are
 * `RecordActions`'; this only writes rows.
 */
@Singleton
class ServiceLogWriter @Inject constructor(private val db: GloveboxDatabase) {
    private val service = db.service()
    private val recalls = db.recalls()
    private val vehicles = db.vehicles()
    private val recommendations = db.recommendations()

    /** A recall marked done; [recordId] is the record added for the work, if there was one. */
    data class Completed(val recordId: Long?)

    /** The recall as it stood before it was opened again, and the record that went with it. */
    data class Reopened(val recall: RecallEntity, val record: ServiceRecordWithItems?)

    /** The DAO calls in [block] are kept together or not at all. */
    internal suspend fun <T> transaction(block: suspend () -> T): T =
        db.useWriterConnection { it.immediateTransaction { block() } }

    /**
     * [photoFiles] are already encrypted in the photo folder; this makes them the record's. [kept] are the shop's
     * recommendations it keeps for later (#572), each replacing the open one it names; [done] are open ones the owner
     * said this record is the work for, which it closes.
     */
    suspend fun add(
        record: ServiceRecordEntity,
        items: List<ServiceRecordItemEntity>,
        photoFiles: List<String> = emptyList(),
        jobs: List<ServiceRecordJobEntity> = emptyList(),
        parts: List<ServiceRecordJobPartEntity> = emptyList(),
        kept: List<KeptRecommendation> = emptyList(),
        done: List<Long> = emptyList(),
    ): Long = transaction {
        val id = service.insertWithItems(record, items, jobs, parts)
        service.insertPhotos(photoFiles.map { PhotoEntity(recordId = id, fileName = it) })
        kept.forEachIndexed { i, k ->
            val keptId = recommendations.insert(k.row.copy(id = 0, vehicleId = record.vehicleId, recordId = id, position = i))
            k.replaces?.let { recommendations.setReplaced(it, keptId) }
        }
        if (done.isNotEmpty()) recommendations.setDone(done, id)
        id
    }

    /**
     * Deletes [record]. What it kept goes with it, and what it closed or replaced opens again (#572), by the foreign
     * keys; the rows say what was there, for [restore] to put back.
     */
    suspend fun delete(record: ServiceRecordEntity): RecordRecommendations = transaction {
        val was = RecordRecommendations(recommendations.keptBy(record.id), recommendations.doneBy(record.id), recommendations.replacedBy(record.id))
        service.deleteRecord(record)
        was
    }

    /**
     * [removedPhotoIds] lose their rows; their files are the caller's to keep for Undo or delete. [done] is what the
     * edit closes of the shop's recommendations and opens again of what it had closed (#593).
     */
    suspend fun replace(
        record: ServiceRecordEntity,
        items: List<ServiceRecordItemEntity>,
        addedPhotos: List<String>,
        removedPhotoIds: Set<Long>,
        jobs: List<ServiceRecordJobEntity> = emptyList(),
        parts: List<ServiceRecordJobPartEntity> = emptyList(),
        done: DoneChange = DoneChange(),
    ) = transaction {
        service.replaceWithItems(record, items, jobs, parts)
        service.deletePhotos(removedPhotoIds.toList())
        service.insertPhotos(addedPhotos.map { PhotoEntity(recordId = record.id, fileName = it) })
        closeAndReopen(record.id, done)
    }

    /**
     * One import, applied whole or not at all (#593): [added] records, [replaced] ones under their own ids, the
     * recommendations the owner left ticked closed by the record each was offered for, and [reopened] (closed by a
     * replaced record that no longer looks like the work) open again.
     */
    suspend fun applyImport(
        added: List<RecordRows>,
        replaced: List<RecordRows>,
        done: List<ImportClose> = emptyList(),
        reopened: List<Long> = emptyList(),
    ) = transaction {
        val ids = added.map { (record, items, jobs, parts) -> service.insertWithItems(record, items, jobs, parts) }
        replaced.forEach { (record, items, jobs, parts) -> service.replaceWithItems(record, items, jobs, parts) }
        if (reopened.isNotEmpty()) recommendations.setDone(reopened, null)
        done.groupBy({ it.savedId ?: ids[checkNotNull(it.added)] }, { it.recommendationId }).forEach { (id, closed) -> recommendations.setDone(closed, id) }
    }

    private suspend fun closeAndReopen(recordId: Long, done: DoneChange) {
        if (done.reopen.isNotEmpty()) recommendations.setDone(done.reopen, null)
        if (done.close.isNotEmpty()) recommendations.setDone(done.close, recordId)
    }

    /** A kept recommendation closed by the owner on [on], or open again with null (#572). Logs nothing. */
    suspend fun closeRecommendation(id: Long, on: LocalDate?) = recommendations.setClosed(id, on)

    /**
     * [previous] back as it was, photos included, and what the edit [done] to the recommendations undone (#593): what
     * it closed opens, what it opened closes again. False if its vehicle has gone.
     */
    suspend fun revertEdit(previous: ServiceRecordWithItems, done: DoneChange = DoneChange()): Boolean = transaction {
        if (vehicles.byId(previous.record.vehicleId) == null) return@transaction false
        service.replaceWithItems(previous.record, previous.items, previous.jobs, previous.parts)
        service.clearPhotos(previous.record.id)
        service.insertPhotos(previous.photos.map { it.copy(id = 0) })
        closeAndReopen(previous.record.id, DoneChange(close = done.reopen, reopen = done.close))
        true
    }

    /**
     * Marks a recall done, with [record] and its [lines] as the work if there is one, joined to the visit of
     * [visitWith] if given. Null, and nothing written, if the recall is already done: that is what stops a
     * second record being added for it.
     */
    suspend fun completeRecall(
        recallId: Long,
        completedOn: LocalDate,
        record: ServiceRecordEntity?,
        lines: List<ServiceRecordItemEntity>,
        visitWith: Long? = null,
    ): Completed? = transaction {
        val recall = recalls.byId(recallId)?.takeIf { it.completedOn == null } ?: return@transaction null
        val recordId = record?.let { service.insertWithItems(it, lines) }
        if (recordId != null && visitWith != null) joinVisit(recordId, visitWith)
        recalls.setDone(recall.id, completedOn, recordId)
        Completed(recordId)
    }

    /** Back to open, removing the record marking it done added. Null if it wasn't done. */
    suspend fun reopenRecall(recallId: Long): Reopened? = transaction {
        val recall = recalls.byId(recallId)?.takeIf { it.completedOn != null } ?: return@transaction null
        val record = recall.serviceRecordId?.let { service.record(it) }
        record?.let { service.deleteRecord(it.record) }
        recalls.setDone(recall.id, null, null)
        Reopened(recall, record)
    }

    /** Undoes [reopenRecall]: the recall done again, with its record back if its vehicle is still there. */
    suspend fun redoRecall(reopened: Reopened) = transaction {
        val recordId = reopened.record?.let { restoreRows(it) }
        recalls.setDone(reopened.recall.id, reopened.recall.completedOn, recordId)
    }

    /**
     * A deleted record back, with its photos and as the work for [recallIds]. Null if its vehicle has gone:
     * there is nothing to put it back on.
     */
    suspend fun restore(record: ServiceRecordWithItems, recallIds: List<Long>, was: RecordRecommendations = RecordRecommendations()): Long? = transaction {
        restoreRows(record)?.also { id ->
            recalls.linkRecord(recallIds, id)
            // What it kept, under their own ids so what they replaced points at them again, and what it closed.
            // A link to a row that went meanwhile is dropped, not left pointing nowhere.
            val rows = recommendations.existing(was.kept.mapNotNull { it.replacedById }).toSet()
            recommendations.insertAll(
                was.kept.map { k ->
                    k.copy(
                        recordId = id,
                        replacedById = k.replacedById?.takeIf { it in rows },
                        doneByRecordId = k.doneByRecordId?.takeIf { service.record(it) != null },
                    )
                },
            )
            was.replaced.forEach { recommendations.setReplaced(it.id, it.replacedById) }
            if (was.done.isNotEmpty()) recommendations.setDone(was.done, id)
        }
    }

    /**
     * [recordId] and [otherId] are one visit (#307), with whatever visit each was in already. Only which
     * visit they are in changes; nothing in either record does.
     */
    suspend fun link(recordId: Long, otherId: Long) = transaction { joinVisit(recordId, otherId) }

    /** [recordId] out of its visit; the rest stay linked, unless one is left. */
    suspend fun unlink(recordId: Long) = transaction {
        val record = service.record(recordId)?.record ?: return@transaction
        val all = service.recordsFor(record.vehicleId).map { it.record.toVisit() }
        val r = all.find { it.id == recordId } ?: return@transaction
        service.unlink(Visits.unlinked(r, all))
    }

    private suspend fun joinVisit(recordId: Long, otherId: Long) {
        val record = service.record(recordId)?.record ?: return
        val all = service.recordsFor(record.vehicleId).map { it.record.toVisit() }
        val a = all.find { it.id == recordId } ?: return
        val b = all.find { it.id == otherId } ?: return
        service.link(Visits.joined(a, b, all))
    }

    private suspend fun restoreRows(record: ServiceRecordWithItems): Long? {
        if (vehicles.byId(record.record.vehicleId) == null) return null
        val id = service.insertWithItems(record.record, record.items, record.jobs, record.parts)
        // The files were left alone when the record went, for exactly this.
        service.insertPhotos(record.photos.map { it.copy(id = 0, recordId = id) })
        return id
    }
}
