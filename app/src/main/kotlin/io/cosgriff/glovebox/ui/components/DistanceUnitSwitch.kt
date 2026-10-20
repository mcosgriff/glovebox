package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.service.titleWord

/** What a vehicle's odometer counts in. Used where a vehicle is added; changing it later is a conversion, on Vehicle details. */
@Composable
internal fun DistanceUnitSwitch(unit: DistanceUnit, onPick: (DistanceUnit) -> Unit, modifier: Modifier = Modifier) {
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.secondary,
        activeContentColor = MaterialTheme.colorScheme.onSecondary,
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth().height(48.dp)) {
        DistanceUnit.entries.forEachIndexed { i, u ->
            SegmentedButton(
                selected = unit == u,
                onClick = { onPick(u) },
                shape = SegmentedButtonDefaults.itemShape(i, DistanceUnit.entries.size, baseShape = RoundedCornerShape(14.dp)),
                colors = colors,
                icon = {},
                modifier = Modifier.fillMaxHeight(),
            ) {
                Text(u.titleWord, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
