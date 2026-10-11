package io.cosgriff.glovebox

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.cosgriff.glovebox.data.settings.Settings
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.lock.AppLock
import io.cosgriff.glovebox.lock.LockScreen
import io.cosgriff.glovebox.ui.GloveboxNavHost
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import io.cosgriff.glovebox.ui.vehicle.VehiclePhotoPicker
import javax.inject.Inject

// FragmentActivity (not plain ComponentActivity) because BiometricPrompt needs it.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appLock: AppLock
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var photoPicker: VehiclePhotoPicker

    // Registered here, not in a screen, so the answer still arrives if the app locked meanwhile.
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { photoPicker.onPicked(it) }

    private val launchPhotoPicker: () -> Unit = {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        photoPicker.attach(launchPhotoPicker)
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = null)
            val locked by appLock.locked.collectAsStateWithLifecycle()

            LaunchedEffect(settings?.hideInRecents) { applySecureFlag(settings) }
            LaunchedEffect(settings?.biometricLock) {
                if (settings?.biometricLock == false) appLock.unlock()
            }

            // The lock screen takes the app's screens out of composition, so nothing of them (sheets
            // and dialogs included) is on screen or readable while locked. Their saved state is kept
            // here, which puts the user back where they were once they unlock.
            val place = rememberSaveableStateHolder()

            GloveboxTheme(mode = settings?.theme ?: Settings().theme) {
                when {
                    settings == null -> Unit // first frame while DataStore loads
                    locked && settings?.biometricLock == true -> LockScreen(onUnlocked = appLock::unlock)
                    else -> place.SaveableStateProvider(APP_SCREENS) { GloveboxNavHost() }
                }
            }
        }
    }

    override fun onDestroy() {
        photoPicker.detach(launchPhotoPicker)
        super.onDestroy()
    }

    private companion object {
        const val APP_SCREENS = "app"
    }

        /** "Hide in recent apps": blocks screenshots and blanks the app-switcher thumbnail. */
    private fun applySecureFlag(settings: Settings?) {
        if (settings?.hideInRecents != false) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
