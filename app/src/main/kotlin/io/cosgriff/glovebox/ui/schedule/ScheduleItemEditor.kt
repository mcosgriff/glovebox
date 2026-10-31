package io.cosgriff.glovebox.ui.schedule

import io.cosgriff.glovebox.domain.OdometerInput
import io.cosgriff.glovebox.ui.components.BorderedCard
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.typedNumber
import io.cosgriff.glovebox.ui.service.ruleInSentence
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.service.label
import io.cosgriff.glovebox.ui.service.ruleLabel
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.ui.service.pluralWord
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.ui.theme.PlexMono
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import io.cosgriff.glovebox.domain.LastDone
import io.cosgriff.glovebox.domain.nextDueAfter
import io.cosgriff.glovebox.ui.service.asDistance
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.text
import io.cosgriff.glovebox.ui.theme.statusColors
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.CountedInterval
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.OilMonths
import io.cosgriff.glovebox.domain.OwnOil
import io.cosgriff.glovebox.ui.components.Chevron
import io.cosgriff.glovebox.ui.service.every
import io.cosgriff.glovebox.domain.IntervalInput
import io.cosgriff.glovebox.domain.ServiceCatalog

/**
 * Edits one schedule item. Stateless towards the database: the caller decides what Save means,
 * so the same editor serves first-time setup and items already being tracked. [history] is what
 * the service log says about a tracked item; setup has none.
 */
