package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistorySearchTest {
    private val belt = SearchableRecord(listOf("Serpentine belt", "Drive belt"), notes = "Squeal on cold starts", shop = "Main Street Auto")
    private val oil = SearchableRecord(listOf("Oil change"), notes = null, shop = null)

    @Test
    fun `finds a line, a note and a shop, ignoring case and the spaces round it`() {
        assertTrue(HistorySearch.matches(belt, "serpentine"))
        assertTrue(HistorySearch.matches(belt, "  DRIVE BELT "))
        assertTrue(HistorySearch.matches(belt, "squeal"))
        assertTrue(HistorySearch.matches(belt, "main street"))
        assertFalse(HistorySearch.matches(oil, "belt"))
    }

    @Test
    fun `an empty search matches everything`() {
        assertTrue(HistorySearch.matches(oil, ""))
        assertTrue(HistorySearch.matches(oil, "   "))
        assertEquals(null, HistorySearch.query("  "))
        assertEquals("belt", HistorySearch.query(" belt "))
    }

    @Test
    fun `nothing missing is matched`() {
        assertFalse(HistorySearch.matches(oil, "null"))
    }

    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun `shops most recently used first, at most five`() {
        val uses = listOf("A", "B", "C", "D", "E", "F").mapIndexed { i, n -> ShopUse(n, today.minusDays(i.toLong()), i.toLong()) }
        assertEquals(listOf("A", "B", "C", "D", "E"), Shops.suggest(uses, ""))
    }

    @Test
    fun `one shop however it was typed, offered as last typed`() {
        val uses = listOf(
            ShopUse("main street auto", today.minusYears(2), 1),
            ShopUse(" Main Street Auto ", today.minusMonths(1), 5),
            ShopUse("Quick Lube", today.minusMonths(3), 2),
        )
        assertEquals(listOf("Main Street Auto", "Quick Lube"), Shops.suggest(uses, ""))
        // On one day, the later saved wins.
        val sameDay = listOf(ShopUse("MAIN STREET AUTO", today, 9), ShopUse("Main Street Auto", today, 3))
        assertEquals(listOf("MAIN STREET AUTO"), Shops.suggest(sameDay, ""))
    }

    @Test
    fun `narrowed by what is typed, and what is typed in full isn't offered back`() {
        val uses = listOf(ShopUse("Main Street Auto", today, 1), ShopUse("Quick Lube", today.minusDays(1), 2), ShopUse("Main Tire", today.minusDays(2), 3))
        assertEquals(listOf("Main Street Auto", "Main Tire"), Shops.suggest(uses, " main"))
        assertEquals(listOf("Main Street Auto"), Shops.suggest(uses, "street"))
        assertEquals(emptyList<String>(), Shops.suggest(uses, "quick lube"))
        assertEquals(emptyList<String>(), Shops.suggest(listOf(ShopUse("  ", today, 1)), ""))
    }
}
