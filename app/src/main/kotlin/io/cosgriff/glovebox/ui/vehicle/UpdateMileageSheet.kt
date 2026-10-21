package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.components.SheetActions
import io.cosgriff.glovebox.ui.components.GloveboxSheet
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.symbolWord
import io.cosgriff.glovebox.ui.service.shortDate
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.cosgriff.glovebox.domain.MileagePreview
import io.cosgriff.glovebox.domain.MileageUpdate
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlin.math.abs


/** Records the current odometer without logging a service. Design: 4b · Update mileage. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateMileageSheet(
    vehicleName: String,
    /** What this vehicle's odometer counts in; the reading is typed and saved in it. */
    unit: DistanceUnit,
    preview: (miles: Int) -> MileagePreview,
    onSave: (miles: Int) -> Unit,
    onDismiss: () -> Unit,
    /** Opens the camera to read the odometer off the dash. The sheet comes back with [dashReading] set. */
    onReadDash: () -> Unit,
    dashReading: Int?,
    onDashReadingUsed: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var digits by rememberSaveable { mutableStateOf("") }
    // What the camera read, while it is still what is in the field. It only fills the field: the user saves it or changes it.
    var fromDash by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(dashReading) {
        if (dashReading != null) {
            digits = dashReading.toString()
            fromDash = digits
            onDashReadingUsed()
        }
    }
    val miles = digits.toIntOrNull()
    // With nothing typed, preview the current reading so the sheet can still say what the last one was.
    val shown = preview(miles ?: 0)
    val low = miles != null && shown.below
    val far = miles != null && shown.far

    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.update_mileage_title),
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(vehicleName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }

            Surface(
                color = scheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, if (low || far) scheme.error else scheme.onSurface),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                    },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.update_mileage_now), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            (miles ?: 0).asNumber(),
                            fontFamily = PlexMono,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (miles == null) scheme.onSurfaceVariant else scheme.onSurface,
                        )
                        Text(unit.symbolWord, Modifier.padding(bottom = 6.dp), color = scheme.onSurfaceVariant)
                    }
                    shown.deltaLine(typed = miles != null, unit)?.let { (text, tone) ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (tone) {
                                Tone.GOOD -> statusColors().ok
                                Tone.BAD -> scheme.error
                                Tone.PLAIN -> scheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            if (fromDash != null && fromDash == digits) {
                Text(
                    stringResource(R.string.update_mileage_from_dash),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onPrimaryContainer,
                )
            }

            shown.impactLine(typed = miles != null, unit)?.let {
                Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        it,
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("123", "456", "789").forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { d -> DigitKey(d, Modifier.weight(1f)) { digits = MileageUpdate.press(digits, d) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Key(stringResource(R.string.clear), Modifier.weight(1f), muted = true, onClick = { digits = "" }) { KeyLabel(stringResource(R.string.update_mileage_clear_key), Modifier.clearAndSetSemantics { }) }
                    DigitKey('0', Modifier.weight(1f)) { digits = MileageUpdate.press(digits, '0') }
                    Key(stringResource(R.string.update_mileage_delete_digit), Modifier.weight(1f), muted = true, onClick = { digits = digits.dropLast(1) }) {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null)
                    }
                }
            }

            SheetActions(
                onSecondary = onReadDash,
                secondary = {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(18.dp))
                    Text(stringResource(R.string.read_dash_title), maxLines = 1)
                },
                onConfirm = { miles?.let(onSave) },
                enabled = miles != null,
                colors = if (low || far) {
                    ButtonDefaults.buttonColors(containerColor = scheme.onSurfaceVariant, contentColor = scheme.surface)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(
                    when {
                        miles == null -> stringResource(R.string.update_mileage_enter)
                        low || far -> stringResource(R.string.save_anyway)
                        else -> stringResource(R.string.update_mileage_save, miles.asDistance(unit))
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

private enum class Tone { GOOD, BAD, PLAIN }

/** "+538 mi since Oct 10", or what the last reading was when there's nothing to add up yet. */
@Composable
private fun MileagePreview.deltaLine(typed: Boolean, unit: DistanceUnit): Pair<String, Tone>? {
    val last = last ?: return null
    val lastText = stringResource(R.string.update_mileage_last, last.odometer.asDistance(unit), last.date.format(shortDate))
    val delta = delta
    return when {
        !typed || delta == null -> lastText to Tone.PLAIN
        below || far -> lastText to Tone.BAD
        else -> stringResource(R.string.update_mileage_since, delta.asDistance(unit), last.date.format(shortDate)) to Tone.GOOD
    }
}

/** "Oil & filter now due in 1,150 mi · Tire rotation overdue by 3,850 mi." */
@Composable
private fun MileagePreview.impactLine(typed: Boolean, unit: DistanceUnit): String? = when {
    !typed -> null
    below -> stringResource(R.string.update_mileage_below, last?.odometer?.asDistance(unit).orEmpty())
    // Said in place of what it would do to the reminders: if the number is wrong, those are wrong too.
    far -> stringResource(R.string.update_mileage_far, delta?.asDistance(unit).orEmpty())
    impact.isEmpty() -> null
    else -> stringResource(
        R.string.sentence,
        impact.map { (status, headline) ->
            when {
                headline.remaining < 0 -> stringResource(R.string.update_mileage_overdue_by, status.item.name, abs(headline.remaining).asDistance(unit))
                headline.remaining == 0 -> stringResource(R.string.update_mileage_due_now, status.item.name)
                else -> stringResource(R.string.update_mileage_due_in, status.item.name, headline.remaining.asDistance(unit))
            }
        }.joinToString(" · "),
    )
}

@Composable
private fun DigitKey(digit: Char, modifier: Modifier, onClick: () -> Unit) {
    Key(label = null, modifier, muted = false, onClick = onClick) { KeyLabel(digit.toString()) }
}

@Composable
private fun KeyLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontFamily = PlexMono, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
}

/** [label] is what a screen reader says when the key's face isn't a word or a digit. */
@Composable
private fun Key(label: String?, modifier: Modifier, muted: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp).semantics {
            role = Role.Button
            if (label != null) contentDescription = label
        },
        shape = RoundedCornerShape(14.dp),
        color = if (muted) scheme.surfaceVariant else scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
