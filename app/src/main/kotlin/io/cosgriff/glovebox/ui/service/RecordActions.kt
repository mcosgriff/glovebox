package io.cosgriff.glovebox.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.nhtsa.toNotice
import io.cosgriff.glovebox.domain.Recalls
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RecordEvent {
    data class Saved(val recordId: Long) : RecordEvent
    data class Edited(val previous: ServiceRecordWithItems) : RecordEvent
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
) {
    private val _events = Channel<RecordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun add(record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>) {
        _events.send(RecordEvent.Saved(service.insertWithItems(record, items)))
    }

    suspend fun replace(previous: ServiceRecordWithItems, record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>) {
        service.replaceWithItems(record, items)
        _events.send(RecordEvent.Edited(previous))
    }

    suspend fun delete(record: ServiceRecordWithItems) {
        // The recall stays marked done; only its link to this record goes.
        val recallIds = recalls.idsLinkedTo(record.record.id)
        service.deleteRecord(record.record)
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

    suspend fun undo(event: RecordEvent) {
        when (event) {
            is RecordEvent.Edited ->
                if (vehicles.byId(event.previous.record.vehicleId) != null) {
                    service.replaceWithItems(event.previous.record, event.previous.items)
                }
            // TODO: once photos exist, a deleted record's photo files must outlive the Undo window.
            is RecordEvent.Deleted -> restore(event.record)?.let { recalls.linkRecord(event.recallIds, it) }
            is RecordEvent.RecallDone -> if (event.recordId == null) recalls.setDone(event.recallId, null, null)
            is RecordEvent.RecallReopened ->
                recalls.setDone(event.recall.id, event.recall.completedOn, event.record?.let { restore(it) })
            is RecordEvent.Saved -> Unit
        }
    }

    /** Null if the vehicle was deleted while the snackbar was up: there is nothing to put the record back on. */
    private suspend fun restore(record: ServiceRecordWithItems): Long? {
        if (vehicles.byId(record.record.vehicleId) == null) return null
        return service.insertWithItems(record.record, record.items.map { it.copy(id = 0) })
    }
}

/** Lets the nav host listen for log changes and run Undo. */
@HiltViewModel
class RecordEventsViewModel @Inject constructor(private val actions: RecordActions) : ViewModel() {
    val events = actions.events
    fun undo(event: RecordEvent) = viewModelScope.launch { actions.undo(event) }
}
