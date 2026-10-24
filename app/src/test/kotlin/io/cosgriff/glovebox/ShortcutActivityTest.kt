package io.cosgriff.glovebox

import android.content.Intent
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.notify.ShortcutTask
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class ShortcutActivityTest {
    @After fun close() = ShellTestApp.graph.close()

    private fun open(intent: Intent): Pair<ShortcutActivity, Intent> {
        val activity = Robolectric.buildActivity(ShortcutActivity::class.java, intent).create().get()
        return activity to shadowOf(activity).nextStartedActivity
    }

    @Test
    fun `a shortcut hands its task to the app and closes, drawing nothing`() {
        val (activity, started) = open(Intent().also(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE)::putInto))
        assertEquals(MainActivity::class.java.name, started.component?.className)
        assertEquals(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE), Destination.from(started))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `anything but a shortcut's own task opens the app as an ordinary launch`() {
        val (activity, started) = open(Intent().also(Destination.Recalls(3)::putInto))
        assertEquals(MainActivity::class.java.name, started.component?.className)
        assertNull(Destination.from(started))
        assertTrue(activity.isFinishing)
    }
}
