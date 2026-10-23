package io.cosgriff.glovebox.ui.components

import androidx.annotation.StringRes
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Powertrain

/** How a powertrain is named on screen, wherever a vehicle's is shown or picked. */
@StringRes
internal fun Powertrain.label(): Int = when (this) {
    Powertrain.GAS -> R.string.powertrain_gas
    Powertrain.HYBRID -> R.string.powertrain_hybrid
    Powertrain.PLUG_IN -> R.string.powertrain_plug_in
    Powertrain.FLEX_FUEL -> R.string.powertrain_flex_fuel
    Powertrain.ELECTRIC -> R.string.powertrain_electric
}
