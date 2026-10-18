package io.cosgriff.glovebox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecallsTest {
    private val today = LocalDate.of(2026, 10, 11)

    private fun notice(campaign: String, component: String = "STEERING", summary: String = "Summary", reportedOn: LocalDate? = null) =
        RecallNotice(campaign, component, summary, consequence = null, remedy = null, reportedOn = reportedOn)

    private val fuelPump = notice("20V682000", "FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP", reportedOn = LocalDate.of(2020, 11, 4))
    private val steering = notice("19V670000", "STEERING", reportedOn = LocalDate.of(2019, 9, 20))
    private val labels = notice("18V753000", "EQUIPMENT:OTHER:LABELS", reportedOn = LocalDate.of(2018, 10, 25))

    @Test
    fun `a first check adds everything`() {
        val changes = Recalls.merge(emptyList(), listOf(fuelPump, steering))
        assertEquals(listOf(fuelPump, steering), changes.added)
        assertTrue(changes.updated.isEmpty())
        assertTrue(changes.withdrawn.isEmpty())
    }

    @Test
    fun `an unchanged list changes nothing, done or not`() {
        val stored = listOf(StoredRecall(1, fuelPump, done = false), StoredRecall(2, steering, done = true))
        val changes = Recalls.merge(stored, listOf(steering, fuelPump))
        assertEquals(RecallChanges(emptyList(), emptyList(), emptyList()), changes)
    }

    @Test
    fun `a new campaign is added beside the ones already kept`() {
        val stored = listOf(StoredRecall(1, fuelPump, done = true))
        val changes = Recalls.merge(stored, listOf(fuelPump, steering))
        assertEquals(listOf(steering), changes.added)
        assertTrue(changes.updated.isEmpty())
    }

    @Test
    fun `reworded campaigns are updated by id`() {
        val stored = listOf(StoredRecall(7, fuelPump, done = true))
        val reworded = fuelPump.copy(remedy = "Dealers will replace the fuel pump.")
        val changes = Recalls.merge(stored, listOf(reworded))
        assertEquals(listOf(7L to reworded), changes.updated)
        assertTrue(changes.added.isEmpty())
    }

    @Test
    fun `a dropped campaign goes only while it is open`() {
        val stored = listOf(StoredRecall(1, fuelPump, done = false), StoredRecall(2, steering, done = true))
        val changes = Recalls.merge(stored, emptyList())
        assertEquals(listOf(1L), changes.withdrawn)
    }

    @Test
    fun `blank and repeated campaign numbers are ignored`() {
        val changes = Recalls.merge(emptyList(), listOf(notice(""), fuelPump, fuelPump.copy(summary = "Again")))
        assertEquals(listOf(fuelPump), changes.added)
    }

    @Test
    fun `report dates are day first`() {
        assertEquals(LocalDate.of(2018, 10, 25), Recalls.parseReportDate("25/10/2018"))
        assertEquals(LocalDate.of(2020, 11, 4), Recalls.parseReportDate("04/11/2020"))
        assertNull(Recalls.parseReportDate("10/25/2018"))
        assertNull(Recalls.parseReportDate(""))
        assertNull(Recalls.parseReportDate(null))
    }

    @Test
    fun `campaign numbers read the way NHTSA writes them`() {
        assertEquals("20V-682", Recalls.campaignLabel("20V682000"))
        assertEquals("19E-012", Recalls.campaignLabel("19e012000"))
        // Anything unexpected is shown as sent rather than guessed at.
        assertEquals("20V682001", Recalls.campaignLabel("20V682001"))
        assertEquals("ABC", Recalls.campaignLabel("abc"))
    }

    @Test
    fun `components are named by their last meaningful part`() {
        assertEquals("Fuel pump", Recalls.componentLabel(fuelPump.component))
        assertEquals("Steering", Recalls.componentLabel("STEERING"))
        assertEquals("Labels", Recalls.componentLabel("EQUIPMENT:OTHER:LABELS"))
        assertEquals("Electrical system", Recalls.componentLabel("ELECTRICAL SYSTEM:OTHER"))
        assertEquals("Other", Recalls.componentLabel("OTHER"))
        assertNull(Recalls.componentLabel(""))
    }

    @Test
    fun `a campaign reads the way NHTSA and dealers write it`() {
        assertEquals("20V-682", Recalls.campaignLabel(fuelPump.campaign))
    }

    @Test
    fun `a vehicle needs a real year, make and model to be checked`() {
        assertTrue(Recalls.canCheck(2019, "TOYOTA", "4Runner"))
        assertFalse(Recalls.canCheck(0, "TOYOTA", "4Runner"))
        assertFalse(Recalls.canCheck(2019, "Unknown", "4Runner"))
        assertFalse(Recalls.canCheck(2019, "TOYOTA", " "))
    }

    @Test
    fun `the list goes stale after a week`() {
        assertTrue(Recalls.isStale(null, today))
        assertFalse(Recalls.isStale(today, today))
        assertFalse(Recalls.isStale(today.minusDays(6), today))
        assertTrue(Recalls.isStale(today.minusDays(7), today))
    }

    @Test
    fun `open recalls come first, newest report first`() {
        val stored = listOf(
            StoredRecall(1, labels, done = false),
            StoredRecall(2, fuelPump, done = true),
            StoredRecall(3, steering, done = false),
            StoredRecall(4, notice("17V000000"), done = false),
        )
        assertEquals(listOf(3L, 1L, 4L, 2L), Recalls.sorted(stored).map { it.id })
    }

    @Test
    fun `one open recall is named, several are only counted`() {
        val one = listOf(StoredRecall(1, fuelPump, done = false), StoredRecall(2, steering, done = true))
        assertEquals(OpenRecalls(1, "Fuel pump"), Recalls.open(one))
        val two = one.map { it.copy(done = false) }
        assertEquals(OpenRecalls(2, null), Recalls.open(two))
        assertEquals(OpenRecalls(0, null), Recalls.open(emptyList()))
    }

    @Test
    fun `an archived recall isn't open, and sits between open and done`() {
        val stored = listOf(
            StoredRecall(1, fuelPump, done = true),
            StoredRecall(2, steering, done = false, archived = true),
            StoredRecall(3, labels, done = false),
        )
        assertEquals(listOf(3L, 2L, 1L), Recalls.sorted(stored).map { it.id })
        assertEquals(OpenRecalls(1, Recalls.componentLabel(labels.component)), Recalls.open(stored))
        // NHTSA dropping it takes it away, as it would an open one.
        assertEquals(listOf(2L, 3L), Recalls.merge(stored, emptyList()).withdrawn)
    }

    @Test
    fun `manufacturer lookup matches the make however it is cased`() {
        assertEquals("Toyota", Recalls.manufacturerSite("TOYOTA")?.name)
        assertEquals("Mercedes-Benz", Recalls.manufacturerSite(" mercedes-benz ")?.name)
        assertNull(Recalls.manufacturerSite("DeLorean"))
    }

    @Test
    fun `makes are written the way people write them`() {
        assertEquals("GMC", Recalls.makeName("GMC"))
        assertEquals("Toyota", Recalls.makeName("TOYOTA"))
        assertEquals("Land Rover", Recalls.makeName("LAND ROVER"))
    }
}
