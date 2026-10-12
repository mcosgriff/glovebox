package io.cosgriff.glovebox.ui.service

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.nhtsa.toNotice
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.RecordMark
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The vehicle History should show next, and the record to mark in it: one just added, or just restored. */
data class HistoryFocus(val vehicleId: Long, val recordId: Long? = null, val mark: RecordMark? = null)

sealed interface RecordEvent {
    data class Saved(val recordId: Long) : RecordEvent
    /** [addedPhotos] and [removedPhotos] are file names; Undo takes the first away again and brings the second back. */
    data class Edited(
        val previous: ServiceRecordWithItems,
        val addedPhotos: List<String> = emptyList(),
        val removedPhotos: List<String> = emptyList(),
    ) : RecordEvent
    /** [recallIds] are recalls this record was the work for; Undo links them again. */
    data class Deleted(val record: ServiceRecordWithItems, val recallIds: List<Long> = emptyList()) : RecordEvent

    /** A recall marked done; [recordId] is the record added for the work, if the user wanted one. */
    data class RecallDone(val recallId: Long, val recordId: Long?) : RecordEvent

    /** A recall put back to open. [recall] is how it stood; [record] went with it and returns on Undo. */
    data class RecallReopened(val recall: RecallEntity, val record: ServiceRecordWithItems?) : RecordEvent
}

/**
 * Every write to the service log goes through here so the app can announce it (snackbar) and
 * offer Undo from whichever screen the user lands on. Reminders need no fixing up either way:
 * "last done" is derived from the log.
 */
