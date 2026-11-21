package io.cosgriff.glovebox.data.db

import io.cosgriff.glovebox.data.log.gearboxOnRecord
import io.cosgriff.glovebox.data.milestones.StartedFrom
import io.cosgriff.glovebox.domain.ActiveInterval
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DrivingUse
import io.cosgriff.glovebox.domain.DueRule
import io.cosgriff.glovebox.domain.Gearbox
import io.cosgriff.glovebox.domain.Gearboxes
import io.cosgriff.glovebox.domain.HistoryImport
import io.cosgriff.glovebox.domain.ImportMode
import io.cosgriff.glovebox.domain.ImportPlan
import io.cosgriff.glovebox.domain.ImportPlans
import io.cosgriff.glovebox.domain.ImportedScheduleItem
import io.cosgriff.glovebox.domain.Interval
import io.cosgriff.glovebox.domain.KnownEngine
import io.cosgriff.glovebox.domain.KnownIntervalPlan
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import io.cosgriff.glovebox.domain.SavedScheduleItem
import io.cosgriff.glovebox.domain.ScheduleImport
import io.cosgriff.glovebox.domain.ScheduleSummaries
import io.cosgriff.glovebox.domain.ScheduleTemplateItem
import io.cosgriff.glovebox.domain.ScheduleTemplates
import javax.inject.Inject
import javax.inject.Singleton

/** A schedule item being set up or edited; nothing is written to the database until the owner confirms. */
data class DraftItem(
    val key: String,
    val name: String,
    val normal: Interval,
    val severe: Interval?,
    val useSevere: Boolean = false,
    /** Null = use the app-wide default rule. */
    val rule: DueRule? = null,
    val tracked: Boolean = true,
    /** The suggestion this came from; null for items the user added. */
    val template: ScheduleTemplateItem? = null,
    /** One the user added, not one of the typical items Glovebox suggested. Only these can be pointed at a service line. */
    val own: Boolean = template == null,
    /** For the user's own: the CatalogItem id of the built-in service line that resets it; null = logged under "Your items". */
    val service: String? = null,
    /** For an item from a maker's schedule that ships with the app: that list's id, kept on the row for good. */
    val knownList: String? = null,
    /** The distance covered at which the job is first due, when that isn't one interval in; null for most. */
    val firstAt: Int? = null,
    /** A job that exists only under severe use; [normal] is its interval then. On normal use it is listed, switched off. */
    val severeOnly: Boolean = false,
    /** A flex-fuel maker's interval for a vehicle run mostly on E85; null for every other item. */
    val e85: Interval? = null,
    /** The vehicle runs mostly on E85. Changes nothing on an item without an [e85] interval. */
    val useE85: Boolean = false,
    /** The one gearbox a maker's job is for (#549, `dsgOnly` / `automaticOnly`); null for any. */
    val gearboxOnly: Gearbox? = null,
) {
    /** On the list but not reminding: a job only for severe use, while the schedule is on normal use. */
    val dormant: Boolean get() = severeOnly && !useSevere
    /** The interval its reminder counts by, as [ActiveInterval.of] works it out: on severe use, figure by figure (#616). */
    val active: Interval get() = ActiveInterval.of(normal, severe, useSevere, e85, useE85) ?: normal
    /** On severe use, with a severe interval that changes a figure in force. */
    val showsSevere: Boolean get() = !showsE85 && useSevere && active != normal
    /** On its E85 interval. */
    val showsE85: Boolean get() = useE85 && e85 != null

    companion object {
        /** A new item of the user's own, starting at 5,000 mi or 6 months: Glovebox's figure, so a schedule figure in [unit]. */
        fun blank(key: String, unit: DistanceUnit) = DraftItem(key = key, name = "", normal = Interval(unit.scheduled(5_000), 6), severe = null)
    }
}

