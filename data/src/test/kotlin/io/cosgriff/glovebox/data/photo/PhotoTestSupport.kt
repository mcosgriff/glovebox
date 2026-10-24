package io.cosgriff.glovebox.data.photo

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import org.robolectric.Robolectric
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

/**
 * Another app's files, as the system picker hands them over: `content://picked/<name>` opens the bytes put in
 * [files] under that name and says its display name. A name in [denied] is refused, as a provider that has
 * revoked access does.
 */
class PickedFilesProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val name = uri.lastPathSegment!!
        if (name in denied) throw SecurityException("access revoked")
        if (name !in files) return null
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf(displayNames[name])) }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val name = uri.lastPathSegment!!
        if (name in denied) throw SecurityException("access revoked")
        val bytes = files[name] ?: throw FileNotFoundException(name)
        val file = File.createTempFile("picked", null).apply { writeBytes(bytes); deleteOnExit() }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val AUTHORITY = "picked"
        val files = mutableMapOf<String, ByteArray>()
        val displayNames = mutableMapOf<String, String?>()
        val denied = mutableSetOf<String>()

        fun install() {
            files.clear()
            displayNames.clear()
            denied.clear()
            Robolectric.setupContentProvider(PickedFilesProvider::class.java, AUTHORITY)
        }

        /** Offers [bytes] as a picked file and returns its uri. */
        fun offer(name: String, bytes: ByteArray, displayName: String? = name): Uri {
            files[name] = bytes
            displayNames[name] = displayName
            return uri(name)
        }

        fun uri(name: String): Uri = Uri.parse("content://$AUTHORITY/$name")
    }
}

/** A plain picture of [width] by [height], made here; nobody's photo. */
fun picture(width: Int, height: Int, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(194, 65, 12)) }
    return ByteArrayOutputStream().also { bitmap.compress(format, 90, it) }.toByteArray()
}

/** The smallest thing that starts the way a PDF does; nothing renders it here. */
val madeUpPdf: ByteArray = "%PDF-1.4\n% made-up document\n%%EOF\n".toByteArray()
