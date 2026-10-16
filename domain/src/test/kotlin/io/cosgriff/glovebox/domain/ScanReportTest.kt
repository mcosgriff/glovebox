package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanReportTest {
    // A made-up VIN with a valid check digit.
    private val vin = "1M8GDM9AXKP042788"

    @Test
    fun `the rate says the camera is still starting until a frame has come`() {
        assertEquals("Starting the camera…", ScanReport.rate(null))
        assertEquals("Waiting for the camera…", ScanReport.rate(0))
        assertEquals("Reading 1 frame a second", ScanReport.rate(1))
        assertEquals("Reading 14 frames a second", ScanReport.rate(14))
    }

    @Test
    fun `a barcode is reported as missing, unreadable, a VIN or not one`() {
        assertEquals("No barcode in the box", ScanReport.barcode(null))
        assertEquals("Code 39 found but not readable. Hold still, closer, out of glare", ScanReport.barcode(BarcodeSeen("Code 39", null)))
        assertEquals("Code 39: $vin", ScanReport.barcode(BarcodeSeen("Code 39", "I$vin")))
        assertEquals("QR code: “TIRE 265/70R17” isn't a VIN", ScanReport.barcode(BarcodeSeen("QR code", "TIRE 265/70R17")))
        assertEquals("Code 128: “${"A".repeat(27)}…” isn't a VIN", ScanReport.barcode(BarcodeSeen("Code 128", "A".repeat(40))))
    }

    @Test
    fun `print is reported as nothing, a VIN, or what was read instead`() {
        assertEquals("No printed text read", ScanReport.print(null))
        assertEquals("No printed text read", ScanReport.print("  "))
        assertEquals("Print: $vin", ScanReport.print("VIN $vin"))
        assertEquals("Print: “1M8GDM9AXKP04 2788X” isn't a whole VIN", ScanReport.print("1M8GDM9AXKP04\n2788X"))
    }
}
