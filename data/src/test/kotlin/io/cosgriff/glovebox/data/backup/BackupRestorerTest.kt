package io.cosgriff.glovebox.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobEntity
import io.cosgriff.glovebox.data.db.ServiceRecordJobPartEntity
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.data.settings.NoticeLogStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.NoticeLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupRestorerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)
    private val password = "made-up password".toCharArray()
    private val quick = BackupKdf(memoryKiB = 64, iterations = 1, parallelism = 1)

    /** The phone the backup was made on, and the one it is restored to; they share the files folder here. */
    private lateinit var source: GloveboxDatabase
    private lateinit var target: GloveboxDatabase
    private lateinit var crypto: PhotoCrypto
    private lateinit var settings: SettingsRepository
    private lateinit var noticeLog: NoticeLogStore

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        resetDataStores()
        source = inMemoryDatabase(context)
        target = inMemoryDatabase(context)
        crypto = PhotoCrypto(context)
        settings = SettingsRepository(context)
        noticeLog = NoticeLogStore(context)
    }

    @After fun tearDown() {
        source.close()
        target.close()
        FakeAndroidKeyStore.uninstall()
    }

    private val restorer get() = BackupRestorer(target, crypto, settings, noticeLog)

    private fun photo(name: String, text: String = "made-up $name") = name.also { crypto.encryptTo(it, text.toByteArray().inputStream()) }

    /** A backup of [source] holding a 4Runner with its owner's oil change interval, a photographed, priced and itemized record, a recall marked done and a recall name ticked. */
    private suspend fun backupOfSource(): File {
        val id = source.vehicles().add(testVehicle(photoFile = photo("vehicle-a.jpg")), firstReading = 60_000)
        source.vehicles().setOwnOil(id, miles = 7_500, months = 9)
        val record = source.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = day, odometer = 60_000, diy = false, shopName = "Made-up Motors", costCents = 8_950, shopSuppliesCents = 450, taxCents = 600),
            listOf(ServiceRecordItemEntity(id = 1, recordId = 0, scheduleItemId = null, label = "Oil change", catalogId = "oil_change", jobId = 1)),
            listOf(ServiceRecordJobEntity(id = 1, recordId = 0, wording = "Made-up oil service", laborCents = 3_000)),
            listOf(ServiceRecordJobPartEntity(jobId = 1, wording = "Made-up oil", quantityMilli = 5_700, totalCents = 4_500, itemId = 1)),
        )
        source.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = photo("receipt-a.jpg", "the receipt"))))
        source.recalls().insertAll(listOf(RecallEntity(vehicleId = id, campaign = "99V000000", component = "Made-up", summary = "Made-up", firstSeenOn = day, completedOn = day, serviceRecordId = record, doNotDrive = true, parkOutside = true, overTheAir = true)))
        source.recalls().setNames(id, listOf("4RUNNER MADE-UP"))
        return File(context.cacheDir, "backup.glovebox").also { f ->
            f.outputStream().use { BackupWriter(source, crypto, settings).write(it, password, day) }
        }
    }

    /** What is on the phone before a restore: a different vehicle with a photo of its own. */
    private suspend fun fillTarget() {
        target.vehicles().add(testVehicle(make = "Volkswagen", model = "GTI", photoFile = photo("vehicle-b.jpg")), firstReading = 10_000)
    }

    private suspend fun targetState() = Triple(target.vehicles().all(), target.service().recordsFor(target.vehicles().all().first().id), crypto.photoDir.list()!!.sorted())

    /** A file written the way BackupWriter writes one, with whatever entries a test needs. */
    private fun craft(vararg entries: Triple<BackupEntryKind, String, String>, schemaVersion: Int = 39): File {
        val file = File(context.cacheDir, "crafted.glovebox")
        file.outputStream().use { out ->
            BackupEntryWriter(BackupOutputStream(out, password, quick)).use { w ->
                w.write(BackupEntryKind.MANIFEST, "", """{"format":1,"createdOn":"2026-05-04","schemaVersion":$schemaVersion,"counts":{"vehicles":1,"records":0,"photos":1}}""".toByteArray())
                entries.forEach { (kind, name, body) -> w.write(kind, name, body.toByteArray()) }
            }
        }
        return file
    }

    private suspend fun reasonOf(file: File, pass: CharArray = password): BackupException.Reason {
        try {
            restorer.restore(file, pass)
        } catch (e: BackupException) {
            return e.reason
        }
        fail("expected a BackupException")
        throw AssertionError()
    }

    @Test
    fun `a restore replaces the database, the files and the settings with the backup's`() = runBlocking {
        settings.setTheme(ThemeMode.DARK)
        val file = backupOfSource()
        fillTarget()
        settings.setTheme(ThemeMode.LIGHT)
        settings.setBiometricLock(false)
        noticeLog.write(NoticeLog(dueAnnounced = mapOf(1L to "cycle")))
        val progress = mutableListOf<Pair<Int, Int>>()

        val summary = restorer.restore(file, password) { done, total -> progress += done to total }

        assertEquals(BackupSummary(createdOn = day, schemaVersion = 39, vehicles = 1, records = 1, photos = 2, documents = 0), summary)
        assertEquals(source.vehicles().all(), target.vehicles().all())
        val id = target.vehicles().all().single().id
        // The owner's own oil change interval (#517) comes back with the vehicle.
        assertEquals(7_500 to 9, target.vehicles().all().single().let { it.ownOilMiles to it.ownOilMonths })
        assertEquals(source.service().recordsFor(id), target.service().recordsFor(id))
        // The job and its line still point at each other.
        val restoredRecord = target.service().recordsFor(id).single()
        assertEquals(restoredRecord.jobs.single().id, restoredRecord.items.single().jobId)
        // And its part row still points at both.
        assertEquals(restoredRecord.jobs.single().id, restoredRecord.parts.single().jobId)
        assertEquals(restoredRecord.items.single().id, restoredRecord.parts.single().itemId)
        assertEquals(source.recalls().recallsFor(id), target.recalls().recallsFor(id))
        assertEquals(listOf("4RUNNER MADE-UP"), target.recalls().namesFor(id))
        assertEquals(listOf(60_000), target.odometer().readingsFor(id).map { it.odometer })

        // The replaced data's photo is gone; the backup's are in place and readable.
        assertEquals(listOf("receipt-a.jpg", "vehicle-a.jpg"), crypto.photoDir.list()!!.sorted())
        assertArrayEquals("the receipt".toByteArray(), crypto.decryptBytes("receipt-a.jpg"))
        assertFalse(crypto.restoreDir.exists())
        assertEquals(listOf(1 to 2, 2 to 2), progress)

        val restored = settings.settings.first()
        assertEquals(ThemeMode.DARK, restored.theme)
        // This phone's lock is its own.
        assertFalse(restored.biometricLock)
        assertEquals(NoticeLog(), noticeLog.read())
    }

    @Test
    fun `inspecting a backup reads its summary and changes nothing`() = runBlocking {
        val file = backupOfSource()
        fillTarget()
        val before = targetState()

        val summary = restorer.inspect(file, password)

        assertEquals(1, summary.vehicles)
        assertEquals(day, summary.createdOn)
        assertEquals(before, targetState())
        assertTrue(restorer.hasData())
    }

    @Test
    fun `a phone with no vehicles has no data to replace`() = runBlocking {
        assertFalse(restorer.hasData())
    }

    @Test
    fun `the wrong password changes nothing`() = runBlocking {
        val file = backupOfSource()
        fillTarget()
        val before = targetState()

        assertEquals(BackupException.Reason.WRONG_PASSWORD, reasonOf(file, "not it".toCharArray()))
        assertEquals(before, targetState())
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `a file cut short changes nothing, even after its photos were read`() = runBlocking {
        val file = backupOfSource()
        fillTarget()
        val before = targetState()
        val cut = File(context.cacheDir, "cut.glovebox").apply { writeBytes(file.readBytes().copyOfRange(0, file.length().toInt() - 20)) }

        assertEquals(BackupException.Reason.INCOMPLETE, reasonOf(cut))
        assertEquals(before, targetState())
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `a backup from a newer version of the database is refused before anything is staged`() = runBlocking {
        fillTarget()
        val before = targetState()
        val file = craft(Triple(BackupEntryKind.PHOTO, "receipt.jpg", "made-up"), schemaVersion = 99)

        assertEquals(BackupException.Reason.NEWER_VERSION, reasonOf(file))
        assertEquals(before, targetState())
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `a file name is only ever a name, never a path`() = runBlocking {
        fillTarget()
        val before = targetState()
        val file = craft(Triple(BackupEntryKind.PHOTO, "../../shared_prefs/escaped.xml", "made-up"))

        assertEquals(BackupException.Reason.DAMAGED, reasonOf(file))
        assertEquals(before, targetState())
        assertFalse(File(context.filesDir.parentFile, "shared_prefs/escaped.xml").exists())
    }

    @Test
    fun `a row naming a file outside the photos is damage`() = runBlocking {
        fillTarget()
        val before = targetState()
        val rows = listOf(
            "vehicles" to """[{"id":7,"year":2013,"make":"Volkswagen","model":"GTI","createdOn":19000,"odometerOffset":0,"photoFile":"../../no_backup/db.key"}]""",
            "photos" to """[{"id":1,"recordId":1,"fileName":"../../shared_prefs/glovebox_photo_keys.xml"}]""",
            "documents" to """[{"id":1,"vehicleId":7,"type":"OTHER","name":"Made-up","fileName":"../photos-restore/x","format":"PDF","sizeBytes":1,"addedOn":19000}]""",
        )
        for ((table, body) in rows) {
            assertEquals(table, BackupException.Reason.DAMAGED, reasonOf(craft(Triple(BackupEntryKind.TABLE, table, body))))
        }
        assertEquals(before, targetState())
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `a table that isn't a list of rows is damage`() = runBlocking {
        fillTarget()
        val before = targetState()

        assertEquals(BackupException.Reason.DAMAGED, reasonOf(craft(Triple(BackupEntryKind.TABLE, "vehicles", "not json"))))
        assertEquals(BackupException.Reason.DAMAGED, reasonOf(craft(Triple(BackupEntryKind.TABLE, "vehicles", """{"a":1}"""))))
        assertEquals(before, targetState())
    }

    @Test
    fun `a file without a manifest first, or one without a schema version, is damage`() = runBlocking {
        val noManifest = File(context.cacheDir, "no-manifest.glovebox").also { f ->
            f.outputStream().use { out -> BackupEntryWriter(BackupOutputStream(out, password, quick)).use { it.write(BackupEntryKind.TABLE, "vehicles", "[]".toByteArray()) } }
        }
        assertEquals(BackupException.Reason.DAMAGED, reasonOf(noManifest))

        val noVersion = File(context.cacheDir, "no-version.glovebox").also { f ->
            f.outputStream().use { out -> BackupEntryWriter(BackupOutputStream(out, password, quick)).use { it.write(BackupEntryKind.MANIFEST, "", """{"format":1}""".toByteArray()) } }
        }
        assertEquals(BackupException.Reason.DAMAGED, reasonOf(noVersion))
    }

    @Test
    fun `rows from an older version take the defaults of newer columns, and unknown tables and columns are passed over`() = runBlocking {
        fillTarget()
        val file = craft(
            Triple(
                BackupEntryKind.TABLE,
                "vehicles",
                """[{"id":7,"vin":null,"year":2013,"make":"Volkswagen","model":"GTI","createdOn":19000,"odometerOffset":0,"columnFromTheFuture":"x"},{"notAColumn":1}]""",
            ),
            Triple(BackupEntryKind.TABLE, "table_from_the_future", """[{"id":1}]"""),
            Triple(BackupEntryKind.SETTINGS, "", """{"theme":{"type":"string","value":"DARK"}}"""),
        )

        restorer.restore(file, password)

        val vehicle = target.vehicles().all().single()
        assertEquals(7L, vehicle.id)
        assertEquals("GTI", vehicle.model)
        assertEquals(LocalDate.ofEpochDay(19000), vehicle.createdOn)
        assertEquals("MILES", vehicle.distanceUnit)
        assertFalse(vehicle.enteredByHand)
        assertNull(vehicle.photoFile)
        assertEquals(ThemeMode.DARK, settings.settings.first().theme)
        // Nothing in the backup is a file the database points at, so the old ones stay as they were.
        assertEquals(listOf("vehicle-b.jpg"), crypto.photoDir.list()!!.toList())
    }

    @Test
    fun `a restore cut off after the database was replaced finishes moving the photos in on the next launch`() = runBlocking {
        target.vehicles().add(testVehicle(photoFile = "vehicle-a.jpg"), firstReading = null)
        photo("old.jpg")
        crypto.restoreDir.mkdirs()
        crypto.encryptTo(File(crypto.restoreDir, "vehicle-a.jpg"), "vehicle-a.jpg", "the restored photo".toByteArray().inputStream())
        crypto.encryptTo(File(crypto.restoreDir, "stray.jpg"), "stray.jpg", "nothing points here".toByteArray().inputStream())

        restorer.finishInterrupted()

        assertEquals(listOf("vehicle-a.jpg"), crypto.photoDir.list()!!.toList())
        assertArrayEquals("the restored photo".toByteArray(), crypto.decryptBytes("vehicle-a.jpg"))
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `staged photos from a restore that never replaced the database are thrown away`() = runBlocking {
        fillTarget()
        crypto.restoreDir.mkdirs()
        crypto.encryptTo(File(crypto.restoreDir, "vehicle-a.jpg"), "vehicle-a.jpg", "from the abandoned restore".toByteArray().inputStream())

        restorer.finishInterrupted()

        assertEquals(listOf("vehicle-b.jpg"), crypto.photoDir.list()!!.toList())
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `a restore cut off before the database was replaced leaves this phone's photos alone`() = runBlocking {
        // A backup made earlier on this phone shares file names with the photos still here.
        target.vehicles().add(testVehicle(photoFile = photo("vehicle-a.jpg", "this phone's photo")), firstReading = null)
        target.vehicles().add(testVehicle(make = "Volkswagen", model = "GTI", photoFile = photo("vehicle-c.jpg", "added since the backup")), firstReading = null)
        crypto.restoreDir.mkdirs()
        crypto.encryptTo(File(crypto.restoreDir, "vehicle-a.jpg"), "vehicle-a.jpg", "this phone's photo".toByteArray().inputStream())

        restorer.finishInterrupted()

        assertEquals(listOf("vehicle-a.jpg", "vehicle-c.jpg"), crypto.photoDir.list()!!.sorted())
        assertArrayEquals("added since the backup".toByteArray(), crypto.decryptBytes("vehicle-c.jpg"))
        assertFalse(crypto.restoreDir.exists())
    }

    @Test
    fun `an ordinary launch has nothing to finish`() = runBlocking {
        fillTarget()
        restorer.finishInterrupted()
        assertEquals(listOf("vehicle-b.jpg"), crypto.photoDir.list()!!.toList())
    }
}
