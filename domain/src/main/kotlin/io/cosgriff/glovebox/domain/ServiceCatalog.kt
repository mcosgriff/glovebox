package io.cosgriff.glovebox.domain

/**
 * A kind of work offered on the Log service screen. [resets] is a [ScheduleTemplateItem.key].
 *
 * [covers] are other lines this work also counts as: changing an oil includes checking it, so a
 * logged change resets the reminder to check it and completes that job on a milestone. It goes one
 * way only; a check never counts as a change.
 */
data class CatalogItem(val id: String, val resets: String? = null, val covers: Set<String> = emptySet())

data class CatalogCategory(val id: String, val items: List<CatalogItem>)

/**
 * A schedule item as the catalog needs to see it. [service] is the [CatalogItem] id one of the
 * user's own items answers to, if they said so; a typical item answers through its [templateKey].
 */
data class ScheduleRef(val id: Long, val name: String, val templateKey: String?, val tracked: Boolean, val service: String? = null)

/**
 * Which schedule items a line of logged work counts for.
 *
 * A line counts for the item it was saved against, and, when it is one of the catalog's lines, for
 * every item that answers to that line: the typical item the line resets, and any of the user's own
 * items set to it. So one tick of "Oil & filter change" counts for both, and it is worked out from
 * the log each time, never stored: linking an item to a service makes past work count straight away.
 */
object ServiceLinks {
    /**
     * Catalog item id -> the schedule items that are that line's own: the typical item it resets and
     * the user's items set to it. **This is what a line on Log service stands for**, so "Log it" on a
     * reminder ticks its own line and never a line that merely includes it.
     */
    fun own(schedule: List<ScheduleRef>): Map<String, Set<Long>> {
        val byKey = schedule.filter { it.templateKey != null }.associateBy { it.templateKey }
        val out = mutableMapOf<String, MutableSet<Long>>()
        ServiceCatalog.categories.flatMap { it.items }.forEach { item ->
            item.resets?.let(byKey::get)?.let { out.getOrPut(item.id) { mutableSetOf() } += it.id }
        }
        // Only the user's own items carry a service; on a typical item it would be ignored.
        schedule.filter { it.templateKey == null && it.service != null }.forEach { out.getOrPut(it.service!!) { mutableSetOf() } += it.id }
        return out
    }

    /**
     * Catalog item id -> every schedule item a logged line of it counts for: its [own], and those of
     * the lines it covers. **This is what logged work resets**, wider than what the line stands for.
     */
    fun answering(schedule: List<ScheduleRef>): Map<String, Set<Long>> {
        val byKey = schedule.filter { it.templateKey != null }.associateBy { it.templateKey }
        val out = mutableMapOf<String, MutableSet<Long>>()
        ServiceCatalog.categories.flatMap { it.items }.forEach { item ->
            item.resets?.let(byKey::get)?.let { out.getOrPut(item.id) { mutableSetOf() } += it.id }
        }
        // Only the user's own items carry a service; on a typical item it would be ignored.
        schedule.filter { it.templateKey == null && it.service != null }.forEach { out.getOrPut(it.service!!) { mutableSetOf() } += it.id }
        // Work that includes other work answers for that too: a change of oil for the check of it.
        val direct = out.mapValues { it.value.toSet() }
        ServiceCatalog.categories.flatMap { it.items }.forEach { item ->
            item.covers.forEach { covered -> direct[covered]?.let { out.getOrPut(item.id) { mutableSetOf() } += it } }
        }
        return out
    }

    /** [lines] are each line's (schedule item it was saved against, catalog item it was picked from). */
    fun itemIds(lines: List<Pair<Long?, String?>>, answering: Map<String, Set<Long>>): Set<Long> =
        lines.flatMap { (itemId, catalogId) -> listOfNotNull(itemId) + catalogId?.let(answering::get).orEmpty() }.toSet()
}

