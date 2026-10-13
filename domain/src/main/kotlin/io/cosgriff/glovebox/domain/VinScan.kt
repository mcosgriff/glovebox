package io.cosgriff.glovebox.domain

/** Finding a VIN in what a barcode says, or in what text recognition makes of a printed one. */
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

    /** How many of the last [WINDOW] frames must read the same printed VIN before it is offered. */
    const val AGREE = 2
    const val WINDOW = 5

    /**
     * The VIN in [text] as recognised from print (a windshield plate, a registration card), or null.
     *
     * A VIN never has the letters I, O or Q, exactly because they look like 1 and 0, so where
     * recognition reads one it is the digit: that is the one repair made. Nothing else is guessed:
     * every other misread (S for 5, B for 8) leaves a VIN that fails its check digit and is dropped,
     * and trying substitutions until one passed would pass a wrong VIN about one time in eleven.
     *
     * A card often prints the VIN in groups, so a line is also tried with its spaces closed up. As
     * with a barcode, only a run of exactly 17 is tried, never a slice of something longer.
     */
    fun printed(text: String): String? = text.uppercase().lineSequence().firstNotNullOfOrNull { line ->
        val words = Regex("[A-Z0-9]+").findAll(line).map { it.value }.toList()
        // Each word by itself, then the pieces of the line between punctuation with their spaces closed up.
        val joined = line.split(Regex("[^A-Z0-9 ]+")).map { it.replace(" ", "") }
        (words + joined).map(::asDigits).firstOrNull { it.length == 17 && Vin.hasValidCheckDigit(it) }
    }

    private fun asDigits(run: String): String = run.replace('O', '0').replace('Q', '0').replace('I', '1')

    /**
     * The printed VIN to offer after the frames so far, newest last: one that at least [AGREE] of
     * the last [WINDOW] frames read. The check digit catches most misreads but lets one in eleven
     * through; the same wrong VIN twice running is far less likely. A barcode needs no such wait:
     * it carries its own error checking. A frame nothing was read from is null.
     */
    fun agreed(frames: List<String?>): String? =
        frames.takeLast(WINDOW).filterNotNull().groupingBy { it }.eachCount().filterValues { it >= AGREE }.maxByOrNull { it.value }?.key
}
