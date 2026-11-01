package io.cosgriff.glovebox.domain

/**
 * A guess at the catalog line for wording no pattern or rule knows (#312): a typo ("Watr pump replaced"),
 * shop shorthand ("R&R H2O pump") or another word order ("Pads, front, replaced"). **Only ever a
 * suggestion, labelled a guess and never picked for the user**, and the last thing asked: a file's own
 * `service`, the user's rules ([CatalogRules]) and the plain wording [CarfaxReport.service] knows all come
 * first. Everything here runs on the phone and looks nothing up.
 *
 * Each line has the words that name its part ([LineWords.parts], any of the phrases, its words in any
 * order) and whether it is a check. A word of five letters or more may be one edit out (Damerau–Levenshtein).
 * The action decides between a job and its check: a check line needs the text to say checked or
 * inspected; any other line is refused when that is all the text says, since filing "Water pump checked"
 * under the water pump would count it as replaced. A line with a check twin ([LineWords.needsAction])
 * also needs the text to say what was done, so "front differential" alone suggests neither.
 *
 * A guess is made only when one line is clearly ahead: two lines that each match their own words
 * ("water pump and thermostat replaced") are two jobs, and two that match about as well are a close call.
 * Either way the text stays as written, as it would for [CarfaxReport.service].
 */
object LineGuess {
    /** The line [text] looks like, or null when nothing is clearly ahead. */
    fun guess(text: String): String? {
        val words = words(text)
        if (words.isEmpty()) return null
        val action = action(words)
        val found = LineWords.all.mapNotNull { (id, line) -> match(id, line, words, action) }
        if (found.isEmpty()) return null
        val ranked = found.sortedByDescending { it.score }
        val best = ranked.first()
        // A line whose words are all among the best one's is the same job said less fully ("brake" in "brake pads").
        val rivals = ranked.drop(1).filterNot { best.used.containsAll(it.used) }
        // Two jobs: another line on words of its own that says about as much, or that "and" sets apart from the best
        // ("oil change and tire rotation"). A generic word beside the best line ("fluid" in "front diff fluid checked")
        // is not a second job.
        if (rivals.any { it.used.none(best.used::contains) && (it.score * 2 >= best.score || apart(best.used, it.used, words)) }) return null
        if (rivals.any { it.score * 4 > best.score * 3 }) return null // a close call
        // A line the text names in full but whose action rules it out still says something the guess doesn't
        // account for: "diff fluid checked" is a check of the differential, which has no check line, not of the fluids.
        val unsaid = LineWords.all.mapNotNull { (id, line) -> match(id, line, words, action = null) }
            .filterNot { best.used.containsAll(it.used) }
        if (unsaid.any { it.score * 2 >= best.score }) return null
        return best.id
    }

    /**
     * The lines [text] looks like when it may name more than one job (#483): a shop's one service is often two
     * catalog lines ("Remove & Replace Spark Plugs and Ignition Coils"). Asked whole first; when that is no clear
     * one line, each piece between "and" or "plus" is asked by itself, and a piece that says nothing of what was
     * done ("ignition coils") takes the whole text's action words. Each answer is still only a guess, offered.
     */
    fun guesses(text: String): List<String> {
        guess(text)?.let { return listOf(it) }
        val words = words(text)
        val pieces = mutableListOf(mutableListOf<String>())
        words.forEach { if (it in SPLITTING) pieces += mutableListOf<String>() else pieces.last() += it }
        val real = pieces.filter { it.isNotEmpty() }
        if (real.size < 2) return emptyList()
        val actions = words.filter { it in DONE || it in CHECKED }
        return real.mapNotNull { piece -> guess((if (action(piece) == Action.NONE) piece + actions else piece).joinToString(" ")) }.distinct()
    }

