package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import io.cosgriff.glovebox.data.crypto.DatabaseKeyManager
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import java.time.LocalDate

class Converters {
    @ColumnTypeConverter fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
    @ColumnTypeConverter fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
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
        CatalogRuleEntity::class,
        RecallNameEntity::class,
    ],
    version = 34,
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
        AutoMigration(from = 25, to = 26), // vehicles.takesKnownList, takesToCheck
        AutoMigration(from = 26, to = 27), // vehicles.mileageCheckInDays
        AutoMigration(from = 27, to = 28), // catalog_rules
        AutoMigration(from = 28, to = 29), // service_records.visitId
        AutoMigration(from = 29, to = 30), // vehicles.powertrain
        AutoMigration(from = 30, to = 31), // schedule_items.e85Miles, e85Months, useE85
        AutoMigration(from = 31, to = 32), // documents.expiresOn
        AutoMigration(from = 32, to = 33), // vehicles.engineCode, tpms, series
        AutoMigration(from = 33, to = 34), // recall_names
    ],
)
@ColumnTypeConverters(Converters::class)
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
    abstract fun catalogRules(): CatalogRuleDao

    companion object {
        const val FILE_NAME = "glovebox.db"

        fun open(context: Context, keys: DatabaseKeyManager): GloveboxDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, GloveboxDatabase::class.java, FILE_NAME)
                // Opens the file exactly as the support open helper did before (#220): the same
                // SQLiteDatabase.openDatabase with the same passphrase bytes, no hook, no error handler.
                .setDriver(SQLCipherDriver(keys.passphrase(), null, null))
                .build()
        }
    }
}
