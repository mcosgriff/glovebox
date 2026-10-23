package io.cosgriff.glovebox.data.backup

import androidx.room3.PooledConnection
import androidx.room3.deferredTransaction
import androidx.room3.useReaderConnection
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteStatement
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

/** What a backup would hold, for the screen to show before one is made. [documentBytes] is before encryption. */
data class BackupContents(val vehicles: Int, val records: Int, val photos: Int, val documents: Int = 0, val documentBytes: Long = 0)

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
        val documents = db.documents().allFiles().size
        val (records, documentBytes) = db.useReaderConnection { connection ->
            connection.count("service_records") to
                connection.usePrepared("SELECT COALESCE(SUM(sizeBytes), 0) FROM documents") { if (it.step()) it.getLong(0) else 0L }
        }
        BackupContents(
            vehicles = db.vehicles().all().size,
            records = records,
            photos = photoFiles().size - documents,
            documents = documents,
            documentBytes = documentBytes,
        )
    }

    /**
     * Writes a whole backup to [out] and closes it. [onProgress] is called with (done, total) as
     * photos go in, which is where the time goes. Throws if a photo can't be read, so a backup
     * that is missing something is never reported as made.
     */
    suspend fun write(out: OutputStream, password: CharArray, today: LocalDate, onProgress: (Int, Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val photos = photoFiles()
        val documents = db.documents().allFiles().size
        val prefs = settings.raw()
        BackupEntryWriter(BackupOutputStream(out, password)).use { entries ->
            // One read transaction, so the tables agree with each other even if something is saved meanwhile.
            db.useReaderConnection { connection ->
                connection.deferredTransaction {
                    val tables = usePrepared("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name") { s ->
                        buildList { while (s.step()) add(s.getText(0)) }
                    }.filterNot { it.startsWith("sqlite_") || it == "android_metadata" || it == "room_master_table" }
                    val vehicles = count("vehicles")
                    val records = count("service_records")
                    val schemaVersion = usePrepared("PRAGMA user_version") { s -> s.step(); s.getInt(0) }

                    entries.write(
                        BackupEntryKind.MANIFEST,
                        "",
                        buildJsonObject {
                            put("format", BackupFormat.VERSION)
                            put("createdOn", today.toString())
                            put("schemaVersion", schemaVersion)
                            put(
                                "counts",
                                buildJsonObject {
                                    put("vehicles", vehicles)
                                    put("records", records)
                                    put("photos", photos.size - documents)
                                    put("documents", documents)
                                },
                            )
                        }.toString().toByteArray(),
                    )
                    for (table in tables) {
                        val rows = usePrepared("SELECT * FROM `$table`") { s -> JsonArray(buildList { while (s.step()) add(s.row()) }) }
                        entries.write(BackupEntryKind.TABLE, table, rows.toString().toByteArray())
                    }
                }
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

    /** Every encrypted file the database points at: photos, and documents, which a backup carries the same way. */
    private suspend fun photoFiles(): List<String> = db.documents().allEncryptedFiles()

    private suspend fun PooledConnection.count(table: String): Int =
        usePrepared("SELECT COUNT(*) FROM `$table`") { if (it.step()) it.getInt(0) else 0 }

    private fun SQLiteStatement.row(): JsonObject = JsonObject(
        (0 until getColumnCount()).associate { i ->
            val value: JsonElement = when (getColumnType(i)) {
                SQLITE_DATA_NULL -> JsonNull
                SQLITE_DATA_INTEGER -> JsonPrimitive(getLong(i))
                SQLITE_DATA_FLOAT -> JsonPrimitive(getDouble(i))
                else -> JsonPrimitive(getText(i))
            }
            getColumnName(i) to value
        },
    )
}
