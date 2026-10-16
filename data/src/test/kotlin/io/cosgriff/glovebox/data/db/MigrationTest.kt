package io.cosgriff.glovebox.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
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
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), GloveboxDatabase::class.java)

    // Gradle runs a module's tests from the module's folder.
    private val exported: List<Int> =
        File("schemas/${GloveboxDatabase::class.java.name}").listFiles { f -> f.extension == "json" }.orEmpty().map { it.nameWithoutExtension.toInt() }.sorted()

    private val current: Int get() = exported.last()

    /** Parents before children, so every foreign key points at a row that is there. */
    private val order = listOf(
        "vehicles", "odometer_readings", "schedule_items", "service_records", "service_record_items",
        "photos", "torque_specs", "recalls", "milestones", "milestone_jobs", "documents",
    )

    private data class Column(val name: String, val type: String)

    private fun SupportSQLiteDatabase.tables(): List<String> =
        query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name != 'room_master_table'").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    private fun SupportSQLiteDatabase.columns(table: String): List<Column> =
        query("PRAGMA table_info(`$table`)").use { c ->
            buildList { while (c.moveToNext()) add(Column(c.getString(1), c.getString(2).uppercase())) }
        }

    /** One value per column type. Ids and foreign keys are all 1, so the one row of each table hangs together. */
    private fun value(column: Column): Any = when (column.type) {
        "INTEGER" -> 1L
        "REAL" -> 1.5
        else -> "x"
    }

    /** One row in every table, every column filled, nullable ones too. What was written, by table and column. */
    private fun seed(db: SupportSQLiteDatabase): Map<String, Map<String, Any>> {
        val present = db.tables()
        assertTrue("a table the test doesn't know how to order: ${present - order.toSet()}", order.containsAll(present))
        return order.filter { it in present }.associateWith { table ->
            val columns = db.columns(table)
            val values = columns.associate { it.name to value(it) }
            db.execSQL(
                "INSERT INTO `$table` (${columns.joinToString { "`${it.name}`" }}) VALUES (${columns.joinToString { "?" }})",
                columns.map { values.getValue(it.name) }.toTypedArray(),
            )
            values
        }
    }

    private fun migrated(name: String): GloveboxDatabase =
        Room.databaseBuilder(ApplicationProvider.getApplicationContext(), GloveboxDatabase::class.java, name).build().also {
            // Opening it is what runs the migrations and checks the schema they leave.
            it.openHelper.writableDatabase
        }

    @Test
    fun `the schema folder has every version up to the one the app builds`() {
        assertEquals("a version is missing from data/schemas", (1..current).toList(), exported)
        val db = migrated("fresh")
        assertEquals(current, db.openHelper.readableDatabase.version)
        db.close()
    }

    @Test
    fun `every past version upgrades to the current one and keeps what was in it`() {
        exported.dropLast(1).forEach { from ->
            val name = "from-$from"
            val written = helper.createDatabase(name, from).use { seed(it) }
            val db = try {
                migrated(name)
            } catch (e: Exception) {
                throw AssertionError("version $from doesn't upgrade to $current: ${e.message}", e)
            }
            val now = db.openHelper.readableDatabase
            written.forEach { (table, values) ->
                assertTrue("from $from: table $table is gone", table in now.tables())
                val kept = now.columns(table).map { it.name }.filter { it in values }
                now.query("SELECT ${kept.joinToString { "`$it`" }} FROM `$table`").use { c ->
                    assertEquals("from $from: rows in $table", 1, c.count)
                    c.moveToFirst()
                    kept.forEachIndexed { i, column ->
                        val expected = values.getValue(column)
                        val actual: Any = when (expected) {
                            is Long -> c.getLong(i)
                            is Double -> c.getDouble(i)
                            else -> c.getString(i)
                        }
                        assertEquals("from $from: $table.$column", expected, actual)
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
        helper.createDatabase("oldest", exported.first()).use { seed(it) }
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
            assertEquals(null, item.firstMiles)
            assertEquals(null, item.knownList)
            assertEquals(null, item.service)
        }
        db.close()
    }
}
