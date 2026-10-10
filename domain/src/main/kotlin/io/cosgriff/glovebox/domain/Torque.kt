package io.cosgriff.glovebox.domain

import kotlin.math.roundToInt

enum class TorqueUnit(val symbol: String, val newtonMetersPerUnit: Double) {
    NEWTON_METER("N·m", 1.0),
    FOOT_POUND("ft-lb", 1.3558179483314004),
    INCH_POUND("in-lb", 0.1129848290276167),
}

/** Torque stored canonically in newton-metres; shown in whatever unit the user picks. */
@JvmInline
value class Torque(val newtonMeters: Double) {
    fun inUnit(unit: TorqueUnit): Double = newtonMeters / unit.newtonMetersPerUnit

    /** Display string, rounded the way a torque wrench is read. */
    fun format(unit: TorqueUnit): String {
        val v = inUnit(unit)
        val rounded = when (unit) {
            TorqueUnit.INCH_POUND -> (v / 5).roundToInt() * 5 // in-lb wrenches step in 5s
            else -> v.roundToInt()
        }
        return "$rounded ${unit.symbol}"
    }

    companion object {
        fun of(value: Double, unit: TorqueUnit) = Torque(value * unit.newtonMetersPerUnit)
    }
}
