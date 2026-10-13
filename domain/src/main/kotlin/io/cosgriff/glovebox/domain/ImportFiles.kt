package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToLong

/** Something that couldn't go into a file written from the app, and why, in the reader's own words. */
data class LeftOut(val name: String, val why: String)

/**
 * An import file written from what is on a vehicle. [text] is null when nothing could be written.
 * [count] is how many items the file holds; [leftOut] are the ones it doesn't, which the user is
 * told about before anything is saved.
 */
data class WrittenFile(val text: String?, val count: Int, val leftOut: List<LeftOut> = emptyList())

/**
 * What the writers of import files share ([ScheduleExport], [TorqueExport], [MilestoneExport]).
 *
 * **A file written here is always one [ImportFormat] accepts.** The app's own screens are looser than
 * the format in places (a schedule item's interval has no upper limit, two torque rows can share a
 * name), so nothing is written on trust: each item is put through the kind's own reader first, and
 * one the reader would refuse is left out and reported instead of spoiling the whole file. Like any
 * import file, what is written never names a vehicle.
 */
object ImportFiles {
    const val MIME_TYPE = "application/json"

    /** Lets an editor check the file as it is changed; Glovebox ignores it on the way back in. */
    fun schema(kind: ImportKind): String = "https://raw.githubusercontent.com/mcosgriff/glovebox/main/docs/import/schema/${kind.id}.schema.json"

    /** "glovebox-torque-specs-2003-honda-accord.json". [model] is the year, make and model: who else the file would suit. */
    fun fileName(what: String, model: String): String = "glovebox-$what-${Exports.slug(model)}.json"

    private val pretty = Json { prettyPrint = true }

    internal fun text(kind: ImportKind, items: List<JsonObject>, own: JsonObjectBuilder.() -> Unit = {}): String = pretty.encodeToString(
        buildJsonObject {
            put("\$schema", schema(kind))
            put("glovebox", "import")
            put("version", ImportFormat.VERSION)
            put("kind", kind.id)
            own()
            put("items", JsonArray(items))
        },
    ) + "\n"

    /**
     * Sorts [candidates] (a name, what makes it the same item as another, and how it would be
     * written) into what goes in the file and what is left out: anything [read] refuses, a second
     * item under one [key], and whatever is past [max].
     */
    internal fun <K> sift(
        kind: ImportKind,
        candidates: List<Triple<String, K, JsonObject>>,
        max: Int,
        read: (JsonArray, ImportCheck) -> Unit,
        twice: String,
        own: JsonObjectBuilder.() -> Unit = {},
    ): WrittenFile {
        val kept = mutableListOf<JsonObject>()
        val leftOut = mutableListOf<LeftOut>()
        val seen = mutableSetOf<K>()
        for ((name, key, json) in candidates) {
            val check = ImportCheck()
            read(buildJsonArray { add(json) }, check)
            val refused = check.invalid().errors.firstOrNull()
            when {
                refused != null -> leftOut += LeftOut(name, why(refused))
                !seen.add(key) -> leftOut += LeftOut(name, twice)
                kept.size >= max -> leftOut += LeftOut(name, "a file holds $max at most")
                else -> kept += json
            }
        }
        return WrittenFile(if (kept.isEmpty()) null else text(kind, kept, own), kept.size, leftOut)
    }

    /** The reader's message without "items[0].", which means nothing outside a file. */
    private fun why(error: ImportError): String {
        val field = error.path.removePrefix("items[0]").removePrefix(".")
        return if (field.isEmpty()) error.message else "$field ${error.message}"
    }

    /** A figure as a file should show it: 100, not 100.0, and no more than two decimals. */
    internal fun JsonObjectBuilder.putFigure(key: String, value: Double) {
        val hundredths = (value * 100).roundToLong()
        if (hundredths % 100 == 0L) put(key, hundredths / 100) else put(key, hundredths / 100.0)
    }
}

/**
 * **An import file is always in miles**, whatever the vehicle counts in: its fields say so
 * (`everyMiles`, `atMiles`), and a file is passed between owners. So a file's figures are put into the
 * vehicle's unit before anything is compared or saved, and a file written from a kilometre vehicle is
 * put into miles. Schedule figures go the way a manual gives them (5,000 mi is 8,000 km), readings
 * exactly. For a vehicle in miles every one of these returns what it was given.
 *
 * A kilometre figure that isn't a multiple of 8 has no whole number of miles, so it can come back
 * from a file 1 km out. Until the format can carry kilometres, that is the price of the file.
 */
object FileDistances {
    private val miles = DistanceUnit.MILES

    private fun Interval.into(unit: DistanceUnit) = copy(miles = this.miles?.let(unit::scheduled))
    private fun Interval.out(unit: DistanceUnit) = copy(miles = this.miles?.let(unit::scheduledInMiles))

    /** A schedule file's items for a vehicle in [unit]. */
    fun schedule(items: List<ImportedScheduleItem>, unit: DistanceUnit): List<ImportedScheduleItem> =
        if (unit == miles) items else items.map { it.copy(normal = it.normal.into(unit), severe = it.severe?.into(unit)) }

    /** A vehicle's schedule items as a file holds them. */
    fun scheduleOut(items: List<SavedScheduleItem>, unit: DistanceUnit): List<SavedScheduleItem> =
        if (unit == miles) items else items.map { it.copy(normal = it.normal?.out(unit), severe = it.severe?.out(unit)) }

    /** A milestone file's table, in mileage order, and how it repeats, for a vehicle in [unit]. */
    fun milestones(items: List<ImportedMilestone>, repeats: Int?, unit: DistanceUnit): Pair<List<ImportedMilestone>, Int?> {
        if (unit == miles) return items to repeats
        val sorted = items.sortedBy { it.atMiles }
        val at = DistanceConversion.milestones(sorted.map { it.atMiles }, miles, unit)
        // A cycle is never shorter than its table.
        return sorted.mapIndexed { i, m -> m.copy(atMiles = at[i]) } to repeats?.let { maxOf(unit.scheduled(it), at.lastOrNull() ?: 0) }
    }

    /** A vehicle's milestone table and how it repeats, as a file holds them. */
    fun milestonesOut(table: List<SavedMilestone>, repeats: Int?, unit: DistanceUnit): Pair<List<SavedMilestone>, Int?> {
        if (unit == miles) return table to repeats
        val sorted = table.sortedBy { it.atMiles }
        val at = DistanceConversion.milestones(sorted.map { it.atMiles }, unit, miles)
        return sorted.mapIndexed { i, m -> m.copy(atMiles = at[i]) } to repeats?.let { maxOf(unit.scheduledInMiles(it), at.lastOrNull() ?: 0) }
    }

    /** A history file's records, with each reading as the vehicle's dash would have shown it. */
    fun history(items: List<ImportedRecord>, unit: DistanceUnit): List<ImportedRecord> =
        if (unit == miles) items else items.map { it.copy(odometer = unit.measured(it.odometer)) }
}
