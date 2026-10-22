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
/** Custom lines to sort, and the wordings they come in: what History says above the sort page's rows. */
data class CustomCount(val lines: Int, val wordings: Int)

object CustomLines {
    fun isCustom(line: SavedLine): Boolean = line.catalogId == null && line.scheduleItemId == null && !line.recall

    /**
     * How many Custom lines [groups] would sort, and in how many wordings: "21 lines, written 5 ways". The sort page
     * shows one row per wording, so History says both or the two read as if they disagree.
     */
    fun count(lines: List<SavedLine>): CustomCount {
        val sortable = lines.filter(::isCustom).filter { it.label.isNotBlank() }
        return CustomCount(sortable.size, sortable.map { CatalogRules.key(it.label) }.distinct().size)
    }

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

    /**
     * The line [group] goes under: what the user chose for it in [chosen] (by key; a null there is "leave it
     * under Custom"), or, if they haven't, its suggestion, which starts ticked since it comes from plain wording
     * or their own rule.
     */
    fun picked(group: CustomGroup, chosen: Map<String, String?>): String? =
        if (group.key in chosen) chosen[group.key] else group.suggestion?.catalogId

    /** Whether to offer "Remember this": not with nothing picked, nor when the pick is already the user's rule. */
    fun asksToRemember(group: CustomGroup, picked: String?): Boolean =
        picked != null && !(group.suggestion?.source == SuggestionSource.YOUR_RULE && group.suggestion.catalogId == picked)

    /** Each line of [groups] to be filed, with the line picked for its group ([picked]). */
    fun assignments(groups: List<CustomGroup>, chosen: Map<String, String?>): List<Pair<Long, String>> =
        groups.flatMap { group -> picked(group, chosen)?.let { id -> group.lineIds.map { it to id } }.orEmpty() }

    /**
     * The jobs of the user's own milestone table ([jobs]) the picks would make these lines count for, where
     * their wording alone ([MilestoneImport.doneKeys] with no catalog line) didn't already. Generated milestones
     * follow the tracked items, so the reminders that move say that for them.
     */
    fun tableJobs(groups: List<CustomGroup>, chosen: Map<String, String?>, jobs: List<TableJob>): List<TableJob> {
        val newKeys = groups.flatMap { g ->
            picked(g, chosen)?.let { MilestoneImport.doneKeys(g.label, it) - MilestoneImport.doneKeys(g.label, null).toSet() }.orEmpty()
        }.toSet()
        return jobs.filter { MilestoneImport.jobKey(it) in newKeys }.distinctBy { MilestoneImport.jobKey(it) }
    }

    /**
     * The rules to keep: a group's wording as last written and its pick, where "Remember this" is asked and
     * not unticked in [remember] (it starts ticked: this is ongoing work, and the next import should know).
     */
    fun rulesToKeep(groups: List<CustomGroup>, chosen: Map<String, String?>, remember: Map<String, Boolean>): List<CatalogRule> =
        groups.mapNotNull { g ->
            picked(g, chosen)?.takeIf { asksToRemember(g, it) && remember[g.key] != false }?.let { CatalogRule(g.label, it) }
        }
}
