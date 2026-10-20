package io.cosgriff.glovebox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.MileageCheckIns
import io.cosgriff.glovebox.domain.VehicleCheckIn
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.theme.PlexMono

// What a row stands for, besides a number of days: kept as an Int so it survives in saved state.
private const val DEFAULT = -1
private const val OFF = -2
private const val CUSTOM = -3

/**
 * How often to ask for a fresh odometer reading. With [default] it is one vehicle's, which can
 * follow the default (those days) or be off; without, it is the default itself, and [current] is an
 * [VehicleCheckIn.Every].
 */
@Composable
fun CheckInDialog(current: VehicleCheckIn, default: Int?, onSave: (VehicleCheckIn) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable {
        mutableIntStateOf(
            when (current) {
                VehicleCheckIn.Default -> DEFAULT
                VehicleCheckIn.Off -> OFF
                is VehicleCheckIn.Every -> current.days.takeIf { it in MileageCheckIns.QUICK } ?: CUSTOM
            },
        )
    }
    var typed by rememberSaveable { mutableStateOf((current as? VehicleCheckIn.Every)?.days?.takeIf { it !in MileageCheckIns.QUICK }?.asNumber().orEmpty()) }
    val customDays = MileageCheckIns.parse(typed.typedNumber())
    val choice: VehicleCheckIn? = when (picked) {
        DEFAULT -> VehicleCheckIn.Default
        OFF -> VehicleCheckIn.Off
        CUSTOM -> customDays?.let(VehicleCheckIn::Every)
        else -> VehicleCheckIn.Every(picked)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.channel_mileage)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (default != null) {
                    Text(
                        stringResource(R.string.checkin_vehicle_note),
                        Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.selectableGroup()) {
                    if (default != null) Choice(stringResource(R.string.checkin_as_default, checkInLabel(default)), picked == DEFAULT) { picked = DEFAULT }
                    MileageCheckIns.QUICK.forEach { days -> Choice(checkInLabel(days), picked == days) { picked = days } }
                    Choice(stringResource(R.string.checkin_custom), picked == CUSTOM) { picked = CUSTOM }
                    if (picked == CUSTOM) {
                        OutlinedTextField(
                            value = typed,
                            onValueChange = { typed = it.filter(Char::isDigit).take(3) },
                            label = { Text(stringResource(R.string.checkin_days_field)) },
                            supportingText = {
                                Text(stringResource(R.string.checkin_days_range, MileageCheckIns.MIN_DAYS.asNumber(), MileageCheckIns.MAX_DAYS.asNumber()))
                            },
                            isError = typed.isNotEmpty() && customDays == null,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlexMono),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 4.dp),
                        )
                    }
                    if (default != null) Choice(stringResource(R.string.checkin_off), picked == OFF) { picked = OFF }
                }
            }
        },
        confirmButton = { TextButton(onClick = { choice?.let(onSave) }, enabled = choice != null) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}
