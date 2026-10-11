package io.cosgriff.glovebox.ui.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import io.cosgriff.glovebox.domain.VinScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VinBarcodeReaderTest {
    // The valid test VIN from the NHTSA / ISO 3779 examples; not a real vehicle's.
    private val vin = "1M8GDM9AXKP042788"

    /** A barcode drawn black on white with a quiet margin, as a camera frame's luminance plane. */
    private fun frame(text: String, format: BarcodeFormat, width: Int, height: Int, margin: Int = 60): Triple<ByteArray, Int, Int> {
        val code = MultiFormatWriter().encode(text, format, width, height)
        val w = width + 2 * margin
        val h = height + 2 * margin
        val pixels = ByteArray(w * h) { 0xFF.toByte() }
        for (y in 0 until height) for (x in 0 until width) if (code[x, y]) pixels[(y + margin) * w + x + margin] = 0
        return Triple(pixels, w, h)
    }

    @Test
    fun `reads a Code 39 VIN barcode`() {
        val (pixels, w, h) = frame(vin, BarcodeFormat.CODE_39, 900, 160)
        assertEquals(vin, VinBarcodeReader().read(pixels, w, h))
    }

    @Test
    fun `reads a QR code`() {
        val (pixels, w, h) = frame("VIN:$vin", BarcodeFormat.QR_CODE, 300, 300)
        assertEquals(vin, VinScan.vin(VinBarcodeReader().read(pixels, w, h).orEmpty()))
    }

    @Test
    fun `a frame from a sideways sensor is read once turned upright`() {
        val (upright, w, h) = frame(vin, BarcodeFormat.CODE_39, 900, 160)
        // What the sensor delivers when the upright frame needs a 90 degree clockwise turn.
        val (sideways, sw, sh) = VinBarcodeReader.rotate(upright, w, h, 270)
        val (back, bw, bh) = VinBarcodeReader.rotate(sideways, sw, sh, 90)
        assertEquals(w to h, bw to bh)
        assertEquals(vin, VinBarcodeReader().read(back, bw, bh))
        // And half a turn, for a phone held upside down.
        val (flipped, fw, fh) = VinBarcodeReader.rotate(upright, w, h, 180)
        val (again, aw, ah) = VinBarcodeReader.rotate(flipped, fw, fh, 180)
        assertEquals(vin, VinBarcodeReader().read(again, aw, ah))
    }

    @Test
    fun `rotation moves pixels where a clockwise turn puts them`() {
        // 3 wide, 2 high:  1 2 3
        //                  4 5 6
        val (out, w, h) = VinBarcodeReader.rotate(byteArrayOf(1, 2, 3, 4, 5, 6), 3, 2, 90)
        assertEquals(2 to 3, w to h)
        assertEquals(listOf<Byte>(4, 1, 5, 2, 6, 3), out.toList())
        val (ccw, _, _) = VinBarcodeReader.rotate(byteArrayOf(1, 2, 3, 4, 5, 6), 3, 2, 270)
        assertEquals(listOf<Byte>(3, 6, 2, 5, 1, 4), ccw.toList())
    }

    @Test
    fun `an empty frame has nothing to read`() {
        assertNull(VinBarcodeReader().read(ByteArray(640 * 480) { 0xFF.toByte() }, 640, 480))
    }

    @Test
    fun `the reader can be used again after a miss`() {
        val reader = VinBarcodeReader()
        assertNull(reader.read(ByteArray(640 * 480) { 0xFF.toByte() }, 640, 480))
        val (pixels, w, h) = frame(vin, BarcodeFormat.CODE_39, 900, 160)
        assertEquals(vin, reader.read(pixels, w, h))
    }
}
