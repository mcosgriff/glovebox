package io.cosgriff.glovebox.data.settings

import io.cosgriff.glovebox.domain.enumOrNull
import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.cosgriff.glovebox.domain.DistanceConversion
import io.cosgriff.glovebox.domain.DistanceKind
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.forUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.MileageCheckIns
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
    /** How early a reminder starts counting as due soon. Its distance is as typed, in [distanceUnit]; use [leadFor] for a vehicle. */
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
    /** The VIN scanner shows what it is reading (the band, any barcode found, the print), for a scan that won't take. */
    val scannerDetails: Boolean = false,
    /**
     * The home-screen widget is offered (#238). Off by default: it shows vehicles and their next service
     * outside the app lock. The widget's receiver is switched on and off with it.
     */
    val homeWidget: Boolean = false,
) {
    /** [lead] for a vehicle that counts in [unit]: this is what every reminder calculation takes. */
    fun leadFor(unit: DistanceUnit): LeadTime = lead.forUnit(unit, from = distanceUnit)
}

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
        val notifyDocuments = booleanPreferencesKey("notify_documents")
        val notifyHour = intPreferencesKey("notify_hour")
        val notifyMinute = intPreferencesKey("notify_minute")
        val mileageEveryDays = intPreferencesKey("mileage_every_days")
        /** The mileage check-in's own time of day; absent asks with everything else. */
        val mileageHour = intPreferencesKey("notify_mileage_hour")
        val mileageMinute = intPreferencesKey("notify_mileage_minute")

        /** The three-way choice before #287 (a MileageCheckIns.legacy name); read until the setting is next written. */
        val mileageCadence = stringPreferencesKey("mileage_cadence")
        val recallCadence = stringPreferencesKey("recall_cadence")
        val welcomeSeen = booleanPreferencesKey("welcome_seen")
        val lastBackup = longPreferencesKey("last_backup_on")
        val backupReminder = booleanPreferencesKey("backup_reminder")
        val backupReminded = longPreferencesKey("backup_reminded_on")
        val scannerDetails = booleanPreferencesKey("scanner_details")
        val homeWidget = booleanPreferencesKey("home_widget")
    }

    // Facts about this phone, not choices: they mean nothing on the phone a backup is restored to.
    private val deviceOnly = setOf(Keys.lastBackup.name, Keys.backupReminded.name, Keys.welcomeSeen.name)

    // This phone's own security choices. A backup made on a phone with the lock off must not switch it off here.
    // The widget is one of them: it shows the garage without unlocking.
    private val keptOnRestore = setOf(Keys.biometric.name, Keys.lockAfter.name, Keys.hideRecents.name, Keys.homeWidget.name)

    /** Every setting this version knows, by name, each putting back a value only of its own type. */
    private val restorable: Map<String, (MutablePreferences, Any) -> Unit> = with(Keys) {
        fun <T : Any> typed(key: Preferences.Key<T>, cast: (Any) -> T?) = key.name to { p: MutablePreferences, v: Any -> cast(v)?.let { p[key] = it }; Unit }
        fun bool(key: Preferences.Key<Boolean>) = typed(key) { it as? Boolean }
        fun int(key: Preferences.Key<Int>) = typed(key) { it as? Int }
        fun long(key: Preferences.Key<Long>) = typed(key) { it as? Long }
        fun string(key: Preferences.Key<String>) = typed(key) { it as? String }
        mapOf(
            string(theme), bool(biometric), int(lockAfter), bool(hideRecents), string(rule), int(leadMiles), int(leadDays),
            string(torque), string(distance), bool(notifyDue), bool(notifyOverdue), bool(notifyRecalls), bool(notifyMileage), bool(notifyDocuments),
            int(notifyHour), int(notifyMinute), int(mileageEveryDays), int(mileageHour), int(mileageMinute), string(recallCadence),
            bool(welcomeSeen), long(lastBackup), bool(backupReminder), long(backupReminded), bool(scannerDetails), bool(homeWidget),
        )
    }

    /**
     * Puts back settings read from a backup: name to value, as [raw] wrote them. Security settings
     * and anything this version doesn't know are left alone.
     */
    suspend fun restore(values: Map<String, Any>) {
        context.dataStore.edit { p ->
            for ((name, value) in values) {
                if (name in keptOnRestore || name in deviceOnly) continue
                // A backup made before #287 has the old three-way choice: it comes back as its days.
                if (name == Keys.mileageCadence.name) {
                    (value as? String)?.let(MileageCheckIns::legacy)?.let { p[Keys.mileageEveryDays] = it }
                    continue
                }
                // Only as the type this version reads it as: one stored as another would throw on every read.
                restorable[name]?.invoke(p, value)
            }
        }
    }

    /** Every setting as stored, for the backup file. */
    suspend fun raw(): Map<String, Any> = context.dataStore.data.first().asMap().mapKeys { it.key.name }.filterKeys { it !in deviceOnly }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            theme = enumOrNull<ThemeMode>(p[Keys.theme]) ?: d.theme,
            biometricLock = p[Keys.biometric] ?: d.biometricLock,
            lockAfterSeconds = p[Keys.lockAfter] ?: d.lockAfterSeconds,
            hideInRecents = p[Keys.hideRecents] ?: d.hideInRecents,
            defaultRule = enumOrNull<DueRule>(p[Keys.rule]) ?: d.defaultRule,
            lead = LeadTime(p[Keys.leadMiles] ?: d.lead.miles, p[Keys.leadDays] ?: d.lead.days),
            torqueUnit = enumOrNull<TorqueUnit>(p[Keys.torque]) ?: d.torqueUnit,
            distanceUnit = p[Keys.distance]?.let(DistanceUnit::of) ?: d.distanceUnit,
            notices = NoticePrefs(
                serviceDue = p[Keys.notifyDue] ?: d.notices.serviceDue,
                overdue = p[Keys.notifyOverdue] ?: d.notices.overdue,
                recalls = p[Keys.notifyRecalls] ?: d.notices.recalls,
                mileage = p[Keys.notifyMileage] ?: d.notices.mileage,
                documents = p[Keys.notifyDocuments] ?: d.notices.documents,
                hour = (p[Keys.notifyHour] ?: d.notices.hour).coerceIn(0, 23),
                minute = (p[Keys.notifyMinute] ?: d.notices.minute).coerceIn(0, 59),
                mileageEveryDays = (p[Keys.mileageEveryDays] ?: p[Keys.mileageCadence]?.let(MileageCheckIns::legacy))
                    ?.takeIf(MileageCheckIns::valid) ?: d.notices.mileageEveryDays,
                mileageHour = p[Keys.mileageHour]?.coerceIn(0, 23),
                mileageMinute = (p[Keys.mileageMinute] ?: 0).coerceIn(0, 59),
            ),
            recallCadence = enumOrNull<RecallCadence>(p[Keys.recallCadence]) ?: d.recallCadence,
            welcomeSeen = p[Keys.welcomeSeen] ?: d.welcomeSeen,
            lastBackupOn = p[Keys.lastBackup]?.let(LocalDate::ofEpochDay),
            backupReminder = p[Keys.backupReminder] ?: d.backupReminder,
            scannerDetails = p[Keys.scannerDetails] ?: d.scannerDetails,
            homeWidget = p[Keys.homeWidget] ?: d.homeWidget,
            backupRemindedOn = p[Keys.backupReminded]?.let(LocalDate::ofEpochDay),
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setBiometricLock(on: Boolean) = context.dataStore.edit { it[Keys.biometric] = on }
    suspend fun setLockAfterSeconds(s: Int) = context.dataStore.edit { it[Keys.lockAfter] = s }
    suspend fun setHideInRecents(on: Boolean) = context.dataStore.edit { it[Keys.hideRecents] = on }
    suspend fun setDefaultRule(rule: DueRule) = context.dataStore.edit { it[Keys.rule] = rule.name }
    /**
     * The unit new vehicles get. The lead distance is typed in this unit, so it is converted with it
     * (500 mi becomes 800 km) and means the same as before. No vehicle is touched.
     */
    suspend fun setDistanceUnit(unit: DistanceUnit) = context.dataStore.edit { p ->
        val from = p[Keys.distance]?.let(DistanceUnit::of) ?: Settings().distanceUnit
        if (from != unit) {
            p[Keys.leadMiles] = DistanceConversion.convert(p[Keys.leadMiles] ?: Settings().lead.miles, from, unit, DistanceKind.SCHEDULED)
            p[Keys.distance] = unit.name
        }
    }
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
        it[Keys.notifyDocuments] = prefs.documents
        it[Keys.notifyHour] = prefs.hour
        it[Keys.notifyMinute] = prefs.minute
        it[Keys.mileageEveryDays] = prefs.mileageEveryDays
        val ownHour = prefs.mileageHour
        if (ownHour != null) {
            it[Keys.mileageHour] = ownHour
            it[Keys.mileageMinute] = prefs.mileageMinute
        } else {
            it.remove(Keys.mileageHour)
            it.remove(Keys.mileageMinute)
        }
        it.remove(Keys.mileageCadence)
    }
    suspend fun setRecallCadence(cadence: RecallCadence) = context.dataStore.edit { it[Keys.recallCadence] = cadence.name }
    suspend fun setWelcomeSeen() = context.dataStore.edit { it[Keys.welcomeSeen] = true }
    suspend fun setLastBackup(date: LocalDate) = context.dataStore.edit { it[Keys.lastBackup] = date.toEpochDay() }
    suspend fun setBackupReminder(on: Boolean) = context.dataStore.edit { it[Keys.backupReminder] = on }
    suspend fun setBackupReminded(date: LocalDate) = context.dataStore.edit { it[Keys.backupReminded] = date.toEpochDay() }
    suspend fun setScannerDetails(on: Boolean) = context.dataStore.edit { it[Keys.scannerDetails] = on }
    suspend fun setHomeWidget(on: Boolean) = context.dataStore.edit { it[Keys.homeWidget] = on }
}
