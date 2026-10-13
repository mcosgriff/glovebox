package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A manufacturer's maintenance schedule that ships with the app for one model: the table as the
 * maker's guide prints it, and the reminder intervals that follow from it. [id] is kept on what is
 * added from it (`vehicles.milestonesKnownList`, `schedule_items.knownList`), for good.
 */
data class KnownScheduleList(
    val id: String,
    /** "2017–2024 4Runner" */
    val title: String,
    val make: String,
    val model: String,
    val years: IntRange,
    val source: KnownSpecSource,
    /** What [milestones] and [intervals] are in. A vehicle that counts in the other gets them as schedule figures. */
    val unit: DistanceUnit,
    /** The guide's table, every line of it, in mileage order. */
    val milestones: List<ImportedMilestone>,
    /** The jobs of the table that come round evenly, as reminders. Never anything the table doesn't say. */
    val intervals: List<ImportedScheduleItem>,
)

sealed interface KnownScheduleFile {
    data class Ok(val list: KnownScheduleList) : KnownScheduleFile
    data class Invalid(val errors: List<ImportError>) : KnownScheduleFile
}

/** What adding a list's reminders to a vehicle would do. Nothing the user has is ever written over. */
data class KnownIntervalPlan(
    /** The user has no item of their own with this name. */
    val new: List<ImportedScheduleItem>,
    /** The user already has an item with this name. It is theirs, and stays exactly as it is. */
    val kept: List<ImportedScheduleItem>,
    /** Typical items being tracked that would be a second reminder for a job in [new]. */
    val doubles: List<SavedScheduleItem>,
)

/**
 * Manufacturers' maintenance schedules that ship with the app.
 *
 * **Glovebox has no schedule of its own for any model and works none out.** A bundled schedule is a
 * maker's published guide, named: the file says which document it was read from and why it may be
 * shipped ([KnownSpecSource]), it is offered only for the make, model and years whose guides were
 * read, it is never applied without being asked for, and what comes from it says so for good, with
 * the advice to check it against the owner's own guide. Only the facts are taken (mileages, months,
 * which jobs); the jobs are named in Glovebox's words.
 *
 * A list is a file in `assets/known-schedules/`. Its `milestones` are written exactly as the items
 * of a `milestone-schedule` import file and its `intervals` as those of an `interval-schedule` one,
 * and both are read by the same strict readers. **An interval has to be the table's own:** [parse]
 * refuses one whose job isn't in the table at exactly the multiples of that interval, so a reminder
 * can never say something the guide's table doesn't. A job that doesn't come round evenly (a first
 * replacement at one mileage and a shorter gap afterwards, say) stays in the table only.
 */
object KnownSchedules {
    /** The folder in the app's assets the lists live in. */
    const val ASSET_DIR = "known-schedules"

    private val keys = KnownLists.headerKeys + setOf("milestones", "intervals")

