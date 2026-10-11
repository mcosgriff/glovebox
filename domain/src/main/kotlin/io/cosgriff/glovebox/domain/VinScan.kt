package io.cosgriff.glovebox.domain

/** Finding a VIN in what a barcode says. */
object VinScan {
    /**
     * The VIN in [text], or null if there isn't one that passes the check digit. A door-jamb
     * Code 39 barcode is usually just the VIN, sometimes with a leading "I" (the import marker);
     * a QR code may carry it among other fields, set off by separators. Anything that fails the
     * check digit is rejected, so a misread never reaches the decoder.
     */
    fun vin(text: String): String? =
        // Runs of VIN characters; everything else (separators, and I, O and Q) ends a run, which
        // also drops the import marker. Only a run of exactly 17 is tried: sliding a window along
        // a longer run would find a chance check-digit match about one time in eleven.
        Regex("[A-HJ-NPR-Z0-9]+").findAll(text.uppercase()).map { it.value }.firstOrNull { it.length == 17 && Vin.hasValidCheckDigit(it) }
}
