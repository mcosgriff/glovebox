package io.cosgriff.glovebox.domain

import io.cosgriff.glovebox.domain.VehicleSpecEntry.Pressure
import io.cosgriff.glovebox.domain.VehicleSpecEntry.Volume

/**
 * Everything entered under "What it takes" for one vehicle, already checked (VehicleSpecEntry). It is
 * all the owner's own entry: nothing here is looked up or suggested from the model.
 *
 * One place says how each item reads, so Vehicle details, Log service's hints and the export agree.
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
    /** How [item] reads on one line, or null when nothing of it is entered. */
    fun summary(item: TakesItem): String? = when (item) {
        TakesItem.OIL -> VehicleSpecEntry.oilSummary(oilGrade, oilApproval, oilCapacity, oilCapacityUnit, oilFilter)
        TakesItem.TIRES -> listOfNotNull(
            VehicleSpecEntry.tireSummary(tireSize, tireSizeRear),
            VehicleSpecEntry.pressureSummary(tirePressureFront, tirePressureRear, tirePressureSpare, tirePressureUnit),
        ).joinToString(" · ").ifEmpty { null }
        TakesItem.WIPERS -> VehicleSpecEntry.wiperSummary(wiperDriver, wiperPassenger, wiperRear, wiperNote)
        TakesItem.TRANSMISSION -> transmissionFluid
        TakesItem.DIFFERENTIAL -> differentialOil
        TakesItem.TRANSFER_CASE -> transferCaseOil
        TakesItem.COOLANT -> coolant
        TakesItem.BRAKE_FLUID -> brakeFluid
        TakesItem.AIR_FILTERS -> listOfNotNull(engineAirFilter?.let { "engine $it" }, cabinAirFilter?.let { "cabin $it" }).joinToString(" · ").ifEmpty { null }
        TakesItem.SPARK_PLUGS -> listOfNotNull(sparkPlug, sparkPlugGap?.let { "gap $it" }).joinToString(" · ").ifEmpty { null }
        TakesItem.BATTERY -> listOfNotNull(batteryGroup?.let { "group $it" }, batteryCca?.let { "$it CCA" }).joinToString(" · ").ifEmpty { null }
        TakesItem.BULBS -> listOfNotNull(bulbLow?.let { "low $it" }, bulbHigh?.let { "high $it" }, bulbFog?.let { "fog $it" }).joinToString(" · ").ifEmpty { null }
    }

    /** The part numbers on [item]'s row, for a long press to copy; null when it has none. */
    fun partNumbers(item: TakesItem): String? = when (item) {
        TakesItem.OIL -> oilFilter
        TakesItem.AIR_FILTERS -> listOfNotNull(engineAirFilter, cabinAirFilter).joinToString(", ").ifEmpty { null }
        TakesItem.SPARK_PLUGS -> sparkPlug
        TakesItem.WIPERS -> wiperNote
        else -> null
    }

    /** The rows to show: oil, tires and wipers always, the rest only once something is entered. */
    fun shown(): List<TakesItem> = TakesItem.entries.filter { it.always || summary(it) != null }

    /** The rows "Add more" offers: everything not shown yet. */
    fun addable(): List<TakesItem> = TakesItem.entries.filter { !it.always && summary(it) == null }

    /**
     * What to say beside a line on Log service, by catalog id: the fluid or part that job uses.
     */
    fun hint(catalogId: String): String? = when (catalogId) {
        "oil" -> VehicleSpecEntry.oilSummary(oilGrade, oilApproval, oilCapacity, oilCapacityUnit, oilFilter)
        "new-tires" -> VehicleSpecEntry.tireSummary(tireSize, tireSizeRear)
        "rotation" -> VehicleSpecEntry.pressureSummary(tirePressureFront, tirePressureRear, null, tirePressureUnit)
        "wipers" -> summary(TakesItem.WIPERS)
        "trans-fluid" -> transmissionFluid
        "diff-fluid" -> listOfNotNull(differentialOil, transferCaseOil?.let { "transfer case $it" }).joinToString(" · ").ifEmpty { null }
        "front-diff", "rear-diff", "front-diff-check", "rear-diff-check" -> differentialOil
        "transfer-case", "transfer-case-check" -> transferCaseOil
        "coolant" -> coolant
        "brake-fluid" -> brakeFluid
        "engine-air" -> engineAirFilter
        "cabin-air" -> cabinAirFilter
        "spark-plugs" -> summary(TakesItem.SPARK_PLUGS)
        "battery" -> summary(TakesItem.BATTERY)
        "bulbs" -> summary(TakesItem.BULBS)
        else -> null
    }

    /** Every row with something entered, as (label, line), in the card's order: what an export prints. */
    fun lines(): List<Pair<String, String>> = TakesItem.entries.mapNotNull { item -> summary(item)?.let { item.label to it } }
}

/** The rows of "What it takes", in the order they are listed. [always] rows show even when empty, as "Add". */
enum class TakesItem(val label: String, val always: Boolean = false) {
    OIL("Engine oil", always = true),
    TIRES("Tires", always = true),
    WIPERS("Wiper blades", always = true),
    TRANSMISSION("Transmission fluid"),
    DIFFERENTIAL("Differential oil"),
    TRANSFER_CASE("Transfer case oil"),
    COOLANT("Coolant"),
    BRAKE_FLUID("Brake fluid"),
    AIR_FILTERS("Air filters"),
    SPARK_PLUGS("Spark plugs"),
    BATTERY("Battery"),
    BULBS("Headlight bulbs"),
}
