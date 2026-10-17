package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanReportTest {
    // A made-up VIN with a valid check digit.
    private val vin = "1M8GDM9AXKP042788"

    @Test
    fun `a barcode is reported as missing, unreadable, a VIN or not one`() {
        assertEquals(BarcodeReport.None, ScanReport.barcode(null))
        assertEquals(BarcodeReport.Unreadable(BarcodeKind.CODE_39), ScanReport.barcode(BarcodeSeen(BarcodeKind.CODE_39, null)))
        assertEquals(BarcodeReport.Vin(BarcodeKind.CODE_39, vin), ScanReport.barcode(BarcodeSeen(BarcodeKind.CODE_39, "I$vin")))
        assertEquals(BarcodeReport.NotAVin(BarcodeKind.QR_CODE, "TIRE 265/70R17"), ScanReport.barcode(BarcodeSeen(BarcodeKind.QR_CODE, "TIRE 265/70R17")))
        assertEquals(
            BarcodeReport.NotAVin(BarcodeKind.CODE_128, "A".repeat(27) + "…"),
            ScanReport.barcode(BarcodeSeen(BarcodeKind.CODE_128, "A".repeat(40))),
        )
    }

    @Test
    fun `print is reported as nothing, a VIN, or what was read instead`() {
        assertEquals(PrintReport.None, ScanReport.print(null))
        assertEquals(PrintReport.None, ScanReport.print("  "))
        assertEquals(PrintReport.Vin(vin), ScanReport.print("VIN $vin"))
        assertEquals(PrintReport.NotAVin("1M8GDM9AXKP04 2788X"), ScanReport.print("1M8GDM9AXKP04\n2788X"))
    }
}