    /**
     * Whether [done] is worded like [recommended] (#572): most of what the recommendation names (two in three of its
     * words, leaving out what was done to it and words like "and"), each the same word as one of [done]'s, give or
     * take a plural or a typo, as [guess] compares them. "Electrical concern testing and analysis" is worded like
     * "Electrical concern analysis done"; never like "Oil change".
     */
    fun similar(recommended: String, done: String): Boolean {
        val named = words(recommended).filterNot { it in DONE || it in CHECKED || it in FILLER }.distinct()
        if (named.isEmpty()) return false
        val theirs = words(done)
        val found = named.count { w -> theirs.any { same(it, w) || same(w, it) } }
        return found * 3 >= named.size * 2
    }

    /** Words that name nothing. */
    private val FILLER = setOf("and", "plus", "with", "the", "a", "an", "of", "for", "to", "on", "in", "at", "or", "both", "all", "system")

    private val SPLITTING = setOf("and", "plus")

    /** "and" (or "plus", "with") stands between the two sets of words. */
    private fun apart(a: Set<Int>, b: Set<Int>, words: List<String>): Boolean = words.indices.any { i ->
        words[i] in JOINING && ((a.all { it < i } && b.all { it > i }) || (b.all { it < i } && a.all { it > i }))
    }

    /** What was done, as far as the words say. Doing the work outranks looking at it ("inspected and replaced"). */
    internal enum class Action { DO, CHECK, NONE }

    private class Match(val id: String, val score: Int, val used: Set<Int>)

    /** [action] null matches the words alone, whatever the text says was done. */
    private fun match(id: String, line: LineWords.Line, words: List<String>, action: Action?): Match? {
        when {
            action == null -> Unit
            line.check && action != Action.CHECK -> return null
            !line.check && action == Action.CHECK -> return null
            line.needsAction && action == Action.NONE -> return null
        }
        return line.parts.mapNotNull { phrase ->
            val used = mutableSetOf<Int>()
            for (part in phrase) {
                val at = words.indices.firstOrNull { it !in used && same(words[it], part) } ?: return@mapNotNull null
                used += at
            }
            // The more of the text a phrase accounts for, the more it says: "front pad" over "pad".
            Match(id, phrase.sumOf { it.length }, used)
        }.maxByOrNull { it.score }
    }

    internal fun action(words: List<String>): Action = when {
        words.any { it in DONE } -> Action.DO
        words.any { it in CHECKED } -> Action.CHECK
        else -> Action.NONE
    }

    /** A typed word and a vocabulary word are the same when they are, give or take a plural, or one edit apart in a long word. */
    internal fun same(typed: String, word: String): Boolean {
        if (typed == word || stem(typed) == stem(word)) return true
        // A word spelled right is that word, not a typo of another: "lower" is never "blower".
        if (stem(typed) in KNOWN) return false
        return listOf(word, word + "s").any { w -> w.length >= 5 && kotlin.math.abs(typed.length - w.length) <= 1 && distance(typed, w) <= 1 }
    }

    /** Every vocabulary word, and the everyday words a shop writes beside them, none of which is ever a typo. */
    private val KNOWN: Set<String> by lazy {
        LineWords.all.values.flatMap { it.parts.flatten() }.map(::stem).toSet() + EVERYDAY
    }

    private val EVERYDAY = setOf(
        "lower", "upper", "inner", "outer", "left", "right", "both", "front", "rear", "older", "under", "over", "other",
        "noise", "leak", "leaking", "worn", "broken", "cracked", "customer", "states", "vehicle", "parts", "labor",
    )

    private val JOINING = setOf("and", "plus", "with")

    /** "plugs" as "plug", "flushed" as "flush": enough to compare a typed word with a vocabulary word, never shown. */
    private fun stem(word: String): String {
        val one = if (word.length > 3 && word.endsWith("s") && !word.endsWith("ss")) word.dropLast(1) else word
        if (one.length <= 5 || !one.endsWith("ed")) return one
        val root = one.dropLast(2)
        // "plugged" is "plug", as "patched" is "patch".
        return if (root.length > 3 && root.last() == root[root.length - 2]) root.dropLast(1) else root
    }

