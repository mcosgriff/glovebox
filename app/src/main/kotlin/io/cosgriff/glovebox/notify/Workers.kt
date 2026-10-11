package io.cosgriff.glovebox.notify

import io.cosgriff.glovebox.domain.BackupRules
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.cosgriff.glovebox.data.db.OdometerDao
import io.cosgriff.glovebox.data.db.ScheduleDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.nhtsa.RecallRefresh
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.data.settings.NoticeLogStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.Notice
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.NoticeRules
import io.cosgriff.glovebox.domain.NoticeVehicle
import io.cosgriff.glovebox.domain.OdometerReading
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.VehicleReminders
import io.cosgriff.glovebox.ui.service.toEntry
import io.cosgriff.glovebox.ui.service.toTracked
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What the workers need. They are created by WorkManager, not Hilt, so they reach in through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotifyEntryPoint {
    fun vehicles(): VehicleDao
    fun schedule(): ScheduleDao
    fun service(): ServiceDao
    fun odometer(): OdometerDao
    fun settings(): SettingsRepository
    fun noticeLog(): NoticeLogStore
    fun recalls(): RecallRepository
    fun notifier(): Notifier
}

private fun Context.notify(): NotifyEntryPoint = EntryPointAccessors.fromApplication(applicationContext, NotifyEntryPoint::class.java)

/**
 * The daily check: services coming due, overdue nudges and the mileage check-in. It opens the
 * encrypted database on its own, which works with the phone locked because the database key isn't
 * tied to the user unlocking (see CLAUDE.md). Nothing here uses the network.
 */
class DailyCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = applicationContext.notify()
        val settings = deps.settings().settings.first()
        val today = LocalDate.now()
        // Sold vehicles are in the archive and get no reminders of any kind.
        val vehicles = deps.vehicles().active().map { v ->
            val records = deps.service().recordsFor(v.id)
            val entries = records.map { it.toEntry() }
            val updates = deps.odometer().readingsFor(v.id).map { OdometerReading(it.date, it.odometer) }
            val tracked = deps.schedule().allFor(v.id).filter { it.tracked }.mapNotNull { it.toTracked(settings.defaultRule) }
            NoticeVehicle(
                id = v.id,
                name = "${v.year} ${v.nickname ?: v.model}",
                statuses = VehicleReminders.statuses(tracked, entries, updates, today, settings.lead),
                lastReadingOn = ServiceLog.readings(entries, updates).maxOfOrNull { it.date },
            )
        }
        val decision = NoticeRules.daily(vehicles, deps.noticeLog().read(), settings.notices, today)
        // The log is kept even if Android won't show anything, so allowing notifications later doesn't release a backlog.
        deps.noticeLog().write(decision.log)
        decision.notices.forEach(deps.notifier()::post)

        // Sold vehicles count here: their history is still worth keeping.
        val hasData = deps.vehicles().all().isNotEmpty()
        if (settings.backupReminder && BackupRules.shouldRemind(hasData, settings.lastBackupOn, settings.backupRemindedOn, today)) {
            deps.settings().setBackupReminded(today)
            deps.notifier().postBackupReminder(never = settings.lastBackupOn == null)
        }
        return Result.success()
    }
}

/**
 * Asks NHTSA about each garage vehicle whose recall list is older than the "Check for new recalls"
 * setting, and announces campaigns that weren't on file. A failure is never announced: the job
 * asks WorkManager to try again later.
 */
class RecallCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = applicationContext.notify()
        val settings = deps.settings().settings.first()
        var failed = false
        for (v in deps.vehicles().active()) {
            // Never checked before: everything listed would be "new". The app shows those; only later additions are news.
            val firstCheck = v.recallsCheckedOn == null
            when (val result = deps.recalls().refreshIfStale(v.id)) {
                is RecallRefresh.Checked ->
                    if (!firstCheck && settings.notices.recalls && result.added.isNotEmpty()) {
                        deps.notifier().post(
                            Notice.NewRecalls(v.id, "${v.year} ${v.nickname ?: v.model}", result.added.map { Recalls.componentLabel(it.component) }),
                        )
                    }
                RecallRefresh.Failed -> failed = true
                RecallRefresh.CannotCheck, null -> Unit
            }
        }
        return if (failed) Result.retry() else Result.success()
    }
}

/** Keeps the two background jobs in step with the settings. */
@Singleton
class NoticeScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    /** Call on launch and whenever a notification setting changes. */
    fun sync(prefs: NoticePrefs, recallCadence: RecallCadence) {
        val work = WorkManager.getInstance(context)
        if (prefs.wantsDailyCheck) {
            val now = LocalTime.now()
            val delay = NoticeRules.minutesUntil(prefs.hour, prefs.minute, now.hour * 60 + now.minute)
            work.enqueueUniquePeriodicWork(
                DAILY,
                // UPDATE keeps the job's place in its cycle unless the time of day moved.
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay.toLong(), TimeUnit.MINUTES)
                    .build(),
            )
        } else {
            work.cancelUniqueWork(DAILY)
        }

        if (recallCadence.days != null) {
            work.enqueueUniquePeriodicWork(
                RECALLS,
                ExistingPeriodicWorkPolicy.KEEP,
                // Daily at most; each vehicle is only asked about once its list is as old as the setting says.
                PeriodicWorkRequestBuilder<RecallCheckWorker>(1, TimeUnit.DAYS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build(),
            )
        } else {
            work.cancelUniqueWork(RECALLS)
        }
    }

    private companion object {
        const val DAILY = "daily-check"
        const val RECALLS = "recall-check"
    }
}

/** Holds where a tapped notification leads until the app is unlocked and its screens are up. */
@Singleton
class PendingDestination @Inject constructor() {
    private val _destination = kotlinx.coroutines.flow.MutableStateFlow<Destination?>(null)
    val destination: kotlinx.coroutines.flow.StateFlow<Destination?> = _destination

    fun set(destination: Destination?) {
        if (destination != null) _destination.value = destination
    }

    /** Taken exactly once, by the nav host. */
    fun take(): Destination? = _destination.value.also { _destination.value = null }
}

/** Hands the nav host a tapped notification's destination, dropping it if its vehicle is gone or sold. */
@dagger.hilt.android.lifecycle.HiltViewModel
class DestinationViewModel @Inject constructor(private val holder: PendingDestination, private val vehicles: VehicleDao) : androidx.lifecycle.ViewModel() {
    val pending = holder.destination

    suspend fun take(): Destination? {
        val to = holder.take() ?: return null
        val vehicleId = when (to) {
            Destination.Reminders, Destination.Backup -> return to
            is Destination.Recalls -> to.vehicleId
            is Destination.UpdateMileage -> to.vehicleId
        }
        return to.takeIf { vehicles.byId(vehicleId)?.archivedOn == null && vehicles.byId(vehicleId) != null }
    }
}
