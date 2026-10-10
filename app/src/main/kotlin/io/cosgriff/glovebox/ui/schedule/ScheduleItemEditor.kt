package io.cosgriff.glovebox.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
 * so the same editor serves first-time setup and (later) items already being tracked.
 */
@Composable
fun ScheduleItemEditor(
    item: DraftItem,
    isNew: Boolean,
    defaultRule: DueRule,
    onSave: (DraftItem) -> Unit,
    onStopTracking: () -> Unit,
    onClose: () -> Unit,
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

            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("WHICH COMES DUE")
                RuleOption("Use my default (${defaultRule.label().lowercase(Locale.US)})", rule == null) { rule = null }
                DueRule.entries.forEach { r ->
                    RuleOption(r.label(), rule == r) { rule = r }
                }
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

@Composable
private fun RuleOption(text: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text)
    }
}

private fun DueRule.label(): String = when (this) {
    DueRule.WHICHEVER_FIRST -> "Whichever comes first"
    DueRule.WHICHEVER_LAST -> "Whichever comes last"
    DueRule.MILEAGE_ONLY -> "Mileage only"
    DueRule.TIME_ONLY -> "Time only"
}

private fun Int?.asText(): String = this?.let { String.format(Locale.US, "%,d", it) }.orEmpty()
