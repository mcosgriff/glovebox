package io.cosgriff.glovebox.domain

/**
 * What a vehicle's drive type says, as vPIC spells it ("4WD/4-Wheel Drive/4x4", "AWD/All-Wheel
 * Drive", "4x2") or as it was picked when the vehicle was added (FWD, RWD, AWD, 4WD).
 *
 * **Four-wheel drive and all-wheel drive are different systems and are kept apart here.** A maker's
 * guide marks its jobs for one or the other, and a list says which of its jobs are for which; nothing
 * marked for one is ever assumed to be for the other.
 */
enum class DriveKind { FOUR_WHEEL, ALL_WHEEL, TWO_WHEEL, UNKNOWN }

/** What a vehicle's transmission says, as vPIC spells it ("Manual/Standard", "Automatic") or as it was typed. */
object Transmissions {
    /**
     * True only when the gearbox is known to be a manual with a clutch pedal. vPIC calls a
     * dual-clutch gearbox "Automatic" or "Dual-Clutch Transmission (DCT)", and an "Automated Manual"
     * is not a manual in this sense; nothing on record is "can't tell", never "manual".
     */
    fun isManual(transmission: String?): Boolean {
        val t = transmission?.uppercase().orEmpty()
        return "MANUAL" in t && "AUTOMATED" !in t
    }
}

object Drivetrains {
    /** [DriveKind.UNKNOWN] is nothing on record, or something else (a 6x4 truck): "can't tell", never "two". */
    fun kind(drivetrain: String?): DriveKind {
        val d = drivetrain?.uppercase().orEmpty()
        return when {
            "4WD" in d || "4X4" in d -> DriveKind.FOUR_WHEEL
            "AWD" in d -> DriveKind.ALL_WHEEL
            "FWD" in d || "RWD" in d || "2WD" in d || "4X2" in d -> DriveKind.TWO_WHEEL
            else -> DriveKind.UNKNOWN
        }
    }
}
