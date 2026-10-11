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

    /** Associated data binds each ciphertext to its file name so files can't be swapped. */
    fun encryptTo(fileName: String, source: InputStream) {
        File(photoDir, fileName).outputStream().use { raw ->
            aead.newEncryptingStream(raw, fileName.toByteArray()).use { enc -> source.copyTo(enc) }
        }
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
