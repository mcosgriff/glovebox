package io.cosgriff.glovebox.data.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.db.DocumentEntity
import io.cosgriff.glovebox.data.db.GloveboxDatabase
import io.cosgriff.glovebox.data.db.PhotoEntity
import io.cosgriff.glovebox.data.db.RecallEntity
import io.cosgriff.glovebox.data.db.ScheduleItemEntity
import io.cosgriff.glovebox.data.db.ServiceRecordEntity
import io.cosgriff.glovebox.data.db.ServiceRecordItemEntity
import io.cosgriff.glovebox.data.db.VehicleEntity
import io.cosgriff.glovebox.data.db.toRows
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.data.log.VehicleLogs
import io.cosgriff.glovebox.data.resetDataStores
import io.cosgriff.glovebox.data.settings.SettingsRepository
import io.cosgriff.glovebox.data.testVehicle
import io.cosgriff.glovebox.domain.DistanceUnit
import io.cosgriff.glovebox.domain.DocumentType
import io.cosgriff.glovebox.domain.ExportPartRow
import io.cosgriff.glovebox.domain.InvoiceCharges
import io.cosgriff.glovebox.domain.RecordJob
import io.cosgriff.glovebox.domain.RecordLine
import io.cosgriff.glovebox.domain.RecordPartRow
import io.cosgriff.glovebox.domain.ServiceGroup
import io.cosgriff.glovebox.domain.ServiceRecord
import io.cosgriff.glovebox.domain.TrackedItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** An export put together from a seeded vehicle log, its recalls and its documents (#579). Everything is made up. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExportBuilderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 6, 14)
    private lateinit var db: GloveboxDatabase
    private lateinit var builder: ExportBuilder

    /** Words that show where each came from. */
    private val names = object : ExportNames {
        override val unknownVehicle = "UNKNOWN"
        override val diy = "OWNER"
        override val aShop = "A SHOP"
        override fun whatItTakes(vehicle: VehicleEntity) = listOfNotNull(vehicle.tireSize?.let { "TIRES" to it })
        override fun documentType(type: DocumentType) = "TYPE ${type.name}"
        override fun recallComponent(component: String) = "PART $component"
        override fun every(item: TrackedItem, unit: DistanceUnit) = "EVERY ${item.interval.miles} ${unit.name}"
    }

    @Before fun open() {
        resetDataStores()
        db = inMemoryDatabase(context)
        val logs = VehicleLogs(db.vehicles(), db.schedule(), db.service(), db.odometer(), SettingsRepository(context))
        builder = ExportBuilder(db.vehicles(), logs, db.recalls(), db.documents())
    }

    @After fun close() {
        db.close()
        resetDataStores()
    }

    @Test
    fun `a vehicle's export comes from its log, recalls and documents, each record read through the one summary`() = runBlocking {
        val car = db.vehicles().insert(testVehicle(vin = "JTEBU5JR7K5900001").copy(nickname = "Blue", tireSize = "265/70R17", plate = "ABC 1234", plateRegion = "CO"))
        db.schedule().insertAll(
            listOf(
                ScheduleItemEntity(vehicleId = car, name = "Oil change", templateKey = "oil", intervalMiles = 5_000, intervalMonths = 6),
                // The owner's own item, filed under the catalog's shocks.
                ScheduleItemEntity(vehicleId = car, name = "Hitch service", service = "shocks", intervalMiles = 10_000, intervalMonths = null),
            ),
        )
        val items = db.schedule().allFor(car)
        val oil = items.single { it.templateKey == "oil" }.id
        val own = items.single { it.service == "shocks" }.id
        val rows = ServiceRecord(
            vehicleId = car, date = day, odometer = 41_000, diy = false, shopName = "Corner Garage", costCents = 15_000, notes = "Made-up note",
            lines = listOf(
                RecordLine("Oil & filter change", catalogId = "oil", scheduleItemId = oil, job = 0),
                // As Log service saves a line for the owner's own item: no catalog line.
                RecordLine("Hitch service", catalogId = null, scheduleItemId = own, job = 0),
                RecordLine("Air freshener"),
            ),
            jobs = listOf(RecordJob("Oil and hitch", laborCents = 6_000, rows = listOf(RecordPartRow("Oil filter", 1_000, 1_500, 1_500, line = 0)))),
            charges = InvoiceCharges(taxCents = 600),
        ).toRows()
        val record = db.service().insertWithItems(rows.record, rows.items, rows.jobs, rows.parts)
        db.service().insertPhotos(listOf(PhotoEntity(recordId = record, fileName = "receipt.enc")))
        db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = car, date = day.minusYears(1), odometer = 30_000, diy = true),
            listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = oil, label = "Oil change", catalogId = "oil")),
        )
        db.recalls().insertAll(listOf(RecallEntity(vehicleId = car, campaign = "99V999000", component = "MADE UP", summary = "Invented.", firstSeenOn = day, completedOn = day)))
        db.documents().insert(DocumentEntity(vehicleId = car, type = "TITLE", name = "Title", fileName = "doc.enc", format = "PDF", sizeBytes = 10, addedOn = day))

        val (vehicle, exported) = builder.all(names, day).single()
        assertEquals(car, vehicle.id)
        assertEquals("2019 Toyota 4Runner", exported.title)
        assertEquals("Blue", exported.nickname)
        assertEquals(41_000, exported.odometer)
        assertEquals(listOf("TIRES" to "265/70R17"), exported.whatItTakes)
        assertEquals(listOf("TYPE TITLE"), exported.documents.map { it.type })
        assertEquals(listOf("PART MADE UP" to day), exported.recalls.map { it.component to it.completedOn })
        assertEquals(setOf("Oil change" to "EVERY 5000 MILES", "Hitch service" to "EVERY 10000 MILES"), exported.schedule.map { it.name to it.every }.toSet())
        assertEquals(41_000, exported.schedule.single { it.name == "Oil change" }.lastDone?.odometer)
        assertEquals(mapOf(oil to ServiceGroup.OIL_ENGINE, own to ServiceGroup.SUSPENSION), exported.report!!.itemCategories)

        val shop = exported.records.single { !it.diy }
        assertEquals("Corner Garage", shop.doneBy)
        assertEquals(listOf("receipt.enc"), shop.photoFiles)
        // The owner's own item's line is in that item's group, as on the timeline.
        assertEquals(listOf(ServiceGroup.OIL_ENGINE, ServiceGroup.SUSPENSION, ServiceGroup.OTHER), shop.summary.groups)
        assertEquals(listOf(0, 0, null), shop.summary.jobOf)
        assertEquals(listOf(ExportPartRow("Oil filter", 1_000, 1_500, 1_500, "Oil & filter change")), shop.summary.jobs.single().rows)
        assertEquals(15_000L, shop.costCents)
        // 150.00 less 60.00 labor, 15.00 parts and 6.00 tax.
        assertEquals(6_900L, shop.summary.breakdown!!.gapCents)
        assertEquals("OWNER", exported.records.single { it.diy }.doneBy)
    }

    @Test
    fun `a vehicle with nothing to name it by is the unknown vehicle, and a shop with no name is a shop`() = runBlocking {
        val car = db.vehicles().insert(testVehicle(year = 0, make = "", model = ""))
        db.service().insertWithItems(ServiceRecordEntity(vehicleId = car, date = day, odometer = 1_000, diy = false), listOf(ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Wipers")))
        val exported = builder.all(names, day).single().second
        assertEquals("UNKNOWN", exported.title)
        assertEquals("A SHOP", exported.records.single().doneBy)
        assertNull(exported.records.single().summary.breakdown)
    }
}
