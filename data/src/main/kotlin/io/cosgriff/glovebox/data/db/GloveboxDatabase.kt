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
    ],
    version = 11,
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
