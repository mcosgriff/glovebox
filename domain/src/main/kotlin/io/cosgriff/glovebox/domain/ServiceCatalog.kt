package io.cosgriff.glovebox.domain

/** A kind of work offered on the Log service screen. [resets] is a [ScheduleTemplateItem.key]. */
data class CatalogItem(val id: String, val label: String, val resets: String? = null)

data class CatalogCategory(val id: String, val label: String, val items: List<CatalogItem>)

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
    /** Catalog item id -> the schedule items that answer to it. */
    fun answering(schedule: List<ScheduleRef>): Map<String, Set<Long>> {
        val byKey = schedule.filter { it.templateKey != null }.associateBy { it.templateKey }
        val out = mutableMapOf<String, MutableSet<Long>>()
        ServiceCatalog.categories.flatMap { it.items }.forEach { item ->
            item.resets?.let(byKey::get)?.let { out.getOrPut(item.id) { mutableSetOf() } += it.id }
        }
        // Only the user's own items carry a service; on a typical item it would be ignored.
        schedule.filter { it.templateKey == null && it.service != null }.forEach { out.getOrPut(it.service!!) { mutableSetOf() } += it.id }
        return out
    }

    /** [lines] are each line's (schedule item it was saved against, catalog item it was picked from). */
    fun itemIds(lines: List<Pair<Long?, String?>>, answering: Map<String, Set<Long>>): Set<Long> =
        lines.flatMap { (itemId, catalogId) -> listOfNotNull(itemId) + catalogId?.let(answering::get).orEmpty() }.toSet()
}

/** One tickable line on the Log service screen. [scheduleItemId] is the reminder it resets, if any. */
data class WorkOption(val id: String, val label: String, val scheduleItemId: Long? = null)

data class WorkCategory(val id: String, val label: String, val options: List<WorkOption>)

/** What the user has written down on Vehicle details. Any of it may be missing. */
data class VehicleSpecs(val oil: String? = null, val tireSize: String? = null, val wiperBlades: String? = null)

