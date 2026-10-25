package io.cosgriff.glovebox.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.data.testVehicle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupWriterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 5, 4)
    private val password = "made-up password".toCharArray()
    private lateinit var db: GloveboxDatabase
    private lateinit var crypto: PhotoCrypto
    private lateinit var settings: SettingsRepository

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        resetDataStores()
        db = inMemoryDatabase(context)
        crypto = PhotoCrypto(context)
        settings = SettingsRepository(context)
    }

    @After fun tearDown() {
        db.close()
        FakeAndroidKeyStore.uninstall()
    }

    /** A vehicle with two records (one with a receipt photo), a vehicle photo and a 300-byte document. */
    private suspend fun fill() {
        crypto.encryptTo("vehicle.jpg", "made-up vehicle photo".toByteArray().inputStream())
        crypto.encryptTo("receipt.jpg", "made-up receipt".toByteArray().inputStream())
        crypto.encryptTo("document.pdf", ByteArray(300) { 1 }.inputStream())
        val id = db.vehicles().add(testVehicle(photoFile = "vehicle.jpg"), firstReading = 60_000)
        val record = db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = id, date = day, odometer = 60_000, diy = true, shopName = "Made-up Motors"),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Oil change", catalogId = "oil_change")),
        )
        db.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = "receipt.jpg")))
        db.service().insertWithItems(ServiceRecordEntity(vehicleId = id, date = day, odometer = 61_000, diy = true), emptyList())
        db.documents().insert(DocumentEntity(vehicleId = id, type = "TITLE", name = "Title", fileName = "document.pdf", format = "PDF", sizeBytes = 300, addedOn = day))
    }

    private fun entries(file: ByteArray): List<BackupEntry> =
        BackupEntryReader(BackupInputStream(ByteArrayInputStream(file), password)).use { r -> generateSequence { r.next() }.toList() }

    @Test
    fun `what a backup would hold is counted before one is made`() = runBlocking {
        fill()
        assertEquals(BackupContents(vehicles = 1, records = 2, photos = 2, documents = 1, documentBytes = 300), BackupWriter(db, crypto, settings).contents())
    }

    @Test
    fun `an empty phone has nothing to back up`() = runBlocking {
        assertEquals(BackupContents(vehicles = 0, records = 0, photos = 0, documents = 0, documentBytes = 0), BackupWriter(db, crypto, settings).contents())
    }

    @Test
    fun `a backup holds the manifest, every table, the settings and every file in the clear inside its own encryption`() = runBlocking {
        fill()
        settings.setTheme(ThemeMode.DARK)
        settings.setLastBackup(day.minusDays(30))
        val out = ByteArrayOutputStream()
        val progress = mutableListOf<Pair<Int, Int>>()

        BackupWriter(db, crypto, settings).write(out, password, day) { done, total -> progress += done to total }
        val entries = entries(out.toByteArray())

        val manifest = Json.parseToJsonElement(String(entries.first().bytes)).jsonObject
        assertEquals(BackupEntryKind.MANIFEST, entries.first().kind)
        assertEquals("2026-05-04", manifest["createdOn"]!!.jsonPrimitive.content)
        assertEquals(33, manifest["schemaVersion"]!!.jsonPrimitive.int)
        val counts = manifest["counts"]!!.jsonObject
        assertEquals(listOf(1, 2, 2, 1), listOf("vehicles", "records", "photos", "documents").map { counts[it]!!.jsonPrimitive.int })

        val tables = entries.filter { it.kind == BackupEntryKind.TABLE }.associate { it.name to Json.parseToJsonElement(String(it.bytes)).jsonArray }
        assertTrue(tables.keys.containsAll(listOf("vehicles", "service_records", "service_record_items", "photos", "documents", "odometer_readings")))
        assertFalse(tables.keys.any { it.startsWith("sqlite_") || it == "room_master_table" || it == "android_metadata" })
        assertEquals("Made-up Motors", tables.getValue("service_records").first().jsonObject["shopName"]!!.jsonPrimitive.content)

        val prefs = BackupFormat.settingsFromJson(Json.parseToJsonElement(String(entries.single { it.kind == BackupEntryKind.SETTINGS }.bytes)).jsonObject)
        assertEquals("DARK", prefs["theme"])
        // When this phone last made a backup means nothing on another.
        assertFalse("last_backup_on" in prefs)

        val files = entries.filter { it.kind == BackupEntryKind.PHOTO }.associate { it.name to it.bytes }
        assertEquals(setOf("vehicle.jpg", "receipt.jpg", "document.pdf"), files.keys)
        assertArrayEquals("made-up receipt".toByteArray(), files["receipt.jpg"])
        assertEquals(listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3), progress)
    }

    @Test
    fun `a backup whose file can't be read fails rather than being reported as made`() = runBlocking {
        fill()
        val receipt = File(crypto.photoDir, "receipt.jpg")
        receipt.writeBytes(receipt.readBytes().also { it[it.size - 3] = (it[it.size - 3] + 1).toByte() })

        assertThrows(IOException::class.java) { runBlocking { BackupWriter(db, crypto, settings).write(ByteArrayOutputStream(), password, day) } }
        Unit
    }
}
