package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordPhotosTest {
    private val now = 1_000_000_000L
    private val old = now - RecordPhotos.SWEEP_MIN_AGE_MILLIS

    @Test
    fun `room counts down to none`() {
        assertEquals(10, RecordPhotos.room(0))
        assertEquals(1, RecordPhotos.room(9))
        assertEquals(0, RecordPhotos.room(10))
        assertEquals(0, RecordPhotos.room(12))
    }

    @Test
    fun `only old files nothing points at are orphans`() {
        val files = listOf(
            StoredFile("kept.jpg", old),
            StoredFile("orphan.jpg", old),
            StoredFile("vehicle.jpg", old - 5),
            StoredFile("just-attached.jpg", now - 1_000),
        )
        assertEquals(listOf("orphan.jpg"), RecordPhotos.orphans(files, setOf("kept.jpg", "vehicle.jpg"), now))
    }

    @Test
    fun `sample size keeps the image at least as big as the target`() {
        assertEquals(1, RecordPhotos.sampleSize(600, 400, 360))
        assertEquals(2, RecordPhotos.sampleSize(800, 600, 360))
        assertEquals(4, RecordPhotos.sampleSize(2000, 1500, 360))
        assertEquals(1, RecordPhotos.sampleSize(2000, 1500, 2000))
    }

    @Test
    fun `ten photos are enough to earn the size warning`() {
        assertEquals(0L, RecordPhotos.pdfBytes(0))
        assertEquals(15_000_000L, RecordPhotos.pdfBytes(10))
        assertEquals(true, RecordPhotos.pdfBytes(10) / 1_000_000 >= RecordPhotos.LARGE_EXPORT_MB)
        assertEquals(false, RecordPhotos.pdfBytes(9) / 1_000_000 >= RecordPhotos.LARGE_EXPORT_MB)
    }
}
