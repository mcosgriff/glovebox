package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class VisitsTest {
    private val day = LocalDate.of(2026, 5, 4)

    private fun rec(id: Long, visit: Long? = null, date: LocalDate = day, odometer: Int = 60_000, shop: String? = null) =
        VisitRecord(id, visit, date, odometer, shop)

    @Test
    fun `a visit's records are shown together, where its first one comes`() {
        val records = listOf(rec(1, visit = 7), rec(2), rec(3, visit = 7), rec(4, visit = 9), rec(5))
        // Visit 9 has one record left, so it is no visit.
        assertEquals(listOf(listOf(1L, 3L), listOf(2L), listOf(4L), listOf(5L)), Visits.grouped(records) { it.visitId }.map { g -> g.map { it.id } })
    }

    @Test
    fun `the other records in a visit, and none for a record alone in one`() {
        val all = listOf(rec(1, visit = 7), rec(2, visit = 7), rec(3, visit = 7), rec(4, visit = 9), rec(5))
        assertEquals(listOf(1L, 3L), Visits.others(all[1], all).map { it.id })
        assertEquals(emptyList<VisitRecord>(), Visits.others(all[3], all))
        assertEquals(emptyList<VisitRecord>(), Visits.others(all[4], all))
    }

    @Test
    fun `same visit as lists the same day first, then newest, and never what is already in it`() {
        val record = rec(1, visit = 7)
        val all = listOf(
            record,
            rec(2, visit = 7),
            rec(3, date = day.minusDays(30)),
            rec(4, date = day, odometer = 61_000),
            rec(5, date = day.plusDays(10)),
        )
        assertEquals(listOf(4L, 5L, 3L), Visits.choices(record, all).map { it.id })
    }

    @Test
    fun `a link is only offered for the same day and the same reading or shop`() {
        val record = rec(1, odometer = 60_000, shop = "Lex Brodie's")
        val all = listOf(
            record,
            rec(2, odometer = 60_000),
            rec(3, odometer = 60_012, shop = "  lex brodie's "),
            rec(4, odometer = 60_012, shop = "Other shop"),
            rec(5, odometer = 60_012),
            rec(6, date = day.minusDays(1), odometer = 60_000, shop = "Lex Brodie's"),
        )
        assertEquals(setOf(2L, 3L), Visits.offers(record, all).map { it.id }.toSet())
        // Two records with no shop don't share one.
        assertEquals(emptyList<VisitRecord>(), Visits.offers(rec(10, odometer = 1), listOf(rec(10, odometer = 1), rec(11, odometer = 2))))
    }

    @Test
    fun `nothing already in the visit is offered again`() {
        val record = rec(1, visit = 7)
        val all = listOf(record, rec(2, visit = 7), rec(3))
        assertEquals(listOf(3L), Visits.offers(record, all).map { it.id })
    }

    @Test
    fun `linking into a visit joins all of it`() {
        val all = listOf(rec(1, visit = 7), rec(2, visit = 7), rec(3), rec(4, visit = 9), rec(5, visit = 9), rec(6))
        assertEquals(setOf(3L, 6L), Visits.joined(all[2], all[5], all))
        assertEquals(setOf(1L, 2L, 3L), Visits.joined(all[2], all[0], all))
        assertEquals(setOf(1L, 2L, 4L, 5L), Visits.joined(all[0], all[3], all))
    }

    @Test
    fun `unlinking leaves the rest linked, and a visit of one is no visit`() {
        val three = listOf(rec(1, visit = 7), rec(2, visit = 7), rec(3, visit = 7))
        assertEquals(setOf(2L), Visits.unlinked(three[1], three))
        val two = listOf(rec(1, visit = 7), rec(2, visit = 7))
        assertEquals(setOf(1L, 2L), Visits.unlinked(two[0], two))
    }

    @Test
    fun `deleting a record from a visit leaves the others linked`() {
        val after = listOf(rec(1, visit = 7), rec(3, visit = 7), rec(4))
        assertEquals(listOf(listOf(1L, 3L), listOf(4L)), Visits.grouped(after) { it.visitId }.map { g -> g.map { it.id } })
        // The last two: one left is no visit.
        val last = listOf(rec(1, visit = 7), rec(4))
        assertEquals(listOf(listOf(1L), listOf(4L)), Visits.grouped(last) { it.visitId }.map { g -> g.map { it.id } })
        assertEquals(emptyList<VisitRecord>(), Visits.others(last[0], last))
    }

    @Test
    fun `a visit's cost is its records' own added up, and readings that disagree are both kept`() {
        assertEquals(13_850L, Visits.total(listOf(4_850L, null, 9_000L)))
        assertNull(Visits.total(listOf(null, null)))
        assertEquals(listOf(60_000, 60_012), Visits.readings(listOf(60_012, 60_000, 60_012)))
    }
}
