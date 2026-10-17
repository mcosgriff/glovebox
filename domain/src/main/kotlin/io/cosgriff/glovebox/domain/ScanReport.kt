package io.cosgriff.glovebox.domain

/**
 * What one camera frame came to, as the VIN scanner reads it. [barcode] is set when a barcode was
 * found in the frame, read or not; [print] is what text recognition made of it, when it ran.
 */
data class FrameReading(val barcode: BarcodeSeen? = null, val print: String? = null)

/** The kinds of barcode the scanner reads; `app` names them. */
enum class BarcodeKind { CODE_39, CODE_128, QR_CODE, DATA_MATRIX, OTHER }

/** A barcode in a frame. [text] is null when it was found but couldn't be decoded. */
data class BarcodeSeen(val kind: BarcodeKind, val text: String?)

/** What the last barcode read came to; `app` words it. */
sealed interface BarcodeReport {
    /** A frame with no barcode in it. */
    data object None : BarcodeReport

    data class Unreadable(val kind: BarcodeKind) : BarcodeReport

    data class Vin(val kind: BarcodeKind, val vin: String) : BarcodeReport

    /** [shown] is what it said, cut to [ScanReport.SHOWN]. */
    data class NotAVin(val kind: BarcodeKind, val shown: String) : BarcodeReport
}

/** What the last print read came to; `app` words it. */
sealed interface PrintReport {
    /** No text read. */
    data object None : PrintReport

    data class Vin(val vin: String) : PrintReport

    /** [shown] is what was read, on one line and cut to [ScanReport.SHOWN]. */
    data class NotAVin(val shown: String) : PrintReport
}

/**
 * The scanner's running account of what it is doing, so a scan that finds nothing doesn't look like
 * a scanner that isn't trying. Nothing here decides anything; [VinScan] does that.
 */
object ScanReport {
    /** Longer text is cut, so a QR code full of fields doesn't fill the screen. */
    const val SHOWN = 28

    /** Null [seen] is a frame with no barcode in it. */
    fun barcode(seen: BarcodeSeen?): BarcodeReport {
        if (seen == null) return BarcodeReport.None
        val text = seen.text ?: return BarcodeReport.Unreadable(seen.kind)
        return VinScan.vin(text)?.let { BarcodeReport.Vin(seen.kind, it) } ?: BarcodeReport.NotAVin(seen.kind, cut(text))
    }

    /** Null [text] is a frame with no text read. */
    fun print(text: String?): PrintReport {
        if (text.isNullOrBlank()) return PrintReport.None
        return VinScan.printed(text)?.let(PrintReport::Vin) ?: PrintReport.NotAVin(cut(text.replace(Regex("\\s+"), " ").trim()))
    }

    private fun cut(text: String): String = if (text.length <= SHOWN) text else text.take(SHOWN - 1) + "…"
}
