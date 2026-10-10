package io.cosgriff.glovebox.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RecordEvent {
    data class Saved(val recordId: Long) : RecordEvent
    data class Edited(val previous: ServiceRecordWithItems) : RecordEvent
    data class Deleted(val record: ServiceRecordWithItems) : RecordEvent
}

/**
 * Every write to the service log goes through here so the app can announce it (snackbar) and
 * offer Undo from whichever screen the user lands on. Reminders need no fixing up either way:
 * "last done" is derived from the log.
 */
@Singleton
class RecordActions @Inject constructor(private val service: ServiceDao) {
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
        service.deleteRecord(record.record)
        _events.send(RecordEvent.Deleted(record))
    }

    suspend fun undo(event: RecordEvent) {
        when (event) {
            is RecordEvent.Edited -> service.replaceWithItems(event.previous.record, event.previous.items)
            // TODO: once photos exist, a deleted record's photo files must outlive the Undo window.
            is RecordEvent.Deleted -> service.insertWithItems(event.record.record, event.record.items.map { it.copy(id = 0) })
            is RecordEvent.Saved -> Unit
        }
    }
}

/** Lets the nav host listen for log changes and run Undo. */
@HiltViewModel
class RecordEventsViewModel @Inject constructor(private val actions: RecordActions) : ViewModel() {
    val events = actions.events
    fun undo(event: RecordEvent) = viewModelScope.launch { actions.undo(event) }
}
