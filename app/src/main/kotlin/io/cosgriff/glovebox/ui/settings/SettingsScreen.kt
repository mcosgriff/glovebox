package io.cosgriff.glovebox.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.domain.ExportFormat
import io.cosgriff.glovebox.domain.LockDelay
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.SettingsText
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.lock.canLockApp
import io.cosgriff.glovebox.ui.export.ExportSheet
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import io.cosgriff.glovebox.ui.service.longDate
import io.cosgriff.glovebox.ui.service.ruleLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A vehicle whose schedule can be opened from Settings. */
data class ScheduleOwner(val id: Long, val name: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository, vehicles: VehicleDao) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    /** Vehicles in the garage; archived ones have no schedule to keep. */
    val vehicles = vehicles.observeActive()
        .map { list -> list.map { ScheduleOwner(it.id, "${it.year} ${it.nickname ?: it.model}") } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(m: ThemeMode) = viewModelScope.launch { repo.setTheme(m) }
    fun setBiometric(on: Boolean) = viewModelScope.launch { repo.setBiometricLock(on) }
    fun setLockDelay(d: LockDelay) = viewModelScope.launch { repo.setLockAfterSeconds(d.seconds) }
    fun setHideInRecents(on: Boolean) = viewModelScope.launch { repo.setHideInRecents(on) }
    fun setTorque(u: TorqueUnit) = viewModelScope.launch { repo.setTorqueUnit(u) }
    fun setRecallCadence(c: RecallCadence) = viewModelScope.launch { repo.setRecallCadence(c) }
}

private enum class Picking { TORQUE, RECALLS, SCHEDULE }

