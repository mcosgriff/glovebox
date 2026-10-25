package io.cosgriff.glovebox.data.nhtsa

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.domain.RecallNotice
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecallRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 5, 4)
    private val nhtsa = FakeNhtsa()
    private lateinit var db: GloveboxDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var repository: RecallRepository

    @Before fun setUp() {
        resetDataStores()
        db = inMemoryDatabase(context)
        settings = SettingsRepository(context)
        repository = RecallRepository(nhtsa.client, db.vehicles(), db.recalls(), settings)
    }

    @After fun close() = db.close()

    private fun answer(vararg recalls: String) {
        nhtsa.answer = { json(recallsJson(*recalls)) }
    }

    private suspend fun vehicle(checkedOn: LocalDate? = null) = db.vehicles().add(testVehicle(), firstReading = null).also { id ->
        checkedOn?.let { db.vehicles().setRecallsChecked(id, it) }
    }

    /** NHTSA's answer for each model name; a name missing from [byModel] can't be reached. */
    private fun answerByModel(byModel: Map<String, List<String>>) {
        nhtsa.answer = { request ->
            val recalls = byModel[request.url.parameters["model"]] ?: throw IOException("offline")
            json(recallsJson(*recalls.toTypedArray()))
        }
    }

    @Test
    fun `recalls are checked under every name the owner ticked, as well as the model, and kept together`() = runBlocking {
        val id = db.vehicles().add(testVehicle(make = "Ford", model = "F-250"), firstReading = null)
        db.recalls().setNames(id, listOf("F-250 SD"))
        answerByModel(mapOf("F-250" to listOf(recallJson("99V000001")), "F-250 SD" to listOf(recallJson("99V000002"), recallJson("99V000001"))))

        repository.refresh(id, today)

        assertEquals(listOf("99V000001", "99V000002"), db.recalls().recallsFor(id).map { it.campaign }.sorted())
        assertEquals(listOf("F-250", "F-250 SD"), nhtsa.requests.map { it.url.parameters["model"] })
        // Still only the year, make and model, for each name.
        assertTrue(nhtsa.requests.all { it.url.parameters.names() == setOf("make", "model", "modelYear") })
    }

    @Test
    fun `when one name can't be checked the stored list is left alone, so nothing filed under it is lost`() = runBlocking {
        val id = db.vehicles().add(testVehicle(make = "Ford", model = "F-250"), firstReading = null)
        db.recalls().setNames(id, listOf("F-250 SD"))
        answerByModel(mapOf("F-250" to listOf(recallJson("99V000001")), "F-250 SD" to listOf(recallJson("99V000002"))))
        repository.refresh(id, today)

        answerByModel(mapOf("F-250" to listOf(recallJson("99V000001"))))
        val result = repository.refresh(id, today.plusDays(8))

        assertEquals(RecallRefresh.Failed, result)
        assertEquals(listOf("99V000001", "99V000002"), db.recalls().recallsFor(id).map { it.campaign }.sorted())
        assertEquals(today, db.vehicles().byId(id)!!.recallsCheckedOn)
    }

    @Test
    fun `unticking a name drops its open recalls at the next check, and keeps the ones marked done`() = runBlocking {
        val id = db.vehicles().add(testVehicle(make = "Ford", model = "F-250"), firstReading = null)
        db.recalls().setNames(id, listOf("F-250 SD"))
        answerByModel(mapOf("F-250" to emptyList(), "F-250 SD" to listOf(recallJson("99V000002"), recallJson("99V000003"))))
        repository.refresh(id, today)
        val done = db.recalls().recallsFor(id).single { it.campaign == "99V000003" }
        db.recalls().setDone(done.id, today, null)

        db.recalls().setNames(id, emptyList())
        repository.refresh(id, today)

        assertEquals(listOf("99V000003"), db.recalls().recallsFor(id).map { it.campaign })
        assertEquals(emptyList<String>(), db.recalls().namesFor(id))
    }

    @Test
    fun `a check stores what NHTSA lists and when it answered`() = runBlocking {
        val id = vehicle()
        answer(recallJson("99V000001"), recallJson("99V000002"))

        val result = repository.refresh(id, today)

        assertEquals(listOf("99V000001", "99V000002"), (result as RecallRefresh.Checked).added.map { it.campaign })
        assertEquals(listOf("99V000001", "99V000002"), db.recalls().recallsFor(id).map { it.campaign }.sorted())
        assertTrue(db.recalls().recallsFor(id).all { it.firstSeenOn == today })
        assertEquals(today, db.vehicles().byId(id)!!.recallsCheckedOn)
        // Only the year, make and model went.
        assertEquals(setOf("make", "model", "modelYear"), nhtsa.requests.single().url.parameters.names())
    }

    @Test
    fun `NHTSA's 400 with an empty list is an answer, no recalls, and the vehicle counts as checked`() = runBlocking {
        val id = vehicle()
        nhtsa.answer = { json("""{"Count":0,"results":[]}""", HttpStatusCode.BadRequest) }

        assertEquals(RecallRefresh.Checked(emptyList()), repository.refresh(id, today))
        assertEquals(today, db.vehicles().byId(id)!!.recallsCheckedOn)
    }

    @Test
    fun `a failed check leaves the stored list and the last check alone`() = runBlocking {
        val id = vehicle(checkedOn = today.minusDays(10))
        answer(recallJson("99V000001"))
        repository.refresh(id, today.minusDays(10))
        val before = db.recalls().recallsFor(id)

        nhtsa.answer = { json("""{"results":[]}""", HttpStatusCode.InternalServerError) }
        assertEquals(RecallRefresh.Failed, repository.refresh(id, today))
        nhtsa.answer = { throw IOException("offline") }
        assertEquals(RecallRefresh.Failed, repository.refresh(id, today))

        assertEquals(before, db.recalls().recallsFor(id))
        assertEquals(today.minusDays(10), db.vehicles().byId(id)!!.recallsCheckedOn)
    }

    @Test
    fun `the user's done mark survives a refresh that rewrites the recall`() = runBlocking {
        val id = vehicle()
        answer(recallJson("99V000001", summary = "First wording"))
        repository.refresh(id, today.minusDays(30))
        val recall = db.recalls().recallsFor(id).single()
        db.recalls().setDone(recall.id, today.minusDays(20), null)

        answer(recallJson("99V000001", summary = "Second wording"))
        assertEquals(RecallRefresh.Checked(emptyList()), repository.refresh(id, today))

        val after = db.recalls().recallsFor(id).single()
        assertEquals("Second wording", after.summary)
        assertEquals(today.minusDays(20), after.completedOn)
        assertEquals(today.minusDays(30), after.firstSeenOn)
    }

    @Test
    fun `a campaign NHTSA drops goes only while it is open`() = runBlocking {
        val id = vehicle()
        answer(recallJson("99V000001"), recallJson("99V000002"))
        repository.refresh(id, today.minusDays(30))
        val done = db.recalls().recallsFor(id).single { it.campaign == "99V000002" }
        db.recalls().setDone(done.id, today.minusDays(20), null)

        answer()
        repository.refresh(id, today)

        assertEquals(listOf("99V000002"), db.recalls().recallsFor(id).map { it.campaign })
    }

    @Test
    fun `a vehicle that can't be asked about is never sent`() = runBlocking {
        val sold = vehicle().also { db.vehicles().setArchived(it, today.minusDays(1)) }
        val unknown = db.vehicles().add(testVehicle(make = "Unknown"), firstReading = null)

        assertEquals(RecallRefresh.CannotCheck, repository.refresh(sold, today))
        assertEquals(RecallRefresh.CannotCheck, repository.refresh(unknown, today))
        assertEquals(RecallRefresh.CannotCheck, repository.refresh(999, today))
        assertTrue(nhtsa.requests.isEmpty())
    }

    @Test
    fun `a list checked recently enough isn't asked about again`() = runBlocking {
        settings.setRecallCadence(RecallCadence.WEEKLY)
        val fresh = vehicle(checkedOn = today.minusDays(6))
        val stale = vehicle(checkedOn = today.minusDays(7))
        answer(recallJson("99V000001"))

        assertNull(repository.refreshIfStale(fresh, today))
        assertEquals(1, (repository.refreshIfStale(stale, today) as RecallRefresh.Checked).added.size)
        assertEquals(1, nhtsa.requests.size)
    }

    @Test
    fun `with checks set to Never, nothing is asked unless the user refreshes`() = runBlocking {
        settings.setRecallCadence(RecallCadence.NEVER)
        val id = vehicle()

        assertNull(repository.refreshIfStale(id, today))
        assertTrue(nhtsa.requests.isEmpty())
        assertTrue(repository.refresh(id, today) is RecallRefresh.Checked)
    }

    @Test
    fun `the background check covers the garage but never a sold vehicle`() = runBlocking {
        settings.setRecallCadence(RecallCadence.WEEKLY)
        val a = vehicle()
        val b = db.vehicles().add(testVehicle(make = "Volkswagen", model = "GTI"), firstReading = null)
        val sold = db.vehicles().add(testVehicle(make = "Honda", model = "Civic"), firstReading = null).also { db.vehicles().setArchived(it, today) }
        answer(recallJson("99V000001"))

        repository.refreshStale(today)

        assertEquals(setOf("4Runner", "GTI"), nhtsa.requests.map { it.url.parameters["model"] }.toSet())
        assertEquals(listOf(today, today, null), listOf(a, b, sold).map { db.vehicles().byId(it)!!.recallsCheckedOn })
    }

    @Test
    fun `a list fetched while adding the vehicle is stored as a check`() = runBlocking {
        val id = vehicle()
        val fetched = listOf(RecallNotice("99V000001", "MADE-UP", "Made-up", null, null, null))

        assertEquals(1, repository.store(id, fetched, today))
        assertEquals(0, repository.store(id, fetched, today))
        assertEquals(listOf("99V000001"), db.recalls().recallsFor(id).map { it.campaign })
        assertEquals(today, db.vehicles().byId(id)!!.recallsCheckedOn)
    }

    @Test
    fun `a stored recall reads back as the notice and marks it was saved with`() = runBlocking {
        val id = vehicle()
        answer(recallJson("99V000001"))
        repository.refresh(id, today)
        val entity = db.recalls().recallsFor(id).single()
        db.recalls().setArchived(entity.id, today)

        val stored = db.recalls().byId(entity.id)!!.toStored()
        assertEquals(entity.id, stored.id)
        assertEquals("99V000001", stored.notice.campaign)
        assertEquals(false, stored.done)
        assertEquals(true, stored.archived)
    }
}
