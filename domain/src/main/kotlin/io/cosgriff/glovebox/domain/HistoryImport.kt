package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** One line of work on an imported record. [service] is a [CatalogItem] id; null for anything else. */
data class ImportedWork(val label: String, val service: String? = null)

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
)

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

    /** What `service` may be: every line the Log service screen offers. */
    val services: Map<String, CatalogItem> = ServiceCatalog.categories.flatMap { it.items }.associateBy { it.id }

    val ODOMETER = DistanceField("odometer", "odometerKm", MAX_ODOMETER, MAX_ODOMETER)

    private val itemKeys = setOf("date", "work", "doneBy", "cost", "notes") + ODOMETER.names
    private val date = Regex("""\d{4}-\d{2}-\d{2}""")

    fun read(items: JsonElement, check: ImportCheck, today: LocalDate): List<ImportedRecord> {
        val list = check.array(items, "items", 1, MAX_ITEMS) ?: return emptyList()
        val seen = Repeats<Pair<LocalDate, Int>>()
        return list.mapIndexedNotNull { i, e ->
            val path = ImportCheck.at("items", i)
            val o = check.obj(e, path, itemKeys, required = setOf("date", "work", "doneBy")) ?: return@mapIndexedNotNull null
            val on = o["date"]?.let { date(it, "$path.date", check, today) }
            // Miles as `odometer`, or kilometres as `odometerKm`. The limit is the seven digits a dash shows, in either.
            if (!check.has(o, ODOMETER)) check.fail(path, "needs \"${ODOMETER.miles}\" (miles) or \"${ODOMETER.km}\"")
            val odometer = check.distance(o, path, ODOMETER)
            val work = o["work"]?.let { work(it, "$path.work", check) }
            val doneBy = o["doneBy"]?.let { doneBy(it, "$path.doneBy", check) }
            val cost = o["cost"]?.let { cost(it, "$path.cost", check) }
            val notes = o["notes"]?.let { check.text(it, "$path.notes", MAX_NOTES) }
            if (on == null || odometer == null || work == null || doneBy == null) return@mapIndexedNotNull null
            seen.seen(on to odometer, i)?.let { first ->
                check.fail(path, "has the same date and odometer as items[$first]; put the work of one visit in one record")
            }
            ImportedRecord(on, odometer, work, doneBy.first, doneBy.second, cost, notes)
        }
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

    /** Every schedule item [record] would count for once imported. */
    fun itemIds(record: ImportedRecord, schedule: List<ScheduleRef>): Set<Long> =
        ServiceLinks.itemIds(record.work.map { resolve(it, schedule).scheduleItemId to it.service }, ServiceLinks.answering(schedule))

    private fun labels(labels: List<String>): List<String> = labels.map(Names::key).sorted()

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
                !e.enteredAsLastDone && labels(i.work.map { it.label }) == labels(e.labels) &&
                    i.diy == e.diy && i.shopName == e.shopName && i.costCents == e.costCents && i.notes == e.notes
            },
            blank = { i, e -> e.enteredAsLastDone && itemIds(i, schedule).containsAll(e.itemIds) },
        )

    /** The log as it would stand after the import, for working out which reminders move. Dash terms in, dash terms out. */
    fun entriesAfter(
        plan: ImportPlan<ImportedRecord, SavedRecord>,
        mode: ImportMode,
        before: List<ServiceEntry>,
        schedule: List<ScheduleRef>,
        swap: ClusterSwap?,
    ): List<ServiceEntry> {
        fun ids(r: ImportedRecord) = itemIds(r, schedule)
        val replaced = plan.replaced(mode).associate { (item, saved) -> saved.id to ids(item) }
        val kept = before.map { e -> replaced[e.id]?.let { e.copy(itemIds = it) } ?: e }
        // Ids below zero: not saved yet, and never equal to a real record's.
        val added = plan.new.mapIndexed { i, r -> swap.toDash(ServiceEntry(-1L - i, r.date, r.odometer, ids(r))) }
        return kept + added
    }

    /**
     * A record from the file whose reading looks wrong. [before] is the reading it jumps far ahead
     * of; null when it doesn't fit the readings around it at all.
     */
    data class Doubt(val entry: ServiceEntry, val before: OdometerReading? = null)

    /**
     * New records whose odometer looks mistyped once the import is in: worth asking about before
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
