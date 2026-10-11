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

    @Test
    fun `the middle of a frame is cut out`() {
        // 4 wide, 4 high, numbered row by row.
        val pixels = ByteArray(16) { it.toByte() }
        val (out, w, h) = VinBarcodeReader.centre(pixels, 4, 4, 0.5, 0.5)
        assertEquals(2 to 2, w to h)
        assertEquals(listOf<Byte>(5, 6, 9, 10), out.toList())
        val (all, aw, ah) = VinBarcodeReader.centre(pixels, 4, 4, 1.0, 1.0)
        assertEquals(4 to 4, aw to ah)
        assertEquals(pixels.toList(), all.toList())
    }

    @Test
    fun `a barcode inside the guide band is read from the band alone`() {
        // The barcode in the middle of a tall frame, as when it is lined up with the guide.
        val (barcode, bw, bh) = frame(vin, BarcodeFormat.CODE_39, 800, 160, margin = 20)
        val w = 1080
        val h = 1920
        val pixels = ByteArray(w * h) { 0xFF.toByte() }
        val left = (w - bw) / 2
        val top = (h - bh) / 2
        for (y in 0 until bh) System.arraycopy(barcode, y * bw, pixels, (top + y) * w + left, bw)
        // Clutter outside the band that would otherwise be in the reader's way.
        for (y in 0 until 300) for (x in 0 until w) pixels[y * w + x] = if ((x / 7 + y / 5) % 2 == 0) 0 else 0xFF.toByte()
        val (band, gw, gh) = VinBarcodeReader.centre(pixels, w, h, 0.8, 0.3)
        assertEquals(vin, VinBarcodeReader().read(band, gw, gh))
    }

    @Test
    fun `a barcode half in shadow is still read`() {
        val (pixels, w, h) = frame(vin, BarcodeFormat.CODE_39, 900, 160)
        // Darken the right-hand side the way a door frame's shadow would: white drops to mid grey, black stays black.
        val shaded = ByteArray(pixels.size) { i ->
            val x = i % w
            val v = pixels[i].toInt() and 0xFF
            (if (x > w / 2) v * 110 / 255 else v).toByte()
        }
        assertEquals(vin, VinBarcodeReader().read(shaded, w, h))
    }

    @Test
    fun `evening out makes bars black on white whatever the light`() {
        // One row: bright background with a dark bar, then the same in shadow at under half the brightness.
        val row = intArrayOf(250, 250, 20, 20, 250, 250, 250, 250, 100, 100, 8, 8, 100, 100, 100, 100)
        val out = VinBarcodeReader.evenOut(ByteArray(row.size) { row[it].toByte() }, row.size, 1, radius = 3).map { it.toInt() and 0xFF }
        assertEquals(listOf(255, 255, 0, 0, 255, 255), out.subList(0, 6))
        assertEquals(listOf(255, 255, 0, 0, 255, 255), out.subList(8, 14))
    }

    @Test
    fun `an empty area stays white when evened out`() {
        val flat = ByteArray(64) { 120 }
        assertEquals(List(64) { 255 }, VinBarcodeReader.evenOut(flat, 64, 1).map { it.toInt() and 0xFF })
    }
}
