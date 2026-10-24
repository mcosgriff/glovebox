package io.cosgriff.glovebox.ui.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.photo.RecordPhotoStore
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.serialization.encodeToSavedState
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.ui.photo.RecordPhotoPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.time.LocalDate

/**
 * What the tests of the service log, History and photos share: an in-memory database through Android's own
 * driver (as `:data`'s tests build it), the photo store over it, and made-up vehicles and records.
 */
internal class ServiceTestDb {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: GloveboxDatabase = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    val crypto = PhotoCrypto(context)
    val photos = RecordPhotoStore(context, crypto, db.service(), db.vehicles(), db.documents())

    val settings = SettingsRepository(context)

    /** Runs its work at once on the caller's thread, so a test sees it done when the call returns. */
    val appScope = CoroutineScope(Dispatchers.Unconfined)
    val picker = RecordPhotoPicker(context, photos, db.service(), appScope)
    val actions = RecordActions(db.service(), db.recalls(), db.vehicles(), photos)

    /** The folder encrypted files live in; the tests only ever look at which names are there. */
    val photoDir: File get() = crypto.photoDir

    /** A stand-in for an encrypted photo: only its name and whether it is still there matter here. */
    fun photoFile(name: String): String {
        File(photoDir, name).writeText("not a real photo")
        return name
    }

    private var images = 0

    /**
     * A plain red picture another app could hand over, at a content URI of its own. Only a test-drawn
     * bitmap, never a photo. Needs `@GraphicsMode(NATIVE)` to draw and decode.
     */
    fun imageUri(width: Int = 40, height: Int = 30): Uri {
        val png = ByteArrayOutputStream().also {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val uri = Uri.parse("content://made.up.provider/picture-${images++}.png")
        shadowOf(context.contentResolver).registerInputStream(uri, png.inputStream())
        return uri
    }

    /** A picture encrypted into the photo folder the way the app keeps one; its file name. */
    suspend fun realPhoto(width: Int = 40, height: Int = 30): String = checkNotNull(photos.import(imageUri(width, height)))

    fun hasFile(name: String) = File(photoDir, name).exists()

    /** ViewModels made by a test, cleared before the database closes so none is left reading a closed one. */
    private val viewModels = androidx.lifecycle.ViewModelStore()
    private var owned = 0

    fun <T : androidx.lifecycle.ViewModel> owned(vm: T): T {
        androidx.lifecycle.ViewModelProvider.create(
            viewModels,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <V : androidx.lifecycle.ViewModel> create(modelClass: kotlin.reflect.KClass<V>, extras: androidx.lifecycle.viewmodel.CreationExtras): V = vm as V
            },
        )["vm-${owned++}", vm::class]
        return vm
    }

    fun close() {
        viewModels.clear()
        db.close()
        photoDir.deleteRecursively()
    }

    suspend fun vehicle(
        model: String = "Corolla",
        unit: String = "MILES",
        firstReading: Int? = null,
        nickname: String? = null,
        swapOn: LocalDate? = null,
        offset: Int = 0,
        startReading: Int? = null,
    ): Long = db.vehicles().add(
        VehicleEntity(
            vin = null, year = 2019, make = "Toyota", model = model, nickname = nickname, createdOn = ServiceFixtures.DAY,
            distanceUnit = unit, clusterSwappedOn = swapOn, odometerOffset = offset, clusterStartReading = startReading,
        ),
        firstReading = firstReading,
    )

    suspend fun record(
        vehicleId: Long,
        date: LocalDate = ServiceFixtures.DAY,
        odometer: Int = 40_000,
        lines: List<ServiceRecordItemEntity> = listOf(line("Wiper blades replaced")),
        shop: String? = null,
        costCents: Long? = null,
        notes: String? = null,
    ): Long = db.service().insertWithItems(
        ServiceRecordEntity(vehicleId = vehicleId, date = date, odometer = odometer, diy = shop == null, shopName = shop, costCents = costCents, notes = notes),
        lines,
    )

    suspend fun recall(vehicleId: Long, campaign: String = "24V123000", completedOn: LocalDate? = null, recordId: Long? = null): Long {
        db.recalls().apply(
            listOf(
                RecallEntity(
                    vehicleId = vehicleId, campaign = campaign, component = "FUEL SYSTEM, GASOLINE", summary = "A made-up recall.",
                    firstSeenOn = ServiceFixtures.DAY, completedOn = completedOn, serviceRecordId = recordId,
                ),
            ),
            emptyList(), emptyList(),
        )
        return db.recalls().recallsFor(vehicleId).single { it.campaign == campaign }.id
    }
}

internal object ServiceFixtures {
    val DAY: LocalDate = LocalDate.of(2026, 5, 4)
}

internal fun line(label: String, catalogId: String? = null, scheduleItemId: Long? = null) =
    ServiceRecordItemEntity(recordId = 0, scheduleItemId = scheduleItemId, label = label, catalogId = catalogId)

/** What a screen's ViewModel finds in its `SavedStateHandle` when Navigation opens it with [key] (see `ProvideRoute`). */
internal fun routeHandle(key: NavKey) = SavedStateHandle(mapOf("io.cosgriff.glovebox.route" to encodeToSavedState(NavKeySerializer(), key)))

/**
 * Waits for work a ViewModel started on the database's own threads. With `Dispatchers.Main` set to
 * `Unconfined` it carries on there, so there is no test clock to advance.
 */
internal fun awaitUntil(what: String = "the condition", timeoutMs: Long = 5_000, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        check(System.currentTimeMillis() < end) { "Timed out waiting for $what" }
        Thread.sleep(5)
    }
}

/**
 * FileProvider keeps each authority's folders for the life of the process, and every Robolectric test has a
 * folder of its own, so the one cached by an earlier test would refuse this test's files. Call before a test
 * that hands a file out.
 */
internal fun forgetFileProviderRoots() {
    val cache = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.get(null) as MutableMap<*, *>
    synchronized(cache) { cache.clear() }
}

/**
 * Lets the main thread run until [condition] holds: work a ViewModel started goes back and forth between the
 * database's threads and the main one, and `waitUntil` alone doesn't always let the main one run.
 */
internal fun androidx.compose.ui.test.junit4.ComposeTestRule.settle(what: String = "the condition", timeoutMs: Long = 5_000, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (true) {
        waitForIdle()
        if (condition()) return
        check(System.currentTimeMillis() < end) { "Timed out waiting for $what" }
        Thread.sleep(10)
    }
}