/** One tickable line on the Log service screen. [scheduleItemId] is the reminder it resets, if any. */
data class WorkOption(
    val id: String,
    /** The user's own item's name, under "Your items"; null for a catalog line, which `app` names by its id. */
    val name: String? = null,
    val scheduleItemId: Long? = null,
)

/** `app` names it by its id ([ServiceCatalog.YOUR_ITEMS] for the user's own items). */
data class WorkCategory(val id: String, val options: List<WorkOption>)

/** The note beside a Log service line; `app` words it. */
sealed interface WorkHint {
    /** What this vehicle takes for the job, as the owner wrote it down. */
    data class Takes(val parts: List<TakesPart>) : WorkHint

    /** How often the reminder it resets comes round. */
    data class Every(val interval: Interval) : WorkHint
}


object ServiceCatalog {
    /**
     * The small note on the right of a Log service line: what this vehicle takes for the job, where
     * the user has written it down, otherwise how often the reminder it resets comes round.
     * [interval] is that reminder's, when the line resets a tracked one. Null when there is nothing
     * worth saying. Nothing here is looked up: it is all the user's own data.
     */
    fun hint(optionId: String, interval: Interval?, specs: WhatItTakes): WorkHint? =
        specs.hint(optionId)?.let(WorkHint::Takes) ?: interval?.let(WorkHint::Every)

    const val YOUR_ITEMS = "yours"

