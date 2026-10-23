package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WholeNumberTest {
    @Test
    fun `digits with their grouping, within the minimum and the digit cap`() {
        assertEquals(12_345, WholeNumber.parse("12,345"))
        assertEquals(12_345, WholeNumber.parse(" 12 345 "))
        assertEquals(0, WholeNumber.parse("0"))
        assertNull(WholeNumber.parse("0", min = 1))
        assertNull(WholeNumber.parse("1234567", maxDigits = 6))
        assertNull(WholeNumber.parse(""))
        assertNull(WholeNumber.parse("12.5"))
        assertNull(WholeNumber.parse("-3"))
        assertNull(WholeNumber.parse("9999999999"))
    }

    @Test
    fun `a year from the first allowed to next year's models`() {
        assertEquals(2027, WholeNumber.year("2027", 1981, 2026))
        assertNull(WholeNumber.year("2028", 1981, 2026))
        assertNull(WholeNumber.year("1979", 1981, 2026))
        assertEquals(1979, WholeNumber.year(" 1979 ", 1900, 2026))
    }
}
