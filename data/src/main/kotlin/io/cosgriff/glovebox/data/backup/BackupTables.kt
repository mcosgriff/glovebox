package io.cosgriff.glovebox.data.backup

import androidx.room3.PooledConnection

/**
 * The database's own tables, the ones a backup carries and a restore replaces, by name: SQLite's and Room's
 * bookkeeping (`sqlite_…`, `android_metadata`, `room_master_table`) is never in a backup.
 */
internal suspend fun PooledConnection.backedUpTables(): List<String> =
    usePrepared("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name") { s ->
        buildList { while (s.step()) add(s.getText(0)) }
    }.filterNot { it.startsWith("sqlite_") || it == "android_metadata" || it == "room_master_table" }

/** The schema version Room keeps in the database file. */
internal suspend fun PooledConnection.schemaVersion(): Int = usePrepared("PRAGMA user_version") { s -> s.step(); s.getInt(0) }
