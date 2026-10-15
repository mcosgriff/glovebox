package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
    @TypeConverter fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}

@Database(
    entities = [
        VehicleEntity::class,
        OdometerReadingEntity::class,
        ScheduleItemEntity::class,
        ServiceRecordEntity::class,
        ServiceRecordItemEntity::class,
        PhotoEntity::class,
        TorqueSpecEntity::class,
        RecallEntity::class,
        MilestoneEntity::class,
        MilestoneJobEntity::class,
        DocumentEntity::class,
    ],
    version = 25,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // schedule_items.templateKey
        AutoMigration(from = 2, to = 3), // service_record_items.catalogId
        AutoMigration(from = 3, to = 4), // recalls, vehicles.recallsCheckedOn
        AutoMigration(from = 4, to = 5), // vehicles.oilFilter, wiperBlades, photoFile
        AutoMigration(from = 5, to = 6), // vehicles.fuel
        AutoMigration(from = 6, to = 7), // vehicles.transmission, trimConfirmed
        AutoMigration(from = 7, to = 8), // vehicles.enteredByHand
        AutoMigration(from = 8, to = 9), // vehicles.clusterSwappedOn, clusterStartReading
        AutoMigration(from = 9, to = 10), // service_records.enteredAsLastDone
        AutoMigration(from = 10, to = 11), // service_records.imported
        AutoMigration(from = 11, to = 12), // milestones, milestone_jobs
        AutoMigration(from = 12, to = 13), // schedule_items.service
        AutoMigration(from = 13, to = 14), // vehicles.milestonesRepeatEveryMiles
        AutoMigration(from = 14, to = 15), // milestone_jobs.position
        AutoMigration(from = 15, to = 16), // vehicles.distanceUnit
        AutoMigration(from = 16, to = 17), // torque_specs.knownList
        AutoMigration(from = 17, to = 18), // vehicles.milestonesKnownList, schedule_items.knownList
        AutoMigration(from = 18, to = 19), // schedule_items.firstMiles
        AutoMigration(from = 19, to = 20), // schedule_items.severeOnly
        AutoMigration(from = 20, to = 21), // vehicles: oilApproval, oilCapacity(Unit), tireSizeRear, wiper lengths
        AutoMigration(from = 21, to = 22), // vehicles: tire pressures and the rest of "What it takes"
        AutoMigration(from = 22, to = 23), // recalls.archivedOn
        AutoMigration(from = 23, to = 24), // vehicles.plate, plateRegion
        AutoMigration(from = 24, to = 25), // documents
    ],
)
@TypeConverters(Converters::class)
abstract class GloveboxDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao
    abstract fun service(): ServiceDao
    abstract fun schedule(): ScheduleDao
    abstract fun odometer(): OdometerDao
    abstract fun torque(): TorqueDao
    abstract fun recalls(): RecallDao
    abstract fun milestones(): MilestoneDao
    abstract fun units(): UnitDao
    abstract fun documents(): DocumentDao

    companion object {
        const val FILE_NAME = "glovebox.db"

        fun open(context: Context, keys: DatabaseKeyManager): GloveboxDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, GloveboxDatabase::class.java, FILE_NAME)
                .openHelperFactory(SupportOpenHelperFactory(keys.passphrase()))
                .build()
        }
    }
}
