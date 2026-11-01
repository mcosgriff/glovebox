package io.cosgriff.glovebox.domain

import java.time.LocalDate

/**
 * Work a shop recommended, or found, that the owner kept for later (#572). It waits on the vehicle until the work is
 * logged or the owner closes it, and **never moves a reminder or the schedule**: closing it logs nothing, and an open
 * one makes nothing overdue.
 *
 * Its shop and date are the record it was kept from ([recordId]): an edit to that record moves them, and deleting it
 * takes this with it. [line] is the catalog line whose logging offers to close it; null for one that no line fits,
 * which is matched on its wording. It is closed by the owner ([closedOn]), by a later record of the work ([doneBy],
 * dated [doneOn]), or by a newer recommendation of the same work from the same shop ([replacedBy]).
 */
data class ShopRecommendation(
    val id: Long,
    val recordId: Long,
    val date: LocalDate,
    val shop: String?,
    val item: RecommendedItem,
    val line: String?,
    val closedOn: LocalDate? = null,
    val doneBy: Long? = null,
    val doneOn: LocalDate? = null,
    val replacedBy: Long? = null,
) {
    val open: Boolean get() = closedOn == null && doneBy == null && replacedBy == null
}

/** The catalog line a kept item comes off with: the user's rule's, or [LineGuess]'s ([guessed]), which they can change. */
data class KeptLine(val catalogId: String, val guessed: Boolean)

/**
 * One item read from an invoice as the form holds it (#572): kept for later, put in the notes, both, or neither (let
 * go, and nothing is saved for it). [line] is what it comes off with when kept; null when no line fits.
 */
data class RecommendedChoice(
    val item: RecommendedItem,
    val keep: Boolean,
    val note: Boolean,
    val line: KeptLine?,
) {
    val letGo: Boolean get() = !keep && !note
}

/** A line of a record about to be saved, as matching sees it: its catalog line, if it has one, and its wording. */
data class LoggedLine(val catalogId: String?, val label: String)

/** An open recommendation the record being logged looks like the work for, and the line it matched. */
data class DoneMatch(val recommendation: ShopRecommendation, val line: LoggedLine)

/** What vehicle home's row says of the open recommendations (#572): how many, and how many are red and yellow. */
data class OpenRecommendations(val count: Int = 0, val red: Int = 0, val amber: Int = 0)

object ShopRecommendations {
    fun summary(kept: List<ShopRecommendation>): OpenRecommendations {
        val open = kept.filter { it.open }
        return OpenRecommendations(open.size, open.count { it.item.colour == FindingColour.RED }, open.count { it.item.colour == FindingColour.AMBER })
    }

    /** Wording or a shop's name as compared: case and spacing aside. */
    fun key(text: String): String = text.trim().lowercase().split(Regex("\\s+")).joinToString(" ")

    /** The same shop, as the record names it (a saved shop's spelling, #484); no shop is never the same as another. */
    fun sameShop(a: String?, b: String?): Boolean = !a.isNullOrBlank() && !b.isNullOrBlank() && key(a) == key(b)

    /**
     * The choices an invoice's [items] start with: every one kept, under its rule's line or [LineGuess]'s guess, save a
     * finding with no colour standing alone (a report without marks, whose items may well be fine), which starts let go.
     */
    fun choices(items: List<RecommendedItem>, rules: List<CatalogRule>): List<RecommendedChoice> = items.map { item ->
        val alone = item.cents == null && item.finding == null && item.area != null
        RecommendedChoice(item, keep = !(alone && item.colour == null), note = false, line = lineFor(item, rules))
    }

    /** The line [item] comes off with: the user's rule for its wording first, otherwise a guess, offered. */
    fun lineFor(item: RecommendedItem, rules: List<CatalogRule>): KeptLine? {
        val texts = listOfNotNull(item.text, item.area?.let { a -> "$a ${item.finding ?: item.text}" })
        texts.firstNotNullOfOrNull { CatalogRules.match(it, rules) }?.let { return KeptLine(it, guessed = false) }
        return texts.firstNotNullOfOrNull { LineGuess.guess(it) }?.let { KeptLine(it, guessed = true) }
    }

    /**
     * The open recommendation a new one from [shop] for [item] replaces: the same shop and the same wording, case and
     * spacing aside. A different shop never replaces another's.
     */
    fun replaced(shop: String?, item: RecommendedItem, kept: List<ShopRecommendation>): ShopRecommendation? =
        kept.firstOrNull { it.open && sameShop(it.shop, shop) && key(it.item.text) == key(item.text) }

    /**
     * The open recommendations a record dated [date] with [lines] looks like the work for, whoever did it: one kept
     * under a line by a line of the record with that line; one with no line by a line worded like it
     * ([LineGuess.similar]). Only recommendations made on or before [date]. **Only ever asked**: the form offers these
     * and the owner decides.
     */
    fun matches(kept: List<ShopRecommendation>, date: LocalDate, lines: List<LoggedLine>): List<DoneMatch> =
        ordered(kept.filter { it.open && !it.date.isAfter(date) }).mapNotNull { r ->
            val line = if (r.line != null) {
                lines.firstOrNull { it.catalogId == r.line }
            } else {
                lines.firstOrNull { LineGuess.similar(r.item.text, it.label) }
            }
            line?.let { DoneMatch(r, it) }
        }

    /** The line of a record that did [recommendation]'s work, for saying how it was closed; null if none matches now. */
    fun doneLine(recommendation: ShopRecommendation, lines: List<LoggedLine>): LoggedLine? =
        matches(listOf(recommendation.copy(doneBy = null, doneOn = null)), LocalDate.MAX, lines).firstOrNull()?.line

    /** Red first, then yellow, then the rest; oldest first in each. */
    fun ordered(kept: List<ShopRecommendation>): List<ShopRecommendation> =
        kept.sortedWith(compareBy<ShopRecommendation>({ it.item.colour?.ordinal ?: FindingColour.entries.size }, { it.date }, { it.id }))
}
