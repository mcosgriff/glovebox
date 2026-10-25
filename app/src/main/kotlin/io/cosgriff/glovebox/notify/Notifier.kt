package io.cosgriff.glovebox.notify

import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.settings.servicesText
import io.cosgriff.glovebox.ui.AppText
import androidx.annotation.StringRes
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cosgriff.glovebox.MainActivity
import io.cosgriff.glovebox.ShortcutActivity
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.Notice
import javax.inject.Inject
import javax.inject.Singleton

/** Where tapping a notification leads. Carried on the launch intent, then held until the app is unlocked. */
sealed interface Destination {
    /** The Reminders tab, where the due and overdue services are. */
    data object Reminders : Destination
    data class Recalls(val vehicleId: Long) : Destination

    /** The vehicle's home with the Update mileage sheet open. */
    data class UpdateMileage(val vehicleId: Long) : Destination

    /** The vehicle's home. Not from a notification: where a row of the home-screen widget leads (#238). */
    data class VehicleHome(val vehicleId: Long) : Destination

    /** The Encrypted backup screen. */
    data object Backup : Destination

    /** Add a vehicle. Not from a notification: where the welcome's first button leads. */
    data object AddVehicle : Destination

    /**
     * A launcher shortcut (#235, `res/xml/shortcuts.xml`): [task] for a vehicle the shortcut can't name,
     * so the nav host picks the only one or asks which.
     */
    data class Shortcut(val task: ShortcutTask) : Destination

    fun putInto(intent: Intent) {
        intent.putExtra(KIND, when (this) {
            Reminders -> "reminders"
            is Recalls -> "recalls"
            is UpdateMileage -> "mileage"
            is VehicleHome -> "vehicle"
            Backup -> "backup"
            AddVehicle -> "add"
            is Shortcut -> task.kind
        })
        intent.putExtra(VEHICLE, when (this) {
            Reminders, Backup, AddVehicle, is Shortcut -> 0L
            is Recalls -> vehicleId
            is UpdateMileage -> vehicleId
            is VehicleHome -> vehicleId
        })
    }

    companion object {
        private const val KIND = "glovebox.destination"
        private const val VEHICLE = "glovebox.vehicle"

        /** Null for an ordinary launch. */
        fun from(intent: Intent?): Destination? {
            val vehicle = intent?.getLongExtra(VEHICLE, 0L) ?: return null
            return when (intent.getStringExtra(KIND)) {
                "reminders" -> Reminders
                "recalls" -> Recalls(vehicle)
                "mileage" -> UpdateMileage(vehicle)
                "vehicle" -> VehicleHome(vehicle)
                "backup" -> Backup
                "add" -> AddVehicle
                else -> ShortcutTask.entries.firstOrNull { it.kind == intent.getStringExtra(KIND) }?.let(::Shortcut)
            }
        }
    }
}

/**
 * Opens Glovebox at this destination, or as it was left when there is none; the lock comes first either way.
 * It goes through [ShortcutActivity], which isn't exported, so only this app's own notifications, widget and
 * shortcuts can name a destination. `MainActivity` is exported, as the launcher's, and reads none from its intent.
 */
fun Destination?.launchIntent(context: Context): Intent = Intent(context, ShortcutActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
    this@launchIntent?.putInto(this)
}

/**
 * The running app brought forward rather than a second `MainActivity` started, which `MainActivity`'s
 * "a launcher intent that isn't the task's root finishes at once" check relies on.
 */
fun Context.appIntent(): Intent = Intent(this, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}

/** What a launcher shortcut does. [kind] is the extra `res/xml/shortcuts.xml` sets, so it never changes. */
enum class ShortcutTask(val kind: String) {
    LOG_SERVICE("log-service"),
    UPDATE_MILEAGE("update-mileage"),
}

/**
 * Posts the app's notifications. One channel per kind, so each can be silenced in Android's own
 * settings as well as in the app.
 *
 * Wording is kept to the vehicle's name and the service: notifications can show on the lock screen.
 * On a phone set to hide sensitive content there, only a line saying which kind it is appears.
 */