object ServiceCatalog {
    /**
     * The small note on the right of a Log service line: what this vehicle takes for the job, where
     * the user has written it down, otherwise how often the reminder it resets comes round.
     * [interval] is that reminder's, when the line resets a tracked one. Null when there is nothing
     * worth saying. Nothing here is looked up: it is all the user's own data.
     */
    fun hint(optionId: String, interval: Interval?, specs: VehicleSpecs, unit: DistanceUnit): String? {
        val spec = when (optionId) {
            "oil" -> specs.oil
            "new-tires" -> specs.tireSize
            "wipers" -> specs.wiperBlades
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
        return spec ?: interval?.let { "Every ${it.label(unit)}" }
    }

    const val YOUR_ITEMS = "yours"

    /** Each template key is reset by at most one catalog item, so a reminder maps to one tick box. */
    val categories: List<CatalogCategory> = listOf(
        CatalogCategory(
            "engine", "Oil & engine",
            listOf(
                CatalogItem("oil", "Oil & filter change", resets = "oil"),
                CatalogItem("engine-air", "Engine air filter", resets = "engine-air"),
                CatalogItem("spark-plugs", "Spark plugs", resets = "spark-plugs"),
                CatalogItem("belt", "Serpentine belt"),
                CatalogItem("pcv", "PCV valve"),
            ),
        ),
        CatalogCategory(
            "tires", "Tires & wheels",
            listOf(
                CatalogItem("rotation", "Tire rotation", resets = "rotation"),
                CatalogItem("balance", "Balance"),
                CatalogItem("alignment", "Alignment"),
                CatalogItem("new-tires", "New tires"),
                CatalogItem("flat", "Flat repair"),
            ),
        ),
        CatalogCategory(
            "brakes", "Brakes",
            listOf(
                CatalogItem("front-pads", "Front pads"),
                CatalogItem("rear-pads", "Rear pads"),
                CatalogItem("rotors", "Rotors"),
                CatalogItem("brake-fluid", "Brake fluid flush", resets = "brake-fluid"),
                CatalogItem("brake-inspect", "Brake inspection", resets = "brake-inspect"),
            ),
        ),
        CatalogCategory(
            "fluids", "Fluids",
            listOf(
                CatalogItem("coolant", "Coolant", resets = "coolant"),
                CatalogItem("trans-fluid", "Transmission fluid", resets = "trans-fluid"),
                CatalogItem("power-steering", "Power steering fluid"),
                CatalogItem("washer", "Washer fluid"),
            ),
        ),
        CatalogCategory(
            "driveline", "Driveline",
            listOf(
                CatalogItem("diff-fluid", "Differential / transfer case fluid", resets = "diff-fluid"),
                CatalogItem("driveshaft", "Driveshaft grease"),
                CatalogItem("cv-axle", "CV axle / boot"),
            ),
        ),
        CatalogCategory(
            "cabin", "Cabin & electrical",
            listOf(
                CatalogItem("cabin-air", "Cabin air filter", resets = "cabin-air"),
                CatalogItem("wipers", "Wiper blades"),
                CatalogItem("battery", "Battery"),
                CatalogItem("bulbs", "Bulbs"),
            ),
        ),
        CatalogCategory(
            "inspection", "Inspection",
            listOf(
                CatalogItem("multi-point", "Multi-point inspection"),
                CatalogItem("emissions", "Emissions test"),
                CatalogItem("recall", "Recall work"),
            ),
        ),
        CatalogCategory(
            "other", "Other",
            listOf(
                CatalogItem("accessory", "Accessory install"),
                CatalogItem("body", "Body / glass"),
                CatalogItem("detail", "Detail / wash"),
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
        val answering = ServiceLinks.answering(schedule)
        val linked = answering.values.flatten().toMutableSet()
        val fromCatalog = categories.map { c ->
            WorkCategory(
                c.id, c.label,
                c.items.map { item ->
                    val target = item.resets?.let(byKey::get)?.id ?: answering[item.id]?.minOrNull()
                    WorkOption(item.id, item.label, target)
                },
            )
        }
        val yours = schedule
            .filter { it.tracked && it.id !in linked }
            .map { WorkOption("schedule-${it.id}", it.name, it.id) }
        return if (yours.isEmpty()) fromCatalog else listOf(WorkCategory(YOUR_ITEMS, "Your items", yours)) + fromCatalog
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
data class TrackedItem(val id: Long, val name: String, val interval: Interval, val rule: DueRule, val snooze: Snooze? = null, val firstAt: Int? = null)

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
     * Never logged, and the vehicle hasn't yet reached the mileage the job is first due at
     * ([TrackedItem.firstAt]). [status] then counts down to that mileage, so this is a reminder like
     * any logged one and not a "Not logged yet". Past that mileage with nothing logged it is false:
     * Glovebox can't know whether the job was done, and asks.
     */
    val awaitingFirst: Boolean = false,
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
            if (last == null && item.firstAt != null && odometer < item.firstAt) {
                return@map ItemStatus(item, null, ReminderEngine.first(item.firstAt, odometer, today, pace, lead), awaitingFirst = true)
            }
            val status = ReminderEngine.status(item.interval, item.rule, last, odometer, today, pace, lead)
            // A snooze has nothing to hold back until the reminder is due, or before it was ever logged.
            val snooze = item.snooze?.takeIf { last != null && status.state != DueState.OK && it.isActive(odometer, today) }
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
    /** Whole miles above zero; thousands separators are fine. */
    fun parse(text: String): Int? {
        val cleaned = text.filterNot { it == ',' || it.isWhitespace() }
        if (cleaned.isEmpty() || !cleaned.all(Char::isDigit)) return null
        return cleaned.toIntOrNull()?.takeIf { it > 0 }
    }
}
