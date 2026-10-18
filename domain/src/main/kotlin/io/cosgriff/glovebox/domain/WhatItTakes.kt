package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.VehicleSpecEntry.Pressure
import io.cosgriff.glovebox.domain.VehicleSpecEntry.Volume

/**
 * Everything entered under "What it takes" for one vehicle, already checked (VehicleSpecEntry). It is
 * all the owner's own entry: nothing here is looked up or suggested from the model.
 *
 * One place says what each item's line is made of, so Vehicle details, Log service's hints and the export agree.
 */
data class WhatItTakes(
    val oilGrade: String? = null,
    val oilApproval: String? = null,
    val oilCapacity: Double? = null,
    val oilCapacityUnit: Volume? = null,
    val oilFilter: String? = null,
    val tireSize: String? = null,
    val tireSizeRear: String? = null,
    val tirePressureFront: Int? = null,
    val tirePressureRear: Int? = null,
    val tirePressureSpare: Int? = null,
    val tirePressureUnit: Pressure? = null,
    val wiperDriver: Int? = null,
    val wiperPassenger: Int? = null,
    val wiperRear: Int? = null,
    val wiperNote: String? = null,
    val transmissionFluid: String? = null,
    val differentialOil: String? = null,
    val transferCaseOil: String? = null,
    val coolant: String? = null,
    val brakeFluid: String? = null,
    val engineAirFilter: String? = null,
    val cabinAirFilter: String? = null,
    val sparkPlug: String? = null,
    val sparkPlugGap: String? = null,
    val batteryGroup: String? = null,
    val batteryCca: Int? = null,
    val bulbLow: String? = null,
    val bulbHigh: String? = null,
    val bulbFog: String? = null,
) {
    /** How [item] reads on one line, as parts that `app` words and joins; null when nothing of it is entered. */
    fun summary(item: TakesItem): List<TakesPart>? = when (item) {
        TakesItem.OIL -> VehicleSpecEntry.oilParts(oilGrade, oilApproval, oilCapacity, oilCapacityUnit, oilFilter)
        TakesItem.TIRES -> (
            VehicleSpecEntry.tireParts(tireSize, tireSizeRear).orEmpty() +
                VehicleSpecEntry.pressureParts(tirePressureFront, tirePressureRear, tirePressureSpare, tirePressureUnit).orEmpty()
            ).ifEmpty { null }
        TakesItem.WIPERS -> VehicleSpecEntry.wiperParts(wiperDriver, wiperPassenger, wiperRear, wiperNote)
        TakesItem.TRANSMISSION -> entered(transmissionFluid)
        TakesItem.DIFFERENTIAL -> entered(differentialOil)
        TakesItem.TRANSFER_CASE -> entered(transferCaseOil)
        TakesItem.COOLANT -> entered(coolant)
        TakesItem.BRAKE_FLUID -> entered(brakeFluid)
        TakesItem.AIR_FILTERS -> parts(TakesRole.ENGINE_AIR to engineAirFilter, TakesRole.CABIN_AIR to cabinAirFilter)
        TakesItem.SPARK_PLUGS -> listOfNotNull(sparkPlug?.let(TakesPart::Entered), named(TakesRole.PLUG_GAP, sparkPlugGap)).ifEmpty { null }
        TakesItem.BATTERY -> listOfNotNull(named(TakesRole.BATTERY_GROUP, batteryGroup), batteryCca?.let(TakesPart::Cca)).ifEmpty { null }
        TakesItem.BULBS -> parts(TakesRole.BULB_LOW to bulbLow, TakesRole.BULB_HIGH to bulbHigh, TakesRole.BULB_FOG to bulbFog)
    }

    /** The part numbers on [item]'s row, for a long press to copy; null when it has none. */
    fun partNumbers(item: TakesItem): String? = when (item) {
        TakesItem.OIL -> oilFilter
        TakesItem.AIR_FILTERS -> listOfNotNull(engineAirFilter, cabinAirFilter).joinToString(", ").ifEmpty { null }
        TakesItem.SPARK_PLUGS -> sparkPlug
        TakesItem.WIPERS -> wiperNote
        else -> null
    }

    /** The rows on Vehicle details itself: oil, tires and wipers, entered or not. */
    fun basics(): List<TakesItem> = TakesItem.entries.filter { it.always }

    /** The rest (fluids, filters, plugs, battery, bulbs) with something entered: the cards on their own page. */
    fun entered(): List<TakesItem> = TakesItem.entries.filter { !it.always && summary(it) != null }

    /** The rest with nothing entered yet, which that page offers to add. */
    fun notEntered(): List<TakesItem> = TakesItem.entries.filter { !it.always && summary(it) == null }

    /**
     * What to say beside a line on Log service, by catalog id: the fluid or part that job uses.
     */
    fun hint(catalogId: String): List<TakesPart>? = when (catalogId) {
        "oil" -> summary(TakesItem.OIL)
        "new-tires" -> VehicleSpecEntry.tireParts(tireSize, tireSizeRear)
        "rotation" -> VehicleSpecEntry.pressureParts(tirePressureFront, tirePressureRear, null, tirePressureUnit)
        "wipers" -> summary(TakesItem.WIPERS)
        "trans-fluid" -> entered(transmissionFluid)
        "diff-fluid" -> listOfNotNull(differentialOil?.let(TakesPart::Entered), named(TakesRole.TRANSFER_CASE, transferCaseOil)).ifEmpty { null }
        "front-diff", "rear-diff", "front-diff-check", "rear-diff-check" -> entered(differentialOil)
        "transfer-case", "transfer-case-check" -> entered(transferCaseOil)
        "coolant" -> entered(coolant)
        "brake-fluid", "brake-fluid-check" -> entered(brakeFluid)
        "engine-air" -> entered(engineAirFilter)
        "cabin-air" -> entered(cabinAirFilter)
        "spark-plugs" -> summary(TakesItem.SPARK_PLUGS)
        "battery" -> summary(TakesItem.BATTERY)
        "bulbs" -> summary(TakesItem.BULBS)
        else -> null
    }

    /** Every row with something entered, with its line, in the card's order: what an export prints. */
    fun lines(): List<Pair<TakesItem, List<TakesPart>>> = TakesItem.entries.mapNotNull { item -> summary(item)?.let { item to it } }

    private fun entered(text: String?): List<TakesPart>? = text?.let { listOf(TakesPart.Entered(it)) }

    private fun named(role: TakesRole, text: String?): TakesPart? = text?.let { TakesPart.For(role, TakesPart.Entered(it)) }

    private fun parts(vararg named: Pair<TakesRole, String?>): List<TakesPart>? = named.mapNotNull { (role, text) -> named(role, text) }.ifEmpty { null }
}

