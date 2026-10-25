package io.cosgriff.glovebox.ui.garage

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.room3.Room
import androidx.savedstate.serialization.encodeToSavedState
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.nhtsa.NhtsaClient
import io.cosgriff.glovebox.data.nhtsa.RecallRepository
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.TorqueUnit
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException
import java.util.Collections

// Shared by the tests of the garage, recalls, torque and scan screens. Everything here is made up: the
// VINs are the screenshot fixtures' (they pass the check digit and belong to no one), and NHTSA's answers
// are invented and never come from the network.

/** The app's database in memory, through Android's own driver as `:data`'s tests build it. */
fun testDatabase(context: Context = ApplicationProvider.getApplicationContext()): GloveboxDatabase =
    Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()

/**
 * Settings put back to what each test expects. The DataStore behind [SettingsRepository] lives as long as the
 * test JVM, so every test that reads a setting says what it is.
 */
fun testSettings(
    distance: DistanceUnit = DistanceUnit.MILES,
    torque: TorqueUnit = TorqueUnit.NEWTON_METER,
    cadence: RecallCadence = RecallCadence.WEEKLY,
    scannerDetails: Boolean = false,
): SettingsRepository = SettingsRepository(ApplicationProvider.getApplicationContext()).also {
    runBlocking {
        it.setDistanceUnit(distance)
        it.setTorqueUnit(torque)
        it.setRecallCadence(cadence)
        it.setScannerDetails(scannerDetails)
    }
}

/** What a ViewModel's `SavedStateHandle` holds when its screen was opened with [key] (as `ProvideRoute` puts it). */
fun routeHandle(key: NavKey): SavedStateHandle =
    SavedStateHandle(mapOf("io.cosgriff.glovebox.route" to encodeToSavedState(NavKeySerializer(), key)))

/** How long a test waits on the wall clock for other threads; generous, as the machine running them may be busy. */
const val TIMEOUT = 15_000L

/** The first value of this flow that [matches], waiting on the wall clock for Room's threads. */
suspend fun <T> Flow<T>.awaitMatching(matches: (T) -> Boolean): T =
    withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { first(matches) } }

/**
 * Runs ViewModels' `viewModelScope` without Robolectric's paused main looper, so a test blocked on a flow
 * doesn't hold up the coroutine it is waiting for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UnconfinedMainRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * NHTSA, answered in the test. Each answer is the JSON NHTSA would send, or null to fail as if offline.
 * Every URL asked for is kept in [asked], so a test can say what left the phone.
 */
class FakeNhtsa {
    /** vPIC's decode result fields, e.g. "ModelYear" to "2019". */
    var decode: Map<String, String>? = emptyMap()

    /** Makes of each vehicle type (car, truck, mpv), all the same here. */
    var makes: List<String>? = emptyList()

    /** Models for any make and year. */
    var models: List<String>? = emptyList()

    /** The recall rows NHTSA lists; each row's fields as NHTSA names them. Null: NHTSA can't be reached. */
    var recalls: List<Map<String, String>>? = emptyList()

    /** The model names NHTSA files the make's recalls under, for any year. Null: NHTSA can't be reached. */
    var recallModels: List<String>? = emptyList()

    /** How long each answer takes, to catch a request made while another is still out. */
    var slowness: Long = 0

    val asked: MutableList<Url> = Collections.synchronizedList(mutableListOf())

    val client: NhtsaClient = NhtsaClient(HttpClient(Engine()) { install(JsonBodies) })

    fun repository(db: GloveboxDatabase, settings: SettingsRepository) = RecallRepository(client, db.vehicles(), db.recalls(), settings)

