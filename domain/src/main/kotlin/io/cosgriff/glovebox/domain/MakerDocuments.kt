package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.net.URI
import java.net.URISyntaxException

/** What a maker's document is; `app` names each. */
enum class MakerDocumentKind { OWNERS_MANUAL, MAINTENANCE_GUIDE, WARRANTY, FLUID_CHART }

/**
 * One of a maker's own documents for a model and run of years, where the maker publishes it: the
 * owner's manual, the warranty and maintenance guide, a fluid capacity chart. Glovebox only links to
 * it; the owner's browser fetches it, and nothing about the vehicle is sent.
 */
data class MakerDocument(
    val kind: MakerDocumentKind,
    val years: IntRange,
    val url: String,
    /**
     * The document's own code ("OM35B48U"), or NHTSA's id for a copy filed there: what to quote to find
     * it again. Null only for a [vinLookup].
     */
    val code: String?,
    /**
     * Which of two documents for one year this is, in the maker's words, where the maker splits a
     * year ("through Oct. 2021 production"); passed through as it comes. Null where there is one.
     */
    val edition: String? = null,
    /**
     * The link is the maker's page that finds the document by VIN, not the document: Volkswagen's
     * digital manuals. Glovebox copies the VIN for the owner to paste there and never puts it in the link.
     */
    val vinLookup: Boolean = false,
) {
    /** A copy the maker filed with NHTSA, rather than one on the maker's own site. */
    val viaNhtsa: Boolean get() = URI(url).host == "static.nhtsa.gov"
}

data class MakerDocumentList(val make: String, val model: String, val documents: List<MakerDocument>)

sealed interface MakerDocumentFile {
    data class Ok(val list: MakerDocumentList) : MakerDocumentFile
    data class Invalid(val errors: List<ImportError>) : MakerDocumentFile
}

/**
 * Links to the makers' own documents for the models Glovebox has bundled lists for, so the owner can
 * open the manual or guide a list was read from. A file in `assets/maker-documents/` per model;
 * `docs/known-takes.md` says how each year's document was identified.
 */
object MakerDocuments {
    const val ASSET_DIR = "maker-documents"

    /**
     * Where a link may point: the makers' own publication servers and NHTSA's copies of what makers
     * file with it. A new maker's host is added here, which a test checks every file against.
     */
    val hosts = setOf("assets.sipb.toyota.com", "www.vw.com", "ownersliterature.vw.com", "static.nhtsa.gov")

    private val kinds = mapOf(
        "owners-manual" to MakerDocumentKind.OWNERS_MANUAL,
        "maintenance-guide" to MakerDocumentKind.MAINTENANCE_GUIDE,
        "warranty" to MakerDocumentKind.WARRANTY,
        "fluid-chart" to MakerDocumentKind.FLUID_CHART,
    )

    fun parse(text: String): MakerDocumentFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return MakerDocumentFile.Invalid(listOf(ImportError("", "This isn't a JSON file.")))
        }
        val check = ImportCheck()
        val keys = setOf("make", "model", "documents")
        val o = check.obj(root, "", keys, required = keys) ?: return MakerDocumentFile.Invalid(check.invalid().errors)
        val make = o["make"]?.let { check.string(it, "make", 40) }
        val model = o["model"]?.let { check.string(it, "model", 40) }
        val documents = o["documents"]?.let { d ->
            check.array(d, "documents", 1, 100)?.mapIndexedNotNull { i, e ->
                val path = ImportCheck.at("documents", i)
                check.obj(e, path, setOf("kind", "years", "url", "code", "edition", "vinLookup"), required = setOf("kind", "years", "url"))?.let { doc ->
                    val kind = doc["kind"]?.let { check.choice(it, ImportCheck.at(path, "kind"), kinds) }
                    val years = doc["years"]?.let { y ->
                        check.obj(y, ImportCheck.at(path, "years"), setOf("from", "to"), required = setOf("from", "to"))?.let { r ->
                            val from = r["from"]?.let { check.int(it, ImportCheck.at(path, "years.from"), 1900, 2100) }
                            val to = r["to"]?.let { check.int(it, ImportCheck.at(path, "years.to"), 1900, 2100) }
                            if (from != null && to != null && to < from) check.fail(ImportCheck.at(path, "years.to"), "is before years.from")
                            if (from != null && to != null && to >= from) from..to else null
                        }
                    }
                    val url = doc["url"]?.let { check.string(it, ImportCheck.at(path, "url"), 300) }?.also { u ->
                        val host = try {
                            URI(u).takeIf { it.scheme == "https" }?.host
                        } catch (_: URISyntaxException) {
                            null
                        }
                        if (host == null) check.fail(ImportCheck.at(path, "url"), "must be an https address")
                        else if (host !in hosts) check.fail(ImportCheck.at(path, "url"), "is on $host, which isn't a maker's or NHTSA's; add the host to MakerDocuments.hosts only if it is")
                    }
                    val code = doc["code"]?.let { check.string(it, ImportCheck.at(path, "code"), 40) }
                    val edition = doc["edition"]?.let { check.string(it, ImportCheck.at(path, "edition"), 60) }
                    val lookup = doc["vinLookup"]?.let { v ->
                        (v as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
                            ?: null.also { check.fail(ImportCheck.at(path, "vinLookup"), "must be true or false, without quotes") }
                    } == true
                    // A document is quoted by its code; a lookup page has none of its own.
                    if (lookup == ("code" in doc)) check.fail(ImportCheck.at(path, "code"), if (lookup) "isn't given for a VIN lookup page" else "is required")
                    if (kind != null && years != null && url != null && (lookup || code != null) && ("edition" !in doc || edition != null)) MakerDocument(kind, years, url, code, edition, lookup) else null
                }
            }
        }.orEmpty()
        // One document of each kind for a year, or several that each say which edition they are, so the owner can tell them apart.
        for (i in documents.indices) for (j in i + 1 until documents.size) {
            val a = documents[i]
            val b = documents[j]
            if (a.kind == b.kind && a.years.first <= b.years.last && b.years.first <= a.years.last && (a.edition == null || b.edition == null || a.edition == b.edition)) {
                check.fail("documents", "two ${a.kind.name.lowercase()} documents cover ${maxOf(a.years.first, b.years.first)}")
            }
        }
        if (check.failed || make == null || model == null) return MakerDocumentFile.Invalid(check.invalid().errors)
        return MakerDocumentFile.Ok(MakerDocumentList(make, model, documents))
    }

    /**
     * A vehicle's documents for its make, model and year, in [MakerDocumentKind] order, with the make and
     * model as the file spells them; null when there are none.
     */
    fun forVehicle(lists: List<MakerDocumentList>, year: Int, make: String, model: String): MakerDocumentList? =
        lists.firstOrNull { KnownLists.same(it.make, make) && KnownLists.same(it.model, model) }
            ?.let { list -> list.copy(documents = list.documents.filter { year in it.years }.sortedBy { it.kind }) }
            ?.takeIf { it.documents.isNotEmpty() }
}
