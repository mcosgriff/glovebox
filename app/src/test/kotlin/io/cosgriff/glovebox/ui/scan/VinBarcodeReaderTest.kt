package io.cosgriff.glovebox.ui.scan

import org.junit.Assert.assertEquals
import org.junit.Test

/** The frame maths around the reader; reading itself is native code and is tested on a device (VinBarcodeReaderDeviceTest). */
class VinBarcodeReaderTest {
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