/** Design: 9 · Settings. */
@Composable
fun SettingsScreen(
    onOpenRules: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenImport: () -> Unit,
    onDeleteAll: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val scheme = MaterialTheme.colorScheme
    val s by vm.settings.collectAsStateWithLifecycle()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val dark = scheme.background.luminance() < 0.5f

    // A screen lock can be set or removed in Android's settings while this screen is open.
    val context = LocalContext.current
    var canLock by remember { mutableStateOf(context.canLockApp()) }
    LifecycleResumeEffect(Unit) {
        canLock = context.canLockApp()
        onPauseOrDispose {}
    }

    var exporting by rememberSaveable { mutableStateOf<ExportFormat?>(null) }
    exporting?.let { format -> ExportSheet(vehicleId = null, format = format, onDismiss = { exporting = null }) }

    var picking by rememberSaveable { mutableStateOf<Picking?>(null) }
    when (picking) {
        Picking.TORQUE -> ChoiceDialog(
            title = "Torque units",
            options = TorqueUnit.entries,
            selected = s.torqueUnit,
            label = { it.symbol },
            note = "Torque is stored once and converted for display, so switching loses nothing.",
            onPick = vm::setTorque,
            onDismiss = { picking = null },
        )
        Picking.RECALLS -> ChoiceDialog(
            title = "Check for new recalls",
            options = RecallCadence.entries,
            selected = s.recallCadence,
            label = { it.label },
            note = "Only the year, make and model are sent to NHTSA. With Never, recalls are checked only when you tap Refresh.",
            onPick = vm::setRecallCadence,
            onDismiss = { picking = null },
        )
        Picking.SCHEDULE -> ChoiceDialog(
            title = "Maintenance schedule",
            options = vehicles,
            selected = null,
            label = { it.name },
            note = "Each vehicle has its own schedule.",
            onPick = { onOpenSchedule(it.id) },
            onDismiss = { picking = null },
        )
        null -> Unit
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)

        Section("APPEARANCE") {
            Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(scheme.background).padding(4.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val on = mode == s.theme
                        Box(
                            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (on) scheme.onSurface else Color.Transparent)
                                .selectable(selected = on, role = Role.RadioButton, onClick = { vm.setTheme(mode) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                mode.name.lowercase().replaceFirstChar(Char::titlecase),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (on) scheme.surface else scheme.onSurface,
                            )
                        }
                    }
                }
                Text(
                    SettingsText.theme(followsPhone = s.theme == ThemeMode.SYSTEM, dark = dark),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Section("SECURITY") {
            ToggleRow(
                title = "Unlock with biometrics",
                subtitle = if (canLock) {
                    "Fingerprint or face \u00b7 device PIN as fallback"
                } else {
                    // Said plainly: with no screen lock there is nothing for the app to lock with.
                    "This phone has no screen lock, so the app can't be locked. Set one in Android's settings first."
                },
                checked = s.biometricLock && canLock,
                enabled = canLock,
                onChange = vm::setBiometric,
            )
            Divider()
            if (s.biometricLock && canLock) {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lock the app", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val current = LockDelay.fromSeconds(s.lockAfterSeconds)
                    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LockDelay.entries.forEach { delay ->
                            val on = delay == current
                            // 48dp tall to touch; the pill inside is the design's 36dp.
                            Box(
                                Modifier.height(48.dp).selectable(selected = on, role = Role.RadioButton, onClick = { vm.setLockDelay(delay) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    color = if (on) scheme.onSurface else scheme.surface,
                                    contentColor = if (on) scheme.surface else scheme.onSurface,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.5.dp, if (on) scheme.onSurface else scheme.outlineVariant),
                                ) {
                                    Text(
                                        delay.label,
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
                Divider()
            }
            ToggleRow(
                title = "Hide in recent apps",
                subtitle = "Blocks screenshots and blurs the app switcher",
                checked = s.hideInRecents,
                onChange = vm::setHideInRecents,
            )
            Divider()
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Encrypted on this device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "All vehicles, service records and photos are stored with AES-256. The keys are held by Android Keystore and never leave the phone. Always on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        Section("MAINTENANCE") {
            ValueRow("Reminder rule", s.defaultRule.ruleLabel(), onOpenRules)
            SettingsText.schedules(vehicles.map { it.name })?.let { value ->
                Divider()
                ValueRow("Maintenance schedule", value) {
                    vehicles.singleOrNull()?.let { onOpenSchedule(it.id) } ?: run { picking = Picking.SCHEDULE }
                }
            }
            Divider()
            ValueRow("Torque units", s.torqueUnit.symbol) { picking = Picking.TORQUE }
            Divider()
            ValueRow("Notifications", SettingsText.notifications(s.notices), onOpenNotifications)
            Divider()
            ValueRow("Check for new recalls", s.recallCadence.label) { picking = Picking.RECALLS }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Section("YOUR DATA") {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onOpenBackup).padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Encrypted backup", style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        s.lastBackupOn?.let { "Last backup ${it.format(longDate)} \u00b7 password-protected file you keep" }
                            ?: "Never backed up \u00b7 password-protected file you keep",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Divider()
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Export history", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Service records, schedule and recalls \u00b7 exported files are not encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportButton("PDF", "For resale or a shop", Modifier.weight(1f)) { exporting = ExportFormat.PDF }
                        ExportButton("Markdown", "Plain text, .md", Modifier.weight(1f)) { exporting = ExportFormat.MARKDOWN }
                    }
                }
                // Needs a vehicle to import into; with none there is nowhere for it to lead.
                if (vehicles.isNotEmpty()) {
                    Divider()
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onOpenImport).padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Import", style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            "History, schedules or torque specs from a JSON file \u00b7 nothing of yours changes unless you say so",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                Divider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onDeleteAll),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Delete all data", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.error)
                }
            }
            Text(
                "Only a VIN, or a year, make and model, is sent to NHTSA, to decode it and look up recalls. Nothing else leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Surface(
            Modifier.fillMaxWidth(),
            color = scheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Row(
                Modifier.heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onOpenAbout).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("About Glovebox", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** A labelled card; its rows draw their own dividers. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, Modifier.semantics { heading() })
        Surface(
            Modifier.fillMaxWidth(),
            color = scheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

/** A row that leads somewhere, with its current value on the right. */
@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ExportButton(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, scheme.onSurface),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

/** One choice from a short list. Picking closes the dialog. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    note: String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = option == selected,
                            role = if (selected == null) Role.Button else Role.RadioButton,
                            onClick = {
                                onDismiss()
                                onPick(option)
                            },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // A list to go somewhere from has nothing selected, so it gets no radio buttons.
                        if (selected != null) RadioButton(selected = option == selected, onClick = null)
                        Text(label(option))
                    }
                }
                Text(note, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
