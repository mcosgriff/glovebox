package io.cosgriff.glovebox.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Owns the SQLCipher passphrase.
 *
 * - A random 32-byte passphrase encrypts the database.
 * - That passphrase is stored only in wrapped form, encrypted by an AES-256-GCM key that lives in
 *   Android Keystore (StrongBox when available) and never leaves the secure hardware.
 * - The Keystore key does NOT require user authentication and is NOT bound to an unlocked device,
 *   so WorkManager can compute reminders and check recalls while the phone is locked.
 *   Biometrics gate the UI only (see AppLock in :app).
 */
class DatabaseKeyManager(private val context: Context) {

    private val wrappedKeyFile: File get() = File(context.noBackupFilesDir, WRAPPED_KEY_FILE)

    /** Returns the raw passphrase, creating and wrapping a new one on first launch. */
    @Synchronized
    fun passphrase(): ByteArray {
        val kek = getOrCreateKek()
        val file = wrappedKeyFile
        if (file.exists()) return unwrap(kek, file.readBytes())

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        file.writeBytes(wrap(kek, passphrase))
        return passphrase
    }

    /**
     * Crypto-erase: deletes the Keystore key and the wrapped passphrase. After this, any database
     * bytes left on flash storage are unreadable. Callers then delete the database and photo files.
     */
    @Synchronized
    fun destroy() {
        keyStore().deleteEntry(KEK_ALIAS)
        wrappedKeyFile.delete()
    }

    private fun wrap(kek: SecretKey, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, kek) }
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }

    private fun unwrap(kek: SecretKey, blob: ByteArray): ByteArray {
        val ivLen = blob[0].toInt()
        val iv = blob.copyOfRange(1, 1 + ivLen)
        val body = blob.copyOfRange(1 + ivLen, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, iv))
        }
        return cipher.doFinal(body)
    }

    private fun keyStore() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKek(): SecretKey {
        (keyStore().getKey(KEK_ALIAS, null) as? SecretKey)?.let { return it }
        return try {
            generateKek(strongBox = true)
        } catch (e: StrongBoxUnavailableException) {
            generateKek(strongBox = false)
        }
    }

    private fun generateKek(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEK_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // background work must be able to open the DB
            .setIsStrongBoxBacked(strongBox)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEK_ALIAS = "glovebox_db_kek"
        const val WRAPPED_KEY_FILE = "db.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
