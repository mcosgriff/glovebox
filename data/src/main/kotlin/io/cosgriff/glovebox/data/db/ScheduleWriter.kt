package io.cosgriff.glovebox.data.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.KnownEngine
import io.cosgriff.glovebox.domain.KnownScheduleList
import io.cosgriff.glovebox.domain.KnownSchedules
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes to a vehicle's schedule that take more than one step, each in one transaction (#429), so a process
 * that dies part-way never leaves a maker's schedule without its table. The owner's picks are the caller's:
 * this only writes rows.
 */
@Singleton
class ScheduleWriter @Inject constructor(private val db: GloveboxDatabase) {
    private val schedule = db.schedule()
    private val milestones = db.milestones()
    private val vehicles = db.vehicles()

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
        val vehicle = vehicles.byId(vehicleId) ?: return@transaction
        makers?.let {
            val table = KnownSchedules.milestonesIn(it, DistanceUnit.of(vehicle.distanceUnit), vehicle.drivetrain, vehicle.transmission)
            milestones.addKnown(vehicleId, it.id, table.map { m -> m.toRows(vehicleId) })
        }
    }
}
