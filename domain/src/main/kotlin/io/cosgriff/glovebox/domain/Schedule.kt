package io.cosgriff.glovebox.domain

import java.util.Locale

/**
 * One suggested maintenance item. [severe] is null when hard use (towing, off-road, dust,
 * short trips) doesn't change the interval.
 */
data class ScheduleTemplateItem(
    val key: String,
    val name: String,
    val normal: Interval,
    val severe: Interval? = null,
) {
    fun interval(severeUse: Boolean): Interval = if (severeUse) severe ?: normal else normal

    val changesWhenSevere: Boolean get() = severe != null && severe != normal
}

object ScheduleTemplates {
    /**
     * A generic starting schedule. These are common intervals, NOT manufacturer data — the UI must
     * say so and point the user at their owner's manual. Every value is editable.
     *
     * [drivetrain] is the vPIC drive type (e.g. "4WD/4-Wheel Drive/4x4"); it only decides whether
     * differential / transfer case fluid is suggested. [fuel] is vPIC's primary fuel type; a
     * battery-electric vehicle has no engine oil, engine air filter or spark plugs to suggest.
     */
    fun generic(drivetrain: String?, fuel: String?, unit: DistanceUnit): List<ScheduleTemplateItem> =
        inMiles(drivetrain, fuel).map { it.copy(normal = it.normal.scheduledIn(unit), severe = it.severe?.scheduledIn(unit)) }

    /** The list is kept in miles; a kilometre vehicle gets the figures a manual for that market would print (5,000 mi as 8,000 km). */
    private fun Interval.scheduledIn(unit: DistanceUnit) = copy(miles = miles?.let(unit::scheduled))

    private fun inMiles(drivetrain: String?, fuel: String?): List<ScheduleTemplateItem> = buildList {
        val engine = !isElectric(fuel)
        if (engine) add(ScheduleTemplateItem("oil", "Engine oil & filter", Interval(7_500, 12), Interval(5_000, 6)))
        add(ScheduleTemplateItem("rotation", "Tire rotation", Interval(5_000, 6)))
        if (engine) add(ScheduleTemplateItem("engine-air", "Engine air filter", Interval(30_000, null), Interval(15_000, null)))
        add(ScheduleTemplateItem("cabin-air", "Cabin air filter", Interval(15_000, 12)))
        add(ScheduleTemplateItem("brake-inspect", "Brake inspection", Interval(15_000, 12)))
        add(ScheduleTemplateItem("brake-fluid", "Brake fluid", Interval(null, 36)))
        add(ScheduleTemplateItem("trans-fluid", "Transmission fluid", Interval(60_000, null), Interval(30_000, null)))
        when (driveKind(drivetrain)) {
            DriveKind.FOUR_OR_ALL_WHEEL -> add(
                ScheduleTemplateItem("diff-fluid", "Differential & transfer case fluid", Interval(60_000, null), Interval(30_000, null)),
            )
            DriveKind.REAR -> add(
                ScheduleTemplateItem("diff-fluid", "Differential fluid", Interval(60_000, null), Interval(30_000, null)),
            )
            DriveKind.OTHER -> Unit
        }
        add(ScheduleTemplateItem("coolant", if (engine) "Engine coolant" else "Coolant", Interval(100_000, 120)))
        if (engine) add(ScheduleTemplateItem("spark-plugs", "Spark plugs", Interval(100_000, null)))
    }

    /**
     * The typical items a vehicle's schedule doesn't have yet, tracked or not. [haveKeys] are the
     * template keys of its items; the user's own items have none and never count as a match.
     */
    fun missing(drivetrain: String?, fuel: String?, unit: DistanceUnit, haveKeys: Collection<String?>): List<ScheduleTemplateItem> =
        generic(drivetrain, fuel, unit).filter { it.key !in haveKeys }

    /** vPIC says "Electric" for battery-electric cars; hybrids report their combustion fuel as primary. */
    fun isElectric(fuel: String?): Boolean = fuel?.trim().equals("Electric", ignoreCase = true)

    private enum class DriveKind { FOUR_OR_ALL_WHEEL, REAR, OTHER }

    private fun driveKind(drivetrain: String?): DriveKind {
        val d = drivetrain?.uppercase().orEmpty()
        return when {
            "4WD" in d || "AWD" in d || "4X4" in d -> DriveKind.FOUR_OR_ALL_WHEEL
            "RWD" in d -> DriveKind.REAR
            else -> DriveKind.OTHER
        }
    }
}

/** "7,500 mi / 12 mo", "30,000 mi" or "36 mo". [unit] is the vehicle's, which is what the interval is in. */
fun Interval.label(unit: DistanceUnit): String = listOfNotNull(
    miles?.let(unit::format),
    months?.let { "$it mo" },
).joinToString(" / ")

/** "First at 100,000 mi, then every 50,000 mi / 60 mo" for a job with a first time of its own; the plain label otherwise. */
fun Interval.label(unit: DistanceUnit, firstAt: Int?): String =
    if (firstAt == null) label(unit) else "First at ${unit.format(firstAt)}, then every ${label(unit)}"

object IntervalInput {
    /**
     * Parses what the user typed in the miles / months boxes. Blank means "doesn't apply";
     * thousands separators are fine. Returns null when the entry can't be used: both blank,
     * zero, negative, or not a whole number.
     */
    fun parse(milesText: String, monthsText: String): Interval? {
        val miles = part(milesText) ?: return null
        val months = part(monthsText) ?: return null
        if (miles.value == null && months.value == null) return null
        return Interval(miles.value, months.value)
    }

    private class Part(val value: Int?)

    /** Null = invalid; Part(null) = left blank. */
    private fun part(text: String): Part? {
        val cleaned = text.filterNot { it == ',' || it.isWhitespace() }
        if (cleaned.isEmpty()) return Part(null)
        if (!cleaned.all(Char::isDigit)) return null
        val n = cleaned.toIntOrNull() ?: return null
        return if (n > 0) Part(n) else null
    }
}
