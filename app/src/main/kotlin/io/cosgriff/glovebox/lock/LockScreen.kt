package io.cosgriff.glovebox.lock

import io.cosgriff.glovebox.domain.ScreenLock
import android.content.Context
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

/** Asks for the fingerprint, face or screen lock before something drastic. Cancelling does nothing. */
fun FragmentActivity.confirmWithScreenLock(title: String, subtitle: String, onConfirmed: () -> Unit) {
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(authenticators)
        .build()
    BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onConfirmed()
        },
    ).authenticate(info)
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalActivity.current as FragmentActivity
    // Nothing to gate with, so don't trap the user.
    val canAuthenticate = activity.canLockApp()

    fun prompt() {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Glovebox")
            .setSubtitle("Your data is locked and encrypted")
            .setAllowedAuthenticators(authenticators) // PIN fallback
            .build()
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()
            },
        ).authenticate(info)
    }

    LaunchedEffect(Unit) { if (canAuthenticate) prompt() else onUnlocked() }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.size(96.dp))
            Text("Glovebox", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Your data is locked and encrypted",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(onClick = ::prompt, modifier = Modifier.size(96.dp)) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = "Unlock", Modifier.size(48.dp))
            }
            Text("Touch the sensor to open your Glovebox")
            Spacer(Modifier.size(32.dp))
        }
    }
}
