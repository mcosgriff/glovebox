package io.cosgriff.glovebox.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.domain.PhotoSizing
import io.cosgriff.glovebox.domain.RecordPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val jpeg = redrawJpeg(ImageDecoder.createSource(context.contentResolver, source), PhotoSizing.MAX_EDGE, QUALITY)
            ?: return@withContext false
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

    /**
     * Decrypted in memory only; null if the file is missing or can't be read. Shrunk to about [PhotoSizing.MAX_EDGE]
     * while it is decoded: a photo from a backup was never redrawn, and a huge one would run the garage out of memory.
     */
    suspend fun load(fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        val bytes = crypto.decryptBytes(fileName) ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply { inSampleSize = RecordPhotos.sampleSize(bounds.outWidth, bounds.outHeight, PhotoSizing.MAX_EDGE) }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private companion object {
        /** The JPEG quality a vehicle's photo is saved at. */
        const val QUALITY = 85
    }
}
