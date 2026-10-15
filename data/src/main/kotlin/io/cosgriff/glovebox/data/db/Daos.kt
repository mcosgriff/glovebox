package io.cosgriff.glovebox.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import io.cosgriff.glovebox.domain.DistanceConversion
import io.cosgriff.glovebox.domain.DistanceKind
import io.cosgriff.glovebox.domain.DistanceUnit
import java.time.LocalDate

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE archivedOn IS NULL ORDER BY createdOn")
    fun observeActive(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE archivedOn IS NOT NULL ORDER BY archivedOn DESC")
    fun observeArchived(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observe(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun byId(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE archivedOn IS NULL")
    suspend fun active(): List<VehicleEntity>

    /** Garage first in the order it shows them, then archived vehicles, most recently sold first. */
    @Query("SELECT * FROM vehicles ORDER BY archivedOn IS NOT NULL, archivedOn DESC, createdOn")
    suspend fun all(): List<VehicleEntity>

    @Query("UPDATE vehicles SET recallsCheckedOn = :date WHERE id = :id")
    suspend fun setRecallsChecked(id: Long, date: LocalDate)

    /** [date] is the day it was sold; null moves it back to the garage. */
    @Query("UPDATE vehicles SET archivedOn = :date WHERE id = :id")
    suspend fun setArchived(id: Long, date: LocalDate?)

    /** Every vehicle's own photo file, archived vehicles included. */
    @Query("SELECT photoFile FROM vehicles WHERE photoFile IS NOT NULL")
    suspend fun allPhotoFiles(): List<String>

    /** For a vehicle added without one. */
    @Query("UPDATE vehicles SET vin = :vin WHERE id = :id")
    suspend fun setVin(id: Long, vin: String)

    // Single-column writes, so an edit never puts back a stale copy of the rest of the row.
    @Query("UPDATE vehicles SET nickname = :nickname WHERE id = :id")
    suspend fun setNickname(id: Long, nickname: String?)

    @Query("UPDATE vehicles SET plate = :plate, plateRegion = :region WHERE id = :id")
    suspend fun setPlate(id: Long, plate: String?, region: String?)

    /** Fills in the engine for a vehicle that has none on record; one that has is left exactly as it is. */
    @Query("UPDATE vehicles SET engine = :engine WHERE id = :id AND (engine IS NULL OR TRIM(engine) = '')")
    suspend fun setEngineIfBlank(id: Long, engine: String)

    @Query(
        "UPDATE vehicles SET oilSpec = :grade, oilApproval = :approval, oilCapacity = :capacity, oilCapacityUnit = :unit, " +
            "oilFilter = :filter WHERE id = :id",
    )
    suspend fun setOil(id: Long, grade: String?, approval: String?, capacity: Double?, unit: String?, filter: String?)

    @Query(
        "UPDATE vehicles SET tireSize = :front, tireSizeRear = :rear, tirePressureFront = :pFront, tirePressureRear = :pRear, " +
            "tirePressureSpare = :pSpare, tirePressureUnit = :pUnit WHERE id = :id",
    )
    suspend fun setTires(id: Long, front: String?, rear: String?, pFront: Int?, pRear: Int?, pSpare: Int?, pUnit: String?)

    @Query("UPDATE vehicles SET transmissionFluid = :value WHERE id = :id")
    suspend fun setTransmissionFluid(id: Long, value: String?)

    @Query("UPDATE vehicles SET differentialOil = :value WHERE id = :id")
    suspend fun setDifferentialOil(id: Long, value: String?)

    @Query("UPDATE vehicles SET transferCaseOil = :value WHERE id = :id")
    suspend fun setTransferCaseOil(id: Long, value: String?)

    @Query("UPDATE vehicles SET coolantType = :value WHERE id = :id")
    suspend fun setCoolant(id: Long, value: String?)

    @Query("UPDATE vehicles SET brakeFluid = :value WHERE id = :id")
    suspend fun setBrakeFluid(id: Long, value: String?)

    @Query("UPDATE vehicles SET engineAirFilter = :engine, cabinAirFilter = :cabin WHERE id = :id")
    suspend fun setAirFilters(id: Long, engine: String?, cabin: String?)

    @Query("UPDATE vehicles SET sparkPlug = :part, sparkPlugGap = :gap WHERE id = :id")
    suspend fun setSparkPlugs(id: Long, part: String?, gap: String?)

    @Query("UPDATE vehicles SET batteryGroup = :group, batteryCca = :cca WHERE id = :id")
    suspend fun setBattery(id: Long, group: String?, cca: Int?)

    @Query("UPDATE vehicles SET bulbLow = :low, bulbHigh = :high, bulbFog = :fog WHERE id = :id")
    suspend fun setBulbs(id: Long, low: String?, high: String?, fog: String?)

    @Query("UPDATE vehicles SET wiperDriverIn = :driver, wiperPassengerIn = :passenger, wiperRearIn = :rear, wiperBlades = :note WHERE id = :id")
    suspend fun setWipers(id: Long, driver: Int?, passenger: Int?, rear: Int?, note: String?)

    @Query("UPDATE vehicles SET photoFile = :fileName WHERE id = :id")
    suspend fun setPhotoFile(id: Long, fileName: String?)

    @Query("UPDATE vehicles SET odometerOffset = :offset, clusterSwappedOn = :on, clusterStartReading = :startReading WHERE id = :id")
    suspend fun writeClusterSwap(id: Long, offset: Int, on: LocalDate?, startReading: Int?)

    @Query("UPDATE schedule_items SET snoozedUntilOdometer = NULL, snoozedUntilDate = NULL WHERE vehicleId = :vehicleId AND snoozedUntilOdometer IS NOT NULL")
    suspend fun clearMileageSnoozes(vehicleId: Long)

    /**
     * Sets, corrects or (with [on] null) removes the cluster replacement. A snooze that ends at an
     * odometer reading was worked out on the old footing and would end at the wrong point, so those go.
     */
    @Transaction
    suspend fun setClusterSwap(id: Long, offset: Int, on: LocalDate?, startReading: Int?) {
        writeClusterSwap(id, offset, on, startReading)
        clearMileageSnoozes(id)
    }

    @Insert suspend fun insert(vehicle: VehicleEntity): Long
    @Update suspend fun update(vehicle: VehicleEntity)

    /** Cascades to readings, schedule, records, record items, photos rows, torque specs and recalls. */
    @Delete suspend fun delete(vehicle: VehicleEntity)

    @Query("SELECT p.fileName FROM photos p JOIN service_records r ON p.recordId = r.id WHERE r.vehicleId = :vehicleId")
    suspend fun photoFilesFor(vehicleId: Long): List<String>
}

@Dao
interface ServiceDao {
    /** How many records [clearRecords] with [all] would delete: every one, or only the imported. */
    @Query("SELECT COUNT(*) FROM service_records WHERE vehicleId = :vehicleId AND (imported = 1 OR :all)")
    suspend fun countRecords(vehicleId: Long, all: Boolean): Int

    @Query("SELECT p.fileName FROM photos p JOIN service_records r ON p.recordId = r.id WHERE r.vehicleId = :vehicleId AND (r.imported = 1 OR :all)")
    suspend fun photoFilesOf(vehicleId: Long, all: Boolean): List<String>

    /** Lines and photo rows cascade; a recall it was the work for keeps its done mark. */
    @Query("DELETE FROM service_records WHERE vehicleId = :vehicleId AND (imported = 1 OR :all)")
    suspend fun clearRecords(vehicleId: Long, all: Boolean): Int
    @Transaction
    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun observeRecords(vehicleId: Long): Flow<List<ServiceRecordWithItems>>

    /** Every vehicle's records, for screens that summarise the whole garage. */
    @Transaction
    @Query("SELECT * FROM service_records")
    fun observeAllRecords(): Flow<List<ServiceRecordWithItems>>

    @Transaction
    @Query("SELECT * FROM service_records WHERE id = :id")
    fun observeRecord(id: Long): Flow<ServiceRecordWithItems?>

    @Transaction
    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun record(id: Long): ServiceRecordWithItems?

    @Transaction
    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId")
    suspend fun recordsFor(vehicleId: Long): List<ServiceRecordWithItems>

    @Insert suspend fun insertRecord(record: ServiceRecordEntity): Long
    @Insert suspend fun insertItems(items: List<ServiceRecordItemEntity>)
    @Update suspend fun updateRecord(record: ServiceRecordEntity)
    @Query("DELETE FROM service_record_items WHERE recordId = :recordId")
    suspend fun clearItems(recordId: Long)

    @Transaction
    suspend fun insertWithItems(record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>): Long {
        val id = insertRecord(record)
        insertItems(items.map { it.copy(recordId = id) })
        return id
    }

    /** Edit = replace the record and its items atomically. Reminders recompute from the log. */
    @Transaction
    suspend fun replaceWithItems(record: ServiceRecordEntity, items: List<ServiceRecordItemEntity>) {
        updateRecord(record)
        clearItems(record.id)
        insertItems(items.map { it.copy(id = 0, recordId = record.id) })
    }

    @Delete suspend fun deleteRecord(record: ServiceRecordEntity)

    /**
     * One import, applied whole or not at all. [replaced] records keep their id, and so their photos
     * and any recall they were the work for; nothing is deleted.
     */
    @Transaction
    suspend fun applyImport(added: List<Pair<ServiceRecordEntity, List<ServiceRecordItemEntity>>>, replaced: List<Pair<ServiceRecordEntity, List<ServiceRecordItemEntity>>>) {
        added.forEach { (record, items) -> insertWithItems(record, items) }
        replaced.forEach { (record, items) -> replaceWithItems(record, items) }
    }

    @Insert suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotos(ids: List<Long>)

    @Query("DELETE FROM photos WHERE recordId = :recordId")
    suspend fun clearPhotos(recordId: Long)

    @Query("SELECT COUNT(*) FROM photos WHERE recordId = :recordId")
    suspend fun photoCount(recordId: Long): Int

    /** Every receipt and parts photo file any record points at. */
    @Query("SELECT fileName FROM photos")
    suspend fun allPhotoFiles(): List<String>
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items WHERE vehicleId = :vehicleId AND tracked = 1 ORDER BY name")
    fun observeTracked(vehicleId: Long): Flow<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items WHERE tracked = 1 ORDER BY name")
    fun observeAllTracked(): Flow<List<ScheduleItemEntity>>

    /** Tracked and untracked. */
    @Query("SELECT * FROM schedule_items WHERE vehicleId = :vehicleId ORDER BY name")
    fun observeAll(vehicleId: Long): Flow<List<ScheduleItemEntity>>

    /** Tracked and untracked. */
    @Query("SELECT * FROM schedule_items WHERE vehicleId = :vehicleId ORDER BY name")
    suspend fun allFor(vehicleId: Long): List<ScheduleItemEntity>

    /** [rule] is a DueRule name; null goes back to the default rule. */
    @Query("UPDATE schedule_items SET ruleOverride = :rule WHERE id = :id")
    suspend fun setRuleOverride(id: Long, rule: String?)

    /** Both null clears the snooze. */
    @Query("UPDATE schedule_items SET snoozedUntilOdometer = :odometer, snoozedUntilDate = :date WHERE id = :id")
    suspend fun setSnooze(id: Long, odometer: Int?, date: LocalDate?)

    @Insert suspend fun insertAll(items: List<ScheduleItemEntity>)
    @Update suspend fun update(item: ScheduleItemEntity)
    @Update suspend fun updateAll(items: List<ScheduleItemEntity>)

    @Query("UPDATE schedule_items SET tracked = 0 WHERE id IN (:ids)")
    suspend fun stopTracking(ids: List<Long>)

    /** Typical items only, and only those no logged line was ever saved against. */
    @Query(
        "DELETE FROM schedule_items WHERE id IN (:ids) AND templateKey IS NOT NULL " +
            "AND id NOT IN (SELECT scheduleItemId FROM service_record_items WHERE scheduleItemId IS NOT NULL)",
    )
    suspend fun removeUnlogged(ids: List<Long>)

    /**
     * A maker's schedule taking over from typical items, whole or not at all. [replaced] are the
     * typical items that are the same jobs as [added]: each is removed, unless a service was logged
     * against it, in which case it stays, untracked, so that record keeps its link.
     */
    @Transaction
    suspend fun applyKnown(added: List<ScheduleItemEntity>, replaced: List<Long>) {
        insertAll(added)
        removeUnlogged(replaced)
        stopTracking(replaced)
    }

    /**
     * One import, applied whole or not at all. Nothing is deleted by one; [stopped] are typical items
     * the user chose to stop tracking because the file has the same job.
     */
    @Transaction
    suspend fun applyImport(added: List<ScheduleItemEntity>, replaced: List<ScheduleItemEntity>, stopped: List<Long>) {
        insertAll(added)
        updateAll(replaced)
        stopTracking(stopped)
    }
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY date")
    fun observe(vehicleId: Long): Flow<List<OdometerReadingEntity>>

    @Query("SELECT * FROM odometer_readings ORDER BY date")
    fun observeAll(): Flow<List<OdometerReadingEntity>>

    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY date")
    suspend fun readingsFor(vehicleId: Long): List<OdometerReadingEntity>

    @Insert suspend fun insert(reading: OdometerReadingEntity): Long

    @Query("DELETE FROM odometer_readings WHERE id = :id")
    suspend fun delete(id: Long)

    /** Puts a reading back as it was: a deleted one returns under its own id, a corrected one gets its old date and value. */
    @Upsert suspend fun put(reading: OdometerReadingEntity)

    /** Corrects a reading that was typed wrong, or dated wrong. */
    @Query("UPDATE odometer_readings SET date = :date, odometer = :odometer WHERE id = :id")
    suspend fun correct(id: Long, date: LocalDate, odometer: Int)
}

@Dao
interface TorqueDao {
    @Query("SELECT * FROM torque_specs WHERE vehicleId = :vehicleId ORDER BY category, name")
    fun observe(vehicleId: Long): Flow<List<TorqueSpecEntity>>

    @Query("SELECT * FROM torque_specs WHERE vehicleId = :vehicleId")
    suspend fun specsFor(vehicleId: Long): List<TorqueSpecEntity>

    @Insert suspend fun insert(spec: TorqueSpecEntity): Long
    @Insert suspend fun insertAll(specs: List<TorqueSpecEntity>)
    @Update suspend fun update(spec: TorqueSpecEntity)
    @Update suspend fun updateAll(specs: List<TorqueSpecEntity>)

    /** One import, applied whole or not at all. Nothing is ever deleted by one. */
    @Transaction
    suspend fun applyImport(added: List<TorqueSpecEntity>, replaced: List<TorqueSpecEntity>) {
        insertAll(added)
        updateAll(replaced)
    }
    @Delete suspend fun delete(spec: TorqueSpecEntity)
}

@Dao
interface RecallDao {
    @Query("SELECT * FROM recalls WHERE vehicleId = :vehicleId")
    fun observe(vehicleId: Long): Flow<List<RecallEntity>>

    /** Every vehicle's recalls, for screens that summarise the whole garage. */
    @Query("SELECT * FROM recalls")
    fun observeAll(): Flow<List<RecallEntity>>

    @Query("SELECT * FROM recalls WHERE vehicleId = :vehicleId")
    suspend fun recallsFor(vehicleId: Long): List<RecallEntity>

    @Query("SELECT * FROM recalls WHERE id = :id")
    suspend fun byId(id: Long): RecallEntity?

    @Query("SELECT id FROM recalls WHERE serviceRecordId = :recordId")
    suspend fun idsLinkedTo(recordId: Long): List<Long>

    /** [completedOn] null marks it open again. */
    @Query("UPDATE recalls SET completedOn = :completedOn, serviceRecordId = :recordId WHERE id = :id")
    suspend fun setDone(id: Long, completedOn: LocalDate?, recordId: Long?)

    /** [archivedOn] null moves it back to the open list. */
    @Query("UPDATE recalls SET archivedOn = :archivedOn WHERE id = :id")
    suspend fun setArchived(id: Long, archivedOn: LocalDate?)

    @Query("UPDATE recalls SET serviceRecordId = :recordId WHERE id IN (:ids)")
    suspend fun linkRecord(ids: List<Long>, recordId: Long)

    @Insert suspend fun insertAll(recalls: List<RecallEntity>)
    @Update suspend fun updateAll(recalls: List<RecallEntity>)

    @Query("DELETE FROM recalls WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    /** One refresh, applied whole or not at all. */
    @Transaction
    suspend fun apply(added: List<RecallEntity>, updated: List<RecallEntity>, withdrawn: List<Long>) {
        insertAll(added)
        updateAll(updated)
        delete(withdrawn)
    }
}

@Dao
interface MilestoneDao {
    /** The user's own milestone table, by mileage; empty when they haven't imported one. */
    @Transaction
    @Query("SELECT * FROM milestones WHERE vehicleId = :vehicleId ORDER BY atMiles")
    fun observe(vehicleId: Long): Flow<List<MilestoneWithJobs>>

    @Transaction
    @Query("SELECT * FROM milestones WHERE vehicleId = :vehicleId ORDER BY atMiles")
    suspend fun tableFor(vehicleId: Long): List<MilestoneWithJobs>

    @Insert suspend fun insert(milestone: MilestoneEntity): Long
    @Insert suspend fun insertJobs(jobs: List<MilestoneJobEntity>)
    @Update suspend fun update(milestone: MilestoneEntity)

    @Query("DELETE FROM milestone_jobs WHERE milestoneId = :milestoneId")
    suspend fun clearJobs(milestoneId: Long)

    @Query("DELETE FROM milestones WHERE vehicleId = :vehicleId")
    suspend fun deleteMilestones(vehicleId: Long)

    /** Whether the table repeats is the table's, kept on its vehicle; one column, like every other write to `vehicles`. */
    @Query("UPDATE vehicles SET milestonesRepeatEveryMiles = :everyMiles WHERE id = :vehicleId")
    suspend fun setRepeat(vehicleId: Long, everyMiles: Int?)

    @Query("UPDATE vehicles SET milestonesKnownList = :list WHERE id = :vehicleId")
    suspend fun setKnownList(vehicleId: Long, list: String?)

    @Query("DELETE FROM milestones WHERE id = :milestoneId")
    suspend fun deleteById(milestoneId: Long)

    @Query("SELECT COUNT(*) FROM milestones WHERE vehicleId = :vehicleId")
    suspend fun count(vehicleId: Long): Int

    /**
     * A milestone typed in the app: a new one when [milestone] has no id, otherwise that row with its
     * mileage, months and jobs as they now stand. [jobs] are in the user's order.
     */
    @Transaction
    suspend fun save(milestone: MilestoneEntity, jobs: List<MilestoneJobEntity>) {
        val id = if (milestone.id == 0L) insert(milestone) else milestone.id.also { update(milestone) }
        clearJobs(id)
        insertJobs(jobs.mapIndexed { i, job -> job.copy(id = 0, milestoneId = id, position = i) })
    }

    /** Deletes one milestone. The last one takes the table with it, and so that it repeats. */
    @Transaction
    suspend fun deleteMilestone(vehicleId: Long, milestoneId: Long) {
        deleteById(milestoneId)
        if (count(vehicleId) == 0) {
            setRepeat(vehicleId, null)
            setKnownList(vehicleId, null)
        }
    }

    /** Removes the whole table, and that it repeats; the Milestones screen then goes back to the view built from the schedule. */
    @Transaction
    suspend fun deleteTable(vehicleId: Long) {
        deleteMilestones(vehicleId)
        setRepeat(vehicleId, null)
        setKnownList(vehicleId, null)
    }

    /**
     * A bundled schedule's table, for a vehicle that has none: the rows, and which list they came from.
     * Does nothing if a table has appeared meanwhile, since a table is never written over by a list.
     */
    @Transaction
    suspend fun addKnown(vehicleId: Long, list: String, table: List<Pair<MilestoneEntity, List<MilestoneJobEntity>>>) {
        if (count(vehicleId) > 0) return
        applyImport(vehicleId, null, table, emptyList())
        setKnownList(vehicleId, list)
    }

    /**
     * One import, applied whole or not at all. Nothing is deleted by one; a replaced milestone keeps its row.
     * [repeatsEveryMiles] is the file's, which is the saved table's too unless this is the vehicle's first.
     */
    @Transaction
    suspend fun applyImport(
        vehicleId: Long,
        repeatsEveryMiles: Int?,
        added: List<Pair<MilestoneEntity, List<MilestoneJobEntity>>>,
        replaced: List<Pair<MilestoneEntity, List<MilestoneJobEntity>>>,
    ) {
        setRepeat(vehicleId, repeatsEveryMiles)
        added.forEach { (milestone, jobs) ->
            val id = insert(milestone)
            insertJobs(jobs.mapIndexed { i, job -> job.copy(id = 0, milestoneId = id, position = i) })
        }
        replaced.forEach { (milestone, jobs) ->
            update(milestone)
            clearJobs(milestone.id)
            insertJobs(jobs.mapIndexed { i, job -> job.copy(id = 0, milestoneId = milestone.id, position = i) })
        }
    }
}

/** A row's id and one whole-number column of it. */
data class IdValue(val id: Long, val value: Int?)

data class ScheduleDistances(val id: Long, val intervalMiles: Int?, val severeMiles: Int?, val snoozedUntilOdometer: Int?, val firstMiles: Int?)

/**
 * Changing what a vehicle's odometer counts in. Every distance stored for a vehicle is in its own
 * unit, so the unit can't change without the figures changing with it: [convert] does both in one
 * transaction, once, and nothing else writes `vehicles.distanceUnit` for a vehicle that exists.
 */
@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId")
    fun observe(vehicleId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun byId(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId")
    suspend fun documentsFor(vehicleId: Long): List<DocumentEntity>

    @Insert suspend fun insert(document: DocumentEntity): Long

    @Query("UPDATE documents SET type = :type, name = :name, date = :date WHERE id = :id")
    suspend fun describe(id: Long, type: String, name: String, date: LocalDate?)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: Long)

    /** Every document file, archived vehicles' included. */
    @Query("SELECT fileName FROM documents")
    suspend fun allFiles(): List<String>
}

@Dao
interface UnitDao {
    @Query("SELECT id, odometer AS value FROM odometer_readings WHERE vehicleId = :vehicleId")
    suspend fun readings(vehicleId: Long): List<IdValue>

    @Query("UPDATE odometer_readings SET odometer = :value WHERE id = :id")
    suspend fun setReading(id: Long, value: Int)

    @Query("SELECT id, odometer AS value FROM service_records WHERE vehicleId = :vehicleId")
    suspend fun records(vehicleId: Long): List<IdValue>

    @Query("UPDATE service_records SET odometer = :value WHERE id = :id")
    suspend fun setRecord(id: Long, value: Int)

    @Query("SELECT id, intervalMiles, severeMiles, snoozedUntilOdometer, firstMiles FROM schedule_items WHERE vehicleId = :vehicleId")
    suspend fun schedule(vehicleId: Long): List<ScheduleDistances>

    @Query("UPDATE schedule_items SET intervalMiles = :interval, severeMiles = :severe, snoozedUntilOdometer = :snooze, firstMiles = :first WHERE id = :id")
    suspend fun setSchedule(id: Long, interval: Int?, severe: Int?, snooze: Int?, first: Int?)

    @Query("SELECT id, atMiles AS value FROM milestones WHERE vehicleId = :vehicleId ORDER BY atMiles")
    suspend fun milestones(vehicleId: Long): List<IdValue>

    @Query("UPDATE milestones SET atMiles = :value WHERE id = :id")
    suspend fun setMilestone(id: Long, value: Int)

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun vehicle(id: Long): VehicleEntity?

    @Query("UPDATE vehicles SET distanceUnit = :unit, odometerOffset = :offset, clusterStartReading = :start, milestonesRepeatEveryMiles = :repeat WHERE id = :id")
    suspend fun setVehicle(id: Long, unit: String, offset: Int, start: Int?, repeat: Int?)

    /**
     * Puts the vehicle and everything stored for it into [to]. Measurements (readings, snooze points,
     * the cluster offset) are converted exactly; schedule figures (intervals, milestones, how a
     * milestone table repeats) the way a manual gives them. See DistanceConversion.
     */
    @Transaction
    suspend fun convert(vehicleId: Long, to: DistanceUnit) {
        val vehicle = vehicle(vehicleId) ?: return
        val from = DistanceUnit.of(vehicle.distanceUnit)
        if (from == to) return
        fun measured(value: Int) = DistanceConversion.convert(value, from, to, DistanceKind.MEASURED)
        fun scheduled(value: Int) = DistanceConversion.convert(value, from, to, DistanceKind.SCHEDULED)

        readings(vehicleId).forEach { r -> r.value?.let { setReading(r.id, measured(it)) } }
        records(vehicleId).forEach { r -> r.value?.let { setRecord(r.id, measured(it)) } }
        schedule(vehicleId).forEach { s ->
            setSchedule(s.id, s.intervalMiles?.let(::scheduled), s.severeMiles?.let(::scheduled), s.snoozedUntilOdometer?.let(::measured), s.firstMiles?.let(::scheduled))
        }

        val table = milestones(vehicleId)
        val moved = DistanceConversion.milestones(table.map { it.value ?: 0 }, from, to)
        // One milestone per mileage is enforced row by row, so they are moved in the order that never
        // steps on a row still waiting: from the top when the figures grow, from the bottom when they shrink.
        val order = if (to == DistanceUnit.KILOMETERS) table.indices.reversed() else table.indices
        for (i in order) setMilestone(table[i].id, moved[i])
        // A cycle is never shorter than its table.
        val repeat = vehicle.milestonesRepeatEveryMiles?.let { maxOf(scheduled(it), moved.lastOrNull() ?: 0) }

        setVehicle(vehicleId, to.name, measured(vehicle.odometerOffset), vehicle.clusterStartReading?.let(::measured), repeat)
    }
}
