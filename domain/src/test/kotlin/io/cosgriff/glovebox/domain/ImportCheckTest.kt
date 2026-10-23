package io.cosgriff.glovebox.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ImportCheckTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun `a run of years has both ends, in order`() {
        val check = ImportCheck()
        assertEquals(2017..2024, check.yearRange(json("""{ "from": 2017, "to": 2024 }"""), "years"))
        assertNull(check.yearRange(json("""{ "from": 2024, "to": 2017 }"""), "years"))
        assertEquals(listOf("years.to: is before years.from"), check.invalid().errors.map { "${it.path}: ${it.message}" })
    }

    @Test
    fun `a date is written YYYY-MM-DD and is a day the calendar has`() {
        val check = ImportCheck()
        assertEquals(LocalDate.of(2026, 10, 12), check.date(json("\"2026-10-12\""), "retrieved"))
        assertNull(check.date(json("\"12/10/2026\""), "retrieved"))
        assertNull(check.date(json("\"2026-02-30\""), "retrieved"))
        assertEquals(2, check.invalid().errors.size)
    }
}
