package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.LocalDate

/**
 * What an import file can hold; one kind per file. [id] is the file's `kind`. The format is
 * specified in `docs/import/README.md` and the JSON Schemas beside it: change those, this code
 * and the example files together, and never change how a version-1 file is read.
 */
/**
 * The kinds of import file; `app` names them. [version] is the newest format version of that kind this app reads;
 * it reads every version from 1 up to it, each exactly as that version is specified.
 */
enum class ImportKind(val id: String, val version: Int = 1) {
    /** Version 2 adds a record's priced jobs (#481). */
    SERVICE_HISTORY("service-history", 2),
    INTERVAL_SCHEDULE("interval-schedule"),
    MILESTONE_SCHEDULE("milestone-schedule"),
    TORQUE_SPECS("torque-specs"),
}

/** One thing wrong with a file. [path] points at it the way the file is written: `items[3].torque.unit`. */
data class ImportError(val path: String, val message: String) {
    override fun toString(): String = if (path.isEmpty()) message else "$path: $message"
}

/** What reading a file came to. */
sealed interface ImportFile {
    /** [errors] is capped at [ImportFormat.MAX_ERRORS]; [more] is how many were left off. */
    /** [carfax] is set, with no [errors], when a PDF was read and isn't a report that can be imported. */
    data class Invalid(val errors: List<ImportError>, val more: Int = 0, val carfax: CarfaxRefusal? = null) : ImportFile

    data class TorqueSpecs(val items: List<ImportedTorqueSpec>) : ImportFile

    /** [unit] is what the file's intervals are in; they are put into the vehicle's unit by [FileDistances]. */
    data class IntervalSchedule(val items: List<ImportedScheduleItem>, val unit: DistanceUnit = DistanceUnit.MILES) : ImportFile

    /** [unit] is what the file's odometer readings are in. */
    data class ServiceHistory(val items: List<ImportedRecord>, val unit: DistanceUnit = DistanceUnit.MILES) : ImportFile

    /** [repeatsEveryMiles] is the length of the cycle when the table starts again after its last milestone; null when it simply ends. */
    data class MilestoneSchedule(val items: List<ImportedMilestone>, val repeatsEveryMiles: Int? = null, val unit: DistanceUnit = DistanceUnit.MILES) : ImportFile
}

object ImportFormat {
    /** The version a file the app writes says it is. No kind the app writes has a version beyond 1. */
    const val VERSION = 1

    /** Checked before a file is read at all. */
    const val MAX_BYTES = 2 * 1024 * 1024

    const val MAX_ERRORS = 25

    /** The kinds this version of the app can import: all of them. */
    val supported: Set<ImportKind> = ImportKind.entries.toSet()

    private val envelopeKeys = setOf("\$schema", "glovebox", "version", "kind", "items")

    /** Fields beside `items` that only one kind has. In any other kind's file they are unknown fields, and refuse it. */
    private fun ownKeys(kind: ImportKind?): Set<String> = when (kind) {
        ImportKind.MILESTONE_SCHEDULE -> setOf(MilestoneImport.REPEATS, MilestoneImport.REPEATS_KM)
        else -> emptySet()
    }

