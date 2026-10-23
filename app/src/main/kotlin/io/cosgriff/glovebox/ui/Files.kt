package io.cosgriff.glovebox.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** A file in one of the cache folders `res/xml/file_paths.xml` lists, as a URI another app can be given. */
fun Context.contentUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.files", file)

/** A new file in [dir] for the camera app to write a photo into, and the URI to give it. */
fun Context.newCapture(dir: File): Pair<File, Uri> {
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return file to contentUri(file)
}

/**
 * Hands [file], a decrypted copy, to another app: one to view it, or the share sheet when [share]. Only when the
 * user chose to. False if nothing on the phone can take it.
 */
fun Context.handOut(file: File, mimeType: String, share: Boolean): Boolean {
    val uri = contentUri(file)
    val intent = if (share) {
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null,
        )
    } else {
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType)
    }
    return try {
        startActivity(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
