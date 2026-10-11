package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehiclePickTest {
    private val makes = listOf("ACURA", "FORD", "HONDA", "LAND ROVER", "ROVER")

    @Test
    fun `years run from next model year back to 1900`() {
        val years = VehiclePick.years(2026)
        assertEquals(2027, years.first())
        assertEquals(1900, years.last())
    }

    @Test
    fun `typed years older than a VIN allows are fine here`() {
        assertEquals(1967, VehiclePick.parseYear(" 1967 ", 2026))
        assertNull(VehiclePick.parseYear("1899", 2026))
        assertNull(VehiclePick.parseYear("2028", 2026))
        assertNull(VehiclePick.parseYear("67", 2026))
    }

    @Test
    fun `filter puts names that start with the text first`() {
        assertEquals(listOf("ROVER", "LAND ROVER"), VehiclePick.filter(makes, "rov"))
        assertEquals(makes, VehiclePick.filter(makes, " "))
        assertEquals(emptyList<String>(), VehiclePick.filter(makes, "zz"))
    }

    @Test
    fun `typed text is its own value only when it isn't already listed`() {
        assertEquals("DeLorean", VehiclePick.custom(" DeLorean ", makes))
        assertNull(VehiclePick.custom("honda", makes))
        assertNull(VehiclePick.custom("  ", makes))
    }

    @Test
    fun `lists are tidied and sorted`() {
        assertEquals(listOf("Golf", "GTI", "Jetta"), VehiclePick.tidy(listOf("Jetta", " GTI", "Golf", "", "golf")))
    }
}
