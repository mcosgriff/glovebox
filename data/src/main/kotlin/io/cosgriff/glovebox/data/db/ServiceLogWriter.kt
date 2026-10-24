package io.cosgriff.glovebox.data.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.domain.VisitRecord
import io.cosgriff.glovebox.domain.Visits
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

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

    /** A recall marked done; [recordId] is the record added for the work, if there was one. */
    data class Completed(val recordId: Long?)

    /** The recall as it stood before it was opened again, and the record that went with it. */
    data class Reopened(val recall: RecallEntity, val record: ServiceRecordWithItems?)

    /** The DAO calls in [block] are kept together or not at all. */
    internal suspend fun <T> transaction(block: suspend () -> T): T =
        db.useWriterConnection { it.immediateTransaction { block() } }

    /** [photoFiles] are already encrypted in the photo folder; this makes them the record's. */
    suspend fun add(record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>, photoFiles: List<String> = emptyList()): Long = transaction {
        val id = service.insertWithItems(record, items)
        service.insertPhotos(photoFiles.map { PhotoEntity(recordId = id, fileName = it) })
        id
    }

    /** [removedPhotoIds] lose their rows; their files are the caller's to keep for Undo or delete. */
    suspend fun replace(record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>, addedPhotos: List<String>, removedPhotoIds: Set<Long>) = transaction {
        service.replaceWithItems(record, items)
        service.deletePhotos(removedPhotoIds.toList())
        service.insertPhotos(addedPhotos.map { PhotoEntity(recordId = record.id, fileName = it) })
    }

    /** [previous] back as it was, photos included. False if its vehicle has gone. */
    suspend fun revertEdit(previous: ServiceRecordWithItems): Boolean = transaction {
        if (vehicles.byId(previous.record.vehicleId) == null) return@transaction false
        service.replaceWithItems(previous.record, previous.items)
        service.clearPhotos(previous.record.id)
        service.insertPhotos(previous.photos.map { it.copy(id = 0) })
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
    suspend fun restore(record: ServiceRecordWithItems, recallIds: List<Long>): Long? = transaction {
        restoreRows(record)?.also { id -> recalls.linkRecord(recallIds, id) }
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
        val id = service.insertWithItems(record.record, record.items.map { it.copy(id = 0) })
        // The files were left alone when the record went, for exactly this.
        service.insertPhotos(record.photos.map { it.copy(id = 0, recordId = id) })
        return id
    }
}
