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
        val repaired = repairLigatures(words.map { it.text })
        val reading = visits(words.mapIndexed { i, w -> w.copy(text = repaired[i]) }, today)
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
        """cabin air filters?""" to "cabin-air",
        """(engine )?air filters? (replaced|changed)""" to "engine-air",
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

    /** The ligatures a font may draw as one glyph, longest first so "ffi" is tried before "ff" and "fi". */
    private val ligatures = listOf("ffi", "ffl", "ff", "fi", "fl")

    private val ligatureChars = mapOf('\uFB00' to "ff", '\uFB01' to "fi", '\uFB02' to "fl", '\uFB03' to "ffi", '\uFB04' to "ffl")

    /**
     * Words a Carfax report uses that have a ligature in them: the service and inspection vocabulary, and the
     * common words of shop names and sources. A misread is only ever repaired into one of these.
     */
    private val ligatureVocabulary = setOf(
        // Fluids and filters
        "filter", "filters", "fluid", "fluids", "flush", "flushed", "flushes", "filled", "refill", "refilled", "flow", "airflow",
        "overflow",
        // Parts
        "differential", "differentials", "diff", "flywheel", "fitting", "fittings", "flex", "flexible", "flasher", "flashers",
        "floor", "flared", "muffler", "mufflers", "reflector", "deflector", "baffle", "fluorescent",
        // What was done
        "inflated", "inflation", "fixed", "fitted", "refinished", "flat", "buffed", "scuffed", "affected", "effect", "effects",
        "verified", "certified", "first", "final", "flagged", "fleet", "field",
        // Reports and sources
        "office", "officer", "official", "efficiency", "traffic", "affidavit", "sheriff", "profile", "certificate", "notification",
    )

    /** A word as [head], the letters and slashes between ([core]) and [tail], so "(clter," is looked at as "clter". */
    private class Parts(word: String) {
        val core = word.trimEnd(',', '.', ';', ':', ')').trimStart('(')
        val head = word.substring(0, word.indexOf(core))
        val tail = word.substring(head.length + core.length)
    }

    /** Known words [core] becomes when one of its letters is read as a ligature: (that letter, the ligature, the word). */
    private fun misreadings(core: String): List<Triple<Char, String, String>> {
        val lower = core.lowercase(Locale.US)
        if (lower in ligatureVocabulary || core.length < 3 || core.any { !it.isLetter() }) return emptyList()
        return lower.indices.flatMap { i ->
            ligatures.map { lig -> Triple(lower[i], lig, lower.substring(0, i) + lig + lower.substring(i + 1)) }
        }.filter { it.third in ligatureVocabulary }
    }

    /**
     * Some PDF writers name a ligature's glyph as an ordinary letter, and which letter depends on the report:
     * printed on a Mac, one reads "Oil and clter changed", another "Differential iuid iushed". A glyph is
     * named the same way throughout a report, so the letter standing in for each ligature is learnt from the
     * whole report: the letter that turns the most different words into known ones, and at least two. One
     * word alone proves nothing: "Door" is a letter away from "floor", "Low" from "flow", "did" from "diff".
     */
    private fun standIns(words: Collection<String>): Map<Char, String> {
        val evidence = mutableMapOf<Pair<Char, String>, MutableSet<String>>()
        words.forEach { w ->
            Parts(w).core.split('/').forEach { core ->
                misreadings(core).forEach { (letter, lig, known) -> evidence.getOrPut(letter to lig) { mutableSetOf() } += known }
            }
        }
        val found = mutableMapOf<Char, String>()
        evidence.entries.filter { it.value.size >= 2 }.sortedByDescending { it.value.size }.forEach { (key, _) ->
            val (letter, lig) = key
            if (letter !in found && lig !in found.values) found[letter] = lig
        }
        return found
    }

    /**
     * A word that isn't one we know, but becomes one when a letter that stands in for a ligature in this
     * report ([standIns]) is read as it, is put back. A place name ending "…celd" is "…field" (Springfield)
     * when "c" stands for "fi", unless it is "i", where "…ield" is a word of its own (yield). Anything else is
     * left as it reads: a repair is only ever into a word of [ligatureVocabulary].
     */
    private fun misnamedLigature(word: String, standIns: Map<Char, String>): String {
        if (standIns.isEmpty()) return word
        val parts = Parts(word)
        if (parts.core.isEmpty()) return word
        // Words joined by a slash ("flushed/changed") are each looked at.
        val core = parts.core.split('/').joinToString("/") { core ->
            val known = misreadings(core).firstOrNull { (letter, lig, _) -> standIns[letter] == lig }
            when {
                known != null -> matchCase(core, known.third)
                else -> standIns.entries.firstOrNull { it.value == "fi" && it.key != 'i' }
                    ?.let { core.replace(Regex("""${it.key}eld(s?)$"""), "field$1") } ?: core
            }
        }
        return parts.head + core + parts.tail
    }

    private fun matchCase(original: String, fixed: String): String = when {
        original.all { it.isUpperCase() } -> fixed.uppercase(Locale.US)
        original.first().isUpperCase() -> fixed.replaceFirstChar { it.uppercase() }
        else -> fixed
    }

    /**
     * A report saved from a browser often draws "fi", "fl", "ff", "ffi" and "ffl" as one glyph. The PDF may
     * name it as the ligature character, as a replacement character ("Oil and �lter changed"), or as an
     * ordinary letter ([standIns]), which only the rest of the report can tell, so this takes every word of
     * it. A replacement character is put back as the ligature that makes a known word, otherwise as "fi",
     * the commonest.
     */
    fun repairLigatures(words: List<String>): List<String> {
        val named = words.map { word ->
            var w = word
            ligatureChars.forEach { (c, s) -> w = w.replace(c.toString(), s) }
            if ('\uFFFD' !in w) return@map w
            val core = Parts(w).core.lowercase(Locale.US)
            val fits = ligatures.firstOrNull { lig -> core.replace("\uFFFD", lig).split('/').any { it in ligatureVocabulary } }
            w.replace("\uFFFD", fits ?: "fi")
        }
        val standIns = standIns(named.toSet())
        return named.map { misnamedLigature(it, standIns) }
    }
}
