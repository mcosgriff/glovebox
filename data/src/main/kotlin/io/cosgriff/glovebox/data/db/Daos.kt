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

    @Insert suspend fun insert(spec: TorqueSpecEntity): Long
    @Insert suspend fun insertAll(specs: List<TorqueSpecEntity>)
    @Update suspend fun update(spec: TorqueSpecEntity)
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
