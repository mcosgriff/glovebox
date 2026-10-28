package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * One line of work on an imported record. [service] is a [CatalogItem] id; null for anything else. [job] is the
 * index of the record's job this line is part of (#481); null for a line with no price.
 */
data class ImportedWork(val label: String, val service: String? = null, val job: Int? = null)

/**
 * A job from a version-2 file (#481): one labor and one parts price for its lines, never split between them. From
 * version 3 its parts may instead be [rows], the invoice's part rows (#502); then [partsCents] is null.
 */
data class ImportedJob(
    val wording: String? = null,
    val laborCents: Long? = null,
    val partsCents: Long? = null,
    val rows: List<ImportedPartRow> = emptyList(),
)

/**
 * One part row of a job from a version-3 file (#502), as written: nothing is worked out from the others. [line] is
 * the index among the job's own lines of the one it is `for`; null for the whole job.
 */
data class ImportedPartRow(
    val wording: String? = null,
    val quantityMilli: Long? = null,
    val eachCents: Long? = null,
    val totalCents: Long? = null,
    val line: Int? = null,
)

/** A part row as an import compares it: its figures and the label of the line it is for, ignoring capitals. */
data class PartSummary(val wording: String?, val quantityMilli: Long?, val eachCents: Long?, val totalCents: Long?, val line: String?)

/**
 * A job as an import compares it with a saved one: its wording, its prices, its lines' labels and its part rows, in
 * no order and ignoring capitals.
 */
data class JobSummary(val wording: String?, val laborCents: Long?, val partsCents: Long?, val labels: List<String>, val rows: List<PartSummary> = emptyList())

/** One visit from a `service-history` import file, already checked. */
data class ImportedRecord(
    val date: LocalDate,
    /** As the dash read that day, like every reading the user types. */
    val odometer: Int,
    val work: List<ImportedWork>,
    val diy: Boolean,
    val shopName: String? = null,
    val costCents: Long? = null,
    val notes: String? = null,
    /** Its priced jobs, which its [work] lines point at by index; only a file of version 2 or later has any. */
    val jobs: List<ImportedJob> = emptyList(),
    /** What the invoice charged besides the jobs; only a version-3 file has any. */
    val charges: InvoiceCharges = InvoiceCharges.NONE,
) {
    val jobSummaries: List<JobSummary>
        get() = jobs.mapIndexed { i, j ->
            val lines = work.filter { it.job == i }.map { it.label }
            JobSummary(
                j.wording, j.laborCents, j.partsCents, lines,
                j.rows.map { PartSummary(it.wording, it.quantityMilli, it.eachCents, it.totalCents, it.line?.let(lines::getOrNull)) },
            )
        }
}

/** A saved record, as far as an import needs to know it. */
data class SavedRecord(
    val id: Long,
    val date: LocalDate,
    val odometer: Int,
    val labels: List<String>,
    /** Schedule items its lines reset. */
    val itemIds: Set<Long>,
    val diy: Boolean,
    val shopName: String?,
    val costCents: Long?,
    val notes: String?,
    /** A bare "last done" (a date and a mileage for one reminder), which knows less than any real record. */
    val enteredAsLastDone: Boolean,
    val jobs: List<JobSummary> = emptyList(),
    val charges: InvoiceCharges = InvoiceCharges.NONE,
)

/** An imported line with what it is filed under and which reminder, if any, it resets. */
data class ResolvedWork(val label: String, val catalogId: String?, val scheduleItemId: Long?)

/** The `service-history` kind: `docs/import/schema/service-history.schema.json`. */
object HistoryImport {
    const val MAX_ITEMS = 1_000
    const val MAX_WORK = 30
    const val MAX_LABEL = 80
    const val MAX_SHOP = 60
    const val MAX_NOTES = 1_000
    const val MAX_ODOMETER = 9_999_999
    const val MAX_JOBS = 30
    const val MAX_WORDING = 120
    const val MAX_PART_ROWS = 30