@Singleton
class RecordActions @Inject constructor(
    private val service: ServiceDao,
    private val recalls: RecallDao,
    private val vehicles: VehicleDao,
    private val photos: RecordPhotoStore,
) {
    private val _events = Channel<RecordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _focus = MutableStateFlow<HistoryFocus?>(null)

    /** Set by a save, a delete or a restore, which all end on the History list. Cleared once History has shown it. */
    val focus: StateFlow<HistoryFocus?> = _focus.asStateFlow()

    fun clearFocus() {
        _focus.value = null
    }

    /** [photoFiles] are already encrypted in the photo folder; this makes them the record's. */
    suspend fun add(record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>, photoFiles: List<String> = emptyList()) {
        val id = service.insertWithItems(record, items)
        service.insertPhotos(photoFiles.map { PhotoEntity(recordId = id, fileName = it) })
        _focus.value = HistoryFocus(record.vehicleId, id, RecordMark.ADDED)
        _events.send(RecordEvent.Saved(id))
    }

    /**
     * A bare "last done" for one schedule item: a record with that one line, flagged as such. It is
     * entered on the spot where its result shows, so there is no snackbar and History isn't opened.
     */
    suspend fun addLastDone(vehicleId: Long, scheduleItemId: Long, name: String, date: LocalDate, odometer: Int) {
        service.insertWithItems(
            ServiceRecordEntity(vehicleId = vehicleId, date = date, odometer = odometer, diy = true, enteredAsLastDone = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = scheduleItemId, label = name)),
        )
    }

    /**
     * [removedPhotoIds] lose their rows now; their files wait for [discard], so Undo can still
     * bring them back.
     */
    suspend fun replace(
        previous: ServiceRecordWithItems,
        record: ServiceRecordEntity,
        items: List<ServiceRecordItemEntity>,
        addedPhotos: List<String> = emptyList(),
        removedPhotoIds: Set<Long> = emptySet(),
    ) {
        service.replaceWithItems(record, items)
        service.deletePhotos(removedPhotoIds.toList())
        service.insertPhotos(addedPhotos.map { PhotoEntity(recordId = record.id, fileName = it) })
        val removed = previous.photos.filter { it.id in removedPhotoIds }.map { it.fileName }
        _events.send(RecordEvent.Edited(previous, addedPhotos, removed))
    }

    suspend fun delete(record: ServiceRecordWithItems) {
        // The recall stays marked done; only its link to this record goes.
        val recallIds = recalls.idsLinkedTo(record.record.id)
        service.deleteRecord(record.record)
        _focus.value = HistoryFocus(record.record.vehicleId)
        _events.send(RecordEvent.Deleted(record, recallIds))
    }

    /**
     * Marks a recall done. With [record] the work also goes in the service log, as a line linked to
     * no schedule item, so it never resets a reminder. Does nothing if the recall is already done,
     * which is what stops a second record being added for it.
     */
    suspend fun completeRecall(recallId: Long, completedOn: LocalDate, record: ServiceRecordEntity?) {
        val recall = recalls.byId(recallId)?.takeIf { it.completedOn == null } ?: return
        val recordId = record?.let {
            val line = ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = Recalls.serviceLabel(recall.toNotice()))
            service.insertWithItems(it, listOf(line))
        }
        recalls.setDone(recall.id, completedOn, recordId)
        _events.send(RecordEvent.RecallDone(recall.id, recordId))
    }

    /** Back to open. The record that marking it done added is removed with it. */
    suspend fun reopenRecall(recallId: Long) {
        val recall = recalls.byId(recallId)?.takeIf { it.completedOn != null } ?: return
        val record = recall.serviceRecordId?.let { service.record(it) }
        record?.let { service.deleteRecord(it.record) }
        recalls.setDone(recall.id, null, null)
        _events.send(RecordEvent.RecallReopened(recall, record))
    }

    /** False if there was nothing left to undo it on: the vehicle was deleted while the bar was up. */
    suspend fun undo(event: RecordEvent): Boolean = when (event) {
        is RecordEvent.Edited ->
            if (vehicles.byId(event.previous.record.vehicleId) != null) {
                service.replaceWithItems(event.previous.record, event.previous.items)
                service.clearPhotos(event.previous.record.id)
                service.insertPhotos(event.previous.photos.map { it.copy(id = 0) })
                photos.delete(event.addedPhotos)
                true
            } else {
                false
            }
        is RecordEvent.Deleted -> {
            val id = restore(event.record)
            if (id != null) {
                recalls.linkRecord(event.recallIds, id)
                _focus.value = HistoryFocus(event.record.record.vehicleId, id, RecordMark.RESTORED)
            }
            id != null
        }
        is RecordEvent.RecallDone -> {
            if (event.recordId == null) recalls.setDone(event.recallId, null, null)
            true
        }
        is RecordEvent.RecallReopened -> {
            recalls.setDone(event.recall.id, event.recall.completedOn, event.record?.let { restore(it) })
            true
        }
        is RecordEvent.Saved -> true
    }

    /** Null if the vehicle was deleted while the snackbar was up: there is nothing to put the record back on. */
    private suspend fun restore(record: ServiceRecordWithItems): Long? {
        if (vehicles.byId(record.record.vehicleId) == null) return null
        val id = service.insertWithItems(record.record, record.items.map { it.copy(id = 0) })
        // The files were left alone when the record went, for exactly this.
        service.insertPhotos(record.photos.map { it.copy(id = 0, recordId = id) })
        return id
    }

    /**
     * The Undo for [event] was not taken, so the photo files it was keeping can go. Deleting a
     * record only removes its rows; the files outlive the snackbar and are removed here. If the app
     * is closed first, the launch sweep removes them instead.
     */
    suspend fun discard(event: RecordEvent) {
        photos.delete(
            when (event) {
                is RecordEvent.Deleted -> event.record.photos.map { it.fileName }
                is RecordEvent.Edited -> event.removedPhotos
                is RecordEvent.RecallReopened -> event.record?.photos.orEmpty().map { it.fileName }
                is RecordEvent.Saved, is RecordEvent.RecallDone -> emptyList()
            },
        )
    }
}

/** Lets the nav host listen for log changes and run Undo. */
@HiltViewModel
class RecordEventsViewModel @Inject constructor(private val actions: RecordActions) : ViewModel() {
    val events = actions.events
    /** [onUndone] runs only if there was still something to undo it on. */
    fun undo(event: RecordEvent, onUndone: () -> Unit) = viewModelScope.launch { if (actions.undo(event)) onUndone() }
    fun discard(event: RecordEvent) = viewModelScope.launch { actions.discard(event) }
}
