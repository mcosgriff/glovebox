package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.testVehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DocumentDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val documents = db.documents()
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    private fun document(vehicleId: Long, name: String) =
        DocumentEntity(vehicleId = vehicleId, type = "OTHER", name = name, fileName = "document-$name.pdf", format = "PDF", sizeBytes = 10, addedOn = day)

    @Test
    fun `a vehicle's documents are listed, described and deleted`() = runBlocking {
        val v = db.vehicles().add(testVehicle(), firstReading = null)
        val other = db.vehicles().add(testVehicle(model = "GTI"), firstReading = null)
        val id = documents.insert(document(v, "a"))
        documents.insert(document(other, "b"))

        assertEquals(listOf("a"), documents.observe(v).first().map { it.name })
        documents.describe(id, "REGISTRATION", "Registration", day.minusDays(400), day.plusDays(20))
        val described = documents.observeOne(id).first()!!
        assertEquals(Triple("REGISTRATION", "Registration", day.minusDays(400)), Triple(described.type, described.name, described.date))
        assertEquals(day.plusDays(20), described.expiresOn)
        // An expiry is cleared as it is given.
        documents.describe(id, "REGISTRATION", "Registration", null, null)
        assertNull(documents.observeOne(id).first()!!.expiresOn)
        assertEquals(setOf("document-a.pdf", "document-b.pdf"), documents.allFiles().toSet())

        documents.delete(id)
        assertNull(documents.observeOne(id).first())
        assertEquals(emptyList<DocumentEntity>(), documents.documentsFor(v))
    }
}
