package io.cosgriff.glovebox.ui.settings

import io.cosgriff.glovebox.R
import io.cosgriff.glovebox.domain.BundledLibrary
import io.cosgriff.glovebox.domain.Licence
import io.cosgriff.glovebox.domain.LicenceGroup
import io.cosgriff.glovebox.domain.LicenceName
import io.cosgriff.glovebox.domain.LockDelay
import io.cosgriff.glovebox.domain.NoticePrefs
import io.cosgriff.glovebox.domain.PasswordStrength
import io.cosgriff.glovebox.domain.RecallCadence
import io.cosgriff.glovebox.ui.screens.ScreenshotApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The words for Settings' values, the notifications and the backup, as screens and workers say them. */
@RunWith(RobolectricTestRunner::class)
@Config(application = ScreenshotApp::class, sdk = [35])
class SettingsWordsTest {
    @Test
    fun `each choice has its own words`() {
        assertEquals(listOf("Immediately", "After 1 min", "After 5 min", "On restart"), LockDelay.entries.map { it.label })
        assertEquals(listOf("Weekly", "Monthly", "Never"), RecallCadence.entries.map { it.label })
        assertEquals(listOf("weekly", "monthly", "never"), RecallCadence.entries.map { it.inSentence })
        assertEquals(listOf("Weak", "Fair", "Strong", "Very strong"), PasswordStrength.entries.map { it.label })
    }

    @Test
    fun `a check-in is said as a week, two weeks, a month, or a number of days`() {
        assertEquals("Every week", checkInLabel(7))
        assertEquals("Every 2 weeks", checkInLabel(14))
        assertEquals("Every month", checkInLabel(30))
        assertEquals(counted(R.plurals.cadence_every_days, 10, "10"), checkInLabel(10))
    }

    @Test
    fun `the theme says what is showing and whether it follows the phone`() {
        assertEquals("Follows your phone — currently dark", themeText(followsPhone = true, dark = true))
        assertEquals("Always light, whatever your phone is set to", themeText(followsPhone = false, dark = false))
    }

    @Test
    fun `notifications are On only when every kind is`() {
        val all = NoticePrefs()
        assertEquals(text(R.string.on), notificationsText(all))
        assertEquals(text(R.string.off), notificationsText(all.copy(serviceDue = false, overdue = false, recalls = false, mileage = false)))
        assertEquals("3 of 4 on", notificationsText(all.copy(recalls = false)))
    }

    @Test
    fun `the schedule row names the one vehicle, counts several, and is left out with none`() {
        assertNull(schedulesText(emptyList()))
        assertEquals("2019 4Runner", schedulesText(listOf("2019 4Runner")))
        assertEquals(counted(R.plurals.garage_vehicles, 2, "2"), schedulesText(listOf("2019 4Runner", "2013 GTI")))
    }

    @Test
    fun `what a backup holds names documents and their size only when there are some`() {
        val base = listOf(counted(R.plurals.garage_vehicles, 2, "2"), counted(R.plurals.records, 14, "14"), counted(R.plurals.photos, 9, "9"))
        assertEquals(base.joinToString(" · "), backupContents(2, 14, 9))
        assertEquals(4, backupContents(2, 14, 9, documents = 3, documentBytes = 2_000_000).split(" · ").size)
    }

    @Test
    fun `a backup's contents are listed with an and before the last`() {
        val three = restoreSummary(1, 2, 0, 0)
        assertEquals(
            text(R.string.list_and, counted(R.plurals.garage_vehicles, 1, "1") + ", " + counted(R.plurals.service_records, 2, "2"), counted(R.plurals.photos, 0, "0")),
            three,
        )
        assertEquals(true, restoreSummary(1, 2, 3, 4).endsWith(text(R.string.list_and, "", counted(R.plurals.documents, 4, "4")).trimStart()))
    }

    @Test
    fun `services are named when one or two, counted when more`() {
        assertEquals("Tire rotation", servicesText(listOf("Tire rotation")))
        assertEquals(text(R.string.list_and, "Tire rotation", "Oil change"), servicesText(listOf("Tire rotation", "Oil change")))
        assertEquals(counted(R.plurals.services, 3, "3"), servicesText(listOf("a", "b", "c")))
    }

    @Test
    fun `a time is said in the app's language`() {
        assertEquals("8:05 AM", timeLabel(8, 5).replace(' ', ' '))
    }

    @Test
    fun `a licence group is called by its name, its library's, or said to be unrecorded`() {
        val licence = Licence("x", "X", null, null)
        val lib = BundledLibrary("a:b", "A", "1", listOf("x"))
        assertEquals("MIT", LicenceGroup(licence, LicenceName.Given("MIT"), listOf(lib)).title)
        assertEquals("Cipher licence", LicenceGroup(licence, LicenceName.OwnOf("Cipher"), listOf(lib)).title)
        assertEquals("Licence not recorded", LicenceGroup(licence, LicenceName.NotRecorded, listOf(lib)).title)
        val groups = listOf(LicenceGroup(licence, LicenceName.Given("MIT"), listOf(lib, lib.copy(id = "c:d"))), LicenceGroup(licence, LicenceName.NotRecorded, listOf(lib)))
        assertEquals(
            text(R.string.licences_summary, counted(R.plurals.libraries, 2, "2"), counted(R.plurals.licences, 2, "2")),
            licencesSummary(groups),
        )
    }
}
