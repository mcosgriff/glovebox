package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentReadsTest {
    @Test
    fun `keeps the last few reads, oldest first, and forgets them on clear`() {
        val reads = RecentReads<Int?>(3)
        reads.add(1)
        reads.add(null)
        reads.add(3)
        assertEquals(listOf(null, 3, 4), reads.add(4))
        reads.clear()
        assertEquals(listOf<Int?>(5), reads.add(5))
    }
}