/**
 * One piece of a "What it takes" line; `app` words each and joins them with " · ". The figures and
 * entries are the owner's own; only the words around them are the app's.
 */
sealed interface TakesPart {
    /** As entered and checked: a grade, an approval, a fluid, a tire size, a part number, a note. */
    data class Entered(val text: String) : TakesPart

    /** An oil capacity: "6.6 qt". */
    data class Capacity(val amount: Double, val unit: VehicleSpecEntry.Volume) : TakesPart

    /** A cold tire pressure: "32 psi". */
    data class Pressure(val value: Int, val unit: VehicleSpecEntry.Pressure) : TakesPart

    /** A wiper blade's length in whole inches: 26". */
    data class Inches(val value: Int) : TakesPart

    /** A battery's cold cranking amps: "650 CCA". */
    data class Cca(val amps: Int) : TakesPart

    /** Two of a kind side by side: "26\" / 17\"", "35 psi front / 38 psi rear". */
    data class Both(val first: TakesPart, val second: TakesPart) : TakesPart

    /** A part said with what it is: "filter 04152-YZZA1", "Rear 255/35R18", "38 psi rear". */
    data class For(val role: TakesRole, val part: TakesPart) : TakesPart
}

/** What a [TakesPart.For] is; each has its own wording in `app`. */
enum class TakesRole {
    OIL_FILTER, TIRE_FRONT, TIRE_REAR, PRESSURE_FRONT, PRESSURE_REAR, PRESSURE_SPARE, WIPER_REAR,
    ENGINE_AIR, CABIN_AIR, PLUG_GAP, BATTERY_GROUP, BULB_LOW, BULB_HIGH, BULB_FOG, TRANSFER_CASE,
}

/** The rows of "What it takes", in the order they are listed; `app` names them. [always] rows show even when empty, as "Add". */
enum class TakesItem(val always: Boolean = false) {
    OIL(always = true),
    TIRES(always = true),
    WIPERS(always = true),
    TRANSMISSION,
    DIFFERENTIAL,
    TRANSFER_CASE,
    COOLANT,
    BRAKE_FLUID,
    AIR_FILTERS,
    SPARK_PLUGS,
    BATTERY,
    BULBS,
}
