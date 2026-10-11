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

    /** The text of a barcode in the frame, or null. [luminance] is [width] x [height], one byte per pixel, rows top to bottom. */
    fun read(luminance: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }

    companion object {
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
