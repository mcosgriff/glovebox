package io.cosgriff.glovebox.lock

import androidx.compose.runtime.rememberUpdatedState
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.app.KeyguardManager
import android.app.Activity
import io.cosgriff.glovebox.domain.ScreenLock
import android.content.Context
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.res.stringResource
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Unlock
import io.cosgriff.glovebox.domain.UnlockError
import io.cosgriff.glovebox.domain.UnlockMessage
import io.cosgriff.glovebox.domain.UnlockPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// BIOMETRIC_STRONG or DEVICE_CREDENTIAL is unsupported on API 29 and throws.
private val authenticators: Int
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG else BIOMETRIC_WEAK) or DEVICE_CREDENTIAL

/** False on a phone with no screen lock and no biometrics enrolled: there is nothing to lock the app with. */
fun Context.canLockApp(): Boolean = BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

fun Context.screenLock(): ScreenLock = when {
    !canLockApp() -> ScreenLock.NONE
    BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> ScreenLock.BIOMETRIC
    else -> ScreenLock.CREDENTIAL
}

/**
 * Asks Android for the fingerprint, face or screen lock, for unlocking the app and before something
 * drastic. Returns the function that asks; `screenLockOnly` skips the biometric where Android can
 * (11 and later). [onError] is not called for a finger that isn't recognised: Android says so in its
 * own prompt and lets them try again.
 *
 * **Android 10 goes a different way, on purpose.** There the biometric prompt can't offer the screen
 * lock itself; the system puts its own confirm-credentials screen in front, the app's activity
 * stops, and androidx.biometric (1.1.0) cancels the check when it sees the stop. The user taps
 * "Use PIN", enters it correctly, and nothing comes back: with a finger that won't read they could
 * never get in. So on Android 10 that same system screen is opened directly and answered as an
 * ordinary activity result. It still offers the fingerprint first, with the screen lock beside it.
 */
@Composable
fun rememberScreenLockCheck(
    title: String,
    subtitle: String,
    onPassed: () -> Unit,
    onError: (error: UnlockError, systemMessage: String?) -> Unit = { _, _ -> },
): (screenLockOnly: Boolean) -> Unit {
    val activity = LocalActivity.current as FragmentActivity
    val passed by rememberUpdatedState(onPassed)
    val failed by rememberUpdatedState(onError)
    val systemScreen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Anything but OK is the user backing out; Android has already said whatever else there was to say.
        if (result.resultCode == Activity.RESULT_OK) passed() else failed(UnlockError.CANCELLED, null)
    }
    return remember(activity, title, subtitle) {
        { screenLockOnly ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION") // Deprecated for the prompt that doesn't work here; see above.
                val intent = activity.getSystemService(KeyguardManager::class.java)?.createConfirmDeviceCredentialIntent(title, subtitle)
                // Null when the phone has no screen lock, which callers have already ruled out.
                if (intent != null) systemScreen.launch(intent) else failed(UnlockError.UNAVAILABLE, null)
            } else {
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(if (screenLockOnly) DEVICE_CREDENTIAL else authenticators) // PIN fallback
                    .build()
                BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = passed()
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = failed(unlockError(errorCode), errString.toString())
                    },
                ).authenticate(info)
            }
        }
    }
}

private fun unlockError(code: Int): UnlockError = when (code) {
    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_CANCELED -> UnlockError.CANCELLED
    BiometricPrompt.ERROR_LOCKOUT -> UnlockError.LOCKED_OUT
    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> UnlockError.LOCKED_OUT_UNTIL_SCREEN_LOCK
    BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_HW_NOT_PRESENT,
    BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
    -> UnlockError.UNAVAILABLE
    else -> UnlockError.OTHER
}

