package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Shop-recommended work kept for later (#572). Every shop, item and price here is made up. */
class ShopRecommendationsTest {
    private val mar5 = LocalDate.of(2026, 3, 5)
    private val oct29 = LocalDate.of(2026, 10, 29)
    private val cedar = "Cedar Hollow Auto & Truck"
    private val pine = "Pine Ridge Tire & Lube"

    private fun kept(
        id: Long,
        text: String,
        line: String?,
        date: LocalDate = mar5,
        shop: String? = cedar,
        colour: FindingColour? = null,
    ) = ShopRecommendation(id, recordId = 100 + id, date = date, shop = shop, item = RecommendedItem(text, 22_999, colour = colour), line = line)

    @Test
    fun `every item starts kept under its rule's line, or a guess, and a colourless finding alone starts let go`() {
        val rules = listOf(CatalogRule("Coolant System Service", "coolant"))
        val c = ShopRecommendations.choices(
            listOf(
                RecommendedItem("Coolant System Service", 23_999),
                RecommendedItem("Battery Replacement", 28_900),
                RecommendedItem("Oil pan seeping", null, area = "Fluid Leaks", colour = FindingColour.AMBER),
                RecommendedItem("Front 10/32, rear 9/32", null, area = "Wheels/Tires"),
            ),
            rules,
        )
        assertEquals(KeptLine("coolant", guessed = false), c[0].line)
        assertEquals(KeptLine("battery", guessed = true), c[1].line)
        assertEquals(listOf(true, true, true, false), c.map { it.keep })
        assertTrue(c.none { it.note })
        assertTrue(c[3].letGo)
    }

    @Test
    fun `the same shop and the same wording replace the open one, case and spacing aside, and another shop never does`() {
        val open = listOf(kept(1, "Coolant  System Service", "coolant"))
        val again = RecommendedItem("coolant system service", 23_999)
        assertEquals(1L, ShopRecommendations.replaced("cedar hollow auto & truck", again, open)?.id)
        assertNull(ShopRecommendations.replaced(pine, again, open))
        assertNull(ShopRecommendations.replaced(null, again, open))
        assertNull(ShopRecommendations.replaced(cedar, RecommendedItem("Coolant Flush", null), open))
        assertNull(ShopRecommendations.replaced(cedar, again, listOf(open[0].copy(closedOn = oct29))))
    }

    @Test
    fun `logged work matches by line whoever did it, and by wording only where no line fits`() {
        val open = listOf(
            kept(1, "Coolant System Service", "coolant"),
            kept(2, "Electrical Concern Testing and Analysis", null),
            kept(3, "Battery Replacement", "battery", date = oct29),
        )
        val diy = listOf(LoggedLine("coolant", "Coolant flushed"), LoggedLine(null, "Electrical concern analysis done"))
        val m = ShopRecommendations.matches(open, LocalDate.of(2026, 6, 1), diy)
        assertEquals(listOf(1L, 2L), m.map { it.recommendation.id })
        assertEquals("Coolant flushed", m[0].line.label)
        // A line with the wording but no catalog line doesn't close one kept under a line.
        assertTrue(ShopRecommendations.matches(open.take(1), oct29, listOf(LoggedLine(null, "Coolant System Service"))).isEmpty())
    }

    @Test
    fun `only work dated on or after the recommendation matches, and never a closed one`() {
        val open = listOf(kept(3, "Battery Replacement", "battery", date = oct29))
        val battery = listOf(LoggedLine("battery", "Battery"))
        assertTrue(ShopRecommendations.matches(open, oct29.minusDays(1), battery).isEmpty())
        assertEquals(1, ShopRecommendations.matches(open, oct29, battery).size)
        assertTrue(ShopRecommendations.matches(listOf(open[0].copy(replacedBy = 9)), oct29, battery).isEmpty())
    }

    @Test
    fun `similar wording is most of what the recommendation names, typos forgiven`() {
        assertTrue(LineGuess.similar("Electrical Concern Testing and Analysis", "electrical concern analysis"))
        assertTrue(LineGuess.similar("Wiper Blade Replacement", "Replaced wiper blades"))
        assertTrue(LineGuess.similar("Wiper Blade Replacement", "wiper blads"))
        assertFalse(LineGuess.similar("Electrical Concern Testing and Analysis", "Oil change"))
        assertFalse(LineGuess.similar("Replacement", "Oil change"))
    }

    @Test
    fun `red first, then yellow, then the rest, oldest first in each`() {
        val list = listOf(
            kept(1, "Transmission Fluid Exchange", null, date = oct29),
            kept(2, "Power Steering Fluid Service", null, date = mar5),
            kept(3, "Oil pan seeping", null, colour = FindingColour.AMBER),
            kept(4, "Replace CV Axle Assembly", null, colour = FindingColour.RED, date = oct29),
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), ShopRecommendations.ordered(list).map { it.id })
    }
}
