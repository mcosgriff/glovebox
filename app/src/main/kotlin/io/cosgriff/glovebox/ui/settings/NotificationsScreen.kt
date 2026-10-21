package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.ui.components.ValueRow
import io.cosgriff.glovebox.ui.components.SwitchRow
import io.cosgriff.glovebox.ui.components.ScreenHeader
import io.cosgriff.glovebox.ui.components.RowsCard
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.PaddingValues
import io.cosgriff.glovebox.widget.GarageWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import android.appwidget.AppWidgetManager
import androidx.compose.ui.res.pluralStringResource
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.R
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.domain.DistanceUnit
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.VehicleCheckIn
import io.cosgriff.glovebox.notify.Notifier
import io.cosgriff.glovebox.ui.components.SectionLabel
import io.cosgriff.glovebox.ui.service.asDistance
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

    /** The app switches the widget's receiver to follow (GloveboxApp), as it does the background jobs. */
    fun setHomeWidget(on: Boolean) = viewModelScope.launch { repo.setHomeWidget(on) }
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
    var pickingMileageTime by rememberSaveable { mutableStateOf(false) }

    if (pickingTime) {
        val time = rememberTimePickerState(initialHour = prefs.hour, initialMinute = prefs.minute)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(R.string.notifications_time)) },
            text = { TimePicker(state = time) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.change(prefs.copy(hour = time.hour, minute = time.minute))
                        pickingTime = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (pickingMileageTime) {
        val time = rememberTimePickerState(initialHour = prefs.mileageHour ?: prefs.hour, initialMinute = if (prefs.mileageHour != null) prefs.mileageMinute else prefs.minute)
        AlertDialog(
            onDismissRequest = { pickingMileageTime = false },
            title = { Text(stringResource(R.string.notifications_mileage_time)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.notifications_mileage_time_note), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    TimePicker(state = time)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.change(prefs.copy(mileageHour = time.hour, mileageMinute = time.minute))
                        pickingMileageTime = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                Row {
                    if (prefs.mileageHour != null) {
                        TextButton(
                            onClick = {
                                vm.change(prefs.copy(mileageHour = null, mileageMinute = 0))
                                pickingMileageTime = false
                            },
                        ) { Text(stringResource(R.string.notifications_mileage_time_same)) }
                    }
                    TextButton(onClick = { pickingMileageTime = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
    if (pickingCadence) {
        CheckInDialog(
            current = VehicleCheckIn.Every(prefs.mileageEveryDays),
            default = null,
            onSave = { choice ->
                (choice as? VehicleCheckIn.Every)?.let { vm.change(prefs.copy(mileageEveryDays = it.days)) }
                pickingCadence = false
            },
            onDismiss = { pickingCadence = false },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.notifications_title), onBack)

        Column(
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (allowed) {
                Surface(color = scheme.tertiaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = scheme.onTertiaryContainer)
                        Text(stringResource(R.string.notifications_allowed), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.onTertiaryContainer)
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
                        Text(stringResource(R.string.notifications_blocked), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.notifications_blocked_note),
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
                            Text(stringResource(if (canAsk) R.string.notifications_allow else R.string.notifications_open_settings))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.notifications_tell_me))
                RowsCard {
                    SwitchRow(stringResource(R.string.channel_due), stringResource(R.string.notifications_due_note), prefs.serviceDue) { vm.change(prefs.copy(serviceDue = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(stringResource(R.string.channel_overdue), stringResource(R.string.channel_overdue_about), prefs.overdue) { vm.change(prefs.copy(overdue = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(stringResource(R.string.channel_recalls), stringResource(R.string.notifications_recalls_checked, settings.recallCadence.inSentence).takeIf { settings.recallCadence.days != null } ?: stringResource(R.string.notifications_recalls_off), prefs.recalls) {
                        vm.change(prefs.copy(recalls = it))
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(stringResource(R.string.channel_mileage), stringResource(R.string.notifications_mileage_note), prefs.mileage) { vm.change(prefs.copy(mileage = it)) }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SwitchRow(stringResource(R.string.notifications_widget), stringResource(R.string.notifications_widget_note), settings.homeWidget, onChange = vm::setHomeWidget)
                    if (settings.homeWidget) AddWidget()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.notifications_when))
                RowsCard {
                    ValueRow(stringResource(R.string.notifications_time), timeLabel(prefs.hour, prefs.minute)) { pickingTime = true }
                    HorizontalDivider(color = scheme.outlineVariant)
                    ValueRow(stringResource(R.string.channel_mileage), checkInLabel(prefs.mileageEveryDays)) { pickingCadence = true }
                    HorizontalDivider(color = scheme.outlineVariant)
                    ValueRow(
                        stringResource(R.string.notifications_mileage_time),
                        prefs.mileageHour?.let { timeLabel(it, prefs.mileageMinute) } ?: stringResource(R.string.notifications_mileage_time_with, timeLabel(prefs.hour, prefs.minute)),
                    ) { pickingMileageTime = true }
                    HorizontalDivider(color = scheme.outlineVariant)
                    // The same lead time as on Reminder rules; it is kept in one place.
                    ValueRow(stringResource(R.string.notifications_how_early), stringResource(R.string.notifications_how_early_value, settings.lead.miles.asDistance(settings.distanceUnit), pluralStringResource(R.plurals.days, settings.lead.days, settings.lead.days.asNumber())), onClick = onOpenRules)
                }
                Text(
                    stringResource(R.string.notifications_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Where to find the widget once it is on. Android can offer to place it, but not every launcher takes the
 * request (Kvaesitso doesn't), so the button is only shown where it can work; otherwise a line says where.
 */
@Composable
private fun AddWidget() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canPin = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    if (canPin) {
        TextButton(
            onClick = { scope.launch { GlanceAppWidgetManager(context).requestPinGlanceAppWidget(GarageWidgetReceiver::class.java) } },
            modifier = Modifier.heightIn(min = 48.dp).padding(bottom = 4.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(stringResource(R.string.notifications_widget_add))
        }
    } else {
        Text(
            stringResource(R.string.notifications_widget_from_launcher),
            Modifier.padding(bottom = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun Context.openNotificationSettings() {
    startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName))
}
