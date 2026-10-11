package io.cosgriff.glovebox.domain

import java.util.Locale

/** How torque specs are grouped. Stored by [name] in `torque_specs.category`. */
enum class TorqueCategory(val label: String) {
    ENGINE("Engine & oil"),
    WHEELS("Wheels & brakes"),
    DRIVELINE("Driveline"),
    SUSPENSION("Suspension & steering"),
    OTHER("Other"),
    ;

    companion object {
        /** Anything unrecognised (an older or hand-edited value) lands in Other rather than vanishing. */
        fun of(stored: String): TorqueCategory = entries.firstOrNull { it.name == stored } ?: OTHER
    }
}

/** Where a value came from. Stored by [name] in `torque_specs.source`. */
enum class TorqueSource {
    /** The user copied it from the factory service manual. */
    FACTORY,

    /** Anything else the user entered. */
    USER,
    ;

    companion object {
        fun of(stored: String): TorqueSource = entries.firstOrNull { it.name == stored } ?: USER
    }
}

/** A fastener most vehicles have, offered as a named row with nothing filled in. */
data class TorqueTemplate(val name: String, val category: TorqueCategory)

object TorqueSpecs {
    /** No wrench a car owner uses goes higher; catches a slipped decimal or the wrong unit. */
    const val MAX_NEWTON_METERS = 1_000.0

    /**
     * Names only. There is no source for factory torque values, so these start empty and the user
     * fills them in from the service manual.
     */
    val common: List<TorqueTemplate> = listOf(
        TorqueTemplate("Oil drain plug", TorqueCategory.ENGINE),
        TorqueTemplate("Oil filter", TorqueCategory.ENGINE),
        TorqueTemplate("Spark plugs", TorqueCategory.ENGINE),
        TorqueTemplate("Lug nuts", TorqueCategory.WHEELS),
        TorqueTemplate("Front caliper bracket bolts", TorqueCategory.WHEELS),
        TorqueTemplate("Rear caliper bracket bolts", TorqueCategory.WHEELS),
        TorqueTemplate("Transmission drain plug", TorqueCategory.DRIVELINE),
    )

    /** What was typed, in [unit]; null unless it is a positive number a torque wrench could be set to. */
    fun parse(text: String, unit: TorqueUnit): Torque? {
        val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (value <= 0 || value.isNaN()) return null
        return Torque.of(value, unit).takeIf { it.newtonMeters <= MAX_NEWTON_METERS }
    }

    /** The value to put in the edit field: one decimal at most, so 83 ft-lb comes back as "83". */
    fun editText(torque: Torque, unit: TorqueUnit): String =
        String.format(Locale.US, "%.1f", torque.inUnit(unit)).removeSuffix(".0")

    /** Search across everything a row shows. A blank query matches all. */
    fun matches(query: String, vararg fields: String?): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val text = fields.filterNotNull().joinToString(" ").lowercase()
        return words.all { it in text }
    }
}