    private fun answer(url: Url): Pair<HttpStatusCode, String> {
        val path = url.encodedPath
        val body: String? = when {
            "DecodeVinValues" in path -> decode?.let { results(listOf(it)) }
            "GetMakesForVehicleType" in path -> makes?.let { m -> results(m.map { mapOf("MakeName" to it) }) }
            "GetModelsForMakeYear" in path -> models?.let { m -> results(m.map { mapOf("Model_Name" to it) }) }
            "recallsByVehicle" in path -> recalls?.let { r ->
                // NHTSA's way of saying "none": a 400 with an empty list.
                return (if (r.isEmpty()) HttpStatusCode.BadRequest else HttpStatusCode.OK) to json("results", r)
            }
            "products/vehicle/models" in path -> recallModels?.let { m -> json("results", m.map { mapOf("model" to it) }) }
            else -> error("Nothing else is ever asked of NHTSA: $url")
        }
        return (body ?: throw IOException("offline")).let { HttpStatusCode.OK to it }
    }

    private fun results(rows: List<Map<String, String>>) = json("Results", rows)

    private fun json(key: String, rows: List<Map<String, String>>): String =
        JsonObject(mapOf(key to Json.parseToJsonElement(Json.encodeToString(rows)))).toString()

    private inner class Engine : HttpClientEngineBase("fake-nhtsa") {
        override val config = HttpClientEngineConfig()

        @OptIn(InternalAPI::class)
        override suspend fun execute(data: HttpRequestData): HttpResponseData {
            asked += data.url
            kotlinx.coroutines.delay(slowness)
            val (status, body) = answer(data.url)
            return HttpResponseData(
                status,
                GMTDate(),
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                HttpProtocolVersion.HTTP_1_1,
                ByteReadChannel(body.encodeToByteArray()),
                callContext(),
            )
        }
    }

    private companion object {
        /** What the app's ContentNegotiation does for `body<JsonObject>()`, without it on the test classpath. */
        val JsonBodies = createClientPlugin("JsonBodies") {
            transformResponseBody { _, content, requested ->
                if (requested.type != JsonObject::class) null else Json.parseToJsonElement(content.toByteArray().decodeToString()).jsonObject
            }
        }
    }
}

/** A clean vPIC decode of the screenshot truck's made-up VIN, in vPIC's field names. Every value is invented. */
val truckDecode: Map<String, String> = mapOf(
    "ModelYear" to "2019",
    "Make" to "TOYOTA",
    "Model" to "4Runner",
    "Trim" to "SR5",
    "BodyClass" to "Sport Utility Vehicle (SUV)",
    "DriveType" to "4WD/4-Wheel Drive/4x4",
    "DisplacementL" to "3.956",
    "EngineCylinders" to "6",
    "EngineModel" to "1GR-XX",
    "EngineHP" to "270",
    "FuelTypePrimary" to "Gasoline",
    "TransmissionSpeeds" to "5",
    "TransmissionStyle" to "Automatic",
    "GVWR" to "Class 1D",
    "PlantCity" to "Testville",
    "PlantCountry" to "Nowhere",
    "TPMS" to "Direct",
    "Series" to "XYZ100L",
    "ErrorCode" to "0",
)

/** One recall row as NHTSA lists it; invented. */
fun recallRow(campaign: String, component: String = "FUEL SYSTEM") =
    mapOf("NHTSACampaignNumber" to campaign, "Component" to component, "Summary" to "A made-up summary.", "Remedy" to "A made-up remedy.")

/** Waits on the wall clock for work a ViewModel started on another thread (NHTSA's, Room's) to land. */
fun waitFor(what: String = "the condition", condition: () -> Boolean) {
    val until = System.currentTimeMillis() + TIMEOUT
    while (!condition()) {
        check(System.currentTimeMillis() < until) { "Timed out waiting for $what" }
        Thread.sleep(10)
    }
}

/**
 * Holds a test's ViewModels the way a screen's entry does, so [clear] stops their flows before the test closes the
 * database under them.
 */
class TestViewModels {
    private val store = ViewModelStore()
    private var made = 0

    fun <T : ViewModel> make(build: () -> T): T {
        val vm = build()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") // Only ever asked for the class of the one it holds.
            override fun <V : ViewModel> create(modelClass: Class<V>): V = vm as V
        }
        return ViewModelProvider(store, factory)["vm${made++}", vm.javaClass]
    }

    fun clear() = store.clear()
}
