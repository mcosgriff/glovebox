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

    // ---- Fluids -------------------------------------------------------------------------------------

    private val gearWinter = setOf(70, 75, 80, 85)
    private val gearSummer = setOf(80, 85, 90, 110, 140, 190, 250)

    /**
     * Gear oil for a differential or transfer case. A grade is written one way ("75w90 gl5" is
     * "75W-90 GL-5"); anything else is taken as the fluid's name, as some use ATF or a maker's own fluid,
     * up to 40 characters. Null only when it is empty, too long, or a grade that doesn't exist.
     */
    fun gearOil(text: String): String? {
        val t = text.trim().replace(Regex("""\s+"""), " ")
        val m = Regex("""(?i)(?:SAE\s*)?(\d{2,3})\s*W\s*-?\s*(\d{2,3})(?:\s*(?:API\s*)?GL\s*-?\s*([45]))?""").matchEntire(t)
            ?: return fluidName(t)
        val (w, s, gl) = m.destructured
        if (w.toInt() !in gearWinter || s.toInt() !in gearSummer) return null
        return "${w.toInt()}W-${s.toInt()}" + if (gl.isNotEmpty()) " GL-$gl" else ""
    }

    /** A fluid by its name ("ATF WS", "Toyota SLLC pink", "G 052 182"): one line, up to 40 characters. */
    fun fluidName(text: String): String? = text.trim().replace(Regex("""\s+"""), " ").takeIf { it.isNotEmpty() && it.length <= 40 }

    private val dot = Regex("""(?i)DOT\s*([345](?:\.1)?)(\s*LV)?""")

    /** A brake fluid's DOT grade, written "DOT 4" or "DOT 4 LV"; DOT 3, 4, 5 and 5.1 exist. */
    fun brakeFluid(text: String): String? {
        val m = dot.matchEntire(text.trim()) ?: return null
        val (grade, lv) = m.destructured
        if (grade !in setOf("3", "4", "5", "5.1")) return null
        return "DOT $grade" + if (lv.isNotBlank()) " LV" else ""
    }

    // ---- Parts ---------------------------------------------------------------------------------------

    /** A BCI battery group size ("24F", "35", "94R", "H6", "LN3"), uppercased. */
    fun batteryGroup(text: String): String? = text.trim().uppercase(Locale.US).replace(" ", "")
        .takeIf { Regex("""\d{1,3}[A-Z]{0,2}|[HLT]\d|LN\d|H\d{1,2}""").matches(it) }

    /** Cold-cranking amps, 100 to 1,500. */
    fun cca(text: String): Int? = text.trim().replace(",", "").removeSuffix("CCA").removeSuffix("cca").trim().toIntOrNull()?.takeIf { it in 100..1500 }

    /**
     * A spark plug gap in inches ("0.044", ".044", "0.044 in") or millimetres ("1.1 mm"), written with
     * its unit: 0.020 to 0.070 in, or 0.5 to 1.8 mm. A bare number below 0.1 is inches.
     */
    fun plugGap(text: String): String? {
        val t = text.trim().lowercase(Locale.US).replace(",", ".")
        Regex("""(\d*\.\d+|\d+)\s*mm""").matchEntire(t)?.let { m ->
            val v = m.groupValues[1].toDouble()
            return if (v in 0.5..1.8) "${trimZero(v)} mm" else null
        }
        Regex("""(\d*\.\d{2,3})\s*(?:in|"|inch|inches)?""").matchEntire(t)?.let { m ->
            val v = m.groupValues[1].toDouble()
            return if (v in 0.020..0.070) String.format(Locale.US, "%.3f in", v) else null
        }
        return null
    }

    private val bulb = Regex("""H\d{1,2}[A-Z]{0,2}|HB[1-5]|HIR[12]|9\d{3}[A-Z]{0,3}|D[1-8][SR]|\d{3,4}[A-Z]{0,3}|P13W|PSX\d{2}W|PS\d{2}W""")

    /** A bulb's code ("H11", "9005", "D4S", "3157"), uppercased and checked against the common families. */
    fun bulbCode(text: String): String? = text.trim().uppercase(Locale.US).replace(" ", "").takeIf { bulb.matches(it) }

    // ---- Tire pressure -------------------------------------------------------------------------------

    enum class Pressure(val symbol: String, val range: IntRange) { PSI("psi", 15..110), KPA("kPa", 100..760) }

    /** A cold inflation pressure as the door placard prints it, in whole psi or kPa. */
    fun tirePressure(text: String, unit: Pressure): Int? =
        text.trim().removeSuffix(unit.symbol).removeSuffix(unit.symbol.uppercase(Locale.US)).trim().toIntOrNull()?.takeIf { it in unit.range }

    /** "32 psi", "35 psi front / 38 psi rear", then "spare 60 psi" when the spare is given. */
    fun pressureParts(front: Int?, rear: Int?, spare: Int?, unit: Pressure?): List<TakesPart>? {
        val u = unit ?: Pressure.PSI
        fun p(v: Int) = TakesPart.Pressure(v, u)
        val main = when {
            front == null && rear == null -> null
            rear == null || rear == front -> p(front ?: rear!!)
            front == null -> TakesPart.For(TakesRole.PRESSURE_REAR, p(rear))
            else -> TakesPart.Both(TakesPart.For(TakesRole.PRESSURE_FRONT, p(front)), TakesPart.For(TakesRole.PRESSURE_REAR, p(rear)))
        }
        return listOfNotNull(main, spare?.let { TakesPart.For(TakesRole.PRESSURE_SPARE, p(it)) }).ifEmpty { null }
    }

    // ---- How each reads ----------------------------------------------------------------------------

    /** "0W-20 · API SP · 6.6 qt · filter 04152-YZZA1", or whatever of that there is; null when nothing is. */
    fun oilParts(grade: String?, approval: String?, capacity: Double?, unit: Volume?, filter: String?): List<TakesPart>? = listOfNotNull(
        grade?.let(TakesPart::Entered),
        approval?.let(TakesPart::Entered),
        capacity?.let { TakesPart.Capacity(it, unit ?: Volume.QUARTS) },
        filter?.let { TakesPart.For(TakesRole.OIL_FILTER, TakesPart.Entered(it)) },
    ).ifEmpty { null }

    /** "P265/70R17", or "Front 225/40R18 · Rear 255/35R18" for different sizes front and rear. */
    fun tireParts(front: String?, rear: String?): List<TakesPart>? = when {
        front == null -> null
        rear == null || rear == front -> listOf(TakesPart.Entered(front))
        else -> listOf(TakesPart.For(TakesRole.TIRE_FRONT, TakesPart.Entered(front)), TakesPart.For(TakesRole.TIRE_REAR, TakesPart.Entered(rear)))
    }

    /** "26\" / 17\" · rear 14\" · Bosch ICON", or whatever of that there is. */
    fun wiperParts(driver: Int?, passenger: Int?, rear: Int?, note: String?): List<TakesPart>? = listOfNotNull(
        when {
            driver != null && passenger != null -> TakesPart.Both(TakesPart.Inches(driver), TakesPart.Inches(passenger))
            else -> (driver ?: passenger)?.let(TakesPart::Inches)
        },
        rear?.let { TakesPart.For(TakesRole.WIPER_REAR, TakesPart.Inches(it)) },
        note?.let(TakesPart::Entered),
    ).ifEmpty { null }

    // How a stored figure is written ("0.9 mm"): part of the entry, not of the wording.
    private fun trimZero(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
