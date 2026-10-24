package io.cosgriff.glovebox.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlin.properties.ReadOnlyProperty
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavKey
import androidx.room3.Room
import androidx.savedstate.serialization.encodeToSavedState
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.ui.service.RecordActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import androidx.navigation3.runtime.serialization.NavKeySerializer
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.Closeable
import java.time.LocalDate

/**
 * What the settings, export, about and welcome tests share: a real in-memory database (through Android's own
 * driver, as `:data`'s tests build it), the real settings store, and the app's classes built over them the way
 * Hilt builds them. Nothing here is a mock; the encryption key store is the one thing Robolectric lacks, and no
 * test here reaches it.
 */
class TestGraph : Closeable {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: GloveboxDatabase = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    val settings = SettingsRepository(context)
    val crypto = PhotoCrypto(context)
    val photos = RecordPhotoStore(context, crypto, db.service(), db.vehicles(), db.documents())
    val documents = DocumentStore(context, crypto, db.documents(), photos)
    val actions = RecordActions(db.service(), db.recalls(), db.vehicles(), photos)

    /** For the singletons that launch on the application's scope. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Adds a made-up vehicle with a first reading; returns its id. */
    suspend fun vehicle(
        year: Int = 2019,
        make: String = "Toyota",
        model: String = "4Runner",
        firstReading: Int? = 30_000,
        nickname: String? = null,
        distanceUnit: String? = null,
        created: LocalDate = LocalDate.of(2023, 1, 1),
    ): Long {
        val base = VehicleEntity(vin = null, year = year, make = make, model = model, nickname = nickname, createdOn = created)
        return db.vehicles().add(if (distanceUnit != null) base.copy(distanceUnit = distanceUnit) else base, firstReading)
    }

    /**
     * The settings store is one per process, so it outlives a test; every test starts from the defaults.
     * Nothing in the app clears it short of a wipe, so the store is reached the way the app's own delegate
     * holds it.
     */
    suspend fun resetSettings() {
        settingsStore(context).edit { it.clear() }
    }

    override fun close() {
        appScope.cancel()
        db.close()
    }
}

@Suppress("UNCHECKED_CAST")
private fun settingsStore(context: Context): DataStore<Preferences> {
    val field = Class.forName("io.cosgriff.glovebox.data.settings.SettingsRepositoryKt").getDeclaredField("dataStore\$delegate")
    field.isAccessible = true
    val delegate = field.get(null) as ReadOnlyProperty<Context, DataStore<Preferences>>
    return delegate.getValue(context, Context::storeName)
}

/** Any property will do: the delegate goes by the context alone. */
private val Context.storeName: String get() = "settings"

/** A SavedStateHandle as the nav host hands one to a screen's ViewModel: holding the route it was opened with. */
fun routeHandle(route: NavKey): SavedStateHandle =
    SavedStateHandle(mapOf(ROUTE_KEY to encodeToSavedState(NavKeySerializer(), route)))

/** The nav host's key for the route (`Navigation.kt`); a test can't reach the private constant. */
private const val ROUTE_KEY = "io.cosgriff.glovebox.route"

/** ViewModels launch on Main; here that is a test dispatcher, so their work runs as soon as it is started. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Waits, in real time, until [check] holds. The database and the settings store answer on their own threads,
 * so a test's virtual clock can't wait for them.
 */
suspend fun eventually(timeoutMs: Long = 10_000, check: suspend () -> Boolean) = withContext(Dispatchers.Default) {
    withTimeout(timeoutMs) {
        while (!check()) delay(10)
    }
}

/** The first value of a screen's state that satisfies [check], waited for in real time while subscribed. */
suspend fun <T> kotlinx.coroutines.flow.Flow<T>.awaitValue(timeoutMs: Long = 10_000, check: (T) -> Boolean): T = withContext(Dispatchers.Default) {
    withTimeout(timeoutMs) { first(check) }
}
