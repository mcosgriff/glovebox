package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.domain.RecallCadence
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import io.cosgriff.glovebox.ui.export.ExportSheet
import io.cosgriff.glovebox.domain.ExportFormat
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.service.ruleLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())
    fun setTheme(m: ThemeMode) = viewModelScope.launch { repo.setTheme(m) }
    fun setBiometric(on: Boolean) = viewModelScope.launch { repo.setBiometricLock(on) }
    fun setHideInRecents(on: Boolean) = viewModelScope.launch { repo.setHideInRecents(on) }
    fun setTorque(u: TorqueUnit) = viewModelScope.launch { repo.setTorqueUnit(u) }
    fun setRecallCadence(c: RecallCadence) = viewModelScope.launch { repo.setRecallCadence(c) }
}

@Composable
fun SettingsScreen(
    onOpenRules: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackup: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var exporting by rememberSaveable { mutableStateOf<ExportFormat?>(null) }
    exporting?.let { format -> ExportSheet(vehicleId = null, format = format, onDismiss = { exporting = null }) }
    var pickingRecalls by rememberSaveable { mutableStateOf(false) }
    if (pickingRecalls) {
        AlertDialog(
            onDismissRequest = { pickingRecalls = false },
            title = { Text("Check for new recalls") },
            text = {
                Column {
                    RecallCadence.entries.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = c == s.recallCadence,
                                role = Role.RadioButton,
                                onClick = {
                                    vm.setRecallCadence(c)
                                    pickingRecalls = false
                                },
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = c == s.recallCadence, onClick = null)
                            Text(c.label)
                        }
                    }
                    Text(
                        "Only the year, make and model are sent to NHTSA. With Never, recalls are checked only when you tap Refresh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { pickingRecalls = false }) { Text("Cancel") } },
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Section("Appearance") {
            Segmented(ThemeMode.entries, s.theme, { it.name.lowercase().replaceFirstChar(Char::titlecase) }, vm::setTheme)
        }
        Section("Security") {
            ToggleRow("Unlock with biometrics", "Fingerprint or face · device PIN as fallback", s.biometricLock, vm::setBiometric)
            ToggleRow("Hide in recent apps", "Blocks screenshots and blurs the app switcher", s.hideInRecents, vm::setHideInRecents)
            Text(
                "Encrypted on this device — AES-256, key held in Android Keystore. Always on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Section("Reminders") {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onOpenRules),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Reminder rule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${s.defaultRule.ruleLabel()} · ${s.lead.miles.asMiles()} or ${s.lead.days} days early",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            }
        }
        Section("Notifications") {
            val n = s.notices
            val on = listOf(n.serviceDue, n.overdue, n.recalls, n.mileage).count { it }
            ExportRow("Notifications", if (on == 0) "Off" else "$on of 4 kinds on", onOpenNotifications)
            ExportRow("Check for new recalls", s.recallCadence.label) { pickingRecalls = true }
        }
        Section("Your data") {
            ExportRow(
                "Encrypted backup",
                s.lastBackupOn?.let { "Last backup ${it.format(longDate)}" } ?: "Never backed up · password-protected file you keep",
                onOpenBackup,
            )
        }
        Section("Export") {
            ExportRow("PDF", "For resale or a shop") { exporting = ExportFormat.PDF }
            ExportRow("Markdown", "Plain text, .md") { exporting = ExportFormat.MARKDOWN }
        }
        Section("Units") {
            Segmented(TorqueUnit.entries, s.torqueUnit, { it.symbol }, vm::setTorque)
        }
    }
}

@Composable
private fun ExportRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, opt ->
            SegmentedButton(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label(opt)) }
        }
    }
}
