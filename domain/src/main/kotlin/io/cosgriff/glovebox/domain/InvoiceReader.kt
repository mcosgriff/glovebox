package io.cosgriff.glovebox.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.LocalDate
import kotlin.math.abs

/**
 * Reads a shop's invoice from its words (#483), into what a record with jobs needs: date, shop, VIN, odometer in
 * and out, grand total, and the services, each a job with its wording, labor and parts and the catalog lines
 * guessed from it. Every decision goes in a typed trace.
 *
 * **No per-shop templates.** An invoice is lines of text with prices beside them under headings; the reader
 * finds the services by a heading ("SERVICES"), a table whose header names labor and parts columns, or a line
 * carrying a subtotal, and tells figures apart by the invoice's own labels ("Labor:", "Total Parts:") and
 * columns. Pages are read as one text, with the header every page repeats skipped, so a service whose figures
 * run onto the next page joins up.
 *
 * **The sums vouch for every figure.** An invoice checks itself: parts rows (quantity × each = total) add up to
 * the service's parts, labor + parts + fees + sublet to its subtotal, every service's labor to the total labor,
 * and the totals block to the grand total. A figure is taken only when a sum it is in agrees; otherwise it is
 * left out, never guessed or worked out from the others. Text recognition's dangerous misreads are figures that
 * look right and are a digit off (#482); those break a sum.
 *
 * **Jobs come only from the services**, up to the totals. Recommended, declined or suggested work, and an
 * inspection report's priced items after the totals, are kept as text for notes: a job for work not done would
 * reset its reminder.
 */
object InvoiceReader {
    /** Words whose middles are within this many line heights of each other are on one line. */
    private const val SAME_LINE = 0.5f

    /** A gap this many line heights wide between two words on a line is a gap between columns. */
    private const val COLUMN_GAP = 1.5f

    /** A job's number printed this many times the line height is a service's number, not a word of its text. */
    private const val BIG = 1.4f

    /** How far down (and up) a page its repeated header (and footer) is looked for, in lines. */
    private const val EDGE_LINES = 12

    /** How far a shop drives a car between "in" and "out" at most; further apart, one of them is misread. In miles. */
    const val SHOP_DRIVE = 1_000

    /** How many look-alike characters a VIN may have folded before it passes its check digit. */
    private const val MAX_FOLDS = 2

    /**
     * [marks] are the small filled shapes on the PDF's pages (#572): an inspection report's red, yellow and green
     * marks, read by hue and paired with the item on their line. None (photos, a scan) gives the findings uncoloured.
     */
    fun read(
        words: List<InvoiceWord>,
        today: LocalDate,
        readings: List<OdometerReading>,
        unit: DistanceUnit,
        rules: List<CatalogRule> = emptyList(),
        marks: List<InvoiceMark> = emptyList(),
    ): InvoiceReading = Reading(words, today, readings, unit, rules, marks).run()

    // ---- Lines -------------------------------------------------------------------------------------------------

    private class Seg(val words: List<InvoiceWord>) {
        val text: String get() = words.joinToString(" ") { it.text }
    }

    private class Line(val index: Int, val page: Int, val segs: List<Seg>, val height: Float) {
        val words: List<InvoiceWord> get() = segs.flatMap { it.words }
        val text: String get() = segs.joinToString("  ") { it.text }
    }

    private fun middle(w: InvoiceWord) = (w.top + w.bottom) / 2

    private fun buildLines(words: List<InvoiceWord>): List<Line> {
        val out = mutableListOf<Line>()
        words.filter { it.text.isNotBlank() }.map { it.copy(text = it.text.trim()) }.groupBy { it.page }.toSortedMap().forEach { (page, onPage) ->
            val heights = onPage.map { (it.bottom - it.top).coerceAtLeast(1f) }.sorted()
            val h = heights[heights.size / 2]
            val rows = mutableListOf<MutableList<InvoiceWord>>()
            var centre = 0f
            onPage.sortedBy(::middle).forEach { w ->
                // A word printed taller than the page's lines (a service's number) may sit further off its line's middle.
                val reach = maxOf(h, w.bottom - w.top) * SAME_LINE
                if (rows.isNotEmpty() && abs(middle(w) - centre) <= reach) {
                    rows.last() += w
                    centre = rows.last().map(::middle).average().toFloat()
                } else {
                    rows += mutableListOf(w)
                    centre = middle(w)
                }
            }
            rows.forEach { row ->
                val sorted = row.sortedBy { it.left }
                val segs = mutableListOf(mutableListOf(sorted.first()))
                sorted.zipWithNext { a, b -> if (b.left - a.right > h * COLUMN_GAP) segs += mutableListOf(b) else segs.last() += b }
                out += Line(out.size, page, segs.map(::Seg), h)
            }
        }
        return out
    }

    /**
     * The lines every page repeats at its top (past the first page) or its bottom: the shop's header and footer.
     * A line is repeated when every other page has it among its first (last) [EDGE_LINES] lines, with digits
     * masked ("Page 2 of 7") and a misread letter or two forgiven; skipping stops at the first line that isn't.
     */
    private fun repeated(lines: List<Line>): Set<Int> {
        val byPage = lines.groupBy { it.page }
        if (byPage.size < 2) return emptySet()
        fun key(l: Line) = l.text.lowercase().map { if (it.isDigit()) '#' else it }.filter { it in 'a'..'z' || it == '#' }.joinToString("")
        val tops = byPage.mapValues { (_, ls) -> ls.take(EDGE_LINES).map(::key) }
        val bottoms = byPage.mapValues { (_, ls) -> ls.takeLast(EDGE_LINES).map(::key) }
        fun everywhere(k: String, page: Int, edges: Map<Int, List<String>>) = edges.filterKeys { it != page }.values.all { ks -> ks.any { similar(it, k) } }
        val first = byPage.keys.min()
        val skip = mutableSetOf<Int>()
        byPage.forEach { (page, ls) ->
            if (page != first) {
                for (l in ls) {
                    val k = key(l)
                    if (k.length < 3 || everywhere(k, page, tops)) skip += l.index else break
                }
            }
            for (l in ls.asReversed()) {
                if (l.index in skip) break
                val k = key(l)
                if (k.length >= 3 && everywhere(k, page, bottoms)) skip += l.index else break
            }
        }
        return skip
    }

    private fun similar(a: String, b: String): Boolean =
        a == b || (minOf(a.length, b.length) >= 8 && LineGuess.distance(a, b) <= maxOf(a.length, b.length) / 10)

    // ---- Words -------------------------------------------------------------------------------------------------

    private val MONEY = Regex("""^\(?(-)?\$?(-)?(\d{1,3}(?:,\d{3})+|\d+)\.(\d{2})\)?$""")
    private val FILLER = Regex("""(?i)^(\d+(\.\d+)?\s*(hrs?|hours?)\.?(\s*@\s*\$?\d+(\.\d{2})?(/hr)?)?|\(?\d+(\.\d+)?\s?%\)?)$""")
    private val QUANTITY = Regex("""^\d+(\.\d+)?$""")

    /** A printed amount in cents, signed; null when [text] isn't one (two decimals, a $ allowed). */
    private fun money(text: String): Long? {
        val m = MONEY.matchEntire(text) ?: return null
        val cents = m.groupValues[3].replace(",", "").toLong() * 100 + m.groupValues[4].toLong()
        val negative = m.groupValues[1].isNotEmpty() || m.groupValues[2].isNotEmpty() || text.startsWith("(")
        return if (negative) -cents else cents
    }

