package io.cosgriff.glovebox.ui.service

import io.cosgriff.glovebox.domain.CatalogCategory
import io.cosgriff.glovebox.domain.CatalogItem
import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.ServiceCatalog
import io.cosgriff.glovebox.domain.WorkCategory
import io.cosgriff.glovebox.domain.WorkOption
import io.cosgriff.glovebox.ui.AppText

/**
 * The names of Log service's lines and categories, by catalog id (`ServiceCatalog` in `:domain` has
 * only ids). `CatalogTextTest` holds these maps to the catalog, so a new line can't go unnamed.
 * A line's name is saved with a record as it was then, like anything typed.
 */
/** Catalog id to its name's string resource. */
internal val catalogLines: Map<String, Int> = mapOf(
    "oil" to R.string.catalog_oil,
    "engine-air" to R.string.catalog_engine_air,
    "spark-plugs" to R.string.catalog_spark_plugs,
    "plug-wires" to R.string.catalog_plug_wires,
    "ignition-coils" to R.string.catalog_ignition_coils,
    "glow-plugs" to R.string.catalog_glow_plugs,
    "belt" to R.string.catalog_belt,
    "belt-check" to R.string.catalog_belt_check,
    "timing-belt" to R.string.catalog_timing_belt,
    "timing-chain" to R.string.catalog_timing_chain,
    "fuel-filter" to R.string.catalog_fuel_filter,
    "fuel-system" to R.string.catalog_fuel_system,
    "throttle-body" to R.string.catalog_throttle_body,
    "fuel-injectors" to R.string.catalog_fuel_injectors,
    "fuel-pump" to R.string.catalog_fuel_pump,
    "pcv" to R.string.catalog_pcv,
    "egr-valve" to R.string.catalog_egr_valve,
    "secondary-air-pump" to R.string.catalog_secondary_air_pump,
    "evap-system" to R.string.catalog_evap_system,
    "oxygen-sensor" to R.string.catalog_oxygen_sensor,
    "engine-sensors" to R.string.catalog_engine_sensors,
    "turbo" to R.string.catalog_turbo,
    "valve-cover-gasket" to R.string.catalog_valve_cover_gasket,
    "head-gasket" to R.string.catalog_head_gasket,
    "intake-gasket" to R.string.catalog_intake_gasket,
    "oil-pan-gasket" to R.string.catalog_oil_pan_gasket,
    "rear-main-seal" to R.string.catalog_rear_main_seal,
    "engine-mounts" to R.string.catalog_engine_mounts,
    "catalytic-converter" to R.string.catalog_catalytic_converter,
    "muffler" to R.string.catalog_muffler,
    "exhaust" to R.string.catalog_exhaust,
    "rotation" to R.string.catalog_rotation,
    "balance" to R.string.catalog_balance,
    "alignment" to R.string.catalog_alignment,
    "new-tires" to R.string.catalog_new_tires,
    "flat" to R.string.catalog_flat,
    "tire-check" to R.string.catalog_tire_check,
    "tpms" to R.string.catalog_tpms,
    "front-pads" to R.string.catalog_front_pads,
    "rear-pads" to R.string.catalog_rear_pads,
    "brake-pads" to R.string.catalog_brake_pads,
    "rotors" to R.string.catalog_rotors,
    "brake-shoes" to R.string.catalog_brake_shoes,
    "brake-drums" to R.string.catalog_brake_drums,
    "brake-fluid" to R.string.catalog_brake_fluid,
    "brake-fluid-check" to R.string.catalog_brake_fluid_check,
    "brake-inspect" to R.string.catalog_brake_inspect,
    "brake-service" to R.string.catalog_brake_service,
    "calipers" to R.string.catalog_calipers,
    "wheel-cylinders" to R.string.catalog_wheel_cylinders,
    "master-cylinder" to R.string.catalog_master_cylinder,
    "brake-booster" to R.string.catalog_brake_booster,
    "brake-lines" to R.string.catalog_brake_lines,
    "parking-brake" to R.string.catalog_parking_brake,
    "abs-sensor" to R.string.catalog_abs_sensor,
    "trans-fluid" to R.string.catalog_trans_fluid,
    "power-steering" to R.string.catalog_power_steering,
    "washer" to R.string.catalog_washer,
    "fluid-check" to R.string.catalog_fluid_check,
    "coolant" to R.string.catalog_coolant,
    "cooling-check" to R.string.catalog_cooling_check,
    "water-pump" to R.string.catalog_water_pump,
    "thermostat" to R.string.catalog_thermostat,
    "radiator" to R.string.catalog_radiator,
    "radiator-cap" to R.string.catalog_radiator_cap,
    "coolant-reservoir" to R.string.catalog_coolant_reservoir,
    "coolant-hoses" to R.string.catalog_coolant_hoses,
    "cooling-fan" to R.string.catalog_cooling_fan,
    "fan-clutch" to R.string.catalog_fan_clutch,
    "heater-core" to R.string.catalog_heater_core,
    "diff-fluid" to R.string.catalog_diff_fluid,
    "front-diff-check" to R.string.catalog_front_diff_check,
    "front-diff" to R.string.catalog_front_diff,
    "rear-diff-check" to R.string.catalog_rear_diff_check,
    "rear-diff" to R.string.catalog_rear_diff,
    "transfer-case-check" to R.string.catalog_transfer_case_check,
    "transfer-case" to R.string.catalog_transfer_case,
    "driveshaft" to R.string.catalog_driveshaft,
    "cv-axle" to R.string.catalog_cv_axle,
    "axle-seals" to R.string.catalog_axle_seals,
    "u-joints" to R.string.catalog_u_joints,
    "clutch" to R.string.catalog_clutch,
    "clutch-hydraulics" to R.string.catalog_clutch_hydraulics,
    "flywheel" to R.string.catalog_flywheel,
    "trans-pan-gasket" to R.string.catalog_trans_pan_gasket,
    "shift-cable" to R.string.catalog_shift_cable,
    "shocks" to R.string.catalog_shocks,
    "springs" to R.string.catalog_springs,
    "control-arms" to R.string.catalog_control_arms,
    "ball-joints" to R.string.catalog_ball_joints,
    "bushings" to R.string.catalog_bushings,
    "sway-bar-links" to R.string.catalog_sway_bar_links,
    "wheel-bearing" to R.string.catalog_wheel_bearing,
    "tie-rods" to R.string.catalog_tie_rods,
    "steering-rack" to R.string.catalog_steering_rack,
    "power-steering-pump" to R.string.catalog_power_steering_pump,
    "steering-parts" to R.string.catalog_steering_parts,
    "suspension-check" to R.string.catalog_suspension_check,
    "cabin-air" to R.string.catalog_cabin_air,
    "wipers" to R.string.catalog_wipers,
    "wipers-check" to R.string.catalog_wipers_check,
    "battery" to R.string.catalog_battery,
    "battery-check" to R.string.catalog_battery_check,
    "key-fob-battery" to R.string.catalog_key_fob_battery,
    "bulbs" to R.string.catalog_bulbs,
    "headlight-assembly" to R.string.catalog_headlight_assembly,
    "lights-check" to R.string.catalog_lights_check,
    "windows-check" to R.string.catalog_windows_check,
    "window-regulator" to R.string.catalog_window_regulator,
    "door-lock-actuator" to R.string.catalog_door_lock_actuator,
    "air-conditioning" to R.string.catalog_air_conditioning,
    "ac-flush" to R.string.catalog_ac_flush,
    "ac-compressor" to R.string.catalog_ac_compressor,
    "ac-condenser" to R.string.catalog_ac_condenser,
    "blower-motor" to R.string.catalog_blower_motor,
    "starter" to R.string.catalog_starter,
    "alternator" to R.string.catalog_alternator,
    "fuses" to R.string.catalog_fuses,
    "horn" to R.string.catalog_horn,
    "multi-point" to R.string.catalog_multi_point,
    "scheduled-service" to R.string.catalog_scheduled_service,
    "emissions" to R.string.catalog_emissions,
    "safety-inspection" to R.string.catalog_safety_inspection,
    "recall" to R.string.catalog_recall,
    "accessory" to R.string.catalog_accessory,
    "windshield" to R.string.catalog_windshield,
    "mirror" to R.string.catalog_mirror,
    "door-handle" to R.string.catalog_door_handle,
    "body" to R.string.catalog_body,
    "detail" to R.string.catalog_detail,
)

