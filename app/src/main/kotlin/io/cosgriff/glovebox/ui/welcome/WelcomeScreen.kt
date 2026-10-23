package io.cosgriff.glovebox.ui.welcome

import io.cosgriff.glovebox.ApplicationScope
import io.cosgriff.glovebox.ui.components.PrimaryButton
import io.cosgriff.glovebox.ui.components.SwitchRow
import io.cosgriff.glovebox.ui.components.BorderedCard
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build as WrenchIcon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.Welcome
import io.cosgriff.glovebox.lock.canLockApp
import io.cosgriff.glovebox.ui.components.SectionLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** What the user set on the welcome screen. */
data class WelcomeChoice(val lockApp: Boolean, val notifications: Boolean)

enum class WelcomeAction { ADD_VEHICLE, RESTORE }

/** Decides, once per launch, whether this is a first run, and records the welcome's outcome. */
@Singleton
class FirstRun @Inject constructor(private val settings: SettingsRepository, private val vehicles: VehicleDao, @ApplicationScope private val scope: CoroutineScope) {
    private val _needsWelcome = MutableStateFlow<Boolean?>(null)

    /** Null until the settings and the database have been read. */
    val needsWelcome: StateFlow<Boolean?> = _needsWelcome.asStateFlow()

    init {
        scope.launch {
            val seen = settings.settings.first().welcomeSeen
            val show = Welcome.shouldShow(seen, hasVehicles = !seen && vehicles.all().isNotEmpty())
            // An install from before the welcome existed: note that it has no need of one.
            if (!show && !seen) settings.setWelcomeSeen()
            _needsWelcome.value = show
        }
    }

    suspend fun finish(choice: WelcomeChoice) {
        settings.setBiometricLock(choice.lockApp)
        if (!choice.notifications) {
            // Everything off; each kind can be turned on again under Settings, Notifications.
            val current = settings.settings.first().notices
            settings.setNotices(current.copy(serviceDue = false, overdue = false, recalls = false, mileage = false))
            settings.setBackupReminder(false)
        }
        settings.setWelcomeSeen()
        _needsWelcome.value = false
    }
}

/** Design: 0a · First-run welcome. Shown once, before the lock, on a phone with nothing on it yet. */
@Composable
fun WelcomeScreen(onDone: (WelcomeChoice, WelcomeAction) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val canLock = remember { context.canLockApp() }
    var lockApp by rememberSaveable { mutableStateOf(canLock) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var then by rememberSaveable { mutableStateOf<WelcomeAction?>(null) }
    // Android 13 and later ask before the first notification can be shown; older versions allow them from the start.
    val asks = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Whatever the answer, carry on. The switches stay on; Settings shows if Android is blocking them.
        then?.let { onDone(WelcomeChoice(lockApp && canLock, notifications), it) }
    }

    fun go(action: WelcomeAction) {
        if (notifications && asks) {
            then = action
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onDone(WelcomeChoice(lockApp && canLock, notifications), action)
        }
    }

    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 40.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(color = scheme.primary, shape = RoundedCornerShape(18.dp), modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.WrenchIcon, contentDescription = null, modifier = Modifier.size(34.dp), tint = scheme.onPrimary)
                    }
                }
                Text(
                    stringResource(R.string.welcome_title),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.welcome_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.welcome_set_up))
                BorderedCard {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SwitchRow(
                            title = stringResource(R.string.welcome_lock_title),
                            about = stringResource(
                                // Said plainly: with no screen lock there is nothing for the app to lock with.
                                if (canLock) R.string.welcome_lock_fallback else R.string.welcome_lock_no_screen_lock,
                            ),
                            checked = lockApp && canLock,
                            enabled = canLock,
                            onChange = { lockApp = it },
                        )
                        HorizontalDivider(color = scheme.outlineVariant)
                        SwitchRow(
                            title = stringResource(R.string.welcome_notifications_title),
                            about = stringResource(
                                when {
                                    !notifications -> R.string.welcome_notifications_off
                                    asks -> R.string.welcome_notifications_ask
                                    else -> R.string.welcome_notifications_on
                                },
                            ),
                            checked = notifications,
                            onChange = { notifications = it },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(onClick = { go(WelcomeAction.ADD_VEHICLE) }) {
                    Text(stringResource(R.string.welcome_add_vehicle), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = { go(WelcomeAction.RESTORE) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.5.dp, scheme.onSurface),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
                ) {
                    Text(stringResource(R.string.welcome_restore))
                }
                Text(
                    stringResource(R.string.welcome_privacy),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

