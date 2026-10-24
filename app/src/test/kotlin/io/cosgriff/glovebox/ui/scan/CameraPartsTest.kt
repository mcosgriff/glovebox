package io.cosgriff.glovebox.ui.scan

import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.ExifData
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.cosgriff.glovebox.ui.garage.waitFor
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class CameraPartsTest {
    @get:Rule val compose = createComposeRule()

    /** A camera frame of [w] x [h] grey pixels, each row padded to [stride] bytes, turned [rotation] degrees. */
    private class Frame(private val pixels: ByteArray, private val w: Int, private val h: Int, private val stride: Int, private val rotation: Int) : ImageProxy {
        private val plane: ImageProxy.PlaneProxy = object : ImageProxy.PlaneProxy {
            private val buffer: ByteBuffer = ByteBuffer.wrap(ByteArray(stride * h).also { padded ->
                for (row in 0 until h) {
                    System.arraycopy(pixels, row * w, padded, row * stride, w)
                    // Padding that must never be read as picture.
                    for (x in w until stride) padded[row * stride + x] = 99
                }
            })
            override fun getRowStride(): Int = stride
            override fun getPixelStride(): Int = 1
            override fun getBuffer(): ByteBuffer = buffer
        }
        private val info: ImageInfo = object : ImageInfo {
            override fun getTagBundle(): TagBundle = TagBundle.emptyBundle()
            override fun getTimestamp(): Long = 0L
            override fun getRotationDegrees(): Int = rotation
            override fun populateExifData(exifBuilder: ExifData.Builder) = Unit
        }
        override fun close() = Unit
        override fun getCropRect(): Rect = Rect(0, 0, w, h)
        override fun setCropRect(rect: Rect?) = Unit
        override fun getFormat(): Int = 35
        override fun getHeight(): Int = h
        override fun getWidth(): Int = w
        override fun getPlanes(): Array<ImageProxy.PlaneProxy> = arrayOf(plane)
        override fun getImageInfo(): ImageInfo = info
        override fun getImage(): Image? = null
    }

    @Test
    fun `the band read is the middle of the upright frame, without the rows' padding`() {
        val w = 10
        val h = 6
        val pixels = ByteArray(w * h) { it.toByte() }
        val (band, bw, bh) = Frame(pixels, w, h, stride = 16, rotation = 90).guideBand()
        val (upright, uw, uh) = VinBarcodeReader.rotate(pixels, w, h, 90)
        val (expected, ew, eh) = VinBarcodeReader.centre(upright, uw, uh, GUIDE_WIDTH, GUIDE_HEIGHT)
        assertEquals(ew to eh, bw to bh)
        assertEquals(expected.toList(), band.toList())
        assertEquals(false, band.contains(99.toByte()))
    }

    @Test
    fun `a frame job is started for the screen and closed on its own thread once the screen goes`() {
        val analysis = ImageAnalysis.Builder().build()
        var shown by mutableStateOf(true)
        var started = 0
        var closedOn: Thread? = null
        compose.setContent {
            if (shown) {
                FrameReader(analysis) {
                    started++
                    object : FrameJob {
                        override fun read(image: ImageProxy) = Unit
                        override fun close() {
                            closedOn = Thread.currentThread()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(1, started)
        shown = false
        compose.waitForIdle()
        waitFor("the job to close") { closedOn != null }
        // The native readers can only be closed from the thread that read the frames.
        assertNotEquals(Thread.currentThread(), closedOn)
        // A job that keeps the default close is fine too.
        object : FrameJob { override fun read(image: ImageProxy) = Unit }.close()
    }

    @Test
    fun `the found card reads again or uses what it found`() {
        val taps = mutableListOf<String>()
        compose.setContent {
            FoundCard(
                heading = { ScrimText("Made-up heading", MaterialTheme.typography.bodySmall, live = true) },
                value = "61,250 mi",
                valueStyle = MaterialTheme.typography.titleLarge,
                again = "Again",
                onAgain = { taps += "again" },
                use = "Use it",
                onUse = { taps += "use" },
            )
            GuideBox()
            Text("x")
        }
        compose.onNodeWithText("Made-up heading").assertExists()
        compose.onNodeWithText("61,250 mi").assertExists()
        compose.onNodeWithText("Again").performClick()
        compose.onNodeWithText("Use it").performClick()
        assertEquals(listOf("again", "use"), taps)
    }
}
