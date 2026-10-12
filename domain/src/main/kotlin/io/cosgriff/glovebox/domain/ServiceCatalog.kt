package io.cosgriff.glovebox.domain

/** A kind of work offered on the Log service screen. [resets] is a [ScheduleTemplateItem.key]. */
data class CatalogItem(val id: String, val label: String, val resets: String? = null)

data class CatalogCategory(val id: String, val label: String, val items: List<CatalogItem>)

/** A schedule item as the catalog needs to see it. */
data class ScheduleRef(val id: Long, val name: String, val templateKey: String?, val tracked: Boolean)

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
    fun hint(optionId: String, interval: Interval?, specs: VehicleSpecs): String? {
        val spec = when (optionId) {
            "oil" -> specs.oil
            "new-tires" -> specs.tireSize
            "wipers" -> specs.wiperBlades
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
        return spec ?: interval?.let { "Every ${it.label()}" }
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
     * The catalog for one vehicle. Catalog lines are linked to the vehicle's schedule item with the
     * matching template key; tracked schedule items the catalog doesn't cover (the user's own) are
     * listed first under "Your items" so they can be logged too.
     */
    fun forVehicle(schedule: List<ScheduleRef>): List<WorkCategory> {
        val byKey = schedule.filter { it.templateKey != null }.associateBy { it.templateKey }
        val linked = mutableSetOf<Long>()
        val fromCatalog = categories.map { c ->
            WorkCategory(
                c.id, c.label,
                c.items.map { item ->
                    val target = item.resets?.let(byKey::get)
                    target?.let { linked += it.id }
                    WorkOption(item.id, item.label, target?.id)
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
/** [snooze] is whatever is stored for the item, whether or not it has passed. */
data class TrackedItem(val id: Long, val name: String, val interval: Interval, val rule: DueRule, val snooze: Snooze? = null)

/**
 * [snooze] is set only while a snooze is holding the reminder back; [status] then reads
 * [DueState.OK] but keeps the real distances, so the reminder is honest once the snooze passes.
 */
data class ItemStatus(val item: TrackedItem, val lastDone: LastDone?, val status: DueStatus, val snooze: Snooze? = null)

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
