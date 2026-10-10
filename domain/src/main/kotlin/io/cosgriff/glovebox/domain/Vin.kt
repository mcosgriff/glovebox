package io.cosgriff.glovebox.domain

/** North American VIN validation (17 chars, no I/O/Q, ISO 3779 check digit in position 9). */
object Vin {
    private val transliteration: Map<Char, Int> = buildMap {
        "ABCDEFGH".forEachIndexed { i, c -> put(c, i + 1) }
        "JKLMN".forEachIndexed { i, c -> put(c, i + 1) }
        put('P', 7); put('R', 9)
        "STUVWXYZ".forEachIndexed { i, c -> put(c, i + 2) }
        ('0'..'9').forEach { put(it, it - '0') }
    }
    private val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    fun normalize(input: String): String = input.trim().uppercase().filterNot { it.isWhitespace() }

    fun isWellFormed(vin: String): Boolean =
        vin.length == 17 && vin.all { it in transliteration } // excludes I, O, Q

    fun hasValidCheckDigit(vin: String): Boolean {
        if (!isWellFormed(vin)) return false
        val sum = vin.indices.sumOf { i -> transliteration.getValue(vin[i]) * weights[i] }
        val remainder = sum % 11
        val expected = if (remainder == 10) 'X' else ('0' + remainder)
        return vin[8] == expected
    }
}
