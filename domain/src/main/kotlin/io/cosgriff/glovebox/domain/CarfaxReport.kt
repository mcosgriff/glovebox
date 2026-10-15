package io.cosgriff.glovebox.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

/** One word of a PDF page, where it starts: [x] from the left, [y] from the top, in points. */
data class PdfWord(val page: Int, val x: Float, val y: Float, val text: String)

/**
 * Reads the service visits out of a Carfax vehicle history report (#160), into the same items a
 * `service-history` import file gives, so the Import page previews and writes them like any import.
 *
 * A report's history is a table: the date at the left, the mileage beside it, the source (who reported
 * it: a shop, a DMV) and, further right, the comments, one item per line ("Oil and filter changed").
 * Columns are found from the words' positions, not from fixed numbers: an entry starts on a line whose
 * first word is a date; a wide gap on a line separates the source from the comments.
 *
 * **Only service visits are taken**, and only what they say: an entry is a visit when one of its
 * comments is work (a known service, or a line saying something was replaced, changed, serviced,
 * inspected…); titles, registrations, ownership and damage reports are not. A comment becomes one of
 * Glovebox's service lines only where its wording is unmistakable ([services]); anything else is kept as
 * written. A visit with no mileage can't be a record and is counted, not guessed.
 */
object CarfaxReport {
    data class Reading(val records: List<ImportedRecord>, val withoutMileage: Int)

