package io.cosgriff.glovebox.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.TorqueSpecEntity
import io.cosgriff.glovebox.data.db.VehicleRowCounts
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

    /** A vehicle with one of every row Delete vehicle counts, three records' photos, its own photo and a document, all made up. */
    private suspend fun fullVehicle(tag: String, make: String): Long {
        val id = db.vehicles().add(testVehicle(make = make, photoFile = photo("vehicle-$tag.jpg")), firstReading = 60_000)
        record(id, imported = false, photo("receipt-$tag.jpg"), photo("parts-$tag.jpg"))
        record(id, imported = true, photo("imported-$tag.jpg"))
        db.schedule().insertAll(listOf(ScheduleItemEntity(vehicleId = id, name = "Oil change", intervalMiles = 5_000, intervalMonths = 6)))
        db.torque().insert(TorqueSpecEntity(vehicleId = id, name = "Lug nuts", category = "WHEELS"))
        db.recalls().insertAll(listOf(RecallEntity(vehicleId = id, campaign = "26V00000$tag", component = "BRAKES", summary = "Made up.", firstSeenOn = day)))
        db.documents().insert(DocumentEntity(vehicleId = id, type = "TITLE", name = "Title", fileName = photo("document-$tag.pdf"), format = "PDF", sizeBytes = 1, addedOn = day))
        return id
    }

    private fun onDisk() = photos.photoDir.list().orEmpty().toSet()

    @Test
    fun `deleting a vehicle takes what it said would go, every file of it, and nothing of the other vehicle's`() = runBlocking {
        val gone = fullVehicle("1", make = "Toyota")
        val kept = fullVehicle("2", make = "Volkswagen")
        val wipe = DataWipe(context, db, keys, photos)
        val keptBefore = wipe.whatGoes(kept)!!
        val said = wipe.whatGoes(gone)!!

        assertEquals(VehicleRowCounts(records = 2, photos = 3, scheduleItems = 1, torqueSpecs = 1, recalls = 1, mileageReadings = 1, documents = 1), said.rows)
        assertEquals(setOf("vehicle-1.jpg", "receipt-1.jpg", "parts-1.jpg", "imported-1.jpg", "document-1.pdf"), said.files.toSet())

        assertEquals(said, wipe.deleteVehicle(gone))

        assertNull(db.vehicles().byId(gone))
        assertEquals(VehicleRowCounts(0, 0, 0, 0, 0, 0, 0), db.vehicles().rowCounts(gone))
        // No orphan: every file left is one a row still points at, and they are all the other vehicle's.
        assertEquals(db.documents().allEncryptedFiles().toSet(), onDisk())
        assertEquals(keptBefore.files.toSet(), onDisk())
        assertEquals(keptBefore, wipe.whatGoes(kept))
    }

    @Test
    fun `a vehicle already gone has nothing to go`() = runBlocking {
        val wipe = DataWipe(context, db, keys, photos)
        assertNull(wipe.whatGoes(404))
        assertNull(wipe.deleteVehicle(404))
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
