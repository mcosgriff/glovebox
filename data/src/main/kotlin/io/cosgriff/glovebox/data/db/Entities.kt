package io.cosgriff.glovebox.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

// Every child table cascades from vehicles, so "Delete vehicle and all its data" is one DELETE
// (plus removing that vehicle's photo files).

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vin: String?,
    val year: Int,
    val make: String,
    val model: String,
    val trim: String? = null,
    val engine: String? = null,
    /** vPIC's primary fuel type, e.g. "Gasoline" or "Electric". Null for vehicles added before it was kept. */
    val fuel: String? = null,
    val drivetrain: String? = null,
    val bodyClass: String? = null,
    val nickname: String? = null,
    val oilSpec: String? = null,
    val oilFilter: String? = null,
    val tireSize: String? = null,
    val wiperBlades: String? = null,
    /** Name of the vehicle's own encrypted photo in PhotoCrypto.photoDir. */
    val photoFile: String? = null,
    /** Miles to add to the displayed odometer after a cluster replacement. */
    val odometerOffset: Int = 0,
    val archivedOn: LocalDate? = null,
    val createdOn: LocalDate,
    /** When NHTSA last answered for this vehicle's recalls; null = never checked. */
    val recallsCheckedOn: LocalDate? = null,
)

@Entity(
    tableName = "odometer_readings",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE)],
    indices = [Index("vehicleId")],
)
data class OdometerReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometer: Int,
)

@Entity(
    tableName = "schedule_items",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE)],
    indices = [Index("vehicleId")],
)
data class ScheduleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    /** ScheduleTemplateItem.key this came from; null for the user's own items. Links catalog work to it. */
    val templateKey: String? = null,
    val intervalMiles: Int?,
    val intervalMonths: Int?,
    val severeMiles: Int? = null,
    val severeMonths: Int? = null,
    val useSevere: Boolean = false,
    /** Null = use the app-wide default rule. Stores DueRule.name. */
    val ruleOverride: String? = null,
    val tracked: Boolean = true,
    val snoozedUntilOdometer: Int? = null,
    val snoozedUntilDate: LocalDate? = null,
)

@Entity(
    tableName = "service_records",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE)],
    indices = [Index("vehicleId")],
)
data class ServiceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val date: LocalDate,
    val odometer: Int,
    val diy: Boolean,
    val shopName: String? = null,
    val costCents: Long? = null,
    val notes: String? = null,
)

/** One line of work on a record. [scheduleItemId] links it to the reminder it resets, if any. */
@Entity(
    tableName = "service_record_items",
    foreignKeys = [
        ForeignKey(ServiceRecordEntity::class, ["id"], ["recordId"], onDelete = CASCADE),
        ForeignKey(ScheduleItemEntity::class, ["id"], ["scheduleItemId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("recordId"), Index("scheduleItemId")],
)
data class ServiceRecordItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val scheduleItemId: Long?,
    val label: String,
    /** CatalogItem.id this line was picked from; null for custom lines and the user's own schedule items. */
    val catalogId: String? = null,
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(ServiceRecordEntity::class, ["id"], ["recordId"], onDelete = CASCADE)],
    indices = [Index("recordId")],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    /** Name of the encrypted file in PhotoCrypto.photoDir. */
    val fileName: String,
)

@Entity(
    tableName = "torque_specs",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE)],
    indices = [Index("vehicleId")],
)
data class TorqueSpecEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val category: String,
    val socketSize: String? = null,
    /** Canonical storage in N·m; null = not entered yet. */
    val newtonMeters: Double? = null,
    /** FACTORY or USER. */
    val source: String = "USER",
    val note: String? = null,
)

/**
 * A recall NHTSA lists for the vehicle's year, make and model. [completedOn] is the user's own mark:
 * NHTSA can't say whether a given VIN was repaired. [serviceRecordId] is the record added when it
 * was marked done, if there is one.
 */
@Entity(
    tableName = "recalls",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE),
        ForeignKey(ServiceRecordEntity::class, ["id"], ["serviceRecordId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId", "campaign", unique = true), Index("serviceRecordId")],
)
data class RecallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    /** NHTSA campaign number as sent, e.g. "20V682000". */
    val campaign: String,
    val component: String,
    val summary: String,
    val consequence: String? = null,
    val remedy: String? = null,
    val reportedOn: LocalDate? = null,
    val firstSeenOn: LocalDate,
    val completedOn: LocalDate? = null,
    val serviceRecordId: Long? = null,
)

data class ServiceRecordWithItems(
    @Embedded val record: ServiceRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId") val items: List<ServiceRecordItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "recordId") val photos: List<PhotoEntity>,
)
