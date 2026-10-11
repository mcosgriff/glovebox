package io.cosgriff.glovebox.data.backup

import android.database.Cursor
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.time.LocalDate

/** What a backup would hold, for the screen to show before one is made. */
data class BackupContents(val vehicles: Int, val records: Int, val photos: Int)

/**
 * Gathers everything a backup holds and writes it through [BackupOutputStream]. Tables are read
 * generically from SQLite, so a column added later is backed up without touching this class.
 */
class BackupWriter(
    private val db: GloveboxDatabase,
    private val crypto: PhotoCrypto,
    private val settings: SettingsRepository,
) {
    suspend fun contents(): BackupContents = withContext(Dispatchers.IO) {
        BackupContents(
            vehicles = db.vehicles().all().size,
            records = count("service_records"),
            photos = photoFiles().size,
        )
    }

    /**
     * Writes a whole backup to [out] and closes it. [onProgress] is called with (done, total) as
     * photos go in, which is where the time goes. Throws if a photo can't be read, so a backup
     * that is missing something is never reported as made.
     */
    suspend fun write(out: OutputStream, password: CharArray, today: LocalDate, onProgress: (Int, Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val photos = photoFiles()
        val prefs = settings.raw()
        BackupEntryWriter(BackupOutputStream(out, password)).use { entries ->
            val sqlite = db.openHelper.readableDatabase
            // One read transaction, so the tables agree with each other even if something is saved meanwhile.
            sqlite.beginTransactionNonExclusive()
            try {
                val tables = sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").use { c ->
                    generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList()
                }.filterNot { it.startsWith("sqlite_") || it == "android_metadata" || it == "room_master_table" }

                entries.write(
                    BackupEntryKind.MANIFEST,
                    "",
                    buildJsonObject {
                        put("format", BackupFormat.VERSION)
                        put("createdOn", today.toString())
                        put("schemaVersion", sqlite.version)
                        put(
                            "counts",
                            buildJsonObject {
                                put("vehicles", count("vehicles"))
                                put("records", count("service_records"))
                                put("photos", photos.size)
                            },
                        )
                    }.toString().toByteArray(),
                )
                for (table in tables) {
                    val rows = sqlite.query("SELECT * FROM `$table`").use { c ->
                        JsonArray(generateSequence { if (c.moveToNext()) c.row() else null }.toList())
                    }
                    entries.write(BackupEntryKind.TABLE, table, rows.toString().toByteArray())
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }

            entries.write(
                BackupEntryKind.SETTINGS,
                "",
                JsonObject(
                    prefs.mapValues { (_, v) ->
                        buildJsonObject {
                            when (v) {
                                is Boolean -> { put("type", "boolean"); put("value", v) }
                                is Int -> { put("type", "int"); put("value", v) }
                                is Long -> { put("type", "long"); put("value", v) }
                                is String -> { put("type", "string"); put("value", v) }
                                else -> { put("type", "string"); put("value", v.toString()) }
                            }
                        }
                    },
                ).toString().toByteArray(),
            )

            photos.forEachIndexed { i, name ->
                onProgress(i, photos.size)
                // Stored under this phone's key; written here in the clear, inside the backup's own encryption.
                val plain = try {
                    ByteArrayOutputStream().also { crypto.decrypt(name, it) }.toByteArray()
                } catch (e: GeneralSecurityException) {
                    throw IOException("Photo $name couldn't be decrypted", e)
                }
                entries.write(BackupEntryKind.PHOTO, name, plain)
            }
            onProgress(photos.size, photos.size)
        }
    }

    private suspend fun photoFiles(): List<String> = (db.service().allPhotoFiles() + db.vehicles().allPhotoFiles()).distinct()

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun Cursor.row(): JsonObject = JsonObject(
        (0 until columnCount).associate { i ->
            val value: JsonElement = when (getType(i)) {
                Cursor.FIELD_TYPE_NULL -> JsonNull
                Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(i))
                else -> JsonPrimitive(getString(i))
            }
            getColumnName(i) to value
        },
    )
}
