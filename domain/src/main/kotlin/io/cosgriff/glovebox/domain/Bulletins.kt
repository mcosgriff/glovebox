package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

/** The free-repair kinds of maker's bulletin Glovebox bundles (#459); `app` names each. */
enum class BulletinKind { SERVICE_CAMPAIGN, WARRANTY_EXTENSION, OVER_THE_AIR }

/** A model, as NHTSA spells it, and the model years a bulletin names it for. */
data class BulletinProduct(val model: String, val years: List<Int>)

/**
 * A maker's communication to its dealers that NHTSA holds: a service campaign, a warranty extension or an
 * over-the-air update. Text is NHTSA's, in English as it comes.
 */
data class Bulletin(
    /** NHTSA's id for the communication. */
    val id: String,
    /** The maker's own number for it ("VWP-18-07", "26TD01"): what to quote to a dealer. */
    val number: String,
    /** When the maker sent it, if NHTSA says. */
    val date: LocalDate?,
    val kind: BulletinKind,
    /**
     * NHTSA didn't type it (it typed nothing before 2024); it is here because its summary names a warranty
     * extension, service campaign or customer support program, and [kind] is read from those words.
     */
    val byWording: Boolean,
    val summary: String,
    val products: List<BulletinProduct>,
    val components: List<String>,
    /** The bulletin itself on NHTSA's server, where one answered when the list was made; opened in the browser. */
    val pdf: String?,
)

/** One model's bundled bulletins for the model [years] its maker's schedules cover, as NHTSA's files stood on [asOf]. */
data class BulletinList(
    val make: String,
    val model: String,
    val modelAliases: List<String>,
    val years: Set<Int>,
    val asOf: LocalDate,
    val bulletins: List<Bulletin>,
)

/** A vehicle's bulletins for its year, newest first; [asOf] is how current the bundled list is. */
data class VehicleBulletins(val asOf: LocalDate, val bulletins: List<Bulletin>)

sealed interface BulletinFile {
    data class Ok(val list: BulletinList) : BulletinFile
    data class Invalid(val errors: List<ImportError>) : BulletinFile
}

/**
 * The maker's free-repair bulletins for the models Glovebox has maker's schedules for, bundled from NHTSA's
 * manufacturer communications files by `scripts/bulletins.py` (#459). NHTSA has no API for them; nothing is
 * fetched, and the bulletins are matched by year, make and model like recalls.
 */
object Bulletins {
    const val ASSET_DIR = "bulletins"

    private const val NHTSA_HOST = "static.nhtsa.gov"

    private val kinds = BulletinKind.entries.associateBy { it.name }

    fun parse(text: String): BulletinFile {
        val (o, check) = KnownLists.root(text, setOf("make", "model", "modelAliases", "years", "asOf", "bulletins"))
        if (o == null) return BulletinFile.Invalid(check.invalid().errors)
        val make = o["make"]?.let { check.string(it, "make", 40) }
        val model = o["model"]?.let { check.string(it, "model", 40) }
        val aliases = o["modelAliases"]?.let { e -> check.array(e, "modelAliases", 0, 5)?.mapIndexedNotNull { i, n -> check.string(n, ImportCheck.at("modelAliases", i), 40) } }
        val years = o["years"]?.let { years(it, "years", check) }
        val asOf = o["asOf"]?.let { check.date(it, "asOf") }
        val bulletins = o["bulletins"]?.let { all ->
            check.array(all, "bulletins", 0, 5_000)?.mapIndexedNotNull { i, e -> bulletin(e, ImportCheck.at("bulletins", i), check) }
        }
        if (check.failed || make == null || model == null || aliases == null || years == null || asOf == null || bulletins == null) {
            return BulletinFile.Invalid(check.invalid().errors)
        }
        return BulletinFile.Ok(BulletinList(make, model, aliases, years.toSet(), asOf, bulletins))
    }

