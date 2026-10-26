package io.cosgriff.glovebox.domain

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
    override val id: String,
    /** "5th-generation 4Runner" */
    override val title: String,
    override val make: String,
    override val model: String,
    override val years: IntRange,
    override val source: KnownSpecSource,
    val items: List<ImportedTorqueSpec>,
) : KnownList

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

    private val keys = KnownLists.headerKeys + "items"

    fun parse(text: String): KnownSpecFile {
        val (o, check) = KnownLists.root(text, keys, required = keys)
        if (o == null) return KnownSpecFile.Invalid(check.invalid().errors)
        val header = KnownLists.header(o, check)
        val raw = o["items"]
        val items = raw?.let { TorqueImport.read(it, check) }.orEmpty()
        // A list supplies values, and says nothing about where a user got theirs.
        (raw as? JsonArray)?.forEachIndexed { i, e ->
            val item = e as? JsonObject ?: return@forEachIndexed
            if ("torque" !in item) check.fail("items[$i]", "has no torque value; a known spec is a value")
            if ("source" in item) check.fail("items[$i].source", "isn't for a bundled list: every value in one is a reference to double-check")
        }
        if (check.failed || header == null) return KnownSpecFile.Invalid(check.invalid().errors)
        return KnownSpecFile.Ok(KnownSpecList(header.id, header.title, header.make, header.model, header.years, header.source, items))
    }

    /**
     * The list for a vehicle, or null: the make and model as NHTSA or the user spelled them
     * ([KnownLists.same]), and the model year in the list's range. No match means no offer; nothing is
     * guessed from a near miss, because a torque value for the wrong model is worse than none.
     */
    fun match(lists: List<KnownSpecList>, year: Int, make: String, model: String): KnownSpecList? =
        lists.forModel(year, make, model).firstOrNull()

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
}
