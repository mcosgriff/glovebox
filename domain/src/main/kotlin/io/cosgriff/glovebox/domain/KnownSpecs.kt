package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.math.abs

/** Where a bundled list came from. Shown wherever the list is offered or one of its values is. */
data class KnownSpecSource(
    /** Short enough for a row: a manual's title, or a site's name. */
    val name: String,
    /** Where it can be read, if it is online. A printed manual has none. */
    val url: String?,
    /** The day the values were copied, as YYYY-MM-DD. */
    val retrieved: String,
    /** What kind of source it is, in a sentence: who compiled it and what it isn't. Shown to the user. */
    val note: String,
    /**
     * Why Glovebox may ship these values: written permission and from whom, or that the maintainer took
     * them from a manual they have. Not shown in the app; it is the record for whoever asks later.
     */
    val basis: String,
)

/**
 * A list of torque values that ships with the app for one model, to save the user starting from
 * empty rows. [id] is kept on every row added from it (`torque_specs.knownList`), for good.
 */
data class KnownSpecList(
    val id: String,
    /** "5th-generation 4Runner" */
    val title: String,
    val make: String,
    val model: String,
    val years: IntRange,
    val source: KnownSpecSource,
    val items: List<ImportedTorqueSpec>,
)

sealed interface KnownSpecFile {
    data class Ok(val list: KnownSpecList) : KnownSpecFile
    data class Invalid(val errors: List<ImportError>) : KnownSpecFile
}

/** What adding a list to a vehicle would do. Nothing the user has filled in is ever written over. */
data class KnownSpecPlan(
    /** No row of the vehicle's has this name in this category. */
    val new: List<ImportedTorqueSpec>,
    /** A row with this name exists but is only a name (from "Start with common fasteners", say); it takes the list's values. */
    val fills: List<Pair<ImportedTorqueSpec, SavedTorqueSpec>>,
    /** A row with this name already says something. It is the user's, and stays exactly as it is. */
    val kept: List<Pair<ImportedTorqueSpec, SavedTorqueSpec>>,
) {
    val changes: Int get() = new.size + fills.size
}

/**
 * Torque values that ship with the app.
 *
 * **Glovebox has no factory torque data and invents none.** A bundled list is somebody's list,
 * named: it says where it came from and why it may be shipped ([KnownSpecSource]), it is offered only
 * for the model it is for,
 * and every value from it arrives as [TorqueSource.REFERENCE], which the screen tags DOUBLE-CHECK.
 * The tag goes when the user says they have checked the value (against the factory manual or otherwise),
 * or changes it; that the row started from a list is remembered permanently beside it.
 *
 * A list is a file in `assets/known-specs/`, with the items written exactly as in a `torque-specs`
 * import file and read by the same strict reader, so a new list is a data-only addition.
 */
object KnownSpecs {
    /** The folder in the app's assets the lists live in. */
    const val ASSET_DIR = "known-specs"

    private val keys = setOf("id", "title", "make", "model", "years", "source", "items")

