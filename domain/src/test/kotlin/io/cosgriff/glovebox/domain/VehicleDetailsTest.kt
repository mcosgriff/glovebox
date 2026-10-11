package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleDetailsTest {
    @Test
    fun `typed text is trimmed, single-spaced and capped`() {
        assertEquals("Old Blue", VehicleText.clean("  Old   Blue \n", VehicleText.MAX_NICKNAME))
        assertEquals("abcde", VehicleText.clean("abcde fghij", 6))
        assertNull(VehicleText.clean("   ", VehicleText.MAX_NICKNAME))
    }

    @Test
    fun `title leaves out what the decode didn't give`() {
        assertEquals("2019 Toyota 4Runner SR5", VehicleText.title(2019, "TOYOTA", "4Runner", "SR5"))
        assertEquals("2019 Toyota 4Runner", VehicleText.title(2019, "TOYOTA", "4Runner", null))
        assertEquals("4Runner", VehicleText.title(0, "Unknown", "4Runner", " "))
        assertEquals("Unknown vehicle", VehicleText.title(0, "Unknown", "Unknown", null))
    }

    @Test
    fun `schedule summary counts tracked items and how they are driven`() {
        val normal = ScheduleSummaries.of(listOf(true to false, true to false, false to true))
        assertEquals(ScheduleSummary(tracked = 2, severe = 0), normal)
        assertEquals(DrivingUse.NORMAL, normal.use)
        assertEquals(DrivingUse.SEVERE, ScheduleSummaries.of(listOf(true to true, true to true)).use)
        assertEquals(DrivingUse.MIXED, ScheduleSummaries.of(listOf(true to true, true to false)).use)
        // Nothing tracked reads as normal driving rather than "all severe".
        assertEquals(DrivingUse.NORMAL, ScheduleSummaries.of(listOf(false to true)).use)
    }

    @Test
    fun `photos shrink to the longest edge and never grow`() {
        assertEquals(1600 to 1200, PhotoSizing.fit(4000, 3000))
        assertEquals(1200 to 1600, PhotoSizing.fit(3000, 4000))
        assertEquals(800 to 600, PhotoSizing.fit(800, 600))
        assertEquals(1600 to 1, PhotoSizing.fit(16000, 2))
    }
}
