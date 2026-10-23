package io.cosgriff.glovebox.data.crypto

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Encrypts receipt, parts and vehicle photos at rest with Tink streaming AEAD.
 * The Tink keyset is itself encrypted by a separate Android Keystore key.
 */
class PhotoCrypto(private val context: Context) {

    private val aead: StreamingAead by lazy {
        StreamingAeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFS_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }

    val photoDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Where a restore keeps its photos until the database has been replaced. See BackupRestorer. */
    val restoreDir: File get() = File(context.filesDir, "photos-restore")

    /** Associated data binds each ciphertext to its file name so files can't be swapped. */
    fun encryptTo(fileName: String, source: InputStream) = encryptTo(File(photoDir, fileName), fileName, source)

    /** Encrypts to [target], which may sit outside [photoDir] for now; it must end up there under [fileName] to be readable. */
    fun encryptTo(target: File, fileName: String, source: InputStream) {
        target.outputStream().use { raw ->
            aead.newEncryptingStream(raw, fileName.toByteArray()).use { enc -> source.copyTo(enc) }
        }
    }

    /** Deletes these encrypted files; one already gone is no matter. */
    fun delete(fileNames: Iterable<String>) {
        fileNames.forEach { File(photoDir, it).delete() }
    }

    fun delete(fileName: String) = delete(listOf(fileName))

    /** The file decrypted in memory; null when it is missing or can't be read or decrypted. */
    fun decryptBytes(fileName: String): ByteArray? = try {
        java.io.ByteArrayOutputStream().also { decrypt(fileName, it) }.toByteArray()
    } catch (_: java.io.IOException) {
        null
    } catch (_: java.security.GeneralSecurityException) {
        null
    }

    fun decrypt(fileName: String, sink: OutputStream) {
        File(photoDir, fileName).inputStream().use { raw ->
            aead.newDecryptingStream(raw, fileName.toByteArray()).use { dec -> dec.copyTo(sink) }
        }
    }

    /** Crypto-erase for "Delete all data": drop the keyset, then the files. */
    fun destroy() {
        context.deleteSharedPreferences(PREFS_NAME)
        java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            .deleteEntry(MASTER_KEY_URI.removePrefix("android-keystore://"))
        photoDir.deleteRecursively()
    }

    private companion object {
        const val KEYSET_NAME = "photo_keyset"
        const val PREFS_NAME = "glovebox_photo_keys"
        const val MASTER_KEY_URI = "android-keystore://glovebox_photo_kek"
    }
}
