package io.cosgriff.glovebox.data.nhtsa

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * NHTSA answered from memory: [answer] is asked for each request (and may throw, as a phone offline does),
 * and every request is kept in [requests]. The client is set up as DataModule sets up the real one.
 */
class FakeNhtsa {
    val requests = mutableListOf<HttpRequestData>()
    var answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { json("""{"results":[]}""") }

    val client = NhtsaClient(
        HttpClient(MockEngine { request -> requests += request; answer(request) }) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )
}

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

/** A recall as NHTSA's recallsByVehicle lists it; every figure made up. */
fun recallJson(
    campaign: String,
    summary: String = "Made-up summary for $campaign",
    component: String = "MADE-UP COMPONENT",
    parkIt: Boolean = false,
    parkOutSide: Boolean = false,
    overTheAirUpdate: Boolean = false,
) = """
    {"Manufacturer":"Made-up","NHTSACampaignNumber":"$campaign","parkIt":$parkIt,"parkOutSide":$parkOutSide,"overTheAirUpdate":$overTheAirUpdate,
     "ReportReceivedDate":"04/05/2026","Component":"$component","Summary":"$summary","Consequence":"Made-up consequence",
     "Remedy":"Made-up remedy","Notes":"","ModelYear":"2019","Make":"TOYOTA","Model":"4RUNNER"}
""".trimIndent()

fun recallsJson(vararg recalls: String) = """{"Count":${recalls.size},"Message":"Results returned successfully","results":[${recalls.joinToString(",")}]}"""
