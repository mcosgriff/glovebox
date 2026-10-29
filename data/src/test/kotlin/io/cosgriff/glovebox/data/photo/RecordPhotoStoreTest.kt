package io.cosgriff.glovebox.data.photo

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.RecordPhotos
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordPhotoStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: GloveboxDatabase
    private lateinit var crypto: PhotoCrypto
    private lateinit var store: RecordPhotoStore

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        PickedFilesProvider.install()
        db = inMemoryDatabase(context)
        crypto = PhotoCrypto(context)
        store = RecordPhotoStore(context, crypto, db.service(), db.vehicles(), db.documents())
    }

    @After fun tearDown() {
        db.close()
        FakeAndroidKeyStore.uninstall()
    }

    private fun bounds(bytes: ByteArray) = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it) }

    @Test
    fun `a picked photo is redrawn as a smaller JPEG and kept only encrypted`() = runBlocking {
        val uri = PickedFilesProvider.offer("big.png", picture(3000, 1500))

        val name = store.import(uri)!!

        assertTrue(name.startsWith("record-") && name.endsWith(".jpg"))
        val plain = crypto.decryptBytes(name)!!
        assertEquals(0xFF.toByte(), plain[0])
        assertEquals(0xD8.toByte(), plain[1])
        val size = bounds(plain)
        assertEquals(RecordPhotos.MAX_EDGE, size.outWidth)
        assertEquals(RecordPhotos.MAX_EDGE / 2, size.outHeight)
        assertFalse(File(crypto.photoDir, name).readBytes().contentEquals(plain))
    }

    @Test
    fun `an invoice page the camera took is held full size, only encrypted, until it is read`() = runBlocking {
        val bytes = "made-up camera bytes".toByteArray()
        val capture = File(context.cacheDir, "capture.jpg").apply { writeBytes(bytes) }

        val name = store.hold(capture)!!

        assertTrue(name.startsWith("held-"))
        assertFalse(File(crypto.photoDir, name).readBytes().contentEquals(bytes))
        assertTrue(store.held(name)!!.contentEquals(bytes))
    }

    @Test
    fun `something that isn't a picture is not imported`() = runBlocking {
        assertNull(store.import(PickedFilesProvider.offer("notes.txt", "made-up notes".toByteArray())))
        assertEquals(0, crypto.photoDir.list()!!.size)
    }

    @Test
    fun `a photo is loaded shrunk for where it is shown`() = runBlocking {
        val name = store.import(PickedFilesProvider.offer("p.png", picture(1600, 800)))!!

        val thumb = store.load(name, 360)!!
        assertEquals(400, thumb.width)

        val within = store.loadWithin(name, 360)!!
        assertEquals(360, within.width)
        assertEquals(180, within.height)

        val small = store.loadWithin(name, 2000)!!
        assertEquals(1600, small.width)

        assertNull(store.load("missing.jpg", 360))
        assertNull(store.loadWithin("missing.jpg", 360))
    }

    @Test
    fun `sharing writes one decrypted copy to the share folder, replacing the last`() = runBlocking {
        crypto.encryptTo("record-a.jpg", picture(10, 10, android.graphics.Bitmap.CompressFormat.JPEG).inputStream())
        File(store.sharedDir, "older-copy.jpg").writeText("left from the last share")

        val copy = store.shareCopy("record-a.jpg")!!

        assertEquals(listOf("glovebox-photo.jpg"), store.sharedDir.list()!!.toList())
        assertArrayEquals(crypto.decryptBytes("record-a.jpg"), copy.readBytes())
        assertNull(store.shareCopy("missing.jpg"))
        assertEquals(0, store.sharedDir.list()!!.size)
    }

    @Test
    fun `deleting photos removes their files`() = runBlocking {
        crypto.encryptTo("a.jpg", "a".toByteArray().inputStream())
        crypto.encryptTo("b.jpg", "b".toByteArray().inputStream())
        store.delete(listOf("a.jpg"))
        assertEquals(listOf("b.jpg"), crypto.photoDir.list()!!.toList())
    }

    @Test
    fun `the launch sweep clears plaintext and old files nothing points at, and nothing else`() = runBlocking {
        val id = db.vehicles().add(testVehicle(photoFile = "vehicle.jpg"), firstReading = null)
        val record = db.service().insertWithItems(ServiceRecordEntity(vehicleId = id, date = LocalDate.of(2026, 5, 4), odometer = 1, diy = true), emptyList())
        db.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = "kept.jpg")))
        val old = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        listOf("kept.jpg", "vehicle.jpg", "orphan.jpg", "just-taken.jpg").forEach { crypto.encryptTo(it, it.toByteArray().inputStream()) }
        listOf("kept.jpg", "vehicle.jpg", "orphan.jpg").forEach { File(crypto.photoDir, it).setLastModified(old) }
        listOf(store.captureDir, store.sharedDir, store.viewingDir).forEach { File(it, "plain.jpg").writeText("in the clear") }

        store.sweep()

        assertEquals(listOf("just-taken.jpg", "kept.jpg", "vehicle.jpg"), crypto.photoDir.list()!!.sorted())
        listOf(store.captureDir, store.sharedDir, store.viewingDir).forEach { assertEquals(0, it.list()!!.size) }
    }

    @Test
    fun `a sweep with no encrypted files only clears the cache`() = runBlocking {
        File(store.captureDir, "plain.jpg").writeText("in the clear")
        store.sweep()
        assertEquals(0, store.captureDir.list()!!.size)
        assertNotNull(crypto.photoDir.list())
    }

    @Test
    fun `the capture folder is where the camera writes, in the cache`() {
        assertEquals(File(context.cacheDir, "capture"), store.captureDir)
        assertTrue(store.captureDir.isDirectory)
    }
}
