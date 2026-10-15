package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.util.Locale

/** What a piece of a vehicle's paperwork is. Stored by name, so the order here is only how they are listed. */
enum class DocumentType(val label: String) {
    REPORT("Vehicle history report"),
    TITLE("Title"),
    REGISTRATION("Registration"),
    PURCHASE("Purchase agreement"),
    WARRANTY("Warranty"),
    INSURANCE("Insurance card"),
    OTHER("Other"),
}

/** How a document's file is kept: a PDF as it came, or a picture redrawn as a JPEG. */
enum class DocumentFormat(val mimeType: String, val extension: String) {
    PDF("application/pdf", "pdf"),
    JPEG("image/jpeg", "jpg"),
}

/** One document as the list shows it. [sizeBytes] is the file before encryption. */
data class StoredDocument(
    val id: Long,
    val type: DocumentType,
    val name: String,
    val date: LocalDate?,
    val format: DocumentFormat,
    val sizeBytes: Long,
)

/**
 * A vehicle's paperwork: a history report, the title, the registration, warranty papers. The files
 * are the owner's own, kept encrypted on the phone; nothing is read out of them or sent anywhere.
 */
object VehicleDocuments {
    /** Bigger than any scan of a title or a history report needs, and small enough to hold in memory while it is encrypted. */
    const val MAX_BYTES = 20L * 1024 * 1024

    const val MAX_NAME = 80

    /** Longest edge a picture is kept at: the small print on a title or an insurance card stays readable. */
    const val MAX_EDGE = 2500

    /** A stored name, or [DocumentType.OTHER] for one this version doesn't know. */
    fun type(name: String?): DocumentType = DocumentType.entries.firstOrNull { it.name == name } ?: DocumentType.OTHER

    fun format(name: String?): DocumentFormat = DocumentFormat.entries.firstOrNull { it.name == name } ?: DocumentFormat.PDF

    /** A PDF says so in its first bytes, whatever the file is called. */
    fun isPdf(start: ByteArray): Boolean = start.size >= 4 && String(start, 0, 4, Charsets.US_ASCII) == "%PDF"

    /** The name with runs of space made single and the ends trimmed, cut to [MAX_NAME]; null when nothing is left. */
    fun cleanName(text: String): String? = text.replace(Regex("\\s+"), " ").trim().take(MAX_NAME).trim().ifEmpty { null }

    /** A starting name from the picked file's own: "carfax_report-2026.pdf" is "carfax report 2026". */
    fun nameFromFile(displayName: String?, type: DocumentType): String =
        displayName?.substringBeforeLast('.')?.replace(Regex("[_-]+"), " ")?.let(::cleanName) ?: type.label

    /** Grouped by type in [DocumentType]'s order, then newest first, undated last. */
    fun sorted(documents: List<StoredDocument>): List<StoredDocument> = documents.sortedWith(
        compareBy<StoredDocument> { it.type.ordinal }
            .thenByDescending { it.date ?: LocalDate.MIN }
            .thenBy { it.name.lowercase(Locale.US) }
            .thenBy { it.id },
    )

    /** "340 KB", "4.2 MB". */
    fun size(bytes: Long): String = when {
        bytes < 1024 * 1024 -> "${((bytes + 1023) / 1024).coerceAtLeast(1)} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /** "3 documents · 4.2 MB", or "None yet". */
    fun summary(count: Int, totalBytes: Long): String = when (count) {
        0 -> "None yet"
        1 -> "1 document · ${size(totalBytes)}"
        else -> "$count documents · ${size(totalBytes)}"
    }

    /** Why a file can't be added, or null when it can. */
    fun problem(sizeBytes: Long): String? = when {
        sizeBytes <= 0 -> "That file is empty."
        sizeBytes > MAX_BYTES -> "That file is too big (the most is ${MAX_BYTES / (1024 * 1024)} MB)."
        else -> null
    }

    /**
     * The file name another app sees for a copy: the document's name with characters no file system
     * takes left out, and the format's extension. Never a path.
     */
    fun fileName(name: String, format: DocumentFormat): String {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "").replace(Regex("\\s+"), " ").trim().trim('.').take(60).trim()
        return "${safe.ifEmpty { "document" }}.${format.extension}"
    }
}
