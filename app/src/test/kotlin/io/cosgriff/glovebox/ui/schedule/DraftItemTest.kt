package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.domain.Interval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a schedule row shows is the interval its reminder counts by (#616). */
class DraftItemTest {
    private val item = DraftItem(key = "1", name = "Oil", normal = Interval(10_000, 12), severe = Interval(5_000, null))

    @Test
    fun `a severe interval with miles only counts the normal months, figure by figure`() {
        assertEquals(Interval(10_000, 12), item.active)
        val severe = item.copy(useSevere = true)
        assertEquals(Interval(5_000, 12), severe.active)
        assertTrue(severe.showsSevere)
    }

    @Test
    fun `a severe interval that changes no figure in force is not shown as severe`() {
        val same = item.copy(severe = Interval(10_000, null), useSevere = true)
        assertEquals(Interval(10_000, 12), same.active)
        assertFalse(same.showsSevere)
    }

    @Test
    fun `the E85 interval is the whole one, over severe use`() {
        val e85 = item.copy(useSevere = true, e85 = Interval(3_000, null), useE85 = true)
        assertEquals(Interval(3_000, null), e85.active)
        assertTrue(e85.showsE85)
        assertFalse(e85.showsSevere)
    }
}
