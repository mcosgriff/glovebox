package io.cosgriff.glovebox.ui.recalls

import io.cosgriff.glovebox.ui.components.SheetActions
import io.cosgriff.glovebox.ui.components.SwitchRow
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.ui.components.OdometerField
import io.cosgriff.glovebox.ui.components.DateButton
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.service.recallComponent
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.domain.Recalls
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
    val miles = OdometerInput.parse(odometerText.typedNumber())


    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.recalls_mark_done_title),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.recalls_campaign_component, Recalls.campaignLabel(recall.campaign), recallComponent(recall.component)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            DateButton(R.string.recalls_repaired_on, date) { dateDay = it.toEpochDay() }

            SwitchRow(stringResource(R.string.recalls_add_to_history), stringResource(R.string.recalls_add_note), addRecord) { addRecord = it }

            if (addRecord) {
                OdometerField(odometerText, { odometerText = it }, stringResource(R.string.recalls_repair_odometer, unit.symbolWord))
                OutlinedTextField(
                    value = shop,
                    onValueChange = { shop = it },
                    label = { Text(stringResource(R.string.recalls_dealer_optional, makeName)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SheetActions(
                onCancel = onDismiss,
                onConfirm = { onConfirm(RecallCompletion(date, miles.takeIf { addRecord }, shop.trim().takeIf { addRecord && it.isNotEmpty() })) },
                enabled = !addRecord || miles != null,
            ) {
                Text(stringResource(R.string.recalls_mark_done), maxLines = 1)
            }
        }
    }
}
