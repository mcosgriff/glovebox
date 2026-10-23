package io.cosgriff.glovebox.domain

/** A whole number as the owner typed it: digits, with the commas and spaces that group them ignored. */
object WholeNumber {
    /**
     * [text] as a number of [min] or more with at most [maxDigits] digits; null for anything else, blank included.
     * Callers that allow a blank field say so themselves.
     */
    fun parse(text: String, min: Int = 0, maxDigits: Int = 10): Int? {
        val cleaned = text.filterNot { it == ',' || it.isWhitespace() }
        if (cleaned.isEmpty() || cleaned.length > maxDigits || !cleaned.all(Char::isDigit)) return null
        return cleaned.toIntOrNull()?.takeIf { it >= min }
    }

    /** A model year typed by hand: from [first] to next year's models. */
    fun year(text: String, first: Int, thisYear: Int): Int? = text.trim().toIntOrNull()?.takeIf { it in first..thisYear + 1 }
}
