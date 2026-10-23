package io.cosgriff.glovebox.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.scale
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.PhotoSizing
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.domain.StoredFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Receipt and parts photos, kept encrypted in [PhotoCrypto.photoDir]. This class only deals in
 * files; which record a file belongs to is the `photos` table's business.
 */
class RecordPhotoStore(
    private val context: Context,
    private val crypto: PhotoCrypto,
    private val service: ServiceDao,
    private val vehicles: VehicleDao,
    private val documents: DocumentDao,
) {
    /** Where the camera app writes a new photo before it is encrypted. Emptied as soon as it has been. */
    val captureDir: File get() = File(context.cacheDir, "capture").apply { mkdirs() }

    /** Where a decrypted copy sits while it is being shared. Emptied before the next share and on launch. */
    val sharedDir: File get() = File(context.cacheDir, "shared").apply { mkdirs() }

    /**
     * Android 10 only: where a document is decrypted for the in-app viewer, which has no memory-only file
     * there. Never exported through the FileProvider; a file is deleted as soon as it is open, and the
     * folder is emptied on launch.
     */
    val viewingDir: File get() = File(context.cacheDir, "viewing").apply { mkdirs() }

    /**
     * Encrypts the image at [source] and returns the new file's name, or null if it couldn't be
     * read. The image is redrawn as a smaller JPEG first, which also drops its metadata (where and
     * when it was taken).
     */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        val jpeg = redrawJpeg(ImageDecoder.createSource(context.contentResolver, source), RecordPhotos.MAX_EDGE, QUALITY)
            ?: return@withContext null
        val name = "record-${UUID.randomUUID()}.jpg"
        crypto.encryptTo(name, jpeg.inputStream())
        name
    }

    /** Decrypted in memory only, shrunk to about [edge] on its longest side; null if the file is missing or unreadable. */
    suspend fun load(fileName: String, edge: Int): Bitmap? = withContext(Dispatchers.IO) {
        val bytes = decrypt(fileName) ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply { inSampleSize = RecordPhotos.sampleSize(bounds.outWidth, bounds.outHeight, edge) }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** Like [load], but never larger than [edge] on its longest side: a PDF stores every pixel it is given. */
    suspend fun loadWithin(fileName: String, edge: Int): Bitmap? = withContext(Dispatchers.IO) {
        val bitmap = load(fileName, edge) ?: return@withContext null
        val (w, h) = PhotoSizing.fit(bitmap.width, bitmap.height, edge)
        if (w == bitmap.width) bitmap else bitmap.scale(w, h).also { bitmap.recycle() }
    }

    suspend fun delete(fileNames: Collection<String>) = withContext(Dispatchers.IO) {
        crypto.delete(fileNames)
    }

    /**
     * Writes a decrypted copy for the share sheet and returns it, or null if the photo can't be
     * read. This is the one place a photo leaves the app unencrypted. Earlier copies are removed first.
     */
    suspend fun shareCopy(fileName: String): File? = withContext(Dispatchers.IO) {
        clearShared()
        val bytes = decrypt(fileName) ?: return@withContext null
        File(sharedDir, "glovebox-photo.jpg").apply { writeBytes(bytes) }
    }

    fun clearShared() {
        sharedDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Run once per launch. Removes plaintext left in the cache, and encrypted files nothing points
     * at: photos of a deleted record whose Undo was never taken, or of a form that was abandoned, and
     * a document whose row never got written.
     */
    suspend fun sweep() = withContext(Dispatchers.IO) {
        clearShared()
        captureDir.listFiles()?.forEach { it.delete() }
        viewingDir.listFiles()?.forEach { it.delete() }
        val files = crypto.photoDir.listFiles().orEmpty().filter { it.isFile }.map { StoredFile(it.name, it.lastModified()) }
        if (files.isEmpty()) return@withContext
        val referenced = documents.allEncryptedFiles().toSet()
        crypto.delete(RecordPhotos.orphans(files, referenced, System.currentTimeMillis()))
    }

    private fun decrypt(fileName: String): ByteArray? = crypto.decryptBytes(fileName)

    private companion object {
        /** The JPEG quality a receipt or parts photo is saved at. */
        const val QUALITY = 82
    }
}