    /** Each template key is reset by at most one catalog item, so a reminder maps to one tick box. */
    val categories: List<CatalogCategory> = listOf(
        CatalogCategory(
            "engine",
            listOf(
                CatalogItem("oil", resets = "oil"),
                CatalogItem("engine-air", resets = "engine-air"),
                CatalogItem("spark-plugs", resets = "spark-plugs"),
                CatalogItem("plug-wires"),
                CatalogItem("ignition-coils"),
                CatalogItem("glow-plugs"),
                CatalogItem("belt", covers = setOf("belt-check")),
                CatalogItem("belt-check"),
                CatalogItem("timing-belt"),
                CatalogItem("timing-chain"),
                CatalogItem("pcv"),
                CatalogItem("engine-sensors"),
                CatalogItem("turbo"),
                CatalogItem("valve-cover-gasket"),
                CatalogItem("head-gasket"),
                CatalogItem("intake-gasket"),
                CatalogItem("oil-pan-gasket"),
                CatalogItem("rear-main-seal"),
                CatalogItem("engine-mounts"),
            ),
        ),
        CatalogCategory(
            "tires",
            listOf(
                CatalogItem("rotation", resets = "rotation"),
                CatalogItem("balance"),
                CatalogItem("alignment"),
                CatalogItem("new-tires"),
                CatalogItem("flat"),
                CatalogItem("tire-check"),
                CatalogItem("tpms"),
            ),
        ),
        CatalogCategory(
            "brakes",
            listOf(
                CatalogItem("front-pads"),
                CatalogItem("rear-pads"),
                // A report that doesn't say which axle; never taken for front or rear.
                CatalogItem("brake-pads"),
                CatalogItem("rotors"),
                CatalogItem("brake-shoes"),
                CatalogItem("brake-drums"),
                CatalogItem("brake-fluid", resets = "brake-fluid", covers = setOf("brake-fluid-check")),
                CatalogItem("brake-fluid-check"),
                CatalogItem("brake-inspect", resets = "brake-inspect"),
                CatalogItem("brake-service"),
                CatalogItem("calipers"),
                CatalogItem("wheel-cylinders"),
                CatalogItem("master-cylinder"),
                CatalogItem("brake-booster"),
                CatalogItem("brake-lines"),
                CatalogItem("parking-brake"),
                CatalogItem("abs-sensor"),
            ),
        ),
        CatalogCategory(
            "fluids",
            listOf(
                CatalogItem("trans-fluid", resets = "trans-fluid"),
                CatalogItem("power-steering"),
                CatalogItem("washer"),
                CatalogItem("fluid-check"),
            ),
        ),
        CatalogCategory(
            "cooling",
            listOf(
                CatalogItem("coolant", resets = "coolant", covers = setOf("cooling-check")),
                CatalogItem("cooling-check"),
                CatalogItem("water-pump"),
                CatalogItem("thermostat"),
                CatalogItem("radiator"),
                CatalogItem("radiator-cap"),
                CatalogItem("coolant-reservoir"),
                CatalogItem("coolant-hoses"),
                CatalogItem("cooling-fan"),
                CatalogItem("fan-clutch"),
                CatalogItem("heater-core"),
            ),
        ),
        CatalogCategory(
            "fuel",
            listOf(
                CatalogItem("fuel-filter"),
                CatalogItem("fuel-system"),
                CatalogItem("throttle-body"),
                CatalogItem("fuel-injectors"),
                CatalogItem("fuel-pump"),
                CatalogItem("evap-system"),
            ),
        ),
        CatalogCategory(
            "exhaust",
            listOf(
                CatalogItem("oxygen-sensor"),
                CatalogItem("egr-valve"),
                CatalogItem("secondary-air-pump"),
                CatalogItem("catalytic-converter"),
                CatalogItem("muffler"),
                // Pipes, hangers and anything else in the exhaust; the converter and muffler have their own lines.
                CatalogItem("exhaust"),
            ),
        ),
        CatalogCategory(
            "driveline",
            listOf(
                CatalogItem(
                    "diff-fluid", resets = "diff-fluid",
                    covers = setOf("front-diff-check", "rear-diff-check", "transfer-case-check"),
                ),
                CatalogItem("front-diff-check"),
                CatalogItem("front-diff", covers = setOf("front-diff-check")),
                CatalogItem("rear-diff-check"),
                CatalogItem("rear-diff", covers = setOf("rear-diff-check")),
                CatalogItem("transfer-case-check"),
                CatalogItem("transfer-case", covers = setOf("transfer-case-check")),
                CatalogItem("driveshaft"),
                CatalogItem("cv-axle"),
                CatalogItem("axle-seals"),
                CatalogItem("u-joints"),
                CatalogItem("clutch"),
                CatalogItem("clutch-hydraulics"),
                CatalogItem("flywheel"),
                CatalogItem("trans-pan-gasket"),
                CatalogItem("shift-cable"),
            ),
        ),
        CatalogCategory(
            "suspension",
            listOf(
                CatalogItem("shocks"),
                CatalogItem("springs"),
                CatalogItem("control-arms"),
                CatalogItem("ball-joints"),
                CatalogItem("bushings"),
                CatalogItem("sway-bar-links"),
                CatalogItem("wheel-bearing"),
                CatalogItem("tie-rods"),
                CatalogItem("steering-rack"),
                CatalogItem("power-steering-pump"),
                // Idler and pitman arms, the column, linkage: what the lines above don't name.
                CatalogItem("steering-parts"),
                CatalogItem("suspension-check"),
            ),
        ),
        CatalogCategory(
            "cabin",
            listOf(
                CatalogItem("cabin-air", resets = "cabin-air"),
                // New blades include looking at the old ones.
                CatalogItem("wipers", covers = setOf("wipers-check")),
                CatalogItem("wipers-check"),
                CatalogItem("battery", covers = setOf("battery-check")),
                CatalogItem("battery-check"),
                CatalogItem("key-fob-battery"),
                CatalogItem("bulbs"),
                CatalogItem("headlight-assembly"),
                CatalogItem("lights-check"),
                CatalogItem("windows-check"),
                CatalogItem("window-regulator"),
                CatalogItem("door-lock-actuator"),
                CatalogItem("air-conditioning"),
                // A flush ends with the system refilled, so it is the service too.
                CatalogItem("ac-flush", covers = setOf("air-conditioning")),
                CatalogItem("ac-compressor"),
                CatalogItem("ac-condenser"),
                CatalogItem("blower-motor"),
                CatalogItem("starter"),
                CatalogItem("alternator"),
                CatalogItem("fuses"),
                CatalogItem("horn"),
            ),
        ),
        CatalogCategory(
            "inspection",
            listOf(
                CatalogItem("multi-point"),
                // "The 30,000 mile service": it doesn't say which jobs, so it resets nothing.
                CatalogItem("scheduled-service"),
                CatalogItem("emissions"),
                CatalogItem("safety-inspection"),
                CatalogItem("recall"),
            ),
        ),
        CatalogCategory(
            "other",
            listOf(
                CatalogItem("accessory"),
                CatalogItem("windshield"),
                CatalogItem("mirror"),
                CatalogItem("door-handle"),
                // Body work and any other glass.
                CatalogItem("body"),
                CatalogItem("detail"),
            ),
        ),
    )

