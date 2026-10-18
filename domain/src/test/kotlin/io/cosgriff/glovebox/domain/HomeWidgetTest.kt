package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeWidgetTest {
    @Test
    fun `up to five vehicles are shown, in the order given`() {
        val rows = HomeWidget.rows(listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), rows.shown)
        assertEquals(0, rows.more)

        val five = HomeWidget.rows((1..5).toList())
        assertEquals((1..5).toList(), five.shown)
        assertEquals(0, five.more)
    }

    @Test
    fun `vehicles past five are counted, not shown`() {
        val rows = HomeWidget.rows((1..8).toList())
        assertEquals((1..5).toList(), rows.shown)
        assertEquals(3, rows.more)
    }

    @Test
    fun `an empty garage has no rows`() {
        val rows = HomeWidget.rows(emptyList<Int>())
        assertEquals(emptyList<Int>(), rows.shown)
        assertEquals(0, rows.more)
    }

    @Test
    fun `two of a model are told apart by year and make, without the trim`() {
        assertEquals(HomeWidget.Name(null, "2011 Honda Accord"), HomeWidget.name(null, 2011, "HONDA", "Accord"))
        assertEquals(HomeWidget.Name(null, "2026 Honda Accord"), HomeWidget.name(null, 2026, "HONDA", "Accord"))
    }

    @Test
    fun `a nickname comes with the vehicle's title, never in place of it`() {
        assertEquals(HomeWidget.Name("Big Red", "2008 Ford F-150"), HomeWidget.name("Big Red", 2008, "FORD", "F-150"))
    }

    @Test
    fun `a blank nickname is no nickname`() {
        assertNull(HomeWidget.name("  ", 2008, "FORD", "F-150").nickname)
    }

    @Test
    fun `a vehicle the decode knew nothing about has no title`() {
        assertNull(HomeWidget.name(null, 0, "Unknown", "").title)
    }
}
