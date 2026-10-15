package io.cosgriff.glovebox.domain

import java.util.Locale

/**
 * What a vehicle takes, as entered under "What it takes" on Vehicle details: checked as it is typed and
 * written back in one standard form, so it reads the same everywhere it is shown. Nothing here is
 * looked up or suggested from the model; it is all the owner's own entry.
 */
object VehicleSpecEntry {
    // ---- Engine oil ------------------------------------------------------------------------------

    /** The grades offered in the picker, commonest first. Others can be typed. */
    val commonGrades = listOf("0W-20", "5W-30", "0W-16", "5W-20", "0W-30", "0W-40", "5W-40", "10W-30", "10W-40", "15W-40", "0W-8", "0W-12")

    private val winter = setOf(0, 5, 10, 15, 20, 25)
    private val summer = setOf(8, 12, 16, 20, 30, 40, 50, 60)

    /**
     * An SAE viscosity grade in its standard form ("5w30", "SAE 5W 30" and "5W-30" are all "5W-30"),
     * or null if it isn't one. A single grade such as "30" is allowed, as some older engines take.
     */
    fun oilGrade(text: String): String? {
        val t = text.trim().uppercase(Locale.US).removePrefix("SAE").trim()
        Regex("""(\d{1,2})\s*W\s*-?\s*(\d{1,2})""").matchEntire(t)?.let { m ->
            val (w, s) = m.destructured
            return if (w.toInt() in winter && s.toInt() in summer) "${w.toInt()}W-${s.toInt()}" else null
        }
        return t.toIntOrNull()?.takeIf { it in summer }?.toString()
    }

    /** The approval or specification the manual names ("API SP", "VW 502 00", "dexos1 Gen 3"): one line, as typed. */
    fun oilApproval(text: String): String? = text.trim().replace(Regex("""\s+"""), " ").takeIf { it.isNotEmpty() && it.length <= 60 }

    enum class Volume(val symbol: String, val max: Double) { QUARTS("qt", 20.0), LITRES("L", 19.0) }

    /** How much oil it holds, with the filter: 1 to 20 quarts or 1 to 19 litres, to one decimal place. */
    fun oilCapacity(text: String, unit: Volume): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it >= 1.0 && it <= unit.max && Regex("""\d{1,2}(\.\d)?""").matches(text.trim().replace(',', '.')) }

    /**
     * Filter part numbers: letters, digits, dashes, dots, slashes and spaces, uppercased, up to three
     * separated by commas (the factory number and the one bought, say). Null if anything else is in it.
     * Formats differ by maker, so nothing more is checked.
     */
    fun partNumbers(text: String): String? {
        val parts = text.split(',').map { it.trim().replace(Regex("""\s+"""), " ").uppercase(Locale.US) }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 3) return null
        if (parts.any { !Regex("""[A-Z0-9][A-Z0-9 ./-]{1,29}""").matches(it) }) return null
        return parts.joinToString(", ")
    }

    // ---- Tires -----------------------------------------------------------------------------------

    private val metric = Regex("""(P|LT|ST|T)?\s*(\d{3})\s*/\s*(\d{2})\s*(ZR|R|D|B)\s*(\d{2})(?:\s+(\d{2,3}(?:/\d{2,3})?)\s*([A-Z]))?""")
    private val flotation = Regex("""(\d{2}(?:\.\d)?)\s*[Xx]\s*(\d{1,2}\.\d{2})\s*R\s*(\d{2})(\s*LT)?(?:\s+(\d{2,3})\s*([A-Z]))?""")
    private val speedRatings = "LMNPQRSTUHVWYZ".toSet()

    /**
     * A tire size in its standard form, or null if it isn't one: metric sizes as printed on the sidewall
     * ("P265/70R17", "225/40ZR18 92Y", "LT285/75R16 126/123R") with the width a multiple of 5 from 125
     * to 395, an aspect ratio from 25 to 90 and a rim from 12 to 26 inches; or flotation sizes
     * ("33x12.50R15LT"). The load index and speed rating are optional and checked when given.
     */
    fun tireSize(text: String): String? {
        val t = text.trim().uppercase(Locale.US)
        metric.matchEntire(t)?.let { m ->
            val (type, width, aspect, construction, rim, load, speed) = m.destructured
            val w = width.toInt()
            if (w !in 125..395 || w % 5 != 0 || aspect.toInt() !in 25..90 || rim.toInt() !in 12..26) return null
            if (load.isNotEmpty() && (load.split('/').any { it.toInt() !in 60..150 } || speed.single() !in speedRatings)) return null
            return "$type$width/$aspect$construction$rim" + if (load.isNotEmpty()) " $load$speed" else ""
        }
        flotation.matchEntire(t)?.let { m ->
            val (diameter, width, rim, lt, load, speed) = m.destructured
            if (diameter.toDouble() !in 26.0..44.0 || width.toDouble() !in 7.5..17.5 || rim.toInt() !in 14..26) return null
            if (load.isNotEmpty() && (load.toInt() !in 60..150 || speed.single() !in speedRatings)) return null
            return "${diameter}x${width}R$rim" + (if (lt.isNotBlank()) "LT" else "") + if (load.isNotEmpty()) " $load$speed" else ""
        }
        return null
    }

    // ---- Wiper blades ----------------------------------------------------------------------------

    /** A blade length in whole inches, 10 to 34. */
    fun wiperLength(text: String): Int? = text.trim().removeSuffix("\"").removeSuffix("in").trim().toIntOrNull()?.takeIf { it in 10..34 }

    // ---- How each reads ----------------------------------------------------------------------------

    /** "0W-20 · API SP · 6.6 qt · filter 04152-YZZA1", or whatever of that there is; null when nothing is. */
    fun oilSummary(grade: String?, approval: String?, capacity: Double?, unit: Volume?, filter: String?): String? = listOfNotNull(
        grade,
        approval,
        capacity?.let { "${trimZero(it)} ${(unit ?: Volume.QUARTS).symbol}" },
        filter?.let { "filter $it" },
    ).joinToString(" · ").ifEmpty { null }

    /** "P265/70R17", or "Front 225/40R18 · Rear 255/35R18" for different sizes front and rear. */
    fun tireSummary(front: String?, rear: String?): String? = when {
        front == null -> null
        rear == null || rear == front -> front
        else -> "Front $front · Rear $rear"
    }

    /** "26\" / 17\" · rear 14\" · Bosch ICON", or whatever of that there is. */
    fun wiperSummary(driver: Int?, passenger: Int?, rear: Int?, note: String?): String? = listOfNotNull(
        listOfNotNull(driver, passenger).takeIf { it.isNotEmpty() }?.joinToString(" / ") { "$it\"" },
        rear?.let { "rear $it\"" },
        note,
    ).joinToString(" · ").ifEmpty { null }

    private fun trimZero(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
