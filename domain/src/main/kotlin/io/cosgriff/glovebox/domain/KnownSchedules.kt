package io.cosgriff.glovebox.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A manufacturer's maintenance schedule that ships with the app for one model: the table as the
 * maker's guide prints it, and the reminder intervals that follow from it. [id] is kept on what is
 * added from it (`vehicles.milestonesKnownList`, `schedule_items.knownList`), for good.
 */
/**
 * The engine a list is for, when a model's guide has a different schedule for each.
 * [label] is what the owner is shown when they have to say which is theirs ("2.7L 4-cylinder").
 * [fuel] is set only where two engines of one size differ by it ("Diesel"), as vPIC spells it.
 */
data class KnownEngine(val label: String, val displacementL: Double, val fuel: String? = null)

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
    /**
     * The names of the jobs the guide marks as for four-wheel drive only, in the table or among the
     * reminders. A vehicle known to drive two wheels doesn't get them ([KnownSchedules.applies]).
     */
    val fourWheelDriveOnly: Set<String> = emptySet(),
    /**
     * The names of the jobs that aren't for a manual gearbox (a dual-clutch or automatic
     * transmission's fluid change). A vehicle known to have a manual doesn't get them.
     */
    val manualExcluded: Set<String> = emptySet(),
    /** Null for a list that is for every engine the model had in those years. */
    val engine: KnownEngine? = null,
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

    private val required = KnownLists.headerKeys + setOf("milestones", "intervals")
    private val keys = required + setOf("fourWheelDriveOnly", "manualExcluded", "engine")

    fun parse(text: String): KnownScheduleFile {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            return KnownScheduleFile.Invalid(listOf(ImportError("", "This isn't a JSON file.")))
        }
        val check = ImportCheck()
        val o = check.obj(root, "", keys, required = required) ?: return KnownScheduleFile.Invalid(check.invalid().errors)
        val header = KnownLists.header(o, check)
        // Both readers report under "items", as they do for an import file; the two errors lists are kept apart.
        val milestones = o["milestones"]?.let { MilestoneImport.read(it, check) }.orEmpty()
        val tableFailed = check.failed
        val intervals = o["intervals"]?.let { ScheduleImport.read(it, check) }.orEmpty()
        if (!tableFailed && !check.failed) contradictions(milestones, intervals).forEach { check.fail("intervals", it) }
        val names = (milestones.flatMap { m -> m.work.map { it.name } } + intervals.map { it.name }).map(MilestoneImport::nameKey).toSet()
        // A field that names some of the list's jobs: every name has to be one.
        fun jobs(field: String): Set<String> = o[field]?.let { e ->
            check.array(e, field, 1, 200)?.mapIndexedNotNull { i, n ->
                val path = ImportCheck.at(field, i)
                check.string(n, path, MilestoneImport.MAX_NAME)?.also { if (MilestoneImport.nameKey(it) !in names) check.fail(path, "\"$it\" isn't a job in this list") }
            }
        }.orEmpty().map(MilestoneImport::nameKey).toSet()
        val fourWheel = jobs("fourWheelDriveOnly")
        val notManual = jobs("manualExcluded")
        val engine = o["engine"]?.let { e ->
            check.obj(e, "engine", setOf("label", "displacementL", "fuel"), required = setOf("label", "displacementL"))?.let { en ->
                val label = en["label"]?.let { check.string(it, "engine.label", 40) }
                val litres = en["displacementL"]?.let { check.number(it, "engine.displacementL") }
                val fuel = en["fuel"]?.let { check.string(it, "engine.fuel", 30) }
                if (litres != null && (litres < 0.1 || litres > 20.0)) check.fail("engine.displacementL", "must be between 0.1 and 20")
                if (label != null && litres != null && ("fuel" !in en || fuel != null)) KnownEngine(label, litres, fuel) else null
            }
        }
        if (check.failed || header == null) return KnownScheduleFile.Invalid(check.invalid().errors)
        return KnownScheduleFile.Ok(
            KnownScheduleList(
                header.id, header.title, header.make, header.model, header.years, header.source, check.unit, milestones, intervals,
                fourWheelDriveOnly = fourWheel,
                manualExcluded = notManual,
                engine = engine,
            ),
        )
    }

    /**
     * Where [intervals] say something [table] doesn't. An interval is the table's own when its job
     * (the same service, or failing that the same name) is at every multiple of the interval up to
     * the table's end and nowhere else, and its months are the months printed at those milestones.
     * One with a first time of its own ([ImportedScheduleItem.firstAt]) is in the table at that
     * mileage and then every interval after it; the table's months aren't held against it, since
     * they count from new and the interval's from the last time.
     */
    fun contradictions(table: List<ImportedMilestone>, intervals: List<ImportedScheduleItem>): List<String> {
        val end = table.maxOfOrNull { it.atMiles } ?: return listOf("there is no table for the intervals to come from")
        return intervals.mapNotNull { item ->
            val key = MilestoneImport.jobKey(TableJob(item.name, item.service))
            val at = table.filter { m -> m.work.any { MilestoneImport.jobKey(it) == key } }
            val every = item.normal.miles
            when {
                at.isEmpty() -> "\"${item.name}\" isn't a job in the table"
                // A job the guide gives by time alone ("every 2 years regardless of mileage") is in the table
                // wherever the maker chose to draw it; there is no mileage to hold it to.
                every == null -> if (item.rule == DueRule.TIME_ONLY) null else "\"${item.name}\" has no distance and isn't marked time-only"
                at.map { it.atMiles } != ((item.firstAt ?: every)..end step every).toList() ->
                    "\"${item.name}\" is in the table at ${at.joinToString { it.atMiles.toString() }}, which isn't " +
                        (item.firstAt?.let { "$it and then every $every" } ?: "every $every")
                item.firstAt == null && at.any { m -> m.orMonths != null && item.normal.months != null && m.orMonths != item.normal.months * (m.atMiles / every) } ->
                    "\"${item.name}\" has months that aren't the table's"
                else -> null
            }
        }
    }

    private fun forModel(lists: List<KnownScheduleList>, year: Int, make: String, model: String) =
        lists.filter { year in it.years && KnownLists.same(it.make, make) && KnownLists.same(it.model, model) }

    private fun sameSize(list: KnownEngine, litres: Double) = kotlin.math.abs(list.displacementL - litres) < 0.05

    private fun fuelFits(list: KnownEngine, fuel: String?): Boolean? =
        if (list.fuel == null) true else fuel?.takeIf { it.isNotBlank() }?.contains(list.fuel, ignoreCase = true)

    /**
     * The list for a vehicle, or null. As for torque: the make, model and a year whose guide was read;
     * no near misses. Where a model has a list per engine, [engine] (the vehicle's engine text) has to
     * give that engine's size, and [fuel] its fuel where the list says one: **an engine that isn't
     * known matches no engine's list**, and the owner is asked instead ([engineChoices]).
     */
    fun match(lists: List<KnownScheduleList>, year: Int, make: String, model: String, engine: String? = null, fuel: String? = null): KnownScheduleList? {
        val candidates = forModel(lists, year, make, model)
        val litres = Engines.displacementL(engine)
        return candidates.firstOrNull { c -> c.engine != null && litres != null && sameSize(c.engine, litres) && fuelFits(c.engine, fuel) == true }
            ?: candidates.firstOrNull { it.engine == null }
    }

    /**
     * The lists the owner could pick between when [match] finds none because the vehicle's engine, or
     * its fuel, isn't on record: every engine's list for the model and year that what is on record
     * doesn't rule out. Empty when nothing is offered for the model, or the vehicle has an engine none
     * of them is for. The pick is the owner's; nothing here chooses for them.
     */
    fun engineChoices(lists: List<KnownScheduleList>, year: Int, make: String, model: String, engine: String? = null, fuel: String? = null): List<KnownScheduleList> {
        if (match(lists, year, make, model, engine, fuel) != null) return emptyList()
        val litres = Engines.displacementL(engine)
        return forModel(lists, year, make, model).filter { c ->
            c.engine != null && (litres == null || sameSize(c.engine, litres)) && fuelFits(c.engine, fuel) != false
        }
    }

    /**
     * Whether a job of [list] is for a vehicle with this [drivetrain] (vPIC's drive type, or the one
     * picked when the vehicle was added). Only a vehicle **known** to drive two wheels loses the
     * jobs the guide marks "(4WD)". Anything else keeps them, with "(4WD)" in their names: a
     * four-wheel-drive vehicle because they are its jobs, and one on record as all-wheel drive or as
     * nothing at all because the guide has no other line for it and leaving a job out is the worse
     * mistake. That is not a claim that all-wheel drive is four-wheel drive ([DriveKind]).
     */
    fun applies(list: KnownScheduleList, jobName: String, drivetrain: String?, transmission: String? = null): Boolean {
        val key = MilestoneImport.nameKey(jobName)
        val twoWheel = Drivetrains.kind(drivetrain) == DriveKind.TWO_WHEEL
        // The same rule for the gearbox: only a vehicle known to have a manual loses the jobs that aren't for one.
        return !(twoWheel && key in list.fourWheelDriveOnly) && !(Transmissions.isManual(transmission) && key in list.manualExcluded)
    }

    /**
     * [list]'s table and intervals for one vehicle: in [unit], which changes nothing when it is the
     * list's own, and without the jobs that aren't for its [drivetrain].
     */
    fun milestonesIn(list: KnownScheduleList, unit: DistanceUnit, drivetrain: String?, transmission: String? = null): List<ImportedMilestone> =
        FileDistances.milestones(list.milestones, null, list.unit, unit).first
            .map { m -> m.copy(work = m.work.filter { applies(list, it.name, drivetrain, transmission) }) }
            .filter { it.work.isNotEmpty() }

    fun intervalsIn(list: KnownScheduleList, unit: DistanceUnit, drivetrain: String?, transmission: String? = null): List<ImportedScheduleItem> =
        FileDistances.schedule(list.intervals, list.unit, unit).filter { applies(list, it.name, drivetrain, transmission) }

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
