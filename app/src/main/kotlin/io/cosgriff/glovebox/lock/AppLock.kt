package io.cosgriff.glovebox.lock

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.LockDelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Biometrics gate the UI only — the database key itself never needs a fingerprint, so
 * background reminder work keeps running while the phone is locked.
 *
 * Starts locked on every process start. Re-locks when the app returns from the background after
 * the "Lock the app" delay from Settings.
 */
@Singleton
class AppLock @Inject constructor(settingsRepo: SettingsRepository) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settings = settingsRepo.settings
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAt: Long? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** True once settings have loaded and the user has turned biometric lock off. */
    fun lockDisabled(): Boolean = settings.value?.biometricLock == false

    fun unlock() { _locked.value = false }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        val since = backgroundedAt ?: return
        val s = settings.value ?: return
        if (!s.biometricLock) return
        val elapsedSec = (SystemClock.elapsedRealtime() - since) / 1000
        if (LockDelay.relocks(s.lockAfterSeconds, elapsedSec)) _locked.value = true
    }
}