@Composable
fun ScheduleItemEditor(
    item: DraftItem,
    isNew: Boolean,
    defaultRule: DueRule,
    /** What the vehicle's odometer counts in; the interval is typed and saved in it. */
    unit: DistanceUnit,
    onSave: (DraftItem) -> Unit,
    onStopTracking: () -> Unit,
    onClose: () -> Unit,
    history: ItemHistory? = null,
    /** For an oil change already on the schedule (#517): the owner's own interval for the vehicle, and its sheet. */
    ownOil: OwnOilLink? = null,
) {
    var name by remember(item.key) { mutableStateOf(item.name) }
    var normal by remember(item.key) { mutableStateOf(item.normal) }
    var severe by remember(item.key) { mutableStateOf(item.severe) }
    var useSevere by remember(item.key) { mutableStateOf(item.useSevere && item.severe != null) }
    // A flex-fuel maker's E85 interval, edited in the same boxes while it is on, as the severe one is.
    var e85 by remember(item.key) { mutableStateOf(item.e85) }
    var useE85 by remember(item.key) { mutableStateOf(item.showsE85) }
    var rule by remember(item.key) { mutableStateOf(item.rule) }
    var service by remember(item.key) { mutableStateOf(item.service) }
    // A new item starts blank: the interval is the user's to say, not ours to guess.
    var milesText by remember(item.key) { mutableStateOf(if (isNew) "" else item.active.miles.asText()) }
    var monthsText by remember(item.key) { mutableStateOf(if (isNew) "" else item.active.months.asText()) }
    var firstText by remember(item.key) { mutableStateOf(item.firstAt.asText()) }
    // Blank is "one interval in, like any other"; otherwise a whole distance above zero, read as an odometer is.
    val firstTyped = firstText.typedNumber().takeIf { it.isNotBlank() }?.let(OdometerInput::parse)
    val firstBad = firstText.isNotBlank() && firstTyped == null
    var showErrors by remember(item.key) { mutableStateOf(false) }

    val typed = IntervalInput.parse(milesText.typedNumber(), monthsText.typedNumber())

    fun load(interval: Interval) {
        milesText = interval.miles.asText()
        monthsText = interval.months.asText()
    }

    /** Keeps what's in the boxes for the interval being edited, when it's usable. */
    fun commitTyped() {
        val t = typed ?: return
        when {
            useE85 -> e85 = t
            useSevere -> severe = t
            else -> normal = t
        }
    }

    /** What the boxes show: the interval in force for these switches. */
    fun shown(): Interval = if (useE85) e85 ?: normal else if (useSevere) severe ?: normal else normal

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close)) }
            Text(
                stringResource(if (isNew) R.string.item_add_title else R.string.item_edit_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            Button(
                onClick = {
                    val interval = typed
                    if (name.isBlank() || interval == null || firstBad) {
                        showErrors = true
                    } else {
                        onSave(
                            item.copy(
                                name = name.trim(),
                                normal = if (useE85 || useSevere) normal else interval,
                                severe = if (!useE85 && useSevere) interval else severe,
                                e85 = if (useE85) interval else e85,
                                useE85 = useE85,
                                // A job only for severe use has no second interval to switch to here; whether it is on follows the schedule.
                                useSevere = if (item.severeOnly) item.useSevere else useSevere,
                                rule = rule,
                                service = service,
                                firstAt = firstTyped,
                            ),
                        )
                    }
                },
            ) { Text(stringResource(R.string.save)) }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.documents_name)) },
                singleLine = true,
                isError = showErrors && name.isBlank(),
                supportingText = if (showErrors && name.isBlank()) {
                    { Text(stringResource(R.string.documents_give_name)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.item_repeat_every))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(unit.titleWord, milesText, { milesText = it }, showErrors && typed == null, Modifier.weight(1f))
                    NumberField(stringResource(R.string.item_months), monthsText, { monthsText = it }, showErrors && typed == null, Modifier.weight(1f))
                }
                Text(
                    if (showErrors && typed == null) {
                        stringResource(R.string.item_interval_error, unit.pluralWord)
                    } else {
                        stringResource(R.string.item_interval_note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showErrors && typed == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (item.severeOnly) {
                    Text(
                        stringResource(R.string.item_severe_only_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val suggestedSevere = item.template?.severe
                if (severe != null) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.item_severe_interval), style = MaterialTheme.typography.titleSmall)
                            Text(
                                suggestedSevere?.let { stringResource(R.string.item_severe_typical, it.text(unit)) }
                                    ?: stringResource(R.string.item_severe_for),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = useSevere,
                            onCheckedChange = { on ->
                                commitTyped()
                                useSevere = on
                                load(shown())
                            },
                        )
                    }
                }
                if (e85 != null) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.item_e85_interval), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.item_e85_for),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = useE85,
                            onCheckedChange = { on ->
                                commitTyped()
                                useE85 = on
                                load(shown())
                            },
                        )
                    }
                }
            }

            ownOil?.let { link -> OwnOilCard(link, unit) }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(R.string.item_first_at))
                NumberField(unit.titleWord, firstText, { firstText = it }, showErrors && firstBad, Modifier.fillMaxWidth())
                Text(
                    if (showErrors && firstBad) {
                        stringResource(R.string.item_first_error, unit.pluralWord)
                    } else {
                        stringResource(R.string.item_first_note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showErrors && firstBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RuleDropdown(rule, defaultRule, onPick = { rule = it })

            // The typical items already answer to their own service line; this is for the user's.
            if (item.own) ServiceDropdown(service, onPick = { service = it })

            if (history != null) {
                LastDoneCard(history, unit)
                NextDueCard(
                    history = history,
                    // As the reminder will count it (#517): an oil change by the owner's own interval when there is one, and
                    // one in miles alone with Glovebox's months.
                    counted = typed?.let { OwnOil.counted(it, ownOil?.own, OilMonths.isOil(item.template?.key, service.takeIf { item.own }), unit) },
                    rule = rule ?: defaultRule,
                    unit = unit,
                    // The pace estimate was worked out for what is saved, not for what is being typed.
                    showEstimate = typed == item.active && rule == item.rule,
                )
            }

            Column {
                item.template?.let { template ->
                    TextButton(
                        onClick = {
                            normal = template.normal
                            severe = template.severe
                            if (template.severe == null) useSevere = false
                            load(shown())
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.item_reset)) }
                }
                if (!isNew && item.tracked) {
                    TextButton(onClick = onStopTracking, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.item_stop_tracking), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NumberField(label: String, value: String, onChange: (String) -> Unit, isError: Boolean, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** The owner's own oil change interval for the vehicle ([OwnOil]), null when not set, and [onOpen] for its sheet. */
data class OwnOilLink(val own: Interval?, val onOpen: () -> Unit)

/** The way from an oil change's editor to the vehicle's own oil change interval (#517), saying what it is. */
@Composable
private fun OwnOilCard(link: OwnOilLink, unit: DistanceUnit) {
    BorderedCard(onClick = link.onOpen) {
        Row(
            Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.item_oil_own), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val counted = link.own?.let { OwnOil.counted(it, it, oil = true, unit) }
                Text(
                    counted?.let { stringResource(R.string.item_oil_own_set, it.every(unit)) }
                        ?: stringResource(R.string.item_oil_own_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Chevron()
        }
    }
}

/** What the service log says about an item that is already being tracked. */
data class ItemHistory(
    /** Null when the item has never been logged. In dash terms, which is what the next due point is counted from. */
    val lastDone: LastDone?,
    /** [lastDone] as the odometer read that day; differs only for a service before a cluster replacement. */
    val lastRead: LastDone? = lastDone,
    /** Opens the record [lastDone] comes from. */
    val onOpenRecord: (() -> Unit)?,
    /** That record is a bare "last done" the user entered, not a service logged at the time. */
    val entered: Boolean = false,
    /** Offered while the item has never been logged: asks for a date and mileage. */
    val onEnterLastDone: (() -> Unit)? = null,
    /** The Log service lines that reset this reminder; null for the user's own items, where the picker above the card says it. */
    val resetBy: List<String>?,
    /** When it is expected to come due at the user's driving pace, for the saved interval and rule. */
    val estimate: LocalDate?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleDropdown(rule: DueRule?, defaultRule: DueRule, onPick: (DueRule?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val useDefault = stringResource(R.string.item_rule_default, defaultRule.ruleInSentence())
    fun text(r: DueRule?) = r?.label() ?: useDefault
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = text(rule),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.item_rule_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (listOf<DueRule?>(null) + DueRule.entries).forEach { r ->
                DropdownMenuItem(
                    text = { Text(text(r)) },
                    onClick = {
                        onPick(r)
                        open = false
                    },
                )
            }
        }
    }
}

/**
 * Which built-in line on Log service resets one of the user's own items. With one chosen, ticking
 * that line counts for this item (and for the typical item it resets, if that is tracked too), and
 * work already logged under it counts from the moment this is saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDropdown(service: String?, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val lines = remember { ServiceCatalog.categories.flatMap { c -> c.items.map { it to c.label } } }
    val chosen = lines.firstOrNull { it.first.id == service }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
            OutlinedTextField(
                value = chosen?.first?.label ?: stringResource(R.string.item_service_own),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.item_resets_it)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.item_service_own)) },
                    onClick = {
                        onPick(null)
                        open = false
                    },
                )
                lines.forEach { (line, category) ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(line.label)
                                Text(category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            onPick(line.id)
                            open = false
                        },
                    )
                }
            }
        }
        Text(
            if (chosen == null) {
                stringResource(R.string.item_service_pick)
            } else {
                stringResource(R.string.item_service_chosen, chosen.first.label)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LastDoneCard(history: ItemHistory, unit: DistanceUnit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.item_last_done))
        BorderedCard {
            Column(Modifier.padding(horizontal = 16.dp)) {
                val last = history.lastRead
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .then((if (last == null) history.onEnterLastDone else history.onOpenRecord)?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        last?.let { "${it.date.format(longDate)} · ${it.odometer.asDistance(unit)}" } ?: stringResource(R.string.due_not_logged),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (last == null && history.onEnterLastDone != null) {
                        Text(stringResource(R.string.item_enter_it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                    if (last != null && history.onOpenRecord != null) {
                        Text(stringResource(if (history.entered) R.string.details_entered_by_you else R.string.from_your_log), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
                history.resetBy?.let { resetBy ->
                    HorizontalDivider(color = scheme.outlineVariant)
                    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            // None is this item itself, said as a list of one: "any of these".
                            pluralStringResource(R.plurals.item_resets, if (resetBy.size == 1) 1 else 2),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            resetBy.joinToString(" · ").ifEmpty { stringResource(R.string.item_service_own_log) },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** [interval] is what is in the boxes right now, so the card follows the edit; null while it can't be used. */
@Composable
private fun NextDueCard(history: ItemHistory, counted: CountedInterval?, rule: DueRule, unit: DistanceUnit, showEstimate: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val last = history.lastDone
    val interval = counted?.interval
    Surface(
        Modifier.fillMaxWidth(),
        color = scheme.primaryContainer,
        contentColor = scheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.item_next_due), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
            when {
                last == null -> {
                    Text(stringResource(R.string.due_not_logged), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.item_log_once),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors().soon,
                    )
                }
                interval == null -> Text(stringResource(R.string.item_enter_interval), style = MaterialTheme.typography.titleMedium)
                else -> {
                    Text(interval.nextDueAfter(last).text(unit), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(
                            rule.label().takeIf { interval.miles != null && interval.months != null },
                            interval.months?.takeIf { counted.monthsWorkedOut }?.let {
                                stringResource(R.string.item_months_ours, pluralStringResource(R.plurals.months, it, it.asNumber()))
                            },
                            history.estimate?.takeIf { showEstimate }?.let { stringResource(R.string.item_estimate, it.format(longDate)) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors().soon,
                    )
                }
            }
        }
    }
}

private fun DueRule.label(): String = ruleLabel()

private fun Int?.asText(): String = this?.asNumber().orEmpty()
