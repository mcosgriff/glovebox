package io.cosgriff.glovebox.domain

import java.time.LocalDate

/**
 * A saved line of work as sorting Custom lines needs it. [recall] is a line on the record a recall marked
 * done added, which has its home there and is never sorted.
 */
data class SavedLine(
    val id: Long,
    val recordId: Long,
    val date: LocalDate,
    val label: String,
    val catalogId: String?,
    val scheduleItemId: Long?,
    val recall: Boolean = false,
)

/** Where a suggested line comes from, which the sort screen says. */
enum class SuggestionSource {
    /** One of the user's own rules ([CatalogRules]). */
    YOUR_RULE,

    /** Wording Glovebox recognises (`CarfaxReport.service`). */
    GLOVEBOX,
}

data class LineSuggestion(val catalogId: String, val source: SuggestionSource)

/**
 * Custom lines written the same way ([CatalogRules.key]), sorted together. [label] is the text as last
 * written; [dates] are the records', oldest first.
 */
data class CustomGroup(
    val key: String,
    val label: String,
    val lineIds: List<Long>,
    val recordIds: Set<Long>,
    val dates: List<LocalDate>,
    val suggestion: LineSuggestion?,
) {
    val count: Int get() = lineIds.size
}

/**
 * Lines filed under Custom, sorted in bulk (#310). Only a line with no home is one: not a catalog line, not
 * one of the user's own schedule items, not a recall's. Nothing here changes a line; a group's catalog line
 * is only ever the user's pick, and its label stays as written.
 */
object CustomLines {
    fun isCustom(line: SavedLine): Boolean = line.catalogId == null && line.scheduleItemId == null && !line.recall

    fun count(lines: List<SavedLine>): Int = lines.count(::isCustom)

    /** [lines]' Custom ones by text, most lines first, then by text; each with what it looks like. */
    fun groups(lines: List<SavedLine>, rules: List<CatalogRule>): List<CustomGroup> =
        lines.filter(::isCustom)
            .filter { it.label.isNotBlank() }
            .groupBy { CatalogRules.key(it.label) }
            .map { (key, same) ->
                val label = same.maxWith(compareBy<SavedLine> { it.date }.thenBy { it.id }).label.trim()
                CustomGroup(
                    key = key,
                    label = label,
                    lineIds = same.map { it.id },
                    recordIds = same.map { it.recordId }.toSet(),
                    dates = same.map { it.date }.distinct().sorted(),
                    suggestion = suggest(label, rules),
                )
            }
            .sortedWith(compareByDescending<CustomGroup> { it.count }.thenBy { it.key })

    /**
     * The line [label] looks like: the user's rule first, since they chose it, then the wording Glovebox
     * recognises. Every time Glovebox learns more wording, groups it recognises are offered here.
     */
    fun suggest(label: String, rules: List<CatalogRule>): LineSuggestion? =
        CatalogRules.match(label, rules)?.let { LineSuggestion(it, SuggestionSource.YOUR_RULE) }
            ?: CarfaxReport.service(label)?.let { LineSuggestion(it, SuggestionSource.GLOVEBOX) }

    /** Each line of [groups] to be filed, with the catalog line picked for its group by key in [picks]. */
    fun assignments(groups: List<CustomGroup>, picks: Map<String, String>): List<Pair<Long, String>> =
        groups.flatMap { group -> picks[group.key]?.let { id -> group.lineIds.map { it to id } }.orEmpty() }
}