    /**
     * Reads and checks a whole file. Nothing is taken on trust and nothing is repaired: an unknown
     * field, a wrong type or a value out of range refuses the file, and every problem found is
     * reported, not just the first. [today] is what "not in the future" is measured against.
     */
    fun parse(text: String, today: LocalDate = LocalDate.now()): ImportFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return ImportFile.Invalid(listOf(ImportError("", "This isn't a JSON file, or it has a mistake such as a missing comma or quote.")))
        }
        val check = ImportCheck()
        // Which fields may sit beside `items` depends on the kind, so that is looked at first; it is checked properly below.
        val named = ((root as? JsonObject)?.get("kind") as? JsonPrimitive)?.takeIf { it.isString }?.content
        val allowed = envelopeKeys + ownKeys(ImportKind.entries.firstOrNull { it.id == named })
        val envelope = check.obj(root, "", allowed, required = setOf("glovebox", "version", "kind", "items")) ?: return check.invalid()

        envelope["\$schema"]?.let { check.string(it, "\$schema", max = 300) }
        envelope["glovebox"]?.let { if (check.string(it, "glovebox", max = 20) != "import") check.fail("glovebox", "must be \"import\"") }
        val version = envelope["version"]?.let { check.int(it, "version", 1, Int.MAX_VALUE) }
        // Each kind has versions of its own; a kind this app doesn't know is judged by the newest of any.
        val newest = ImportKind.entries.firstOrNull { it.id == named }?.version ?: ImportKind.entries.maxOf { it.version }
        if (version != null && version > newest) {
            // Nothing else in a newer file can be judged by this version's rules.
            val reads = if (newest == 1) "1" else "1 to $newest"
            return ImportFile.Invalid(listOf(ImportError("version", "this file is for a newer version of Glovebox (format $version; this app reads $reads)")))
        }
        val kind = envelope["kind"]?.let { e ->
            val id = check.string(e, "kind", max = 40) ?: return@let null
            ImportKind.entries.firstOrNull { it.id == id } ?: null.also { check.fail("kind", "must be one of ${ImportKind.entries.joinToString { it.id }}") }
        }
        if (kind == null || version == null || check.failed) return check.invalid()

        return when (kind) {
            ImportKind.TORQUE_SPECS -> {
                val items = TorqueImport.read(envelope.getValue("items"), check)
                if (check.failed) check.invalid() else ImportFile.TorqueSpecs(items)
            }
            ImportKind.INTERVAL_SCHEDULE -> {
                val items = ScheduleImport.read(envelope.getValue("items"), check)
                if (check.failed) check.invalid() else ImportFile.IntervalSchedule(items, check.unit)
            }
            ImportKind.SERVICE_HISTORY -> {
                val items = HistoryImport.read(envelope.getValue("items"), check, today, version)
                if (check.failed) check.invalid() else ImportFile.ServiceHistory(items, check.unit)
            }
            ImportKind.MILESTONE_SCHEDULE -> {
                val items = MilestoneImport.read(envelope.getValue("items"), check)
                val repeats = MilestoneImport.repeats(envelope, items, check)
                if (check.failed) check.invalid() else ImportFile.MilestoneSchedule(items, repeats, check.unit)
            }
        }
    }
}

/**
 * Collects what is wrong with a file while it is read. Each reader returns null and records why
 * when a value isn't acceptable, so the caller can carry on and find the next problem.
 */
/**
 * The entries of a list said twice, found as they are read: [seen] gives the index of the entry before this one
 * with the same key, so the reader can report it with the rest of that item's errors and in their order.
 */
class Repeats<K : Any> {
    private val at = mutableMapOf<K, Int>()

    /** The index of the last earlier entry with [key], or null for the first; this one is remembered either way. */
    fun seen(key: K, index: Int): Int? = at.put(key, index)
}

/**
 * A distance an import file gives in either unit, under the field named for it: [miles] or [km], each with its cap.
 * The readers check it and the writers name it from this one value, so a file the app writes never uses a name its
 * reader doesn't know.
 */
data class DistanceField(val miles: String, val km: String, val maxMiles: Int, val maxKm: Int) {
    fun name(unit: DistanceUnit): String = if (unit == DistanceUnit.KILOMETERS) km else miles

    val names: Set<String> get() = setOf(miles, km)
}

class ImportCheck {
    private val errors = mutableListOf<ImportError>()
    val failed: Boolean get() = errors.isNotEmpty()

    fun fail(path: String, message: String) {
        errors += ImportError(path, message)
    }

    private var seenUnit: DistanceUnit? = null
    private var seenAt = ""

    /** What the file's distances are in: kilometres if it used the kilometre fields, otherwise miles. */
    val unit: DistanceUnit get() = seenUnit ?: DistanceUnit.MILES

