package io.cosgriff.glovebox.ui

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import io.cosgriff.glovebox.ShellTestApp
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = ShellTestApp::class, sdk = [35])
class FilesTest {
    // An activity, as the screens that hand files out are.
    private val app by lazy { Robolectric.buildActivity(Activity::class.java).setup().get() }
    private val shared get() = File(app.cacheDir, "shared").apply { mkdirs() }

    // FileProvider keeps the folders it has read, for the life of the JVM; each test has a new cache folder.
    @Before fun forgetFolders() = (FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.get(null) as MutableMap<*, *>).clear()

    @After fun close() = ShellTestApp.graph.close()

    @Test
    fun `a file in a listed cache folder is handed out through the app's own provider`() {
        val uri = app.contentUri(File(shared, "receipt.pdf"))
        assertEquals("content", uri.scheme)
        assertEquals("${app.packageName}.files", uri.authority)
        assertTrue(uri.toString(), uri.path!!.endsWith("/receipt.pdf"))
    }

    @Test
    fun `a capture is a new photo file in the folder, with the address the camera app writes to`() {
        val dir = File(app.cacheDir, "capture").apply { mkdirs() }
        val (file, uri) = app.newCapture(dir)
        val (other, _) = app.newCapture(dir)
        assertEquals(dir, file.parentFile)
        assertTrue(file.name.endsWith(".jpg"))
        assertTrue(file.name != other.name)
        assertEquals(app.contentUri(file), uri)
    }

    @Test
    fun `viewing hands the file to another app with permission to read it`() {
        val file = File(shared, "title.pdf")
        assertTrue(app.handOut(file, "application/pdf", share = false))
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(app.contentUri(file), started.data)
        assertEquals("application/pdf", started.type)
        assertTrue(started.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun `sharing opens the share sheet with the file attached`() {
        val file = File(shared, "photo.jpg")
        assertTrue(app.handOut(file, "image/jpeg", share = true))
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/jpeg", send.type)
        @Suppress("DEPRECATION")
        assertEquals(app.contentUri(file), send.getParcelableExtra(Intent.EXTRA_STREAM))
    }

    @Test
    fun `nothing on the phone to open it with is a false, not a crash`() {
        shadowOf(app.application).checkActivities(true)
        assertFalse(app.handOut(File(shared, "title.pdf"), "application/pdf", share = false))
    }
}