internal val catalogCategories: Map<String, Int> = mapOf(
    "engine" to R.string.catalog_category_engine,
    "tires" to R.string.catalog_category_tires,
    "brakes" to R.string.catalog_category_brakes,
    "fluids" to R.string.catalog_category_fluids,
    "cooling" to R.string.catalog_category_cooling,
    "fuel" to R.string.catalog_category_fuel,
    "exhaust" to R.string.catalog_category_exhaust,
    "driveline" to R.string.catalog_category_driveline,
    "suspension" to R.string.catalog_category_suspension,
    "cabin" to R.string.catalog_category_cabin,
    "inspection" to R.string.catalog_category_inspection,
    "other" to R.string.catalog_category_other,
    ServiceCatalog.YOUR_ITEMS to R.string.catalog_category_yours,
)

/** "Oil & filter change"; the id itself for one this version doesn't know. */
fun catalogLabel(id: String): String = catalogLines[id]?.let(AppText.res::getString) ?: id

/** "Oil & engine" */
fun catalogCategoryLabel(id: String): String = catalogCategories[id]?.let(AppText.res::getString) ?: id

/** A catalog line by its name, one of the user's own items by theirs. */
val WorkOption.label: String get() = name ?: catalogLabel(id)

val WorkCategory.label: String get() = catalogCategoryLabel(id)

val CatalogItem.label: String get() = catalogLabel(id)

val CatalogCategory.label: String get() = catalogCategoryLabel(id)
