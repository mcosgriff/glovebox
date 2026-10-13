package io.cosgriff.glovebox.data.db

import androidx.room.ColumnInfo
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
    /** Added without a VIN decode: every detail is the user's own, even if a VIN is filled in later. */
    @ColumnInfo(defaultValue = "0") val enteredByHand: Boolean = false,
    val year: Int,
    val make: String,
    val model: String,
    val trim: String? = null,
    /** The user picked or typed the trim when adding the vehicle, instead of taking vPIC's as it came. */
    @ColumnInfo(defaultValue = "0") val trimConfirmed: Boolean = false,
    val engine: String? = null,
    /** vPIC's primary fuel type, e.g. "Gasoline" or "Electric". Null for vehicles added before it was kept. */
    val fuel: String? = null,
    val drivetrain: String? = null,
    val transmission: String? = null,
    val bodyClass: String? = null,
    val nickname: String? = null,
    val oilSpec: String? = null,
    val oilFilter: String? = null,
    val tireSize: String? = null,
    val wiperBlades: String? = null,
    /** Name of the vehicle's own encrypted photo in PhotoCrypto.photoDir. */
    val photoFile: String? = null,
    /**
     * An instrument cluster replacement (ClusterSwap in :domain), in force only when [clusterSwappedOn]
     * and [clusterStartReading] are both set: from that day the dash reads [odometerOffset] miles less
     * than the old cluster would have. Readings themselves are stored as typed.
     */
    val odometerOffset: Int = 0,
    val clusterSwappedOn: LocalDate? = null,
    /** What the replacement cluster read when it went in. */
    val clusterStartReading: Int? = null,
    val archivedOn: LocalDate? = null,
    val createdOn: LocalDate,
    /** When NHTSA last answered for this vehicle's recalls; null = never checked. */
    val recallsCheckedOn: LocalDate? = null,
    /**
     * The length of the cycle when the user's own milestone table (`milestones`) starts again after
     * its last row; null for a table that ends. It belongs to the table as a whole, is written only
     * by an import, and is cleared when the table is removed. Means nothing with no table.
     */
    val milestonesRepeatEveryMiles: Int? = null,
    /**
     * A DistanceUnit name: what this vehicle's odometer counts in. Every distance stored for the vehicle
     * (readings, intervals, snooze points, the cluster offset, milestones) is in this unit as it was typed;
     * column names that say "miles" are older than the choice. Changed only through UnitDao.convert, which
     * converts those figures in the same transaction.
     */
    @ColumnInfo(defaultValue = "MILES") val distanceUnit: String = "MILES",
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
    /**
     * For the user's own items only: the CatalogItem id of the built-in service line that resets it.
     * Null means it is logged under "Your items". Typical items answer through [templateKey] instead.
     */
    val service: String? = null,
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
    /**
     * Given as a bare "last done" (a date and a mileage) to start a reminder counting, not logged as a
     * service when the work was done. Shown as such wherever the record appears.
     */
    @ColumnInfo(defaultValue = "0") val enteredAsLastDone: Boolean = false,
    /** Came from an import file (or was replaced by one), not logged in the app. Shown as such wherever the record appears. */
    @ColumnInfo(defaultValue = "0") val imported: Boolean = false,
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

/**
 * One point of the user's own milestone table, imported from a file: [atMiles] miles really covered,
 * with the work due there in `milestone_jobs`. Information for the Milestones screen only; nothing
 * about reminders reads these tables, and whether a job was done is derived from the service log.
 */
@Entity(
    tableName = "milestones",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE)],
    indices = [Index("vehicleId", "atMiles", unique = true)],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val atMiles: Int,
    /** The vehicle's age at which the same work is due if the mileage isn't reached; shown, not acted on. */
    val orMonths: Int? = null,
)

@Entity(
    tableName = "milestone_jobs",
    foreignKeys = [ForeignKey(MilestoneEntity::class, ["id"], ["milestoneId"], onDelete = CASCADE)],
    indices = [Index("milestoneId")],
)
data class MilestoneJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val milestoneId: Long,
    val name: String,
    /** A CatalogItem id, when the job is one of Glovebox's own service lines. */
    val service: String? = null,
    /** Where the job comes in its milestone, from 0. Rows from before this was kept are all 0 and fall back to the order they were added. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
)

data class MilestoneWithJobs(
    @Embedded val milestone: MilestoneEntity,
    @Relation(parentColumn = "id", entityColumn = "milestoneId") val jobs: List<MilestoneJobEntity>,
) {
    /** The jobs in the order the user gave them; a relation comes in no particular order. */
    val ordered: List<MilestoneJobEntity> get() = jobs.sortedWith(compareBy({ it.position }, { it.id }))
}

data class ServiceRecordWithItems(
    @Embedded val record: ServiceRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId") val items: List<ServiceRecordItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "recordId") val photos: List<PhotoEntity>,
)
