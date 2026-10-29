package io.cosgriff.glovebox.domain

/**
 * Makes a photo of a paper invoice ready for Tesseract (#513), as #482 measured best on real photos: the page
 * [READ_WIDTH] wide in greyscale, with its light flattened.
 */
object PageLight {
    /** 3400 px wide read 55 of 56 figures; 2550 (300 dpi) only 52. */
    const val READ_WIDTH = 3400

    /** The size a [width] x [height] photo is read at: [READ_WIDTH] wide, enlarged if it is smaller, height in proportion. */
    fun readSize(width: Int, height: Int): Pair<Int, Int> =
        READ_WIDTH to (height.toLong() * READ_WIDTH / width.coerceAtLeast(1)).toInt().coerceAtLeast(1)

    /** The page is looked at in squares this many pixels across to find its paper. */
    private const val CELL = 8

    /** How many squares either way the paper's light is averaged over: about a line and a half of print at [READ_WIDTH]. */
    private const val BLUR_CELLS = 4

    /**
     * The page divided by its own light, so paper reads as white wherever it is lit and print stays dark: a hand's
     * shadow across a page cost #482 four figures that flattening won back. The light is the brightest pixel of each
     * [CELL] square (paper, not print), averaged over its neighbours and spread smoothly back over the page.
     * [luminance] is [width] x [height], one byte per pixel, rows top to bottom; so is what comes back.
     */
    fun flatten(luminance: ByteArray, width: Int, height: Int): ByteArray {
        val across = (width + CELL - 1) / CELL
        val down = (height + CELL - 1) / CELL
        val brightest = IntArray(across * down)
        for (y in 0 until height) {
            val row = (y / CELL) * across
            for (x in 0 until width) {
                val v = luminance[y * width + x].toInt() and 0xFF
                val c = row + x / CELL
                if (v > brightest[c]) brightest[c] = v
            }
        }
        val light = blur(brightest, across, down)
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            // Between the middles of the squares above and below, and of those either side.
            val gy = ((y + 0.5f) / CELL - 0.5f).coerceIn(0f, (down - 1).toFloat())
            val y0 = gy.toInt()
            val y1 = minOf(y0 + 1, down - 1)
            val fy = gy - y0
            for (x in 0 until width) {
                val gx = ((x + 0.5f) / CELL - 0.5f).coerceIn(0f, (across - 1).toFloat())
                val x0 = gx.toInt()
                val x1 = minOf(x0 + 1, across - 1)
                val fx = gx - x0
                val top = light[y0 * across + x0] * (1 - fx) + light[y0 * across + x1] * fx
                val bottom = light[y1 * across + x0] * (1 - fx) + light[y1 * across + x1] * fx
                val paper = (top * (1 - fy) + bottom * fy).coerceAtLeast(1f)
                val v = luminance[y * width + x].toInt() and 0xFF
                out[y * width + x] = (v * 255f / paper).toInt().coerceAtMost(255).toByte()
            }
        }
        return out
    }

    /** Each square the average of those within [BLUR_CELLS] of it, across then down; the edges repeat outwards. */
    private fun blur(cells: IntArray, across: Int, down: Int): FloatArray {
        val span = 2 * BLUR_CELLS + 1
        val wide = FloatArray(cells.size)
        for (y in 0 until down) for (x in 0 until across) {
            var sum = 0
            for (d in -BLUR_CELLS..BLUR_CELLS) sum += cells[y * across + (x + d).coerceIn(0, across - 1)]
            wide[y * across + x] = sum.toFloat() / span
        }
        val out = FloatArray(cells.size)
        for (y in 0 until down) for (x in 0 until across) {
            var sum = 0f
            for (d in -BLUR_CELLS..BLUR_CELLS) sum += wide[(y + d).coerceIn(0, down - 1) * across + x]
            out[y * across + x] = sum / span
        }
        return out
    }
}
