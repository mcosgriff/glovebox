package io.cosgriff.glovebox

import android.content.Intent
import io.cosgriff.glovebox.notify.Destination
import io.cosgriff.glovebox.notify.PendingDestination
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

    private val pending get() = ShellTestApp.graph.get<PendingDestination>()

    @Test
    fun `a shortcut hands its task to the app and closes, drawing nothing`() {
        val (activity, started) = open(Intent().also(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE)::putInto))
        assertEquals(Destination.Shortcut(ShortcutTask.UPDATE_MILEAGE), pending.destination.value)
        assertEquals(MainActivity::class.java.name, started.component?.className)
        // MainActivity takes it from PendingDestination, never from an intent another app could send too.
        assertNull(Destination.from(started))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `a notification's or the widget's destination is handed over the same way`() {
        val (activity, started) = open(Intent().also(Destination.Recalls(3)::putInto))
        assertEquals(Destination.Recalls(3), pending.destination.value)
        assertEquals(MainActivity::class.java.name, started.component?.className)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `an intent without one opens the app as it was left`() {
        val (_, started) = open(Intent())
        assertNull(pending.destination.value)
        assertEquals(MainActivity::class.java.name, started.component?.className)
    }
}