    fun parse(text: String): KnownScheduleFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return KnownScheduleFile.Invalid(listOf(ImportError("", "This isn't a JSON file.")))
        }
        val check = ImportCheck()
        val o = check.obj(root, "", keys, required = keys) ?: return KnownScheduleFile.Invalid(check.invalid().errors)
        val header = KnownLists.header(o, check)
        // Both readers report under "items", as they do for an import file; the two errors lists are kept apart.
        val milestones = o["milestones"]?.let { MilestoneImport.read(it, check) }.orEmpty()
        val tableFailed = check.failed
        val intervals = o["intervals"]?.let { ScheduleImport.read(it, check) }.orEmpty()
        if (!tableFailed && !check.failed) contradictions(milestones, intervals).forEach { check.fail("intervals", it) }
        if (check.failed || header == null) return KnownScheduleFile.Invalid(check.invalid().errors)
        return KnownScheduleFile.Ok(
            KnownScheduleList(header.id, header.title, header.make, header.model, header.years, header.source, check.unit, milestones, intervals),
        )
    }

    /**
     * Where [intervals] say something [table] doesn't. An interval is the table's own when its job
     * (the same service, or failing that the same name) is at every multiple of the interval up to
     * the table's end and nowhere else, and its months are the months printed at those milestones.
     */
    fun contradictions(table: List<ImportedMilestone>, intervals: List<ImportedScheduleItem>): List<String> {
        val end = table.maxOfOrNull { it.atMiles } ?: return listOf("there is no table for the intervals to come from")
        return intervals.mapNotNull { item ->
            val key = MilestoneImport.jobKey(TableJob(item.name, item.service))
            val at = table.filter { m -> m.work.any { MilestoneImport.jobKey(it) == key } }
            val every = item.normal.miles
            when {
                every == null -> "\"${item.name}\" has no distance; a table is by distance"
                at.isEmpty() -> "\"${item.name}\" isn't a job in the table"
                at.map { it.atMiles } != (every..end step every).toList() ->
                    "\"${item.name}\" is in the table at ${at.joinToString { it.atMiles.toString() }}, which isn't every $every"
                at.any { m -> m.orMonths != null && item.normal.months != null && m.orMonths != item.normal.months * (m.atMiles / every) } ->
                    "\"${item.name}\" has months that aren't the table's"
                else -> null
            }
        }
    }

    /** The list for a vehicle, or null. As for torque: the make, model and a year whose guide was read; no near misses. */
    fun match(lists: List<KnownScheduleList>, year: Int, make: String, model: String): KnownScheduleList? =
        lists.firstOrNull { year in it.years && KnownLists.same(it.make, make) && KnownLists.same(it.model, model) }

    /** [list]'s table and intervals in [unit], the vehicle's. Nothing changes when the two agree. */
    fun milestonesIn(list: KnownScheduleList, unit: DistanceUnit): List<ImportedMilestone> =
        FileDistances.milestones(list.milestones, null, list.unit, unit).first

    fun intervalsIn(list: KnownScheduleList, unit: DistanceUnit): List<ImportedScheduleItem> =
        FileDistances.schedule(list.intervals, list.unit, unit)

    /**
     * What adding [intervals] (already in the vehicle's unit) would do. Each becomes an item of the
     * user's own. Unlike an import there is no "replace": an item of theirs with the same name stays
     * as it is. A typical item is never changed either; the ones that would now be a second reminder
     * for the same job are [KnownIntervalPlan.doubles], which the user is offered to stop tracking.
     */
    fun plan(intervals: List<ImportedScheduleItem>, existing: List<SavedScheduleItem>): KnownIntervalPlan {
        val mine = existing.filter { it.templateKey == null }.map { ScheduleImport.key(it.name) }.toSet()
        val (kept, new) = intervals.partition { ScheduleImport.key(it.name) in mine }
        return KnownIntervalPlan(new, kept, ScheduleImport.doubles(new, existing))
    }

    /** "Toyota Warranty and Maintenance Guide, 2017–2024 4Runner" */
    fun credit(list: KnownScheduleList): String = "${list.source.name}, ${list.title}"

    /** What is said where the table is offered. */
    fun tableOffer(list: KnownScheduleList): String =
        "${count(list.milestones.size, "milestone")}, as the ${list.source.name} lists them. ${list.source.note} " +
            "It is for reference here: it doesn't create or change a reminder."

    /** What is said where the reminders are offered. */
    fun intervalOffer(list: KnownScheduleList, plan: KnownIntervalPlan): String =
        "${count(plan.new.size, "reminder")} from the ${list.source.name}: the jobs its table repeats evenly. ${list.source.note}" +
            when (plan.kept.size) {
                0 -> ""
                1 -> " 1 item of your own with the same name is left as it is."
                else -> " ${plan.kept.size} items of your own with the same names are left as they are."
            }

    /** The line kept beside whatever came from a list. */
    fun startedFrom(credit: String?): String =
        "Started from the ${credit ?: "maker's maintenance guide"}. Check it against the guide for your own vehicle."

    private fun count(n: Int, noun: String) = if (n == 1) "1 $noun" else "$n ${noun}s"
}
