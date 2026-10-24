package io.cosgriff.glovebox.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.cosgriff.glovebox.data.inMemoryDatabase
import io.cosgriff.glovebox.domain.CatalogRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CatalogRuleDaoInteractionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase(context)
    private val day = LocalDate.of(2026, 5, 4)

    @After fun close() = db.close()

    @Test
    fun `the rules screen lists rules by their wording, whatever its case, and each reads back as the rule it was`() = runBlocking {
        db.catalogRules().put("lube oil filter", "oil_change", day)
        db.catalogRules().put("Brake job", "brake_pads_front", day)
        db.catalogRules().put("  Cabin air  ", "cabin_filter", day)

        val shown = db.catalogRules().observeAll().first()
        assertEquals(listOf("Brake job", "Cabin air", "lube oil filter"), shown.map { it.text })
        assertEquals(CatalogRule("Brake job", "brake_pads_front"), shown.first().toRule())
    }
}
