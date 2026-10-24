package io.cosgriff.glovebox.notify

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ShellTestApp
import io.cosgriff.glovebox.domain.Notice
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class NotifierTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val system = app.getSystemService(NotificationManager::class.java)
    private val notifier = Notifier(app)

    @Before fun allow() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier.createChannels()
    }

    @After fun close() = ShellTestApp.graph.close()

    private val posted: List<Notification> get() = shadowOf(system).allNotifications
    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
    private fun Notification.destination() = Destination.from(shadowOf(contentIntent).savedIntent)

    @Test
    fun `there is a channel for each kind, named and described, so each can be silenced in Android's settings`() {
        val channels = Notifier.Channel.entries.map { system.getNotificationChannel(it.id) }
        assertEquals(
            listOf(R.string.channel_due, R.string.channel_overdue, R.string.channel_recalls, R.string.channel_mileage, R.string.channel_backup).map(app::getString),
            channels.map { it.name.toString() },
        )
        assertEquals(app.getString(R.string.channel_recalls_about), channels[2].description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channels[2].importance)
        // Making them again on the next launch changes nothing.
        notifier.createChannels()
        assertEquals(5, system.notificationChannels.size)
    }

    @Test
    fun `nothing is posted while Android won't show it`() {
        shadowOf(system).setNotificationsEnabled(false)
        assertFalse(notifier.allowed)
        notifier.post(Notice.MileageCheckIn(1, "Tacoma"))
        notifier.postBackupReminder(never = true)
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `nothing is posted when the permission was refused`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(notifier.allowed)
        notifier.post(Notice.Overdue(1, "Tacoma", listOf("Oil change")))
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `a service coming due names the vehicle and the services, and opens Reminders`() {
        notifier.post(Notice.ServiceDue(4, "Tacoma", listOf("Oil change", "Tire rotation")))
        val shown = posted.single()
        assertEquals(app.getString(R.string.notify_due_title, "Tacoma"), shown.title())
        assertTrue(shown.text(), shown.text().contains("Oil change") && shown.text().contains("Tire rotation"))
        assertEquals(Notifier.Channel.SERVICE_DUE.id, shown.channelId)
        assertEquals(Destination.Reminders, shown.destination())
    }

    @Test
    fun `an overdue nudge opens Reminders`() {
        notifier.post(Notice.Overdue(4, "Tacoma", listOf("Brake fluid")))
        val shown = posted.single()
        assertEquals(app.getString(R.string.notify_overdue_title, "Tacoma"), shown.title())
        assertEquals(Notifier.Channel.OVERDUE.id, shown.channelId)
        assertEquals(Destination.Reminders, shown.destination())
    }

    @Test
    fun `the mileage check-in opens that vehicle's Update mileage sheet`() {
        notifier.post(Notice.MileageCheckIn(9, "Civic"))
        val shown = posted.single()
        assertEquals(app.getString(R.string.notify_mileage_title, "Civic"), shown.title())
        assertEquals(app.getString(R.string.notify_mileage_body), shown.text())
        assertEquals(Destination.UpdateMileage(9), shown.destination())
    }

    @Test
    fun `new recalls list three components at most, each once, and open the vehicle's recalls`() {
        notifier.post(Notice.NewRecalls(5, "GTI", listOf("Airbags", "Brakes", "Airbags")))
        val few = posted.single()
        assertEquals("Airbags, Brakes", few.text())
        assertEquals(Destination.Recalls(5), few.destination())
        assertEquals(Notifier.Channel.RECALLS.id, few.channelId)

        notifier.post(Notice.NewRecalls(5, "GTI", listOf("Airbags", "Brakes", "Fuel system", "Steering")))
        // The same vehicle and kind: the newer one takes the older one's place.
        assertEquals(app.getString(R.string.notify_and_more, "Airbags, Brakes, Fuel system"), posted.single().text())
    }

    @Test
    fun `each vehicle has its own notification of each kind`() {
        notifier.post(Notice.MileageCheckIn(1, "Tacoma"))
        notifier.post(Notice.MileageCheckIn(2, "Civic"))
        notifier.post(Notice.MileageCheckIn(1, "Tacoma"))
        notifier.post(Notice.Overdue(1, "Tacoma", listOf("Oil change")))
        assertEquals(3, posted.size)
    }

    @Test
    fun `the lock screen version says only the kind`() {
        notifier.post(Notice.Overdue(1, "Tacoma", listOf("Oil change")))
        val shown = posted.single()
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, shown.visibility)
        val public = checkNotNull(shown.publicVersion)
        assertEquals(app.getString(R.string.app_name), public.title())
        assertEquals(app.getString(R.string.channel_overdue_hidden), public.text())
        assertFalse(public.text().contains("Tacoma"))
    }

    @Test
    fun `the backup reminder says whether a backup was ever made, and opens the backup screen`() {
        notifier.postBackupReminder(never = true)
        assertEquals(app.getString(R.string.notify_backup_never), posted.single().title())
        assertEquals(app.getString(R.string.notify_backup_body), posted.single().text())
        assertEquals(Destination.Backup, posted.single().destination())

        notifier.postBackupReminder(never = false)
        assertEquals(app.getString(R.string.notify_backup_fresh), posted.single().title())
    }
}
