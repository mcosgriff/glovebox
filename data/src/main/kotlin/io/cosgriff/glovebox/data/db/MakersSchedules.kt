package io.cosgriff.glovebox.data.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.BodyStyles
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.KnownEngine
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import io.cosgriff.glovebox.domain.Powertrains
import javax.inject.Inject
import javax.inject.Singleton

/** The makers' schedules that ship with the app; the app reads them from its assets. */
fun interface KnownScheduleLists {
    suspend fun lists(): List<KnownScheduleList>
}

/**
 * The one place that knows which maker's schedule a vehicle is on, and takes one up (#576). The screens ask here
 * instead of matching for themselves, so every one of them gives the same answer.
 *
 * Writes that take more than one step are each one transaction (#429), so a process that dies part-way never
 * leaves a maker's schedule without its table. The owner's picks are the caller's: this only writes rows.
 */
@Singleton
class MakersSchedules @Inject constructor(private val known: KnownScheduleLists, private val db: GloveboxDatabase) {
    private val schedule = db.schedule()
    private val milestones = db.milestones()
    private val vehicles = db.vehicles()

    /** Every bundled list. */
    suspend fun lists(): List<KnownScheduleList> = known.lists()

    /**
     * The maker's schedule [vehicle] is on: the list its items were taken up from (`knownList`) while that list
     * still ships, else the one that covers it as it is on record. Null when neither. The stored list comes first
     * so that a vehicle whose engine text never matched, or no longer does, keeps the list its owner took up.
     */
    suspend fun forVehicle(vehicle: VehicleEntity): KnownScheduleList? {
        val lists = lists()
        val stored = schedule.allFor(vehicle.id).firstNotNullOfOrNull { it.knownList }
        return stored?.let { id -> lists.firstOrNull { it.id == id } } ?: match(lists, vehicle)
    }

    /**
     * The lists the owner could pick between, one per engine (or body), when no list matches because the vehicle's
     * engine, fuel, body or trim isn't on record ([KnownSchedules.engineChoices]).
     */
    suspend fun choices(vehicle: VehicleEntity): List<KnownScheduleList> =
        KnownSchedules.engineChoices(lists(), vehicle.year, vehicle.make, vehicle.model, vehicle.engine, vehicle.fuel, Powertrains.of(vehicle.powertrain), BodyStyles.of(vehicle.bodyStyle), vehicle.trim)

    /** What is on record about [vehicle] that a list is matched by: the only place that is said (#569 added the trim). */
    private fun match(lists: List<KnownScheduleList>, vehicle: VehicleEntity): KnownScheduleList? =
        KnownSchedules.match(lists, vehicle.year, vehicle.make, vehicle.model, vehicle.engine, vehicle.fuel, Powertrains.of(vehicle.powertrain), BodyStyles.of(vehicle.bodyStyle), vehicle.trim)

    /** The DAO calls in [block] are kept together or not at all. */
    internal suspend fun <T> transaction(block: suspend () -> T): T =
        db.useWriterConnection { it.immediateTransaction { block() } }

    /**
     * Takes up a schedule: [items] as the caller built them, and, for a maker's schedule ([makers]), its table as
     * the vehicle's milestones. Null [makers] is the typical list, which has no table. [replaced] are typical
     * items that are the same jobs as [items] ([ScheduleDao.applyKnown]). [engine] is the one the owner chose
     * among the maker's lists, written with its powertrain only where the vehicle has none on record; [body] is the
     * body of the list they chose, written the same way.
     */
    suspend fun adopt(
        vehicleId: Long,
        items: List<ScheduleItemEntity>,
        makers: KnownScheduleList? = null,
        replaced: List<Long> = emptyList(),
        engine: KnownEngine? = null,
        body: BodyStyle? = null,
    ) = transaction {
        schedule.applyKnown(items, replaced)
        engine?.let {
            vehicles.setEngineIfBlank(vehicleId, it.label)
            it.powertrain?.let { p -> vehicles.setPowertrainIfBlank(vehicleId, p.name) }
        }
        body?.let { vehicles.setBodyStyleIfBlank(vehicleId, it.name) }
        makers?.let { writeTable(vehicleId, it) }
    }

    /**
     * Adds [list]'s table as the vehicle's milestones, unless it has a table already: "Use the maker's table" on
     * Milestones. It is then an ordinary table, edited, saved as a file or removed like one the user typed.
     */
    suspend fun addTable(vehicleId: Long, list: KnownScheduleList) = transaction { writeTable(vehicleId, list) }

    /** The table as the vehicle is on record now: its unit, and the jobs for its drive and gearbox. */
    private suspend fun writeTable(vehicleId: Long, list: KnownScheduleList) {
        val vehicle = vehicles.byId(vehicleId) ?: return
        val table = KnownSchedules.milestonesIn(list, DistanceUnit.of(vehicle.distanceUnit), vehicle.drivetrain, vehicle.transmission)
        milestones.addKnown(vehicleId, list.id, table.map { m -> m.toRows(vehicleId) })
    }
}
