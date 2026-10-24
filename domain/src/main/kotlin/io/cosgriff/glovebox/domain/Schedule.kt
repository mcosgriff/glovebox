package io.cosgriff.glovebox.domain

/**
 * One suggested maintenance item. [severe] is null when hard use (towing, off-road, dust,
 * short trips) doesn't change the interval.
 */
data class ScheduleTemplateItem(
    val key: String,
    /** `app` words it; the user's copy of the item keeps the name it had when it was added. */
    val name: TemplateName,
    val normal: Interval,
    val severe: Interval? = null,
) {
    fun interval(severeUse: Boolean): Interval = if (severeUse) severe ?: normal else normal

    val changesWhenSevere: Boolean get() = severe != null && severe != normal
}

/** The typical items' names. One key can have two ("Coolant" on an electric vehicle). */
enum class TemplateName {
    OIL, ROTATION, ENGINE_AIR, CABIN_AIR, BRAKE_INSPECTION, BRAKE_FLUID, TRANSMISSION_FLUID,
    DIFF_AND_TRANSFER, DIFF, ENGINE_COOLANT, COOLANT, SPARK_PLUGS,
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
        if (engine) add(ScheduleTemplateItem("oil", TemplateName.OIL, Interval(7_500, 12), Interval(5_000, 6)))
        add(ScheduleTemplateItem("rotation", TemplateName.ROTATION, Interval(5_000, 6)))
        if (engine) add(ScheduleTemplateItem("engine-air", TemplateName.ENGINE_AIR, Interval(30_000, null), Interval(15_000, null)))
        add(ScheduleTemplateItem("cabin-air", TemplateName.CABIN_AIR, Interval(15_000, 12)))
        add(ScheduleTemplateItem("brake-inspect", TemplateName.BRAKE_INSPECTION, Interval(15_000, 12)))
        add(ScheduleTemplateItem("brake-fluid", TemplateName.BRAKE_FLUID, Interval(null, 36)))
        add(ScheduleTemplateItem("trans-fluid", TemplateName.TRANSMISSION_FLUID, Interval(60_000, null), Interval(30_000, null)))
        when (templateDrive(drivetrain)) {
            TemplateDrive.FOUR_OR_ALL_WHEEL -> add(
                ScheduleTemplateItem("diff-fluid", TemplateName.DIFF_AND_TRANSFER, Interval(60_000, null), Interval(30_000, null)),
            )
            TemplateDrive.REAR -> add(
                ScheduleTemplateItem("diff-fluid", TemplateName.DIFF, Interval(60_000, null), Interval(30_000, null)),
            )
            TemplateDrive.OTHER -> Unit
        }
        add(ScheduleTemplateItem("coolant", if (engine) TemplateName.ENGINE_COOLANT else TemplateName.COOLANT, Interval(100_000, 120)))
        if (engine) add(ScheduleTemplateItem("spark-plugs", TemplateName.SPARK_PLUGS, Interval(100_000, null)))
    }

    /**
     * The typical items a vehicle's schedule doesn't have yet, tracked or not. [haveKeys] are the
     * template keys of its items; the user's own items have none and never count as a match.
     */
    fun missing(drivetrain: String?, fuel: String?, unit: DistanceUnit, haveKeys: Collection<String?>): List<ScheduleTemplateItem> =
        generic(drivetrain, fuel, unit).filter { it.key !in haveKeys }

    /** vPIC says "Electric" for battery-electric cars; hybrids report their combustion fuel as primary. */
    fun isElectric(fuel: String?): Boolean = fuel?.trim().equals("Electric", ignoreCase = true)

    private enum class TemplateDrive { FOUR_OR_ALL_WHEEL, REAR, OTHER }

    private fun templateDrive(drivetrain: String?): TemplateDrive {
        val d = drivetrain?.uppercase().orEmpty()
        return when {
            "4WD" in d || "AWD" in d || "4X4" in d -> TemplateDrive.FOUR_OR_ALL_WHEEL
            "RWD" in d -> TemplateDrive.REAR
            else -> TemplateDrive.OTHER
        }
    }
}

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
    private fun part(text: String): Part? =
        if (text.none { it != ',' && !it.isWhitespace() }) Part(null) else WholeNumber.parse(text, min = 1)?.let(::Part)
}

/**
 * The interval a schedule item counts by. Its E85 interval while the owner has said the vehicle runs mostly
 * on E85 and the item has one (a flex-fuel guide's oil change, #33); otherwise its severe interval while
 * severe use is on, part by part, falling back to the normal one; otherwise the normal one. Null when the
 * item has no usable interval. **Every place a reminder is counted from goes through this.**
 */
object ActiveInterval {
    fun of(normal: Interval?, severe: Interval?, useSevere: Boolean, e85: Interval? = null, useE85: Boolean = false): Interval? {
        if (useE85 && e85 != null) return e85
        val miles = if (useSevere) severe?.miles ?: normal?.miles else normal?.miles
        val months = if (useSevere) severe?.months ?: normal?.months else normal?.months
        return if (miles == null && months == null) null else Interval(miles, months)
    }
}
