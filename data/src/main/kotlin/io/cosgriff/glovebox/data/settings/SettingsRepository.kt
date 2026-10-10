package io.cosgriff.glovebox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.TorqueUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val biometricLock: Boolean = true,
    /** 0 = lock immediately when the app leaves the foreground. -1 = only on restart. */
    val lockAfterSeconds: Int = 0,
    val hideInRecents: Boolean = true,
    val defaultRule: DueRule = DueRule.WHICHEVER_FIRST,
    val torqueUnit: TorqueUnit = TorqueUnit.FOOT_POUND,
)

private val Context.dataStore by preferencesDataStore("settings")

/** Non-sensitive preferences only — vehicle data lives in the encrypted database. */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val biometric = booleanPreferencesKey("biometric_lock")
        val lockAfter = intPreferencesKey("lock_after_seconds")
        val hideRecents = booleanPreferencesKey("hide_in_recents")
        val rule = stringPreferencesKey("default_rule")
        val torque = stringPreferencesKey("torque_unit")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            biometricLock = p[Keys.biometric] ?: d.biometricLock,
            lockAfterSeconds = p[Keys.lockAfter] ?: d.lockAfterSeconds,
            hideInRecents = p[Keys.hideRecents] ?: d.hideInRecents,
            defaultRule = p[Keys.rule]?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: d.defaultRule,
            torqueUnit = p[Keys.torque]?.let { runCatching { TorqueUnit.valueOf(it) }.getOrNull() } ?: d.torqueUnit,
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setBiometricLock(on: Boolean) = context.dataStore.edit { it[Keys.biometric] = on }
    suspend fun setLockAfterSeconds(s: Int) = context.dataStore.edit { it[Keys.lockAfter] = s }
    suspend fun setHideInRecents(on: Boolean) = context.dataStore.edit { it[Keys.hideRecents] = on }
    suspend fun setDefaultRule(rule: DueRule) = context.dataStore.edit { it[Keys.rule] = rule.name }
    suspend fun setTorqueUnit(unit: TorqueUnit) = context.dataStore.edit { it[Keys.torque] = unit.name }
}
