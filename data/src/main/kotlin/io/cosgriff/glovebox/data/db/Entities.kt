package io.cosgriff.glovebox.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.ForeignKey.Companion.CASCADE
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
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
    /** A `Powertrain` name ("HYBRID"): from a plain vPIC decode, or the owner's pick. Null is not known, never petrol. */
    val powertrain: String? = null,
    val drivetrain: String? = null,
    val transmission: String? = null,
    val bodyClass: String? = null,
    val nickname: String? = null,
    /** As typed, in capitals (`LicensePlates.clean`). Kept on the phone only. */
    val plate: String? = null,
    /** Where the plate was issued: a `PlateRegion` code ("CO", "ON"). */
    val plateRegion: String? = null,
    /** The oil grade ("0W-20"). Entered before #165 it may be free text, which the editor asks to fix. */
    val oilSpec: String? = null,
    /** Filter part numbers, up to three separated by commas. */
    val oilFilter: String? = null,
    /** The approval the manual names ("API SP", "VW 502 00"). */
    val oilApproval: String? = null,
    /** How much oil it holds with the filter, in [oilCapacityUnit] (a VehicleSpecEntry.Volume name). */
    val oilCapacity: Double? = null,
    val oilCapacityUnit: String? = null,
    /** The tire size, all round or at the front; [tireSizeRear] only when the rear differs. */
    val tireSize: String? = null,
    val tireSizeRear: String? = null,
    /** Wiper blade part numbers or type, as typed; the lengths are in the three columns after it, in inches. */
    val wiperBlades: String? = null,
    val wiperDriverIn: Int? = null,
    val wiperPassengerIn: Int? = null,
    val wiperRearIn: Int? = null,
    /** Cold tire pressures from the door placard, in [tirePressureUnit] (a VehicleSpecEntry.Pressure name). */
    val tirePressureFront: Int? = null,
    val tirePressureRear: Int? = null,
    val tirePressureSpare: Int? = null,
    val tirePressureUnit: String? = null,
    // The rest of "What it takes" (#166), each already checked (VehicleSpecEntry); null is not entered.
    val transmissionFluid: String? = null,
    val differentialOil: String? = null,
    val transferCaseOil: String? = null,
    val coolantType: String? = null,
    val brakeFluid: String? = null,
    val engineAirFilter: String? = null,
    val cabinAirFilter: String? = null,
    val sparkPlug: String? = null,
    val sparkPlugGap: String? = null,
    val batteryGroup: String? = null,
    val batteryCca: Int? = null,
    val bulbLow: String? = null,
    val bulbHigh: String? = null,
    val bulbFog: String? = null,
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
     * The id of the bundled schedule (KnownSchedules) the milestone table was added from; null for a
     * table the user typed or imported. Kept while the table is, whatever is edited in it, and cleared
     * with it: the Milestones screen says where the table started from.
     */
    val milestonesKnownList: String? = null,
    /**
     * A DistanceUnit name: what this vehicle's odometer counts in. Every distance stored for the vehicle
     * (readings, intervals, snooze points, the cluster offset, milestones) is in this unit as it was typed;
     * column names that say "miles" are older than the choice. Changed only through UnitDao.convert, which
     * converts those figures in the same transaction.
     */
    @ColumnInfo(defaultValue = "MILES") val distanceUnit: String = "MILES",
    /**
     * The id of the bundled list (KnownTakes) rows of "What it takes" were last filled from; null if
     * none ever was. Kept after the rows are checked or changed: it names the source on the screen.
     */
    val takesKnownList: String? = null,
    /**
     * TakesItem names, comma-separated, of the rows filled from [takesKnownList] that the owner hasn't
     * checked or changed yet: each is tagged DOUBLE-CHECK. Null when there are none.
     */
    val takesToCheck: String? = null,
    /**
     * This vehicle's own mileage check-in, in days (VehicleCheckIn in :domain): null asks as often as
     * the Notifications setting, 0 never asks.
     */
    val mileageCheckInDays: Int? = null,
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
    /**
     * The id of the bundled schedule (KnownSchedules) this item was added from, and stays set whatever
     * happens to it later. Null for everything else. The item is the user's own all the same.
     */
    val knownList: String? = null,
    /**
     * For a job whose first time isn't one interval in ("first at 100,000, then every 50,000"): the
     * distance the vehicle has covered when it is first due, in the vehicle's unit. It matters only
     * until the job has been logged once. Null for every other item.
     */
    val firstMiles: Int? = null,
    /**
     * A job that exists only under severe use (a maker's "special operating conditions": differential
     * oil when towing). Its interval is in [intervalMiles] / [intervalMonths], and it reminds only while
     * [useSevere] is on; on normal use it stays on the list, switched off.
     */
    @ColumnInfo(defaultValue = "0") val severeOnly: Boolean = false,
    /**
     * A flex-fuel maker's interval for a vehicle run mostly on E85 (a schedule figure in the vehicle's unit),
     * from a bundled list; null for every other item. It is the one in force while [useE85] is on (#33).
     */
    val e85Miles: Int? = null,
    val e85Months: Int? = null,
    /** The owner has said the vehicle runs mostly on E85. Changes nothing on an item without an E85 interval. */
    @ColumnInfo(defaultValue = "0") val useE85: Boolean = false,
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
    /**
     * Shared by the records the user linked as one visit to a shop (#307); null for a record in none. A visit
     * only groups records: each keeps its own lines, flags, photos and cost. An id held by one record alone
     * means nothing (see `Visits` in `:domain`), so deleting or unlinking the others needs no tidying.
     */
    val visitId: Long? = null,
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

