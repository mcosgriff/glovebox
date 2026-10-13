package io.cosgriff.glovebox.domain

/**
 * Finding the odometer reading in what text recognition makes of a picture of the dash.
 *
 * A cluster shows several numbers at once (a trip meter, the clock, the temperature, the range),
 * and recognition misreads now and then. So a reading is only ever an offer: it fills the field on
 * the Update mileage sheet and the user saves it or corrects it. Nothing here is saved by itself.
 */
object DashReading {
    /** How far past the last reading a number can be and still be taken for the odometer over the others in view. In miles; 320,000 km for a kilometre vehicle. */
    const val FAR_AHEAD = 200_000

    /** How many of the last [WINDOW] frames must agree before a reading is offered. */
    const val AGREE = 3
    const val WINDOW = 5

    /**
     * Every whole number in [text] that could be an odometer reading, in the order read. A number
     * with a decimal point is a trip meter or an odometer with tenths, so its whole part is offered
     * but counts for less ([Candidate.decimal]). Two numbers side by side on a line are also offered
     * joined when the second has three digits, since a gap is often drawn where a comma would go.
     */
    fun candidates(text: String): List<Candidate> {
        val out = mutableListOf<Candidate>()
        for (line in text.lines()) {
            val tokens = Regex("""\d[\d,]*(\.\d+)?""").findAll(line).map { it.value }.toList()
            val whole = tokens.map { it.substringBefore('.').replace(",", "") }
            tokens.forEachIndexed { i, token ->
                number(whole[i])?.let { out += Candidate(it, decimal = '.' in token) }
                if (i > 0 && '.' !in tokens[i - 1] && '.' !in token && whole[i].length == 3) {
                    number(whole[i - 1] + whole[i])?.let { out += Candidate(it, decimal = false, joined = true) }
                }
            }
        }
        return out
    }

    /** Leading zeros are a mechanical odometer's empty drums. Zero and anything too long to type on the sheet are not readings. */
    private fun number(digits: String): Int? =
        digits.trimStart('0').takeIf { it.isNotEmpty() && it.length <= MileageUpdate.MAX_DIGITS }?.toIntOrNull()

    data class Candidate(val value: Int, val decimal: Boolean, val joined: Boolean = false)

    /**
     * The one number in [text] to take for the odometer, or null. [last] is the vehicle's latest
     * known reading in dash terms, which is the best clue there is: the odometer is the number that
     * is at or a little above it, where a trip meter or a clock is far below. **With a last reading,
     * nothing outside that stretch is ever offered**: recognition will find a stray digit in the trim
     * round a cluster, and a number that can't be the odometer is worse than none. A dash that really
     * does read lower (a mistyped last reading) is typed in by hand.
     *
     * With no last reading there is nothing to judge by, so the longest whole number is taken, and
     * one made by joining two is never preferred to one read in a piece.
     */
    fun pick(text: String, last: Int?, unit: DistanceUnit): Int? {
        val all = candidates(text)
        if (last != null) {
            return all.filter { it.value in last..last + unit.scheduled(FAR_AHEAD) }.minWithOrNull(compareBy({ it.value - last }, { it.decimal }))?.value
        }
        return all.filterNot { it.joined }
            .maxWithOrNull(compareBy<Candidate>({ !it.decimal }, { it.value.toString().length }, { it.value }))?.value
    }

    /**
     * The reading to offer after the frames so far, newest last: the one that at least [AGREE] of
     * the last [WINDOW] frames came to, so a single misread is never put in front of the user.
     * A frame nothing was read from is null.
     */
    fun steady(frames: List<Int?>): Int? =
        frames.takeLast(WINDOW).filterNotNull().groupingBy { it }.eachCount().filterValues { it >= AGREE }.maxByOrNull { it.value }?.key
}
