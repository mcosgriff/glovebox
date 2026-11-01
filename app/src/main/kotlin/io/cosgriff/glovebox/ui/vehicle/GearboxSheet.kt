package io.cosgriff.glovebox.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Gearbox
import io.cosgriff.glovebox.ui.components.GearboxChoice
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.components.SheetActions

/**
 * Which gearbox the vehicle has (#549, design `GearboxSheet`), from its Vehicle details row. [current] null is
 * "Don't know"; [onSave] gets the owner's pick, null for "Don't know".
 */
@Composable
internal fun GearboxSheet(current: Gearbox?, onSave: (Gearbox?) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var picked by rememberSaveable { mutableStateOf(current) }
    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.gearbox_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.gearbox_sheet_intro), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            GearboxChoice(picked, onPick = { picked = it })
            Text(stringResource(R.string.gearbox_sheet_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            SheetActions(onCancel = onDismiss, onConfirm = { onSave(picked) }) { Text(stringResource(R.string.save)) }
        }
    }
}
