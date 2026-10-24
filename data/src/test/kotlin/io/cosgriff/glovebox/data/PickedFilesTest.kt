package io.cosgriff.glovebox.data

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.photo.PickedFilesProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PickedFilesTest {
    private val resolver: ContentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver

    @Before fun provider() = PickedFilesProvider.install()

    @Test
    fun `a picked file goes by the name its provider gives`() {
        assertEquals("made-up report.pdf", resolver.displayName(PickedFilesProvider.offer("a", byteArrayOf(1), "made-up report.pdf")))
    }

    @Test
    fun `no name when the provider gives none, doesn't know the file, or refuses`() {
        assertNull(resolver.displayName(PickedFilesProvider.offer("a", byteArrayOf(1), displayName = null)))
        assertNull(resolver.displayName(PickedFilesProvider.uri("unknown")))
        PickedFilesProvider.denied += "denied"
        assertNull(resolver.displayName(PickedFilesProvider.uri("denied")))
    }

    @Test
    fun `a file is read whole up to the cap`() {
        val bytes = ByteArray(200_000) { (it % 251).toByte() }
        assertArrayEquals(bytes, resolver.readCapped(PickedFilesProvider.offer("a", bytes), 200_000))
    }

    @Test
    fun `a file over the cap is read only far enough to tell`() {
        val read = resolver.readCapped(PickedFilesProvider.offer("a", ByteArray(1_000_000)), 100_000)!!
        // Past the cap, so the caller can say it is too big, but nowhere near the whole file.
        assertEquals(true, read.size in 100_001..100_000 + 64 * 1024)
    }

    @Test
    fun `a file that can't be opened or is refused reads as nothing`() {
        assertNull(resolver.readCapped(PickedFilesProvider.uri("missing"), 100))
        PickedFilesProvider.denied += "denied"
        assertNull(resolver.readCapped(PickedFilesProvider.uri("denied"), 100))
    }
}
