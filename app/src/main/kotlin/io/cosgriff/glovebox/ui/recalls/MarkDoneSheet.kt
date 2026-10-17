package io.cosgriff.glovebox.ui.recalls

import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.ui.service.DatePickerSheet
import io.cosgriff.glovebox.ui.service.longDate
import java.time.LocalDate

/**
 * Marks a recall done. By default the repair also goes in Service history, which needs a date and
 * an odometer; that can be turned off for work done before the user kept records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkDoneSheet(
    recall: RecallEntity,
    odometer: Int?,
    /** What the vehicle's odometer counts in. */
    unit: DistanceUnit,
    makeName: String,
    onConfirm: (RecallCompletion) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var addRecord by rememberSaveable { mutableStateOf(true) }
    var dateDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val date = LocalDate.ofEpochDay(dateDay)
    var odometerText by rememberSaveable { mutableStateOf(odometer?.toString().orEmpty()) }
    var shop by rememberSaveable { mutableStateOf("") }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val miles = odometerText.toIntOrNull()

    if (pickingDate) {
        DatePickerSheet(
            initial = date,
            onPick = {
                dateDay = it.toEpochDay()
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.background,
    ) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Mark recall as done",
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "NHTSA ${Recalls.campaignLabel(recall.campaign)} · ${Recalls.componentLabel(recall.component)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text("Repaired on ${date.format(longDate)}")
            }

            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = addRecord, role = Role.Switch, onValueChange = { addRecord = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Add to Service history", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Turn off if it was fixed before you kept records. It won't change any reminder either way.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Switch(checked = addRecord, onCheckedChange = null)
            }

            if (addRecord) {
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it.filter(Char::isDigit).trimStart('0').take(MileageUpdate.MAX_DIGITS) },
                    label = { Text("Odometer at the repair (${unit.symbolWord})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = shop,
                    onValueChange = { shop = it },
                    label = { Text("$makeName dealer (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.onSurface),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onConfirm(RecallCompletion(date, miles.takeIf { addRecord }, shop.trim().takeIf { addRecord && it.isNotEmpty() }))
                    },
                    enabled = !addRecord || miles != null,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Mark as done", maxLines = 1)
                }
            }
        }
    }
}