    /**
     * The catalog for one vehicle. A catalog line is linked to the typical item with the matching
     * template key, or failing that to one of the user's own items that answers to it. Tracked
     * items no catalog line covers are listed first under "Your items" so they can be logged too;
     * an item that answers to a catalog line is logged through that line and isn't listed twice.
     *
     * [WorkOption.scheduleItemId] is the one item the line is saved against. Which reminders the
     * line counts for is wider than that: see [ServiceLinks].
     */
    fun forVehicle(schedule: List<ScheduleRef>): List<WorkCategory> {
        val byKey = schedule.filter { it.templateKey != null }.associateBy { it.templateKey }
        // What each line stands for, not everything it would reset: a change of oil isn't the line for the check of it.
        val answering = ServiceLinks.own(schedule)
        val linked = answering.values.flatten().toMutableSet()
        val fromCatalog = categories.map { c ->
            WorkCategory(
                c.id,
                c.items.map { item ->
                    val target = item.resets?.let(byKey::get)?.id ?: answering[item.id]?.minOrNull()
                    WorkOption(item.id, scheduleItemId = target)
                },
            )
        }
        val yours = schedule
            .filter { it.tracked && it.id !in linked }
            .map { WorkOption("schedule-${it.id}", it.name, it.id) }
        return if (yours.isEmpty()) fromCatalog else listOf(WorkCategory(YOUR_ITEMS, yours)) + fromCatalog
    }
}

/** A schedule item being tracked, with the interval and rule already resolved. */
/**
 * [snooze] is whatever is stored for the item, whether or not it has passed.
 *
 * [firstAt] is for a job whose first time isn't one interval in: "first at 100,000 miles, then every
 * 50,000". It is the odometer, in dash terms, the job is first due at. It only matters until the job
 * has been logged once; from then on the interval counts from the last time, as for any other item.
 */
data class TrackedItem(
    val id: Long,
    val name: String,
    val interval: Interval,
    val rule: DueRule,
    val snooze: Snooze? = null,
    val firstAt: Int? = null,
    /**
     * What the dash read when the vehicle had covered no distance: 0, unless the cluster has been
     * replaced. A schedule counts from there, so "one interval from zero" is this plus the interval.
     */
    val zeroAt: Int = 0,
)

/**
 * For a job with nothing logged that the vehicle is already past: the schedule's most recent point
 * for it, [at] on the dash, and how far back that is. A fact about the schedule and the odometer,
 * never a claim that the job was missed; it may have been done and not logged.
 */
data class PastPoint(val at: Int, val ago: Int)

/**
 * [snooze] is set only while a snooze is holding the reminder back; [status] then reads
 * [DueState.OK] but keeps the real distances, so the reminder is honest once the snooze passes.
 */
