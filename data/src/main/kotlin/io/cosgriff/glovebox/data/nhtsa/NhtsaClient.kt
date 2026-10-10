package io.cosgriff.glovebox.data.nhtsa

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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

data class Recall(
    val campaign: String,
    val component: String,
    val summary: String,
    val consequence: String?,
    val remedy: String?,
    val reportedOn: String?,
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

    /** Recalls by year/make/model. NHTSA's public API can't confirm whether a specific VIN was repaired. */
    suspend fun recalls(year: Int, make: String, model: String): List<Recall> {
        val body: JsonObject = http.get("$RECALLS/recalls/recallsByVehicle") {
            parameter("make", make)
            parameter("model", model)
            parameter("modelYear", year)
        }.body()
        return body["results"]?.jsonArray.orEmpty().map { el ->
            val o = el.jsonObject
            fun f(key: String) = o[key]?.jsonPrimitive?.contentOrNull
            Recall(
                campaign = f("NHTSACampaignNumber").orEmpty(),
                component = f("Component").orEmpty(),
                summary = f("Summary").orEmpty(),
                consequence = f("Consequence"),
                remedy = f("Remedy"),
                reportedOn = f("ReportReceivedDate"),
            )
        }
    }

    private companion object {
        const val VPIC = "https://vpic.nhtsa.dot.gov/api"
        const val RECALLS = "https://api.nhtsa.gov"
    }
}
