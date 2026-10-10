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
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // schedule_items.templateKey
        AutoMigration(from = 2, to = 3), // service_record_items.catalogId
    ],
)
@TypeConverters(Converters::class)
abstract class GloveboxDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao
    abstract fun service(): ServiceDao
    abstract fun schedule(): ScheduleDao
    abstract fun odometer(): OdometerDao
    abstract fun torque(): TorqueDao

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