/**
 * A piece of a vehicle's paperwork (a history report, the title, the registration). The file is
 * encrypted in PhotoCrypto.photoDir under [fileName], beside the photos, so the sweep, backups and
 * Delete all data treat it as they treat a photo.
 */
@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = CASCADE)],
    indices = [Index("vehicleId")],
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    /** A DocumentType name. */
    val type: String,
    val name: String,
    /** The date on the document, if the owner gave one: an issue date, a report's date. */
    val date: LocalDate? = null,
    /** Name of the encrypted file in PhotoCrypto.photoDir. */
    val fileName: String,
    /** A DocumentFormat name: PDF, or JPEG for a picture. */
    val format: String,
    /** Size before encryption. */
    val sizeBytes: Long,
    val addedOn: LocalDate,
    /** When it runs out, if the owner gave a date (#283): a registration, a policy, a warranty. */
    val expiresOn: LocalDate? = null,
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
    /** A TorqueSource name: FACTORY, USER, or REFERENCE for a value from a bundled list the user hasn't confirmed. */
    val source: String = "USER",
    val note: String? = null,
    /**
     * The id of the bundled list (KnownSpecs) this row was added from, and stays set whatever happens to
     * the row afterwards: a value that began as somebody else's reference figure always says so.
     */
    val knownList: String? = null,
)

/**
 * A recall NHTSA lists for the vehicle's year, make and model. [completedOn] is the user's own mark:
 * NHTSA can't say whether a given VIN was repaired. [serviceRecordId] is the record added when it
 * was marked done, if there is one. [archivedOn] is the user's other mark: the recall doesn't apply to
 * this vehicle (no notice came, and the maker's VIN lookup doesn't list it), so it no longer counts as open.
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
    val archivedOn: LocalDate? = null,
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
    @Relation(parentColumns = ["id"], entityColumns = ["milestoneId"]) val jobs: List<MilestoneJobEntity>,
) {
    /** The jobs in the order the user gave them; a relation comes in no particular order. */
    val ordered: List<MilestoneJobEntity> get() = jobs.sortedWith(compareBy({ it.position }, { it.id }))
}

/**
 * The user's rule that a line written [text] is the catalog line [catalogId] (#310, `CatalogRules` in
 * `:domain`). [textKey] is `CatalogRules.key(text)`: one rule per wording, whatever its case or spacing.
 * Not a vehicle's: rules are the user's, and stay when a vehicle is deleted.
 */
@Entity(tableName = "catalog_rules", indices = [Index(value = ["textKey"], unique = true)])
data class CatalogRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val textKey: String,
    val catalogId: String,
    val createdOn: LocalDate,
)

data class ServiceRecordWithItems(
    @Embedded val record: ServiceRecordEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["recordId"]) val items: List<ServiceRecordItemEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["recordId"]) val photos: List<PhotoEntity>,
)
