package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DocumentExpiryTest {
    private val today = LocalDate.of(2026, 10, 24)

    @Test
    fun `a date more than 30 days out is later, from 30 days out soon, and past once the day has gone`() {
        assertEquals(ExpiryState.LATER, DocumentExpiry.state(LocalDate.of(2026, 11, 24), today))
        assertEquals(ExpiryState.SOON, DocumentExpiry.state(LocalDate.of(2026, 11, 23), today))
        assertEquals(ExpiryState.SOON, today.let { DocumentExpiry.state(it, it) })
        assertEquals(ExpiryState.PAST, DocumentExpiry.state(LocalDate.of(2026, 10, 23), today))
    }

    private fun doc(id: Long, type: DocumentType, expiresOn: LocalDate?) =
        StoredDocument(id, type, "Paper $id", null, DocumentFormat.PDF, 1_000, expiresOn)

    @Test
    fun `only the latest expiry of each type is watched, and each Other on its own`() {
        val docs = listOf(
            doc(1, DocumentType.REGISTRATION, LocalDate.of(2025, 11, 1)),
            doc(2, DocumentType.REGISTRATION, LocalDate.of(2026, 11, 1)),
            doc(3, DocumentType.INSURANCE, LocalDate.of(2026, 12, 1)),
            doc(4, DocumentType.INSURANCE, null),
            doc(5, DocumentType.OTHER, LocalDate.of(2026, 1, 1)),
            doc(6, DocumentType.OTHER, LocalDate.of(2027, 1, 1)),
            doc(7, DocumentType.TITLE, null),
        )
        assertEquals(setOf(2L, 3L, 5L, 6L), DocumentExpiry.watched(docs))
    }

    @Test
    fun `two of a type with the same expiry watch the one added last`() {
        val day = LocalDate.of(2026, 11, 1)
        assertEquals(setOf(9L), DocumentExpiry.watched(listOf(doc(9, DocumentType.WARRANTY, day), doc(8, DocumentType.WARRANTY, day))))
    }
}
