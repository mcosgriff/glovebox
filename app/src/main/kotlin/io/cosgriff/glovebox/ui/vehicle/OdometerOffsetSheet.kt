package io.cosgriff.glovebox.ui.vehicle

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.ClusterSwap
import io.cosgriff.glovebox.domain.ClusterSwaps
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.SwapProblem
import io.cosgriff.glovebox.ui.service.DatePickerSheet
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.theme.PlexMono
import java.time.LocalDate

/**
 * Records an instrument cluster replacement: the day, and what each cluster read. The offset is
 * worked out from the two readings rather than typed. [onSave] with null removes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdometerOffsetSheet(current: ClusterSwap?, unit: DistanceUnit, onSave: (ClusterSwap?) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var day by rememberSaveable { mutableLongStateOf((current?.on ?: LocalDate.now()).toEpochDay()) }
    var oldText by rememberSaveable { mutableStateOf(current?.let { ClusterSwaps.oldReading(it).toString() }.orEmpty()) }
    var newText by rememberSaveable { mutableStateOf(current?.startReading?.toString().orEmpty()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val on = LocalDate.ofEpochDay(day)
    // The old cluster had miles on it or there would be nothing to offset; a new one may start at zero.
    val oldReading = ClusterSwaps.parseReading(oldText)?.takeIf { it > 0 }
    val newReading = ClusterSwaps.parseReading(newText)
    val problems = ClusterSwaps.problems(on, oldReading, newReading, LocalDate.now())
    val same = SwapProblem.SAME_READING in problems

    if (pickingDate) {
        DatePickerSheet(
            initial = on,
            onPick = {
                day = it.toEpochDay()
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
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Odometer offset", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "For a vehicle whose instrument cluster was replaced, so the dash no longer shows its real mileage. " +
                    "Keep entering what the dash shows; Glovebox works out the rest.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text("Replaced on · ${on.format(longDate)}")
            }

            ReadingField(
                value = oldText,
                onChange = { oldText = it },
                label = "Old cluster read (${unit.symbol})",
                note = if (showErrors && oldReading == null) "Enter its last reading in whole ${unit.plural}." else "Its last reading before it came out.",
                isError = showErrors && oldReading == null,
            )
            ReadingField(
                value = newText,
                onChange = { newText = it },
                label = "New cluster read (${unit.symbol})",
                note = when {
                    same -> "The same as the old one, so there is nothing to offset."
                    showErrors && newReading == null -> "Enter what it read when it went in."
                    else -> "When it went in. 0 for a brand-new one."
                },
                isError = same || showErrors && newReading == null,
            )

            if (problems.isEmpty()) {
                Text(
                    "Offset ${ClusterSwaps.offsetLabel(oldReading!! - newReading!!, unit)}. Readings dated ${on.format(longDate)} or later " +
                        "count as the new cluster's, earlier ones as the old one's. Reminders snoozed to a mileage come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (problems.isEmpty()) onSave(ClusterSwaps.of(on, oldReading!!, newReading!!)) else showErrors = true
                    },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Save")
                }
            }
            if (current != null) {
                TextButton(
                    onClick = { onSave(null) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                ) {
                    Text("Remove the offset", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReadingField(value: String, onChange: (String) -> Unit, label: String, note: String, isError: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(MileageUpdate.MAX_DIGITS)) },
        label = { Text(label) },
        supportingText = { Text(note) },
        isError = isError,
        singleLine = true,
        textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
