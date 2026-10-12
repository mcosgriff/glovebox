package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * What an import file can hold; one kind per file. [id] is the file's `kind`. The format is
 * specified in `docs/import/README.md` and the JSON Schemas beside it: change those, this code
 * and the example files together, and never change how a version-1 file is read.
 */
enum class ImportKind(val id: String, val label: String) {
    SERVICE_HISTORY("service-history", "Service history"),
    INTERVAL_SCHEDULE("interval-schedule", "Maintenance schedule"),
    MILESTONE_SCHEDULE("milestone-schedule", "Milestone schedule"),
    TORQUE_SPECS("torque-specs", "Torque specs"),
}

/** One thing wrong with a file. [path] points at it the way the file is written: `items[3].torque.unit`. */
data class ImportError(val path: String, val message: String) {
    override fun toString(): String = if (path.isEmpty()) message else "$path: $message"
}

/** What reading a file came to. */
sealed interface ImportFile {
    /** [errors] is capped at [ImportFormat.MAX_ERRORS]; [more] is how many were left off. */
    data class Invalid(val errors: List<ImportError>, val more: Int = 0) : ImportFile

    /** A well-formed file of a kind this version of the app doesn't import yet. */
    data class NotYet(val kind: ImportKind) : ImportFile

    data class TorqueSpecs(val items: List<ImportedTorqueSpec>) : ImportFile
}

object ImportFormat {
    const val VERSION = 1

    /** Checked before a file is read at all. */
    const val MAX_BYTES = 2 * 1024 * 1024

    const val MAX_ERRORS = 25

    /** The kinds this version of the app can import; the rest are specified but not built. */
    val supported: Set<ImportKind> = setOf(ImportKind.TORQUE_SPECS)

    private val envelopeKeys = setOf("\$schema", "glovebox", "version", "kind", "items")

    /**
     * Reads and checks a whole file. Nothing is taken on trust and nothing is repaired: an unknown
     * field, a wrong type or a value out of range refuses the file, and every problem found is
     * reported, not just the first.
     */
    fun parse(text: String): ImportFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return ImportFile.Invalid(listOf(ImportError("", "This isn't a JSON file, or it has a mistake such as a missing comma or quote.")))
        }
        val check = ImportCheck()
        val envelope = check.obj(root, "", envelopeKeys, required = setOf("glovebox", "version", "kind", "items")) ?: return check.invalid()

        envelope["\$schema"]?.let { check.string(it, "\$schema", max = 300) }
        envelope["glovebox"]?.let { if (check.string(it, "glovebox", max = 20) != "import") check.fail("glovebox", "must be \"import\"") }
        val version = envelope["version"]?.let { check.int(it, "version", 1, Int.MAX_VALUE) }
        if (version != null && version > VERSION) {
            // Nothing else in a newer file can be judged by this version's rules.
            return ImportFile.Invalid(listOf(ImportError("version", "this file is for a newer version of Glovebox (format $version; this app reads $VERSION)")))
        }
        val kind = envelope["kind"]?.let { e ->
            val id = check.string(e, "kind", max = 40) ?: return@let null
            ImportKind.entries.firstOrNull { it.id == id } ?: null.also { check.fail("kind", "must be one of ${ImportKind.entries.joinToString { it.id }}") }
        }
        if (kind == null || check.failed) return check.invalid()
        if (kind !in supported) return ImportFile.NotYet(kind)

        return when (kind) {
            ImportKind.TORQUE_SPECS -> {
                val items = TorqueImport.read(envelope.getValue("items"), check)
                if (check.failed) check.invalid() else ImportFile.TorqueSpecs(items)
            }
            else -> ImportFile.NotYet(kind)
        }
    }
}

/**
 * Collects what is wrong with a file while it is read. Each reader returns null and records why
 * when a value isn't acceptable, so the caller can carry on and find the next problem.
 */
class ImportCheck {
    private val errors = mutableListOf<ImportError>()
    val failed: Boolean get() = errors.isNotEmpty()

    fun fail(path: String, message: String) {
        errors += ImportError(path, message)
    }

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
    fun string(e: JsonElement, path: String, max: Int): String? {
        if (e !is JsonPrimitive || !e.isString) return null.also { fail(path, "must be text in quotes") }
        val s = e.content
        return when {
            s.isEmpty() -> null.also { fail(path, "must not be empty; leave the field out instead") }
            s.length > max -> null.also { fail(path, "is ${s.length} characters; the most allowed is $max") }
            s.first().isWhitespace() || s.last().isWhitespace() -> null.also { fail(path, "must not start or end with a space") }
            s.any { it.isISOControl() } -> null.also { fail(path, "must be on one line, with no tabs or line breaks") }
            else -> s
        }
    }

    /** One of [values], spelled exactly. */
    fun <T> choice(e: JsonElement, path: String, values: Map<String, T>): T? {
        val s = (e as? JsonPrimitive)?.takeIf { it.isString }?.content
        return values[s] ?: null.also { fail(path, "must be one of ${values.keys.joinToString { "\"$it\"" }}") }
    }

    /** A whole number, written without quotes or a decimal point. */
    fun int(e: JsonElement, path: String, min: Int, max: Int): Int? {
        val n = (e as? JsonPrimitive)?.takeIf { !it.isString && it !is JsonNull }?.longOrNull
            ?: return null.also { fail(path, "must be a whole number, without quotes") }
        if (n < min || n > max) return null.also { fail(path, if (max == Int.MAX_VALUE) "must be $min or more" else "must be from $min to $max") }
        return n.toInt()
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
    /** These match a saved item that has nothing in it yet, so filling it in loses nothing; always done. */
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
        blank: (E) -> Boolean = { false },
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
                blank(match) -> fills += item to match
                else -> differs += item to match
            }
        }
        return ImportPlan(new, fills, unchanged, differs)
    }
}
