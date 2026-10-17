package io.cosgriff.glovebox.ui.scan

import io.cosgriff.glovebox.domain.BarcodeKind
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import zxingcpp.BarcodeReader

/**
 * Reads a barcode out of one camera frame with zxing-cpp, entirely on the phone. Only the greyscale
 * (luminance) plane is needed. Not thread-safe: use one per analyzer.
 *
 * zxing-cpp (#161) replaced the Java ZXing library after a phone trial: on live frames of a door-jamb
 * sticker, blurred or under glare, the Java reader read nothing or read wrongly, where zxing-cpp is
 * built for poor frames. Google's ML Kit would read better still but sends usage logs to Google, which
 * the app's network rule doesn't allow.
 */
class VinBarcodeReader {
    private val reader = BarcodeReader(
        BarcodeReader.Options(
            // Code 39 on most door-jamb stickers; QR, Data Matrix and Code 128 on others. Whatever carries
            // it, VinScan only accepts 17 characters with a valid check digit.
            formats = setOf(BarcodeReader.Format.CODE_39, BarcodeReader.Format.QR_CODE, BarcodeReader.Format.DATA_MATRIX, BarcodeReader.Format.CODE_128),
            tryHarder = true,
            // A QR or Data Matrix code printed light on dark; zxing-cpp doesn't invert a 1D code, and labels don't print one so.
            tryInvert = true,
            tryDownscale = true,
            // Two: a code that decoded and one that didn't, so the overlay can show the second when there is no first.
            maxNumberOfSymbols = 2,
            // Barcodes found but not decoded come back too, marked with their error, so the scanner can say one is in
            // view. They are never taken as a reading.
            returnErrors = true,
        ),
    )

    /**
     * A barcode found in a frame, read or not. [text] is null when it was found but couldn't be
     * decoded. [corners] are its outline in the frame's pixels, clockwise from top left.
     */
    data class Sighting(val kind: BarcodeKind, val text: String?, val corners: List<Point>)

    /**
     * The text of a barcode in the frame, or null. [luminance] is [width] x [height], one byte per
     * pixel, rows top to bottom.
     *
     * If the frame can't be read as it is, it is tried again with the lighting evened out: a barcode
     * half in shadow or under glare otherwise reads as all black or all white on one side.
     */
    fun read(luminance: ByteArray, width: Int, height: Int): String? = look(luminance, width, height)?.text

    /**
     * The barcode in the frame: one that decoded if there is one, otherwise one that was found and
     * couldn't be, otherwise null. Tried as [read] tries it, evened out when the frame as it is gives
     * no text.
     */
    fun look(luminance: ByteArray, width: Int, height: Int): Sighting? {
        val first = attempt(luminance, width, height)
        if (first?.text != null) return first
        return attempt(evenOut(luminance, width, height), width, height)?.takeIf { it.text != null } ?: first
    }

    private fun attempt(luminance: ByteArray, width: Int, height: Int): Sighting? {
        // zxing-cpp takes a Bitmap; grey in all three colours, so it reads back as the same luminance.
        val pixels = IntArray(width * height) { i ->
            val l = luminance[i].toInt() and 0xFF
            (0xFF shl 24) or (l shl 16) or (l shl 8) or l
        }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        return try {
            val results = reader.read(bitmap, Rect(0, 0, width, height), 0)
            val result = results.firstOrNull { it.error == null && it.text != null } ?: results.firstOrNull() ?: return null
            val p = result.position
            Sighting(
                kind = kind(result.format),
                text = result.text.takeIf { result.error == null && !it.isNullOrEmpty() },
                corners = listOf(p.topLeft, p.topRight, p.bottomRight, p.bottomLeft),
            )
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        fun kind(format: BarcodeReader.Format): BarcodeKind = when (format) {
            BarcodeReader.Format.CODE_39 -> BarcodeKind.CODE_39
            BarcodeReader.Format.CODE_128 -> BarcodeKind.CODE_128
            BarcodeReader.Format.QR_CODE -> BarcodeKind.QR_CODE
            BarcodeReader.Format.DATA_MATRIX -> BarcodeKind.DATA_MATRIX
            else -> BarcodeKind.OTHER
        }

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
         * [degrees] is how far the frame must turn clockwise: the frame's `rotationDegrees`, which
         * [CameraFeed] keeps to the way the screen is turned.
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
