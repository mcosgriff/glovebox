package io.cosgriff.glovebox.ui.service

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.ServiceLogWriter
import io.cosgriff.glovebox.data.db.KeptRecommendation
import io.cosgriff.glovebox.data.db.ImportClose
import io.cosgriff.glovebox.domain.DoneChange
import io.cosgriff.glovebox.data.db.RecordRecommendations
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.RecordRows
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobPartEntity
import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.data.nhtsa.toNotice
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.RecordMark
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.lineFor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import io.cosgriff.glovebox.data.db.CatalogRuleEntity
import io.cosgriff.glovebox.data.db.toEntity
import io.cosgriff.glovebox.domain.CatalogRule
import io.cosgriff.glovebox.domain.CatalogRules
import javax.inject.Inject
import javax.inject.Singleton

/** The vehicle History should show next, and the record to mark in it: one just added, or just restored. */
data class HistoryFocus(val vehicleId: Long, val recordId: Long? = null, val mark: RecordMark? = null)

sealed interface RecordEvent {
    data class Saved(val recordId: Long) : RecordEvent
    /**
     * [addedPhotos] and [removedPhotos] are file names; Undo takes the first away again and brings the second back.
     * [done] is what the edit closed and opened again of the shop's recommendations (#593); Undo puts them back.
     */
    data class Edited(
        val previous: ServiceRecordWithItems,
        val addedPhotos: List<String> = emptyList(),
        val removedPhotos: List<String> = emptyList(),
        val done: DoneChange = DoneChange(),
    ) : RecordEvent
    /**
     * [recallIds] are recalls this record was the work for; Undo links them again. [recommendations] are what it kept,
     * closed and replaced of the shop's recommendations (#572); Undo puts those back too.
     */
    data class Deleted(
        val record: ServiceRecordWithItems,
        val recallIds: List<Long> = emptyList(),
        val recommendations: RecordRecommendations = RecordRecommendations(),
    ) : RecordEvent

    /** A kept recommendation closed by hand (#572), named by its [wording]; Undo opens it again. */
    data class RecommendationClosed(val id: Long, val wording: String) : RecordEvent

    /** A recall marked done; [recordId] is the record added for the work, if the user wanted one. */
    data class RecallDone(val recallId: Long, val recordId: Long?) : RecordEvent

    /** A recall put back to open. [recall] is how it stood; [record] went with it and returns on Undo. */
    data class RecallReopened(val recall: RecallEntity, val record: ServiceRecordWithItems?) : RecordEvent

    /**
     * Custom lines filed under catalog lines (#310). Undo puts [lineIds] back under Custom, takes away the rules
     * kept for [ruleKeys] and brings back the ones they [replaced].
     */
    data class Filed(val lineIds: List<Long>, val ruleKeys: List<String>, val replaced: List<CatalogRuleEntity>) : RecordEvent
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
    private val writer: ServiceLogWriter,
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
    suspend fun add(
        record: ServiceRecordEntity,
        items: List<ServiceRecordItemEntity>,
        photoFiles: List<String> = emptyList(),
        jobs: List<ServiceRecordJobEntity> = emptyList(),
        parts: List<ServiceRecordJobPartEntity> = emptyList(),
        kept: List<KeptRecommendation> = emptyList(),
        done: List<Long> = emptyList(),
    ) {
        val id = writer.add(record, items, photoFiles, jobs, parts, kept, done)
        _focus.value = HistoryFocus(record.vehicleId, id, RecordMark.ADDED)
        _events.send(RecordEvent.Saved(id))
    }

    /**
     * A bare "last done" for one schedule item: a record with that one line, flagged as such. It is
     * entered on the spot where its result shows, so there is no snackbar and History isn't opened.
     */
    suspend fun addLastDone(vehicleId: Long, item: ScheduleItemEntity, date: LocalDate, odometer: Int) {
        // Filed under the catalog line that resets this reminder, so History puts it in the right category.
        val line = ServiceRecordItemEntity(recordId = 0, scheduleItemId = item.id, label = item.name, catalogId = ServiceCatalog.lineFor(item.templateKey, item.service)?.id)
        service.insertWithItems(
            ServiceRecordEntity(vehicleId = vehicleId, date = date, odometer = odometer, diy = true, enteredAsLastDone = true),
            listOf(line),
        )
    }

    /**
     * An import of service history, in one transaction. Like [addLastDone] it announces nothing: the
     * Import page shows its own result. [replaced] records keep their id, photos and recall links. [done] are the
     * shop's recommendations the owner left ticked in the preview, closed by their records, and [reopened] those a
     * replaced record no longer looks like the work for (#593).
     */
    suspend fun applyImport(added: List<RecordRows>, replaced: List<RecordRows>, done: List<ImportClose> = emptyList(), reopened: List<Long> = emptyList()) =
        writer.applyImport(added, replaced, done, reopened)

