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
            return refuse(CarfaxRefusal.NotAReport)
        }
        val repaired = repairLigatures(words.map { it.text })
        val reading = visits(words.mapIndexed { i, w -> w.copy(text = repaired[i]) }, today)
        if (reading.records.isEmpty()) {
            return refuse(CarfaxRefusal.NoVisits(reading.withoutMileage))
        }
        return ImportFile.ServiceHistory(reading.records, DistanceUnit.MILES)
    }

    private fun refuse(why: CarfaxRefusal) = ImportFile.Invalid(emptyList(), carfax = why)

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
                work = oneLinePerService(work.map { ImportedWork(it.take(HistoryImport.MAX_LABEL), service(it)) }).take(HistoryImport.MAX_WORK),
                diy = false,
                shopName = shop,
            )
        }
        return Reading(records, withoutMileage)
    }

    /**
     * A report can name one job twice in a visit ("Four tires balanced", "Tire(s) balanced"; "Alignment
     * performed", "Four wheel alignment performed"). That is one service line, worded as the longer of
     * the two, where the detail is; it stays where the first one was.
     */
    fun oneLinePerService(work: List<ImportedWork>): List<ImportedWork> {
        val wordiest = work.filter { it.service != null }.groupBy { it.service }.mapValues { (_, same) -> same.maxBy { it.label.length } }
        return work.filter { it.service == null || it === work.first { w -> w.service == it.service } }
            .map { w -> w.service?.let(wordiest::getValue) ?: w }
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
            """adjusted|cleaned|lubricated|resurfaced|tested|mounted|recharged|topped off|updated|reprogrammed|reflashed)\b""",
    )
    private val notWork = Regex("""(?i)\b(title|titled|registration|registered|owner|odometer|sold|for sale|auction|damage|accident|lien|recall)\b""")

    /** Whether a comment is work done on the vehicle, not a record about it. */
    fun isWork(item: String): Boolean = workWords.containsMatchIn(item) && !notWork.containsMatchIn(item)

    /**
     * The Log service line a comment is, where the wording leaves no doubt; null keeps it as written.
     * "Brake pads replaced" (which axle?) is the line for pads on an axle not given, never front or rear.
     */
    private val services: List<Pair<Regex, String>> = listOf(
        """oil and filter changed|oil changed""" to "oil",
        // A hybrid's and a turbo's own cooling, before the engine's coolant and air filter take their words.
        """inverter coolant (replaced|changed|flushed|serviced)""" to "inverter-coolant",
        """intercooler coolant (replaced|changed|flushed|serviced)""" to "intercooler-coolant",
        """hybrid battery (cooling |intake )?(air )?filters? (replaced|cleaned|changed|serviced)""" to "hybrid-battery-filter",
        """cabin air filters?""" to "cabin-air",
        """(engine )?air filters? (replaced|changed)""" to "engine-air",
        """tires? rotated|tire rotation""" to "rotation",
        // The sensors before the pressure, which "tire pressure monitoring system checked" would otherwise be.
        """(tpms|tire pressure (monitoring |monitor )?(system )?sensors?|tire pressure monitoring system)( sensors?)? """ +
            """(replaced|installed|serviced|repaired|reset|programmed|checked|inspected)""" to "tpms",
        """tires?( condition)?( and| &)?( pressure)? (checked|inspected)|tire pressure (checked|adjusted|set)""" to "tire-check",
        """(wheels?|tires?) balanced""" to "balance",
        """(wheels?|front end) aligned|alignment performed""" to "alignment",
        """tires? replaced|new tires? installed""" to "new-tires",
        """(?<!rear and )front brake pads replaced""" to "front-pads",
        """(?<!front and )rear brake pads replaced""" to "rear-pads",
        // Which axle? The record doesn't say, so it is neither front nor rear. Both axles named is two jobs, and stays as written.
        """(?<!(front|rear) )brake pads? replaced""" to "brake-pads",
        """(front |rear )?brake rotors? (replaced|resurfaced)""" to "rotors",
        """brake fluid (flushed|changed|replaced)""" to "brake-fluid",
        """brake fluid (checked|inspected|tested)""" to "brake-fluid-check",
        """brakes? (system )?(checked|inspected)|brake inspection""" to "brake-inspect",
        """brakes? (system )?serviced""" to "brake-service",
        """(brake )?calipers? (replaced|rebuilt|installed)""" to "calipers",
        """brake (lines?|hoses?) (replaced|repaired|installed)""" to "brake-lines",
        // The parking brake's own shoes before brake shoes in general.
        """(parking|emergency) brake( cables?| shoes?)? (replaced|adjusted|repaired)""" to "parking-brake",
        """(rear )?brake shoes? (replaced|installed)""" to "brake-shoes",
        """(rear )?brake drums? (replaced|resurfaced|machined)""" to "brake-drums",
        """wheel cylinders? (replaced|rebuilt)""" to "wheel-cylinders",
        // A clutch has a master cylinder too.
        """(?<!clutch )(brake )?master cylinder (replaced|rebuilt|installed)""" to "master-cylinder",
        """(power )?brake boosters? (replaced|installed)""" to "brake-booster",
        """(abs|wheel speed) sensors? replaced""" to "abs-sensor",
        // A gasket, seal or housing is part of the job it belongs to.
        """water pump( gasket| seal)? (replaced|installed|repaired)""" to "water-pump",
        """thermostat( gasket| housing)? (replaced|installed)""" to "thermostat",
        """heater core (replaced|flushed|installed)""" to "heater-core",
        """radiator (pressure )?cap replaced""" to "radiator-cap",
        """(coolant|overflow|expansion) (reservoir|tank|bottle) replaced""" to "coolant-reservoir",
        """fan clutch replaced""" to "fan-clutch",
        """((radiator|engine|electric) )?cooling fans?( motor| assembly)? (replaced|installed)|radiator fan( motor)? replaced""" to "cooling-fan",
        // Hoses before the radiator, which "radiator hose replaced" would otherwise be.
        """(coolant|radiator|heater) hoses? (replaced|installed)""" to "coolant-hoses",
        """radiator (replaced|installed|repaired)""" to "radiator",
        """(coolant|cooling system|antifreeze)( level)? (checked|inspected|tested)""" to "cooling-check",
        """(coolant|cooling system|radiator) (flushed|changed|serviced)|antifreeze (flushed|changed)|antifreeze/coolant flushed""" to "coolant",
        """transmission (fluid|oil) (flushed|changed|replaced)|transmission serviced""" to "trans-fluid",
        // Front and rear have their own lines, so they come before the differential in general.
        """front (differential|diff) (fluid |oil )?(changed|serviced|services|flushed|replaced)""" to "front-diff",
        """front (differential|diff) (fluid |oil )?(checked|inspected)""" to "front-diff-check",
        """rear (differential|diff) (fluid |oil )?(changed|serviced|services|flushed|replaced)""" to "rear-diff",
        """rear (differential|diff) (fluid |oil )?(checked|inspected)""" to "rear-diff-check",
        """transfer case (fluid |oil )?(changed|serviced|services|flushed|replaced)""" to "transfer-case",
        """transfer case (fluid |oil )?(checked|inspected)""" to "transfer-case-check",
        """differentials? (fluid |oil )?(changed|serviced|services|flushed|replaced)""" to "diff-fluid",
        """spark plugs? replaced""" to "spark-plugs",
        """(spark plug|ignition) wires?( set)? replaced|plug wires? replaced""" to "plug-wires",
        """glow plugs? replaced""" to "glow-plugs",
        // A key fob's battery before the vehicle's.
        """(key fob|remote|keyless entry remote|key) batter(y|ies) replaced""" to "key-fob-battery",
        """battery replaced|new battery installed""" to "battery",
        """wiper blades? replaced|wipers? replaced""" to "wipers",
        """(windshield )?wiper( blade)?s?(/washers?)? (checked|inspected)""" to "wipers-check",
        """(serpentine|drive|accessory) belts? replaced""" to "belt",
        """(serpentine|drive|accessory) belts? (checked|inspected)""" to "belt-check",
        """timing belts? (replaced|changed)""" to "timing-belt",
        """timing chains?( kit)? (replaced|installed)""" to "timing-chain",
        """fuel filters? (replaced|changed)""" to "fuel-filter",
        """fuel (system|injection system|injectors|induction system) (cleaned|serviced|flushed)""" to "fuel-system",
        """throttle body (cleaned|serviced)""" to "throttle-body",
        """fuel injectors? (replaced|installed)""" to "fuel-injectors",
        """fuel pump( assembly| module)? (replaced|installed)""" to "fuel-pump",
        """(egr|exhaust gas recirculation)( valve)? (replaced|cleaned|installed)""" to "egr-valve",
        """secondary air( injection)?( pump| system| check valve| valve)? (replaced|repaired|installed)""" to "secondary-air-pump",
        """(evap|evaporative emissions?)( system)?( canister| purge valve| vent valve)? (replaced|repaired)|charcoal canister replaced""" to "evap-system",
        """battery(/charging system)? (checked|tested|inspected)""" to "battery-check",
        """(shocks|struts|shock absorbers)((/| and )(shocks|struts))? (replaced|installed)""" to "shocks",
        """wheel bearings? (replaced|installed)""" to "wheel-bearing",
        """(coil|leaf|suspension) springs? (replaced|installed)""" to "springs",
        // Bushings before the arm or bar they are in, and only a suspension part's: an engine mount has them too.
        """(suspension|control arm|sway bar|stabilizer bar|strut|leaf spring) bushings? replaced""" to "bushings",
        """(upper |lower )?control arms? (replaced|installed)""" to "control-arms",
        """ball joints? (replaced|installed)""" to "ball-joints",
        """(sway|stabilizer|anti-roll) bar( end)? links? (replaced|installed)""" to "sway-bar-links",
        """tie rods?( ends?)? (replaced|installed)""" to "tie-rods",
        """(steering rack|rack and pinion|steering gear( box)?|steering gearbox)( assembly)? (replaced|installed)""" to "steering-rack",
        """power steering pump (replaced|installed)""" to "power-steering-pump",
        """(idler arm|pitman arm|drag link|center link|steering (column|linkage|knuckle)) (replaced|installed)""" to "steering-parts",
        """(light )?bulbs? replaced""" to "bulbs",
        """headlights? (assembly|assemblies|housings?|lens|lenses) (replaced|installed)""" to "headlight-assembly",
        """(power )?window (regulators?|motors?)( and (regulators?|motors?))? replaced""" to "window-regulator",
        """door lock actuators? replaced""" to "door-lock-actuator",
        """(a/c|ac|air conditioning) compressor (replaced|installed)""" to "ac-compressor",
        """(a/c|ac|air conditioning) condenser (replaced|installed)""" to "ac-condenser",
        """(heater |hvac )?blower motor replaced""" to "blower-motor",
        """(fuses?|relays?)( and (fuses?|relays?))? replaced""" to "fuses",
        """horns? replaced""" to "horn",
        """power steering (fluid )?(flushed|changed|serviced)""" to "power-steering",
        """washer fluid (topped off|filled|added|replaced)""" to "washer",
        // Only fluids in general: "Brake fluid checked" has its own line above.
        """^(all )?fluids?( levels?)? (checked|inspected|topped off)|^fluid levels? (checked|inspected|topped off)""" to "fluid-check",
        """((ignition )?coil packs?|ignition coils?) replaced""" to "ignition-coils",
        """(oxygen|o2) sensors? replaced""" to "oxygen-sensor",
        """(mass air ?flow|maf|camshaft position|crankshaft position|cam position|crank position|knock) sensors? replaced""" to "engine-sensors",
        """(turbo(charger)?|supercharger) (replaced|rebuilt|installed)""" to "turbo",
        """valve cover gaskets? replaced""" to "valve-cover-gasket",
        """(cylinder )?head gaskets? (replaced|repaired)""" to "head-gasket",
        """intake( manifold)? gaskets? replaced""" to "intake-gasket",
        """oil pan gaskets? replaced""" to "oil-pan-gasket",
        """rear main( crankshaft)?( oil)? seals? replaced""" to "rear-main-seal",
        """(engine|motor) mounts? replaced""" to "engine-mounts",
        """catalytic converters? (replaced|installed)""" to "catalytic-converter",
        """mufflers? (replaced|installed)""" to "muffler",
        """(exhaust( system| pipes?)?|mufflers?|catalytic converters?) (replaced|repaired|installed)""" to "exhaust",
        """starter( motor)? (replaced|installed)""" to "starter",
        """alternator (replaced|installed)""" to "alternator",
        // A flush before the service, which "A/C system flushed and recharged" would otherwise be.
        """(air conditioning|a/c)( system)? flush(ed| performed)""" to "ac-flush",
        """(air conditioning|a/c|heating|heater|hvac|climate control)( and (air conditioning|a/c|heating))?( systems?)? (checked|inspected|tested)""" to "hvac-check",
        """(air conditioning|a/c)( system)?( refrigerant)? (recharged|serviced|evacuated|repaired)|refrigerant (recharged|added|charged)""" to "air-conditioning",
        """clutch( kit| assembly)? (replaced|installed)""" to "clutch",
        """clutch (master|slave)( and (master|slave))? cylinders? (replaced|installed)|slave cylinder (replaced|installed)""" to "clutch-hydraulics",
        """flywheel (replaced|resurfaced|machined)""" to "flywheel",
        """(axle|differential|pinion)( shaft)? seals? (replaced|installed)""" to "axle-seals",
        """transmission( oil)? pan gaskets? replaced""" to "trans-pan-gasket",
        """shift(er)? (cables?|linkage) (replaced|adjusted|repaired)""" to "shift-cable",
        """(u-joints?|universal joints?) (replaced|installed|lubricated|greased)""" to "u-joints",
        """((door|hood|trunk|tailgate|liftgate) )?(hinges?|latch(es)?|door checks?)( and (hinges?|latch(es)?|door checks?))? (lubricated|greased|lubed)|body lubricated""" to "body-lube",
        """(sunroof|moonroof)( (drains?|tracks?|rails?|motor|glass|seals?))? (cleaned|lubricated|repaired|replaced|serviced|adjusted)""" to "sunroof",
        """((engine|transmission|powertrain) )?(computer|ecu|ecm|pcm|tcm|control module)( software)? (updated|reprogrammed|reflashed|programmed)|software updated""" to "ecu-update",
        """(computer|ecu|ecm|pcm|on-board) (diagnostics?|system) (checked|performed|tested)|(computer|ecu|ecm|pcm) (checked|tested)|diagnostics? (performed|checked|test performed)|(check engine light|trouble codes?) (checked|inspected)""" to "diagnostic",
        """chassis (lubricated|greased|lubed)|(lubricate|lube|grease)d? chassis|chassis lub(e|rication)( performed| serviced)?""" to "chassis-lube",
        """(suspension|steering)( and (suspension|steering))?( system)? (checked|inspected)""" to "suspension-check",
        // "Check engine light checked" is a diagnosis, not the lights.
        """(?<!engine )((head|exterior |interior )?lights|lighting)( system)? (checked|inspected)""" to "lights-check",
        """windows? (checked|inspected)""" to "windows-check",
        """windshield( glass)? (replaced|repaired|installed)|windshield (chips?|cracks?) repaired""" to "windshield",
        """((side|outside|door|rear ?view) )?mirrors?( glass| assembly)? (replaced|installed|repaired)""" to "mirror",
        """((exterior|interior|outside|inside) )?door handles? (replaced|repaired)""" to "door-handle",
        """(windows?|glass) (replaced|repaired)""" to "body",
        """maintenance inspection completed|multi-point inspection""" to "multi-point",
        """pre-?delivery inspection|\bpdi\b""" to "pdi",
        // Which jobs isn't said, so it stays a line of its own and resets nothing.
        """\d{1,3}(,\d{3}|k|000) (mile |mi |km )?(service|maintenance)|(recommended|scheduled|factory scheduled) maintenance (performed|completed)""" to "scheduled-service",
        """emissions? (inspection|test)""" to "emissions",
        // "Emissions or safety inspection performed" says neither for sure, and stays as written.
        """(?<!or )safety inspection""" to "safety-inspection",
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

/** Why a PDF can't be imported as a report; `app` words it. */
sealed interface CarfaxRefusal {
    data object NotAReport : CarfaxRefusal

    /** No visit had a mileage; [withoutMileage] had none and so can't be records. */
    data class NoVisits(val withoutMileage: Int) : CarfaxRefusal
}
