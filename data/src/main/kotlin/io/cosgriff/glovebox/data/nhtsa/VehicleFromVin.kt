package io.cosgriff.glovebox.data.nhtsa

import io.cosgriff.glovebox.data.db.VehicleDao
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.domain.BodyStyle
import io.cosgriff.glovebox.domain.BodyStyles
import io.cosgriff.glovebox.domain.Powertrain
import io.cosgriff.glovebox.domain.Powertrains
import io.cosgriff.glovebox.domain.VinRefresh
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * A value vPIC decodes that is kept with the vehicle, in a column of its own: corrected before the vehicle is saved,
 * or filled later from the VIN (#462). In the order "Is this your vehicle?" lists them; `:app` words each.
 */
enum class DecodedField { BODY, DRIVE, ENGINE, ENGINE_CODE, FUEL, TRANSMISSION, TPMS, SERIES }

/** What a decode starts a new vehicle at, before the owner corrects it on "Is this your vehicle?". */
data class DecodedStart(
    /** What vPIC said plainly for each field, as it is stored; a field it left blank is missing. */
    val fields: Map<DecodedField, String>,
    /** Only what vPIC says plainly ([Powertrains.fromVpic]); null is "not known". */
    val powertrain: Powertrain?,
    /** Only what vPIC says plainly ([BodyStyles.fromVpic]); null is "not known". */
    val bodyStyle: BodyStyle?,
)

/** What looking a saved vehicle's VIN up again found. */
sealed interface VinLookupResult {
    /**
     * What NHTSA has that is empty here; nothing else is ever offered, and never the drivetrain. [bodyStyle] and
     * [powertrain] are what vPIC says plainly, by the rules adding a vehicle starts them with ([VehicleFromVin.start]),
     * and only while the vehicle has none (#644).
     */
    data class Found(
        val additions: Map<DecodedField, String>,
        val bodyStyle: BodyStyle? = null,
        val powertrain: Powertrain? = null,
    ) : VinLookupResult {
        /** Nothing to fill. */
        val isEmpty: Boolean get() = additions.isEmpty() && bodyStyle == null && powertrain == null
    }

    /** NHTSA decodes the VIN as another year or make; this is what it says it is, each null where it couldn't say. */
    data class NotThisVehicle(val year: Int?, val make: String?, val model: String?) : VinLookupResult

    /** NHTSA couldn't be reached, or the vehicle has no VIN to look up. */
    data object Failed : VinLookupResult
}

/**
 * Turns a vPIC decode into a vehicle's columns (docs/rules/vehicles.md): for adding a vehicle by VIN ([start], then
 * [withDecoded] when it is saved), and for looking a saved one's VIN up again on Vehicle details ([lookUp], then
 * [fill]). Only the VIN is sent (ADR-0007).
 */
class VehicleFromVin @Inject constructor(private val nhtsa: NhtsaClient, private val vehicles: VehicleDao) {

    /**
     * Asks vPIC about [vehicle]'s VIN alone, never its year: vPIC answers with whatever year it is sent, which
     * would hide a VIN that is another vehicle's. Offers only what is empty on [vehicle], never the drivetrain (the
     * owner's pick), and nothing when the decode's year or make differ ([VinRefresh.sameVehicle]). The body style and
     * powertrain come from the decode by [start]'s rules, offered only while the vehicle's is null.
     */
    suspend fun lookUp(vehicle: VehicleEntity): VinLookupResult {
        val vin = vehicle.vin ?: return VinLookupResult.Failed
        val decoded = try {
            nhtsa.decodeVin(vin)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return VinLookupResult.Failed
        }
        return if (VinRefresh.sameVehicle(vehicle.year, vehicle.make, decoded.year, decoded.make)) {
            val asNew = start(decoded)
            VinLookupResult.Found(
                additions = VinRefresh.additions(vehicle.savedFields(), asNew.fields - DecodedField.DRIVE),
                bodyStyle = asNew.bodyStyle.takeIf { vehicle.bodyStyle == null },
                powertrain = asNew.powertrain.takeIf { vehicle.powertrain == null },
            )
        } else {
            VinLookupResult.NotThisVehicle(decoded.year, decoded.make, decoded.model)
        }
    }

    /**
     * Fills what [found] (a [lookUp]'s) offers into the vehicle, each column only while it is still empty, so an edit
     * saved while the lookup was out stands ([VehicleDao.fillFromVin], [VehicleDao.setBodyStyleIfBlank],
     * [VehicleDao.setPowertrainIfBlank]): the owner's pick of body style or powertrain is never replaced. The
     * drivetrain is never written.
     */
    suspend fun fill(vehicleId: Long, found: VinLookupResult.Found) {
        val additions = found.additions
        vehicles.fillFromVin(
            vehicleId,
            bodyClass = additions[DecodedField.BODY],
            engine = additions[DecodedField.ENGINE],
            fuel = additions[DecodedField.FUEL],
            transmission = additions[DecodedField.TRANSMISSION],
            engineCode = additions[DecodedField.ENGINE_CODE],
            tpms = additions[DecodedField.TPMS],
            series = additions[DecodedField.SERIES],
        )
        found.bodyStyle?.let { vehicles.setBodyStyleIfBlank(vehicleId, it.name) }
        found.powertrain?.let { vehicles.setPowertrainIfBlank(vehicleId, it.name) }
    }

    companion object {
        /** What [decoded] starts a new vehicle at: each field vPIC filled, and the body style and powertrain it says plainly. */
        fun start(decoded: DecodedVehicle) = DecodedStart(
            fields = decoded.fields(),
            powertrain = Powertrains.fromVpic(decoded.electrification, decoded.fuel, decoded.fuelSecondary),
            bodyStyle = BodyStyles.fromVpic(decoded.bodyClass),
        )
    }
}

/** This vehicle with [fields] (a [DecodedStart]'s, as the owner corrected them) in their columns; one missing is saved empty. */
fun VehicleEntity.withDecoded(fields: Map<DecodedField, String>) = copy(
    bodyClass = fields[DecodedField.BODY],
    drivetrain = fields[DecodedField.DRIVE],
    engine = fields[DecodedField.ENGINE],
    engineCode = fields[DecodedField.ENGINE_CODE],
    fuel = fields[DecodedField.FUEL],
    transmission = fields[DecodedField.TRANSMISSION],
    tpms = fields[DecodedField.TPMS],
    series = fields[DecodedField.SERIES],
)

private fun DecodedVehicle.fields(): Map<DecodedField, String> = buildMap {
    bodyClass?.let { put(DecodedField.BODY, it) }
    driveType?.let { put(DecodedField.DRIVE, it) }
    // Stored as the vehicle's engine, in the form vPIC's figures are usually written; data, not wording.
    listOfNotNull(displacementL?.let { "${it}L" }, cylinders?.let { "$it-cyl" }).joinToString(" ").ifEmpty { null }
        ?.let { put(DecodedField.ENGINE, it) }
    engineModel?.let { put(DecodedField.ENGINE_CODE, it) }
    fuel?.let { put(DecodedField.FUEL, it) }
    transmission?.let { put(DecodedField.TRANSMISSION, it) }
    tpms?.let { put(DecodedField.TPMS, it) }
    series?.let { put(DecodedField.SERIES, it) }
}

/** What the vehicle has saved for each field, empty or not. */
private fun VehicleEntity.savedFields(): Map<DecodedField, String?> = mapOf(
    DecodedField.BODY to bodyClass,
    DecodedField.DRIVE to drivetrain,
    DecodedField.ENGINE to engine,
    DecodedField.ENGINE_CODE to engineCode,
    DecodedField.FUEL to fuel,
    DecodedField.TRANSMISSION to transmission,
    DecodedField.TPMS to tpms,
    DecodedField.SERIES to series,
)
