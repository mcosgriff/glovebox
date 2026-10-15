package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LicensePlatesTest {
    @Test
    fun `a plate is kept in capitals with only what a plate can hold`() {
        assertEquals("ABC 1234", LicensePlates.clean("  abc  1234 "))
        assertEquals("AB-12C", LicensePlates.clean("ab-12c!"))
        assertEquals("ABCDEFGHIJ", LicensePlates.clean("abcdefghijklm"))
        assertNull(LicensePlates.clean("  #  "))
    }

    @Test
    fun `every region has its own two-letter code`() {
        val codes = LicensePlates.regions.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertEquals(51 + 5 + 13, codes.size) // states and D.C., U.S. territories, Canada
        codes.forEach { assertEquals(it, 2, it.length) }
        assertEquals("Colorado", LicensePlates.region("CO")?.name)
        assertNull(LicensePlates.region("XX"))
    }

    @Test
    fun `the label says what is set`() {
        assertEquals("CO · ABC 1234", LicensePlates.label("ABC 1234", "CO"))
        assertEquals("ABC 1234", LicensePlates.label("ABC 1234", null))
        assertEquals("Colorado", LicensePlates.label(null, "CO"))
        assertNull(LicensePlates.label(null, null))
    }
}
