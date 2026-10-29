package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A photo of a paper invoice made ready for Tesseract (#513), as #482 measured best. */
class PageLightTest {
    @Test
    fun `a photo is read at 3400 pixels wide, its height in proportion`() {
        assertEquals(3400 to 4533, PageLight.readSize(6000, 8000))
        // A smaller photo is enlarged: 3400 wide read 55 of 56 figures, 300 dpi (2550) only 52.
        assertEquals(3400 to 4400, PageLight.readSize(1700, 2200))
    }

    @Test
    fun `a hand's shadow across the page is flattened out, and the print stays dark on both sides of it`() {
        val width = 400
        val height = 300
        // Paper at 110 in the shadow on the left, 230 in the light on the right; print a third as bright as its paper.
        val paper = { x: Int -> if (x < width / 2) 110 else 230 }
        val inPrint = { x: Int, y: Int -> y in 140..147 && (x in 60..67 || x in 330..337) }
        val photo = ByteArray(width * height) { i ->
            val x = i % width
            val y = i / width
            (if (inPrint(x, y)) paper(x) / 3 else paper(x)).toByte()
        }

        val flat = PageLight.flatten(photo, width, height)
        val at = { x: Int, y: Int -> flat[y * width + x].toInt() and 0xFF }

        assertEquals(width * height, flat.size)
        // Paper reads as white wherever the light fell.
        listOf(30 to 40, 120 to 250, 280 to 40, 370 to 250).forEach { (x, y) -> assertTrue("paper at $x,$y is ${at(x, y)}", at(x, y) >= 240) }
        // Print is dark in the shadow and in the light alike.
        listOf(63 to 143, 333 to 143).forEach { (x, y) -> assertTrue("print at $x,$y is ${at(x, y)}", at(x, y) <= 110) }
    }

    @Test
    fun `a photo smaller than the blur is still flattened`() {
        val flat = PageLight.flatten(ByteArray(6) { 100 }, 3, 2)
        assertEquals(List(6) { 255 }, flat.map { it.toInt() and 0xFF })
    }
}
