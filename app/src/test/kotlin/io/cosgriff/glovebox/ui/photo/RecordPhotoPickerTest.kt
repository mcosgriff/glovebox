package io.cosgriff.glovebox.ui.photo

import android.net.Uri
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import io.cosgriff.glovebox.ui.service.ServiceTestDb
import io.cosgriff.glovebox.ui.service.asNumber
import io.cosgriff.glovebox.ui.service.awaitUntil
import androidx.core.content.FileProvider
import org.junit.Before
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, sdk = [35])
class RecordPhotoPickerTest {
    private val t = ServiceTestDb()
    private val picker = t.picker
    private var picks = 0
    private val cameraUris = mutableListOf<Uri>()
    private val openPicker: () -> Unit = { picks++ }
    private val openCamera: (Uri) -> Unit = { cameraUris += it }

    @After fun close() = t.close()

    /**
     * FileProvider keeps each authority's folders for the life of the process, and every Robolectric test has a
     * folder of its own, so the one cached by an earlier test would refuse this test's capture file.
     */
    @Before fun forgetFileProviderRoots() {
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.get(null) as MutableMap<*, *>
        synchronized(cache) { cache.clear() }
    }

    private fun attach() = picker.attach(openPicker, openCamera)

    private fun string(id: Int, vararg args: Any) = t.context.getString(id, *args)

    /** The capture file the camera was pointed at; the test writes to it as the camera app would. */
    private fun captureFile(): File = File(t.photos.captureDir, cameraUris.last().lastPathSegment!!)

    private fun draft(key: String): List<String> {
        awaitUntil("photos on $key") { picker.drafts.value[key] != null }
        return picker.drafts.value.getValue(key)
    }

    private fun awaitProblem(): String? {
        awaitUntil("a problem to be said") { picker.problem.value != null }
        return picker.problem.value
    }

    @Test
    fun `nothing opens until an activity has attached its launchers, and after it lets go`() {
        picker.pick(PhotoTarget.Draft("form"), room = 3)
        picker.capture(PhotoTarget.Draft("form"), room = 3)
        assertEquals(0, picks)
        assertTrue(cameraUris.isEmpty())

        attach()
        // Someone else's launcher doesn't detach this one.
        picker.detach {}
        picker.pick(PhotoTarget.Draft("form"), room = 3)
        assertEquals(1, picks)

        picker.detach(openPicker)
        picker.pick(PhotoTarget.Draft("form"), room = 3)
        picker.capture(PhotoTarget.Draft("form"), room = 3)
        assertEquals(1, picks)
        assertTrue(cameraUris.isEmpty())
    }

    @Test
    fun `a full form opens nothing and says why`() {
        attach()
        picker.pick(PhotoTarget.Draft("form"), room = 0)
        assertEquals(0, picks)
        assertEquals(string(R.string.photos_full, RecordPhotos.MAX_PER_RECORD.asNumber()), picker.problem.value)
        // The next attempt clears it.
        picker.pick(PhotoTarget.Draft("form"), room = 1)
        assertNull(picker.problem.value)
    }

    @Test
    fun `picked pictures are encrypted onto the form, no more than it has room for`() {
        attach()
        picker.pick(PhotoTarget.Draft("form"), room = 2)
        picker.onPicked(listOf(t.imageUri(), t.imageUri(), t.imageUri()))

        val names = draft("form")
        assertEquals(2, names.size)
        names.forEach { assertTrue(t.hasFile(it)) }
        assertEquals(string(R.string.photos_too_many, 2.asNumber(), RecordPhotos.MAX_PER_RECORD.asNumber()), awaitProblem())
    }

    @Test
    fun `a picture that can't be read is counted and the rest are kept`() {
        attach()
        picker.pick(PhotoTarget.Draft("form"), room = 5)
        picker.onPicked(listOf(t.imageUri(), Uri.parse("content://made.up.provider/missing.png")))
        assertEquals(string(R.string.photos_unreadable, 1.asNumber()), awaitProblem())
        assertEquals(1, draft("form").size)
    }

    @Test
    fun `backing out of the picker adds nothing, and a late answer goes nowhere`() {
        attach()
        picker.pick(PhotoTarget.Draft("form"), room = 2)
        picker.onPicked(emptyList())
        picker.onPicked(listOf(t.imageUri()))
        assertTrue(picker.drafts.value.isEmpty())
        assertNull(picker.problem.value)
    }

