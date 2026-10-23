package io.cosgriff.glovebox.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException

/** The name a picked file goes by; null when its provider doesn't say, or won't be asked. */
fun ContentResolver.displayName(uri: Uri): String? = try {
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
} catch (_: SecurityException) {
    null
}

/**
 * A picked file's bytes, up to [cap] and one more, so a caller can tell a file over the cap without a huge one
 * ever being held in memory; null when it can't be opened or read.
 */
fun ContentResolver.readCapped(uri: Uri, cap: Long): ByteArray? = try {
    openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (out.size() <= cap) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        out.toByteArray()
    }
} catch (_: IOException) {
    null
} catch (_: SecurityException) {
    null
}
