package io.cosgriff.glovebox.notify

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = io.cosgriff.glovebox.ShellTestApp::class, sdk = [35])
class DestinationTest {
    private val everyKind = listOf(
        Destination.Reminders,
        Destination.Recalls(7),
        Destination.UpdateMileage(8),
        Destination.VehicleHome(9),
        Destination.Backup,
        Destination.AddVehicle,
        Destination.Shortcut(ShortcutTask.LOG_SERVICE),
        Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE),
    )

    @Test
    fun `every destination comes back off the intent it was put on`() {
        for (destination in everyKind) {
            val intent = Intent().also(destination::putInto)
            assertEquals(destination, Destination.from(intent))
        }
    }

    @Test
    fun `an ordinary launch, or one with a kind this version doesn't know, has no destination`() {
        assertNull(Destination.from(null))
        assertNull(Destination.from(Intent(Intent.ACTION_MAIN)))
        assertNull(Destination.from(Intent().putExtra("glovebox.destination", "somewhere-new")))
    }

    @Test
    fun `a shortcut's kind is the one res-xml-shortcuts sets, so it never changes`() {
        assertEquals(listOf("log-service", "update-mileage"), ShortcutTask.entries.map { it.kind })
        val intent = Intent().putExtra("glovebox.destination", "update-mileage")
        assertEquals(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE), Destination.from(intent))
    }

    @Test
    fun `the launch intent brings the running app forward with the destination on it`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Destination.Recalls(3).launchIntent(context)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        val wanted = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(wanted, intent.flags and wanted)
        assertEquals(Destination.Recalls(3), Destination.from(intent))

        // None opens the app as it was left.
        assertNull(Destination.from((null as Destination?).launchIntent(context)))
    }
}
