package io.cosgriff.glovebox.ui.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.cosgriff.glovebox.domain.DashReading
import io.cosgriff.glovebox.domain.DistanceUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tesseract is native code, so it can only be tried on a phone or an emulator:
 * `./gradlew :app:connectedDebugAndroidTest`. These draw a reading the way a cluster shows one and
 * check that the engine starts, finds its language file and reads it. They say nothing about how
 * well a real dash reads through a real camera; only a real dash can.
 */
@RunWith(AndroidJUnit4::class)
class DashReaderTest {
    private val reader = DashReader(InstrumentationRegistry.getInstrumentation().targetContext)

    // The size of the band the camera screen reads from a 1080p frame.
    private val width = 864
    private val height = 576

    @After
    fun close() = reader.close()

    /** A picture as the camera's greyscale plane would hold it. */
    private fun picture(lit: Boolean, draw: (Canvas, Paint) -> Unit): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (lit) Color.rgb(14, 14, 16) else Color.rgb(232, 230, 224))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (lit) Color.rgb(238, 238, 232) else Color.rgb(24, 24, 24)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        draw(canvas, paint)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return ByteArray(pixels.size) { ((Color.red(pixels[it]) * 299 + Color.green(pixels[it]) * 587 + Color.blue(pixels[it]) * 114) / 1000).toByte() }
    }

    private fun text(lit: Boolean, vararg lines: Triple<String, Float, Float>) = picture(lit) { canvas, paint ->
        for ((line, size, y) in lines) {
            paint.textSize = size
            canvas.drawText(line, (width - paint.measureText(line)) / 2, y, paint)
        }
    }

    private fun read(picture: ByteArray): String? {
        Log.i("DashReaderTest", reader.words(picture, width, height).joinToString { "${it.text}@${it.confidence.toInt()}" })
        return reader.read(picture, width, height)
    }

    @Test
    fun readsLitDigitsOnADarkDisplay() {
        val read = read(text(lit = true, Triple("93412", 150f, 330f)))
        assertFalse(reader.failed)
        assertEquals(93_412, DashReading.pick(read.orEmpty(), last = 93_000, unit = DistanceUnit.MILES))
    }

    @Test
    fun readsDarkDigitsOnALightDisplay() {
        assertEquals(128_540, DashReading.pick(read(text(lit = false, Triple("128540", 130f, 330f))).orEmpty(), last = null, unit = DistanceUnit.MILES))
    }

    @Test
    fun readsAReadingWithAComma() {
        assertEquals(93_412, DashReading.pick(read(text(lit = true, Triple("93,412", 140f, 330f))).orEmpty(), last = 93_000, unit = DistanceUnit.MILES))
    }

    @Test
    fun tellsTheOdometerFromATripMeterAboveIt() {
        val read = read(text(lit = true, Triple("345.6", 90f, 200f), Triple("93412", 130f, 400f)))
        assertEquals(93_412, DashReading.pick(read.orEmpty(), last = 93_000, unit = DistanceUnit.MILES))
        assertEquals(93_412, DashReading.pick(read.orEmpty(), last = null, unit = DistanceUnit.MILES))
    }

    @Test
    fun readsNothingFromAPictureWithNoDigits() {
        // Shapes with edges, like the trim and vents round a cluster.
        val shapes = picture(lit = false) { canvas, paint ->
            for (i in 0 until 8) canvas.drawRect(40f + i * 100, 80f + (i % 3) * 120, 100f + i * 100, 300f + (i % 3) * 60, paint)
            paint.strokeWidth = 6f
            for (i in 0 until 12) canvas.drawLine(0f, i * 48f, width.toFloat(), i * 40f + 30, paint)
        }
        assertNull(DashReading.pick(read(shapes).orEmpty(), last = null, unit = DistanceUnit.MILES))
        assertNull(DashReading.pick(read(ByteArray(width * height) { 120 }).orEmpty(), last = null, unit = DistanceUnit.MILES))
    }
}
