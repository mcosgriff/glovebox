package io.cosgriff.glovebox.ui.vehicle

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import io.cosgriff.glovebox.domain.DocumentProblem
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * The pickers the activity owns, so an answer that comes back after the app has locked still finds its vehicle,
 * and what they file: the picked bytes, checked, then encrypted. Native graphics, so a file that isn't a picture
 * is refused as it would be on a phone. A vehicle photo is decoded from its content URI, which Robolectric
 * can't open, so only its refusal is driven here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = ScreenshotApp::class, sdk = [35])
class DocumentPickerTest {
    @get:Rule val g = VehicleTestGarage()

    private var picks = 0
    private val captures = mutableListOf<Uri>()
    private val pick: () -> Unit = { picks++ }
    private val camera = { uri: Uri -> captures += uri }
    private val pdf = "%PDF-1.4\n% made up for a test\n%%EOF\n".toByteArray()

    private fun file(name: String, bytes: ByteArray) = Uri.fromFile(File(g.context.cacheDir, name).apply { writeBytes(bytes) })

    /** Picks [uri] for [vehicleId], and waits for the picker to settle. */
    private fun picked(uri: Uri, vehicleId: Long = g.add(VehicleFixtures.wagon)): Long = runBlocking {
        g.documentPicker.pick(vehicleId)
        g.documentPicker.onPicked(uri)
        eventually { g.documentPicker.problem.value != null || g.documentPicker.added.value != null }
        assertFalse(g.documentPicker.working.value)
        vehicleId
    }

    private fun refusal(uri: Uri, vehicleId: Long = g.add(VehicleFixtures.wagon)): DocumentProblem? {
        picked(uri, vehicleId)
        assertNull(g.documentPicker.added.value)
        return g.documentPicker.problem.value
    }

    @Test
    fun `nothing opens until the activity attaches its launchers`() {
        g.documentPicker.pick(1)
        g.documentPicker.capture(1)
        assertEquals(0, picks)
        assertTrue(captures.isEmpty())
        // An answer nobody asked for is ignored.
        g.documentPicker.onPicked(file("stray.pdf", pdf))
        g.documentPicker.onCaptured(saved = true)
        Thread.sleep(100)
        assertNull(g.documentPicker.problem.value)
        assertNull(g.documentPicker.added.value)
    }

