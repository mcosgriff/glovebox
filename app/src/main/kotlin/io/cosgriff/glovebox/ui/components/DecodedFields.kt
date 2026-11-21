package io.cosgriff.glovebox.ui.components

import androidx.annotation.StringRes
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.nhtsa.DecodedField

/** What each decoded field is called on "Is this your vehicle?" and in the VIN lookup's dialog. */
@StringRes
internal fun DecodedField.label(): Int = when (this) {
    DecodedField.BODY -> R.string.add_vehicle_body_class
    DecodedField.DRIVE -> R.string.add_vehicle_drive_type
    DecodedField.ENGINE -> R.string.add_vehicle_engine
    DecodedField.ENGINE_CODE -> R.string.add_vehicle_engine_code
    DecodedField.FUEL -> R.string.add_vehicle_fuel
    DecodedField.TRANSMISSION -> R.string.add_vehicle_transmission
    DecodedField.TPMS -> R.string.add_vehicle_tpms
    DecodedField.SERIES -> R.string.add_vehicle_series
}
