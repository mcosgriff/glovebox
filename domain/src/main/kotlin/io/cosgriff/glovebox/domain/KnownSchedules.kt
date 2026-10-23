package io.cosgriff.glovebox.domain


/**
 * A manufacturer's maintenance schedule that ships with the app for one model: the table as the
 * maker's guide prints it, and the reminder intervals that follow from it. [id] is kept on what is
 * added from it (`vehicles.milestonesKnownList`, `schedule_items.knownList`), for good.
 */
/**
 * The engine a list is for, when a model's guide has a different schedule for each.
 * [label] is what the owner is shown when they have to say which is theirs ("2.7L 4-cylinder").
 * [fuel] is set only where two engines of one size differ by it ("Diesel"), as vPIC spells it.
 * [powertrain] is set only where a hybrid, plug-in or flex-fuel model and its petrol twin share a size.
 */
data class KnownEngine(val label: String, val displacementL: Double, val fuel: String? = null, val powertrain: Powertrain? = null)

data class KnownScheduleList(
    override val id: String,
    /** "2017–2024 4Runner" */
    override val title: String,
    override val make: String,
    override val model: String,
    override val years: IntRange,
    override val source: KnownSpecSource,
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
     * The names of the jobs the guide marks as for all-wheel drive only. Kept apart from
     * [fourWheelDriveOnly] because the two are different systems ([DriveKind]); the rule is the same.
     */
    val allWheelDriveOnly: Set<String> = emptySet(),
    /**
     * The names of the jobs that aren't for a manual gearbox (a dual-clutch or automatic
     * transmission's fluid change). A vehicle known to have a manual doesn't get them.
     */
    val manualExcluded: Set<String> = emptySet(),
    /**
     * The names of the jobs that are for a manual gearbox only (its oil change, the clutch). The other way
     * round from [manualExcluded]: a vehicle known to shift itself doesn't get them.
     */
    val manualOnly: Set<String> = emptySet(),
    /** Null for a list that is for every engine the model had in those years. */
    val engine: KnownEngine? = null,
    override val modelAliases: List<String> = emptyList(),
    /**
     * Jobs the guide gives only as an interval, apart from its table (Volkswagen's "Additional Maintenance
     * Items", a "Time-Dependent Maintenance Items" box): reminders with the guide's own figures, **never placed
     * on the table**, so nothing holds them to it. None of them is also a job of the table or of [intervals].
     */
    val additional: List<ImportedScheduleItem> = emptyList(),
) : KnownList

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
    private val keys = required + setOf("fourWheelDriveOnly", "allWheelDriveOnly", "manualExcluded", "manualOnly", "engine", "modelAliases", "additional")

    fun parse(text: String): KnownScheduleFile {
        val (o, check) = KnownLists.root(text, keys, required = required)
        if (o == null) return KnownScheduleFile.Invalid(check.invalid().errors)
        val header = KnownLists.header(o, check)
        // Both readers report under "items", as they do for an import file; the two errors lists are kept apart.
        val milestones = o["milestones"]?.let { MilestoneImport.read(it, check) }.orEmpty()
        val tableFailed = check.failed
        val intervals = o["intervals"]?.let { ScheduleImport.read(it, check) }.orEmpty()
        if (!tableFailed && !check.failed) contradictions(milestones, intervals).forEach { check.fail("intervals", it) }
        val additional = o["additional"]?.let { ScheduleImport.read(it, check) }.orEmpty()
        val tableJobs = milestones.flatMap { m -> m.work.map { MilestoneImport.jobKey(it) } }.toSet() +
            intervals.map { MilestoneImport.jobKey(TableJob(it.name, it.service)) }
        additional.filter { MilestoneImport.jobKey(TableJob(it.name, it.service)) in tableJobs }
            .forEach { check.fail("additional", "\"${it.name}\" is a job of the table; it can't also be apart from it") }
        val names = (milestones.flatMap { m -> m.work.map { it.name } } + (intervals + additional).map { it.name }).map(MilestoneImport::nameKey).toSet()
        // A field that names some of the list's jobs: every name has to be one.
        fun jobs(field: String): Set<String> = o[field]?.let { e ->
            check.array(e, field, 1, 200)?.mapIndexedNotNull { i, n ->
                val path = ImportCheck.at(field, i)
                check.string(n, path, MilestoneImport.MAX_NAME)?.also { if (MilestoneImport.nameKey(it) !in names) check.fail(path, "\"$it\" isn't a job in this list") }
            }
        }.orEmpty().map(MilestoneImport::nameKey).toSet()
        val fourWheel = jobs("fourWheelDriveOnly")
        val allWheel = jobs("allWheelDriveOnly")
        val notManual = jobs("manualExcluded")
        val manual = jobs("manualOnly")
        (notManual intersect manual).forEach { check.fail("manualOnly", "\"$it\" is also in manualExcluded") }
        val engine = o["engine"]?.let { KnownLists.engine(it, check) }
        val aliases = o["modelAliases"]?.let { e ->
            check.array(e, "modelAliases", 1, 5)?.mapIndexedNotNull { i, n -> check.string(n, ImportCheck.at("modelAliases", i), 40) }
        }.orEmpty()
        if (header != null && aliases.any { KnownLists.same(it, header.model) }) check.fail("modelAliases", "repeats the model")
        if (check.failed || header == null) return KnownScheduleFile.Invalid(check.invalid().errors)
        return KnownScheduleFile.Ok(
            KnownScheduleList(
                header.id, header.title, header.make, header.model, header.years, header.source, check.unit, milestones, intervals,
                fourWheelDriveOnly = fourWheel,
                allWheelDriveOnly = allWheel,
                manualExcluded = notManual,
                manualOnly = manual,
                engine = engine,
                modelAliases = aliases,
                additional = additional,
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

    /**
     * The list for a vehicle, or null. As for torque: the make, model and a year whose guide was read;
     * no near misses. Where a model has a list per engine, [engine] (the vehicle's engine text) has to
     * give that engine's size, [fuel] its fuel where the list says one, and [powertrain] its powertrain
     * where the list says one: **an engine or powertrain that isn't known matches no list that needs it**,
     * and the owner is asked instead ([engineChoices]).
     */
    fun match(
        lists: List<KnownScheduleList>, year: Int, make: String, model: String,
        engine: String? = null, fuel: String? = null, powertrain: Powertrain? = null,
    ): KnownScheduleList? = KnownLists.forEngine(lists.forModel(year, make, model), KnownScheduleList::engine, engine, fuel, powertrain)

    /**
     * The lists the owner could pick between when [match] finds none because the vehicle's engine, or
     * its fuel, isn't on record: every engine's list for the model and year that what is on record
     * doesn't rule out. Empty when nothing is offered for the model, or the vehicle has an engine none
     * of them is for. The pick is the owner's; nothing here chooses for them.
     */
    fun engineChoices(
        lists: List<KnownScheduleList>, year: Int, make: String, model: String,
        engine: String? = null, fuel: String? = null, powertrain: Powertrain? = null,
    ): List<KnownScheduleList> = KnownLists.engineChoices(lists.forModel(year, make, model), KnownScheduleList::engine, engine, fuel, powertrain)

    /**
     * Whether a job of [list] is for a vehicle with this [drivetrain] (vPIC's drive type, or the one
     * picked when the vehicle was added). Only a vehicle **known** to drive two wheels loses the
     * jobs the guide marks "(4WD)" or "(AWD)". Anything else keeps them, with the mark in their
     * names: a vehicle with that drive because they are its jobs, and one on record as the other
     * kind or as nothing at all because the guide has no other line for it and leaving a job out is
     * the worse mistake. That is not a claim that all-wheel drive is four-wheel drive ([DriveKind]).
     */
    fun applies(list: KnownScheduleList, jobName: String, drivetrain: String?, transmission: String? = null): Boolean {
        val key = MilestoneImport.nameKey(jobName)
        val twoWheel = Drivetrains.kind(drivetrain) == DriveKind.TWO_WHEEL
        // The same rule for the gearbox: only a vehicle known to have a manual loses the jobs that aren't for one,
        // and only one known to shift itself loses the manual's own.
        return !(twoWheel && (key in list.fourWheelDriveOnly || key in list.allWheelDriveOnly)) &&
            !(Transmissions.isManual(transmission) && key in list.manualExcluded) &&
            !(Transmissions.isAutomatic(transmission) && key in list.manualOnly)
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
        FileDistances.schedule(list.intervals + list.additional, list.unit, unit).filter { applies(list, it.name, drivetrain, transmission) }

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
}
