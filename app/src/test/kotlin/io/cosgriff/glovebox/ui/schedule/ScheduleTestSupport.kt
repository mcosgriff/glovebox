package io.cosgriff.glovebox.ui.schedule

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import io.cosgriff.glovebox.data.db.ServiceLogWriter
import io.cosgriff.glovebox.data.settings.ThemeMode
import io.cosgriff.glovebox.ui.theme.GloveboxTheme
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.room3.Room
import androidx.savedstate.serialization.encodeToSavedState
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.OdometerReadingEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.ui.export.ExportSaver
import io.cosgriff.glovebox.ui.service.RecordActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import kotlin.reflect.KClass

/** Must match the private key in `Navigation.kt` that `SavedStateHandle.route()` reads. */
private const val ROUTE = "io.cosgriff.glovebox.route"

/** A `SavedStateHandle` as the nav host hands a ViewModel opened with [key]. */
fun routeHandle(key: NavKey): SavedStateHandle =
    SavedStateHandle(mapOf(ROUTE to encodeToSavedState(NavKeySerializer(), key)))

/**
 * Everything the schedule and reminders ViewModels are built from, over a fresh in-memory database, as
 * `:data`'s DAO tests build theirs (Android's own driver). Made-up vehicles only.
 */
class ScheduleWorld {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: GloveboxDatabase = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    val settings = SettingsRepository(context)
    val catalog = KnownScheduleCatalog(context)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val saver = ExportSaver(context, appScope)
    val actions by lazy {
        val crypto = PhotoCrypto(context)
        RecordActions(db.service(), db.recalls(), ServiceLogWriter(db), RecordPhotoStore(context, crypto, db.service(), db.vehicles(), db.documents()))
    }

    private val store = ViewModelStore()
    private val holders = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** A ViewModel cleared with the world, so nothing it started outlives the database. */
    fun <T : ViewModel> viewModel(type: KClass<T>, make: () -> T): T =
        ViewModelProvider.create(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <V : ViewModel> create(modelClass: KClass<V>, extras: CreationExtras): V = make() as V
            },
        )[type]

    /** Keeps [flow] subscribed, as a screen would, so its `WhileSubscribed` upstream runs. */
    fun <T> keep(flow: StateFlow<T>): StateFlow<T> {
        holders.launch { flow.collect {} }
        return flow
    }

    fun vehicle(entity: VehicleEntity, firstReading: Int? = null): Long = runBlocking { db.vehicles().add(entity, firstReading) }

    fun item(entity: ScheduleItemEntity): Long = runBlocking {
        db.schedule().insertAll(listOf(entity))
        db.schedule().allFor(entity.vehicleId).maxOf { it.id }
    }

    fun record(vehicleId: Long, date: LocalDate, odometer: Int, vararg lines: ServiceRecordItemEntity, enteredAsLastDone: Boolean = false): Long = runBlocking {
        db.service().insertWithItems(ServiceRecordEntity(vehicleId = vehicleId, date = date, odometer = odometer, diy = true, enteredAsLastDone = enteredAsLastDone), lines.toList())
    }

    fun reading(vehicleId: Long, date: LocalDate, odometer: Int) = runBlocking { db.odometer().insert(OdometerReadingEntity(vehicleId = vehicleId, date = date, odometer = odometer)) }

    fun items(vehicleId: Long): List<ScheduleItemEntity> = runBlocking { db.schedule().allFor(vehicleId) }

    fun close() {
        store.clear()
        holders.cancel()
        appScope.cancel()
        idle()
        db.close()
    }
}

/** A line of a record, against [itemId] and/or a catalog line. */
fun line(label: String, itemId: Long? = null, catalogId: String? = null) = ServiceRecordItemEntity(recordId = 0, scheduleItemId = itemId, label = label, catalogId = catalogId)

fun idle() = shadowOf(Looper.getMainLooper()).idle()

/** Runs the main looper until [condition] holds: Room and DataStore answer on threads of their own. */
fun eventually(timeoutMs: Long = 10_000, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (true) {
        idle()
        if (condition()) return
        check(System.currentTimeMillis() < end) { "Condition not met in $timeoutMs ms" }
        Thread.sleep(5)
    }
}

/** Made-up vehicles: VINs are left out, so none belongs to anyone. */
object ScheduleFixtures {
    val ADDED: LocalDate = LocalDate.of(2026, 1, 5)

    /** Matches the bundled 2017–2024 4Runner list. */
    fun fourRunner(engine: String? = "4.0L V6") = VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", engine = engine, drivetrain = "4WD", createdOn = ADDED)

    /** A model with no maker's list: the typical list. */
    fun hatch() = VehicleEntity(vin = null, year = 2008, make = "Volkswagen", model = "Golf", drivetrain = "FWD", fuel = "Gasoline", createdOn = ADDED)
}

/** Draws [content] as the app does, in the light theme on the page colour. */
fun ComposeContentTestRule.show(content: @Composable () -> Unit) = setContent {
    GloveboxTheme(ThemeMode.LIGHT) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
    }
}

/** The app's wording, so tests follow `strings.xml` rather than English typed here. */
fun text(@StringRes id: Int, vararg args: Any): String = ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
    ApplicationProvider.getApplicationContext<Context>().resources.getQuantityString(id, count, *args)

/** Waits for the database to show up on screen. */
fun ComposeContentTestRule.waitFor(text: String, substring: Boolean = false) =
    until { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

/**
 * Waits for [condition] while the screen and the main looper run: a ViewModel's write goes through Room's
 * own threads and comes back on the main looper, which the compose rule alone doesn't always turn.
 */
fun ComposeContentTestRule.until(timeoutMs: Long = 10_000, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (true) {
        waitForIdle()
        idle()
        if (condition()) return
        check(System.currentTimeMillis() < end) { "Condition not met in $timeoutMs ms" }
        mainClock.advanceTimeBy(16)
        Thread.sleep(10)
    }
}
