package io.cosgriff.glovebox.ui.scan

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tesseract itself is native and is tested on a device (TextReaderTest in androidTest); here, what is around it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class TextReaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `lit digits on a dark display are turned dark on light for Tesseract`() {
        val dark = byteArrayOf(10, 20, 230.toByte(), 15)
        assertArrayEquals(byteArrayOf(245.toByte(), 235.toByte(), 25, 240.toByte()), TextReader.darkOnLight(dark))
    }

    @Test
    fun `a frame that is already light is left as it is`() {
        val light = byteArrayOf(200.toByte(), 180.toByte(), 30, 250.toByte())
        assertSame(light, TextReader.darkOnLight(light))
        val empty = ByteArray(0)
        assertSame(empty, TextReader.darkOnLight(empty))
    }

    @Test
    fun `where the engine can't start, nothing is read and nothing more is tried`() {
        // The JVM has no Tesseract library, as a phone without one for its processor wouldn't.
        val reader = TextReader.digits(context)
        assertFalse(reader.failed)
        assertNull(reader.read(ByteArray(16) { 0 }, 4, 4))
        assertTrue(reader.failed)
        assertEquals(emptyList<TextReader.Word>(), reader.words(ByteArray(16), 4, 4))
        reader.close()
    }

    @Test
    fun `every reader after the first also fails quietly when the engine can't start`() {
        // Once the library has failed to load, TessBaseAPI is left unusable for the life of the process and
        // a new one throws NoClassDefFoundError: reopening the dash reader, or the scanner's print reader
        // after it, must give up the same way instead of crashing the analyzer thread.
        repeat(3) {
            val reader = if (it % 2 == 0) TextReader.digits(context) else TextReader.vin(context)
            assertNull(reader.read(ByteArray(16) { 0 }, 4, 4))
            assertTrue(reader.failed)
            reader.close()
        }
    }
}
