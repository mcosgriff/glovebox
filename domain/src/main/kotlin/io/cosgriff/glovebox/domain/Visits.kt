package io.cosgriff.glovebox.domain

import java.time.LocalDate

/**
 * A record as a visit sees it. [visitId] is shared by the records linked into one visit, null for one
 * that isn't; a visit id held by a single record means nothing (the others were unlinked or deleted).
 */
data class VisitRecord(val id: Long, val visitId: Long?, val date: LocalDate, val odometer: Int, val shop: String?)

/**
 * Records that were one visit to a shop (#307): an imported Carfax entry and the recall fixed on the same
 * day, say. **A visit only groups records**: each keeps its own lines, flags, photos, recall link and cost,
 * and nothing about reminders or spending changes. Nothing is ever linked for the user; [offers] only
 * suggests, and the user links.
 */
object Visits {
    /**
     * [records] with each visit's records together, in the order given: a visit takes the place of its
     * first record there, and the rest of it follow. A record in no visit, or alone in one, is a group of one.
     */
    fun <T> grouped(records: List<T>, visitId: (T) -> Long?): List<List<T>> {
        val visits = records.filter { visitId(it) != null }.groupBy(visitId).filterValues { it.size > 1 }
        val placed = mutableSetOf<Long>()
        return records.mapNotNull { r ->
            val visit = visitId(r)?.let(visits::get) ?: return@mapNotNull listOf(r)
            if (placed.add(visitId(r)!!)) visit else null
        }
    }

    /** The other records in [record]'s visit, in the order of [all]; empty when it is in none. */
    fun others(record: VisitRecord, all: List<VisitRecord>): List<VisitRecord> {
        val visit = record.visitId ?: return emptyList()
        val members = all.filter { it.visitId == visit }
        return if (members.size > 1) members.filter { it.id != record.id } else emptyList()
    }

    /**
     * What "Same visit as…" lists for [record]: the vehicle's other records not already in its visit, those
     * from the same day first, then newest first. [all] is that vehicle's records.
     */
    fun choices(record: VisitRecord, all: List<VisitRecord>): List<VisitRecord> {
        val already = others(record, all).map { it.id }.toSet() + record.id
        return all.filter { it.id !in already }
            .sortedWith(compareByDescending<VisitRecord> { it.date == record.date }.thenByDescending { it.date }.thenByDescending { it.odometer }.thenBy { it.id })
    }

    /**
     * The records the record screen may offer to link [record] with: the same day, and the same reading or
     * the same shop (ignoring case and the spaces round it), not already in its visit. Only ever an offer.
     */
    fun offers(record: VisitRecord, all: List<VisitRecord>): List<VisitRecord> =
        choices(record, all).filter { it.date == record.date && (it.odometer == record.odometer || sameShop(it.shop, record.shop)) }

    /**
     * The records that make one visit once [a] and [b] are linked: both, and whatever visit each was
     * already in, so linking into a visit joins all of it. They are given one new visit id.
     */
    fun joined(a: VisitRecord, b: VisitRecord, all: List<VisitRecord>): Set<Long> =
        (listOf(a, b) + others(a, all) + others(b, all)).map { it.id }.toSet()

    /**
     * The records whose visit id is cleared when [record] is unlinked: it, and the one other record when
     * only one would be left, since a visit of one record is no visit.
     */
    fun unlinked(record: VisitRecord, all: List<VisitRecord>): Set<Long> {
        val rest = others(record, all)
        return setOf(record.id) + if (rest.size == 1) setOf(rest.single().id) else emptySet()
    }

    /**
     * What marking a recall done offers to add its record to: the vehicle's records on [date], a visit's
     * records together, in the order given. Each group is one choice, and none is chosen for the user.
     */
    fun onDay(all: List<VisitRecord>, date: LocalDate): List<List<VisitRecord>> =
        grouped(all.filter { it.date == date }) { it.visitId }

    /** The readings a visit's records give, each once, lowest first: more than one is a disagreement to show, never to settle. */
    fun readings(odometers: List<Int>): List<Int> = odometers.distinct().sorted()

    /** The visit's cost: each record's own, added up; null when none has one. The same sum [Spending] makes, so nothing is counted twice. */
    fun total(costs: List<Long?>): Long? = Spending.total(costs)

    private fun sameShop(a: String?, b: String?): Boolean =
        a != null && b != null && a.isNotBlank() && Shops.key(a) == Shops.key(b)
}
