package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room3.PooledConnection
import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Every schema the app has ever shipped can be upgraded to the current one with its data intact.
 *
 * For each exported version in `data/schemas`, a database is made at that version, one row is put in
 * every table with every column filled, and it is opened as the current database: Room runs each
 * migration in between and checks the result against the current schema. Then every value that was
 * put in is read back.
 *
 * **A phone only ever holds one copy of the user's records, with no cloud to fall back on, so a
 * migration that loses or mangles a column is the worst bug this app can ship.** A new schema
 * version is covered automatically: its file is in the folder.
 *
 * The app opens its database through SQLCipher's driver, whose native library doesn't load on the JVM,
 * so this opens the same database class through Android's own driver (Robolectric's SQLite). The
 * migrations and schema checks are Room's either way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Makes the database [name] at schema [version] and hands over a connection to it, closed after [block]. */
    private fun <R> create(name: String, version: Int, block: (SQLiteConnection) -> R): R =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), context.getDatabasePath(name), AndroidSQLiteDriver(), GloveboxDatabase::class)
            .let { helper -> runBlocking { helper.createDatabase(version) } }.use(block)

    // Gradle runs a module's tests from the module's folder.
    private val exported: List<Int> =
        File("schemas/${GloveboxDatabase::class.java.name}").listFiles { f -> f.extension == "json" }.orEmpty().map { it.nameWithoutExtension.toInt() }.sorted()

    private val current: Int get() = exported.last()

    /** Parents before children, so every foreign key points at a row that is there. */
    private val order = listOf(
        "vehicles", "odometer_readings", "schedule_items", "service_records", "service_record_items",
        "photos", "torque_specs", "recalls", "milestones", "milestone_jobs", "documents", "catalog_rules",
        "recall_names",
    )

    private data class Column(val name: String, val type: String)

    private val tablesSql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name != 'room_master_table'"

    private fun SQLiteConnection.tables(): List<String> = prepare(tablesSql).use { s -> buildList { while (s.step()) add(s.getText(0)) } }

    private fun SQLiteConnection.columns(table: String): List<Column> =
        prepare("PRAGMA table_info(`$table`)").use { s -> buildList { while (s.step()) add(Column(s.getText(1), s.getText(2).uppercase())) } }

    private suspend fun PooledConnection.tables(): List<String> = usePrepared(tablesSql) { s -> buildList { while (s.step()) add(s.getText(0)) } }

    private suspend fun PooledConnection.columns(table: String): List<String> =
        usePrepared("PRAGMA table_info(`$table`)") { s -> buildList { while (s.step()) add(s.getText(1)) } }

    private suspend fun GloveboxDatabase.version(): Int = useReaderConnection { it.usePrepared("PRAGMA user_version") { s -> s.step(); s.getInt(0) } }

    /** One value per column type. Ids and foreign keys are all 1, so the one row of each table hangs together. */
    private fun value(column: Column): Any = when (column.type) {
        "INTEGER" -> 1L
        "REAL" -> 1.5
        else -> "x"
    }

    /** One row in every table, every column filled, nullable ones too. What was written, by table and column. */
    private fun seed(db: SQLiteConnection): Map<String, Map<String, Any>> {
        val present = db.tables()
        assertTrue("a table the test doesn't know how to order: ${present - order.toSet()}", order.containsAll(present))
        return order.filter { it in present }.associateWith { table ->
            val columns = db.columns(table)
            val values = columns.associate { it.name to value(it) }
            db.prepare("INSERT INTO `$table` (${columns.joinToString { "`${it.name}`" }}) VALUES (${columns.joinToString { "?" }})").use { s ->
                columns.forEachIndexed { i, column ->
                    when (val v = values.getValue(column.name)) {
                        is Long -> s.bindLong(i + 1, v)
                        is Double -> s.bindDouble(i + 1, v)
                        else -> s.bindText(i + 1, v.toString())
                    }
                }
                s.step()
            }
            values
        }
    }

    private fun migrated(name: String): GloveboxDatabase =
        Room.databaseBuilder(context, GloveboxDatabase::class.java, name).setDriver(AndroidSQLiteDriver()).build().also {
            // Opening it is what runs the migrations and checks the schema they leave.
            runBlocking { it.useWriterConnection { } }
        }

    @Test
    fun `the schema folder has every version up to the one the app builds`() {
        assertEquals("a version is missing from data/schemas", (1..current).toList(), exported)
        val db = migrated("fresh")
        assertEquals(current, runBlocking { db.version() })
        db.close()
    }

    @Test
    fun `every past version upgrades to the current one and keeps what was in it`() {
        exported.dropLast(1).forEach { from ->
            val name = "from-$from"
            val written = create(name, from) { seed(it) }
            val db = try {
                migrated(name)
            } catch (e: Exception) {
                throw AssertionError("version $from doesn't upgrade to $current: ${e.message}", e)
            }
            runBlocking {
                db.useReaderConnection { now ->
                    val tables = now.tables()
                    written.forEach { (table, values) ->
                        assertTrue("from $from: table $table is gone", table in tables)
                        val kept = now.columns(table).filter { it in values }
                        val rows = now.usePrepared("SELECT ${kept.joinToString { "`$it`" }} FROM `$table`") { s ->
                            buildList {
                                while (s.step()) {
                                    add(
                                        kept.mapIndexed { i, column ->
                                            when (values.getValue(column)) {
                                                is Long -> s.getLong(i)
                                                is Double -> s.getDouble(i)
                                                else -> s.getText(i)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        assertEquals("from $from: rows in $table", 1, rows.size)
                        kept.forEachIndexed { i, column -> assertEquals("from $from: $table.$column", values.getValue(column), rows.single()[i]) }
                    }
                }
            }
            // And it reads as the current app reads it, new columns and all.
            runBlocking {
                val vehicle = db.vehicles().byId(1)
                assertNotNull("from $from: the vehicle", vehicle)
                assertEquals("from $from: schedule items", 1, db.schedule().allFor(1).size)
            }
            db.close()
        }
    }

    @Test
    fun `columns added along the way arrive with the value an old row should have`() {
        create("oldest", exported.first()) { seed(it) }
        val db = migrated("oldest")
        runBlocking {
            val vehicle = db.vehicles().byId(1)!!
            // A vehicle from before units were a choice counts in miles; nothing about it was typed in.
            assertEquals("MILES", vehicle.distanceUnit)
            assertEquals(false, vehicle.enteredByHand)
            assertEquals(null, vehicle.milestonesKnownList)
            assertEquals(null, vehicle.clusterSwappedOn)
            val item = db.schedule().allFor(1).single()
            assertEquals(false, item.severeOnly)
            // Nobody said it runs on E85, and no item had an E85 interval.
            assertEquals(false, item.useE85)
            assertEquals(null, item.e85Miles)
            assertEquals(null, item.e85Months)
            assertEquals(null, item.firstMiles)
            assertEquals(null, item.knownList)
            assertEquals(null, item.service)
        }
        db.close()
    }
}