    /** What `service` may be: every line the Log service screen offers. */
    val services: Map<String, CatalogItem> = ServiceCatalog.categories.flatMap { it.items }.associateBy { it.id }

    val ODOMETER = DistanceField("odometer", "odometerKm", MAX_ODOMETER, MAX_ODOMETER)

    private val itemKeys = setOf("date", "work", "doneBy", "cost", "notes") + ODOMETER.names
    private val jobKeys = setOf("wording", "labor", "parts", "work")
    private val chargeKeys = setOf("shopSupplies", "tax", "fees")
    private val partRowKeys = setOf("part", "quantity", "each", "total", "for")
    private val date = Regex("""\d{4}-\d{2}-\d{2}""")

    /**
     * A file's records, read by the rules of its [version]. Version 2 adds `jobs` (#481); version 3 a job's
     * `partRows` and the record's `shopSupplies`, `tax` and `fees` (#502). **A version-1 or version-2 file is read
     * exactly as it always was**, and what a later version adds is an unknown field in one.
     */
    fun read(items: JsonElement, check: ImportCheck, today: LocalDate, version: Int = 1): List<ImportedRecord> {
        val list = check.array(items, "items", 1, MAX_ITEMS) ?: return emptyList()
        val seen = Repeats<Pair<LocalDate, Int>>()
        val priced = version >= 2
        val itemized = version >= 3
        return list.mapIndexedNotNull { i, e ->
            val path = ImportCheck.at("items", i)
            val o = if (priced) {
                check.obj(e, path, itemKeys + "jobs" + (if (itemized) chargeKeys else emptySet()), required = setOf("date", "doneBy"))
            } else {
                check.obj(e, path, itemKeys, required = setOf("date", "work", "doneBy"))
            } ?: return@mapIndexedNotNull null
            val on = o["date"]?.let { date(it, "$path.date", check, today) }
            // Miles as `odometer`, or kilometres as `odometerKm`. The limit is the seven digits a dash shows, in either.
            if (!check.has(o, ODOMETER)) check.fail(path, "needs \"${ODOMETER.miles}\" (miles) or \"${ODOMETER.km}\"")
            val odometer = check.distance(o, path, ODOMETER)
            if (priced && "work" !in o && "jobs" !in o) check.fail(path, "needs \"work\", \"jobs\" or both")
            val loose = o["work"]?.let { work(it, "$path.work", check) }
            val jobs = if (priced) o["jobs"]?.let { jobs(it, "$path.jobs", check, itemized) } else null
            val charges = chargeKeys.associateWith { k -> o[k]?.let { money(it, "$path.$k", check, "38.50") } }
            val doneBy = o["doneBy"]?.let { doneBy(it, "$path.doneBy", check) }
            val cost = o["cost"]?.let { cost(it, "$path.cost", check) }
            val notes = o["notes"]?.let { check.text(it, "$path.notes", MAX_NOTES) }
            // Unpriced lines first, then each job's, in the file's order; every line knows its job by index.
            val work = if ("work" in o && loose == null || "jobs" in o && jobs == null) {
                null
            } else {
                (loose.orEmpty() + jobs.orEmpty().flatMapIndexed { j, (_, lines) -> lines.map { it.copy(job = j) } })
                    .takeIf { it.isNotEmpty() }
            }
            if (work != null && work.size > MAX_WORK) {
                check.fail(path, "has ${work.size} lines of work, in \"work\" and \"jobs\" together; the most allowed is $MAX_WORK")
            }
            if (on == null || odometer == null || work == null || work.size > MAX_WORK || doneBy == null) return@mapIndexedNotNull null
            if (chargeKeys.any { it in o && charges[it] == null }) return@mapIndexedNotNull null
            seen.seen(on to odometer, i)?.let { first ->
                check.fail(path, "has the same date and odometer as items[$first]; put the work of one visit in one record")
            }
            ImportedRecord(
                on, odometer, work, doneBy.first, doneBy.second, cost, notes, jobs.orEmpty().map { it.first },
                InvoiceCharges(charges["shopSupplies"], charges["tax"], charges["fees"]),
            )
        }
    }