    /** Lower case, shorthand expanded, punctuation gone: the words matching looks at. */
    internal fun words(text: String): List<String> {
        var t = " " + text.lowercase().replace("(s)", "s") + " "
        PHRASES.forEach { (from, to) -> t = t.replace(from, to) }
        return t.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.flatMap { w -> SHORTHAND[w]?.split(' ') ?: listOf(w) }
    }

    /**
     * Optimal string alignment distance: insertions, deletions, substitutions and swapping two neighbours each
     * cost one. Only ever asked about short words, so the whole table is fine.
     */
    internal fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
        }
        return d[a.length][b.length]
    }

    /** Shorthand written across punctuation, put right before the text is split ("R&R", "A/C"). */
    private val PHRASES = listOf(
        "r&r" to " replaced ", "r/r" to " replaced ", "r & r" to " replaced ", "r+r" to " replaced ", "&" to " and ",
        "e-brake" to " emergency brake ",
        "a/c" to " ac ", "air conditioning" to " ac ", "air conditioner" to " ac ", "w/" to " with ",
        "u-joint" to " ujoint ", "u joint" to " ujoint ", "multi-point" to " multipoint ", "multi point" to " multipoint ",
        "t-stat" to " thermostat ",
        // The check engine light is a diagnosis, not the lights; a leak test is not a check of the fluid levels.
        "check engine light" to " cel ", "check engine lamp" to " cel ", "fluid leak" to " leak ",
    )

    /** One word of shop shorthand, and what it is short for. */
    internal val SHORTHAND: Map<String, String> = mapOf(
        "repl" to "replaced", "rpl" to "replaced", "rplc" to "replaced", "rplcd" to "replaced", "replc" to "replaced", "rpld" to "replaced",
        "chk" to "checked", "chkd" to "checked", "ck" to "checked", "ckd" to "checked", "chked" to "checked",
        "insp" to "inspected", "inspd" to "inspected", "inspct" to "inspected",
        "svc" to "serviced", "svcd" to "serviced", "serv" to "serviced", "srvc" to "serviced", "srvcd" to "serviced",
        "flt" to "filter", "fltr" to "filter", "filt" to "filter",
        "xmsn" to "transmission", "trans" to "transmission", "tranny" to "transmission", "trns" to "transmission",
        "atf" to "transmission fluid", "diff" to "differential", "dif" to "differential", "diffs" to "differential",
        "brk" to "brake", "brks" to "brake", "frt" to "front", "frnt" to "front", "fr" to "front", "lf" to "front", "rf" to "front",
        "rr" to "rear", "lr" to "rear", "h2o" to "water", "ps" to "power steering", "o2" to "oxygen", "tyre" to "tire", "tyres" to "tire",
        "coolent" to "coolant", "antifreeze" to "coolant", "tstat" to "thermostat", "plugs" to "plug", "struts" to "strut",
        "cat" to "catalytic converter", "maf" to "mass airflow", "tpms" to "tpms", "abs" to "abs", "evap" to "evap",
    )

    /** Words that say the work was done. */
    private val DONE = setOf(
        "replaced", "replace", "replacing", "changed", "change", "new", "installed", "install", "renewed", "swapped", "rebuilt",
        "repaired", "repair", "serviced", "service", "flushed", "flush", "cleaned", "clean", "topped", "filled", "adjusted",
        "lubricated", "lubed", "greased", "rotated", "rotation", "balanced", "aligned", "alignment", "recharged", "resurfaced",
        "machined", "performed", "done", "fixed", "patched", "plugged", "mounted", "bled", "programmed", "reset",
        "exchange", "exchanged", "rotate", "rotating", "lube", "replacement", "updated", "update", "reprogrammed", "reflashed", "flashed",
    )

    /** Words that say it was only looked at. */
    private val CHECKED = setOf(
        "checked", "check", "inspected", "inspect", "inspection", "tested", "test", "examined", "verified",
        "scanned", "scan", "diagnosed", "diagnose", "diagnosis", "diagnostic",
    )
}

