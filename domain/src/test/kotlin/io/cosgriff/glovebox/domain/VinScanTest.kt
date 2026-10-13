package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VinScanTest {
    // The valid test VIN from the NHTSA / ISO 3779 examples.
    private val vin = "1M8GDM9AXKP042788"

    @Test
    fun `a barcode that is just the VIN`() {
        assertEquals(vin, VinScan.vin(vin))
        assertEquals(vin, VinScan.vin(" ${vin.lowercase()} "))
    }

    @Test
    fun `the import marker in front is dropped`() {
        assertEquals(vin, VinScan.vin("I$vin"))
    }

    @Test
    fun `a VIN among other fields is found`() {
        assertEquals(vin, VinScan.vin("VIN:$vin;MY:1989;PLANT:P"))
        assertEquals(vin, VinScan.vin("https://example.test/v?vin=$vin&x=1"))
    }

    @Test
    fun `a misread that fails the check digit is rejected`() {
        assertNull(VinScan.vin("1M8GDM9A1KP042788"))
        assertNull(VinScan.vin("1M8GDM9AXKP04278"))
    }

    @Test
    fun `a VIN run together with other characters is not guessed at`() {
        // Some 17-character slice of this would pass the check digit by chance.
        assertNull(VinScan.vin("00$vin"))
    }

    @Test
    fun `other barcodes are not VINs`() {
        assertNull(VinScan.vin("0123456789012"))
        assertNull(VinScan.vin("https://example.test/"))
        assertNull(VinScan.vin(""))
    }

    @Test
    fun `a printed VIN is read as it stands`() {
        assertEquals(vin, VinScan.printed(vin))
        assertEquals(vin, VinScan.printed("VIN $vin\nGVWR 4500"))
        assertEquals(vin, VinScan.printed("*$vin*"))
        assertEquals(vin, VinScan.printed(vin.lowercase()))
    }

    @Test
    fun `a letter a VIN can't have is the digit it looks like`() {
        // 0 read as O or Q, 1 read as I.
        assertEquals(vin, VinScan.printed("IM8GDM9AXKPO42788"))
        assertEquals(vin, VinScan.printed("1M8GDM9AXKPQ42788"))
    }

    @Test
    fun `a VIN printed in groups is closed up`() {
        assertEquals(vin, VinScan.printed("1M8 GDM9AX KP042788"))
        assertEquals(vin, VinScan.printed("VIN: 1M8GDM9AX KP042788, 1989"))
        // Not across lines.
        assertNull(VinScan.printed("1M8GDM9AX\nKP042788"))
    }

    @Test
    fun `any other misread fails the check digit and nothing is guessed`() {
        // 8 read as B, 5 for S: not repaired.
        assertNull(VinScan.printed("1MBGDM9AXKP042788"))
        assertNull(VinScan.printed("1M8GDM9AXKP04278"))
        assertNull(VinScan.printed("00$vin"))
        assertNull(VinScan.printed("MODEL YEAR 1989 GROSS WEIGHT"))
        assertNull(VinScan.printed(""))
    }

    @Test
    fun `a printed VIN is offered once two of the last five frames agree`() {
        assertNull(VinScan.agreed(listOf(vin)))
        assertEquals(vin, VinScan.agreed(listOf(vin, null, vin)))
        assertNull(VinScan.agreed(listOf(vin, null, null, null, null, vin).drop(1)))
        // A different VIN each time is never offered.
        assertNull(VinScan.agreed(listOf(vin, "1M8GDM9A1KP042788", null)))
        assertNull(VinScan.agreed(emptyList()))
    }
}
