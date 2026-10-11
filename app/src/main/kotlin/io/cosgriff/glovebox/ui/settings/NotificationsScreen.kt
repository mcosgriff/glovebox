package io.cosgriff.glovebox.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.MileageCadence
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.NoticeRules
import io.cosgriff.glovebox.notify.Notifier
import io.cosgriff.glovebox.ui.schedule.SectionLabel
import io.cosgriff.glovebox.ui.service.asMiles
import io.cosgriff.glovebox.ui.theme.statusColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repo: SettingsRepository, val notifier: Notifier) : ViewModel() {
    /** Null while loading. */
    val settings: StateFlow<Settings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The background jobs follow these on their own: the app watches the settings and reschedules. */
    fun change(prefs: NoticePrefs) = viewModelScope.launch { repo.setNotices(prefs) }
}

/** Design: 9a · Notifications. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpenRules: () -> Unit, vm: NotificationsViewModel = hiltViewModel()) {
    val settings = vm.settings.collectAsStateWithLifecycle().value ?: return // first frame while loading
    val prefs = settings.notices
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(vm.notifier.allowed) }
    // Android only shows its own prompt a couple of times; after a refusal the way back is its settings.
    var refused by rememberSaveable { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = vm.notifier.allowed
        refused = !granted
    }
    // The answer may also change in Android's settings while this screen is behind them.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) allowed = vm.notifier.allowed }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var pickingCadence by rememberSaveable { mutableStateOf(false) }

    if (pickingTime) {
        val time = rememberTimePickerState(initialHour = prefs.hour, initialMinute = prefs.minute)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Time of day") },
            text = { TimePicker(state = time) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.change(prefs.copy(hour = time.hour, minute = time.minute))
                        pickingTime = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
    if (pickingCadence) {
        AlertDialog(
            onDismissRequest = { pickingCadence = false },
            title = { Text("Mileage check-in") },
            text = {
                Column(Modifier.selectableGroup()) {
                    MileageCadence.entries.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = c == prefs.mileageCadence,
                                role = Role.RadioButton,
                                onClick = {
                                    vm.change(prefs.copy(mileageCadence = c))
                                    pickingCadence = false
                                },
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = c == prefs.mileageCadence, onClick = null)
                            Text(c.label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingCadence = false }) { Text("Cancel") } },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 8.dp, top = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text(
                "Notifications",
                Modifier.padding(start = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (allowed) {
                Surface(color = scheme.tertiaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = scheme.onTertiaryContainer)
                        Text("Allowed in Android settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.onTertiaryContainer)
                    }
                }
            } else {
                Surface(
                    color = scheme.primaryContainer,
                    contentColor = scheme.onSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, scheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Notifications are off for Glovebox", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Nothing below can reach you until Android allows it. Reminders still show inside the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColors().soon,
                        )
                        // Below Android 13 there is no prompt to show: the switch is in Android's settings.
                        val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !refused
                        Button(
                            onClick = { if (canAsk) ask.launch(Manifest.permission.POST_NOTIFICATIONS) else context.openNotificationSettings() },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(if (canAsk) "Allow notifications" else "Open Android settings")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("TELL ME ABOUT")
                Card {
                    SwitchRow("Service coming due", "Based on your reminder rules", prefs.serviceDue) { vm.change(prefs.copy(serviceDue = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow("Overdue nudges", "Once a week until it's logged or snoozed", prefs.overdue) { vm.change(prefs.copy(overdue = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow("New recalls", "Checked ${settings.recallCadence.label.lowercase()} for each vehicle".takeIf { settings.recallCadence.days != null } ?: "Automatic recall checks are off in Settings", prefs.recalls) {
                        vm.change(prefs.copy(recalls = it))
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow("Mileage check-in", "A quick prompt to update the odometer", prefs.mileage) { vm.change(prefs.copy(mileage = it)) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("WHEN")
                Card {
                    ChoiceRow("Time of day", NoticeRules.timeLabel(prefs.hour, prefs.minute)) { pickingTime = true }
                    HorizontalDivider(color = scheme.outlineVariant)
                    ChoiceRow("Mileage check-in", prefs.mileageCadence.label) { pickingCadence = true }
                    HorizontalDivider(color = scheme.outlineVariant)
                    // The same lead time as on Reminder rules; it is kept in one place.
                    ChoiceRow("How early to remind", "${settings.lead.miles.asMiles()} / ${settings.lead.days} days", onClick = onOpenRules)
                }
                Text(
                    "Mileage reminders only work if the odometer is current, so we'll ask you for a quick reading on a schedule. " +
                        "The time of day is approximate: Android runs background checks when it suits the battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, about: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun Context.openNotificationSettings() {
    startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName))
}