data class ItemStatus(
    val item: TrackedItem,
    val lastDone: LastDone?,
    val status: DueStatus,
    val snooze: Snooze? = null,
    /**
     * Never logged, and the vehicle hasn't yet reached the mileage the job is first due at:
     * [TrackedItem.firstAt], or one interval from zero. [status] then counts down to that mileage, so this is a reminder like
     * any logged one and not a "Not logged yet". Past that mileage with nothing logged it is false:
     * Glovebox can't know whether the job was done, and asks.
     */
    val awaitingFirst: Boolean = false,
    /** Set only for a job that is "not logged yet" and goes by distance, on a vehicle with a reading. */
    val lastScheduled: PastPoint? = null,
) {
    /** There is something to count from: a logged service, or a first mileage still ahead. */
    val counting: Boolean get() = lastDone != null || awaitingFirst
}

object VehicleReminders {
    /** Latest known odometer from the service log and standalone mileage updates. */
    fun currentOdometer(entries: List<ServiceEntry>, updates: List<OdometerReading>): Int? =
        ServiceLog.readings(entries, updates).maxOfOrNull { it.odometer }

    /** Due status of every tracked item, all derived from the log. */
    fun statuses(
        items: List<TrackedItem>,
        entries: List<ServiceEntry>,
        updates: List<OdometerReading>,
        today: java.time.LocalDate,
        lead: LeadTime = LeadTime(),
    ): List<ItemStatus> {
        val readings = ServiceLog.readings(entries, updates)
        val odometer = readings.maxOfOrNull { it.odometer } ?: 0
        val pace = ReminderEngine.milesPerDay(readings)
        return items.map { item ->
            val last = ServiceLog.lastDone(item.id, entries)
            // A job the vehicle hasn't reached yet can't be owed: with nothing logged it counts down to its
            // first time, which is one interval from zero unless the item says otherwise. That needs a
            // reading to go on, and a job that goes by time alone has no first mileage.
            val step = item.interval.miles?.takeIf { item.rule != DueRule.TIME_ONLY }
            val first = item.firstAt ?: step?.let { item.zeroAt + it }
            if (last == null && first != null && readings.isNotEmpty() && odometer < first) {
                return@map ItemStatus(item, null, ReminderEngine.first(first, odometer, today, pace, lead), awaitingFirst = true)
            }
            val status = ReminderEngine.status(item.interval, item.rule, last, odometer, today, pace, lead)
            if (last == null) {
                // Past its first time with nothing logged: where the schedule last put it, for the screen to say.
                val point = if (first != null && step != null && readings.isNotEmpty()) (first + (odometer - first) / step * step).let { PastPoint(it, odometer - it) } else null
                return@map ItemStatus(item, null, status, lastScheduled = point)
            }
            // A snooze has nothing to hold back until the reminder is due (one never logged has returned above).
            val snooze = item.snooze?.takeIf { status.state != DueState.OK && it.isActive(odometer, today) }
            ItemStatus(item, last, if (snooze != null) status.copy(state = DueState.OK) else status, snooze)
        }
    }
}

object Money {
    /** "$1,234.5" -> 123450. Null when it isn't a plain non-negative amount with at most two decimals. */
    fun parseCents(text: String): Long? {
        val cleaned = text.filterNot { it == '$' || it == ',' || it.isWhitespace() }
        val match = Regex("""(\d{1,12})(?:\.(\d{1,2}))?""").matchEntire(cleaned) ?: return null
        val dollars = match.groupValues[1].toLong()
        val cents = match.groupValues[2].padEnd(2, '0').toLong()
        return dollars * 100 + cents
    }
}

object OdometerInput {
    /**
     * What an odometer field keeps of what is typed: digits only, at most [MileageUpdate.MAX_DIGITS], without
     * leading zeros unless [keepZero] (a replaced cluster can start at 0; a reading anywhere else can't).
     */
    fun typed(text: String, keepZero: Boolean = false): String {
        val digits = text.filter(Char::isDigit)
        return (if (keepZero) digits else digits.trimStart('0')).take(MileageUpdate.MAX_DIGITS)
    }

    /** Whole miles above zero; thousands separators are fine. */
    fun parse(text: String): Int? = WholeNumber.parse(text, min = 1)
}
