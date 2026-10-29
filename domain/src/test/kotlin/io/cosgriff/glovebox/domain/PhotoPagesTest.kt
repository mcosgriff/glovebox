package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** The pages of a paper invoice as they are photographed and read (#513). File names are made up. */
class PhotoPagesTest {
    private fun word(text: String) = InvoiceWord(text, page = 0, left = 10f, top = 10f, right = 90f, bottom = 40f, confidence = 91f)

    @Test
    fun `each page's words take its place in the strip, wherever it was moved`() {
        val pages = PhotoPages()
            .add("record-a.jpg")
            .add("record-b.jpg")
            .read("record-b.jpg", listOf(word("TOTAL")), millis = 6100)
            .read("record-a.jpg", listOf(word("SERVICES")), millis = 5400)

        assertEquals(listOf("SERVICES" to 1, "TOTAL" to 2), pages.words().map { it.text to it.page })
        assertEquals(listOf("TOTAL" to 1, "SERVICES" to 2), pages.move("record-b.jpg", -1).words().map { it.text to it.page })
    }

    @Test
    fun `the form fills only once every page is read, and a retaken page is read again in its place`() {
        val two = PhotoPages().add("record-a.jpg").add("record-b.jpg").read("record-a.jpg", listOf(word("SERVICES")), millis = 5400)
        assertEquals(1, two.readCount)
        assertEquals(false, two.ready)

        val both = two.read("record-b.jpg", listOf(word("TOTAL")), millis = 6100)
        assertEquals(true, both.ready)

        val retaken = both.retake("record-a.jpg", "record-c.jpg")
        assertEquals(listOf("record-c.jpg", "record-b.jpg"), retaken.pages.map { it.file })
        assertEquals(false, retaken.ready)
        assertEquals(listOf("TOTAL" to 2), retaken.words().map { it.text to it.page })

        assertEquals(true, retaken.remove("record-c.jpg").ready)
        assertEquals(false, PhotoPages().ready)
    }

    @Test
    fun `pages with nothing read on any of them can't fill the form`() {
        val blank = PhotoPages().add("record-a.jpg").read("record-a.jpg", emptyList(), millis = 3000)
        assertEquals(1, blank.readCount)
        assertEquals(false, blank.ready)
        assertEquals(true, blank.nothingRead)
    }
}