    fun parse(text: String): KnownSpecFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return KnownSpecFile.Invalid(listOf(ImportError("", "This isn't a JSON file.")))
        }
        val check = ImportCheck()
        val o = check.obj(root, "", keys, required = keys) ?: return KnownSpecFile.Invalid(check.invalid().errors)
        val id = o["id"]?.let { check.string(it, "id", 60) }
        val title = o["title"]?.let { check.string(it, "title", 60) }
        val make = o["make"]?.let { check.string(it, "make", 40) }
        val model = o["model"]?.let { check.string(it, "model", 40) }
        val years = o["years"]?.let { y ->
            check.obj(y, "years", setOf("from", "to"), required = setOf("from", "to"))?.let { range ->
                val from = range["from"]?.let { check.int(it, "years.from", 1900, 2100) }
                val to = range["to"]?.let { check.int(it, "years.to", 1900, 2100) }
                if (from != null && to != null && to < from) check.fail("years.to", "is before years.from")
                if (from != null && to != null && to >= from) from..to else null
            }
        }
        val source = o["source"]?.let { s ->
            check.obj(s, "source", setOf("name", "url", "retrieved", "note", "basis"), required = setOf("name", "retrieved", "note", "basis"))?.let { src ->
                val name = src["name"]?.let { check.string(it, "source.name", 40) }
                val url = src["url"]?.let { check.string(it, "source.url", 300) }
                val retrieved = src["retrieved"]?.let { check.string(it, "source.retrieved", 10) }
                val note = src["note"]?.let { check.string(it, "source.note", 200) }
                val basis = src["basis"]?.let { check.string(it, "source.basis", 300) }
                if (url != null && !url.startsWith("https://")) check.fail("source.url", "must be an https address")
                if (retrieved != null && !Regex("""\d{4}-\d{2}-\d{2}""").matches(retrieved)) check.fail("source.retrieved", "must be a date written as \"YYYY-MM-DD\"")
                if (name != null && retrieved != null && note != null && basis != null && ("url" !in src || url != null)) {
                    KnownSpecSource(name, url, retrieved, note, basis)
                } else {
                    null
                }
            }
        }
        val raw = o["items"]
        val items = raw?.let { TorqueImport.read(it, check) }.orEmpty()
        // A list supplies values, and says nothing about where a user got theirs.
        (raw as? JsonArray)?.forEachIndexed { i, e ->
            val item = e as? JsonObject ?: return@forEachIndexed
            if ("torque" !in item) check.fail("items[$i]", "has no torque value; a known spec is a value")
            if ("source" in item) check.fail("items[$i].source", "isn't for a bundled list: every value in one is a reference to double-check")
        }
        if (check.failed || id == null || title == null || make == null || model == null || years == null || source == null) {
            return KnownSpecFile.Invalid(check.invalid().errors)
        }
        return KnownSpecFile.Ok(KnownSpecList(id, title, make, model, years, source, items))
    }

    /**
     * The list for a vehicle, or null: the make and model as NHTSA or the user spelled them, ignoring
     * case and space, and the model year in the list's range. No match means no offer; nothing is
     * guessed from a near miss, because a torque value for the wrong model is worse than none.
     */
    fun match(lists: List<KnownSpecList>, year: Int, make: String, model: String): KnownSpecList? =
        lists.firstOrNull { year in it.years && same(it.make, make) && same(it.model, model) }

    private fun same(a: String, b: String) = a.filterNot(Char::isWhitespace).equals(b.filterNot(Char::isWhitespace), ignoreCase = true)

    /**
     * What adding [list] to a vehicle with [existing] rows would do. Rows match on category and name,
     * as an import's do. Unlike an import there is no "replace": a row the user has put anything in
     * is theirs, and a reference value never writes over it.
     */
    fun plan(list: KnownSpecList, existing: List<SavedTorqueSpec>): KnownSpecPlan {
        val plan = ImportPlans.plan(
            items = list.items,
            existing = existing,
            key = { TorqueImport.key(it.name, it.category) },
            existingKey = { TorqueImport.key(it.name, it.category) },
            same = { _, _ -> false },
            blank = { _, e -> e.socket == null && e.torque == null && e.note == null },
        )
        return KnownSpecPlan(plan.new, plan.fills, plan.differs)
    }

    /**
     * Where a row's value is said to come from once it has been through the editor. [before] is what
     * it was. A reference value stays one until the user does one of three things: says the factory
     * service manual agrees ([fromFactoryManual]), says they have checked it some other way
     * ([checked]: most owners have a repair manual or a second source, not the factory one), or
     * changes it. The last two make it their own entry. Touching the socket size or the note changes
     * nothing about the value. A row with no value has no source to speak of.
     */
    fun sourceAfterEdit(before: TorqueSource?, was: Torque?, now: Torque?, fromFactoryManual: Boolean, checked: Boolean = false): TorqueSource = when {
        now == null -> TorqueSource.USER
        fromFactoryManual -> TorqueSource.FACTORY
        checked -> TorqueSource.USER
        before == TorqueSource.REFERENCE && was != null && abs(was.newtonMeters - now.newtonMeters) < 0.05 -> TorqueSource.REFERENCE
        else -> TorqueSource.USER
    }

    /** "26 reference values" */
    fun count(n: Int): String = if (n == 1) "1 reference value" else "$n reference values"

    /** What is said where a list is offered: whose it is, what it isn't, and what happens to the user's own rows. */
    fun offer(list: KnownSpecList, plan: KnownSpecPlan): String =
        "${count(plan.changes).replaceFirstChar { it.uppercase() }} from ${list.source.name}. ${list.source.note} " +
            "Each is tagged DOUBLE-CHECK until you have checked it against a service or repair manual and said so." +
            when (plan.kept.size) {
                0 -> ""
                1 -> " 1 fastener you have already filled in is left as it is."
                else -> " ${plan.kept.size} fasteners you have already filled in are left as they are."
            }
}
