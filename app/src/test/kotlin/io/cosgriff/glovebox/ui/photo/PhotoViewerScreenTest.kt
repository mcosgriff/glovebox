package io.cosgriff.glovebox.ui.photo

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.ui.PhotoViewerRoute
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.ServiceTestDb
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.forgetFileProviderRoots
import io.cosgriff.glovebox.ui.service.routeHandle
import io.cosgriff.glovebox.ui.service.settle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** A record's photos full screen, decrypted from the store: paging, sharing a decrypted copy, and closing. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class PhotoViewerScreenTest {
    @get:Rule val compose = createComposeRule()
    private val t = ServiceTestDb()
    private var closes = 0
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before fun roots() = forgetFileProviderRoots()

    @After fun close() = t.close()

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)

    private fun waitFor(description: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private suspend fun recordWith(vararg files: String): Long {
        val id = t.record(t.vehicle())
        t.db.service().insertPhotos(files.map { PhotoEntity(recordId = id, fileName = it) })
        return id
    }

    private fun show(recordId: Long, start: Int = 0): PhotoViewerViewModel {
        val vm = t.owned(PhotoViewerViewModel(routeHandle(PhotoViewerRoute(recordId, start)), t.db.service(), t.photos))
        val loader = t.owned(PhotoLoaderViewModel(t.photos))
        compose.setContent { PhotoViewerScreen(onClose = { closes++ }, vm = vm, loader = loader) }
        return vm
    }

    @Test
    fun `the photo tapped opens first, and a swipe moves to the next`() = runBlocking<Unit> {
        val id = recordWith(t.realPhoto(), t.realPhoto())
        val vm = show(id, start = 1)
        waitForText(string(R.string.photo_title_of, 2.asNumber(), 2.asNumber()))
        waitFor(string(R.string.photo_of, 2.asNumber(), 2.asNumber()))
        assertEquals(1, vm.start)

        compose.onRoot().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        // Already the last: a swipe on it goes nowhere.
        compose.onNodeWithText(string(R.string.photo_title_of, 2.asNumber(), 2.asNumber())).assertExists()
    }

    @Test
    fun `a swipe from the first goes to the second`() = runBlocking<Unit> {
        show(recordWith(t.realPhoto(), t.realPhoto()))
        waitFor(string(R.string.photo_of, 1.asNumber(), 2.asNumber()))
        compose.onNodeWithContentDescription(string(R.string.photo_of, 1.asNumber(), 2.asNumber())).performTouchInput { swipeLeft() }
        waitForText(string(R.string.photo_title_of, 2.asNumber(), 2.asNumber()))
    }

    @Test
    fun `one photo is just Photo, and Close closes`() = runBlocking<Unit> {
        show(recordWith(t.realPhoto()), start = 5)
        waitForText(string(R.string.photo))
        compose.onNodeWithContentDescription(string(R.string.close)).performClick()
        assertEquals(1, closes)
    }

    @Test
    fun `sharing asks first, then hands a decrypted copy from the shared folder to the share sheet`() = runBlocking<Unit> {
        show(recordWith(t.realPhoto()))
        waitForText(string(R.string.photo))
        compose.onNodeWithContentDescription(string(R.string.photo_share)).performClick()
        compose.onNodeWithText(string(R.string.photo_share_title)).assertExists()
        compose.onNodeWithText(string(R.string.cancel)).performClick()
        compose.onNodeWithText(string(R.string.photo_share_title)).assertDoesNotExist()
        assertNull(shadowOf(app).nextStartedActivity)

        compose.onNodeWithContentDescription(string(R.string.photo_share)).performClick()
        compose.onNodeWithText(string(R.string.share)).performClick()
        var started: Intent? = null
        compose.settle("the share sheet") { (started ?: shadowOf(app).nextStartedActivity)?.also { started = it } != null }
        val chooser = started!!
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("image/jpeg", send.type)
        @Suppress("DEPRECATION")
        val stream = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", stream.scheme)
        assertTrue(File(t.photos.sharedDir, "glovebox-photo.jpg").exists())
    }

    @Test
    fun `a photo that can't be read can't be shared, and says so`() = runBlocking<Unit> {
        show(recordWith(t.photoFile("record-garbled.jpg")))
        waitForText(string(R.string.photo))
        compose.onNodeWithContentDescription(string(R.string.photo_share)).performClick()
        compose.onNodeWithText(string(R.string.share)).performClick()
        compose.settle("the failure") { compose.onAllNodes(hasText(string(R.string.photo_share_failed))).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(string(R.string.ok)).performClick()
        compose.onNodeWithText(string(R.string.photo_share_failed)).assertDoesNotExist()
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `the viewer closes itself when the record's last photo goes`() = runBlocking<Unit> {
        val id = recordWith(t.realPhoto())
        show(id)
        waitForText(string(R.string.photo))
        t.db.service().clearPhotos(id)
        compose.settle("the viewer to close") { closes == 1 }
    }
}