    /** Points History at [vehicleId], for a screen that sends the user there. */
    fun showIn(vehicleId: Long) {
        _focus.value = HistoryFocus(vehicleId)
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
        jobs: List<ServiceRecordJobEntity> = emptyList(),
        parts: List<ServiceRecordJobPartEntity> = emptyList(),
        done: DoneChange = DoneChange(),
    ) {
        writer.replace(record, items, addedPhotos, removedPhotoIds, jobs, parts, done)
        val removed = previous.photos.filter { it.id in removedPhotoIds }.map { it.fileName }
        _events.send(RecordEvent.Edited(previous, addedPhotos, removed, done))
    }

    suspend fun delete(record: ServiceRecordWithItems) {
        // The recall stays marked done; only its link to this record goes.
        val recallIds = recalls.idsLinkedTo(record.record.id)
        val was = writer.delete(record.record)
        _focus.value = HistoryFocus(record.record.vehicleId)
        _events.send(RecordEvent.Deleted(record, recallIds, was))
    }

    /**
     * A kept recommendation closed by the owner (#572): it leaves the open list, logs nothing and moves no reminder.
     * Undo opens it again.
     */
    suspend fun closeRecommendation(id: Long, wording: String, today: LocalDate) {
        writer.closeRecommendation(id, today)
        _events.send(RecordEvent.RecommendationClosed(id, wording))
    }

    /**
     * Marks a recall done. With [record] the work also goes in the service log, as a line linked to
     * no schedule item, so it never resets a reminder. Does nothing if the recall is already done,
     * which is what stops a second record being added for it. [visitWith] is a record the user chose to
     * link the new one with, as one visit.
     */
    suspend fun completeRecall(recallId: Long, completedOn: LocalDate, record: ServiceRecordEntity?, visitWith: Long? = null) {
        val recall = recalls.byId(recallId) ?: return
        val line = ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = recallServiceLine(recall.toNotice()))
        // "Add to the visit on …" (#307): the recall's record joins the visit it was done on from the start.
        val done = writer.completeRecall(recall.id, completedOn, record, listOf(line), visitWith) ?: return
        _events.send(RecordEvent.RecallDone(recall.id, done.recordId))
    }

    /** Back to open. The record that marking it done added is removed with it. */
    suspend fun reopenRecall(recallId: Long) {
        val (recall, record) = writer.reopenRecall(recallId) ?: return
        _events.send(RecordEvent.RecallReopened(recall, record))
    }

    /**
     * Custom lines filed in bulk: each of [lines] (line id, catalog line) under its line, with [rules] kept for
     * future imports, in one transaction. Labels stay as written. One Undo puts it all back.
     */
    suspend fun fileCustom(lines: List<Pair<Long, String>>, rules: List<CatalogRule>, today: LocalDate) {
        if (lines.isEmpty()) return
        val keys = rules.map { CatalogRules.key(it.text) }
        val replaced = if (keys.isEmpty()) emptyList() else service.rulesFor(keys)
        service.fileLines(lines.groupBy({ it.second }, { it.first }), rules.map { it.toEntity(today) })
        _events.send(RecordEvent.Filed(lines.map { it.first }, keys, replaced))
    }

    /**
     * [recordId] and [otherId] are one visit (#307), with whatever visit each was in already. Only which
     * visit they are in changes; nothing in either record does, so there is nothing to undo but Unlink.
     */
    suspend fun link(recordId: Long, otherId: Long) = writer.link(recordId, otherId)

    /** [recordId] out of its visit; the rest stay linked, unless one is left. */
    suspend fun unlink(recordId: Long) = writer.unlink(recordId)

    /** False if there was nothing left to undo it on: the vehicle was deleted while the bar was up. */
    suspend fun undo(event: RecordEvent): Boolean = when (event) {
        is RecordEvent.Edited ->
            writer.revertEdit(event.previous, event.done).also { if (it) photos.delete(event.addedPhotos) }
        is RecordEvent.Deleted -> {
            val id = writer.restore(event.record, event.recallIds, event.recommendations)
            if (id != null) {
                _focus.value = HistoryFocus(event.record.record.vehicleId, id, RecordMark.RESTORED)
            }
            id != null
        }
        is RecordEvent.RecallDone -> {
            if (event.recordId == null) recalls.setDone(event.recallId, null, null)
            true
        }
        is RecordEvent.RecallReopened -> {
            writer.redoRecall(ServiceLogWriter.Reopened(event.recall, event.record))
            true
        }
        is RecordEvent.Filed -> {
            // Lines whose record went meanwhile are simply not there to move back.
            service.unfileLines(event.lineIds, event.ruleKeys, event.replaced)
            true
        }
        is RecordEvent.RecommendationClosed -> {
            writer.closeRecommendation(event.id, null)
            true
        }
        is RecordEvent.Saved -> true
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
                is RecordEvent.Saved, is RecordEvent.RecallDone, is RecordEvent.Filed, is RecordEvent.RecommendationClosed -> emptyList()
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
