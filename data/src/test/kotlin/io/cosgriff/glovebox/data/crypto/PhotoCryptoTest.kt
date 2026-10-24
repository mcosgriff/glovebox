package io.cosgriff.glovebox.data.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PhotoCryptoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val plain = "a made-up receipt: oil and filter, 4.5 quarts".toByteArray()

    @Before fun keystore() = FakeAndroidKeyStore.install()

    @After fun removeKeystore() = FakeAndroidKeyStore.uninstall()

    private fun File.contains(bytes: ByteArray): Boolean {
        val all = readBytes()
        return (0..all.size - bytes.size).any { i -> bytes.indices.all { all[i + it] == bytes[it] } }
    }

    @Test
    fun `a file is encrypted on disk and reads back as it went in`() {
        val crypto = PhotoCrypto(context)
        crypto.encryptTo("record-a.jpg", plain.inputStream())

        val onDisk = File(crypto.photoDir, "record-a.jpg")
        assertTrue(onDisk.exists())
        assertFalse(onDisk.contains(plain))
        assertArrayEquals(plain, crypto.decryptBytes("record-a.jpg"))
        assertArrayEquals(plain, ByteArrayOutputStream().also { PhotoCrypto(context).decrypt("record-a.jpg", it) }.toByteArray())
    }

    @Test
    fun `the keyset is kept wrapped by a keystore key`() {
        PhotoCrypto(context).encryptTo("record-a.jpg", plain.inputStream())
        assertTrue("glovebox_photo_kek" in FakeAndroidKeyStore.keys)
    }

    @Test
    fun `a file is bound to its name, so two can't be swapped`() {
        val crypto = PhotoCrypto(context)
        crypto.encryptTo("record-a.jpg", plain.inputStream())
        File(crypto.photoDir, "record-a.jpg").copyTo(File(crypto.photoDir, "record-b.jpg"))

        assertNull(crypto.decryptBytes("record-b.jpg"))
    }

    @Test
    fun `a missing or damaged file reads as nothing rather than failing`() {
        val crypto = PhotoCrypto(context)
        assertNull(crypto.decryptBytes("never-written.jpg"))

        crypto.encryptTo("record-a.jpg", plain.inputStream())
        val file = File(crypto.photoDir, "record-a.jpg")
        file.writeBytes(file.readBytes().also { it[it.size - 5] = (it[it.size - 5] + 1).toByte() })
        assertNull(crypto.decryptBytes("record-a.jpg"))
    }

    @Test
    fun `a file staged elsewhere is readable once it is moved in under its name`() {
        val crypto = PhotoCrypto(context)
        crypto.restoreDir.mkdirs()
        val staged = File(crypto.restoreDir, "record-a.jpg")
        crypto.encryptTo(staged, "record-a.jpg", plain.inputStream())
        assertNull(crypto.decryptBytes("record-a.jpg"))

        assertTrue(staged.renameTo(File(crypto.photoDir, "record-a.jpg")))
        assertArrayEquals(plain, crypto.decryptBytes("record-a.jpg"))
    }

    @Test
    fun `deleting files takes only those named, and one already gone is no matter`() {
        val crypto = PhotoCrypto(context)
        crypto.encryptTo("a.jpg", plain.inputStream())
        crypto.encryptTo("b.jpg", plain.inputStream())

        crypto.delete(listOf("a.jpg", "gone.jpg"))
        assertFalse(File(crypto.photoDir, "a.jpg").exists())
        assertTrue(File(crypto.photoDir, "b.jpg").exists())

        crypto.delete("b.jpg")
        assertFalse(File(crypto.photoDir, "b.jpg").exists())
    }

    @Test
    fun `destroying the keys makes a surviving copy of a file unreadable`() {
        val crypto = PhotoCrypto(context)
        crypto.encryptTo("record-a.jpg", plain.inputStream())
        val leftover = File(context.cacheDir, "leftover").also { File(crypto.photoDir, "record-a.jpg").copyTo(it) }

        crypto.destroy()
        assertFalse(File(context.filesDir, "photos").exists())
        assertFalse("glovebox_photo_kek" in FakeAndroidKeyStore.keys)

        // A new start makes new keys, under which the old bytes mean nothing.
        val after = PhotoCrypto(context)
        leftover.copyTo(File(after.photoDir, "record-a.jpg"))
        assertNull(after.decryptBytes("record-a.jpg"))
    }
}
