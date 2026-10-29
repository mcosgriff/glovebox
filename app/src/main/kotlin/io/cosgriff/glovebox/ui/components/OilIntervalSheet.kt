package io.cosgriff.glovebox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.log.activeInterval
import io.cosgriff.glovebox.data.log.isOil
import io.cosgriff.glovebox.domain.CountedInterval
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.OwnOil
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.orText
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.theme.PlexMono

/** Where the vehicle's oil change figure comes from, which the sheet and the Vehicle details row name. */
enum class OilSource { MAKER, TYPICAL, SCHEDULE }

/**
 * The vehicle's oil change as its schedule gives it (#517): [schedule] is what it counts by without the owner's own
 * interval (with Glovebox's months when it has none), null when the item has no figures; [source] says whose it is.
 */
data class OilChange(val schedule: CountedInterval?, val source: OilSource)

/**
 * The oil change among a vehicle's schedule items, a tracked one first; null when it has none, and then there is no
 * "Oil change" row: the owner's interval would have nothing to count for.
 */
fun List<ScheduleItemEntity>.oilChange(unit: DistanceUnit): OilChange? {
    val item = filter { it.isOil() }.sortedBy { !it.tracked }.firstOrNull() ?: return null
    return OilChange(
        schedule = OwnOil.counted(item.activeInterval(), own = null, oil = true, unit),
        source = when {
            item.knownList != null -> OilSource.MAKER
            item.templateKey != null -> OilSource.TYPICAL
            else -> OilSource.SCHEDULE
        },
    )
}

/** "Yours: every 7,500 mi or 9 mo (Glovebox's: check your manual)", or the schedule's, as the Vehicle details row says it. */
@Composable
fun oilChangeLine(oil: OilChange, own: Interval?, unit: DistanceUnit): String {
    val counted = OwnOil.counted(oil.schedule?.interval, own, oil = true, unit)
        ?: return stringResource(R.string.oil_none)
    val every = counted.interval.orText(unit, counted.monthsWorkedOut)
    return when {
        counted.owners -> stringResource(R.string.oil_yours_every, every)
        else -> stringResource(oil.source.everyRes, every)
    }
}

private val OilSource.everyRes: Int
    get() = when (this) {
        OilSource.MAKER -> R.string.oil_maker_every
        OilSource.TYPICAL -> R.string.oil_typical_every
        OilSource.SCHEDULE -> R.string.oil_schedule_every
    }

/**
 * The owner's own oil change interval for one vehicle (#517), with the schedule's figure beside it. Miles and months are
 * typed in the vehicle's [unit]; months left blank follow the miles (one a month for every 833, Glovebox's), and typed
 * months stay as typed. [onSave] gets the interval as typed, months null when they follow; null is "Use the maker's",
 * offered only while the owner has one ([own]).
 */
@Composable
fun OilIntervalSheet(oil: OilChange, own: Interval?, unit: DistanceUnit, onSave: (Interval?) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var milesText by rememberSaveable { mutableStateOf(own?.miles?.toString().orEmpty()) }
    var monthsText by rememberSaveable { mutableStateOf(own?.months?.toString().orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val typed = OwnOil.parse(milesText.typedNumber(), monthsText.typedNumber())
    val followed = if (monthsText.isBlank()) typed?.miles?.let { OwnOil.months(it, typed = null, unit) } else null

    GloveboxSheet(onDismiss = onDismiss) {
        Column(
            Modifier.imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.oil_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.oil_intro), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

            RowsCard {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(oil.source.labelRes), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Text(
                        oil.schedule?.interval?.text(unit) ?: stringResource(R.string.oil_none),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlexMono,
                        textAlign = TextAlign.End,
                    )
                }
                oil.schedule?.takeIf { it.monthsWorkedOut }?.interval?.months?.let { months ->
                    HorizontalDivider(color = scheme.outlineVariant)
                    Text(
                        stringResource(R.string.oil_schedule_months_ours, pluralStringResource(R.plurals.months, months, months.asNumber())),
                        Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            SectionLabel(stringResource(R.string.oil_yours))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OdometerField(milesText, { milesText = it }, unit.titleWord, Modifier.weight(1f), isError = showErrors && typed == null)
                OdometerField(monthsText, { monthsText = it }, stringResource(R.string.item_months), Modifier.weight(1f), isError = showErrors && typed == null)
            }
            Text(
                when {
                    showErrors && typed == null -> stringResource(R.string.item_interval_error, unit.pluralWord)
                    followed != null -> stringResource(R.string.oil_months_follow, pluralStringResource(R.plurals.months, followed, followed.asNumber()))
                    monthsText.isNotBlank() -> stringResource(R.string.oil_months_typed)
                    else -> stringResource(R.string.oil_months_blank)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (showErrors && typed == null) scheme.error else scheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.oil_check_manual), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)

            SheetActions(onCancel = onDismiss, onConfirm = { if (typed != null) onSave(typed) else showErrors = true }) { Text(stringResource(R.string.save)) }
            if (own != null) {
                TextButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(oil.source.useRes), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private val OilSource.labelRes: Int
    get() = when (this) {
        OilSource.MAKER -> R.string.oil_maker
        OilSource.TYPICAL -> R.string.oil_typical
        OilSource.SCHEDULE -> R.string.oil_schedule
    }

private val OilSource.useRes: Int
    get() = when (this) {
        OilSource.MAKER -> R.string.oil_use_maker
        OilSource.TYPICAL -> R.string.oil_use_typical
        OilSource.SCHEDULE -> R.string.oil_use_schedule
    }
