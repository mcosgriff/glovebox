package io.cosgriff.glovebox.data.photo

import io.cosgriff.glovebox.data.readCapped
import io.cosgriff.glovebox.domain.DocumentProblem
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.RequiresApi
import io.cosgriff.glovebox.data.crypto.PhotoCrypto
import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.domain.DocumentFormat
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.VehicleDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.time.LocalDate
import java.util.UUID

/** What adding a document came to: the new row, or why nothing was added. */
sealed interface AddedDocument {
    data class Added(val id: Long) : AddedDocument
    data class Refused(val problem: DocumentProblem) : AddedDocument
}

/**
 * A vehicle's paperwork, kept encrypted in [PhotoCrypto.photoDir] beside the photos. A PDF is kept
 * as it came; a picture is redrawn as a JPEG, which drops its metadata (where and when it was taken).
 * It is viewed in the app ([openPdf], [loadPicture]) from memory; it is only ever in the clear on
 * disk in the copy handed to another app, or briefly on Android 10 (see [openPdf]).
 */
class DocumentStore(
    private val context: Context,
    private val crypto: PhotoCrypto,
    private val documents: DocumentDao,
    private val photos: RecordPhotoStore,
) {
    /**
     * Reads the file at [source] (a PDF or a picture) and files it with [vehicleId], named from
     * [displayName] until the owner names it, or [unnamed] (the type's name) when that gives none.
     * The file is read into memory, never past the size cap.
     */
    suspend fun add(vehicleId: Long, source: Uri, displayName: String?, unnamed: String, today: LocalDate): AddedDocument = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.readCapped(source, VehicleDocuments.MAX_BYTES)
            ?: return@withContext AddedDocument.Refused(DocumentProblem.UNREADABLE)
        VehicleDocuments.problem(bytes.size.toLong())?.let { return@withContext AddedDocument.Refused(it) }
        val name = VehicleDocuments.nameFromFile(displayName) ?: unnamed
        if (VehicleDocuments.isPdf(bytes)) {
            AddedDocument.Added(save(vehicleId, bytes, DocumentFormat.PDF, DocumentType.OTHER, name, null, today))
        } else {
            val jpeg = redraw(bytes) ?: return@withContext AddedDocument.Refused(DocumentProblem.NOT_A_DOCUMENT)
            AddedDocument.Added(save(vehicleId, jpeg, DocumentFormat.JPEG, DocumentType.OTHER, name, null, today))
        }
    }

    /** Files a PDF that is already in memory, as the Carfax import has its report. */
    suspend fun addPdf(vehicleId: Long, bytes: ByteArray, type: DocumentType, name: String, date: LocalDate?, today: LocalDate): Long =
        withContext(Dispatchers.IO) { save(vehicleId, bytes, DocumentFormat.PDF, type, name, date, today) }

    private suspend fun save(vehicleId: Long, bytes: ByteArray, format: DocumentFormat, type: DocumentType, name: String, date: LocalDate?, today: LocalDate): Long {
        val fileName = "document-${UUID.randomUUID()}.${format.extension}"
        crypto.encryptTo(fileName, bytes.inputStream())
        return try {
            documents.insert(
                DocumentEntity(
                    vehicleId = vehicleId,
                    type = type.name,
                    name = name,
                    date = date,
                    fileName = fileName,
                    format = format.name,
                    sizeBytes = bytes.size.toLong(),
                    addedOn = today,
                ),
            )
        } catch (e: Exception) {
            // The vehicle went meanwhile; the file has nothing to belong to.
            crypto.delete(fileName)
            throw e
        }
    }

    /** Removes the row, then the file. No Undo: nothing else hangs off a document. */
    suspend fun delete(document: DocumentEntity) = withContext(Dispatchers.IO) {
        documents.delete(document.id)
        crypto.delete(document.fileName)
    }

    /**
     * Writes a decrypted copy for another app to open and returns it, or null if the file can't be
     * read. It goes in the share folder, which is emptied before the next share and on launch.
     */
    suspend fun openCopy(document: DocumentEntity): File? = withContext(Dispatchers.IO) {
        photos.clearShared()
        val copy = File(photos.sharedDir, VehicleDocuments.fileName(document.name, VehicleDocuments.format(document.format)))
        try {
            copy.outputStream().use { crypto.decrypt(document.fileName, it) }
            copy
        } catch (_: IOException) {
            copy.delete()
            null
        } catch (_: GeneralSecurityException) {
            copy.delete()
            null
        }
    }

    /**
     * The decrypted PDF for the in-app viewer, as a descriptor the viewer's sandboxed renderer reads, or
     * null if the file can't be read. The caller closes it. On Android 11 and later it is an anonymous
     * file in memory (`memfd_create`), so nothing is written to disk. Android 10 has no such call: the
     * PDF is written to [RecordPhotoStore.viewingDir] and deleted as soon as it is open, leaving the
     * descriptor the only way to it.
     */
    suspend fun openPdf(document: DocumentEntity): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inMemory(document) else viaPrivateFile(document)
        } catch (_: IOException) {
            null
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: ErrnoException) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun inMemory(document: DocumentEntity): ParcelFileDescriptor {
        val fd = Os.memfd_create("glovebox-document", OsConstants.MFD_CLOEXEC)
        try {
            // Not closed: closing the stream would close the descriptor it writes to.
            crypto.decrypt(document.fileName, FileOutputStream(fd))
            Os.lseek(fd, 0, OsConstants.SEEK_SET)
            return ParcelFileDescriptor.dup(fd)
        } finally {
            Os.close(fd)
        }
    }

    private fun viaPrivateFile(document: DocumentEntity): ParcelFileDescriptor {
        val file = File(photos.viewingDir, "document-${UUID.randomUUID()}")
        try {
            file.outputStream().use { crypto.decrypt(document.fileName, it) }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } finally {
            file.delete()
        }
    }

    /** A picture document, decrypted in memory for the in-app viewer; null if it can't be read. */
    suspend fun loadPicture(document: DocumentEntity): Bitmap? = photos.load(document.fileName, VehicleDocuments.MAX_EDGE)

    private fun redraw(bytes: ByteArray): ByteArray? = redrawJpeg(ImageDecoder.createSource(ByteBuffer.wrap(bytes)), VehicleDocuments.MAX_EDGE, QUALITY)

    private companion object {
        /** The JPEG quality a picture document is saved at. */
        const val QUALITY = 88
    }
}
