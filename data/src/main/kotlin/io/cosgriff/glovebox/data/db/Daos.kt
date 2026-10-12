package io.cosgriff.glovebox.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
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

    @Query("UPDATE vehicles SET oilSpec = :value WHERE id = :id")
    suspend fun setOilSpec(id: Long, value: String?)

    @Query("UPDATE vehicles SET oilFilter = :value WHERE id = :id")
    suspend fun setOilFilter(id: Long, value: String?)

    @Query("UPDATE vehicles SET tireSize = :value WHERE id = :id")
    suspend fun setTireSize(id: Long, value: String?)

    @Query("UPDATE vehicles SET wiperBlades = :value WHERE id = :id")
    suspend fun setWiperBlades(id: Long, value: String?)

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

    /** Removes the whole table; the Milestones screen then goes back to the view built from the schedule. */
    @Query("DELETE FROM milestones WHERE vehicleId = :vehicleId")
    suspend fun deleteTable(vehicleId: Long)

    /** One import, applied whole or not at all. Nothing is deleted by one; a replaced milestone keeps its row. */
    @Transaction
    suspend fun applyImport(added: List<Pair<MilestoneEntity, List<MilestoneJobEntity>>>, replaced: List<Pair<MilestoneEntity, List<MilestoneJobEntity>>>) {
        added.forEach { (milestone, jobs) ->
            val id = insert(milestone)
            insertJobs(jobs.map { it.copy(id = 0, milestoneId = id) })
        }
        replaced.forEach { (milestone, jobs) ->
            update(milestone)
            clearJobs(milestone.id)
            insertJobs(jobs.map { it.copy(id = 0, milestoneId = milestone.id) })
        }
    }
}