    @Test
    fun `an empty file, a missing one, and one that is neither a PDF nor a picture are each refused with why`() {
        g.documentPicker.attach(pick, camera)
        assertEquals(DocumentProblem.EMPTY, refusal(file("empty.pdf", ByteArray(0))))
        assertEquals(DocumentProblem.UNREADABLE, refusal(Uri.fromFile(File(g.context.cacheDir, "not-there.pdf"))))
        assertEquals(DocumentProblem.NOT_A_DOCUMENT, refusal(file("notes.txt", "Made-up notes, not a document.".toByteArray())))
        assertEquals(3, picks)
        assertTrue(g.crypto.photoDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun `a PDF is filed as it came, encrypted, and the screen is told which to name`() {
        g.documentPicker.attach(pick, camera)
        val id = picked(file("made-up.pdf", pdf))
        val added = checkNotNull(g.documentPicker.added.value)
        assertEquals(id, added.vehicleId)
        val row = checkNotNull(runBlocking { g.db.documents().byId(added.id) })
        assertEquals("PDF", row.format)
        assertEquals(pdf.size.toLong(), row.sizeBytes)
        // On disk only encrypted; decrypted, it is the file as picked.
        assertFalse(File(g.crypto.photoDir, row.fileName).readBytes().contentEquals(pdf))
        assertArrayEquals(pdf, g.crypto.decryptBytes(row.fileName))

        g.documentPicker.consumeAdded()
        assertNull(g.documentPicker.added.value)
    }

    @Test
    fun `a picture is redrawn as a JPEG before it is filed`() {
        g.documentPicker.attach(pick, camera)
        picked(file("receipt.png", png()))
        val row = checkNotNull(runBlocking { g.db.documents().byId(g.documentPicker.added.value!!.id) })
        assertEquals("JPEG", row.format)
        val bytes = checkNotNull(g.crypto.decryptBytes(row.fileName))
        // A JPEG starts FF D8.
        assertEquals(0xFF.toByte(), bytes[0])
        assertEquals(0xD8.toByte(), bytes[1])
    }

    @Test
    fun `a file picked for a vehicle deleted meanwhile is refused, and leaves no file behind`() {
        g.documentPicker.attach(pick, camera)
        assertEquals(DocumentProblem.FAILED, refusal(file("made-up.pdf", pdf), vehicleId = 404))
        assertTrue(g.crypto.photoDir.listFiles().isNullOrEmpty())
        g.documentPicker.clearProblem()
        assertNull(g.documentPicker.problem.value)
    }

    @Test
    fun `backing out of the picker adds nothing, and a late answer after it finds no vehicle`() {
        g.documentPicker.attach(pick, camera)
        g.documentPicker.pick(7)
        g.documentPicker.onPicked(null)
        g.documentPicker.onPicked(file("late.pdf", pdf))
        Thread.sleep(100)
        assertNull(g.documentPicker.problem.value)
        assertNull(g.documentPicker.added.value)
    }

    @Test
    fun `only the activity that attached lets go, and the camera goes with the picker`() {
        g.documentPicker.attach(pick, camera)
        g.documentPicker.detach {}
        g.documentPicker.pick(1)
        assertEquals(1, picks)

        g.documentPicker.detach(pick)
        g.documentPicker.pick(1)
        g.documentPicker.capture(1)
        assertEquals(1, picks)
        assertTrue(captures.isEmpty())
    }

    @Test
    fun `a photo the camera saved is filed, and its clear copy deleted either way`() {
        g.documentPicker.attach(pick, camera)
        val id = g.add(VehicleFixtures.wagon)
        g.documentPicker.capture(id)
        val written = File(g.recordPhotos.captureDir, captures.single().lastPathSegment!!).apply { writeBytes(png()) }
        g.documentPicker.onCaptured(saved = true)
        runBlocking { eventually { g.documentPicker.added.value != null && !written.exists() } }
        assertEquals(id, g.documentPicker.added.value!!.vehicleId)

        // Not kept: the copy still goes, and nothing is filed.
        g.documentPicker.consumeAdded()
        g.documentPicker.capture(id)
        val second = File(g.recordPhotos.captureDir, captures.last().lastPathSegment!!).apply { writeBytes(png()) }
        g.documentPicker.onCaptured(saved = false)
        runBlocking { eventually { !second.exists() } }
        assertNull(g.documentPicker.added.value)
        assertEquals(1, runBlocking { g.db.documents().documentsFor(id) }.size)
    }

    @Test
    fun `the vehicle photo picker remembers the vehicle, and says when a picked image can't be read`() {
        var launched = 0
        val launch: () -> Unit = { launched++ }
        val picker = g.photoPicker
        picker.pick(3)
        assertEquals(0, launched)

        picker.attach(launch)
        val id = g.add(VehicleFixtures.wagon)
        picker.pick(id)
        assertEquals(1, launched)
        picker.onPicked(file("not-an-image.jpg", "Made-up text, not an image.".toByteArray()))
        runBlocking { eventually { picker.failedFor.value == id } }
        assertNull(g.vehicle(id)!!.photoFile)

        // The next attempt clears it; backing out, or an answer after that, changes nothing.
        picker.pick(id)
        assertNull(picker.failedFor.value)
        picker.onPicked(null)
        picker.onPicked(file("late.png", png()))
        Thread.sleep(100)
        assertNull(picker.failedFor.value)
        assertNull(g.vehicle(id)!!.photoFile)

        picker.detach {}
        picker.pick(id)
        assertEquals(3, launched)
        picker.detach(launch)
        picker.pick(id)
        assertEquals(3, launched)
    }

    /** A small made-up picture. */
    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(194, 65, 12)) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }
}
