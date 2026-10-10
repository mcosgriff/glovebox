package io.cosgriff.glovebox.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE archivedOn IS NULL ORDER BY createdOn")
    fun observeActive(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE archivedOn IS NOT NULL ORDER BY archivedOn DESC")
    fun observeArchived(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observe(id: Long): Flow<VehicleEntity?>

    @Query(
        "SELECT vehicleId, MAX(odometer) AS odometer FROM (" +
            "SELECT vehicleId, odometer FROM odometer_readings " +
            "UNION ALL SELECT vehicleId, odometer FROM service_records" +
            ") GROUP BY vehicleId",
    )
    fun observeOdometers(): Flow<List<VehicleOdometer>>

    @Insert suspend fun insert(vehicle: VehicleEntity): Long
    @Update suspend fun update(vehicle: VehicleEntity)

    /** Cascades to readings, schedule, records, record items, photos rows and torque specs. */
    @Delete suspend fun delete(vehicle: VehicleEntity)

    @Query("SELECT p.fileName FROM photos p JOIN service_records r ON p.recordId = r.id WHERE r.vehicleId = :vehicleId")
    suspend fun photoFilesFor(vehicleId: Long): List<String>
}

@Dao
interface ServiceDao {
    @Transaction
    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun observeRecords(vehicleId: Long): Flow<List<ServiceRecordWithItems>>

    @Transaction
    @Query("SELECT * FROM service_records WHERE id = :id")
    fun observeRecord(id: Long): Flow<ServiceRecordWithItems?>

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
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items WHERE vehicleId = :vehicleId AND tracked = 1 ORDER BY name")
    fun observeTracked(vehicleId: Long): Flow<List<ScheduleItemEntity>>

    /** Tracked and untracked. */
    @Query("SELECT * FROM schedule_items WHERE vehicleId = :vehicleId ORDER BY name")
    suspend fun allFor(vehicleId: Long): List<ScheduleItemEntity>

    @Insert suspend fun insertAll(items: List<ScheduleItemEntity>)
    @Update suspend fun update(item: ScheduleItemEntity)
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY date")
    fun observe(vehicleId: Long): Flow<List<OdometerReadingEntity>>

    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY date")
    suspend fun readingsFor(vehicleId: Long): List<OdometerReadingEntity>

    @Insert suspend fun insert(reading: OdometerReadingEntity)
}

@Dao
interface TorqueDao {
    @Query("SELECT * FROM torque_specs WHERE vehicleId = :vehicleId ORDER BY category, name")
    fun observe(vehicleId: Long): Flow<List<TorqueSpecEntity>>

    @Insert suspend fun insert(spec: TorqueSpecEntity): Long
    @Update suspend fun update(spec: TorqueSpecEntity)
    @Delete suspend fun delete(spec: TorqueSpecEntity)
}
