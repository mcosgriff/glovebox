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
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
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
    val transmission: String?,
    val gvwr: String?,
    val plant: String?,
    /** vPIC "0" means a clean decode; anything else is a partial decode or a warning. */
    val errorCode: String?,
    val errorText: String?,
)

/**
 * The only network calls Glovebox makes. Only the VIN (or year/make/model) leaves the phone.
 */
class NhtsaClient(private val http: HttpClient) {

    suspend fun decodeVin(vin: String): DecodedVehicle {
        val body: JsonObject = http.get("$VPIC/vehicles/DecodeVinValues/$vin") {
            parameter("format", "json")
        }.body()
        val r = body["Results"]!!.jsonArray.first().jsonObject
        fun f(key: String) = r[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

        val speeds = f("TransmissionSpeeds")
        val style = f("TransmissionStyle")
        return DecodedVehicle(
            vin = vin,
            year = f("ModelYear")?.toIntOrNull(),
            make = f("Make"),
            model = f("Model"),
            trim = f("Trim"),
            bodyClass = f("BodyClass"),
            driveType = f("DriveType"),
            // vPIC reports e.g. "2.998832712"; show it the way people say it ("3.0").
            displacementL = f("DisplacementL")?.let { raw ->
                raw.toDoubleOrNull()?.let { String.format(Locale.US, "%.1f", it) } ?: raw
            },
            cylinders = f("EngineCylinders"),
            engineModel = f("EngineModel"),
            horsepower = f("EngineHP"),
            fuel = f("FuelTypePrimary"),
            transmission = listOfNotNull(speeds?.let { "$it-speed" }, style).joinToString(" ").ifEmpty { null },
            gvwr = f("GVWR"),
            plant = listOfNotNull(f("PlantCity"), f("PlantCountry")).joinToString(", ").ifEmpty { null },
            errorCode = f("ErrorCode"),
            errorText = f("ErrorText"),
        )
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
            fun f(key: String) = o[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
            RecallNotice(
                campaign = f("NHTSACampaignNumber").orEmpty(),
                component = f("Component").orEmpty(),
                summary = f("Summary").orEmpty(),
                consequence = f("Consequence"),
                remedy = f("Remedy"),
                reportedOn = Recalls.parseReportDate(f("ReportReceivedDate")),
            )
        }
    }

    private companion object {
        const val VPIC = "https://vpic.nhtsa.dot.gov/api"
        const val RECALLS = "https://api.nhtsa.gov"
    }
}
