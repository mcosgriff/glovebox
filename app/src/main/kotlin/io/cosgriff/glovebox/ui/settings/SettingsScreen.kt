package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.components.ValueRow
import io.cosgriff.glovebox.ui.components.SwitchRow
import io.cosgriff.glovebox.ui.components.LabelledCard
import io.cosgriff.glovebox.ui.components.BorderedCard
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.ui.torque.symbolWord
import io.cosgriff.glovebox.ui.service.titleWord
import io.cosgriff.glovebox.domain.DistanceUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import io.cosgriff.glovebox.domain.TorqueUnit
import io.cosgriff.glovebox.lock.canLockApp
import io.cosgriff.glovebox.ui.export.ExportSheet
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
    fun setDistance(u: DistanceUnit) = viewModelScope.launch { repo.setDistanceUnit(u) }
    fun setRecallCadence(c: RecallCadence) = viewModelScope.launch { repo.setRecallCadence(c) }
    fun setScannerDetails(on: Boolean) = viewModelScope.launch { repo.setScannerDetails(on) }
}

internal enum class Picking { TORQUE, RECALLS, SCHEDULE, DISTANCE }

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
    val s by vm.settings.collectAsStateWithLifecycle()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()

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
            title = stringResource(R.string.settings_torque_units),
            options = TorqueUnit.entries,
            selected = s.torqueUnit,
            label = { it.symbolWord },
            note = stringResource(R.string.settings_torque_note),
            onPick = vm::setTorque,
            onDismiss = { picking = null },
        )
        Picking.DISTANCE -> ChoiceDialog(
            title = stringResource(R.string.details_units),
            options = DistanceUnit.entries,
            selected = s.distanceUnit,
            label = { it.titleWord },
            note = stringResource(R.string.settings_distance_note),
            onPick = vm::setDistance,
            onDismiss = { picking = null },
        )
        Picking.RECALLS -> ChoiceDialog(
            title = stringResource(R.string.settings_recall_checks),
            options = RecallCadence.entries,
            selected = s.recallCadence,
            label = { it.label },
            note = stringResource(R.string.settings_recall_note),
            onPick = vm::setRecallCadence,
            onDismiss = { picking = null },
        )
        Picking.SCHEDULE -> ChoiceDialog(
            title = stringResource(R.string.details_schedule),
            options = vehicles,
            selected = null,
            label = { it.name },
            note = stringResource(R.string.settings_schedule_note),
            onPick = { onOpenSchedule(it.id) },
            onDismiss = { picking = null },
        )
        null -> Unit
    }

    SettingsContent(
        s, vehicles, canLock, onOpenRules, onOpenNotifications, onOpenBackup, onOpenImport, onDeleteAll, onOpenAbout, onOpenSchedule,
        onSetTheme = { vm.setTheme(it) }, onSetBiometric = { vm.setBiometric(it) }, onSetLockDelay = { vm.setLockDelay(it) },
        onSetHideInRecents = { vm.setHideInRecents(it) }, onSetScannerDetails = { vm.setScannerDetails(it) },
        onPick = { picking = it }, onExport = { exporting = it },
    )
}