    /**
     * A record's jobs, each with its own lines. A job has to say something a line alone can't: a price, wording of its
     * own or several lines. From version 3 ([itemized]) its parts may be `partRows` instead of `parts`.
     */
    private fun jobs(e: JsonElement, path: String, check: ImportCheck, itemized: Boolean): List<Pair<ImportedJob, List<ImportedWork>>>? {
        val list = check.array(e, path, 1, MAX_JOBS) ?: return null
        var ok = true
        val says = setOf("wording", "labor", "parts", "partRows")
        val jobs = list.mapIndexedNotNull { i, element ->
            val at = ImportCheck.at(path, i)
            val o = check.obj(element, at, if (itemized) jobKeys + "partRows" else jobKeys, required = setOf("work")) ?: return@mapIndexedNotNull null.also { ok = false }
            val wording = o["wording"]?.let { check.string(it, "$at.wording", MAX_WORDING) }
            val labor = o["labor"]?.let { money(it, "$at.labor", check) }
            val parts = o["parts"]?.let { money(it, "$at.parts", check) }
            val lines = o["work"]?.let { work(it, "$at.work", check) }
            if ("parts" in o && "partRows" in o) {
                check.fail(at, "has both \"parts\" and \"partRows\"; give the parts as one figure or as rows, not both")
                ok = false
            }
            val rows = o["partRows"]?.let { partRows(it, "$at.partRows", check, lines) }
            if (lines != null && lines.size < 2 && o.keys.none { it in says }) {
                check.fail(at, "needs \"labor\", \"parts\", \"wording\" or more than one line of work; a line with none of them goes in the record's \"work\"")
                ok = false
            }
            if (lines == null || ("wording" in o && wording == null) || ("labor" in o && labor == null) || ("parts" in o && parts == null) || ("partRows" in o && rows == null)) {
                ok = false
                return@mapIndexedNotNull null
            }
            ImportedJob(wording, labor, parts, rows.orEmpty()) to lines
        }
        return jobs.takeIf { ok }
    }

    /**
     * A job's part rows, as the invoice prints them. Each says at least one thing; none is worked out from the others.
     * `for` names one of the job's own [lines] by its label (ignoring capitals); left out, the row is for the whole job.
     */
    private fun partRows(e: JsonElement, path: String, check: ImportCheck, lines: List<ImportedWork>?): List<ImportedPartRow>? {
        val list = check.array(e, path, 1, MAX_PART_ROWS) ?: return null
        var ok = true
        val rows = list.mapIndexedNotNull { i, element ->
            val at = ImportCheck.at(path, i)
            val o = check.obj(element, at, partRowKeys) ?: return@mapIndexedNotNull null.also { ok = false }
            if (o.isEmpty()) {
                check.fail(at, "says nothing; a row needs at least one of \"part\", \"quantity\", \"each\" or \"total\"")
                ok = false
            }
            val wording = o["part"]?.let { check.string(it, "$at.part", MAX_WORDING) }
            val quantity = o["quantity"]?.let { quantity(it, "$at.quantity", check) }
            val each = o["each"]?.let { money(it, "$at.each", check, "12.50") }
            val total = o["total"]?.let { money(it, "$at.total", check, "50.00") }
            val line = o["for"]?.let { f ->
                val label = check.string(f, "$at.for", MAX_LABEL) ?: return@let null
                val index = lines?.indexOfFirst { Names.key(it.label) == Names.key(label) } ?: return@let null
                index.takeIf { it >= 0 } ?: null.also { check.fail("$at.for", "must be the label of one of this job's lines of work") }
            }
            if (o.keys.size == 1 && "for" in o) {
                check.fail(at, "says only which line it is for; a row needs at least one of \"part\", \"quantity\", \"each\" or \"total\"")
                ok = false
            }
            if (("part" in o && wording == null) || ("quantity" in o && quantity == null) || ("each" in o && each == null) ||
                ("total" in o && total == null) || ("for" in o && line == null)
            ) {
                ok = false
                return@mapIndexedNotNull null
            }
            ImportedPartRow(wording, quantity, each, total, line)
        }
        return rows.takeIf { ok }
    }

