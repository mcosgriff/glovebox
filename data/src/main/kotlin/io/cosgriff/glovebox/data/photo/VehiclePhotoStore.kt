package io.cosgriff.glovebox.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.PhotoSizing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.UUID

/**
 * A vehicle's own photo, kept encrypted beside the receipt photos. The picked image is redrawn
 * as a smaller JPEG first, which also drops its metadata (where and when it was taken).
 */
class VehiclePhotoStore(
    private val context: Context,
    private val crypto: PhotoCrypto,
    private val vehicles: VehicleDao,
) {
    /** Replaces the vehicle's photo with the image at [source]. False if it couldn't be read. */
    suspend fun set(vehicleId: Long, source: Uri): Boolean = withContext(Dispatchers.IO) {
        val jpeg = try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val (w, h) = PhotoSizing.fit(info.size.width, info.size.height)
                decoder.setTargetSize(w, h)
            }
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
        } catch (_: IOException) {
            return@withContext false
        }
        // The vehicle may have been deleted while the picker was open.
        val vehicle = vehicles.byId(vehicleId) ?: return@withContext false
        val name = "vehicle-$vehicleId-${UUID.randomUUID()}.jpg"
        crypto.encryptTo(name, jpeg.inputStream())
        vehicles.setPhotoFile(vehicleId, name)
        vehicle.photoFile?.let(crypto::delete)
        true
    }

    suspend fun remove(vehicleId: Long) = withContext(Dispatchers.IO) {
        val old = vehicles.byId(vehicleId)?.photoFile ?: return@withContext
        vehicles.setPhotoFile(vehicleId, null)
        crypto.delete(old)
    }

    /** Decrypted in memory only; null if the file is missing or can't be read. */
    suspend fun load(fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bytes = ByteArrayOutputStream().also { crypto.decrypt(fileName, it) }.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: IOException) {
            null
        } catch (_: GeneralSecurityException) {
            null
        }
    }
}