    private fun norm(word: String) = word.lowercase().trim(':', '.', ',', ';', '*')

    private enum class Label {
        GRAND_TOTAL, TOTAL, BALANCE, PAID, TOTAL_LABOR, TOTAL_PARTS, TOTAL_FEES, TOTAL_SUBLETS, SUPPLIES, TAX, CHARGE, DISCOUNT,
        SUBTOTAL, LABOR, PARTS, FEES, SUBLET,
    }

    /** The labels an invoice prints beside a figure, longest first so "total labor" is never read as "labor". */
    private val LABELS: List<Pair<List<String>, Label>> = listOf(
        "grand total" to Label.GRAND_TOTAL, "invoice total" to Label.GRAND_TOTAL, "total due" to Label.GRAND_TOTAL,
        "total amount" to Label.GRAND_TOTAL, "repair order total" to Label.GRAND_TOTAL, "total" to Label.TOTAL,
        "balance due" to Label.BALANCE, "amount due" to Label.BALANCE, "balance" to Label.BALANCE,
        "amount paid" to Label.PAID, "paid" to Label.PAID, "payment" to Label.PAID, "payments" to Label.PAID, "deposit" to Label.PAID,
        "total labor" to Label.TOTAL_LABOR, "total labour" to Label.TOTAL_LABOR, "labor total" to Label.TOTAL_LABOR,
        "total parts" to Label.TOTAL_PARTS, "parts total" to Label.TOTAL_PARTS,
        "total fees" to Label.TOTAL_FEES, "total sublets" to Label.TOTAL_SUBLETS, "total sublet" to Label.TOTAL_SUBLETS,
        "shop supplies" to Label.SUPPLIES, "supplies" to Label.SUPPLIES, "shop supply" to Label.SUPPLIES,
        "sales tax" to Label.TAX, "tax" to Label.TAX,
        "environmental fee" to Label.CHARGE, "hazardous waste" to Label.CHARGE, "disposal fee" to Label.CHARGE, "hazmat" to Label.CHARGE,
        "discount" to Label.DISCOUNT,
        "subtotal" to Label.SUBTOTAL, "sub total" to Label.SUBTOTAL, "sub-total" to Label.SUBTOTAL, "job total" to Label.SUBTOTAL,
        "service total" to Label.SUBTOTAL, "line total" to Label.SUBTOTAL,
        "labor" to Label.LABOR, "labour" to Label.LABOR, "parts" to Label.PARTS, "fees" to Label.FEES, "fee" to Label.FEES,
        "sublet" to Label.SUBLET, "sublets" to Label.SUBLET,
    ).map { (k, v) -> k.split(' ') to v }.sortedByDescending { it.first.size }

    private class Labelled(val label: Label, val cents: Long)

    /**
     * Takes "Label: $1.23" pairs off the right of a segment, as many as there are: what is left is its text.
     * A figure with no label before it stays in the text, for a table or a parts row to read.
     */
    private fun peel(seg: Seg): Pair<List<InvoiceWord>, List<Labelled>> {
        var ws = seg.words
        val found = mutableListOf<Labelled>()
        while (true) {
            val last = ws.lastOrNull() ?: break
            val cents = money(last.text) ?: break
            // "$" read as a word of its own.
            val used = if ('$' !in last.text && ws.size >= 2 && ws[ws.size - 2].text == "$") 2 else 1
            var before = ws.dropLast(used)
            while (before.isNotEmpty() && norm(before.last().text).isEmpty()) before = before.dropLast(1)
            // Hours, a rate or a percentage between a label and its figure ("Labor 3.2 hrs", "Tax 7%") belong to the label.
            val filler = (4 downTo 1).firstOrNull { n -> n <= before.size && FILLER.matches(before.takeLast(n).joinToString(" ") { it.text }) } ?: 0
            before = before.dropLast(filler)
            val match = LABELS.firstOrNull { (phrase, _) ->
                phrase.size <= before.size && phrase.indices.all { i -> norm(before[before.size - phrase.size + i].text) == phrase[i] }
            } ?: break
            found.add(0, Labelled(match.second, cents))
            ws = before.dropLast(match.first.size)
        }
        return ws to found
    }

    /**
     * [peel] on each of a line's segments, and a label standing alone in one segment paired with a lone figure in
     * the next ("Total Labor        79.90"): the line's text and its labelled figures.
     */
    private fun peelLine(line: Line): Pair<List<List<InvoiceWord>>, List<Labelled>> {
        val peeled = line.segs.map(::peel)
        val rest = mutableListOf<List<InvoiceWord>>()
        val labelled = mutableListOf<Labelled>()
        peeled.forEachIndexed { i, (words, found) ->
            val previous = rest.lastOrNull()
            val cents = words.singleOrNull()?.let { money(it.text) }
            val label = previous?.let(::wholeLabel)
            if (cents != null && label != null && i > 0 && peeled[i - 1].second.isEmpty()) {
                rest.removeAt(rest.lastIndex)
                labelled += Labelled(label, cents)
            } else if (words.isNotEmpty()) {
                rest += words
            }
            labelled += found
        }
        return rest to labelled
    }

    /** The label [words] are, all of them and nothing else. */
    private fun wholeLabel(words: List<InvoiceWord>): Label? {
        // With hours, a rate or a percentage after it, as [peel] allows.
        val filler = (4 downTo 1).firstOrNull { n -> n < words.size && FILLER.matches(words.takeLast(n).joinToString(" ") { it.text }) } ?: 0
        val ws = words.dropLast(filler).map { norm(it.text) }.filter { it.isNotEmpty() }
        return LABELS.firstOrNull { it.first == ws }?.second
    }

    private fun hasLetters(text: String) = text.count { it.isLetter() } >= 2

    // ---- Headings ----------------------------------------------------------------------------------------------

    private val SERVICES_HEADING = Regex("""(?i)^(services|services performed|work performed|repairs performed|work done|jobs|repairs)$""")
    private val RECOMMENDED_HEADING = Regex(
        """(?i)^(recommended( services| work| repairs| maintenance)?|recommendations|declined( services| work| repairs)?|deferred( work| services)?|""" +
            """suggested( services| work| maintenance)?|future (work|services|recommendations)|past recommendations|needs attention|""" +
            """items? needing attention|advised (work|services))$""",
    )
    private val ATTACHMENTS_HEADING = Regex("""(?i)^(attached )?(images|photos|pictures|attachments)$""")

    /** "Approved by phone, 1:44pm 1/7/26": work the owner said yes to, which the invoice has as a service. */
    private val APPROVED = Regex("""(?i)^approved\b""")

    /**
     * An inspection or health report (#572). "Vehicle health report" is also a service's name, so outside an explicit
     * "inspection" heading it starts a report only after the totals.
     */
    private val INSPECTION_HEADING = Regex(
        """(?i)^(inspection (report|results)|(vehicle |multi-?point |digital (vehicle )?|courtesy )inspection( report| results)?|""" +
            """(vehicle )?health (report|check))$""",
    )

    /** "Written by … at 01/07/2026 08:16 PM": who wrote an inspection item up, not what was found. */
    private val WRITTEN_BY = Regex("""(?i)^(written|inspected|performed|completed) by\b""")

    /** A note in brackets after an item's name ("Axles (Driveshaft if equipped)"): what it covers, not its name. */
    private val TRAILING_NOTE = Regex("""\s*\([^()]*\)\s*$""")

    /** An icon font's glyph (a private-use character) printed before an item's name. */
    private fun plain(text: String): String = text.filterNot { it in ''..'' }.trim()