/** A saved row as the editor's draft, keyed by its id; null for one with no interval. [template] is the typical item it is, if any. */
fun ScheduleItemEntity.toDraft(template: ScheduleTemplateItem?): DraftItem? {
    val normal = interval(intervalMiles, intervalMonths) ?: return null
    return DraftItem(
        key = id.toString(),
        name = name,
        normal = normal,
        severe = interval(severeMiles, severeMonths),
        useSevere = useSevere,
        rule = rule,
        tracked = tracked,
        template = template,
        own = templateKey == null,
        service = service,
        knownList = knownList,
        firstAt = firstMiles,
        severeOnly = severeOnly,
        e85 = interval(e85Miles, e85Months),
        useE85 = useE85,
        gearboxOnly = Gearboxes.of(gearboxOnly),
    )
}

private fun interval(miles: Int?, months: Int?): Interval? = if (miles == null && months == null) null else Interval(miles, months)

/**
 * An interval from a maker's schedule or an import file as a draft of an item of the user's own, tracked. [key]
 * tells it apart on setup's list; [knownList] is the bundled list it came from, kept on the row for good.
 */
fun ImportedScheduleItem.toDraft(key: String, knownList: String? = null): DraftItem = DraftItem(
    key = key, name = name, normal = normal, severe = severe, rule = rule, own = true, service = service,
    knownList = knownList, firstAt = firstAt, severeOnly = severeOnly, e85 = e85, gearboxOnly = gearbox,
)

/** A typical item as a draft, keyed by its template key. [name] is the app's wording of it. */
fun ScheduleTemplateItem.toDraft(name: String): DraftItem = DraftItem(key = key, name = name, normal = normal, severe = severe, template = this)

/**
 * The row a draft is saved as: the one place a schedule item's row is built (#620). A service is kept only on an
 * item of the user's own, and E85 use only on an item with an E85 interval. Only [ScheduleItems] writes these.
 */
internal fun DraftItem.toEntity(vehicleId: Long): ScheduleItemEntity = ScheduleItemEntity(
    vehicleId = vehicleId,
    name = name,
    templateKey = template?.key,
    intervalMiles = normal.miles,
    intervalMonths = normal.months,
    severeMiles = severe?.miles,
    severeMonths = severe?.months,
    useSevere = useSevere,
    ruleOverride = rule?.name,
    tracked = tracked,
    service = service.takeIf { own },
    knownList = knownList,
    firstMiles = firstAt,
    severeOnly = severeOnly,
    e85Miles = e85?.miles,
    e85Months = e85?.months,
    useE85 = useE85 && e85 != null,
    gearboxOnly = gearboxOnly?.name,
)

/** An interval from an import file as an item of the user's own, tracked, built as every other row is ([toEntity]). */
fun ImportedScheduleItem.toEntity(vehicleId: Long, knownList: String? = null): ScheduleItemEntity = toDraft(key = "", knownList).toEntity(vehicleId)

/**
 * The one place a vehicle's schedule items are built and written (#620), beside [MakersSchedules], which knows
 * which maker's schedule a vehicle is on. The screens hand it the owner's intents; every row goes through
 * [toEntity], and every later change writes only the columns it changes, never a whole row from a copy read
 * earlier (the rule vehicles.md gives `vehicles`).
 *
 * Setup is refused for a vehicle that already has items, checked inside the write, so a second Save (a
 * double tap) never inserts the schedule twice. Adding the maker's items or the missing typical ones works
 * out what to add inside its write too, so doing it twice adds nothing the second time.
 */
@Singleton
class ScheduleItems @Inject constructor(private val makers: MakersSchedules, db: GloveboxDatabase) {
    private val schedule = db.schedule()
    private val vehicles = db.vehicles()

    /**
     * What adding the maker's schedule [vehicle] is on would do, given its [items] (tracked or not), while it
     * would add something. It is offered, never applied.
     */
    suspend fun offer(vehicle: VehicleEntity, items: List<ScheduleItemEntity>): Pair<KnownScheduleList, KnownIntervalPlan>? =
        MakersSchedules.forVehicle(makers.lists(), vehicle, items)?.let { list -> list to plan(list, vehicle, items) }?.takeIf { (_, plan) -> plan.new.isNotEmpty() }

    /** The guide [items] were taken up from, when any was; said once above the schedule. */
    suspend fun startedFrom(items: List<ScheduleItemEntity>): StartedFrom? {
        val id = items.firstNotNullOfOrNull { it.knownList } ?: return null
        return StartedFrom(makers.lists().firstOrNull { it.id == id }?.let(KnownSchedules::credit))
    }