    /**
     * A distance that a file gives either in miles ([milesKey]) or in kilometres ([kmKey]): each
     * figure names its own unit, and **a file gives every distance in one unit**, so the first one
     * read settles it and one in the other unit is refused. Null when the field is absent, and when
     * it is there but wrong, which has been reported; [has] tells the two apart.
     */
    fun distance(o: JsonObject, path: String, field: DistanceField): Int? {
        val (milesKey, kmKey, maxMiles, maxKm) = field
        if (milesKey in o && kmKey in o) return null.also { fail(at(path, kmKey), "give \"$milesKey\" or \"$kmKey\", not both") }
        val (key, found, max) = when {
            milesKey in o -> Triple(milesKey, DistanceUnit.MILES, maxMiles)
            kmKey in o -> Triple(kmKey, DistanceUnit.KILOMETERS, maxKm)
            else -> return null
        }
        val value = int(o.getValue(key), at(path, key), 1, max)
        val first = seenUnit
        if (first == null) {
            seenUnit = found
            seenAt = at(path, key)
        } else if (first != found) {
            return null.also { fail(at(path, key), "is in ${found.plural}, but $seenAt is in ${first.plural}; a file gives every distance in one unit") }
        }
        return value
    }

    fun has(o: JsonObject, field: DistanceField): Boolean = field.miles in o || field.km in o

    fun invalid(): ImportFile.Invalid =
        ImportFile.Invalid(errors.take(ImportFormat.MAX_ERRORS), (errors.size - ImportFormat.MAX_ERRORS).coerceAtLeast(0))

    /** An object with only [allowed] fields and all of [required]. */
    fun obj(e: JsonElement, path: String, allowed: Set<String>, required: Set<String> = emptySet()): JsonObject? {
        if (e !is JsonObject) return null.also { fail(path, "must be an object: { ... }") }
        (e.keys - allowed).forEach { fail(at(path, it), "isn't a field Glovebox knows; remove it or check the spelling") }
        (required - e.keys).forEach { fail(at(path, it), "is required") }
        return e
    }

    fun array(e: JsonElement, path: String, min: Int, max: Int): JsonArray? {
        if (e !is JsonArray) return null.also { fail(path, "must be a list: [ ... ]") }
        if (e.size < min) return null.also { fail(path, if (min == 1) "must have at least one entry" else "must have at least $min entries") }
        if (e.size > max) return null.also { fail(path, "has ${e.size} entries; the most allowed is $max") }
        return e
    }

    /**
     * Text of 1 to [max] characters with no space at either end and no line breaks. Nothing is
     * trimmed for the user: what is in the file is exactly what would be saved.
     */
    fun string(e: JsonElement, path: String, max: Int): String? = written(e, path, max, multiline = false)

    /**
     * Longer text that may run over several lines: 1 to [max] characters, no space or blank line at
     * either end, and no control characters other than a line break.
     */
    fun text(e: JsonElement, path: String, max: Int): String? = written(e, path, max, multiline = true)

    private fun written(e: JsonElement, path: String, max: Int, multiline: Boolean): String? {
        if (e !is JsonPrimitive || !e.isString) return null.also { fail(path, "must be text in quotes") }
        val s = e.content
        return when {
            s.isEmpty() -> null.also { fail(path, "must not be empty; leave the field out instead") }
            s.length > max -> null.also { fail(path, "is ${s.length} characters; the most allowed is $max") }
            s.first().isWhitespace() || s.last().isWhitespace() ->
                null.also { fail(path, if (multiline) "must not start or end with a space or a blank line" else "must not start or end with a space") }
            multiline && s.any { it.isISOControl() && it != '\n' } ->
                null.also { fail(path, "must not contain tabs or other control characters; line breaks are fine") }
            !multiline && s.any { it.isISOControl() } -> null.also { fail(path, "must be on one line, with no tabs or line breaks") }
            else -> s
        }
    }

    /** One of [values], spelled exactly. */
    fun <T> choice(e: JsonElement, path: String, values: Map<String, T>): T? {
        val s = (e as? JsonPrimitive)?.takeIf { it.isString }?.content
        return values[s] ?: null.also { fail(path, "must be one of ${values.keys.joinToString { "\"$it\"" }}") }
    }

    /** A whole number, written without quotes. `7500.0` counts, as it does to a JSON Schema; `7500.5` doesn't. */
    fun int(e: JsonElement, path: String, min: Int, max: Int): Int? {
        val n = (e as? JsonPrimitive)?.takeIf { !it.isString && it !is JsonNull }?.doubleOrNull?.takeIf { it.isFinite() && it == Math.floor(it) }
            ?: return null.also { fail(path, "must be a whole number, without quotes") }
        if (n < min || n > max) return null.also { fail(path, if (max == Int.MAX_VALUE) "must be $min or more" else "must be from $min to $max") }
        return n.toInt()
    }

