package io.cosgriff.glovebox.domain

/**
 * A vehicle's body, as far as a maker's schedule tells them apart: the 2026 Corolla sedan and hatchback share an
 * engine and a name in NHTSA's data, and their guides differ (#185). Stored by name (`vehicles.bodyStyle`); null is
 * "not known", never guessed.
 */
enum class BodyStyle(val fileName: String) {
    SEDAN("sedan"),
    HATCHBACK("hatchback"),
}

object BodyStyles {
    /** A stored name, or null for nothing on record (or a name a later version dropped). */
    fun of(stored: String?): BodyStyle? = BodyStyle.entries.firstOrNull { it.name == stored }

    /** As a bundled list writes it ("hatchback"). */
    fun fromFile(name: String): BodyStyle? = BodyStyle.entries.firstOrNull { it.fileName == name }

    /**
     * What vPIC's `BodyClass` says, only where it is one of these plainly: "Sedan/Saloon" or
     * "Hatchback/Liftback/Notchback", its own spellings. Anything else ("Coupe", "Wagon", a blank) is "can't tell".
     */
    fun fromVpic(bodyClass: String?): BodyStyle? = when (bodyClass?.trim()?.lowercase()) {
        "sedan/saloon" -> BodyStyle.SEDAN
        "hatchback/liftback/notchback" -> BodyStyle.HATCHBACK
        else -> null
    }
}