    private val TERMS_HEADING =Regex("""(?i)^(terms( of service| and conditions| & conditions)?|authorization|customer authorization|warranty|disclaimer)$""")

    private fun heading(text: String): String = text.trim().trimEnd(':').trim()

    private val COLUMN_WORDS = mapOf(
        "labor" to InvoiceFigure.LABOR, "labour" to InvoiceFigure.LABOR, "parts" to InvoiceFigure.PARTS, "sublet" to InvoiceFigure.SUBLET,
        "fees" to InvoiceFigure.FEES, "total" to InvoiceFigure.SUBTOTAL, "amount" to InvoiceFigure.SUBTOTAL,
    )

    // ---- Reading -----------------------------------------------------------------------------------------------

    private class Fig(val figure: InvoiceFigure, val cents: Long, val line: Int, val job: Int?) {
        /** A discount takes away, however it was printed. */
        val signed: Long get() = if (figure == InvoiceFigure.DISCOUNT) -abs(cents) else cents
    }

    private class Row(val line: Int, val quantity: BigDecimal, val quantityText: String, val each: Fig, val total: Fig)

    private enum class Mode { DESCRIPTION, LABOR, PARTS }

    private class JobB(val id: Int, val number: Int?, val line: Int, val wording: String) {
        val description = mutableListOf<String>()
        val labor = mutableListOf<String>()
        val figures = mutableMapOf<InvoiceFigure, Fig>()
        val conflicted = mutableSetOf<InvoiceFigure>()
        val extras = mutableListOf<Fig>()
        val rows = mutableListOf<Row>()
        var mode = Mode.DESCRIPTION
    }

    private enum class Zone { BEFORE, SERVICES, AFTER }

    private enum class NumberStyle { APART, MARKED }

    private val NUMBER = Regex("""^#?(\d{1,2})([.):]?)$""")
    private val NUMBER_WORDS = setOf("job", "line", "service", "op", "item")

    private class Column(val figure: InvoiceFigure, val centre: Float)

    private class Held(val text: String, val approved: Boolean, val kept: Boolean)

    /** An inspection report's item being read: its name, its mark's colour, what was found and the work under it. */
    private class Item(val line: Int, val area: String, val colour: FindingColour?) {
        val found = mutableListOf<String>()
        val recommendations = mutableListOf<Int>()
        var approved = false
        var foundIndent: Float? = null
    }

    private class Check(val kind: InvoiceCheck, val job: Int?, val terms: List<Fig>, val total: Fig) {
        val sum: Long get() = terms.sumOf { it.signed }
        val agrees: Boolean get() = sum == total.cents
    }