    @Test
    fun `a photo the camera didn't save adds nothing and says nothing, and its file still goes`() {
        attach()
        picker.capture(PhotoTarget.Draft("form"), room = 1)
        // The camera app is handed a content URI in the capture folder, never the file's path.
        assertEquals("content", cameraUris.single().scheme)
        val file = captureFile().apply { writeText("half written") }
        assertEquals(t.photos.captureDir, file.parentFile)
        picker.onCaptured(saved = false)
        awaitUntil("the capture file to go") { !file.exists() }
        assertTrue(picker.drafts.value.isEmpty())
        assertNull(picker.problem.value)
        // Nothing was waiting for a second answer.
        picker.onCaptured(saved = true)
        assertNull(picker.problem.value)
    }

    @Test
    fun `a photo the camera saved but that can't be read is said so`() {
        attach()
        picker.capture(PhotoTarget.Draft("form"), room = 1)
        val file = captureFile().apply { writeText("not a picture") }
        picker.onCaptured(saved = true)
        assertEquals(string(R.string.photo_unreadable), awaitProblem())
        assertFalse(file.exists())
    }

    @Test
    fun `photos for a saved record are added to it, as many as still fit`() = runBlocking {
        attach()
        val car = t.vehicle()
        val id = t.record(car)
        t.db.service().insertPhotos((1 until RecordPhotos.MAX_PER_RECORD).map { PhotoEntity(recordId = id, fileName = "record-old-$it.jpg") })

        picker.pick(PhotoTarget.Record(id), room = 5)
        picker.onPicked(listOf(t.imageUri(), t.imageUri()))
        awaitUntil("the photo to be added") { runBlocking { t.db.service().photoCount(id) } == RecordPhotos.MAX_PER_RECORD && t.photoDir.listFiles().orEmpty().size == 1 }

        val photos = t.db.service().record(id)!!.photos
        assertEquals(RecordPhotos.MAX_PER_RECORD, photos.size)
        val added = photos.filterNot { it.fileName.startsWith("record-old") }.single()
        assertTrue(t.hasFile(added.fileName))
        // The one that didn't fit isn't left in the folder.
        assertEquals(listOf(added.fileName), t.photoDir.listFiles().orEmpty().map { it.name })
        assertTrue(picker.drafts.value.isEmpty())
    }

    @Test
    fun `photos for a record deleted meanwhile are not kept`() = runBlocking {
        attach()
        val car = t.vehicle()
        val kept = t.record(car)
        picker.pick(PhotoTarget.Record(404), room = 2)
        picker.onPicked(listOf(t.imageUri()))
        // A second pick, for a record that is there, shows when the first has been dealt with.
        picker.pick(PhotoTarget.Record(kept), room = 2)
        picker.onPicked(listOf(t.imageUri()))
        awaitUntil("the second photo") { runBlocking { t.db.service().photoCount(kept) } == 1 }
        awaitUntil("the first photo's file to go") { t.photoDir.listFiles().orEmpty().size == 1 }
        assertEquals(t.db.service().record(kept)!!.photos.single().fileName, t.photoDir.listFiles()!!.single().name)
    }

    @Test
    fun `a form's photos are its own until saved, and go when it is abandoned`() {
        attach()
        picker.pick(PhotoTarget.Draft("a"), room = 3)
        picker.onPicked(listOf(t.imageUri(), t.imageUri()))
        val (first, second) = draft("a")
        picker.pick(PhotoTarget.Draft("b"), room = 3)
        picker.onPicked(listOf(t.imageUri()))
        val other = draft("b").single()

        picker.removeDraft("a", first)
        awaitUntil("the removed photo's file to go") { !t.hasFile(first) }
        assertEquals(listOf(second), picker.drafts.value.getValue("a"))

        // Saved: the record owns the files now, so they stay.
        assertEquals(listOf(second), picker.takeDraft("a"))
        assertNull(picker.drafts.value["a"])
        picker.discardDraft("a")
        assertTrue(t.hasFile(second))

        // Abandoned: they go.
        picker.discardDraft("b")
        awaitUntil("the abandoned photo's file to go") { !t.hasFile(other) }
        assertTrue(picker.drafts.value.isEmpty())
    }
}
