package io.cosgriff.glovebox.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.LeadTime
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.TorqueUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)
    private lateinit var repo: SettingsRepository

    @Before fun setUp() {
        resetDataStores()
        repo = SettingsRepository(context)
    }

    private suspend fun now() = repo.settings.first()

    @Test
    fun `a new install reads every default`() = runBlocking {
        assertEquals(Settings(), now())
        assertEquals(emptyMap<String, Any>(), repo.raw())
    }

    @Test
    fun `every choice is kept`() = runBlocking {
        repo.setTheme(ThemeMode.DARK)
        repo.setBiometricLock(false)
        repo.setLockAfterSeconds(-1)
        repo.setHideInRecents(false)
        repo.setDefaultRule(DueRule.MILEAGE_ONLY)
        repo.setLeadTime(LeadTime(miles = 750, days = 21))
        repo.setTorqueUnit(TorqueUnit.NEWTON_METER)
        repo.setRecallCadence(RecallCadence.MONTHLY)
        repo.setWelcomeSeen()
        repo.setLastBackup(day)
        repo.setBackupReminder(false)
        repo.setBackupReminded(day.plusDays(1))
        repo.setScannerDetails(true)
        repo.setHomeWidget(true)

        assertEquals(
            Settings(
                theme = ThemeMode.DARK, biometricLock = false, lockAfterSeconds = -1, hideInRecents = false, defaultRule = DueRule.MILEAGE_ONLY,
                lead = LeadTime(750, 21), torqueUnit = TorqueUnit.NEWTON_METER, recallCadence = RecallCadence.MONTHLY, welcomeSeen = true,
                lastBackupOn = day, backupReminder = false, backupRemindedOn = day.plusDays(1), scannerDetails = true, homeWidget = true,
            ),
            now(),
        )
    }

    @Test
    fun `notifications keep their own mileage time, or ask with everything else`() = runBlocking {
        val own = NoticePrefs(serviceDue = false, overdue = true, recalls = false, mileage = true, hour = 19, minute = 30, mileageEveryDays = 30, mileageHour = 7, mileageMinute = 45)
        repo.setNotices(own)
        assertEquals(own, now().notices)

        repo.setNotices(own.copy(mileageHour = null, mileageMinute = 45))
        assertNull(now().notices.mileageHour)
        assertEquals(0, now().notices.mileageMinute)
    }

    @Test
    fun `changing the unit for new vehicles converts the lead distance with it, once`() = runBlocking {
        repo.setDistanceUnit(DistanceUnit.KILOMETERS)
        assertEquals(DistanceUnit.KILOMETERS, now().distanceUnit)
        assertEquals(800, now().lead.miles)

        repo.setDistanceUnit(DistanceUnit.KILOMETERS)
        assertEquals(800, now().lead.miles)

        // Each vehicle takes the lead in its own unit.
        assertEquals(500, now().leadFor(DistanceUnit.MILES).miles)
        assertEquals(800, now().leadFor(DistanceUnit.KILOMETERS).miles)

        repo.setDistanceUnit(DistanceUnit.MILES)
        assertEquals(500, now().lead.miles)
    }

    @Test
    fun `a backup carries choices but not facts about this phone`() = runBlocking {
        repo.setTheme(ThemeMode.LIGHT)
        repo.setWelcomeSeen()
        repo.setLastBackup(day)
        repo.setBackupReminded(day)

        assertEquals(mapOf<String, Any>("theme" to "LIGHT"), repo.raw())
    }

    @Test
    fun `a restore puts back choices but never this phone's lock, the widget or its own facts`() = runBlocking {
        repo.setBiometricLock(true)
        repo.restore(
            mapOf(
                "theme" to "DARK", "torque_unit" to "INCH_POUND", "lead_miles" to 1000, "notify_recalls" to false, "last_backup_on" to 1L,
                "biometric_lock" to false, "lock_after_seconds" to 300, "hide_in_recents" to false, "home_widget" to true,
                "welcome_seen" to true, "backup_reminded_on" to 2L, "some_long" to 5L,
            ),
        )

        val s = now()
        assertEquals(ThemeMode.DARK, s.theme)
        assertEquals(TorqueUnit.INCH_POUND, s.torqueUnit)
        assertEquals(1000, s.lead.miles)
        assertFalse(s.notices.recalls)
        assertEquals(true, s.biometricLock)
        assertEquals(0, s.lockAfterSeconds)
        assertEquals(true, s.hideInRecents)
        assertFalse(s.homeWidget)
        assertFalse(s.welcomeSeen)
        assertNull(s.lastBackupOn)
        assertNull(s.backupRemindedOn)
        // A setting this version doesn't know is left out.
        assertNull(repo.raw()["some_long"])
    }

    @Test
    fun `a setting restored with the wrong type is left out instead of breaking every launch`() = runBlocking {
        repo.restore(mapOf("lead_miles" to "x", "theme" to 3, "notify_recalls" to 1, "last_backup_on" to "y", "lead_days" to 10))

        val s = now()
        assertEquals(Settings().lead.miles, s.lead.miles)
        assertEquals(10, s.lead.days)
        assertEquals(ThemeMode.SYSTEM, s.theme)
        assertEquals(Settings().notices.recalls, s.notices.recalls)
    }

    @Test
    fun `a backup from before the mileage check-in took days comes back as its days`() = runBlocking {
        repo.restore(mapOf("mileage_cadence" to "MONTHLY"))
        assertEquals(30, now().notices.mileageEveryDays)

        repo.restore(mapOf("mileage_cadence" to "NOT_A_CADENCE"))
        assertEquals(30, now().notices.mileageEveryDays)
    }

    @Test
    fun `values this version can't use read as their defaults`() = runBlocking {
        repo.restore(
            mapOf(
                "theme" to "PURPLE", "default_rule" to "NOT_A_RULE", "torque_unit" to "STONE_FOOT", "recall_cadence" to "HOURLY",
                "notify_hour" to 40, "notify_minute" to -5, "notify_mileage_hour" to 25, "notify_mileage_minute" to 99, "mileage_every_days" to 0,
            ),
        )

        val s = now()
        assertEquals(ThemeMode.SYSTEM, s.theme)
        assertEquals(DueRule.WHICHEVER_FIRST, s.defaultRule)
        assertEquals(TorqueUnit.FOOT_POUND, s.torqueUnit)
        assertEquals(RecallCadence.WEEKLY, s.recallCadence)
        assertEquals(23, s.notices.hour)
        assertEquals(0, s.notices.minute)
        assertEquals(23, s.notices.mileageHour)
        assertEquals(59, s.notices.mileageMinute)
        assertEquals(NoticePrefs().mileageEveryDays, s.notices.mileageEveryDays)
    }
}
