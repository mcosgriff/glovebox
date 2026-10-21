package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.notify.ShortcutTask

/** A vehicle as the chooser names it: year, make and model, and the nickname under it when there is one. */
data class VehicleChoice(val id: Long, val title: String?, val nickname: String?)

fun VehicleEntity.asChoice() = VehicleChoice(id, VehicleText.title(year, make, model, trim = null), nickname?.takeIf { it.isNotBlank() })

/**
 * Which vehicle a launcher shortcut is for, asked when the garage has more than one (#235). Each is named in
 * full, so two of the same model only differ by year or nickname, never by a shortened name.
 */
@Composable
internal fun ChooseVehicleDialog(task: ShortcutTask, vehicles: List<VehicleChoice>, onChoose: (Long) -> Unit, onDismiss: () -> Unit) {
    ChooseVehicleDialog(
        stringResource(
            when (task) {
                ShortcutTask.LOG_SERVICE -> R.string.choose_vehicle_log
                ShortcutTask.UPDATE_MILEAGE -> R.string.choose_vehicle_mileage
            },
        ),
        vehicles,
        onChoose,
        onDismiss,
    )
}

/** Which vehicle, named in full with the nickname under it, as the shortcut asks; [note] under the list. */
@Composable
internal fun ChooseVehicleDialog(title: String, vehicles: List<VehicleChoice>, onChoose: (Long) -> Unit, onDismiss: () -> Unit, note: String? = null) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                vehicles.forEach { v ->
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { onChoose(v.id) }
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(v.title ?: stringResource(R.string.unknown_vehicle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        v.nickname?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                note?.let { Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
