package io.cosgriff.glovebox.ui.scan

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import io.cosgriff.glovebox.domain.VinScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The barcode reader on real native code (zxing-cpp): every kind of barcode a VIN comes as, and the
 * frames a camera gives. The barcodes are drawn with the Java ZXing writer.
 */
@RunWith(AndroidJUnit4::class)
class VinBarcodeReaderDeviceTest {
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
    fun readsACode39VINBarcode() {
        val (pixels, w, h) = frame(vin, BarcodeFormat.CODE_39, 900, 160)
        assertEquals(vin, VinBarcodeReader().read(pixels, w, h))
    }

    @Test
    fun readsAQRCode() {
        val (pixels, w, h) = frame("VIN:$vin", BarcodeFormat.QR_CODE, 300, 300)
        assertEquals(vin, VinScan.vin(VinBarcodeReader().read(pixels, w, h).orEmpty()))
    }

    @Test
    fun aFrameFromASidewaysSensorIsReadOnceTurnedUpright() {
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
    fun anEmptyFrameHasNothingToRead() {
        assertNull(VinBarcodeReader().read(ByteArray(640 * 480) { 0xFF.toByte() }, 640, 480))
    }

    @Test
    fun theReaderCanBeUsedAgainAfterAMiss() {
        val reader = VinBarcodeReader()
        assertNull(reader.read(ByteArray(640 * 480) { 0xFF.toByte() }, 640, 480))
        val (pixels, w, h) = frame(vin, BarcodeFormat.CODE_39, 900, 160)
        assertEquals(vin, reader.read(pixels, w, h))
    }

    @Test
    fun aBarcodeInsideTheGuideBandIsReadFromTheBandAlone() {
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
    fun aBarcodeHalfInShadowIsStillRead() {
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
    fun readsADoorjambBarcodeWithTheImportMarkerInFrontOfTheVIN() {
        val (pixels, w, h) = frame("I$vin", BarcodeFormat.CODE_39, 960, 160)
        assertEquals(vin, VinScan.vin(VinBarcodeReader().read(pixels, w, h).orEmpty()))
    }

    @Test
    fun readsADataMatrixAndACode128() {
        val (dm, dw, dh) = frame(vin, BarcodeFormat.DATA_MATRIX, 240, 240)
        assertEquals(vin, VinBarcodeReader().read(dm, dw, dh))
        val (c128, cw, ch) = frame(vin, BarcodeFormat.CODE_128, 900, 160)
        assertEquals(vin, VinBarcodeReader().read(c128, cw, ch))
    }

    @Test
    fun readsAQRCodePrintedLightOnDark() {
        // Inverting helps two-dimensional codes; a Code 39 printed light on dark isn't read, and isn't how door-jamb labels print it.
        val (pixels, w, h) = frame(vin, BarcodeFormat.QR_CODE, 300, 300)
        val inverted = ByteArray(pixels.size) { (255 - (pixels[it].toInt() and 0xFF)).toByte() }
        assertEquals(vin, VinBarcodeReader().read(inverted, w, h))
    }

    @Test
    fun readsABlurredBarcode() {
        val (pixels, w, h) = frame(vin, BarcodeFormat.CODE_39, 1000, 160)
        // A box blur along the rows, about a bar's width, as a camera a little out of focus gives.
        val r = 1
        val blurred = ByteArray(pixels.size) { i ->
            val x = i % w
            var sum = 0
            var n = 0
            for (dx in -r..r) if (x + dx in 0 until w) { sum += pixels[i + dx].toInt() and 0xFF; n++ }
            (sum / n).toByte()
        }
        assertEquals(vin, VinBarcodeReader().read(blurred, w, h))
    }
}