    private fun bulletin(e: JsonElement, path: String, check: ImportCheck): Bulletin? {
        val b = check.obj(e, path, setOf("id", "number", "date", "kind", "byWording", "summary", "products", "components", "pdf")) ?: return null
        fun at(field: String) = ImportCheck.at(path, field)
        val id = b["id"]?.let { check.string(it, at("id"), 12) }
        val number = b["number"]?.let { check.string(it, at("number"), 128) }
        val date = b["date"]?.takeUnless { it is JsonNull }?.let { check.date(it, at("date")) }
        val kind = b["kind"]?.let { check.choice(it, at("kind"), kinds) }
        val byWording = b["byWording"]?.let { v ->
            (v as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
                ?: null.also { check.fail(at("byWording"), "must be true or false, without quotes") }
        }
        val summary = b["summary"]?.let { check.string(it, at("summary"), 4_000) }
        val products = b["products"]?.let { p -> check.array(p, at("products"), 1, 20)?.mapIndexedNotNull { j, pe -> product(pe, ImportCheck.at(at("products"), j), check) } }
        val components = b["components"]?.let { c ->
            check.array(c, at("components"), 0, 20)?.mapIndexedNotNull { j, ce -> check.string(ce, ImportCheck.at(at("components"), j), 256) }
        }
        val pdf = b["pdf"]?.takeUnless { it is JsonNull }?.let { pdf(it, at("pdf"), check) }
        // date and pdf may be null, written as null; anything else that didn't read is already a failure.
        val nullable = (b["date"] is JsonNull || date != null) && (b["pdf"] is JsonNull || pdf != null)
        if (id == null || number == null || kind == null || byWording == null || summary == null || products == null || components == null || !nullable) return null
        return Bulletin(id, number, date, kind, byWording, summary, products, components, pdf)
    }

    private fun product(e: JsonElement, path: String, check: ImportCheck): BulletinProduct? {
        val p = check.obj(e, path, setOf("model", "years")) ?: return null
        val model = p["model"]?.let { check.string(it, ImportCheck.at(path, "model"), 256) }
        val years = p["years"]?.let { years(it, ImportCheck.at(path, "years"), check) }
        return if (model != null && years != null) BulletinProduct(model, years) else null
    }

    private fun years(e: JsonElement, path: String, check: ImportCheck): List<Int>? =
        check.array(e, path, 1, 100)?.mapIndexedNotNull { k, ye -> check.int(ye, ImportCheck.at(path, k), 1900, 2100) }

    /** Only an https address on NHTSA's server. */
    private fun pdf(e: JsonElement, path: String, check: ImportCheck): String? {
        val url = check.string(e, path, 300) ?: return null
        val host = try {
            URI(url).takeIf { it.scheme == "https" }?.host
        } catch (_: URISyntaxException) {
            null
        }
        return url.takeIf { host == NHTSA_HOST } ?: null.also { check.fail(path, "must be an https address on $NHTSA_HOST") }
    }

    /**
     * The bulletins for a vehicle's year, from the list for its make and model (as NHTSA or the owner spelled them) that
     * covers that year. A list named for the model wins over one that only has it as an alias (the RAV4's has "RAV4 Prime
     * (PHEV)" for 2026, when Toyota folded the plug-in into the RAV4). Null when no list covers the vehicle; an empty list
     * when one does with nothing for that year.
     */
    fun forVehicle(lists: List<BulletinList>, year: Int, make: String, model: String): VehicleBulletins? {
        val forYear = lists.filter { year in it.years && KnownLists.same(it.make, make) }
        val named = forYear.filter { KnownLists.same(it.model, model) }
        val matched = named.ifEmpty { forYear.filter { list -> list.modelAliases.any { KnownLists.same(it, model) } } }
        if (matched.isEmpty()) return null
        return VehicleBulletins(
            matched.maxOf { it.asOf },
            matched.flatMap { it.bulletins }.filter { b -> b.products.any { year in it.years } }.distinctBy { it.id },
        )
    }

    // Strict: "February 30" is no date, not March 1.
    private val written = DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
    private val numeric = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)

    /** "December 1, 2022" or "2/7/2025", month first, as Toyota writes them. */
    private const val DATE =
        """((?:January|February|March|April|May|June|July|August|September|October|November|December) \d{1,2}, \d{4}|\d{1,2}/\d{1,2}/\d{4})"""

    /** "expired December 1, 2022", "Expired on …", "has expired as of …". */
    private val expired = Regex("""\b(?i:expired)(?: (?i:on|as of))? $DATE""")

    /** "As of February 20, 2018, Special Service Campaign H0Y is no longer active", within the one sentence. */
    private val noLongerActive = Regex("""\b(?i:as of) $DATE,[^.]{0,80}? is no longer active""")

    /**
     * When the program ended, if [summary] says so with a date (#474): NHTSA has no field for it, so this is read from
     * the maker's own words, and only from a plain statement. A date that doesn't exist is no end.
     */
    fun endedOn(summary: String): LocalDate? {
        val date = (expired.find(summary) ?: noLongerActive.find(summary))?.groupValues?.get(1) ?: return null
        return try {
            LocalDate.parse(date, if ('/' in date) numeric else written)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** Model years as runs, for the app to word: 2016, 2017, 2018, 2020 are 2016..2018 and 2020..2020. */
    fun yearRuns(years: List<Int>): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        for (y in years.sorted().distinct()) {
            val last = runs.lastOrNull()
            if (last != null && y == last.last + 1) runs[runs.lastIndex] = last.first..y else runs += y..y
        }
        return runs
    }
}
