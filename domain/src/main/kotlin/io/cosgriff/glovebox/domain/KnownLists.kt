package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** What every bundled list says about itself, whatever it holds: torque specs, schedules, "What it takes". */
interface KnownList {
    val id: String
    val title: String
    val make: String
    val model: String
    val years: IntRange
    val source: KnownSpecSource

    /**
     * Other names NHTSA gives the same car in these years ("Golf GTI" beside "GTI"), each one seen in vPIC's own
     * model list for the make and year and recorded on the list's page. Never a near miss or a related model.
     */
    val modelAliases: List<String> get() = emptyList()
}

/**
 * The lists for a vehicle's make and model (as NHTSA or the owner spelled them, matched by [KnownLists.same], or as one
 * of the list's [KnownList.modelAliases]) and model year.
 */
fun <T : KnownList> List<T>.forModel(year: Int, make: String, model: String): List<T> =
    filter { year in it.years && KnownLists.same(it.make, make) && (listOf(it.model) + it.modelAliases).any { m -> KnownLists.same(m, model) } }

internal data class KnownHeader(val id: String, val title: String, val make: String, val model: String, val years: IntRange, val source: KnownSpecSource)

internal object KnownLists {
    val headerKeys = setOf("id", "title", "make", "model", "years", "source")

    /**
     * A bundled file's text as its top-level object, with only [keys] and all of [required], and the check that
     * goes on reading it; the object is null when the text isn't JSON or isn't an object, which the check says.
     */
    fun root(text: String, keys: Set<String>, required: Set<String> = keys): Pair<JsonObject?, ImportCheck> {
        val check = ImportCheck()
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            check.fail("", "This isn't a JSON file.")
            return null to check
        }
        return check.obj(root, "", keys, required) to check
    }

    /** Null when any part is missing or wrong, which [check] then says. */
    fun header(o: JsonObject, check: ImportCheck): KnownHeader? {
        val id = o["id"]?.let { check.string(it, "id", 60) }
        val title = o["title"]?.let { check.string(it, "title", 60) }
        val make = o["make"]?.let { check.string(it, "make", 40) }
        val model = o["model"]?.let { check.string(it, "model", 40) }
        val years = o["years"]?.let { check.yearRange(it, "years") }
        val source = o["source"]?.let { s ->
            check.obj(s, "source", setOf("name", "url", "retrieved", "note", "basis"), required = setOf("name", "retrieved", "note", "basis"))?.let { src ->
                val name = src["name"]?.let { check.string(it, "source.name", 40) }
                val url = src["url"]?.let { check.string(it, "source.url", 300) }
                val retrieved = src["retrieved"]?.let { check.date(it, "source.retrieved") }?.toString()
                val note = src["note"]?.let { check.string(it, "source.note", 200) }
                val basis = src["basis"]?.let { check.string(it, "source.basis", 300) }
                if (url != null && !url.startsWith("https://")) check.fail("source.url", "must be an https address")
                if (name != null && retrieved != null && note != null && basis != null && ("url" !in src || url != null)) {
                    KnownSpecSource(name, url, retrieved, note, basis)
                } else {
                    null
                }
            }
        }
        if (id == null || title == null || make == null || model == null || years == null || source == null) return null
        return KnownHeader(id, title, make, model, years, source)
    }

    /** A make or model as NHTSA or the user spelled it, ignoring case, space and a trailing "(PHEV)" or "(HEV)". */
    fun same(a: String, b: String) = bare(a).equals(bare(b), ignoreCase = true)

    /**
     * vPIC tags some plug-ins and hybrids "(PHEV)" or "(HEV)" ("Prius Prime (PHEV)"), which an owner typing the name
     * leaves off (#475). Only those tags: other brackets name the model (Honda's "VT750 (Shadow Aero 750)").
     */
    private val powertrainTag = Regex("""\((PHEV|HEV)\)$""", RegexOption.IGNORE_CASE)

    private fun bare(name: String) = name.filterNot(Char::isWhitespace).replace(powertrainTag, "")

    /** An `engine` block: the engine a list is for, where the model had several in those years. */
    fun engine(e: JsonElement, check: ImportCheck): KnownEngine? =
        check.obj(e, "engine", setOf("label", "displacementL", "fuel", "powertrain"), required = setOf("label", "displacementL"))?.let { en ->
            val label = en["label"]?.let { check.string(it, "engine.label", 40) }
            val litres = en["displacementL"]?.let { check.number(it, "engine.displacementL") }
            val fuel = en["fuel"]?.let { check.string(it, "engine.fuel", 30) }
            val powertrain = en["powertrain"]?.let { check.string(it, "engine.powertrain", 20) }?.let { name ->
                Powertrains.fromFile(name).also {
                    if (it == null) check.fail("engine.powertrain", "must be one of ${Powertrain.entries.joinToString { p -> p.fileName }}")
                }
            }
            if (litres != null && (litres < 0.1 || litres > 20.0)) check.fail("engine.displacementL", "must be between 0.1 and 20")
            if (label != null && litres != null && ("fuel" !in en || fuel != null) && ("powertrain" !in en || powertrain != null)) {
                KnownEngine(label, litres, fuel, powertrain)
            } else {
                null
            }
        }

    private fun sameSize(list: KnownEngine, litres: Double) = kotlin.math.abs(list.displacementL - litres) < 0.05

    private fun fuelFits(list: KnownEngine, fuel: String?): Boolean? =
        if (list.fuel == null) true else fuel?.takeIf { it.isNotBlank() }?.contains(list.fuel, ignoreCase = true)

    /** Null is "can't tell": the list names a powertrain and none is on record. */
    private fun powertrainFits(list: KnownEngine, powertrain: Powertrain?): Boolean? =
        if (list.powertrain == null) true else powertrain?.let { it == list.powertrain }

    /**
     * Of [candidates] (the lists for one make, model and year), the one for the vehicle's [engine] text,
     * [fuel] and [powertrain]: an engine's own list when the size on record is its size (and the fuel and
     * powertrain, where it says them), otherwise one for every engine. **An engine or powertrain that isn't
     * known matches no list that names one.**
     */
    fun <T> forEngine(candidates: List<T>, engineOf: (T) -> KnownEngine?, engine: String?, fuel: String?, powertrain: Powertrain? = null): T? {
        val litres = Engines.displacementL(engine)
        return candidates.firstOrNull { c ->
            engineOf(c)?.let { e -> litres != null && sameSize(e, litres) && fuelFits(e, fuel) == true && powertrainFits(e, powertrain) == true } == true
        } ?: candidates.firstOrNull { engineOf(it) == null }
    }

    /** The engines' lists among [candidates] that what is on record doesn't rule out, for the owner to pick from. */
    fun <T> engineChoices(candidates: List<T>, engineOf: (T) -> KnownEngine?, engine: String?, fuel: String?, powertrain: Powertrain? = null): List<T> {
        if (forEngine(candidates, engineOf, engine, fuel, powertrain) != null) return emptyList()
        val litres = Engines.displacementL(engine)
        return candidates.filter { c ->
            engineOf(c)?.let { e -> (litres == null || sameSize(e, litres)) && fuelFits(e, fuel) != false && powertrainFits(e, powertrain) != false } == true
        }
    }
}