    /** How many, as text so a decimal (oil by the quart) arrives exactly: "4", "5.7", at most three decimals. */
    private fun quantity(e: JsonElement, path: String, check: ImportCheck): Long? {
        val text = (e as? JsonPrimitive)?.takeIf { it.isString }?.content
        return text?.takeIf { Regex("""\d{1,7}(\.\d{1,3})?""").matches(it) }?.let(PartQuantity::parseMilli)
            ?: null.also { check.fail(path, "must be a number as text in quotes, with at most three decimals: \"4\" or \"5.7\"") }
    }

    private fun date(e: JsonElement, path: String, check: ImportCheck, today: LocalDate): LocalDate? {
        val text = (e as? JsonPrimitive)?.takeIf { it.isString }?.content
        val parsed = text?.takeIf { date.matches(it) }?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        } ?: return null.also { check.fail(path, "must be a real date written as \"YYYY-MM-DD\"") }
        if (parsed.isAfter(today)) return null.also { check.fail(path, "is in the future; a record is of work already done") }
        return parsed
    }

    private fun work(e: JsonElement, path: String, check: ImportCheck): List<ImportedWork>? {
        val list = check.array(e, path, 1, MAX_WORK) ?: return null
        var ok = true
        val lines = list.mapIndexedNotNull { i, line ->
            val at = ImportCheck.at(path, i)
            val o = check.obj(line, at, setOf("label", "service"), required = setOf("label"))
            val label = o?.get("label")?.let { check.string(it, "$at.label", MAX_LABEL) }
            val service = o?.get("service")?.let { check.choice(it, "$at.service", services.mapValues { s -> s.key }) }
            if (o == null || label == null || ("service" in o && service == null)) ok = false
            label?.let { ImportedWork(it, service) }
        }
        return lines.takeIf { ok }
    }

    /** DIY or not, and the shop's name if one was given. */
    private fun doneBy(e: JsonElement, path: String, check: ImportCheck): Pair<Boolean, String?>? {
        val o = check.obj(e, path, setOf("type", "name"), required = setOf("type")) ?: return null
        val diy = o["type"]?.let { check.choice(it, "$path.type", mapOf("diy" to true, "shop" to false)) }
        val name = o["name"]?.let { check.string(it, "$path.name", MAX_SHOP) }
        if (diy == true && "name" in o) check.fail("$path.name", "only a shop has a name; leave it out for \"diy\"")
        if (diy == null || ("name" in o && name == null)) return null
        return diy to name.takeIf { !diy }
    }

    /** Dollars as text, so no rounding ever reaches the stored cents: "62.50", "1200". */
    private fun cost(e: JsonElement, path: String, check: ImportCheck): Long? {
        val text = (e as? JsonPrimitive)?.takeIf { it.isString }?.content
        return text?.takeIf { Regex("""\d{1,9}(\.\d{1,2})?""").matches(it) }?.let(Money::parseCents)
            ?: null.also { check.fail(path, "must be dollars as text in quotes, with at most two decimals: \"62.50\"") }
    }

    /**
     * A job's labor or parts, a part row's figures or an invoice charge, written as [cost] is. Zero is a price
     * ("0.00" labor under warranty), not "not entered". [example] is the one the message gives.
     */
    private fun money(e: JsonElement, path: String, check: ImportCheck, example: String = "180.00"): Long? {
        val text = (e as? JsonPrimitive)?.takeIf { it.isString }?.content
        return text?.takeIf { Regex("""\d{1,9}(\.\d{1,2})?""").matches(it) }?.let(Money::parseCents)
            ?: null.also { check.fail(path, "must be dollars as text in quotes, with at most two decimals: \"$example\"") }
    }

    /**
     * What an imported line is filed under and which reminder it resets. A line with a `service`
     * is saved against the item ticking it on Log service would be: the typical item that service
     * resets, or failing that one of the user's own that answers to it. A line without one resets
     * one of the user's own items if its label is that item's name.
     *
     * This is only the item the line is saved against; [itemIds] is everything it counts for.
     */
    fun resolve(line: ImportedWork, schedule: List<ScheduleRef>): ResolvedWork {
        val target = if (line.service != null) {
            services[line.service]?.resets?.let { key -> schedule.firstOrNull { it.templateKey == key } }
                ?: schedule.filter { it.templateKey == null && it.service == line.service }.minByOrNull { it.id }
        } else {
            schedule.firstOrNull { it.templateKey == null && Names.key(it.name) == Names.key(line.label) }
        }
        return ResolvedWork(line.label, line.service, target?.id)
    }

    /** [record]'s lines as they would be saved: each one's (`scheduleItemId`, `catalogId`). */
    fun lines(record: ImportedRecord, schedule: List<ScheduleRef>): List<Pair<Long?, String?>> =
        record.work.map { resolve(it, schedule).scheduleItemId to it.service }

    /** Every schedule item [record] would count for once imported. */
    fun itemIds(record: ImportedRecord, schedule: List<ScheduleRef>): Set<Long> =
        ServiceLinks.itemIds(lines(record, schedule), ServiceLinks.answering(schedule))

    private fun labels(labels: List<String>): List<String> = labels.map(Names::key).sorted()

    private fun jobs(jobs: List<JobSummary>): List<String> =
        jobs.map { j ->
            val rows = j.rows.map { listOf(it.wording?.let(Names::key), it.quantityMilli, it.eachCents, it.totalCents, it.line?.let(Names::key)).toString() }.sorted()
            listOf(j.wording?.let(Names::key), j.laborCents, j.partsCents, labels(j.labels), rows).toString()
        }.sorted()

    /**
     * Which of a saved record's jobs each of a file record's lines stays in, when the file says nothing about prices
     * (no `jobs`, as in every version-1 file): the file can't take away what it doesn't mention. Only when its lines
     * are the saved record's, by label ignoring capitals; each line takes the job of the first saved line with its
     * label not yet taken. Null when the lines differ, and then the saved jobs go with the lines they priced.
     * [saved] is each saved line's label and job.
     */
    fun <J> keptJobs(record: ImportedRecord, saved: List<Pair<String, J?>>): List<J?>? {
        if (record.jobs.isNotEmpty() || labels(record.work.map { it.label }) != labels(saved.map { it.first })) return null
        val left = saved.groupBy({ Names.key(it.first) }, { it.second }).mapValues { ArrayDeque(it.value) }
        return record.work.map { left.getValue(Names.key(it.label)).removeFirst() }
    }

    /**
     * The charges a record replaced from [record] keeps: the file's when it gives any, else [saved], since a file
     * that names no charge (every version-1 and version-2 file) can't take away what it doesn't mention.
     */
    fun keptCharges(record: ImportedRecord, saved: InvoiceCharges): InvoiceCharges = if (record.charges.entered) record.charges else saved

    /**
     * The file against the vehicle's log. A record matches a saved one with the same date and
     * odometer. A saved one that is only a bare "last done" is filled in whatever the mode, as long
     * as the imported record resets every reminder it did, so no reminder loses its last done.
     */
    fun plan(items: List<ImportedRecord>, existing: List<SavedRecord>, schedule: List<ScheduleRef>): ImportPlan<ImportedRecord, SavedRecord> =
        ImportPlans.plan(
            items = items,
            existing = existing,
            key = { it.date to it.odometer },
            existingKey = { it.date to it.odometer },
            same = { i, e ->
                // A file with no jobs says nothing about prices, so the saved ones are no difference (see keptJobs); nor
                // does one with no charges say anything about them (see keptCharges).
                !e.enteredAsLastDone && labels(i.work.map { it.label }) == labels(e.labels) &&
                    i.diy == e.diy && i.shopName == e.shopName && i.costCents == e.costCents && i.notes == e.notes &&
                    (i.jobs.isEmpty() || jobs(i.jobSummaries) == jobs(e.jobs)) &&
                    (!i.charges.entered || i.charges == e.charges)
            },
            blank = { i, e -> e.enteredAsLastDone && itemIds(i, schedule).containsAll(e.itemIds) },
        )

    /**
     * A record from the file whose reading looks wrong. [before] is the reading it jumps far ahead
     * of; null when it doesn't fit the readings around it at all.
     */
    data class Doubt(val entry: ServiceEntry, val before: OdometerReading? = null)

    /**
     * New records (ids below zero) whose odometer looks mistyped once the import is in: worth asking about before
     * they go in, never refused. A report copies whatever the shop typed, and the vehicle's odometer
     * is the highest reading on record, so one slipped digit would put every mileage reminder out.
     * [after] and [updates] are in dash terms, and [unit] is the vehicle's.
     *
     * An odometer only goes up, so the readings that agree with each other are the longest run that
     * never goes down when put in date order; whatever falls outside it is the odd one out. That
     * names the one mistyped reading, not every record it happens to disagree with. Where it could be
     * either of two, a reading already on the vehicle is believed over one from the file.
     *
     * One inside that run is still doubtful when it is far above the reading before it: further than
     * a vehicle is driven in the time ([MileageReadings.farAhead]), or, with a year of readings to
     * go by, over [FAR_PACE] times this vehicle's own pace up to then. The newest reading has nothing
     * after it to disagree with, so the jump is the only sign of a wrong one.
     */
    fun doubtful(after: List<ServiceEntry>, updates: List<OdometerReading>, unit: DistanceUnit): List<Doubt> {
        data class Point(val date: LocalDate, val odometer: Int, val entry: ServiceEntry?) {
            val new get() = entry != null && entry.id < 0
        }
        val points = (after.map { Point(it.date, it.odometer, it) } + updates.map { Point(it.date, it.odometer, null) })
            .sortedWith(compareBy<Point> { it.date }.thenBy { it.odometer })
        if (points.isEmpty()) return emptyList()
        // A saved reading weighs a little more than a new one, which settles ties: the file is the likelier to be wrong.
        val fits = MileageReadings.agreeing(points.map { it.odometer }, weight = { if (points[it].new) 2 else 3 }, preferLater = false)
        val run = fits.sorted()
        return points.mapIndexedNotNull { i, p ->
            if (!p.new) return@mapIndexedNotNull null
            if (i !in fits) return@mapIndexedNotNull Doubt(p.entry!!)
            val k = run.indexOf(i)
            val before = run.getOrNull(k - 1)?.let { OdometerReading(points[it].date, points[it].odometer) } ?: return@mapIndexedNotNull null
            val first = points[run.first()]
            val far = MileageReadings.farAhead(p.odometer, p.date, before, unit) || run.first() != run[k - 1] && fasterThanUsual(
                OdometerReading(first.date, first.odometer), before, OdometerReading(p.date, p.odometer), unit,
            )
            Doubt(p.entry!!, before).takeIf { far }
        }
    }

    /** How many times its usual pace a vehicle has to have gone for a jump to be asked about. */
    const val FAR_PACE = 3

    /**
     * Whether going from [before] to [reading] is over [FAR_PACE] times the pace from [first] to
     * [before], and more than [MileageReadings.FAR_MILES]. Only once a year of readings shows a pace.
     */
    private fun fasterThanUsual(first: OdometerReading, before: OdometerReading, reading: OdometerReading, unit: DistanceUnit): Boolean {
        val history = ChronoUnit.DAYS.between(first.date, before.date)
        if (history < 365) return false
        val delta = reading.odometer.toLong() - before.odometer
        if (delta <= unit.scheduled(MileageReadings.FAR_MILES)) return false
        val usual = ReminderEngine.milesPerDay(listOf(first, before)) ?: 0.0
        val days = ChronoUnit.DAYS.between(before.date, reading.date).coerceAtLeast(1)
        return delta > usual * FAR_PACE * days
    }
}