    /**
     * Saves setup's [drafts] as the vehicle's schedule, in one write with the maker's table ([list], null for the
     * typical list) and the engine and body the owner chose ([MakersSchedules.adopt]). False, with nothing
     * written, when the vehicle already has items: setup would insert every one of them again (schedule.md).
     */
    suspend fun setUp(vehicleId: Long, drafts: List<DraftItem>, list: KnownScheduleList?, engine: KnownEngine? = null, body: BodyStyle? = null): Boolean =
        makers.transaction {
            if (schedule.allFor(vehicleId).isNotEmpty()) return@transaction false
            makers.adopt(vehicleId, drafts.map { it.toEntity(vehicleId) }, list, engine = engine, body = body)
            true
        }

    /** A new item of the owner's own, from the editor. */
    suspend fun add(vehicleId: Long, draft: DraftItem) = schedule.insertAll(listOf(draft.toEntity(vehicleId)))

    /**
     * Saves the editor's [draft] over item [id]: only what the editor shows, so a snooze, the list it came from
     * and its template key stay as they are, whatever was read before.
     */
    suspend fun edit(id: Long, draft: DraftItem) {
        val row = draft.toEntity(vehicleId = 0)
        schedule.edit(
            id, row.name, row.intervalMiles, row.intervalMonths, row.severeMiles, row.severeMonths, row.useSevere,
            row.e85Miles, row.e85Months, row.useE85, row.ruleOverride, row.tracked, row.firstMiles, row.service,
        )
    }

    suspend fun setTracked(id: Long, tracked: Boolean) = schedule.setTracked(id, tracked)

    /** Ticks or unticks every item of the vehicle at once; nothing else on them changes. */
    suspend fun setAllTracked(vehicleId: Long, tracked: Boolean) = schedule.setTrackedFor(vehicleId, tracked)

    /** The one Normal / Severe switch, on every item of the vehicle. */
    suspend fun setSevereUse(vehicleId: Long, on: Boolean) = schedule.setSevereUseFor(vehicleId, on)

    /** "Runs mostly on E85", on every item of the vehicle that has an E85 interval. */
    suspend fun setE85Use(vehicleId: Long, on: Boolean) = schedule.setE85UseFor(vehicleId, on)

    /**
     * Adds the typical items the schedule lacks ([missing]), unticked: the owner picks which to track, so a dozen
     * "not logged yet" reminders don't appear at once. On severe use when the schedule is. [name] words each.
     */
    suspend fun addMissing(vehicleId: Long, name: (ScheduleTemplateItem) -> String) = makers.transaction {
        val vehicle = vehicles.byId(vehicleId) ?: return@transaction
        val items = schedule.allFor(vehicleId)
        val severe = use(items.drafts()) == DrivingUse.SEVERE
        // Every item, as setup's switch does (an open question on #620; addKnown sets only those it changes).
        schedule.insertAll(missing(vehicle, items).map { it.toDraft(name(it)).copy(useSevere = severe, tracked = false).toEntity(vehicleId) })
    }

    /**
     * Adds the reminders of the maker's schedule ([offer]) as items of the owner's own, tracked, each remembering
     * the list it started from, with the maker's table unless the vehicle has one. [stop] are the typical items the
     * owner agreed to have replaced because one of these is the same job: each is removed, or kept untracked if a
     * service was logged against it. On severe use, and on E85, only where the schedule already is.
     */
    suspend fun takeUpMakers(vehicleId: Long, stop: Set<Long>) = makers.transaction {
        val vehicle = vehicles.byId(vehicleId) ?: return@transaction
        val items = schedule.allFor(vehicleId)
        val (list, plan) = offer(vehicle, items) ?: return@transaction
        val drafts = items.drafts()
        val severe = use(drafts) == DrivingUse.SEVERE
        // On E85 only where the owner has already said so on this schedule; nothing is assumed.
        val e85 = e85(drafts) == true
        makers.adopt(
            vehicleId,
            plan.new.map {
                // Only where severe use changes something (an open question on #620; setup and addMissing set every item).
                it.toDraft(key = "", list.id).copy(useSevere = severe && (it.severe != null || it.severeOnly), useE85 = e85).toEntity(vehicleId)
            },
            list,
            replaced = plan.doubles.map { it.id }.filter { it in stop },
        )
    }

