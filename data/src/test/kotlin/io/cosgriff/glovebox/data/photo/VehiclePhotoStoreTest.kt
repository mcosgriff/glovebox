package io.cosgriff.glovebox.data.photo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.PhotoSizing
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VehiclePhotoStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: GloveboxDatabase
    private lateinit var crypto: PhotoCrypto
    private lateinit var store: VehiclePhotoStore

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        PickedFilesProvider.install()
        db = inMemoryDatabase(context)
        crypto = PhotoCrypto(context)
        store = VehiclePhotoStore(context, crypto, db.vehicles())
    }

    @After fun tearDown() {
        db.close()
        FakeAndroidKeyStore.uninstall()
    }

    @Test
    fun `a new photo replaces the old one, file and all`() = runBlocking {
        val id = db.vehicles().add(testVehicle(), firstReading = null)

        assertTrue(store.set(id, PickedFilesProvider.offer("first.png", picture(4000, 3000))))
        val first = db.vehicles().byId(id)!!.photoFile!!
        assertTrue(first.startsWith("vehicle-$id-"))
        val shown = store.load(first)!!
        assertEquals(PhotoSizing.MAX_EDGE, maxOf(shown.width, shown.height))

        assertTrue(store.set(id, PickedFilesProvider.offer("second.png", picture(40, 30))))
        val second = db.vehicles().byId(id)!!.photoFile!!
        assertEquals(listOf(second), crypto.photoDir.list()!!.toList())
        assertEquals(40, store.load(second)!!.width)
    }

    @Test
    fun `a photo picked for a vehicle deleted meanwhile, or one that won't read, changes nothing`() = runBlocking {
        assertFalse(store.set(999, PickedFilesProvider.offer("p.png", picture(40, 30))))
        val id = db.vehicles().add(testVehicle(), firstReading = null)
        assertFalse(store.set(id, PickedFilesProvider.offer("notes.txt", "made-up".toByteArray())))

        assertNull(db.vehicles().byId(id)!!.photoFile)
        assertEquals(0, crypto.photoDir.list()!!.size)
    }

    @Test
    fun `removing the photo clears the row and the file`() = runBlocking {
        val id = db.vehicles().add(testVehicle(), firstReading = null)
        store.set(id, PickedFilesProvider.offer("p.png", picture(40, 30)))

        store.remove(id)
        assertNull(db.vehicles().byId(id)!!.photoFile)
        assertEquals(0, crypto.photoDir.list()!!.size)

        // A vehicle with none, or none at all, is no matter.
        store.remove(id)
        store.remove(999)
        assertNull(store.load("missing.jpg"))
    }
}
