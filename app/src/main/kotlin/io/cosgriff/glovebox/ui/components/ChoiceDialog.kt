package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R

/**
 * One of [options], picked in a dialog that closes as soon as one is tapped. With a [selected] option the
 * rows are radio buttons; without one it is a plain list of things to go to. [note] sits under the list.
 */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    note: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = option == selected,
                            role = if (selected == null) Role.Button else Role.RadioButton,
                            onClick = {
                                onDismiss()
                                onPick(option)
                            },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selected != null) RadioButton(selected = option == selected, onClick = null)
                        Text(label(option))
                    }
                }
                note?.let { Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
