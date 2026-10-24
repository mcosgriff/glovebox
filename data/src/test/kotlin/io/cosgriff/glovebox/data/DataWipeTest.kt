package io.cosgriff.glovebox.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataWipeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)
    private lateinit var db: GloveboxDatabase
    private lateinit var photos: PhotoCrypto
    private lateinit var keys: DatabaseKeyManager

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        db = inMemoryDatabase(context)
        photos = PhotoCrypto(context)
        keys = DatabaseKeyManager(context)
    }

    @After fun tearDown() {
        runCatching { db.close() }
        FakeAndroidKeyStore.uninstall()
    }

    private fun photo(name: String) = name.also { photos.encryptTo(it, "made-up $it".toByteArray().inputStream()) }

    private fun exists(name: String) = File(photos.photoDir, name).exists()

    private suspend fun record(vehicleId: Long, imported: Boolean, vararg files: String): Long {
        val id = db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = vehicleId, date = day, odometer = 60_000, diy = true, imported = imported),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Oil change", catalogId = "oil_change")),
        )
        db.service().insertPhotos(files.map { PhotoEntity(recordId = id, fileName = it) })
        return id
    }

    @Test
    fun `deleting a vehicle takes its rows and its files, and leaves the other vehicle's`() = runBlocking {
        val gone = db.vehicles().add(testVehicle(photoFile = photo("vehicle-1.jpg")), firstReading = 60_000)
        val kept = db.vehicles().add(testVehicle(make = "Volkswagen", model = "GTI", photoFile = photo("vehicle-2.jpg")), firstReading = 30_000)
        record(gone, imported = false, photo("record-1.jpg"))
        record(kept, imported = false, photo("record-2.jpg"))
        db.documents().insert(DocumentEntity(vehicleId = gone, type = "TITLE", name = "Title", fileName = photo("document-1.pdf"), format = "PDF", sizeBytes = 1, addedOn = day))

        DataWipe(context, db, keys, photos).deleteVehicle(db.vehicles().byId(gone)!!)

        assertNull(db.vehicles().byId(gone))
        assertTrue(db.service().recordsFor(gone).isEmpty())
        assertTrue(db.odometer().readingsFor(gone).isEmpty())
        assertTrue(db.documents().documentsFor(gone).isEmpty())
        assertFalse(exists("vehicle-1.jpg") || exists("record-1.jpg") || exists("document-1.pdf"))

        assertEquals(1, db.service().recordsFor(kept).size)
        assertTrue(exists("vehicle-2.jpg") && exists("record-2.jpg"))
    }

    @Test
    fun `clearing history takes only imported records by default, with their photos`() = runBlocking {
        val id = db.vehicles().add(testVehicle(), firstReading = 60_000)
        record(id, imported = true, photo("imported.jpg"))
        record(id, imported = false, photo("logged.jpg"))
        val wipe = DataWipe(context, db, keys, photos)

        assertEquals(1, wipe.clearHistory(id, all = false))
        assertEquals(listOf(false), db.service().recordsFor(id).map { it.record.imported })
        assertFalse(exists("imported.jpg"))
        assertTrue(exists("logged.jpg"))

        assertEquals(1, wipe.clearHistory(id, all = true))
        assertTrue(db.service().recordsFor(id).isEmpty())
        assertFalse(exists("logged.jpg"))
        // Everything else about the vehicle stays.
        assertEquals(listOf(60_000), db.odometer().readingsFor(id).map { it.odometer })
    }

    /** Files standing in for everything the app keeps, so a wipe can be seen to take them all. */
    private fun fillDisk(): List<File> {
        keys.passphrase()
        photo("record-1.jpg")
        val database = context.getDatabasePath(GloveboxDatabase.FILE_NAME).apply { parentFile?.mkdirs(); writeText("made-up database") }
        val lock = File(database.path + ".lck").apply { writeText("") }
        val staged = File(photos.restoreDir, "record-2.jpg").apply { parentFile?.mkdirs(); writeText("staged") }
        val settings = File(context.filesDir, "datastore/settings.preferences_pb").apply { parentFile?.mkdirs(); writeText("made-up settings") }
        val export = File(context.cacheDir, "export/glovebox.pdf").apply { parentFile?.mkdirs(); writeText("made-up export") }
        val shared = File(context.cacheDir, "shared.jpg").apply { writeText("made-up copy") }
        return listOf(File(context.noBackupFilesDir, "db.key"), File(photos.photoDir, "record-1.jpg"), database, lock, staged, settings, export, shared)
    }

    @Test
    fun `delete everything destroys the keys before any file, then leaves nothing behind`() {
        val files = fillDisk()
        val stillThere = mutableMapOf<String, Boolean>()
        FakeAndroidKeyStore.onDelete = { alias -> stillThere[alias] = File(context.getDatabasePath(GloveboxDatabase.FILE_NAME).path).exists() && files[1].exists() }

        DataWipe(context, db, keys, photos).deleteEverything()

        // Both keys went while the database and the photos were still on disk: crypto-erase first.
        assertEquals(mapOf("glovebox_db_kek" to true, "glovebox_photo_kek" to true), stillThere)
        assertTrue(FakeAndroidKeyStore.keys.isEmpty())
        assertEquals(emptyList<File>(), files.filter { it.exists() })
        assertFalse(File(context.noBackupFilesDir, "wipe.pending").exists())
        assertEquals(emptyList<String>(), context.cacheDir.list()?.toList().orEmpty())
    }

    @Test
    fun `a wipe the process didn't survive is finished on the next launch`() {
        val files = fillDisk()
        File(context.noBackupFilesDir, "wipe.pending").createNewFile()

        DataWipe.finishInterrupted(context)

        assertTrue(FakeAndroidKeyStore.keys.isEmpty())
        assertEquals(emptyList<File>(), files.filter { it.exists() })
        assertFalse(File(context.noBackupFilesDir, "wipe.pending").exists())
    }

    @Test
    fun `an ordinary launch touches nothing`() {
        val files = fillDisk()
        val keysBefore = FakeAndroidKeyStore.keys.keys.toSet()

        DataWipe.finishInterrupted(context)

        assertEquals(files, files.filter { it.exists() })
        assertEquals(keysBefore, FakeAndroidKeyStore.keys.keys.toSet())
    }
}
