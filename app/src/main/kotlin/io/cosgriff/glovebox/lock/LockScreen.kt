package io.cosgriff.glovebox.lock

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

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalActivity.current as FragmentActivity
    // BIOMETRIC_STRONG or DEVICE_CREDENTIAL is unsupported on API 29 and throws.
    val authenticators =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG else BIOMETRIC_WEAK) or DEVICE_CREDENTIAL
    // No screen lock and no biometrics enrolled: there is nothing to gate with, so don't trap the user.
    val canAuthenticate =
        BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

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
