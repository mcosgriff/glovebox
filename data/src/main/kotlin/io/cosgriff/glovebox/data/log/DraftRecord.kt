package io.cosgriff.glovebox.data.log

import io.cosgriff.glovebox.data.db.ServiceRecordWithItems
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.domain.ImportedRecord
import io.cosgriff.glovebox.domain.ReminderChange
import io.cosgriff.glovebox.domain.SavedRecord
import io.cosgriff.glovebox.domain.ServiceEntry
import java.time.LocalDate

/**
 * A record as far as the reminders care, saved or not, for [VehicleLog.whatIf]. [id] is below zero while it is
 * unsaved; [odometer] is as typed; [lines] are each line's (`scheduleItemId`, `catalogId`).
 */
data class DraftRecord(val id: Long, val date: LocalDate, val odometer: Int, val lines: List<Pair<Long?, String?>>)

fun ServiceRecordWithItems.toDraft(): DraftRecord = DraftRecord(record.id, record.date, record.odometer, items.map { it.scheduleItemId to it.catalogId })

/** What [VehicleLog.whatIf] found: the reminders that move, and the log's entries after the change, in dash terms. */
data class WhatIf(val changes: List<ReminderChange>, val entries: List<ServiceEntry>)

/**
 * The vehicle's records as they would stand once [plan] is imported in [mode]: a replaced record keeps its date and
 * reading and takes the file's lines, and the file's new records follow, numbered -1, -2, … in [ImportPlan.new]'s
 * order, so an entry leads back to the record it came from.
 */
fun VehicleLog.afterImport(plan: ImportPlan<ImportedRecord, SavedRecord>, mode: ImportMode): List<DraftRecord> {
    val schedule = items.map { it.toRef() }
    fun lines(r: ImportedRecord) = HistoryImport.lines(r, schedule)
    val replaced = plan.replaced(mode).associate { (item, saved) -> saved.id to lines(item) }
    val kept = records.map { r -> r.toDraft().let { d -> replaced[d.id]?.let { d.copy(lines = it) } ?: d } }
    return kept + plan.new.mapIndexed { i, r -> DraftRecord(-1L - i, r.date, r.odometer, lines(r)) }
}

/** Which of [ImportPlan.new] the record [id] from [afterImport] is; null for a saved record. */
fun newIndexOf(id: Long): Int? = if (id < 0) (-1L - id).toInt() else null
