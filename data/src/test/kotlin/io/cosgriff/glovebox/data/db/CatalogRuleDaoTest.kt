package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.domain.CatalogRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Through Android's own driver, as [MigrationTest] explains. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CatalogRuleDaoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, GloveboxDatabase::class.java).setDriver(AndroidSQLiteDriver()).build()
    private val rules = db.catalogRules()
    private val today = LocalDate.of(2026, 10, 20)

    @After fun close() = db.close()

    @Test
    fun `one rule per wording, whatever its case or spacing`() = runBlocking {
        rules.put("Water pump gasket replaced", "water-pump", today)
        rules.put("  water pump GASKET replaced", "cooling-check", today)
        rules.put("Tire(s) mounted", "new-tires", today)
        assertEquals(
            listOf(CatalogRule("Tire(s) mounted", "new-tires"), CatalogRule("water pump GASKET replaced", "cooling-check")),
            rules.all().map { it.toRule() }.sortedBy { it.text },
        )
    }

    @Test
    fun `filing lines and keeping rules is one step, and undoing it puts both back`() = runBlocking {
        val vehicleId = db.vehicles().insert(VehicleEntity(vin = null, year = 2019, make = "Toyota", model = "4Runner", createdOn = today))
        val recordId = db.service().insertWithItems(
            ServiceRecordEntity(vehicleId = vehicleId, date = today, odometer = 60_000, diy = false),
            listOf(
                ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Water pump gasket replaced"),
                ServiceRecordItemEntity(recordId = 0, scheduleItemId = null, label = "Hitch pin replaced"),
            ),
        )
        val (pump, hitch) = db.service().record(recordId)!!.items.sortedBy { it.id }
        // A rule for the hitch's wording already, which keeping a new one replaces.
        rules.put("Hitch pin replaced", "body", today)
        val before = rules.all()

        db.service().fileLines(
            mapOf("water-pump" to listOf(pump.id), "accessory" to listOf(hitch.id)),
            listOf(CatalogRule("Water pump gasket replaced", "water-pump").toEntity(today), CatalogRule("Hitch pin replaced", "accessory").toEntity(today)),
        )
        val filed = db.service().record(recordId)!!.items.sortedBy { it.id }
        assertEquals(listOf("water-pump", "accessory"), filed.map { it.catalogId })
        // The wording stays as written.
        assertEquals(listOf("Water pump gasket replaced", "Hitch pin replaced"), filed.map { it.label })
        assertEquals(setOf(CatalogRule("Water pump gasket replaced", "water-pump"), CatalogRule("Hitch pin replaced", "accessory")), rules.all().map { it.toRule() }.toSet())

        db.service().unfileLines(listOf(pump.id, hitch.id), addedKeys = listOf("water pump gasket replaced", "hitch pin replaced"), replaced = before)
        assertEquals(listOf(null, null), db.service().record(recordId)!!.items.map { it.catalogId })
        assertEquals(before, rules.all())
    }

    @Test
    fun `a rule's line can be changed, and a rule deleted`() = runBlocking {
        val id = rules.put("Hitch pin replaced", "accessory", today)
        rules.setLine(id, "body")
        assertEquals(listOf(CatalogRule("Hitch pin replaced", "body")), rules.all().map { it.toRule() })
        rules.delete(id)
        assertEquals(emptyList<CatalogRuleEntity>(), rules.all())
    }
}
