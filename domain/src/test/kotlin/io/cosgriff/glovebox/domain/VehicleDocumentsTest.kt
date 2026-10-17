package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VehicleDocumentsTest {
    private fun doc(id: Long, type: DocumentType, name: String, date: LocalDate? = null) =
        StoredDocument(id, type, name, date, DocumentFormat.PDF, 1_000)

    @Test
    fun `a stored type or format this version doesn't know reads as other or a PDF`() {
        assertEquals(DocumentType.TITLE, VehicleDocuments.type("TITLE"))
        assertEquals(DocumentType.OTHER, VehicleDocuments.type("LEASE"))
        assertEquals(DocumentType.OTHER, VehicleDocuments.type(null))
        assertEquals(DocumentFormat.JPEG, VehicleDocuments.format("JPEG"))
        assertEquals(DocumentFormat.PDF, VehicleDocuments.format("TIFF"))
    }

    @Test
    fun `a PDF is known by its first bytes`() {
        assertTrue(VehicleDocuments.isPdf("%PDF-1.7\n".toByteArray()))
        assertFalse(VehicleDocuments.isPdf(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())))
        assertFalse(VehicleDocuments.isPdf("%PD".toByteArray()))
    }

    @Test
    fun `a name is trimmed, kept on one line and cut to the limit`() {
        assertEquals("Carfax report", VehicleDocuments.cleanName("  Carfax \n  report "))
        assertEquals(VehicleDocuments.MAX_NAME, VehicleDocuments.cleanName("x".repeat(200))!!.length)
        assertNull(VehicleDocuments.cleanName("   "))
    }

    @Test
    fun `a starting name comes from the file's own, or the type`() {
        assertEquals("carfax report 2026", VehicleDocuments.nameFromFile("carfax_report-2026.pdf"))
        assertNull(VehicleDocuments.nameFromFile(null))
        assertNull(VehicleDocuments.nameFromFile("___.pdf"))
    }

    @Test
    fun `documents are grouped by type, newest first, undated last`() {
        val old = doc(1, DocumentType.INSURANCE, "Card 2025", LocalDate.of(2025, 4, 1))
        val new = doc(2, DocumentType.INSURANCE, "Card 2026", LocalDate.of(2026, 4, 1))
        val undated = doc(3, DocumentType.INSURANCE, "Old card")
        val report = doc(4, DocumentType.REPORT, "Carfax")
        val other = doc(5, DocumentType.OTHER, "Window sticker", LocalDate.of(2026, 1, 1))
        assertEquals(listOf(4L, 2L, 1L, 3L, 5L), VehicleDocuments.sorted(listOf(other, undated, old, report, new)).map { it.id })
    }

    @Test
    fun `sizes read in kilobytes below a megabyte`() {
        assertEquals("1 KB", VehicleDocuments.size(10))
        assertEquals("340 KB", VehicleDocuments.size(340 * 1024))
        assertEquals("4.2 MB", VehicleDocuments.size(4_400_000))
    }

    @Test
    fun `an empty file or one over the cap is refused`() {
        assertEquals(DocumentProblem.EMPTY, VehicleDocuments.problem(0))
        assertNull(VehicleDocuments.problem(VehicleDocuments.MAX_BYTES))
        assertEquals(DocumentProblem.TOO_BIG, VehicleDocuments.problem(VehicleDocuments.MAX_BYTES + 1))
    }

    @Test
    fun `a shared copy's file name is never a path`() {
        assertEquals("Title, 2019.pdf", VehicleDocuments.fileName("Title, 2019", DocumentFormat.PDF))
        assertEquals("etcpasswd.jpg", VehicleDocuments.fileName("../etc/passwd", DocumentFormat.JPEG))
        assertEquals("document.pdf", VehicleDocuments.fileName("///", DocumentFormat.PDF))
    }
}
