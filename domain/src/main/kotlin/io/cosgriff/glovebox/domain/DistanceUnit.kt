package io.cosgriff.glovebox.domain

import kotlin.math.roundToInt

/**
 * What a vehicle's odometer counts in. **It belongs to the vehicle, not to the app:** every reading,
 * interval, snooze point and milestone of a vehicle is stored in that vehicle's unit, exactly as it
 * was typed, and everything worked out from them is in the same unit. So nothing is converted when
 * it is shown, and a figure typed in always reads back the same. Almost all of `:domain` neither
 * knows nor cares which unit its numbers are in; the unit is needed where a number is put into
 * words, and in the few places a figure comes from outside the vehicle (below).
 *
 * A number is converted in only three situations, and each says which of two conversions it uses:
 * - the vehicle's unit is changed (`DistanceConversion`),
 * - an import file, which is always in miles, is read into or written from a kilometre vehicle,
 * - one of Glovebox's own figures in miles (a typical interval, the lead distance, a snooze step)
 *   is used for a kilometre vehicle.
 */
/** [symbol] and [plural] are how an import file's error messages name it (`app` has its own words). */
enum class DistanceUnit(val symbol: String, val plural: String) {
    MILES("mi", "miles"),
    KILOMETERS("km", "kilometres"),
    ;

    /**
     * A **measurement** in miles in this unit, as exactly as a whole number allows: an odometer
     * reading, a snooze point, a cluster offset. 100 mi is 161 km.
     */
    fun measured(miles: Int): Int = if (this == MILES) miles else (miles * KM_PER_MILE).roundToInt()

    /** The other way: a measurement in this unit, in miles. */
    fun measuredInMiles(distance: Int): Int = if (this == MILES) distance else (distance / KM_PER_MILE).roundToInt()

    /**
     * A **schedule figure** in miles in this unit, the way a manual printed for both markets gives
     * it: 5,000 mi is 8,000 km, 30,000 mi is 48,000 km. Manuals use 1.6, not 1.609, so that the
     * figures stay round; so does this, for intervals, milestones and Glovebox's own distances.
     */
    fun scheduled(miles: Int): Int = if (this == MILES) miles else (miles * SCHEDULE_KM_PER_MILE).roundToInt()

    /** The other way: a schedule figure in this unit, in miles. 8,000 km is 5,000 mi. */
    fun scheduledInMiles(distance: Int): Int = if (this == MILES) distance else (distance / SCHEDULE_KM_PER_MILE).roundToInt()

    companion object {
        const val KM_PER_MILE = 1.609344
        const val SCHEDULE_KM_PER_MILE = 1.6

        /** Whatever is stored; anything unrecognised is miles, which is what every vehicle was before units existed. */
        fun of(name: String?): DistanceUnit = entries.firstOrNull { it.name == name } ?: MILES
    }
}

/** Which of the two conversions a figure takes when a vehicle's unit changes. */
enum class DistanceKind { MEASURED, SCHEDULED }

/** Moving a vehicle's figures from one unit to the other, once, when its unit is changed. */
object DistanceConversion {
    /** [value] in [from], in [to]. The same unit is the same number. */
    fun convert(value: Int, from: DistanceUnit, to: DistanceUnit, kind: DistanceKind): Int = when {
        from == to -> value
        kind == DistanceKind.MEASURED -> to.measured(from.measuredInMiles(value))
        else -> to.scheduled(from.scheduledInMiles(value))
    }

    /**
     * Milestone mileages in the new unit, in the order given (lowest first). No two may land on
     * the same figure, since a table has one milestone per mileage; one that would is moved up by one.
     */
    fun milestones(ascending: List<Int>, from: DistanceUnit, to: DistanceUnit): List<Int> {
        var floor = 0
        return ascending.map { at -> maxOf(convert(at, from, to, DistanceKind.SCHEDULED), floor + 1).also { floor = it } }
    }

    /**
     * A milestone table in the new unit: its mileages ([milestones]) and the length of the cycle it [repeats] over,
     * if it does. **A cycle is never shorter than its table**, so one the rounding would leave short takes the
     * last milestone's figure. Used for an import file and for a vehicle whose unit is changed alike.
     */
    fun table(ascending: List<Int>, repeats: Int?, from: DistanceUnit, to: DistanceUnit): Pair<List<Int>, Int?> {
        val at = milestones(ascending, from, to)
        return at to repeats?.let { maxOf(convert(it, from, to, DistanceKind.SCHEDULED), at.lastOrNull() ?: 0) }
    }
}

/**
 * How early a reminder starts for a vehicle in [unit]. The setting is one for the app and is kept as
 * typed, in [from], the unit new vehicles get; for a vehicle in the other unit it is converted the
 * way a schedule figure is (500 mi as 800 km).
 */
fun LeadTime.forUnit(unit: DistanceUnit, from: DistanceUnit): LeadTime =
    copy(miles = DistanceConversion.convert(miles, from, unit, DistanceKind.SCHEDULED))
