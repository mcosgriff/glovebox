package io.cosgriff.glovebox.domain

/**
 * What one camera frame came to, as the VIN scanner reads it. [barcode] is set when a barcode was
 * found in the frame, read or not; [print] is what text recognition made of it, when it ran.
 */
data class FrameReading(val barcode: BarcodeSeen? = null, val print: String? = null)

/** A barcode in a frame. [text] is null when it was found but couldn't be decoded. [format] is its name ("Code 39"). */
data class BarcodeSeen(val format: String, val text: String?)

/**
 * The scanner's running account of what it is doing, in words, so a scan that finds nothing doesn't
 * look like a scanner that isn't trying. Nothing here decides anything; [VinScan] does that.
 */
object ScanReport {
    /** Longer text is cut, so a QR code full of fields doesn't fill the screen. */
    const val SHOWN = 28

    /** "Reading 12 frames a second", or that the camera hasn't sent one yet. */
    fun rate(framesPerSecond: Int?): String = when (framesPerSecond) {
        null -> "Starting the camera…"
        0 -> "Waiting for the camera…"
        1 -> "Reading 1 frame a second"
        else -> "Reading $framesPerSecond frames a second"
    }

    /** What the last barcode read came to; null [seen] is a frame with no barcode in it. */
    fun barcode(seen: BarcodeSeen?): String = when {
        seen == null -> "No barcode in the box"
        seen.text == null -> "${seen.format} found but not readable. Hold still, closer, out of glare"
        VinScan.vin(seen.text) != null -> "${seen.format}: ${VinScan.vin(seen.text)}"
        else -> "${seen.format}: “${cut(seen.text)}” isn't a VIN"
    }

    /** What the last print read came to; null [text] is a frame with no text read. */
    fun print(text: String?): String = when {
        text.isNullOrBlank() -> "No printed text read"
        VinScan.printed(text) != null -> "Print: ${VinScan.printed(text)}"
        else -> "Print: “${cut(text.replace(Regex("\\s+"), " ").trim())}” isn't a whole VIN"
    }

    private fun cut(text: String): String = if (text.length <= SHOWN) text else text.take(SHOWN - 1) + "…"
}