    private val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US)
    private val date = Regex("""\d{2}/\d{2}/\d{4}""")
    private val mileage = Regex("""\d{1,3}(,\d{3})*""")

    /** Lines whose words are this close vertically are one line. */
    private const val SAME_LINE = 3f

    /** A gap this wide between two words on a line is a gap between columns. */
    private const val COLUMN_GAP = 40f

    /** A vertical gap this tall ends an entry: what follows is a box or a summary, not more of it. */
    private const val ENTRY_GAP = 22f

    /**
     * The report as an import: [ImportFile.ServiceHistory] with its visits, or [ImportFile.Invalid]
     * saying why not. [today] is for refusing a date in the future, as an import file does.
     */
    fun read(words: List<PdfWord>, today: LocalDate): ImportFile {
        if (words.none { it.text.contains("CARFAX", ignoreCase = true) }) {
            return refuse("This doesn't look like a Carfax vehicle history report.")
        }
        val reading = visits(words.map { it.copy(text = repairLigatures(it.text)) }, today)
        if (reading.records.isEmpty()) {
            return refuse(
                "No service visits with a mileage were found in this report." +
                    if (reading.withoutMileage > 0) " ${reading.withoutMileage} had no mileage, so they can't be records." else "",
            )
        }
        return ImportFile.ServiceHistory(reading.records, DistanceUnit.MILES)
    }

    private fun refuse(message: String) = ImportFile.Invalid(listOf(ImportError("", message)))

    private data class Line(val page: Int, val y: Float, val words: List<PdfWord>)

    private fun lines(words: List<PdfWord>): List<Line> = words.groupBy { it.page }.toSortedMap().flatMap { (page, onPage) ->
        val sorted = onPage.sortedWith(compareBy({ it.y }, { it.x }))
        val out = mutableListOf<MutableList<PdfWord>>()
        sorted.forEach { w -> if (out.isNotEmpty() && abs(out.last().first().y - w.y) <= SAME_LINE) out.last() += w else out += mutableListOf(w) }
        out.map { l -> Line(page, l.first().y, l.sortedBy { it.x }) }
    }

    /** Splits a line at its wide gaps: the pieces, left to right, each a run of words. */
    private fun columns(line: Line): List<List<PdfWord>> {
        val out = mutableListOf(mutableListOf(line.words.first()))
        line.words.zipWithNext { a, b ->
            // A word's right edge isn't known; its length in average glyphs is close enough to find a wide gap.
            val end = a.x + a.text.length * 5.5f
            if (b.x - end > COLUMN_GAP) out += mutableListOf(b) else out.last() += b
        }
        return out
    }

    fun visits(words: List<PdfWord>, today: LocalDate): Reading {
        val all = lines(words)
        // Where an entry starts: a date at the left margin of the history, the same x on every page.
        val dateLines = all.filter { l -> date.matches(l.words.first().text) }
        val dateX = dateLines.groupingBy { (it.words.first().x / 4).toInt() }.eachCount().maxByOrNull { it.value }?.key ?: return Reading(emptyList(), 0)
        val entryStarts = dateLines.filter { (it.words.first().x / 4).toInt() == dateX }.toSet()
        // The date and mileage come off the front of an entry's first line; what is left starts with the source.
        fun afterDate(l: Line): List<PdfWord> = l.words.drop(1).let { w -> if (w.firstOrNull()?.let { mileage.matches(it.text) } == true) w.drop(1) else w }
        val sourceX = entryStarts.mapNotNull { afterDate(it).firstOrNull()?.x }.groupingBy { (it / 4).toInt() }.eachCount().maxByOrNull { it.value }?.key?.let { it * 4f }
            ?: return Reading(emptyList(), 0)
        // Where the comments start: the commonest left edge of a piece split off by a wide gap, right of the source.
        val pieces = entryStarts.flatMap { l -> columns(Line(l.page, l.y, afterDate(l).ifEmpty { return@flatMap emptyList() })).drop(1).map { it.first().x } } +
            all.filter { it !in entryStarts }.flatMap { l -> columns(l).drop(1).map { it.first().x } }
        val commentX = pieces.filter { it > sourceX + 60 }.groupingBy { (it / 8).toInt() }.eachCount().maxByOrNull { it.value }?.key
            ?.let { bucket -> pieces.filter { (it / 8).toInt() == bucket }.min() }

        val records = mutableListOf<ImportedRecord>()
        var withoutMileage = 0
        var i = 0
        while (i < all.size) {
            val start = all[i]
            if (start !in entryStarts) { i++; continue }
            val entry = mutableListOf(start)
            var j = i + 1
            while (j < all.size && all[j] !in entryStarts && all[j].page == start.page && all[j].y - entry.last().y <= ENTRY_GAP && !isFooter(all[j])) entry += all[j++]
            i = j

            val day = try { LocalDate.parse(start.words.first().text, dateFormat) } catch (_: DateTimeParseException) { continue }
            val miles = start.words.getOrNull(1)?.takeIf { mileage.matches(it.text) }?.text?.replace(",", "")?.toIntOrNull()
            val source = mutableListOf<String>()
            val comments = mutableListOf<String>()
            entry.forEachIndexed { n, line ->
                val words = if (n == 0) line.words.drop(if (miles != null) 2 else 1) else line.words
                words.forEach { w -> if (commentX != null && w.x >= commentX - 2) comments += w.text else source += w.text }
                comments += "\n"
                source += "\n"
            }
            val items = commentItems(comments.joinToString(" "))
            val work = items.filter(::isWork)
            if (work.isEmpty()) continue
            if (miles == null) { withoutMileage++; continue }
            if (day.isAfter(today) || miles > HistoryImport.MAX_ODOMETER) continue
            val shop = source.joinToString(" ").lines().map { it.trim() }.firstOrNull { it.isNotEmpty() }?.take(HistoryImport.MAX_SHOP)
            records += ImportedRecord(
                date = day,
                odometer = miles,
                // A report can name one job twice ("Four tires balanced", "Tire(s) balanced"): the first is the service line.
                work = work.take(HistoryImport.MAX_WORK).fold(listOf<ImportedWork>()) { done, item ->
                    val id = service(item)?.takeIf { s -> done.none { it.service == s } }
                    done + ImportedWork(item.take(HistoryImport.MAX_LABEL), id)
                },
                diy = false,
                shopName = shop,
            )
        }
        return Reading(records, withoutMileage)
    }

    /** The page's own small print ("…at www.carfax.com."), which can sit right under the last entry. */
    private fun isFooter(line: Line): Boolean = line.words.any { it.text.contains("carfax.com", ignoreCase = true) || it.text.contains("CARFAX") }

    /**
     * The comment column's items: one per line, a line starting with a small letter continuing the one
     * before ("Titled or registered as commercial" / "vehicle").
     */
    private fun commentItems(column: String): List<String> {
        val out = mutableListOf<String>()
        column.lines().map { tidy(it) }.filter { it.isNotEmpty() }.forEach { line ->
            if (out.isNotEmpty() && line.first().isLowerCase()) out[out.lastIndex] = out.last() + " " + line else out += line
        }
        return out
    }

    /**
     * One comment line, without the list bullets a printed report can leave in the text: a dash on its own,
     * or stuck to the first letter of the word it stands before ("V- ehicle serviced" is "Vehicle serviced").
     */
    private fun tidy(line: String): String = line.trim().replace(Regex("""\s+"""), " ")
        .replace(Regex("""^(-+\s*)+"""), "")
        .replace(Regex("""\b([A-Za-z])-+ ([a-z])"""), "$1$2")
        .replace(Regex("""(^|\s)-+(?=\s|$)"""), " ")
        .trim()

    private val workWords = Regex(
        """(?i)\b(replaced|changed|rotated|serviced|inspected|inspection|repaired|flushed|installed|performed|checked|aligned|balanced|""" +
            """adjusted|cleaned|lubricated|resurfaced|tested|mounted|recharged|topped off)\b""",
    )
    private val notWork = Regex("""(?i)\b(title|titled|registration|registered|owner|odometer|sold|for sale|auction|damage|accident|lien|recall)\b""")

    /** Whether a comment is work done on the vehicle, not a record about it. */
    fun isWork(item: String): Boolean = workWords.containsMatchIn(item) && !notWork.containsMatchIn(item)

    /**
     * The Log service line a comment is, where the wording leaves no doubt; null keeps it as written.
     * "Brakes checked" or "Brake pads replaced" (which axle?) stay as they are.
     */
    private val services: List<Pair<Regex, String>> = listOf(
        """oil and filter changed|oil changed""" to "oil",
        """cabin air filter""" to "cabin-air",
        """(engine )?air filter (replaced|changed)""" to "engine-air",
        """tires? rotated|tire rotation""" to "rotation",
        """(wheels?|tires?) balanced""" to "balance",
        """(wheels?|front end) aligned|alignment performed""" to "alignment",
        """tires? replaced|new tires? installed""" to "new-tires",
        """front brake pads replaced""" to "front-pads",
        """rear brake pads replaced""" to "rear-pads",
        """(front |rear )?brake rotors? (replaced|resurfaced)""" to "rotors",
        """brake fluid (flushed|changed|replaced)""" to "brake-fluid",
        """(coolant|cooling system|radiator) (flushed|changed|serviced)|antifreeze (flushed|changed)|antifreeze/coolant flushed""" to "coolant",
        """transmission (fluid|oil) (flushed|changed|replaced)|transmission serviced""" to "trans-fluid",
        """transfer case (fluid|oil)? ?(changed|serviced|flushed)""" to "transfer-case",
        """differential (fluid|oil)? ?(changed|serviced|flushed)|differentials? serviced""" to "diff-fluid",
        """spark plugs? replaced""" to "spark-plugs",
        """battery replaced|new battery installed""" to "battery",
        """wiper blades? replaced|wipers? replaced""" to "wipers",
        """(serpentine|drive|accessory) belts? replaced""" to "belt",
        """(light )?bulbs? replaced""" to "bulbs",
        """power steering (fluid )?(flushed|changed|serviced)""" to "power-steering",
        """maintenance inspection completed|multi-point inspection""" to "multi-point",
        """emissions? (inspection|test)""" to "emissions",
    ).map { (pattern, id) -> Regex("""(?i)\b($pattern)\b""") to id }

    /** Carfax writes "Tire(s) replaced"; the patterns are written for "Tires replaced". */
    fun service(item: String): String? {
        val plain = item.replace("(s)", "s")
        return services.firstOrNull { (re, _) -> re.containsMatchIn(plain) }?.second
    }

    private val ligatureWords = listOf("filter", "fluid", "flushed", "verified", "certified", "refilled", "filled", "inflated", "flat", "first", "final", "fixed", "field", "fleet", "flex")

    /**
     * Some PDF writers name the "fi" and "fl" glyphs as other letters: a report printed to PDF on a Mac
     * reads "Oil and clter changed", "Brake guid gushed", "Springceld". Only the words a service line or
     * a place name is made of are put back; anything else is left as it reads.
     */
    private val misnamed: List<Pair<Regex, String>> = listOf(
        """(?i)\bclters?\b""" to "filter",
        """(?i)\bguids?\b""" to "fluid",
        """(?i)\bgushed\b""" to "flushed",
        """(?i)\bVericed\b""" to "Verified",
        """(?i)\bcrst\b""" to "first",
        """(?i)\bcnal\b""" to "final",
        """(?i)\binated\b""" to "inflated",
        """(?i)\bcxed\b""" to "fixed",
        """celd(s?)\b""" to "field$1",
    ).map { (pattern, word) -> Regex(pattern) to word }

    /**
     * A report saved from a browser often draws "fi" and "fl" as one glyph the PDF can't name, which
     * reads back as a replacement character ("Oil and �lter changed"), or names it as other letters
     * ([misnamed]). The one that makes a known word is put back; otherwise "fi", the commoner.
     */
    fun repairLigatures(word: String): String {
        var w = word.replace("\uFB01", "fi").replace("\uFB02", "fl")
        misnamed.forEach { (re, fix) ->
            w = re.replace(w) { m ->
                val r = if (fix.contains("$1")) fix.replace("$1", m.groupValues.getOrElse(1) { "" }) else fix
                // Keep a capital where the misread word had one, and an "s" it ended with.
                val cased = if (m.value.first().isUpperCase()) r.replaceFirstChar { it.uppercase() } else r
                if (!fix.contains("$1") && m.value.endsWith("s") && !cased.endsWith("s")) cased + "s" else cased
            }
        }
        if ('\uFFFD' !in w) return w
        val fl = w.replace("\uFFFD", "fl")
        return if (ligatureWords.any { fl.lowercase(Locale.US).trimEnd(',', '.').contains(it) }) fl else w.replace("\uFFFD", "fi")
    }
}
