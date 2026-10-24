package io.cosgriff.glovebox

import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.ui.AppText
import io.cosgriff.glovebox.widget.GarageWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The real application, with its real Hilt graph: what it sets up on launch. Nothing here opens the database
 * (the widget, which would, is off), so the graph's SQLCipher and Keystore are never reached. WorkManager isn't
 * set up under Robolectric before the application starts, so the job scheduling is `NoticeSchedulerTest`'s.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GloveboxAppTest {
    private val app = ApplicationProvider.getApplicationContext<GloveboxApp>()

    @Test
    fun `launch puts the app's language in place, makes the notification channels and follows the widget switch`() {
        assertEquals(app.getString(R.string.format_locale), AppText.locale.toLanguageTag())
        assertEquals(5, app.getSystemService(NotificationManager::class.java).notificationChannels.size)

        // Off unless the user turns it on: the widget shows the garage outside the lock.
        val receiver = ComponentName(app, GarageWidgetReceiver::class.java)
        idleMainUntil { app.packageManager.getComponentEnabledSetting(receiver) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED }
    }
}
