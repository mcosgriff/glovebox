package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.data.DataWipe
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration

/** Delete all data, once confirmed: a crypto-erase, keys first, run once however often it is asked for, then the restart. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class DeleteAllViewModelTest {
    @get:Rule(order = 0) val g = TestGraph()
    @get:Rule(order = 1) val main = MainDispatcherRule()

    @Before fun keyStore() {
        // In place of the one ScreenshotApp installs: this one shows which keys are left.
        io.cosgriff.glovebox.data.FakeAndroidKeyStore.uninstall()
        FakeKeyStore.entries.clear()
        FakeKeyStore.entries += setOf("glovebox_db_kek", "glovebox_photo_kek")
        Security.insertProviderAt(FakeKeyStore.Provider, 1)
    }

    @After fun noKeyStore() = Security.removeProvider(FakeKeyStore.Provider.name)

    @Test
    fun `the keys go, then every file, and only then is the app restarted`() = runTest {
        g.vehicle()
        val leftover = File(g.context.cacheDir, "export/export.tmp").apply { parentFile!!.mkdirs(); writeText("in the clear") }
        File(g.crypto.photoDir, "photo.enc").writeText("encrypted")
        val wrapped = File(g.context.noBackupFilesDir, "db.key").apply { writeText("wrapped") }
        val vm = DeleteAllViewModel(DataWipe(g.context, g.db, DatabaseKeyManager(g.context), g.crypto))
        var restarts = 0
        vm.delete { restarts++ }
        assertTrue(vm.deleting)
        // A second tap while it runs starts nothing more.
        vm.delete { restarts++ }
        eventually { restarts > 0 }
        assertEquals(1, restarts)
        assertTrue("both keys are destroyed", FakeKeyStore.entries.isEmpty())
        assertFalse(wrapped.exists())
        assertFalse(leftover.exists())
        assertFalse(g.crypto.photoDir.listFiles().orEmpty().any { it.name == "photo.enc" })
        assertFalse("the wipe is finished, so nothing is left to finish on the next launch", File(g.context.noBackupFilesDir, "wipe.pending").exists())
    }
}

/**
 * Android's key store, which Robolectric doesn't have, reduced to what the wipe asks of it: the names of the keys,
 * and deleting them.
 */
private object FakeKeyStore {
    val entries: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    object Provider : java.security.Provider("AndroidKeyStore", 1.0, "A test's stand-in for Android's key store") {
        private fun readResolve(): Any = Provider

        init {
            put("KeyStore.AndroidKeyStore", Spi::class.java.name)
        }
    }

    class Spi : KeyStoreSpi() {
        override fun engineGetKey(alias: String, password: CharArray?): Key? = null
        override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null
        override fun engineGetCertificate(alias: String): Certificate? = null
        override fun engineGetCreationDate(alias: String): Date? = null
        override fun engineSetKeyEntry(alias: String, key: Key, password: CharArray?, chain: Array<out Certificate>?) {
            entries += alias
        }
        override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<out Certificate>?) {
            entries += alias
        }
        override fun engineSetCertificateEntry(alias: String, cert: Certificate) = Unit
        override fun engineDeleteEntry(alias: String) {
            entries -= alias
        }
        override fun engineAliases(): Enumeration<String> = Collections.enumeration(entries.toList())
        override fun engineContainsAlias(alias: String): Boolean = alias in entries
        override fun engineSize(): Int = entries.size
        override fun engineIsKeyEntry(alias: String): Boolean = alias in entries
        override fun engineIsCertificateEntry(alias: String): Boolean = false
        override fun engineGetCertificateAlias(cert: Certificate): String? = null
        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit
        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
    }
}

