package io.cosgriff.glovebox.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.VehicleEntity
import java.time.LocalDate

/** The app's database in memory, through Android's own driver (SQLCipher's native library isn't on the JVM; see MigrationTest). */
fun inMemoryDatabase(context: Context): GloveboxDatabase =
    Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()

/** A made-up vehicle; no VIN unless one is given. */
fun testVehicle(
    make: String = "Toyota",
    model: String = "4Runner",
    year: Int = 2019,
    createdOn: LocalDate = LocalDate.of(2026, 5, 4),
    vin: String? = null,
    photoFile: String? = null,
) = VehicleEntity(vin = vin, year = year, make = make, model = model, createdOn = createdOn, photoFile = photoFile)

/**
 * Forgets the DataStores the `preferencesDataStore` delegates hold. They are process-wide singletons, and
 * Robolectric runs every test in one process with a new files folder each time, so without this a test
 * would read the previous test's settings from memory.
 */
fun resetDataStores() {
    listOf(
        "io.cosgriff.glovebox.data.settings.SettingsRepositoryKt" to "dataStore\$delegate",
        "io.cosgriff.glovebox.data.settings.NoticeLogStoreKt" to "noticeLog\$delegate",
    ).forEach { (owner, field) ->
        val delegate = Class.forName(owner).getDeclaredField(field).apply { isAccessible = true }.get(null)
        delegate.javaClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(delegate, null)
    }
}
