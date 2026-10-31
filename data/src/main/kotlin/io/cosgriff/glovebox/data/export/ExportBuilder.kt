package io.cosgriff.glovebox.data.export

import io.cosgriff.glovebox.data.db.DocumentDao
import io.cosgriff.glovebox.data.db.RecallDao
import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.toStored
import io.cosgriff.glovebox.data.log.VehicleLog
import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.data.nhtsa.toStored
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.ExportPaper
import io.cosgriff.glovebox.domain.ExportRecall
import io.cosgriff.glovebox.domain.ExportRecord
import io.cosgriff.glovebox.domain.ExportScheduleLine
import io.cosgriff.glovebox.domain.ExportVehicle
import io.cosgriff.glovebox.domain.LicensePlates
import io.cosgriff.glovebox.domain.Recalls
import io.cosgriff.glovebox.domain.ReportFacts
import io.cosgriff.glovebox.domain.ServiceLog
import io.cosgriff.glovebox.domain.TrackedItem
import io.cosgriff.glovebox.domain.VehicleDocuments
import io.cosgriff.glovebox.domain.VehicleText
import io.cosgriff.glovebox.domain.asReadOn
import java.time.LocalDate
import javax.inject.Inject

/**
 * The words an export's facts are named in that only `app` has (#579). The builder is handed them, as the writers
 * are handed `ExportWords`; figures, names and notes the owner typed go in as they are.
 */
interface ExportNames {
    /** The title of a vehicle whose year, make and model say nothing. */
    val unknownVehicle: String

    /** Who did a record's work when the owner did. */
    val diy: String

    /** Who did a shop's work when no shop is named. */
    val aShop: String

    /** "What it takes", as (label, one line), only the rows the owner has filled in. */
    fun whatItTakes(vehicle: VehicleEntity): List<Pair<String, String>>

    fun documentType(type: DocumentType): String

    /** A recall's component as NHTSA's text, worded. */
    fun recallComponent(component: String): String

    /** How often [item] comes round: "Every 5,000 mi or 6 months". */
    fun every(item: TrackedItem, unit: DistanceUnit): String
}

/**
 * Puts an export together from each vehicle's log, its recalls and its documents (#579), so the export sheet only
 * holds the choices. Each record's work comes from [VehicleLog.summary], the one the timeline reads too.
 */
class ExportBuilder @Inject constructor(
    private val vehicles: VehicleDao,
    private val logs: VehicleLogs,
    private val recalls: RecallDao,
    private val documents: DocumentDao,
) {
    /** Every vehicle, garage and sold, with what an export says of it. A vehicle deleted meanwhile is left out. */
    suspend fun all(names: ExportNames, today: LocalDate): List<Pair<VehicleEntity, ExportVehicle>> =
        vehicles.all().mapNotNull { v -> logs.load(v.id)?.let { log -> log.vehicle to vehicle(log, names, today) } }

    /** What an export says of [log]'s vehicle, every record and table in full; `Exports.document` applies the options. */
    suspend fun vehicle(log: VehicleLog, names: ExportNames, today: LocalDate): ExportVehicle {
        val v = log.vehicle
        val swap = log.swap
        val stored = recalls.recallsFor(v.id).associateBy { it.id }
        return ExportVehicle(
            title = VehicleText.title(v.year, v.make, v.model, v.trim) ?: names.unknownVehicle,
            nickname = v.nickname,
            vin = v.vin,
            plate = LicensePlates.label(v.plate, v.plateRegion),
            unit = log.unit,
            odometer = log.odometer,
            soldOn = v.archivedOn,
            clusterSwap = swap,
            whatItTakes = names.whatItTakes(v),
            documents = VehicleDocuments.sorted(documents.documentsFor(v.id).map { it.toStored() })
                .map { ExportPaper(names.documentType(it.type), it.name, it.date, it.expiresOn) },
            records = log.records.map { r ->
                ExportRecord(
                    date = r.record.date,
                    odometer = r.record.odometer,
                    summary = log.summary(r),
                    doneBy = if (r.record.diy) names.diy else r.record.shopName ?: names.aShop,
                    notes = r.record.notes,
                    photoFiles = r.photos.map { it.fileName },
                    enteredAsLastDone = r.record.enteredAsLastDone,
                    imported = r.record.imported,
                    visitId = r.record.visitId,
                    diy = r.record.diy,
                )
            },
            schedule = log.items.filter { it.tracked }.mapNotNull { item ->
                val tracked = log.tracked(item.id) ?: return@mapNotNull null
                // As the odometer read that day, to match the record it comes from.
                ExportScheduleLine(item.name, names.every(tracked, log.unit), ServiceLog.lastDone(item.id, log.entries)?.let { swap.asReadOn(it) }, item.id)
            },
            recalls = Recalls.sorted(stored.values.map { it.toStored() }).map { r ->
                val row = stored.getValue(r.id)
                ExportRecall(Recalls.campaignLabel(r.notice.campaign), names.recallComponent(r.notice.component), row.completedOn, row.archivedOn)
            },
            specs = listOfNotNull(v.drivetrain?.substringBefore('/'), v.engine).joinToString(" · ").ifEmpty { null },
            report = ReportFacts(log.statuses(today), log.itemGroups, ServiceLog.readings(log.entries, log.updates)),
        )
    }
}