    private class Reading(
        val words: List<InvoiceWord>,
        val today: LocalDate,
        val readings: List<OdometerReading>,
        val unit: DistanceUnit,
        val rules: List<CatalogRule>,
        marks: List<InvoiceMark>,
    ) {
        /** The marks that read as a colour; a report with none is read without colours. */
        val coloured = marks.mapNotNull { m -> FindingColour.of(m.rgb)?.let { m to it } }
        val findings = mutableListOf<InvoiceFinding>()
        var inspecting = false
        var item: Item? = null

        /** Where an inspection report's items start on their lines, once one is read. */
        var itemIndent: Float? = null
        val trace = mutableListOf<InvoiceStep>()
        val jobs = mutableListOf<JobB>()
        val totals = mutableMapOf<InvoiceFigure, MutableList<Fig>>()
        val grandTotals = mutableListOf<Fig>()
        val recommendations = mutableListOf<InvoiceRecommendation>()
        val checks = mutableListOf<Check>()
        val products = mutableListOf<Pair<Row, Boolean>>()

        var zone = Zone.BEFORE
        var recommending = false
        var inTerms = false
        var table: List<Column>? = null
        var job: JobB? = null
        var nextNumber = 1
        var sawServices = false
        var numberStyle: NumberStyle? = null

        /** The line just read after the services, unpriced, in case the next line finishes its wrapped title. */
        var held: Held? = null

        /** Before any services: lines of text with one price beside them, and whether the totals have begun. */
        val loose = mutableListOf<Pair<Line, String>>()
        val looseCents = mutableListOf<Long>()
        var totalsBegun = false
        val invoiceSubtotals = mutableListOf<Fig>()

        fun run(): InvoiceReading {
            val all = buildLines(words)
            val skipped = repeated(all)
            val lines = all.filter { it.index !in skipped }
            all.filter { it.index in skipped }.forEach { trace += InvoiceStep.RepeatedLineSkipped(it.index) }

            val zoneOf = mutableMapOf<Int, Zone>()
            var page = lines.firstOrNull()?.page
            for (line in lines) {
                if (line.page != page) {
                    page = line.page
                    inTerms = false
                }
                zoneOf[line.index] = zone
                take(line)
            }
            finishItem()
            if (!sawServices) looseJobs()
            if (!sawServices) trace += InvoiceStep.NoServices

            val date = date(lines)
            val shop = shop(lines.filter { zoneOf[it.index] == Zone.BEFORE && it.page == lines.first().page })
            val vin = vin(lines)
            val (odoIn, odoOut) = odometer(lines, date)
            val vouched = vouch()
            val grand = grandTotal(vouched)
            val out = jobs.map { finish(it, vouched) }
            return InvoiceReading(
                date = date,
                shop = shop,
                vin = vin,
                odometerIn = odoIn,
                odometerOut = odoOut,
                grandTotalCents = grand,
                jobs = out,
                recommendations = recommendations,
                lines = all.map { l -> InvoiceLine(l.page, l.segs.map { it.text }) },
                trace = trace,
                findings = findings,
            )
        }

        // -- One line, through the sections --

        fun take(line: Line) {
            val previous = held
            held = null
            val (rest, labelled) = peelLine(line)
            val restText = rest.map { ws -> ws.joinToString(" ") { it.text } }
            val only = restText.singleOrNull()?.let(::heading)

            // The totals end the services: a grand total, or a figure labelled a total of everything.
            val endsServices = labelled.any { it.label in ENDING && !(it.label == Label.TOTAL && recommending) }
            if (zone == Zone.SERVICES && endsServices) endServices(line, InvoiceSection.TOTALS)
            // The totals end a list of recommendations too.
            if (endsServices) recommending = false

            val heading = only?.let(::plain)
            val inspection = heading != null && INSPECTION_HEADING.matches(heading) &&
                (zone == Zone.AFTER || heading.contains("inspection", ignoreCase = true))
            // Any other heading ends an inspection report, and so does the end of the services.
            val endsReport = heading != null && listOf(SERVICES_HEADING, RECOMMENDED_HEADING, ATTACHMENTS_HEADING, TERMS_HEADING).any { it.matches(heading) }
            if (inspecting && (endsReport || endsServices)) {
                finishItem()
                inspecting = false
            }
            when {
                inspection -> {
                    if (zone == Zone.SERVICES) endServices(line, null)
                    finishItem()
                    inspecting = true
                    recommending = false
                    inTerms = false
                    trace += InvoiceStep.SectionStarted(line.index, InvoiceSection.INSPECTION)
                    return figuresOnly(line, labelled)
                }
                only != null && SERVICES_HEADING.matches(only) && !sawServices -> {
                    recommending = false
                    inTerms = false
                    startServices(line, InvoiceSection.SERVICES)
                    return figuresOnly(line, labelled)
                }
                only != null && RECOMMENDED_HEADING.matches(only) -> {
                    if (zone == Zone.SERVICES) endServices(line, null)
                    recommending = true
                    inTerms = false
                    trace += InvoiceStep.SectionStarted(line.index, InvoiceSection.RECOMMENDED)
                    return figuresOnly(line, labelled)
                }
                // Photos attached after the recommendations end them: their captions are no work.
                only != null && ATTACHMENTS_HEADING.matches(only) -> {
                    if (zone == Zone.SERVICES) endServices(line, null)
                    recommending = false
                    inTerms = true
                    return figuresOnly(line, labelled)
                }
                only != null && TERMS_HEADING.matches(only) -> {
                    if (zone == Zone.SERVICES) endServices(line, null)
                    if (zone == Zone.BEFORE && sawServices) zone = Zone.AFTER
                    inTerms = true
                    recommending = false
                    trace += InvoiceStep.SectionStarted(line.index, InvoiceSection.TERMS)
                    return figuresOnly(line, labelled)
                }
            }
            // Inside a numbered service, "PARTS  QTY  EACH  TOTAL" is that service's parts table, read as part rows.
            val partsOfService = job != null && table == null
            if (labelled.isEmpty() && (zone == Zone.SERVICES || (zone == Zone.BEFORE && !recommending && !inTerms))) {
                tableHeader(line)?.takeUnless { cols -> partsOfService && cols.none { it.figure == InvoiceFigure.LABOR } }?.let { columns ->
                    if (zone == Zone.BEFORE) startServices(line, InvoiceSection.LINE_TABLE) else trace += InvoiceStep.SectionStarted(line.index, InvoiceSection.LINE_TABLE)
                    job = null
                    table = columns
                    return
                }
            }
            // A line with a subtotal and some words is a service, heading or not.
            if (zone == Zone.BEFORE && labelled.any { it.label == Label.SUBTOTAL } && restText.any(::hasLetters)) startServices(line, InvoiceSection.SERVICES)

            when (zone) {
                // Recommendations or terms before any services (a summary box on the first page) are read as they are after.
                Zone.BEFORE -> if (inspecting) {
                    inspectionLine(line, rest, labelled, previous)
                } else if (recommending || inTerms) {
                    afterLine(line, rest, labelled, previous)
                } else {
                    figuresOnly(line, labelled)
                    if (labelled.any { it.label in ENDING || it.label == Label.SUBTOTAL }) totalsBegun = true
                    if (!totalsBegun && labelled.isEmpty()) looseLine(line, rest)
                }
                Zone.SERVICES -> if (table != null) tableLine(line, rest, labelled) else serviceLine(line, rest, labelled)
                Zone.AFTER -> if (inspecting) inspectionLine(line, rest, labelled, previous) else afterLine(line, rest, labelled, previous)
            }
        }

        // -- An inspection report (#572) --

        /** The colour of the mark at the start of [line], if it has one: the item's on that line. */
        fun markOf(line: Line): FindingColour? {
            val ws = line.words
            val top = ws.minOf { it.top } - line.height / 2
            val bottom = ws.maxOf { it.bottom } + line.height / 2
            val start = ws.minOf { it.left } + 2 * line.height
            return coloured.firstOrNull { (m, _) ->
                val size = maxOf(m.right - m.left, m.bottom - m.top)
                m.page == line.page && (m.top + m.bottom) / 2 in top..bottom && m.left <= start && size <= 3 * line.height
            }?.second
        }

        /**
         * A line of an inspection report. With marks, a line with one is an item ("Axles"); without, a line where the
         * items start is. Under an item, what is in line with its first indented line is what was found ("Dr side axle
         * boot torn"); priced or approved work, further in, is a recommendation as anywhere after the totals.
         */
        fun inspectionLine(line: Line, rest: List<List<InvoiceWord>>, labelled: List<Labelled>, previous: Held?) {
            val ws = rest.flatten().filter { plain(it.text).isNotEmpty() }
            val text = plain(ws.joinToString(" ") { it.text })
            if (ws.isEmpty() || WRITTEN_BY.containsMatchIn(text)) return figuresOnly(line, labelled)
            val left = ws.first().left
            val priced = ws.any { money(it.text) != null }
            val approved = APPROVED.containsMatchIn(text) || rest.any { seg -> APPROVED.containsMatchIn(seg.joinToString(" ") { it.text }) }
            val atItems = itemIndent?.let { abs(left - it) <= line.height / 2 } ?: true
            val mark = markOf(line)
            val isItem = if (coloured.isNotEmpty()) mark != null else atItems && !priced && !approved && labelled.isEmpty()
            if (isItem) {
                finishItem()
                if (itemIndent == null) itemIndent = left
                val area = text.replace(TRAILING_NOTE, "").trim().ifEmpty { text }
                item = Item(line.index, area, mark)
                return figuresOnly(line, labelled)
            }
            // A line where the items start with no mark beside it, in a report with marks, or one further out than the
            // items: a heading of its own.
            val outside = itemIndent?.let { left < it - line.height / 2 } ?: false
            if (((atItems && itemIndent != null) || outside) && !priced && !approved) {
                finishItem()
                return figuresOnly(line, labelled)
            }
            val current = item
            if (current != null && !priced && !approved && hasLetters(text)) {
                val indent = current.foundIndent ?: left.also { current.foundIndent = it }
                if (abs(left - indent) <= line.height / 2) {
                    current.found += text
                    held = null
                    return figuresOnly(line, labelled)
                }
            }
            if (current != null && approved) current.approved = true
            val before = recommendations.size
            // A title wrapped above its price ("Remove & Replace Spark Plugs" over "$739.96 +tax"): the price's wording.
            val wording = ws.filter { money(it.text) == null && norm(it.text) !in PRICE_WORDS && it.text != "$" }.joinToString(" ") { it.text }
            if (priced && !hasLetters(wording) && previous != null && !previous.approved && !approved) {
                val cents = ws.firstNotNullOf { money(it.text) }
                recommendations += InvoiceRecommendation(line.index, previous.text, cents)
                trace += InvoiceStep.RecommendationKept(line.index, previous.text, cents)
            } else {
                afterLine(line, rest, labelled, previous)
            }
            if (current != null) current.recommendations += (before until recommendations.size)
        }

        /** The item being read is done: its finding goes on the work under it, or stands as a finding of its own. */
        fun finishItem() {
            val done = item ?: return
            item = null
            val found = done.found.fold("") { acc, t ->
                when {
                    acc.isEmpty() -> t
                    acc.last() in ".!?;:," || !t.first().isUpperCase() -> "$acc $t"
                    else -> "$acc. $t"
                }
            }.ifEmpty { null }
            trace += InvoiceStep.InspectionItem(done.line, done.area, found, done.colour)
            val flagged = done.colour?.takeIf { it != FindingColour.GREEN }
            done.recommendations.forEach { i -> recommendations[i] = recommendations[i].copy(area = done.area, finding = found, colour = flagged) }
            // Approved work under it was done on this visit: what it was found for is no longer open.
            if (done.recommendations.isNotEmpty() || done.approved) return
            val listed = if (coloured.isNotEmpty()) flagged != null else found != null
            if (listed) findings += InvoiceFinding(done.line, done.area, found, flagged)
        }

        fun startServices(line: Line, section: InvoiceSection) {
            zone = Zone.SERVICES
            sawServices = true
            trace += InvoiceStep.SectionStarted(line.index, section)
        }

        fun endServices(line: Line, section: InvoiceSection?) {
            zone = Zone.AFTER
            job = null
            table = null
            section?.let { trace += InvoiceStep.SectionStarted(line.index, it) }
        }

        /** Figures outside the services: the totals block's, read wherever it is. */
        fun figuresOnly(line: Line, labelled: List<Labelled>) {
            labelled.forEach { l ->
                val figure = when (l.label) {
                    Label.GRAND_TOTAL -> InvoiceFigure.GRAND_TOTAL
                    // A bare "total" under recommendations is theirs.
                    Label.TOTAL -> if (recommending) null else InvoiceFigure.GRAND_TOTAL
                    Label.BALANCE -> InvoiceFigure.BALANCE_DUE
                    Label.PAID -> InvoiceFigure.PAID
                    Label.TOTAL_LABOR, Label.LABOR -> InvoiceFigure.TOTAL_LABOR
                    Label.TOTAL_PARTS, Label.PARTS -> InvoiceFigure.TOTAL_PARTS
                    Label.TOTAL_FEES, Label.FEES -> InvoiceFigure.TOTAL_FEES
                    Label.TOTAL_SUBLETS, Label.SUBLET -> InvoiceFigure.TOTAL_SUBLETS
                    Label.SUPPLIES -> InvoiceFigure.SUPPLIES
                    Label.TAX -> InvoiceFigure.TAX
                    Label.CHARGE -> InvoiceFigure.OTHER_CHARGE
                    Label.DISCOUNT -> InvoiceFigure.DISCOUNT
                    // A subtotal of the whole invoice is made of the figures beside it, which are read instead; it only
                    // vouches for a list of priced lines with no services heading.
                    Label.SUBTOTAL -> {
                        if (!sawServices) invoiceSubtotals += Fig(InvoiceFigure.SUBTOTAL, l.cents, line.index, null)
                        null
                    }
                } ?: return@forEach
                val fig = Fig(figure, l.cents, line.index, null)
                trace += InvoiceStep.FigureRead(line.index, figure, l.cents, null)
                when (figure) {
                    InvoiceFigure.GRAND_TOTAL -> grandTotals += fig
                    InvoiceFigure.BALANCE_DUE, InvoiceFigure.PAID -> Unit
                    else -> totals.getOrPut(figure) { mutableListOf() } += fig
                }
            }
        }

        // -- Priced lines with no heading --

        /** "Full Synthetic Oil Change      $79.99": text with one price at its right, kept in case no services are found. */
        fun looseLine(line: Line, rest: List<List<InvoiceWord>>) {
            val ws = rest.flatten()
            val priced = ws.filter { money(it.text) != null }
            if (priced.size != 1 || ws.last() !== priced.single() || rest.size < 2) return
            val text = ws.dropLast(1).joinToString(" ") { it.text }.trim()
            if (!hasLetters(text) || DATE.containsMatchIn(text)) return
            loose += line to text
            looseCents += money(priced.single().text)!!
        }

        /**
         * With no services found, the priced lines before the totals are the services, **but only when their prices add
         * up** to the invoice's subtotal or, with its charges, to the grand total: a deductible or a balance forward
         * beside some text is not a job. A price there is the line's whole price, neither labor nor parts.
         */
        fun looseJobs() {
            if (loose.isEmpty()) return
            val figs = loose.indices.map { Fig(InvoiceFigure.SUBTOTAL, looseCents[it], loose[it].first.index, null) }
            val charges = listOf(InvoiceFigure.SUPPLIES, InvoiceFigure.OTHER_CHARGE, InvoiceFigure.DISCOUNT, InvoiceFigure.TAX).mapNotNull { f -> totals[f]?.singleOrNull() }
            val sum = figs.sumOf { it.cents }
            val agrees = invoiceSubtotals.any { it.cents == sum } || grandTotals.any { it.cents == sum + charges.sumOf { c -> c.signed } }
            if (!agrees) return
            sawServices = true
            trace += InvoiceStep.SectionStarted(loose.first().first.index, InvoiceSection.SERVICES)
            loose.forEachIndexed { i, (line, text) ->
                startJob(line, null, text)
                val j = jobs.last()
                trace += InvoiceStep.FigureRead(line.index, InvoiceFigure.SUBTOTAL, looseCents[i], j.id)
                j.figures[InvoiceFigure.SUBTOTAL] = Fig(InvoiceFigure.SUBTOTAL, looseCents[i], line.index, j.id)
            }
        }

        // -- Numbered services --

        fun serviceLine(line: Line, rest: List<List<InvoiceWord>>, labelled: List<Labelled>) {
            val hasSubtotal = labelled.any { it.label == Label.SUBTOTAL }
            val first = rest.firstOrNull()
            val all = rest.flatten()
            // "Job 1", "Line 1": the word before the number.
            val k = if (all.size > 2 && norm(all[0].text) in NUMBER_WORDS) 1 else 0
            val match = all.getOrNull(k)?.let { NUMBER.matchEntire(it.text) }
            val number = match?.groupValues?.get(1)?.toInt()
            // How the number is set apart: its own segment or printed large, or marked ("1.", "#1", "Job 1"). Every service
            // is numbered the way the first was, so a description listing "2. Replaced…" is not service 2.
            val style = when {
                first == null || match == null -> null
                k == 0 && (first.size == 1 || (first.first().bottom - first.first().top) >= line.height * BIG) -> NumberStyle.APART
                k == 1 || match.groupValues[2].isNotEmpty() || all[k].text.startsWith("#") -> NumberStyle.MARKED
                else -> null
            }
            val numbered = number != null && number == nextNumber && (style != null || hasSubtotal) &&
                (numberStyle == null || style == numberStyle || hasSubtotal) && all.drop(k + 1).any { hasLetters(it.text) }
            val current = job
            val words = if (numbered) all.drop(k + 1) else all
            val wording = words.filterNot { money(it.text) != null }.joinToString(" ") { it.text }.trim().trimEnd(':').trim()
            val subtotalStarts = hasSubtotal && hasLetters(wording) &&
                (current == null || InvoiceFigure.SUBTOTAL in current.figures || InvoiceFigure.SUBTOTAL in current.conflicted)
            if (numbered || subtotalStarts) {
                if (numbered) {
                    nextNumber++
                    if (numberStyle == null) numberStyle = style
                }
                startJob(line, if (numbered) number else null, wording)
            } else {
                // Text under the heading before the first service is no service's.
                val j = current ?: return
                val row = if (j.mode == Mode.PARTS) partRow(line, j, rest.flatten()) else null
                if (row != null) addRow(line, j, row) else rest.forEach { ws -> serviceText(line, j, ws) }
            }
            labelled.forEach { jobFigure(line, it) }
        }

        fun startJob(line: Line, number: Int?, wording: String) {
            val j = JobB(jobs.size, number, line.index, wording)
            jobs += j
            job = j
            trace += InvoiceStep.JobStarted(line.index, j.id, number, wording)
        }

        fun serviceText(line: Line, j: JobB, ws: List<InvoiceWord>) {
            val text = ws.joinToString(" ") { it.text }
            when (heading(text).lowercase()) {
                "labor", "labour" -> { j.mode = Mode.LABOR; return }
                "parts", "parts used" -> { j.mode = Mode.PARTS; return }
            }
            if (j.mode == Mode.PARTS) {
                // A row was tried on the whole line; this is the table's own header ("QTY EACH TOTAL") or a note.
                return
            }
            if (!hasLetters(text)) return
            if (j.mode == Mode.LABOR) j.labor += text else j.description += text
        }

        fun addRow(line: Line, j: JobB, row: Row) {
            j.rows += row
            trace += InvoiceStep.PartRow(line.index, j.id, row.quantityText, row.each.cents, row.total.cents)
        }

        /** "Spark Plug PFR7S8EG 4.0 $44.49 $177.96": the last three are quantity, each and total. */
        fun partRow(line: Line, j: JobB, ws: List<InvoiceWord>): Row? {
            if (ws.size < 4) return null
            val total = money(ws[ws.size - 1].text) ?: return null
            val each = money(ws[ws.size - 2].text) ?: return null
            val q = ws[ws.size - 3].text
            if (!QUANTITY.matches(q) || ws.dropLast(3).none { hasLetters(it.text) }) return null
            return Row(line.index, BigDecimal(q), q, Fig(InvoiceFigure.PART_EACH, each, line.index, j.id), Fig(InvoiceFigure.PART_TOTAL, total, line.index, j.id))
        }

        fun jobFigure(line: Line, l: Labelled) {
            val j = job ?: return
            val figure = when (l.label) {
                Label.SUBTOTAL -> InvoiceFigure.SUBTOTAL
                Label.LABOR -> InvoiceFigure.LABOR
                Label.PARTS -> InvoiceFigure.PARTS
                Label.FEES -> InvoiceFigure.FEES
                Label.SUBLET -> InvoiceFigure.SUBLET
                Label.SUPPLIES -> InvoiceFigure.SUPPLIES
                Label.TAX -> InvoiceFigure.TAX
                Label.CHARGE -> InvoiceFigure.OTHER_CHARGE
                Label.DISCOUNT -> InvoiceFigure.DISCOUNT
                else -> return
            }
            trace += InvoiceStep.FigureRead(line.index, figure, l.cents, j.id)
            addJobFigure(j, Fig(figure, l.cents, line.index, j.id))
        }

        fun addJobFigure(j: JobB, fig: Fig) {
            if (fig.figure in EXTRAS) { j.extras += fig; return }
            if (fig.figure in j.conflicted) return
            val had = j.figures[fig.figure]
            when {
                had == null -> j.figures[fig.figure] = fig
                had.cents != fig.cents -> {
                    j.figures -= fig.figure
                    j.conflicted += fig.figure
                    trace += InvoiceStep.FigureNotTaken(fig.figure, j.id, fig.cents, FigureLeftOut.READ_TWICE)
                }
            }
        }

        // -- A table of work --

        fun tableHeader(line: Line): List<Column>? {
            val cols = line.words.mapNotNull { w -> COLUMN_WORDS[norm(w.text)]?.let { Column(it, (w.left + w.right) / 2) } }
            if (cols.size < 2 || cols.none { it.figure == InvoiceFigure.LABOR || it.figure == InvoiceFigure.PARTS }) return null
            if (line.words.any { money(it.text) != null }) return null
            return cols.distinctBy { it.figure }
        }

        fun tableLine(line: Line, rest: List<List<InvoiceWord>>, labelled: List<Labelled>) {
            val columns = table ?: return
            val ws = rest.flatten()
            val priced = ws.filter { money(it.text) != null }
            val text = ws.filter { money(it.text) == null }.dropWhile { Regex("""^[A-Z0-9][.)]?$""").matches(it.text) && ws.size > 1 }
            if (priced.isEmpty() || text.none { hasLetters(it.text) }) {
                labelled.forEach { jobFigure(line, it) }
                return
            }
            val wording = text.joinToString(" ") { it.text }
            startJob(line, null, wording)
            val j = job ?: return
            priced.forEach { w ->
                val centre = (w.left + w.right) / 2
                val col = columns.minByOrNull { abs(it.centre - centre) } ?: return@forEach
                val cents = money(w.text) ?: return@forEach
                trace += InvoiceStep.FigureRead(line.index, col.figure, cents, j.id)
                addJobFigure(j, Fig(col.figure, cents, line.index, j.id))
            }
            labelled.forEach { jobFigure(line, it) }
        }

        // -- After the services --

        fun afterLine(line: Line, rest: List<List<InvoiceWord>>, labelled: List<Labelled>, previous: Held?) {
            figuresOnly(line, labelled)
            if (inTerms) return
            val approved = rest.any { seg -> APPROVED.containsMatchIn(seg.joinToString(" ") { it.text }) }
            val ws = rest.filterNot { seg -> APPROVED.containsMatchIn(seg.joinToString(" ") { it.text }) }.flatten()
            val priced = ws.mapNotNull { money(it.text) }
            fun wording(of: List<InvoiceWord>) = of.filter { money(it.text) == null && norm(it.text) !in PRICE_WORDS && it.text != "$" }.joinToString(" ") { it.text }.trim()
            // What follows the price is the price's ("$58.33 +taxes and fees"); the wording is what comes before it.
            val beforePrice = wording(ws.takeWhile { money(it.text) == null })
            val text = if (priced.isNotEmpty() && hasLetters(beforePrice)) beforePrice else wording(ws)
            if (!hasLetters(text)) return
            // "and fees" wrapped from the price on the line above is that price's, not a recommendation.
            if (priced.isEmpty() && recommendations.isNotEmpty() && PRICE_WRAP.matches(text)) return
            // A title wrapped onto this line ("Sides )" under "… ( Front, Both") finishes the line above, which takes
            // this line's price and keeps its own approval.
            val wraps = previous != null && priced.isNotEmpty() && text.count { it == ')' } > text.count { it == '(' }
            if (wraps && previous.kept) {
                recommendations.removeAt(recommendations.lastIndex)
                trace.removeAt(trace.indexOfLast { it is InvoiceStep.RecommendationKept })
            }
            val whole = if (wraps) "${previous.text} $text" else text
            // Approved work is the invoice's own service: the owner said yes to it, so it is no recommendation.
            if (approved || (wraps && previous.approved)) {
                if (priced.isEmpty()) held = Held(text, approved = true, kept = false)
                return
            }
            // Under a recommendations heading, every line; after the totals with none, the priced ones (an inspection report's).
            val keep = recommending || priced.isNotEmpty()
            if (keep) {
                val cents = priced.firstOrNull()
                recommendations += InvoiceRecommendation(line.index, whole, cents)
                trace += InvoiceStep.RecommendationKept(line.index, whole, cents)
            }
            if (priced.isEmpty()) held = Held(text, approved = false, kept = keep)
        }

        // -- The sums --

        fun vouch(): Set<Fig> {
            jobs.forEach { j ->
                j.rows.forEach { r ->
                    val exact = r.quantity.multiply(BigDecimal(r.each.cents))
                    val ok = r.total.cents in listOf(exact.setScale(0, RoundingMode.FLOOR).toLong(), exact.setScale(0, RoundingMode.CEILING).toLong())
                    products += r to ok
                    trace += InvoiceStep.SumChecked(InvoiceCheck.PART_ROW, j.id, exact.setScale(0, RoundingMode.HALF_UP).toLong(), r.total.cents, ok, listOf(r.line))
                }
                j.figures[InvoiceFigure.PARTS]?.let { parts -> if (j.rows.isNotEmpty()) checks += Check(InvoiceCheck.JOB_PARTS, j.id, j.rows.map { it.total }, parts) }
                j.figures[InvoiceFigure.SUBTOTAL]?.let { sub ->
                    val terms = listOf(InvoiceFigure.LABOR, InvoiceFigure.PARTS, InvoiceFigure.FEES, InvoiceFigure.SUBLET).mapNotNull { j.figures[it] } + j.extras
                    if (terms.isNotEmpty()) checks += Check(InvoiceCheck.JOB_SUBTOTAL, j.id, terms, sub)
                }
            }
            val block = totals.mapNotNull { (figure, figs) -> figs.takeIf { f -> f.map { it.cents }.distinct().size == 1 }?.first()?.let { figure to it } }.toMap()
            totals.filter { (_, figs) -> figs.map { it.cents }.distinct().size > 1 }.forEach { (figure, figs) ->
                figs.forEach { trace += InvoiceStep.FigureNotTaken(figure, null, it.cents, FigureLeftOut.READ_TWICE) }
            }
            if (jobs.isNotEmpty()) {
                listOf(
                    Triple(InvoiceFigure.LABOR, InvoiceFigure.TOTAL_LABOR, InvoiceCheck.ALL_LABOR),
                    Triple(InvoiceFigure.PARTS, InvoiceFigure.TOTAL_PARTS, InvoiceCheck.ALL_PARTS),
                    Triple(InvoiceFigure.FEES, InvoiceFigure.TOTAL_FEES, InvoiceCheck.ALL_FEES),
                    Triple(InvoiceFigure.SUBLET, InvoiceFigure.TOTAL_SUBLETS, InvoiceCheck.ALL_SUBLETS),
                ).forEach { (each, total, kind) ->
                    val terms = jobs.mapNotNull { it.figures[each] }
                    val printed = block[total]
                    if (printed != null && terms.isNotEmpty()) checks += Check(kind, null, terms, printed)
                }
            }
            val charges = listOf(InvoiceFigure.SUPPLIES, InvoiceFigure.OTHER_CHARGE, InvoiceFigure.DISCOUNT, InvoiceFigure.TAX).mapNotNull { block[it] }
            val blockTerms = listOf(InvoiceFigure.TOTAL_LABOR, InvoiceFigure.TOTAL_PARTS, InvoiceFigure.TOTAL_FEES, InvoiceFigure.TOTAL_SUBLETS).mapNotNull { block[it] } + charges
            val subtotals = jobs.mapNotNull { it.figures[InvoiceFigure.SUBTOTAL] }
            if (jobs.isNotEmpty() && subtotals.size == jobs.size) invoiceSubtotals.forEach { checks += Check(InvoiceCheck.SUBTOTALS, null, subtotals, it) }
            grandTotals.forEach { gt ->
                if (blockTerms.size >= 2) checks += Check(InvoiceCheck.TOTALS_BLOCK, null, blockTerms, gt)
                if (jobs.isNotEmpty() && subtotals.size == jobs.size) checks += Check(InvoiceCheck.SUBTOTALS, null, subtotals + charges, gt)
            }
            checks.forEach { c -> trace += InvoiceStep.SumChecked(c.kind, c.job, c.sum, c.total.cents, c.agrees, (c.terms.map { it.line } + c.total.line).distinct().sorted()) }
            val vouched = mutableSetOf<Fig>()
            checks.filter { it.agrees }.forEach { vouched += it.terms; vouched += it.total }
            products.filter { it.second }.forEach { (r, _) -> vouched += r.total; vouched += r.each }
            return vouched
        }

        fun grandTotal(vouched: Set<Fig>): Long? {
            val good = grandTotals.filter { it in vouched }.map { it.cents }.distinct()
            grandTotals.filter { it !in vouched }.forEach { trace += InvoiceStep.FigureNotTaken(InvoiceFigure.GRAND_TOTAL, null, it.cents, FigureLeftOut.NO_SUM_AGREES) }
            val taken = good.singleOrNull() ?: return null
            trace += InvoiceStep.FigureTaken(InvoiceFigure.GRAND_TOTAL, null, taken)
            return taken
        }

        fun finish(j: JobB, vouched: Set<Fig>): InvoiceJob {
            fun take(figure: InvoiceFigure): Long? {
                val fig = j.figures[figure] ?: return null
                return if (fig in vouched) {
                    trace += InvoiceStep.FigureTaken(figure, j.id, fig.cents)
                    fig.cents
                } else {
                    trace += InvoiceStep.FigureNotTaken(figure, j.id, fig.cents, FigureLeftOut.NO_SUM_AGREES)
                    null
                }
            }
            val labor = take(InvoiceFigure.LABOR)
            val parts = take(InvoiceFigure.PARTS)
            val fees = take(InvoiceFigure.FEES)
            val sublet = take(InvoiceFigure.SUBLET)
            val subtotal = take(InvoiceFigure.SUBTOTAL)
            val laborWording = j.labor.joinToString(" ").takeIf { it.isNotBlank() }
            val filed = CatalogRules.match(j.wording, rules) ?: laborWording?.let { CatalogRules.match(it, rules) }
            val guesses = if (filed != null) emptyList() else LineGuess.guesses(j.wording).ifEmpty { laborWording?.let(LineGuess::guesses).orEmpty() }
            if (filed != null) trace += InvoiceStep.JobFiled(j.id, filed)
            if (guesses.isNotEmpty()) trace += InvoiceStep.JobGuessed(j.id, guesses)
            return InvoiceJob(
                line = j.line,
                number = j.number,
                wording = j.wording,
                laborWording = laborWording,
                description = j.description.joinToString(" ").takeIf { it.isNotBlank() },
                laborCents = labor,
                partsCents = parts,
                feesCents = fees,
                subletCents = sublet,
                subtotalCents = subtotal,
                filed = filed,
                guesses = guesses,
            )
        }

        // -- The vehicle, the visit and the shop --

        fun date(lines: List<Line>): LocalDate? {
            for (line in lines) {
                for (m in DATE.findAll(line.text)) {
                    val g = m.groupValues
                    val day = try {
                        if (g[1].isNotEmpty()) LocalDate.of(g[1].toInt(), g[2].toInt(), g[3].toInt()) else LocalDate.of(g[6].toInt(), g[4].toInt(), g[5].toInt())
                    } catch (_: DateTimeException) {
                        continue
                    }
                    if (day.isAfter(today)) {
                        trace += InvoiceStep.DateNotTaken(line.index, day)
                        return null
                    }
                    trace += InvoiceStep.DateTaken(line.index, day)
                    return day
                }
            }
            return null
        }

        /** The first words at the top of the first page that read as a name: not an address, a phone, a web or mail address, or a label. */
        fun shop(lines: List<Line>): String? {
            for (line in lines) for (seg in line.segs) {
                val text = seg.text.trim()
                val confident = seg.words.mapNotNull { it.confidence }.let { it.isEmpty() || it.average() >= 60 }
                val letters = text.count { it.isLetter() }
                val plain = text.filterNot { it.isWhitespace() }
                if (!confident || letters < 4 || letters * 10 < plain.length * 6) continue
                if (text.first().isDigit() || '@' in text || '$' in text || Regex("""(?i)www\.|\.com\b|\.example\b|https?:""").containsMatchIn(text)) continue
                if (Regex("""(?i)\b(invoice|page|repair order|estimate|date|customer|vin|odometer|mileage)\b|:""").containsMatchIn(text)) continue
                val name = text.take(HistoryImport.MAX_SHOP)
                trace += InvoiceStep.ShopTaken(line.index, name)
                return name
            }
            return null
        }

        /**
         * The VIN: a run of exactly 17 that passes its check digit, I, O and Q read as 1 and 0 as [VinScan] does.
         * On a line labelled VIN, a run that fails may have look-alikes folded (S and 5, B and 8, Z and 2, G and 6),
         * at most [MAX_FOLDS], and is taken only when exactly one way of folding the fewest passes.
         */
        fun vin(lines: List<Line>): String? {
            for (line in lines) {
                val labelled = Regex("""(?i)\bVIN\b""").containsMatchIn(line.text)
                for (seg in line.segs) {
                    val upper = seg.text.uppercase()
                    val runs = Regex("[A-Z0-9]+").findAll(upper).map { it.value }.toList() +
                        upper.replace(Regex("""\bVIN\b"""), " ").split(Regex("[^A-Z0-9 ]+")).map { it.replace(" ", "") }
                    for (run in runs.map(::asDigits).filter { it.length == 17 }.distinct()) {
                        if (Vin.hasValidCheckDigit(run)) {
                            trace += InvoiceStep.VinTaken(line.index, run, emptyList())
                            return run
                        }
                        if (!labelled) continue
                        val folded = fold(run)
                        if (folded != null) {
                            trace += InvoiceStep.VinTaken(line.index, folded.first, folded.second)
                            return folded.first
                        }
                        trace += InvoiceStep.VinNotTaken(line.index, run)
                    }
                }
            }
            return null
        }

        // -- The odometer --

        fun odometer(lines: List<Line>, date: LocalDate?): Pair<Int?, Int?> {
            val found = mutableMapOf<OdometerRole, Pair<Int, Int>>()
            val twice = mutableSetOf<OdometerRole>()
            for (line in lines) for (seg in line.segs) for (m in ODOMETER.findAll(seg.text) + (if (ODOMETER_WORD.containsMatchIn(line.text)) OUT_ALONE.findAll(seg.text) else emptySequence())) {
                val role = when (m.groupValues[2].lowercase()) {
                    "in" -> OdometerRole.IN
                    "out" -> OdometerRole.OUT
                    else -> OdometerRole.UNLABELLED
                }
                val value = m.groupValues[3].replace(",", "").toIntOrNull() ?: continue
                val had = found[role]
                if (had == null) found[role] = value to line.index else if (had.first != value) twice += role
            }
            twice.forEach { role -> found.remove(role)?.let { trace += InvoiceStep.OdometerNotTaken(it.second, role, it.first, OdometerLeftOut.ReadTwice) } }
            val floor = readings.filter { date == null || !it.date.isAfter(date) }.maxOfOrNull { it.odometer }
            val range = if (floor != null) floor..floor + unit.scheduled(DashReading.FAR_AHEAD) else 1..HistoryImport.MAX_ODOMETER
            val inRange = found.filter { (role, v) ->
                (v.first in range).also { ok -> if (!ok) trace += InvoiceStep.OdometerNotTaken(v.second, role, v.first, OdometerLeftOut.OutOfRange(range.first, range.last)) }
            }
            var odoIn = inRange[OdometerRole.IN] ?: inRange[OdometerRole.UNLABELLED]
            var odoOut = inRange[OdometerRole.OUT]
            val roleIn = if (inRange[OdometerRole.IN] != null) OdometerRole.IN else OdometerRole.UNLABELLED
            if (odoIn != null && odoOut != null) {
                val why = when {
                    odoOut.first < odoIn.first -> OdometerLeftOut.OutBelowIn
                    odoOut.first - odoIn.first > unit.scheduled(SHOP_DRIVE) -> OdometerLeftOut.OutFarPastIn
                    else -> null
                }
                if (why != null) {
                    trace += InvoiceStep.OdometerNotTaken(odoIn.second, roleIn, odoIn.first, why)
                    trace += InvoiceStep.OdometerNotTaken(odoOut.second, OdometerRole.OUT, odoOut.first, why)
                    odoIn = null
                    odoOut = null
                }
            }
            odoIn?.let { trace += InvoiceStep.OdometerTaken(it.second, roleIn, it.first) }
            odoOut?.let { trace += InvoiceStep.OdometerTaken(it.second, OdometerRole.OUT, it.first) }
            return odoIn?.first to odoOut?.first
        }
    }

