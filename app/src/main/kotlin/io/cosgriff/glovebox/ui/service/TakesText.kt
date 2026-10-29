package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.TakesItem
import io.cosgriff.glovebox.domain.TakesPart
import io.cosgriff.glovebox.domain.TakesRole
import io.cosgriff.glovebox.domain.VehicleSpecEntry
import io.cosgriff.glovebox.domain.WhatItTakes
import io.cosgriff.glovebox.domain.WorkHint
import io.cosgriff.glovebox.ui.AppText
import java.text.NumberFormat

/** "Engine oil" */
val TakesItem.label: String
    get() = AppText.res.getString(
        when (this) {
            TakesItem.OIL -> R.string.takes_oil
            TakesItem.TIRES -> R.string.takes_tires
            TakesItem.WIPERS -> R.string.takes_wipers
            TakesItem.TRANSMISSION -> R.string.takes_transmission
            TakesItem.DIFFERENTIAL -> R.string.takes_differential
            TakesItem.TRANSFER_CASE -> R.string.takes_transfer_case
            TakesItem.COOLANT -> R.string.takes_coolant
            TakesItem.BRAKE_FLUID -> R.string.takes_brake_fluid
            TakesItem.AIR_FILTERS -> R.string.takes_air_filters
            TakesItem.SPARK_PLUGS -> R.string.takes_spark_plugs
            TakesItem.BATTERY -> R.string.takes_battery
            TakesItem.BULBS -> R.string.takes_bulbs
        },
    )

/** "0W-20 · API SP · 6.6 qt · filter 04152-YZZA1" */
fun List<TakesPart>.text(): String = joinToString(" · ") { it.text() }

/** How [item] reads on one line; null when nothing of it is entered. */
fun WhatItTakes.line(item: TakesItem): String? = summary(item)?.text()

/** Every row with something entered, as (label, line): what an export prints. */
fun WhatItTakes.wordedLines(): List<Pair<String, String>> = lines().map { (item, parts) -> item.label to parts.text() }

/** The note beside a Log service line: what the vehicle takes, or "Every 5,000 mi / 6 mo". */
fun WorkHint.text(unit: DistanceUnit): String = when (this) {
    is WorkHint.Takes -> parts.text()
    is WorkHint.Every -> AppText.res.getString(R.string.interval_every, interval.text(unit, monthsOurs = monthsWorkedOut))
}

fun TakesPart.text(): String {
    val res = AppText.res
    return when (this) {
        is TakesPart.Entered -> text
        is TakesPart.Capacity -> res.getString(
            when (unit) {
                VehicleSpecEntry.Volume.QUARTS -> R.string.takes_quarts
                VehicleSpecEntry.Volume.LITRES -> R.string.takes_litres
            },
            // Whole or to one place, as it was entered: "6 qt", "6.6 qt".
            NumberFormat.getNumberInstance(AppText.locale).apply { maximumFractionDigits = 1 }.format(amount),
        )
        is TakesPart.Pressure -> res.getString(
            when (unit) {
                VehicleSpecEntry.Pressure.PSI -> R.string.takes_psi
                VehicleSpecEntry.Pressure.KPA -> R.string.takes_kpa
            },
            value.asNumber(),
        )
        is TakesPart.Inches -> res.getString(R.string.takes_inches, value.asNumber())
        is TakesPart.Cca -> res.getString(R.string.takes_cca, amps.asNumber())
        is TakesPart.Both -> res.getString(R.string.takes_both, first.text(), second.text())
        is TakesPart.For -> res.getString(
            when (role) {
                TakesRole.OIL_FILTER -> R.string.takes_role_filter
                TakesRole.TIRE_FRONT -> R.string.takes_role_tire_front
                TakesRole.TIRE_REAR -> R.string.takes_role_tire_rear
                TakesRole.PRESSURE_FRONT -> R.string.takes_role_pressure_front
                TakesRole.PRESSURE_REAR -> R.string.takes_role_pressure_rear
                TakesRole.PRESSURE_SPARE -> R.string.takes_role_spare
                TakesRole.WIPER_REAR -> R.string.takes_role_wiper_rear
                TakesRole.ENGINE_AIR -> R.string.takes_role_engine_air
                TakesRole.CABIN_AIR -> R.string.takes_role_cabin_air
                TakesRole.PLUG_GAP -> R.string.takes_role_gap
                TakesRole.BATTERY_GROUP -> R.string.takes_role_group
                TakesRole.BULB_LOW -> R.string.takes_role_low
                TakesRole.BULB_HIGH -> R.string.takes_role_high
                TakesRole.BULB_FOG -> R.string.takes_role_fog
                TakesRole.TRANSFER_CASE -> R.string.takes_role_transfer_case
            },
            part.text(),
        )
    }
}
