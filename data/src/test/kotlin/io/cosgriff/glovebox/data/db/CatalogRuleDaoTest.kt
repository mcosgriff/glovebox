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
    fun `a rule's line can be changed, and a rule deleted`() = runBlocking {
        val id = rules.put("Hitch pin replaced", "accessory", today)
        rules.setLine(id, "body")
        assertEquals(listOf(CatalogRule("Hitch pin replaced", "body")), rules.all().map { it.toRule() })
        rules.delete(id)
        assertEquals(emptyList<CatalogRuleEntity>(), rules.all())
    }
}