/** Settings as given, with no ViewModel: what [SettingsScreen] shows, and what the screenshot tests draw. */
@Composable
internal fun SettingsContent(
    s: Settings,
    vehicles: List<ScheduleOwner>,
    /** The phone has a screen lock for the app to lock with. */
    canLock: Boolean,
    onOpenRules: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenImport: () -> Unit,
    onDeleteAll: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSchedule: (vehicleId: Long) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetBiometric: (Boolean) -> Unit,
    onSetLockDelay: (LockDelay) -> Unit,
    onSetHideInRecents: (Boolean) -> Unit,
    onSetScannerDetails: (Boolean) -> Unit,
    onPick: (Picking) -> Unit,
    onExport: (ExportFormat) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.settings_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)

        LabelledCard(stringResource(R.string.settings_appearance)) {
            Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(scheme.background).padding(4.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val on = mode == s.theme
                        Box(
                            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (on) scheme.onSurface else Color.Transparent)
                                .selectable(selected = on, role = Role.RadioButton, onClick = { onSetTheme(mode) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(mode.label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (on) scheme.surface else scheme.onSurface,
                            )
                        }
                    }
                }
                Text(
                    themeText(followsPhone = s.theme == ThemeMode.SYSTEM, dark = dark),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        LabelledCard(stringResource(R.string.settings_security)) {
            SwitchRow(
                title = stringResource(R.string.settings_biometrics),
                about = if (canLock) {
                    stringResource(R.string.settings_biometrics_note)
                } else {
                    // Said plainly: with no screen lock there is nothing for the app to lock with.
                    stringResource(R.string.settings_no_screen_lock)
                },
                checked = s.biometricLock && canLock,
                enabled = canLock,
                onChange = onSetBiometric,
            )
            Divider()
            if (s.biometricLock && canLock) {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_lock_the_app), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val current = LockDelay.fromSeconds(s.lockAfterSeconds)
                    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LockDelay.entries.forEach { delay ->
                            val on = delay == current
                            // 48dp tall to touch; the pill inside is the design's 36dp.
                            Box(
                                Modifier.height(48.dp).selectable(selected = on, role = Role.RadioButton, onClick = { onSetLockDelay(delay) }),
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
            SwitchRow(
                title = stringResource(R.string.settings_hide_recents),
                about = stringResource(R.string.settings_hide_recents_note),
                checked = s.hideInRecents,
                onChange = onSetHideInRecents,
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
                    Text(stringResource(R.string.settings_encrypted), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.settings_encrypted_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        LabelledCard(stringResource(R.string.settings_maintenance)) {
            ValueRow(stringResource(R.string.settings_reminder_rule), s.defaultRule.ruleLabel(), onClick = onOpenRules)
            schedulesText(vehicles.map { it.name })?.let { value ->
                Divider()
                ValueRow(stringResource(R.string.details_schedule), value) {
                    vehicles.singleOrNull()?.let { onOpenSchedule(it.id) } ?: run { onPick(Picking.SCHEDULE) }
                }
            }
            Divider()
            ValueRow(stringResource(R.string.details_units), s.distanceUnit.titleWord) { onPick(Picking.DISTANCE) }
            Divider()
            ValueRow(stringResource(R.string.settings_torque_units), s.torqueUnit.symbolWord) { onPick(Picking.TORQUE) }
            Divider()
            ValueRow(stringResource(R.string.notifications_title), notificationsText(s.notices), onClick = onOpenNotifications)
            Divider()
            ValueRow(stringResource(R.string.settings_recall_checks), s.recallCadence.label) { onPick(Picking.RECALLS) }
        }

        LabelledCard(stringResource(R.string.settings_camera)) {
            SwitchRow(
                title = stringResource(R.string.settings_scanner_details),
                about = stringResource(R.string.settings_scanner_details_note),
                checked = s.scannerDetails,
                onChange = onSetScannerDetails,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelledCard(stringResource(R.string.settings_your_data)) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(role = Role.Button, onClick = onOpenBackup).padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        s.lastBackupOn?.let { stringResource(R.string.settings_backup_last, it.format(longDate)) }
                            ?: stringResource(R.string.settings_backup_never),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Divider()
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.settings_export), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.settings_export_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportButton(stringResource(R.string.export_pdf), stringResource(R.string.export_pdf_note), Modifier.weight(1f)) { onExport(ExportFormat.PDF) }
                        ExportButton(stringResource(R.string.export_markdown), stringResource(R.string.export_markdown_note), Modifier.weight(1f)) { onExport(ExportFormat.MARKDOWN) }
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
                            Text(stringResource(R.string.settings_import), style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            stringResource(R.string.settings_import_note),
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
                    Text(stringResource(R.string.settings_delete_all), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.error)
                }
            }
            Text(
                stringResource(R.string.settings_privacy_line),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        BorderedCard {
            Row(
                Modifier.heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onOpenAbout).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.about_title), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light_title
        ThemeMode.DARK -> R.string.theme_dark_title
    }
