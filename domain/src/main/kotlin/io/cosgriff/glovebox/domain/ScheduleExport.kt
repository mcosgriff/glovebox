package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.ImportFiles.putFigure
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A vehicle's schedule written out as an `interval-schedule` import file.
 *
 * **Every tracked item goes out, the typical ones Glovebox suggested as well as the user's own**,
 * with the intervals as the user has them: a schedule is what is being tracked, and one made by
 * ticking and adjusting the typical list would otherwise be an empty file. Untracked items are not
 * part of it. A typical item is written with the `service` line that resets it, so that in the
 * vehicle it is imported into it is reset by the same tick, and the import can offer to stop
 * tracking the typical item it doubles ([ScheduleImport.doubles]).
 *
 * The file has both intervals of an item that has a severe one, but not which is in use, a snooze,
 * or when anything was last done: those belong to the vehicle, not the schedule.
 */
object ScheduleExport {
    /** [unit] is what the vehicle counts in; the file is in miles ([FileDistances]). */
    fun file(items: List<SavedScheduleItem>, unit: DistanceUnit): WrittenFile = ImportFiles.sift(
        kind = ImportKind.INTERVAL_SCHEDULE,
        candidates = FileDistances.scheduleOut(items.filter { it.tracked }, unit).map { item -> Triple(item.name.trim(), ScheduleImport.key(item.name), json(item)) },
        max = ScheduleImport.MAX_ITEMS,
        read = { array, check -> ScheduleImport.read(array, check) },
        twice = "another item in the schedule has the same name",
    )

    private fun json(item: SavedScheduleItem): JsonObject = buildJsonObject {
        put("name", item.name.trim())
        // An item with no interval is written without one, which the reader refuses in its own words.
        item.normal?.let { interval(it) }
        item.severe?.let { put("severe", buildJsonObject { interval(it) }) }
        item.rule?.let { rule -> put("rule", ScheduleImport.rules.entries.first { it.value == rule }.key) }
        service(item)?.let { put("service", it) }
    }

    private fun JsonObjectBuilder.interval(interval: Interval) {
        interval.miles?.let { put("everyMiles", it) }
        interval.months?.let { put("everyMonths", it) }
    }

    /** The user's own item says which line resets it; a typical one is reset by the line for its template. */
    private fun service(item: SavedScheduleItem): String? =
        if (item.templateKey == null) item.service?.takeIf { it in HistoryImport.services }
        else HistoryImport.services.values.firstOrNull { it.resets == item.templateKey }?.id

    fun fileName(model: String): String = ImportFiles.fileName("schedule", model)
}

/**
 * A vehicle's torque specs written out as a `torque-specs` import file: every row, filled in or
 * not. Each value is written in [unit], the one the user works in, to two decimals, which is
 * closer than [TorqueImport] needs to call it the same setting when the file is read back.
 */
object TorqueExport {
    fun file(rows: List<SavedTorqueSpec>, unit: TorqueUnit): WrittenFile = ImportFiles.sift(
        kind = ImportKind.TORQUE_SPECS,
        candidates = rows.map { row -> Triple(row.name.trim(), TorqueImport.key(row.name, row.category), json(row, unit)) },
        max = TorqueImport.MAX_ITEMS,
        read = { array, check -> TorqueImport.read(array, check) },
        twice = "another row in the same category has the same name",
    )

    private fun json(row: SavedTorqueSpec, unit: TorqueUnit): JsonObject = buildJsonObject {
        put("name", row.name.trim())
        put("category", TorqueImport.categories.entries.first { it.value == row.category }.key)
        row.socket?.let { put("socket", it) }
        row.torque?.let { torque ->
            // The file's cap is a whole figure per unit, a shade under 1,000 N·m in the others; a value up there is written in N·m.
            val written = unit.takeIf { torque.inUnit(it) <= TorqueImport.maxIn(it) } ?: TorqueUnit.NEWTON_METER
            put(
                "torque",
                buildJsonObject {
                    putFigure("value", torque.inUnit(written))
                    put("unit", TorqueImport.units.entries.first { it.value == written }.key)
                },
            )
            // Where a value came from means nothing without the value.
            put("source", TorqueImport.sources.entries.first { it.value == row.source }.key)
        }
        row.note?.let { put("note", it) }
    }

    fun fileName(model: String): String = ImportFiles.fileName("torque-specs", model)
}
