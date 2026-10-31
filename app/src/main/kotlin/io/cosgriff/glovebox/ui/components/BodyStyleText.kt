package io.cosgriff.glovebox.ui.components

import androidx.annotation.StringRes
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.BodyStyle

/** How a body style is named on screen, wherever a vehicle's is shown or picked. */
@StringRes
internal fun BodyStyle.label(): Int = when (this) {
    BodyStyle.SEDAN -> R.string.body_style_sedan
    BodyStyle.HATCHBACK -> R.string.body_style_hatchback
    BodyStyle.CONVERTIBLE -> R.string.body_style_convertible
}
