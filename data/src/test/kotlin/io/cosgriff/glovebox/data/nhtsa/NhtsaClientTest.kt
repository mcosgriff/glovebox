package io.cosgriff.glovebox.data.nhtsa

import io.cosgriff.glovebox.domain.RecallNotice
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NhtsaClientTest {
    private val nhtsa = FakeNhtsa()

    /** Made up, with a check digit that works out; it belongs to no one. */
    private val vin = "1M8GDM9AXKP042788"

    private fun decodeAnswer(vararg fields: Pair<String, String>) =
        """{"Count":1,"Message":"Results returned successfully","Results":[{${fields.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" }}}]}"""

    @Test
    fun `a decode sends the VIN and nothing else, to vPIC`() = runBlocking {
        nhtsa.answer = { json(decodeAnswer("ModelYear" to "2019", "Make" to "TOYOTA", "Model" to "4Runner")) }
        nhtsa.client.decodeVin(vin)

        val url = nhtsa.requests.single().url
        assertEquals("vpic.nhtsa.dot.gov", url.host)
        assertEquals("/api/vehicles/DecodeVinValues/$vin", url.encodedPath)
        assertEquals(listOf("format"), url.parameters.names().toList())
    }

    @Test
    fun `a decode keeps vPIC's values, with blanks as nothing and figures said the way people say them`() = runBlocking {
        nhtsa.answer = {
            json(
                decodeAnswer(
                    "ModelYear" to "2019", "Make" to "TOYOTA", "Model" to "4Runner", "Trim" to " SR5 ", "BodyClass" to "Sport Utility Vehicle (SUV)",
                    "DriveType" to "4WD/4-Wheel Drive/4x4", "DisplacementL" to "3.956", "EngineCylinders" to "6", "EngineModel" to "",
                    "EngineHP" to "270", "FuelTypePrimary" to "Gasoline", "FuelTypeSecondary" to "", "ElectrificationLevel" to "",
                    "TransmissionSpeeds" to "5", "TransmissionStyle" to "Automatic", "GVWR" to "Class 1D", "PlantCity" to "TAHARA",
                    "PlantCountry" to "JAPAN", "ErrorCode" to "0", "ErrorText" to "0 - VIN decoded clean.",
                ),
            )
        }

        val decoded = nhtsa.client.decodeVin(vin)

        assertEquals(
            DecodedVehicle(
                vin = vin, year = 2019, make = "TOYOTA", model = "4Runner", trim = "SR5", bodyClass = "Sport Utility Vehicle (SUV)",
                driveType = "4WD/4-Wheel Drive/4x4", displacementL = "4.0", cylinders = "6", engineModel = null, horsepower = "270",
                fuel = "Gasoline", fuelSecondary = null, electrification = null, transmission = "5-speed Automatic", gvwr = "Class 1D",
                plant = "TAHARA, JAPAN", errorCode = "0", errorText = "0 - VIN decoded clean.",
            ),
            decoded,
        )
    }

    @Test
    fun `a decode keeps the engine code, the tire pressure sensors and the chassis code`() = runBlocking {
        nhtsa.answer = { json(decodeAnswer("ModelYear" to "2019", "EngineModel" to "1GR-FE", "TPMS" to "Direct", "Series" to "GRN280L/GRN285L")) }

        val decoded = nhtsa.client.decodeVin(vin)

        assertEquals("1GR-FE", decoded.engineModel)
        assertEquals("Direct", decoded.tpms)
        assertEquals("GRN280L/GRN285L", decoded.series)
    }

    @Test
    fun `a partial decode leaves what vPIC didn't say empty`() = runBlocking {
        nhtsa.answer = { json(decodeAnswer("ModelYear" to "", "DisplacementL" to "about two", "TransmissionStyle" to "Manual/Standard", "PlantCountry" to "MEXICO", "ErrorCode" to "6")) }

        val decoded = nhtsa.client.decodeVin(vin)

        assertNull(decoded.year)
        assertNull(decoded.make)
        assertEquals("about two", decoded.displacementL)
        assertEquals("Manual/Standard", decoded.transmission)
        assertEquals("MEXICO", decoded.plant)
        assertEquals("6", decoded.errorCode)
    }

    @Test
    fun `a decode with no transmission or plant leaves them out`() = runBlocking {
        nhtsa.answer = { json(decodeAnswer("ModelYear" to "2013")) }
        val decoded = nhtsa.client.decodeVin(vin)
        assertNull(decoded.transmission)
        assertNull(decoded.plant)
        assertNull(decoded.displacementL)
    }

    @Test
    fun `makes come from the car, truck and multipurpose lists, sending nothing about the vehicle`() = runBlocking {
        nhtsa.answer = { request ->
            val type = request.url.encodedPath.substringAfterLast('/')
            json("""{"Results":[{"MakeId":1,"MakeName":"MADE-UP $type"},{"MakeId":2}]}""")
        }

        val makes = nhtsa.client.makes()

        assertEquals(setOf("MADE-UP car", "MADE-UP truck", "MADE-UP mpv"), makes.toSet())
        assertEquals(
            setOf("/api/vehicles/GetMakesForVehicleType/car", "/api/vehicles/GetMakesForVehicleType/truck", "/api/vehicles/GetMakesForVehicleType/mpv"),
            nhtsa.requests.map { it.url.encodedPath }.toSet(),
        )
        assertTrue(nhtsa.requests.all { it.url.parameters.names() == setOf("format") })
    }

    @Test
    fun `models send only the make and the year, the make made safe for a path`() = runBlocking {
        nhtsa.answer = { json("""{"Results":[{"Model_Name":"Defender"},{"Model_Name":"Discovery"}]}""") }

        assertEquals(listOf("Defender", "Discovery"), nhtsa.client.models("Land Rover", 2021))
        assertEquals("/api/vehicles/GetModelsForMakeYear/make/Land%20Rover/modelyear/2021", nhtsa.requests.single().url.encodedPath)
    }

    @Test
    fun `models for a make NHTSA knows nothing about are none`() = runBlocking {
        nhtsa.answer = { json("""{"Count":0}""") }
        assertEquals(emptyList<String>(), nhtsa.client.models("Made-up", 1990))
    }

    @Test
    fun `the model names NHTSA files recalls under are asked for by year and make only, each once`() = runBlocking {
        nhtsa.answer = {
            json(
                """{"count":4,"message":"Results returned successfully","results":[
                {"modelYear":"2019","make":"FORD","model":"F-250 SD"},{"modelYear":"2019","make":"FORD","model":"F-250 SD"},
                {"modelYear":"2019","make":"FORD","model":" TRANSIT VAN "},{"modelYear":"2019","make":"FORD"}]}""",
            )
        }

        assertEquals(listOf("F-250 SD", "TRANSIT VAN"), nhtsa.client.recallModels(2019, "Ford"))

        val url = nhtsa.requests.single().url
        assertEquals("api.nhtsa.gov", url.host)
        assertEquals("/products/vehicle/models", url.encodedPath)
        assertEquals(setOf("modelYear", "make", "issueType"), url.parameters.names())
        assertEquals("2019", url.parameters["modelYear"])
        assertEquals("Ford", url.parameters["make"])
        assertEquals("r", url.parameters["issueType"])
    }

    @Test
    fun `recalls are asked for by year, make and model, and read into notices`() = runBlocking {
        nhtsa.answer = { json(recallsJson(recallJson("99V000001", summary = " Made-up summary "))) }

        val recalls = nhtsa.client.recalls(2019, "Toyota", "4Runner")

        val url = nhtsa.requests.single().url
        assertEquals("api.nhtsa.gov", url.host)
        assertEquals("/recalls/recallsByVehicle", url.encodedPath)
        assertEquals(mapOf("make" to "Toyota", "model" to "4Runner", "modelYear" to "2019"), url.parameters.names().associateWith { url.parameters[it] })
        assertEquals(
            listOf(RecallNotice("99V000001", "MADE-UP COMPONENT", "Made-up summary", "Made-up consequence", "Made-up remedy", LocalDate.of(2026, 5, 4))),
            recalls,
        )
    }

    @Test
    fun `NHTSA's 400 with an empty list means no recalls, not a failure`() = runBlocking {
        nhtsa.answer = { json("""{"Count":0,"Message":"Results returned successfully","results":[]}""", HttpStatusCode.BadRequest) }
        assertEquals(emptyList<RecallNotice>(), nhtsa.client.recalls(2019, "Toyota", "4Runner"))
    }

    @Test
    fun `a 400 without a list is a failure`() {
        nhtsa.answer = { json("""{"message":"bad request"}""", HttpStatusCode.BadRequest) }
        assertThrows(ResponseException::class.java) { runBlocking { nhtsa.client.recalls(2019, "Toyota", "4Runner") } }
    }

    @Test
    fun `any other error is a failure, whatever the body says`() {
        nhtsa.answer = { json("""{"results":[]}""", HttpStatusCode.ServiceUnavailable) }
        assertThrows(ResponseException::class.java) { runBlocking { nhtsa.client.recalls(2019, "Toyota", "4Runner") } }
    }

    @Test
    fun `a recall NHTSA leaves fields out of is still read`() = runBlocking {
        nhtsa.answer = { json("""{"results":[{"NHTSACampaignNumber":"99V000002"}]}""") }
        assertEquals(listOf(RecallNotice("99V000002", "", "", null, null, null)), nhtsa.client.recalls(2019, "Toyota", "4Runner"))
    }
}
