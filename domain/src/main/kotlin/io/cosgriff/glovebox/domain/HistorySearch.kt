package io.cosgriff.glovebox.domain

import java.time.LocalDate

/** What History's search reads of a record: its lines (as typed, and a catalog line's name as shown), notes and shop. */
data class SearchableRecord(val labels: List<String>, val notes: String?, val shop: String?)

object HistorySearch {
    /** What is searched for: [typed] without the spaces round it, or null when nothing is. */
    fun query(typed: String): String? = typed.trim().takeIf { it.isNotEmpty() }

    /** True when any line, the notes or the shop contains [typed], ignoring case. Everything matches an empty search. */
    fun matches(record: SearchableRecord, typed: String): Boolean {
        val q = query(typed) ?: return true
        return (record.labels + listOfNotNull(record.notes, record.shop)).any { it.contains(q, ignoreCase = true) }
    }
}

/** A shop as one earlier record named it. [order] breaks a tie between records of one day: the later saved is higher. */
data class ShopUse(val name: String, val date: LocalDate, val order: Long)

object Shops {
    const val MAX = 5

    /**
     * Up to [MAX] shops from [uses] (every vehicle's records) containing [typed], most recently used first.
     * Names that differ only in case or the spaces round them are one shop, offered as it was last typed.
     * The name already typed in full isn't offered back.
     */
    fun suggest(uses: List<ShopUse>, typed: String): List<String> {
        val q = typed.trim()
        return uses.filter { it.name.isNotBlank() }
            .groupBy { key(it.name) }
            .values
            .map { same -> same.maxWith(compareBy<ShopUse> { it.date }.thenBy { it.order }) }
            .sortedWith(compareByDescending<ShopUse> { it.date }.thenByDescending { it.order })
            .map { it.name.trim() }
            .filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
            .take(MAX)
    }

    /** A shop name as compared: case and the spaces round it ignored. */
    fun key(name: String) = Names.key(name)
}
