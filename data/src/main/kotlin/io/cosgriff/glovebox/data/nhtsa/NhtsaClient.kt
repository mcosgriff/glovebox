package io.cosgriff.glovebox.data.nhtsa

import io.cosgriff.glovebox.domain.RecallNotice
import io.cosgriff.glovebox.domain.Recalls
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/** What we keep from a vPIC decode. Blank vPIC fields become null. */
data class DecodedVehicle(
    val vin: String,
    val year: Int?,
    val make: String?,
    val model: String?,
    val trim: String?,
    val bodyClass: String?,
    val driveType: String?,
    val displacementL: String?,
    val cylinders: String?,
    val engineModel: String?,
    val horsepower: String?,
    val fuel: String?,
    /** vPIC's `FuelTypeSecondary` ("Electric", "Ethanol (E85)") and `ElectrificationLevel`, read for [powertrain] only. */
    val fuelSecondary: String? = null,
    val electrification: String? = null,
    val transmission: String?,
    val gvwr: String?,
    val plant: String?,
    /** Tire pressure monitoring: "Direct" or "Indirect". */
    val tpms: String? = null,
    /** The maker's chassis code ("GRN280L/GRN285L"), as service manuals and parts catalogues name the vehicle. */
    val series: String? = null,
    /** vPIC "0" means a clean decode; anything else is a partial decode or a warning. */
    val errorCode: String?,
    val errorText: String?,
)

/**
 * The only network calls Glovebox makes. Only the VIN (or year/make/model) leaves the phone.
 */
class NhtsaClient(private val http: HttpClient) {

    /**
     * The VIN alone. vPIC suggests sending `modelyear` too, but it answers with whatever year it is sent, so a year
     * would hide a VIN that belongs to another vehicle (#462).
     */
    suspend fun decodeVin(vin: String): DecodedVehicle {
        val body: JsonObject = http.get("$VPIC/vehicles/DecodeVinValues/$vin") {
            parameter("format", "json")
        }.body()
        val r = body["Results"]!!.jsonArray.first().jsonObject
        val speeds = r.text("TransmissionSpeeds")
        val style = r.text("TransmissionStyle")
        return DecodedVehicle(
            vin = vin,
            year = r.text("ModelYear")?.toIntOrNull(),
            make = r.text("Make"),
            model = r.text("Model"),
            trim = r.text("Trim"),
            bodyClass = r.text("BodyClass"),
            driveType = r.text("DriveType"),
            // vPIC reports e.g. "2.998832712"; show it the way people say it ("3.0").
            displacementL = r.text("DisplacementL")?.let { raw ->
                raw.toDoubleOrNull()?.let { String.format(Locale.US, "%.1f", it) } ?: raw
            },
            cylinders = r.text("EngineCylinders"),
            engineModel = r.text("EngineModel"),
            horsepower = r.text("EngineHP"),
            fuel = r.text("FuelTypePrimary"),
            fuelSecondary = r.text("FuelTypeSecondary"),
            electrification = r.text("ElectrificationLevel"),
            transmission = listOfNotNull(speeds?.let { "$it-speed" }, style).joinToString(" ").ifEmpty { null },
            gvwr = r.text("GVWR"),
            plant = listOfNotNull(r.text("PlantCity"), r.text("PlantCountry")).joinToString(", ").ifEmpty { null },
            tpms = r.text("TPMS"),
            series = r.text("Series"),
            errorCode = r.text("ErrorCode"),
            errorText = r.text("ErrorText"),
        )
    }

    /**
     * Makes of cars, trucks and SUVs, for picking one by hand. Nothing about the user's vehicle is
     * sent. Motorcycle and trailer makes run to thousands and are left to free text.
     */
    suspend fun makes(): List<String> = coroutineScope {
        listOf("car", "truck", "mpv")
            .map { type -> async { names("$VPIC/vehicles/GetMakesForVehicleType/$type", "MakeName") } }
            .awaitAll()
            .flatten()
    }

    /** Models NHTSA lists for a make in a model year; empty for years or makes it has nothing on. Only those two are sent. */
    suspend fun models(make: String, year: Int): List<String> =
        names("$VPIC/vehicles/GetModelsForMakeYear/make/${make.encodeURLPathPart()}/modelyear/$year", "Model_Name")

    private suspend fun names(url: String, key: String): List<String> {
        val body: JsonObject = http.get(url) { parameter("format", "json") }.body()
        return body["Results"]?.jsonArray.orEmpty().mapNotNull { it.jsonObject[key]?.jsonPrimitive?.contentOrNull }
    }

    /**
     * The model names NHTSA files the make's recalls under for that year (#460), which can differ from vPIC's
     * ("F-250 SD" for vPIC's "F-250"). Only the year and make are sent. Throws if NHTSA can't be reached.
     */
    suspend fun recallModels(year: Int, make: String): List<String> {
        val body: JsonObject = http.get("$RECALLS/products/vehicle/models") {
            parameter("modelYear", year)
            parameter("make", make)
            parameter("issueType", "r")
        }.body()
        return body["results"]?.jsonArray.orEmpty().mapNotNull { it.jsonObject.text("model") }.distinct()
    }

    /**
     * Recalls by year/make/model. NHTSA's public API can't confirm whether a specific VIN was repaired.
     * Throws if NHTSA can't be reached or answers with anything but a list.
     */
    suspend fun recalls(year: Int, make: String, model: String): List<RecallNotice> {
        val response = http.get("$RECALLS/recalls/recallsByVehicle") {
            // NHTSA answers 400 with an empty list when it has nothing for the vehicle, so the
            // status alone can't be trusted; the body has to be a list either way.
            expectSuccess = false
            parameter("make", make)
            parameter("model", model)
            parameter("modelYear", year)
        }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.BadRequest) {
            throw ResponseException(response, "NHTSA recalls: ${response.status}")
        }
        val results = response.body<JsonObject>()["results"]?.jsonArray
            ?: throw ResponseException(response, "NHTSA recalls: no results list")
        return results.map { el ->
            val o = el.jsonObject
            RecallNotice(
                campaign = o.text("NHTSACampaignNumber").orEmpty(),
                component = o.text("Component").orEmpty(),
                summary = o.text("Summary").orEmpty(),
                consequence = o.text("Consequence"),
                remedy = o.text("Remedy"),
                reportedOn = Recalls.parseReportDate(o.text("ReportReceivedDate")),
                // Not in NHTSA's documentation, only in its answers (#461). A flag that is missing or isn't
                // plainly true counts as false; nothing ever says a vehicle is safe to drive.
                doNotDrive = o.flag("parkIt"),
                parkOutside = o.flag("parkOutSide"),
                overTheAir = o.flag("overTheAirUpdate"),
            )
        }
    }

    private fun JsonObject.flag(key: String) = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull == true

    /** A field's text, or null when NHTSA left it out or blank. */
    private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val VPIC = "https://vpic.nhtsa.dot.gov/api"
        const val RECALLS = "https://api.nhtsa.gov"
    }
}