    /**
     * Writes an `interval-schedule` file (#645) as its [preview] planned it, in one transaction: the plan is checked
     * again against the items as they stand inside it ([ImportPlans.recheck]), so nothing is added twice and no item
     * changed since is written over unless the preview listed it. New items are the owner's own, tracked, built as
     * every row is ([toEntity]); a replaced one gets only what the file says of it ([ScheduleDao.setFromFile]), so a
     * snooze, whether it is tracked and which interval is in use stay as they are, whatever was read before. [stop]
     * are the typical twins the owner chose to stop tracking ([ScheduleImport.doubles]): only `tracked` changes.
     */
    suspend fun import(vehicleId: Long, preview: ImportPlan<ImportedScheduleItem, SavedScheduleItem>, mode: ImportMode, stop: List<Long>) = makers.transaction {
        val now = ScheduleImport.plan(preview.items, schedule.allFor(vehicleId).map { it.toSaved() })
        val write = ImportPlans.recheck(preview, now, mode) { it.id }
        schedule.insertAll(write.added.map { it.toEntity(vehicleId) })
        write.replaced.forEach { (item, mine) ->
            schedule.setFromFile(
                mine.id, item.normal.miles, item.normal.months, item.severe?.miles, item.severe?.months, item.rule?.name, item.service,
                item.firstAt, item.severeOnly,
            )
        }
        schedule.stopTracking(stop)
    }

    companion object {
        /** Setup's list for a maker's schedule: [list]'s reminders for [vehicle] as it is on record (its unit, drive and gearbox). */
        fun drafts(list: KnownScheduleList, vehicle: VehicleEntity): List<DraftItem> =
            KnownSchedules.intervalsIn(list, vehicle.unit(), vehicle.drivetrain, vehicle.gearboxOnRecord()).mapIndexed { i, it -> it.toDraft("known-$i", list.id) }

        /** Setup's list where no maker's schedule covers the vehicle: the typical items, each worded by [name]. */
        fun typicalDrafts(vehicle: VehicleEntity?, name: (ScheduleTemplateItem) -> String): List<DraftItem> =
            ScheduleTemplates.generic(vehicle?.drivetrain, vehicle?.fuel, vehicle?.unit() ?: DistanceUnit.MILES).map { it.toDraft(name(it)) }

        /**
         * Typical items [items] don't have, e.g. after a skipped setup. Not one whose job one of the owner's own
         * already is: adding it back would be a second reminder.
         */
        fun missing(vehicle: VehicleEntity, items: List<ScheduleItemEntity>): List<ScheduleTemplateItem> {
            val covered = items.filter { it.templateKey == null }.mapNotNull { HistoryImport.services[it.service]?.resets }.toSet()
            return ScheduleTemplates.missing(vehicle.drivetrain, vehicle.fuel, vehicle.unit(), items.map { it.templateKey }).filter { it.key !in covered }
        }

        /** Normal, severe or mixed, over the tracked items severe use changes something on. */
        fun use(items: List<DraftItem>): DrivingUse =
            ScheduleSummaries.of(items.filter { it.severe != null || it.severeOnly }.map { it.tracked to it.useSevere }).use

        /** Whether the tracked items with an E85 interval are on it; null when some are and some aren't. */
        fun e85(items: List<DraftItem>): Boolean? =
            items.filter { it.e85 != null && it.tracked }.map { it.useE85 }.distinct().let { if (it.size > 1) null else it.singleOrNull() ?: false }

        private fun plan(list: KnownScheduleList, vehicle: VehicleEntity, items: List<ScheduleItemEntity>): KnownIntervalPlan =
            KnownSchedules.plan(KnownSchedules.intervalsIn(list, vehicle.unit(), vehicle.drivetrain, vehicle.gearboxOnRecord()), items.map { it.toSaved() })

        private fun List<ScheduleItemEntity>.drafts() = mapNotNull { it.toDraft(null) }

        private fun VehicleEntity.unit() = DistanceUnit.of(distanceUnit)
    }
}