@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** False when Android won't show them: the permission was refused, or notifications are off for the app. */
    val allowed: Boolean
        get() = manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Safe to call on every launch: creating a channel that exists changes nothing the user set. */
    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        val res = AppText.res
        Channel.entries.forEach { c ->
            system.createNotificationChannel(
                NotificationChannel(c.id, res.getString(c.label), c.importance).apply { description = res.getString(c.about) },
            )
        }
    }

    /** [never] when no backup has ever been made from this phone. */
    fun postBackupReminder(never: Boolean) {
        if (!allowed) return
        show(
            Shown(
                Channel.BACKUP,
                AppText.res.getString(if (never) R.string.notify_backup_never else R.string.notify_backup_fresh),
                AppText.res.getString(R.string.notify_backup_body),
                Destination.Backup,
            ),
            id = Channel.BACKUP.ordinal * 1_000_000,
        )
    }

    fun post(notice: Notice) {
        if (!allowed) return
        val res = AppText.res
        val shown = when (notice) {
            is Notice.ServiceDue -> Shown(
                Channel.SERVICE_DUE,
                res.getString(R.string.notify_due_title, notice.vehicleName),
                res.getQuantityString(R.plurals.notify_due_body, notice.services.size, servicesText(notice.services)),
                Destination.Reminders,
            )
            is Notice.Overdue -> Shown(
                Channel.OVERDUE,
                res.getString(R.string.notify_overdue_title, notice.vehicleName),
                res.getQuantityString(R.plurals.notify_overdue_body, notice.services.size, servicesText(notice.services)),
                Destination.Reminders,
            )
            is Notice.MileageCheckIn -> Shown(
                Channel.MILEAGE,
                res.getString(R.string.notify_mileage_title, notice.vehicleName),
                res.getString(R.string.notify_mileage_body),
                Destination.UpdateMileage(notice.vehicleId),
            )
            is Notice.NewRecalls -> Shown(
                Channel.RECALLS,
                res.getQuantityString(R.plurals.notify_recalls_title, notice.components.size, notice.vehicleName, notice.components.size.asNumber()),
                notice.components.distinct().let { all ->
                    all.take(3).joinToString(", ").let { if (all.size > 3) res.getString(R.string.notify_and_more, it) else it }
                },
                Destination.Recalls(notice.vehicleId),
            )
        }
        // One per vehicle and kind: a newer one replaces the older instead of piling up.
        show(shown, id = shown.channel.ordinal * 1_000_000 + (notice.vehicleId % 1_000_000).toInt())
    }

    private fun show(what: Shown, id: Int) {
        val (channel, title, text, destination) = what
        val open = PendingIntent.getActivity(
            context,
            id,
            destination.launchIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun builder(title: String, text: String?) = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
        val shown = builder(title, text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(builder(AppText.res.getString(R.string.app_name), AppText.res.getString(channel.hidden)).build())
            .build()
        try {
            manager.notify(id, shown)
        } catch (_: SecurityException) {
            // The permission was withdrawn between the check and the post.
        }
    }

    private data class Shown(val channel: Channel, val title: String, val text: String, val destination: Destination)

    /** [hidden] is all a locked phone shows when it is set to hide sensitive notification content. */
    enum class Channel(val id: String, @StringRes val label: Int, @StringRes val about: Int, @StringRes val hidden: Int, val importance: Int) {
        SERVICE_DUE("service_due", R.string.channel_due, R.string.channel_due_about, R.string.channel_due_hidden, NotificationManager.IMPORTANCE_DEFAULT),
        OVERDUE("overdue", R.string.channel_overdue, R.string.channel_overdue_about, R.string.channel_overdue_hidden, NotificationManager.IMPORTANCE_DEFAULT),
        RECALLS("recalls", R.string.channel_recalls, R.string.channel_recalls_about, R.string.channel_recalls_hidden, NotificationManager.IMPORTANCE_HIGH),
        MILEAGE("mileage", R.string.channel_mileage, R.string.channel_mileage_about, R.string.channel_mileage_hidden, NotificationManager.IMPORTANCE_LOW),
        BACKUP("backup", R.string.channel_backup, R.string.channel_backup_about, R.string.channel_backup_hidden, NotificationManager.IMPORTANCE_LOW),
    }
}
