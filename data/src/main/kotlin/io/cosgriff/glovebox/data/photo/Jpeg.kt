package io.cosgriff.glovebox.data.photo

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import io.cosgriff.glovebox.domain.PhotoSizing
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * An image redrawn as a JPEG no larger than [maxEdge] on its long side, at [quality]. Redrawing also drops the
 * file's metadata (where and when it was taken), which is half the point. Null when it can't be read as an image.
 */
internal fun redrawJpeg(source: ImageDecoder.Source, maxEdge: Int, quality: Int): ByteArray? = try {
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val (w, h) = PhotoSizing.fit(info.size.width, info.size.height, maxEdge)
        decoder.setTargetSize(w, h)
    }
    ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it); bitmap.recycle() }.toByteArray()
} catch (_: IOException) {
    null
}
