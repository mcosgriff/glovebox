package io.cosgriff.glovebox.domain

/** A file in the photo folder, as the sweep sees it. */
data class StoredFile(val name: String, val lastModifiedMillis: Long)

/** Rules for the receipt and parts photos kept with a service record. */
object RecordPhotos {
    /** Enough for a receipt, the parts and the odd before-and-after, without backups ballooning. */
    const val MAX_PER_RECORD = 10

    /** Longest edge kept. Larger than a vehicle photo's so the small print on a receipt stays readable. */
    const val MAX_EDGE = 2000

    /** Longest edge of a grid thumbnail. */
    const val THUMBNAIL_EDGE = 360

    /**
     * Longest edge a photo is given in a PDF export. Android's PDF writer stores pictures without
     * JPEG compression, so size climbs fast with resolution; this is about 140 dots per inch across
     * a page, enough to read a receipt in print.
     */
    const val PDF_EDGE = 1000

    /**
     * About what one photo adds to a PDF at [PDF_EDGE]: its pixels at three bytes each, less what
     * lossless compression saves on an ordinary photograph. An estimate for the size warning only.
     */
    const val PDF_BYTES_PER_PHOTO = 1_500_000L

    /** An export this big is awkward to email, so the sheet says so. */
    const val LARGE_EXPORT_MB = 15

    /** A file this new may belong to a form that is still open, so the sweep leaves it alone. */
    const val SWEEP_MIN_AGE_MILLIS = 10 * 60 * 1000L

    /** How many more photos a record with [have] can take. */
    fun room(have: Int): Int = (MAX_PER_RECORD - have).coerceAtLeast(0)

    /** "2 photos", "1 photo"; null for none, so callers can leave the phrase out. */
    fun countLabel(count: Int): String? = when {
        count <= 0 -> null
        count == 1 -> "1 photo"
        else -> "$count photos"
    }

    /**
     * Files nothing points at any more: a photo whose record was deleted and not brought back, or
     * one attached to a form that was abandoned. [referenced] is every file name the database
     * knows, for records and vehicles alike.
     */
    fun orphans(files: List<StoredFile>, referenced: Set<String>, nowMillis: Long): List<String> =
        files.filter { it.name !in referenced && nowMillis - it.lastModifiedMillis >= SWEEP_MIN_AGE_MILLIS }.map { it.name }

    /** Roughly what [count] photos add to a PDF export. */
    fun pdfBytes(count: Int): Long = count.coerceAtLeast(0) * PDF_BYTES_PER_PHOTO

    /**
     * The power-of-two factor to shrink a [width] x [height] image by while decoding, so its longest
     * edge ends up at least [targetEdge] and no more than twice that.
     */
    fun sampleSize(width: Int, height: Int, targetEdge: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= targetEdge) sample *= 2
        return sample
    }
}
