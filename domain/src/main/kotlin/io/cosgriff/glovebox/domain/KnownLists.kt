package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonObject

/** What every bundled list says about itself, whatever it holds: [KnownSpecs] and [KnownSchedules]. */
internal data class KnownHeader(val id: String, val title: String, val make: String, val model: String, val years: IntRange, val source: KnownSpecSource)

internal object KnownLists {
    val headerKeys = setOf("id", "title", "make", "model", "years", "source")

    /** Null when any part is missing or wrong, which [check] then says. */
    fun header(o: JsonObject, check: ImportCheck): KnownHeader? {
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
        if (id == null || title == null || make == null || model == null || years == null || source == null) return null
        return KnownHeader(id, title, make, model, years, source)
    }

    /** A make or model as NHTSA or the user spelled it, ignoring case and space. */
    fun same(a: String, b: String) = a.filterNot(Char::isWhitespace).equals(b.filterNot(Char::isWhitespace), ignoreCase = true)
}
