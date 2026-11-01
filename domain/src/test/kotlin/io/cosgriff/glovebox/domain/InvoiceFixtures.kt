package io.cosgriff.glovebox.domain

/**
 * Lays out made-up invoices as Tesseract gives them: words with boxes on a 2550 px wide page, lines 40 px tall,
 * a character 20 px wide and a space as wide again. Columns are placed by their left edge. Every shop, person,
 * VIN, figure and address here is invented.
 */
class InvoiceBuilder {
    val words = mutableListOf<InvoiceWord>()

    /** The small filled shapes a PDF's pages draw (#572), as `PdfWords` gives them. */
    val marks = mutableListOf<InvoiceMark>()
    private var pages = 0

    fun page(build: InvoicePage.() -> Unit) {
        pages++
        InvoicePage(pages, words, marks).build()
    }
}

class InvoicePage(private val page: Int, private val out: MutableList<InvoiceWord>, private val marks: MutableList<InvoiceMark> = mutableListOf()) {
    private var y = 120f

    /** A line with a round mark of colour [rgb] (0xRRGGBB) at its left, as a health report marks an item. */
    fun marked(rgb: Int, vararg cells: Pair<Int, String>) {
        val middle = y + HEIGHT / 2
        marks += InvoiceMark(page, 60f, middle - HEIGHT * 0.6f, 60f + HEIGHT * 1.2f, middle + HEIGHT * 0.6f, rgb)
        row(*cells)
    }

    /** One line: each cell's text from its left edge, as words. */
    fun row(vararg cells: Pair<Int, String>, conf: Float = 93f) {
        cells.forEach { (x, text) -> place(x.toFloat(), text, HEIGHT, conf) }
        y += HEIGHT * 1.6f
    }

    /**
     * A service's title line with its number printed large at the left, as Shop-Ware prints it. [drop] moves the
     * number's middle down by that many line heights, as Shop-Ware's sits a little below its title's.
     */
    fun numbered(number: String, vararg cells: Pair<Int, String>, drop: Float = 0f) {
        place(60f, number, HEIGHT * 2, 90f, HEIGHT * drop)
        row(*cells)
        y += HEIGHT * 0.6f
    }

    fun gap(lines: Int = 1) {
        y += HEIGHT * 1.6f * lines
    }

    private fun place(x: Float, text: String, height: Float, conf: Float, drop: Float = 0f) {
        val middle = y + HEIGHT / 2 + drop
        var left = x
        text.split(' ').filter { it.isNotEmpty() }.forEach { w ->
            val width = w.length * height / 2
            out += InvoiceWord(w, page, left, middle - height / 2, left + width, middle + height / 2, conf)
            left += width + height / 2
        }
    }

    companion object {
        const val HEIGHT = 40f
    }
}

fun invoice(build: InvoiceBuilder.() -> Unit): List<InvoiceWord> = InvoiceBuilder().apply(build).words

/** A made-up invoice's words and its pages' marks. */
fun invoiceWithMarks(build: InvoiceBuilder.() -> Unit): Pair<List<InvoiceWord>, List<InvoiceMark>> =
    InvoiceBuilder().apply(build).let { it.words to it.marks }

/** [base] with its '?' replaced by the check digit that makes it a valid VIN. */
fun madeUpVin(base: String): String = ("0123456789X").map { base.replace('?', it) }.single { Vin.hasValidCheckDigit(it) }
