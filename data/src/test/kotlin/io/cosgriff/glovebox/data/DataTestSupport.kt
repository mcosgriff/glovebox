package io.cosgriff.glovebox.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.StrongBoxUnavailableException
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.VehicleEntity
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.time.LocalDate
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Android Keystore for the JVM. Robolectric has none, so DatabaseKeyManager and PhotoCrypto (through Tink)
 * would fail at `KeyStore.getInstance("AndroidKeyStore")`. This registers a JCA provider under that name
 * that keeps AES keys in memory, records the spec each key was asked for (so the tests can hold the key
 * to the rules in CLAUDE.md) and can refuse StrongBox the way a phone without it does. Production code
 * is untouched: it asks the JCA for "AndroidKeyStore" exactly as it does on a phone.
 */
object FakeAndroidKeyStore {
    const val NAME = "AndroidKeyStore"

    val keys = LinkedHashMap<String, SecretKey>()

    /** Every spec a key was generated from, StrongBox attempts that were refused included. */
    val requested = mutableListOf<KeyGenParameterSpec>()

    /** Whether this "phone" has StrongBox. Without it, a StrongBox-backed key is refused as on a real one. */
    var strongBox = false

    /** Called with the alias just before a key is deleted, to see what else is still there at that moment. */
    var onDelete: (String) -> Unit = {}

    fun install() {
        keys.clear()
        requested.clear()
        strongBox = false
        onDelete = {}
        Security.removeProvider(NAME)
        Security.insertProviderAt(TestProvider(), 1)
    }

    fun uninstall() {
        Security.removeProvider(NAME)
    }

    @Suppress("DEPRECATION")
    private class TestProvider : Provider(NAME, 1.0, "In-memory Android Keystore for tests") {
        init {
            putService(Factory(this, "KeyStore", NAME) { Spi() })
            putService(Factory(this, "KeyGenerator", "AES") { Generator() })
        }
    }

    private class Factory(provider: Provider, type: String, algorithm: String, private val make: () -> Any) :
        Provider.Service(provider, type, algorithm, make.javaClass.name, null, null) {
        override fun newInstance(constructorParameter: Any?): Any = make()
    }

    private class Generator : KeyGeneratorSpi() {
        private var spec: KeyGenParameterSpec? = null

        override fun engineInit(random: SecureRandom?) = throw UnsupportedOperationException("Android Keystore needs a KeyGenParameterSpec")
        override fun engineInit(keysize: Int, random: SecureRandom?) = throw UnsupportedOperationException("Android Keystore needs a KeyGenParameterSpec")
        override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) {
            spec = params as KeyGenParameterSpec
        }

        override fun engineGenerateKey(): SecretKey {
            val spec = checkNotNull(spec)
            requested += spec
            if (spec.isStrongBoxBacked && !strongBox) throw StrongBoxUnavailableException()
            val size = if (spec.keySize > 0) spec.keySize else 256
            val key = SecretKeySpec(ByteArray(size / 8).also { SecureRandom().nextBytes(it) }, "AES")
            keys[spec.keystoreAlias] = key
            return key
        }
    }

    private class Spi : KeyStoreSpi() {
        override fun engineGetKey(alias: String, password: CharArray?): Key? = keys[alias]
        override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null
        override fun engineGetCertificate(alias: String?): Certificate? = null
        override fun engineGetCreationDate(alias: String?): Date? = null
        override fun engineSetKeyEntry(alias: String, key: Key, password: CharArray?, chain: Array<out Certificate>?) {
            keys[alias] = key as SecretKey
        }
        override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) = throw UnsupportedOperationException()
        override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) = throw UnsupportedOperationException()
        override fun engineDeleteEntry(alias: String) {
            onDelete(alias)
            keys.remove(alias)
        }
        override fun engineAliases(): Enumeration<String> = Collections.enumeration(keys.keys.toList())
        override fun engineContainsAlias(alias: String) = alias in keys
        override fun engineSize() = keys.size
        override fun engineIsKeyEntry(alias: String) = alias in keys
        override fun engineIsCertificateEntry(alias: String?) = false
        override fun engineGetCertificateAlias(cert: Certificate?): String? = null
        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit
        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
    }
}

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
