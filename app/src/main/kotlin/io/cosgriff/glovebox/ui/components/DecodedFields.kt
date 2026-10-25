package io.cosgriff.glovebox.ui.components

import androidx.annotation.StringRes
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.nhtsa.DecodedVehicle

/** A decoded value kept with the vehicle: corrected before it is saved, or filled later from the VIN (#462). */
enum class DecodedField(@StringRes val label: Int) {
    BODY(R.string.add_vehicle_body_class),
    DRIVE(R.string.add_vehicle_drive_type),
    ENGINE(R.string.add_vehicle_engine),
    ENGINE_CODE(R.string.add_vehicle_engine_code),
    FUEL(R.string.add_vehicle_fuel),
    TRANSMISSION(R.string.add_vehicle_transmission),
    TPMS(R.string.add_vehicle_tpms),
    SERIES(R.string.add_vehicle_series),
}

/** What vPIC said plainly for each field, as it is stored; a field it left blank is missing. */
fun DecodedVehicle.storedFields(): Map<DecodedField, String> = buildMap {
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
fun VehicleEntity.savedFields(): Map<DecodedField, String?> = mapOf(
    DecodedField.BODY to bodyClass,
    DecodedField.DRIVE to drivetrain,
    DecodedField.ENGINE to engine,
    DecodedField.ENGINE_CODE to engineCode,
    DecodedField.FUEL to fuel,
    DecodedField.TRANSMISSION to transmission,
    DecodedField.TPMS to tpms,
    DecodedField.SERIES to series,
)
