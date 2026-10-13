package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Locale

/** One job from an `interval-schedule` import file, already checked. [rule] null means the app-wide default. */
data class ImportedScheduleItem(
    val name: String,
    val normal: Interval,
    val severe: Interval? = null,
    val rule: DueRule? = null,
    /** The built-in service line that resets it, as a [CatalogItem] id; null when it is logged under "Your items". */
    val service: String? = null,
    /**
     * The odometer reading the job is first due at, when that isn't one interval in ("first at 100,000,
     * then every 50,000"); in the file's unit. It matters only until the job has been logged once.
     */
    val firstAt: Int? = null,
)

/**
 * A saved schedule item, as far as an import needs to know it. [templateKey] is set for the typical
 * items Glovebox suggested, which an import never matches or changes.
 */
data class SavedScheduleItem(
    val id: Long,
    val name: String,
    val normal: Interval?,
    val severe: Interval?,
    val rule: DueRule?,
    val templateKey: String?,
    val tracked: Boolean,
    val service: String? = null,
    val firstAt: Int? = null,
)

/** The `interval-schedule` kind: `docs/import/schema/interval-schedule.schema.json`. */
object ScheduleImport {
    const val MAX_ITEMS = 200
    const val MAX_NAME = 60
    const val MAX_MILES = 500_000

    /** The same limit for an interval given in kilometres: 500,000 mi as a schedule figure. */
    const val MAX_KM = 800_000
    const val MAX_MONTHS = 600

    val rules: Map<String, DueRule> = mapOf(
        "whichever-first" to DueRule.WHICHEVER_FIRST,
        "whichever-last" to DueRule.WHICHEVER_LAST,
        "mileage-only" to DueRule.MILEAGE_ONLY,
        "time-only" to DueRule.TIME_ONLY,
    )

    private val intervalKeys = setOf("everyMiles", "everyKm", "everyMonths")
    /** The file's fields for a first time that isn't one interval in: one or the other, in the file's unit. */
    const val FIRST = "firstAtMiles"
    const val FIRST_KM = "firstAtKm"

    private val itemKeys = intervalKeys + setOf("name", "severe", "rule", "service", FIRST, FIRST_KM)

    fun read(items: JsonElement, check: ImportCheck): List<ImportedScheduleItem> {
        val list = check.array(items, "items", 1, MAX_ITEMS) ?: return emptyList()
        val seen = mutableMapOf<String, Int>()
        return list.mapIndexedNotNull { i, e ->
            val path = ImportCheck.at("items", i)
            val o = check.obj(e, path, itemKeys, required = setOf("name")) ?: return@mapIndexedNotNull null
            val name = o["name"]?.let { check.string(it, "$path.name", MAX_NAME) }
            val normal = interval(o, path, check)
            val severe = o["severe"]?.let { s -> check.obj(s, "$path.severe", intervalKeys)?.let { interval(it, "$path.severe", check) } }
            val rule = o["rule"]?.let { check.choice(it, "$path.rule", rules) }
            val service = o["service"]?.let { check.choice(it, "$path.service", HistoryImport.services.mapValues { s -> s.key }) }
            val first = check.distance(o, path, FIRST, FIRST_KM, MAX_MILES, MAX_KM)
            if (name == null || normal == null || (check.has(o, FIRST, FIRST_KM) && first == null)) return@mapIndexedNotNull null
            seen.put(key(name), i)?.let { first -> check.fail("$path.name", "\"$name\" is already in this file, at items[$first]") }
            ImportedScheduleItem(name, normal, severe, rule, service, first)
        }
    }

    /**
     * A distance (`everyMiles`, or `everyKm`), `everyMonths`, or a distance and months; one of them
     * has to be there. The distance comes back in the file's own unit ([ImportCheck.unit]).
     */
    private fun interval(o: JsonObject, path: String, check: ImportCheck): Interval? {
        val distance = check.distance(o, path, "everyMiles", "everyKm", MAX_MILES, MAX_KM)
        val months = o["everyMonths"]?.let { check.int(it, "$path.everyMonths", 1, MAX_MONTHS) }
        if (!check.has(o, "everyMiles", "everyKm") && "everyMonths" !in o) {
            return null.also { check.fail(path.ifEmpty { "items" }, "needs \"everyMiles\" or \"everyKm\", \"everyMonths\", or a distance and months") }
        }
        // Null when a field was there but wrong: that has been reported already.
        if ((check.has(o, "everyMiles", "everyKm") && distance == null) || ("everyMonths" in o && months == null)) return null
        return Interval(distance, months)
    }

    /** Two items are the same job when their names agree, ignoring case. */
    fun key(name: String): String = name.trim().lowercase(Locale.US)

    /**
     * The file against the items the user added themselves. The typical items Glovebox suggested
     * are left out of the matching altogether, so an import can add beside them but never change one.
     */
    fun plan(items: List<ImportedScheduleItem>, existing: List<SavedScheduleItem>): ImportPlan<ImportedScheduleItem, SavedScheduleItem> =
        ImportPlans.plan(
            items = items,
            existing = existing.filter { it.templateKey == null },
            key = { key(it.name) },
            existingKey = { key(it.name) },
            same = { i, e -> i.normal == e.normal && i.severe == e.severe && i.rule == e.rule && i.service == e.service && i.firstAt == e.firstAt },
        )

    /**
     * Typical items being tracked that are the same job as something in the file: the same name,
     * or the item the file's `service` resets. After the import there would be two reminders for one
     * job, so the user is offered the choice to stop tracking these. Only `tracked` would change,
     * and only if they say so; left alone, both stay and both are reset by the same logged work.
     */
    fun doubles(items: List<ImportedScheduleItem>, existing: List<SavedScheduleItem>): List<SavedScheduleItem> {
        val names = items.map { key(it.name) }.toSet()
        val keys = items.mapNotNull { it.service?.let(HistoryImport.services::get)?.resets }.toSet()
        return existing.filter { it.templateKey != null && it.tracked && (key(it.name) in names || it.templateKey in keys) }
    }
}
