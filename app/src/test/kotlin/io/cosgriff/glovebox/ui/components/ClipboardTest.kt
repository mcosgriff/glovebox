package io.cosgriff.glovebox.ui.components

import android.content.ClipboardManager
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** The screenshot fixtures' made-up VIN: it passes the check digit and belongs to no one. */
private const val VIN = "JTEBU5JR7K5900001"

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class)
class ClipboardTest {
    private val context = Components.context
    private val clipboard get() = context.getSystemService(ClipboardManager::class.java)

    @Test
    @Config(sdk = [35])
    fun `a VIN is copied marked sensitive, and Android says it was copied itself`() {
        copyText(context, "VIN", VIN, sensitive = true)
        val clip = clipboard.primaryClip!!
        assertEquals(VIN, clip.getItemAt(0).text.toString())
        assertTrue(clip.description.extras.getBoolean("android.content.extra.IS_SENSITIVE"))
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    @Config(sdk = [29])
    fun `on older Android a copy is said here, and plain text isn't marked`() {
        copyText(context, "Plate", "MADE UP", sensitive = false)
        val clip = clipboard.primaryClip!!
        assertEquals("MADE UP", clip.getItemAt(0).text.toString())
        assertFalse(clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") ?: false)
        assertEquals(Components.string(R.string.copied, "Plate"), ShadowToast.getTextOfLatestToast())
    }
}
