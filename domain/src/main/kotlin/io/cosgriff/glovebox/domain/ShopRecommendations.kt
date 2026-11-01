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

/**
 * What saving a record does to the recommendations (#593): [close] are the ones it now closes, [reopen] the ones it
 * had closed and no longer does (the owner unticked it, or the record no longer looks like the work).
 */
data class DoneChange(val close: List<Long> = emptyList(), val reopen: List<Long> = emptyList()) {
    val none: Boolean get() = close.isEmpty() && reopen.isEmpty()
}

/** An open recommendation a record being imported looks like the work for (#593): that record, by its place, and the line. */
data class ImportedDone(val recommendation: ShopRecommendation, val record: Int, val line: LoggedLine)

/** A record's lines as matching sees them: each line's catalog line and wording. */
fun ServiceRecord.loggedLines(): List<LoggedLine> = lines.map { LoggedLine(it.catalogId, it.label) }

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
        ordered(kept.filter { it.open && !it.date.isAfter(date) }).mapNotNull { r -> lineFor(r, lines)?.let { DoneMatch(r, it) } }

    /** The line of a record that did [recommendation]'s work, for saying how it was closed; null if none matches now. */
    fun doneLine(recommendation: ShopRecommendation, lines: List<LoggedLine>): LoggedLine? = lineFor(recommendation, lines)

    /**
     * What the form offers for record [recordId] dated [date] with [lines] (#593): on a new record (0) the open ones
     * it looks like the work for ([matches]); on an edit, those and the ones it closed already that it still matches,
     * ticked again. Never what the record kept itself.
     */
    fun offered(kept: List<ShopRecommendation>, recordId: Long, date: LocalDate, lines: List<LoggedLine>): List<DoneMatch> =
        matches(kept.filter { it.recordId != recordId }.map { if (recordId != 0L && it.doneBy == recordId) it.copy(doneBy = null, doneOn = null) else it }, date, lines)

    /**
     * Saving record [recordId] with what [offered] gives, less the [unticked] (#593): the ticked ones it didn't close
     * before are closed; what it closed before and is now unticked, or no longer looks like the work for (its lines or
     * date changed), opens again. One closed some other way too stays closed that way.
     */
    fun onSave(kept: List<ShopRecommendation>, recordId: Long, date: LocalDate, lines: List<LoggedLine>, unticked: Set<Long>): DoneChange {
        val offered = offered(kept, recordId, date, lines).map { it.recommendation.id }.toSet()
        val ticked = offered - unticked
        val was = if (recordId == 0L) emptyList() else kept.filter { it.doneBy == recordId }
        val stays = { r: ShopRecommendation -> r.id in ticked || (r.id !in offered && stillMatches(r, date, lines)) }
        return DoneChange(close = (ticked - was.map { it.id }.toSet()).toList(), reopen = was.filterNot(stays).map { it.id })
    }

    /**
     * The open recommendations [records] being imported look like the work for (#593), each offered for the earliest
     * record dated on or after it whose lines match; never for the record that kept it. **Only offered**: the import
     * preview asks and the owner decides.
     */
    fun offeredOnImport(kept: List<ShopRecommendation>, records: List<ServiceRecord>): List<ImportedDone> {
        val byDate = records.indices.sortedBy { records[it].date }
        return ordered(kept.filter { it.open }).mapNotNull { r ->
            byDate.firstNotNullOfOrNull { i ->
                val record = records[i]
                if (record.id == r.recordId || record.date.isBefore(r.date)) null else lineFor(r, record.loggedLines())?.let { ImportedDone(r, i, it) }
            }
        }
    }

    /**
     * The recommendations saved records that an import replaces had closed, and no longer look like the work for once
     * replaced (#593): they open again, as on an edit.
     */
    fun reopenedOnImport(kept: List<ShopRecommendation>, replaced: List<ServiceRecord>): List<Long> = replaced.flatMap { record ->
        kept.filter { it.doneBy == record.id && !stillMatches(it, record.date, record.loggedLines()) }.map { it.id }
    }

    /** One kept under a line by a line of the record with that line; one with no line by a line worded like it. */
    private fun lineFor(r: ShopRecommendation, lines: List<LoggedLine>): LoggedLine? = if (r.line != null) {
        lines.firstOrNull { it.catalogId == r.line }
    } else {
        lines.firstOrNull { LineGuess.similar(r.item.text, it.label) }
    }

    private fun stillMatches(r: ShopRecommendation, date: LocalDate, lines: List<LoggedLine>): Boolean = !r.date.isAfter(date) && lineFor(r, lines) != null

    /** Red first, then yellow, then the rest; oldest first in each. */
    fun ordered(kept: List<ShopRecommendation>): List<ShopRecommendation> =
        kept.sortedWith(compareBy<ShopRecommendation>({ it.item.colour?.ordinal ?: FindingColour.entries.size }, { it.date }, { it.id }))
}
