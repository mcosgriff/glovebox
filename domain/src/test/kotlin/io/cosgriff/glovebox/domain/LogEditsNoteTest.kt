package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LogEditsNoteTest {
    private val day = LocalDate.of(2026, 10, 10)
    private val readings = listOf(OdometerReading(LocalDate.of(2026, 6, 14), 60_100), OdometerReading(LocalDate.of(2026, 11, 1), 70_000))

    private fun note(entered: Int?, original: Int? = null, last: Int? = 60_100, errors: Boolean = false) =
        LogEdits.odometerNote(entered, original, day, readings, last, errors)

    @Test
    fun `a new service shows the last recorded reading`() {
        assertEquals(OdometerNote.LastRecorded(60_100), note(null))
        assertEquals(OdometerNote.LastRecorded(60_100), note(68_412))
        assertNull(note(null, last = null))
    }

    @Test
    fun `a missing reading is only an error once saving was tried`() {
        assertEquals(OdometerNote.Missing, note(null, errors = true))
    }

    @Test
    fun `an edit says what the reading was`() {
        assertEquals(OdometerNote.Was(68_412), note(68_450, original = 68_412))
        assertEquals(NoteTone.CHANGED, note(68_450, original = 68_412)!!.tone)
        assertEquals(OdometerNote.Unchanged, note(68_412, original = 68_412))
    }

    @Test
    fun `lower than an earlier reading warns and names it`() {
        assertEquals(OdometerNote.LowerThanEarlier(60_100), note(59_000, original = 68_412))
        assertEquals(NoteTone.WARNING, note(59_000)!!.tone)
    }

    @Test
    fun `a later reading is not an earlier one`() {
        // 70,000 was read after this service's date, so 68,412 is not below anything earlier.
        assertEquals(NoteTone.PLAIN, note(68_412)!!.tone)
    }

    @Test
    fun `who did it, and whether it was just added or restored`() {
        assertEquals(Who(DoneBy.Diy, false, null), LogEdits.who(diy = true, shopName = null, mark = null))
        assertEquals(Who(DoneBy.Diy, false, RecordMark.ADDED), LogEdits.who(diy = true, shopName = "ignored", mark = RecordMark.ADDED))
        assertEquals(Who(DoneBy.Shop(null), false, null), LogEdits.who(diy = false, shopName = " ", mark = null))
        assertEquals(Who(DoneBy.Shop("Corner Garage"), true, RecordMark.RESTORED), LogEdits.who(false, "Corner Garage", RecordMark.RESTORED, imported = true))
    }
}
