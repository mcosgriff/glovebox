package io.cosgriff.glovebox.ui.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Reads a barcode out of one camera frame with ZXing, entirely on the phone. Only the greyscale
 * (luminance) plane is needed. Not thread-safe: use one per analyzer.
 */
class VinBarcodeReader {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                // What a VIN is printed as: Code 39 on the door-jamb sticker, sometimes a QR code.
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.CODE_39, BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    /**
     * The text of a barcode in the frame, or null. [luminance] is [width] x [height], one byte per
     * pixel, rows top to bottom.
     *
     * If the frame can't be read as it is, it is tried again with the lighting evened out. For a
     * 1D barcode ZXing picks a single black/white threshold per row, whichever of its binarizers
     * is used, so a barcode half in shadow or under glare reads as all black or all white on one
     * side. [evenOut] fixes that before ZXing sees it.
     */
    fun read(luminance: ByteArray, width: Int, height: Int): String? =
        attempt(luminance, width, height) ?: attempt(evenOut(luminance, width, height), width, height)

    private fun attempt(luminance: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return attempt(BinaryBitmap(HybridBinarizer(source)))
    }

    private fun attempt(bitmap: BinaryBitmap): String? = try {
        reader.decodeWithState(bitmap).text
    } catch (_: NotFoundException) {
        null
    } finally {
        reader.reset()
    }

    companion object {
        /** Wider than the widest bar or gap of a barcode filling the guide, narrower than a shadow's edge is soft. */
        private const val EVEN_OUT_RADIUS = 24

        /** Less difference than this within the window is plain background, not bars. */
        private const val FLAT = 40

        /**
         * Stretches each pixel between the darkest and lightest pixels near it on its row, so the
         * bars come out black on white wherever the light falls. Areas with nothing in them go white.
         *
         * "Near" is looked at to the left and to the right separately, and the side with the smaller
         * spread wins. At the edge of a shadow one side straddles the edge and the other doesn't;
         * taking both together would turn the paper beside the edge grey, which reads as a bar.
         */
        fun evenOut(luminance: ByteArray, width: Int, height: Int, radius: Int = EVEN_OUT_RADIUS): ByteArray {
            val out = ByteArray(luminance.size)
            val row = IntArray(width)
            val leftLow = IntArray(width)
            val leftHigh = IntArray(width)
            val rightLow = IntArray(width)
            val rightHigh = IntArray(width)
            for (y in 0 until height) {
                val start = y * width
                for (x in 0 until width) row[x] = luminance[start + x].toInt() and 0xFF
                slide(row, back = radius, ahead = 0, into = leftLow, min = true)
                slide(row, back = radius, ahead = 0, into = leftHigh, min = false)
                slide(row, back = 0, ahead = radius, into = rightLow, min = true)
                slide(row, back = 0, ahead = radius, into = rightHigh, min = false)
                for (x in 0 until width) {
                    val left = leftHigh[x] - leftLow[x]
                    val right = rightHigh[x] - rightLow[x]
                    val useLeft = left >= FLAT && (right < FLAT || left <= right)
                    out[start + x] = when {
                        useLeft -> (row[x] - leftLow[x]) * 255 / left
                        right >= FLAT -> (row[x] - rightLow[x]) * 255 / right
                        else -> 255
                    }.toByte()
                }
            }
            return out
        }

        /** Minimum or maximum of [values] from [back] before each position to [ahead] after it, in one pass with a monotonic queue. */
        private fun slide(values: IntArray, back: Int, ahead: Int, into: IntArray, min: Boolean) {
            val n = values.size
            val queue = IntArray(n)
            var head = 0
            var tail = 0
            var next = 0
            for (x in 0 until n) {
                val end = minOf(n - 1, x + ahead)
                while (next <= end) {
                    while (tail > head && (if (min) values[queue[tail - 1]] >= values[next] else values[queue[tail - 1]] <= values[next])) tail--
                    queue[tail++] = next++
                }
                while (queue[head] < x - back) head++
                into[x] = values[queue[head]]
            }
        }

        /**
         * The middle of an upright frame, [widthFraction] by [heightFraction] of it: the part the
         * on-screen guide asks the user to put the barcode in. Reading only this keeps the labels
         * and door edges around the sticker out of the reader's way, and is quicker.
         */
        fun centre(luminance: ByteArray, width: Int, height: Int, widthFraction: Double, heightFraction: Double): Triple<ByteArray, Int, Int> {
            val w = (width * widthFraction).toInt().coerceIn(1, width)
            val h = (height * heightFraction).toInt().coerceIn(1, height)
            val left = (width - w) / 2
            val top = (height - h) / 2
            val out = ByteArray(w * h)
            for (row in 0 until h) System.arraycopy(luminance, (top + row) * width + left, out, row * w, w)
            return Triple(out, w, h)
        }

        /**
         * Turns a frame upright. The camera sensor is mounted sideways, so with the phone held
         * upright a horizontal barcode arrives vertical, and a 1D reader only scans along rows.
         * [degrees] is how far the frame must turn clockwise (CameraX's rotationDegrees).
         */
        fun rotate(luminance: ByteArray, width: Int, height: Int, degrees: Int): Triple<ByteArray, Int, Int> = when (degrees) {
            90 -> {
                val out = ByteArray(luminance.size)
                for (y in 0 until height) for (x in 0 until width) out[x * height + (height - 1 - y)] = luminance[y * width + x]
                Triple(out, height, width)
            }
            180 -> Triple(luminance.reversedArray(), width, height)
            270 -> {
                val out = ByteArray(luminance.size)
                for (y in 0 until height) for (x in 0 until width) out[(width - 1 - x) * height + y] = luminance[y * width + x]
                Triple(out, height, width)
            }
            else -> Triple(luminance, width, height)
        }
    }
}
