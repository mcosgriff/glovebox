package io.cosgriff.glovebox.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.ServiceDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.PhotoSizing
import io.cosgriff.glovebox.domain.RecordPhotos
import io.cosgriff.glovebox.domain.StoredFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
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
) {
    /** Where the camera app writes a new photo before it is encrypted. Emptied as soon as it has been. */
    val captureDir: File get() = File(context.cacheDir, "capture").apply { mkdirs() }

    /** Where a decrypted copy sits while it is being shared. Emptied before the next share and on launch. */
    val sharedDir: File get() = File(context.cacheDir, "shared").apply { mkdirs() }

    /**
     * Encrypts the image at [source] and returns the new file's name, or null if it couldn't be
     * read. The image is redrawn as a smaller JPEG first, which also drops its metadata (where and
     * when it was taken).
     */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        val jpeg = try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val (w, h) = PhotoSizing.fit(info.size.width, info.size.height, RecordPhotos.MAX_EDGE)
                decoder.setTargetSize(w, h)
            }
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }.toByteArray()
        } catch (_: IOException) {
            return@withContext null
        }
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
        if (w == bitmap.width) bitmap else Bitmap.createScaledBitmap(bitmap, w, h, true).also { bitmap.recycle() }
    }

    suspend fun delete(fileNames: Collection<String>) = withContext(Dispatchers.IO) {
        fileNames.forEach { File(crypto.photoDir, it).delete() }
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
     * at: photos of a deleted record whose Undo was never taken, or of a form that was abandoned.
     */
    suspend fun sweep() = withContext(Dispatchers.IO) {
        clearShared()
        captureDir.listFiles()?.forEach { it.delete() }
        val files = crypto.photoDir.listFiles().orEmpty().filter { it.isFile }.map { StoredFile(it.name, it.lastModified()) }
        if (files.isEmpty()) return@withContext
        val referenced = (service.allPhotoFiles() + vehicles.allPhotoFiles()).toSet()
        RecordPhotos.orphans(files, referenced, System.currentTimeMillis()).forEach { File(crypto.photoDir, it).delete() }
    }

    private fun decrypt(fileName: String): ByteArray? = try {
        ByteArrayOutputStream().also { crypto.decrypt(fileName, it) }.toByteArray()
    } catch (_: IOException) {
        null
    } catch (_: GeneralSecurityException) {
        null
    }
}