    /** Labels that end the services: what follows is the totals block. */
    private val ENDING = setOf(Label.GRAND_TOTAL, Label.TOTAL, Label.TOTAL_LABOR, Label.TOTAL_PARTS, Label.TOTAL_FEES, Label.TOTAL_SUBLETS)

    /** A service's own charges besides labor, parts, fees and sublet, which still add to its subtotal. */
    private val EXTRAS = setOf(InvoiceFigure.SUPPLIES, InvoiceFigure.TAX, InvoiceFigure.OTHER_CHARGE, InvoiceFigure.DISCOUNT)

    /** Words beside a recommendation's price that are not its wording ("$141.99 +tax"). */
    private val PRICE_WORDS = setOf("+tax", "+ tax", "plus", "tax", "+", "ea", "each")

    /** The rest of a price's "+taxes and fees" wrapped onto a line of its own. */
    private val PRICE_WRAP = Regex("""(?i)^((and|&|\+|plus)\s*)?(tax(es)?|fees?)(\s*(and|&|\+)\s*(tax(es)?|fees?))?$""")

    /** Year-month-day, or month/day/year as a US shop prints it. A two-digit year is left out: which century is a guess. */
    private val DATE = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b|\b(\d{1,2})/(\d{1,2})/(\d{4})\b""")

    private val ODOMETER = Regex("""(?i)\b(odometer|odo|mileage|miles)\s*(in|out)?\s*[:#]?\s*(\d{1,3}(?:,\d{3})+|\d{1,7})(?![\w,.])""")

    private val ODOMETER_WORD = Regex("""(?i)\b(odometer|odo|mileage|miles)\b""")

    /** "Odometer In 45,880   Out 45,882": an "out" on an odometer's line, without the label again. */
    private val OUT_ALONE = Regex("""(?i)(?<!odometer |odo |mileage |miles |odometer|odo|mileage|miles)\b()(out)\s*[:#]?\s*(\d{1,3}(?:,\d{3})+|\d{1,7})(?![\w,.])""")

    private fun asDigits(run: String): String = run.replace('O', '0').replace('Q', '0').replace('I', '1')

    private val LOOK_ALIKES = mapOf('S' to '5', '5' to 'S', 'B' to '8', '8' to 'B', 'Z' to '2', '2' to 'Z', 'G' to '6', '6' to 'G')

    /**
     * [run] with the fewest look-alikes folded that makes it pass, when exactly one way does. A digit is only
     * read as a letter in positions 1–8, 10 and 11: the check digit (9) is a digit or X, and the serial (12–17)
     * is digits for any maker of a thousand vehicles a year.
     */
    private fun fold(run: String): Pair<String, List<VinFold>>? {
        val places = run.indices.filter { i ->
            val c = run[i]
            c in LOOK_ALIKES && (c.isLetter() || i in 0..7 || i in 9..10)
        }
        for (n in 1..MAX_FOLDS) {
            val passing = combinations(places, n).map { chosen ->
                val chars = run.toCharArray()
                chosen.forEach { chars[it] = LOOK_ALIKES.getValue(chars[it]) }
                String(chars) to chosen
            }.filter { Vin.hasValidCheckDigit(it.first) }.toList()
            if (passing.size == 1) return passing.single().let { (vin, chosen) -> vin to chosen.map { VinFold(it + 1, run[it], vin[it]) } }
            if (passing.size > 1) return null
        }
        return null
    }

    private fun combinations(items: List<Int>, n: Int): Sequence<List<Int>> = sequence {
        if (n == 0) { yield(emptyList()); return@sequence }
        items.forEachIndexed { i, x -> combinations(items.drop(i + 1), n - 1).forEach { yield(listOf(x) + it) } }
    }
}
