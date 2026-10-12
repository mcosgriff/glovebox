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
)

/** The `interval-schedule` kind: `docs/import/schema/interval-schedule.schema.json`. */
object ScheduleImport {
    const val MAX_ITEMS = 200
    const val MAX_NAME = 60
    const val MAX_MILES = 500_000
    const val MAX_MONTHS = 600

    val rules: Map<String, DueRule> = mapOf(
        "whichever-first" to DueRule.WHICHEVER_FIRST,
        "whichever-last" to DueRule.WHICHEVER_LAST,
        "mileage-only" to DueRule.MILEAGE_ONLY,
        "time-only" to DueRule.TIME_ONLY,
    )

    private val intervalKeys = setOf("everyMiles", "everyMonths")
    private val itemKeys = intervalKeys + setOf("name", "severe", "rule")

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
            if (name == null || normal == null) return@mapIndexedNotNull null
            seen.put(key(name), i)?.let { first -> check.fail("$path.name", "\"$name\" is already in this file, at items[$first]") }
            ImportedScheduleItem(name, normal, severe, rule)
        }
    }

    /** `everyMiles`, `everyMonths` or both; one of them has to be there. */
    private fun interval(o: JsonObject, path: String, check: ImportCheck): Interval? {
        val miles = o["everyMiles"]?.let { check.int(it, "$path.everyMiles", 1, MAX_MILES) }
        val months = o["everyMonths"]?.let { check.int(it, "$path.everyMonths", 1, MAX_MONTHS) }
        if ("everyMiles" !in o && "everyMonths" !in o) {
            return null.also { check.fail(path.ifEmpty { "items" }, "needs \"everyMiles\", \"everyMonths\" or both") }
        }
        // Null when a field was there but wrong: that has been reported already.
        if (("everyMiles" in o && miles == null) || ("everyMonths" in o && months == null)) return null
        return Interval(miles, months)
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
            same = { i, e -> i.normal == e.normal && i.severe == e.severe && i.rule == e.rule },
        )

    /**
     * Typical items being tracked that have the same name as something in the file. After the
     * import there would be two reminders for one job, so the user is offered the choice to stop
     * tracking these. Only `tracked` would change, and only if they say so.
     */
    fun doubles(items: List<ImportedScheduleItem>, existing: List<SavedScheduleItem>): List<SavedScheduleItem> {
        val names = items.map { key(it.name) }.toSet()
        return existing.filter { it.templateKey != null && it.tracked && key(it.name) in names }
    }
}
