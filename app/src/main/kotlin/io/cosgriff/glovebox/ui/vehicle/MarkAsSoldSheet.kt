package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.SheetHandle
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.theme.PlexMono
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
import io.cosgriff.glovebox.domain.Archive
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.domain.Sale
import io.cosgriff.glovebox.domain.SaleProblem
import io.cosgriff.glovebox.ui.service.DatePickerSheet
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import java.time.LocalDate

/** Design: the "Mark as sold" sheet on 4d · Vehicle details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAsSoldSheet(
    vehicleName: String,
    /** The highest reading so far; the final odometer starts there and can't go below it. */
    lastOdometer: Int?,
    /** What this vehicle's odometer counts in. */
    unit: DistanceUnit,
    onArchive: (Sale) -> Unit,
    onDismiss: () -> Unit,
    /** Opens the export sheet for this vehicle, as a PDF. */
    onExport: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var soldDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var odometerText by rememberSaveable { mutableStateOf(lastOdometer?.toString().orEmpty()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val sale = Sale(LocalDate.ofEpochDay(soldDay), odometerText.toIntOrNull())
    val problems = Archive.problems(sale, lastOdometer, LocalDate.now())
    val tooLow = SaleProblem.ODOMETER_BELOW_LAST in problems

    if (pickingDate) {
        DatePickerSheet(
            initial = sale.soldOn,
            onPick = {
                soldDay = it.toEpochDay()
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { SheetHandle() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.background,
    ) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.sold_title, vehicleName),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedButton(
                onClick = { pickingDate = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text(stringResource(R.string.sold_date, sale.soldOn.format(longDate)))
            }

            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it.filter(Char::isDigit).trimStart('0').take(MileageUpdate.MAX_DIGITS) },
                label = { Text(stringResource(R.string.sold_final_odometer, unit.symbolWord)) },
                supportingText = {
                    Text(
                        when {
                            tooLow -> stringResource(R.string.sold_too_low, lastOdometer?.asDistance(unit).orEmpty())
                            lastOdometer == null -> stringResource(R.string.optional)
                            else -> stringResource(R.string.sold_last_reading, lastOdometer.asDistance(unit))
                        },
                    )
                },
                isError = tooLow,
                singleLine = true,
                textStyle = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            ) {
                Text(stringResource(R.string.sold_export))
            }

            Text(
                stringResource(R.string.sold_note),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onArchive(sale) },
                    enabled = problems.isEmpty(),
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.sold_archive))
                }
            }
        }
    }
}
