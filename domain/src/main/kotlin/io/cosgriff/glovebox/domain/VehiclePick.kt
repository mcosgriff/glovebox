package io.cosgriff.glovebox.domain

/** Choosing a year, make and model by hand, for a vehicle added without a VIN. */
object VehiclePick {
    /** Old enough for any car someone still maintains; a VIN-less vehicle is often an old one. */
    const val FIRST_YEAR = 1900

    val drivetrains: List<String> = listOf("FWD", "RWD", "AWD", "4WD")

    /** Newest first, starting with next model year. */
    fun years(thisYear: Int): List<Int> = (thisYear + 1 downTo FIRST_YEAR).toList()

    fun parseYear(text: String, thisYear: Int): Int? =
        text.trim().toIntOrNull()?.takeIf { it in FIRST_YEAR..thisYear + 1 }

    /** [options] matching what was typed: names that start with it first, then names that contain it. */
    fun filter(options: List<String>, query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return options
        val (starts, rest) = options.filter { q in it.lowercase() }.partition { it.lowercase().startsWith(q) }
        return starts + rest
    }

    /**
     * What was typed, as a value of its own: null when it is blank or already one of [options],
     * in which case the option should be picked instead.
     */
    fun custom(query: String, options: List<String>): String? =
        VehicleText.clean(query, VehicleText.MAX_NICKNAME)?.takeIf { typed -> options.none { it.equals(typed, ignoreCase = true) } }

    /** Tidies NHTSA's lists: no blanks, no repeats that differ only by case, in alphabetical order. */
    fun tidy(names: List<String>): List<String> =
        names.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
}