    /** A run of model years, `{ "from": 2017, "to": 2024 }`, both ends included. */
    fun yearRange(e: JsonElement, path: String): IntRange? =
        obj(e, path, setOf("from", "to"), required = setOf("from", "to"))?.let { range ->
            val from = range["from"]?.let { int(it, at(path, "from"), 1900, 2100) }
            val to = range["to"]?.let { int(it, at(path, "to"), 1900, 2100) }
            if (from != null && to != null && to < from) fail(at(path, "to"), "is before $path.from")
            if (from != null && to != null && to >= from) from..to else null
        }

    /** A day written `"YYYY-MM-DD"`, and one the calendar has. */
    fun date(e: JsonElement, path: String): java.time.LocalDate? {
        val s = string(e, path, 10) ?: return null
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) return null.also { fail(path, "must be a date written as \"YYYY-MM-DD\"") }
        return try {
            java.time.LocalDate.parse(s)
        } catch (_: java.time.format.DateTimeParseException) {
            null.also { fail(path, "isn't a day the calendar has") }
        }
    }

    /** A number, written without quotes. */
    fun number(e: JsonElement, path: String): Double? =
        (e as? JsonPrimitive)?.takeIf { !it.isString && it !is JsonNull }?.doubleOrNull?.takeIf { it.isFinite() }
            ?: null.also { fail(path, "must be a number, without quotes") }

    companion object {
        fun at(path: String, field: String): String = if (path.isEmpty()) field else "$path.$field"
        fun at(path: String, index: Int): String = "$path[$index]"
    }
}

/** What the user chose to do with items that match something already saved. */
enum class ImportMode {
    /** Matching items are left exactly as they are. */
    ADD_NEW,

    /** Matching items that differ are replaced with the file's version. */
    REPLACE_MATCHING,
}

/**
 * A file's items set against what the vehicle already has, worked out before anything is written.
 * Nothing already saved is ever deleted, whichever [ImportMode] is chosen.
 *
 * [I] is an item from the file and [E] the saved one it matches.
 */
data class ImportPlan<I, E>(
    /** Nothing saved matches these; they are always added. */
    val new: List<I>,
    /** These match a saved item that says less than the file and loses nothing by taking it; always done. */
    val fills: List<Pair<I, E>>,
    /** These match a saved item that already says the same; never touched. */
    val same: List<Pair<I, E>>,
    /** These match a saved item that says something else; replaced only in [ImportMode.REPLACE_MATCHING]. */
    val differs: List<Pair<I, E>>,
) {
    val total: Int get() = new.size + fills.size + same.size + differs.size

    /** Saved items that will be written over, with what replaces each. */
    fun replaced(mode: ImportMode): List<Pair<I, E>> = fills + if (mode == ImportMode.REPLACE_MATCHING) differs else emptyList()

    /** How many of the file's items the import will write. */
    fun changes(mode: ImportMode): Int = new.size + replaced(mode).size
}

object ImportPlans {
    /**
     * [key] decides what matches; a file never has two items with the same key (its reader refuses
     * that). When several saved items share a key, the first is the match and the rest are left be.
     */
    fun <I, E, K> plan(
        items: List<I>,
        existing: List<E>,
        key: (I) -> K,
        existingKey: (E) -> K,
        same: (I, E) -> Boolean,
        blank: (I, E) -> Boolean = { _, _ -> false },
    ): ImportPlan<I, E> {
        val saved = existing.groupBy(existingKey).mapValues { it.value.first() }
        val new = mutableListOf<I>()
        val fills = mutableListOf<Pair<I, E>>()
        val unchanged = mutableListOf<Pair<I, E>>()
        val differs = mutableListOf<Pair<I, E>>()
        for (item in items) {
            val match = saved[key(item)]
            when {
                match == null -> new += item
                same(item, match) -> unchanged += item to match
                blank(item, match) -> fills += item to match
                else -> differs += item to match
            }
        }
        return ImportPlan(new, fills, unchanged, differs)
    }
}
