package io.cosgriff.glovebox.data.backup

import androidx.room3.PooledConnection
import androidx.room3.executeSQL
import androidx.room3.immediateTransaction
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.settings.NoticeLogStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.NoticeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.time.LocalDate

/** What a backup says it holds, read from its manifest once the password has worked. */
data class BackupSummary(val createdOn: LocalDate?, val schemaVersion: Int, val vehicles: Int, val records: Int, val photos: Int, val documents: Int = 0)

/**
 * Puts a backup's contents on this phone, replacing what is there.
 *
 * It is all or nothing. The whole file is read and checked first; the database is then replaced in
 * one transaction; and the photos, which were staged in [PhotoCrypto.restoreDir] under this phone's
 * key while the file was being read, are moved into place afterwards. If the app dies between those
 * last two steps, [finishInterrupted] completes the move on the next launch.
 */
class BackupRestorer(
    private val db: GloveboxDatabase,
    private val crypto: PhotoCrypto,
    private val settings: SettingsRepository,
    private val noticeLog: NoticeLogStore,
) {
    /** Opens [file] far enough to prove the password and read what it holds. Throws [BackupException]. */
    suspend fun inspect(file: File, password: CharArray): BackupSummary = withContext(Dispatchers.IO) {
        BackupEntryReader(BackupInputStream(file.inputStream().buffered(), password)).use { entries -> summary(entries.next()) }
    }

    suspend fun hasData(): Boolean = withContext(Dispatchers.IO) { db.vehicles().all().isNotEmpty() }

    /**
     * Replaces everything on this phone with what [file] holds. Throws [BackupException] and
     * changes nothing if the file can't be read to its end.
     */
    suspend fun restore(file: File, password: CharArray, onProgress: (Int, Int) -> Unit = { _, _ -> }): BackupSummary = withContext(Dispatchers.IO) {
        val staging = crypto.restoreDir
        staging.deleteRecursively()
        staging.mkdirs()
        var prefs: JsonObject? = null
        val summary: BackupSummary
        try {
            val tables = LinkedHashMap<String, JsonArray>()
            var staged = 0
            BackupEntryReader(BackupInputStream(file.inputStream().buffered(), password)).use { entries ->
                summary = summary(entries.next())
                // Tables written by a newer version of the app may have columns this one would silently drop.
                if (summary.schemaVersion > schemaVersion()) throw BackupException(BackupException.Reason.NEWER_VERSION)
                while (true) {
                    val entry = entries.next() ?: break
                    when (entry.kind) {
                        BackupEntryKind.TABLE -> tables[entry.name] = json(entry.bytes).jsonArray
                        BackupEntryKind.SETTINGS -> prefs = json(entry.bytes).jsonObject
                        BackupEntryKind.PHOTO -> {
                            // A name from the file is only ever used as a name, never as a path.
                            val name = File(entry.name).name
                            if (name != entry.name || name.isEmpty()) throw BackupException(BackupException.Reason.DAMAGED)
                            crypto.encryptTo(File(staging, name), name, entry.bytes.inputStream())
                            onProgress(++staged, summary.photos + summary.documents)
                        }
                        BackupEntryKind.MANIFEST -> Unit
                    }
                }
            }
            // Reaching here means every block was genuine and the file was complete. Only now is anything replaced.
            replaceTables(tables)
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw when (e) {
                is BackupException -> e
                is SerializationException, is IllegalArgumentException, is IllegalStateException -> BackupException(BackupException.Reason.DAMAGED, e)
                else -> e
            }
        }
        movePhotosIn()
        prefs?.let { settings.restore(BackupFormat.settingsFromJson(it)) }
        // Ids mean different things now; what was announced about the old data says nothing about the new.
        noticeLog.write(NoticeLog())
        summary
    }

    /**
     * Run on every launch, before the orphan sweep. If a restore replaced the database but died
     * before its photos were moved in, this finishes the job; staged photos the database doesn't
     * point at belong to a restore that never got that far and are removed.
     */
    suspend fun finishInterrupted() = withContext(Dispatchers.IO) {
        if (crypto.restoreDir.exists()) movePhotosIn()
    }

    private suspend fun movePhotosIn() {
        val staging = crypto.restoreDir
        val wanted = db.documents().allEncryptedFiles().toSet()
        val staged = staging.listFiles().orEmpty()
        if (staged.any { it.name in wanted }) {
            // The old photos belonged to the data that was replaced.
            crypto.photoDir.listFiles()?.forEach { it.delete() }
            staged.filter { it.name in wanted }.forEach { f ->
                val target = File(crypto.photoDir, f.name)
                if (!f.renameTo(target)) {
                    f.copyTo(target, overwrite = true)
                    f.delete()
                }
            }
        }
        staging.deleteRecursively()
    }

    private suspend fun schemaVersion(): Int = db.useReaderConnection { it.schemaVersion() }

    private suspend fun replaceTables(tables: Map<String, JsonArray>) {
        // Room's own write connection, so screens watching these tables refresh once it is done.
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                // Rows go in table by table, children sometimes before parents; the links are checked at commit.
                executeSQL("PRAGMA defer_foreign_keys = ON")
                val current = backedUpTables()
                current.forEach { executeSQL("DELETE FROM `$it`") }
                for ((table, rows) in tables) {
                    if (table !in current) continue
                    val columns = columns(table)
                    for (row in rows) {
                        // A backup from an older version lacks newer columns; those take their defaults.
                        val values = row.jsonObject.filterKeys { it in columns }
                        if (values.isEmpty()) continue
                        val names = values.keys.joinToString(", ") { "`$it`" }
                        val marks = values.keys.joinToString(", ") { "?" }
                        usePrepared("INSERT INTO `$table` ($names) VALUES ($marks)") { s ->
                            values.values.forEachIndexed { i, value ->
                                when (val v = value.sqlValue()) {
                                    null -> s.bindNull(i + 1)
                                    is Long -> s.bindLong(i + 1, v)
                                    is Double -> s.bindDouble(i + 1, v)
                                    else -> s.bindText(i + 1, v.toString())
                                }
                            }
                            s.step()
                        }
                    }
                }
            }
        }
    }

    private suspend fun PooledConnection.columns(table: String): Set<String> = usePrepared("PRAGMA table_info(`$table`)") { s ->
        val name = (0 until s.getColumnCount()).first { s.getColumnName(it) == "name" }
        buildSet { while (s.step()) add(s.getText(name)) }
    }

    private fun kotlinx.serialization.json.JsonElement.sqlValue(): Any? {
        if (this is JsonNull) return null
        val p = jsonPrimitive
        return if (p.isString) p.content else p.longOrNull ?: p.doubleOrNull ?: p.content
    }

    private fun summary(first: BackupEntry?): BackupSummary {
        if (first == null || first.kind != BackupEntryKind.MANIFEST) throw BackupException(BackupException.Reason.DAMAGED)
        val manifest = try {
            json(first.bytes).jsonObject
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupException.Reason.DAMAGED, e)
        }
        fun int(o: JsonObject?, key: String) = (o?.get(key) as? JsonPrimitive)?.intOrNull
        val counts = manifest["counts"] as? JsonObject
        return BackupSummary(
            createdOn = (manifest["createdOn"] as? JsonPrimitive)?.content?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            schemaVersion = int(manifest, "schemaVersion") ?: throw BackupException(BackupException.Reason.DAMAGED),
            vehicles = int(counts, "vehicles") ?: 0,
            records = int(counts, "records") ?: 0,
            photos = int(counts, "photos") ?: 0,
            documents = int(counts, "documents") ?: 0,
        )
    }

    private fun json(bytes: ByteArray) = try {
        Json.parseToJsonElement(String(bytes))
    } catch (e: SerializationException) {
        throw BackupException(BackupException.Reason.DAMAGED, e)
    }
}
