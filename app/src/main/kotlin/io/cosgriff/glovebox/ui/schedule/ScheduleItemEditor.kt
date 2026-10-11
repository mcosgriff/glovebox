package io.cosgriff.glovebox.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.cosgriff.glovebox.ui.service.asMiles
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.IntervalInput
import io.cosgriff.glovebox.domain.label
import java.util.Locale

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
    onSave: (DraftItem) -> Unit,
    onStopTracking: () -> Unit,
    onClose: () -> Unit,
    history: ItemHistory? = null,
) {
    var name by remember(item.key) { mutableStateOf(item.name) }
    var normal by remember(item.key) { mutableStateOf(item.normal) }
    var severe by remember(item.key) { mutableStateOf(item.severe) }
    var useSevere by remember(item.key) { mutableStateOf(item.useSevere && item.severe != null) }
    var rule by remember(item.key) { mutableStateOf(item.rule) }
    // A new item starts blank: the interval is the user's to say, not ours to guess.
    var milesText by remember(item.key) { mutableStateOf(if (isNew) "" else item.active.miles.asText()) }
    var monthsText by remember(item.key) { mutableStateOf(if (isNew) "" else item.active.months.asText()) }
    var showErrors by remember(item.key) { mutableStateOf(false) }

    val typed = IntervalInput.parse(milesText, monthsText)

    fun load(interval: Interval) {
        milesText = interval.miles.asText()
        monthsText = interval.months.asText()
    }

    /** Keeps what's in the boxes for the interval being edited, when it's usable. */
    fun commitTyped() {
        val t = typed ?: return
        if (useSevere) severe = t else normal = t
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
            Text(
                if (isNew) "Add schedule item" else "Edit schedule item",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            Button(
                onClick = {
                    val interval = typed
                    if (name.isBlank() || interval == null) {
                        showErrors = true
                    } else {
                        onSave(
                            item.copy(
                                name = name.trim(),
                                normal = if (useSevere) normal else interval,
                                severe = if (useSevere) interval else severe,
                                useSevere = useSevere,
                                rule = rule,
                            ),
                        )
                    }
                },
            ) { Text("Save") }
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                isError = showErrors && name.isBlank(),
                supportingText = if (showErrors && name.isBlank()) {
                    { Text("Give it a name") }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("REPEAT EVERY")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Miles", milesText, { milesText = it }, showErrors && typed == null, Modifier.weight(1f))
                    NumberField("Months", monthsText, { monthsText = it }, showErrors && typed == null, Modifier.weight(1f))
                }
                Text(
                    if (showErrors && typed == null) {
                        "Enter miles, months, or both as whole numbers above zero."
                    } else {
                        "Leave one blank if it doesn't apply."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showErrors && typed == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val suggestedSevere = item.template?.severe
                if (severe != null) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Severe-use interval", style = MaterialTheme.typography.titleSmall)
                            Text(
                                suggestedSevere?.let { "Typical: ${it.label()} when towing, off-road or in dust" }
                                    ?: "For towing, off-road or dusty driving",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = useSevere,
                            onCheckedChange = { on ->
                                commitTyped()
                                useSevere = on
                                load(if (on) severe ?: normal else normal)
                            },
                        )
                    }
                }
            }

            RuleDropdown(rule, defaultRule, onPick = { rule = it })

            if (history != null) {
                LastDoneCard(history)
                NextDueCard(
                    history = history,
                    interval = typed,
                    rule = rule ?: defaultRule,
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
                            load(if (useSevere) template.severe ?: template.normal else template.normal)
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Reset to the suggested interval") }
                }
                if (!isNew && item.tracked) {
                    TextButton(onClick = onStopTracking, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Stop tracking this item", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, isError: Boolean, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** What the service log says about an item that is already being tracked. */
data class ItemHistory(
    /** Null when the item has never been logged. */
    val lastDone: LastDone?,
    /** Opens the record [lastDone] comes from. */
    val onOpenRecord: (() -> Unit)?,
    /** The Log service lines that reset this reminder. */
    val resetBy: List<String>,
    /** When it is expected to come due at the user's driving pace, for the saved interval and rule. */
    val estimate: LocalDate?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleDropdown(rule: DueRule?, defaultRule: DueRule, onPick: (DueRule?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    fun text(r: DueRule?) = r?.label() ?: "Use my default (${defaultRule.label().lowercase(Locale.US)})"
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = text(rule),
            onValueChange = {},
            readOnly = true,
            label = { Text("Which comes due") },
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

@Composable
private fun LastDoneCard(history: ItemHistory) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("LAST DONE")
        Surface(
            Modifier.fillMaxWidth(),
            color = scheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                val last = history.lastDone
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .then(history.onOpenRecord?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        last?.let { "${it.date.format(longDate)} · ${it.odometer.asMiles()}" } ?: "Not logged yet",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (last != null && history.onOpenRecord != null) {
                        Text("From your log", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = scheme.outlineVariant)
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (history.resetBy.size == 1) "Logging this resets it" else "Logging any of these resets it",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        history.resetBy.joinToString(" · ").ifEmpty { "This item, under Your items in Log service" },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** [interval] is what is in the boxes right now, so the card follows the edit; null while it can't be used. */
@Composable
private fun NextDueCard(history: ItemHistory, interval: Interval?, rule: DueRule, showEstimate: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val last = history.lastDone
    Surface(
        Modifier.fillMaxWidth(),
        color = scheme.primaryContainer,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("NEXT DUE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
            when {
                last == null -> {
                    Text("Not logged yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Log it once and the countdown starts from that date and mileage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors().soon,
                    )
                }
                interval == null -> Text("Enter an interval above", style = MaterialTheme.typography.titleMedium)
                else -> {
                    Text(interval.nextDueAfter(last).text(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(
                            rule.label().takeIf { interval.miles != null && interval.months != null },
                            history.estimate?.takeIf { showEstimate }?.let { "est. ${it.format(longDate)} at your pace" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors().soon,
                    )
                }
            }
        }
    }
}

private fun DueRule.label(): String = when (this) {
    DueRule.WHICHEVER_FIRST -> "Whichever comes first"
    DueRule.WHICHEVER_LAST -> "Whichever comes last"
    DueRule.MILEAGE_ONLY -> "Mileage only"
    DueRule.TIME_ONLY -> "Time only"
}

private fun Int?.asText(): String = this?.let { String.format(Locale.US, "%,d", it) }.orEmpty()
