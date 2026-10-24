package io.cosgriff.glovebox.data.crypto

import android.content.Context
import android.security.keystore.KeyProperties
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.FakeAndroidKeyStore
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseKeyManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val wrapped get() = File(context.noBackupFilesDir, "db.key")

    @Before fun keystore() = FakeAndroidKeyStore.install()

    @After fun removeKeystore() = FakeAndroidKeyStore.uninstall()

    @Test
    fun `the passphrase is made once, 32 random bytes, and read back the same by later launches`() {
        val first = DatabaseKeyManager(context).passphrase()
        assertEquals(32, first.size)
        assertArrayEquals(first, DatabaseKeyManager(context).passphrase())
        assertArrayEquals(first, DatabaseKeyManager(context).passphrase())
        assertEquals(1, FakeAndroidKeyStore.keys.size)
    }

    @Test
    fun `only the wrapped passphrase is on disk, outside what Android backs up`() {
        val passphrase = DatabaseKeyManager(context).passphrase()
        assertTrue(wrapped.exists())
        val stored = wrapped.readBytes()
        // An IV length, the IV, then the passphrase sealed with its GCM tag: never the passphrase itself.
        assertEquals(1 + 12 + 32 + 16, stored.size)
        val raw = passphrase.toList()
        assertFalse((0..stored.size - raw.size).any { stored.toList().subList(it, it + raw.size) == raw })
    }

    @Test
    fun `the key that wraps it can be used while the phone is locked, so the workers can open the database`() {
        DatabaseKeyManager(context).passphrase()
        val spec = FakeAndroidKeyStore.requested.last()
        assertEquals("glovebox_db_kek", spec.keystoreAlias)
        assertFalse(spec.isUserAuthenticationRequired)
        assertFalse(spec.isUnlockedDeviceRequired)
        assertEquals(256, spec.keySize)
        assertArrayEquals(arrayOf(KeyProperties.BLOCK_MODE_GCM), spec.blockModes)
        assertArrayEquals(arrayOf(KeyProperties.ENCRYPTION_PADDING_NONE), spec.encryptionPaddings)
        assertEquals(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT, spec.purposes)
    }

    @Test
    fun `StrongBox is used where the phone has it`() {
        FakeAndroidKeyStore.strongBox = true
        DatabaseKeyManager(context).passphrase()
        assertEquals(listOf(true), FakeAndroidKeyStore.requested.map { it.isStrongBoxBacked })
    }

    @Test
    fun `without StrongBox the key is made in the ordinary keystore instead`() {
        FakeAndroidKeyStore.strongBox = false
        val passphrase = DatabaseKeyManager(context).passphrase()
        assertEquals(listOf(true, false), FakeAndroidKeyStore.requested.map { it.isStrongBoxBacked })
        assertArrayEquals(passphrase, DatabaseKeyManager(context).passphrase())
    }

    @Test
    fun `destroying the keys leaves nothing that could open the old database`() {
        val keys = DatabaseKeyManager(context)
        val old = keys.passphrase()
        keys.destroy()
        assertFalse(wrapped.exists())
        assertTrue(FakeAndroidKeyStore.keys.isEmpty())

        val fresh = keys.passphrase()
        assertFalse(old.contentEquals(fresh))
    }

    @Test
    fun `a wrapped passphrase whose keystore key is gone is refused, never quietly replaced`() {
        DatabaseKeyManager(context).passphrase()
        val before = wrapped.readBytes()
        FakeAndroidKeyStore.keys.clear()

        assertThrows(AEADBadTagException::class.java) { DatabaseKeyManager(context).passphrase() }
        assertArrayEquals(before, wrapped.readBytes())
    }
}
