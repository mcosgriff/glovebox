package io.cosgriff.glovebox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.MileageCadence
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.TorqueUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val biometricLock: Boolean = true,
    /** 0 = lock immediately when the app leaves the foreground. -1 = only on restart. */
    val lockAfterSeconds: Int = 0,
    val hideInRecents: Boolean = true,
    val defaultRule: DueRule = DueRule.WHICHEVER_FIRST,
    /** How early a reminder starts counting as due soon. */
    val lead: LeadTime = LeadTime(),
    val torqueUnit: TorqueUnit = TorqueUnit.FOOT_POUND,
    /**
     * What a vehicle added from now on counts in, until the user says otherwise for it. A vehicle keeps
     * its own unit (`vehicles.distanceUnit`); changing this changes nothing already in the garage.
     */
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val notices: NoticePrefs = NoticePrefs(),
    /** How often NHTSA is asked about recalls without the user asking. */
    val recallCadence: RecallCadence = RecallCadence.WEEKLY,
    /** The first-run welcome has been shown, or wasn't needed. */
    val welcomeSeen: Boolean = false,
    /** When a backup file was last saved from this phone; null = never. */
    val lastBackupOn: LocalDate? = null,
    val backupReminder: Boolean = true,
    /** When the backup reminder last went out. */
    val backupRemindedOn: LocalDate? = null,
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
        val leadMiles = intPreferencesKey("lead_miles")
        val leadDays = intPreferencesKey("lead_days")
        val torque = stringPreferencesKey("torque_unit")
        val distance = stringPreferencesKey("distance_unit")
        val notifyDue = booleanPreferencesKey("notify_service_due")
        val notifyOverdue = booleanPreferencesKey("notify_overdue")
        val notifyRecalls = booleanPreferencesKey("notify_recalls")
        val notifyMileage = booleanPreferencesKey("notify_mileage")
        val notifyHour = intPreferencesKey("notify_hour")
        val notifyMinute = intPreferencesKey("notify_minute")
        val mileageCadence = stringPreferencesKey("mileage_cadence")
        val recallCadence = stringPreferencesKey("recall_cadence")
        val welcomeSeen = booleanPreferencesKey("welcome_seen")
        val lastBackup = longPreferencesKey("last_backup_on")
        val backupReminder = booleanPreferencesKey("backup_reminder")
        val backupReminded = longPreferencesKey("backup_reminded_on")
    }

    // Facts about this phone, not choices: they mean nothing on the phone a backup is restored to.
    private val deviceOnly = setOf(Keys.lastBackup.name, Keys.backupReminded.name, Keys.welcomeSeen.name)

    // This phone's own security choices. A backup made on a phone with the lock off must not switch it off here.
    private val keptOnRestore = setOf(Keys.biometric.name, Keys.lockAfter.name, Keys.hideRecents.name)

    /**
     * Puts back settings read from a backup: name to value, as [raw] wrote them. Security settings
     * and anything this version doesn't know are left alone.
     */
    suspend fun restore(values: Map<String, Any>) {
        context.dataStore.edit { p ->
            for ((name, value) in values) {
                if (name in keptOnRestore || name in deviceOnly) continue
                when (value) {
                    is Boolean -> p[booleanPreferencesKey(name)] = value
                    is Int -> p[intPreferencesKey(name)] = value
                    is Long -> p[longPreferencesKey(name)] = value
                    is String -> p[stringPreferencesKey(name)] = value
                }
            }
        }
    }

    /** Every setting as stored, for the backup file. */
    suspend fun raw(): Map<String, Any> = context.dataStore.data.first().asMap().mapKeys { it.key.name }.filterKeys { it !in deviceOnly }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            biometricLock = p[Keys.biometric] ?: d.biometricLock,
            lockAfterSeconds = p[Keys.lockAfter] ?: d.lockAfterSeconds,
            hideInRecents = p[Keys.hideRecents] ?: d.hideInRecents,
            defaultRule = p[Keys.rule]?.let { runCatching { DueRule.valueOf(it) }.getOrNull() } ?: d.defaultRule,
            lead = LeadTime(p[Keys.leadMiles] ?: d.lead.miles, p[Keys.leadDays] ?: d.lead.days),
            torqueUnit = p[Keys.torque]?.let { runCatching { TorqueUnit.valueOf(it) }.getOrNull() } ?: d.torqueUnit,
            distanceUnit = p[Keys.distance]?.let(DistanceUnit::of) ?: d.distanceUnit,
            notices = NoticePrefs(
                serviceDue = p[Keys.notifyDue] ?: d.notices.serviceDue,
                overdue = p[Keys.notifyOverdue] ?: d.notices.overdue,
                recalls = p[Keys.notifyRecalls] ?: d.notices.recalls,
                mileage = p[Keys.notifyMileage] ?: d.notices.mileage,
                hour = (p[Keys.notifyHour] ?: d.notices.hour).coerceIn(0, 23),
                minute = (p[Keys.notifyMinute] ?: d.notices.minute).coerceIn(0, 59),
                mileageCadence = p[Keys.mileageCadence]?.let { runCatching { MileageCadence.valueOf(it) }.getOrNull() } ?: d.notices.mileageCadence,
            ),
            recallCadence = p[Keys.recallCadence]?.let { runCatching { RecallCadence.valueOf(it) }.getOrNull() } ?: d.recallCadence,
            welcomeSeen = p[Keys.welcomeSeen] ?: d.welcomeSeen,
            lastBackupOn = p[Keys.lastBackup]?.let(LocalDate::ofEpochDay),
            backupReminder = p[Keys.backupReminder] ?: d.backupReminder,
            backupRemindedOn = p[Keys.backupReminded]?.let(LocalDate::ofEpochDay),
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setBiometricLock(on: Boolean) = context.dataStore.edit { it[Keys.biometric] = on }
    suspend fun setLockAfterSeconds(s: Int) = context.dataStore.edit { it[Keys.lockAfter] = s }
    suspend fun setHideInRecents(on: Boolean) = context.dataStore.edit { it[Keys.hideRecents] = on }
    suspend fun setDefaultRule(rule: DueRule) = context.dataStore.edit { it[Keys.rule] = rule.name }
    suspend fun setDistanceUnit(unit: DistanceUnit) = context.dataStore.edit { it[Keys.distance] = unit.name }
    suspend fun setLeadTime(lead: LeadTime) = context.dataStore.edit {
        it[Keys.leadMiles] = lead.miles
        it[Keys.leadDays] = lead.days
    }
    suspend fun setTorqueUnit(unit: TorqueUnit) = context.dataStore.edit { it[Keys.torque] = unit.name }
    suspend fun setNotices(prefs: NoticePrefs) = context.dataStore.edit {
        it[Keys.notifyDue] = prefs.serviceDue
        it[Keys.notifyOverdue] = prefs.overdue
        it[Keys.notifyRecalls] = prefs.recalls
        it[Keys.notifyMileage] = prefs.mileage
        it[Keys.notifyHour] = prefs.hour
        it[Keys.notifyMinute] = prefs.minute
        it[Keys.mileageCadence] = prefs.mileageCadence.name
    }
    suspend fun setRecallCadence(cadence: RecallCadence) = context.dataStore.edit { it[Keys.recallCadence] = cadence.name }
    suspend fun setWelcomeSeen() = context.dataStore.edit { it[Keys.welcomeSeen] = true }
    suspend fun setLastBackup(date: LocalDate) = context.dataStore.edit { it[Keys.lastBackup] = date.toEpochDay() }
    suspend fun setBackupReminder(on: Boolean) = context.dataStore.edit { it[Keys.backupReminder] = on }
    suspend fun setBackupReminded(date: LocalDate) = context.dataStore.edit { it[Keys.backupReminded] = date.toEpochDay() }
}
