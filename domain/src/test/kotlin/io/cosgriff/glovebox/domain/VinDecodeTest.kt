package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VinDecodeTest {
    @Test
    fun `code 0 with a year, make and model is clean`() {
        assertEquals(DecodeQuality.CLEAN, VinDecode.quality("0", 2019, "TOYOTA", "4Runner"))
        assertEquals(DecodeQuality.CLEAN, VinDecode.quality(null, 2019, "TOYOTA", "4Runner"))
    }

    @Test
    fun `any other code with the basics present is partial`() {
        assertEquals(DecodeQuality.PARTIAL, VinDecode.quality("1", 2019, "TOYOTA", "4Runner"))
        assertEquals(DecodeQuality.PARTIAL, VinDecode.quality("0, 14", 2019, "TOYOTA", "4Runner"))
    }

    @Test
    fun `a missing year, make or model is unidentified whatever the code`() {
        assertEquals(DecodeQuality.UNIDENTIFIED, VinDecode.quality("0", null, "TOYOTA", "4Runner"))
        assertEquals(DecodeQuality.UNIDENTIFIED, VinDecode.quality("8", 2019, " ", "4Runner"))
        assertEquals(DecodeQuality.UNIDENTIFIED, VinDecode.quality("6", 2019, "TOYOTA", null))
    }

    @Test
    fun `trims vPIC lists together are offered one by one`() {
        assertEquals(listOf("SR5", "SR5 Premium"), VinDecode.trimChoices("SR5/SR5 Premium"))
        assertEquals(listOf("LX", "EX"), VinDecode.trimChoices("LX, EX, LX"))
        assertEquals(listOf("EX-V6"), VinDecode.trimChoices("EX-V6"))
        assertEquals(emptyList<String>(), VinDecode.trimChoices(null))
    }

    @Test
    fun `drive type is shortened to its first part`() {
        assertEquals("4WD", VinDecode.shortDrive("4WD/4-Wheel Drive/4x4"))
        assertEquals("FWD", VinDecode.shortDrive("FWD"))
        assertNull(VinDecode.shortDrive(" "))
    }

    @Test
    fun `typed model years must be plausible`() {
        assertEquals(2019, VinDecode.parseYear(" 2019 ", thisYear = 2026))
        assertEquals(2027, VinDecode.parseYear("2027", thisYear = 2026))
        assertNull(VinDecode.parseYear("2028", thisYear = 2026))
        assertNull(VinDecode.parseYear("1980", thisYear = 2026))
        assertNull(VinDecode.parseYear("19", thisYear = 2026))
    }
}
