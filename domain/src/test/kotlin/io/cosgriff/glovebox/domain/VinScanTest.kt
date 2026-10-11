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
}