/** Each catalog line's words for [LineGuess]. English, like [CarfaxReport.service]'s wording; never shown. */
object LineWords {
    /**
     * [parts] are the phrases that name the line's part, any of which will do, each word written as it would be
     * after [LineGuess.words] (singular, shorthand expanded). [check] says the line is looking, not doing.
     */
    class Line(val parts: List<List<String>>, val check: Boolean = false, val needsAction: Boolean = false)

    private fun line(vararg parts: String, check: Boolean = false, needsAction: Boolean = false) =
        Line(parts.map { it.split(' ') }, check, needsAction)

    /** Every catalog line, by id. `LineGuessTest` holds this to the catalog. */
    val all: Map<String, Line> = mapOf(
        "oil" to line("oil", "oil filter", "oil and filter"),
        "engine-air" to line("engine air filter", "air filter", "engine filter"),
        "spark-plugs" to line("spark plug"),
        "plug-wires" to line("spark plug wire", "plug wire", "ignition wire"),
        "ignition-coils" to line("ignition coil", "coil pack", "coil"),
        "glow-plugs" to line("glow plug"),
        "belt" to line("serpentine belt", "drive belt", "accessory belt", "fan belt", needsAction = true),
        "belt-check" to line("serpentine belt", "drive belt", "accessory belt", "fan belt", "belt", check = true),
        "timing-belt" to line("timing belt"),
        "timing-chain" to line("timing chain"),
        "pcv" to line("pcv", "pcv valve", "crankcase ventilation"),
        "engine-sensors" to line("mass airflow sensor", "camshaft position sensor", "crankshaft position sensor", "cam sensor", "crank sensor", "knock sensor"),
        "turbo" to line("turbo", "turbocharger", "supercharger"),
        "valve-cover-gasket" to line("valve cover gasket", "valve cover"),
        "head-gasket" to line("head gasket"),
        "intake-gasket" to line("intake gasket", "intake manifold gasket"),
        "oil-pan-gasket" to line("oil pan gasket", "oil pan"),
        "rear-main-seal" to line("rear main seal", "rear main"),
        "engine-mounts" to line("engine mount", "motor mount"),

        "rotation" to line("tire rotation", "rotation", "rotated", "tire rotated", "rotate tire"),
        "balance" to line("balance", "balanced", "wheel balance"),
        "alignment" to line("alignment", "aligned", "front end alignment"),
        "new-tires" to line("tire", needsAction = true),
        "flat" to line("flat", "flat tire", "puncture", "tire patch", "tire plug"),
        "tire-check" to line("tire", "tire pressure", check = true),
        "tpms" to line("tpms", "tire pressure sensor", "tire sensor"),

        "front-pads" to line("front pad", "front brake pad"),
        "rear-pads" to line("rear pad", "rear brake pad"),
        "brake-pads" to line("brake pad", "pad"),
        "rotors" to line("rotor", "brake disc", "disc"),
        "brake-shoes" to line("brake shoe", "shoe"),
        "brake-drums" to line("brake drum", "drum"),
        "brake-fluid" to line("brake fluid", needsAction = true),
        "brake-fluid-check" to line("brake fluid", check = true),
        "brake-inspect" to line("brake", check = true),
        "brake-service" to line("brake", needsAction = true),
        "calipers" to line("caliper", "brake caliper"),
        "wheel-cylinders" to line("wheel cylinder"),
        "master-cylinder" to line("master cylinder", "brake master cylinder"),
        "brake-booster" to line("brake booster", "booster"),
        "brake-lines" to line("brake line", "brake hose"),
        "parking-brake" to line("parking brake", "emergency brake", "hand brake"),
        "abs-sensor" to line("abs sensor", "wheel speed sensor", "abs"),

        "trans-fluid" to line("transmission fluid", "transmission oil", "transmission"),
        "power-steering" to line("power steering fluid", "power steering"),
        "washer" to line("washer fluid", "windshield washer"),
        "fluid-check" to line("fluid", "fluid level", "all fluid", check = true),

        "coolant" to line("coolant", "cooling system", needsAction = true),
        "cooling-check" to line("coolant", "cooling system", check = true),
        "water-pump" to line("water pump", "coolant pump"),
        "thermostat" to line("thermostat"),
        "radiator" to line("radiator"),
        "radiator-cap" to line("radiator cap"),
        "coolant-reservoir" to line("coolant reservoir", "overflow tank", "expansion tank", "coolant tank"),
        "coolant-hoses" to line("coolant hose", "radiator hose", "heater hose", "upper hose", "lower hose"),
        "cooling-fan" to line("cooling fan", "radiator fan", "fan motor"),
        "fan-clutch" to line("fan clutch"),
        "heater-core" to line("heater core"),
        "inverter-coolant" to line("inverter coolant", "hybrid coolant", "power control unit coolant"),
        "intercooler-coolant" to line("intercooler coolant", "charge air coolant"),
        "hybrid-battery-filter" to line("hybrid battery filter", "battery cooling filter", "battery intake filter", "hybrid filter"),

        "fuel-filter" to line("fuel filter", "gas filter"),
        "fuel-system" to line("fuel system", "fuel injection", "injector cleaning", "induction"),
        "throttle-body" to line("throttle body"),
        "fuel-injectors" to line("fuel injector", "injector"),
        "fuel-pump" to line("fuel pump", "gas pump"),
        "evap-system" to line("evap", "charcoal canister", "purge valve", "vent valve", "evaporative"),

        "oxygen-sensor" to line("oxygen sensor", "oxygen"),
        "egr-valve" to line("egr", "egr valve"),
        "secondary-air-pump" to line("secondary air pump", "secondary air", "air injection pump"),
        "catalytic-converter" to line("catalytic converter", "converter"),
        "muffler" to line("muffler", "silencer"),
        "exhaust" to line("exhaust", "exhaust pipe", "tail pipe", "tailpipe", "flex pipe"),

        "diff-fluid" to line("differential fluid", "differential oil", "differential", needsAction = true),
        "front-diff-check" to line("front differential", "front differential fluid", "front differential oil", check = true),
        "front-diff" to line("front differential", "front differential fluid", "front differential oil", needsAction = true),
        "rear-diff-check" to line("rear differential", "rear differential fluid", "rear differential oil", check = true),
        "rear-diff" to line("rear differential", "rear differential fluid", "rear differential oil", needsAction = true),
        "transfer-case-check" to line("transfer case", "transfer case fluid", "transfer case oil", check = true),
        "transfer-case" to line("transfer case", "transfer case fluid", "transfer case oil", needsAction = true),
        "driveshaft" to line("driveshaft", "drive shaft", "prop shaft", "propeller shaft"),
        "cv-axle" to line("cv axle", "cv boot", "cv joint", "half shaft", "axle shaft"),
        "axle-seals" to line("axle seal", "differential seal", "pinion seal"),
        "u-joints" to line("ujoint", "universal joint"),
        "clutch" to line("clutch", "clutch kit"),
        "clutch-hydraulics" to line("clutch master cylinder", "slave cylinder", "clutch slave"),
        "flywheel" to line("flywheel"),
        "trans-pan-gasket" to line("transmission pan gasket", "pan gasket"),
        "shift-cable" to line("shift cable", "shifter cable", "shift linkage"),

        "shocks" to line("shock", "strut", "shock absorber"),
        "springs" to line("coil spring", "leaf spring", "spring"),
        "control-arms" to line("control arm"),
        "ball-joints" to line("ball joint"),
        "bushings" to line("bushing", "control arm bushing", "sway bar bushing"),
        "sway-bar-links" to line("sway bar link", "stabilizer link", "end link", "sway bar"),
        "wheel-bearing" to line("wheel bearing", "hub bearing", "hub assembly"),
        "tie-rods" to line("tie rod", "tie rod end"),
        "steering-rack" to line("steering rack", "rack and pinion", "steering gear", "steering box"),
        "power-steering-pump" to line("power steering pump"),
        "steering-parts" to line("idler arm", "pitman arm", "drag link", "center link", "steering column", "steering linkage"),
        "chassis-lube" to line("chassis", "chassis lubrication", "grease fitting", "zerk", "zerk fitting", needsAction = true),
        "suspension-check" to line("suspension", "steering", "front end", check = true),

        "cabin-air" to line("cabin air filter", "cabin filter", "pollen filter"),
        "wipers" to line("wiper", "wiper blade", needsAction = true),
        "wipers-check" to line("wiper", "wiper blade", check = true),
        "battery" to line("battery", needsAction = true),
        "battery-check" to line("battery", "charging system", check = true),
        "key-fob-battery" to line("key fob battery", "fob battery", "remote battery", "key battery"),
        "bulbs" to line("bulb", "light bulb", "headlight bulb", "brake light bulb", "brake light", "tail light"),
        "headlight-assembly" to line("headlight assembly", "headlight housing", "headlight lens"),
        "lights-check" to line("light", "lighting", check = true),
        "windows-check" to line("window", check = true),
        "sunroof" to line("sunroof", "moonroof", "sunroof rail", "sunroof track", "sunroof drain"),
        "window-regulator" to line("window regulator", "window motor"),
        "door-lock-actuator" to line("door lock actuator", "lock actuator", "door lock"),
        "air-conditioning" to line("ac", "refrigerant", "ac recharge", needsAction = true),
        "ac-flush" to line("ac flush", "ac system flush"),
        "ac-compressor" to line("ac compressor", "compressor"),
        "ac-condenser" to line("ac condenser", "condenser"),
        "hvac-check" to line("ac", "heating", "heater", "hvac", "climate control", "heating and ac", check = true),
        "blower-motor" to line("blower motor", "blower", "heater fan"),
        "starter" to line("starter", "starter motor"),
        "alternator" to line("alternator"),
        "fuses" to line("fuse", "relay"),
        "horn" to line("horn"),
        "ecu-update" to line("ecu", "ecm", "pcm", "tcm", "computer", "software", "software update", "control module", needsAction = true),

        // Inspections are looking by name: their own wording ("inspection", "test") is the check.
        "multi-point" to line("multipoint", "courtesy", "vehicle", check = true),
        "pdi" to line("pre delivery", "predelivery", "pdi", "new vehicle", check = true),
        "diagnostic" to line(
            "code", "trouble code", "scan", "ecu", "ecm", "pcm", "computer", "check engine light", "obd",
            check = true,
        ),
        "scheduled-service" to line("scheduled maintenance", "scheduled service", "factory maintenance", "mile service"),
        "emissions" to line("emission", "smog", check = true),
        "safety-inspection" to line("safety", check = true),
        "recall" to line("recall", "campaign"),

        "accessory" to line("hitch", "accessory", "roof rack", "dash cam", "remote start"),
        "windshield" to line("windshield", "windscreen"),
        "mirror" to line("mirror", "side mirror"),
        "door-handle" to line("door handle"),
        "body-lube" to line("hinge", "latch", "door check", "door hinge", "hood latch", "body lube", "body lubricated", "body lubrication", needsAction = true),
        "body" to line("body work", "bumper", "fender", "dent", "paint", "glass"),
        "detail" to line("detail", "car wash", "wash", "wax"),
    )
}