/**
 * Design: 0 · Biometric unlock. The check itself is Android's own prompt, shown over this screen;
 * this is what is behind it, and what is left when the prompt is dismissed. Success opens the app.
 * This gates the screens only: the data is not tied to the fingerprint (see AppLock).
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val activity = LocalActivity.current as FragmentActivity
    // Read again on every return: a screen lock or fingerprint can be added or removed in Android's settings.
    var lock by remember { mutableStateOf(activity.screenLock()) }
    LifecycleResumeEffect(Unit) {
        lock = activity.screenLock()
        onPauseOrDispose {}
    }
    val fingerprintSensor = remember { activity.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) }
    // Asking for the screen lock alone is unsupported before Android 11.
    val screenLockAlone = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val options = Unlock.options(lock, fingerprintSensor, screenLockAlone)
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val check = rememberScreenLockCheck(
        title = stringResource(R.string.unlock_prompt_title),
        subtitle = stringResource(R.string.unlock_locked_and_encrypted),
        onPassed = onUnlocked,
        onError = { error, systemMessage -> message = Unlock.message(error, activity.screenLock(), systemMessage)?.let(activity::words) },
    )

    fun prompt(screenLockOnly: Boolean = false) {
        message = null
        check(screenLockOnly)
    }

    // Nothing to gate with, so don't trap the user. Otherwise ask once on arrival; after that it is up to them.
    LaunchedEffect(options == null) { if (options == null) onUnlocked() else prompt() }

    Surface(color = scheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(start = 24.dp, top = 96.dp, end = 24.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = scheme.primary, shape = RoundedCornerShape(20.dp), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(38.dp), tint = scheme.onPrimary)
                }
            }
            Text(stringResource(R.string.app_name), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = scheme.onSurfaceVariant)
                Text(stringResource(R.string.unlock_locked_and_encrypted), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }

            if (options == null) return@Column // about to open

            Spacer(Modifier.weight(1f))

            // Light: a soft orange disc with an orange ring. Dark: the ring alone, in the lighter orange.
            val dark = scheme.background.luminance() < 0.5f
            val ring = if (dark) scheme.onPrimaryContainer else scheme.primary
            Surface(
                onClick = { prompt() },
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = if (dark) Color.Transparent else scheme.primaryContainer,
                contentColor = ring,
                border = BorderStroke(2.dp, ring),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (options.fingerprint) Icons.Outlined.Fingerprint else Icons.Outlined.Lock,
                        contentDescription = stringResource(options.prompt.buttonLabel),
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Text(stringResource(options.prompt.instruction), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            message?.let {
                Text(
                    it,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            if (options.screenLockLink) {
                Box(
                    Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { prompt(screenLockOnly = true) }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.unlock_use_screen_lock),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ring,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}

private val UnlockPrompt.instruction: Int
    get() = when (this) {
        UnlockPrompt.SCREEN_LOCK -> R.string.unlock_instruction_screen_lock
        UnlockPrompt.FINGERPRINT -> R.string.unlock_instruction_fingerprint
        UnlockPrompt.BIOMETRIC -> R.string.unlock_instruction_biometric
    }

/** What the round button does, for a screen reader. */
private val UnlockPrompt.buttonLabel: Int
    get() = when (this) {
        UnlockPrompt.SCREEN_LOCK -> R.string.unlock_button_screen_lock
        UnlockPrompt.FINGERPRINT -> R.string.unlock_button_fingerprint
        UnlockPrompt.BIOMETRIC -> R.string.unlock_button_biometric
    }

// Worded once, when it happens, so the saved message is plain text.
private fun Context.words(message: UnlockMessage): String = when (message) {
    UnlockMessage.WaitOrUseScreenLock -> getString(R.string.unlock_wait_or_use_screen_lock)
    UnlockMessage.Wait -> getString(R.string.unlock_wait)
    UnlockMessage.OffUntilScreenLock -> getString(R.string.unlock_off_until_screen_lock)
    is UnlockMessage.FromSystem -> message.text
    UnlockMessage.CouldntCheck -> getString(R.string.unlock_couldnt_check)
}
