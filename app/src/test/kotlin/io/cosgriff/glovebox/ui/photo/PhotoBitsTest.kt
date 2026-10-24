package io.cosgriff.glovebox.ui.photo

import android.Manifest
import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.ui.screens.PHONE
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.ServiceTestDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Photo tiles, the attach menu and the zoomable picture, with a loader made here rather than by Hilt. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, qualifiers = PHONE, sdk = [35])
class PhotoBitsTest {
    @get:Rule val compose = createComposeRule()
    private val t = ServiceTestDb()
    private val loader = t.owned(PhotoLoaderViewModel(t.photos))

    @After fun close() = t.close()

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)

    @Test
    fun `a thumbnail is decrypted once and kept, a full picture each time, and a missing file is nothing`() = runBlocking<Unit> {
        val name = t.realPhoto(width = 1_200, height = 900)
        val thumb = loader.thumbnail(name)
        assertNotNull(thumb)
        assertSame(thumb, loader.thumbnail(name))
        val full: ImageBitmap = loader.full(name)!!
        assertEquals(1_200, full.width)
        assertNull(loader.thumbnail("record-missing.jpg"))
        assertNull(loader.full("record-missing.jpg"))
    }

    @Test
    fun `a tile shows its photo, opens it on a tap, and can be taken off`() = runBlocking<Unit> {
        val name = t.realPhoto()
        var opened = 0
        var removed = 0
        compose.setContent { PhotoThumb(name, "photo 1", Modifier.size(96.dp), onClick = { opened++ }, onRemove = { removed++ }, loader = loader) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("photo 1")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("photo 1").performClick()
        assertEquals(1, opened)
        compose.onNodeWithContentDescription(string(R.string.photo_remove, "photo 1")).performClick()
        assertEquals(1, removed)
    }

    @Test
    fun `a tile without actions offers none`() = runBlocking<Unit> {
        val name = t.realPhoto()
        compose.setContent { PhotoThumb(name, "photo 1", Modifier.size(96.dp), loader = loader) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("photo 1")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(string(R.string.photo_remove, "photo 1")).assertDoesNotExist()
    }

    @Test
    fun `the attach menu chooses photos, and takes one once the camera is allowed`() {
        var took = 0
        var chose = 0
        compose.setContent { AttachMenu(onTake = { took++ }, onChoose = { chose++ }) { open -> TextButton(onClick = open) { Text("Attach") } } }
        compose.onNodeWithText("Attach").performClick()
        compose.onNodeWithText(string(R.string.photo_choose)).performClick()
        assertEquals(1, chose)

        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.CAMERA)
        compose.onNodeWithText("Attach").performClick()
        compose.onNodeWithText(string(R.string.documents_take_photo)).performClick()
        assertEquals(1, took)
    }

    @Test
    fun `a double tap zooms in and another zooms back out`() = runBlocking<Unit> {
        val image = loader.full(t.realPhoto())
        val zoom = mutableListOf<Boolean>()
        compose.setContent { ZoomableImage(image, "receipt", key = "receipt", onZoomChange = { zoom += it }) }
        compose.onNodeWithContentDescription("receipt").performTouchInput { doubleClick() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("receipt").performTouchInput { doubleClick() }
        compose.waitForIdle()
        assertEquals(listOf(true, false), zoom)
    }
}
