package io.cosgriff.glovebox.domain

import kotlin.math.roundToInt

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
 * item has no usable interval. **Every place a reminder is counted from goes through this**, by way of [counted].
 */
object ActiveInterval {
    fun of(normal: Interval?, severe: Interval?, useSevere: Boolean, e85: Interval? = null, useE85: Boolean = false): Interval? {
        if (useE85 && e85 != null) return e85
        val miles = if (useSevere) severe?.miles ?: normal?.miles else normal?.miles
        val months = if (useSevere) severe?.months ?: normal?.months else normal?.months
        return if (miles == null && months == null) null else Interval(miles, months)
    }

    /**
     * [of], with the months Glovebox works out for an oil change that has none ([OilMonths]). [oil] is whether the item
     * is the oil change ([OilMonths.isOil]); [unit] is the vehicle's, which the interval is in; [own] is the owner's own
     * oil change interval for the vehicle ([OwnOil]), which an oil change counts by in place of every figure it has.
     * **A reminder is counted from this**; [of] alone is the item's own figures, as saved.
     */
    fun counted(
        normal: Interval?,
        severe: Interval?,
        useSevere: Boolean,
        e85: Interval? = null,
        useE85: Boolean = false,
        oil: Boolean,
        unit: DistanceUnit,
        own: Interval? = null,
    ): CountedInterval? = OwnOil.counted(of(normal, severe, useSevere, e85, useE85), own, oil, unit)
}

/**
 * The interval a reminder counts by; whether its months are Glovebox's ([OilMonths]) rather than the item's own or the
 * owner's; and whether it is the owner's own oil change interval ([OwnOil]) rather than the item's figures.
 */
data class CountedInterval(val interval: Interval, val monthsWorkedOut: Boolean = false, val owners: Boolean = false)

/**
 * The owner's own oil change interval for one vehicle (#517), set from Vehicle details and stored on the vehicle in its
 * own unit, as typed. **Once set, every oil change on the vehicle counts by it, whether Severe (or E85) is on or off**;
 * "Use the maker's" clears it, and the maker's (or typical) figure counts again, with its worked-out months. Its months
 * follow its miles at [OilMonths.MILES_A_MONTH] a month (stored as none) until the owner types months; then they stay
 * as typed, whatever the miles. Glovebox never suggests a shorter interval as the vehicle ages: the owner decides.
 */
object OwnOil {
    /** The owner's interval as stored: [distance] and [months] as typed, [months] null while they follow the miles. Null when it isn't set. */
    fun of(distance: Int?, months: Int?): Interval? = if (distance == null && months == null) null else Interval(distance, months)

    /** What the sheet's boxes hold, as it would be saved; null when it can't be (both blank, zero, not a whole number). */
    fun parse(distanceText: String, monthsText: String): Interval? = IntervalInput.parse(distanceText, monthsText)

    /** The months the owner's interval counts by: [typed] when there are some, else worked out from [distance]. */
    fun months(distance: Int?, typed: Int?, unit: DistanceUnit): Int? = typed ?: distance?.let { OilMonths.forDistance(it, unit) }

    /**
     * What an item counts by, given [item]'s own interval in force: [own] when the item is the oil change ([oil]) and the
     * owner has set one, else [item]; either way with the months Glovebox works out when it has none.
     */
    fun counted(item: Interval?, own: Interval?, oil: Boolean, unit: DistanceUnit): CountedInterval? {
        val owners = oil && own != null
        val interval = (if (owners) own else item) ?: return null
        return OilMonths.counted(interval, oil, unit).copy(owners = owners)
    }
}

/**
 * The one time limit Glovebox works out (#517, ADR-0015). An oil change given in miles alone gets a month for every
 * [MILES_A_MONTH] miles, rounded, never more than [MOST_MONTHS]: 10,000 mi is 12 months, 7,500 is 9, 5,000 is 6. Oil
 * ages in the sump whether the car is driven or not, and a maker that prints no months still means some. It is worked
 * out each time reminders are read and never stored, so editing the miles moves it, and months the item has (the
 * maker's or the owner's) are always left as they are. **Only the oil change**; every other job counts by what it has.
 */
object OilMonths {
    const val MILES_A_MONTH = 833
    const val MOST_MONTHS = 12

    /**
     * The months for an oil change every [distance], in [unit]. A kilometre figure is taken as the schedule figure it
     * is (16,000 km is a manual's 10,000 mi, `DistanceUnit.scheduledInMiles`), so it gets the months its miles would.
     * Also what an owner's own interval in miles is given until the owner types months.
     */
    fun forDistance(distance: Int, unit: DistanceUnit): Int =
        (unit.scheduledInMiles(distance).toDouble() / MILES_A_MONTH).roundToInt().coerceIn(1, MOST_MONTHS)

    /** The typical oil change, or one of the owner's set to that catalog line: the items `ServiceLinks.own` gives line `oil`. */
    fun isOil(templateKey: String?, service: String?): Boolean =
        if (templateKey != null) templateKey == Snoozing.OIL_TEMPLATE_KEY else service == Snoozing.OIL_TEMPLATE_KEY

    /** [interval] with its worked-out months when it is an oil change in miles alone; as it is otherwise. */
    fun counted(interval: Interval, oil: Boolean, unit: DistanceUnit): CountedInterval {
        val miles = interval.miles
        return if (oil && miles != null && interval.months == null) {
            CountedInterval(interval.copy(months = forDistance(miles, unit)), monthsWorkedOut = true)
        } else {
            CountedInterval(interval)
        }
    }
}
