package io.cosgriff.glovebox.ui.reminders

import io.cosgriff.glovebox.ui.components.SheetActions
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.styledStringResource
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.domain.Snoozing
import io.cosgriff.glovebox.ui.service.unit
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.Snooze
import io.cosgriff.glovebox.domain.SnoozeKind
import io.cosgriff.glovebox.domain.SnoozeOption
import io.cosgriff.glovebox.domain.UrgentItem
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.until

/** Pushes a due reminder back without logging it. Design: 10a · Snooze sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSheet(
    reminder: UrgentItem<VehicleEntity>,
    subtitle: String,
    options: List<SnoozeOption>,
    /** Null when there is no later interval to wait for. */
    skip: Snooze?,
    onSnooze: (Snooze) -> Unit,
    onSkip: (Snooze) -> Unit,
    onAlreadyDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val unit = reminder.vehicle.unit
    var kind by rememberSaveable { mutableStateOf(options.first().kind) }
    // The choices can change under the sheet (a mileage update, say); fall back to the first.
    val chosen = options.firstOrNull { it.kind == kind } ?: options.first()

    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.snooze_title, reminder.status.item.name),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Column(Modifier.selectableGroup().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    options.forEachIndexed { i, option ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = scheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .selectable(selected = option == chosen, role = Role.RadioButton, onClick = { kind = option.kind })
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == chosen, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
                            Text(option.kind.label(unit), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text(option.hint(unit), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text(
                styledStringResource(R.string.snooze_reminded_again, chosen.whenText(unit), SpanStyle(fontWeight = FontWeight.Bold)),
                style = MaterialTheme.typography.bodySmall,
            )

            SheetActions(
                onSecondary = onAlreadyDone,
                secondary = { Text(stringResource(R.string.snooze_already_done), maxLines = 1) },
                onConfirm = { onSnooze(chosen.snooze) },
            ) {
                Text(stringResource(R.string.snooze))
            }

            if (skip != null) {
                TextButton(
                    onClick = { onSkip(skip) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.onSurfaceVariant),
                ) {
                    Text(stringResource(R.string.snooze_skip, skip.until(unit)))
                }
            }
        }
    }
}

@Composable
private fun SnoozeKind.label(unit: DistanceUnit): String = when (this) {
    // 500 and 1,000 miles, or 800 and 1,600 kilometres.
    SnoozeKind.MILES_500, SnoozeKind.MILES_1000 -> stringResource(R.string.snooze_distance, Snoozing.distance(this, unit).asNumber(), unit.pluralWord)
    SnoozeKind.WEEKS_2 -> stringResource(R.string.snooze_two_weeks)
    SnoozeKind.MONTH_1 -> stringResource(R.string.snooze_one_month)
    SnoozeKind.NEXT_OIL_CHANGE -> stringResource(R.string.snooze_with_oil)
}

/** The oil change is an estimate of when it will be due, so it reads "~70,100 mi". */
@Composable
private fun SnoozeOption.hint(unit: DistanceUnit): String = when (kind) {
    SnoozeKind.MILES_500, SnoozeKind.MILES_1000 -> stringResource(R.string.snooze_at, snooze.until(unit))
    SnoozeKind.WEEKS_2, SnoozeKind.MONTH_1 -> snooze.until(unit)
    SnoozeKind.NEXT_OIL_CHANGE -> stringResource(R.string.snooze_about, snooze.untilOdometer?.asDistance(unit) ?: snooze.until(unit))
}

@Composable
private fun SnoozeOption.whenText(unit: DistanceUnit): String = when (kind) {
    SnoozeKind.MILES_500, SnoozeKind.MILES_1000 -> stringResource(R.string.snooze_at, snooze.until(unit))
    SnoozeKind.WEEKS_2, SnoozeKind.MONTH_1 -> stringResource(R.string.snooze_on, snooze.until(unit))
    SnoozeKind.NEXT_OIL_CHANGE -> stringResource(R.string.snooze_with_oil_around, snooze.until(unit))
}
