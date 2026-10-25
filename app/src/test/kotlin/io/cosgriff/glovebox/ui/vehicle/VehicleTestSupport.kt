package io.cosgriff.glovebox.ui.vehicle

import io.cosgriff.glovebox.ui.bulletins.BulletinCatalog
import android.app.Application
import android.os.Looper
import androidx.core.content.FileProvider
import org.robolectric.Shadows.shadowOf
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.room3.Room
import androidx.savedstate.serialization.encodeToSavedState
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.data.photo.DocumentStore
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import io.cosgriff.glovebox.data.photo.VehiclePhotoStore
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.rules.ExternalResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * What the tests of the vehicle screens share: a real database in memory, the app's own stores and
 * helpers built on it as Hilt would build them, and the route a screen's ViewModel reads.
 *
 * Nothing here reaches the network: NHTSA is answered by [RefusingEngine], which counts what was asked
 * and refuses it. Nothing reaches the Android Keystore either, so only the paths that read or delete
 * files (never the ones that encrypt) can be driven.
 */
class VehicleTestGarage : ExternalResource() {
    val context: Application = ApplicationProvider.getApplicationContext()
    val db: GloveboxDatabase = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()

    /** Stands in for `@ApplicationScope`: work that outlives a screen, such as an Undo. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> appErrors += e; e.printStackTrace() })

    /** What failed on [appScope], which would otherwise go unseen. */
    val appErrors: MutableList<Throwable> = CopyOnWriteArrayList()

    val crypto = PhotoCrypto(context)
    val settings = SettingsRepository(context)
    val engine = RefusingEngine()
    val recallRepository = RecallRepository(NhtsaClient(HttpClient(engine)), db.vehicles(), db.recalls(), settings)
    val vehiclePhotos = VehiclePhotoStore(context, crypto, db.vehicles())
    val recordPhotos = RecordPhotoStore(context, crypto, db.service(), db.vehicles(), db.documents())
    val documentStore = DocumentStore(context, crypto, db.documents(), recordPhotos)
    val wipe = DataWipe(context, db, DatabaseKeyManager(context), crypto)
    val archiver = VehicleArchiver(db.vehicles(), db.odometer(), appScope)
    val knownTakes = KnownTakesRows(KnownTakesCatalog(context), db.vehicles())
    val makerDocuments = MakerDocumentCatalog(context)
    val bulletins = BulletinCatalog(context)
    val photoPicker = VehiclePhotoPicker(vehiclePhotos, appScope)
    val documentPicker = DocumentPicker(context, documentStore, recordPhotos, appScope)
    val readingUndo = MileageReadingUndo(db.odometer(), db.vehicles(), appScope)

    /** Adds [vehicle] (its id is the database's), with [firstReading] as its first odometer reading. */
    fun add(vehicle: VehicleEntity, firstReading: Int? = null): Long = runBlocking { db.vehicles().add(vehicle.copy(id = 0), firstReading) }

    fun vehicle(id: Long): VehicleEntity? = runBlocking { db.vehicles().byId(id) }

    fun string(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    private val store = ViewModelStore()

    /** [create]'s ViewModel, cleared (its work cancelled) before the database closes. */
    @Suppress("UNCHECKED_CAST")
    fun <T : ViewModel> viewModel(create: () -> T): T {
        val made = create()
        return ViewModelProvider.create(store, object : ViewModelProvider.Factory {
            override fun <V : ViewModel> create(modelClass: Class<V>): V = made as V
        })[made.javaClass.name, made.javaClass]
    }

    /**
     * FileProvider keeps the folders it serves in a static cache, so a test after the first would be handed the
     * first test's (Robolectric gives each test new ones) and refuse every file. Forget them before each test.
     */
    override fun before() {
        FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.let { (it.get(null) as MutableMap<*, *>).clear() }
    }

    /** Use as the outer rule around the Compose rule, so the screens have gone before the database closes. */
    override fun after() {
        store.clear()
        appScope.cancel()
        db.close()
    }
}

/** A [SavedStateHandle] holding [key] where `ProvideRoute` puts it for a screen's ViewModels. */
internal fun routeHandle(key: NavKey) = SavedStateHandle(mapOf(ROUTE_KEY to encodeToSavedState(NavKeySerializer(), key)))

/** `Navigation.kt`'s private key for the route; a test that can't find its route fails on the first line. */
private const val ROUTE_KEY = "io.cosgriff.glovebox.route"

/**
 * Waits until [condition] holds, letting the main looper run between looks: the database answers on its
 * own threads and the ViewModels pick that up on the main one, which `waitUntil` alone doesn't drive.
 */
internal fun ComposeContentTestRule.awaitThat(timeoutMillis: Long = 10_000, condition: suspend () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMillis
    while (true) {
        waitForIdle()
        if (runBlocking { condition() }) return
        check(System.currentTimeMillis() < end) { "Still not so after $timeoutMillis ms" }
        Thread.sleep(10)
    }
}

/**
 * Waits until [condition] holds, for work on the app's own scope (an Undo, a picked file), which needs no
 * screen; the main looper runs between looks, for whatever that work hands back to it.
 */
internal suspend fun eventually(timeoutMillis: Long = 10_000, condition: suspend () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMillis
    while (!condition()) {
        check(System.currentTimeMillis() < end) { "Still not so after $timeoutMillis ms" }
        if (Looper.myLooper() == Looper.getMainLooper()) shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(10)
    }
}

/** Clicks through the node's semantics, so a sheet only partly on screen doesn't swallow the tap. */
internal fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction = performSemanticsAction(SemanticsActions.OnClick)

/** A node whose click is labelled [label] for screen readers. */
internal fun clickLabelled(label: String) = SemanticsMatcher("clicks to $label") { it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label }

/** NHTSA's stand-in: every request is counted and refused, as if the phone were offline. */
class RefusingEngine : HttpClientEngineBase("refusing") {
    val requests = AtomicInteger()
    override val config = HttpClientEngineConfig()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        requests.incrementAndGet()
        throw IOException("No network in tests: ${data.url}")
    }
}

/** Made-up vehicles. The VINs pass the check digit and belong to no one. */
internal object VehicleFixtures {
    val DAY: LocalDate = LocalDate.of(2026, 6, 15)

    /** Covered by the bundled 4Runner "What it takes" list and the maker's documents. */
    val truck = VehicleEntity(
        vin = "JTEBU5JR7K5900001",
        year = 2019,
        make = "Toyota",
        model = "4Runner",
        trim = "SR5",
        nickname = "Blue truck",
        drivetrain = "4WD/4-Wheel Drive/4x4",
        transmission = "Automatic",
        engine = "4.0L V6",
        bodyClass = "Sport Utility Vehicle (SUV)",
        createdOn = DAY.minusYears(2),
        // Checked recently, so opening the vehicle asks NHTSA nothing.
        recallsCheckedOn = LocalDate.now(),
    )

    /** Covered by the maker's documents, one of which is found by VIN. */
    val hatch = VehicleEntity(
        vin = "WVWHV7AJ0DW900002",
        year = 2013,
        make = "Volkswagen",
        model = "GTI",
        nickname = "Hatch",
        createdOn = DAY.minusYears(1),
        recallsCheckedOn = LocalDate.now(),
    )

    /** Entered by hand, with no VIN and nothing a bundled list covers. */
    val wagon = VehicleEntity(
        vin = null,
        year = 1998,
        make = "Volvo",
        model = "V70",
        createdOn = DAY.minusYears(1),
        enteredByHand = true,
        recallsCheckedOn = LocalDate.now(),
    )
}
