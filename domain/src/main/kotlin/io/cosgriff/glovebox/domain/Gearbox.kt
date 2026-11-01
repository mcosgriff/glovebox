package io.cosgriff.glovebox.domain

/**
 * Which kind of gearbox the owner says a vehicle has (#549), as far as a maker's schedule tells them apart:
 * Volkswagen's cards give a dual-clutch (DSG) gearbox one fluid change and a conventional automatic another.
 * Stored by name (`vehicles.gearbox`); **null is "Don't know"**, and nothing is ever guessed into it.
 */
enum class Gearbox {
    MANUAL,
    AUTOMATIC,
    DUAL_CLUTCH,
}

object Gearboxes {
    /** A stored name, or null for "Don't know" (or a name a later version dropped). */
    fun of(stored: String?): Gearbox? = Gearbox.entries.firstOrNull { it.name == stored }

    /**
     * What the gearbox question starts at when a vehicle is added by VIN, from the transmission the decode wrote
     * ("7-speed Automatic", "Manual/Standard"). **Only where the decode settles it:** vPIC's "Manual" is
     * [Gearbox.MANUAL]. vPIC calls a dual-clutch "Automatic" too, so an automatic is never filled in from its style
     * alone. The one hint is Volkswagen's speeds ([volkswagenHint]). Anything else is null, and the owner is asked;
     * whatever this gives, the owner sees it and can change it.
     */
    fun suggested(make: String?, transmission: String?): Gearbox? = when {
        Transmissions.isManual(transmission) -> Gearbox.MANUAL
        else -> volkswagenHint(make, transmission)
    }

    /**
     * Volkswagen's speeds, for a gearbox known to shift itself: its 7-speed is a dual-clutch (DSG) and its 8-speed a
     * conventional automatic. A 6-speed can be either, and any other make or count says nothing. Null when it can't
     * tell.
     */
    fun volkswagenHint(make: String?, transmission: String?): Gearbox? {
        if (!make.orEmpty().trim().equals("Volkswagen", ignoreCase = true)) return null
        if (!Transmissions.isAutomatic(transmission)) return null
        return when (speeds(transmission)) {
            7 -> Gearbox.DUAL_CLUTCH
            8 -> Gearbox.AUTOMATIC
            else -> null
        }
    }

    private val speedsText = Regex("""(\d{1,2})\s*-?\s*speed""", RegexOption.IGNORE_CASE)

    /** The number of speeds the transmission text gives ("7-speed Automatic"); null when it gives none. */
    fun speeds(transmission: String?): Int? = transmission?.let { speedsText.find(it) }?.groupValues?.get(1)?.toIntOrNull()

    /**
     * What a stored transmission tells for a vehicle that was on record before the question was asked (#549):
     * Manual where it says so plainly, else "Don't know". The migration does the same in SQL.
     */
    fun fromStored(transmission: String?): Gearbox? = if (Transmissions.isManual(transmission)) Gearbox.MANUAL else null

    /**
     * Whether a schedule item for [only] one gearbox (from a maker's `dsgOnly` / `automaticOnly`) reminds on a vehicle
     * whose owner said [vehicle]: only when they are the same. On "Don't know" (null), or after the owner changed the
     * answer, it gives no reminder. An item for any gearbox ([only] null) always may.
     */
    fun reminds(only: Gearbox?, vehicle: Gearbox?): Boolean = only == null || only == vehicle
}

/**
 * What is known of a vehicle's gearbox when a maker's list is matched to it: the owner's answer ([gearbox], null for
 * "Don't know") and, where they haven't given one, the transmission text on record. The answer wins; the text only
 * tells a manual from a gearbox that shifts itself, as before the question was asked.
 */
data class GearboxOnRecord(val gearbox: Gearbox?, val transmission: String? = null) {
    /** Known to be a manual with a clutch pedal. */
    val manual: Boolean get() = gearbox?.let { it == Gearbox.MANUAL } ?: Transmissions.isManual(transmission)

    /** Known to shift itself: an automatic, a dual-clutch, a CVT or an automated manual. */
    val shiftsItself: Boolean get() = gearbox?.let { it != Gearbox.MANUAL } ?: Transmissions.isAutomatic(transmission)

    companion object {
        /** Only the transmission text: what a caller has that predates the question. */
        fun ofText(transmission: String?): GearboxOnRecord = GearboxOnRecord(null, transmission)
    }
}
