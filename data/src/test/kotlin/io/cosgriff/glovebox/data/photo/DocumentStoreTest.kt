package io.cosgriff.glovebox.data.photo

import android.content.Context
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.DocumentProblem
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.VehicleDocuments
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
class DocumentStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 5, 4)
    private lateinit var db: GloveboxDatabase
    private lateinit var crypto: PhotoCrypto
    private lateinit var photos: RecordPhotoStore
    private lateinit var store: DocumentStore
    private var vehicle = 0L

    @Before fun setUp() = runBlocking {
        FakeAndroidKeyStore.install()
        PickedFilesProvider.install()
        db = inMemoryDatabase(context)
        crypto = PhotoCrypto(context)
        photos = RecordPhotoStore(context, crypto, db.service(), db.vehicles(), db.documents())
        store = DocumentStore(context, crypto, db.documents(), photos)
        vehicle = db.vehicles().add(testVehicle(), firstReading = null)
    }

    @After fun tearDown() {
        db.close()
        FakeAndroidKeyStore.uninstall()
    }

    private suspend fun added(result: AddedDocument): DocumentEntity = db.documents().byId((result as AddedDocument.Added).id)!!

    @Test
    fun `a PDF is kept exactly as it came, encrypted, named from its file`() = runBlocking {
        val doc = added(store.add(vehicle, PickedFilesProvider.offer("a", madeUpPdf), "made-up_title-2026.pdf", "Other", today))

        assertEquals("made up title 2026", doc.name)
        assertEquals("PDF", doc.format)
        assertEquals("OTHER", doc.type)
        assertEquals(madeUpPdf.size.toLong(), doc.sizeBytes)
        assertEquals(today, doc.addedOn)
        assertTrue(doc.fileName.startsWith("document-") && doc.fileName.endsWith(".pdf"))
        assertArrayEquals(madeUpPdf, crypto.decryptBytes(doc.fileName))
    }

    @Test
    fun `a picture is redrawn as a JPEG no larger than a document needs, named after its type when its file gives no name`() = runBlocking {
        val doc = added(store.add(vehicle, PickedFilesProvider.offer("b", picture(5000, 2500)), null, "Other", today))

        assertEquals("Other", doc.name)
        assertEquals("JPEG", doc.format)
        val plain = crypto.decryptBytes(doc.fileName)!!
        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeByteArray(plain, 0, plain.size, it) }
        assertEquals(VehicleDocuments.MAX_EDGE, size.outWidth)
        assertEquals(plain.size.toLong(), doc.sizeBytes)
        assertEquals(VehicleDocuments.MAX_EDGE, store.loadPicture(doc)!!.width)
    }

    @Test
    fun `a file that is empty, too big, unreadable or neither a PDF nor a picture is refused, and nothing is kept`() = runBlocking {
        PickedFilesProvider.denied += "denied"
        val cases = mapOf(
            PickedFilesProvider.offer("empty", ByteArray(0)) to DocumentProblem.EMPTY,
            PickedFilesProvider.offer("big", ByteArray((VehicleDocuments.MAX_BYTES + 1).toInt())) to DocumentProblem.TOO_BIG,
            PickedFilesProvider.uri("missing") to DocumentProblem.UNREADABLE,
            PickedFilesProvider.uri("denied") to DocumentProblem.UNREADABLE,
            PickedFilesProvider.offer("text", "made-up notes".toByteArray()) to DocumentProblem.NOT_A_DOCUMENT,
        )
        for ((uri, problem) in cases) {
            assertEquals(uri.toString(), AddedDocument.Refused(problem), store.add(vehicle, uri, "x.pdf", "Other", today))
        }
        assertTrue(db.documents().documentsFor(vehicle).isEmpty())
        assertEquals(0, crypto.photoDir.list()!!.size)
    }

    @Test
    fun `a report already in memory is filed with its type and date`() = runBlocking {
        val id = store.addPdf(vehicle, madeUpPdf, DocumentType.REPORT, "History report", today.minusDays(3), today)
        val doc = db.documents().byId(id)!!
        assertEquals("REPORT", doc.type)
        assertEquals(today.minusDays(3), doc.date)
        assertArrayEquals(madeUpPdf, crypto.decryptBytes(doc.fileName))
    }

    @Test
    fun `a document for a vehicle deleted meanwhile leaves no file behind`() {
        assertThrows(Exception::class.java) { runBlocking { store.addPdf(999, madeUpPdf, DocumentType.REPORT, "Report", null, today) } }
        assertEquals(0, crypto.photoDir.list()!!.size)
    }

    @Test
    fun `deleting a document removes its row and its file`() = runBlocking {
        val doc = db.documents().byId(store.addPdf(vehicle, madeUpPdf, DocumentType.TITLE, "Title", null, today))!!
        store.delete(doc)
        assertNull(db.documents().byId(doc.id))
        assertEquals(0, crypto.photoDir.list()!!.size)
    }

    @Test
    fun `a copy for another app goes in the share folder under the document's name, replacing the last`() = runBlocking {
        val doc = db.documents().byId(store.addPdf(vehicle, madeUpPdf, DocumentType.TITLE, "Title", null, today))!!
        File(photos.sharedDir, "older.jpg").writeText("left from the last share")

        val copy = store.openCopy(doc)!!

        assertEquals(photos.sharedDir, copy.parentFile)
        assertEquals(VehicleDocuments.fileName("Title", VehicleDocuments.format("PDF")), copy.name)
        assertEquals(listOf(copy.name), photos.sharedDir.list()!!.toList())
        assertArrayEquals(madeUpPdf, copy.readBytes())
    }

    @Test
    fun `a document whose file can't be read gives no copy and no viewer`() = runBlocking {
        val doc = db.documents().byId(store.addPdf(vehicle, madeUpPdf, DocumentType.TITLE, "Title", null, today))!!
        val file = File(crypto.photoDir, doc.fileName)
        file.writeBytes(file.readBytes().also { it[it.size - 3] = (it[it.size - 3] + 1).toByte() })

        assertNull(store.openCopy(doc))
        assertEquals(0, photos.sharedDir.list()!!.size)
        assertNull(store.openPdf(doc))

        crypto.delete(doc.fileName)
        assertNull(store.openCopy(doc))
        assertNull(store.loadPicture(doc))
    }

    @Test
    fun `the viewer's PDF is handed over in memory`() = runBlocking {
        val doc = db.documents().byId(store.addPdf(vehicle, madeUpPdf, DocumentType.TITLE, "Title", null, today))!!

        val descriptor = store.openPdf(doc)!!

        assertArrayEquals(madeUpPdf, ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() })
        assertEquals(0, photos.viewingDir.list()!!.size)
    }

    @Test
    @Config(sdk = [29])
    fun `on Android 10 the viewer's PDF passes through a private file that is gone once it is open`() = runBlocking {
        val doc = db.documents().byId(store.addPdf(vehicle, madeUpPdf, DocumentType.TITLE, "Title", null, today))!!

        val descriptor = store.openPdf(doc)!!

        assertArrayEquals(madeUpPdf, ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() })
        assertEquals(0, photos.viewingDir.list()!!.size)
    }
}
