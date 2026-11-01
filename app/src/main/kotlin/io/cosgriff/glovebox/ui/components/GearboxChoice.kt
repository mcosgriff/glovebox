package io.cosgriff.glovebox.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Gearbox
import io.cosgriff.glovebox.domain.Gearboxes
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.theme.statusColors

/** How a gearbox is named on screen, wherever a vehicle's is shown or picked (#549). */
@StringRes
internal fun Gearbox.label(): Int = when (this) {
    Gearbox.MANUAL -> R.string.gearbox_manual
    Gearbox.AUTOMATIC -> R.string.gearbox_automatic
    Gearbox.DUAL_CLUTCH -> R.string.gearbox_dual_clutch
}

/** A vehicle's gearbox as its Vehicle details row says it; null is "Don't know". */
@StringRes
internal fun Gearbox?.labelOrUnknown(): Int = this?.label() ?: R.string.gearbox_unknown

/**
 * The four answers to "which gearbox?" (#549) as a 2×2 grid of 48dp segments, ink-filled when picked (design
 * `GearboxAdd`, `GearboxSheet`). Null is "Don't know", a real answer: the grid always has one picked.
 */
@Composable
internal fun GearboxChoice(selected: Gearbox?, onPick: (Gearbox?) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(Gearbox.MANUAL, Gearbox.AUTOMATIC, Gearbox.DUAL_CLUTCH, null)
    Column(modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option -> GearboxSegment(option, picked = option == selected, onClick = { onPick(option) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GearboxSegment(option: Gearbox?, picked: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(48.dp).selectable(selected = picked, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (picked) scheme.secondary else scheme.surface,
        contentColor = if (picked) scheme.onSecondary else scheme.onSurface,
        border = if (picked) null else BorderStroke(1.5.dp, scheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                stringResource(option.labelOrUnknown()),
                Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * The gearbox question where a vehicle is added (#549), next to the odometer: its label, the grid and why it is asked.
 * [hintedFrom] is the decode's transmission text when the answer was started from Volkswagen's speeds ([Gearboxes.volkswagenHint]),
 * which then says so until the owner picks; null by hand, or when the decode settled nothing that way.
 */
@Composable
internal fun GearboxQuestion(selected: Gearbox?, onPick: (Gearbox?) -> Unit, modifier: Modifier = Modifier, hintedFrom: String? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.gearbox_title), style = MaterialTheme.typography.labelLarge, color = statusColors().soon)
        GearboxChoice(selected, onPick)
        hintedFrom?.let(Gearboxes::speeds)?.let { speeds ->
            Text(stringResource(R.string.gearbox_from_speeds, speeds.asNumber()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.gearbox_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}
