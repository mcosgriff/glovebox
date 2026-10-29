package io.cosgriff.glovebox.domain

/**
 * One photographed page of a paper invoice (#513): its encrypted [file], and once read, its [words] (on page 0;
 * [PhotoPages.words] numbers them) and how long reading took, for the debug trace.
 */
data class PhotoPage(val file: String, val words: List<InvoiceWord>? = null, val millis: Long? = null)

/**
 * The pages of a paper invoice in the order the user put them in, each read as soon as it is taken. Each change gives
 * a new list; a page is named by its file.
 */
data class PhotoPages(val pages: List<PhotoPage> = emptyList()) {
    fun add(file: String) = PhotoPages(pages + PhotoPage(file))

    /** [file] read as [words] in [millis]. Nothing happens if it was taken off meanwhile. */
    fun read(file: String, words: List<InvoiceWord>, millis: Long) =
        PhotoPages(pages.map { if (it.file == file) it.copy(words = words, millis = millis) else it })

    /** [file] moved [by] places, -1 earlier and 1 later; no further than either end. */
    fun move(file: String, by: Int): PhotoPages {
        val from = pages.indexOfFirst { it.file == file }
        if (from < 0) return this
        val to = (from + by).coerceIn(0, pages.size - 1)
        return PhotoPages(pages.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun remove(file: String) = PhotoPages(pages.filter { it.file != file })

    /** [file] taken again as [with]: in the same place, to be read again. */
    fun retake(file: String, with: String) = PhotoPages(pages.map { if (it.file == file) PhotoPage(with) else it })

    /** How many pages have been read so far. */
    val readCount: Int get() = pages.count { it.words != null }

    /** Every page is read and something was read on one: the form can be filled from them. */
    val ready: Boolean get() = pages.isNotEmpty() && readCount == pages.size && !nothingRead

    /** Every page is read and not a word was found on any. */
    val nothingRead: Boolean get() = pages.isNotEmpty() && pages.all { it.words?.isEmpty() == true }

    /** Every page's words, numbered from 1 by where the page is now. */
    fun words(): List<InvoiceWord> = pages.flatMapIndexed { i, p -> p.words.orEmpty().map { it.copy(page = i + 1) } }
}
